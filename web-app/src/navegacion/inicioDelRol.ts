import type { Rol } from '../features/auth/contextoSesion'

/** Pantalla de inicio de cada rol (ADR-16). */
export function inicioDelRol(rol: Rol): string {
  switch (rol) {
    case 'CURADOR':
      return '/curaduria'
    case 'ADMIN':
      return '/admin'
    default:
      return '/'
  }
}
