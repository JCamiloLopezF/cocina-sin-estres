import { useState } from 'react'
import type { VideoReceta as Video } from '../recetario/tipos'
import { relojDe } from './guion'
import './cocinar.css'

/**
 * Fragmento de YouTube que escogió el curador (máximo la mitad del tiempo de la
 * receta). Primero se ve solo la portada: YouTube no carga nada ni pone cookies
 * hasta que la persona toca reproducir, y se usa el dominio sin cookies.
 */
export function VideoReceta({ video, receta }: { video: Video; receta: string }) {
  const [reproducir, setReproducir] = useState(false)
  const duracion = relojDe(video.finSeg - video.inicioSeg)
  const fuente =
    `https://www.youtube-nocookie.com/embed/${encodeURIComponent(video.youtubeId)}` +
    `?start=${video.inicioSeg}&end=${video.finSeg}&autoplay=1&rel=0&modestbranding=1&playsinline=1`

  return (
    <section className="video-receta" aria-label={`Video de ${receta}`}>
      <div className="video-receta__marco">
        {reproducir ? (
          <iframe
            src={fuente}
            title={`Video de ${receta}`}
            allow="autoplay; encrypted-media; picture-in-picture"
            allowFullScreen
            referrerPolicy="strict-origin-when-cross-origin"
          />
        ) : (
          <button
            type="button"
            className="video-receta__portada"
            aria-label={`Ver el video de ${receta} (${duracion})`}
            onClick={() => setReproducir(true)}
          >
            <img
              src={`https://i.ytimg.com/vi/${encodeURIComponent(video.youtubeId)}/hqdefault.jpg`}
              alt=""
              loading="lazy"
            />
            <span className="video-receta__play" aria-hidden="true">
              <span>▶</span>
            </span>
          </button>
        )}
      </div>
      <p className="video-receta__nota">Video de {duracion} en YouTube con lo más importante de la preparación.</p>
    </section>
  )
}
