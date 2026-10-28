-- =====================================================================
-- V16: dos métricas del Plan de Gestión de la Calidad que mide la web
-- (KAN-54). Solo cifras: ni correos ni contraseñas.
--
--   inicio_sesion                    cada inicio de sesión que termina bien:
--                                    cuánto tardó (desde «Entrar» hasta que
--                                    la API reconoce la cuenta) y cuántos
--                                    intentos fallidos hubo antes en ese
--                                    navegador (éxito de autenticación y
--                                    tiempo de respuesta de login)
--   plan_semanal.segundos_planificacion
--                                    segundos desde que la usuaria abre la
--                                    semana sin completar hasta que queda
--                                    con almuerzo y cena los 7 días (tiempo
--                                    de planificación semanal); solo la
--                                    primera vez
-- =====================================================================

CREATE TABLE inicio_sesion (
    id                BIGINT            NOT NULL AUTO_INCREMENT,
    usuario_id        BIGINT            NOT NULL,
    duracion_ms       INT UNSIGNED      NOT NULL,
    fallidos_previos  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    creado_en         DATETIME(3)       NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_inicio_sesion PRIMARY KEY (id),
    CONSTRAINT fk_inicio_sesion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_inicio_sesion_creado_en ON inicio_sesion (creado_en);

ALTER TABLE plan_semanal ADD COLUMN segundos_planificacion INT UNSIGNED NULL;
