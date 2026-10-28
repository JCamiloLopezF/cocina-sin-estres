-- =====================================================================
-- V17__momentos_y_categorias.sql
-- Cocina sin estrés · En qué comida va cada receta y categorías de una
-- cocina colombiana (KAN-13, RNF-24)
--
-- Momentos de la receta
--   Hasta ahora el plan solo sabía si una receta era "plato fuerte", así que
--   unos huevos pericos podían quedar de almuerzo. Ahora cada receta dice en
--   qué comidas va: para_desayuno, para_almuerzo y para_cena. El generador
--   solo pone en cada comida recetas de ese momento y que sean plato fuerte.
--
--   Regla del almuerzo (docs/adr/ADR-24, sección "Qué es un almuerzo"): un
--   almuerzo es un plato completo, con una proteína (carne, pollo, pescado,
--   huevo o legumbres) y algo que llene (arroz, papa, yuca, plátano, arepa o
--   pasta). Unos huevos solos, una crema o una pasta sin proteína son
--   desayuno o cena. La aplicación valida la regla cuando el curador marca
--   una receta para el almuerzo; ComposicionAlmuerzoIntegracionTest la
--   revisa sobre todo el catálogo.
--
-- Categorías
--   Se agregan LEGUMBRES, TUBERCULOS, HARINAS y BEBIDAS. Los fríjoles y las
--   lentejas pasan a LEGUMBRES y la papa a TUBERCULOS. Toda categoría fuera
--   de la lista pasa a OTROS y un CHECK impide categorías inventadas
--   (auditoría del 27 de octubre: la API aceptaba "Hackeo").
-- =====================================================================

SET @sistema = (
    SELECT id FROM usuario WHERE firebase_uid = 'sistema-carga-inicial'
);

-- ---------------------------------------------------------------------
-- Momentos de cada receta
-- ---------------------------------------------------------------------
ALTER TABLE receta
    ADD COLUMN para_desayuno BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN para_almuerzo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN para_cena     BOOLEAN NOT NULL DEFAULT FALSE;

-- Lo que ya era plato fuerte sigue sirviendo de almuerzo y cena, salvo lo que
-- se corrige abajo con nombre propio.
UPDATE receta
SET para_almuerzo = es_plato_fuerte,
    para_cena     = es_plato_fuerte;

-- Recetas de V3 y V4. Las que no son plato completo dejan de ser almuerzo: los
-- huevos, las cremas, la pasta con queso y los guisos que se sirven sin arroz ni
-- papa (pollo guisado con verduras, estofado con zanahoria) quedan para la cena.
UPDATE receta
SET para_desayuno = nombre IN (
        'Huevos pericos',
        'Huevos revueltos con queso',
        'Tortilla de papa y cebolla',
        'Papas salteadas con cebolla y huevo',
        'Batido de banano y leche',
        'Ensalada de frutas'
    ),
    para_almuerzo = nombre IN (
        'Arroz con pollo y verduras',
        'Lentejas guisadas con papa',
        'Frijoles con arroz',
        'Pasta con atún y tomate',
        'Carne guisada con papa',
        'Atún a la plancha con arroz',
        'Arroz con lentejas',
        'Sopa de pollo con arroz y verduras'
    ),
    para_cena = nombre IN (
        'Arroz con pollo y verduras',
        'Lentejas guisadas con papa',
        'Frijoles con arroz',
        'Huevos pericos',
        'Pasta con atún y tomate',
        'Crema de papa con queso',
        'Carne guisada con papa',
        'Arroz con huevo y tomate',
        'Tortilla de papa y cebolla',
        'Pollo guisado con verduras',
        'Atún a la plancha con arroz',
        'Estofado de carne con zanahoria',
        'Arroz con lentejas',
        'Sopa de pollo con arroz y verduras',
        'Papas salteadas con cebolla y huevo',
        'Pasta con tomate y queso'
    ),
    modificado_por = @sistema
WHERE nombre IN (
    'Arroz con pollo y verduras', 'Lentejas guisadas con papa', 'Frijoles con arroz',
    'Huevos pericos', 'Pasta con atún y tomate', 'Crema de papa con queso',
    'Carne guisada con papa', 'Batido de banano y leche', 'Arroz con huevo y tomate',
    'Ensalada fresca de tomate y cebolla', 'Tortilla de papa y cebolla',
    'Pollo guisado con verduras', 'Atún a la plancha con arroz',
    'Estofado de carne con zanahoria', 'Arroz con lentejas',
    'Sopa de pollo con arroz y verduras', 'Huevos revueltos con queso',
    'Papas salteadas con cebolla y huevo', 'Pasta con tomate y queso',
    'Ensalada de frutas', 'Tomate de árbol en salsa'
);

-- Los desayunos de huevo son plato fuerte de desayuno o cena (el curador los
-- había desmarcado para que no salieran de almuerzo; ahora lo decide el momento).
UPDATE receta
SET es_plato_fuerte = TRUE,
    modificado_por = @sistema
WHERE nombre IN ('Huevos pericos', 'Huevos revueltos con queso', 'Tortilla de papa y cebolla',
                 'Papas salteadas con cebolla y huevo')
  AND es_plato_fuerte = FALSE;

INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'RECETA', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V17__momentos_y_categorias',
                   'campo', 'momentos',
                   'despues', JSON_OBJECT('desayuno', para_desayuno = 1,
                                          'almuerzo', para_almuerzo = 1,
                                          'cena', para_cena = 1))
FROM receta;

-- ---------------------------------------------------------------------
-- Categorías de producto
-- ---------------------------------------------------------------------
UPDATE producto
SET categoria = 'LEGUMBRES', modificado_por = COALESCE(modificado_por, @sistema)
WHERE hogar_id IS NULL AND nombre IN ('Lentejas', 'Frijol') AND categoria = 'GRANOS';

UPDATE producto
SET categoria = 'TUBERCULOS', modificado_por = COALESCE(modificado_por, @sistema)
WHERE hogar_id IS NULL AND nombre = 'Papa' AND categoria = 'VERDURAS';

INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'PRODUCTO', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V17__momentos_y_categorias', 'campo', 'categoria', 'despues', categoria)
FROM producto
WHERE hogar_id IS NULL
  AND ((nombre IN ('Lentejas', 'Frijol') AND categoria = 'LEGUMBRES')
    OR (nombre = 'Papa' AND categoria = 'TUBERCULOS'));

-- Categorías que no son de la lista (texto libre que aceptaba la API) pasan a OTROS.
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'PRODUCTO', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V17__momentos_y_categorias', 'campo', 'categoria',
                   'antes', categoria, 'despues', 'OTROS')
FROM producto
WHERE hogar_id IS NULL
  AND categoria NOT IN ('GRANOS', 'LEGUMBRES', 'TUBERCULOS', 'HARINAS', 'PROTEINAS', 'LACTEOS',
                        'VERDURAS', 'FRUTAS', 'ACEITES', 'CONDIMENTOS', 'BEBIDAS', 'OTROS');

UPDATE producto
SET categoria = 'OTROS'
WHERE categoria NOT IN ('GRANOS', 'LEGUMBRES', 'TUBERCULOS', 'HARINAS', 'PROTEINAS', 'LACTEOS',
                        'VERDURAS', 'FRUTAS', 'ACEITES', 'CONDIMENTOS', 'BEBIDAS', 'OTROS');

ALTER TABLE producto ADD CONSTRAINT ck_producto_categoria CHECK (
    categoria IN ('GRANOS', 'LEGUMBRES', 'TUBERCULOS', 'HARINAS', 'PROTEINAS', 'LACTEOS',
                  'VERDURAS', 'FRUTAS', 'ACEITES', 'CONDIMENTOS', 'BEBIDAS', 'OTROS')
);
