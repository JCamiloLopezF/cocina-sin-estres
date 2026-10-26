# Backend — API REST

API de **Cocina sin estrés** en Java con Spring Boot. Concentra la lógica de negocio, verifica el token de Firebase en cada petición y aplica la autorización por rol.

## Requisitos

- Java 21 (LTS)
- Docker (para MySQL local con Docker Compose)
- No hace falta instalar Gradle: el proyecto usa el wrapper (`./gradlew`).

Versiones: Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7, Hibernate 7.4), Flyway 12, MySQL 8.4, Firebase Admin SDK 9.9.

## Configuración

1. Copia `.env.example` a `.env` y completa los valores. El archivo `.env` nunca se sube.
2. Siempre hay que activar un perfil: `dev` o `prod`.

| Variable | Para qué | dev | prod |
| --- | --- | --- | --- |
| `DB_URL`, `DB_USUARIO`, `DB_CONTRASENA` | Conexión a MySQL | Opcional (toma los valores del `docker-compose.yml`) | Obligatoria |
| `PUERTO` | Puerto de la API | Opcional (8080) | Opcional (8080) |
| `LOG_NIVEL` | Nivel de log de la aplicación | Opcional (DEBUG) | Opcional (INFO) |
| `FIREBASE_CREDENCIALES_RUTA` | JSON de la cuenta de servicio, **fuera del repositorio** | Se ignora | **Obligatoria**: sin ella la aplicación no arranca |
| `CIFRADO_CLAVE_V1` | Clave AES-256 en Base64 para datos de salud (ADR-20) | Opcional mientras no haya datos cifrados | Obligatoria |
| `ADMIN_INICIAL_CORREO` | Correo que entra como ADMIN la primera vez (ADR-16) | Opcional | Recomendada |
| `CORS_ORIGENES` | Orígenes del navegador permitidos | Opcional (`http://localhost:5173`) | Obligatoria |

El repositorio es público: ningún valor real se escribe en el código ni en los `application*.yml` (CONTRIBUTING.md §8).

## Ejecución

```bash
# Base de datos local (desde la raíz del repositorio)
docker compose up -d mysql

# API en el perfil dev. Flyway aplica las migraciones al arrancar.
./gradlew bootRun --args='--spring.profiles.active=dev'

# Pruebas (la de integración necesita Docker en ejecución)
./gradlew test
```

**Qué hace cada pieza.** Docker solo sirve para tener un MySQL en tu computador (`docker compose up -d mysql`); no ejecuta nuestro código ni las migraciones. Las migraciones las aplica **Flyway cuando arranca la API** (`bootRun`) o cuando corre la prueba de integración. Hacer `commit` y `push` solo sube los archivos: no toca ninguna base de datos. Para comprobar `V1` y `V2` en tu equipo basta con levantar MySQL y arrancar la API en `dev`; en el log aparece `Successfully applied 2 migrations`.

Si ya habías arrancado la API con una versión anterior de una migración y luego la cambias, Flyway falla por *checksum*. En tu base local se resuelve con `docker compose down -v` (borra los datos locales) y volviendo a arrancar.

## Autenticación

No hay endpoint de registro ni de inicio de sesión. El frontend entra con Firebase y manda el token en cada petición; el backend lo verifica en un filtro y busca o crea el `usuario` por `firebase_uid` (**alta perezosa**, ADR-05). La primera cuenta cuyo correo coincida con `ADMIN_INICIAL_CORREO` entra con rol `ADMIN`.

```bash
# prod: token de Firebase
curl -H 'Authorization: Bearer <idToken>' http://localhost:8080/api/yo
```

En el perfil `dev` no hace falta Firebase: un filtro alternativo acepta el encabezado `X-Dev-User` con el `firebase_uid` que quieras simular (ADR-19). El correo se inventa como `<uid>@dev.local`.

```bash
# dev: usuaria simulada
curl -H 'X-Dev-User: uid-mariana' http://localhost:8080/api/yo
```

> `FiltroUsuarioDev` es la única clase que lee ese encabezado y está marcada con `@Profile("dev")`: en `prod` el bean no se crea y el encabezado no significa nada.

## Autorización

Siempre en el backend (ADR-16). El frontend puede esconder botones, pero lo que decide es la cadena de seguridad.

| Ruta | Quién entra |
| --- | --- |
| `GET /api/salud` | Cualquiera, sin token |
| `/api/**` | Cuenta autenticada y activa |
| `/api/curaduria/**` | `CURADOR` o `ADMIN` |
| `/api/admin/**` | `ADMIN` |
| Todo lo demás | Nadie |

## Endpoints

| Método y ruta | Qué hace |
| --- | --- |
| `GET /api/salud` | Comprobación de salud. Público. Devuelve estado, versión y hora UTC. |
| `GET /api/yo` | Devuelve la cuenta autenticada: `id`, `correo`, `rol` y `perfilCompleto`. |
| `GET /api/curaduria/productos` | Lista los productos activos del catálogo, ordenados por categoría y nombre. `CURADOR` o `ADMIN`. |
| `GET /api/curaduria/productos/{id}` | Devuelve un producto. 404 si no existe. |
| `POST /api/curaduria/productos` | Crea un producto del catálogo global. Responde 201 con `Location`. |
| `PUT /api/curaduria/productos/{id}` | Actualiza un producto. Si no cambia ningún campo, no se modifica el responsable ni se audita. |
| `DELETE /api/curaduria/productos/{id}` | Desactiva el producto (no lo borra). Responde 204; repetirlo no tiene efecto. |
| `GET /api/admin/cuentas` | Lista las cuentas con su rol y si están activas. Solo `ADMIN`. |
| `GET /api/admin/analisis` | (ADMIN) Análisis del uso y los indicadores del Plan de Calidad (KAN-54): `uso` (usuarias, hogares activos, planes, recetas cocinadas, modo rescate), `indicadores` por proceso con `valor`, `meta`, `cumple` y `muestra`, recetas más cocinadas y planeadas y las que no se usan. Eventos de los últimos 30 días; planes de las últimas 8 semanas. Ver `docs/calidad/indicadores.md`. |
| `PUT /api/admin/cuentas/{id}` | Cambia el `rol` (`USUARIA`, `CURADOR`, `ADMIN`) o `activo` de otra cuenta. Un administrador no puede cambiarse a sí mismo (409 `ADMIN_CUENTA_PROPIA`). |
| `GET /api/items-despensa` | Lista la despensa del hogar de la usuaria. Parámetros opcionales: `filtro` (`TODO`, `POR_VENCER`, `BAJO_MINIMO`) y `buscar`. |
| `GET /api/items-despensa/resumen` | Totales para la pantalla de inicio: `total`, `porVencer` y `bajoMinimo`. |
| `GET /api/items-despensa/catalogo` | Productos del catálogo que la usuaria puede agregar (pantalla 16), por nombre. Parámetro opcional `buscar`. Cada uno trae `itemDespensaId` si ya está en la despensa. |
| `GET /api/items-despensa/{id}` | Devuelve un producto de la despensa. 404 si no existe o es de otro hogar. |
| `POST /api/items-despensa` | Agrega un producto del catálogo a la despensa. Responde 201 con `Location`. |
| `POST /api/items-despensa/adicional` | Registra un producto que no está en el catálogo (solo para ese hogar, sin precio, ADR-18) y lo agrega a la despensa. Lleva `nombre`, `tipoCantidad`, `unidadBase` opcional, `categoria` opcional y los datos del ítem. 409 `DESPENSA_PRODUCTO_EXISTE` si el nombre ya existe. |
| `PUT /api/items-despensa/{id}` | Edita cantidad o nivel, mínimo, vencimiento y ubicación. |
| `DELETE /api/items-despensa/{id}` | Quita el producto de la despensa. Responde 204. |
| `POST /api/planes-semanales` | Crea (o devuelve si ya existe) el plan de la semana. El `semanaInicio` debe ser lunes. |
| `GET /api/planes-semanales?semanaInicio=...` | Devuelve el plan de la semana con sus comidas. 404 si no hay plan para esa semana. |
| `PUT /api/planes-semanales/{planId}/comidas` | Asigna una receta a un día y tipo de comida. Si el hueco ya está ocupado, reemplaza la receta. |
| `DELETE /api/planes-semanales/{planId}/comidas/{comidaId}` | Quita la comida del plan. Responde 204. |
| `PUT /api/planes-semanales/{planId}/comidas/{comidaId}/marcar-preparada` | Marca la comida como preparada y descuenta de la despensa los ingredientes obligatorios de la receta, solo la primera vez (KAN-16). |
| `GET /api/planes-semanales/{planId}/lista-compras` | Lista de compras generada del plan con su costo total, ya descontando lo que el hogar tiene en su despensa (sin contar lo vencido). Si el plan no tiene comidas aún, responde lista vacía y costo cero. 404 si el plan no existe o es de otro hogar. |
| `PUT /api/planes-semanales/{planId}/lista-compras/items/{itemId}` | Cuerpo `{ "comprado": true }`: el producto entra a la despensa en presentaciones completas con vencimiento estimado según su vida útil (ADR-24). `{ "comprado": false }` lo deshace. Idempotente. Devuelve el ítem y `venceEn`. 404 si el plan o el ítem no son del hogar. |
| `GET /api/tiendas` | Tiendas que la usuaria guardó para pedir el mercado por WhatsApp (KAN-53), por nombre. Cada una trae `whatsapp` listo para `https://wa.me/`. |
| `POST /api/tiendas` | Cuerpo `{ "nombre": "Tienda de don Jorge", "whatsapp": "300 123 4567" }`. Acepta el celular con o sin `+57`, espacios o guiones, y lo guarda como `573001234567`. Responde 201. 400 `TIENDA_DATOS_INVALIDOS`; 409 `TIENDA_REPETIDA` (ya hay una con ese número) o `TIENDAS_MAXIMO` (hasta 5 por hogar). |
| `DELETE /api/tiendas/{id}` | Quita la tienda. 204. 404 si es de otro hogar. |
| `PUT /api/planes-semanales/{planId}/lista-compras/items/{itemId}/costo` | Cuerpo `{ "costoReal": 12500 }`: lo que la usuaria pagó de verdad (KAN-26). `{ "costoReal": null }` vuelve al estimado. Devuelve la lista con `costoTotal` ajustado (lo pagado donde se corrigió, el estimado en lo demás); cada ítem trae `costoEstimado` y `costoReal`. 400 `LISTA_COSTO_INVALIDO` si es negativo. 404 si el plan o el ítem no son del hogar. |
| `GET /api/planes-semanales/{planId}/costo` | Costo acumulado del plan por día (índice 0 = lunes), presupuesto semanal del hogar (ADR-14) y `excedePresupuesto`. El hogar sale de la usuaria autenticada. 404 si el plan no existe o es de otro hogar. |
| `POST /api/planes-semanales/{planId}/generar` | "Armar mi semana" (ADR-24): llena los huecos vacíos de almuerzo y cena con recetas aptas para las alergias del hogar, rápidas en los días sin tiempo, con lo que hay en la despensa (primero lo por vencer) y dentro del presupuesto. Cuerpo opcional `{ "comidas": ["ALMUERZO","CENA"] }`. Si en el hogar hay niños (12 años o menos), prefiere las recetas marcadas `gusta_a_ninos`. Devuelve el plan, los `cambios`, `costoEstimado`, `presupuestoSemanal`, `excedePresupuesto`, `aprovecha` (productos por vencer que usa), `paraNinos` (comidas que suelen gustarles a los niños) y `diasDespuesDelPago` (KAN-25: los días de la semana en que recién le pagan, donde quedaron las comidas más costosas). |
| `POST /api/planes-semanales/{planId}/comidas/{comidaId}/otra-opcion` | Cambia la receta de una comida por la siguiente mejor. Cuerpo opcional `{ "excluir": [ids] }` con las que ya se descartaron. 409 `PLAN_SIN_ALTERNATIVAS` si no quedan; 409 `PLAN_COMIDA_PREPARADA` si ya está preparada. |
| `POST /api/planes-semanales/{planId}/ajustar-presupuesto` | Cambia las comidas no preparadas más caras por otras más baratas hasta que el plan quepa en el presupuesto. Misma respuesta que `/generar`. |
| `PUT /api/planes-semanales/{planId}/confirmar` | La usuaria se queda con la semana que la app armó sola: `borrador` pasa a `false` (ADR-24). Idempotente. 404 si el plan no es de su hogar. |
| `GET /api/rescate?maxMinutos=20` | Modo rescate (KAN-25): recetas que se pueden hacer hoy sin comprar nada (la despensa alcanza, nadie tiene alergia grave y los reemplazos o platos aparte también salen de la despensa). Primero las que usan lo que vence antes. Cada opción trae `aprovecha` (producto, `venceEn`, `dias`) y `estado` frente a las alergias. Máximo 12. `maxMinutos` opcional, de 5 a 240. |
| `GET /api/avisos` | Motor de avisos (ADR-17, KAN-27): corre las cinco reglas que la usuaria tiene encendidas, guarda los avisos sin repetirlos (clave por tipo, hogar y día) y devuelve hasta 5 sin leer, en el orden de ADR-17. Desde el viernes, si la semana siguiente no tiene comidas, deja un borrador armado (ADR-24). Sin perfil del hogar responde lista vacía. |
| `GET /api/avisos/recientes` | Centro de avisos: los de los últimos 30 días, leídos o no. |
| `PUT /api/avisos/{id}/leido` · `PUT /api/avisos/leidos` | Marca uno o todos como leídos. 204. 404 si el aviso es de otro hogar. |
| `GET /api/avisos/preferencias` · `PUT /api/avisos/preferencias/{tipo}` | Qué tipos de aviso recibe el hogar. Cuerpo `{ "activo": false }`. 400 `AVISO_PREFERENCIA_INVALIDA` si el tipo no existe. |
| `GET /api/adaptaciones?recetas=1,2,3` | Cómo adaptar cada receta a las alergias de cada persona del hogar (ADR-24): `estado` (`APTA`, `ADAPTADA`, `PLATO_APARTE`, `NO_APTA`) y `cambios` por persona (`REEMPLAZO` con el producto y su reemplazo, `SIN`, `PLATO_APARTE` con la receta propuesta o `NO_APTA` por alergia grave). Máximo 30 recetas. |
| `GET /api/curaduria/productos/{id}/reemplazos` | (CURADOR) Reemplazos del producto para adaptar recetas, activos y quitados, con su `origen` (`SISTEMA` o `CURADOR`). |
| `PUT /api/curaduria/productos/{id}/reemplazos/{reemplazoId}` | (CURADOR) Agrega el reemplazo o lo vuelve a usar. Idempotente. Queda en la auditoría del catálogo. |
| `DELETE /api/curaduria/productos/{id}/reemplazos/{reemplazoId}` | (CURADOR) Lo quita; la aplicación no lo vuelve a proponer. Idempotente. |
| `POST /api/preparaciones` | Cuerpo `{ "recetaId": 10 }`: "La preparé" del Modo cocinar cuando la receta no está en el plan. Descuenta sus ingredientes obligatorios de la despensa (KAN-16). Responde 204. Si está en el plan se usa `marcar-preparada`. |
| `GET /api/curaduria/recetas` | (CURADOR) Recetas activas con su video y `maximoSegundos` (la mitad del tiempo de la receta, ADR-25). |
| `PUT /api/curaduria/recetas/{id}/video` | (CURADOR) Cuerpo `{ "enlace": "https://youtu.be/…", "inicioSeg": 30, "finSeg": 210 }`. Acepta enlaces de `watch`, `youtu.be`, `shorts`, `embed` y `live`. 400 `RECETA_VIDEO_ENLACE_INVALIDO`, `RECETA_VIDEO_FRAGMENTO_INVALIDO` o `RECETA_VIDEO_MUY_LARGO`. Queda en la auditoría del catálogo. |
| `DELETE /api/curaduria/recetas/{id}/video` | (CURADOR) Quita el video. Idempotente. |
| `PUT /api/curaduria/recetas/{id}/etiquetas` | (CURADOR) Cuerpo `{ "platoFuerte": true, "gustaANinos": false }`; lo que no se envía no cambia (KAN-17). "Armar mi semana" solo usa platos fuertes y prefiere los que les gustan a los niños (ADR-24). 400 `RECETA_SIN_CAMBIOS` sin campos. Queda en la auditoría del catálogo. `GET /api/curaduria/recetas` trae `platoFuerte` y `gustaANinos`. |
