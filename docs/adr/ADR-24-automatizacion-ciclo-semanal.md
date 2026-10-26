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

Este ADR detalla las piezas 1 y 2. La pieza 3 se precisa en ADR-17 y en la sección de abajo; la 4, también abajo.

## Contexto

En la sustentación el profesor observó que la aplicación era "muy CRUD": la usuaria arma la semana receta por receta y actualiza la despensa producto por producto. La gente usa software para ahorrar tiempo, así que el sistema debe tomar las decisiones repetitivas. La Definición dejaba la "recomendación automática" fuera de alcance; esta retroalimentación la vuelve parte del alcance.

## Detalle del generador

- El dominio vive en `planificador/domain/GeneradorPlan`, sin Spring ni JPA (ADR-09). Recibe un `ContextoPlan` y devuelve qué receta va en cada hueco. Se prueba de forma unitaria.
- **Reglas, en orden de peso:**
  1. **Alergias por persona** (KAN-14, ver "Adaptar recetas a una alergia"). Una alergia grave descarta la receta para toda la casa. Una alergia normal no la descarta: la receta entra adaptada para esa persona, pero las que todos comen igual van primero, porque son menos trabajo (−8 por cada cambio en una porción, −30 por cada plato aparte).
  2. **Días sin tiempo.** Solo llevan recetas de 20 minutos o menos. Si no hay ninguna apta, va la más rápida.
  3. **Despensa.** Puntúa más las recetas que se pueden hacer con lo que hay y, sobre todo, las que gastan algo que vence en 3 días o menos. La despensa se simula: lo que gasta una comida ya no cuenta para la siguiente.
  4. **Presupuesto** (ADR-14). Lo que queda del presupuesto semanal se reparte entre las comidas que faltan. Si al final el plan se pasa, cambia lo que propuso por recetas más baratas.
  5. **Niños.** Si en el hogar vive alguien de 12 años o menos (niño según la Ley 1098 de 2006), prefiere las recetas marcadas `receta.gusta_a_ninos` (V7): arroz con pollo, pasta, huevos, crema de papa… Es una preferencia: no pasa por encima de las alergias, el tiempo ni el presupuesto.
  6. **Variedad.** No repite receta el mismo día y evita repetirla en la semana y en días seguidos.
  7. **Condiciones de salud** (diabetes, hipertensión, colesterol). La receta queda más abajo, pero no se excluye: igual que en el recetario, solo es una precaución.
- **Comidas costosas después del pago** (Definición, proceso 3; KAN-25). Al final, si el perfil dice cuándo le pagan (ADR-14), el generador cambia de día lo que propuso para que las comidas más costosas queden en el día del pago y los siguientes. `CicloIngreso` calcula cuántos días van desde el último pago en cada día de la semana: semanal por día de la semana, mensual por día del mes (si el mes no tiene ese día, el último del mes) y quincenal con el más reciente de los dos pagos.
  - Solo intercambia recetas entre las comidas que propuso: la semana tiene las mismas recetas y el mismo costo.
  - No mueve lo que gasta productos por vencer, ni pone una receta demorada en un día sin tiempo, ni repite receta en el mismo día.
  - La respuesta trae `diasDespuesDelPago` y la pantalla lo cuenta: "Dejamos las comidas más costosas para el viernes y el sábado, cuando recién te pagan".
- **Empates.** Gana la receta más barata y luego la de menor id. Con los mismos datos el plan sale igual, lo que hace las pruebas repetibles.
- **Costo.** Se calcula con la fórmula del costo del plan (`cantidad / cantidadUnidadCompra × precio`, solo ingredientes obligatorios). Así el resultado coincide con la barra de presupuesto.
- **Plato fuerte.** Solo las recetas con `receta.es_plato_fuerte` (V7) van al almuerzo o la cena. Un batido o una ensalada de frutas no son una comida.
- **Edad de las personas.** El perfil ya pide la edad de cada persona (ADR-13). `ResumenHogarService` la usa para saber si hay niños, sin exponer quién es.
- El generador lee el catálogo con su propio puerto (`CatalogoParaPlan`). El perfil le entrega las restricciones ya descifradas mediante `ResumenHogarService` (RNF-26, ADR-20).

### Adaptar recetas a una alergia

En la revisión del 23 de octubre el profesor señaló que una alergia no impide cocinar la receta: una madre cabeza de hogar cocina para todos y a quien es alérgico le cambia el ingrediente (por ejemplo, el huevo por otra proteína que tenga en la despensa) o le hace otro almuerzo. `AdaptadorAlergias` (dominio, sin Spring) decide, persona por persona:

| Caso | Qué propone | Estado de la receta |
| --- | --- | --- |
| La persona marcó la alergia como **grave** ("no puede ni tocarlo") | Nada: la receta no se cocina en la casa, por el riesgo de contaminación en la misma cocina | `NO_APTA` |
| La alergia toca el **ingrediente principal** (`ingrediente_receta.es_principal`) | Un **plato aparte** para esa persona: la receta más rápida y barata que sí puede comer | `PLATO_APARTE` |
| Toca un ingrediente secundario y hay **reemplazo** que no le haga daño | Cambiarlo en su porción, primero por uno que haya en la despensa ("cambia huevos por pollo, que ya tienes en la despensa") | `ADAPTADA` |
| Toca un ingrediente secundario y no hay reemplazo | Servir su porción **sin** ese ingrediente | `ADAPTADA` |

- **Alergia grave.** Cada persona marca en el perfil cuáles de sus alergias son graves (`persona.alergias_graves`, cifrado igual que las demás, V8).
- **Ingrediente principal** (V8). El catálogo inicial se marca con una regla: es principal el producto que da nombre al plato (la primera palabra de la receta contiene la del producto). Así, en "Huevos pericos" lo principal es el huevo, y en "Arroz con huevo y tomate" es el arroz, de modo que el huevo sí se puede cambiar. Se corrigieron a mano dos platos cuyo nombre no lo dice (tortilla de papa y crema de papa).
- **Tabla de reemplazos** (`reemplazo_producto`, V8). La mantiene la aplicación sola con `ReglaReemplazo`: un producto con alérgenos se puede cambiar por otro de la **misma categoría** que no tenga esos alérgenos (huevos → pollo, carne, atún). Se recalcula al arrancar y cada vez que el curador crea, edita o quita un producto. El curador puede **agregar** un reemplazo que la regla no ve, **quitar** uno que no tenga sentido (la aplicación no lo vuelve a proponer) o **volver a usarlo**. Lo que él decide queda con origen `CURADOR`, a su nombre y en la auditoría del catálogo (ADR-23).
- **El reemplazo también se revisa.** Un reemplazo que tenga otra alergia de la misma persona no se propone.

### Endpoints

| Método y ruta | Qué hace |
| --- | --- |
| `POST /api/planes-semanales/{id}/generar` | Llena los huecos vacíos del almuerzo y la cena. Devuelve el plan, los cambios, el costo, el presupuesto, los productos por vencer que aprovecha y cuántas comidas son de las que les gustan a los niños. |
| `POST /api/planes-semanales/{id}/comidas/{comidaId}/otra-opcion` | Cambia una comida por la siguiente mejor opción, sin repetir las descartadas (`excluir`). Responde 409 `PLAN_SIN_ALTERNATIVAS` cuando no quedan opciones y 409 `PLAN_COMIDA_PREPARADA` si la comida ya está preparada. |
| `POST /api/planes-semanales/{id}/ajustar-presupuesto` | Cambia las comidas no preparadas más caras por otras más baratas hasta caber en el presupuesto, o hasta que no haya un cambio que ahorre. |
| `GET /api/adaptaciones?recetas=1,2` | Cómo adaptar cada receta a las alergias de cada persona del hogar. Lo usan el detalle de receta y el día del plan. |
| `GET /api/curaduria/productos/{id}/reemplazos` | Reemplazos de un producto, activos y quitados, con su origen (curador). |
| `PUT` / `DELETE /api/curaduria/productos/{id}/reemplazos/{reemplazoId}` | El curador agrega o vuelve a usar un reemplazo, o lo quita. Ambos son idempotentes. |

Las rutas del plan regeneran la lista de compras una sola vez, al final.

## Detalle de la despensa automática (pieza 2, KAN-16)

| Evento | Qué hace la aplicación sola |
| --- | --- |
| La usuaria marca un producto de la lista como **comprado** | Entra a la despensa en **presentaciones completas** (si faltan 300 g de arroz, entra la bolsa de 1 kg) con vencimiento = hoy + `producto.vida_util_dias` (V9). Si ya había del mismo producto, suma y conserva el vencimiento más próximo: se gasta primero lo más viejo. Desmarcarlo lo deshace. |
| Marca una comida como **preparada** | Descuenta de la despensa los ingredientes obligatorios de la receta, sin bajar de cero y solo la primera vez. |
| Un producto queda **por debajo de su mínimo** | Aparece solo en la lista de compras como "Para reponer" (prioridad 2), si el plan no lo pide ya. |
| Alguien tiene **alergia** | La lista suma una porción del reemplazo (si no está en la despensa) o los ingredientes de su plato aparte, marcados como "Para la porción de quien tiene alergia". |

- La lista ya no cuenta las comidas preparadas, porque sus ingredientes ya salieron de la despensa.
- Lo comprado queda en la lista aunque el plan cambie, para poder deshacerlo. Antes se guardaba en el teléfono; ahora se guarda en el servidor.
- **Puertos:** el planificador mueve la despensa con su propio puerto `MovimientosDespensa` (SQL directo sobre `item_despensa`), sin las entidades del módulo de despensa (RNF-26).
- **Vida útil:** la vida útil del catálogo inicial es una referencia para una casa colombiana (granos 1 año, pollo y carne 3 días en la nevera, leche 7 días…). El curador la corrige en la ficha del producto.

## Borrador de la semana y modo rescate (piezas 3 y 4, KAN-27 y KAN-25)

**Borrador automático.** Desde el viernes, cuando la usuaria abre la app (`GET /api/avisos`) y la semana siguiente no tiene comidas, el motor de avisos le pide al generador que la arme. El plan queda con `plan_semanal.borrador = TRUE` (V11) y el aviso «Te armamos la próxima semana» lleva a revisarlo (`/plan?semana=proxima`).

- Si la usuaria ya puso alguna comida en esa semana, no se toca.
- Se confirma con «Me sirve, la dejo así» (`PUT /api/planes-semanales/{id}/confirmar`). Mientras tanto se puede cambiar como cualquier plan.
- Si el generador no encuentra recetas, no hay borrador: el aviso invita a armar la semana.
- El tipo de aviso «Próxima semana» se puede apagar; apagado, tampoco se arma el borrador.

**Modo rescate** (`GET /api/rescate`, pantalla «¿Qué cocino hoy?»). Propone recetas que se pueden hacer hoy **sin comprar nada**:

- La despensa (sin lo vencido) alcanza para todos los ingredientes obligatorios.
- Nadie tiene alergia grave a la receta. Cada reemplazo por alergia ya está en la despensa, y si a alguien le toca plato aparte, ese plato también se puede hacer con lo que hay.
- Orden: primero la que salva el producto que vence antes (ventana de 7 días, la misma de la despensa), luego las que aprovechan más productos, los platos fuertes, las que todos comen igual y las más rápidas.
- Se puede limitar el tiempo (20 o 40 minutos).

El aviso «Aprovechar lo que tienes» (ADR-17) es la primera opción del modo rescate que salva algo que vence en 3 días o menos.

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
- La lista de compras ya suma el reemplazo y el plato aparte de quien tiene alergia (pieza 2). El **costo del plan** (`/costo`) todavía no los suma: sigue siendo el costo de las recetas, igual que la barra de presupuesto.
- El vencimiento de lo comprado es una estimación: si el empaque dice otra fecha, la usuaria la corrige en la despensa.
- Qué alérgeno tiene cada producto (`producto_restriccion`) solo se carga por migración: la pantalla del curador todavía no lo edita. Tampoco hay editor de recetas, así que `es_principal` solo se cambia por migración.
- La Definición y el C4 deben mencionar la automatización y retirar "recomendación automática" de lo que está fuera de alcance.
