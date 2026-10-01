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
| ADR-13 | Campos del perfil del hogar | Propuesta |
| ADR-14 | Presupuesto por ciclo de ingreso | Propuesta |
| ADR-15 | Niveles de granel sobre una cantidad estimada | Propuesta |
| ADR-16 | Tres roles con mínimo privilegio | Propuesta |
| ADR-17 | Motor de avisos en el backend | Propuesta |
| ADR-18 | Las recetas referencian productos del catálogo | Propuesta |
| ADR-19 | Alta perezosa del usuario local | Propuesta |
| ADR-20 | Cifrado de los datos de salud | Propuesta |
| ADR-21 | Planificador tolerante a cortes de conexión | Propuesta |
| ADR-22 | Proveedor de nube | Abierta |
| ADR-23 | Trazabilidad del catálogo | Propuesta |

ADR-01 a ADR-12 están en el documento de Arquitectura C4 (`docs/curso/`). ADR-13 a ADR-23 se agregan aquí, un archivo por decisión, cuando el equipo los apruebe.

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
