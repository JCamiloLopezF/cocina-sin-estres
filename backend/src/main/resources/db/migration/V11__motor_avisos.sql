-- =====================================================================
-- V11__motor_avisos.sql
-- Cocina sin estrés · Motor de avisos (ADR-17, KAN-27) y borrador de la
-- semana siguiente (ADR-24, pieza 3)
--
--   aviso.titulo          frase corta que se lee primero ("La leche vence mañana")
--   aviso.enlace          a dónde lleva el botón del aviso (ruta de la app)
--   aviso.accion          texto del botón ("Ver en la despensa")
--   aviso.actualizado_en  un aviso del día sin leer se actualiza si cambia
--                         la situación (un producto más por vencer)
--   plan_semanal.borrador la semana la armó la app sola; la usuaria la
--                         revisa y la confirma
-- =====================================================================

ALTER TABLE aviso
    ADD COLUMN titulo         VARCHAR(120) NULL,
    ADD COLUMN enlace         VARCHAR(160) NULL,
    ADD COLUMN accion         VARCHAR(40)  NULL,
    ADD COLUMN actualizado_en DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- Los avisos del centro de avisos se listan por hogar y fecha.
CREATE INDEX idx_aviso_hogar_id_creado_en ON aviso (hogar_id, creado_en);

ALTER TABLE plan_semanal ADD COLUMN borrador BOOLEAN NOT NULL DEFAULT FALSE;
