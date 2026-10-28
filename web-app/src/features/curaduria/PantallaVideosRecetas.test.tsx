import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, respuestaJson } from '../../prueba/servicioFalso'
import type { RecetaConVideo } from './apiCuraduria'
import { inicioDelEnlace, segundosDe } from './tiempoVideo'

const CURADORA = { uid: 'c', correo: 'curadora@correo.com', nombre: null }
const SOPA: RecetaConVideo = {
  id: 10,
  nombre: 'Sopa de lentejas',
  tiempoMinutos: 10,
  maximoSegundos: 300,
  platoFuerte: true,
  gustaANinos: false,
}
const ARROZ: RecetaConVideo = {
  id: 11,
  nombre: 'Arroz con pollo',
  tiempoMinutos: 50,
  maximoSegundos: 1500,
  youtubeId: 'dQw4w9WgXcQ',
  inicioSeg: 30,
  finSeg: 150,
  platoFuerte: true,
  gustaANinos: true,
}

function backend(extra = {}) {
  return backendFalso({
    'GET /yo': () => respuestaJson({ id: 2, correo: CURADORA.correo, rol: 'CURADOR', perfilCompleto: false }),
    'GET /curaduria/productos': () => respuestaJson([]),
    'GET /curaduria/recetas': () => respuestaJson([SOPA, ARROZ]),
    'GET /curaduria/recetas/catalogo': () => respuestaJson([]),
    ...extra,
  })
}

describe('Recetas del curador: video y etiquetas', () => {
  it('entiende los tiempos como minutos:segundos y el inicio que trae el enlace', () => {
    expect(segundosDe('1:30')).toBe(90)
    expect(segundosDe('01:05')).toBe(65)
    expect(segundosDe('90')).toBe(90)
    expect(segundosDe('1:75')).toBeUndefined()
    expect(segundosDe('uno')).toBeUndefined()
    expect(inicioDelEnlace('https://youtu.be/dQw4w9WgXcQ?t=95')).toBe(95)
    expect(inicioDelEnlace('https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1m35s')).toBe(95)
    expect(inicioDelEnlace('https://www.youtube.com/watch?v=dQw4w9WgXcQ')).toBeUndefined()
  })

  it('se llega desde el catálogo y muestra qué recetas tienen video', async () => {
    backend()
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria')

    await usuario.click(await screen.findByRole('link', { name: 'Recetas' }))
    await usuario.click(await screen.findByRole('link', { name: 'Videos y etiquetas' }))
    expect(await screen.findByText('1 de 2 recetas tienen video')).toBeInTheDocument()
    expect(screen.getByText('Sin video · máximo 05:00')).toBeInTheDocument()
    expect(screen.getByText('Video de 02:00 (desde 00:30)')).toBeInTheDocument()
  })

  it('no deja guardar un video de más de la mitad del tiempo de la receta', async () => {
    const { llamadas } = backend({
      'PUT /curaduria/recetas/10/video': (cuerpo: unknown) =>
        respuestaJson({ ...SOPA, youtubeId: 'dQw4w9WgXcQ', ...(cuerpo as object) }),
    })
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/recetas/videos')

    await usuario.click(await screen.findByRole('button', { name: 'Agregar video de Sopa de lentejas' }))
    await usuario.type(screen.getByLabelText('Enlace de YouTube'), 'https://youtu.be/dQw4w9WgXcQ?t=60')
    expect(screen.getByLabelText('Desde')).toHaveValue('01:00')
    await usuario.type(screen.getByLabelText('Hasta'), '7:00')
    await usuario.click(screen.getByRole('button', { name: 'Guardar video' }))
    expect(screen.getByText('El video dura 06:00; para esta receta el máximo es 05:00.')).toBeInTheDocument()
    expect(llamadas.some((l) => l.metodo === 'PUT')).toBe(false)

    await usuario.clear(screen.getByLabelText('Hasta'))
    await usuario.type(screen.getByLabelText('Hasta'), '4:30')
    expect(screen.getByText(/Dura 03:30 de un máximo de 05:00/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Guardar video' }))

    expect(await screen.findByText('Guardamos el video de Sopa de lentejas.')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({
      enlace: 'https://youtu.be/dQw4w9WgXcQ?t=60',
      inicioSeg: 60,
      finSeg: 270,
    })
    expect(screen.getByText('2 de 2 recetas tienen video')).toBeInTheDocument()
  })

  it('muestra el error del servidor y permite quitar el video', async () => {
    backend({
      'PUT /curaduria/recetas/11/video': () =>
        respuestaJson({ status: 400, codigo: 'RECETA_VIDEO_ENLACE_INVALIDO', detail: 'Pega el enlace de un video de YouTube.' }, 400),
      'DELETE /curaduria/recetas/11/video': () => respuestaJson({ ...ARROZ, youtubeId: null, inicioSeg: null, finSeg: null }),
    })
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/recetas/videos')

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar video de Arroz con pollo' }))
    const enlace = screen.getByLabelText('Enlace de YouTube')
    expect(enlace).toHaveValue('https://youtu.be/dQw4w9WgXcQ')
    await usuario.clear(enlace)
    await usuario.type(enlace, 'https://vimeo.com/123')
    await usuario.click(screen.getByRole('button', { name: 'Guardar video' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Pega el enlace de un video de YouTube.')

    await usuario.click(screen.getByRole('button', { name: 'Quitar el video' }))
    expect(await screen.findByText('Quitamos el video de Arroz con pollo.')).toBeInTheDocument()
    const fila = screen.getByText('Arroz con pollo').closest('li')!
    expect(within(fila).getByText('Sin video · máximo 25:00')).toBeInTheDocument()
  })

  it('marca si la receta es plato fuerte y si les gusta a los niños (KAN-17)', async () => {
    const { llamadas } = backend({
      'PUT /curaduria/recetas/10/etiquetas': (cuerpo: unknown) => respuestaJson({ ...SOPA, ...(cuerpo as object) }),
    })
    const usuario = userEvent.setup()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/recetas/videos')

    const sopa = within((await screen.findByText('Sopa de lentejas')).closest('li')!)
    expect(sopa.getByRole('checkbox', { name: 'Plato fuerte (almuerzo o cena)' })).toBeChecked()
    await usuario.click(sopa.getByRole('checkbox', { name: 'Les gusta a los niños' }))

    expect(await screen.findByText('Sopa de lentejas queda entre las que les gustan a los niños.')).toBeInTheDocument()
    expect(sopa.getByRole('checkbox', { name: 'Les gusta a los niños' })).toBeChecked()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ gustaANinos: true })

    await usuario.click(sopa.getByRole('checkbox', { name: 'Plato fuerte (almuerzo o cena)' }))
    expect(await screen.findByText('Sopa de lentejas ya no se propone como almuerzo o cena.')).toBeInTheDocument()
  })
})
