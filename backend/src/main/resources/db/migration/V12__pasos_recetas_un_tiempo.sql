-- =====================================================================
-- V12: pasos de las recetas más claros para el Modo cocinar (KAN-52)
--
-- En el Modo cocinar cada paso se lee en voz alta y, si dice un tiempo, ofrece
-- el temporizador. Había pasos con dos tiempos ("sofríe la cebolla 2 minutos,
-- añade el tomate y cocina 4 minutos"), rangos ("8-10 minutos") o remisiones
-- ("según el empaque") que el temporizador no sabe contar y que confunden a
-- quien cocina por primera vez. Se reescriben los pasos de las 21 recetas:
--
--   * una acción por paso, en lenguaje sencillo y con la señal de que quedó
--     bien ("hasta que estén blandos");
--   * como máximo UN tiempo por paso, con un solo número ("cocina 20 minutos");
--   * esperar a que hierva va en su propio paso o con "cuando hierva", para
--     que el temporizador se ponga en el momento correcto;
--   * solo ingredientes que están en la receta (sin mantequilla ni caldo).
--
-- No cambian los ingredientes ni las cantidades. V3 y V4 no se tocan: Flyway
-- valida su checksum. PasosRecetasIntegracionTest vigila la regla de un tiempo
-- por paso para las recetas que se carguen después.
-- =====================================================================

SET @sistema = (
    SELECT id
    FROM usuario
    WHERE firebase_uid = 'sistema-carga-inicial'
);

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pica la cebolla, el tomate y la zanahoria en cubos pequeños.',
        'Corta el pollo en trozos medianos y échale la mitad de la sal.',
        'Calienta el aceite en una olla a fuego medio-alto y dora el pollo 8 minutos, volteándolo para que se dore por todos lados.',
        'Saca el pollo de la olla y déjalo en un plato aparte.',
        'En la misma olla, sofríe la cebolla y el tomate 5 minutos, hasta que estén blandos.',
        'Agrega el arroz y revuélvelo con el sofrito hasta que brille.',
        'Vuelve a poner el pollo en la olla. Añade la zanahoria, el resto de la sal y 600 ml de agua caliente (unas 2 tazas y media).',
        'Espera a que hierva. Entonces baja el fuego al mínimo y tapa la olla.',
        'Cocina 20 minutos sin destapar.',
        'Apaga el fuego y deja reposar tapado 5 minutos antes de servir.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Arroz con pollo y verduras';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Enjuaga las lentejas con agua fría y quita las piedritas o los granos dañados.',
        'Pela la papa y la zanahoria y córtalas en cubos medianos.',
        'Pica la cebolla y el tomate.',
        'Calienta el aceite en una olla a fuego medio y sofríe la cebolla y el tomate 5 minutos.',
        'Agrega las lentejas, la papa, la zanahoria y 1 litro de agua. Espera a que hierva.',
        'Baja el fuego, tapa la olla y cocina 30 minutos, hasta que las lentejas estén blandas.',
        'Agrega la sal, revuelve y cocina destapado 5 minutos más para que espese.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Lentejas guisadas con papa';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Escurre los frijoles que dejaste en remojo y ponlos en una olla con 2 litros de agua limpia.',
        'Pon la olla a fuego alto y espera a que hierva.',
        'Baja el fuego, tapa la olla y cocina 60 minutos, hasta que los frijoles estén blandos. Revisa de vez en cuando que no les falte agua.',
        'Mientras los frijoles se cocinan, pica la cebolla, el tomate y la zanahoria.',
        'Calienta la mitad del aceite en una sartén a fuego medio y sofríe la cebolla, el tomate y la zanahoria hasta que estén blandos.',
        'Cuando los frijoles estén blandos, agrégales el sofrito y la mitad de la sal. Déjalos a fuego bajo mientras haces el arroz.',
        'Para el arroz, calienta el resto del aceite en otra olla, agrega el arroz y revuélvelo hasta que brille.',
        'Añade 600 ml de agua y el resto de la sal. Cuando hierva, baja el fuego al mínimo y tapa la olla.',
        'Cocina el arroz 20 minutos sin destapar. Mientras tanto, revuelve los frijoles de vez en cuando para que no se peguen.',
        'Sirve los frijoles con el arroz al lado.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Frijoles con arroz';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pica la cebolla y el tomate en cubos muy pequeños.',
        'Calienta el aceite en una sartén a fuego medio y sofríe la cebolla 2 minutos.',
        'Agrega el tomate y cocínalo 3 minutos, hasta que suelte su jugo.',
        'En un tazón, bate los huevos con la sal.',
        'Vierte los huevos en la sartén, baja el fuego y revuelve sin parar hasta que cuajen. Retíralos antes de que se sequen.',
        'Sirve enseguida. Si quieres, ponles queso rallado por encima.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Huevos pericos';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pica la cebolla y el tomate.',
        'Pon a hervir 2 litros de agua con la mitad de la sal en una olla grande.',
        'Cuando el agua hierva, echa la pasta y cocínala 10 minutos, revolviendo de vez en cuando. Debe quedar blanda pero firme.',
        'Mientras la pasta se cocina, calienta el aceite en una sartén a fuego medio y sofríe la cebolla 3 minutos.',
        'Agrega el tomate y cocina 8 minutos, hasta que se forme una salsa.',
        'Abre las latas de atún, escúrrelas y agrega el atún a la salsa con el resto de la sal. Cocina 2 minutos más.',
        'Escurre la pasta y mézclala con la salsa.',
        'Sirve caliente. Si quieres, ponle queso rallado por encima.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Pasta con atún y tomate';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pela la papa y córtala en cubos. Pica la cebolla y, si la vas a usar, la zanahoria.',
        'Calienta el aceite en una olla a fuego medio y sofríe la cebolla 3 minutos.',
        'Agrega la papa, la zanahoria, la sal y 500 ml de agua.',
        'Tapa la olla y cocina 20 minutos, hasta que la papa se deshaga al pincharla con un tenedor.',
        'Deja que se enfríe un poco y licúa todo con la leche hasta que quede una crema suave. Ten cuidado: lo caliente salpica.',
        'Vierte la crema de nuevo en la olla y agrega el queso en trocitos.',
        'Calienta a fuego bajo 3 minutos, revolviendo, sin dejar que hierva.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Crema de papa con queso';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Corta la carne en cubos y échale la mitad de la sal.',
        'Pica la cebolla, el tomate y la zanahoria. Pela la papa y córtala en trozos grandes.',
        'Calienta el aceite en una olla a fuego medio-alto y dora la carne 8 minutos, volteándola para que se dore por todos lados.',
        'Agrega la cebolla y el tomate y sofríe 5 minutos.',
        'Añade 500 ml de agua y espera a que hierva.',
        'Baja el fuego, tapa la olla y cocina 40 minutos, hasta que la carne esté blandita.',
        'Agrega la papa, la zanahoria y el resto de la sal. Tapa y cocina 20 minutos más, hasta que la papa esté blanda.',
        'Prueba, ajusta la sal si hace falta y sirve caliente.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Carne guisada con papa';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pela los bananos y córtalos en trozos.',
        'Ponlos en la licuadora con la leche fría y licúa hasta que no queden grumos.',
        'Prueba. Si lo quieres más dulce, agrega el azúcar y licúa otra vez.',
        'Sirve enseguida, antes de que el banano se oscurezca.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Batido de banano y leche';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'En una olla, calienta un chorrito del aceite a fuego medio, agrega el arroz y revuélvelo hasta que brille.',
        'Añade 400 ml de agua y una pizca de la sal. Cuando hierva, baja el fuego al mínimo y tapa la olla.',
        'Cocina el arroz 20 minutos sin destapar.',
        'Mientras el arroz se cocina, corta el tomate en cubos y pica la cebolla.',
        'En una sartén, calienta otro chorrito de aceite y sofríe la cebolla 2 minutos.',
        'Agrega el tomate y cocina 4 minutos, hasta que se ablande. Pásalo a un plato.',
        'En la misma sartén, con el aceite que queda, fríe los huevos a tu gusto y échales el resto de la sal.',
        'Sirve el arroz con el tomate por encima y el huevo al lado. Si quieres, agrega queso rallado.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Arroz con huevo y tomate';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Lava los tomates y córtalos en cubos.',
        'Corta la cebolla en tiras muy delgadas.',
        'Pon todo en un tazón, agrega el aceite y la sal, y revuelve con cuidado.',
        'Deja reposar 5 minutos para que se mezclen los sabores, y sirve.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Ensalada fresca de tomate y cebolla';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pela las papas y córtalas en cubos pequeños. Pica la cebolla.',
        'Pon las papas en una olla con agua y una pizca de la sal. Cuando hierva, cocínalas 10 minutos, hasta que estén blandas.',
        'Escurre las papas y deja que se enfríen un poco.',
        'En un tazón grande, bate los huevos con el resto de la sal. Agrega las papas y la cebolla, y mezcla.',
        'Calienta el aceite en una sartén a fuego medio y vierte la mezcla.',
        'Baja el fuego y cocina 6 minutos, hasta que los bordes estén firmes.',
        'Pon un plato encima de la sartén, voltea la tortilla sobre el plato con cuidado y deslízala otra vez a la sartén.',
        'Cocina el otro lado 4 minutos y sirve.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Tortilla de papa y cebolla';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Corta el pollo en presas y échale la mitad de la sal.',
        'Pica la cebolla y el tomate, y corta la zanahoria en rodajas.',
        'Calienta el aceite en una olla a fuego medio-alto y dora el pollo 8 minutos, volteándolo para que se dore por todos lados.',
        'Saca el pollo de la olla y déjalo en un plato aparte.',
        'En la misma olla, sofríe la cebolla y el tomate 5 minutos.',
        'Agrega la zanahoria, vuelve a poner el pollo en la olla y añade 400 ml de agua con el resto de la sal.',
        'Cuando hierva, baja el fuego, tapa la olla y cocina 35 minutos, hasta que el pollo esté blandito.',
        'Sirve caliente, con su caldito.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Pollo guisado con verduras';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'En una olla, calienta la mitad del aceite a fuego medio, agrega el arroz y revuélvelo hasta que brille.',
        'Añade 400 ml de agua y la mitad de la sal. Cuando hierva, baja el fuego al mínimo y tapa la olla.',
        'Cocina el arroz 20 minutos sin destapar.',
        'Mientras tanto, corta el tomate en rodajas y échale una pizca de sal.',
        'Abre las latas de atún y escúrrelas bien.',
        'Calienta el resto del aceite en una sartén y calienta el atún 3 minutos, moviéndolo con cuidado para que no se desbarate.',
        'Sirve el arroz con el atún encima y las rodajas de tomate al lado.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Atún a la plancha con arroz';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Corta la carne en trozos y échale la mitad de la sal.',
        'Pica la cebolla y el tomate, y corta la zanahoria en rodajas gruesas.',
        'Calienta el aceite en una olla a fuego medio-alto y dora la carne 8 minutos, volteándola para que se dore por todos lados.',
        'Saca la carne de la olla y déjala en un plato aparte.',
        'En la misma olla, sofríe la cebolla y el tomate 5 minutos.',
        'Vuelve a poner la carne en la olla, añade 500 ml de agua y espera a que hierva.',
        'Baja el fuego, tapa la olla y cocina 45 minutos.',
        'Agrega la zanahoria y el resto de la sal. Tapa y cocina 20 minutos más, hasta que todo esté blandito.',
        'Sirve caliente.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Estofado de carne con zanahoria';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Enjuaga las lentejas con agua fría y quita las piedritas.',
        'Pon las lentejas en una olla con 1 litro de agua. Cuando hierva, baja el fuego y cocina 15 minutos.',
        'Mientras tanto, pica la cebolla.',
        'Calienta el aceite en una sartén a fuego medio y sofríe la cebolla 3 minutos, hasta que esté transparente.',
        'Agrega a las lentejas el arroz, la cebolla y la sal, y revuelve. El agua debe cubrir todo por un dedo; si falta, agrega un poco.',
        'Tapa la olla y cocina a fuego bajo 20 minutos, hasta que el arroz y las lentejas estén blandos.',
        'Apaga el fuego, deja reposar tapado 5 minutos y sirve.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Arroz con lentejas';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Corta el pollo en presas y pica la cebolla.',
        'Pon el pollo y la cebolla en una olla con 1,5 litros de agua y la mitad de la sal. Espera a que hierva.',
        'Baja el fuego, tapa la olla y cocina 20 minutos.',
        'Mientras tanto, pela la papa y córtala en cubos, y corta la zanahoria en rodajas.',
        'Agrega a la olla el arroz, la papa y la zanahoria.',
        'Cocina destapado a fuego medio 20 minutos, hasta que el arroz y la papa estén blandos. Revuelve de vez en cuando para que el arroz no se pegue.',
        'Prueba, ajusta con el resto de la sal si hace falta y sirve caliente.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Sopa de pollo con arroz y verduras';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Ralla o desmenuza el queso.',
        'En un tazón, bate los huevos con la sal.',
        'Calienta el aceite en una sartén a fuego bajo y vierte los huevos.',
        'Revuelve despacio hasta que estén casi cuajados.',
        'Agrega el queso, revuelve 1 minuto más para que se derrita y sirve.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Huevos revueltos con queso';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pela las papas y córtalas en cubos pequeños. Pica la cebolla.',
        'Calienta casi todo el aceite en una sartén grande a fuego medio. Guarda un poquito para los huevos.',
        'Agrega las papas y cocínalas 15 minutos, moviéndolas de vez en cuando, hasta que estén doradas y blandas.',
        'Agrega la cebolla y la mitad de la sal, y cocina 4 minutos más.',
        'Pasa las papas a los platos. En la misma sartén, con el aceite que guardaste, fríe los huevos.',
        'Pon un huevo encima de cada plato, échales el resto de la sal y sirve.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Papas salteadas con cebolla y huevo';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pica la cebolla y el tomate.',
        'Pon a hervir 2 litros de agua con la mitad de la sal en una olla grande.',
        'Cuando el agua hierva, echa la pasta y cocínala 10 minutos, revolviendo de vez en cuando. Debe quedar blanda pero firme.',
        'Mientras la pasta se cocina, calienta el aceite en una sartén a fuego medio y sofríe la cebolla 3 minutos.',
        'Agrega el tomate y el resto de la sal, y cocina 8 minutos, hasta que se forme una salsa.',
        'Escurre la pasta y mézclala con la salsa.',
        'Sirve caliente. Si quieres, ponle queso rallado por encima.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Pasta con tomate y queso';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Lava las manzanas. Pela los bananos y las naranjas.',
        'Corta las manzanas y los bananos en cubos, y separa las naranjas en gajos sin semillas.',
        'Mezcla todas las frutas en un tazón. Exprime encima un gajo de naranja para que la manzana y el banano no se oscurezcan.',
        'Si la quieres más dulce, espolvorea el azúcar y revuelve.',
        'Sirve enseguida o guárdala en la nevera hasta la hora de comer.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Ensalada de frutas';

UPDATE receta
SET pasos = CONCAT_WS(CHAR(10),
        'Pon a hervir agua en una olla.',
        'Cuando hierva, sumerge los tomates de árbol 2 minutos, para que la cáscara salga fácil.',
        'Sácalos del agua, pélalos, quítales el tallo y córtalos en trozos.',
        'Ponlos en una olla pequeña con el azúcar, la sal y medio vaso de agua.',
        'Cocina a fuego bajo 10 minutos, revolviendo, hasta que el tomate se deshaga.',
        'Si la quieres más suave, tritúrala con un tenedor o pásala por la licuadora.',
        'Cocina 5 minutos más para que espese. Sirve caliente o fría, como acompañamiento.'
    ),
    modificado_por = @sistema
WHERE nombre = 'Tomate de árbol en salsa';

-- Trazabilidad del catálogo (ADR-23): un registro EDITAR por receta cambiada.
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'RECETA', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V12__pasos_recetas_un_tiempo', 'campo', 'pasos')
FROM receta
WHERE nombre IN (
    'Arroz con pollo y verduras',
    'Lentejas guisadas con papa',
    'Frijoles con arroz',
    'Huevos pericos',
    'Pasta con atún y tomate',
    'Crema de papa con queso',
    'Carne guisada con papa',
    'Batido de banano y leche',
    'Arroz con huevo y tomate',
    'Ensalada fresca de tomate y cebolla',
    'Tortilla de papa y cebolla',
    'Pollo guisado con verduras',
    'Atún a la plancha con arroz',
    'Estofado de carne con zanahoria',
    'Arroz con lentejas',
    'Sopa de pollo con arroz y verduras',
    'Huevos revueltos con queso',
    'Papas salteadas con cebolla y huevo',
    'Pasta con tomate y queso',
    'Ensalada de frutas',
    'Tomate de árbol en salsa'
);
