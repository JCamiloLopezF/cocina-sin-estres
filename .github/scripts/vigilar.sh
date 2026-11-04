#!/usr/bin/env bash
# Auditoría del 4 de noviembre · «No hay alarmas automáticas que avisen si la app se cae»
# y «no se registran incidentes».
#
# Lo corre .github/workflows/vigilancia.yml cada 30 minutos. Pregunta a /api/salud, que
# además revisa la base de datos (SaludController). Si no responde en 3 intentos:
#   - abre un incidente en GitHub Issues (etiquetas «incidente» y «caida-automatica»), o
#     comenta en el que ya está abierto; GitHub le avisa por correo a quien vigila el repo;
#   - termina en error, y GitHub también avisa del workflow fallido.
# Cuando vuelve a responder, comenta la hora y cierra el incidente automático.
#
# Variables: URL_SALUD (variable del repositorio), GH_TOKEN y GH_REPO (las pone el workflow).
set -uo pipefail

if [[ -z "${URL_SALUD:-}" ]]; then
  echo "::notice::Falta la variable del repositorio URL_SALUD (https://…cloudfront.net/api/salud). No se vigila nada."
  exit 0
fi

AHORA="$(TZ=America/Bogota date '+%Y-%m-%d %H:%M') (hora de Colombia)"
CORRIDA="${GITHUB_SERVER_URL:-https://github.com}/${GH_REPO}/actions/runs/${GITHUB_RUN_ID:-0}"

respuesta=""
for intento in 1 2 3; do
  if respuesta="$(curl -sS -m 20 -w ' [HTTP %{http_code}, %{time_total} s]' "$URL_SALUD" 2>&1)" \
     && [[ "$respuesta" == *'"estado":"ok"'* ]]; then
    echo "Responde: $respuesta"
    abierto="$(gh issue list --label caida-automatica --state open --json number --jq '.[0].number // empty')"
    if [[ -n "$abierto" ]]; then
      gh issue comment "$abierto" --body "Volvió a responder el $AHORA. Respuesta: \`$respuesta\`. Corrida: $CORRIDA"
      gh issue close "$abierto" --reason completed \
        --comment "Cerrado por la vigilancia. Antes de dar el incidente por terminado, completa la causa y la solución en docs/operacion/incidentes.md."
    fi
    exit 0
  fi
  echo "Intento $intento: no responde bien. $respuesta"
  [[ $intento -lt 3 ]] && sleep 60
done

gh label create incidente --color B60205 --description "Falla en producción" 2>/dev/null || true
gh label create caida-automatica --color D93F0B --description "Abierto por la vigilancia" 2>/dev/null || true

abierto="$(gh issue list --label caida-automatica --state open --json number --jq '.[0].number // empty')"
if [[ -n "$abierto" ]]; then
  gh issue comment "$abierto" --body "Sigue sin responder el $AHORA. Última respuesta: \`$respuesta\`. Corrida: $CORRIDA"
else
  gh issue create --label incidente --label caida-automatica \
    --title "Incidente: la API no responde ($AHORA)" \
    --body "$(cat <<CUERPO
La vigilancia no obtuvo respuesta de \`/api/salud\` en 3 intentos seguidos.

- **Desde:** $AHORA
- **Última respuesta:** \`$respuesta\`
- **Corrida:** $CORRIDA

## Qué revisar
1. Elastic Beanstalk → \`cocina-sin-estres-api\` → Estado y Registros.
2. Si \`estado\` es \`sin-base-de-datos\`: RDS → \`cocina-sin-estres-mysql\` → Estado y Eventos.
3. Si un despliegue está en curso, esperar a que termine (1 a 2 minutos sin servicio).

Cuando se resuelva, completar causa, solución y duración en \`docs/operacion/incidentes.md\`.
CUERPO
)"
fi
exit 1
