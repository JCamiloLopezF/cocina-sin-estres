import { createContext, useContext } from 'react'
import { ErrorApi } from '../../api/cliente'
import type { ServicioAuth, SesionAuth } from './servicioAuth'

/** Roles de ADR-16. Los decide el backend; el frontend solo los usa para el menú. */
export type Rol = 'USUARIA' | 'CURADOR' | 'ADMIN'

/** Respuesta de `GET /api/yo` (UsuarioResponse en el backend). */
export interface Cuenta {
  id: number
  correo: string
  rol: Rol
  perfilCompleto: boolean
}

export type EstadoSesion =
  | { tipo: 'cargando' }
  | { tipo: 'sin-sesion' }
  | { tipo: 'con-sesion'; sesion: SesionAuth; cuenta: Cuenta }
  /** Entró en Firebase pero el backend no respondió o rechazó la cuenta. */
  | { tipo: 'error'; sesion: SesionAuth; error: unknown }

export interface ContextoSesion {
  estado: EstadoSesion
  servicio: ServicioAuth
  /** Vuelve a pedir la cuenta al backend, por ejemplo después de completar el perfil. */
  recargarCuenta(): Promise<void>
  salir(): Promise<void>
}

export const Contexto = createContext<ContextoSesion | null>(null)

/** Acceso a la sesión desde cualquier pantalla. */
export function useSesion(): ContextoSesion {
  const contexto = useContext(Contexto)
  if (!contexto) {
    throw new Error('useSesion se usa dentro de <ProveedorSesion>')
  }
  return contexto
}

/** La cuenta de quien está dentro. Solo se usa en rutas protegidas. */
export function useCuenta(): Cuenta {
  const { estado } = useSesion()
  if (estado.tipo !== 'con-sesion') {
    throw new Error('useCuenta se usa solo dentro de una ruta con sesión')
  }
  return estado.cuenta
}

/** Indica si el error del backend obliga a cerrar la sesión (cuenta desactivada o token rechazado). */
export function esErrorDeCuenta(error: unknown): boolean {
  return error instanceof ErrorApi && (error.status === 401 || error.codigo === 'AUTH_CUENTA_INACTIVA')
}
