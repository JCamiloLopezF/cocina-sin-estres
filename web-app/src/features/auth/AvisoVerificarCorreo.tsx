import { useState } from 'react'
import { Alerta, Boton } from '../../shared/componentes'
import { useSesion } from './contextoSesion'
import { mensajeDeErrorAuth } from './mensajesAuth'

/**
 * Pide confirmar el correo mientras no se haya hecho (auditoría del 27 de octubre: las
 * cuentas entraban y guardaban datos de salud sin verificar el correo). No bloquea: la
 * usuaria puede seguir usando la app, pero ve el aviso hasta que confirme.
 */
export function AvisoVerificarCorreo() {
  const { estado, servicio } = useSesion()
  const [mensaje, setMensaje] = useState<{ tono: 'info' | 'error'; texto: string }>()
  const [ocupado, setOcupado] = useState(false)

  if (estado.tipo !== 'con-sesion' || estado.sesion.correoVerificado !== false) {
    return null
  }

  async function hacer(accion: (() => Promise<void>) | undefined, siSale: string) {
    if (!accion) return
    setOcupado(true)
    setMensaje(undefined)
    try {
      await accion()
      setMensaje({ tono: 'info', texto: siSale })
    } catch (error) {
      setMensaje({ tono: 'error', texto: mensajeDeErrorAuth(error) })
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="aviso-correo">
      <Alerta tono="info" titulo="Confirma tu correo">
        Te enviamos un enlace a {estado.sesion.correo}. Ábrelo para proteger tu cuenta y tus datos de salud.
        <div className="aviso-correo__acciones">
          <Boton
            variante="secundario"
            cargando={ocupado}
            onClick={() =>
              void hacer(
                servicio.reenviarVerificacion?.bind(servicio),
                'Te enviamos el enlace otra vez. Revisa también la carpeta de spam.',
              )
            }
          >
            Reenviar el enlace
          </Boton>
          <Boton
            variante="texto"
            disabled={ocupado}
            onClick={() =>
              void hacer(
                servicio.comprobarVerificacion?.bind(servicio),
                'Todavía no vemos el correo confirmado. Abre el enlace y vuelve a intentarlo.',
              )
            }
          >
            Ya lo confirmé
          </Boton>
        </div>
      </Alerta>
      {mensaje && <Alerta tono={mensaje.tono}>{mensaje.texto}</Alerta>}
    </div>
  )
}
