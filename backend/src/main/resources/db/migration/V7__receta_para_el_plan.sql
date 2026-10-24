-- =====================================================================
-- V7__receta_para_el_plan.sql
-- Cocina sin estrés · Datos de la receta que usa "Armar mi semana" (ADR-24)
--
-- es_plato_fuerte: el generador llena el almuerzo y la cena solo con platos
--   fuertes; un batido, una ensalada de frutas o una salsa no son una comida.
--   Por defecto toda receta lo es (también las que cree el curador).
-- gusta_a_ninos: si en el hogar hay niños (12 años o menos), el generador
--   prefiere estas recetas. Es una marca del curador sobre recetas que en
--   general les gustan (sabores suaves, huevo, pasta, arroz, papa); no excluye
--   nada. Por defecto ninguna la tiene.
-- =====================================================================

ALTER TABLE receta
    ADD COLUMN es_plato_fuerte BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN gusta_a_ninos   BOOLEAN NOT NULL DEFAULT FALSE;

SET @sistema = (
    SELECT id FROM usuario WHERE firebase_uid = 'sistema-carga-inicial'
);

UPDATE receta
SET es_plato_fuerte = FALSE,
    modificado_por = @sistema
WHERE nombre IN (
    'Batido de banano y leche',
    'Ensalada de frutas',
    'Ensalada fresca de tomate y cebolla',
    'Tomate de árbol en salsa'
);

UPDATE receta
SET gusta_a_ninos = TRUE,
    modificado_por = @sistema
WHERE nombre IN (
    'Arroz con pollo y verduras',
    'Huevos pericos',
    'Pasta con atún y tomate',
    'Crema de papa con queso',
    'Arroz con huevo y tomate',
    'Tortilla de papa y cebolla',
    'Huevos revueltos con queso',
    'Pasta con tomate y queso',
    'Sopa de pollo con arroz y verduras',
    'Batido de banano y leche',
    'Ensalada de frutas'
);

-- Trazabilidad del catálogo (ADR-23): un registro EDITAR por receta y campo.
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'RECETA', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V7__receta_para_el_plan', 'campo', 'es_plato_fuerte',
                   'antes', TRUE, 'despues', FALSE)
FROM receta
WHERE es_plato_fuerte = FALSE;

INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'RECETA', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V7__receta_para_el_plan', 'campo', 'gusta_a_ninos',
                   'antes', FALSE, 'despues', TRUE)
FROM receta
WHERE gusta_a_ninos = TRUE;
