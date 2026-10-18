-- =====================================================================
-- V5__agregar_aviso_presupuesto.sql
-- Agrega el tipo de aviso PRESUPUESTO_EXCEDIDO a los CHECK de
-- aviso y hogar_preferencia_aviso.
--
-- Se modifica el CHECK en el sitio (sin renombrar ni copiar tablas):
--   * Los nombres de CHECK son únicos por esquema en MySQL, así que
--     recrear la tabla con el mismo nombre de constraint mientras existe
--     el backup falla con "Duplicate check constraint name".
--   * No se tocan datos, FKs, índices ni claves únicas.
--
-- Requiere MySQL 8.0.16+ (CHECK aplicados). Si usas MariaDB, cambia
-- "DROP CHECK" por "DROP CONSTRAINT".
-- =====================================================================

-- aviso
ALTER TABLE aviso DROP CHECK ck_aviso_tipo;

ALTER TABLE aviso
    ADD CONSTRAINT ck_aviso_tipo CHECK (
        tipo IN ('BAJO_MINIMO', 'POR_VENCER', 'PREPARACION_PREVIA',
                 'SEMANA_SIN_PLANIFICAR', 'APROVECHAR_PRODUCTO',
                 'PRESUPUESTO_EXCEDIDO')
        );

-- hogar_preferencia_aviso
ALTER TABLE hogar_preferencia_aviso DROP CHECK ck_hogar_preferencia_aviso_tipo;

ALTER TABLE hogar_preferencia_aviso
    ADD CONSTRAINT ck_hogar_preferencia_aviso_tipo CHECK (
        tipo IN ('BAJO_MINIMO', 'POR_VENCER', 'PREPARACION_PREVIA',
                 'SEMANA_SIN_PLANIFICAR', 'APROVECHAR_PRODUCTO',
                 'PRESUPUESTO_EXCEDIDO')
        );
