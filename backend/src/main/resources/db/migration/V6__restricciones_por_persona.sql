-- =====================================================================
-- V6: alergias y condiciones por persona (ajuste de la sustentación del
-- prototipo 2) y productos marcados con sus restricciones.
--
-- - persona.restricciones: lista JSON de códigos de restriccion, cifrada
--   por la aplicación con AES-GCM igual que hogar.restricciones (ADR-20).
--   hogar.restricciones queda como la unión de las de todas las personas.
-- - restriccion: catálogo de códigos que usa el perfil del hogar.
--   ALERGIA excluye la receta para esa persona; CONDICION solo avisa.
-- - producto_restriccion: qué productos del catálogo global tienen cada
--   restricción. Una receta "tiene" una restricción si alguno de sus
--   ingredientes la tiene.
-- =====================================================================

ALTER TABLE persona
    ADD COLUMN restricciones TEXT NULL;

INSERT INTO restriccion (codigo, nombre, tipo)
VALUES
    ('lactosa',      'Lactosa',         'ALERGIA'),
    ('gluten',       'Gluten',          'ALERGIA'),
    ('mani',         'Maní',            'ALERGIA'),
    ('huevo',        'Huevo',           'ALERGIA'),
    ('mariscos',     'Mariscos',        'ALERGIA'),
    ('pescado',      'Pescado',         'ALERGIA'),
    ('hipertension', 'Hipertensión',    'CONDICION'),
    ('diabetes',     'Diabetes',        'CONDICION'),
    ('colesterol',   'Colesterol alto', 'CONDICION');

INSERT INTO producto_restriccion (producto_id, restriccion_id)
SELECT p.id, r.id
FROM producto p
JOIN restriccion r
  ON (r.codigo = 'lactosa' AND p.nombre IN ('Leche', 'Queso'))
  OR (r.codigo = 'gluten'  AND p.nombre IN ('Pasta'))
  OR (r.codigo = 'huevo'   AND p.nombre IN ('Huevos'))
  OR (r.codigo = 'pescado' AND p.nombre IN ('Atún'))
  OR (r.codigo = 'diabetes' AND p.nombre IN ('Azúcar'))
WHERE p.hogar_id IS NULL;
