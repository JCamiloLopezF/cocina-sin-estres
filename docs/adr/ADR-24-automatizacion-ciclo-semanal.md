# ADR-24: Automatización del ciclo semanal

- **Estado:** Propuesta
- **Fecha:** 2026-10-24
- **Origen:** Retroalimentación de la sustentación del prototipo (19 de octubre) · Precisa ADR-09 y ADR-17

## Decisión

La aplicación deja de esperar a que la usuaria llene todo a mano cada semana. El sistema propone y ella corrige. La automatización se hace en cuatro piezas:

1. **Generador del plan** ("Armar mi semana"). Llena el almuerzo y la cena de los siete días con recetas del catálogo. Lo que la usuaria ya puso no se toca. Desde el plan se puede pedir "Otra opción" para una comida y "Ajustar al presupuesto" para toda la semana.
2. **Despensa que se actualiza sola.**
   - Lo que se marca como comprado en la lista entra a la despensa, con un vencimiento estimado según la vida útil del producto.
   - "La preparé" descuenta los ingredientes de la receta (KAN-16).
   - Lo que queda por debajo del mínimo pasa a la lista de compras.
3. **Motor de avisos** (ADR-17), que además deja un borrador de la semana siguiente cuando esta llega sin plan.
4. **Modo rescate** (KAN-25). Ante un imprevisto, propone qué cocinar hoy solo con lo que hay.

Este ADR detalla la pieza 1. Las piezas 2 a 4 se precisan en su propia tarea, sin cambiar las reglas de aquí.

## Contexto

En la sustentación el profesor observó que la aplicación era "muy CRUD": la usuaria arma la semana receta por receta y actualiza la despensa producto por producto. La gente usa software para ahorrar tiempo, así que el sistema debe tomar las decisiones repetitivas. La Definición dejaba la "recomendación automática" fuera de alcance; esta retroalimentación la vuelve parte del alcance.

## Detalle del generador

- El dominio vive en `planificador/domain/GeneradorPlan`, sin Spring ni JPA (ADR-09). Recibe un `ContextoPlan` y devuelve qué receta va en cada hueco. Se prueba de forma unitaria.
- **Reglas, en orden de peso:**
  1. **Alergias.** Nunca propone una receta con un ingrediente obligatorio al que alguien del hogar es alérgico. Las alergias son por persona (KAN-14): basta con una persona para excluir la receta, porque en casa todos comen lo mismo.
  2. **Días sin tiempo.** Solo llevan recetas de 20 minutos o menos. Si no hay ninguna apta, va la más rápida.
  3. **Despensa.** Puntúa más las recetas que se pueden hacer con lo que hay y, sobre todo, las que gastan algo que vence en 3 días o menos. La despensa se simula: lo que gasta una comida ya no cuenta para la siguiente.
  4. **Presupuesto** (ADR-14). Lo que queda del presupuesto semanal se reparte entre las comidas que faltan. Si al final el plan se pasa, cambia lo que propuso por recetas más baratas.
  5. **Niños.** Si en el hogar vive alguien de 12 años o menos (niño según la Ley 1098 de 2006), prefiere las recetas marcadas `receta.gusta_a_ninos` (V7): arroz con pollo, pasta, huevos, crema de papa… Es una preferencia: no pasa por encima de las alergias, el tiempo ni el presupuesto.
  6. **Variedad.** No repite receta el mismo día y evita repetirla en la semana y en días seguidos.
  7. **Condiciones de salud** (diabetes, hipertensión, colesterol). La receta queda más abajo, pero no se excluye: igual que en el recetario, solo es una precaución.
- **Empates.** Gana la receta más barata y luego la de menor id. Con los mismos datos el plan sale igual, lo que hace las pruebas repetibles.
- **Costo.** Se calcula con la fórmula del costo del plan (`cantidad / cantidadUnidadCompra × precio`, solo ingredientes obligatorios). Así el resultado coincide con la barra de presupuesto.
- **Plato fuerte.** Solo las recetas con `receta.es_plato_fuerte` (V7) van al almuerzo o la cena. Un batido o una ensalada de frutas no son una comida.
- **Edad de las personas.** El perfil ya pide la edad de cada persona (ADR-13). `ResumenHogarService` la usa para saber si hay niños, sin exponer quién es.
- El generador lee el catálogo con su propio puerto (`CatalogoParaPlan`). El perfil le entrega las restricciones ya descifradas mediante `ResumenHogarService` (RNF-26, ADR-20).

### Endpoints

| Método y ruta | Qué hace |
| --- | --- |
| `POST /api/planes-semanales/{id}/generar` | Llena los huecos vacíos del almuerzo y la cena. Devuelve el plan, los cambios, el costo, el presupuesto, los productos por vencer que aprovecha y cuántas comidas son de las que les gustan a los niños. |
| `POST /api/planes-semanales/{id}/comidas/{comidaId}/otra-opcion` | Cambia una comida por la siguiente mejor opción, sin repetir las descartadas (`excluir`). Responde 409 `PLAN_SIN_ALTERNATIVAS` cuando no quedan opciones y 409 `PLAN_COMIDA_PREPARADA` si la comida ya está preparada. |
| `POST /api/planes-semanales/{id}/ajustar-presupuesto` | Cambia las comidas no preparadas más caras por otras más baratas hasta caber en el presupuesto, o hasta que no haya un cambio que ahorre. |

Todas las rutas regeneran la lista de compras una sola vez, al final.

## Alternativas

- **Recomendación con aprendizaje** (historial y gustos). Se descartó: no hay datos de uso y el resultado no se puede explicar en la sustentación.
- **Generar en el navegador.** Se descartó: el navegador no ve las alergias descifradas y el costo debe ser el mismo que calcula el backend.
- **Optimización exacta del presupuesto** (programación lineal). Se descartó: con 14 comidas y un catálogo de decenas de recetas, el método voraz con ajuste final da un plan razonable en milisegundos y se puede explicar.

## Justificación

- El plan pasa de 14 decisiones manuales a una sola acción y unos pocos ajustes.
- Las reglas son las mismas que la usuaria ya veía por separado (alergias, días sin tiempo, presupuesto y despensa), ahora combinadas.
- Al ser determinista y estar aislado, se puede probar y explicar.

## Consecuencias

- La migración V7 agrega `receta.es_plato_fuerte` (por defecto sí) y `receta.gusta_a_ninos` (por defecto no). Falta exponer los dos campos en la pantalla de curaduría para que el curador los marque en recetas nuevas.
- El generador lee el catálogo completo en cada llamada. Si el catálogo crece a cientos de recetas, habrá que ponerle caché.
- No escala las cantidades por número de personas: usa las porciones de la receta, igual que el costo y la lista de compras actuales. Cambiarlo afecta las tres cosas a la vez.
- La Definición y el C4 deben mencionar la automatización y retirar "recomendación automática" de lo que está fuera de alcance.
