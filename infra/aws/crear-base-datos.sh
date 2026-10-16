#!/usr/bin/env bash
# KAN-9 · Base de datos MySQL en AWS con ambientes de desarrollo y producción (ADR-22).
#
# Crea, si no existen, en la región us-east-1 (Norte de Virginia):
#   - un grupo de seguridad sin entradas abiertas (cocina-sin-estres-mysql);
#   - un grupo de parámetros que exige conexiones cifradas (require_secure_transport);
#   - una instancia de Amazon RDS para MySQL 8.4 (db.t4g.micro, 20 GB gp3, una sola
#     zona, cifrada, respaldo diario con 1 día de retención, protección contra borrado);
#   - las bases cocina_desarrollo y cocina_produccion y un usuario por ambiente
#     (app_desarrollo, app_produccion) con permisos solo sobre su base.
#
# Se ejecuta en AWS CloudShell (ícono >_ en la barra de la consola), que ya trae
# aws, git, python3 y openssl y tiene la sesión iniciada:
#
#   git clone -b chore/KAN-9-base-datos-nube https://github.com/JCamiloLopezF/cocina-sin-estres.git
#   cd cocina-sin-estres
#   bash infra/aws/crear-base-datos.sh
#
# Guía completa: docs/despliegue/base-de-datos.md
#
# Contraseñas:
#   - La del administrador la escribes tú (no se muestra ni se guarda).
#   - Las de los usuarios de la aplicación se generan al azar y se muestran UNA vez.
#     Ningún valor se escribe en el repositorio: es público.
#   - Si vuelves a ejecutar el script, esas contraseñas cambian (sirve para rotarlas).
set -euo pipefail

export AWS_DEFAULT_REGION="${REGION:-us-east-1}"
# Sin paginador: si no, la salida larga se abre en less y el script se detiene.
export AWS_PAGER=""
INSTANCIA="${INSTANCIA:-cocina-sin-estres-mysql}"
GRUPO_SEGURIDAD="${INSTANCIA}"
GRUPO_PARAMETROS="${INSTANCIA}-tls"
ADMIN_USUARIO="${ADMIN_USUARIO:-cocinaadmin}"
CLASE="${CLASE:-db.t4g.micro}"
# El plan gratuito de AWS no permite más de 1 día de retención (FreeTierRestrictionError).
# Con una cuenta de pago se puede subir: RETENCION=7 bash infra/aws/crear-base-datos.sh
RETENCION="${RETENCION:-1}"
AMBIENTES=(desarrollo produccion)
AQUI="$(cd "$(dirname "$0")" && pwd)"

falla() { echo "ERROR: $*" >&2; exit 1; }

for comando in aws python3 openssl curl; do
  command -v "$comando" >/dev/null 2>&1 || falla "falta '$comando'. Ejecuta este script en AWS CloudShell."
done
aws sts get-caller-identity >/dev/null 2>&1 || falla "no hay sesión de AWS. Abre CloudShell desde la consola."

CUENTA="$(aws sts get-caller-identity --query Account --output text)"
echo "Cuenta:     $CUENTA"
echo "Región:     $AWS_DEFAULT_REGION"
echo "Instancia:  $INSTANCIA (RDS, MySQL 8.4, $CLASE, 20 GB gp3, una zona)"
echo "Respaldo:   diario, retención de $RETENCION día(s)"
echo "Bases:      ${AMBIENTES[*]/#/cocina_}"
echo
read -r -p "¿Crear estos recursos? (s/N) " respuesta
[[ "$respuesta" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

# ─── Cliente de MySQL para Python (CloudShell no trae cliente mysql) ───────────
# En una carpeta propia: el Python de CloudShell es un entorno virtual y no acepta --user.
LIBS_PYTHON="$HOME/.cocina-python"
export PYTHONPATH="$LIBS_PYTHON${PYTHONPATH:+:$PYTHONPATH}"
python3 -c "import pymysql" 2>/dev/null || python3 -m pip install --quiet --target "$LIBS_PYTHON" pymysql

# ─── Grupo de seguridad (firewall) ─────────────────────────────────────────────
VPC="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
[[ "$VPC" != "None" && -n "$VPC" ]] || falla "la cuenta no tiene VPC predeterminada en $AWS_DEFAULT_REGION."

SG="$(aws ec2 describe-security-groups --filters "Name=group-name,Values=$GRUPO_SEGURIDAD" "Name=vpc-id,Values=$VPC" \
  --query 'SecurityGroups[0].GroupId' --output text)"
if [[ "$SG" == "None" || -z "$SG" ]]; then
  echo "Creando el grupo de seguridad (sin entradas abiertas)..."
  SG="$(aws ec2 create-security-group --group-name "$GRUPO_SEGURIDAD" --vpc-id "$VPC" \
    --description "MySQL de Cocina sin estres: solo IPs autorizadas a mano y el backend" \
    --query GroupId --output text)"
fi

# ─── Grupo de parámetros: solo conexiones cifradas ─────────────────────────────
if ! aws rds describe-db-parameter-groups --db-parameter-group-name "$GRUPO_PARAMETROS" >/dev/null 2>&1; then
  echo "Creando el grupo de parámetros (exige TLS)..."
  aws rds create-db-parameter-group --db-parameter-group-name "$GRUPO_PARAMETROS" \
    --db-parameter-group-family mysql8.4 --description "Cocina sin estres: exige TLS" >/dev/null
fi
aws rds modify-db-parameter-group --db-parameter-group-name "$GRUPO_PARAMETROS" \
  --parameters "ParameterName=require_secure_transport,ParameterValue=1,ApplyMethod=immediate" >/dev/null

# ─── Instancia ─────────────────────────────────────────────────────────────────
if aws rds describe-db-instances --db-instance-identifier "$INSTANCIA" >/dev/null 2>&1; then
  echo "La instancia $INSTANCIA ya existe; no se modifica."
  aws rds wait db-instance-available --db-instance-identifier "$INSTANCIA"
  echo "Escribe la contraseña del administrador '$ADMIN_USUARIO'."
  read -r -s -p "Contraseña: " ADMIN_CONTRASENA; echo
else
  echo "Elige la contraseña del administrador '$ADMIN_USUARIO'."
  echo "Entre 12 y 41 caracteres, con al menos tres de: mayúsculas, minúsculas, números y"
  echo "símbolos (sin / \" ' @ ni espacios, que RDS no acepta)."
  echo "Guárdala en tu gestor de contraseñas: solo tú la usas, nunca la aplicación."
  prohibidos="[/\"'@[:space:]]"
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
    elif [[ "$ADMIN_CONTRASENA" =~ $prohibidos ]]; then
      echo "No uses / \" ' @ ni espacios. Otra vez."
    elif (( ${#ADMIN_CONTRASENA} < 12 || ${#ADMIN_CONTRASENA} > 41 || clases < 3 )); then
      echo "No cumple el largo o es muy débil. Otra vez."
    else
      break
    fi
  done
  unset repetida

  VERSION="$(aws rds describe-db-engine-versions --engine mysql --engine-version 8.4 --default-only \
    --query 'DBEngineVersions[0].EngineVersion' --output text)"
  [[ "$VERSION" != "None" && -n "$VERSION" ]] || falla "no encontré MySQL 8.4 en RDS para esta región."

  echo "Creando la instancia MySQL $VERSION (tarda entre 10 y 20 minutos)..."
  # Una sola zona (sin Multi-AZ), disco fijo de 20 GB sin crecimiento automático:
  # es lo que cubre el plan gratuito con sus créditos.
  aws rds create-db-instance \
    --db-instance-identifier "$INSTANCIA" \
    --engine mysql \
    --engine-version "$VERSION" \
    --db-instance-class "$CLASE" \
    --allocated-storage 20 \
    --storage-type gp3 \
    --storage-encrypted \
    --no-multi-az \
    --master-username "$ADMIN_USUARIO" \
    --master-user-password "$ADMIN_CONTRASENA" \
    --vpc-security-group-ids "$SG" \
    --db-parameter-group-name "$GRUPO_PARAMETROS" \
    --publicly-accessible \
    --backup-retention-period "$RETENCION" \
    --preferred-backup-window 08:00-08:30 \
    --preferred-maintenance-window sun:09:00-sun:09:30 \
    --deletion-protection \
    --copy-tags-to-snapshot \
    --tags Key=proyecto,Value=cocina-sin-estres >/dev/null
  aws rds wait db-instance-available --db-instance-identifier "$INSTANCIA"
fi

HOST="$(aws rds describe-db-instances --db-instance-identifier "$INSTANCIA" \
  --query 'DBInstances[0].Endpoint.Address' --output text)"

# ─── Bases y usuarios ──────────────────────────────────────────────────────────
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

# Certificados de RDS para verificar la conexión TLS.
CA="$HOME/rds-global-bundle.pem"
[[ -s "$CA" ]] || curl -fsS -o "$CA" https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem

# Se abre el puerto 3306 solo para la IP de este CloudShell, y se cierra al terminar.
MI_IP="$(curl -fsS https://checkip.amazonaws.com | tr -d '[:space:]')"
echo "Abriendo el acceso temporal para este CloudShell ($MI_IP)..."
aws ec2 authorize-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 \
  --cidr "$MI_IP/32" >/dev/null 2>&1 || true
cerrar_acceso() {
  aws ec2 revoke-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 \
    --cidr "$MI_IP/32" >/dev/null 2>&1 || true
}
trap cerrar_acceso EXIT
sleep 5

declare -A CONTRASENAS
for ambiente in "${AMBIENTES[@]}"; do
  CONTRASENAS[$ambiente]="$(generar_contrasena)"
done

echo "Creando las bases y los usuarios..."
DB_HOST="$HOST" DB_ADMIN_USUARIO="$ADMIN_USUARIO" DB_ADMIN_CONTRASENA="$ADMIN_CONTRASENA" DB_CA="$CA" \
CONTRASENA_DESARROLLO="${CONTRASENAS[desarrollo]}" CONTRASENA_PRODUCCION="${CONTRASENAS[produccion]}" \
  python3 "$AQUI/mysql_admin.py" crear \
  || falla "no se pudieron crear los usuarios. Revisa la contraseña del administrador y vuelve a ejecutar el script (no se pierde nada)."
unset ADMIN_CONTRASENA

# ─── Resultado ─────────────────────────────────────────────────────────────────
cat <<FIN

Listo. Estas son las variables de cada ambiente. Se muestran solo esta vez:
guárdalas ya en tu gestor de contraseñas y como secretos de GitHub
(Settings > Environments > desarrollo / produccion). Nunca en el repositorio.

Grupo de seguridad de la base (lo usa KAN-11 para dejar entrar al backend): $SG
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
Para revisar la instancia y los respaldos:          bash infra/aws/verificar-base-datos.sh
FIN
