#!/usr/bin/env bash
# KAN-11 · Arranque de la API en Elastic Beanstalk.
#
# Carga los secretos que dejó el hook de prebuild en /etc/cocina/secretos.env y
# arranca el jar. Así la contraseña de la base y la clave de cifrado no quedan en
# la configuración del ambiente de Elastic Beanstalk, que se ve en la consola.
set -euo pipefail

set -a
# shellcheck disable=SC1091  # lo escribe el hook 01_secretos.sh en cada despliegue
. /etc/cocina/secretos.env
set +a

# t4g.micro tiene 1 GB: 60 % para el heap y el resto para la JVM, nginx y el sistema.
exec java -XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError -jar app.jar
