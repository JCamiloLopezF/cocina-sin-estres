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

# Si otro despliegue sigue en curso, se espera a que termine.
aws elasticbeanstalk wait environment-updated --application-name "$EB_APLICACION" --environment-names "$EB_ENTORNO"
aws elasticbeanstalk update-environment --application-name "$EB_APLICACION" \
  --environment-name "$EB_ENTORNO" --version-label "$VERSION" >/dev/null
echo "Desplegando $VERSION en $EB_ENTORNO..."
aws elasticbeanstalk wait environment-updated --application-name "$EB_APLICACION" --environment-names "$EB_ENTORNO"

read -r ACTUAL SALUD < <(aws elasticbeanstalk describe-environments --application-name "$EB_APLICACION" \
  --environment-names "$EB_ENTORNO" --query 'Environments[0].[VersionLabel,Health]' --output text)
echo "Versión en el ambiente: $ACTUAL · salud: $SALUD"
if [[ "$ACTUAL" != "$VERSION" ]]; then
  echo "Elastic Beanstalk no dejó la versión nueva (volvió a la anterior). Revisa los eventos:" >&2
  aws elasticbeanstalk describe-events --application-name "$EB_APLICACION" --environment-name "$EB_ENTORNO" \
    --max-items 15 --query 'Events[].[EventDate,Severity,Message]' --output text >&2
  exit 1
fi
