# Planificador semanal

Módulo responsable de crear el plan semanal del hogar, asignar recetas por día y comida, y generar la lista de compras a partir del plan.

## Objetivo

Permitir que la usuaria del hogar organise la semana de comidas:
- elegir una receta para cada día y comida,
- mantener una única receta por espacio (día + comida),
- consultar el plan semanal,
- marcar alimentos como preparados,
- generar la lista de compras automáticamente.

La lógica respeta la estructura definida en V1 (`plan_semanal` y `comida_plan`).

## Reglas de negocio

- La semana se planifica por `semana_inicio` (lunes).
- Cada plan pertenece a un hogar.
- Hay una sola receta por combinación:
  - `plan_semanal_id + dia + tipo_comida`
  - Eso hace que cada día/ comida tenga un único registro.
- La receta debe existir y estar activa (`receta.activa = true`).
- El día debe estar en el rango 1..7.
- El tipo de comida debe ser uno de:
  - `DESAYUNO`
  - `ALMUERZO`
  - `CENA`
- La usuaria solo puede operar sobre planes de su propio hogar.
- Se usa `id_cambio_cliente` para manejar idempotencia y reenvíos del navegador.
- La lista de compras se genera automáticamente después de asignar una receta o al cambiar el plan.
- La usuaria puede corregir lo que pagó por un producto de la lista (`costo_real`, KAN-26) y guardar hasta 5 tiendas para pedirles por WhatsApp (`tienda`, KAN-53).
- "Armar mi semana" deja las comidas más costosas en el día del pago y los siguientes (`CicloIngreso`, ADR-24), sin cambiar las recetas ni el costo.
- Todo se calcula para las personas del hogar (`Porciones`, KAN-25): el costo, la lista y lo que se descuenta al cocinar usan `personas / porciones` de cada receta.
- "Armar mi semana" no planea los días que ya pasaron, no repite una receta más de dos veces en la semana si hay otras y prefiere las recetas que usan productos de la despensa que el plan todavía no usa.

## Endpoints

### POST /api/plan-semanal
Crea o devuelve el plan semanal para la semana indicada.

Request body:
```json
{
  "semanaInicio": "2026-10-12"
}
