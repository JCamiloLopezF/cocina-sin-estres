import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, CampoTexto } from '../../shared/componentes'
import { DisenoAcceso } from './DisenoAcceso'
import { enfocarPrimerError } from './enfocarPrimerError'
import { mensajeDeErrorAuth } from './mensajesAuth'
import { useSesion } from './contextoSesion'
import { LARGO_MINIMO_CONTRASENA, validarContrasenaNueva, validarCorreo, validarNombre } from './validaciones'

interface Errores {
  nombre?: string
  correo?: string
  contrasena?: string
}

/** Crear cuenta con nombre, correo y contraseña. Firebase manda el correo de verificación. */
export function PantallaRegistro() {
  const { servicio } = useSesion()
  const formulario = useRef<HTMLFormElement>(null)
  const [nombre, setNombre] = useState('')
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
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
    }
    setErrores(nuevos)
    if (nuevos.nombre || nuevos.correo || nuevos.contrasena) {
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
          ayuda={`Mínimo ${LARGO_MINIMO_CONTRASENA} caracteres.`}
          type={mostrar ? 'text' : 'password'}
          name="contrasena"
          autoComplete="new-password"
          value={contrasena}
          onChange={(e) => setContrasena(e.target.value)}
          error={errores.contrasena}
        />
        <label className="formulario__mostrar">
          <input type="checkbox" checked={mostrar} onChange={(e) => setMostrar(e.target.checked)} />
          Mostrar contraseña
        </label>
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
