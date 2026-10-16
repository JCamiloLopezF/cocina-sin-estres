#!/usr/bin/env bash
# KAN-9 · Revisa la instancia de RDS y deja la evidencia del RNF-20 (respaldo diario).
# No cambia la instancia.
#
# Se ejecuta en AWS CloudShell:
#   bash infra/aws/verificar-base-datos.sh
#
# Muestra la configuración, el firewall y los respaldos automáticos. Si respondes que
# sí al final, abre el puerto solo para este CloudShell por un momento, entra como
# administrador y muestra los permisos de cada usuario y las migraciones de Flyway.
set -euo pipefail

export AWS_DEFAULT_REGION="${REGION:-us-east-1}"
# Sin paginador: si no, la salida larga se abre en less y el script se detiene.
export AWS_PAGER=""
INSTANCIA="${INSTANCIA:-cocina-sin-estres-mysql}"
GRUPO_PARAMETROS="${INSTANCIA}-tls"
ADMIN_USUARIO="${ADMIN_USUARIO:-cocinaadmin}"
AQUI="$(cd "$(dirname "$0")" && pwd)"

titulo() { printf '\n== %s ==\n' "$1"; }

titulo "Instancia"
aws rds describe-db-instances --db-instance-identifier "$INSTANCIA" --output yaml --query 'DBInstances[0].{
  Estado: DBInstanceStatus,
  Motor: Engine,
  Version: EngineVersion,
  Clase: DBInstanceClass,
  AlmacenamientoGB: AllocatedStorage,
  TipoAlmacenamiento: StorageType,
  CrecimientoMaximoGB: MaxAllocatedStorage,
  Cifrada: StorageEncrypted,
  MultiAZ: MultiAZ,
  AccesoPublico: PubliclyAccessible,
  ProteccionBorrado: DeletionProtection,
  Host: Endpoint.Address,
  RetencionRespaldoDias: BackupRetentionPeriod,
  VentanaRespaldoUTC: PreferredBackupWindow,
  RestaurableHasta: LatestRestorableTime,
  GrupoParametros: DBParameterGroups[0].DBParameterGroupName,
  GrupoSeguridad: VpcSecurityGroups[0].VpcSecurityGroupId
}'

titulo "Conexión cifrada obligatoria (require_secure_transport debe ser 1)"
aws rds describe-db-parameters --db-parameter-group-name "$GRUPO_PARAMETROS" \
  --query "Parameters[?ParameterName=='require_secure_transport'].ParameterValue" --output text

SG="$(aws rds describe-db-instances --db-instance-identifier "$INSTANCIA" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text)"
titulo "Quién puede entrar al puerto 3306 (debería estar vacío, salvo el backend o una IP temporal)"
# shellcheck disable=SC2016  # los backticks son literales de JMESPath
aws ec2 describe-security-groups --group-ids "$SG" --output table --query \
  'SecurityGroups[0].IpPermissions[].{Puerto: FromPort, IPs: join(`, `, IpRanges[].CidrIp), Grupos: join(`, `, UserIdGroupPairs[].GroupId)}'

titulo "Respaldos automáticos (RNF-20: uno por día)"
aws rds describe-db-snapshots --db-instance-identifier "$INSTANCIA" --snapshot-type automated \
  --output table --query 'reverse(sort_by(DBSnapshots, &SnapshotCreateTime))[].{Respaldo: DBSnapshotIdentifier, Creado: SnapshotCreateTime, Estado: Status}'

echo
read -r -p "¿Revisar permisos y migraciones? Pide la contraseña del administrador (s/N) " respuesta
[[ "$respuesta" =~ ^[sS]$ ]] || exit 0
read -r -s -p "Contraseña del administrador: " ADMIN_CONTRASENA; echo

# En una carpeta propia: el Python de CloudShell es un entorno virtual y no acepta --user.
LIBS_PYTHON="$HOME/.cocina-python"
export PYTHONPATH="$LIBS_PYTHON${PYTHONPATH:+:$PYTHONPATH}"
python3 -c "import pymysql" 2>/dev/null || python3 -m pip install --quiet --target "$LIBS_PYTHON" pymysql
CA="$HOME/rds-global-bundle.pem"
[[ -s "$CA" ]] || curl -fsS -o "$CA" https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
HOST="$(aws rds describe-db-instances --db-instance-identifier "$INSTANCIA" \
  --query 'DBInstances[0].Endpoint.Address' --output text)"

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
  python3 "$AQUI/mysql_admin.py" revisar
