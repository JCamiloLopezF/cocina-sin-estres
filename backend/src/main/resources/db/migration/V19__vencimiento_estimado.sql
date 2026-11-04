-- =====================================================================
-- V19__vencimiento_estimado.sql
-- Cocina sin estrés · Marca la fecha de vencimiento que calcula la aplicación
--
-- Auditoría del 4 de noviembre (no conformidad «Cantidad y vencimiento
-- automáticos»): al agregar un producto sin fecha, o al registrar una compra,
-- la aplicación pone el vencimiento con la vida útil del catálogo (KAN-27),
-- pero la usuaria no sabía que era un cálculo. Con esta columna la despensa
-- lo muestra como «estimado» hasta que la usuaria ponga la fecha del empaque.
--
-- La cantidad estimada ya existía (item_despensa.es_estimada, ADR-15); ahora
-- también la usa la carga rápida para lo que se cuenta.
--
-- Los ítems que ya existen quedan sin marca: no se sabe si su fecha la puso
-- la usuaria o la aplicación.
-- =====================================================================

ALTER TABLE item_despensa
    ADD COLUMN vence_estimado BOOLEAN NOT NULL DEFAULT FALSE AFTER vence_en;
