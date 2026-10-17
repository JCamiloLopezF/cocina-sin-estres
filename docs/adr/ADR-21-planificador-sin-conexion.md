# ADR-21: Planificador tolerante a cortes de conexión

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D9 · KAN-21

## Decisión

Mientras la usuaria arma el plan, cada cambio (asignar o quitar una receta) se encola en el navegador y se envía al backend. Si no hay conexión, la cola se guarda en `localStorage` y se reenvía cuando el navegador vuelve a estar en línea.

## Contexto

El RNF-17 exige cero pérdidas de datos al cortar la red en el planificador, pero no había diseño. Una aplicación completa sin conexión (service worker) está fuera del alcance de un semestre.

## Detalle

- Alcance mínimo: solo las pantallas del planificador, con la aplicación ya cargada. No se soporta abrir la aplicación sin conexión.
- Cada cambio lleva un identificador generado en el cliente; el backend ignora un cambio que ya aplicó (`comida_plan.id_cambio_cliente` es `UNIQUE`), así que los reenvíos no duplican comidas.
- Si dos cambios tocan la misma casilla (día y comida), gana el último.
- Sin conexión, la interfaz muestra "Sin conexión. Guardaremos tus cambios" y marca el costo como pendiente de recalcular.

## Alternativas

Aplicación web progresiva con service worker; no soportar cortes.

## Justificación

Cubre el RNF-17 con poco código y no toca el backend más allá de la idempotencia.

## Consecuencias

- La prueba del RNF-17 se hace con el modo sin conexión de las herramientas del navegador.
- Es parte de KAN-21.
