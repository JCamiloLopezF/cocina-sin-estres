# Backend — API REST

API de **Cocina sin estrés** en Java con Spring Boot. Concentra la lógica de negocio, verifica el token de Firebase en cada petición y aplica la autorización por rol.

## Requisitos

- Java 21 (LTS)
- Docker (para MySQL local con Docker Compose)
- No hace falta instalar Gradle: el proyecto usa el wrapper (`./gradlew`).

Versiones: Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7, Hibernate 7.4), Flyway 12, MySQL 8.4, Firebase Admin SDK 9.9.

## Configuración

1. Copia `.env.example` a `.env` y completa los valores. El archivo `.env` nunca se sube.
2. Siempre hay que activar un perfil: `dev` o `prod`.

| Variable | Para qué | dev | prod |
| --- | --- | --- | --- |
| `DB_URL`, `DB_USUARIO`, `DB_CONTRASENA` | Conexión a MySQL | Opcional (toma los valores del `docker-compose.yml`) | Obligatoria |
| `PUERTO` | Puerto de la API | Opcional (8080) | Opcional (8080) |
| `LOG_NIVEL` | Nivel de log de la aplicación | Opcional (DEBUG) | Opcional (INFO) |
| `FIREBASE_CREDENCIALES_RUTA` | JSON de la cuenta de servicio, **fuera del repositorio** | Se ignora | **Obligatoria**: sin ella la aplicación no arranca |
| `CIFRADO_CLAVE_V1` | Clave AES-256 en Base64 para datos de salud (ADR-20) | Opcional mientras no haya datos cifrados | Obligatoria |
| `ADMIN_INICIAL_CORREO` | Correo que entra como ADMIN la primera vez (ADR-16) | Opcional | Recomendada |
| `CORS_ORIGENES` | Orígenes del navegador permitidos | Opcional (`http://localhost:5173`) | Obligatoria |

El repositorio es público: ningún valor real se escribe en el código ni en los `application*.yml` (CONTRIBUTING.md §8).

## Ejecución

```bash
# Base de datos local (desde la raíz del repositorio)
docker compose up -d mysql

# API en el perfil dev. Flyway aplica las migraciones al arrancar.
./gradlew bootRun --args='--spring.profiles.active=dev'

# Pruebas (la de integración necesita Docker en ejecución)
./gradlew test
```

## Autenticación

No hay endpoint de registro ni de inicio de sesión. El frontend entra con Firebase y manda el token en cada petición; el backend lo verifica en un filtro y busca o crea el `usuario` por `firebase_uid` (**alta perezosa**, ADR-05). La primera cuenta cuyo correo coincida con `ADMIN_INICIAL_CORREO` entra con rol `ADMIN`.

```bash
# prod: token de Firebase
curl -H 'Authorization: Bearer <idToken>' http://localhost:8080/api/yo
```

En el perfil `dev` no hace falta Firebase: un filtro alternativo acepta el encabezado `X-Dev-User` con el `firebase_uid` que quieras simular (ADR-19). El correo se inventa como `<uid>@dev.local`.

```bash
# dev: usuaria simulada
curl -H 'X-Dev-User: uid-mariana' http://localhost:8080/api/yo
```

> `FiltroUsuarioDev` es la única clase que lee ese encabezado y está marcada con `@Profile("dev")`: en `prod` el bean no se crea y el encabezado no significa nada.

## Autorización

Siempre en el backend (ADR-16). El frontend puede esconder botones, pero lo que decide es la cadena de seguridad.

| Ruta | Quién entra |
| --- | --- |
| `GET /api/salud` | Cualquiera, sin token |
| `/api/**` | Cuenta autenticada y activa |
| `/api/curaduria/**` | `CURADOR` o `ADMIN` |
| `/api/admin/**` | `ADMIN` |
| Todo lo demás | Nadie |

## Endpoints

| Método y ruta | Qué hace |
| --- | --- |
| `GET /api/salud` | Comprobación de salud. Público. Devuelve estado, versión y hora UTC. |
| `GET /api/yo` | Devuelve la cuenta autenticada: `id`, `correo`, `rol` y `perfilCompleto`. |

Cada módulo agrega sus rutas; las convenciones están en [`CONTRIBUTING.md`](../CONTRIBUTING.md#api-rest).

## Errores

Todos salen en formato *Problem Details* (RFC 9457) desde un único `@RestControllerAdvice` en `comun/error`, con el campo `codigo` estable que el frontend usa para decidir el mensaje. Los controladores no arman respuestas de error: lanzan una `ExcepcionNegocio`.

```json
{
  "status": 403,
  "title": "Sin permiso",
  "detail": "Tu cuenta no tiene permiso para esta sección.",
  "instance": "/api/admin/cuentas",
  "codigo": "AUTH_SIN_PERMISO"
}
```

Los códigos transversales están en `comun/error/CodigoError`; cada módulo declara los suyos. La tabla de situación y estado HTTP está en [`CONTRIBUTING.md`](../CONTRIBUTING.md#7-manejo-de-errores-de-la-api).

## Estructura

Organizado por módulo de negocio (RNF-26), paquete raíz `co.edu.uniquindio.cocinasinestres`. Cada módulo tiene las capas `controller`, `dto`, `service`, `domain`, `model` y `repository`, y cada paquete un `package-info.java` que dice qué va ahí.

```
co.edu.uniquindio.cocinasinestres
├── perfil         ← única entidad JPA por ahora: Usuario
├── despensa
├── recetario
├── planificador
├── avisos
├── curaduria
├── admin
└── comun
    ├── config     Configuración tipada y Firebase Admin SDK
    ├── seguridad  Filtros de autenticación, cadena de seguridad, roles
    ├── error      Problem Details y excepciones del negocio
    └── controller Endpoints transversales (salud)
```

| Módulo | Responsabilidad | Depende de |
| --- | --- | --- |
| `perfil` | Usuario local, hogar, personas y restricciones | `comun` |
| `despensa` | Productos del hogar, cantidades, vencimientos y mínimos | `perfil`, `recetario` (catálogo) |
| `recetario` | Catálogo de productos y recetas (lectura) | `comun` |
| `planificador` | Plan semanal, cruce con la despensa, costo, modo rescate y lista de compras | `perfil`, `despensa`, `recetario` |
| `avisos` | Motor de reglas y centro de avisos | `despensa`, `planificador` |
| `curaduria` | Edición del catálogo y auditoría (rol CURADOR) | `recetario` |
| `admin` | Gestión de cuentas y roles (rol ADMIN) | `perfil` |
| `comun` | Seguridad, configuración, manejo de errores | `perfil` (para resolver la usuaria autenticada) |

Un módulo no usa las entidades ni los repositorios de otro: llama a su servicio. `domain/` va sin Spring y sin JPA (ADR-09).

**Entidades JPA:** por ahora solo `perfil/model/Usuario`. Las otras 16 tablas de `V1` las mapea cada módulo cuando le toque.

## Migraciones

En `src/main/resources/db/migration/`. Flyway las aplica al arrancar, y Hibernate corre con `ddl-auto=validate`: si una entidad no concuerda con su tabla, la aplicación no arranca. Reglas en [`CONTRIBUTING.md`](../CONTRIBUTING.md#6-migraciones-de-base-de-datos-flyway).

## Pruebas

```bash
./gradlew test
```

- **Dominio** (`perfil/domain/RolTest`): JUnit puro, sin contexto de Spring.
- **Servicios** (`perfil/service/UsuarioActualServiceTest`): dobles de Mockito para los repositorios, sin Spring ni base de datos. Cubre el alta perezosa, el ADMIN inicial, la cuenta desactivada y dos peticiones simultáneas del mismo `firebase_uid`.
- **Integración** (`comun/seguridad/SeguridadIntegracionTest`): levanta la aplicación con el perfil `dev` contra un MySQL 8.4 en Docker (Testcontainers). Verifica 401 sin credenciales, 200 con `X-Dev-User`, 403 en `/api/admin` y `/api/curaduria` con rol `USUARIA`, y que la ruta de salud sea pública. **Necesita Docker en ejecución**; el solo arranque del contexto comprueba además que Flyway aplica `V1` y que `validate` no encuentra diferencias.
