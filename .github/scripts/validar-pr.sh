#!/usr/bin/env bash
# Valida que un PR cumpla las convenciones de CONTRIBUTING.md.
# Variables de entrada: TITULO, RAMA_ORIGEN, RAMA_DESTINO.
# Uso local: TITULO="feat(despensa): KAN-15 algo" RAMA_ORIGEN=feature/KAN-15-algo RAMA_DESTINO=develop bash .github/scripts/validar-pr.sh

set -uo pipefail

TIPOS='feat|fix|test|docs|refactor|chore'
MODULOS='auth|perfil|despensa|planificador|recetario|avisos|admin|curaduria|web|api|db|ci|docs'
REGEX_TITULO="^(${TIPOS})\((${MODULOS})\): KAN-[0-9]+ .+$"
REGEX_RELEASE='^release: .+$'
REGEX_RAMA='^(feature|fix|docs|chore)/KAN-[0-9]+-[a-z0-9]+(-[a-z0-9]+)*$'

errores=0

error() {
  echo "::error::$1"
  errores=$((errores + 1))
}

echo "Título:  ${TITULO}"
echo "Origen:  ${RAMA_ORIGEN}"
echo "Destino: ${RAMA_DESTINO}"

# 1. Título del PR
if [[ "${RAMA_DESTINO}" == "main" && "${RAMA_ORIGEN}" == "develop" ]]; then
  if [[ ! "${TITULO}" =~ ${REGEX_RELEASE} ]]; then
    error "Un PR de develop hacia main debe titularse 'release: descripción'. Ejemplo: 'release: prototipo 1'."
  fi
elif [[ ! "${TITULO}" =~ ${REGEX_TITULO} ]]; then
  error "El título debe tener el formato 'tipo(módulo): KAN-xx descripción'. Ejemplo: 'feat(despensa): KAN-15 agregar filtro por vencer'. Tipos: ${TIPOS//|/, }. Módulos: ${MODULOS//|/, }."
fi

# 2. Rama de origen
if [[ "${RAMA_ORIGEN}" != "develop" && ! "${RAMA_ORIGEN}" =~ ${REGEX_RAMA} ]]; then
  error "La rama debe llamarse 'tipo/KAN-xx-descripcion' en minúsculas y sin tildes. Ejemplo: 'feature/KAN-15-filtro-por-vencer'. Tipos: feature, fix, docs, chore."
fi

# 3. Qué puede entrar a main
if [[ "${RAMA_DESTINO}" == "main" && "${RAMA_ORIGEN}" != "develop" && "${RAMA_ORIGEN}" != fix/* ]]; then
  error "A main solo entran PRs desde develop o desde una rama fix/. Abre este PR hacia develop."
fi

# 4. La clave KAN del título y de la rama deben coincidir
clave_titulo=$(grep -oE 'KAN-[0-9]+' <<< "${TITULO}" | head -n1 || true)
clave_rama=$(grep -oE 'KAN-[0-9]+' <<< "${RAMA_ORIGEN}" | head -n1 || true)
if [[ -n "${clave_titulo}" && -n "${clave_rama}" && "${clave_titulo}" != "${clave_rama}" ]]; then
  error "La clave del título (${clave_titulo}) no coincide con la de la rama (${clave_rama})."
fi

if (( errores > 0 )); then
  echo "Se encontraron ${errores} problema(s). Revisa CONTRIBUTING.md, secciones 2 a 4."
  exit 1
fi

echo "El PR cumple las convenciones."
