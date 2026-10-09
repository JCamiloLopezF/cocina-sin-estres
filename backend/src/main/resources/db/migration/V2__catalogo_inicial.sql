
-- ============================================================
-- USUARIO DE SISTEMA PARA LA CARGA INICIAL
-- ============================================================

INSERT INTO usuario (
    firebase_uid,
    correo,
    rol,
    perfil_completo,
    activo
)
VALUES (
    'sistema-carga-inicial',
    'sistema@cocinasinestres.local',
    'CURADOR',
    TRUE,
    FALSE
);

SET @sistema = (
    SELECT id
    FROM usuario
    WHERE firebase_uid = 'sistema-carga-inicial'
);

-- ============================================================
-- CEREALES Y GRANOS
-- ============================================================

INSERT INTO producto (
    hogar_id, nombre, categoria, tipo_cantidad, unidad_base,
    unidad_compra, cantidad_unidad_compra, precio_unidad_compra,
    capacidad_referencia, es_basico, activo, modificado_por
)
VALUES
(NULL, 'Arroz', 'GRANOS', 'GRANEL', 'g', 'Bolsa', 1000, 4500.00, 1000, TRUE, TRUE, @sistema),
(NULL, 'Lentejas', 'GRANOS', 'GRANEL', 'g', 'Bolsa', 500, 4000.00, 500, TRUE, TRUE, @sistema),
(NULL, 'Frijol', 'GRANOS', 'GRANEL', 'g', 'Bolsa', 500, 5500.00, 500, TRUE, TRUE, @sistema),
(NULL, 'Pasta', 'GRANOS', 'GRANEL', 'g', 'Paquete', 500, 3500.00, 500, TRUE, TRUE, @sistema);

-- ============================================================
-- ACEITES Y CONDIMENTOS
-- ============================================================

INSERT INTO producto (
    hogar_id, nombre, categoria, tipo_cantidad, unidad_base,
    unidad_compra, cantidad_unidad_compra, precio_unidad_compra,
    capacidad_referencia, es_basico, activo, modificado_por
)
VALUES
(NULL, 'Aceite vegetal', 'ACEITES', 'GRANEL', 'ml', 'Botella', 1000, 8000.00, 1000, TRUE, TRUE, @sistema),
(NULL, 'Sal', 'CONDIMENTOS', 'GRANEL', 'g', 'Paquete', 500, 2000.00, 500, TRUE, TRUE, @sistema),
(NULL, 'Azucar', 'CONDIMENTOS', 'GRANEL', 'g', 'Bolsa', 1000, 4500.00, 1000, TRUE, TRUE, @sistema);

-- ============================================================
-- LÁCTEOS
-- ============================================================

INSERT INTO producto (
    hogar_id, nombre, categoria, tipo_cantidad, unidad_base,
    unidad_compra, cantidad_unidad_compra, precio_unidad_compra,
    capacidad_referencia, es_basico, activo, modificado_por
)
VALUES
(NULL, 'Leche', 'LACTEOS', 'GRANEL', 'ml', 'Bolsa', 1000, 4500.00, 1000, TRUE, TRUE, @sistema),
(NULL, 'Queso', 'LACTEOS', 'GRANEL', 'g', 'Paquete', 250, 7000.00, 250, FALSE, TRUE, @sistema);

-- ============================================================
-- PROTEÍNAS
-- ============================================================

INSERT INTO producto (
    hogar_id, nombre, categoria, tipo_cantidad, unidad_base,
    unidad_compra, cantidad_unidad_compra, precio_unidad_compra,
    capacidad_referencia, es_basico, activo, modificado_por
)
VALUES
(NULL, 'Huevos', 'PROTEINAS', 'CONTABLE', 'unidad', 'Cubeta', 30, 18000.00, NULL, TRUE, TRUE, @sistema),
(NULL, 'Pollo', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 12000.00, 500, FALSE, TRUE, @sistema),
(NULL, 'Carne de res', 'PROTEINAS', 'GRANEL', 'g', 'Libra', 500, 18000.00, 500, FALSE, TRUE, @sistema),
(NULL, 'Atun', 'PROTEINAS', 'CONTABLE', 'g', 'Lata', 160, 6000.00, NULL, FALSE, TRUE, @sistema);

-- ============================================================
-- VERDURAS Y TUBÉRCULOS
-- ============================================================

INSERT INTO producto (
    hogar_id, nombre, categoria, tipo_cantidad, unidad_base,
    unidad_compra, cantidad_unidad_compra, precio_unidad_compra,
    capacidad_referencia, es_basico, activo, modificado_por
)
VALUES
(NULL, 'Papa', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 3500.00, 500, TRUE, TRUE, @sistema),
(NULL, 'Tomate', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 4000.00, 500, TRUE, TRUE, @sistema),
(NULL, 'Cebolla', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 3500.00, 500, TRUE, TRUE, @sistema),
(NULL, 'Zanahoria', 'VERDURAS', 'GRANEL', 'g', 'Libra', 500, 3000.00, 500, FALSE, TRUE, @sistema),
(NULL, 'Tomate de arbol', 'FRUTAS', 'GRANEL', 'g', 'Libra', 500, 5000.00, 500, FALSE, TRUE, @sistema);

-- ============================================================
-- FRUTAS
-- ============================================================

INSERT INTO producto (
    hogar_id, nombre, categoria, tipo_cantidad, unidad_base,
    unidad_compra, cantidad_unidad_compra, precio_unidad_compra,
    capacidad_referencia, es_basico, activo, modificado_por
)
VALUES
(NULL, 'Banano', 'FRUTAS', 'CONTABLE', 'unidad', 'Racimo', 6, 5000.00, NULL, FALSE, TRUE, @sistema),
(NULL, 'Manzana', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1500.00, NULL, FALSE, TRUE, @sistema),
(NULL, 'Naranja', 'FRUTAS', 'CONTABLE', 'unidad', 'Unidad', 1, 1000.00, NULL, FALSE, TRUE, @sistema);

-- ============================================================
-- AUDITORÍA DE LA CARGA INICIAL
-- Un registro CREAR por cada producto global cargado.
-- ============================================================

INSERT INTO auditoria_catalogo (
    entidad,
    entidad_id,
    accion,
    usuario_id,
    cambios
)
SELECT
    'PRODUCTO',
    id,
    'CREAR',
    @sistema,
    JSON_OBJECT('origen', 'V2__catalogo_inicial')
FROM producto
WHERE hogar_id IS NULL
  AND modificado_por = @sistema;

