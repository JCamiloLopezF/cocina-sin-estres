import { describe, expect, it, vi } from 'vitest'
import { crearServicioAuthDev } from './servicioAuthDev'

describe('servicio de acceso en modo dev', () => {
  it('entra con un nombre de prueba, lo manda como X-Dev-User y lo recuerda', async () => {
    const servicio = crearServicioAuthDev(window.localStorage)
    const alCambiar = vi.fn()
    servicio.escuchar(alCambiar)
    await Promise.resolve()
    expect(alCambiar).toHaveBeenLastCalledWith(null)

    await servicio.entrar('  Luisa@algo.com ', '')

    expect(alCambiar).toHaveBeenLastCalledWith({ uid: 'luisa', correo: 'luisa@dev.local', nombre: 'luisa' })
    expect(await servicio.credenciales()).toEqual({ 'X-Dev-User': 'luisa' })
    // Al reabrir la aplicación la sesión sigue (RNF-19).
    expect(await crearServicioAuthDev(window.localStorage).credenciales()).toEqual({ 'X-Dev-User': 'luisa' })
  })

  it('al salir deja de mandar credenciales', async () => {
    const servicio = crearServicioAuthDev(window.localStorage)
    await servicio.entrar('dillan', '')
    await servicio.salir()
    expect(await servicio.credenciales()).toEqual({})
  })
})
