# ADR-13: Campos del perfil del hogar

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D1 · KAN-12

## Decisión

El perfil del hogar usa los campos de la Definición del Proyecto: personas y edades, restricciones alimentarias (alergias y condiciones de salud), presupuesto, ciclo de ingreso y días sin tiempo para cocinar. Se descartan ciudad, número de hijos y "preferencias" del C4. El número de personas se calcula a partir de la tabla `persona`.

## Contexto

El C4 (sección 1.4) y la Definición describían perfiles distintos. El RNF-01 exige no avanzar con campos obligatorios vacíos, así que había que fijar cuáles son.

## Detalle

- **Personas:** al menos una, con edad. El nombre es opcional (puede ser un alias como "Hijo mayor").
- **Restricciones:** se eligen de un catálogo cerrado (tabla `restriccion`: código, nombre y tipo `ALERGIA` o `CONDICION`). Ejemplos: lactosa, gluten, maní, huevo, mariscos, azúcar (diabetes), sodio (hipertensión). Se guardan a nivel de hogar, no por persona, porque el filtro del recetario aplica la unión de todas.
- **"Sin restricciones" es una respuesta explícita**, para distinguir un hogar sin restricciones de un perfil incompleto y poder verificar el RNF-01.
- **Presupuesto y ciclo de ingreso:** obligatorios (ver ADR-14).
- **Días sin tiempo:** opcional.
- **Consentimiento:** se registra la fecha en que la usuaria autoriza el tratamiento de sus datos de salud (Ley 1581 de 2012, datos sensibles).

## Alternativas

Campos del C4; restricciones en texto libre; restricciones por persona.

## Justificación

Los campos de la Definición son los que usan el planificador y los avisos. Un catálogo cerrado permite cruzar restricciones con productos de forma automática; el texto libre no.

## Consecuencias

- Cada producto del catálogo declara qué restricciones incumple (tabla `producto_restriccion`), y las recetas las heredan de sus ingredientes. Una receta es incompatible si alguno de sus productos tiene una restricción del hogar (RNF-03).
- Hay que corregir la sección 1.4 del C4.

## Notas de implementación

- En `V1__esquema_inicial.sql`, `restriccion` tiene `id` como clave primaria y `codigo` único, para seguir la convención de `CONTRIBUTING.md`. `producto_restriccion` referencia `restriccion_id`. El código sigue siendo el valor estable que se guarda (cifrado) en `hogar.restricciones`.
