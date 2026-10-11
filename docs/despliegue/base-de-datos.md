# Base de datos en la nube (KAN-9)

MySQL administrado en Azure, con un ambiente de desarrollo y uno de producción y respaldo automático diario (RNF-20). Lo crea y lo mantiene Juan; el resto del equipo sigue trabajando con el MySQL del `docker-compose.yml` en su computador.

Decisión de proveedor: [ADR-22](../adr/). Firebase solo maneja la identidad (ADR-05); los datos de la aplicación viven en MySQL.

## Qué se crea

| Recurso | Nombre | Configuración |
| --- | --- | --- |
| Grupo de recursos | `rg-cocina-sin-estres` | En una región permitida por la suscripción |
| Servidor | `cocina-sin-estres-mysql` | Azure Database for MySQL Flexible Server, MySQL 8.4, Burstable `Standard_B1ms`, 32 GB, sin alta disponibilidad |
| Base de desarrollo | `cocina_desarrollo` | `utf8mb4` / `utf8mb4_unicode_ci`, igual que la local |
| Base de producción | `cocina_produccion` | Igual |
| Usuario de desarrollo | `app_desarrollo` | Todos los permisos, **solo** sobre `cocina_desarrollo`; exige TLS |
| Usuario de producción | `app_produccion` | Todos los permisos, **solo** sobre `cocina_produccion`; exige TLS |
| Administrador | `cocinaadmin` | Solo para Juan (crear bases y usuarios, revisar). La aplicación nunca lo usa |

Host: `cocina-sin-estres-mysql.mysql.database.azure.com`, puerto 3306.

### Por qué un servidor con dos bases

La oferta gratuita de Azure cubre **750 horas al mes de un servidor B1ms** durante 12 meses, con 32 GB de almacenamiento y 32 GB de respaldo. Eso alcanza para un servidor encendido todo el mes, no para dos. Separar los ambientes por base, cada una con su propio usuario, cumple el criterio de la ADR-22 sin costo:

- `app_desarrollo` no puede leer ni modificar `cocina_produccion`, y al revés.
- Flyway lleva su historial (`flyway_schema_history`) dentro de cada base, así que cada ambiente avanza por su cuenta.
- La contrapartida es que comparten CPU y memoria. Para el volumen del curso no importa; si en algún momento hiciera falta, se crea un segundo servidor con el crédito de Azure for Students.

### Red y cifrado

- **TLS obligatorio:** `require_secure_transport = ON` en el servidor, y los dos usuarios se crearon con `REQUIRE SSL`. La URL JDBC lleva `sslMode=REQUIRED`.
- **Firewall:** solo se aceptan conexiones desde servicios de Azure (regla `0.0.0.0`), que es por donde llegarán Container Apps (KAN-11) y Cloud Shell. Para conectarse desde un computador hay que agregar su IP (ver [Conectarte desde tu computador](#conectarte-desde-tu-computador)).
- La regla de servicios de Azure admite cualquier recurso de Azure, no solo los nuestros. Lo que protege la base son las contraseñas largas al azar y el TLS. La alternativa (red virtual privada) cuesta más y no se justifica en el curso.

### Spring: qué perfil usa cada ambiente

| Ambiente | Dónde corre la API | Perfil de Spring | Base |
| --- | --- | --- | --- |
| Local | Tu computador | `dev` | MySQL del `docker-compose.yml` |
| Desarrollo | Azure (KAN-11) | `prod` | `cocina_desarrollo` |
| Producción | Azure (KAN-11) | `prod` | `cocina_produccion` |

**Los dos ambientes de la nube usan el perfil `prod`.** El perfil `dev` activa el encabezado `X-Dev-User`, que permite entrar como cualquier usuario sin Firebase: en una URL pública sería una puerta abierta. Lo único que cambia entre desarrollo y producción son los valores de las variables.

## Costo

- Con la configuración del script (B1ms, 32 GB, crecimiento automático del disco apagado, sin alta disponibilidad, sin respaldo geográfico) el servidor queda dentro de la oferta gratuita. El almacenamiento de respaldo es gratis hasta el tamaño del disco.
- Azure for Students no pide tarjeta: si algo se pasara de la oferta, se descuenta del crédito de USD 100.
- Revisa el consumo cada semana en el portal: **Suscripciones → Azure for Students → Análisis de costos**.
- **No** actives alta disponibilidad, respaldo geográfico ni réplicas de lectura: cada uno cobra aparte.

## Crear la base de datos (una sola vez)

Lo hace Juan con su cuenta de Azure (`juanc.lopezf@uqvirtual.edu.co`).

1. Entra a [portal.azure.com](https://portal.azure.com) y abre **Cloud Shell** (ícono `>_` arriba), en modo **Bash**. Si pide crear almacenamiento, acepta la opción gratuita o elige *No se requiere cuenta de almacenamiento*.
2. Busca las regiones permitidas. Azure for Students solo deja crear recursos en algunas regiones, y la lista cambia por cuenta. El script las muestra si lo ejecutas sin argumentos:
   ```bash
   git clone https://github.com/JCamiloLopezF/cocina-sin-estres.git
   cd cocina-sin-estres
   bash infra/azure/crear-base-datos.sh
   ```
   Si no aparecen, búscalas en el portal: **Policy → Assignments → Allowed resource deployment regions → Allowed locations**. Elige la más cercana a Colombia entre las permitidas (por ejemplo `brazilsouth`, `eastus` o `southcentralus`, si están en tu lista).
3. Ejecuta el script con la región:
   ```bash
   bash infra/azure/crear-base-datos.sh eastus
   ```
   Te pide confirmar y escribir la contraseña del administrador (guárdala en tu gestor de contraseñas). Tarda entre 5 y 15 minutos.
4. Al final muestra `DB_URL`, `DB_USUARIO` y `DB_CONTRASENA` de cada ambiente, **una sola vez**:
   - Guárdalas en tu gestor de contraseñas.
   - En GitHub, crea los ambientes **Settings → Environments → New environment**: `desarrollo` y `produccion`. En cada uno agrega los tres secretos con los valores de su ambiente. Los usará el despliegue de KAN-11.
   - Borra la pantalla con `clear`.
5. Revisa que todo quedó bien:
   ```bash
   bash infra/azure/verificar-base-datos.sh
   ```

El script se puede volver a ejecutar sin romper nada: no recrea el servidor ni borra datos. **Sí cambia las contraseñas de `app_desarrollo` y `app_produccion`**, así que sirve para rotarlas; después hay que actualizar los secretos de GitHub.

## Conectarte desde tu computador

Hace falta para aplicar las migraciones antes de que exista el despliegue (KAN-11) o para revisar los datos con MySQL Workbench o DBeaver.

1. Averigua tu IP pública (por ejemplo en [ifconfig.me](https://ifconfig.me)) y agrégala al firewall desde Cloud Shell:
   ```bash
   az mysql flexible-server firewall-rule create -g rg-cocina-sin-estres -n cocina-sin-estres-mysql \
     --rule-name juan-casa --start-ip-address <tu-ip> --end-ip-address <tu-ip>
   ```
   La IP de tu casa puede cambiar: si la conexión deja de responder, revisa la regla. Cuando ya no la necesites, bórrala con `az mysql flexible-server firewall-rule delete ... --rule-name juan-casa`.
2. Aplica las migraciones a `cocina_desarrollo` arrancando la API en tu computador contra esa base. Flyway aplica lo que haya en `db/migration`:
   ```bash
   cd backend
   export DB_URL='jdbc:mysql://cocina-sin-estres-mysql.mysql.database.azure.com:3306/cocina_desarrollo?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&sslMode=REQUIRED'
   export DB_USUARIO=app_desarrollo
   read -r -s -p "Contraseña: " DB_CONTRASENA; export DB_CONTRASENA; echo
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```
   En el log debe aparecer `Successfully applied N migrations to schema cocina_desarrollo`. Así la contraseña no queda en el historial de la terminal. Aquí sí se usa el perfil `dev`, porque la API corre solo en tu computador.
3. **No hagas esto contra `cocina_produccion`.** Producción solo recibe migraciones desde el despliegue (KAN-11), cuando el código ya pasó por `main`.

Para MySQL Workbench o DBeaver: host `cocina-sin-estres-mysql.mysql.database.azure.com`, puerto 3306 y SSL en *Require*. Para mirar, usa `app_desarrollo`. Nunca cambies el esquema a mano: los cambios van como migración nueva de Flyway (ADR-07).

## Respaldo diario (RNF-20)

Azure hace los respaldos solo; no hay que programar nada:

- **Un respaldo completo al día**, y respaldo del registro de transacciones cada 5 minutos.
- **Retención de 7 días:** se puede restaurar la base a cualquier minuto de la última semana.
- Los respaldos son del servidor completo, así que cubren las dos bases.

### Evidencia para la sustentación

1. `bash infra/azure/verificar-base-datos.sh` muestra la retención (`RetencionRespaldoDias: 7`), la fecha más antigua restaurable y la lista de respaldos completos con su fecha, uno por día.
2. En el portal, **servidor → Backup and restore** muestra la misma lista. Toma una captura cada semana y súbela a KAN-9 en Jira.

### Prueba de restauración (una vez, antes de la sustentación)

Comprobar que el respaldo sirve es mejor evidencia que mostrar que existe.

1. En el portal: **servidor → Backup and restore → Restore** sobre el último respaldo. Nombre del servidor nuevo: `cocina-sin-estres-mysql-prueba`.
2. Cuando termine (unos 15 minutos), conéctate al servidor nuevo con el administrador y comprueba que `cocina_produccion` tiene sus tablas y datos.
3. Toma la captura y **borra el servidor de prueba en cuanto termines**: consume horas de la misma oferta gratuita.

La restauración siempre crea un servidor nuevo y deja intacto el original.

## Problemas comunes

| Síntoma | Causa y solución |
| --- | --- |
| `RequestDisallowedByAzure` al crear | La región no está permitida para Azure for Students. Usa una de la lista del paso 2. |
| `--version 8.4` no se acepta | Cloud Shell con la CLI desactualizada o región sin 8.4. Ejecuta con `VERSION_MYSQL=8.0.21 bash infra/azure/crear-base-datos.sh <región>` y avisa al equipo: local y nube quedarían en versiones distintas. |
| La conexión se queda esperando y falla por tiempo | Falta la regla de firewall de tu IP, o tu IP cambió. |
| `Connections using insecure transport are prohibited` | Falta `sslMode=REQUIRED` en la URL JDBC (o `--ssl-mode=REQUIRED` en el cliente `mysql`). |
| `Access denied for user 'app_desarrollo'` | Contraseña equivocada o rotada. Vuelve a ejecutar el script y actualiza los secretos. |
| Flyway falla por *checksum* | Alguien modificó una migración que ya estaba aplicada. Nunca se edita una migración unida a `develop`: se crea una nueva. |

## Al terminar el semestre

Para no consumir crédito, Juan borra todo el grupo de recursos desde Cloud Shell: `az group delete -n rg-cocina-sin-estres`. Antes, si se quiere conservar una copia, se exportan los datos con `mysqldump`, porque los respaldos de Azure no se pueden descargar.
