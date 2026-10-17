import { vi } from 'vitest'
import type { ServicioAuth, SesionAuth } from '../features/auth/servicioAuth'

/** Proveedor de identidad de mentiras para las pruebas: se controla la sesión a mano. */
export function crearServicioFalso(inicial: SesionAuth | null = null) {
  let sesion = inicial
  const oyentes = new Set<(s: SesionAuth | null) => void>()
  const cambiar = (nueva: SesionAuth | null) => {
    sesion = nueva
    oyentes.forEach((o) => o(sesion))
  }

  const servicio = {
    escuchar: vi.fn((alCambiar: (s: SesionAuth | null) => void) => {
      oyentes.add(alCambiar)
      queueMicrotask(() => alCambiar(sesion))
      return () => oyentes.delete(alCambiar)
    }),
    entrar: vi.fn(async (_correo: string, _contrasena: string): Promise<void> => undefined),
    registrar: vi.fn(async (_nombre: string, _correo: string, _contrasena: string): Promise<void> => undefined),
    recuperar: vi.fn(async (_correo: string): Promise<void> => undefined),
    salir: vi.fn(async (): Promise<void> => cambiar(null)),
    credenciales: vi.fn(async (): Promise<Record<string, string>> => (sesion ? { Authorization: 'Bearer prueba' } : {})),
  } satisfies ServicioAuth

  return { servicio, cambiar }
}

export const LUISA: SesionAuth = { uid: 'uid-luisa', correo: 'luisa@correo.com', nombre: 'Luisa' }

/** Respuesta JSON como la que da `fetch`. */
export function respuestaJson(cuerpo: unknown, status = 200, tipo = 'application/json'): Response {
  return new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': tipo } })
}
