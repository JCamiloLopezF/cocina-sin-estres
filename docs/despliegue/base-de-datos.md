# Base de datos en la nube (KAN-9)

MySQL administrado en Google Cloud (Cloud SQL), con un ambiente de desarrollo y uno de producción y respaldo automático diario (RNF-20). Lo crea y lo mantiene Juan; el resto del equipo sigue trabajando con el MySQL del `docker-compose.yml` en su computador.

Decisión de proveedor: [ADR-22](../adr/). Todo vive en **un solo proyecto de Firebase / Google Cloud**: Firebase Authentication (identidad, ADR-05), Firebase Hosting (frontend), Cloud Run (backend, KAN-11) y Cloud SQL (esta base).

## Qué se crea

| Recurso | Nombre | Configuración |
| --- | --- | --- |
| Instancia | `cocina-sin-estres-mysql` | Cloud SQL para MySQL 8.4, edición **Enterprise**, `db-f1-micro`, 10 GB SSD fijos, zonal (sin alta disponibilidad), región `us-east1` |
| Base de desarrollo | `cocina_desarrollo` | `utf8mb4` / `utf8mb4_unicode_ci`, igual que la local |
| Base de producción | `cocina_produccion` | Igual |
| Usuario de desarrollo | `app_desarrollo` | Todos los permisos, **solo** sobre `cocina_desarrollo` |
| Usuario de producción | `app_produccion` | Todos los permisos, **solo** sobre `cocina_produccion` |
| Administrador | `root` | Solo para Juan (crear usuarios, revisar). La aplicación nunca lo usa |

### Por qué una instancia con dos bases

Cada instancia se cobra por hora. Separar los ambientes por base, cada una con su propio usuario, cumple el criterio de ambientes separados (ADR-12) con la mitad del costo:

- `app_desarrollo` no puede leer ni modificar `cocina_produccion`, y al revés.
- Flyway lleva su historial (`flyway_schema_history`) dentro de cada base, así que cada ambiente avanza por su cuenta.
- La contrapartida es que comparten CPU y memoria. Para el volumen del curso no importa.

Los usuarios se crean con SQL y no con `gcloud sql users create`, porque estos últimos reciben permisos sobre todas las bases.

### Red y cifrado

- **Sin redes autorizadas.** Nadie se conecta por IP salvo que se autorice a mano. Cloud Run (KAN-11) entra con el **conector de Cloud SQL**, que usa los permisos del proyecto y cifra la conexión; no necesita abrir la base a internet. KAN-11 agrega su dependencia al backend (`com.google.cloud.sql:mysql-socket-factory-connector-j-8`); por eso la `DB_URL` de la nube lleva `socketFactory=...`.
- **Solo conexiones cifradas** (`ssl-mode=ENCRYPTED_ONLY`). Desde un computador, la URL JDBC lleva `sslMode=REQUIRED`.
- **Protección contra borrado** activada: para borrar la instancia hay que quitarla primero, a propósito.

### Spring: qué perfil usa cada ambiente

| Ambiente | Dónde corre la API | Perfil de Spring | Base |
| --- | --- | --- | --- |
| Local | Tu computador | `dev` | MySQL del `docker-compose.yml` |
| Desarrollo | Cloud Run (KAN-11) | `prod` | `cocina_desarrollo` |
| Producción | Cloud Run (KAN-11) | `prod` | `cocina_produccion` |

**Los dos ambientes de la nube usan el perfil `prod`.** El perfil `dev` activa el encabezado `X-Dev-User`, que permite entrar como cualquier usuario sin Firebase: en una URL pública sería una puerta abierta. Lo único que cambia entre desarrollo y producción son los valores de las variables.

## Costo

| Concepto | Costo aproximado |
| --- | --- |
| Instancia `db-f1-micro` (Enterprise) | USD 7,7 al mes |
| 10 GB de SSD | USD 1,7 al mes |
| Respaldos (7 copias) y registros de transacciones | Menos de USD 1 al mes |
| **Total** | **Unos USD 10 al mes** |

- Se paga con el crédito de la **prueba gratuita de Google Cloud (USD 300)**. La consola muestra cuánto queda y hasta qué fecha.
- La prueba pide una tarjeta solo para verificar la identidad. **No cobra nada mientras no actives una cuenta paga**; si el crédito o el plazo se acaban, los recursos se detienen.
- **La edición importa:** para MySQL 8.4, Google crea por defecto la edición Enterprise Plus, que cuesta unas 25 veces más. El script pide `--edition=enterprise` a propósito; si alguien crea una instancia desde la consola, debe elegir **Enterprise** y **Shared core → db-f1-micro**.
- Crea un **presupuesto con alertas** (paso 3) para enterarte si algo se dispara.

## Crear la base de datos (una sola vez)

Lo hace Juan con su cuenta de Google.

### 1. Activar la prueba gratuita

1. Entra a [console.cloud.google.com/freetrial](https://console.cloud.google.com/freetrial) con tu cuenta de Google. Usa una cuenta personal (Gmail): las cuentas de la universidad pueden tener Google Cloud bloqueado por el administrador.
2. País **Colombia**, tipo de cuenta **Individual**, y la tarjeta (débito o crédito; las prepagadas no sirven). Puede aparecer una retención temporal pequeña que luego se devuelve.
3. Al terminar verás el aviso del crédito de USD 300 arriba en la consola.

### 2. Crear el proyecto de Firebase y vincularle la facturación

El proyecto de Firebase **es** el proyecto de Google Cloud, así que se crea una sola vez y sirve para todo (Auth, Hosting, Cloud Run y Cloud SQL).

1. En [console.firebase.google.com](https://console.firebase.google.com) crea el proyecto **cocina-sin-estres** (Google Analytics no hace falta). Si ya existe para KAN-10, usa ese.
2. Anota el **ID del proyecto** (por ejemplo `cocina-sin-estres` o `cocina-sin-estres-1a2b3`): aparece en *Configuración del proyecto*. No es lo mismo que el nombre.
3. En [console.cloud.google.com/billing](https://console.cloud.google.com/billing), abre la cuenta de facturación de la prueba → **Administración de cuentas** (o *Mis proyectos*) → vincula el proyecto. Firebase pasará a mostrar el plan **Blaze**: es normal, los cargos salen del crédito.

### 3. Crear un presupuesto con alertas

En **Facturación → Presupuestos y alertas → Crear presupuesto**: alcance el proyecto, monto **USD 20 al mes**, alertas al 50 %, 90 % y 100 %. Te llega un correo si el gasto se acerca.

### 4. Ejecutar el script en Cloud Shell

1. En [console.cloud.google.com](https://console.cloud.google.com), con el proyecto seleccionado arriba, abre **Cloud Shell** (ícono `>_`).
2. Ejecuta:
   ```bash
   git clone -b chore/KAN-9-base-datos-nube https://github.com/JCamiloLopezF/cocina-sin-estres.git
   cd cocina-sin-estres
   bash infra/gcp/crear-base-datos.sh ID-DEL-PROYECTO
   ```
   (Cuando KAN-9 esté unido a `develop`, el `-b chore/...` ya no hace falta.)
3. El script:
   - te pide confirmar y elegir la **contraseña de root** (guárdala en tu gestor);
   - crea la instancia (entre 5 y 15 minutos);
   - crea las dos bases;
   - crea los dos usuarios: aquí el cliente `mysql` te pide **otra vez la contraseña de root** (`Enter password:`).
4. Al final muestra, **una sola vez**, el usuario, la contraseña y la URL de cada ambiente:
   - Guárdalos en tu gestor de contraseñas.
   - En GitHub, crea los ambientes **Settings → Environments → New environment**: `desarrollo` y `produccion`. En cada uno agrega los secretos `DB_URL` (la de Cloud Run), `DB_USUARIO` y `DB_CONTRASENA`. Los usará el despliegue de KAN-11.
   - Borra la pantalla con `clear`.
5. Revisa que todo quedó bien:
   ```bash
   bash infra/gcp/verificar-base-datos.sh ID-DEL-PROYECTO
   ```

Si sale un error que diga que `db-f1-micro` no está disponible para esa versión, ejecuta de nuevo con `TIER=db-g1-small bash infra/gcp/crear-base-datos.sh ID-DEL-PROYECTO` (unos USD 26 al mes, también dentro del crédito).

El script se puede volver a ejecutar sin romper nada: no recrea la instancia ni borra datos. **Sí cambia las contraseñas de `app_desarrollo` y `app_produccion`**, así que sirve para rotarlas; después hay que actualizar los secretos de GitHub.

## Conectarte desde tu computador

Hace falta para aplicar las migraciones antes de que exista el despliegue (KAN-11) o para revisar los datos con MySQL Workbench o DBeaver.

1. Averigua tu IP pública (por ejemplo en [ifconfig.me](https://ifconfig.me)) y autorízala desde Cloud Shell:
   ```bash
   gcloud sql instances patch cocina-sin-estres-mysql --authorized-networks=TU_IP/32
   ```
2. Aplica las migraciones a `cocina_desarrollo` arrancando la API en tu computador contra esa base (usa la URL "desde tu computador" que mostró el script):
   ```bash
   cd backend
   export DB_URL='jdbc:mysql://IP-DE-LA-INSTANCIA:3306/cocina_desarrollo?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&sslMode=REQUIRED'
   export DB_USUARIO=app_desarrollo
   read -r -s -p "Contraseña: " DB_CONTRASENA; export DB_CONTRASENA; echo
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```
   En el log debe aparecer `Successfully applied N migrations to schema cocina_desarrollo`. Así la contraseña no queda en el historial de la terminal. Aquí sí se usa el perfil `dev`, porque la API corre solo en tu computador.
3. Cuando termines, quita tu IP:
   ```bash
   gcloud sql instances patch cocina-sin-estres-mysql --clear-authorized-networks
   ```
4. **No hagas esto contra `cocina_produccion`.** Producción solo recibe migraciones desde el despliegue (KAN-11), cuando el código ya pasó por `main`.

Para MySQL Workbench o DBeaver: host = IP de la instancia, puerto 3306, SSL en *Require*, usuario `app_desarrollo`. Nunca cambies el esquema a mano: los cambios van como migración nueva de Flyway (ADR-07).

## Respaldo diario (RNF-20)

Cloud SQL hace los respaldos solo; no hay que programar nada:

- **Un respaldo automático al día**, a las 08:00 UTC (3:00 a. m. en Colombia), y se conservan los **últimos 7**.
- **Registro binario activado**: se puede restaurar a cualquier minuto de los últimos 7 días (recuperación a un punto en el tiempo).
- Los respaldos son de la instancia completa, así que cubren las dos bases.

### Evidencia para la sustentación

1. `bash infra/gcp/verificar-base-datos.sh` muestra la configuración de respaldos (`enabled: true`, `retainedBackups: 7`, `binaryLogEnabled: true`) y la lista de respaldos con su fecha, uno por día.
2. En la consola, **SQL → cocina-sin-estres-mysql → Copias de seguridad** muestra la misma lista. Toma una captura cada semana y súbela a KAN-9 en Jira.

### Prueba de restauración (una vez, antes de la sustentación)

Comprobar que el respaldo sirve es mejor evidencia que mostrar que existe.

1. En la consola: **Copias de seguridad → Restaurar** sobre el último respaldo, eligiendo **crear una instancia nueva** llamada `cocina-sin-estres-prueba` (nunca sobrescribas la original).
2. Cuando termine, conéctate con `gcloud sql connect cocina-sin-estres-prueba --user=root` y comprueba que `cocina_produccion` tiene sus tablas y datos.
3. Toma la captura y **borra la instancia de prueba en cuanto termines**: cobra por hora.

## Problemas comunes

| Síntoma | Causa y solución |
| --- | --- |
| `no tiene una cuenta de facturación vinculada` | Falta el paso 2.3: vincular el proyecto a la cuenta de la prueba. |
| `db-f1-micro` rechazado | Ejecuta con `TIER=db-g1-small` (ver paso 4). |
| La instancia sale como *Enterprise Plus* | Se creó desde la consola sin cambiar la edición. Bórrala (quita antes la protección contra borrado) y créala con el script. |
| `Access denied for user 'root'` al crear los usuarios | Contraseña de root equivocada. Vuelve a ejecutar el script; si la olvidaste: `gcloud sql users set-password root --host=% --instance=cocina-sin-estres-mysql --prompt-for-password`. |
| La conexión desde tu computador se queda esperando | Tu IP no está autorizada o cambió: repite el paso 1 de *Conectarte desde tu computador*. |
| `Connections using insecure transport are prohibited` | Falta `sslMode=REQUIRED` en la URL JDBC. |
| Flyway falla por *checksum* | Alguien modificó una migración que ya estaba aplicada. Nunca se edita una migración unida a `develop`: se crea una nueva. |

## Al terminar el semestre

Para no consumir crédito, Juan borra la instancia: primero `gcloud sql instances patch cocina-sin-estres-mysql --no-deletion-protection` y luego `gcloud sql instances delete cocina-sin-estres-mysql`. Antes, si se quiere conservar una copia, se exportan los datos con **Exportar** en la consola (a un archivo SQL en Cloud Storage) o con `mysqldump`.
