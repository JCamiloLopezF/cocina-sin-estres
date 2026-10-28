-- =====================================================================
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
-- ---------------------------------------------------------------------
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Garbanzo', 'LEGUMBRES', 'GRANEL', 'g', 'Bolsa', 500, 6000.00, 500, FALSE, TRUE, @sistema, 365
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Garbanzo'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Papa criolla', 'TUBERCULOS', 'GRANEL', 'g', 'Libra', 500, 4000.00, 500, FALSE, TRUE, @sistema, 10
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Papa criolla'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Yuca', 'TUBERCULOS', 'GRANEL', 'g', 'Libra', 500, 2500.00, 500, FALSE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Yuca'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Plátano verde', 'TUBERCULOS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500.00, NULL, FALSE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Plátano verde'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Plátano maduro', 'TUBERCULOS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500.00, NULL, FALSE, TRUE, @sistema, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Plátano maduro'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Arepa de maíz', 'HARINAS', 'CONTABLE', 'unidad', 'Paquete', 5, 4500.00, NULL, TRUE, TRUE, @sistema, 10
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Arepa de maíz'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Harina de maíz precocida', 'HARINAS', 'GRANEL', 'g', 'Paquete', 1000, 5500.00, 1000, FALSE, TRUE, @sistema, 365
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Harina de maíz precocida'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Harina de trigo', 'HARINAS', 'GRANEL', 'g', 'Libra', 500, 2800.00, 500, FALSE, TRUE, @sistema, 180
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Harina de trigo'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Pan', 'HARINAS', 'CONTABLE', 'unidad', 'Bolsa', 10, 5000.00, NULL, FALSE, TRUE, @sistema, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Pan'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Avena en hojuelas', 'GRANOS', 'GRANEL', 'g', 'Bolsa', 500, 4500.00, 500, FALSE, TRUE, @sistema, 365
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Avena en hojuelas'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Carne molida', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 14000.00, 500, FALSE, TRUE, @sistema, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Carne molida'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Carne de cerdo', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 13000.00, 500, FALSE, TRUE, @sistema, 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Carne de cerdo'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Costilla de res', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 11000.00, 500, FALSE, TRUE, @sistema, 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Costilla de res'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Chorizo', 'PROTEINAS', 'CONTABLE', 'unidad', 'Paquete', 5, 9000.00, NULL, FALSE, TRUE, @sistema, 15
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Chorizo'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Filete de tilapia', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 15000.00, 500, FALSE, TRUE, @sistema, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Filete de tilapia'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Sardinas en lata', 'PROTEINAS', 'CONTABLE', 'unidad', 'Lata', 1, 5500.00, NULL, FALSE, TRUE, @sistema, 730
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Sardinas en lata'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Crema de leche', 'LACTEOS', 'GRANEL', 'ml', 'Caja', 200, 4200.00, 200, FALSE, TRUE, @sistema, 15
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Crema de leche'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Mantequilla', 'LACTEOS', 'GRANEL', 'g', 'Barra', 125, 4500.00, 125, FALSE, TRUE, @sistema, 30
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Mantequilla'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Ajo', 'VERDURAS', 'GRANEL', 'g', 'Cabeza', 50, 1000.00, 50, TRUE, TRUE, @sistema, 30
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Ajo'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Cebolla larga', 'VERDURAS', 'GRANEL', 'g', 'Atado', 250, 2000.00, 250, TRUE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Cebolla larga'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Cilantro', 'VERDURAS', 'GRANEL', 'g', 'Atado', 50, 1000.00, 50, FALSE, TRUE, @sistema, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Cilantro'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Pimentón', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500.00, NULL, FALSE, TRUE, @sistema, 10
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Pimentón'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Habichuela', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 3500.00, 500, FALSE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Habichuela'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Arveja verde', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 5000.00, 500, FALSE, TRUE, @sistema, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Arveja verde'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Mazorca', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500.00, NULL, FALSE, TRUE, @sistema, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Mazorca'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Repollo', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 3000.00, NULL, FALSE, TRUE, @sistema, 14
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Repollo'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Lechuga', 'VERDURAS', 'CONTABLE', 'unidad', 'Unidad', 1, 2500.00, NULL, FALSE, TRUE, @sistema, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Lechuga'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Ahuyama', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 2000.00, 500, FALSE, TRUE, @sistema, 14
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Ahuyama'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Guascas', 'VERDURAS', 'GRANEL', 'g', 'Atado', 20, 1500.00, 20, FALSE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Guascas'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Limón', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 400.00, NULL, FALSE, TRUE, @sistema, 14
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Limón'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Aguacate', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 3000.00, NULL, FALSE, TRUE, @sistema, 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Aguacate'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Mora', 'FRUTAS', 'GRANEL', 'g', 'Libra', 500, 5000.00, 500, FALSE, TRUE, @sistema, 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Mora'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Lulo', 'FRUTAS', 'GRANEL', 'g', 'Libra', 500, 4500.00, 500, FALSE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Lulo'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Mango', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 2000.00, NULL, FALSE, TRUE, @sistema, 6
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Mango'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Papaya', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 5000.00, NULL, FALSE, TRUE, @sistema, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Papaya'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Piña', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 5000.00, NULL, FALSE, TRUE, @sistema, 6
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Piña'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Panela', 'CONDIMENTOS', 'GRANEL', 'g', 'Bloque', 500, 3000.00, 500, TRUE, TRUE, @sistema, 180
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Panela'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Comino', 'CONDIMENTOS', 'GRANEL', 'g', 'Sobre', 30, 1500.00, 30, FALSE, TRUE, @sistema, 365
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Comino'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Color (achiote)', 'CONDIMENTOS', 'GRANEL', 'g', 'Sobre', 30, 1200.00, 30, FALSE, TRUE, @sistema, 365
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Color (achiote)'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Chocolate de mesa', 'BEBIDAS', 'GRANEL', 'g', 'Paquete', 250, 6500.00, 250, FALSE, TRUE, @sistema, 365
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Chocolate de mesa'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Café molido', 'BEBIDAS', 'GRANEL', 'g', 'Paquete', 250, 9000.00, 250, TRUE, TRUE, @sistema, 180
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Café molido'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Arracacha', 'TUBERCULOS', 'GRANEL', 'g', 'Libra', 500, 3000.00, 500, FALSE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Arracacha'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Hígado de res', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 8000.00, 500, FALSE, TRUE, @sistema, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Hígado de res'));
INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad, unidad_base, unidad_compra,
                      cantidad_unidad_compra, precio_unidad_compra, capacidad_referencia, es_basico,
                      activo, modificado_por, vida_util_dias)
SELECT NULL, 'Maracuyá', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1200.00, NULL, FALSE, TRUE, @sistema, 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND LOWER(TRIM(nombre)) = LOWER('Maracuyá'));

-- ---------------------------------------------------------------------
-- Productos que usan las recetas
-- ---------------------------------------------------------------------
SET @p_aceite_vegetal = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Aceite vegetal') ORDER BY activo DESC, id LIMIT 1);
SET @p_aguacate = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Aguacate') ORDER BY activo DESC, id LIMIT 1);
SET @p_ahuyama = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Ahuyama') ORDER BY activo DESC, id LIMIT 1);
SET @p_ajo = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Ajo') ORDER BY activo DESC, id LIMIT 1);
SET @p_arepa_de_maiz = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Arepa de maíz') ORDER BY activo DESC, id LIMIT 1);
SET @p_arracacha = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Arracacha') ORDER BY activo DESC, id LIMIT 1);
SET @p_arroz = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Arroz') ORDER BY activo DESC, id LIMIT 1);
SET @p_arveja_verde = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Arveja verde') ORDER BY activo DESC, id LIMIT 1);
SET @p_avena_en_hojuelas = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Avena en hojuelas') ORDER BY activo DESC, id LIMIT 1);
SET @p_banano = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Banano') ORDER BY activo DESC, id LIMIT 1);
SET @p_cafe_molido = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Café molido') ORDER BY activo DESC, id LIMIT 1);
SET @p_carne_de_cerdo = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Carne de cerdo') ORDER BY activo DESC, id LIMIT 1);
SET @p_carne_de_res = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Carne de res') ORDER BY activo DESC, id LIMIT 1);
SET @p_carne_molida = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Carne molida') ORDER BY activo DESC, id LIMIT 1);
SET @p_cebolla = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Cebolla') ORDER BY activo DESC, id LIMIT 1);
SET @p_cebolla_larga = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Cebolla larga') ORDER BY activo DESC, id LIMIT 1);
SET @p_chocolate_de_mesa = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Chocolate de mesa') ORDER BY activo DESC, id LIMIT 1);
SET @p_chorizo = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Chorizo') ORDER BY activo DESC, id LIMIT 1);
SET @p_cilantro = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Cilantro') ORDER BY activo DESC, id LIMIT 1);
SET @p_color_achiote = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Color (achiote)') ORDER BY activo DESC, id LIMIT 1);
SET @p_comino = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Comino') ORDER BY activo DESC, id LIMIT 1);
SET @p_costilla_de_res = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Costilla de res') ORDER BY activo DESC, id LIMIT 1);
SET @p_crema_de_leche = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Crema de leche') ORDER BY activo DESC, id LIMIT 1);
SET @p_filete_de_tilapia = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Filete de tilapia') ORDER BY activo DESC, id LIMIT 1);
SET @p_frijol = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Frijol') ORDER BY activo DESC, id LIMIT 1);
SET @p_garbanzo = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Garbanzo') ORDER BY activo DESC, id LIMIT 1);
SET @p_guascas = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Guascas') ORDER BY activo DESC, id LIMIT 1);
SET @p_habichuela = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Habichuela') ORDER BY activo DESC, id LIMIT 1);
SET @p_harina_de_maiz_precocida = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Harina de maíz precocida') ORDER BY activo DESC, id LIMIT 1);
SET @p_huevos = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Huevos') ORDER BY activo DESC, id LIMIT 1);
SET @p_higado_de_res = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Hígado de res') ORDER BY activo DESC, id LIMIT 1);
SET @p_leche = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Leche') ORDER BY activo DESC, id LIMIT 1);
SET @p_lechuga = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Lechuga') ORDER BY activo DESC, id LIMIT 1);
SET @p_lentejas = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Lentejas') ORDER BY activo DESC, id LIMIT 1);
SET @p_limon = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Limón') ORDER BY activo DESC, id LIMIT 1);
SET @p_lulo = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Lulo') ORDER BY activo DESC, id LIMIT 1);
SET @p_mango = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Mango') ORDER BY activo DESC, id LIMIT 1);
SET @p_mantequilla = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Mantequilla') ORDER BY activo DESC, id LIMIT 1);
SET @p_maracuya = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Maracuyá') ORDER BY activo DESC, id LIMIT 1);
SET @p_mazorca = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Mazorca') ORDER BY activo DESC, id LIMIT 1);
SET @p_mora = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Mora') ORDER BY activo DESC, id LIMIT 1);
SET @p_pan = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Pan') ORDER BY activo DESC, id LIMIT 1);
SET @p_panela = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Panela') ORDER BY activo DESC, id LIMIT 1);
SET @p_papa = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Papa') ORDER BY activo DESC, id LIMIT 1);
SET @p_papa_criolla = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Papa criolla') ORDER BY activo DESC, id LIMIT 1);
SET @p_papaya = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Papaya') ORDER BY activo DESC, id LIMIT 1);
SET @p_pasta = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Pasta') ORDER BY activo DESC, id LIMIT 1);
SET @p_pimenton = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Pimentón') ORDER BY activo DESC, id LIMIT 1);
SET @p_pina = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Piña') ORDER BY activo DESC, id LIMIT 1);
SET @p_platano_maduro = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Plátano maduro') ORDER BY activo DESC, id LIMIT 1);
SET @p_platano_verde = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Plátano verde') ORDER BY activo DESC, id LIMIT 1);
SET @p_pollo = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Pollo') ORDER BY activo DESC, id LIMIT 1);
SET @p_queso = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Queso') ORDER BY activo DESC, id LIMIT 1);
SET @p_repollo = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Repollo') ORDER BY activo DESC, id LIMIT 1);
SET @p_sal = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Sal') ORDER BY activo DESC, id LIMIT 1);
SET @p_sardinas_en_lata = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Sardinas en lata') ORDER BY activo DESC, id LIMIT 1);
SET @p_tomate = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Tomate') ORDER BY activo DESC, id LIMIT 1);
SET @p_yuca = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Yuca') ORDER BY activo DESC, id LIMIT 1);
SET @p_zanahoria = (SELECT id FROM producto WHERE hogar_id IS NULL AND LOWER(TRIM(nombre)) = LOWER('Zanahoria') ORDER BY activo DESC, id LIMIT 1);

-- Alergias y condiciones de los productos nuevos (V6). IGNORE: si el curador ya las puso.
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT p.id, r.id FROM producto p JOIN restriccion r ON r.codigo = 'gluten' WHERE p.hogar_id IS NULL AND LOWER(TRIM(p.nombre)) = LOWER('Harina de trigo');
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_pan, id FROM restriccion WHERE codigo = 'gluten';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_avena_en_hojuelas, id FROM restriccion WHERE codigo = 'gluten';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_chorizo, id FROM restriccion WHERE codigo = 'colesterol';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_chorizo, id FROM restriccion WHERE codigo = 'hipertension';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_filete_de_tilapia, id FROM restriccion WHERE codigo = 'pescado';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_sardinas_en_lata, id FROM restriccion WHERE codigo = 'pescado';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_crema_de_leche, id FROM restriccion WHERE codigo = 'lactosa';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_mantequilla, id FROM restriccion WHERE codigo = 'lactosa';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_panela, id FROM restriccion WHERE codigo = 'diabetes';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_chocolate_de_mesa, id FROM restriccion WHERE codigo = 'diabetes';
INSERT IGNORE INTO producto_restriccion (producto_id, restriccion_id) SELECT @p_higado_de_res, id FROM restriccion WHERE codigo = 'colesterol';

-- =====================================================================
-- 1. Bandeja de fríjoles con carne molida y maduro
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Bandeja de fríjoles con carne molida y maduro',
    'El almuerzo de la casa: fríjoles con hogao, arroz, carne molida, tajadas de maduro y aguacate.',
    100,
    4,
    CONCAT_WS(CHAR(10),
        'Escurre los fríjoles que dejaste en remojo y ponlos en una olla con 2 litros de agua limpia.',
        'Cuando hierva, baja el fuego, tapa la olla y cocina 1 hora, hasta que los fríjoles estén blandos.',
        'Mientras se cocinan, pica el tomate, la cebolla larga y el ajo.',
        'En una sartén con una cucharada de aceite, sofríe el tomate, la cebolla y el ajo 5 minutos. Eso es el hogao.',
        'Pasa la mitad del hogao a los fríjoles con el comino y la mitad de la sal. Cocina destapado 20 minutos para que espesen.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y un poco de sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'En la sartén, dora la carne molida con el resto del hogao y de la sal durante 10 minutos, revolviendo para que se suelte.',
        'Pela los plátanos maduros, córtalos en tajadas y fríelos en el aceite que queda hasta que estén dorados por ambos lados.',
        'Sirve en cada plato arroz, fríjoles, carne, tajadas y una tajada de aguacate.'
    ),
    'Deja los fríjoles en remojo con agua la noche anterior.',
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_frijol, 500, FALSE, TRUE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_carne_molida, 400, FALSE, TRUE),
    (@receta, @p_platano_maduro, 2, FALSE, FALSE),
    (@receta, @p_aguacate, 1, FALSE, FALSE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 60, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 2. Sancocho de pollo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sancocho de pollo',
    'Sopa espesa de domingo con pollo, papa, yuca, plátano y mazorca. Se acompaña con arroz y aguacate.',
    75,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el pollo en presas y ponlo en una olla grande con 3 litros de agua, la cebolla larga entera, el ajo machacado y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos, quitando la espuma de encima.',
        'Pela el plátano verde y pártelo en trozos con la mano. Corta las mazorcas en ruedas.',
        'Agrega el plátano y la mazorca a la olla y cocina 15 minutos.',
        'Pela la yuca y la papa y córtalas en trozos grandes.',
        'Agrega la yuca y la papa y cocina 25 minutos, hasta que la yuca esté blanda.',
        'Mientras tanto, lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Saca la cebolla larga del sancocho, pica el cilantro y échalo al final.',
        'Sirve el sancocho con arroz aparte y, si tienes, una tajada de aguacate.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 800, FALSE, TRUE),
    (@receta, @p_papa, 500, FALSE, FALSE),
    (@receta, @p_yuca, 500, FALSE, FALSE),
    (@receta, @p_platano_verde, 2, FALSE, FALSE),
    (@receta, @p_mazorca, 2, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_cilantro, 20, FALSE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_aguacate, 1, TRUE, FALSE),
    (@receta, @p_aceite_vegetal, 15, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 3. Ajiaco santafereño
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Ajiaco santafereño',
    'Sopa bogotana de pollo con tres papas, mazorca y guascas. Se sirve con arroz, aguacate y crema.',
    80,
    4,
    CONCAT_WS(CHAR(10),
        'Pon el pollo en una olla con 3 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 25 minutos.',
        'Mientras tanto, pela la papa y la papa criolla y córtalas en rodajas delgadas. Corta las mazorcas en ruedas.',
        'Saca el pollo de la olla y déjalo enfriar en un plato.',
        'Echa a la olla la papa, la papa criolla y la mazorca y cocina 30 minutos, hasta que la papa criolla se deshaga y espese la sopa.',
        'Desmecha el pollo con los dedos y devuélvelo a la olla.',
        'Agrega las guascas y cocina 5 minutos más.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve el ajiaco con arroz, aguacate y, si quieres, una cucharada de crema de leche.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 600, FALSE, TRUE),
    (@receta, @p_papa, 500, FALSE, TRUE),
    (@receta, @p_papa_criolla, 500, FALSE, FALSE),
    (@receta, @p_mazorca, 2, FALSE, FALSE),
    (@receta, @p_guascas, 10, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_aguacate, 1, FALSE, FALSE),
    (@receta, @p_crema_de_leche, 200, TRUE, FALSE),
    (@receta, @p_aceite_vegetal, 15, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 4. Sudado de pollo con papa y arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sudado de pollo con papa y arroz',
    'Pollo cocinado en su jugo con tomate, cebolla y papa, servido con arroz blanco.',
    60,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el pollo en presas y échale la mitad de la sal, el comino y el color.',
        'Pica el tomate, la cebolla y el ajo. Pela la papa y córtala en cuartos.',
        'Calienta 2 cucharadas de aceite en una olla y sofríe la cebolla, el tomate y el ajo 5 minutos.',
        'Agrega el pollo, revuelve con el sofrito, tapa y cocina 15 minutos a fuego medio.',
        'Añade la papa, el resto de la sal y 300 ml de agua. Tapa y cocina 25 minutos, hasta que la papa esté blanda.',
        'Mientras tanto, lava el arroz y ponlo en otra olla con 800 ml de agua y el resto del aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el cilantro, échalo sobre el sudado y sirve con el arroz.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 700, FALSE, TRUE),
    (@receta, @p_papa, 600, FALSE, FALSE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla, 150, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_color_achiote, 3, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 45, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 5. Carne en bistec con arroz y papa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Carne en bistec con arroz y papa',
    'Carne de res en salsa de tomate y cebolla, con arroz y papa cocida.',
    55,
    4,
    CONCAT_WS(CHAR(10),
        'Corta la carne en filetes delgados y adóbala con el ajo machacado, el comino y la mitad de la sal.',
        'Pela la papa, córtala en mitades y ponla en una olla con agua y un poco de sal.',
        'Cuando el agua hierva, cocina la papa 20 minutos, hasta que esté blanda.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua y una cucharada de aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Corta la cebolla en aros y el tomate en cubos.',
        'Calienta el resto del aceite en una sartén y dora los filetes 3 minutos por cada lado. Sácalos.',
        'En la misma sartén sofríe la cebolla y el tomate con el resto de la sal 5 minutos.',
        'Devuelve la carne, agrega medio pocillo de agua, tapa y cocina 10 minutos.',
        'Sirve el bistec con su salsa, el arroz y la papa.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_carne_de_res, 600, FALSE, TRUE),
    (@receta, @p_cebolla, 200, FALSE, FALSE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_papa, 500, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 50, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 6. Lentejas con chorizo, arroz y maduro
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Lentejas con chorizo, arroz y maduro',
    'Lentejas espesas con hogao y chorizo, con arroz blanco y tajadas de maduro.',
    60,
    4,
    CONCAT_WS(CHAR(10),
        'Lava las lentejas y quita las piedritas.',
        'Ponlas en una olla con 1 litro y medio de agua. Cuando hierva, baja el fuego y cocina 25 minutos.',
        'Pica el tomate, la cebolla, el ajo y el chorizo en rodajas.',
        'En una sartén con una cucharada de aceite, fríe el chorizo con la cebolla, el tomate y el ajo 6 minutos.',
        'Pasa el sofrito a las lentejas con el comino y la mitad de la sal y cocina destapado 10 minutos para que espesen.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los maduros, córtalos en tajadas y fríelos en el aceite que queda hasta que estén dorados.',
        'Sirve las lentejas con el arroz y las tajadas.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_lentejas, 400, FALSE, TRUE),
    (@receta, @p_chorizo, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_platano_maduro, 2, FALSE, FALSE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 60, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 7. Tilapia frita con patacón y arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Tilapia frita con patacón y arroz',
    'Pescado frito con limón, patacones, arroz y ensalada de lechuga y tomate.',
    50,
    4,
    CONCAT_WS(CHAR(10),
        'Adoba los filetes con el ajo machacado, el jugo de un limón y la mitad de la sal.',
        'Lava el arroz y ponlo en una olla con 800 ml de agua y una cucharada de aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los plátanos verdes y córtalos en trozos de dos dedos de grueso.',
        'Fríe los trozos de plátano en aceite caliente 4 minutos, hasta que estén amarillos.',
        'Sácalos, aplástalos con el fondo de un plato y vuelve a freírlos hasta que estén dorados. Échales sal.',
        'En una sartén con poco aceite, fríe los filetes 4 minutos por cada lado.',
        'Pica la lechuga y el tomate y aliña con el otro limón.',
        'Sirve el pescado con patacones, arroz y la ensalada.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_filete_de_tilapia, 600, FALSE, TRUE),
    (@receta, @p_platano_verde, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_limon, 2, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_lechuga, 1, FALSE, FALSE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 150, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 8. Cerdo en salsa criolla con arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Cerdo en salsa criolla con arroz',
    'Trozos de cerdo en salsa de tomate, pimentón y cebolla, con arroz y ensalada de repollo.',
    60,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el cerdo en cubos y échale el ajo machacado, el comino y la mitad de la sal.',
        'Pica el tomate, la cebolla y el pimentón.',
        'Calienta 2 cucharadas de aceite en una olla y dora el cerdo 8 minutos.',
        'Agrega el tomate, la cebolla y el pimentón, revuelve y añade 200 ml de agua.',
        'Tapa y cocina a fuego bajo 30 minutos, hasta que la carne esté blanda y la salsa espesa.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Corta el repollo bien delgado y aliñalo con el jugo del limón.',
        'Sirve el cerdo con el arroz y la ensalada de repollo.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_carne_de_cerdo, 600, FALSE, TRUE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla, 150, FALSE, FALSE),
    (@receta, @p_pimenton, 1, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_repollo, 0.5, FALSE, FALSE),
    (@receta, @p_limon, 1, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 45, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 9. Albóndigas en salsa con arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Albóndigas en salsa con arroz',
    'Albóndigas de carne molida en salsa de tomate casera, con arroz blanco. Les encantan a los niños.',
    50,
    4,
    CONCAT_WS(CHAR(10),
        'Pica muy fino la mitad de la cebolla y el ajo.',
        'En un tazón mezcla la carne molida, el huevo, la harina de maíz, la cebolla picada, el ajo, el comino y la mitad de la sal.',
        'Forma bolitas del tamaño de una nuez.',
        'Licúa el tomate con el resto de la cebolla y 100 ml de agua.',
        'Calienta 2 cucharadas de aceite en una olla y dora las albóndigas 6 minutos, moviéndolas con cuidado.',
        'Echa la salsa licuada, tapa y cocina a fuego bajo 20 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve las albóndigas con su salsa sobre el arroz.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_carne_molida, 500, FALSE, TRUE),
    (@receta, @p_huevos, 1, FALSE, FALSE),
    (@receta, @p_harina_de_maiz_precocida, 40, FALSE, FALSE),
    (@receta, @p_tomate, 400, FALSE, FALSE),
    (@receta, @p_cebolla, 150, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 45, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 10. Garbanzos guisados con cerdo y arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Garbanzos guisados con cerdo y arroz',
    'Garbanzos con cerdo, zanahoria y hogao, servidos con arroz.',
    90,
    4,
    CONCAT_WS(CHAR(10),
        'Escurre los garbanzos que dejaste en remojo y ponlos en una olla con 2 litros de agua limpia.',
        'Cuando hierva, baja el fuego, tapa y cocina 50 minutos, hasta que estén blandos.',
        'Corta el cerdo en cubos pequeños, la zanahoria en rodajas y pica el tomate, la cebolla y el ajo.',
        'En una sartén con 2 cucharadas de aceite, dora el cerdo 8 minutos.',
        'Agrega el tomate, la cebolla y el ajo al cerdo y sofríe 5 minutos.',
        'Pasa el cerdo con el sofrito a los garbanzos, añade la zanahoria, el comino y la mitad de la sal. Cocina 15 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve los garbanzos con el arroz.'
    ),
    'Deja los garbanzos en remojo con agua la noche anterior.',
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_garbanzo, 400, FALSE, TRUE),
    (@receta, @p_carne_de_cerdo, 300, FALSE, FALSE),
    (@receta, @p_zanahoria, 150, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 45, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 11. Pollo asado en sartén con papa criolla
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pollo asado en sartén con papa criolla',
    'Presas de pollo doradas con limón y ajo, papas criollas y ensalada fresca.',
    50,
    4,
    CONCAT_WS(CHAR(10),
        'Adoba las presas de pollo con el jugo de un limón, el ajo machacado, el comino y la mitad de la sal.',
        'Lava las papas criollas y ponlas enteras en una olla con agua y el resto de la sal.',
        'Cuando el agua hierva, cocina las papas 15 minutos.',
        'Calienta el aceite en una sartén grande y pon las presas con la piel hacia abajo.',
        'Tapa y cocina a fuego medio 25 minutos, volteando las presas a la mitad del tiempo, hasta que estén doradas y sin jugo rosado.',
        'Pica la lechuga y el tomate y aliña con el otro limón.',
        'Sirve el pollo con las papas criollas y la ensalada.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 800, FALSE, TRUE),
    (@receta, @p_papa_criolla, 600, FALSE, FALSE),
    (@receta, @p_limon, 2, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_lechuga, 1, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 40, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 12. Arroz atollado
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Arroz atollado',
    'Arroz caldoso del Valle con pollo, cerdo, chorizo y papa. Plato completo en una olla.',
    70,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el pollo y el cerdo en trozos y el chorizo en rodajas. Échales la mitad de la sal.',
        'Pica el tomate, la cebolla larga, el pimentón y el ajo. Pela la papa y córtala en cubos.',
        'En una olla grande con el aceite, dora el pollo, el cerdo y el chorizo 10 minutos.',
        'Agrega el tomate, la cebolla, el pimentón, el ajo y el color y sofríe 5 minutos.',
        'Añade la papa y 1 litro y medio de agua. Cuando hierva, cocina 10 minutos.',
        'Lava el arroz, échalo a la olla con el resto de la sal y revuelve.',
        'Cocina destapado a fuego bajo 25 minutos, revolviendo de vez en cuando. Debe quedar caldoso, no seco.',
        'Sirve enseguida.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_arroz, 400, FALSE, TRUE),
    (@receta, @p_pollo, 400, FALSE, FALSE),
    (@receta, @p_carne_de_cerdo, 300, FALSE, FALSE),
    (@receta, @p_chorizo, 2, FALSE, FALSE),
    (@receta, @p_papa, 400, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_pimenton, 1, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_color_achiote, 3, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 40, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 13. Pasta boloñesa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pasta boloñesa',
    'Pasta con salsa de carne molida, tomate y zanahoria. Rápida y le gusta a toda la familia.',
    40,
    4,
    CONCAT_WS(CHAR(10),
        'Pica la cebolla, el ajo y el tomate. Ralla la zanahoria.',
        'Calienta el aceite en una olla y dora la carne molida 8 minutos, revolviendo para que se suelte.',
        'Agrega la cebolla, el ajo y la zanahoria y sofríe 5 minutos.',
        'Añade el tomate y la mitad de la sal, tapa y cocina a fuego bajo 15 minutos.',
        'Pon a hervir 3 litros de agua con el resto de la sal en otra olla.',
        'Cuando hierva, echa la pasta y cocínala 10 minutos, revolviendo de vez en cuando.',
        'Escurre la pasta y sírvela con la salsa encima y, si quieres, queso rallado.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pasta, 500, FALSE, TRUE),
    (@receta, @p_carne_molida, 400, FALSE, TRUE),
    (@receta, @p_tomate, 500, FALSE, FALSE),
    (@receta, @p_cebolla, 150, FALSE, FALSE),
    (@receta, @p_zanahoria, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_queso, 80, TRUE, FALSE),
    (@receta, @p_aceite_vegetal, 30, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 14. Pollo a la plancha con arroz y aguacate
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pollo a la plancha con arroz y aguacate',
    'Pechuga o muslos a la plancha con limón, arroz blanco, aguacate y ensalada de tomate.',
    40,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el pollo en filetes delgados y adóbalo con el jugo de un limón, el ajo machacado y la mitad de la sal.',
        'Lava el arroz y ponlo en una olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Calienta una sartén con el aceite que queda y asa los filetes 5 minutos por cada lado.',
        'Pica el tomate y la cebolla y aliña con el otro limón.',
        'Sirve el pollo con el arroz, el aguacate en tajadas y la ensalada.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 600, FALSE, TRUE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_aguacate, 1, FALSE, FALSE),
    (@receta, @p_limon, 2, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 35, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 15. Sopa de plátano con carne
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sopa de plátano con carne',
    'Sopa espesa de plátano verde rallado con carne de res y papa. Se acompaña con arroz.',
    60,
    4,
    CONCAT_WS(CHAR(10),
        'Corta la carne en cubos pequeños y ponla en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Pela los plátanos y rállalos por el lado grueso del rallador. Pela la papa y córtala en cubos.',
        'Agrega el plátano rallado, la papa y el comino, revolviendo para que no se pegue.',
        'Cocina 20 minutos a fuego bajo, revolviendo de vez en cuando, hasta que espese.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el cilantro, échalo a la sopa y sirve con el arroz aparte.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_platano_verde, 3, FALSE, TRUE),
    (@receta, @p_carne_de_res, 300, FALSE, FALSE),
    (@receta, @p_papa, 300, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_cilantro, 15, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 25, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 16. Picadillo de carne con papa y arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Picadillo de carne con papa y arroz',
    'Carne molida guisada con papa, arveja y zanahoria, con arroz blanco.',
    45,
    4,
    CONCAT_WS(CHAR(10),
        'Pela la papa y la zanahoria y córtalas en cubos pequeños. Pica el tomate, la cebolla y el ajo.',
        'Calienta 2 cucharadas de aceite en una olla y dora la carne molida 8 minutos.',
        'Agrega la cebolla, el tomate y el ajo y sofríe 5 minutos.',
        'Añade la papa, la zanahoria, la arveja, el comino, la mitad de la sal y 300 ml de agua.',
        'Tapa y cocina a fuego bajo 20 minutos, hasta que la papa esté blanda.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, el resto del aceite y de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve el picadillo con el arroz.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_carne_molida, 500, FALSE, TRUE),
    (@receta, @p_papa, 500, FALSE, FALSE),
    (@receta, @p_arveja_verde, 200, FALSE, FALSE),
    (@receta, @p_zanahoria, 150, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 40, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 17. Sardinas guisadas con arroz y patacón
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sardinas guisadas con arroz y patacón',
    'Sardinas en salsa de tomate y cebolla, con arroz y patacones. Almuerzo económico.',
    40,
    4,
    CONCAT_WS(CHAR(10),
        'Lava el arroz y ponlo en una olla con 800 ml de agua, una cucharada de aceite y la mitad de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el tomate, la cebolla y el ajo.',
        'En una sartén con una cucharada de aceite, sofríe la cebolla, el tomate y el ajo 6 minutos.',
        'Agrega las sardinas con su salsa, desmenúzalas un poco y calienta 5 minutos.',
        'Pela los plátanos, córtalos en trozos y fríelos en aceite caliente 4 minutos.',
        'Aplasta los trozos y fríelos otra vez hasta que estén dorados. Échales el resto de la sal.',
        'Sirve las sardinas con el arroz y los patacones.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_sardinas_en_lata, 2, FALSE, TRUE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_platano_verde, 2, FALSE, FALSE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla, 150, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 120, FALSE, FALSE),
    (@receta, @p_sal, 8, FALSE, FALSE);

-- =====================================================================
-- 18. Sopa de ahuyama con pollo y arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sopa de ahuyama con pollo y arroz',
    'Sopa cremosa de ahuyama con pollo desmechado y papa, con arroz aparte.',
    55,
    4,
    CONCAT_WS(CHAR(10),
        'Pon el pollo en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Pela la ahuyama y la papa y córtalas en cubos.',
        'Saca el pollo y desmenúzalo. Echa a la olla la ahuyama y la papa.',
        'Cocina 20 minutos, hasta que la ahuyama se deshaga al tocarla.',
        'Licúa la mitad de la sopa y devuélvela a la olla con el pollo desmechado.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve con cilantro picado, arroz aparte y, si quieres, un chorrito de crema de leche.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_ahuyama, 600, FALSE, TRUE),
    (@receta, @p_pollo, 400, FALSE, TRUE),
    (@receta, @p_papa, 300, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_crema_de_leche, 100, TRUE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 20, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 19. Cerdo guisado con yuca
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Cerdo guisado con yuca',
    'Cerdo en hogao con color y comino, acompañado de yuca cocida.',
    70,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el cerdo en cubos y échale el ajo, el comino y la mitad de la sal.',
        'Pica el tomate y la cebolla larga.',
        'En una olla con el aceite, dora el cerdo 8 minutos.',
        'Agrega el tomate, la cebolla y el color, revuelve y añade 250 ml de agua.',
        'Tapa y cocina a fuego bajo 35 minutos, hasta que el cerdo esté blando.',
        'Pela la yuca, córtala en trozos y ponla en otra olla con agua y el resto de la sal.',
        'Cuando el agua hierva, cocina la yuca 25 minutos, hasta que esté blanda y se abra.',
        'Sirve el cerdo con su salsa y la yuca.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_carne_de_cerdo, 600, FALSE, TRUE),
    (@receta, @p_yuca, 800, FALSE, FALSE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_color_achiote, 3, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 30, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 20. Pollo con habichuela y zanahoria
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pollo con habichuela y zanahoria',
    'Pollo salteado con habichuela y zanahoria, con arroz blanco.',
    45,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el pollo en tiras y échale el ajo, el comino y la mitad de la sal.',
        'Corta la habichuela en trozos, la zanahoria en tiras y la cebolla en plumas.',
        'Lava el arroz y ponlo en una olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'En una sartén grande con el aceite que queda, dora el pollo 8 minutos.',
        'Agrega la habichuela, la zanahoria y la cebolla y saltea 8 minutos, hasta que estén tiernas pero crocantes.',
        'Sirve el pollo con las verduras y el arroz.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 600, FALSE, TRUE),
    (@receta, @p_habichuela, 300, FALSE, FALSE),
    (@receta, @p_zanahoria, 200, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 40, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 21. Carne desmechada con arroz y maduro
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Carne desmechada con arroz y maduro',
    'Ropa vieja: carne de res desmechada en hogao con pimentón, arroz y tajadas de maduro.',
    90,
    4,
    CONCAT_WS(CHAR(10),
        'Pon la carne entera en una olla con 2 litros de agua y la mitad de la sal.',
        'Cuando hierva, baja el fuego y cocina 50 minutos, hasta que se deje desmechar.',
        'Saca la carne y desmenúzala con dos tenedores.',
        'Pica el tomate, la cebolla, el pimentón y el ajo.',
        'En una sartén con una cucharada de aceite, sofríe el picado con el comino 6 minutos.',
        'Agrega la carne desmechada y 100 ml del caldo y cocina 5 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los maduros, córtalos en tajadas y fríelos en el aceite que queda.',
        'Sirve la carne con el arroz y las tajadas.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_carne_de_res, 600, FALSE, TRUE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla, 150, FALSE, FALSE),
    (@receta, @p_pimenton, 1, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_platano_maduro, 2, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 60, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 22. Pasta con pollo en salsa blanca
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pasta con pollo en salsa blanca',
    'Pasta con pollo y crema de leche, cebolla y ajo. Lista en media hora.',
    35,
    4,
    CONCAT_WS(CHAR(10),
        'Pon a hervir 3 litros de agua con la mitad de la sal.',
        'Corta el pollo en cubos pequeños y pica la cebolla y el ajo.',
        'Cuando el agua hierva, echa la pasta y cocínala 10 minutos.',
        'Mientras tanto, dora el pollo en una sartén con el aceite durante 8 minutos.',
        'Agrega la cebolla y el ajo y sofríe 3 minutos.',
        'Baja el fuego, echa la crema de leche con el resto de la sal y deja que hierva despacio 3 minutos.',
        'Escurre la pasta, mézclala con la salsa y sirve con queso rallado si quieres.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pasta, 500, FALSE, TRUE),
    (@receta, @p_pollo, 400, FALSE, TRUE),
    (@receta, @p_crema_de_leche, 200, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_queso, 80, TRUE, FALSE),
    (@receta, @p_aceite_vegetal, 20, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 23. Arepas rellenas de pollo y queso
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Arepas rellenas de pollo y queso',
    'Arepas asadas rellenas de pollo desmechado en hogao y queso.',
    45,
    4,
    CONCAT_WS(CHAR(10),
        'Pon el pollo en una olla con 1 litro de agua y la mitad de la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Saca el pollo y desmenúzalo.',
        'Pica el tomate, la cebolla y el ajo y sofríelos en el aceite 5 minutos.',
        'Agrega el pollo desmechado y el resto de la sal y cocina 3 minutos.',
        'Asa las arepas en una sartén 3 minutos por cada lado.',
        'Ábrelas por un lado y rellénalas con el pollo y un trozo de queso.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_arepa_de_maiz, 8, FALSE, TRUE),
    (@receta, @p_pollo, 400, FALSE, TRUE),
    (@receta, @p_queso, 100, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 20, FALSE, FALSE),
    (@receta, @p_sal, 6, FALSE, FALSE);

-- =====================================================================
-- 24. Sopa de pasta con pollo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sopa de pasta con pollo',
    'Sopa casera de fideos con pollo, papa y zanahoria. Ideal para la noche.',
    40,
    4,
    CONCAT_WS(CHAR(10),
        'Pon el pollo en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 15 minutos.',
        'Pela la papa y la zanahoria y córtalas en cubos pequeños.',
        'Saca el pollo, desmenúzalo y devuélvelo a la olla con la papa y la zanahoria.',
        'Cocina 10 minutos.',
        'Echa la pasta y cocina 8 minutos más.',
        'Pica el cilantro y échalo antes de servir.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pasta, 200, FALSE, TRUE),
    (@receta, @p_pollo, 300, FALSE, FALSE),
    (@receta, @p_papa, 300, FALSE, FALSE),
    (@receta, @p_zanahoria, 100, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_sal, 8, FALSE, FALSE);

-- =====================================================================
-- 25. Crema de ahuyama
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Crema de ahuyama',
    'Crema suave de ahuyama con leche y mantequilla. Se acompaña con pan.',
    35,
    4,
    CONCAT_WS(CHAR(10),
        'Pela la ahuyama y córtala en cubos. Pica la cebolla y el ajo.',
        'Derrite la mantequilla en una olla y sofríe la cebolla y el ajo 4 minutos.',
        'Agrega la ahuyama y 600 ml de agua. Cuando hierva, cocina 20 minutos.',
        'Licúa todo con la leche y la sal.',
        'Devuelve la crema a la olla y caliéntala sin que hierva.',
        'Sirve con pan si quieres.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_ahuyama, 800, FALSE, TRUE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_leche, 300, FALSE, FALSE),
    (@receta, @p_mantequilla, 20, FALSE, FALSE),
    (@receta, @p_pan, 4, TRUE, FALSE),
    (@receta, @p_sal, 6, FALSE, FALSE);

-- =====================================================================
-- 26. Patacones con carne desmechada y hogao
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Patacones con carne desmechada y hogao',
    'Patacones crocantes cubiertos con carne desmechada en hogao.',
    60,
    4,
    CONCAT_WS(CHAR(10),
        'Pon la carne en una olla con 1 litro y medio de agua y la mitad de la sal.',
        'Cuando hierva, baja el fuego y cocina 40 minutos, hasta que se deje desmechar.',
        'Desmecha la carne con dos tenedores.',
        'Pica el tomate, la cebolla larga y el ajo y sofríelos en una cucharada de aceite 6 minutos.',
        'Agrega la carne y un poco del caldo y cocina 5 minutos.',
        'Pela los plátanos, córtalos en trozos y fríelos en aceite caliente 4 minutos.',
        'Aplasta los trozos y fríelos otra vez hasta que estén dorados. Échales el resto de la sal.',
        'Sirve los patacones con la carne encima.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_platano_verde, 3, FALSE, TRUE),
    (@receta, @p_carne_de_res, 400, FALSE, TRUE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 150, FALSE, FALSE),
    (@receta, @p_sal, 8, FALSE, FALSE);

-- =====================================================================
-- 27. Huevos pericos con arepa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Huevos pericos con arepa',
    'El desayuno de siempre: huevos con tomate y cebolla larga, arepa asada y mantequilla.',
    20,
    4,
    CONCAT_WS(CHAR(10),
        'Pica el tomate y la cebolla larga.',
        'Calienta el aceite en una sartén y sofríe el tomate y la cebolla 4 minutos.',
        'Bate los huevos con la sal y échalos a la sartén.',
        'Revuelve despacio a fuego bajo hasta que estén cocidos pero jugosos.',
        'Asa las arepas en otra sartén 3 minutos por cada lado.',
        'Sirve los pericos con la arepa y, si quieres, mantequilla.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_huevos, 8, FALSE, TRUE),
    (@receta, @p_arepa_de_maiz, 4, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_mantequilla, 20, TRUE, FALSE),
    (@receta, @p_aceite_vegetal, 15, FALSE, FALSE),
    (@receta, @p_sal, 5, FALSE, FALSE);

-- =====================================================================
-- 28. Calentado con huevo y arepa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Calentado con huevo y arepa',
    'Arroz con carne molida y hogao, huevo frito y arepa. Desayuno paisa o comida rápida en la noche.',
    40,
    4,
    CONCAT_WS(CHAR(10),
        'Si no tienes arroz del día anterior, lava el arroz y ponlo en una olla con 600 ml de agua y la mitad de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el tomate y la cebolla larga.',
        'En una sartén grande con una cucharada de aceite, dora la carne molida con el tomate y la cebolla 8 minutos.',
        'Agrega el arroz y el resto de la sal y revuelve a fuego medio 5 minutos.',
        'Fríe los huevos en otra sartén con el aceite que queda.',
        'Asa las arepas.',
        'Sirve el calentado con un huevo encima y la arepa al lado.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_arroz, 300, FALSE, TRUE),
    (@receta, @p_carne_molida, 250, FALSE, FALSE),
    (@receta, @p_huevos, 4, FALSE, FALSE),
    (@receta, @p_arepa_de_maiz, 4, FALSE, FALSE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 40, FALSE, FALSE),
    (@receta, @p_sal, 8, FALSE, FALSE);

-- =====================================================================
-- 29. Huevos rancheros con arepa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Huevos rancheros con arepa',
    'Huevos en salsa de tomate, pimentón y cebolla, con arepa.',
    25,
    4,
    CONCAT_WS(CHAR(10),
        'Pica el tomate, la cebolla, el pimentón y el ajo.',
        'Sofríe el picado en una sartén con el aceite 6 minutos.',
        'Haz huecos en la salsa y rompe un huevo en cada uno. Échales la sal.',
        'Tapa la sartén y cocina a fuego bajo 5 minutos, hasta que la clara cuaje.',
        'Asa las arepas y sirve los huevos con su salsa.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_huevos, 8, FALSE, TRUE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla, 100, FALSE, FALSE),
    (@receta, @p_pimenton, 1, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_arepa_de_maiz, 4, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 20, FALSE, FALSE),
    (@receta, @p_sal, 5, FALSE, FALSE);

-- =====================================================================
-- 30. Changua
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Changua',
    'Caldo de leche con huevo, cebolla larga y cilantro, típico del desayuno bogotano.',
    20,
    4,
    CONCAT_WS(CHAR(10),
        'Pica muy fino la cebolla larga y el cilantro.',
        'Pon en una olla la leche, 500 ml de agua, la cebolla y la sal.',
        'Cuando hierva, baja el fuego.',
        'Rompe los huevos uno por uno dentro del caldo, sin revolver, y cocina 4 minutos.',
        'Sirve en tazones con cilantro y, si quieres, pan en trozos.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_leche, 1000, FALSE, TRUE),
    (@receta, @p_huevos, 4, FALSE, TRUE),
    (@receta, @p_cebolla_larga, 50, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_pan, 4, TRUE, FALSE),
    (@receta, @p_sal, 5, FALSE, FALSE);

-- =====================================================================
-- 31. Arepa con queso y chocolate
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Arepa con queso y chocolate',
    'Arepa asada con queso y mantequilla y una taza de chocolate en leche.',
    15,
    4,
    CONCAT_WS(CHAR(10),
        'Pon la leche en una olla con las pastillas de chocolate partidas.',
        'Cuando hierva, baja el fuego y bate con el molinillo o un batidor hasta que haga espuma.',
        'Asa las arepas en una sartén 3 minutos por cada lado.',
        'Úntalas de mantequilla y ponles una tajada de queso encima.',
        'Sirve las arepas con el chocolate caliente.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_arepa_de_maiz, 4, FALSE, TRUE),
    (@receta, @p_queso, 150, FALSE, FALSE),
    (@receta, @p_mantequilla, 20, FALSE, FALSE),
    (@receta, @p_chocolate_de_mesa, 100, FALSE, FALSE),
    (@receta, @p_leche, 800, FALSE, FALSE);

-- =====================================================================
-- 32. Caldo de costilla
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Caldo de costilla',
    'Caldo de costilla de res con papa y cilantro. Se acompaña con arepa.',
    60,
    4,
    CONCAT_WS(CHAR(10),
        'Pon la costilla en una olla con 2 litros de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego, quita la espuma y cocina 35 minutos.',
        'Pela la papa y córtala en trozos.',
        'Agrega la papa y cocina 20 minutos, hasta que esté blanda.',
        'Pica el cilantro y échalo al final.',
        'Sirve con arepa asada.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_costilla_de_res, 600, FALSE, TRUE),
    (@receta, @p_papa, 500, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_cilantro, 15, FALSE, FALSE),
    (@receta, @p_arepa_de_maiz, 4, FALSE, FALSE),
    (@receta, @p_sal, 8, FALSE, FALSE);

-- =====================================================================
-- 33. Pan con huevo y café con leche
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pan con huevo y café con leche',
    'Huevos fritos con pan, y café con leche endulzado con panela.',
    15,
    4,
    CONCAT_WS(CHAR(10),
        'Pon 500 ml de agua con la panela en una olla.',
        'Cuando hierva, apaga, echa el café, revuelve y deja reposar 3 minutos.',
        'Cuela el café y mézclalo con la leche caliente.',
        'Derrite la mantequilla en una sartén y fríe los huevos con la sal.',
        'Sirve los huevos con el pan y el café con leche.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pan, 4, FALSE, FALSE),
    (@receta, @p_huevos, 4, FALSE, TRUE),
    (@receta, @p_mantequilla, 20, FALSE, FALSE),
    (@receta, @p_cafe_molido, 30, FALSE, FALSE),
    (@receta, @p_leche, 500, FALSE, FALSE),
    (@receta, @p_panela, 60, FALSE, FALSE),
    (@receta, @p_sal, 3, FALSE, FALSE);

-- =====================================================================
-- 34. Avena con leche y banano
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Avena con leche y banano',
    'Avena en hojuelas cocida en leche con panela y banano en rodajas.',
    15,
    4,
    CONCAT_WS(CHAR(10),
        'Pon la leche con la panela rallada en una olla.',
        'Cuando hierva, echa la avena y revuelve.',
        'Cocina a fuego bajo 5 minutos, revolviendo para que no se pegue.',
        'Sirve con el banano en rodajas.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_avena_en_hojuelas, 160, FALSE, TRUE),
    (@receta, @p_leche, 1000, FALSE, FALSE),
    (@receta, @p_banano, 2, FALSE, FALSE),
    (@receta, @p_panela, 80, FALSE, FALSE);

-- =====================================================================
-- 35. Tortilla de huevo con maduro y queso
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Tortilla de huevo con maduro y queso',
    'Tortilla de huevo con tajadas de plátano maduro y queso.',
    25,
    4,
    CONCAT_WS(CHAR(10),
        'Pela los maduros y córtalos en rodajas delgadas. Pica la cebolla larga.',
        'Fríe el maduro en una sartén con el aceite 5 minutos, hasta que esté dorado.',
        'Bate los huevos con la sal y la cebolla y échalos sobre el maduro.',
        'Pon el queso en trozos encima, tapa y cocina a fuego bajo 6 minutos.',
        'Sirve en porciones.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_huevos, 6, FALSE, TRUE),
    (@receta, @p_platano_maduro, 2, FALSE, FALSE),
    (@receta, @p_queso, 100, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 50, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 30, FALSE, FALSE),
    (@receta, @p_sal, 4, FALSE, FALSE);

-- =====================================================================
-- 36. Ensalada de aguacate, tomate y cebolla
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Ensalada de aguacate, tomate y cebolla',
    'Ensalada fresca para acompañar el almuerzo.',
    10,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el aguacate y el tomate en cubos y la cebolla en plumas delgadas.',
        'Mezcla todo con el jugo del limón y la sal.',
        'Sirve enseguida para que el aguacate no se oscurezca.'
    ),
    NULL,
    TRUE,
    @sistema,
    FALSE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_aguacate, 1, FALSE, TRUE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_cebolla, 80, FALSE, FALSE),
    (@receta, @p_limon, 1, FALSE, FALSE),
    (@receta, @p_sal, 2, FALSE, FALSE);

-- =====================================================================
-- 37. Jugo de mora
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Jugo de mora',
    'Jugo natural de mora en agua, endulzado con panela.',
    10,
    4,
    CONCAT_WS(CHAR(10),
        'Lava las moras.',
        'Licúa las moras con 1 litro de agua y la panela rallada.',
        'Cuela el jugo y sírvelo frío.'
    ),
    NULL,
    TRUE,
    @sistema,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_mora, 300, FALSE, TRUE),
    (@receta, @p_panela, 80, FALSE, FALSE);

-- =====================================================================
-- 38. Jugo de lulo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Jugo de lulo',
    'Jugo de lulo en agua, ácido y refrescante, endulzado con panela.',
    10,
    4,
    CONCAT_WS(CHAR(10),
        'Lava los lulos, pártelos por la mitad y saca la pulpa con una cuchara.',
        'Licúa la pulpa con 1 litro de agua y la panela rallada.',
        'Cuela el jugo y sírvelo frío.'
    ),
    NULL,
    TRUE,
    @sistema,
    FALSE,
    FALSE,
    TRUE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_lulo, 300, FALSE, TRUE),
    (@receta, @p_panela, 80, FALSE, FALSE);

-- =====================================================================
-- 39. Aguapanela con limón
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Aguapanela con limón',
    'Panela disuelta en agua con limón, fría o caliente.',
    15,
    4,
    CONCAT_WS(CHAR(10),
        'Pon 1 litro y medio de agua con la panela en una olla.',
        'Cuando hierva, baja el fuego y cocina 5 minutos, hasta que la panela se disuelva.',
        'Apaga, deja enfriar y agrega el jugo de los limones.'
    ),
    NULL,
    TRUE,
    @sistema,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_panela, 150, FALSE, TRUE),
    (@receta, @p_limon, 2, FALSE, FALSE);

-- =====================================================================
-- 40. Fruta picada
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Fruta picada',
    'Papaya, mango, piña y banano picados. Para el desayuno o las onces.',
    10,
    4,
    CONCAT_WS(CHAR(10),
        'Pela la papaya, el mango y la piña y córtalos en cubos.',
        'Corta el banano en rodajas.',
        'Mezcla todo con el jugo del limón y sirve.'
    ),
    NULL,
    TRUE,
    @sistema,
    FALSE,
    TRUE,
    TRUE,
    FALSE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_papaya, 0.5, FALSE, TRUE),
    (@receta, @p_mango, 1, FALSE, FALSE),
    (@receta, @p_pina, 0.5, FALSE, FALSE),
    (@receta, @p_banano, 2, FALSE, FALSE),
    (@receta, @p_limon, 1, FALSE, FALSE);

-- =====================================================================
-- 41. Fríjoles con costilla y arroz
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Fríjoles con costilla y arroz',
    'Fríjoles cocinados con costilla de res y hogao, con arroz blanco y tajadas de maduro.',
    110,
    4,
    CONCAT_WS(CHAR(10),
        'Escurre los fríjoles del remojo y ponlos en una olla con la costilla y 2 litros y medio de agua.',
        'Cuando hierva, baja el fuego, tapa y cocina 1 hora y media, hasta que los fríjoles y la costilla estén blandos.',
        'Mientras tanto, pica el tomate, la cebolla larga y el ajo.',
        'Sofríe el tomate, la cebolla y el ajo en una cucharada de aceite 5 minutos para hacer el hogao.',
        'Echa el hogao, el comino y la mitad de la sal a los fríjoles y cocina destapado 15 minutos.',
        'Lava el arroz y ponlo en otra olla con 800 ml de agua, una cucharada de aceite y el resto de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela los maduros, córtalos en tajadas y fríelos en el aceite que queda hasta que doren.',
        'Sirve fríjoles con costilla, arroz y tajadas.'
    ),
    'Deja los fríjoles en remojo con agua la noche anterior.',
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_frijol, 500, FALSE, TRUE),
    (@receta, @p_costilla_de_res, 500, FALSE, TRUE),
    (@receta, @p_arroz, 400, FALSE, FALSE),
    (@receta, @p_platano_maduro, 2, FALSE, FALSE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 100, FALSE, FALSE),
    (@receta, @p_ajo, 10, FALSE, FALSE),
    (@receta, @p_comino, 2, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 50, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 42. Hígado encebollado con arroz y papa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Hígado encebollado con arroz y papa',
    'Hígado de res en tiras con bastante cebolla, arroz blanco y papa cocida.',
    45,
    4,
    CONCAT_WS(CHAR(10),
        'Lava el arroz y ponlo en una olla con 600 ml de agua, una cucharada de aceite y un poco de sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pela las papas y cocínalas en agua con sal 20 minutos.',
        'Mientras tanto, corta el hígado en tiras y la cebolla en plumas. Pica el tomate y el ajo.',
        'Dora el hígado en una sartén caliente con el aceite 4 minutos, sin dejar que se seque.',
        'Agrega la cebolla, el tomate, el ajo y el resto de la sal y cocina 6 minutos.',
        'Sirve el hígado encebollado con arroz y papa.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_higado_de_res, 500, FALSE, TRUE),
    (@receta, @p_cebolla, 300, FALSE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_papa, 500, FALSE, FALSE),
    (@receta, @p_tomate, 100, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 40, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 43. Pescado sudado con arroz y patacón
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pescado sudado con arroz y patacón',
    'Filete de tilapia sudado en salsa de tomate y cebolla, con arroz y patacones.',
    50,
    4,
    CONCAT_WS(CHAR(10),
        'Échale al pescado el jugo del limón y un poco de sal.',
        'Lava el arroz y ponlo en una olla con 600 ml de agua y una cucharada de aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el tomate, la cebolla, el pimentón y el ajo y sofríelos en una cucharada de aceite 6 minutos.',
        'Pon el pescado sobre la salsa, agrega medio pocillo de agua, tapa y cocina 10 minutos a fuego bajo.',
        'Pela los plátanos, córtalos en trozos y fríelos en el resto del aceite 4 minutos.',
        'Aplasta los trozos y fríelos otra vez hasta que doren. Échales sal.',
        'Pica el cilantro, échalo al pescado y sirve con arroz y patacones.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_filete_de_tilapia, 600, FALSE, TRUE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_platano_verde, 2, FALSE, FALSE),
    (@receta, @p_tomate, 300, FALSE, FALSE),
    (@receta, @p_cebolla, 150, FALSE, FALSE),
    (@receta, @p_pimenton, 1, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_limon, 1, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 120, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 44. Arroz con cerdo y verduras
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Arroz con cerdo y verduras',
    'Arroz salteado con cerdo en trocitos, zanahoria, habichuela, arveja y huevo.',
    45,
    4,
    CONCAT_WS(CHAR(10),
        'Lava el arroz y ponlo en una olla con 800 ml de agua y la mitad de la sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Mientras tanto, corta el cerdo en cubos pequeños y pica la zanahoria, la habichuela y la cebolla larga.',
        'Dora el cerdo en una sartén grande con el aceite durante 10 minutos.',
        'Agrega la zanahoria, la habichuela y la arveja y saltea 6 minutos.',
        'Haz un hueco en el centro, echa los huevos batidos y revuelve hasta que cuajen.',
        'Agrega el arroz, la cebolla larga y el resto de la sal y mezcla todo 3 minutos.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_arroz, 400, FALSE, TRUE),
    (@receta, @p_carne_de_cerdo, 400, FALSE, TRUE),
    (@receta, @p_zanahoria, 150, FALSE, FALSE),
    (@receta, @p_habichuela, 150, FALSE, FALSE),
    (@receta, @p_arveja_verde, 150, FALSE, FALSE),
    (@receta, @p_huevos, 2, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 40, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 45. Sopa de arracacha con costilla
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sopa de arracacha con costilla',
    'Sopa espesa de arracacha y papa con costilla de res, servida con arroz y aguacate.',
    80,
    4,
    CONCAT_WS(CHAR(10),
        'Pon la costilla en una olla con 2 litros y medio de agua, la cebolla larga, el ajo y la sal.',
        'Cuando hierva, baja el fuego y cocina 40 minutos.',
        'Pela la arracacha y la papa y córtalas en trozos.',
        'Agrega la arracacha y la papa a la olla y cocina 25 minutos, hasta que se deshagan un poco.',
        'Mientras tanto, lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Pica el cilantro y échalo a la sopa.',
        'Sirve la sopa con arroz aparte y aguacate si tienes.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_costilla_de_res, 500, FALSE, TRUE),
    (@receta, @p_arracacha, 500, FALSE, FALSE),
    (@receta, @p_papa, 300, FALSE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_aguacate, 1, TRUE, FALSE),
    (@receta, @p_aceite_vegetal, 15, FALSE, FALSE),
    (@receta, @p_sal, 12, FALSE, FALSE);

-- =====================================================================
-- 46. Pechuga gratinada con arroz y ensalada
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Pechuga gratinada con arroz y ensalada',
    'Pechuga de pollo a la plancha cubierta con queso derretido, arroz y ensalada de lechuga y tomate.',
    40,
    4,
    CONCAT_WS(CHAR(10),
        'Lava el arroz y ponlo en una olla con 600 ml de agua, una cucharada de aceite y un poco de sal.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Abre las pechugas en filetes delgados y adóbalas con el ajo machacado y el resto de la sal.',
        'Asa los filetes en una sartén con el resto del aceite 5 minutos por cada lado.',
        'Pon una tajada de queso sobre cada filete, tapa la sartén y deja 2 minutos para que se derrita.',
        'Lava la lechuga, córtala con el tomate y aliña con el jugo del limón.',
        'Sirve la pechuga con arroz y ensalada.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 600, FALSE, TRUE),
    (@receta, @p_queso, 150, FALSE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_lechuga, 1, FALSE, FALSE),
    (@receta, @p_tomate, 200, FALSE, FALSE),
    (@receta, @p_limon, 1, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 30, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 47. Cazuela de fríjoles con chorizo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Cazuela de fríjoles con chorizo',
    'Fríjoles espesos con chorizo, maduro y hogao, servidos en cazuela con arroz y aguacate.',
    100,
    4,
    CONCAT_WS(CHAR(10),
        'Escurre los fríjoles del remojo y ponlos en una olla con 2 litros de agua.',
        'Cuando hierva, baja el fuego, tapa y cocina 1 hora.',
        'Mientras tanto, pica el tomate, la cebolla larga y el ajo, y corta el chorizo y el maduro en rodajas.',
        'Dora el chorizo en una sartén 5 minutos y sácalo.',
        'En la misma grasa, sofríe el tomate, la cebolla y el ajo 5 minutos.',
        'Agrega a los fríjoles el hogao, el chorizo, el maduro y la sal, y cocina destapado 20 minutos para que espesen.',
        'Lava el arroz y ponlo en otra olla con 600 ml de agua y el aceite.',
        'Cuando el agua del arroz se seque, baja el fuego al mínimo, tapa y cocina 15 minutos.',
        'Sirve la cazuela con arroz y aguacate si tienes.'
    ),
    'Deja los fríjoles en remojo con agua la noche anterior.',
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    FALSE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_frijol, 500, FALSE, TRUE),
    (@receta, @p_chorizo, 3, FALSE, TRUE),
    (@receta, @p_platano_maduro, 1, FALSE, FALSE),
    (@receta, @p_arroz, 300, FALSE, FALSE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_ajo, 5, FALSE, FALSE),
    (@receta, @p_aguacate, 1, TRUE, FALSE),
    (@receta, @p_aceite_vegetal, 30, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 48. Empanadas de carne y papa
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Empanadas de carne y papa',
    'Empanadas de masa de maíz rellenas de carne molida y papa, fritas y doradas.',
    75,
    4,
    CONCAT_WS(CHAR(10),
        'Pela las papas y cocínalas en agua con sal 20 minutos. Escúrrelas y aplástalas.',
        'Mientras tanto, pica el tomate y la cebolla larga y sofríelos con la carne molida y el comino 10 minutos.',
        'Mezcla la carne con la papa aplastada. Ese es el relleno.',
        'Mezcla la harina con 600 ml de agua tibia, el color y el resto de la sal hasta tener una masa suave.',
        'Haz bolitas de masa, aplánalas sobre un plástico, pon relleno en el centro y dóblalas en media luna.',
        'Calienta el aceite en una olla y fríe las empanadas de a pocas 5 minutos, hasta que doren.',
        'Sácalas sobre papel de cocina y sirve calientes.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_harina_de_maiz_precocida, 500, FALSE, TRUE),
    (@receta, @p_carne_molida, 300, FALSE, TRUE),
    (@receta, @p_papa, 300, FALSE, FALSE),
    (@receta, @p_tomate, 100, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 60, FALSE, FALSE),
    (@receta, @p_color_achiote, 2, FALSE, FALSE),
    (@receta, @p_comino, 1, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 300, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 49. Sándwich de pollo y queso
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sándwich de pollo y queso',
    'Pan con pollo desmechado, queso, lechuga y tomate. Cena rápida.',
    35,
    4,
    CONCAT_WS(CHAR(10),
        'Pon el pollo en una olla con 1 litro de agua y la sal.',
        'Cuando hierva, baja el fuego y cocina 20 minutos.',
        'Saca el pollo y desmenúzalo.',
        'Lava la lechuga y corta el tomate en rodajas.',
        'Arma cada sándwich con pollo, queso, lechuga y tomate.',
        'Si quieres, úntale mantequilla por fuera y dóralo en una sartén 2 minutos por cada lado.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pan, 8, FALSE, TRUE),
    (@receta, @p_pollo, 400, FALSE, TRUE),
    (@receta, @p_queso, 100, FALSE, FALSE),
    (@receta, @p_lechuga, 1, FALSE, FALSE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_mantequilla, 20, TRUE, FALSE),
    (@receta, @p_sal, 5, FALSE, FALSE);

-- =====================================================================
-- 50. Sopa de verduras con pollo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Sopa de verduras con pollo',
    'Sopa liviana de pollo con papa, zanahoria, habichuela y ahuyama.',
    45,
    4,
    CONCAT_WS(CHAR(10),
        'Pon el pollo en una olla con 2 litros de agua, la cebolla larga y la sal.',
        'Cuando hierva, baja el fuego y cocina 15 minutos.',
        'Pela y corta en cubos la papa, la zanahoria y la ahuyama, y la habichuela en trozos.',
        'Agrega las verduras a la olla y cocina 20 minutos.',
        'Saca el pollo, desmenúzalo y devuélvelo a la olla.',
        'Pica el cilantro y échalo antes de servir.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_pollo, 400, FALSE, TRUE),
    (@receta, @p_papa, 300, FALSE, FALSE),
    (@receta, @p_zanahoria, 150, FALSE, FALSE),
    (@receta, @p_habichuela, 150, FALSE, FALSE),
    (@receta, @p_ahuyama, 200, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_sal, 10, FALSE, FALSE);

-- =====================================================================
-- 51. Caldo de papa con huevo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Caldo de papa con huevo',
    'Caldo de papa con huevo, cebolla larga y cilantro, servido con arepa. Para el desayuno o la noche.',
    30,
    4,
    CONCAT_WS(CHAR(10),
        'Pela las papas y córtalas en trozos.',
        'Pon las papas en una olla con 1 litro y medio de agua, la cebolla larga picada y la sal.',
        'Cuando hierva, baja el fuego y cocina 15 minutos.',
        'Agrega la leche y, cuando vuelva a hervir, echa los huevos uno por uno sin revolver.',
        'Deja cocinar los huevos 4 minutos.',
        'Pica el cilantro, échalo y sirve con arepa si quieres.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    FALSE,
    TRUE,
    FALSE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_papa, 600, FALSE, TRUE),
    (@receta, @p_huevos, 4, FALSE, TRUE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_cilantro, 10, FALSE, FALSE),
    (@receta, @p_leche, 200, FALSE, FALSE),
    (@receta, @p_arepa_de_maiz, 4, TRUE, FALSE),
    (@receta, @p_sal, 8, FALSE, FALSE);

-- =====================================================================
-- 52. Migas de arepa con huevo
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Migas de arepa con huevo',
    'Arepa en trocitos revuelta con huevo, tomate y cebolla larga. Desayuno rápido para aprovechar la arepa.',
    15,
    4,
    CONCAT_WS(CHAR(10),
        'Corta las arepas en cuadritos.',
        'Pica el tomate y la cebolla larga.',
        'Sofríe el tomate y la cebolla en el aceite 3 minutos.',
        'Agrega la arepa y dórala 3 minutos.',
        'Echa los huevos batidos con la sal y revuelve hasta que cuajen.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_arepa_de_maiz, 4, FALSE, TRUE),
    (@receta, @p_huevos, 6, FALSE, TRUE),
    (@receta, @p_tomate, 150, FALSE, FALSE),
    (@receta, @p_cebolla_larga, 80, FALSE, FALSE),
    (@receta, @p_aceite_vegetal, 20, FALSE, FALSE),
    (@receta, @p_sal, 4, FALSE, FALSE);

-- =====================================================================
-- 53. Huevos tibios con pan y chocolate
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Huevos tibios con pan y chocolate',
    'Huevos tibios, pan y una taza de chocolate caliente.',
    15,
    4,
    CONCAT_WS(CHAR(10),
        'Pon la leche con el chocolate en una olla.',
        'Cuando hierva, baja el fuego y bate 3 minutos hasta que haga espuma.',
        'En otra olla, pon a hervir agua y echa los huevos con cuidado.',
        'Cocina los huevos 4 minutos y pásalos a agua fría.',
        'Pélalos o pártelos por la mitad, échales sal y sirve con el pan y el chocolate.'
    ),
    NULL,
    TRUE,
    @sistema,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_huevos, 4, FALSE, TRUE),
    (@receta, @p_pan, 4, FALSE, FALSE),
    (@receta, @p_chocolate_de_mesa, 100, FALSE, FALSE),
    (@receta, @p_leche, 600, FALSE, FALSE),
    (@receta, @p_sal, 2, FALSE, FALSE);

-- =====================================================================
-- 54. Jugo de maracuyá
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Jugo de maracuyá',
    'Jugo de maracuyá en agua, endulzado con panela.',
    10,
    4,
    CONCAT_WS(CHAR(10),
        'Parte los maracuyás y saca la pulpa con una cuchara.',
        'Licúa la pulpa con 1 litro de agua y la panela rallada, sin moler mucho las semillas.',
        'Cuela el jugo y sírvelo frío.'
    ),
    NULL,
    TRUE,
    @sistema,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    FALSE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_maracuya, 3, FALSE, TRUE),
    (@receta, @p_panela, 80, FALSE, FALSE);

-- =====================================================================
-- 55. Ensalada de repollo y zanahoria
-- =====================================================================
INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa, activa,
                    modificado_por, es_plato_fuerte, gusta_a_ninos, para_desayuno, para_almuerzo, para_cena)
VALUES (
    'Ensalada de repollo y zanahoria',
    'Repollo y zanahoria rallados con limón. Acompaña cualquier almuerzo.',
    10,
    4,
    CONCAT_WS(CHAR(10),
        'Corta el repollo en tiras muy delgadas y ralla la zanahoria.',
        'Mezcla con el jugo del limón y la sal.',
        'Sirve enseguida.'
    ),
    NULL,
    TRUE,
    @sistema,
    FALSE,
    FALSE,
    FALSE,
    TRUE,
    TRUE
);
SET @receta = LAST_INSERT_ID();

INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal) VALUES
    (@receta, @p_repollo, 0.5, FALSE, TRUE),
    (@receta, @p_zanahoria, 150, FALSE, FALSE),
    (@receta, @p_limon, 1, FALSE, FALSE),
    (@receta, @p_sal, 2, FALSE, FALSE);

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
WHERE id > @ultima_receta;
