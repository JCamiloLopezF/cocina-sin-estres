#!/usr/bin/env bash
# KAN-9 · Revisa el servidor MySQL de Azure y deja la evidencia del RNF-20
# (respaldo diario de la base de datos). No cambia nada.
#
# Se ejecuta en Azure Cloud Shell (Bash):
#   bash infra/azure/verificar-base-datos.sh
#
# Muestra la configuración del servidor, las reglas de firewall y los respaldos
# disponibles. Si escribes la contraseña del administrador, revisa además las
# bases, los usuarios y las migraciones aplicadas por Flyway en cada ambiente.
set -euo pipefail

GRUPO="${GRUPO:-rg-cocina-sin-estres}"
SERVIDOR="${SERVIDOR:-cocina-sin-estres-mysql}"
ADMIN_USUARIO="${ADMIN_USUARIO:-cocinaadmin}"
AMBIENTES=(desarrollo produccion)
HOST="${SERVIDOR}.mysql.database.azure.com"

titulo() { printf '\n== %s ==\n' "$1"; }

titulo "Servidor"
az mysql flexible-server show -g "$GRUPO" -n "$SERVIDOR" \
  --query "{Estado:state, Host:fullyQualifiedDomainName, Version:version, Nivel:sku.tier, Tamano:sku.name, AlmacenamientoGB:storage.storageSizeGb, CrecimientoDisco:storage.autoGrow, RetencionRespaldoDias:backup.backupRetentionDays, RespaldoGeografico:backup.geoRedundantBackup, AltaDisponibilidad:highAvailability.mode, RestaurableDesde:backup.earliestRestoreDate}" \
  -o yaml

titulo "Conexión cifrada obligatoria (debe decir ON)"
az mysql flexible-server parameter show -g "$GRUPO" -s "$SERVIDOR" \
  -n require_secure_transport --query value -o tsv

titulo "Reglas de firewall"
az mysql flexible-server firewall-rule list -g "$GRUPO" -n "$SERVIDOR" \
  --query "[].{Regla:name, Desde:startIpAddress, Hasta:endIpAddress}" -o table

titulo "Respaldos disponibles (RNF-20: uno completo por día)"
az mysql flexible-server backup list -g "$GRUPO" -n "$SERVIDOR" \
  --query "sort_by(@, &completedTime)[].{Respaldo:name, Tipo:backupType, Terminado:completedTime}" -o table \
  || echo "No pude listarlos con la CLI. Míralos en el portal: servidor > Backup and restore."

echo
read -r -s -p "Contraseña del administrador para revisar bases y migraciones (Enter para omitir): " ADMIN_CONTRASENA
echo
if [[ -z "$ADMIN_CONTRASENA" ]]; then
  exit 0
fi

export MYSQL_PWD="$ADMIN_CONTRASENA"
unset ADMIN_CONTRASENA
mysql_admin() {
  mysql --host="$HOST" --user="$ADMIN_USUARIO" --protocol=TCP --ssl-mode=REQUIRED --table "$@"
}

titulo "Usuarios de la aplicación"
mysql_admin -e "SELECT user AS usuario, host, IF(ssl_type = '', 'No', 'Sí') AS exige_tls FROM mysql.user WHERE user LIKE 'app\\_%' ORDER BY user;"

for ambiente in "${AMBIENTES[@]}"; do
  base="cocina_${ambiente}"
  titulo "Permisos de app_${ambiente}"
  mysql_admin -e "SHOW GRANTS FOR 'app_${ambiente}'@'%';"
  titulo "Migraciones aplicadas en ${base}"
  if mysql_admin -N -e "SELECT 1 FROM information_schema.tables WHERE table_schema='${base}' AND table_name='flyway_schema_history';" | grep -q 1; then
    mysql_admin -e "SELECT version AS version, description AS descripcion, installed_on AS aplicada, success AS exito FROM \`${base}\`.flyway_schema_history ORDER BY installed_rank;"
  else
    echo "Todavía sin migraciones: Flyway las aplica la primera vez que la API arranca contra esta base."
  fi
done

unset MYSQL_PWD
