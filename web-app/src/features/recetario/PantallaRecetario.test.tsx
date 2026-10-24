import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import {
  ARROZ_CON_LECHE,
  ARROZ_CON_POLLO,
  PERFIL_CON_ALERGIA_GRAVE,
  PERFIL_CON_ALERGIAS,
  resumenReceta,
  rutasCocina,
  SOPA,
} from '../../prueba/cocinaFalsa'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'

function tarjeta(nombre: string) {
  return screen.getByRole('link', { name: new RegExp(nombre) })
}

describe('Recetario (pantallas 21 a 24)', () => {
  it('muestra costo por porción y lo que falta según la despensa', async () => {
    backendFalso(rutasCocina())
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas')

    expect(await screen.findByRole('link', { name: /Sopa de lentejas/ })).toBeInTheDocument()
    const sopa = within(tarjeta('Sopa de lentejas'))
    expect(sopa.getByText('25 min · 4 ingredientes')).toBeInTheDocument()
    expect(sopa.getByText(/900 \/ porción/)).toBeInTheDocument()
    expect(sopa.getByText('falta 1')).toBeInTheDocument()
    expect(sopa.getByText('preparación previa')).toBeInTheDocument()
    const arroz = within(tarjeta('Arroz con pollo'))
    expect(arroz.getByText(/3\.100 \/ porción/)).toBeInTheDocument()
    expect(arroz.getByText('faltan 2')).toBeInTheDocument()
    expect(screen.getByText('2 recetas')).toBeInTheDocument()
  })

  it('busca por ingrediente y filtra con los atajos y el panel de filtros', async () => {
    backendFalso(rutasCocina())
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas')
    const usuario = userEvent.setup()
    await screen.findByRole('link', { name: /Sopa de lentejas/ })

    await usuario.type(screen.getByRole('searchbox', { name: 'Buscar receta o ingrediente' }), 'zanahória')
    expect(screen.queryByRole('link', { name: /Arroz con pollo/ })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Sopa de lentejas/ })).toBeInTheDocument()
    await usuario.clear(screen.getByRole('searchbox', { name: 'Buscar receta o ingrediente' }))

    await usuario.click(screen.getByRole('button', { name: 'Rápidas' }))
    expect(screen.queryByRole('link', { name: /Arroz con pollo/ })).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Con lo que tengo' }))
    expect(screen.getByText(/Ninguna receta coincide/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Todas' }))

    await usuario.click(screen.getByRole('button', { name: 'Filtros' }))
    await usuario.click(screen.getByRole('radio', { name: '≤ $3k' }))
    expect(screen.getByRole('button', { name: 'Ver 1 receta' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Limpiar' }))
    await usuario.click(screen.getByRole('radio', { name: '≤ 20 min' }))
    expect(screen.getByRole('button', { name: 'Ver 0 recetas' })).toBeInTheDocument()
  })

  it('el detalle marca qué hay en la despensa y escala las porciones', async () => {
    backendFalso(rutasCocina())
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas/10')
    const usuario = userEvent.setup()

    expect(await screen.findByRole('heading', { level: 1, name: 'Sopa de lentejas' })).toBeInTheDocument()
    const ingredientes = screen.getAllByRole('listitem').filter((li) => li.closest('.receta__ingredientes'))
    expect(ingredientes.map((li) => li.textContent)).toEqual([
      'Lenteja · 250 gen despensa',
      'Cebolla · 1 unidaden despensa',
      'Zanahoria · 1 unidadfalta',
      'Sal · 5 gopcional',
    ])
    expect(screen.getByText(/Costo estimado para 4 porciones/)).toHaveTextContent('3.600')

    await usuario.click(screen.getByRole('button', { name: 'Una porción más' }))
    await usuario.click(screen.getByRole('button', { name: 'Una porción más' }))
    await usuario.click(screen.getByRole('button', { name: 'Una porción más' }))
    await usuario.click(screen.getByRole('button', { name: 'Una porción más' }))
    // 8 porciones: el doble de lenteja (500 g) todavía alcanza; la cebolla (2) también.
    expect(screen.getByText('Lenteja · 500 g')).toBeInTheDocument()
    expect(screen.getByText(/Costo estimado para 8 porciones/)).toHaveTextContent('7.200')
  })

  it('agrega la receta al plan: crea la semana si no existe y asigna día y comida', async () => {
    const { llamadas } = backendFalso({
      ...rutasCocina(),
      'POST /planes-semanales': (cuerpo) =>
        respuestaJson({ id: 7, semanaInicio: (cuerpo as { semanaInicio: string }).semanaInicio, comidas: [] }),
      'PUT /planes-semanales/7/comidas': (cuerpo) => respuestaJson({ id: 70, planSemanalId: 7, ...(cuerpo as object), preparada: false }),
    })
    const { router } = montarApp(crearServicioFalso(LUISA).servicio, '/recetas/10')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('link', { name: 'Agregar al plan' }))
    expect(await screen.findByRole('heading', { level: 1, name: '¿Cuándo la preparas?' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByText('Elige el día.')).toBeInTheDocument()
    expect(screen.getByText('Elige la comida.')).toBeInTheDocument()

    await usuario.click(screen.getByRole('radio', { name: 'Martes' }))
    await usuario.click(screen.getByRole('radio', { name: 'Cena' }))
    expect(screen.getByText('El martes está libre.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))

    await screen.findByText('Sopa de lentejas quedó para el martes · cena.')
    expect(router.state.location.pathname).toBe('/plan/dia/2')
    const { semanaInicio } = llamadas.find((l) => l.metodo === 'POST')!.cuerpo as { semanaInicio: string }
    expect(new Date(`${semanaInicio}T12:00`).getDay()).toBe(1)
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ dia: 2, tipoComida: 'CENA', recetaId: 10 })
  })

  it('avisa si el hueco ya tiene receta y la reemplaza', async () => {
    backendFalso({
      ...rutasCocina(),
      'GET /planes-semanales': () =>
        respuestaJson({
          id: 7,
          semanaInicio: '2026-10-19',
          comidas: [{ id: 70, planSemanalId: 7, dia: 2, tipoComida: 'CENA', recetaId: 11, preparada: false }],
        }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas/10/agregar?dia=2&comida=CENA')

    expect(
      await screen.findByText('El martes ya tienes cena: Arroz con pollo. Si continúas, la cambiamos por esta receta.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cambiar la receta' })).toBeInTheDocument()
  })

  describe('alergias por persona', () => {
    function backendConAlergias(perfil: object = PERFIL_CON_ALERGIAS, extra: Record<string, () => Response> = {}) {
      return backendFalso({
        ...rutasCocina(),
        'GET /recetas': () => respuestaJson([ARROZ_CON_POLLO, SOPA, ARROZ_CON_LECHE].map(resumenReceta)),
        'GET /recetas/12': () => respuestaJson(ARROZ_CON_LECHE),
        'GET /perfil/hogar': () => respuestaJson(perfil),
        ...extra,
      })
    }

    it('una alergia que no es grave no oculta la receta: la marca "con ajuste"', async () => {
      backendConAlergias()
      montarApp(crearServicioFalso(LUISA).servicio, '/recetas')

      const tarjeta = await screen.findByRole('link', { name: /Arroz con leche/ })
      expect(within(tarjeta).getByText('con ajuste para Samuel')).toBeInTheDocument()
      expect(screen.queryByText(/Ocultamos/)).not.toBeInTheDocument()
    })

    it('una alergia grave oculta la receta por defecto y deja verla marcada', async () => {
      backendConAlergias(PERFIL_CON_ALERGIA_GRAVE)
      montarApp(crearServicioFalso(LUISA).servicio, '/recetas')
      const usuario = userEvent.setup()

      expect(await screen.findByRole('link', { name: /Sopa de lentejas/ })).toBeInTheDocument()
      expect(screen.queryByRole('link', { name: /Arroz con leche/ })).not.toBeInTheDocument()
      expect(screen.getByText(/Ocultamos 1 receta que no es apta por una alergia grave/)).toBeInTheDocument()

      await usuario.click(screen.getByRole('button', { name: 'Ver todas' }))
      expect(within(screen.getByRole('link', { name: /Arroz con leche/ })).getByText('no apta para Samuel')).toBeInTheDocument()
    })

    it('el detalle dice cómo adaptarla para cada persona y a quién pide precaución', async () => {
      backendConAlergias(PERFIL_CON_ALERGIAS, {
        'GET /adaptaciones': () =>
          respuestaJson([
            {
              recetaId: 12,
              estado: 'PLATO_APARTE',
              cambios: [
                {
                  tipo: 'PLATO_APARTE',
                  persona: 'Samuel',
                  alergias: ['lactosa'],
                  enDespensa: false,
                  recetaAparteId: 10,
                  recetaAparte: 'Sopa de lentejas',
                },
              ],
            },
          ]),
      })
      montarApp(crearServicioFalso(LUISA).servicio, '/recetas/12')

      const ajustes = await screen.findByRole('region', { name: 'Ajustes por alergias' })
      expect(ajustes).toHaveTextContent(
        'Para Samuel (lactosa): no se puede cambiar lo principal del plato; prepárale aparte Sopa de lentejas.',
      )
      expect(within(ajustes).getByRole('link', { name: 'Ver Sopa de lentejas' })).toHaveAttribute('href', '/recetas/10')
      expect(screen.getByText(/Precaución para Luisa \(diabetes\)/)).toBeInTheDocument()
      expect(screen.getByText('Leche · 1 kg')).toBeInTheDocument()
      expect(screen.getByText('· lactosa')).toBeInTheDocument()
    })

    it('si no se pueden consultar los ajustes, igual avisa con lo que sabe del perfil', async () => {
      backendConAlergias(PERFIL_CON_ALERGIA_GRAVE)
      montarApp(crearServicioFalso(LUISA).servicio, '/recetas/12')

      expect(await screen.findByText('No apta para Samuel: lleva lactosa.')).toBeInTheDocument()
    })
  })

  it('el detalle ofrece cocinar paso a paso y el video solo carga YouTube al tocarlo', async () => {
    backendFalso({
      ...rutasCocina(),
      'GET /recetas/10': () => respuestaJson({ ...SOPA, video: { youtubeId: 'dQw4w9WgXcQ', inicioSeg: 30, finSeg: 90 } }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas/10')
    const usuario = userEvent.setup()

    const portada = await screen.findByRole('button', { name: 'Ver el video de Sopa de lentejas (01:00)' })
    expect(document.querySelector('iframe')).toBeNull()
    await usuario.click(screen.getByRole('button', { name: 'Una porción más' }))
    expect(screen.getByRole('link', { name: /Cocinar ahora, paso a paso/ })).toHaveAttribute(
      'href',
      '/recetas/10/cocinar?porciones=5',
    )
    expect(screen.getByRole('link', { name: 'Ponerla hoy en el plan' })).toBeInTheDocument()

    await usuario.click(portada)
    const video = screen.getByTitle('Video de Sopa de lentejas')
    expect(video.getAttribute('src')).toMatch(
      /^https:\/\/www\.youtube-nocookie\.com\/embed\/dQw4w9WgXcQ\?start=30&end=90&autoplay=1/,
    )
  })
})
