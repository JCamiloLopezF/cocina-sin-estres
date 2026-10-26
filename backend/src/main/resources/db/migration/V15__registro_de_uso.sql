-- =====================================================================
-- V15: registro de uso para el análisis del administrador (KAN-54)
--
-- Las métricas del Plan de Gestión de la Calidad (secciones 3.3 y 3.4) que
-- no salen de las tablas de negocio se guardan aquí. Solo cifras: ningún
-- dato de salud ni texto escrito por la usuaria.
--
--   plan_semanal.completado_en  primera vez que el plan tuvo almuerzo y
--                               cena los 7 días (tiempo para armar el plan)
--   apertura_app                cada vez que se abre la app y corre el
--                               motor de avisos: cuántos avisos, cuántas
--                               reglas y cuánto tardó (RNF-04, RNF-07)
--   preparacion                 cada receta cocinada, en el plan o fuera
--                               de él (recetas más cocinadas)
--   uso_rescate                 cada vez que se abre el modo rescate
-- =====================================================================

ALTER TABLE plan_semanal ADD COLUMN completado_en DATETIME(3) NULL;

CREATE TABLE apertura_app (
    id                BIGINT            NOT NULL AUTO_INCREMENT,
    hogar_id          BIGINT            NOT NULL,
    avisos_vigentes   SMALLINT UNSIGNED NOT NULL,
    avisos_mostrados  SMALLINT UNSIGNED NOT NULL,
    reglas_evaluadas  TINYINT UNSIGNED  NOT NULL,
    duracion_ms       INT UNSIGNED      NOT NULL,
    creada_en         DATETIME(3)       NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_apertura_app PRIMARY KEY (id),
    CONSTRAINT fk_apertura_app_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_apertura_app_creada_en ON apertura_app (creada_en);

CREATE TABLE preparacion (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    hogar_id        BIGINT       NOT NULL,
    receta_id       BIGINT       NOT NULL,
    -- NULL cuando se cocinó desde el recetario, fuera del plan
    comida_plan_id  BIGINT       NULL,
    creada_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_preparacion PRIMARY KEY (id),
    CONSTRAINT fk_preparacion_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT fk_preparacion_receta FOREIGN KEY (receta_id) REFERENCES receta (id),
    CONSTRAINT fk_preparacion_comida_plan FOREIGN KEY (comida_plan_id) REFERENCES comida_plan (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_preparacion_creada_en ON preparacion (creada_en);

CREATE TABLE uso_rescate (
    id         BIGINT            NOT NULL AUTO_INCREMENT,
    hogar_id   BIGINT            NOT NULL,
    opciones   SMALLINT UNSIGNED NOT NULL,
    creado_en  DATETIME(3)       NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_uso_rescate PRIMARY KEY (id),
    CONSTRAINT fk_uso_rescate_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_uso_rescate_hogar_id_creado_en ON uso_rescate (hogar_id, creado_en);

-- Lo que ya se marcó como preparado en el plan antes de V15 también cuenta.
INSERT INTO preparacion (hogar_id, receta_id, comida_plan_id, creada_en)
SELECT p.hogar_id, c.receta_id, c.id, COALESCE(c.preparada_en, p.creado_en)
FROM comida_plan c
JOIN plan_semanal p ON p.id = c.plan_semanal_id
WHERE c.preparada = TRUE;
