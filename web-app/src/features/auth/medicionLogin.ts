import { pedir } from '../../api/cliente'

/**
 * Medición del inicio de sesión para el Plan de Calidad (KAN-54): éxito de autenticación
 * (> 95 %) y tiempo de respuesta de login (< 1500 ms). No guarda el correo ni la
 * contraseña: solo cuándo empezó el intento y cuántos fallaron antes en este navegador.
 */

const CLAVE_INICIO = 'cocina.login.inicio'
const CLAVE_FALLIDOS = 'cocina.login.fallidos'
const FALLIDOS_MAXIMO = 50

/** La usuaria tocó «Entrar» con los datos completos. */
export function empezarIntento(ahora = Date.now()) {
  try {
    sessionStorage.setItem(CLAVE_INICIO, String(ahora))
  } catch {
    // Sin almacenamiento no se mide; la app sigue igual.
  }
}

/** El intento no funcionó (contraseña equivocada, sin conexión…). */
export function intentoFallido() {
  try {
    sessionStorage.removeItem(CLAVE_INICIO)
    const antes = Number(localStorage.getItem(CLAVE_FALLIDOS) ?? '0') || 0
    localStorage.setItem(CLAVE_FALLIDOS, String(Math.min(antes + 1, FALLIDOS_MAXIMO)))
  } catch {
    // Sin almacenamiento no se mide.
  }
}

/**
 * La API ya reconoció la cuenta. Si esto vino de un intento de «Entrar» (no de abrir la app
 * con la sesión guardada), manda cuánto tardó y cuántos fallaron antes. Nunca falla: una
 * métrica no puede interrumpir la entrada.
 */
export async function reportarInicioSesion(ahora = Date.now()): Promise<void> {
  let inicio: number
  let fallidosPrevios: number
  try {
    const guardado = sessionStorage.getItem(CLAVE_INICIO)
    if (guardado === null) return
    inicio = Number(guardado)
    fallidosPrevios = Number(localStorage.getItem(CLAVE_FALLIDOS) ?? '0') || 0
    sessionStorage.removeItem(CLAVE_INICIO)
    localStorage.removeItem(CLAVE_FALLIDOS)
  } catch {
    return
  }
  if (!Number.isFinite(inicio)) return
  const duracionMs = Math.max(0, Math.round(ahora - inicio))
  try {
    await pedir('/metricas/inicio-sesion', { metodo: 'POST', cuerpo: { duracionMs, fallidosPrevios } })
  } catch {
    // Se pierde esta medición; no pasa nada más.
  }
}
