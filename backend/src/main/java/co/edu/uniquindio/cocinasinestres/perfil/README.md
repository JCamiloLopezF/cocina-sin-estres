## API: Perfil del hogar

Endpoints para crear/actualizar y consultar el perfil del hogar de la usuaria autenticada.

Prefijo: `/api/perfil/hogar`

Autenticación
- Requiere token Firebase válido en la capa de seguridad (el middleware lo valida).
- El endpoint actúa sobre la usuaria autenticada; no se recibe `usuarioId` en el cuerpo.

PUT /api/perfil/hogar
- Descripción: crea o actualiza el perfil del hogar de la usuaria autenticada.
- Headers: `Authorization: Bearer <token>`
- Body (JSON):
  {
  "presupuestoMonto": 1200000.00,
  "periodicidadIngreso": "MENSUAL",           // SEMANAL | QUINCENAL | MENSUAL
  "diaPago1": 5,
  "diaPago2": null,                           // solo para QUINCENAL
  "diasSinTiempo": [2,6],
  "restricciones": ["lactosa", "gluten"],    // lista de códigos (se cifran en BD)
  "sinRestricciones": false,
  "otrasCondiciones": "Condición adicional", // texto cifrado en BD
  "consentimientoDatos": true,
  "personas": [
  { "nombre": "María", "edad": 34 },
  { "nombre": "Juan", "edad": 3 }
  ]
  }

- Respuesta 200: PerfilHogarResponse (ver abajo)
- Errores esperados:
  - 400 / PERFIL_HOGAR_DATOS_INVALIDOS — validación de campos (ej. presupuesto <= 0, días fuera de rango, personas vacías, restricción + sinRestricciones).
  - 401 — token ausente o inválido.
  - 403 — si rol no permitido (seguridad según reglas del proyecto).
  - 409 — conflicto concurrente (en caso de constraint unique), opcionalmente reintentar.

GET /api/perfil/hogar
- Descripción: obtiene el perfil del hogar de la usuaria autenticada.
- Headers: `Authorization: Bearer <token>`
- Respuesta 200:
  {
  "id": 12,
  "presupuestoMonto": 1200000.00,
  "periodicidadIngreso": "MENSUAL",
  "diaPago1": 5,
  "diaPago2": null,
  "diasSinTiempo": [2,6],
  "restricciones": ["lactosa", "gluten"], // ya descifradas por el servicio
  "sinRestricciones": false,
  "otrasCondiciones": "Condición adicional", // descifrada
  "consentimientoDatosEn": "2026-10-15T14:30:00",
  "personas": [
  { "id": 3, "nombre": "María", "edad": 34 },
  { "id": 4, "nombre": "Juan", "edad": 3 }
  ]
  }

Notas de implementación importantes
- Campos sensibles (`restricciones`, `otrasCondiciones`) se cifran antes de guardarlos en la BD; se descifran en la respuesta si la usuaria ha consentido.
- `diasSinTiempo` se guarda como JSON en la columna `dias_sin_tiempo`.
- Al guardar el perfil completo (según reglas), el servicio actualiza `usuario.perfil_completo = true`.
- Fechas de BD (creado/actualizado/consentimiento) se exponen en formato ISO-8601 (LocalDateTime).

Ejemplo rápido curl (usar token de Firebase válido)
- Guardar:
  curl -X PUT "http://localhost:8080/api/perfil/hogar" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '@perfil.json'

- Obtener:
  curl -X GET "http://localhost:8080/api/perfil/hogar" \
  -H "Authorization: Bearer <TOKEN>"
