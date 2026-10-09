# Guía de contribución

Reglas de trabajo del equipo de **Cocina sin estrés**. Si algo de aquí no te funciona, propón el cambio en un PR a este archivo; no lo ignores en silencio.

## Índice

1. [Flujo de trabajo](#1-flujo-de-trabajo)
2. [Ramas](#2-ramas)
3. [Commits](#3-commits)
4. [Pull requests y Definición de Terminado](#4-pull-requests-y-definición-de-terminado)
5. [Convenciones de nombres](#5-convenciones-de-nombres)
6. [Migraciones de base de datos (Flyway)](#6-migraciones-de-base-de-datos-flyway)
7. [Manejo de errores de la API](#7-manejo-de-errores-de-la-api)
8. [Secretos y datos sensibles](#8-secretos-y-datos-sensibles)
9. [Pruebas](#9-pruebas)
10. [Documentación](#10-documentación)

---

## 1. Flujo de trabajo

```bash
# 1. Parte siempre de develop actualizado
git switch develop
git pull

# 2. Crea tu rama con la clave de Jira
git switch -c feature/KAN-15-filtro-por-vencer

# 3. Trabaja y haz commits pequeños
git add .
git commit -m "feat(despensa): KAN-15 agregar filtro por vencer"

# 4. Antes de abrir el PR, trae lo último de develop
git fetch origin
git rebase origin/develop

# 5. Sube la rama y abre el PR hacia develop
git push -u origin feature/KAN-15-filtro-por-vencer
```

Mueve la tarea en Jira a *En curso* cuando creas la rama y a *En revisión* cuando abras el PR.

## 2. Ramas

| Rama | Para qué | Quién escribe |
| --- | --- | --- |
| `main` | Producción. Lo que está aquí se despliega. | Solo PRs desde `develop` o desde una rama `fix/` urgente |
| `develop` | Integración. Aquí llega todo el trabajo terminado. | Solo PRs |
| `feature/KAN-xx-descripcion` | Funcionalidad nueva | Su autor |
| `fix/KAN-xx-descripcion` | Corrección de un error | Su autor |
| `docs/KAN-xx-descripcion` | Solo documentación | Su autor |
| `chore/KAN-xx-descripcion` | Configuración, dependencias, CI | Su autor |

Reglas:

- La descripción va en minúsculas, sin tildes y separada por guiones: `feature/KAN-14-perfil-del-hogar`.
- Una rama por tarea de Jira. Si una tarea es muy grande, se divide en Jira, no en ramas sin clave.
- Las ramas se borran solas al hacer merge del PR.
- `main` y `develop` están protegidas: no aceptan push directo, force push ni borrado, ni siquiera del dueño del repositorio.

## 3. Commits

Formato (basado en [Conventional Commits](https://www.conventionalcommits.org/es/)):

```
tipo(módulo): KAN-xx descripción en presente
```

| Tipo | Cuándo |
| --- | --- |
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de un error |
| `test` | Pruebas nuevas o corregidas |
| `docs` | Solo documentación |
| `refactor` | Cambio de código sin cambiar comportamiento |
| `chore` | Configuración, dependencias, CI, scripts |

Módulos: `auth`, `perfil`, `despensa`, `planificador`, `recetario`, `avisos`, `admin`, `curaduria`, `web`, `api`, `db`, `ci`, `docs`.

Ejemplos:

```
feat(despensa): KAN-15 agregar filtro por vencer
fix(auth): KAN-10 recuperar sesión al reabrir la app
test(planificador): KAN-22 casos de cruce con productos a granel
chore(ci): KAN-50 agregar análisis estático al pipeline
```

- Descripción en español, en presente, sin punto final y en menos de 72 caracteres.
- Commits pequeños: si necesitas "y" para describir el commit, probablemente son dos.

## 4. Pull requests y Definición de Terminado

**Título del PR:** mismo formato que los commits. Un check automático lo valida y bloquea el merge si no cumple.

```
feat(despensa): KAN-15 despensa virtual completa
```

El PR de `develop` hacia `main` usa el tipo `release`:

```
release: prototipo 1
```

**Reglas del PR:**

- Necesita **una aprobación de otro integrante**. La aprobación del autor no cuenta.
- Si subes cambios después de una aprobación, la aprobación se descarta y hay que pedir otra.
- Todas las conversaciones deben quedar resueltas antes del merge.
- La rama debe estar al día con `develop` (botón *Update branch* o `git rebase origin/develop`).
- Hacia `develop` se usa **Squash and merge**: cada tarea queda como un solo commit. De `develop` hacia `main` se usa **Create a merge commit**.

**Definición de Terminado.** Una tarea de Jira solo pasa a *Finalizado* si:

1. Tiene un PR aprobado por otro integrante y el pipeline en verde.
2. Incluye pruebas unitarias de la lógica nueva, sobre todo en `domain/` y en los servicios.
3. Cumple el criterio de aceptación del RNF relacionado.
4. Respeta esta guía y reutiliza componentes compartidos de la interfaz (RNF-30).
5. Está desplegada en el ambiente de desarrollo.
6. Deja evidencia en Jira: commits y PR vinculados por la clave KAN.

La plantilla del PR trae esta lista como checklist.

## 5. Convenciones de nombres

**Idioma:** el dominio del negocio va en español (clases, tablas, endpoints, variables). Los términos técnicos del framework se quedan en inglés (`Controller`, `Service`, `Repository`, `Request`, `Response`).

### Backend (Java)

Paquete raíz: `co.edu.uniquindio.cocinasinestres`. El código se organiza **por módulo de negocio** (RNF-26), y cada módulo tiene sus capas:

```
co.edu.uniquindio.cocinasinestres
├── despensa
│   ├── controller   DespensaController
│   ├── dto          ItemDespensaRequest, ItemDespensaResponse
│   ├── service      DespensaService
│   ├── domain       Lógica pura, sin Spring ni JPA (ADR-09)
│   ├── model        Entidades JPA (ItemDespensa)
│   └── repository   ItemDespensaRepository
├── perfil
├── planificador
├── recetario
├── avisos
├── curaduria
├── admin
└── comun            Seguridad, configuración, manejo de errores, utilidades
```

| Elemento | Convención | Ejemplo |
| --- | --- | --- |
| Clases | PascalCase, sustantivo singular | `ItemDespensa`, `CalculadoraCosto` |
| Controladores | `<Recurso>Controller` | `DespensaController` |
| Servicios | `<Recurso>Service` | `PlanificacionService` |
| Repositorios | `<Entidad>Repository` | `RecetaRepository` |
| DTOs | `record` con sufijo `Request` o `Response` | `CrearItemRequest` |
| Métodos y variables | camelCase, verbo para métodos | `calcularCostoSemanal()` |
| Constantes | MAYÚSCULAS_CON_GUIONES | `DIAS_AVISO_VENCIMIENTO` |
| Pruebas | `<Clase>Test` | `CruceDespensaTest` |

Un módulo no usa las entidades ni los repositorios de otro módulo directamente: llama a su servicio. Las dependencias entre módulos se documentan en el README del backend.

### API REST

- Prefijo `/api`. Recursos en plural y en kebab-case: `/api/items-despensa`, `/api/recetas/{id}`.
- Rutas de curaduría bajo `/api/curaduria/**` y de administración bajo `/api/admin/**` (ADR-16).
- JSON en camelCase: `{"fechaVencimiento": "2026-10-20"}`.
- Fechas en ISO 8601 (`2026-10-20`, `2026-10-20T14:30:00-05:00`). Dinero como número en pesos (`4200`, no `"$4.200"`).

### Base de datos

| Elemento | Convención | Ejemplo |
| --- | --- | --- |
| Tablas | snake_case, singular | `item_despensa`, `ingrediente_receta` |
| Columnas | snake_case | `fecha_vencimiento`, `firebase_uid` |
| Clave primaria | `id` (`BIGINT AUTO_INCREMENT`) | `id` |
| Claves foráneas | `<tabla>_id` | `hogar_id` |
| Índices | `idx_<tabla>_<columnas>` | `idx_item_despensa_hogar_id` |
| Únicos | `uk_<tabla>_<columnas>` | `uk_usuario_firebase_uid` |
| Llaves foráneas (restricción) | `fk_<tabla>_<tabla_referida>` | `fk_item_despensa_hogar` |

### Frontend (React)

```
web-app/src/
├── features/<modulo>/   Pantallas, componentes y hooks de cada módulo
├── shared/              Componentes reutilizables (RNF-30), utilidades
├── api/                 Cliente HTTP con el token de Firebase
└── config/              Firebase y variables de entorno
```

| Elemento | Convención | Ejemplo |
| --- | --- | --- |
| Componentes | PascalCase, un componente por archivo | `TarjetaReceta.tsx` |
| Hooks | `use` + PascalCase | `useDespensa.ts` |
| Utilidades | camelCase | `formatearPesos.ts` |
| Pruebas | junto al archivo, `.test.tsx` | `TarjetaReceta.test.tsx` |

Antes de crear un componente, revisa `shared/`. Si un componente se repite en dos módulos, se mueve a `shared/`.

Módulos permitidos (el check `convenciones-pr` rechaza cualquier otro). Se escriben en minúsculas y sin tildes:

| Módulo | Cuándo usarlo |
| --- | --- |
| `auth` | Registro, inicio de sesión, recuperación de contraseña, tokens de Firebase y roles |
| `perfil` | Perfil del hogar: personas, presupuesto, restricciones y onboarding |
| `despensa` | Despensa virtual: productos del hogar, cantidades, vencimientos y mínimos |
| `planificador` | Plan semanal, cruce con la despensa, costo, modo rescate y lista de compras |
| `recetario` | Catálogo de recetas: listar, buscar, filtrar y ver el detalle |
| `avisos` | Motor de reglas y centro de avisos |
| `admin` | Gestión de cuentas y roles (rol ADMIN) |
| `curaduria` | Edición del catálogo de productos y recetas (rol CURADOR) |
| `web` | Frontend en general: estructura, navegación y componentes compartidos |
| `api` | Backend en general: configuración, seguridad común y manejo de errores |
| `db` | Base de datos: migraciones Flyway y carga de datos |
| `ci` | GitHub Actions, el pipeline y la configuración del repositorio |
| `docs` | Solo documentación |

Si un cambio toca varios módulos, usa el principal: el esqueleto del backend va con `api` y el del frontend con `web`.

## 6. Migraciones de base de datos (Flyway)

El esquema **solo** cambia con migraciones (ADR-07). Nadie modifica tablas a mano en ningún ambiente.

- Ubicación: `backend/src/main/resources/db/migration/`.
- Nombre: `V<n>__<descripcion_en_snake_case>.sql`, con `n` secuencial: `V1__esquema_inicial.sql`, `V2__catalogo.sql`, `V3__agregar_ubicacion_item.sql`.
- **Una migración que ya se aplicó en cualquier ambiente compartido nunca se edita.** Si tiene un error, se corrige con una migración nueva.
- Si dos PRs abiertos usan el mismo número, **quien hace merge de último renumera la suya** antes del merge.
- Toda tabla nueva: `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.
- Una migración por cambio lógico. No mezcles cambios de esquema con carga de datos.
- Los datos de prueba no van en migraciones: van en un script aparte que solo se carga en el perfil `dev`.

## 7. Manejo de errores de la API

Todos los errores responden con el formato *Problem Details* (RFC 9457), que Spring Boot soporta con `ProblemDetail`. Un único `@RestControllerAdvice` en `comun` los construye; los controladores no arman respuestas de error a mano.

```json
{
  "type": "about:blank",
  "title": "Datos inválidos",
  "status": 400,
  "detail": "La cantidad debe ser mayor que cero.",
  "codigo": "DESPENSA_CANTIDAD_INVALIDA",
  "errores": [
    { "campo": "cantidad", "mensaje": "Debe ser mayor que cero." }
  ]
}
```

| Situación | Estado HTTP |
| --- | --- |
| Validación de datos de entrada | 400 |
| Token ausente o inválido | 401 |
| Rol sin permiso | 403 |
| Recurso inexistente **o de otro hogar** | 404 |
| Conflicto (duplicado, versión desactualizada) | 409 |
| Error inesperado | 500 |

- `codigo` es estable y en MAYÚSCULAS (`MODULO_MOTIVO`); el frontend decide el mensaje según el código.
- `detail` es un mensaje en español, entendible por la usuaria y en tono cordial (RNF-14).
- Pedir un recurso de otro hogar responde 404, no 403, para no confirmar que existe (RNF-22).
- Nunca se devuelve una traza de pila al cliente. El error 500 se registra en el log con un identificador que también va en la respuesta.

## 8. Secretos y datos sensibles

**El repositorio es público.** Todo lo que se suba lo puede ver cualquier persona, incluido el historial.

- Nunca se suben claves, contraseñas, tokens, cuentas de servicio de Firebase (`*-firebase-adminsdk-*.json`), archivos `.env` ni credenciales de la nube.
- Cada módulo tiene un `.env.example` con los nombres de las variables y sin valores. Al agregar una variable nueva, agrégala ahí.
- Si un secreto llega a subirse, **no basta con borrarlo en otro commit**: hay que revocarlo y generar uno nuevo de inmediato, y avisar al equipo.
- Nunca se escriben en logs contraseñas, tokens ni datos de salud (RNF-21, RNF-22).
- Los datos de prueba usan nombres y correos inventados.

## 9. Pruebas

- **Backend:** JUnit 5. Toda clase de `domain/` tiene pruebas unitarias sin Spring. Los servicios se prueban con dobles de los repositorios.
- **Frontend:** Vitest y Testing Library.
- Una prueba por comportamiento, con nombre descriptivo: `descuentaCantidadExactaDeProductoContable()`.
- Un error corregido llega con la prueba que lo habría detectado.

## 10. Documentación

- Cada módulo tiene un `README.md` que explica qué hace, cómo ejecutarlo y de qué otros módulos depende (RNF-27).
- Las funciones públicas de servicios y clases de dominio llevan Javadoc o JSDoc breve: qué hacen, no cómo.
- Las decisiones de arquitectura nuevas se registran como ADR en `docs/adr/`.
