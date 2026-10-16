import { FirebaseError, initializeApp } from 'firebase/app'
import {
  browserLocalPersistence,
  createUserWithEmailAndPassword,
  initializeAuth,
  onIdTokenChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signOut,
  updateProfile,
  type User,
} from 'firebase/auth'
import type { ConfiguracionFirebase } from '../../config/entorno'
import { ErrorAuth, type ServicioAuth, type SesionAuth } from './servicioAuth'

/**
 * Inicio de sesión con correo y contraseña en Firebase Auth.
 *
 * La contraseña va directo de la pantalla a Firebase: el backend nunca la ve, solo
 * recibe el token (RNF-21). La sesión se guarda en el navegador para no pedir
 * credenciales al volver (RNF-19).
 */
export function crearServicioAuthFirebase(configuracion: ConfiguracionFirebase): ServicioAuth {
  const app = initializeApp(configuracion)
  const auth = initializeAuth(app, { persistence: browserLocalPersistence })
  // Correos de verificación y recuperación en español de Latinoamérica (plantillas de la consola).
  auth.languageCode = 'es-419'

  const aSesion = (usuario: User): SesionAuth => ({
    uid: usuario.uid,
    correo: usuario.email ?? '',
    nombre: usuario.displayName,
  })

  return {
    escuchar(alCambiar) {
      return onIdTokenChanged(auth, (usuario) => alCambiar(usuario ? aSesion(usuario) : null))
    },

    async entrar(correo, contrasena) {
      await traducir(() => signInWithEmailAndPassword(auth, correo, contrasena))
    },

    async registrar(nombre, correo, contrasena) {
      const { user } = await traducir(() => createUserWithEmailAndPassword(auth, correo, contrasena))
      await traducir(() => updateProfile(user, { displayName: nombre }))
      // Si el correo de verificación falla, la cuenta igual quedó creada: no se bloquea.
      await sendEmailVerification(user).catch(() => undefined)
    },

    async recuperar(correo) {
      await traducir(() => sendPasswordResetEmail(auth, correo))
    },

    async salir() {
      await signOut(auth)
    },

    async credenciales(): Promise<Record<string, string>> {
      const usuario = auth.currentUser
      if (!usuario) {
        return {}
      }
      // Firebase renueva el token solo cuando está por vencer.
      return { Authorization: `Bearer ${await usuario.getIdToken()}` }
    },
  }
}

async function traducir<T>(accion: () => Promise<T>): Promise<T> {
  try {
    return await accion()
  } catch (error) {
    if (error instanceof FirebaseError) {
      throw new ErrorAuth(error.code)
    }
    throw new ErrorAuth('auth/network-request-failed')
  }
}
