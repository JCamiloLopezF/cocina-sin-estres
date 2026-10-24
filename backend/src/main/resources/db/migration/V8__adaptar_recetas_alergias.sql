-- =====================================================================
-- V8__adaptar_recetas_alergias.sql
-- Cocina sin estrés · Una alergia no siempre descarta la receta (ADR-24)
--
-- Retroalimentación del profesor: si alguien es alérgico, la mamá cocina para
-- todos y a esa persona le cambia el ingrediente o le hace otro plato. Solo
-- una alergia grave ("no puede ni tocarlo") descarta la receta para la casa.
--
--   persona.alergias_graves      alergias graves de la persona (cifradas, ADR-20)
--   ingrediente_receta.es_principal  el ingrediente define el plato: no se
--                                reemplaza; si alguien es alérgico, le toca
--                                plato aparte
--   reemplazo_producto           por qué producto se puede cambiar otro. Las
--                                filas SISTEMA las calcula la aplicación (misma
--                                categoría y sin el alérgeno); el curador puede
--                                agregar, quitar o volver a usar (CURADOR), y el
--                                recálculo automático respeta lo que él decidió
-- =====================================================================

ALTER TABLE persona ADD COLUMN alergias_graves TEXT NULL;

ALTER TABLE ingrediente_receta ADD COLUMN es_principal BOOLEAN NOT NULL DEFAULT FALSE;

-- Regla automática para el catálogo inicial: es principal el ingrediente que da
-- nombre al plato (la primera palabra de la receta contiene la del producto):
-- "Huevos pericos" -> huevos; "Arroz con huevo y tomate" -> arroz (el huevo se
-- puede cambiar). Las palabras de menos de 4 letras no cuentan ("Sal").
UPDATE ingrediente_receta ir
JOIN receta r   ON r.id = ir.receta_id
JOIN producto p ON p.id = ir.producto_id
SET ir.es_principal = TRUE
WHERE CHAR_LENGTH(SUBSTRING_INDEX(p.nombre, ' ', 1)) >= 4
  AND SUBSTRING_INDEX(r.nombre, ' ', 1) LIKE CONCAT(
        '%',
        LEFT(SUBSTRING_INDEX(p.nombre, ' ', 1), CHAR_LENGTH(SUBSTRING_INDEX(p.nombre, ' ', 1)) - 1),
        '%');

-- Platos cuyo nombre no lo dice: una tortilla de papa sin huevo o sin papa, o una
-- crema de papa sin papa, ya no son el mismo plato.
UPDATE ingrediente_receta ir
JOIN receta r   ON r.id = ir.receta_id
JOIN producto p ON p.id = ir.producto_id
SET ir.es_principal = TRUE
WHERE (r.nombre = 'Tortilla de papa y cebolla' AND p.nombre IN ('Huevos', 'Papa'))
   OR (r.nombre = 'Crema de papa con queso' AND p.nombre = 'Papa');

CREATE TABLE reemplazo_producto (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    producto_id     BIGINT       NOT NULL,
    reemplazo_id    BIGINT       NOT NULL,
    -- SISTEMA: lo calculó la aplicación. CURADOR: lo agregó, quitó o confirmó un curador.
    origen          VARCHAR(10)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    modificado_por  BIGINT       NULL,
    modificado_en   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_reemplazo_producto PRIMARY KEY (id),
    CONSTRAINT uk_reemplazo_producto_producto_id_reemplazo_id UNIQUE (producto_id, reemplazo_id),
    CONSTRAINT fk_reemplazo_producto_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT fk_reemplazo_producto_reemplazo FOREIGN KEY (reemplazo_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT fk_reemplazo_producto_usuario FOREIGN KEY (modificado_por) REFERENCES usuario (id),
    CONSTRAINT ck_reemplazo_producto_distintos CHECK (producto_id <> reemplazo_id),
    CONSTRAINT ck_reemplazo_producto_origen CHECK (origen IN ('SISTEMA', 'CURADOR')),
    -- Lo que decide un curador queda a su nombre
    CONSTRAINT ck_reemplazo_producto_curador CHECK (origen = 'SISTEMA' OR modificado_por IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reemplazo_producto_reemplazo_id ON reemplazo_producto (reemplazo_id);
