#!/usr/bin/env bash
# Crea el repositorio público de Cocina sin estrés en GitHub y aplica las reglas de CONTRIBUTING.md.
#
# Requisitos: git y GitHub CLI (gh) con sesión iniciada (gh auth login).
# En Windows, ejecútalo desde Git Bash.
#
# Uso (desde la raíz de esta carpeta):
#   bash scripts/configurar-repositorio.sh <usuario-github-luisa> <usuario-github-dillan>
#
# Qué hace:
#   1. Crea el primer commit en main y el repositorio público en tu cuenta.
#   2. Crea la rama develop y la deja como rama por defecto.
#   3. Configura los merges: squash y merge commit, sin rebase; borra ramas al hacer merge.
#   4. Activa el escaneo de secretos con bloqueo de push y las alertas de Dependabot.
#   5. Protege main y develop con un ruleset (scripts/ruleset-main-develop.json).
#   6. Invita a Luisa y Dillan como colaboradores con permiso de escritura.

set -euo pipefail

NOMBRE_REPO="cocina-sin-estres"
DESCRIPCION="Despensa, planificador semanal y avisos para mujeres cabeza de hogar · Ingeniería de Software III, Uniquindío"
MENSAJE_COMMIT="chore(ci): KAN-49 estructura inicial del repositorio"

paso()  { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
aviso() { printf '\033[1;33m[aviso]\033[0m %s\n' "$1"; }
fallo() { printf '\033[1;31m[error]\033[0m %s\n' "$1" >&2; exit 1; }

# ─── Verificaciones previas ──────────────────────────────────────
if [[ $# -ne 2 ]]; then
  fallo "Uso: bash scripts/configurar-repositorio.sh <usuario-github-luisa> <usuario-github-dillan>"
fi
COLABORADORES=("$1" "$2")

command -v git >/dev/null || fallo "No se encontró git."
command -v gh  >/dev/null || fallo "No se encontró GitHub CLI. Instálalo desde https://cli.github.com"
gh auth status >/dev/null 2>&1 || fallo "No hay sesión en GitHub CLI. Ejecuta: gh auth login"

[[ -f CONTRIBUTING.md && -d .github ]] || fallo "Ejecuta el script desde la raíz de la carpeta cocina-sin-estres."
[[ -d .git ]] && fallo "Esta carpeta ya es un repositorio git. El script es solo para la creación inicial."

DUENO=$(gh api user --jq .login)
REPO="${DUENO}/${NOMBRE_REPO}"

if gh repo view "${REPO}" >/dev/null 2>&1; then
  fallo "El repositorio ${REPO} ya existe en GitHub."
fi

for usuario in "${COLABORADORES[@]}"; do
  gh api "users/${usuario}" >/dev/null 2>&1 || fallo "No existe el usuario de GitHub '${usuario}'."
done

echo "Se creará el repositorio PÚBLICO ${REPO}"
echo "Colaboradores: ${COLABORADORES[*]}"
read -r -p "¿Continuar? (s/n) " respuesta
[[ "${respuesta}" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

# ─── 1. Primer commit y repositorio ──────────────────────────────
paso "Creando el primer commit en main"
chmod +x .github/scripts/validar-pr.sh scripts/configurar-repositorio.sh
git init -b main
git add .
git commit -m "${MENSAJE_COMMIT}"

paso "Creando ${REPO} en GitHub"
gh repo create "${REPO}" --public --description "${DESCRIPCION}" --source . --remote origin --push

# ─── 2. Rama develop ─────────────────────────────────────────────
paso "Creando develop y dejándola como rama por defecto"
git switch -c develop
git push -u origin develop

# ─── 3. Opciones de merge ────────────────────────────────────────
paso "Configurando opciones de merge"
gh repo edit "${REPO}" \
  --default-branch develop \
  --enable-squash-merge \
  --enable-merge-commit \
  --enable-rebase-merge=false \
  --delete-branch-on-merge \
  --allow-update-branch \
  --enable-wiki=false

# ─── 4. Seguridad ────────────────────────────────────────────────
paso "Activando escaneo de secretos y alertas de Dependabot"
gh api -X PATCH "repos/${REPO}" --input - >/dev/null <<'JSON' || aviso "No se pudo activar el escaneo de secretos; actívalo en Settings → Code security."
{
  "security_and_analysis": {
    "secret_scanning": { "status": "enabled" },
    "secret_scanning_push_protection": { "status": "enabled" }
  }
}
JSON
gh api -X PUT "repos/${REPO}/vulnerability-alerts" >/dev/null \
  || aviso "No se pudieron activar las alertas de Dependabot; actívalas en Settings → Code security."

# ─── 5. Protección de main y develop ─────────────────────────────
paso "Protegiendo main y develop"
gh api -X POST "repos/${REPO}/rulesets" --input scripts/ruleset-main-develop.json >/dev/null

# ─── 6. Colaboradores ────────────────────────────────────────────
paso "Invitando colaboradores"
for usuario in "${COLABORADORES[@]}"; do
  gh api -X PUT "repos/${REPO}/collaborators/${usuario}" -f permission=push >/dev/null
  echo "Invitación enviada a ${usuario} (debe aceptarla desde su correo o en github.com/notifications)."
done

# ─── Resumen ─────────────────────────────────────────────────────
paso "Listo"
cat <<TXT
Repositorio: https://github.com/${REPO}

Verifica en Settings → Rules → Rulesets que aparezca "Proteger main y develop".

Pasos manuales que quedan:
  1. Luisa y Dillan aceptan la invitación.
  2. Dillan (administrador de Jira) instala la app "GitHub for Jira" desde
     Jira → Aplicaciones → Explorar aplicaciones, y conecta ${REPO}.
  3. Primer PR de prueba: una rama docs/KAN-49-prueba-reglas hacia develop,
     para confirmar que el check "convenciones-pr" corre y que pide aprobación.
TXT
