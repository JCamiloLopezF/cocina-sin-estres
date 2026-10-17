# Despliegue de la aplicación (KAN-11)

Cómo queda publicada **Cocina sin estrés** en internet y cómo se despliega cada versión. La base de datos está en [base-de-datos.md](base-de-datos.md) (KAN-9) y la decisión de proveedor en el ADR-22.

## Cómo queda

```
Navegador ──HTTPS──► Firebase Hosting  (https://cocina-sin-estres-585ce.web.app)
    │                 la SPA de web-app/
    │
    └──HTTPS──► CloudFront ──HTTP──► Elastic Beanstalk ──TLS──► RDS MySQL
                (certificado)        API Spring Boot, perfil prod   cocina_produccion
                                     1 instancia t4g.micro
```

| Pieza | Servicio | Qué hace |
| --- | --- | --- |
| Frontend | Firebase Hosting (plan Spark) | Sirve la SPA con HTTPS y redirige cualquier ruta a `index.html` |
| HTTPS de la API | Amazon CloudFront | Certificado gratis de `*.cloudfront.net`. No guarda nada en caché |
| API | AWS Elastic Beanstalk, Corretto 21 | Una sola instancia `t4g.micro` con nginx delante del jar |
| Secretos | AWS Systems Manager Parameter Store | Contraseña de la base, clave de cifrado (ADR-20) y cuenta de servicio de Firebase, cifradas |
| Base | Amazon RDS (KAN-9) | `cocina_produccion`. Flyway aplica las migraciones al arrancar la API |
| Despliegue | GitHub Actions (`despliegue.yml`) | Cada merge a `main` publica backend y frontend |

**Los secretos nunca están en el repositorio ni en la configuración visible de Elastic Beanstalk.** En cada despliegue, el hook `backend/eb/.platform/hooks/prebuild/01_secretos.sh` los baja de Parameter Store a `/etc/cocina/`, que solo lee la API. GitHub Actions tampoco guarda llaves de AWS: asume un rol con OIDC que solo sirve para el ambiente `produccion` de este repositorio.

## Costo

Con los créditos del plan gratuito de AWS no se paga nada. Sin créditos, la cuenta completa queda así:

| Recurso | USD al mes, aproximado |
| --- | --- |
| RDS (KAN-9) | 18 |
| Instancia `t4g.micro` | 6 |
| IP pública de la instancia | 3,6 |
| CloudFront, Parameter Store, registros (7 días) | 0–1 |
| **Total** | **≈ 28** |

El total queda por encima del presupuesto de USD 25 que pusimos en KAN-9, así que la alerta va a llegar. Súbela a USD 30 en **Billing → Budgets**. Firebase Hosting y Authentication en el plan Spark no cuestan nada.

## Configuración inicial (una sola vez)

Lo hace Juan, que es el dueño de las cuentas de AWS y Firebase.

### 1. Cuenta de servicio de Firebase

1. En la consola de Firebase ve a **Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada**. Se descarga un `.json`.
2. **No lo subas al repositorio ni lo pegues en ningún chat.** Es la llave que permite verificar tokens en nombre del proyecto.

### 2. Infraestructura en AWS (CloudShell)

1. Abre CloudShell (ícono `>_` en la consola de AWS, región **us-east-1**).
2. Sube el `.json` con **Acciones → Cargar archivo**. Queda en tu carpeta de inicio.
3. Ejecuta:
   ```bash
   cd ~/cocina-sin-estres 2>/dev/null || git clone https://github.com/JCamiloLopezF/cocina-sin-estres.git ~/cocina-sin-estres
   cd ~/cocina-sin-estres
   git fetch origin
   git checkout develop
   git pull
   bash infra/aws/crear-api.sh ~/NOMBRE-DEL-ARCHIVO.json
   ```
4. Te va a pedir dos cosas:
   - el correo del primer ADMIN (ADR-16);
   - la contraseña de `app_produccion`, la que dio `crear-base-datos.sh` y guardaste en tu gestor.

El script tarda unos 10 minutos. Guarda el `.json` cifrado en Parameter Store, **lo borra de CloudShell** y genera la clave de cifrado de producción. Esa clave no se ve nunca y no hace falta que la guardes: si se perdiera, los datos de salud ya guardados quedarían ilegibles, por eso el script nunca la reemplaza. Al final imprime las variables del siguiente paso.

Se puede volver a ejecutar sin romper nada. Para cambiar la contraseña de la base: `CAMBIAR_CONTRASENA_DB=si bash infra/aws/crear-api.sh`.

### 3. Ambiente `produccion` en GitHub

En el repositorio, ve a **Settings → Environments → produccion** (créalo si no existe). En **Environment variables** agrega:

| Variable | Valor |
| --- | --- |
| `AWS_REGION` | lo imprime el script (`us-east-1`) |
| `AWS_ROL_DESPLIEGUE` | lo imprime el script |
| `EB_APLICACION` | lo imprime el script |
| `EB_ENTORNO` | lo imprime el script |
| `EB_BUCKET` | lo imprime el script |
| `API_URL` | lo imprime el script (`https://…cloudfront.net/api`) |
| `VITE_FIREBASE_API_KEY` | `apiKey` del `firebaseConfig` |
| `VITE_FIREBASE_AUTH_DOMAIN` | `authDomain` del `firebaseConfig` |
| `VITE_FIREBASE_PROJECT_ID` | `projectId` del `firebaseConfig` |
| `VITE_FIREBASE_APP_ID` | `appId` del `firebaseConfig` |

Ninguna es secreta: la configuración web de Firebase termina en el navegador de cualquier usuaria. Son variables y no secretos para que se puedan leer en los logs cuando algo falla.

Los secretos de base de datos que creaste en KAN-9 (`DB_URL`, `DB_USUARIO`, `DB_CONTRASENA`) no los usa este workflow; la API los lee de Parameter Store. Puedes dejarlos.

### 4. Permiso para publicar en Firebase Hosting

En tu Mac, desde `web-app/`:

```bash
npx firebase-tools@latest login
npx firebase-tools@latest init hosting:github
```

El comando pide:
- **El repositorio:** escribe `JCamiloLopezF/cocina-sin-estres`.
- **Si quieres un build antes de cada despliegue:** responde **No**.
- **Si quieres desplegar al unir un PR:** responde **No**.

Con eso crea en el proyecto de Firebase una cuenta de servicio que solo puede publicar en Hosting y guarda su llave en GitHub como el secreto `FIREBASE_SERVICE_ACCOUNT_COCINA_SIN_ESTRES_585CE`. El comando también deja archivos `.github/workflows/firebase-hosting-*.yml`: **bórralos sin hacer commit**, porque el despliegue ya lo hace `despliegue.yml`.

## Desplegar

Cada merge a `main` despliega solo. Para el prototipo:

1. Abre un PR de `develop` a `main`, con título `chore(ci): KAN-51 prototipo 1`, y únelo cuando el CI esté en verde.
2. En **Actions → Despliegue** sigue el avance:
   - **desplegar-backend:** compila el jar, lo sube a Elastic Beanstalk, espera a que la versión quede activa y llama a `/api/salud`. La primera vez Flyway aplica V1 a V3 en `cocina_produccion`.
   - **desplegar-frontend:** compila la SPA con las variables del ambiente y la publica en Firebase Hosting.

También se puede lanzar a mano con **Run workflow**, sobre `main`.

### Comprobar que quedó bien

1. `curl https://…cloudfront.net/api/salud` responde `{"estado":"ok",…}`.
2. `https://cocina-sin-estres-585ce.web.app` abre la pantalla de entrar.
3. Crea una cuenta, cierra sesión, recupera la contraseña y vuelve a entrar. El correo debe llegar en español.
4. En CloudShell, `bash infra/aws/verificar-base-datos.sh` muestra V1, V2 y V3 aplicadas también en `cocina_produccion`.
5. La primera vez que entres con el correo del ADMIN, esa cuenta queda como `ADMIN` y la app te lleva a `/admin`.

## Problemas comunes

| Síntoma | Causa probable | Qué hacer |
| --- | --- | --- |
| La API responde 502 o el despliegue vuelve a la versión anterior | La API no arrancó | Revisa los registros en CloudWatch, grupo `/aws/elasticbeanstalk/cocina-sin-estres-api/var/log/web.stdout.log`. Si el error es "Faltan parámetros en SSM", vuelve a ejecutar `crear-api.sh` |
| `Schema-validation` en los registros | Una entidad no coincide con las migraciones | Lo atrapa el CI del PR. Corrígelo con una migración nueva |
| Error de CORS en el navegador | El origen no está en `CORS_ORIGENES` | Lo pone `crear-api.sh` con los dominios `web.app` y `firebaseapp.com` del proyecto. Si cambias de dominio, vuelve a ejecutarlo |
| "Tu sesión expiró" al entrar | La API no pudo verificar el token | Revisa que la cuenta de servicio sea del mismo proyecto que `VITE_FIREBASE_PROJECT_ID` |
| `Not authorized to perform sts:AssumeRoleWithWebIdentity` | El job no corrió en el ambiente `produccion` o el repositorio cambió de nombre | Ejecuta `crear-api.sh` con `REPO_GITHUB=dueño/repo` |
| El paso de Firebase Hosting falla por permisos | Falta el secreto del paso 4 | Repite `init hosting:github` |

## Límites conocidos (prototipo)

- **Entre CloudFront y Elastic Beanstalk el tráfico va por HTTP**, dentro de la red de AWS. El navegador siempre habla HTTPS. Cerrarlo del todo requiere un balanceador con certificado propio, que cuesta unos USD 16 al mes más.
- **El puerto 80 de la instancia está abierto**, porque así lo deja Elastic Beanstalk. Una mejora pendiente es aceptar solo el rango de CloudFront (lista de prefijos `com.amazonaws.global.cloudfront.origin-facing`).
- **Una sola instancia, sin balanceador.** Mientras se despliega una versión, la API deja de responder entre 1 y 2 minutos. Para el 99 % mensual de RNF-16 alcanza si se despliega pocas veces al día.
