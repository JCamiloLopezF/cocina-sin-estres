# ADR-17: Motor de avisos en el backend

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D5 · KAN-27 · Precisa ADR-10

## Decisión

El motor de reglas corre en el backend cuando la aplicación consulta `GET /api/avisos` al abrir la sesión, como dice el ADR-10. Se usan los cinco avisos de la Definición. El RNF-07 se mide como el tiempo de respuesta de ese endpoint.

## Contexto

El ADR-10 ubicaba el motor en el backend; el Proceso 4 y el RNF-07, en el cliente. Además, el ADR-10 listaba "presupuesto o lista de compras pendiente" como quinto aviso, mientras la Definición usa "aprovechar un producto antes de que se dañe".

| Aviso | Se genera cuando | Clave de idempotencia |
| --- | --- | --- |
| `BAJO_MINIMO` | La cantidad de un producto está por debajo de su mínimo | hogar + producto + fecha |
| `POR_VENCER` | Un producto vence en 3 días o menos | hogar + producto + fecha |
| `PREPARACION_PREVIA` | Una receta del plan de mañana requiere preparación previa (por ejemplo, remojo) | hogar + comida + fecha |
| `SEMANA_SIN_PLANIFICAR` | Desde el viernes, la semana siguiente no tiene comidas asignadas | hogar + semana |
| `APROVECHAR_PRODUCTO` | Hay una receta que usa un producto por vencer y se puede hacer sin comprar | hogar + producto + fecha |

## Detalle

- Las reglas viven en `domain/ReglasAviso`, sin Spring ni JPA, para probarlas de forma unitaria (RNF-29).
- La clave de idempotencia es una restricción `UNIQUE` en `aviso`; un aviso repetido no se inserta.
- Solo se evalúan los tipos que la usuaria tiene activados (`hogar_preferencia_aviso`).
- Se muestran como máximo 5 avisos por sesión, en el orden de la tabla. Varios productos en el mismo caso se agrupan en un solo aviso ("2 productos están por vencer").
- Los umbrales (3 días, viernes) son constantes configurables.

## Alternativas

Motor en el cliente; tareas programadas en el servidor; notificaciones push.

## Justificación

En el backend las reglas son verificables, no dependen del dispositivo de gama baja y comparten el dominio con el planificador (modo rescate).

## Consecuencias

- La receta tiene el campo `preparacion_previa` (texto, opcional).
- Hay que corregir el Proceso 4 de la Definición, el RNF-07 y el quinto aviso del ADR-10.
