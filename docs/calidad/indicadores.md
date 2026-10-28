# Indicadores de calidad en la aplicación (KAN-54)

El administrador ve en **Análisis** (`/admin/analisis`) los indicadores del Plan de Gestión de la Calidad (secciones 3.3 y 3.4) calculados con los datos reales de la base. El botón **Copiar para el informe** copia la tabla para pegarla en la sección 5 del plan (evidencias métricas).

- Eventos (aperturas, preparaciones, avisos, modo rescate): **últimos 30 días**.
- Planes: **últimas 8 semanas**, incluida la actual.
- Solo cifras agregadas: el análisis no muestra datos de ninguna usuaria.

## Cómo se calcula cada indicador

| Proceso | Indicador | Cálculo en la app | Meta del plan |
| --- | --- | --- | --- |
| Usuario | Completitud de perfil del hogar | Cuentas USUARIA activas con `perfil_completo` / cuentas USUARIA activas | > 90 % |
| Usuario | Éxito de autenticación | Lo mide la web (`inicio_sesion`, V16): inicios exitosos / (exitosos + intentos fallidos previos en ese navegador). Un intento fallido de alguien que nunca entra no se cuenta | > 95 % |
| Usuario | Tiempo de respuesta de login | Lo mide la web (`inicio_sesion.duracion_ms`, V16): milisegundos desde «Entrar» hasta que `GET /api/yo` reconoce la cuenta | < 1500 ms |
| Despensa | Actualidad del inventario | Promedio, por hogar, de días desde el último cambio en `item_despensa` | < 7 días |
| Despensa | Adopción del catálogo predefinido | Productos de la despensa del catálogo global (`producto.hogar_id IS NULL`) / todos | > 80 % |
| Despensa | Exactitud de existencias | Se verifica en CI con `ExactitudExistenciasIntegracionTest`: cada receta del catálogo para 1 a 4 personas (21 casos), 0 desviaciones (RNF-05) | > 98 % |
| Planificador | Tiempo de planificación semanal | Lo mide la web (`plan_semanal.segundos_planificacion`, V16): desde que la usuaria abre la semana sin completar hasta que tiene almuerzo y cena los 7 días, solo la primera vez. Solo planes completados en menos de un día | < 3 min |
| Planificador | Cumplimiento del presupuesto | Planes con costo ≤ presupuesto semanal del hogar (ADR-14) / planes con comidas | > 85 % |
| Planificador | Aprovechamiento de la despensa | Productos con existencias que pide alguna receta del plan de esta semana / productos con existencias | > 60 % |
| Planificador | Uso del modo rescate | Semanas planeadas en que el hogar abrió el modo rescate (`uso_rescate`, V15) / semanas planeadas | Seguimiento |
| Avisos | Atención de avisos de vencimiento | Avisos `POR_VENCER` marcados como leídos / generados | > 70 % |
| Avisos | Carga de avisos por sesión | Promedio de avisos mostrados por apertura de la app (`apertura_app`, V15) | ≤ 5 |
| Avisos | Tiempo de evaluación del motor de reglas | Promedio de milisegundos del motor por apertura, medido en el servidor (RNF-07) | < 1000 ms |
| Avisos | Cobertura de reglas evaluadas | Aperturas en que se evaluaron las 5 reglas / aperturas (RNF-04). Si la usuaria apaga un tipo de aviso, esa regla no se evalúa | 100 % |

## Qué se registra para medir (V15)

| Tabla | Cuándo se escribe | Qué guarda |
| --- | --- | --- |
| `apertura_app` | Cada `GET /api/avisos` (al abrir la app) | Avisos vigentes y mostrados, reglas evaluadas y milisegundos |
| `preparacion` | "La preparé" en el plan o desde el recetario | Hogar, receta y, si era del plan, la comida |
| `uso_rescate` | Cada `GET /api/rescate` | Cuántas recetas salieron |
| `plan_semanal.completado_en` | La primera vez que el plan tiene almuerzo y cena los 7 días | La fecha |

Si escribir un registro falla, la acción de la usuaria sigue: una métrica nunca impide cocinar o planear. Lo preparado antes de V15 se copió de `comida_plan`; el tiempo de planificación y el uso del modo rescate empiezan a contarse desde V15.
