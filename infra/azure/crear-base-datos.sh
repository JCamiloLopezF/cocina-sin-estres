#!/usr/bin/env bash
# KAN-9 · Base de datos MySQL en Azure con ambientes de desarrollo y producción (ADR-22).
#
# Crea, si no existen:
#   - el grupo de recursos rg-cocina-sin-estres;
#   - un servidor Azure Database for MySQL Flexible Server (Burstable B1ms, 32 GB,
#     MySQL 8.4, respaldo diario con 7 días de retención);
#   - las bases cocina_desarrollo y cocina_produccion;
#   - un usuario por ambiente (app_desarrollo, app_produccion) con permisos solo
#     sobre su base y conexión cifrada obligatoria.
#
# Se ejecuta en Azure Cloud Shell (Bash), que ya trae az, mysql y openssl:
#
#   git clone https://github.com/JCamiloLopezF/cocina-sin-estres.git
#   cd cocina-sin-estres
#   bash infra/azure/crear-base-datos.sh <region>
#
# Sin región, muestra las regiones que permite la suscripción y termina.
# Guía completa: docs/despliegue/base-de-datos.md
#
# Contraseñas:
#   - La del administrador la escribes tú (no se muestra ni se guarda).
#   - Las de los usuarios de la aplicación se generan al azar y se muestran UNA vez
#     al final. Ningún valor se escribe en archivos: el repositorio es público.
#   - Si vuelves a ejecutar el script, esas contraseñas cambian (sirve para rotarlas)
#     y hay que actualizar los secretos donde estén guardadas.
set -euo pipefail

GRUPO="${GRUPO:-rg-cocina-sin-estres}"
SERVIDOR="${SERVIDOR:-cocina-sin-estres-mysql}"
ADMIN_USUARIO="${ADMIN_USUARIO:-cocinaadmin}"
VERSION_MYSQL="${VERSION_MYSQL:-8.4}"
AMBIENTES=(desarrollo produccion)
REGION="${1:-}"

falla() { echo "ERROR: $*" >&2; exit 1; }

for comando in az mysql openssl; do
  command -v "$comando" >/dev/null 2>&1 || falla "falta '$comando'. Ejecuta este script en Azure Cloud Shell (Bash)."
done

az account show >/dev/null 2>&1 || falla "no hay sesión de Azure. Ejecuta 'az login' o usa Cloud Shell."

if [[ -z "$REGION" ]]; then
  echo "Uso: bash infra/azure/crear-base-datos.sh <region>"
  echo
  echo "Regiones que permite tu suscripción (política 'Allowed resource deployment regions'):"
  if ! az policy assignment list \
      --query "[?displayName=='Allowed resource deployment regions'].parameters.listOfAllowedLocations.value | [0]" \
      -o tsv 2>/dev/null | grep .; then
    echo "  No pude leerlas. Búscalas en el portal: Policy > Assignments >"
    echo "  'Allowed resource deployment regions' > parámetro 'Allowed locations'."
  fi
  exit 1
fi

SUSCRIPCION="$(az account show --query name -o tsv)"
HOST="${SERVIDOR}.mysql.database.azure.com"

echo "Suscripción:  $SUSCRIPCION"
echo "Región:       $REGION"
echo "Grupo:        $GRUPO"
echo "Servidor:     $HOST (MySQL $VERSION_MYSQL, Burstable B1ms, 32 GB)"
echo "Bases:        ${AMBIENTES[*]/#/cocina_}"
echo
read -r -p "¿Crear estos recursos? (s/N) " respuesta
[[ "$respuesta" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

# ─── Contraseña del administrador ──────────────────────────────────────────────
servidor_existe=false
if az mysql flexible-server show -g "$GRUPO" -n "$SERVIDOR" -o none 2>/dev/null; then
  servidor_existe=true
fi

if $servidor_existe; then
  echo "El servidor ya existe: escribe la contraseña del administrador '$ADMIN_USUARIO'."
  read -r -s -p "Contraseña: " ADMIN_CONTRASENA; echo
else
  echo "Elige la contraseña del administrador '$ADMIN_USUARIO'."
  echo "Mínimo 12 caracteres, con al menos tres de: mayúsculas, minúsculas, números y símbolos."
  echo "Guárdala en tu gestor de contraseñas: solo tú la usas, nunca la aplicación."
  while :; do
    read -r -s -p "Contraseña: " ADMIN_CONTRASENA; echo
    read -r -s -p "Repítela:   " repetida; echo
    clases=0
    [[ "$ADMIN_CONTRASENA" =~ [A-Z] ]] && clases=$((clases + 1))
    [[ "$ADMIN_CONTRASENA" =~ [a-z] ]] && clases=$((clases + 1))
    [[ "$ADMIN_CONTRASENA" =~ [0-9] ]] && clases=$((clases + 1))
    [[ "$ADMIN_CONTRASENA" =~ [^A-Za-z0-9] ]] && clases=$((clases + 1))
    if [[ "$ADMIN_CONTRASENA" != "$repetida" ]]; then
      echo "No coinciden. Otra vez."
    elif (( ${#ADMIN_CONTRASENA} < 12 || clases < 3 )); then
      echo "Muy débil. Otra vez."
    else
      break
    fi
  done
  unset repetida
fi

# ─── Grupo de recursos y servidor ──────────────────────────────────────────────
echo "Creando el grupo de recursos..."
az group create -n "$GRUPO" -l "$REGION" -o none

if $servidor_existe; then
  echo "El servidor $SERVIDOR ya existe; no se modifica."
else
  echo "Creando el servidor MySQL (tarda entre 5 y 15 minutos)..."
  # Valores elegidos para quedar dentro de la oferta gratuita (750 h/mes de B1ms,
  # 32 GB de almacenamiento y 32 GB de respaldo): sin alta disponibilidad, sin
  # respaldo geográfico y sin crecimiento automático del disco.
  # --public-access 0.0.0.0 permite conexiones solo desde servicios de Azure
  # (Container Apps y Cloud Shell). Tu computador se agrega aparte (ver la guía).
  az mysql flexible-server create \
    --resource-group "$GRUPO" \
    --name "$SERVIDOR" \
    --location "$REGION" \
    --admin-user "$ADMIN_USUARIO" \
    --admin-password "$ADMIN_CONTRASENA" \
    --version "$VERSION_MYSQL" \
    --tier Burstable \
    --sku-name Standard_B1ms \
    --storage-size 32 \
    --storage-auto-grow Disabled \
    --auto-scale-iops Disabled \
    --backup-retention 7 \
    --geo-redundant-backup Disabled \
    --high-availability Disabled \
    --public-access 0.0.0.0 \
    --tags proyecto=cocina-sin-estres \
    --yes \
    -o none
fi

# Conexión cifrada obligatoria (ya viene así; se fija por si alguien la cambió).
az mysql flexible-server parameter set -g "$GRUPO" -s "$SERVIDOR" \
  -n require_secure_transport -v ON -o none

# ─── Bases y usuarios por ambiente ─────────────────────────────────────────────
generar_contrasena() {
  # 32 letras y números: sin símbolos, para que no haya que escaparla en la URL
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

export MYSQL_PWD="$ADMIN_CONTRASENA"
mysql_admin() {
  mysql --host="$HOST" --user="$ADMIN_USUARIO" --protocol=TCP --ssl-mode=REQUIRED "$@"
}

mysql_admin -e "SELECT 1" >/dev/null 2>&1 \
  || falla "no pude conectarme a $HOST como $ADMIN_USUARIO. ¿Contraseña correcta? ¿Existe la regla de firewall de servicios de Azure?"

declare -A CONTRASENAS
for ambiente in "${AMBIENTES[@]}"; do
  base="cocina_${ambiente}"
  usuario="app_${ambiente}"
  contrasena="$(generar_contrasena)"
  CONTRASENAS[$ambiente]="$contrasena"
  echo "Configurando $base y $usuario..."
  # El SQL va por la entrada estándar: la contraseña no aparece en la lista de procesos.
  mysql_admin <<SQL
CREATE DATABASE IF NOT EXISTS \`${base}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${usuario}'@'%' IDENTIFIED BY '${contrasena}' REQUIRE SSL;
ALTER USER '${usuario}'@'%' IDENTIFIED BY '${contrasena}' REQUIRE SSL;
-- Todos los permisos, pero solo sobre su base: Flyway crea y altera las tablas (ADR-07).
GRANT ALL PRIVILEGES ON \`${base}\`.* TO '${usuario}'@'%';
SQL
done

unset MYSQL_PWD ADMIN_CONTRASENA

# ─── Resultado ─────────────────────────────────────────────────────────────────
cat <<FIN

Listo. Estas son las variables de cada ambiente. Se muestran solo esta vez:
guárdalas ya en tu gestor de contraseñas y como secretos de GitHub
(Settings > Environments > desarrollo / produccion). Nunca en el repositorio.
FIN

for ambiente in "${AMBIENTES[@]}"; do
  cat <<FIN

── ${ambiente} ──────────────────────────────────────────────
DB_URL=jdbc:mysql://${HOST}:3306/cocina_${ambiente}?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&sslMode=REQUIRED
DB_USUARIO=app_${ambiente}
DB_CONTRASENA=${CONTRASENAS[$ambiente]}
FIN
done

cat <<FIN

Cuando las hayas guardado, borra la pantalla con:  clear
Para revisar el servidor y los respaldos:          bash infra/azure/verificar-base-datos.sh
FIN
