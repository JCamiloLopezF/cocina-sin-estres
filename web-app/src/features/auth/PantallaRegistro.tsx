import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, CampoTexto } from '../../shared/componentes'
import { DisenoAcceso } from './DisenoAcceso'
import { enfocarPrimerError } from './enfocarPrimerError'
import { mensajeDeErrorAuth } from './mensajesAuth'
import { useSesion } from './contextoSesion'
import {
  REGLAS_CONTRASENA,
  validarConfirmacion,
  validarContrasenaNueva,
  validarCorreo,
  validarNombre,
} from './validaciones'

interface Errores {
  nombre?: string
  correo?: string
  contrasena?: string
  confirmacion?: string
  autorizacion?: string
}

/** Crear cuenta con nombre, correo y contraseña. Firebase manda el correo de verificación. */
export function PantallaRegistro() {
  const { servicio } = useSesion()
  const formulario = useRef<HTMLFormElement>(null)
  const [nombre, setNombre] = useState('')
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [autoriza, setAutoriza] = useState(false)
  const [mostrar, setMostrar] = useState(false)
  const [errores, setErrores] = useState<Errores>({})
  const [errorGeneral, setErrorGeneral] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErrorGeneral(undefined)
    const nuevos: Errores = {
      nombre: validarNombre(nombre),
      correo: validarCorreo(correo),
      contrasena: validarContrasenaNueva(contrasena),
      confirmacion: validarConfirmacion(contrasena, confirmacion),
      autorizacion: autoriza ? undefined : 'Para crear la cuenta necesitamos tu autorización.',
    }
    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) {
      enfocarPrimerError(formulario.current)
      return
    }

    setEnviando(true)
    try {
      await servicio.registrar(nombre.trim(), correo.trim(), contrasena)
    } catch (error) {
      setErrorGeneral(mensajeDeErrorAuth(error))
      setEnviando(false)
    }
  }

  return (
    <DisenoAcceso titulo="Crear cuenta" detalle="Después te pediremos unos datos de tu hogar para personalizar las recetas.">
      <form ref={formulario} className="formulario" onSubmit={enviar} noValidate>
        {errorGeneral && <Alerta tono="error">{errorGeneral}</Alerta>}
        <CampoTexto
          etiqueta="Tu nombre"
          name="nombre"
          autoComplete="given-name"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          error={errores.nombre}
        />
        <CampoTexto
          etiqueta="Correo"
          type="email"
          name="correo"
          inputMode="email"
          autoComplete="email"
          value={correo}
          onChange={(e) => setCorreo(e.target.value)}
          error={errores.correo}
        />
        <CampoTexto
          etiqueta="Contraseña"
          ayuda="Debe cumplir las reglas de la lista."
          type={mostrar ? 'text' : 'password'}
          name="contrasena"
          autoComplete="new-password"
          value={contrasena}
          onChange={(e) => setContrasena(e.target.value)}
          error={errores.contrasena}
        />
        <ul className="reglas-contrasena" aria-label="Reglas de la contraseña">
          {REGLAS_CONTRASENA.map((regla) => {
            const cumple = regla.cumple(contrasena)
            return (
              <li key={regla.texto} className={cumple ? 'reglas-contrasena__ok' : undefined}>
                <span aria-hidden="true">{cumple ? '✓' : '○'}</span> {regla.texto}
                <span className="solo-lectores">{cumple ? ' (cumple)' : ' (falta)'}</span>
              </li>
            )
          })}
        </ul>
        <CampoTexto
          etiqueta="Confirmar contraseña"
          type={mostrar ? 'text' : 'password'}
          name="confirmacion"
          autoComplete="new-password"
          value={confirmacion}
          onChange={(e) => setConfirmacion(e.target.value)}
          error={errores.confirmacion}
        />
        <label className="formulario__mostrar">
          <input type="checkbox" checked={mostrar} onChange={(e) => setMostrar(e.target.checked)} />
          Mostrar contraseña
        </label>
        <div className="campo">
          <label className="casilla">
            <input
              type="checkbox"
              name="autorizacion"
              checked={autoriza}
              aria-invalid={errores.autorizacion ? true : undefined}
              aria-describedby={errores.autorizacion ? 'error-autorizacion' : undefined}
              onChange={(e) => setAutoriza(e.target.checked)}
            />
            Autorizo el tratamiento de mis datos personales y los de salud de mi hogar (Ley 1581 de 2012).
          </label>
          {errores.autorizacion && (
            <span className="campo__error" id="error-autorizacion">
              {errores.autorizacion}
            </span>
          )}
        </div>
        <Boton type="submit" ancho cargando={enviando}>
          Crear cuenta
        </Boton>
      </form>
      <div className="acceso__enlaces">
        <p>
          ¿Ya tienes cuenta? <Link to="/entrar">Entrar</Link>
        </p>
      </div>
    </DisenoAcceso>
  )
}
