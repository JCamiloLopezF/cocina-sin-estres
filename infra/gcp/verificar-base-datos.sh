#!/usr/bin/env bash
# KAN-9 · Revisa la instancia de Cloud SQL y deja la evidencia del RNF-20
# (respaldo diario de la base de datos). No cambia nada en la instancia.
#
# Se ejecuta en Cloud Shell:
#   bash infra/gcp/verificar-base-datos.sh [id-del-proyecto]
#
# Muestra la configuración, las redes autorizadas y los respaldos. Si respondes
# que sí al final, entra con root (pide la contraseña) y muestra los permisos de
# cada usuario y las migraciones que Flyway aplicó en cada base.
set -euo pipefail

PROYECTO="${1:-$(gcloud config get-value project 2>/dev/null || true)}"
INSTANCIA="${INSTANCIA:-cocina-sin-estres-mysql}"
AMBIENTES=(desarrollo produccion)

[[ -n "$PROYECTO" ]] || { echo "Uso: bash infra/gcp/verificar-base-datos.sh <id-del-proyecto>" >&2; exit 1; }

titulo() { printf '\n== %s ==\n' "$1"; }

titulo "Instancia"
gcloud sql instances describe "$INSTANCIA" --project="$PROYECTO" --format="yaml(
  state,
  databaseVersion,
  connectionName,
  region,
  settings.edition,
  settings.tier,
  settings.availabilityType,
  settings.dataDiskType,
  settings.dataDiskSizeGb,
  settings.storageAutoResize,
  settings.deletionProtectionEnabled,
  settings.ipConfiguration.sslMode,
  settings.ipConfiguration.authorizedNetworks,
  settings.backupConfiguration.enabled,
  settings.backupConfiguration.startTime,
  settings.backupConfiguration.binaryLogEnabled,
  settings.backupConfiguration.backupRetentionSettings.retainedBackups,
  settings.backupConfiguration.transactionLogRetentionDays
)"

titulo "Bases"
gcloud sql databases list --instance="$INSTANCIA" --project="$PROYECTO" \
  --format="table(name, charset, collation)"

titulo "Usuarios"
gcloud sql users list --instance="$INSTANCIA" --project="$PROYECTO" --format="table(name, host)"

titulo "Respaldos (RNF-20: uno automático por día, se conservan 7)"
gcloud sql backups list --instance="$INSTANCIA" --project="$PROYECTO" \
  --format="table(id, type, status, windowStartTime, endTime)"

echo
read -r -p "¿Revisar permisos y migraciones? Entra como root y pide su contraseña (s/N) " respuesta
[[ "$respuesta" =~ ^[sS]$ ]] || exit 0

consulta=""
for ambiente in "${AMBIENTES[@]}"; do
  consulta+="SHOW GRANTS FOR 'app_${ambiente}'@'%';"
  consulta+="SELECT 'cocina_${ambiente}' AS base, IF(COUNT(*) = 1, 'sí', 'todavía no') AS tiene_flyway FROM information_schema.tables WHERE table_schema='cocina_${ambiente}' AND table_name='flyway_schema_history';"
done
for ambiente in "${AMBIENTES[@]}"; do
  # Si la tabla de Flyway no existe todavía, esta consulta falla y se avisa abajo.
  consulta+="SELECT 'cocina_${ambiente}' AS base, version, description AS descripcion, installed_on AS aplicada, success AS exito FROM \`cocina_${ambiente}\`.flyway_schema_history ORDER BY installed_rank;"
done

gcloud sql connect "$INSTANCIA" --user=root --project="$PROYECTO" --quiet <<<"$consulta" \
  || echo "Si el error dice que no existe flyway_schema_history, esa base todavía no tiene migraciones: Flyway las aplica la primera vez que la API arranca contra ella."
