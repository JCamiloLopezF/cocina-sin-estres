-- =====================================================================
-- V9__despensa_automatica.sql
-- Cocina sin estrés · La despensa se actualiza sola (ADR-24, pieza 2; KAN-16)
--
--   producto.vida_util_dias       cuántos días dura el producto desde que se
--                                 compra; con esto lo comprado entra a la
--                                 despensa con un vencimiento estimado
--   item_lista_compras.motivo     por qué está en la lista: PLAN (lo pide el
--                                 plan), REPONER (quedó por debajo del mínimo)
--                                 o ALERGIA (reemplazo o plato aparte de alguien)
--   item_lista_compras.cantidad_agregada
--                                 cuánto entró a la despensa al marcarlo como
--                                 comprado; sirve para deshacerlo
-- =====================================================================

ALTER TABLE producto ADD COLUMN vida_util_dias SMALLINT UNSIGNED NULL;
ALTER TABLE producto ADD CONSTRAINT ck_producto_vida_util CHECK (vida_util_dias IS NULL OR vida_util_dias BETWEEN 1 AND 3650);

ALTER TABLE item_lista_compras
    ADD COLUMN motivo VARCHAR(10) NOT NULL DEFAULT 'PLAN',
    ADD COLUMN cantidad_agregada DECIMAL(12,3) NULL;
ALTER TABLE item_lista_compras
    ADD CONSTRAINT ck_item_lista_compras_motivo CHECK (motivo IN ('PLAN', 'REPONER', 'ALERGIA'));

-- Vida útil de referencia del catálogo inicial, guardado como se guarda en una
-- casa colombiana (granos en la alacena; carnes, lácteos y verduras en la nevera).
-- El curador la puede corregir.
SET @sistema = (
    SELECT id FROM usuario WHERE firebase_uid = 'sistema-carga-inicial'
);

UPDATE producto SET vida_util_dias = CASE nombre
        WHEN 'Arroz'           THEN 365
        WHEN 'Lentejas'        THEN 365
        WHEN 'Frijol'          THEN 365
        WHEN 'Pasta'           THEN 365
        WHEN 'Aceite vegetal'  THEN 365
        WHEN 'Sal'             THEN 1095
        WHEN 'Azúcar'          THEN 730
        WHEN 'Leche'           THEN 7
        WHEN 'Queso'           THEN 14
        WHEN 'Huevos'          THEN 21
        WHEN 'Pollo'           THEN 3
        WHEN 'Carne de res'    THEN 3
        WHEN 'Atún'            THEN 730
        WHEN 'Papa'            THEN 21
        WHEN 'Tomate'          THEN 7
        WHEN 'Cebolla'         THEN 30
        WHEN 'Zanahoria'       THEN 21
        WHEN 'Banano'          THEN 5
        WHEN 'Manzana'         THEN 21
        WHEN 'Naranja'         THEN 14
        WHEN 'Tomate de árbol' THEN 10
    END,
    modificado_por = @sistema
WHERE hogar_id IS NULL
  AND nombre IN ('Arroz', 'Lentejas', 'Frijol', 'Pasta', 'Aceite vegetal', 'Sal', 'Azúcar',
                 'Leche', 'Queso', 'Huevos', 'Pollo', 'Carne de res', 'Atún', 'Papa', 'Tomate',
                 'Cebolla', 'Zanahoria', 'Banano', 'Manzana', 'Naranja', 'Tomate de árbol');

-- Trazabilidad del catálogo (ADR-23).
INSERT INTO auditoria_catalogo (entidad, entidad_id, accion, usuario_id, cambios)
SELECT 'PRODUCTO', id, 'EDITAR', @sistema,
       JSON_OBJECT('origen', 'V9__despensa_automatica', 'campo', 'vidaUtilDias',
                   'despues', vida_util_dias)
FROM producto
WHERE hogar_id IS NULL AND vida_util_dias IS NOT NULL;
