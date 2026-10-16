-- =====================================================================
-- V3__recetas_iniciales.sql
-- Cocina sin estrés · Carga de las primeras 8 recetas del catálogo (KAN-13)
--
-- Las tablas receta e ingrediente_receta ya existen desde V1; esta migración
-- solo carga datos (CONTRIBUTING.md §6). Depende de V2: los ingredientes son
-- productos del catálogo global que V2 insertó, y el responsable de cada
-- registro es el usuario de sistema 'sistema-carga-inicial'.
--
-- Convenciones de los datos:
--   * receta.pasos: un paso por línea, sin numerar (la aplicación numera).
--   * ingrediente_receta.cantidad: en la unidad base del producto
--     (g, ml o unidad) y para el número de porciones de la receta.
--   * El agua no se lista como ingrediente; va dentro de los pasos.
--   * Si un producto no existe, el INSERT del ingrediente falla (producto_id
--     es NOT NULL) y Flyway cancela la migración: nunca se carga a medias.
-- =====================================================================

SET @sistema = (
    SELECT id FROM usuario WHERE firebase_uid = 'sistema-carga-inicial'
);

-- ---------------------------------------------------------------------
-- Productos del catálogo global que usan las recetas (cargados en V2)
-- ---------------------------------------------------------------------
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

-- =====================================================================
-- 1. Arroz con pollo y verduras
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Arroz con pollo y verduras',
           'Plato completo en una sola olla, con arroz, pollo y zanahoria. Rinde para toda la familia.',
           50,
           4,
           CONCAT_WS(CHAR(10),
               'Pica la cebolla, el tomate y la zanahoria en cubos pequeños.',
               'Corta el pollo en trozos y sazónalo con la mitad de la sal.',
               'Calienta el aceite en una olla, dora el pollo por todos lados y retíralo.',
               'En la misma olla sofríe la cebolla y el tomate hasta que se ablanden.',
               'Agrega el arroz y revuélvelo un minuto para que se cubra con el sofrito.',
               'Devuelve el pollo, añade la zanahoria, la sal restante y 600 ml de agua caliente.',
               'Cuando hierva, baja el fuego, tapa y cocina 20 minutos sin destapar.',
               'Apaga el fuego y deja reposar tapado 5 minutos antes de servir.'
           ),
           NULL,
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @arroz,     300, FALSE),
                                                                                (@receta, @pollo,     400, FALSE),
                                                                                (@receta, @zanahoria, 100, FALSE),
                                                                                (@receta, @cebolla,   100, FALSE),
                                                                                (@receta, @tomate,    150, FALSE),
                                                                                (@receta, @aceite,     30, FALSE),
                                                                                (@receta, @sal,         5, FALSE);

-- =====================================================================
-- 2. Lentejas guisadas con papa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Lentejas guisadas con papa',
           'Guiso económico y nutritivo. No necesita remojo.',
           45,
           4,
           CONCAT_WS(CHAR(10),
               'Enjuaga las lentejas y revisa que no tengan piedritas.',
               'Pela la papa y la zanahoria y pícalas en cubos medianos; pica también la cebolla y el tomate.',
               'Calienta el aceite en una olla y sofríe la cebolla y el tomate 5 minutos.',
               'Agrega las lentejas, la papa, la zanahoria y 1 litro de agua.',
               'Cuando hierva, baja el fuego, tapa y cocina 30 minutos, hasta que las lentejas estén blandas.',
               'Agrega la sal, revuelve y deja 5 minutos más para que espese.'
           ),
           NULL,
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @lentejas,  250, FALSE),
                                                                                (@receta, @papa,      300, FALSE),
                                                                                (@receta, @zanahoria, 100, FALSE),
                                                                                (@receta, @cebolla,   100, FALSE),
                                                                                (@receta, @tomate,    100, FALSE),
                                                                                (@receta, @aceite,     20, FALSE),
                                                                                (@receta, @sal,         5, FALSE);

-- =====================================================================
-- 3. Frijoles con arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Frijoles con arroz',
           'Clásico de la cocina colombiana. Rinde para 6 porciones y se recalienta bien al día siguiente.',
           90,
           6,
           CONCAT_WS(CHAR(10),
               'Escurre los frijoles remojados y ponlos a cocinar en una olla con 2 litros de agua fresca.',
               'Cuando hiervan, baja el fuego, tapa y cocina 60 minutos, hasta que estén blandos.',
               'Mientras tanto, pica la cebolla, el tomate y la zanahoria.',
               'Calienta la mitad del aceite en una sartén y sofríe la cebolla, el tomate y la zanahoria.',
               'Agrega el sofrito a los frijoles junto con la mitad de la sal y cocina 15 minutos más.',
               'Para el arroz, calienta el aceite restante en otra olla, agrega el arroz y revuélvelo un minuto.',
               'Añade 600 ml de agua y la sal restante, tapa y cocina a fuego bajo 20 minutos.',
               'Sirve los frijoles junto al arroz.'
           ),
           'Remojar los frijoles en agua fría desde la noche anterior (mínimo 8 horas).',
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @frijol,    300, FALSE),
                                                                                (@receta, @arroz,     300, FALSE),
                                                                                (@receta, @cebolla,   100, FALSE),
                                                                                (@receta, @tomate,    150, FALSE),
                                                                                (@receta, @zanahoria, 100, FALSE),
                                                                                (@receta, @aceite,     30, FALSE),
                                                                                (@receta, @sal,         6, FALSE);

-- =====================================================================
-- 4. Huevos pericos
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Huevos pericos',
           'Desayuno rápido de huevos revueltos con tomate y cebolla.',
           15,
           3,
           CONCAT_WS(CHAR(10),
               'Pica la cebolla y el tomate en cubos muy pequeños.',
               'Calienta el aceite en una sartén y sofríe la cebolla 2 minutos.',
               'Agrega el tomate y cocina 3 minutos más, hasta que suelte su jugo.',
               'Bate los huevos con la sal y viértelos en la sartén.',
               'Revuelve a fuego bajo hasta que cuajen, sin dejar que se resequen.',
               'Si quieres, sirve con queso rallado por encima.'
           ),
           NULL,
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @huevos,  6, FALSE),
                                                                                (@receta, @tomate, 150, FALSE),
                                                                                (@receta, @cebolla, 80, FALSE),
                                                                                (@receta, @aceite,  15, FALSE),
                                                                                (@receta, @sal,      3, FALSE),
                                                                                (@receta, @queso,   50, TRUE);

-- =====================================================================
-- 5. Pasta con atún y tomate
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Pasta con atún y tomate',
           'Cena rápida con ingredientes de alacena.',
           25,
           4,
           CONCAT_WS(CHAR(10),
               'Hierve 2 litros de agua con la mitad de la sal y cocina la pasta según el empaque; escúrrela.',
               'Pica la cebolla y el tomate.',
               'Calienta el aceite en una sartén y sofríe la cebolla 3 minutos.',
               'Agrega el tomate y cocina 8 minutos, hasta que forme una salsa.',
               'Escurre el atún, agrégalo a la salsa con la sal restante y cocina 2 minutos.',
               'Mezcla la salsa con la pasta y sirve; si quieres, con queso rallado.'
           ),
           NULL,
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @pasta,   300, FALSE),
                                                                                (@receta, @atun,      2, FALSE),
                                                                                (@receta, @tomate,  300, FALSE),
                                                                                (@receta, @cebolla,  80, FALSE),
                                                                                (@receta, @aceite,   20, FALSE),
                                                                                (@receta, @sal,       5, FALSE),
                                                                                (@receta, @queso,    50, TRUE);

-- =====================================================================
-- 6. Crema de papa con queso
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Crema de papa con queso',
           'Crema suave y económica, ideal para la cena.',
           35,
           4,
           CONCAT_WS(CHAR(10),
               'Pela la papa y córtala en cubos; pica la cebolla y, si la usas, la zanahoria.',
               'Calienta el aceite en una olla y sofríe la cebolla 3 minutos.',
               'Agrega la papa, la zanahoria, la sal y 500 ml de agua.',
               'Cocina tapado 20 minutos, hasta que la papa esté blanda.',
               'Licúa todo con la leche hasta obtener una crema suave.',
               'Devuelve la crema a la olla, agrega el queso en trocitos y calienta 3 minutos sin dejar que hierva.'
           ),
           NULL,
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @papa,      600, FALSE),
                                                                                (@receta, @cebolla,   100, FALSE),
                                                                                (@receta, @leche,     500, FALSE),
                                                                                (@receta, @queso,     100, FALSE),
                                                                                (@receta, @aceite,     15, FALSE),
                                                                                (@receta, @sal,         5, FALSE),
                                                                                (@receta, @zanahoria, 100, TRUE);

-- =====================================================================
-- 7. Carne guisada con papa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Carne guisada con papa',
           'Guiso de carne de res con papa y verduras, cocinado a fuego lento.',
           75,
           4,
           CONCAT_WS(CHAR(10),
               'Corta la carne en cubos y sazónala con la mitad de la sal.',
               'Pica la cebolla, el tomate y la zanahoria; pela la papa y córtala en trozos grandes.',
               'Calienta el aceite en una olla y dora la carne por todos lados.',
               'Agrega la cebolla y el tomate y sofríe 5 minutos.',
               'Añade 500 ml de agua, tapa y cocina a fuego bajo 40 minutos, hasta que la carne esté tierna.',
               'Agrega la papa, la zanahoria y la sal restante, y cocina 20 minutos más.',
               'Prueba la sal y sirve caliente.'
           ),
           'Si la carne está congelada, pásala a la nevera la noche anterior para que se descongele.',
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @carne,     500, FALSE),
                                                                                (@receta, @papa,      600, FALSE),
                                                                                (@receta, @cebolla,   100, FALSE),
                                                                                (@receta, @tomate,    150, FALSE),
                                                                                (@receta, @zanahoria, 100, FALSE),
                                                                                (@receta, @aceite,     20, FALSE),
                                                                                (@receta, @sal,         6, FALSE);

-- =====================================================================
-- 8. Batido de banano y leche
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa, modificado_por)
VALUES (
           'Batido de banano y leche',
           'Desayuno o merienda lista en cinco minutos.',
           5,
           2,
           CONCAT_WS(CHAR(10),
               'Pela los bananos y pícalos en trozos.',
               'Licúa los bananos con la leche fría hasta que quede sin grumos.',
               'Prueba y, si quieres más dulce, agrega el azúcar y licúa de nuevo.',
               'Sirve de inmediato.'
           ),
           NULL,
           TRUE,
           @sistema
       );
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional) VALUES
                                                                                (@receta, @banano, 3,   FALSE),
                                                                                (@receta, @leche,  500, FALSE),
                                                                                (@receta, @azucar, 20,  TRUE);

-- =====================================================================
-- Auditoría de la carga inicial: un registro CREAR por receta (RNF-24).
-- Los ingredientes forman parte de la receta y no se auditan por separado.
-- =====================================================================
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'RECETA', id, 'CREAR', @sistema, JSON_OBJECT('origen', 'V3__recetas_iniciales')
FROM receta
WHERE modificado_por = @sistema;
