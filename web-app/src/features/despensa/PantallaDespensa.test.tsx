import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso, CUENTA_USUARIA } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'
import type { ItemDespensa, ProductoDisponible } from './tipos'

const HUEVOS: ItemDespensa = {
  id: 5,
  productoId: 1,
  nombre: 'Huevos',
  categoria: 'PROTEINAS',
  tipoCantidad: 'CONTABLE',
  unidadBase: 'unidad',
  cantidad: 6,
  esEstimada: false,
  cantidadMinima: 4,
  fechaVencimiento: '2026-10-18',
  diasParaVencer: 2,
  estadoVencimiento: 'POR_VENCER',
  bajoMinimo: false,
  ubicacion: 'Nevera',
}

const ARROZ_EN_CASA: ItemDespensa = {
  id: 6,
  productoId: 2,
  nombre: 'Arroz',
  categoria: 'GRANOS',
  tipoCantidad: 'GRANEL',
  unidadBase: 'g',
  cantidad: 250,
  esEstimada: true,
  nivel: 'POCO',
  nivelMinimo: 'POCO',
  cantidadMinima: 250,
  estadoVencimiento: 'SIN_FECHA',
  bajoMinimo: true,
}

const CATALOGO: ProductoDisponible[] = [
  { id: 2, nombre: 'Arroz', categoria: 'GRANOS', tipoCantidad: 'GRANEL', unidadBase: 'g', unidadCompra: 'Bolsa', precioUnidadCompra: 4500 },
  { id: 1, nombre: 'Huevos', categoria: 'PROTEINAS', tipoCantidad: 'CONTABLE', unidadBase: 'unidad', itemDespensaId: 5 },
]

function entrar(ruta: string) {
  const { servicio } = crearServicioFalso(LUISA)
  return montarApp(servicio, ruta)
}

describe('Despensa', () => {
  it('lista los productos por categoría y filtra por vencer', async () => {
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /items-despensa': (_c, url) =>
        respuestaJson(url.searchParams.get('filtro') === 'POR_VENCER' ? [HUEVOS] : [ARROZ_EN_CASA, HUEVOS]),
    })
    entrar('/despensa')
    const usuario = userEvent.setup()

    expect(await screen.findByRole('heading', { level: 2, name: 'Arroz, pasta y granos' })).toBeInTheDocument()
    expect(screen.getByText('2 productos')).toBeInTheDocument()
    expect(screen.getByText('Nivel: poco · bajo el mínimo')).toBeInTheDocument()
    expect(screen.getByText('6 unidades · vence en 2 días')).toBeInTheDocument()
    expect(screen.getByText('usar ya')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Por vencer' }))

    expect(screen.getByRole('button', { name: 'Por vencer' })).toHaveAttribute('aria-pressed', 'true')
    await waitFor(() => expect(screen.getByText('1 resultado')).toBeInTheDocument())
    expect(llamadas.at(-1)?.consulta.get('filtro')).toBe('POR_VENCER')
  })

  it('vacía, invita a agregar el primer producto', async () => {
    backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /items-despensa': () => respuestaJson([]),
    })
    entrar('/despensa')

    expect(await screen.findByRole('heading', { name: 'Aún no hay nada aquí' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Agregar el primer producto' })).toHaveAttribute('href', '/despensa/agregar')
  })

  it('sin perfil del hogar, lleva a completarlo', async () => {
    backendFalso({
      'GET /yo': () => respuestaJson({ ...CUENTA_USUARIA, perfilCompleto: false }),
      'GET /items-despensa': () =>
        respuestaJson({ status: 409, codigo: 'DESPENSA_HOGAR_REQUERIDO' }, 409, 'application/problem+json'),
    })
    entrar('/despensa')

    expect(await screen.findByRole('link', { name: 'Completar perfil del hogar' })).toHaveAttribute(
      'href',
      '/perfil/hogar',
    )
  })

  it('agrega un producto a granel eligiendo el nivel', async () => {
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /items-despensa/catalogo': () => respuestaJson(CATALOGO),
      'POST /items-despensa': (cuerpo) => respuestaJson({ ...ARROZ_EN_CASA, ...(cuerpo as object) }, 201),
      'GET /items-despensa': () => respuestaJson([ARROZ_EN_CASA]),
    })
    entrar('/despensa/agregar')
    const usuario = userEvent.setup()

    const resultados = await screen.findByRole('list', { name: 'Resultados del catálogo' })
    // Lo que ya está en la despensa se edita, no se agrega otra vez.
    expect(within(resultados).getByRole('link', { name: /Huevos/ })).toHaveAttribute('href', '/despensa/5')

    await usuario.click(within(resultados).getByRole('button', { name: /Arroz/ }))
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(await screen.findByText('Elige cuánto te queda.')).toBeInTheDocument()

    await usuario.click(within(screen.getByRole('group', { name: '¿Cuánto te queda?' })).getByRole('radio', { name: 'Medio' }))
    await usuario.type(screen.getByLabelText('Ubicación (opcional)'), 'Alacena')
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Agregaste Arroz a tu despensa.')).toBeInTheDocument()
    const post = llamadas.find((l) => l.metodo === 'POST')
    expect(post?.cuerpo).toEqual({ productoId: 2, nivel: 'MEDIO', ubicacion: 'Alacena' })
  })

  it('un producto contable se cuenta con los botones de más y menos', async () => {
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /items-despensa/catalogo': () =>
        respuestaJson([{ id: 9, nombre: 'Leche', categoria: 'LACTEOS', tipoCantidad: 'CONTABLE', unidadBase: 'unidad' }]),
      'POST /items-despensa': () => respuestaJson(HUEVOS, 201),
      'GET /items-despensa': () => respuestaJson([HUEVOS]),
    })
    entrar('/despensa/agregar')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: /Leche/ }))
    await usuario.click(screen.getByRole('button', { name: 'Agregar uno' }))
    await usuario.click(screen.getByRole('button', { name: 'Agregar uno' }))
    await usuario.type(screen.getByLabelText(/Nivel mínimo/), '1')
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }))

    await screen.findByText('Agregaste Leche a tu despensa.')
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({ productoId: 9, cantidad: 3, cantidadMinima: 1 })
  })

  it('eliminar pide confirmación y se puede deshacer (RNF-13)', async () => {
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /items-despensa/5': () => respuestaJson(HUEVOS),
      'DELETE /items-despensa/5': () => new Response(null, { status: 204 }),
      'GET /items-despensa': () => respuestaJson([]),
      'POST /items-despensa': () => respuestaJson(HUEVOS, 201),
    })
    entrar('/despensa/5')
    const usuario = userEvent.setup()

    expect(await screen.findByRole('heading', { level: 1, name: 'Huevos' })).toBeInTheDocument()
    expect(screen.getByText('18/10/2026')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Eliminar de mi despensa' }))
    expect(screen.getByRole('alertdialog')).toHaveTextContent('¿Eliminar Huevos de tu despensa?')
    expect(llamadas.some((l) => l.metodo === 'DELETE')).toBe(false)

    await usuario.click(screen.getByRole('button', { name: 'Sí, eliminar' }))
    expect(await screen.findByText('Eliminaste Huevos de tu despensa.')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Deshacer' }))
    expect(await screen.findByText('Huevos volvió a tu despensa.')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({
      productoId: 1,
      cantidad: 6,
      cantidadMinima: 4,
      fechaVencimiento: '2026-10-18',
      ubicacion: 'Nevera',
    })
  })

  it('editar guarda la cantidad nueva', async () => {
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /items-despensa/5': () => respuestaJson(HUEVOS),
      'PUT /items-despensa/5': (cuerpo) => respuestaJson({ ...HUEVOS, ...(cuerpo as object) }),
    })
    entrar('/despensa/5')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Editar cantidad' }))
    await usuario.click(screen.getByRole('button', { name: 'Quitar uno' }))
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByText('5 unidades')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toMatchObject({ cantidad: 5, cantidadMinima: 4 })
  })
})
