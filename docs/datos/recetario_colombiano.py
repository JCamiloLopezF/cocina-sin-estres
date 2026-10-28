# Genera V18__recetario_colombiano.sql y valida los datos antes de escribirlo.
import re, sys

PRODUCTOS_EXISTENTES = {
    # nombre: (categoria, unidad, cantidad_compra, precio, restricciones)
    'Arroz': ('GRANOS', 'g', 1000, 4500, []),
    'Lentejas': ('LEGUMBRES', 'g', 500, 4000, []),
    'Frijol': ('LEGUMBRES', 'g', 500, 5500, []),
    'Pasta': ('GRANOS', 'g', 500, 3500, ['gluten']),
    'Aceite vegetal': ('ACEITES', 'ml', 1000, 8000, []),
    'Sal': ('CONDIMENTOS', 'g', 500, 2000, []),
    'Azúcar': ('CONDIMENTOS', 'g', 1000, 4500, ['diabetes']),
    'Leche': ('LACTEOS', 'ml', 1000, 4500, ['lactosa']),
    'Queso': ('LACTEOS', 'g', 250, 7000, ['lactosa']),
    'Huevos': ('PROTEINAS', 'unidad', 30, 18000, ['huevo']),
    'Pollo': ('PROTEINAS', 'g', 500, 12000, []),
    'Carne de res': ('PROTEINAS', 'g', 500, 18000, []),
    'Atún': ('PROTEINAS', 'unidad', 1, 6000, ['pescado']),
    'Papa': ('TUBERCULOS', 'g', 500, 3500, []),
    'Tomate': ('VERDURAS', 'g', 500, 4000, []),
    'Cebolla': ('VERDURAS', 'g', 500, 3500, []),
    'Zanahoria': ('VERDURAS', 'g', 500, 3000, []),
    'Banano': ('FRUTAS', 'unidad', 6, 5000, []),
    'Manzana': ('FRUTAS', 'unidad', 1, 1500, []),
    'Naranja': ('FRUTAS', 'unidad', 1, 1000, []),
    'Tomate de árbol': ('FRUTAS', 'g', 500, 5000, []),
}

# nombre, categoria, tipo, unidad, compra, cantidad, precio, basico, vida_util, restricciones
PRODUCTOS_NUEVOS = [
    ('Garbanzo', 'LEGUMBRES', 'GRANEL', 'g', 'Bolsa', 500, 6000, False, 365, []),
    ('Papa criolla', 'TUBERCULOS', 'GRANEL', 'g', 'Libra', 500, 4000, False, 10, []),
    ('Yuca', 'TUBERCULOS', 'GRANEL', 'g', 'Libra', 500, 2500, False, 7, []),
    ('Plátano verde', 'TUBERCULOS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500, False, 7, []),
    ('Plátano maduro', 'TUBERCULOS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500, False, 5, []),
    ('Arepa de maíz', 'HARINAS', 'CONTABLE', 'unidad', 'Paquete', 5, 4500, True, 10, []),
    ('Harina de maíz precocida', 'HARINAS', 'GRANEL', 'g', 'Paquete', 1000, 5500, False, 365, []),
    ('Harina de trigo', 'HARINAS', 'GRANEL', 'g', 'Libra', 500, 2800, False, 180, ['gluten']),
    ('Pan', 'HARINAS', 'CONTABLE', 'unidad', 'Bolsa', 10, 5000, False, 5, ['gluten']),
    ('Avena en hojuelas', 'GRANOS', 'GRANEL', 'g', 'Bolsa', 500, 4500, False, 365, ['gluten']),
    ('Carne molida', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 14000, False, 2, []),
    ('Carne de cerdo', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 13000, False, 3, []),
    ('Costilla de res', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 11000, False, 3, []),
    ('Chorizo', 'PROTEINAS', 'CONTABLE', 'unidad', 'Paquete', 5, 9000, False, 15, ['colesterol', 'hipertension']),
    ('Filete de tilapia', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 15000, False, 2, ['pescado']),
    ('Sardinas en lata', 'PROTEINAS', 'CONTABLE', 'unidad', 'Lata', 1, 5500, False, 730, ['pescado']),
    ('Crema de leche', 'LACTEOS', 'GRANEL', 'ml', 'Caja', 200, 4200, False, 15, ['lactosa']),
    ('Mantequilla', 'LACTEOS', 'GRANEL', 'g', 'Barra', 125, 4500, False, 30, ['lactosa']),
    ('Ajo', 'VERDURAS', 'GRANEL', 'g', 'Cabeza', 50, 1000, True, 30, []),
    ('Cebolla larga', 'VERDURAS', 'GRANEL', 'g', 'Atado', 250, 2000, True, 7, []),
    ('Cilantro', 'VERDURAS', 'GRANEL', 'g', 'Atado', 50, 1000, False, 5, []),
    ('Pimentón', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500, False, 10, []),
    ('Habichuela', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 3500, False, 7, []),
    ('Arveja verde', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 5000, False, 5, []),
    ('Mazorca', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500, False, 5, []),
    ('Repollo', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 3000, False, 14, []),
    ('Lechuga', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 2500, False, 5, []),
    ('Ahuyama', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 2000, False, 14, []),
    ('Guascas', 'VERDURAS', 'GRANEL', 'g', 'Atado', 20, 1500, False, 7, []),
    ('Limón', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 400, False, 14, []),
    ('Aguacate', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 3000, False, 4, []),
    ('Mora', 'FRUTAS', 'GRANEL', 'g', 'Libra', 500, 5000, False, 4, []),
    ('Lulo', 'FRUTAS', 'GRANEL', 'g', 'Libra', 500, 4500, False, 7, []),
    ('Mango', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 2000, False, 6, []),
    ('Papaya', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 5000, False, 5, []),
    ('Piña', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 5000, False, 6, []),
    ('Panela', 'CONDIMENTOS', 'GRANEL', 'g', 'Bloque', 500, 3000, True, 180, ['diabetes']),
    ('Comino', 'CONDIMENTOS', 'GRANEL', 'g', 'Sobre', 30, 1500, False, 365, []),
    ('Color (achiote)', 'CONDIMENTOS', 'GRANEL', 'g', 'Sobre', 30, 1200, False, 365, []),
    ('Chocolate de mesa', 'BEBIDAS', 'GRANEL', 'g', 'Paquete', 250, 6500, False, 365, ['diabetes']),
    ('Café molido', 'BEBIDAS', 'GRANEL', 'g', 'Paquete', 250, 9000, True, 180, []),
    ('Arracacha', 'TUBERCULOS', 'GRANEL', 'g', 'Libra', 500, 3000, False, 7, []),
    ('Hígado de res', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 8000, False, 2, ['colesterol']),
    ('Maracuyá', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1200, False, 7, []),
]

CATALOGO = dict(PRODUCTOS_EXISTENTES)
for p in PRODUCTOS_NUEVOS:
    CATALOGO[p[0]] = (p[1], p[3], p[5], p[6], p[9])

# Receta: nombre, descripcion, tiempo, porciones, momentos (D/A/C), plato_fuerte, ninos,
# preparacion_previa, ingredientes [(producto, cantidad, opcional, principal)], pasos
R = []


def receta(nombre, descripcion, tiempo, momentos, ingredientes, pasos, porciones=4, fuerte=True,
           ninos=False, previa=None):
    R.append(dict(nombre=nombre, descripcion=descripcion, tiempo=tiempo, porciones=porciones,
                  momentos=momentos, fuerte=fuerte, ninos=ninos, previa=previa,
                  ingredientes=ingredientes, pasos=pasos))


def i(producto, cantidad, principal=False, opcional=False):
    return (producto, cantidad, opcional, principal)


# ---------------------------------------------------------------- Almuerzos
receta('Bandeja de fríjoles con carne molida y maduro',
       'El almuerzo de la casa: fríjoles con hogao, arroz, carne molida, tajadas de maduro y aguacate.',
       100, 'A',
       [i('Frijol', 500, True), i('Arroz', 400), i('Carne molida', 400, True), i('Plátano maduro', 2),
        i('Aguacate', 1), i('Tomate', 150), i('Cebolla larga', 100), i('Ajo', 10), i('Comino', 2),
        i('Aceite vegetal', 60), i('Sal', 12)],
       ['Escurre los fríjoles que dejaste en remojo y ponlos en una olla con 2 litros de agua limpia.',
        'Cuando hierva, baja el fuego, tapa la olla y cocina 1 hora, hasta que los fríjoles estén blandos.',
        'Mientras se cocinan, pica el tomate, la cebolla larga y el ajo.',
        'En una sartén con una cucharada de aceite, sofríe el tomate, la cebolla y el ajo 5 minutos. Eso es el hogao.',
        'Pasa la mitad del hogao a los fríjoles con el comino y la mitad de la sal. Cocina destapado 20 minutos para que espesen.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y un poco de sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'En la sartén, dora la carne molida con el resto del hogao y de la sal durante 10 minutos, revolviendo para que se suelte.',
        'Pela los plátanos maduros, córtalos en tajadas y fríelos en el aceite que queda hasta que estén dorados por ambos lados.',
        'Sirve en cada plato arroz, fríjoles, carne, tajadas y una tajada de aguacate.'],
       previa='Deja los fríjoles en remojo con agua la noche anterior.')

receta('Sancocho de pollo',
       'Sopa espesa de domingo con pollo, papa, yuca, plátano y mazorca. Se acompaña con arroz y aguacate.',
       75, 'A',
       [i('Pollo', 800, True), i('Papa', 500), i('Yuca', 500), i('Plátano verde', 2), i('Mazorca', 2),
        i('Cebolla larga', 100), i('Ajo', 10), i('Cilantro', 20), i('Arroz', 300), i('Aguacate', 1, opcional=True),
        i('Aceite vegetal', 15), i('Sal', 12)],
       ['Corta el pollo en presas y ponlo en una olla grande con 3 litros de agua, la cebolla larga entera, el ajo machacado y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos, quitando la espuma de encima.',
        'Pela el plátano verde y pártelo en trozos con la mano. Corta las mazorcas en ruedas.',
        'Agrega el plátano y la mazorca a la olla y cocina 15 minutos.',
        'Pela la yuca y la papa y córtalas en trozos grandes.',
        'Agrega la yuca y la papa y cocina 25 minutos, hasta que la yuca esté blanda.',
        'Mientras tanto, lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Saca la cebolla larga del sancocho, pica el cilantro y échalo al final.',
        'Sirve el sancocho con arroz aparte y, si tienes, una tajada de aguacate.'],
       ninos=True)

receta('Ajiaco santafereño',
       'Sopa bogotana de pollo con tres papas, mazorca y guascas. Se sirve con arroz, aguacate y crema.',
       80, 'A',
       [i('Pollo', 600, True), i('Papa', 500, True), i('Papa criolla', 500), i('Mazorca', 2), i('Guascas', 10),
        i('Cebolla larga', 100), i('Ajo', 10), i('Arroz', 300), i('Aguacate', 1),
        i('Crema de leche', 200, opcional=True), i('Aceite vegetal', 15), i('Sal', 10)],
       ['Pon el pollo en una olla con 3 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 25 minutos.',
        'Mientras tanto, pela la papa y la papa criolla y córtalas en rodajas delgadas. Corta las mazorcas en ruedas.',
        'Saca el pollo de la olla y déjalo enfriar en un plato.',
        'Echa a la olla la papa, la papa criolla y la mazorca y cocina 30 minutos, hasta que la papa criolla se deshaga y espese la sopa.',
        'Desmecha el pollo con los dedos y devuélvelo a la olla.',
        'Agrega las guascas y cocina 5 minutos más.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve el ajiaco con arroz, aguacate y, si quieres, una cucharada de crema de leche.'])

receta('Sudado de pollo con papa y arroz',
       'Pollo cocinado en su jugo con tomate, cebolla y papa, servido con arroz blanco.',
       60, 'AC',
       [i('Pollo', 700, True), i('Papa', 600), i('Tomate', 300), i('Cebolla', 150), i('Ajo', 10),
        i('Color (achiote)', 3), i('Comino', 2), i('Cilantro', 10), i('Arroz', 400), i('Aceite vegetal', 45), i('Sal', 12)],
       ['Corta el pollo en presas y échale la mitad de la sal, el comino y el color.',
        'Pica el tomate, la cebolla y el ajo. Pela la papa y córtala en cuartos.',
        'Calienta 2 cucharadas de aceite en una olla y sofríe la cebolla, el tomate y el ajo 5 minutos.',
        'Agrega el pollo, revuelve con el sofrito, tapa y cocina 15 minutos a fuego medio.',
        'Añade la papa, el resto de la sal y 300 ml de agua. Tapa y cocina 25 minutos, hasta que la papa esté blanda.',
        'Mientras tanto, lava el arroz y ponlo en otra olla con 800 ml de agua y el resto del aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el cilantro, échalo sobre el sudado y sirve con el arroz.'],
       ninos=True)

receta('Carne en bistec con arroz y papa',
       'Carne de res en salsa de tomate y cebolla, con arroz y papa cocida.',
       55, 'AC',
       [i('Carne de res', 600, True), i('Cebolla', 200), i('Tomate', 300), i('Ajo', 10), i('Comino', 2),
        i('Arroz', 400), i('Papa', 500), i('Aceite vegetal', 50), i('Sal', 12)],
       ['Corta la carne en filetes delgados y adóbala con el ajo machacado, el comino y la mitad de la sal.',
        'Pela la papa, córtala en mitades y ponla en una olla con agua y un poco de sal.',
        'Cuando el agua hierva, cocina la papa 20 minutos, hasta que esté blanda.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua y una cucharada de aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Corta la cebolla en aros y el tomate en cubos.',
        'Calienta el resto del aceite en una sartén y dora los filetes 3 minutos por cada lado. Sácalos.',
        'En la misma sartén sofríe la cebolla y el tomate con el resto de la sal 5 minutos.',
        'Devuelve la carne, agrega medio pocillo de agua, tapa y cocina 10 minutos.',
        'Sirve el bistec con su salsa, el arroz y la papa.'])

receta('Lentejas con chorizo, arroz y maduro',
       'Lentejas espesas con hogao y chorizo, con arroz blanco y tajadas de maduro.',
       60, 'A',
       [i('Lentejas', 400, True), i('Chorizo', 2), i('Arroz', 400), i('Plátano maduro', 2), i('Tomate', 150),
        i('Cebolla', 100), i('Ajo', 10), i('Comino', 2), i('Aceite vegetal', 60), i('Sal', 12)],
       ['Lava las lentejas y quita las piedritas.',
        'Ponlas en una olla con 1 litro y medio de agua. Cuando hierva, baja el fuego y cocina 25 minutos.',
        'Pica el tomate, la cebolla, el ajo y el chorizo en rodajas.',
        'En una sartén con una cucharada de aceite, fríe el chorizo con la cebolla, el tomate y el ajo 6 minutos.',
        'Pasa el sofrito a las lentejas con el comino y la mitad de la sal y cocina destapado 10 minutos para que espesen.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los maduros, córtalos en tajadas y fríelos en el aceite que queda hasta que estén dorados.',
        'Sirve las lentejas con el arroz y las tajadas.'])

receta('Tilapia frita con patacón y arroz',
       'Pescado frito con limón, patacones, arroz y ensalada de lechuga y tomate.',
       50, 'A',
       [i('Filete de tilapia', 600, True), i('Plátano verde', 2), i('Arroz', 400), i('Limón', 2), i('Ajo', 10),
        i('Lechuga', 1), i('Tomate', 150), i('Aceite vegetal', 150), i('Sal', 10)],
       ['Adoba los filetes con el ajo machacado, el jugo de un limón y la mitad de la sal.',
        'Lava el arroz y ponlo en una olla con 800 ml de agua y una cucharada de aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los plátanos verdes y córtalos en trozos de dos dedos de grueso.',
        'Fríe los trozos de plátano en aceite caliente 4 minutos, hasta que estén amarillos.',
        'Sácalos, aplástalos con el fondo de un plato y vuelve a freírlos hasta que estén dorados. Échales sal.',
        'En una sartén con poco aceite, fríe los filetes 4 minutos por cada lado.',
        'Pica la lechuga y el tomate y aliña con el otro limón.',
        'Sirve el pescado con patacones, arroz y la ensalada.'])

receta('Cerdo en salsa criolla con arroz',
       'Trozos de cerdo en salsa de tomate, pimentón y cebolla, con arroz y ensalada de repollo.',
       60, 'AC',
       [i('Carne de cerdo', 600, True), i('Tomate', 300), i('Cebolla', 150), i('Pimentón', 1), i('Ajo', 10),
        i('Comino', 2), i('Arroz', 400), i('Repollo', 0.5), i('Limón', 1), i('Aceite vegetal', 45), i('Sal', 12)],
       ['Corta el cerdo en cubos y échale el ajo machacado, el comino y la mitad de la sal.',
        'Pica el tomate, la cebolla y el pimentón.',
        'Calienta 2 cucharadas de aceite en una olla y dora el cerdo 8 minutos.',
        'Agrega el tomate, la cebolla y el pimentón, revuelve y añade 200 ml de agua.',
        'Tapa y cocina a fuego bajo 30 minutos, hasta que la carne esté blanda y la salsa espesa.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Corta el repollo bien delgado y aliñalo con el jugo del limón.',
        'Sirve el cerdo con el arroz y la ensalada de repollo.'])

receta('Albóndigas en salsa con arroz',
       'Albóndigas de carne molida en salsa de tomate casera, con arroz blanco. Les encantan a los niños.',
       50, 'AC',
       [i('Carne molida', 500, True), i('Huevos', 1), i('Harina de maíz precocida', 40), i('Tomate', 400),
        i('Cebolla', 150), i('Ajo', 10), i('Comino', 2), i('Arroz', 400), i('Aceite vegetal', 45), i('Sal', 12)],
       ['Pica muy fino la mitad de la cebolla y el ajo.',
        'En un tazón mezcla la carne molida, el huevo, la harina de maíz, la cebolla picada, el ajo, el comino y la mitad de la sal.',
        'Forma bolitas del tamaño de una nuez.',
        'Licúa el tomate con el resto de la cebolla y 100 ml de agua.',
        'Calienta 2 cucharadas de aceite en una olla y dora las albóndigas 6 minutos, moviéndolas con cuidado.',
        'Echa la salsa licuada, tapa y cocina a fuego bajo 20 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve las albóndigas con su salsa sobre el arroz.'],
       ninos=True)

receta('Garbanzos guisados con cerdo y arroz',
       'Garbanzos con cerdo, zanahoria y hogao, servidos con arroz.',
       90, 'A',
       [i('Garbanzo', 400, True), i('Carne de cerdo', 300), i('Zanahoria', 150), i('Tomate', 200), i('Cebolla', 100),
        i('Ajo', 10), i('Comino', 2), i('Arroz', 400), i('Aceite vegetal', 45), i('Sal', 12)],
       ['Escurre los garbanzos que dejaste en remojo y ponlos en una olla con 2 litros de agua limpia.',
        'Cuando hierva, baja el fuego, tapa y cocina 50 minutos, hasta que estén blandos.',
        'Corta el cerdo en cubos pequeños, la zanahoria en rodajas y pica el tomate, la cebolla y el ajo.',
        'En una sartén con 2 cucharadas de aceite, dora el cerdo 8 minutos.',
        'Agrega el tomate, la cebolla y el ajo al cerdo y sofríe 5 minutos.',
        'Pasa el cerdo con el sofrito a los garbanzos, añade la zanahoria, el comino y la mitad de la sal. Cocina 15 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve los garbanzos con el arroz.'],
       previa='Deja los garbanzos en remojo con agua la noche anterior.')

receta('Pollo asado en sartén con papa criolla',
       'Presas de pollo doradas con limón y ajo, papas criollas y ensalada fresca.',
       50, 'AC',
       [i('Pollo', 800, True), i('Papa criolla', 600), i('Limón', 2), i('Ajo', 10), i('Comino', 2),
        i('Lechuga', 1), i('Tomate', 200), i('Aceite vegetal', 40), i('Sal', 10)],
       ['Adoba las presas de pollo con el jugo de un limón, el ajo machacado, el comino y la mitad de la sal.',
        'Lava las papas criollas y ponlas enteras en una olla con agua y el resto de la sal.',
        'Cuando el agua hierva, cocina las papas 15 minutos.',
        'Calienta el aceite en una sartén grande y pon las presas con la piel hacia abajo.',
        'Tapa y cocina a fuego medio 25 minutos, volteando las presas a la mitad del tiempo, hasta que estén doradas y sin jugo rosado.',
        'Pica la lechuga y el tomate y aliña con el otro limón.',
        'Sirve el pollo con las papas criollas y la ensalada.'],
       ninos=True)

receta('Arroz atollado',
       'Arroz caldoso del Valle con pollo, cerdo, chorizo y papa. Plato completo en una olla.',
       70, 'A',
       [i('Arroz', 400, True), i('Pollo', 400), i('Carne de cerdo', 300), i('Chorizo', 2), i('Papa', 400),
        i('Tomate', 200), i('Cebolla larga', 100), i('Pimentón', 1), i('Ajo', 10), i('Color (achiote)', 3),
        i('Aceite vegetal', 40), i('Sal', 12)],
       ['Corta el pollo y el cerdo en trozos y el chorizo en rodajas. Échales la mitad de la sal.',
        'Pica el tomate, la cebolla larga, el pimentón y el ajo. Pela la papa y córtala en cubos.',
        'En una olla grande con el aceite, dora el pollo, el cerdo y el chorizo 10 minutos.',
        'Agrega el tomate, la cebolla, el pimentón, el ajo y el color y sofríe 5 minutos.',
        'Añade la papa y 1 litro y medio de agua. Cuando hierva, cocina 10 minutos.',
        'Lava el arroz, échalo a la olla con el resto de la sal y revuelve.',
        'Cocina destapado a fuego bajo 25 minutos, revolviendo de vez en cuando. Debe quedar caldoso, no seco.',
        'Sirve enseguida.'])

receta('Pasta boloñesa',
       'Pasta con salsa de carne molida, tomate y zanahoria. Rápida y le gusta a toda la familia.',
       40, 'AC',
       [i('Pasta', 500, True), i('Carne molida', 400, True), i('Tomate', 500), i('Cebolla', 150), i('Zanahoria', 100),
        i('Ajo', 10), i('Queso', 80, opcional=True), i('Aceite vegetal', 30), i('Sal', 12)],
       ['Pica la cebolla, el ajo y el tomate. Ralla la zanahoria.',
        'Calienta el aceite en una olla y dora la carne molida 8 minutos, revolviendo para que se suelte.',
        'Agrega la cebolla, el ajo y la zanahoria y sofríe 5 minutos.',
        'Añade el tomate y la mitad de la sal, tapa y cocina a fuego bajo 15 minutos.',
        'Pon a hervir 3 litros de agua con el resto de la sal en otra olla.',
        'Cuando hierva, echa la pasta y cocínala 10 minutos, revolviendo de vez en cuando.',
        'Escurre la pasta y sírvela con la salsa encima y, si quieres, queso rallado.'],
       ninos=True)

receta('Pollo a la plancha con arroz y aguacate',
       'Pechuga o muslos a la plancha con limón, arroz blanco, aguacate y ensalada de tomate.',
       40, 'AC',
       [i('Pollo', 600, True), i('Arroz', 400), i('Aguacate', 1), i('Limón', 2), i('Ajo', 10),
        i('Tomate', 200), i('Cebolla', 100), i('Aceite vegetal', 35), i('Sal', 10)],
       ['Corta el pollo en filetes delgados y adóbalo con el jugo de un limón, el ajo machacado y la mitad de la sal.',
        'Lava el arroz y ponlo en una olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Calienta una sartén con el aceite que queda y asa los filetes 5 minutos por cada lado.',
        'Pica el tomate y la cebolla y aliña con el otro limón.',
        'Sirve el pollo con el arroz, el aguacate en tajadas y la ensalada.'],
       ninos=True)

receta('Sopa de plátano con carne',
       'Sopa espesa de plátano verde rallado con carne de res y papa. Se acompaña con arroz.',
       60, 'AC',
       [i('Plátano verde', 3, True), i('Carne de res', 300), i('Papa', 300), i('Cebolla larga', 100), i('Ajo', 10),
        i('Cilantro', 15), i('Comino', 2), i('Arroz', 300), i('Aceite vegetal', 25), i('Sal', 10)],
       ['Corta la carne en cubos pequeños y ponla en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Pela los plátanos y rállalos por el lado grueso del rallador. Pela la papa y córtala en cubos.',
        'Agrega el plátano rallado, la papa y el comino, revolviendo para que no se pegue.',
        'Cocina 20 minutos a fuego bajo, revolviendo de vez en cuando, hasta que espese.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el cilantro, échalo a la sopa y sirve con el arroz aparte.'])

receta('Picadillo de carne con papa y arroz',
       'Carne molida guisada con papa, arveja y zanahoria, con arroz blanco.',
       45, 'AC',
       [i('Carne molida', 500, True), i('Papa', 500), i('Arveja verde', 200), i('Zanahoria', 150), i('Tomate', 200),
        i('Cebolla', 100), i('Ajo', 10), i('Comino', 2), i('Arroz', 400), i('Aceite vegetal', 40), i('Sal', 12)],
       ['Pela la papa y la zanahoria y córtalas en cubos pequeños. Pica el tomate, la cebolla y el ajo.',
        'Calienta 2 cucharadas de aceite en una olla y dora la carne molida 8 minutos.',
        'Agrega la cebolla, el tomate y el ajo y sofríe 5 minutos.',
        'Añade la papa, la zanahoria, la arveja, el comino, la mitad de la sal y 300 ml de agua.',
        'Tapa y cocina a fuego bajo 20 minutos, hasta que la papa esté blanda.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve el picadillo con el arroz.'],
       ninos=True)

receta('Sardinas guisadas con arroz y patacón',
       'Sardinas en salsa de tomate y cebolla, con arroz y patacones. Almuerzo económico.',
       40, 'AC',
       [i('Sardinas en lata', 2, True), i('Arroz', 400), i('Plátano verde', 2), i('Tomate', 300), i('Cebolla', 150),
        i('Ajo', 10), i('Aceite vegetal', 120), i('Sal', 8)],
       ['Lava el arroz y ponlo en una olla con 800 ml de agua, una cucharada de aceite y la mitad de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el tomate, la cebolla y el ajo.',
        'En una sartén con una cucharada de aceite, sofríe la cebolla, el tomate y el ajo 6 minutos.',
        'Agrega las sardinas con su salsa, desmenúzalas un poco y calienta 5 minutos.',
        'Pela los plátanos, córtalos en trozos y fríelos en aceite caliente 4 minutos.',
        'Aplasta los trozos y fríelos otra vez hasta que estén dorados. Échales el resto de la sal.',
        'Sirve las sardinas con el arroz y los patacones.'])

receta('Sopa de ahuyama con pollo y arroz',
       'Sopa cremosa de ahuyama con pollo desmechado y papa, con arroz aparte.',
       55, 'AC',
       [i('Ahuyama', 600, True), i('Pollo', 400, True), i('Papa', 300), i('Cebolla larga', 100), i('Ajo', 10),
        i('Cilantro', 10), i('Crema de leche', 100, opcional=True), i('Arroz', 300), i('Aceite vegetal', 20), i('Sal', 10)],
       ['Pon el pollo en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Pela la ahuyama y la papa y córtalas en cubos.',
        'Saca el pollo y desmenúzalo. Echa a la olla la ahuyama y la papa.',
        'Cocina 20 minutos, hasta que la ahuyama se deshaga al tocarla.',
        'Licúa la mitad de la sopa y devuélvela a la olla con el pollo desmechado.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve con cilantro picado, arroz aparte y, si quieres, un chorrito de crema de leche.'])

receta('Cerdo guisado con yuca',
       'Cerdo en hogao con color y comino, acompañado de yuca cocida.',
       70, 'AC',
       [i('Carne de cerdo', 600, True), i('Yuca', 800), i('Tomate', 300), i('Cebolla larga', 100), i('Ajo', 10),
        i('Color (achiote)', 3), i('Comino', 2), i('Aceite vegetal', 30), i('Sal', 12)],
       ['Corta el cerdo en cubos y échale el ajo, el comino y la mitad de la sal.',
        'Pica el tomate y la cebolla larga.',
        'En una olla con el aceite, dora el cerdo 8 minutos.',
        'Agrega el tomate, la cebolla y el color, revuelve y añade 250 ml de agua.',
        'Tapa y cocina a fuego bajo 35 minutos, hasta que el cerdo esté blando.',
        'Pela la yuca, córtala en trozos y ponla en otra olla con agua y el resto de la sal.',
        'Cuando el agua hierva, cocina la yuca 25 minutos, hasta que esté blanda y se abra.',
        'Sirve el cerdo con su salsa y la yuca.'])

receta('Pollo con habichuela y zanahoria',
       'Pollo salteado con habichuela y zanahoria, con arroz blanco.',
       45, 'AC',
       [i('Pollo', 600, True), i('Habichuela', 300), i('Zanahoria', 200), i('Cebolla', 100), i('Ajo', 10),
        i('Comino', 2), i('Arroz', 400), i('Aceite vegetal', 40), i('Sal', 10)],
       ['Corta el pollo en tiras y échale el ajo, el comino y la mitad de la sal.',
        'Corta la habichuela en trozos, la zanahoria en tiras y la cebolla en plumas.',
        'Lava el arroz y ponlo en una olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'En una sartén grande con el aceite que queda, dora el pollo 8 minutos.',
        'Agrega la habichuela, la zanahoria y la cebolla y saltea 8 minutos, hasta que estén tiernas pero crocantes.',
        'Sirve el pollo con las verduras y el arroz.'])

receta('Carne desmechada con arroz y maduro',
       'Ropa vieja: carne de res desmechada en hogao con pimentón, arroz y tajadas de maduro.',
       90, 'A',
       [i('Carne de res', 600, True), i('Tomate', 300), i('Cebolla', 150), i('Pimentón', 1), i('Ajo', 10),
        i('Comino', 2), i('Arroz', 400), i('Plátano maduro', 2), i('Aceite vegetal', 60), i('Sal', 12)],
       ['Pon la carne entera en una olla con 2 litros de agua y la mitad de la sal.',
        'Cuando hierva, baja el fuego y cocina 50 minutos, hasta que se deje desmechar.',
        'Saca la carne y desmenúzala con dos tenedores.',
        'Pica el tomate, la cebolla, el pimentón y el ajo.',
        'En una sartén con una cucharada de aceite, sofríe el picado con el comino 6 minutos.',
        'Agrega la carne desmechada y 100 ml del caldo y cocina 5 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los maduros, córtalos en tajadas y fríelos en el aceite que queda.',
        'Sirve la carne con el arroz y las tajadas.'])

receta('Pasta con pollo en salsa blanca',
       'Pasta con pollo y crema de leche, cebolla y ajo. Lista en media hora.',
       35, 'AC',
       [i('Pasta', 500, True), i('Pollo', 400, True), i('Crema de leche', 200), i('Cebolla', 100), i('Ajo', 10),
        i('Queso', 80, opcional=True), i('Aceite vegetal', 20), i('Sal', 10)],
       ['Pon a hervir 3 litros de agua con la mitad de la sal.',
        'Corta el pollo en cubos pequeños y pica la cebolla y el ajo.',
        'Cuando el agua hierva, echa la pasta y cocínala 10 minutos.',
        'Mientras tanto, dora el pollo en una sartén con el aceite durante 8 minutos.',
        'Agrega la cebolla y el ajo y sofríe 3 minutos.',
        'Baja el fuego, echa la crema de leche con el resto de la sal y deja que hierva despacio 3 minutos.',
        'Escurre la pasta, mézclala con la salsa y sirve con queso rallado si quieres.'],
       ninos=True)

# ---------------------------------------------------------------- Cenas
receta('Arepas rellenas de pollo y queso',
       'Arepas asadas rellenas de pollo desmechado en hogao y queso.',
       45, 'C',
       [i('Arepa de maíz', 8, True), i('Pollo', 400, True), i('Queso', 100), i('Tomate', 200), i('Cebolla', 100),
        i('Ajo', 5), i('Aceite vegetal', 20), i('Sal', 6)],
       ['Pon el pollo en una olla con 1 litro de agua y la mitad de la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Saca el pollo y desmenúzalo.',
        'Pica el tomate, la cebolla y el ajo y sofríelos en el aceite 5 minutos.',
        'Agrega el pollo desmechado y el resto de la sal y cocina 3 minutos.',
        'Asa las arepas en una sartén 3 minutos por cada lado.',
        'Ábrelas por un lado y rellénalas con el pollo y un trozo de queso.'],
       ninos=True)

receta('Sopa de pasta con pollo',
       'Sopa casera de fideos con pollo, papa y zanahoria. Ideal para la noche.',
       40, 'C',
       [i('Pasta', 200, True), i('Pollo', 300), i('Papa', 300), i('Zanahoria', 100), i('Cebolla larga', 80),
        i('Ajo', 5), i('Cilantro', 10), i('Sal', 8)],
       ['Pon el pollo en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 15 minutos.',
        'Pela la papa y la zanahoria y córtalas en cubos pequeños.',
        'Saca el pollo, desmenúzalo y devuélvelo a la olla con la papa y la zanahoria.',
        'Cocina 10 minutos.',
        'Echa la pasta y cocina 8 minutos más.',
        'Pica el cilantro y échalo antes de servir.'],
       ninos=True)

receta('Crema de ahuyama',
       'Crema suave de ahuyama con leche y mantequilla. Se acompaña con pan.',
       35, 'C',
       [i('Ahuyama', 800, True), i('Cebolla', 100), i('Ajo', 5), i('Leche', 300), i('Mantequilla', 20),
        i('Pan', 4, opcional=True), i('Sal', 6)],
       ['Pela la ahuyama y córtala en cubos. Pica la cebolla y el ajo.',
        'Derrite la mantequilla en una olla y sofríe la cebolla y el ajo 4 minutos.',
        'Agrega la ahuyama y 600 ml de agua. Cuando hierva, cocina 20 minutos.',
        'Licúa todo con la leche y la sal.',
        'Devuelve la crema a la olla y caliéntala sin que hierva.',
        'Sirve con pan si quieres.'])

receta('Patacones con carne desmechada y hogao',
       'Patacones crocantes cubiertos con carne desmechada en hogao.',
       60, 'C',
       [i('Plátano verde', 3, True), i('Carne de res', 400, True), i('Tomate', 300), i('Cebolla larga', 100),
        i('Ajo', 5), i('Aceite vegetal', 150), i('Sal', 8)],
       ['Pon la carne en una olla con 1 litro y medio de agua y la mitad de la sal.',
        'Cuando hierva, baja el fuego y cocina 40 minutos, hasta que se deje desmechar.',
        'Desmecha la carne con dos tenedores.',
        'Pica el tomate, la cebolla larga y el ajo y sofríelos en una cucharada de aceite 6 minutos.',
        'Agrega la carne y un poco del caldo y cocina 5 minutos.',
        'Pela los plátanos, córtalos en trozos y fríelos en aceite caliente 4 minutos.',
        'Aplasta los trozos y fríelos otra vez hasta que estén dorados. Échales el resto de la sal.',
        'Sirve los patacones con la carne encima.'])

# ---------------------------------------------------------------- Desayunos
receta('Huevos pericos con arepa',
       'El desayuno de siempre: huevos con tomate y cebolla larga, arepa asada y mantequilla.',
       20, 'DC',
       [i('Huevos', 8, True), i('Arepa de maíz', 4), i('Tomate', 200), i('Cebolla larga', 100),
        i('Mantequilla', 20, opcional=True), i('Aceite vegetal', 15), i('Sal', 5)],
       ['Pica el tomate y la cebolla larga.',
        'Calienta el aceite en una sartén y sofríe el tomate y la cebolla 4 minutos.',
        'Bate los huevos con la sal y échalos a la sartén.',
        'Revuelve despacio a fuego bajo hasta que estén cocidos pero jugosos.',
        'Asa las arepas en otra sartén 3 minutos por cada lado.',
        'Sirve los pericos con la arepa y, si quieres, mantequilla.'],
       ninos=True)

receta('Calentado con huevo y arepa',
       'Arroz con carne molida y hogao, huevo frito y arepa. Desayuno paisa o comida rápida en la noche.',
       40, 'DC',
       [i('Arroz', 300, True), i('Carne molida', 250), i('Huevos', 4), i('Arepa de maíz', 4), i('Tomate', 150),
        i('Cebolla larga', 80), i('Aceite vegetal', 40), i('Sal', 8)],
       ['Si no tienes arroz del día anterior, lava el arroz y ponlo en una olla con 600 ml de agua y la mitad de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el tomate y la cebolla larga.',
        'En una sartén grande con una cucharada de aceite, dora la carne molida con el tomate y la cebolla 8 minutos.',
        'Agrega el arroz y el resto de la sal y revuelve a fuego medio 5 minutos.',
        'Fríe los huevos en otra sartén con el aceite que queda.',
        'Asa las arepas.',
        'Sirve el calentado con un huevo encima y la arepa al lado.'])

receta('Huevos rancheros con arepa',
       'Huevos en salsa de tomate, pimentón y cebolla, con arepa.',
       25, 'DC',
       [i('Huevos', 8, True), i('Tomate', 300), i('Cebolla', 100), i('Pimentón', 1), i('Ajo', 5),
        i('Arepa de maíz', 4), i('Aceite vegetal', 20), i('Sal', 5)],
       ['Pica el tomate, la cebolla, el pimentón y el ajo.',
        'Sofríe el picado en una sartén con el aceite 6 minutos.',
        'Haz huecos en la salsa y rompe un huevo en cada uno. Échales la sal.',
        'Tapa la sartén y cocina a fuego bajo 5 minutos, hasta que la clara cuaje.',
        'Asa las arepas y sirve los huevos con su salsa.'])

receta('Changua',
       'Caldo de leche con huevo, cebolla larga y cilantro, típico del desayuno bogotano.',
       20, 'D',
       [i('Leche', 1000, True), i('Huevos', 4, True), i('Cebolla larga', 50), i('Cilantro', 10),
        i('Pan', 4, opcional=True), i('Sal', 5)],
       ['Pica muy fino la cebolla larga y el cilantro.',
        'Pon en una olla la leche, 500 ml de agua, la cebolla y la sal.',
        'Cuando hierva, baja el fuego.',
        'Rompe los huevos uno por uno dentro del caldo, sin revolver, y cocina 4 minutos.',
        'Sirve en tazones con cilantro y, si quieres, pan en trozos.'])

receta('Arepa con queso y chocolate',
       'Arepa asada con queso y mantequilla y una taza de chocolate en leche.',
       15, 'D',
       [i('Arepa de maíz', 4, True), i('Queso', 150), i('Mantequilla', 20), i('Chocolate de mesa', 100),
        i('Leche', 800)],
       ['Pon la leche en una olla con las pastillas de chocolate partidas.',
        'Cuando hierva, baja el fuego y bate con el molinillo o un batidor hasta que haga espuma.',
        'Asa las arepas en una sartén 3 minutos por cada lado.',
        'Úntalas de mantequilla y ponles una tajada de queso encima.',
        'Sirve las arepas con el chocolate caliente.'],
       ninos=True)

receta('Caldo de costilla',
       'Caldo de costilla de res con papa y cilantro. Se acompaña con arepa.',
       60, 'DC',
       [i('Costilla de res', 600, True), i('Papa', 500), i('Cebolla larga', 80), i('Ajo', 10), i('Cilantro', 15),
        i('Arepa de maíz', 4), i('Sal', 8)],
       ['Pon la costilla en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego, quita la espuma y cocina 35 minutos.',
        'Pela la papa y córtala en trozos.',
        'Agrega la papa y cocina 20 minutos, hasta que esté blanda.',
        'Pica el cilantro y échalo al final.',
        'Sirve con arepa asada.'])

receta('Pan con huevo y café con leche',
       'Huevos fritos con pan, y café con leche endulzado con panela.',
       15, 'D',
       [i('Pan', 4), i('Huevos', 4, True), i('Mantequilla', 20), i('Café molido', 30), i('Leche', 500),
        i('Panela', 60), i('Sal', 3)],
       ['Pon 500 ml de agua con la panela en una olla.',
        'Cuando hierva, apaga, echa el café, revuelve y deja reposar 3 minutos.',
        'Cuela el café y mézclalo con la leche caliente.',
        'Derrite la mantequilla en una sartén y fríe los huevos con la sal.',
        'Sirve los huevos con el pan y el café con leche.'])

receta('Avena con leche y banano',
       'Avena en hojuelas cocida en leche con panela y banano en rodajas.',
       15, 'D',
       [i('Avena en hojuelas', 160, True), i('Leche', 1000), i('Banano', 2), i('Panela', 80)],
       ['Pon la leche con la panela rallada en una olla.',
        'Cuando hierva, echa la avena y revuelve.',
        'Cocina a fuego bajo 5 minutos, revolviendo para que no se pegue.',
        'Sirve con el banano en rodajas.'],
       ninos=True)

receta('Tortilla de huevo con maduro y queso',
       'Tortilla de huevo con tajadas de plátano maduro y queso.',
       25, 'DC',
       [i('Huevos', 6, True), i('Plátano maduro', 2), i('Queso', 100), i('Cebolla larga', 50),
        i('Aceite vegetal', 30), i('Sal', 4)],
       ['Pela los maduros y córtalos en rodajas delgadas. Pica la cebolla larga.',
        'Fríe el maduro en una sartén con el aceite 5 minutos, hasta que esté dorado.',
        'Bate los huevos con la sal y la cebolla y échalos sobre el maduro.',
        'Pon el queso en trozos encima, tapa y cocina a fuego bajo 6 minutos.',
        'Sirve en porciones.'],
       ninos=True)

# ---------------------------------------------------------------- Acompañamientos y bebidas
receta('Ensalada de aguacate, tomate y cebolla',
       'Ensalada fresca para acompañar el almuerzo.',
       10, 'A',
       [i('Aguacate', 1, True), i('Tomate', 200), i('Cebolla', 80), i('Limón', 1), i('Sal', 2)],
       ['Corta el aguacate y el tomate en cubos y la cebolla en plumas delgadas.',
        'Mezcla todo con el jugo del limón y la sal.',
        'Sirve enseguida para que el aguacate no se oscurezca.'],
       fuerte=False)

receta('Jugo de mora',
       'Jugo natural de mora en agua, endulzado con panela.',
       10, 'DA',
       [i('Mora', 300, True), i('Panela', 80)],
       ['Lava las moras.',
        'Licúa las moras con 1 litro de agua y la panela rallada.',
        'Cuela el jugo y sírvelo frío.'],
       fuerte=False, ninos=True)

receta('Jugo de lulo',
       'Jugo de lulo en agua, ácido y refrescante, endulzado con panela.',
       10, 'DA',
       [i('Lulo', 300, True), i('Panela', 80)],
       ['Lava los lulos, pártelos por la mitad y saca la pulpa con una cuchara.',
        'Licúa la pulpa con 1 litro de agua y la panela rallada.',
        'Cuela el jugo y sírvelo frío.'],
       fuerte=False)

receta('Aguapanela con limón',
       'Panela disuelta en agua con limón, fría o caliente.',
       15, 'DA',
       [i('Panela', 150, True), i('Limón', 2)],
       ['Pon 1 litro y medio de agua con la panela en una olla.',
        'Cuando hierva, baja el fuego y cocina 5 minutos, hasta que la panela se disuelva.',
        'Apaga, deja enfriar y agrega el jugo de los limones.'],
       fuerte=False, ninos=True)

receta('Fruta picada',
       'Papaya, mango, piña y banano picados. Para el desayuno o las onces.',
       10, 'D',
       [i('Papaya', 0.5, True), i('Mango', 1), i('Piña', 0.5), i('Banano', 2), i('Limón', 1)],
       ['Pela la papaya, el mango y la piña y córtalos en cubos.',
        'Corta el banano en rodajas.',
        'Mezcla todo con el jugo del limón y sirve.'],
       fuerte=False, ninos=True)

# ---------------------------------------------------------------- Segunda tanda (28 de octubre)
receta('Fríjoles con costilla y arroz',
       'Fríjoles cocinados con costilla de res y hogao, con arroz blanco y tajadas de maduro.',
       110, 'A',
       [i('Frijol', 500, True), i('Costilla de res', 500, True), i('Arroz', 400), i('Plátano maduro', 2),
        i('Tomate', 150), i('Cebolla larga', 100), i('Ajo', 10), i('Comino', 2), i('Aceite vegetal', 50), i('Sal', 12)],
       ['Escurre los fríjoles del remojo y ponlos en una olla con la costilla y 2 litros y medio de agua.',
        'Cuando hierva, baja el fuego, tapa y cocina 1 hora y media, hasta que los fríjoles y la costilla estén blandos.',
        'Mientras tanto, pica el tomate, la cebolla larga y el ajo.',
        'Sofríe el tomate, la cebolla y el ajo en una cucharada de aceite 5 minutos para hacer el hogao.',
        'Echa el hogao, el comino y la mitad de la sal a los fríjoles y cocina destapado 15 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los maduros, córtalos en tajadas y fríelos en el aceite que queda hasta que doren.',
        'Sirve fríjoles con costilla, arroz y tajadas.'],
       previa='Deja los fríjoles en remojo con agua la noche anterior.')

receta('Hígado encebollado con arroz y papa',
       'Hígado de res en tiras con bastante cebolla, arroz blanco y papa cocida.',
       45, 'AC',
       [i('Hígado de res', 500, True), i('Cebolla', 300), i('Arroz', 300), i('Papa', 500), i('Tomate', 100),
        i('Ajo', 5), i('Aceite vegetal', 40), i('Sal', 10)],
       ['Lava el arroz y ponlo en una olla con 600 ml de agua, una cucharada de aceite y un poco de sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela las papas y cocínalas en agua con sal 20 minutos.',
        'Mientras tanto, corta el hígado en tiras y la cebolla en plumas. Pica el tomate y el ajo.',
        'Dora el hígado en una sartén caliente con el aceite 4 minutos, sin dejar que se seque.',
        'Agrega la cebolla, el tomate, el ajo y el resto de la sal y cocina 6 minutos.',
        'Sirve el hígado encebollado con arroz y papa.'])

receta('Pescado sudado con arroz y patacón',
       'Filete de tilapia sudado en salsa de tomate y cebolla, con arroz y patacones.',
       50, 'AC',
       [i('Filete de tilapia', 600, True), i('Arroz', 300), i('Plátano verde', 2), i('Tomate', 300),
        i('Cebolla', 150), i('Pimentón', 1), i('Ajo', 5), i('Cilantro', 10), i('Limón', 1),
        i('Aceite vegetal', 120), i('Sal', 10)],
       ['Échale al pescado el jugo del limón y un poco de sal.',
        'Lava el arroz y ponlo en una olla con 600 ml de agua y una cucharada de aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el tomate, la cebolla, el pimentón y el ajo y sofríelos en una cucharada de aceite 6 minutos.',
        'Pon el pescado sobre la salsa, agrega medio pocillo de agua, tapa y cocina 10 minutos a fuego bajo.',
        'Pela los plátanos, córtalos en trozos y fríelos en el resto del aceite 4 minutos.',
        'Aplasta los trozos y fríelos otra vez hasta que doren. Échales sal.',
        'Pica el cilantro, échalo al pescado y sirve con arroz y patacones.'])

receta('Arroz con cerdo y verduras',
       'Arroz salteado con cerdo en trocitos, zanahoria, habichuela, arveja y huevo.',
       45, 'AC',
       [i('Arroz', 400, True), i('Carne de cerdo', 400, True), i('Zanahoria', 150), i('Habichuela', 150),
        i('Arveja verde', 150), i('Huevos', 2), i('Cebolla larga', 80), i('Aceite vegetal', 40), i('Sal', 10)],
       ['Lava el arroz y ponlo en una olla con 800 ml de agua y la mitad de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Mientras tanto, corta el cerdo en cubos pequeños y pica la zanahoria, la habichuela y la cebolla larga.',
        'Dora el cerdo en una sartén grande con el aceite durante 10 minutos.',
        'Agrega la zanahoria, la habichuela y la arveja y saltea 6 minutos.',
        'Haz un hueco en el centro, echa los huevos batidos y revuelve hasta que cuajen.',
        'Agrega el arroz, la cebolla larga y el resto de la sal y mezcla todo 3 minutos.'],
       ninos=True)

receta('Sopa de arracacha con costilla',
       'Sopa espesa de arracacha y papa con costilla de res, servida con arroz y aguacate.',
       80, 'A',
       [i('Costilla de res', 500, True), i('Arracacha', 500), i('Papa', 300), i('Arroz', 300),
        i('Cebolla larga', 80), i('Ajo', 5), i('Cilantro', 10), i('Aguacate', 1, opcional=True),
        i('Aceite vegetal', 15), i('Sal', 12)],
       ['Pon la costilla en una olla con 2 litros y medio de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 40 minutos.',
        'Pela la arracacha y la papa y córtalas en trozos.',
        'Agrega la arracacha y la papa a la olla y cocina 25 minutos, hasta que se deshagan un poco.',
        'Mientras tanto, lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el cilantro y échalo a la sopa.',
        'Sirve la sopa con arroz aparte y aguacate si tienes.'])

receta('Pechuga gratinada con arroz y ensalada',
       'Pechuga de pollo a la plancha cubierta con queso derretido, arroz y ensalada de lechuga y tomate.',
       40, 'AC',
       [i('Pollo', 600, True), i('Queso', 150), i('Arroz', 300), i('Lechuga', 1), i('Tomate', 200),
        i('Limón', 1), i('Ajo', 5), i('Aceite vegetal', 30), i('Sal', 10)],
       ['Lava el arroz y ponlo en una olla con 600 ml de agua, una cucharada de aceite y un poco de sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Abre las pechugas en filetes delgados y adóbalas con el ajo machacado y el resto de la sal.',
        'Asa los filetes en una sartén con el resto del aceite 5 minutos por cada lado.',
        'Pon una tajada de queso sobre cada filete, tapa la sartén y deja 2 minutos para que se derrita.',
        'Lava la lechuga, córtala con el tomate y aliña con el jugo del limón.',
        'Sirve la pechuga con arroz y ensalada.'],
       ninos=True)

receta('Cazuela de fríjoles con chorizo',
       'Fríjoles espesos con chorizo, maduro y hogao, servidos en cazuela con arroz y aguacate.',
       100, 'A',
       [i('Frijol', 500, True), i('Chorizo', 3, True), i('Plátano maduro', 1), i('Arroz', 300),
        i('Tomate', 150), i('Cebolla larga', 80), i('Ajo', 5), i('Aguacate', 1, opcional=True),
        i('Aceite vegetal', 30), i('Sal', 10)],
       ['Escurre los fríjoles del remojo y ponlos en una olla con 2 litros de agua.',
        'Cuando hierva, baja el fuego, tapa y cocina 1 hora.',
        'Mientras tanto, pica el tomate, la cebolla larga y el ajo, y corta el chorizo y el maduro en rodajas.',
        'Dora el chorizo en una sartén 5 minutos y sácalo.',
        'En la misma grasa, sofríe el tomate, la cebolla y el ajo 5 minutos.',
        'Agrega a los fríjoles el hogao, el chorizo, el maduro y la sal, y cocina destapado 20 minutos para que espesen.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve la cazuela con arroz y aguacate si tienes.'],
       previa='Deja los fríjoles en remojo con agua la noche anterior.')

receta('Empanadas de carne y papa',
       'Empanadas de masa de maíz rellenas de carne molida y papa, fritas y doradas.',
       75, 'C',
       [i('Harina de maíz precocida', 500, True), i('Carne molida', 300, True), i('Papa', 300), i('Tomate', 100),
        i('Cebolla larga', 60), i('Color (achiote)', 2), i('Comino', 1), i('Aceite vegetal', 300), i('Sal', 10)],
       ['Pela las papas y cocínalas en agua con sal 20 minutos. Escúrrelas y aplástalas.',
        'Mientras tanto, pica el tomate y la cebolla larga y sofríelos con la carne molida y el comino 10 minutos.',
        'Mezcla la carne con la papa aplastada. Ese es el relleno.',
        'Mezcla la harina con 600 ml de agua tibia, el color y el resto de la sal hasta tener una masa suave.',
        'Haz bolitas de masa, aplánalas sobre un plástico, pon relleno en el centro y dóblalas en media luna.',
        'Calienta el aceite en una olla y fríe las empanadas de a pocas 5 minutos, hasta que doren.',
        'Sácalas sobre papel de cocina y sirve calientes.'],
       ninos=True)

receta('Sándwich de pollo y queso',
       'Pan con pollo desmechado, queso, lechuga y tomate. Cena rápida.',
       35, 'C',
       [i('Pan', 8, True), i('Pollo', 400, True), i('Queso', 100), i('Lechuga', 1), i('Tomate', 150),
        i('Mantequilla', 20, opcional=True), i('Sal', 5)],
       ['Pon el pollo en una olla con 1 litro de agua y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Saca el pollo y desmenúzalo.',
        'Lava la lechuga y corta el tomate en rodajas.',
        'Arma cada sándwich con pollo, queso, lechuga y tomate.',
        'Si quieres, úntale mantequilla por fuera y dóralo en una sartén 2 minutos por cada lado.'],
       ninos=True)

receta('Sopa de verduras con pollo',
       'Sopa liviana de pollo con papa, zanahoria, habichuela y ahuyama.',
       45, 'C',
       [i('Pollo', 400, True), i('Papa', 300), i('Zanahoria', 150), i('Habichuela', 150), i('Ahuyama', 200),
        i('Cebolla larga', 80), i('Cilantro', 10), i('Sal', 10)],
       ['Pon el pollo en una olla con 2 litros de agua, la cebolla larga y la sal.',
        'Cuando hierva, baja el fuego y cocina 15 minutos.',
        'Pela y corta en cubos la papa, la zanahoria y la ahuyama, y la habichuela en trozos.',
        'Agrega las verduras a la olla y cocina 20 minutos.',
        'Saca el pollo, desmenúzalo y devuélvelo a la olla.',
        'Pica el cilantro y échalo antes de servir.'],
       ninos=True)

receta('Caldo de papa con huevo',
       'Caldo de papa con huevo, cebolla larga y cilantro, servido con arepa. Para el desayuno o la noche.',
       30, 'DC',
       [i('Papa', 600, True), i('Huevos', 4, True), i('Cebolla larga', 80), i('Cilantro', 10), i('Leche', 200),
        i('Arepa de maíz', 4, opcional=True), i('Sal', 8)],
       ['Pela las papas y córtalas en trozos.',
        'Pon las papas en una olla con 1 litro y medio de agua, la cebolla larga picada y la sal.',
        'Cuando hierva, baja el fuego y cocina 15 minutos.',
        'Agrega la leche y, cuando vuelva a hervir, echa los huevos uno por uno sin revolver.',
        'Deja cocinar los huevos 4 minutos.',
        'Pica el cilantro, échalo y sirve con arepa si quieres.'])

receta('Migas de arepa con huevo',
       'Arepa en trocitos revuelta con huevo, tomate y cebolla larga. Desayuno rápido para aprovechar la arepa.',
       15, 'D',
       [i('Arepa de maíz', 4, True), i('Huevos', 6, True), i('Tomate', 150), i('Cebolla larga', 80),
        i('Aceite vegetal', 20), i('Sal', 4)],
       ['Corta las arepas en cuadritos.',
        'Pica el tomate y la cebolla larga.',
        'Sofríe el tomate y la cebolla en el aceite 3 minutos.',
        'Agrega la arepa y dórala 3 minutos.',
        'Echa los huevos batidos con la sal y revuelve hasta que cuajen.'],
       ninos=True)

receta('Huevos tibios con pan y chocolate',
       'Huevos tibios, pan y una taza de chocolate caliente.',
       15, 'D',
       [i('Huevos', 4, True), i('Pan', 4), i('Chocolate de mesa', 100), i('Leche', 600), i('Sal', 2)],
       ['Pon la leche con el chocolate en una olla.',
        'Cuando hierva, baja el fuego y bate 3 minutos hasta que haga espuma.',
        'En otra olla, pon a hervir agua y echa los huevos con cuidado.',
        'Cocina los huevos 4 minutos y pásalos a agua fría.',
        'Pélalos o pártelos por la mitad, échales sal y sirve con el pan y el chocolate.'],
       ninos=True)

receta('Jugo de maracuyá',
       'Jugo de maracuyá en agua, endulzado con panela.',
       10, 'DA',
       [i('Maracuyá', 3, True), i('Panela', 80)],
       ['Parte los maracuyás y saca la pulpa con una cuchara.',
        'Licúa la pulpa con 1 litro de agua y la panela rallada, sin moler mucho las semillas.',
        'Cuela el jugo y sírvelo frío.'],
       fuerte=False, ninos=True)

receta('Ensalada de repollo y zanahoria',
       'Repollo y zanahoria rallados con limón. Acompaña cualquier almuerzo.',
       10, 'AC',
       [i('Repollo', 0.5, True), i('Zanahoria', 150), i('Limón', 1), i('Sal', 2)],
       ['Corta el repollo en tiras muy delgadas y ralla la zanahoria.',
        'Mezcla con el jugo del limón y la sal.',
        'Sirve enseguida.'],
       fuerte=False)

# ================================================================ Validación
TIEMPO = re.compile(
    r'(?:\d+\s*(?:a|-|o)\s*)?'
    r'(?:\d+|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|treinta|cuarenta)'
    r'\s*(?:minutos?|min\b)'
    r'|(?:\d+(?:[.,]\d+)?|un|una|media|dos|tres)\s+horas?', re.I)
PROTEINA = {'PROTEINAS', 'LEGUMBRES'}
LLENADOR = {'GRANOS', 'TUBERCULOS', 'HARINAS'}

errores = []
nombres = set()
for r in R:
    if r['nombre'] in nombres:
        errores.append('Repetida: ' + r['nombre'])
    nombres.add(r['nombre'])
    for paso in r['pasos']:
        t = TIEMPO.findall(paso)
        if len(t) > 1:
            errores.append(f"{r['nombre']}: dos tiempos en «{paso}» {t}")
        if 'según' in paso or 'instrucciones' in paso:
            errores.append(f"{r['nombre']}: paso ambiguo «{paso}»")
        if re.search(r'[0-9]+ ?(-|a) ?[0-9]+ minutos', paso):
            errores.append(f"{r['nombre']}: rango en «{paso}»")
    vistos = set()
    for (p, c, op, pr) in r['ingredientes']:
        if p not in CATALOGO:
            errores.append(f"{r['nombre']}: no existe {p}")
        if p in vistos:
            errores.append(f"{r['nombre']}: {p} repetido")
        vistos.add(p)
        if c <= 0:
            errores.append(f"{r['nombre']}: cantidad {c}")
    obligatorios = [CATALOGO[p][0] for (p, c, op, pr) in r['ingredientes'] if not op and p in CATALOGO]
    if 'A' in r['momentos'] and r['fuerte']:
        if not (PROTEINA & set(obligatorios)) or not (LLENADOR & set(obligatorios)):
            errores.append(f"{r['nombre']}: almuerzo sin proteína o sin acompañante que llene")
    if len(r['descripcion']) > 300:
        errores.append(r['nombre'] + ': descripción larga')
    if len(r['nombre']) > 120:
        errores.append(r['nombre'] + ': nombre largo')

if errores:
    print('\n'.join(errores))
    sys.exit(1)

# Costo por porción, para revisar que los precios tengan sentido.
if '--costos' in sys.argv:
    for r in R:
        costo = sum(c / CATALOGO[p][2] * CATALOGO[p][3] for (p, c, op, pr) in r['ingredientes'] if not op)
        print(f"{r['momentos']:3} {'F' if r['fuerte'] else '-'} {costo / r['porciones']:8.0f}/porción  {r['nombre']}")
    print(len(R), 'recetas,', len(PRODUCTOS_NUEVOS), 'productos nuevos')
    sys.exit(0)

# ================================================================ SQL
def q(texto):
    return "'" + texto.replace("\\", "\\\\").replace("'", "''") + "'"


def var(nombre):
    base = nombre.lower()
    for a, b in (('á', 'a'), ('é', 'e'), ('í', 'i'), ('ó', 'o'), ('ú', 'u'), ('ñ', 'n')):
        base = base.replace(a, b)
    return '@p_' + re.sub(r'[^a-z0-9]+', '_', base).strip('_')


def num(v):
    return ('%.3f' % v).rstrip('0').rstrip('.') if isinstance(v, float) else str(v)


out = []
w = out.append
w("""-- =====================================================================
-- V18__recetario_colombiano.sql
-- Cocina sin estrés · Ingredientes y recetas de una cocina colombiana (KAN-13)
--
-- Generado y validado con docs/datos/recetario_colombiano.py: cada paso dice
-- como máximo un tiempo (Modo cocinar, V12), todo almuerzo cumple la regla
-- de V17 (proteína + algo que llene) y cada ingrediente existe.
--
-- Productos: se agregan los que faltaban en una cocina colombiana (plátano,
-- yuca, arepa, cebolla larga, cilantro, panela, carne molida, cerdo, frutas
-- para jugo…), con precio de referencia de una tienda de barrio en el Eje
-- Cafetero (octubre de 2026), vida útil y alérgenos. Si el curador ya creó
-- un producto con el mismo nombre, se usa el suyo.
--
-- Recetas: almuerzos completos (bandeja de fríjoles, sancocho, ajiaco,
-- sudados, arroz atollado…), cenas, desayunos (pericos con arepa, changua,
-- calentado, caldo de costilla) y acompañamientos y jugos, que no son plato
-- fuerte y el plan nunca pone solos.
-- =====================================================================

SET @sistema = (
    SELECT id FROM usuario WHERE firebase_uid = 'sistema-carga-inicial'
);
SET @ultimo_producto = (SELECT COALESCE(MAX(id), 0) FROM producto);
SET @ultima_receta = (SELECT COALESCE(MAX(id), 0) FROM receta);

-- ---------------------------------------------------------------------
-- Productos nuevos (solo si no hay uno activo con el mismo nombre)
-- ---------------------------------------------------------------------""")
for (n, cat, tipo, uni, compra, cant, precio, basico, vida, restr) in PRODUCTOS_NUEVOS:
    cap = num(cant) if tipo == 'GRANEL' else 'NULL'
    w(f"""INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, {q(n)}, {q(cat)}, {q(tipo)}, {q(uni)}, {q(compra)}, {num(cant)}, {precio}.00, {cap}, {'TRUE' if basico else 'FALSE'}, TRUE, @sistema, {vida}
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER({q(n)}));""")

w("""
-- ---------------------------------------------------------------------
-- Productos que usan las recetas
-- ---------------------------------------------------------------------""")
usados = sorted({p for r in R for (p, *_ ) in r['ingredientes']})
for p in usados:
    w(f"SET {var(p)} = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER({q(p)}) "
      f"ORDER BY activo DESC, id LIMIT 1);")

w("""
-- Alergias y condiciones de los productos nuevos (V6). IGNORE: si el curador ya las puso.""")
for (n, cat, tipo, uni, compra, cant, precio, basico, vida, restr) in PRODUCTOS_NUEVOS:
    for codigo in restr:
        w(f"INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) "
          f"SELECT {var(n)}, id FROM restriccion WHERE codigo = {q(codigo)};" if n in usados else
          f"INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) "
          f"SELECT p.id, r.id FROM producto p JOIN restriccion r ON r.codigo = {q(codigo)} "
          f"WHERE p.hogar_id IS NULL AND LOWER(TRIM(p.nombre)) = LOWER({q(n)});")

for idx, r in enumerate(R, 1):
    momentos = r['momentos']
    w(f"""
-- =====================================================================
-- {idx}. {r['nombre']}
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    {q(r['nombre'])},
    {q(r['descripcion'])},
    {r['tiempo']},
    {r['porciones']},
    CONCAT_WS(CHAR(10),
        """ + ",\n        ".join(q(p) for p in r['pasos']) + f"""
    ),
    {q(r['previa']) if r['previa'] else 'NULL'},
    TRUE,
    @sistema,
    {'TRUE' if r['fuerte'] else 'FALSE'},
    {'TRUE' if r['ninos'] else 'FALSE'},
    {'TRUE' if 'D' in momentos else 'FALSE'},
    {'TRUE' if 'A' in momentos else 'FALSE'},
    {'TRUE' if 'C' in momentos else 'FALSE'}
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
""" + ",\n".join(
        f"    (@receta, {var(p)}, {num(c)}, {'TRUE' if op else 'FALSE'}, {'TRUE' if pr else 'FALSE'})"
        for (p, c, op, pr) in r['ingredientes']) + ";")

w("""
-- ---------------------------------------------------------------------
-- Trazabilidad del catálogo (ADR-23): un CREAR por producto y receta nuevos
-- ---------------------------------------------------------------------
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'PRODUCTO', id, 'CREAR', @sistema, JSON_OBJECT('origen', 'V18__recetario_colombiano')
FROM producto
WHERE id > @ultimo_producto AND hogar_id IS NULL;

INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'RECETA', id, 'CREAR', @sistema, JSON_OBJECT('origen', 'V18__recetario_colombiano')
FROM receta
WHERE id > @ultima_receta;""")

with open(sys.argv[1], 'w', encoding='utf-8') as f:
    f.write("\n".join(out) + "\n")
print('Escrito', sys.argv[1], len(R), 'recetas', len(PRODUCTOS_NUEVOS), 'productos')
