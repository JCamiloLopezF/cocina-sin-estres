import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, respuestaJson } from '../../prueba/servicioFalso'
import type { ProductoCurado } from './apiCuraduria'

const CURADORA = { uid: 'c', correo: 'curadora@correo.com', nombre: null }
const LENTEJA: ProductoCurado = {
  id: 3,
  nombre: 'Lenteja',
  categoria: 'GRANOS',
  tipoCantidad: 'GRANEL',
  unidadBase: 'g',
  unidadCompra: 'Libra',
  cantidadUnidadCompra: 500,
  precioUnidadCompra: 4200,
  capacidadReferencia: 1000,
  esBasico: true,
  activo: true,
}
const HUEVOS: ProductoCurado = {
  ...LENTEJA,
  id: 4,
  nombre: 'Huevos',
  categoria: 'LACTEOS',
  tipoCantidad: 'CONTABLE',
  unidadBase: 'unidad',
  unidadCompra: 'Cubeta',
  cantidadUnidadCompra: 30,
  precioUnidadCompra: 18000,
  capacidadReferencia: undefined,
}

function backend(extra = {}) {
  return backendFalso({
    'GET /yo': () => respuestaJson({ id: 2, correo: CURADORA.correo, rol: 'CURADOR', perfilCompleto: false }),
    'GET /curaduria/productos': () => respuestaJson([LENTEJA, HUEVOS]),
    'GET /curaduria/productos/3': () => respuestaJson(LENTEJA),
    'GET /curaduria/productos/3/reemplazos': () => respuestaJson([]),
    ...extra,
  })
}

describe('Catálogo del curador (pantallas 43 y 44)', () => {
  it('lista los productos con su precio y filtra por categoría', async () => {
    backend()
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria')

    expect(await screen.findByRole('link', { name: 'Editar Lenteja' })).toHaveTextContent(/4\.200.*libra/)
    expect(screen.getByText('2 de 2 productos activos')).toBeInTheDocument()
    await usuario.selectOptions(screen.getByLabelText('Categoría'), 'LACTEOS')
    expect(screen.queryByRole('link', { name: 'Editar Lenteja' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Editar Huevos' })).toBeInTheDocument()
  })

  it('crea un producto con precio de referencia y valida los datos', async () => {
    const { llamadas } = backend({
      'POST /curaduria/productos': (cuerpo: unknown) => respuestaJson({ id: 9, activo: true, ...(cuerpo as object) }, 201),
    })
    const usuario = userEvent.setup()
    const { router } = montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/productos/nuevo')

    await usuario.click(await screen.findByRole('button', { name: 'Guardar producto' }))
    expect(screen.getByText('Escribe el nombre.')).toBeInTheDocument()
    expect(screen.getByText('Escribe la presentación de compra.')).toBeInTheDocument()

    await usuario.type(screen.getByLabelText('Nombre'), 'Queso campesino')
    await usuario.selectOptions(screen.getByLabelText('Categoría'), 'LACTEOS')
    await usuario.click(screen.getByRole('radio', { name: 'Gramos' }))
    await usuario.type(screen.getByLabelText('Presentación de compra'), 'Libra')
    await usuario.type(screen.getByLabelText('Cuánto trae'), '500')
    await usuario.type(screen.getByLabelText('Precio'), '6500')
    await usuario.type(screen.getByLabelText('Vida útil (opcional)'), '14')
    await usuario.click(screen.getByRole('button', { name: 'Guardar producto' }))

    expect(await screen.findByText('Queso campesino quedó guardado en el catálogo.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/curaduria')
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({
      nombre: 'Queso campesino',
      categoria: 'LACTEOS',
      tipoCantidad: 'CONTABLE',
      unidadBase: 'g',
      unidadCompra: 'Libra',
      cantidadUnidadCompra: 500,
      precioUnidadCompra: 6500,
      esBasico: false,
      vidaUtilDias: 14,
    })
  })

  it('edita un producto a granel y lo puede quitar del catálogo', async () => {
    const { llamadas } = backend({
      'PUT /curaduria/productos/3': (cuerpo: unknown) => respuestaJson({ ...LENTEJA, ...(cuerpo as object) }),
      'DELETE /curaduria/productos/3': () => new Response(null, { status: 204 }),
    })
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/productos/3')

    const precio = await screen.findByLabelText('Precio')
    expect(screen.getByLabelText('Cantidad que equivale a «Lleno»')).toHaveValue('1000')
    await usuario.clear(precio)
    await usuario.type(precio, '4500')
    await usuario.click(screen.getByRole('button', { name: 'Guardar producto' }))
    expect(await screen.findByText('Lenteja quedó guardado en el catálogo.')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toMatchObject({ precioUnidadCompra: 4500, capacidadReferencia: 1000 })
  })

  it('quitar del catálogo pide confirmación', async () => {
    const { llamadas } = backend({ 'DELETE /curaduria/productos/3': () => new Response(null, { status: 204 }) })
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/productos/3')

    await usuario.click(await screen.findByRole('button', { name: 'Quitar del catálogo' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, quitarlo' }))
    expect(await screen.findByText('Lenteja ya no se ofrece en el catálogo.')).toBeInTheDocument()
    expect(llamadas.some((l) => l.metodo === 'DELETE')).toBe(true)
  })

  it('muestra el detalle de validación del backend', async () => {
    backend({
      'PUT /curaduria/productos/3': () =>
        respuestaJson({ status: 400, codigo: 'PRODUCTO_PRECIO_INVALIDO', detail: 'El precio de referencia no puede ser negativo.' }, 400),
    })
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/productos/3')
    await usuario.click(await screen.findByRole('button', { name: 'Guardar producto' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('El precio de referencia no puede ser negativo.')
  })

  it('corrige los reemplazos que propone la app para las alergias', async () => {
    const POLLO = { ...LENTEJA, id: 11, nombre: 'Pollo', categoria: 'PROTEINAS' }
    const ATUN = { ...LENTEJA, id: 13, nombre: 'Atún', categoria: 'PROTEINAS' }
    const CARNE = { ...LENTEJA, id: 12, nombre: 'Carne de res', categoria: 'PROTEINAS' }
    let reemplazos = [
      { reemplazoId: 11, nombre: 'Pollo', categoria: 'PROTEINAS', origen: 'SISTEMA', activo: true },
      { reemplazoId: 13, nombre: 'Atún', categoria: 'PROTEINAS', origen: 'SISTEMA', activo: true },
    ]
    const { llamadas } = backend({
      'GET /curaduria/productos': () => respuestaJson([HUEVOS, POLLO, ATUN, CARNE]),
      'GET /curaduria/productos/4': () => respuestaJson(HUEVOS),
      'GET /curaduria/productos/4/reemplazos': () => respuestaJson(reemplazos),
      'DELETE /curaduria/productos/4/reemplazos/13': () => {
        reemplazos = reemplazos.map((r) => (r.reemplazoId === 13 ? { ...r, origen: 'CURADOR', activo: false } : r))
        return respuestaJson(reemplazos)
      },
      'PUT /curaduria/productos/4/reemplazos/12': () => {
        reemplazos = [...reemplazos, { reemplazoId: 12, nombre: 'Carne de res', categoria: 'PROTEINAS', origen: 'CURADOR', activo: true }]
        return respuestaJson(reemplazos)
      },
    })
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/productos/4')

    const seccion = within(await screen.findByRole('region', { name: 'Reemplazos para alergias' }))
    const enUso = await seccion.findByRole('list', { name: 'En uso' })
    expect(enUso).toHaveTextContent('Pollo automático')

    await usuario.click(seccion.getByRole('button', { name: 'Quitar Atún' }))
    expect(await seccion.findByRole('list', { name: 'Quitados' })).toHaveTextContent('Atún')
    expect(seccion.getByRole('button', { name: 'Volver a usar Atún' })).toBeInTheDocument()

    await usuario.selectOptions(seccion.getByLabelText('Agregar un reemplazo'), 'Carne de res')
    await usuario.click(seccion.getByRole('button', { name: 'Agregar' }))
    expect(await seccion.findByText('agregado por curador')).toBeInTheDocument()
    expect(llamadas.filter((l) => l.ruta.includes('/reemplazos/')).map((l) => `${l.metodo} ${l.ruta}`)).toEqual([
      'DELETE /curaduria/productos/4/reemplazos/13',
      'PUT /curaduria/productos/4/reemplazos/12',
    ])
  })
})
