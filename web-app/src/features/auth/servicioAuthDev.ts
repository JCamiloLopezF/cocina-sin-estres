import { ErrorAuth, type ServicioAuth, type SesionAuth } from './servicioAuth'

const CLAVE = 'cocina.dev.usuario'
/** Mismo nombre que FiltroUsuarioDev.ENCABEZADO en el backend. */
const ENCABEZADO = 'X-Dev-User'

/**
 * Sesión de mentiras para desarrollo local, sin Firebase (ADR-19).
 *
 * Se entra con un nombre de prueba (por ejemplo `luisa`), que viaja como `X-Dev-User`.
 * El backend con perfil dev crea la cuenta `luisa@dev.local`. En producción ese
 * encabezado no hace nada, así que este modo no abre ninguna puerta.
 */
export function crearServicioAuthDev(almacen: Storage = window.localStorage): ServicioAuth {
  const oyentes = new Set<(sesion: SesionAuth | null) => void>()

  const leer = (): SesionAuth | null => {
    const uid = almacen.getItem(CLAVE)
    return uid ? { uid, correo: `${uid}@dev.local`, nombre: uid } : null
  }
  const avisar = () => {
    const sesion = leer()
    oyentes.forEach((oyente) => oyente(sesion))
  }
  const guardar = (nombre: string) => {
    const uid = nombre.trim().toLowerCase().replace(/@.*$/, '').replace(/[^a-z0-9._-]/g, '')
    if (!uid) {
      throw new ErrorAuth('auth/invalid-email')
    }
    almacen.setItem(CLAVE, uid)
    avisar()
  }

  return {
    escuchar(alCambiar) {
      oyentes.add(alCambiar)
      queueMicrotask(() => alCambiar(leer()))
      return () => oyentes.delete(alCambiar)
    },
    async entrar(correo) {
      guardar(correo)
    },
    async registrar(_nombre, correo) {
      guardar(correo)
    },
    async recuperar() {
      // No hay contraseñas en modo dev.
    },
    async salir() {
      almacen.removeItem(CLAVE)
      avisar()
    },
    async credenciales(): Promise<Record<string, string>> {
      const sesion = leer()
      return sesion ? { [ENCABEZADO]: sesion.uid } : {}
    },
  }
}
