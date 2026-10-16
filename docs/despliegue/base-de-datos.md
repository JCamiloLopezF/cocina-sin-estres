# Base de datos en la nube (KAN-9)

MySQL administrado en AWS (Amazon RDS), con un ambiente de desarrollo y uno de producción y respaldo automático diario (RNF-20). Lo crea y lo mantiene Juan; el resto del equipo sigue trabajando con el MySQL del `docker-compose.yml` en su computador.

Decisión de proveedor: [ADR-22](../adr/). El backend y la base de datos van en AWS; Firebase sigue a cargo de la identidad (ADR-05) y del hosting del frontend, en su plan gratuito.

## Qué se crea

Todo en la región **us-east-1 (Norte de Virginia)**.

| Recurso | Nombre | Configuración |
| --- | --- | --- |
| Instancia | `cocina-sin-estres-mysql` | Amazon RDS para MySQL 8.4, `db.t4g.micro`, 20 GB gp3 fijos, una sola zona, cifrada en reposo, protección contra borrado |
| Grupo de seguridad | `cocina-sin-estres-mysql` | El firewall de la base. **Sin ninguna entrada abierta**: solo se abre a mano para una IP o para el backend |
| Grupo de parámetros | `cocina-sin-estres-mysql-tls` | `require_secure_transport = 1`: solo conexiones cifradas |
| Base de desarrollo | `cocina_desarrollo` | `utf8mb4` / `utf8mb4_unicode_ci`, igual que la local |
| Base de producción | `cocina_produccion` | Igual |
| Usuario de desarrollo | `app_desarrollo` | Todos los permisos, **solo** sobre `cocina_desarrollo`; exige TLS |
| Usuario de producción | `app_produccion` | Todos los permisos, **solo** sobre `cocina_produccion`; exige TLS |
| Administrador | `cocinaadmin` | Solo para Juan (crear usuarios, revisar). La aplicación nunca lo usa |

### Por qué una instancia con dos bases

Cada instancia se cobra por hora. Separar los ambientes por base, cada una con su propio usuario, cumple el criterio de ambientes separados (ADR-12) con la mitad del costo:

- `app_desarrollo` no puede leer ni modificar `cocina_produccion`, y al revés.
- Flyway lleva su historial (`flyway_schema_history`) dentro de cada base, así que cada ambiente avanza por su cuenta.
- La contrapartida es que comparten CPU y memoria. Para el volumen del curso no importa.

### Red y cifrado

- La instancia tiene dirección pública, pero el **grupo de seguridad no deja entrar a nadie** por defecto. Los scripts abren el puerto 3306 solo para la IP de CloudShell mientras trabajan y lo cierran al terminar.
- El backend (KAN-11) entrará porque su grupo de seguridad se agrega como origen permitido; no por IP.
- **Solo conexiones cifradas:** lo exige el servidor (`require_secure_transport`) y cada usuario (`REQUIRE SSL`). La URL JDBC lleva `sslMode=REQUIRED`.

### Spring: qué perfil usa cada ambiente

| Ambiente | Dónde corre la API | Perfil de Spring | Base |
| --- | --- | --- | --- |
| Local | Tu computador | `dev` | MySQL del `docker-compose.yml` |
| Desarrollo | AWS (KAN-11) | `prod` | `cocina_desarrollo` |
| Producción | AWS (KAN-11) | `prod` | `cocina_produccion` |

**Los dos ambientes de la nube usan el perfil `prod`.** El perfil `dev` activa el encabezado `X-Dev-User`, que permite entrar como cualquier usuario sin Firebase: en una URL pública sería una puerta abierta. Lo único que cambia entre desarrollo y producción son los valores de las variables.

## Costo

La cuenta usa el **plan gratuito de AWS**: USD 100 de crédito al registrarse, más hasta USD 100 por completar actividades (crear una base RDS, crear un presupuesto, entre otras), durante 6 meses. **En el plan gratuito AWS no cobra nada a la tarjeta**; si el crédito o el plazo se acaban, la cuenta se cierra sola.

| Concepto | Costo aproximado, descontado del crédito |
| --- | --- |
| Instancia `db.t4g.micro` | USD 12 al mes |
| 20 GB gp3 | USD 2,3 al mes |
| Dirección IPv4 pública | USD 3,7 al mes |
| Respaldos (hasta el tamaño del disco) | Gratis |
| **Total de la base** | **Unos USD 18 al mes** |

- **No** actives Multi-AZ, réplicas de lectura ni Performance Insights pagado: cada uno consume crédito aparte.
- Revisa el saldo cada semana en **Facturación y administración de costos → Créditos**.

## Crear la base de datos (una sola vez)

Lo hace Juan.

### 1. Crear la cuenta de AWS

1. Entra a [aws.amazon.com](https://aws.amazon.com) → **Crear una cuenta de AWS**.
2. Correo: tu Gmail personal. Nombre de la cuenta: `cocina-sin-estres`. Te llega un código de verificación al correo.
3. Contraseña del **usuario raíz**: larga y en tu gestor de contraseñas.
4. Información de contacto: **Personal**, con tus datos reales tal como aparecen en el banco.
5. Tarjeta de débito o crédito: AWS hace una **retención temporal de alrededor de USD 1** para verificarla y luego la libera. Si el banco pide aprobar una compra internacional, apruébala.
6. Verificación de identidad por SMS o llamada.
7. **Elige el "Plan gratuito"** (*Free plan*), no el de pago. Plan de soporte: **Básico** (gratis).

La activación puede tardar desde unos minutos hasta algunas horas; llega un correo cuando la cuenta está lista.

### 2. Proteger la cuenta y elegir la región

1. Arriba a la derecha, en tu nombre → **Credenciales de seguridad** → **Asignar dispositivo MFA** (con una app como Google Authenticator). La cuenta raíz sin MFA es el riesgo más grande.
2. Arriba a la derecha, selector de región: **EE. UU. Este (Norte de Virginia) us-east-1**. Todo el proyecto vive ahí.

### 3. Crear un presupuesto con alertas

**Facturación y administración de costos → Presupuestos → Crear presupuesto → Usar una plantilla → Presupuesto de costo mensual**, USD 20, con tu correo. Además de avisarte si el gasto se dispara, es una de las actividades que suman USD 20 de crédito.

### 4. Ejecutar el script en CloudShell

1. En la consola de AWS, con la región `us-east-1`, abre **CloudShell** (ícono `>_` en la barra de arriba).
2. Ejecuta:
   ```bash
   git clone -b chore/KAN-9-base-datos-nube https://github.com/JCamiloLopezF/cocina-sin-estres.git
   cd cocina-sin-estres
   bash infra/aws/crear-base-datos.sh
   ```
   (Cuando KAN-9 esté unido a `develop`, el `-b chore/...` ya no hace falta.)
3. El script:
   - te pide confirmar y elegir la **contraseña del administrador** (entre 12 y 41 caracteres, sin `/ " ' @` ni espacios; guárdala en tu gestor);
   - crea el grupo de seguridad, el grupo de parámetros y la instancia (entre 10 y 20 minutos);
   - abre el puerto un momento para CloudShell, crea las dos bases y los dos usuarios, y lo vuelve a cerrar.
4. Al final muestra, **una sola vez**, `DB_URL`, `DB_USUARIO` y `DB_CONTRASENA` de cada ambiente, y el id del grupo de seguridad:
   - Guárdalos en tu gestor de contraseñas.
   - En GitHub, crea los ambientes **Settings → Environments → New environment**: `desarrollo` y `produccion`. En cada uno agrega los tres secretos con los valores de su ambiente. Los usará el despliegue de KAN-11.
   - Borra la pantalla con `clear`.
5. Revisa que todo quedó bien:
   ```bash
   bash infra/aws/verificar-base-datos.sh
   ```

El script se puede volver a ejecutar sin romper nada: no recrea la instancia ni borra datos. **Sí cambia las contraseñas de `app_desarrollo` y `app_produccion`**, así que sirve para rotarlas; después hay que actualizar los secretos de GitHub.

## Conectarte desde tu computador

Hace falta para aplicar las migraciones antes de que exista el despliegue (KAN-11) o para revisar los datos con MySQL Workbench o DBeaver.

1. Averigua tu IP pública (por ejemplo en [checkip.amazonaws.com](https://checkip.amazonaws.com)) y ábrele el puerto desde CloudShell:
   ```bash
   SG=$(aws rds describe-db-instances --db-instance-identifier cocina-sin-estres-mysql \
     --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text)
   aws ec2 authorize-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 --cidr TU_IP/32
   ```
2. Aplica las migraciones a `cocina_desarrollo` arrancando la API en tu computador contra esa base:
   ```bash
   cd backend
   export DB_URL='LA_DB_URL_DE_DESARROLLO'
   export DB_USUARIO=app_desarrollo
   read -r -s -p "Contraseña: " DB_CONTRASENA; export DB_CONTRASENA; echo
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```
   En el log debe aparecer `Successfully applied N migrations to schema cocina_desarrollo`. Así la contraseña no queda en el historial de la terminal. Aquí sí se usa el perfil `dev`, porque la API corre solo en tu computador.
3. Cuando termines, **cierra el puerto**:
   ```bash
   aws ec2 revoke-security-group-ingress --group-id "$SG" --protocol tcp --port 3306 --cidr TU_IP/32
   ```
4. **No hagas esto contra `cocina_produccion`.** Producción solo recibe migraciones desde el despliegue (KAN-11), cuando el código ya pasó por `main`.

Para MySQL Workbench o DBeaver: host = el de la `DB_URL`, puerto 3306, SSL en *Require*, usuario `app_desarrollo`. Nunca cambies el esquema a mano: los cambios van como migración nueva de Flyway (ADR-07).

## Respaldo diario (RNF-20)

RDS hace los respaldos solo; no hay que programar nada:

- **Un respaldo automático completo al día**, entre las 08:00 y las 08:30 UTC (3:00 a. m. en Colombia).
- **Retención de 1 día**: es el máximo que permite el plan gratuito de AWS. Se conserva el respaldo más reciente y el registro de transacciones, así que se puede **restaurar a cualquier minuto del último día**.
- Para tener historial, Juan saca una **instantánea manual** cada semana (**RDS → Bases de datos → cocina-sin-estres-mysql → Acciones → Tomar instantánea**, nombre `semanal-AAAA-MM-DD`) y borra la de hace dos semanas. Ocupan poco y quedan como evidencia.
- Los respaldos son de la instancia completa, así que cubren las dos bases.

### Evidencia para la sustentación

1. `bash infra/aws/verificar-base-datos.sh` muestra la retención (`RetencionRespaldoDias: 1`), hasta qué minuto se puede restaurar y el respaldo automático más reciente con su fecha, que cambia cada día.
2. En la consola, **RDS → Bases de datos → cocina-sin-estres-mysql → Mantenimiento y copias de seguridad** muestra lo mismo. Toma una captura cada semana y súbela a KAN-9 en Jira.

### Prueba de restauración (una vez, antes de la sustentación)

Comprobar que el respaldo sirve es mejor evidencia que mostrar que existe.

1. En la consola: **RDS → Bases de datos → cocina-sin-estres-mysql → Acciones → Restaurar a un momento dado**. Nombre de la instancia nueva: `cocina-sin-estres-prueba`, clase `db.t4g.micro`, mismo grupo de seguridad (la restauración siempre crea una instancia nueva y deja intacta la original).
2. Cuando esté disponible, ábrele el puerto a tu IP y comprueba con MySQL Workbench que `cocina_produccion` tiene sus tablas y datos.
3. Toma la captura y **borra la instancia de prueba en cuanto termines** (sin instantánea final): consume crédito por hora.

## Problemas comunes

| Síntoma | Causa y solución |
| --- | --- |
| El registro de la cuenta se queda esperando | La activación puede tardar horas. Espera el correo de AWS antes de crear recursos. |
| `FreeTierRestrictionError` por la retención de respaldos | El plan gratuito permite máximo 1 día, que es el valor por defecto del script. Si alguien lo cambió, ejecuta `RETENCION=1 bash infra/aws/crear-base-datos.sh`. |
| Error porque la clase `db.t4g.micro` no está disponible | Ejecuta `CLASE=db.t3.micro bash infra/aws/crear-base-datos.sh`. |
| `Access denied for user 'cocinaadmin'` | Contraseña del administrador equivocada. Si la olvidaste: **RDS → Modificar → Configuración de credenciales**. |
| La conexión desde tu computador se queda esperando | El puerto no está abierto para tu IP, o tu IP cambió: repite el paso 1 de *Conectarte desde tu computador*. |
| `Connections using insecure transport are prohibited` | Falta `sslMode=REQUIRED` en la URL JDBC. |
| Flyway falla por *checksum* | Alguien modificó una migración que ya estaba aplicada. Nunca se edita una migración unida a `develop`: se crea una nueva. |

## Al terminar el semestre

Para no consumir crédito, Juan borra la instancia: **RDS → Modificar → desmarcar "Habilitar la protección contra eliminación"**, y luego **Acciones → Eliminar**. Antes, si se quiere conservar una copia, se exportan los datos con `mysqldump` o se toma una instantánea manual (que sigue consumiendo almacenamiento mientras exista).
