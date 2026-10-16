-- =====================================================================
-- V4__recetas_adicionales.sql
-- Cocina sin estrés · Carga adicional de recetas para completar catálogo (KAN-42)
--
-- Añade 14 recetas al catálogo, alcanzando ~22 recetas en total.
-- Depende de V2 y V3: usa productos del catálogo global insertados previamente.
-- =====================================================================

SET @sistema = (SELECT id FROM usuario WHERE firebase_uid = 'sistema-carga-inicial');
SET @ultima_receta_v3 = (SELECT COALESCE(MAX(id), 0) FROM receta);
-- Resolver ids de productos usados (debe coincidir con los insert de V2)
SET @arroz     = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Arroz');
SET @lentejas  = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Lentejas');
SET @frijol    = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Frijol');
SET @pasta     = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Pasta');
SET @aceite    = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Aceite vegetal');
SET @sal       = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Sal');
SET @azucar    = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Azúcar');
SET @leche     = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Leche');
SET @queso     = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Queso');
SET @huevos    = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Huevos');
SET @pollo     = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Pollo');
SET @carne     = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Carne de res');
SET @atun      = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Atún');
SET @papa      = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Papa');
SET @tomate    = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Tomate');
SET @cebolla   = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Cebolla');
SET @zanahoria = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Zanahoria');
SET @banano    = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Banano');
SET @manzana   = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Manzana');
SET @naranja   = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Naranja');
SET @tomate_arbol = (SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Tomate de árbol');

-- =====================================================================
-- 9. Arroz con huevo y tomate
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Arroz con huevo y tomate',
           'Plato sencillo de arroz acompañado con huevo frito y tomate salteado.',
           25, 2,
           CONCAT_WS(CHAR(10),
               'Cocina el arroz como en las instrucciones o usando 400 ml de agua por 200 g de arroz.',
               'Corta el tomate en cubos y pica la cebolla.',
               'Calienta 1 cucharada de aceite y sofríe la cebolla 2 minutos, añade el tomate y cocina 4 minutos.',
               'Fríe los huevos al gusto.',
               'Sirve el arroz con el tomate por encima y el huevo al lado.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @arroz,   200, FALSE),
                                                                                (@receta, @huevos,    2, FALSE),
                                                                                (@receta, @tomate,  150, FALSE),
                                                                                (@receta, @cebolla,  50, FALSE),
                                                                                (@receta, @aceite,   15, FALSE),
                                                                                (@receta, @sal,       3, FALSE),
                                                                                (@receta, @queso,    30, TRUE);

-- =====================================================================
-- 10. Ensalada fresca de tomate y cebolla
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Ensalada fresca de tomate y cebolla',
           'Acompañamiento refrescante ideal para comidas y carnes.',
           10, 4,
           CONCAT_WS(CHAR(10),
               'Lava y corta los tomates en cubos; corta la cebolla en juliana fina.',
               'Mezcla en un bol, añade 1-2 cucharadas de aceite, una pizca de sal y remueve.',
               'Deja reposar 5 minutos para que se integren los sabores y sirve.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @tomate,   300, FALSE),
                                                                                (@receta, @cebolla,  100, FALSE),
                                                                                (@receta, @aceite,    15, FALSE),
                                                                                (@receta, @sal,        2, FALSE);

-- =====================================================================
-- 11. Tortilla de papa y cebolla
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Tortilla de papa y cebolla',
           'Plato sencillo a base de huevo y papa, ideal para almuerzos rápidos.',
           30, 4,
           CONCAT_WS(CHAR(10),
               'Pela y corta las papas en cubos pequeños; pica la cebolla.',
               'Hierve las papas 8-10 minutos hasta que estén tiernas; escurre.',
               'Bate los huevos, mezcla con las papas y la cebolla picada, añade sal al gusto.',
               'Vierte en una sartén con aceite caliente y cocina a fuego medio hasta que cuaje por ambos lados.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @papa,     500, FALSE),
                                                                                (@receta, @huevos,     6, FALSE),
                                                                                (@receta, @cebolla,   80, FALSE),
                                                                                (@receta, @aceite,    25, FALSE),
                                                                                (@receta, @sal,        4, FALSE);

-- =====================================================================
-- 12. Pollo guisado con verduras (versión simple)
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Pollo guisado con verduras',
           'Pollo cocinado con tomate, cebolla y zanahoria; rinde para varios días.',
           60, 4,
           CONCAT_WS(CHAR(10),
               'Corta el pollo en piezas y sazónalo con sal.',
               'Dora el pollo en aceite y reserva.',
               'En la misma olla sofríe cebolla y tomate, añade zanahoria en rodajas.',
               'Devuelve el pollo, agrega 400 ml de agua, tapa y cocina 30-35 minutos hasta que esté tierno.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @pollo,     600, FALSE),
                                                                                (@receta, @tomate,    200, FALSE),
                                                                                (@receta, @cebolla,   100, FALSE),
                                                                                (@receta, @zanahoria, 150, FALSE),
                                                                                (@receta, @aceite,     25, FALSE),
                                                                                (@receta, @sal,         6, FALSE);

-- =====================================================================
-- 13. Atún a la plancha con arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Atún a la plancha con arroz',
           'Plato rápido con atún en lata o fresco acompañado de arroz blanco.',
           20, 2,
           CONCAT_WS(CHAR(10),
               'Cocina el arroz según las instrucciones.',
               'Calienta un poco de aceite y cocina el atún escurrido 2-3 minutos por lado si es fresco; si es en lata, solo calentar.',
               'Sirve el atún sobre una porción de arroz y acompaña con rodajas de tomate.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @atun,     2, FALSE),
                                                                                (@receta, @arroz,  200, FALSE),
                                                                                (@receta, @tomate, 100, FALSE),
                                                                                (@receta, @aceite,  10, FALSE),
                                                                                (@receta, @sal,      3, FALSE);

-- =====================================================================
-- 14. Estofado de carne con zanahoria
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Estofado de carne con zanahoria',
           'Guiso de carne con zanahoria y salsa suave, ideal para días fríos.',
           80, 4,
           CONCAT_WS(CHAR(10),
               'Sazona y dora la carne en trozos; retira.',
               'Sofríe cebolla y tomate, añade la carne y 500 ml de agua o caldo; cocina 45 minutos.',
               'Agrega la zanahoria y cocina 20 minutos más hasta que todo esté tierno.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @carne,     500, FALSE),
                                                                                (@receta, @zanahoria, 200, FALSE),
                                                                                (@receta, @tomate,    150, FALSE),
                                                                                (@receta, @cebolla,   100, FALSE),
                                                                                (@receta, @aceite,     20, FALSE),
                                                                                (@receta, @sal,         6, FALSE);

-- =====================================================================
-- 15. Arroz con lentejas
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Arroz con lentejas',
           'Combinación nutritiva de arroz y lentejas, práctica y rendidora.',
           40, 4,
           CONCAT_WS(CHAR(10),
               'Cocina las lentejas según el tiempo indicado (aprox. 25-30 min) o en la misma olla con el arroz si se prefieren tiernas.',
               'Si las cocinas por separado, mezcla con arroz ya cocido y ajusta sal y aceite al gusto.',
               'Sirve caliente.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @arroz,    200, FALSE),
                                                                                (@receta, @lentejas, 200, FALSE),
                                                                                (@receta, @cebolla,   80, FALSE),
                                                                                (@receta, @aceite,    15, FALSE),
                                                                                (@receta, @sal,        4, FALSE);

-- =====================================================================
-- 16. Sopa de pollo con arroz y verduras
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Sopa de pollo con arroz y verduras',
           'Sopa reconfortante con trozos de pollo, arroz y verduras.',
           50, 4,
           CONCAT_WS(CHAR(10),
               'En una olla pon el pollo con agua y una pizca de sal; cocina 20 minutos.',
               'Añade arroz, zanahoria y papa cortada en cubos; cocina 20-25 minutos más.',
               'Rectifica sal y sirve caliente.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @pollo,    400, FALSE),
                                                                                (@receta, @arroz,    150, FALSE),
                                                                                (@receta, @zanahoria,100, FALSE),
                                                                                (@receta, @papa,     200, FALSE),
                                                                                (@receta, @cebolla,   80, FALSE),
                                                                                (@receta, @sal,       4, FALSE);

-- =====================================================================
-- 17. Huevos revueltos con queso
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Huevos revueltos con queso',
           'Desayuno rápido con huevos y queso rallado, cremoso y nutritivo.',
           10, 2,
           CONCAT_WS(CHAR(10),
               'Bate los huevos con una pizca de sal.',
               'Calienta aceite o un poco de mantequilla, vierte los huevos y revuelve a fuego bajo.',
               'Cuando estén casi listos, agrega el queso rallado y mezcla hasta que se funda ligeramente.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @huevos,  4, FALSE),
                                                                                (@receta, @queso,   50, FALSE),
                                                                                (@receta, @aceite,  10, FALSE),
                                                                                (@receta, @sal,      2, FALSE);

-- =====================================================================
-- 18. Papas salteadas con cebolla y huevo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Papas salteadas con cebolla y huevo',
           'Guarnición o plato principal sencillo con papa, cebolla y huevo frito.',
           25, 3,
           CONCAT_WS(CHAR(10),
               'Corta las papas en cubos y fríelas o saltea hasta que estén doradas y tiernas.',
               'Añade la cebolla picada y cocina 3-4 minutos más.',
               'Sirve con huevo frito por encima y una pizca de sal.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @papa,     500, FALSE),
                                                                                (@receta, @cebolla,  80, FALSE),
                                                                                (@receta, @huevos,    3, FALSE),
                                                                                (@receta, @aceite,   30, FALSE),
                                                                                (@receta, @sal,       4, FALSE);

-- =====================================================================
-- 19. Pasta con tomate y queso
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Pasta con tomate y queso',
           'Plato simple de pasta con salsa de tomate casera y queso rallado.',
           25, 4,
           CONCAT_WS(CHAR(10),
               'Cocina la pasta según el empaque y escurre.',
               'Sofríe cebolla y tomate en aceite hasta formar una salsa; ajusta sal.',
               'Mezcla la pasta con la salsa y añade queso rallado al servir.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @pasta,   300, FALSE),
                                                                                (@receta, @tomate,  250, FALSE),
                                                                                (@receta, @cebolla,  80, FALSE),
                                                                                (@receta, @aceite,   15, FALSE),
                                                                                (@receta, @sal,       4, FALSE),
                                                                                (@receta, @queso,    50, TRUE);

-- =====================================================================
-- 20. Ensalada de frutas (banano, manzana y naranja)
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Ensalada de frutas',
           'Postre o merienda fresco con banano, manzana y naranja en trozos.',
           10, 4,
           CONCAT_WS(CHAR(10),
               'Pela y corta las frutas en cubos o gajos.',
               'Mezcla en un bol y sirve fría; si deseas, añade una cucharadita de azúcar o jugo de naranja.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @banano,   3, FALSE),
                                                                                (@receta, @manzana,  2, FALSE),
                                                                                (@receta, @naranja,  2, FALSE),
                                                                                (@receta, @azucar,   10, TRUE);

-- =====================================================================
-- 21. Tomate de árbol en salsa (opción para acompañar)
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Tomate de árbol en salsa',
           'Salsa ácida y dulce de tomate de árbol para acompañar carnes o arroz.',
           20, 4,
           CONCAT_WS(CHAR(10),
               'Pela y corta el tomate de árbol en trozos; cocina con una cucharada de azúcar y sal hasta que se deshaga.',
               'Tritura si deseas una textura más fina y cocina 5 minutos adicionales.',
               'Sirve caliente o frío como acompañamiento.'
           ),
           NULL, TRUE, @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @tomate_arbol, 300, FALSE),
                                                                                (@receta, @azucar,       20, FALSE),
                                                                                (@receta, @sal,           3, FALSE);

-- =====================================================================
-- Auditoría: un registro CREAR por cada receta añadida en esta migración.
-- =====================================================================
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios) SELECT 'RECETA', id, 'CREAR', @sistema, JSON_OBJECT('origen', 'V4__recetas_adicionales') FROM receta WHERE modificado_por = @sistema AND id > @ultima_receta_v3;
