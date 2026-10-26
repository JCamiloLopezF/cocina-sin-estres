-- =====================================================================
-- V14: tiendas de la usuaria para pedir por WhatsApp (KAN-53)
--
-- Muchas usuarias le piden el mercado a la tienda del barrio por WhatsApp.
-- La app arma el mensaje con lo que falta de la lista de compras y lo abre
-- en WhatsApp con el número de la tienda; la usuaria lo revisa y lo envía.
--
--   tienda.whatsapp  número con indicativo de Colombia, solo dígitos
--                    (573001234567), listo para https://wa.me/<número>
--
-- Un hogar guarda hasta 5 tiendas (lo controla el servicio) y no repite
-- número.
-- =====================================================================

CREATE TABLE tienda (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    hogar_id    BIGINT       NOT NULL,
    nombre      VARCHAR(60)  NOT NULL,
    whatsapp    VARCHAR(15)  NOT NULL,
    creada_en   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_tienda PRIMARY KEY (id),
    CONSTRAINT uk_tienda_hogar_id_whatsapp UNIQUE (hogar_id, whatsapp),
    CONSTRAINT fk_tienda_hogar FOREIGN KEY (hogar_id) REFERENCES hogar (id) ON DELETE CASCADE,
    CONSTRAINT ck_tienda_valores CHECK (CHAR_LENGTH(TRIM(nombre)) > 0 AND CHAR_LENGTH(whatsapp) BETWEEN 11 AND 15)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
