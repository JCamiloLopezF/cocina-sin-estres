# Registro de incidentes

Auditoría externa del 4 de noviembre: «las fallas se corrigen, pero no se registran incidentes ni bugs con la plantilla definida». Desde ahora cada falla en producción queda aquí, además del commit que la corrige.

## Cómo se registra

1. **Detectar.** La vigilancia (`.github/workflows/vigilancia.yml`) pregunta a `/api/salud` cada 30 minutos; esa ruta también revisa la base de datos. Si no responde en 3 intentos, abre un *issue* con las etiquetas `incidente` y `caida-automatica`, y GitHub avisa por correo a quien vigila el repositorio. Una falla que reporta una persona se abre a mano con la plantilla **Incidente en producción**.
2. **Atender.** Quien lo toma se asigna el *issue* y comenta lo que va encontrando.
3. **Cerrar.** La vigilancia cierra sola las caídas cuando la API vuelve. Antes de dar el incidente por terminado se completa la fila de la tabla de abajo, con causa, solución y el KAN o PR.
4. **Defectos.** Lo que no es una caída, sino algo que no cumple un requisito o un caso de prueba, se abre con la plantilla **Defecto** (pasos, resultado esperado y obtenido, severidad).

## Incidentes

| # | Inicio (hora de Colombia) | Qué pasó | Impacto | Causa | Solución | Resuelto |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 2026-10-19 | `GET /api/items-despensa/null` y otras rutas con parámetros mal escritos respondían 500 (caso T112 de TestRail) | Bajo | `ManejadorErrores` no atrapaba `TypeMismatchException` ni `ServletRequestBindingException` | Responden 400 `PETICION_MAL_FORMADA` (KAN-32, PR #54) | 2026-10-28 |
| 2 | 2026-10-25 | El despliegue del release se quedó esperando y Elastic Beanstalk marcó *Degraded* | Medio: 1 a 2 minutos sin API y estado dudoso | Siete migraciones seguidas tardaron más de lo que esperaba el script | El script espera hasta 30 minutos (`docs/despliegue/aplicacion.md`, «Problemas comunes») | 2026-10-25 |
| 3 | 2026-10-27 | En la auditoría en vivo, un presupuesto de 999.999.999.999.999 respondía 500 | Bajo | El valor no cabía en `DECIMAL(12,2)` y no se validaba el tope | Tope en la API y en el formulario (KAN-32, PR #52 y #58) | 2026-10-28 |
| 4 | 2026-10-27 | Una prueba de seguridad borró la tienda de una cuenta de prueba | Bajo: cuenta de prueba | La prueba de `DELETE /tiendas/{id}` se hizo con la cuenta dueña | Se recreó la tienda; las pruebas destructivas se hacen con datos creados para la prueba | 2026-10-28 |
| 5 | 2026-10-27 | Auditoría externa: el recetario mostró «Algo falló de nuestro lado» y el servidor dejó de responder un par de veces | Medio: el recetario no cargaba | El recetario pedía el detalle de cada receta por separado (más de 20 peticiones a la vez) a una sola instancia `t4g.micro` | Una sola petición `/api/recetas/detalle` (PR #58); la web reintenta las lecturas ante 502, 503 o 504; `/api/salud` revisa la base y la vigilancia avisa | 2026-11-04 |
