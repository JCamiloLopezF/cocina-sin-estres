# Registro de decisiones de arquitectura (ADR)

Cada decisión de arquitectura queda registrada con su contexto, alternativas, justificación y consecuencias. Una decisión aprobada no se borra: si cambia, se escribe un ADR nuevo que la reemplaza y se marca la anterior como *Reemplazada*.

## Índice

| ADR | Decisión | Estado |
| --- | --- | --- |
| ADR-01 | Aplicación web responsive como canal de la usuaria | Aprobada |
| ADR-02 | Backend propio centralizado como API REST | Aprobada |
| ADR-03 | Backend en Java con Spring Boot | Aprobada |
| ADR-04 | MySQL como base de datos única | Aprobada |
| ADR-05 | Identidad delegada en Firebase Auth, verificada en el backend | Aprobada |
| ADR-06 | Autorización por rol en el backend | Ampliada por ADR-16 |
| ADR-07 | Acceso a datos con JPA y migraciones con Flyway | Aprobada |
| ADR-08 | Recetario propio, sin API externa de recetas | Aprobada |
| ADR-09 | Lógica de planificación como dominio aislado | Aprobada |
| ADR-10 | Avisos internos por motor de reglas | Precisada por ADR-17 |
| ADR-11 | Cantidad mixta en la despensa | Precisada por ADR-15 |
| ADR-12 | Despliegue en la nube desde el primer sprint | Aprobada |
| [ADR-13](ADR-13-campos-perfil-hogar.md) | Campos del perfil del hogar | Aprobada |
| [ADR-14](ADR-14-presupuesto-ciclo-ingreso.md) | Presupuesto por ciclo de ingreso | Aprobada |
| [ADR-15](ADR-15-niveles-granel.md) | Niveles de granel sobre una cantidad estimada | Aprobada |
| [ADR-16](ADR-16-tres-roles.md) | Tres roles con mínimo privilegio | Aprobada |
| [ADR-17](ADR-17-motor-avisos-backend.md) | Motor de avisos en el backend | Aprobada |
| [ADR-18](ADR-18-recetas-producto-catalogo.md) | Las recetas referencian productos del catálogo | Aprobada |
| [ADR-19](ADR-19-alta-perezosa-usuario.md) | Alta perezosa del usuario local | Abierta |
| [ADR-20](ADR-20-cifrado-datos-salud.md) | Cifrado de los datos de salud | Aprobada |
| [ADR-21](ADR-21-planificador-sin-conexion.md) | Planificador tolerante a cortes de conexión | Aprobada |
| [ADR-22](ADR-22-proveedor-nube.md) | Proveedor de nube: Google Cloud, en el proyecto de Firebase | Aprobada |
| [ADR-23](ADR-23-trazabilidad-catalogo.md) | Trazabilidad del catálogo | Abierta |

ADR-01 a ADR-12 están en el documento de Arquitectura C4 (`docs/curso/`). ADR-13 a ADR-23 están aquí, un archivo por decisión. Una ADR *Abierta* sigue en discusión: antes de construir sobre ella, confírmala con el equipo.

## Plantilla

Archivo: `ADR-NN-titulo-corto.md`

```markdown
# ADR-NN: Título

- **Estado:** Propuesta | Aprobada | Reemplazada por ADR-XX
- **Fecha:** AAAA-MM-DD
- **Origen:** D-n / tarea KAN-xx

## Decisión
## Contexto
## Alternativas
## Justificación
## Consecuencias
```
