#!/usr/bin/env bash
# KAN-11 · Empaqueta el jar con la configuración de Elastic Beanstalk y lo despliega.
#
# Lo usa .github/workflows/despliegue.yml después de `./gradlew bootJar`. Necesita
# credenciales de AWS y estas variables:
#   EB_APLICACION, EB_ENTORNO, EB_BUCKET   (los imprime infra/aws/crear-api.sh)
#   VERSION                                 etiqueta única, por ejemplo v12-a1b2c3d
set -euo pipefail
export AWS_PAGER=""

: "${EB_APLICACION:?falta EB_APLICACION}" "${EB_ENTORNO:?falta EB_ENTORNO}"
: "${EB_BUCKET:?falta EB_BUCKET}" "${VERSION:?falta VERSION}"

JAR="$(find backend/build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1)"
[[ -n "$JAR" ]] || { echo "No encontré el jar en backend/build/libs; corre ./gradlew bootJar." >&2; exit 1; }

PAQUETE="$(mktemp -d)"
cp -R backend/eb/. "$PAQUETE/"
cp "$JAR" "$PAQUETE/app.jar"
(cd "$PAQUETE" && zip -qr "$OLDPWD/api-$VERSION.zip" .)
echo "Paquete: api-$VERSION.zip ($(du -h "api-$VERSION.zip" | cut -f1))"

CLAVE="$EB_APLICACION/api-$VERSION.zip"
aws s3 cp --only-show-errors "api-$VERSION.zip" "s3://$EB_BUCKET/$CLAVE"
aws elasticbeanstalk create-application-version --application-name "$EB_APLICACION" \
  --version-label "$VERSION" --description "Commit ${GITHUB_SHA:-local}" \
  --source-bundle "S3Bucket=$EB_BUCKET,S3Key=$CLAVE" --process >/dev/null

# Espera a que el ambiente deje de estar "Updating". El waiter de la CLI se rinde a los
# 6,5 minutos (20 intentos cada 20 s), y un despliegue con varias migraciones en una
# t4g.micro puede tardar más: se espera hasta ESPERA_MINUTOS (30 por defecto).
ESPERA_MINUTOS="${ESPERA_MINUTOS:-30}"
esperar_ambiente() {
  local limite=$((SECONDS + ESPERA_MINUTOS * 60)) estado
  while true; do
    estado="$(aws elasticbeanstalk describe-environments --application-name "$EB_APLICACION" \
      --environment-names "$EB_ENTORNO" --query 'Environments[0].Status' --output text)"
    [[ "$estado" == "Ready" ]] && return 0
    if (( SECONDS >= limite )); then
      echo "El ambiente sigue en '$estado' después de $ESPERA_MINUTOS minutos." >&2
      return 1
    fi
    echo "  ambiente: $estado ($(( SECONDS / 60 )) min)"
    sleep 20
  done
}

# Con --process, Elastic Beanstalk revisa el paquete aparte; mientras dice PROCESSING
# no deja usar la versión ("The application version ... is processing").
esperar_version() {
  local limite=$((SECONDS + 10 * 60)) estado
  while true; do
    estado="$(aws elasticbeanstalk describe-application-versions --application-name "$EB_APLICACION" \
      --version-labels "$VERSION" --query 'ApplicationVersions[0].Status' --output text)"
    case "$estado" in
      PROCESSED) return 0 ;;
      FAILED) echo "Elastic Beanstalk rechazó el paquete $VERSION." >&2; return 1 ;;
    esac
    if (( SECONDS >= limite )); then
      echo "La versión $VERSION sigue en '$estado' después de 10 minutos." >&2
      return 1
    fi
    echo "  versión: $estado"
    sleep 10
  done
}

esperar_version
# Si otro despliegue sigue en curso, se espera a que termine.
esperar_ambiente
# Timeout: minutos que Elastic Beanstalk le da al despliegue antes de abortarlo (por
# defecto 10). Se deja en 30 por la misma razón.
aws elasticbeanstalk update-environment --application-name "$EB_APLICACION" \
  --environment-name "$EB_ENTORNO" --version-label "$VERSION" \
  --option-settings "Namespace=aws:elasticbeanstalk:command,OptionName=Timeout,Value=$((ESPERA_MINUTOS * 60))" >/dev/null
echo "Desplegando $VERSION en $EB_ENTORNO..."
esperar_ambiente

read -r ACTUAL SALUD < <(aws elasticbeanstalk describe-environments --application-name "$EB_APLICACION" \
  --environment-names "$EB_ENTORNO" --query 'Environments[0].[VersionLabel,Health]' --output text)
echo "Versión en el ambiente: $ACTUAL · salud: $SALUD"
if [[ "$ACTUAL" != "$VERSION" ]]; then
  echo "Elastic Beanstalk no dejó la versión nueva (volvió a la anterior). Revisa los eventos:" >&2
  aws elasticbeanstalk describe-events --application-name "$EB_APLICACION" --environment-name "$EB_ENTORNO" \
    --max-items 15 --query 'Events[].[EventDate,Severity,Message]' --output text >&2
  exit 1
fi
