#!/usr/bin/env bash
# Auditoría del 4 de noviembre · Permisos mínimos en la base de datos.
#
# Crea migra_desarrollo y migra_produccion (solo para Flyway) y deja a app_desarrollo y
# app_produccion con SELECT, INSERT, UPDATE y DELETE. Se ejecuta en AWS CloudShell:
#   bash infra/aws/limitar-permisos-base-datos.sh
#
# ANTES de ejecutarlo, pon en Elastic Beanstalk (Configuración → Actualizaciones,
# monitoreo y registros → Propiedades del entorno) DB_MIGRACION_USUARIO=migra_produccion y
# DB_MIGRACION_CONTRASENA con la contraseña que vas a escribir aquí. Si no, el próximo
# despliegue con una migración nueva falla, porque Flyway seguiría usando app_produccion.
# Pasos completos en docs/despliegue/base-de-datos.md, «Permisos mínimos».
set -euo pipefail

export AWS_DEFAULT_REGION="${REGION:-us-east-1}"
export AWS_PAGER=""
INSTANCIA="${INSTANCIA:-cocina-sin-estres-mysql}"
ADMIN_USUARIO="${ADMIN_USUARIO:-cocinaadmin}"
AQUI="$(cd "$(dirname "$0")" && pwd)"

read -r -p "¿Ya pusiste DB_MIGRACION_USUARIO y DB_MIGRACION_CONTRASENA en Elastic Beanstalk? (s/N) " listo
[[ "$listo" =~ ^[sS]$ ]] || { echo "Primero las variables de entorno: ver el encabezado de este script."; exit 1; }

read -r -s -p "Contraseña del administrador: " ADMIN_CONTRASENA; echo
read -r -s -p "Contraseña nueva para migra_desarrollo: " CONTRASENA_MIGRACION_DESARROLLO; echo
read -r -s -p "Contraseña nueva para migra_produccion (la misma de DB_MIGRACION_CONTRASENA): " CONTRASENA_MIGRACION_PRODUCCION; echo
export CONTRASENA_MIGRACION_DESARROLLO CONTRASENA_MIGRACION_PRODUCCION

LIBS_PYTHON="$HOME/.cocina-python"
export PYTHONPATH="$LIBS_PYTHON${PYTHONPATH:+:$PYTHONPATH}"
python3 -c "import pymysql" 2>/dev/null || python3 -m pip install --quiet --target "$LIBS_PYTHON" pymysql
CA="$HOME/rds-global-bundle.pem"
[[ -s "$CA" ]] || curl -fsS -o "$CA" https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
HOST="$(aws rds describe-db-instances --db-instance-identifier "$INSTANCIA" \
  --query 'DBInstances[0].Endpoint.Address' --output text)"
SG="$(aws rds describe-db-instances --db-instance-identifier "$INSTANCIA" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text)"

# El puerto se abre solo para este CloudShell y se cierra al terminar, pase lo que pase.
MI_IP="$(curl -fsS https://checkip.amazonaws.com | tr -d '[:space:]')"
aws ec2 authorize-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 \
  --cidr "$MI_IP/32" >/dev/null 2>&1 || true
cerrar_acceso() {
  aws ec2 revoke-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 \
    --cidr "$MI_IP/32" >/dev/null 2>&1 || true
}
trap cerrar_acceso EXIT
sleep 5

DB_HOST="$HOST" DB_ADMIN_USUARIO="$ADMIN_USUARIO" DB_ADMIN_CONTRASENA="$ADMIN_CONTRASENA" DB_CA="$CA" \
  python3 "$AQUI/mysql_admin.py" limitar
echo
echo "Listo. Comprueba con: bash infra/aws/verificar-base-datos.sh (responde s al final)."
