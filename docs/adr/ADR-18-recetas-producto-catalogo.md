# ADR-18: Las recetas referencian productos del catálogo

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D6 · KAN-12, KAN-22

## Decisión

Cada ingrediente de una receta apunta a un `producto_id` del catálogo, con su cantidad en la unidad base y una marca de opcional. El cruce con la despensa compara por `producto_id`. La pantalla 25 deja de ser un paso obligatorio y solo sirve para registrar equivalencias del hogar.

## Contexto

El ADR-08 dice que las recetas se escriben con productos del catálogo, así que no hay nada que emparejar. Pero la interfaz tenía una pantalla de correspondencia, y la Definición permite registrar productos adicionales que no están en el catálogo.

## Detalle

- `producto.hogar_id` en `NULL` significa producto del catálogo global; con valor, es un producto adicional que registró un hogar.
- Si la receta pide un producto que el hogar no tiene, pero tiene uno que considera equivalente (por ejemplo, un adicional), la pantalla 25 permite elegirlo.
- La equivalencia se guarda en `equivalencia_hogar` (hogar, producto de la receta, producto del hogar) cuando la usuaria marca "Recordar esta correspondencia".
- El costo siempre usa el precio de referencia del producto de la receta, nunca el del equivalente.

## Alternativas

Ingredientes en texto libre con emparejamiento obligatorio; eliminar la pantalla 25.

## Justificación

El cruce queda determinístico y el costo cumple el RNF-02 sin depender de lo que elija la usuaria.

## Consecuencias

- El planificador funciona sin la pantalla 25, que puede entrar al plan de contingencia si hay atraso.
- Que los ingredientes sean siempre productos del catálogo global lo valida la aplicación, no la base de datos.
- Hay que aclarar el alcance de la correspondencia en la Definición (objetivo específico "Recetario").
