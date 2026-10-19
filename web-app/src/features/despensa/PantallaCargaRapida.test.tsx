import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { CATALOGO, rutasCocina } from '../../prueba/cocinaFalsa'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'

describe('Carga rápida y Todo listo (pantallas 11 y 12)', () => {
  it('agrega lo marcado con una cantidad de partida y cierra con el resumen del hogar', async () => {
    const { llamadas } = backendFalso({
      ...rutasCocina(),
      'POST /items-despensa': (cuerpo) => respuestaJson({ id: 90, ...(cuerpo as object) }, 201),
      'GET /perfil/hogar': () =>
        respuestaJson({ personas: [{}, {}], presupuestoMonto: 780000, periodicidadIngreso: 'QUINCENAL' }),
      'GET /items-despensa/resumen': () => respuestaJson({ total: 4, porVencer: 0, bajoMinimo: 0 }),
    })
    const { router } = montarApp(crearServicioFalso(LUISA).servicio, '/despensa/carga-rapida')
    const usuario = userEvent.setup()

    // Lo que ya está en la despensa (lenteja y cebolla) no se ofrece otra vez.
    expect(await screen.findByRole('button', { name: 'Zanahoria' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lenteja' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Marca al menos un producto' })).toBeDisabled()

    await usuario.click(screen.getByRole('button', { name: 'Zanahoria' }))
    await usuario.click(screen.getByRole('button', { name: 'Arroz' }))
    expect(screen.getByRole('button', { name: 'Zanahoria' })).toHaveAttribute('aria-pressed', 'true')
    await usuario.click(screen.getByRole('button', { name: 'Agregar 2 productos' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Tu cocina ya está organizada' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/listo')
    expect(await screen.findByText('2 personas en casa')).toBeInTheDocument()
    expect(screen.getByText(/780\.000 · ingreso quincenal/)).toBeInTheDocument()
    expect(screen.getByText('4 productos en la despensa')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Armar mi primera semana' })).toHaveAttribute('href', '/plan')

    const enviados = llamadas.filter((l) => l.metodo === 'POST').map((l) => l.cuerpo)
    expect(enviados).toEqual(
      expect.arrayContaining([
        { productoId: 3, cantidad: CATALOGO[2].cantidadUnidadCompra },
        { productoId: 4, nivel: 'MEDIO' },
      ]),
    )
  })

  it('"Omitir" va directo a Todo listo', async () => {
    backendFalso(rutasCocina())
    const { router } = montarApp(crearServicioFalso(LUISA).servicio, '/despensa/carga-rapida')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('link', { name: 'Omitir' }))
    expect(router.state.location.pathname).toBe('/listo')
  })
})
