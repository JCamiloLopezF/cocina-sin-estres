#!/usr/bin/env bash
# Auditoría del 4 de noviembre · Respaldo con más historia que el automático.
#
# El plan gratuito solo deja 1 día de retención en los respaldos automáticos (RNF-20).
# Este script toma una instantánea manual «semanal-AAAA-MM-DD» y borra las semanales de
# más de 28 días, así siempre hay unas 4 semanas para volver atrás. Las instantáneas
# manuales no vencen solas y entran en los 20 GB de respaldo gratuitos.
#
# Se ejecuta en AWS CloudShell (bash infra/aws/instantanea-semanal.sh) o cada domingo desde
# .github/workflows/respaldo-semanal.yml.
set -euo pipefail

export AWS_DEFAULT_REGION="${REGION:-${AWS_REGION:-us-east-1}}"
export AWS_PAGER=""
INSTANCIA="${INSTANCIA:-cocina-sin-estres-mysql}"
DIAS_A_CONSERVAR="${DIAS_A_CONSERVAR:-28}"
HOY="$(date -u +%Y-%m-%d)"
NOMBRE="semanal-$HOY"

if aws rds describe-db-snapshots --db-snapshot-identifier "$NOMBRE" >/dev/null 2>&1; then
  echo "Ya existe $NOMBRE: no se toma otra hoy."
else
  echo "Tomando $NOMBRE…"
  aws rds create-db-snapshot --db-instance-identifier "$INSTANCIA" --db-snapshot-identifier "$NOMBRE" \
    --tags Key=proyecto,Value=cocina-sin-estres Key=tipo,Value=semanal >/dev/null
  aws rds wait db-snapshot-available --db-snapshot-identifier "$NOMBRE"
  echo "Lista: $NOMBRE"
fi

LIMITE="$(date -u -d "-$DIAS_A_CONSERVAR days" +%Y-%m-%d)"
for vieja in $(aws rds describe-db-snapshots --db-instance-identifier "$INSTANCIA" --snapshot-type manual \
  --query "DBSnapshots[?starts_with(DBSnapshotIdentifier, 'semanal-')].DBSnapshotIdentifier" --output text); do
  fecha="${vieja#semanal-}"
  if [[ "$fecha" < "$LIMITE" ]]; then
    echo "Borrando $vieja (de antes del $LIMITE)"
    aws rds delete-db-snapshot --db-snapshot-identifier "$vieja" >/dev/null
  fi
done

echo
aws rds describe-db-snapshots --db-instance-identifier "$INSTANCIA" --snapshot-type manual --output table \
  --query "reverse(sort_by(DBSnapshots[?starts_with(DBSnapshotIdentifier, 'semanal-')], &SnapshotCreateTime))[].{Instantanea: DBSnapshotIdentifier, Creada: SnapshotCreateTime, Estado: Status}"
