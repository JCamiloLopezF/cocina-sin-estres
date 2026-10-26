-- =====================================================================
-- V13: costo editable en la lista de compras (KAN-26)
--
-- La lista estima el costo con el precio de referencia del catálogo, pero en
-- la tienda el precio cambia. La usuaria corrige lo que pagó de verdad y el
-- total de la lista se ajusta (Definición, proceso 3: "lista de compras
-- priorizada con costo estimado editable").
--
--   item_lista_compras.costo_real  lo que la usuaria dice que pagó; NULL
--                                  mientras no lo corrija (cuenta el estimado)
--
-- El estimado no se borra: así se puede volver a él y comparar.
-- =====================================================================

ALTER TABLE item_lista_compras
    ADD COLUMN costo_real DECIMAL(12,2) NULL AFTER costo_estimado;

ALTER TABLE item_lista_compras
    ADD CONSTRAINT ck_item_lista_compras_costo_real CHECK (costo_real IS NULL OR costo_real >= 0);
