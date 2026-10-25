-- =====================================================================
-- V10__video_receta.sql
-- Cocina sin estrés · Video de la receta (ajuste de la sustentación)
--
-- El profesor pidió un video que muestre cómo se hace la receta y que dure
-- como máximo la mitad del tiempo de la receta. Se guarda el id del video de
-- YouTube y el fragmento que se muestra (inicio y fin en segundos): así sirve
-- también un video más largo si se muestra solo la parte útil.
-- Los videos los carga el curador desde su pantalla.
-- =====================================================================

ALTER TABLE receta
    ADD COLUMN video_youtube_id VARCHAR(11) NULL,
    ADD COLUMN video_inicio_seg INT NULL,
    ADD COLUMN video_fin_seg    INT NULL;

-- O no hay video, o hay video con su fragmento completo y dura como máximo la
-- mitad del tiempo de la receta: (fin - inicio) * 2 <= tiempo_minutos * 60.
ALTER TABLE receta ADD CONSTRAINT ck_receta_video CHECK (
    (video_youtube_id IS NULL AND video_inicio_seg IS NULL AND video_fin_seg IS NULL)
    OR (video_youtube_id IS NOT NULL
        AND video_inicio_seg IS NOT NULL AND video_inicio_seg >= 0
        AND video_fin_seg IS NOT NULL AND video_fin_seg > video_inicio_seg
        AND (video_fin_seg - video_inicio_seg) * 2 <= tiempo_minutos * 60)
);
