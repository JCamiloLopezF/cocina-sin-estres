import { useCallback, useEffect, useState } from 'react'
import { pedir } from './cliente'

export type EstadoConsulta<T> =
  | { tipo: 'cargando' }
  | { tipo: 'listo'; datos: T }
  | { tipo: 'error'; error: unknown }

/**
 * Hace un GET al montar la pantalla y cancela la petición si la pantalla se cierra
 * antes de que llegue la respuesta. `recargar` repite la consulta.
 */
export function useConsulta<T>(ruta: string): { estado: EstadoConsulta<T>; recargar: () => void } {
  const [estado, setEstado] = useState<EstadoConsulta<T>>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const control = new AbortController()
    pedir<T>(ruta, { senal: control.signal })
      .then((datos) => setEstado({ tipo: 'listo', datos }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [ruta, version])

  const recargar = useCallback(() => {
    setEstado({ tipo: 'cargando' })
    setVersion((v) => v + 1)
  }, [])
  return { estado, recargar }
}
