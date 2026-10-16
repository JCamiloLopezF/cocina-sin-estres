#!/usr/bin/env bash
# KAN-11 · Baja los secretos de AWS Systems Manager antes de arrancar la API.
#
# Corre como root en cada despliegue (y la copia de confighooks, en cada cambio de
# configuración). Lee todos los parámetros bajo PARAMETROS_RUTA y deja:
#   /etc/cocina/secretos.env                 variables para arrancar.sh
#   /etc/cocina/firebase-cuenta-servicio.json la cuenta de servicio de Firebase
# Los dos archivos solo los lee root y el grupo webapp, que es con el que corre la API.
set -euo pipefail

config() { /opt/elasticbeanstalk/bin/get-config environment -k "$1"; }
RUTA="$(config PARAMETROS_RUTA)"
REGION="$(config AWS_REGION)"
DESTINO=/etc/cocina

install -d -m 750 -o root -g webapp "$DESTINO"
TEMPORAL="$(mktemp)"
trap 'rm -f "$TEMPORAL"' EXIT
aws ssm get-parameters-by-path --region "$REGION" --path "$RUTA" \
  --with-decryption --output json > "$TEMPORAL"

python3 - "$TEMPORAL" "$DESTINO" <<'PY'
import json
import os
import shlex
import sys

parametros = json.load(open(sys.argv[1]))["Parameters"]
destino = sys.argv[2]
lineas = []
for parametro in parametros:
    nombre = parametro["Name"].rsplit("/", 1)[1]
    valor = parametro["Value"]
    if nombre == "FIREBASE_CUENTA_SERVICIO":
        ruta = os.path.join(destino, "firebase-cuenta-servicio.json")
        with open(os.open(ruta, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o640), "w") as archivo:
            archivo.write(valor)
        lineas.append("FIREBASE_CREDENCIALES_RUTA=" + shlex.quote(ruta))
    else:
        lineas.append(nombre + "=" + shlex.quote(valor))

faltan = {"DB_URL", "DB_USUARIO", "DB_CONTRASENA", "CIFRADO_CLAVE_V1", "FIREBASE_CREDENCIALES_RUTA"} - {
    linea.split("=", 1)[0] for linea in lineas
}
if faltan:
    sys.exit("Faltan parámetros en SSM: " + ", ".join(sorted(faltan)))

ruta = os.path.join(destino, "secretos.env")
with open(os.open(ruta, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o640), "w") as archivo:
    archivo.write("\n".join(lineas) + "\n")
PY

chown root:webapp "$DESTINO"/secretos.env "$DESTINO"/firebase-cuenta-servicio.json
chmod 640 "$DESTINO"/secretos.env "$DESTINO"/firebase-cuenta-servicio.json
echo "Secretos de $RUTA listos en $DESTINO"
