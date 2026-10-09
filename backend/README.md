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

**Qué hace cada pieza.** Docker solo sirve para tener un MySQL en tu computador (`docker compose up -d mysql`); no ejecuta nuestro código ni las migraciones. Las migraciones las aplica **Flyway cuando arranca la API** (`bootRun`) o cuando corre la prueba de integración. Hacer `commit` y `push` solo sube los archivos: no toca ninguna base de datos. Para comprobar `V1` y `V2` en tu equipo basta con levantar MySQL y arrancar la API en `dev`; en el log aparece `Successfully applied 2 migrations`.

Si ya habías arrancado la API con una versión anterior de una migración y luego la cambias, Flyway falla por *checksum*. En tu base local se resuelve con `docker compose down -v` (borra los datos locales) y volviendo a arrancar.

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
| `GET /api/curaduria/productos` | Lista los productos activos del catálogo, ordenados por categoría y nombre. `CURADOR` o `ADMIN`. |
| `GET /api/curaduria/productos/{id}` | Devuelve un producto. 404 si no existe. |
| `POST /api/curaduria/productos` | Crea un producto del catálogo global. Responde 201 con `Location`. |
| `PUT /api/curaduria/productos/{id}` | Actualiza un producto. Si no cambia ningún campo, no se modifica el responsable ni se audita. |
| `DELETE /api/curaduria/productos/{id}` | Desactiva el producto (no lo borra). Responde 204; repetirlo no tiene efecto. |

Cada módulo agrega sus rutas; las convenciones están en [`CONTRIBUTING.md`](../CONTRIBUTING.md#api-rest).

## Curaduría: catálogo de productos con precios de referencia (KAN-17)

El catálogo es la lista de productos que usan la despensa, las recetas y el planificador para calcular costos. Cada producto del catálogo global (`producto.hogar_id` NULL) tiene un **precio de referencia completo**: una unidad de compra, cuánto trae esa unidad (en la unidad base del producto) y su precio en pesos (RNF-02).

**Datos de un producto**

| Campo | Qué es | Regla |
| --- | --- | --- |
| `nombre` | Nombre del producto | Obligatorio, hasta 120 caracteres |
| `categoria` | Categoría | Obligatoria, hasta 60 caracteres |
| `tipoCantidad` | `CONTABLE` o `GRANEL` | Obligatorio |
| `unidadBase` | `g`, `ml` o `unidad` | Obligatoria |
| `unidadCompra` | Presentación de compra, ej. `Bolsa` | Obligatoria, hasta 30 caracteres |
| `cantidadUnidadCompra` | Cuánto trae la unidad de compra, en unidad base | Obligatoria, mayor que cero, hasta 3 decimales |
| `precioUnidadCompra` | Precio de referencia de la unidad de compra, en pesos | Obligatorio, no negativo, hasta 2 decimales |
| `capacidadReferencia` | Cantidad que equivale a "Lleno" | Obligatoria y mayor que cero en `GRANEL`; no se permite en `CONTABLE` |
| `esBasico` | Si es un producto básico de la despensa | Opcional (por defecto `false`) |

```bash
# dev: crear un producto (la cuenta debe ser CURADOR o ADMIN)
curl -X POST http://localhost:8080/api/curaduria/productos \
  -H 'X-Dev-User: uid-curadora' -H 'Content-Type: application/json' \
  -d '{
    "nombre": "Harina de trigo",
    "categoria": "GRANOS",
    "tipoCantidad": "GRANEL",
    "unidadBase": "g",
    "unidadCompra": "Bolsa",
    "cantidadUnidadCompra": 1000,
    "precioUnidadCompra": 4200,
    "capacidadReferencia": 1000,
    "esBasico": false
  }'
```

**Tener una cuenta con permiso en `dev`.** Toda cuenta nueva entra como `USUARIA`. Para probar la curaduría, arranca la API con el correo de la cuenta simulada como administrador inicial y haz la primera petición con ese `X-Dev-User`:

```bash
ADMIN_INICIAL_CORREO=uid-curadora@dev.local ./gradlew bootRun --args='--spring.profiles.active=dev'
```

Solo funciona si esa cuenta aún no existe: el rol se asigna al crearla.

**Auditoría (RNF-24, ADR-23).** Crear, editar y desactivar escriben un registro en `auditoria_catalogo` dentro de la misma transacción: quién, cuándo, qué acción y los campos que cambiaron (`{"campo": {"antes": ..., "despues": ...}}`). Si falla la auditoría, tampoco se guarda el cambio. Por ahora la auditoría solo se escribe; no hay endpoint para consultarla.

**Catálogo inicial (`V2`).** Carga 21 productos con su precio de referencia, básicos de la despensa colombiana (granos, aceites, lácteos, proteínas, verduras y frutas). Los registra un usuario de sistema (`sistema-carga-inicial`, rol `CURADOR`, desactivado, que no puede iniciar sesión) y deja un registro `CREAR` por producto en la auditoría. Los precios son una referencia aproximada que el curador ajusta con `PUT`.

**Errores de validación.** Responden 400 con estos códigos en `codigo`:

| Código | Cuándo |
| --- | --- |
| `PRODUCTO_NOMBRE_REQUERIDO`, `PRODUCTO_NOMBRE_LARGO` | Nombre vacío o de más de 120 caracteres |
| `PRODUCTO_CATEGORIA_REQUERIDA`, `PRODUCTO_CATEGORIA_LARGA` | Categoría vacía o de más de 60 caracteres |
| `PRODUCTO_TIPO_CANTIDAD_REQUERIDO`, `PRODUCTO_UNIDAD_BASE_REQUERIDA` | Falta el tipo de cantidad o la unidad base |
| `PRODUCTO_UNIDAD_COMPRA_REQUERIDA`, `PRODUCTO_UNIDAD_COMPRA_LARGA` | Unidad de compra vacía o de más de 30 caracteres |
| `PRODUCTO_CANTIDAD_COMPRA_REQUERIDA`, `PRODUCTO_CANTIDAD_COMPRA_INVALIDA` | Falta, es cero o negativa, o excede 3 decimales |
| `PRODUCTO_PRECIO_REQUERIDO`, `PRODUCTO_PRECIO_INVALIDO` | Falta, es negativo o excede 2 decimales |
| `PRODUCTO_CAPACIDAD_REQUERIDA` | Producto a granel sin capacidad de referencia válida |
| `PRODUCTO_CAPACIDAD_NO_PERMITIDA` | Producto contable con capacidad de referencia |

Un producto que no existe responde 404 con `RECURSO_NO_ENCONTRADO`.

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
├── perfil         ← entidad JPA: Usuario
├── despensa
├── recetario
├── planificador
├── avisos
├── curaduria      ← entidades JPA: Producto, AuditoriaCatalogo
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
| `curaduria` | Catálogo de productos con precios de referencia, su edición y auditoría (rol CURADOR) | `perfil` (cuenta autenticada) |
| `admin` | Gestión de cuentas y roles (rol ADMIN) | `perfil` |
| `comun` | Seguridad, configuración, manejo de errores | `perfil` (para resolver la usuaria autenticada) |

Un módulo no usa las entidades ni los repositorios de otro: llama a su servicio. `domain/` va sin Spring y sin JPA (ADR-09).

**Entidades JPA:** `perfil/model/Usuario`, `curaduria/model/Producto` y `curaduria/model/AuditoriaCatalogo`. Las otras 14 tablas de `V1` las mapea cada módulo cuando le toque. Hoy la entidad `Producto` vive en `curaduria`; los demás módulos no la usan directamente: cuando necesiten productos se los piden a un servicio.

## Migraciones

En `src/main/resources/db/migration/`:

| Migración | Contenido |
| --- | --- |
| `V1__esquema_inicial.sql` | Las 17 tablas, con sus llaves y restricciones (KAN-12) |
| `V2__catalogo_inicial.sql` | Usuario de sistema, 21 productos globales con precio de referencia y su auditoría (KAN-17) |

Flyway las aplica al arrancar, en orden y una sola vez, y Hibernate corre con `ddl-auto=validate`: si una entidad no concuerda con su tabla, la aplicación no arranca. Reglas en [`CONTRIBUTING.md`](../CONTRIBUTING.md#6-migraciones-de-base-de-datos-flyway).

## Pruebas

```bash
./gradlew test
```

- **Dominio** (`perfil/domain/RolTest`): JUnit puro, sin contexto de Spring.
- **Servicios** (`perfil/service/UsuarioActualServiceTest`): dobles de Mockito para los repositorios, sin Spring ni base de datos. Cubre el alta perezosa, el ADMIN inicial, la cuenta desactivada y dos peticiones simultáneas del mismo `firebase_uid`.
- **Integración** (`comun/seguridad/SeguridadIntegracionTest`): levanta la aplicación con el perfil `dev` contra un MySQL 8.4 en Docker (Testcontainers). Verifica 401 sin credenciales, 200 con `X-Dev-User`, 403 en `/api/admin` y `/api/curaduria` con rol `USUARIA`, y que la ruta de salud sea pública. **Necesita Docker en ejecución**; el solo arranque del contexto comprueba además que Flyway aplica `V1` y que `validate` no encuentra diferencias.
