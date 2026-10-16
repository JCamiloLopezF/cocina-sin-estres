#!/usr/bin/env bash
# KAN-11 · Infraestructura del backend de producción en AWS (ADR-22).
#
# Crea, si no existen, en us-east-1:
#   - los secretos de producción en AWS Systems Manager (Parameter Store), cifrados:
#     URL, usuario y contraseña de cocina_produccion, la clave de cifrado de datos de
#     salud (ADR-20) y la cuenta de servicio de Firebase;
#   - un grupo de seguridad para la API y el permiso para que entre a MySQL (3306);
#   - los roles de IAM que necesita Elastic Beanstalk;
#   - la aplicación y el ambiente de Elastic Beanstalk (Corretto 21, una sola
#     instancia t4g.micro). Arranca con la aplicación de ejemplo de AWS: la API real
#     la sube el workflow de despliegue;
#   - una distribución de CloudFront delante del ambiente, para tener HTTPS;
#   - el rol que GitHub Actions asume (OIDC, sin llaves guardadas) para desplegar.
#
# Se ejecuta en AWS CloudShell, desde la raíz del repositorio:
#
#   bash infra/aws/crear-api.sh ~/cuenta-servicio-firebase.json
#
# El .json es la cuenta de servicio de Firebase (consola de Firebase → Configuración
# del proyecto → Cuentas de servicio → Generar nueva clave privada). Súbelo a
# CloudShell con Acciones → Cargar archivo. El script lo guarda cifrado en SSM y lo
# BORRA de CloudShell. Solo hace falta la primera vez o para cambiarlo.
#
# Guía completa: docs/despliegue/aplicacion.md
set -euo pipefail

export AWS_DEFAULT_REGION="${REGION:-us-east-1}"
export AWS_PAGER=""
PROYECTO=cocina-sin-estres
REPO_GITHUB="${REPO_GITHUB:-JCamiloLopezF/cocina-sin-estres}"
AMBIENTE_GITHUB=produccion
APLICACION="$PROYECTO"
ENTORNO="${ENTORNO:-$PROYECTO-api}"
CLASE="${CLASE:-t4g.micro}"
INSTANCIA_RDS="${INSTANCIA_RDS:-$PROYECTO-mysql}"
RUTA_PARAMETROS="/$PROYECTO/produccion"
GRUPO_API="$PROYECTO-api"
ROL_SERVICIO=aws-elasticbeanstalk-service-role
ROL_INSTANCIA="$PROYECTO-api-ec2"
ROL_DESPLIEGUE="$PROYECTO-github-despliegue"
COMENTARIO_CDN="$PROYECTO API (KAN-11)"
CUENTA_FIREBASE_ARCHIVO="${1:-}"

falla() { echo "ERROR: $*" >&2; exit 1; }
titulo() { printf '\n== %s ==\n' "$1"; }
existe_parametro() { aws ssm get-parameter --name "$RUTA_PARAMETROS/$1" >/dev/null 2>&1; }
guardar_parametro() {  # nombre tipo valor
  aws ssm put-parameter --name "$RUTA_PARAMETROS/$1" --type "$2" --value "$3" --overwrite >/dev/null
}

for comando in aws python3 openssl; do
  command -v "$comando" >/dev/null 2>&1 || falla "falta '$comando'. Ejecuta este script en AWS CloudShell."
done
aws sts get-caller-identity >/dev/null 2>&1 || falla "no hay sesión de AWS. Abre CloudShell desde la consola."
CUENTA="$(aws sts get-caller-identity --query Account --output text)"

if [[ -n "$CUENTA_FIREBASE_ARCHIVO" ]]; then
  [[ -r "$CUENTA_FIREBASE_ARCHIVO" ]] || falla "no puedo leer $CUENTA_FIREBASE_ARCHIVO."
  python3 - "$CUENTA_FIREBASE_ARCHIVO" <<'PY' || falla "ese archivo no es una cuenta de servicio de Firebase."
import json, sys
datos = json.load(open(sys.argv[1]))
assert datos.get("type") == "service_account" and datos.get("project_id") and datos.get("private_key")
PY
elif ! existe_parametro FIREBASE_CUENTA_SERVICIO; then
  falla "la primera vez hay que pasar la cuenta de servicio de Firebase: bash $0 ~/archivo.json"
fi

ADMIN_INICIAL_CORREO="${ADMIN_INICIAL_CORREO:-}"
if [[ -z "$ADMIN_INICIAL_CORREO" ]]; then
  read -r -p "Correo de la primera cuenta ADMIN (ADR-16): " ADMIN_INICIAL_CORREO
fi
[[ "$ADMIN_INICIAL_CORREO" == *@*.* ]] || falla "el correo del ADMIN no parece válido."

echo
echo "Cuenta:       $CUENTA ($AWS_DEFAULT_REGION)"
echo "Ambiente EB:  $ENTORNO (Corretto 21, una instancia $CLASE)"
echo "Base:         cocina_produccion en $INSTANCIA_RDS"
echo "Secretos:     SSM $RUTA_PARAMETROS (cifrados)"
echo "HTTPS:        CloudFront delante del ambiente"
echo "Despliegue:   GitHub Actions de $REPO_GITHUB (ambiente $AMBIENTE_GITHUB)"
echo "ADMIN:        $ADMIN_INICIAL_CORREO"
echo
read -r -p "¿Crear o actualizar estos recursos? (s/N) " respuesta
[[ "$respuesta" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

# ─── Secretos en Parameter Store ──────────────────────────────────────────────
titulo "Secretos de producción"
aws rds describe-db-instances --db-instance-identifier "$INSTANCIA_RDS" >/dev/null 2>&1 \
  || falla "no existe la instancia $INSTANCIA_RDS. Primero: bash infra/aws/crear-base-datos.sh"
HOST_RDS="$(aws rds describe-db-instances --db-instance-identifier "$INSTANCIA_RDS" \
  --query 'DBInstances[0].Endpoint.Address' --output text)"
SG_RDS="$(aws rds describe-db-instances --db-instance-identifier "$INSTANCIA_RDS" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text)"

guardar_parametro DB_URL String \
  "jdbc:mysql://$HOST_RDS:3306/cocina_produccion?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&sslMode=REQUIRED"
guardar_parametro DB_USUARIO String app_produccion

if existe_parametro DB_CONTRASENA && [[ "${CAMBIAR_CONTRASENA_DB:-no}" != "si" ]]; then
  echo "DB_CONTRASENA ya está guardada (para cambiarla: CAMBIAR_CONTRASENA_DB=si)."
else
  echo "Escribe la contraseña de app_produccion (la que dio crear-base-datos.sh)."
  read -r -s -p "Contraseña: " CONTRASENA_DB; echo
  [[ -n "$CONTRASENA_DB" ]] || falla "la contraseña no puede quedar vacía."
  guardar_parametro DB_CONTRASENA SecureString "$CONTRASENA_DB"
  unset CONTRASENA_DB
fi

# La clave de cifrado se genera una sola vez. Cambiarla deja ilegibles los datos de
# salud ya guardados, así que el script nunca la reemplaza.
if existe_parametro CIFRADO_CLAVE_V1; then
  echo "CIFRADO_CLAVE_V1 ya existe; no se toca."
else
  guardar_parametro CIFRADO_CLAVE_V1 SecureString "$(openssl rand -base64 32)"
  echo "CIFRADO_CLAVE_V1 generada y guardada."
fi

if [[ -n "$CUENTA_FIREBASE_ARCHIVO" ]]; then
  aws ssm put-parameter --name "$RUTA_PARAMETROS/FIREBASE_CUENTA_SERVICIO" --type SecureString \
    --value "file://$CUENTA_FIREBASE_ARCHIVO" --overwrite >/dev/null
  rm -f "$CUENTA_FIREBASE_ARCHIVO"
  echo "Cuenta de servicio de Firebase guardada y borrada de CloudShell."
fi
PROYECTO_FIREBASE="$(aws ssm get-parameter --name "$RUTA_PARAMETROS/FIREBASE_CUENTA_SERVICIO" \
  --with-decryption --query Parameter.Value --output text \
  | python3 -c 'import json, sys; print(json.load(sys.stdin)["project_id"])')"
echo "Proyecto de Firebase: $PROYECTO_FIREBASE"
CORS_ORIGENES="https://$PROYECTO_FIREBASE.web.app,https://$PROYECTO_FIREBASE.firebaseapp.com"

# ─── Red: la API puede entrar a MySQL ─────────────────────────────────────────
titulo "Grupo de seguridad de la API"
VPC="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
SG_API="$(aws ec2 describe-security-groups --filters "Name=group-name,Values=$GRUPO_API" "Name=vpc-id,Values=$VPC" \
  --query 'SecurityGroups[0].GroupId' --output text)"
if [[ "$SG_API" == "None" || -z "$SG_API" ]]; then
  SG_API="$(aws ec2 create-security-group --group-name "$GRUPO_API" --vpc-id "$VPC" \
    --description "API de Cocina sin estres: puede entrar a MySQL" --query GroupId --output text)"
fi
# Se permite por grupo de seguridad, no por IP: si la instancia cambia, sigue entrando.
aws ec2 authorize-security-group-ingress --group-id "$SG_RDS" \
  --ip-permissions "IpProtocol=tcp,FromPort=3306,ToPort=3306,UserIdGroupPairs=[{GroupId=$SG_API,Description=API de produccion}]" \
  >/dev/null 2>&1 || true
echo "MySQL acepta conexiones de $SG_API."

# ─── Roles de IAM ─────────────────────────────────────────────────────────────
titulo "Roles de IAM"
confianza() {  # servicio
  printf '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"%s"},"Action":"sts:AssumeRole"}]}' "$1"
}
crear_rol() {  # nombre servicio
  aws iam get-role --role-name "$1" >/dev/null 2>&1 \
    || aws iam create-role --role-name "$1" --assume-role-policy-document "$(confianza "$2")" >/dev/null
}

crear_rol "$ROL_SERVICIO" elasticbeanstalk.amazonaws.com
for politica in AWSElasticBeanstalkEnhancedHealth AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy; do
  aws iam attach-role-policy --role-name "$ROL_SERVICIO" --policy-arn "arn:aws:iam::aws:policy/service-role/$politica" 2>/dev/null \
    || aws iam attach-role-policy --role-name "$ROL_SERVICIO" --policy-arn "arn:aws:iam::aws:policy/$politica"
done

crear_rol "$ROL_INSTANCIA" ec2.amazonaws.com
aws iam attach-role-policy --role-name "$ROL_INSTANCIA" --policy-arn arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier
# Solo puede leer los parámetros de producción de este proyecto.
aws iam put-role-policy --role-name "$ROL_INSTANCIA" --policy-name leer-secretos-produccion --policy-document "$(printf \
  '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["ssm:GetParametersByPath","ssm:GetParameter"],"Resource":["arn:aws:ssm:%s:%s:parameter%s","arn:aws:ssm:%s:%s:parameter%s/*"]}]}' \
  "$AWS_DEFAULT_REGION" "$CUENTA" "$RUTA_PARAMETROS" "$AWS_DEFAULT_REGION" "$CUENTA" "$RUTA_PARAMETROS")"
if ! aws iam get-instance-profile --instance-profile-name "$ROL_INSTANCIA" >/dev/null 2>&1; then
  aws iam create-instance-profile --instance-profile-name "$ROL_INSTANCIA" >/dev/null
  aws iam add-role-to-instance-profile --instance-profile-name "$ROL_INSTANCIA" --role-name "$ROL_INSTANCIA"
  echo "Esperando a que IAM propague el perfil de instancia..."
  sleep 15
fi
echo "Roles listos: $ROL_SERVICIO y $ROL_INSTANCIA."

# ─── Elastic Beanstalk ────────────────────────────────────────────────────────
titulo "Elastic Beanstalk"
aws elasticbeanstalk describe-applications --application-names "$APLICACION" \
  --query 'Applications[0].ApplicationName' --output text | grep -q "$APLICACION" \
  || aws elasticbeanstalk create-application --application-name "$APLICACION" \
       --description "Cocina sin estres: API REST" >/dev/null
BUCKET="$(aws elasticbeanstalk create-storage-location --query S3Bucket --output text)"

PLATAFORMA="$(aws elasticbeanstalk list-available-solution-stacks \
  --query "SolutionStacks[?contains(@, 'Amazon Linux 2023') && contains(@, 'Corretto 21')] | [0]" --output text)"
[[ "$PLATAFORMA" != "None" && -n "$PLATAFORMA" ]] || falla "no encontré la plataforma Corretto 21 de Elastic Beanstalk."
echo "Plataforma: $PLATAFORMA"

OPCIONES="$(mktemp)"
trap 'rm -f "$OPCIONES"' EXIT
python3 - "$OPCIONES" <<PY
import json, sys
opciones = [
    ("aws:elasticbeanstalk:environment", "EnvironmentType", "SingleInstance"),
    ("aws:elasticbeanstalk:environment", "ServiceRole", "$ROL_SERVICIO"),
    ("aws:autoscaling:launchconfiguration", "IamInstanceProfile", "$ROL_INSTANCIA"),
    ("aws:autoscaling:launchconfiguration", "SecurityGroups", "$SG_API"),
    # IMDSv2 y gp3 hacen que use plantillas de lanzamiento, obligatorias en cuentas nuevas.
    ("aws:autoscaling:launchconfiguration", "DisableIMDSv1", "true"),
    ("aws:autoscaling:launchconfiguration", "RootVolumeType", "gp3"),
    ("aws:ec2:instances", "InstanceTypes", "$CLASE"),
    ("aws:elasticbeanstalk:healthreporting:system", "SystemType", "enhanced"),
    ("aws:elasticbeanstalk:application", "Application Healthcheck URL", "/api/salud"),
    ("aws:elasticbeanstalk:cloudwatch:logs", "StreamLogs", "true"),
    ("aws:elasticbeanstalk:cloudwatch:logs", "RetentionInDays", "7"),
    # Variables que no son secretas. Las secretas las baja el hook desde SSM.
    ("aws:elasticbeanstalk:application:environment", "SPRING_PROFILES_ACTIVE", "prod"),
    ("aws:elasticbeanstalk:application:environment", "PUERTO", "5000"),
    ("aws:elasticbeanstalk:application:environment", "AWS_REGION", "$AWS_DEFAULT_REGION"),
    ("aws:elasticbeanstalk:application:environment", "PARAMETROS_RUTA", "$RUTA_PARAMETROS"),
    ("aws:elasticbeanstalk:application:environment", "CORS_ORIGENES", "$CORS_ORIGENES"),
    ("aws:elasticbeanstalk:application:environment", "ADMIN_INICIAL_CORREO", "$ADMIN_INICIAL_CORREO"),
]
json.dump([{"Namespace": n, "OptionName": o, "Value": v} for n, o, v in opciones], open(sys.argv[1], "w"))
PY

ESTADO="$(aws elasticbeanstalk describe-environments --application-name "$APLICACION" --environment-names "$ENTORNO" \
  --no-include-deleted --query 'Environments[0].Status' --output text)"
if [[ "$ESTADO" == "None" || -z "$ESTADO" ]]; then
  echo "Creando el ambiente $ENTORNO (tarda entre 5 y 10 minutos)..."
  aws elasticbeanstalk create-environment --application-name "$APLICACION" --environment-name "$ENTORNO" \
    --cname-prefix "$ENTORNO" --solution-stack-name "$PLATAFORMA" --option-settings "file://$OPCIONES" \
    --tags Key=proyecto,Value="$PROYECTO" >/dev/null
else
  echo "El ambiente ya existe ($ESTADO); se actualiza su configuración."
  aws elasticbeanstalk wait environment-updated --application-name "$APLICACION" --environment-names "$ENTORNO"
  aws elasticbeanstalk update-environment --application-name "$APLICACION" --environment-name "$ENTORNO" \
    --option-settings "file://$OPCIONES" >/dev/null
fi
aws elasticbeanstalk wait environment-updated --application-name "$APLICACION" --environment-names "$ENTORNO"
CNAME="$(aws elasticbeanstalk describe-environments --application-name "$APLICACION" --environment-names "$ENTORNO" \
  --query 'Environments[0].CNAME' --output text)"
echo "Ambiente listo: http://$CNAME"

# ─── CloudFront: HTTPS delante de la API ─────────────────────────────────────
titulo "CloudFront"
politica_cache="$PROYECTO-api-sin-cache"
ID_CACHE="$(aws cloudfront list-cache-policies --type custom \
  --query "CachePolicyList.Items[?CachePolicy.CachePolicyConfig.Name=='$politica_cache'].CachePolicy.Id | [0]" --output text)"
if [[ "$ID_CACHE" == "None" || -z "$ID_CACHE" ]]; then
  # TTL 0: no se guarda nada (la API además responde Cache-Control: no-store). El
  # encabezado Authorization tiene que estar en la clave para que CloudFront lo
  # mande a la API; así también es imposible servirle a alguien la respuesta de otro.
  ID_CACHE="$(aws cloudfront create-cache-policy --cache-policy-config "{
    \"Name\": \"$politica_cache\", \"Comment\": \"API de Cocina sin estres: sin cache\",
    \"DefaultTTL\": 0, \"MaxTTL\": 1, \"MinTTL\": 0,
    \"ParametersInCacheKeyAndForwardedToOrigin\": {
      \"EnableAcceptEncodingGzip\": true, \"EnableAcceptEncodingBrotli\": true,
      \"HeadersConfig\": {\"HeaderBehavior\": \"whitelist\", \"Headers\": {\"Quantity\": 1, \"Items\": [\"Authorization\"]}},
      \"CookiesConfig\": {\"CookieBehavior\": \"none\"},
      \"QueryStringsConfig\": {\"QueryStringBehavior\": \"all\"}}}" --query CachePolicy.Id --output text)"
fi
politica_origen="$PROYECTO-api-encabezados"
ID_ORIGEN="$(aws cloudfront list-origin-request-policies --type custom \
  --query "OriginRequestPolicyList.Items[?OriginRequestPolicy.OriginRequestPolicyConfig.Name=='$politica_origen'].OriginRequestPolicy.Id | [0]" --output text)"
if [[ "$ID_ORIGEN" == "None" || -z "$ID_ORIGEN" ]]; then
  # Lo que necesita CORS y el cuerpo JSON. El Host no se reenvía: Elastic Beanstalk
  # espera el suyo.
  ID_ORIGEN="$(aws cloudfront create-origin-request-policy --origin-request-policy-config "{
    \"Name\": \"$politica_origen\", \"Comment\": \"API de Cocina sin estres: CORS y JSON\",
    \"HeadersConfig\": {\"HeaderBehavior\": \"whitelist\", \"Headers\": {\"Quantity\": 5, \"Items\": [
      \"Origin\", \"Content-Type\", \"Accept\", \"Access-Control-Request-Method\", \"Access-Control-Request-Headers\"]}},
    \"CookiesConfig\": {\"CookieBehavior\": \"none\"},
    \"QueryStringsConfig\": {\"QueryStringBehavior\": \"none\"}}" --query OriginRequestPolicy.Id --output text)"
fi

ID_CDN="$(aws cloudfront list-distributions \
  --query "DistributionList.Items[?Comment=='$COMENTARIO_CDN'].Id | [0]" --output text)"
if [[ "$ID_CDN" == "None" || -z "$ID_CDN" ]]; then
  echo "Creando la distribución (queda lista en unos 5 minutos)..."
  ID_CDN="$(aws cloudfront create-distribution --distribution-config "{
    \"CallerReference\": \"$PROYECTO-api-$(date +%s)\", \"Comment\": \"$COMENTARIO_CDN\", \"Enabled\": true,
    \"PriceClass\": \"PriceClass_100\", \"HttpVersion\": \"http2and3\",
    \"Origins\": {\"Quantity\": 1, \"Items\": [{\"Id\": \"api\", \"DomainName\": \"$CNAME\",
      \"CustomOriginConfig\": {\"HTTPPort\": 80, \"HTTPSPort\": 443, \"OriginProtocolPolicy\": \"http-only\",
        \"OriginReadTimeout\": 30, \"OriginKeepaliveTimeout\": 5,
        \"OriginSslProtocols\": {\"Quantity\": 1, \"Items\": [\"TLSv1.2\"]}}}]},
    \"DefaultCacheBehavior\": {\"TargetOriginId\": \"api\", \"ViewerProtocolPolicy\": \"https-only\",
      \"CachePolicyId\": \"$ID_CACHE\", \"OriginRequestPolicyId\": \"$ID_ORIGEN\", \"Compress\": true,
      \"AllowedMethods\": {\"Quantity\": 7, \"Items\": [\"GET\", \"HEAD\", \"OPTIONS\", \"PUT\", \"POST\", \"PATCH\", \"DELETE\"],
        \"CachedMethods\": {\"Quantity\": 2, \"Items\": [\"GET\", \"HEAD\"]}}}}" --query Distribution.Id --output text)"
fi
DOMINIO_CDN="$(aws cloudfront get-distribution --id "$ID_CDN" --query Distribution.DomainName --output text)"
echo "API pública: https://$DOMINIO_CDN/api"

# ─── GitHub Actions puede desplegar (OIDC) ───────────────────────────────────
titulo "Rol de despliegue para GitHub Actions"
PROVEEDOR="arn:aws:iam::$CUENTA:oidc-provider/token.actions.githubusercontent.com"
if ! aws iam get-open-id-connect-provider --open-id-connect-provider-arn "$PROVEEDOR" >/dev/null 2>&1; then
  aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com >/dev/null 2>&1 \
  || aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 >/dev/null
fi
# Solo los jobs del ambiente "produccion" de este repositorio pueden asumir el rol.
CONFIANZA_GITHUB="$(printf '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Federated":"%s"},"Action":"sts:AssumeRoleWithWebIdentity","Condition":{"StringEquals":{"token.actions.githubusercontent.com:aud":"sts.amazonaws.com","token.actions.githubusercontent.com:sub":"repo:%s:environment:%s"}}}]}' \
  "$PROVEEDOR" "$REPO_GITHUB" "$AMBIENTE_GITHUB")"
if aws iam get-role --role-name "$ROL_DESPLIEGUE" >/dev/null 2>&1; then
  aws iam update-assume-role-policy --role-name "$ROL_DESPLIEGUE" --policy-document "$CONFIANZA_GITHUB"
else
  aws iam create-role --role-name "$ROL_DESPLIEGUE" --assume-role-policy-document "$CONFIANZA_GITHUB" >/dev/null
fi
aws iam attach-role-policy --role-name "$ROL_DESPLIEGUE" \
  --policy-arn arn:aws:iam::aws:policy/AdministratorAccess-AWSElasticBeanstalk
ARN_DESPLIEGUE="$(aws iam get-role --role-name "$ROL_DESPLIEGUE" --query Role.Arn --output text)"

# ─── Resumen ──────────────────────────────────────────────────────────────────
cat <<FIN

================================================================================
Listo. Crea el ambiente "produccion" en GitHub (Settings → Environments) y agrega
estas VARIABLES (no son secretas):

  AWS_REGION                 $AWS_DEFAULT_REGION
  AWS_ROL_DESPLIEGUE         $ARN_DESPLIEGUE
  EB_APLICACION              $APLICACION
  EB_ENTORNO                 $ENTORNO
  EB_BUCKET                  $BUCKET
  API_URL                    https://$DOMINIO_CDN/api
  VITE_FIREBASE_PROJECT_ID   $PROYECTO_FIREBASE

Orígenes permitidos por CORS: $CORS_ORIGENES
CloudFront tarda unos minutos en quedar activo. Después del primer despliegue:
  curl https://$DOMINIO_CDN/api/salud
================================================================================
FIN
