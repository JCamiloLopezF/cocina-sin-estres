import { useEffect, useState } from 'react'
import { Boton } from '../../shared/componentes'
import type { EstadoTemporizador } from './cuentaRegresiva'
import { minutosEnPalabras, relojDe } from './guion'

interface Props {
  estado: EstadoTemporizador
  alCambiar: (estado: EstadoTemporizador | undefined) => void
  alTerminar: (estado: EstadoTemporizador) => void
}

/**
 * Temporizador de un paso: cuenta con la hora real, así que no se atrasa si la pantalla
 * se duerme. Cuando termina, el aviso se quita solo al tocar «Listo» o «Atrás» (o al
 * decirlo con Manos libres); el botón «Quitar» es por si se quiere quitar sin avanzar.
 */
export function Temporizador({ estado, alCambiar, alTerminar }: Props) {
  const [ahora, setAhora] = useState(() => Date.now())
  const restante = estado.pausado ? estado.restantePausado : estado.terminaEn - ahora
  const paso = `Paso ${estado.paso + 1}`

  useEffect(() => {
    if (estado.pausado || estado.terminado) return
    const intervalo = window.setInterval(() => setAhora(Date.now()), 500)
    return () => window.clearInterval(intervalo)
  }, [estado.pausado, estado.terminado])

  useEffect(() => {
    if (!estado.pausado && !estado.terminado && restante <= 0) {
      const terminado = { ...estado, terminado: true }
      alCambiar(terminado)
      alTerminar(terminado)
    }
  }, [restante, estado, alCambiar, alTerminar])

  return (
    <section
      className={estado.terminado ? 'temporizador temporizador--fin' : 'temporizador'}
      aria-label={`Temporizador del paso ${estado.paso + 1}`}
      role={estado.terminado ? 'alert' : undefined}
    >
      <div>
        <p className="temporizador__reloj" aria-live="off">
          {estado.terminado ? '¡Tiempo!' : relojDe(restante / 1000)}
        </p>
        <p className="temporizador__paso">
          {estado.terminado ? `${paso} · pasaron ${minutosEnPalabras(estado.minutos)}` : paso}
        </p>
      </div>
      <div className="temporizador__acciones">
        {estado.terminado ? (
          <Boton variante="texto" onClick={() => alCambiar(undefined)}>
            Quitar
          </Boton>
        ) : (
          <>
            <Boton
              variante="secundario"
              onClick={() =>
                alCambiar(
                  estado.pausado
                    ? { ...estado, pausado: false, terminaEn: Date.now() + estado.restantePausado }
                    : { ...estado, pausado: true, restantePausado: estado.terminaEn - Date.now() },
                )
              }
            >
              {estado.pausado ? 'Seguir' : 'Pausar'}
            </Boton>
            <Boton variante="texto" onClick={() => alCambiar(undefined)}>
              Cancelar
            </Boton>
          </>
        )}
      </div>
    </section>
  )
}
