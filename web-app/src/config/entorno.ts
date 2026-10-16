/**
 * Variables de entorno de la aplicación, leídas una sola vez y validadas.
 *
 * Vite solo expone al navegador las variables que empiezan por VITE_. Ninguna es
 * secreta: todo lo que llega aquí termina en el código que descarga cualquier persona.
 */

/** Cómo se entra a la aplicación. Ver `.env.example`. */
export type ModoAuth = 'firebase' | 'dev'

export interface ConfiguracionFirebase {
  apiKey: string
  authDomain: string
  projectId: string
  appId: string
}

export interface Entorno {
  /** URL base del backend, terminada en `/api` y sin barra final. */
  apiUrl: string
  modoAuth: ModoAuth
  /** Solo existe cuando `modoAuth` es `firebase`. */
  firebase?: ConfiguracionFirebase
}

/** Se lanza cuando falta una variable obligatoria; `main.tsx` la muestra en pantalla. */
export class ConfiguracionIncompletaError extends Error {
  readonly faltantes: string[]

  constructor(faltantes: string[]) {
    super(`Faltan variables de entorno: ${faltantes.join(', ')}`)
    this.name = 'ConfiguracionIncompletaError'
    this.faltantes = faltantes
  }
}

type Variables = Record<string, string | undefined>

/**
 * Arma el entorno a partir de las variables. Recibe las variables como parámetro
 * para poder probarla sin depender de `import.meta.env`.
 */
export function leerEntorno(variables: Variables): Entorno {
  const faltantes: string[] = []
  const leer = (nombre: string): string => {
    const valor = variables[nombre]?.trim()
    if (!valor) {
      faltantes.push(nombre)
      return ''
    }
    return valor
  }

  const apiUrl = leer('VITE_API_URL').replace(/\/+$/, '')
  const modo = variables.VITE_AUTH_MODO?.trim() || 'firebase'
  if (modo !== 'firebase' && modo !== 'dev') {
    throw new ConfiguracionIncompletaError([`VITE_AUTH_MODO (debe ser firebase o dev, llegó "${modo}")`])
  }

  let firebase: ConfiguracionFirebase | undefined
  if (modo === 'firebase') {
    firebase = {
      apiKey: leer('VITE_FIREBASE_API_KEY'),
      authDomain: leer('VITE_FIREBASE_AUTH_DOMAIN'),
      projectId: leer('VITE_FIREBASE_PROJECT_ID'),
      appId: leer('VITE_FIREBASE_APP_ID'),
    }
  }

  if (faltantes.length > 0) {
    throw new ConfiguracionIncompletaError(faltantes)
  }
  return { apiUrl, modoAuth: modo, firebase }
}

let cache: Entorno | undefined

/** Entorno de la aplicación en ejecución. Lanza {@link ConfiguracionIncompletaError}. */
export function entorno(): Entorno {
  cache ??= leerEntorno(import.meta.env as Variables)
  return cache
}
