#!/usr/bin/env bash
# Auditoría del 4 de noviembre · «La restauración nunca se probó».
#
# Restaura el respaldo de producción en una instancia nueva y temporal, comprueba que la
# base tiene sus tablas y datos, y la borra al final. La instancia original no se toca.
# Se ejecuta en AWS CloudShell:
#   bash infra/aws/probar-restauracion.sh                       # al último minuto restaurable
#   INSTANTANEA=semanal-2026-11-08 bash infra/aws/probar-restauracion.sh   # desde una instantánea
#
# Tarda unos 15 a 25 minutos y cuesta unos centavos de dólar (la instancia vive menos de
# una hora). Guarda la salida completa como evidencia del RNF-20.
set -euo pipefail

export AWS_DEFAULT_REGION="${REGION:-us-east-1}"
export AWS_PAGER=""
ORIGEN="${INSTANCIA:-cocina-sin-estres-mysql}"
PRUEBA="${PRUEBA:-cocina-sin-estres-prueba-restauracion}"
ADMIN_USUARIO="${ADMIN_USUARIO:-cocinaadmin}"
AQUI="$(cd "$(dirname "$0")" && pwd)"

read -r -s -p "Contraseña del administrador (la misma de la instancia original): " ADMIN_CONTRASENA; echo
INICIO="$(date -u +%FT%TZ)"
echo "Inicio: $INICIO"

SG="$(aws rds describe-db-instances --db-instance-identifier "$ORIGEN" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text)"
GRUPO_PARAMETROS="$(aws rds describe-db-instances --db-instance-identifier "$ORIGEN" \
  --query 'DBInstances[0].DBParameterGroups[0].DBParameterGroupName' --output text)"

if [[ -n "${INSTANTANEA:-}" ]]; then
  echo "Restaurando la instantánea $INSTANTANEA en $PRUEBA…"
  aws rds restore-db-instance-from-db-snapshot --db-instance-identifier "$PRUEBA" \
    --db-snapshot-identifier "$INSTANTANEA" --db-instance-class db.t4g.micro \
    --vpc-security-group-ids "$SG" --db-parameter-group-name "$GRUPO_PARAMETROS" \
    --no-multi-az --publicly-accessible >/dev/null
else
  echo "Restaurando $ORIGEN al último minuto restaurable en $PRUEBA…"
  aws rds restore-db-instance-to-point-in-time --source-db-instance-identifier "$ORIGEN" \
    --target-db-instance-identifier "$PRUEBA" --use-latest-restorable-time \
    --db-instance-class db.t4g.micro --vpc-security-group-ids "$SG" \
    --db-parameter-group-name "$GRUPO_PARAMETROS" --no-multi-az --publicly-accessible >/dev/null
fi

borrar_prueba() {
  echo "Borrando la instancia de prueba $PRUEBA (sin instantánea final)…"
  aws rds delete-db-instance --db-instance-identifier "$PRUEBA" --skip-final-snapshot \
    --delete-automated-backups >/dev/null 2>&1 || true
}
trap borrar_prueba EXIT

echo "Esperando a que la instancia esté disponible (15 a 25 minutos)…"
aws rds wait db-instance-available --db-instance-identifier "$PRUEBA"
LISTA="$(date -u +%FT%TZ)"
echo "Disponible: $LISTA"

LIBS_PYTHON="$HOME/.cocina-python"
export PYTHONPATH="$LIBS_PYTHON${PYTHONPATH:+:$PYTHONPATH}"
python3 -c "import pymysql" 2>/dev/null || python3 -m pip install --quiet --target "$LIBS_PYTHON" pymysql
CA="$HOME/rds-global-bundle.pem"
[[ -s "$CA" ]] || curl -fsS -o "$CA" https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
HOST="$(aws rds describe-db-instances --db-instance-identifier "$PRUEBA" \
  --query 'DBInstances[0].Endpoint.Address' --output text)"

MI_IP="$(curl -fsS https://checkip.amazonaws.com | tr -d '[:space:]')"
aws ec2 authorize-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 \
  --cidr "$MI_IP/32" >/dev/null 2>&1 || true
cerrar_todo() {
  aws ec2 revoke-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 \
    --cidr "$MI_IP/32" >/dev/null 2>&1 || true
  borrar_prueba
}
trap cerrar_todo EXIT
sleep 5

DB_HOST="$HOST" DB_ADMIN_USUARIO="$ADMIN_USUARIO" DB_ADMIN_CONTRASENA="$ADMIN_CONTRASENA" DB_CA="$CA" \
  python3 "$AQUI/mysql_admin.py" contar
echo
echo "Restauración comprobada. Inicio $INICIO, instancia lista $LISTA, fin $(date -u +%FT%TZ)."
