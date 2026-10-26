# ADR-25: Modo cocinar con asistente de voz y video de la receta

- **Estado:** Propuesta
- **Fecha:** 2026-10-24
- **Origen:** Retroalimentación de la sustentación: "organizar la receta en formato de tareas" y "video de la receta, máximo la mitad del tiempo de la receta" · Usa KAN-16 (ADR-24, pieza 2)

## Decisión

**Modo cocinar** (`/recetas/:id/cocinar`). La receta se vuelve una lista de tareas, un paso por pantalla:

1. **Alistar**: los ingredientes, escalados a las porciones elegidas, como casillas para marcar. También muestra la preparación previa y los ajustes por alergias de ADR-24.
2. **Pasos**: un paso a la vez, en letra grande, con la barra de avance y los botones «Atrás», «Repetir» y «Listo».
3. **Final**: «La preparé» descuenta los ingredientes de la despensa (KAN-16).
   - Si se entró desde el plan (`?plan=&comida=`), usa `marcar-preparada`, que además marca la comida del día.
   - Si se entró desde el recetario, usa `POST /api/preparaciones`.

**Asistente de voz.**

- Lee cada paso en voz alta con `speechSynthesis` del navegador. Prefiere una voz `es-CO` y habla un poco más despacio que lo normal.
- Saluda por el nombre y anima entre pasos. Avisa «Último paso».
- Se apaga con 🔇 y la preferencia queda guardada en el celular.

**Temporizador.** Si el paso dice un tiempo ("20 minutos", "15 a 20 minutos", "media hora"), ofrece contarlo.

- Cuenta con la hora real, así que no se atrasa si la pantalla se duerme.
- Al terminar suena, vibra y lo dice ("¡Ya pasaron los 20 minutos! Toca «Listo» y seguimos").
- Puede haber varios a la vez, uno por paso: el arroz sigue contando mientras se hace el sofrito. Cada uno dice de qué paso es.
- El aviso de un tiempo que terminó se quita solo al tocar o decir «Listo» (o «Atrás»), sin un clic aparte. «Quitar» lo cierra sin avanzar.
- Regla de los pasos: **como máximo un tiempo por paso** y con un solo número. Un paso como "sofríe la cebolla 2 minutos, luego el tomate 4 minutos" se parte en dos. `PasosReceta.tiemposDelPaso` aplica la regla y `PasosRecetasIntegracionTest` la vigila sobre todo el catálogo; V12 reescribió los pasos de las 21 recetas con ella.

**Manos libres** (comandos de voz). Usa `SpeechRecognition` del navegador.

- Entiende «siguiente», «repite», «atrás» y «temporizador» dentro de una frase ("ya, siguiente").
- Está **apagado por defecto**.
- Solo escucha mientras la pantalla está abierta y se pausa mientras el asistente habla, para no oírse a sí mismo.

**Introducción.** La primera vez salen cuatro tarjetas cortas, leídas en voz alta. Explican cómo avanzar, los comandos de voz y cómo silenciar. En la última se prueba la voz y se enciende Manos libres. Después se vuelve a ver con el botón «?».

**Pantalla encendida.** Mientras cocina, se pide `navigator.wakeLock` para que el celular no se apague. El avance se guarda en el celular durante 12 horas para seguir donde iba si cierra la app.

**Video de la receta.**

- El curador pega el enlace de YouTube y escoge el fragmento útil (inicio y fin). El fragmento dura **como máximo la mitad** del tiempo de la receta.
- La regla se valida en el backend (`VideoYoutube`) y en la base (CHECK `ck_receta_video`, V10).
- Se ve en el **detalle de la receta**, no dentro del Modo cocinar: mientras cocina, la pantalla es para los pasos.
- Al principio solo se muestra la portada. El reproductor de `youtube-nocookie.com` se carga cuando la persona toca reproducir.

## Contexto

- El profesor pidió ver la receta como tareas y un video corto.
- Juan pidió además un asistente que lea los pasos, que se pueda apagar con un botón, y comandos de voz.
- Juan pidió quitar el video del Modo cocinar y que la primera vez haya una introducción muy amigable.
- La usuaria cocina con las manos ocupadas o sucias, a un metro del celular.

## Alternativas

- **Voz en la nube** (Google Cloud TTS, Polly). Se descartó: cuesta por carácter, necesita llaves en el backend y no funciona sin conexión. La voz del navegador es gratis y ya está en Android.
- **Reconocimiento propio** (Whisper, Vosk). Se descartó: un modelo en el navegador pesa decenas de MB y uno en el servidor exige enviar audio a nuestro backend. Para cuatro palabras basta el del navegador.
- **Subir videos propios.** Se descartó: almacenamiento y ancho de banda en AWS, y el curador tendría que editar el video. Con YouTube se guarda solo el id y el fragmento.
- **Video dentro del Modo cocinar.** Se descartó a pedido de Juan: compite con el paso y con la voz del asistente.

## Justificación

- Las tres APIs son estándar del navegador. No agregan dependencias, llaves ni costo.
- Si el navegador no las tiene, el modo funciona igual leyendo y tocando: la voz y Manos libres solo aparecen cuando están disponibles.
- La regla del 50 % queda en tres capas (pantalla, servicio y CHECK), así un dato malo no llega a la usuaria.
- Cargar YouTube solo al tocar evita cookies y datos móviles de quien no ve el video.

## Consecuencias

- `SpeechRecognition` solo existe en Chrome y Edge (Android y escritorio). En Safari y Firefox no aparece el botón 🎤.
- En Chrome el audio de Manos libres lo procesa el servicio de voz de Google. Por eso está apagado por defecto y la introducción lo avisa antes de encenderlo. Se debe mencionar en la política de privacidad.
- La calidad de la voz depende del celular. Se elige la más natural que haya: primero las "Natural", "Mejorada", "Premium", de Google o en línea, aunque sean de otro país, y luego el español latinoamericano. Las voces de efectos de Apple (Eddy, Flo, Grandma…) se descartan. Si la voz elegida es básica, la introducción le dice a la usuaria dónde descargar una mejor en iPhone o Android.
- Habla frase por frase, con velocidad 0,95 y tono normal, para que haga las pausas de la puntuación (y porque Chrome corta las frases de más de 15 segundos).
- Safari solo deja hablar si la primera frase sale de un toque, y pierde la frase si se corta la anterior en el mismo instante. La voz se "despierta" con el primer toque en cualquier parte de la app (una frase vacía en silencio), lo que se quiso decir antes de ese toque se dice con él, y después de cortar se esperan 80 ms. En iPhone, con el interruptor de silencio activado, la voz no suena.
- Los minutos del temporizador salen del texto del paso. Si el curador escribe el tiempo de otra forma, no se ofrece y la usuaria cuenta por su cuenta. La expresión de `guion.ts` y la de `PasosReceta` deben cambiar juntas.
- Las cantidades de agua de los pasos son para las porciones de la receta: si la usuaria cocina para más personas, los ingredientes se escalan pero el texto del paso no.
- `POST /api/preparaciones` descuenta, pero no queda registro de qué se cocinó fuera del plan. El análisis del administrador (bloque 5) puede necesitar guardarlo.
- Si el video se borra de YouTube, la portada sale vacía. El curador lo cambia desde «Videos de recetas».
