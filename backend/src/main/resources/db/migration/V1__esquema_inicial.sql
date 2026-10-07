-- =====================================================================
-- V1__esquema_inicial.sql
-- Cocina sin estrés · Esquema inicial de la base de datos (KAN-12)
--
-- Decisiones de diseño: ADR-13 a ADR-23. Diccionario y diagrama ER en
-- docs/modelo-datos.md. Convenciones de nombres en CONTRIBUTING.md.
--
-- Reglas generales:
--   * Dinero en DECIMAL(12,2) (pesos colombianos); cantidades en DECIMAL(12,3)
--     sobre la unidad base del producto (g, ml o unidad).
--   * Fechas y horas en UTC; la aplicación convierte a America/Bogota.
--   * Los valores cerrados (rol, tipos, estados) son VARCHAR + CHECK, no ENUM,
--     para poder agregar valores con una migración simple.
--   * Los datos de un hogar se borran en cascada con el hogar. El catálogo
--     (productos globales, recetas, restricciones) nunca se borra: se desactiva.
--   * Esta migración no incluye datos. El catálogo se carga en V2.
--   * En MySQL un CHECK que evalúa NULL se acepta: por eso las reglas sobre
--     columnas opcionales piden IS NOT NULL de forma explícita.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios e identidad (ADR-16, ADR-19)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    firebase_uid     VARCHAR(128) NOT NULL,
    correo           VARCHAR(254) NOT NULL,
    rol              VARCHAR(10)  NOT NULL DEFAULT 'USUARIA',
    perfil_completo  BOOLEAN      NOT NULL DEFAULT FALSE,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actualizado_en   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_firebase_uid UNIQUE (firebase_uid),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('USUARIA', 'CURADOR', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_usuario_correo ON usuario (correo);

-- ---------------------------------------------------------------------
-- Perfil del hogar (ADR-13, ADR-14, ADR-20)
-- ---------------------------------------------------------------------
CREATE TABLE hogar (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id               BIGINT        NOT NULL,
    presupuesto_monto        DECIMAL(12,2) NOT NULL,
    periodicidad_ingreso     VARCHAR(10)   NOT NULL,
    dia_pago_1               TINYINT       NOT NULL,
    dia_pago_2               TINYINT       NULL,
    -- Días de la semana sin tiempo para cocinar: arreglo JSON de 1 (lunes) a 7 (domingo)
    dias_sin_tiempo          JSON          NULL,
    -- Datos de salud: texto cifrado con AES-GCM por la aplicación (ADR-20).
    -- restricciones = lista JSON de códigos de la tabla restriccion, cifrada.
    restricciones            TEXT          NULL,
    otras_condiciones        TEXT          NULL,
    -- TRUE cuando la usuaria declara explícitamente que no hay restricciones (ADR-13)
    sin_restricciones        BOOLEAN       NOT NULL DEFAULT FALSE,
    consentimiento_datos_en  DATETIME(3)   NULL,
    creado_en                DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actualizado_en           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_hogar PRIMARY KEY (id),
    CONSTRAINT uk_hogar_usuario_id UNIQUE (usuario_id),
    CONSTRAINT fk_hogar_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_hogar_presupuesto CHECK (presupuesto_monto > 0),
    CONSTRAINT ck_hogar_periodicidad CHECK (periodicidad_ingreso IN ('SEMANAL', 'QUINCENAL', 'MENSUAL')),
    -- SEMANAL: día de la semana (1-7). MENSUAL: día del mes (1-31). QUINCENAL: dos días del mes en orden.
    CONSTRAINT ck_hogar_dias_pago CHECK (
        (periodicidad_ingreso = 'SEMANAL'   AND dia_pago_1 BETWEEN 1 AND 7  AND dia_pago_2 IS NULL)
     OR (periodicidad_ingreso = 'MENSUAL'   AND dia_pago_1 BETWEEN 1 AND 31 AND dia_pago_2 IS NULL)
     OR (periodicidad_ingreso = 'QUINCENAL' AND dia_pago_1 BETWEEN 1 AND 31 AND dia_pago_2 IS NOT NULL
                                            AND dia_pago_2 BETWEEN 1 AND 31 AND dia_pago_1 < dia_pago_2)
    ),
    -- Las restricciones se declaran o se marca "sin restricciones", nunca ambas
    CONSTRAINT ck_hogar_restricciones CHECK (NOT (sin_restricciones AND restricciones IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE persona (
    id        BIGINT           NOT NULL AUTO_INCREMENT,
    hogar_id  BIGINT           NOT NULL,
    nombre    VARCHAR(60)      NULL,
    edad      TINYINT UNSIGNED NOT NULL,
    CONSTRAINT pk_persona PRIMARY KEY (id),
    CONSTRAINT fk_persona_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT ck_persona_edad CHECK (edad <= 120)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_persona_hogar_id ON persona (hogar_id);

-- ---------------------------------------------------------------------
-- Catálogo: restricciones y productos (ADR-13, ADR-15, ADR-18, ADR-23)
-- ---------------------------------------------------------------------
CREATE TABLE restriccion (
    id      BIGINT      NOT NULL AUTO_INCREMENT,
    -- Código estable que se guarda (cifrado) en hogar.restricciones. Ej.: lactosa, gluten, mani
    codigo  VARCHAR(40) NOT NULL,
    nombre  VARCHAR(80) NOT NULL,
    tipo    VARCHAR(10) NOT NULL,
    CONSTRAINT pk_restriccion PRIMARY KEY (id),
    CONSTRAINT uk_restriccion_codigo UNIQUE (codigo),
    CONSTRAINT ck_restriccion_tipo CHECK (tipo IN ('ALERGIA', 'CONDICION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE producto (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    -- NULL = producto del catálogo global. Con valor = producto adicional registrado por un hogar (ADR-18)
    hogar_id                BIGINT        NULL,
    nombre                  VARCHAR(120)  NOT NULL,
    categoria               VARCHAR(60)   NOT NULL,
    tipo_cantidad           VARCHAR(10)   NOT NULL,
    unidad_base             VARCHAR(10)   NOT NULL,
    -- Precio de referencia: cuánto cuesta una unidad de compra y cuánto trae, en unidad base
    unidad_compra           VARCHAR(30)   NULL,
    cantidad_unidad_compra  DECIMAL(12,3) NULL,
    precio_unidad_compra    DECIMAL(12,2) NULL,
    -- Solo granel: cantidad que equivale a "Lleno" (ADR-15)
    capacidad_referencia    DECIMAL(12,3) NULL,
    es_basico               BOOLEAN       NOT NULL DEFAULT FALSE,
    activo                  BOOLEAN       NOT NULL DEFAULT TRUE,
    modificado_por          BIGINT        NULL,
    modificado_en           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_producto PRIMARY KEY (id),
    CONSTRAINT fk_producto_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT fk_producto_usuario FOREIGN KEY (modificado_por) REFERENCES usuario (id),
    CONSTRAINT ck_producto_tipo_cantidad CHECK (tipo_cantidad IN ('CONTABLE', 'GRANEL')),
    CONSTRAINT ck_producto_unidad_base CHECK (unidad_base IN ('g', 'ml', 'unidad')),
    CONSTRAINT ck_producto_capacidad CHECK (
        (tipo_cantidad = 'CONTABLE' AND capacidad_referencia IS NULL)
     OR (tipo_cantidad = 'GRANEL'   AND capacidad_referencia IS NOT NULL AND capacidad_referencia > 0)
    ),
    CONSTRAINT ck_producto_precio CHECK (
        (cantidad_unidad_compra IS NULL OR cantidad_unidad_compra > 0)
        AND (precio_unidad_compra IS NULL OR precio_unidad_compra >= 0)
    ),
    -- Todo producto del catálogo tiene precio de referencia completo (RNF-02) y un curador responsable (RNF-24)
    CONSTRAINT ck_producto_catalogo CHECK (
        hogar_id IS NOT NULL
        OR (unidad_compra IS NOT NULL AND cantidad_unidad_compra IS NOT NULL
            AND precio_unidad_compra IS NOT NULL AND modificado_por IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_producto_hogar_id ON producto (hogar_id);
CREATE INDEX idx_producto_nombre ON producto (nombre);
CREATE INDEX idx_producto_categoria ON producto (categoria);

CREATE TABLE producto_restriccion (
    producto_id     BIGINT NOT NULL,
    restriccion_id  BIGINT NOT NULL,
    CONSTRAINT pk_producto_restriccion PRIMARY KEY (producto_id, restriccion_id),
    CONSTRAINT fk_producto_restriccion_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT fk_producto_restriccion_restriccion FOREIGN KEY (restriccion_id) REFERENCES restriccion (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_producto_restriccion_restriccion_id ON producto_restriccion (restriccion_id);

-- ---------------------------------------------------------------------
-- Despensa virtual (ADR-15)
-- ---------------------------------------------------------------------
CREATE TABLE item_despensa (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    hogar_id         BIGINT        NOT NULL,
    producto_id      BIGINT        NOT NULL,
    cantidad         DECIMAL(12,3) NOT NULL DEFAULT 0,
    -- TRUE cuando la cantidad viene de un nivel (Lleno, Medio, Poco) y no de un conteo
    es_estimada      BOOLEAN       NOT NULL DEFAULT FALSE,
    cantidad_minima  DECIMAL(12,3) NULL,
    vence_en         DATE          NULL,
    ubicacion        VARCHAR(40)   NULL,
    creado_en        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actualizado_en   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_item_despensa PRIMARY KEY (id),
    CONSTRAINT uk_item_despensa_hogar_id_producto_id UNIQUE (hogar_id, producto_id),
    CONSTRAINT fk_item_despensa_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_despensa_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT ck_item_despensa_cantidades CHECK (cantidad >= 0 AND (cantidad_minima IS NULL OR cantidad_minima >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_item_despensa_producto_id ON item_despensa (producto_id);
CREATE INDEX idx_item_despensa_hogar_id_vence_en ON item_despensa (hogar_id, vence_en);

-- ---------------------------------------------------------------------
-- Recetario (ADR-08, ADR-17, ADR-18, ADR-23)
-- ---------------------------------------------------------------------
CREATE TABLE receta (
    id                  BIGINT           NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(120)     NOT NULL,
    descripcion         TEXT             NULL,
    tiempo_minutos      SMALLINT UNSIGNED NOT NULL,
    porciones           TINYINT UNSIGNED NOT NULL,
    pasos               TEXT             NOT NULL,
    -- Indicación para el aviso PREPARACION_PREVIA (ej.: "Remojar los fríjoles la noche anterior")
    preparacion_previa  TEXT             NULL,
    activa              BOOLEAN          NOT NULL DEFAULT TRUE,
    modificado_por      BIGINT           NOT NULL,
    modificado_en       DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_receta PRIMARY KEY (id),
    CONSTRAINT fk_receta_usuario FOREIGN KEY (modificado_por) REFERENCES usuario (id),
    CONSTRAINT ck_receta_valores CHECK (tiempo_minutos > 0 AND porciones > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_receta_nombre ON receta (nombre);

CREATE TABLE ingrediente_receta (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    receta_id    BIGINT        NOT NULL,
    -- Siempre un producto del catálogo global (hogar_id NULL); lo valida la aplicación
    producto_id  BIGINT        NOT NULL,
    cantidad     DECIMAL(12,3) NOT NULL,
    opcional     BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_ingrediente_receta PRIMARY KEY (id),
    CONSTRAINT uk_ingrediente_receta_receta_id_producto_id UNIQUE (receta_id, producto_id),
    CONSTRAINT fk_ingrediente_receta_receta FOREIGN KEY (receta_id) REFERENCES receta (id) ON DELETE CASCADE,
    CONSTRAINT fk_ingrediente_receta_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT ck_ingrediente_receta_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ingrediente_receta_producto_id ON ingrediente_receta (producto_id);

-- Equivalencias que una usuaria marca en la pantalla 25 (ADR-18)
CREATE TABLE equivalencia_hogar (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    hogar_id            BIGINT NOT NULL,
    producto_receta_id  BIGINT NOT NULL,
    producto_hogar_id   BIGINT NOT NULL,
    CONSTRAINT pk_equivalencia_hogar PRIMARY KEY (id),
    CONSTRAINT uk_equivalencia_hogar_hogar_id_producto_receta_id UNIQUE (hogar_id, producto_receta_id),
    CONSTRAINT fk_equivalencia_hogar_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT fk_equivalencia_hogar_producto_receta FOREIGN KEY (producto_receta_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT fk_equivalencia_hogar_producto_hogar FOREIGN KEY (producto_hogar_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT ck_equivalencia_hogar_distintos CHECK (producto_receta_id <> producto_hogar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_equivalencia_hogar_producto_receta_id ON equivalencia_hogar (producto_receta_id);
CREATE INDEX idx_equivalencia_hogar_producto_hogar_id ON equivalencia_hogar (producto_hogar_id);

-- ---------------------------------------------------------------------
-- Planificador y lista de compras (ADR-21)
-- ---------------------------------------------------------------------
CREATE TABLE plan_semanal (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    hogar_id       BIGINT      NOT NULL,
    -- Lunes de la semana planificada
    semana_inicio  DATE        NOT NULL,
    creado_en      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_plan_semanal PRIMARY KEY (id),
    CONSTRAINT uk_plan_semanal_hogar_id_semana_inicio UNIQUE (hogar_id, semana_inicio),
    CONSTRAINT fk_plan_semanal_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comida_plan (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    plan_semanal_id    BIGINT      NOT NULL,
    -- 1 = lunes … 7 = domingo
    dia                TINYINT     NOT NULL,
    tipo_comida        VARCHAR(10) NOT NULL,
    receta_id          BIGINT      NOT NULL,
    preparada          BOOLEAN     NOT NULL DEFAULT FALSE,
    preparada_en       DATETIME(3) NULL,
    -- Identificador del último cambio enviado por el navegador; evita duplicados al reenviar (ADR-21)
    id_cambio_cliente  VARCHAR(64) NULL,
    CONSTRAINT pk_comida_plan PRIMARY KEY (id),
    CONSTRAINT uk_comida_plan_plan_semanal_id_dia_tipo_comida UNIQUE (plan_semanal_id, dia, tipo_comida),
    CONSTRAINT uk_comida_plan_id_cambio_cliente UNIQUE (id_cambio_cliente),
    CONSTRAINT fk_comida_plan_plan_semanal FOREIGN KEY (plan_semanal_id) REFERENCES plan_semanal (id) ON DELETE CASCADE,
    CONSTRAINT fk_comida_plan_receta FOREIGN KEY (receta_id) REFERENCES receta (id),
    CONSTRAINT ck_comida_plan_dia CHECK (dia BETWEEN 1 AND 7),
    CONSTRAINT ck_comida_plan_tipo_comida CHECK (tipo_comida IN ('DESAYUNO', 'ALMUERZO', 'CENA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comida_plan_receta_id ON comida_plan (receta_id);

CREATE TABLE lista_compras (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    plan_semanal_id  BIGINT      NOT NULL,
    generada_en      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_lista_compras PRIMARY KEY (id),
    CONSTRAINT uk_lista_compras_plan_semanal_id UNIQUE (plan_semanal_id),
    CONSTRAINT fk_lista_compras_plan_semanal FOREIGN KEY (plan_semanal_id) REFERENCES plan_semanal (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE item_lista_compras (
    id                BIGINT            NOT NULL AUTO_INCREMENT,
    lista_compras_id  BIGINT            NOT NULL,
    producto_id       BIGINT            NOT NULL,
    cantidad          DECIMAL(12,3)     NOT NULL,
    costo_estimado    DECIMAL(12,2)     NOT NULL,
    -- 1 = más prioritario
    prioridad         SMALLINT UNSIGNED NOT NULL,
    comprado          BOOLEAN           NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_item_lista_compras PRIMARY KEY (id),
    CONSTRAINT uk_item_lista_compras_lista_compras_id_producto_id UNIQUE (lista_compras_id, producto_id),
    CONSTRAINT fk_item_lista_compras_lista_compras FOREIGN KEY (lista_compras_id) REFERENCES lista_compras (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_lista_compras_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT ck_item_lista_compras_valores CHECK (cantidad > 0 AND costo_estimado >= 0 AND prioridad >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_item_lista_compras_producto_id ON item_lista_compras (producto_id);

-- ---------------------------------------------------------------------
-- Avisos (ADR-17)
-- ---------------------------------------------------------------------
CREATE TABLE aviso (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    hogar_id            BIGINT       NOT NULL,
    tipo                VARCHAR(25)  NOT NULL,
    mensaje             VARCHAR(255) NOT NULL,
    -- Ej.: POR_VENCER:12:2026-10-20. Un aviso repetido no se inserta (ADR-17)
    clave_idempotencia  VARCHAR(120) NOT NULL,
    leido               BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_aviso PRIMARY KEY (id),
    CONSTRAINT uk_aviso_clave_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT fk_aviso_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT ck_aviso_tipo CHECK (tipo IN ('BAJO_MINIMO', 'POR_VENCER', 'PREPARACION_PREVIA',
                                             'SEMANA_SIN_PLANIFICAR', 'APROVECHAR_PRODUCTO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_aviso_hogar_id_leido ON aviso (hogar_id, leido);

CREATE TABLE hogar_preferencia_aviso (
    hogar_id  BIGINT      NOT NULL,
    tipo      VARCHAR(25) NOT NULL,
    activo    BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_hogar_preferencia_aviso PRIMARY KEY (hogar_id, tipo),
    CONSTRAINT fk_hogar_preferencia_aviso_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT ck_hogar_preferencia_aviso_tipo CHECK (tipo IN ('BAJO_MINIMO', 'POR_VENCER', 'PREPARACION_PREVIA',
                                                               'SEMANA_SIN_PLANIFICAR', 'APROVECHAR_PRODUCTO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Trazabilidad del catálogo (ADR-23, RNF-24)
-- ---------------------------------------------------------------------
CREATE TABLE auditoria_catalogo (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    entidad     VARCHAR(20) NOT NULL,
    entidad_id  BIGINT      NOT NULL,
    accion      VARCHAR(12) NOT NULL,
    usuario_id  BIGINT      NOT NULL,
    fecha       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    -- Campos cambiados: {"campo": {"antes": ..., "despues": ...}}
    cambios     JSON        NULL,
    CONSTRAINT pk_auditoria_catalogo PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_catalogo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_auditoria_catalogo_entidad CHECK (entidad IN ('PRODUCTO', 'RECETA', 'INGREDIENTE_RECETA', 'RESTRICCION')),
    CONSTRAINT ck_auditoria_catalogo_accion CHECK (accion IN ('CREAR', 'EDITAR', 'DESACTIVAR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_auditoria_catalogo_entidad_entidad_id ON auditoria_catalogo (entidad, entidad_id);
CREATE INDEX idx_auditoria_catalogo_usuario_id ON auditoria_catalogo (usuario_id);
