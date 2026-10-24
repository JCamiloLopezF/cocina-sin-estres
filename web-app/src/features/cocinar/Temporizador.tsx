import { useEffect, useState } from 'react'
import { Boton } from '../../shared/componentes'
import type { EstadoTemporizador } from './cuentaRegresiva'
import { relojDe } from './guion'

interface Props {
  estado: EstadoTemporizador
  alCambiar: (estado: EstadoTemporizador | undefined) => void
  alTerminar: (minutos: number) => void
}

/** Temporizador del paso: cuenta con la hora real, así que no se atrasa si la pantalla se duerme. */
export function Temporizador({ estado, alCambiar, alTerminar }: Props) {
  const [ahora, setAhora] = useState(() => Date.now())
  const restante = estado.pausado ? estado.restantePausado : estado.terminaEn - ahora

  useEffect(() => {
    if (estado.pausado || estado.terminado) return
    const intervalo = window.setInterval(() => setAhora(Date.now()), 500)
    return () => window.clearInterval(intervalo)
  }, [estado.pausado, estado.terminado])

  useEffect(() => {
    if (!estado.pausado && !estado.terminado && restante <= 0) {
      alCambiar({ ...estado, terminado: true })
      alTerminar(estado.minutos)
    }
  }, [restante, estado, alCambiar, alTerminar])

  return (
    <section
      className={estado.terminado ? 'temporizador temporizador--fin' : 'temporizador'}
      aria-label="Temporizador"
      role={estado.terminado ? 'alert' : undefined}
    >
      <p className="temporizador__reloj" aria-live="off">
        {estado.terminado ? '¡Tiempo!' : relojDe(restante / 1000)}
      </p>
      <div className="temporizador__acciones">
        {estado.terminado ? (
          <Boton onClick={() => alCambiar(undefined)}>Listo, ya lo vi</Boton>
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
