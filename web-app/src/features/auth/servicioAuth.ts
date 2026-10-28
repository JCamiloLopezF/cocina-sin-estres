/**
 * Contrato del proveedor de identidad. Hay dos implementaciones:
 *
 * - `servicioAuthFirebase`: correo y contraseña con Firebase Auth (ADR-05).
 * - `servicioAuthDev`: sin Firebase, para trabajar contra el backend con perfil dev (ADR-19).
 *
 * Las pantallas solo conocen este contrato, así que no cambian entre una y otra.
 */

/** Quién entró, según el proveedor de identidad. El rol lo decide el backend, no esto. */
export interface SesionAuth {
  uid: string
  correo: string
  nombre: string | null
  /**
   * `false` si todavía no confirmó el correo con el enlace que le llegó al registrarse
   * (auditoría del 27 de octubre). Ausente en el modo dev, que no verifica correos.
   */
  correoVerificado?: boolean
}

export interface ServicioAuth {
  /**
   * Avisa cada vez que cambia la sesión, incluida la primera vez, cuando se recupera
   * la sesión guardada al reabrir la aplicación (RNF-19). Devuelve cómo dejar de escuchar.
   */
  escuchar(alCambiar: (sesion: SesionAuth | null) => void): () => void
  entrar(correo: string, contrasena: string): Promise<void>
  registrar(nombre: string, correo: string, contrasena: string): Promise<void>
  /** Manda el enlace de recuperación al correo registrado (RNF-25). */
  recuperar(correo: string): Promise<void>
  salir(): Promise<void>
  /** Vuelve a mandar el enlace para confirmar el correo. */
  reenviarVerificacion?(): Promise<void>
  /** Pregunta otra vez al proveedor si ya se confirmó el correo y avisa el cambio por `escuchar`. */
  comprobarVerificacion?(): Promise<void>
  /** Encabezados que identifican la sesión ante el backend. */
  credenciales(): Promise<Record<string, string>>
}

/** Error del proveedor de identidad con un código que las pantallas traducen. */
export class ErrorAuth extends Error {
  readonly codigo: string

  constructor(codigo: string, mensaje = codigo) {
    super(mensaje)
    this.name = 'ErrorAuth'
    this.codigo = codigo
  }
}
