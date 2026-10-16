import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, CampoTexto } from '../../shared/componentes'
import { DisenoAcceso } from './DisenoAcceso'
import { enfocarPrimerError } from './enfocarPrimerError'
import { mensajeDeErrorAuth } from './mensajesAuth'
import { useSesion } from './contextoSesion'
import { validarCorreo } from './validaciones'

interface Errores {
  correo?: string
  contrasena?: string
}

/** Inicio de sesión con correo y contraseña. En modo dev pide solo un nombre de prueba. */
export function PantallaEntrar({ modoDev = false }: { modoDev?: boolean }) {
  const { servicio } = useSesion()
  const formulario = useRef<HTMLFormElement>(null)
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [mostrar, setMostrar] = useState(false)
  const [errores, setErrores] = useState<Errores>({})
  const [errorGeneral, setErrorGeneral] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErrorGeneral(undefined)
    const nuevos: Errores = modoDev
      ? { correo: correo.trim() ? undefined : 'Escribe un nombre de prueba.' }
      : { correo: validarCorreo(correo), contrasena: contrasena ? undefined : 'Escribe tu contraseña.' }
    setErrores(nuevos)
    if (nuevos.correo || nuevos.contrasena) {
      enfocarPrimerError(formulario.current)
      return
    }

    setEnviando(true)
    try {
      // Si sale bien, la sesión cambia y la ruta lleva al inicio.
      await servicio.entrar(correo.trim(), contrasena)
    } catch (error) {
      setErrorGeneral(mensajeDeErrorAuth(error))
      setEnviando(false)
    }
  }

  return (
    <DisenoAcceso titulo="Entrar" detalle={modoDev ? undefined : 'Usa el correo con el que creaste tu cuenta.'}>
      <form ref={formulario} className="formulario" onSubmit={enviar} noValidate>
        {modoDev && (
          <Alerta tono="info" titulo="Modo de desarrollo">
            No se usa Firebase. Escribe cualquier nombre, por ejemplo <code>luisa</code>, y entras como esa
            usuaria de prueba. Solo funciona con el backend en perfil dev.
          </Alerta>
        )}
        {errorGeneral && <Alerta tono="error">{errorGeneral}</Alerta>}

        {modoDev ? (
          <CampoTexto
            etiqueta="Nombre de prueba"
            name="usuario"
            autoComplete="username"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            error={errores.correo}
          />
        ) : (
          <>
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
              type={mostrar ? 'text' : 'password'}
              name="contrasena"
              autoComplete="current-password"
              value={contrasena}
              onChange={(e) => setContrasena(e.target.value)}
              error={errores.contrasena}
            />
            <label className="formulario__mostrar">
              <input type="checkbox" checked={mostrar} onChange={(e) => setMostrar(e.target.checked)} />
              Mostrar contraseña
            </label>
          </>
        )}

        <Boton type="submit" ancho cargando={enviando}>
          Entrar
        </Boton>
      </form>

      {!modoDev && (
        <div className="acceso__enlaces">
          <Link to="/recuperar">Olvidé mi contraseña</Link>
          <p>
            ¿Todavía no tienes cuenta? <Link to="/registro">Crear cuenta</Link>
          </p>
        </div>
      )}
    </DisenoAcceso>
  )
}
