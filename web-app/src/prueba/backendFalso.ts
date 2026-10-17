import { vi } from 'vitest'
import { respuestaJson } from './servicioFalso'

type Manejador = (cuerpo: unknown, url: URL) => Response | Promise<Response>

/**
 * Backend de mentiras para las pruebas de pantallas: cada clave es "MÉTODO /ruta" (sin
 * /api ni consulta). Guarda todas las llamadas para revisar qué mandó la pantalla.
 */
export function backendFalso(rutas: Record<string, Manejador>) {
  const llamadas: { metodo: string; ruta: string; consulta: URLSearchParams; cuerpo: unknown }[] = []
  const fetchFalso = vi.fn(async (entrada: string | URL | Request, opciones?: RequestInit) => {
    const url = new URL(String(entrada))
    const metodo = opciones?.method ?? 'GET'
    const ruta = url.pathname.replace(/^\/api/, '')
    const cuerpo = opciones?.body ? JSON.parse(String(opciones.body)) : undefined
    llamadas.push({ metodo, ruta, consulta: url.searchParams, cuerpo })
    const manejador = rutas[`${metodo} ${ruta}`]
    if (!manejador) {
      return respuestaJson({ status: 404, codigo: 'RECURSO_NO_ENCONTRADO' }, 404)
    }
    return manejador(cuerpo, url)
  })
  vi.stubGlobal('fetch', fetchFalso)
  return { llamadas, fetchFalso }
}

export const CUENTA_USUARIA = { id: 1, correo: 'luisa@correo.com', rol: 'USUARIA', perfilCompleto: true }
