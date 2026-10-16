# ADR-14: Presupuesto por ciclo de ingreso

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D2 · KAN-12

## Decisión

El hogar guarda el monto que destina a alimentación en cada ciclo de ingreso, junto con la periodicidad y los días de pago. El presupuesto semanal que muestra la interfaz ("$142k de $195k") se calcula en `domain/` y no se almacena.

## Contexto

El Proceso 3 de la Definición hablaba de presupuesto mensual; el C4 y la interfaz, de presupuesto semanal. Además, el planificador necesita saber cuándo entra el dinero para ubicar las comidas costosas.

## Detalle

- `hogar.presupuesto_monto`: `DECIMAL(12,2)` en pesos colombianos.
- `hogar.periodicidad_ingreso`: `SEMANAL`, `QUINCENAL` o `MENSUAL`.
- `hogar.dia_pago_1` y `hogar.dia_pago_2`: día de la semana (1-7) si es semanal; día del mes (1-31) si es mensual; dos días del mes en orden si es quincenal (por ejemplo 15 y 30).
- Presupuesto semanal = monto × 7 / días del ciclo (7 semanal, 15,2 quincenal, 30,4 mensual). Lo calcula `CalculadoraPresupuesto`.
- Todos los cálculos de dinero usan `BigDecimal` y redondean a pesos solo al mostrar el valor, para no fallar el RNF-02 por redondeo.

## Alternativas

Guardar solo el presupuesto semanal; guardar un presupuesto mensual fijo.

## Justificación

La usuaria piensa en lo que recibe en cada pago, no en semanas. Guardar el ciclo cubre ambos casos y alimenta la regla de comidas costosas después del ingreso.

## Consecuencias

- El semanal es un prorrateo, no el saldo real del ciclo. Si el equipo prefiere mostrar "lo que queda hasta el próximo pago", basta con cambiar `CalculadoraPresupuesto`, sin tocar el esquema.
- Hay que corregir el Proceso 3 de la Definición.
- La base de datos valida los días de pago según la periodicidad (`ck_hogar_dias_pago`).
