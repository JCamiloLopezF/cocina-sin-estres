import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, CampoTexto } from '../../shared/componentes'
import { DisenoAcceso } from './DisenoAcceso'
import { enfocarPrimerError } from './enfocarPrimerError'
import { mensajeDeErrorAuth } from './mensajesAuth'
import { ErrorAuth } from './servicioAuth'
import { useSesion } from './contextoSesion'
import { validarCorreo } from './validaciones'

/**
 * Recuperar la contraseña. Firebase manda un enlace al correo registrado y la
 * contraseña solo se cambia desde ese enlace (RNF-25).
 *
 * El mensaje de confirmación es el mismo exista o no la cuenta, para no revelar
 * qué correos están registrados.
 */
export function PantallaRecuperar() {
  const { servicio } = useSesion()
  const formulario = useRef<HTMLFormElement>(null)
  const [correo, setCorreo] = useState('')
  const [errorCorreo, setErrorCorreo] = useState<string>()
  const [errorGeneral, setErrorGeneral] = useState<string>()
  const [enviando, setEnviando] = useState(false)
  const [enviadoA, setEnviadoA] = useState<string>()

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErrorGeneral(undefined)
    const error = validarCorreo(correo)
    setErrorCorreo(error)
    if (error) {
      enfocarPrimerError(formulario.current)
      return
    }

    setEnviando(true)
    try {
      await servicio.recuperar(correo.trim())
      setEnviadoA(correo.trim())
    } catch (causa) {
      if (causa instanceof ErrorAuth && causa.codigo === 'auth/user-not-found') {
        setEnviadoA(correo.trim())
      } else {
        setErrorGeneral(mensajeDeErrorAuth(causa))
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <DisenoAcceso
      titulo="Recuperar contraseña"
      detalle="Te enviamos un enlace a tu correo para que crees una contraseña nueva."
    >
      {enviadoA ? (
        <div className="formulario">
          <Alerta tono="exito" titulo="Revisa tu correo">
            Si hay una cuenta con {enviadoA}, te llegará un enlace en unos minutos. Si no lo ves, busca en
            la carpeta de correo no deseado.
          </Alerta>
          <Link to="/entrar">Volver a entrar</Link>
        </div>
      ) : (
        <form ref={formulario} className="formulario" onSubmit={enviar} noValidate>
          {errorGeneral && <Alerta tono="error">{errorGeneral}</Alerta>}
          <CampoTexto
            etiqueta="Correo"
            type="email"
            name="correo"
            inputMode="email"
            autoComplete="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            error={errorCorreo}
          />
          <Boton type="submit" ancho cargando={enviando}>
            Enviar enlace
          </Boton>
          <Link to="/entrar">Volver a entrar</Link>
        </form>
      )}
    </DisenoAcceso>
  )
}
