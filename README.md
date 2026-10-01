# Cocina sin estrés

Aplicación web para que mujeres cabeza de hogar organicen su despensa, planeen las comidas de la semana según su presupuesto y reciban avisos oportunos.

Proyecto del curso **Ingeniería de Software III**, Programa de Ingeniería de Sistemas y Computación, Universidad del Quindío.

| Integrante | Responsabilidad principal |
| --- | --- |
| Juan Camilo López Fuentes | Arquitectura, despliegue y seguridad |
| Luisa Fernanda Osorio Giraldo | Perfil, planificador, catálogo y recetario |
| Dillan Snayder Buitrago Tangarife | Despensa, recetario y pruebas · Administra Jira |

## Estructura del repositorio

```
cocina-sin-estres/
├── web-app/      Aplicación web (React + Vite)
├── backend/      API REST (Java + Spring Boot + Gradle)
├── docs/         Arquitectura C4, ADRs y documentos del curso
├── scripts/      Utilidades del equipo (configuración del repositorio)
└── .github/      Plantilla de PR y flujos de GitHub Actions
```

Cada módulo tiene su propio `README.md` con instrucciones para ejecutarlo.

## Stack

- **Frontend:** React (SPA) con Vite, pensada para navegador móvil de gama baja.
- **Backend:** Java con Spring Boot, API REST, Spring Security, Spring Data JPA.
- **Base de datos:** MySQL (InnoDB, utf8mb4) con migraciones versionadas en Flyway.
- **Identidad:** Firebase Authentication; el backend verifica el token en cada petición.
- **Nube:** por definir (ADR-22).

## Cómo contribuir

Lee [`CONTRIBUTING.md`](CONTRIBUTING.md) antes de tu primer cambio. Resumen:

1. Crea tu rama desde `develop`: `feature/KAN-15-filtro-por-vencer`.
2. Haz commits con el formato `feat(despensa): KAN-15 agregar filtro por vencer`.
3. Abre un PR hacia `develop`. Necesita la aprobación de otro integrante y los checks en verde.

`main` y `develop` están protegidas: nadie puede hacer push directo.

## Gestión del proyecto

Las tareas viven en Jira con la clave **KAN**. Usar la clave en ramas, commits y PRs las vincula automáticamente con la tarea.
