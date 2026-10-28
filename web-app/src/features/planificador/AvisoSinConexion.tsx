import { useEffect, useState } from 'react'
import { Alerta } from '../../shared/componentes'
import { alCambiarPendientes, leerPendientes } from './sinConexion'

function conectado(): boolean {
  return typeof navigator === 'undefined' || navigator.onLine !== false
}

/**
 * RNF-17: avisa que no hay conexión y cuántos cambios del plan están guardados en este
 * celular esperando para enviarse. No se muestra si hay señal y no hay nada pendiente.
 */
export function AvisoSinConexion() {
  const [enLinea, setEnLinea] = useState(conectado)
  const [pendientes, setPendientes] = useState(() => leerPendientes().length)

  useEffect(() => {
    const actualizar = () => {
      setEnLinea(conectado())
      setPendientes(leerPendientes().length)
    }
    window.addEventListener('online', actualizar)
    window.addEventListener('offline', actualizar)
    const quitar = alCambiarPendientes(actualizar)
    return () => {
      window.removeEventListener('online', actualizar)
      window.removeEventListener('offline', actualizar)
      quitar()
    }
  }, [])

  if (enLinea && pendientes === 0) return null
  const cambios = pendientes === 1 ? '1 cambio guardado' : `${pendientes} cambios guardados`
  return (
    <div className="plan__resultado" role="status">
      <Alerta tono="info">
        {enLinea
          ? `Enviando ${pendientes === 1 ? 'tu cambio' : `tus ${pendientes} cambios`} del plan…`
          : pendientes > 0
            ? `Sin conexión. Tienes ${cambios} en este celular; los enviamos apenas vuelva la señal.`
            : 'Sin conexión. Ves tu plan como lo dejaste; lo que cambies se guarda aquí y se envía cuando vuelva la señal.'}
      </Alerta>
    </div>
  )
}
