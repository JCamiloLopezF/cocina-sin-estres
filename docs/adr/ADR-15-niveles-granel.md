# ADR-15: Niveles de granel sobre una cantidad estimada

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D3 · KAN-12 · Precisa ADR-11

## Decisión

Los productos a granel usan los niveles Lleno, Medio, Poco y Agotado en la interfaz. Internamente se guarda una cantidad estimada en la unidad base del producto, y el nivel visible se deriva de ella. Esto reemplaza la escala Alto/Medio/Bajo/Agotado del ADR-11.

## Contexto

La Definición y la interfaz usaban Lleno/Medio/Poco; el ADR-11 usaba otra escala y pedía definir un mapa de nivel a porción. Si solo se guardara el nivel, los descuentos pequeños nunca lo cambiarían: restar 100 g a un arroz "Lleno" lo deja en "Lleno", y la siguiente vez se vuelve a partir del tope.

## Detalle

- Cada producto a granel del catálogo tiene una `capacidad_referencia` en su unidad base: la cantidad que equivale a "Lleno" (por ejemplo, arroz 2.500 g o aceite 1.000 ml). La define el curador.
- Fracciones de cada nivel: Lleno 1,0; Medio 0,5; Poco 0,2; Agotado 0.
- Cuando la usuaria elige un nivel, `cantidad = fracción × capacidad_referencia` y el registro se marca como estimado (`es_estimada`).
- Al descontar una comida preparada se resta de la cantidad y el nivel se recalcula con estos umbrales: más de 0,75 es Lleno; más de 0,35, Medio; más de 0, Poco; 0, Agotado.
- El nivel mínimo ("Avisarme cuando quede en Poco") se guarda también como cantidad mínima, igual que en los productos contables.

## Alternativas

Guardar solo el nivel; exigir siempre cantidad numérica.

## Justificación

La usuaria sigue sin pesar nada, pero el sistema conserva los descuentos acumulados. Contables y granel comparten las mismas columnas y la misma lógica de cruce.

## Consecuencias

- El cruce y el costo de productos a granel son estimaciones, y la interfaz debe decirlo ("probablemente alcanza").
- El catálogo necesita la columna `capacidad_referencia`; la base de datos la exige para todo producto `GRANEL` (`ck_producto_capacidad`).
- Hay que actualizar el ADR-11 en el documento C4.
