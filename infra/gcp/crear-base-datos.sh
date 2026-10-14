#!/usr/bin/env bash
# KAN-9 · Base de datos MySQL en Google Cloud con ambientes de desarrollo y producción (ADR-22).
#
# Crea, si no existen, en el proyecto de Firebase / Google Cloud:
#   - una instancia de Cloud SQL para MySQL 8.4 (edición Enterprise, db-f1-micro,
#     10 GB SSD, respaldo diario con 7 copias y recuperación a un punto en el tiempo);
#   - las bases cocina_desarrollo y cocina_produccion;
#   - un usuario por ambiente (app_desarrollo, app_produccion) con permisos solo
#     sobre su base.
#
# Se ejecuta en Cloud Shell (console.cloud.google.com, ícono >_), que ya trae
# gcloud, mysql y openssl y tiene la sesión iniciada:
#
#   git clone -b chore/KAN-9-base-datos-nube https://github.com/JCamiloLopezF/cocina-sin-estres.git
#   cd cocina-sin-estres
#   bash infra/gcp/crear-base-datos.sh <id-del-proyecto>
#
# Guía completa: docs/despliegue/base-de-datos.md
#
# Contraseñas:
#   - La de root la escribes tú (no se muestra ni se guarda). Se pide dos veces más:
#     al final, el cliente mysql la pide para crear los usuarios.
#   - Las de los usuarios de la aplicación se generan al azar y se muestran UNA vez.
#     Ningún valor se escribe en el repositorio: es público.
#   - Si vuelves a ejecutar el script, esas contraseñas cambian (sirve para rotarlas).
set -euo pipefail

PROYECTO="${1:-$(gcloud config get-value project 2>/dev/null || true)}"
REGION="${REGION:-us-east1}"
INSTANCIA="${INSTANCIA:-cocina-sin-estres-mysql}"
# db-f1-micro es la más barata (unos USD 8 al mes). Si Google la rechaza para
# MySQL 8.4, ejecuta de nuevo con TIER=db-g1-small (unos USD 26 al mes).
TIER="${TIER:-db-f1-micro}"
AMBIENTES=(desarrollo produccion)

falla() { echo "ERROR: $*" >&2; exit 1; }

for comando in gcloud mysql openssl; do
  command -v "$comando" >/dev/null 2>&1 || falla "falta '$comando'. Ejecuta este script en Cloud Shell."
done

[[ -n "$PROYECTO" ]] || falla "indica el id del proyecto: bash infra/gcp/crear-base-datos.sh <id-del-proyecto>"
gcloud projects describe "$PROYECTO" >/dev/null 2>&1 \
  || falla "no encuentro el proyecto '$PROYECTO'. Míralo en la consola, arriba a la izquierda (es el ID, no el nombre)."

if [[ "$(gcloud billing projects describe "$PROYECTO" --format='value(billingEnabled)' 2>/dev/null)" != "True" ]]; then
  falla "el proyecto '$PROYECTO' no tiene una cuenta de facturación vinculada. Vincula la de la prueba gratuita (guía, paso 2)."
fi

echo "Proyecto:   $PROYECTO"
echo "Región:     $REGION"
echo "Instancia:  $INSTANCIA (Cloud SQL, MySQL 8.4, Enterprise, $TIER, 10 GB SSD)"
echo "Bases:      ${AMBIENTES[*]/#/cocina_}"
echo
read -r -p "¿Crear estos recursos? (s/N) " respuesta
[[ "$respuesta" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

gcloud config set project "$PROYECTO" >/dev/null 2>&1

echo "Activando la API de Cloud SQL..."
gcloud services enable sqladmin.googleapis.com --project="$PROYECTO"

# ─── Instancia ─────────────────────────────────────────────────────────────────
if gcloud sql instances describe "$INSTANCIA" --project="$PROYECTO" >/dev/null 2>&1; then
  echo "La instancia $INSTANCIA ya existe; no se modifica."
else
  echo "Elige la contraseña del usuario root de MySQL."
  echo "Mínimo 12 caracteres, con al menos tres de: mayúsculas, minúsculas, números y símbolos."
  echo "Guárdala en tu gestor de contraseñas: solo tú la usas, nunca la aplicación."
  while :; do
    read -r -s -p "Contraseña: " ROOT_CONTRASENA; echo
    read -r -s -p "Repítela:   " repetida; echo
    clases=0
    [[ "$ROOT_CONTRASENA" =~ [A-Z] ]] && clases=$((clases + 1))
    [[ "$ROOT_CONTRASENA" =~ [a-z] ]] && clases=$((clases + 1))
    [[ "$ROOT_CONTRASENA" =~ [0-9] ]] && clases=$((clases + 1))
    [[ "$ROOT_CONTRASENA" =~ [^A-Za-z0-9] ]] && clases=$((clases + 1))
    if [[ "$ROOT_CONTRASENA" != "$repetida" ]]; then
      echo "No coinciden. Otra vez."
    elif (( ${#ROOT_CONTRASENA} < 12 || clases < 3 )); then
      echo "Muy débil. Otra vez."
    else
      break
    fi
  done
  unset repetida

  echo "Creando la instancia (tarda entre 5 y 15 minutos)..."
  # --edition=enterprise es OBLIGATORIO: para MySQL 8.4 Google usa por defecto
  # Enterprise Plus, que cuesta unas 25 veces más y no admite db-f1-micro.
  # Sin alta disponibilidad (zonal), disco fijo de 10 GB y sin redes autorizadas:
  # solo se entra con el conector de Cloud SQL (Cloud Run, KAN-11), con
  # 'gcloud sql connect' o agregando tu IP a mano (ver la guía).
  gcloud sql instances create "$INSTANCIA" \
    --project="$PROYECTO" \
    --region="$REGION" \
    --database-version=MYSQL_8_4 \
    --edition=enterprise \
    --tier="$TIER" \
    --storage-type=SSD \
    --storage-size=10 \
    --no-storage-auto-increase \
    --availability-type=zonal \
    --backup-start-time=08:00 \
    --retained-backups-count=7 \
    --enable-bin-log \
    --retained-transaction-log-days=7 \
    --ssl-mode=ENCRYPTED_ONLY \
    --deletion-protection \
    --root-password="$ROOT_CONTRASENA"
  unset ROOT_CONTRASENA
fi

# ─── Bases ─────────────────────────────────────────────────────────────────────
for ambiente in "${AMBIENTES[@]}"; do
  base="cocina_${ambiente}"
  if gcloud sql databases describe "$base" --instance="$INSTANCIA" --project="$PROYECTO" >/dev/null 2>&1; then
    echo "La base $base ya existe."
  else
    echo "Creando la base $base..."
    gcloud sql databases create "$base" --instance="$INSTANCIA" --project="$PROYECTO" \
      --charset=utf8mb4 --collation=utf8mb4_unicode_ci
  fi
done

# ─── Usuarios por ambiente ─────────────────────────────────────────────────────
generar_contrasena() {
  # 32 letras y números: sin símbolos, para no tener que escaparla en la URL
  # JDBC ni en los secretos. Siempre con mayúscula, minúscula y número.
  local c
  while :; do
    c="$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | cut -c1-32)"
    if [[ ${#c} -eq 32 && "$c" =~ [A-Z] && "$c" =~ [a-z] && "$c" =~ [0-9] ]]; then
      echo "$c"
      return
    fi
  done
}

# Los usuarios se crean con SQL y no con 'gcloud sql users create', porque los
# que crea gcloud reciben permisos sobre TODAS las bases (rol cloudsqlsuperuser).
directorio="$(mktemp -d)"
chmod 700 "$directorio"
trap 'rm -rf "$directorio"' EXIT
sql="$directorio/usuarios.sql"
: > "$sql"
chmod 600 "$sql"

declare -A CONTRASENAS
for ambiente in "${AMBIENTES[@]}"; do
  contrasena="$(generar_contrasena)"
  CONTRASENAS[$ambiente]="$contrasena"
  cat >> "$sql" <<SQL
CREATE USER IF NOT EXISTS 'app_${ambiente}'@'%' IDENTIFIED BY '${contrasena}';
ALTER USER 'app_${ambiente}'@'%' IDENTIFIED BY '${contrasena}';
-- Todos los permisos, pero solo sobre su base: Flyway crea y altera las tablas (ADR-07).
GRANT ALL PRIVILEGES ON \`cocina_${ambiente}\`.* TO 'app_${ambiente}'@'%';
SQL
done

echo
echo "Ahora se crean los usuarios de la aplicación. gcloud autoriza tu Cloud Shell por"
echo "unos minutos y el cliente mysql te pide la CONTRASEÑA DE ROOT (Enter password:)."
if ! gcloud sql connect "$INSTANCIA" --user=root --project="$PROYECTO" --quiet < "$sql"; then
  falla "no se pudieron crear los usuarios. Revisa la contraseña de root y vuelve a ejecutar el script (no se pierde nada)."
fi

CONEXION="$(gcloud sql instances describe "$INSTANCIA" --project="$PROYECTO" --format='value(connectionName)')"
IP="$(gcloud sql instances describe "$INSTANCIA" --project="$PROYECTO" --format='value(ipAddresses[0].ipAddress)')"

# ─── Resultado ─────────────────────────────────────────────────────────────────
cat <<FIN

Listo. Estas son las credenciales de cada ambiente. Se muestran solo esta vez:
guárdalas ya en tu gestor de contraseñas y como secretos de GitHub
(Settings > Environments > desarrollo / produccion). Nunca en el repositorio.

Nombre de conexión de la instancia: ${CONEXION}
FIN

for ambiente in "${AMBIENTES[@]}"; do
  cat <<FIN

── ${ambiente} ──────────────────────────────────────────────
DB_USUARIO=app_${ambiente}
DB_CONTRASENA=${CONTRASENAS[$ambiente]}
# Desde Cloud Run (KAN-11), con el conector de Cloud SQL:
DB_URL=jdbc:mysql:///cocina_${ambiente}?cloudSqlInstance=${CONEXION}&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC
# Desde tu computador, después de autorizar tu IP (ver la guía):
#   jdbc:mysql://${IP}:3306/cocina_${ambiente}?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&sslMode=REQUIRED
FIN
done

cat <<FIN

Cuando las hayas guardado, borra la pantalla con:  clear
Para revisar la instancia y los respaldos:          bash infra/gcp/verificar-base-datos.sh
FIN
