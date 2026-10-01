# Backend — API REST

API de **Cocina sin estrés** en Java con Spring Boot. Concentra la lógica de negocio, verifica el token de Firebase en cada petición y aplica la autorización por rol.

> **Estado:** el esqueleto del proyecto llega con KAN-8. Este README se completa en ese PR.

## Requisitos

- Java 21 (LTS)
- Docker (para MySQL local con Docker Compose)
- No hace falta instalar Gradle: el proyecto usa el wrapper (`./gradlew`).

## Configuración

1. Copia `.env.example` a `.env` y completa los valores. El archivo `.env` nunca se sube.
2. Para trabajar sin Firebase, usa el perfil `dev`: acepta el encabezado `X-Dev-User` para simular un usuario (ADR-19). Ese modo no existe en producción.

## Ejecución

```bash
# Base de datos local
docker compose up -d mysql

# API en el perfil dev
./gradlew bootRun --args='--spring.profiles.active=dev'

# Pruebas
./gradlew test
```

## Módulos

Organizado por módulo de negocio (RNF-26). Cada módulo tiene las capas `controller`, `dto`, `service`, `domain`, `model` y `repository`.

| Módulo | Responsabilidad | Depende de |
| --- | --- | --- |
| `perfil` | Usuario local, hogar, personas y restricciones | `comun` |
| `despensa` | Productos del hogar, cantidades, vencimientos y mínimos | `perfil`, `recetario` (catálogo) |
| `recetario` | Catálogo de productos y recetas (lectura) | `comun` |
| `planificador` | Plan semanal, cruce con la despensa, costo, modo rescate y lista de compras | `perfil`, `despensa`, `recetario` |
| `avisos` | Motor de reglas y centro de avisos | `despensa`, `planificador` |
| `curaduria` | Edición del catálogo y auditoría (rol CURADOR) | `recetario` |
| `admin` | Gestión de cuentas y roles (rol ADMIN) | `perfil` |
| `comun` | Seguridad, configuración, manejo de errores | — |

## Migraciones

En `src/main/resources/db/migration/`. Reglas en [`CONTRIBUTING.md`](../CONTRIBUTING.md#6-migraciones-de-base-de-datos-flyway).
