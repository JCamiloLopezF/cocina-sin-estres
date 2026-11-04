/**
 * Cliente HTTP del backend.
 *
 * Adjunta las credenciales de la sesión (token de Firebase o X-Dev-User) y convierte
 * las respuestas de error en {@link ErrorApi}, que trae el `codigo` estable del
 * backend (CONTRIBUTING.md §7). Las pantallas deciden el mensaje con ese código,
 * nunca con el texto que mande el servidor.
 */

/** Código propio del frontend para cuando la petición ni siquiera llegó al servidor. */
export const SIN_CONEXION = 'RED_SIN_CONEXION'

/** Código propio para respuestas de error que no vienen en formato Problem Details. */
export const RESPUESTA_INESPERADA = 'RESPUESTA_INESPERADA'

export interface ErrorCampo {
  campo: string
  mensaje: string
}

/** Error de una petición al backend. `status` es 0 cuando no hubo respuesta. */
export class ErrorApi extends Error {
  readonly status: number
  readonly codigo: string
  readonly errores: ErrorCampo[]

  constructor(status: number, codigo: string, detalle: string, errores: ErrorCampo[] = []) {
    super(detalle)
    this.name = 'ErrorApi'
    this.status = status
    this.codigo = codigo
    this.errores = errores
  }
}

/** Devuelve los encabezados que identifican la sesión actual, o ninguno si no hay sesión. */
export type ProveedorCredenciales = () => Promise<Record<string, string>>

let urlBase = ''
let credenciales: ProveedorCredenciales = async () => ({})

/** Lo llama la sesión al arrancar: dónde está el backend y cómo identificarse. */
export function configurarCliente(opciones: { urlBase: string; credenciales: ProveedorCredenciales }): void {
  urlBase = opciones.urlBase.replace(/\/+$/, '')
  credenciales = opciones.credenciales
}

export interface OpcionesPeticion {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  cuerpo?: unknown
  senal?: AbortSignal
  /**
   * Solo GET: guarda la última respuesta en este navegador y, si no hay conexión, la devuelve
   * (RNF-17: el plan se puede ver sin señal). No se usa con datos de salud.
   */
  respaldo?: boolean
}

const PREFIJO_RESPALDO = 'cocina.respaldo:'

function leerRespaldo(ruta: string): string | null {
  try {
    return localStorage.getItem(PREFIJO_RESPALDO + ruta)
  } catch {
    return null
  }
}

function guardarRespaldo(ruta: string, texto: string): void {
  try {
    localStorage.setItem(PREFIJO_RESPALDO + ruta, texto)
  } catch {
    // Sin espacio o sin almacenamiento (modo privado): simplemente no queda respaldo.
  }
}

/** La última respuesta guardada de una ruta con `respaldo`, si hay. */
export function respaldoDe<T>(ruta: string): T | undefined {
  const guardado = leerRespaldo(ruta)
  return guardado === null ? undefined : (JSON.parse(guardado) as T)
}

/** Borra las respuestas guardadas para usar sin conexión; se llama al cerrar sesión. */
export function limpiarRespaldos(): void {
  try {
    Object.keys(localStorage)
      .filter((clave) => clave.startsWith(PREFIJO_RESPALDO))
      .forEach((clave) => localStorage.removeItem(clave))
  } catch {
    // Sin almacenamiento: no hay nada que borrar.
  }
}

/**
 * Hace una petición al backend. `ruta` es relativa a `/api`, por ejemplo `/yo`.
 * Devuelve el JSON de la respuesta, o `undefined` si la respuesta no tiene cuerpo.
 */
export async function pedir<T>(ruta: string, opciones: OpcionesPeticion = {}): Promise<T> {
  const encabezados: Record<string, string> = { Accept: 'application/json', ...(await credenciales()) }
  if (opciones.cuerpo !== undefined) {
    encabezados['Content-Type'] = 'application/json'
  }

  const metodo = opciones.metodo ?? 'GET'
  const enviar = () =>
    fetch(`${urlBase}${ruta}`, {
      method: metodo,
      headers: encabezados,
      body: opciones.cuerpo === undefined ? undefined : JSON.stringify(opciones.cuerpo),
      signal: opciones.senal,
    })

  let respuesta: Response
  try {
    respuesta = await enviar()
    // Auditoría del 4 de noviembre: el recetario mostró «Algo falló» cuando la API tardó en
    // responder. Un 502, 503 o 504 viene de CloudFront o nginx mientras la API reinicia o
    // está ocupada; una lectura (GET) se puede repetir sin riesgo, así que se reintenta.
    for (const espera of metodo === 'GET' ? ESPERAS_REINTENTO : []) {
      if (!ESTADOS_TRANSITORIOS.has(respuesta.status)) break
      await esperar(espera, opciones.senal)
      respuesta = await enviar()
    }
  } catch (causa) {
    if (causa instanceof DOMException && causa.name === 'AbortError') {
      throw causa
    }
    const guardado = opciones.respaldo ? leerRespaldo(ruta) : null
    if (guardado !== null) {
      return JSON.parse(guardado) as T
    }
    throw new ErrorApi(0, SIN_CONEXION, 'No hubo respuesta del servidor.')
  }

  if (respuesta.ok) {
    if (respuesta.status === 204) {
      return undefined as T
    }
    const texto = await respuesta.text()
    if (opciones.respaldo && texto) {
      guardarRespaldo(ruta, texto)
    }
    return (texto ? JSON.parse(texto) : undefined) as T
  }
  throw await leerError(respuesta)
}

async function leerError(respuesta: Response): Promise<ErrorApi> {
  try {
    const problema = (await respuesta.json()) as {
      codigo?: unknown
      detail?: unknown
      errores?: unknown
    }
    if (typeof problema.codigo === 'string') {
      return new ErrorApi(
        respuesta.status,
        problema.codigo,
        typeof problema.detail === 'string' ? problema.detail : '',
        Array.isArray(problema.errores) ? (problema.errores as ErrorCampo[]) : [],
      )
    }
  } catch {
    // Cuerpo vacío o que no es JSON: lo resuelve el return de abajo.
  }
  // Un 429 o un 413 pueden venir del balanceador, sin Problem Details: igual se
  // reconocen para mostrar el mensaje que corresponde.
  const codigoPorEstado = CODIGO_POR_ESTADO[respuesta.status] ?? RESPUESTA_INESPERADA
  return new ErrorApi(respuesta.status, codigoPorEstado, `El servidor respondió ${respuesta.status}.`)
}

/** Respuestas de la infraestructura que suelen pasar solas en uno o dos segundos. */
const ESTADOS_TRANSITORIOS = new Set([502, 503, 504])
/** Milisegundos antes de cada reintento de una lectura. */
const ESPERAS_REINTENTO = [500, 1500]

function esperar(ms: number, senal?: AbortSignal): Promise<void> {
  return new Promise((resolver, rechazar) => {
    if (senal?.aborted) {
      rechazar(new DOMException('Cancelada', 'AbortError'))
      return
    }
    const temporizador = setTimeout(resolver, ms)
    senal?.addEventListener(
      'abort',
      () => {
        clearTimeout(temporizador)
        rechazar(new DOMException('Cancelada', 'AbortError'))
      },
      { once: true },
    )
  })
}

const CODIGO_POR_ESTADO: Record<number, string> = {
  413: 'CUERPO_DEMASIADO_GRANDE',
  429: 'LIMITE_PETICIONES',
}
