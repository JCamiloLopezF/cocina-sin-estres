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

### Cómo quedó implementado (KAN-27, octubre de 2026)

- Como los productos se agrupan, la clave de idempotencia es una por tipo y día: `TIPO:hogar:fecha` (`SEMANA_SIN_PLANIFICAR` usa el lunes de la semana; `PREPARACION_PREVIA`, la fecha de mañana). Si la situación cambia en el día y el aviso no se ha leído, se actualiza su texto; si ya se leyó, no vuelve a salir.
- Solo se muestran los avisos de la evaluación actual: si el producto ya se gastó, el aviso de ayer no reaparece.
- V11 agrega a `aviso` un `titulo`, el `enlace` a la pantalla donde se resuelve y el texto del botón (`accion`).
- `SEMANA_SIN_PLANIFICAR` además deja un borrador de la semana (ADR-24). `APROVECHAR_PRODUCTO` sale del modo rescate (KAN-25).
- Tono (RNF-14): dicen qué pasa y qué hacer, sin admiraciones ni palabras de alarma; hay una prueba que lo revisa.
- RNF-07: cada evaluación deja en el log `Avisos evaluados: hogarId=… vigentes=… mostrados=… ms=…`, que sirve para medir el tiempo del motor y los avisos por sesión.
- El centro de avisos (`/avisos`) lista los últimos 30 días y permite encender o apagar cada tipo.

## Alternativas

Motor en el cliente; tareas programadas en el servidor; notificaciones push.

## Justificación

En el backend las reglas son verificables, no dependen del dispositivo de gama baja y comparten el dominio con el planificador (modo rescate).

## Consecuencias

- La receta tiene el campo `preparacion_previa` (texto, opcional).
- Hay que corregir el Proceso 4 de la Definición, el RNF-07 y el quinto aviso del ADR-10.
