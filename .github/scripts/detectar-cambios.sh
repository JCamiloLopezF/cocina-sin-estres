#!/usr/bin/env bash
# Decide si un job de CI debe correr, según los archivos que cambió el PR o el push.
#
# Uso: bash .github/scripts/detectar-cambios.sh <regex de rutas> [archivo requerido]
#   <regex de rutas>      Expresión regular extendida sobre las rutas cambiadas. Ej.: '^(backend/|docker-compose\.yml$)'
#   [archivo requerido]   Si se indica y no existe, el job no aplica (ej.: web-app/package.json).
#
# Variables de entrada: EVENTO, BASE_SHA (las pasa el workflow).
# Salida: escribe "aplica=true|false" en $GITHUB_OUTPUT y una línea en el resumen del job.
#
# Ante cualquier duda (evento manual, primer push de una rama, base desconocida)
# responde "true": es preferible correr pruebas de más que dejar pasar un cambio sin probar.
set -uo pipefail

PATRON="${1:?Falta el patrón de rutas}"
REQUERIDO="${2:-}"
SALIDA="${GITHUB_OUTPUT:-/dev/stdout}"
RESUMEN="${GITHUB_STEP_SUMMARY:-/dev/null}"

responder() {
  echo "aplica=$1" >> "$SALIDA"
  echo "$2" >> "$RESUMEN"
  echo "$2"
}

if [[ -n "$REQUERIDO" && ! -e "$REQUERIDO" ]]; then
  responder false "Se omite: todavía no existe \`$REQUERIDO\`."
  exit 0
fi

if [[ "${EVENTO:-}" != "pull_request" && "${EVENTO:-}" != "push" ]]; then
  responder true "Se ejecuta completo: evento \`${EVENTO:-desconocido}\`."
  exit 0
fi

if [[ -z "${BASE_SHA:-}" || "${BASE_SHA}" =~ ^0+$ ]] || ! git cat-file -e "${BASE_SHA}^{commit}" 2>/dev/null; then
  responder true "Se ejecuta completo: no hay una base con la cual comparar."
  exit 0
fi

if ! cambios=$(git diff --name-only "$BASE_SHA" HEAD); then
  responder true "Se ejecuta completo: no se pudo calcular la diferencia."
  exit 0
fi

coincidencias=$(grep -E -c "$PATRON" <<< "$cambios" || true)
if (( coincidencias > 0 )); then
  responder true "Se ejecuta: $coincidencias archivo(s) relevante(s) cambiaron."
else
  responder false "Se omite: este cambio no toca archivos de este módulo."
fi
