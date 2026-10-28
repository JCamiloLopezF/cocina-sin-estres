import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { rutasCocina } from '../../prueba/cocinaFalsa'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'

describe('Producto propio del hogar (ADR-18)', () => {
  it('desde el catálogo se registra un producto que no existe y queda en la despensa', async () => {
    const { llamadas } = backendFalso({
      ...rutasCocina(),
      'POST /items-despensa/adicional': (cuerpo) => respuestaJson({ id: 50, ...(cuerpo as object) }, 201),
    })
    const { router } = montarApp(crearServicioFalso(LUISA).servicio, '/despensa/agregar')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByRole('searchbox', { name: 'Busca en el catálogo' }), 'Arepas')
    await usuario.click(screen.getByRole('link', { name: /Registra un producto propio/ }))
    expect(await screen.findByLabelText('Nombre del producto')).toHaveValue('Arepas')

    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByText('Elige cómo lo mides.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('radio', { name: 'Por unidades' }))
    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Arepas' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Registraste Arepas y quedó en tu despensa.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/despensa')
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toMatchObject({
      nombre: 'Arepas',
      categoria: 'OTROS',
      tipoCantidad: 'CONTABLE',
      unidadBase: 'unidad',
    })
  })

  it('muestra el error si ya existe un producto con ese nombre', async () => {
    backendFalso({
      ...rutasCocina(),
      'POST /items-despensa/adicional': () => respuestaJson({ status: 409, codigo: 'DESPENSA_PRODUCTO_EXISTE' }, 409),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/despensa/adicional?nombre=Lenteja')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('radio', { name: 'Por peso (arroz, harina…)' }))
    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))
    await usuario.click(await screen.findByRole('radio', { name: 'Medio' }))
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText(/Ya hay un producto llamado así/)).toBeInTheDocument()
  })
})
