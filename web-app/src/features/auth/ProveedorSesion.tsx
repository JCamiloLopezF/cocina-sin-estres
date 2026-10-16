import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { configurarCliente, pedir } from '../../api/cliente'
import { Contexto, type Cuenta, type EstadoSesion } from './contextoSesion'
import type { ServicioAuth, SesionAuth } from './servicioAuth'

interface Props {
  servicio: ServicioAuth
  urlApi: string
  children: ReactNode
}

/**
 * Mantiene la sesión de toda la aplicación.
 *
 * Cuando el proveedor de identidad avisa que alguien entró, pide `GET /api/yo`: esa
 * llamada da de alta la cuenta la primera vez (ADR-19) y trae el rol y si falta el
 * perfil del hogar.
 */
export function ProveedorSesion({ servicio, urlApi, children }: Props) {
  const [estado, setEstado] = useState<EstadoSesion>({ tipo: 'cargando' })
  const sesionActual = useRef<SesionAuth | null>(null)

  useEffect(() => {
    configurarCliente({ urlBase: urlApi, credenciales: () => servicio.credenciales() })
  }, [servicio, urlApi])

  const cargarCuenta = useCallback(async (sesion: SesionAuth) => {
    try {
      const cuenta = await pedir<Cuenta>('/yo')
      if (sesionActual.current?.uid === sesion.uid) {
        setEstado({ tipo: 'con-sesion', sesion, cuenta })
      }
    } catch (error) {
      if (sesionActual.current?.uid === sesion.uid) {
        setEstado({ tipo: 'error', sesion, error })
      }
    }
  }, [])

  useEffect(() => {
    return servicio.escuchar((sesion) => {
      const anterior = sesionActual.current
      sesionActual.current = sesion
      if (!sesion) {
        setEstado({ tipo: 'sin-sesion' })
        return
      }
      // Firebase también avisa cada hora cuando renueva el token: si es la misma
      // persona no hace falta volver a pedir la cuenta.
      if (anterior?.uid === sesion.uid) {
        return
      }
      setEstado({ tipo: 'cargando' })
      void cargarCuenta(sesion)
    })
  }, [servicio, cargarCuenta])

  const recargarCuenta = useCallback(async () => {
    const sesion = sesionActual.current
    if (sesion) {
      await cargarCuenta(sesion)
    }
  }, [cargarCuenta])

  const salir = useCallback(() => servicio.salir(), [servicio])

  const valor = useMemo(
    () => ({ estado, servicio, recargarCuenta, salir }),
    [estado, servicio, recargarCuenta, salir],
  )
  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

