-- =====================================================================
-- V5: pasos de receta más claros (ajuste de la sustentación del prototipo 2)
--
-- "Devuelve el pollo" quería decir "vuelve a poner el pollo en la olla"
-- (se había sacado en un paso anterior), pero se leía como un error. Se
-- reescriben los tres pasos con la acción explícita. V3 y V4 no se tocan:
-- Flyway valida su checksum.
-- =====================================================================

SET @sistema = (
    SELECT id
    FROM usuario
    WHERE firebase_uid = 'sistema-carga-inicial'
);

UPDATE receta
SET pasos = REPLACE(pasos,
        'Devuelve el pollo, añade la zanahoria',
        'Vuelve a poner el pollo en la olla, añade la zanahoria'),
    modificado_por = @sistema
WHERE pasos LIKE '%Devuelve el pollo, añade la zanahoria%';

UPDATE receta
SET pasos = REPLACE(pasos,
        'Devuelve el pollo, agrega 400 ml de agua',
        'Vuelve a poner el pollo en la olla, agrega 400 ml de agua'),
    modificado_por = @sistema
WHERE pasos LIKE '%Devuelve el pollo, agrega 400 ml de agua%';

UPDATE receta
SET pasos = REPLACE(pasos,
        'Devuelve la crema a la olla',
        'Vierte la crema de nuevo en la olla'),
    modificado_por = @sistema
WHERE pasos LIKE '%Devuelve la crema a la olla%';

-- Trazabilidad del catálogo (ADR-23): un registro EDITAR por receta cambiada.
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'RECETA', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V5__aclarar_pasos_recetas', 'campo', 'pasos')
FROM receta
WHERE pasos LIKE '%Vuelve a poner el pollo en la olla%'
   OR pasos LIKE '%Vierte la crema de nuevo en la olla%';
