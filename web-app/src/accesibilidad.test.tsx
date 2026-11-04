import { screen } from '@testing-library/react'
import axe from 'axe-core'
import { describe, expect, it } from 'vitest'
import { backendFalso } from './prueba/backendFalso'
import { rutasCocina } from './prueba/cocinaFalsa'
import { montarApp } from './prueba/montar'
import { crearServicioFalso, LUISA } from './prueba/servicioFalso'

/**
 * Pruebas automáticas de accesibilidad (auditoría externa del 4 de noviembre: «no se cuenta
 * con pruebas automatizadas de accesibilidad»). axe-core revisa cada pantalla con las reglas
 * de WCAG 2.1 A y AA (RNF-12): nombres de botones y campos, etiquetas, roles ARIA, orden de
 * encabezados, idioma… El contraste de color no se puede medir en jsdom (no pinta), así que
 * esa regla se apaga aquí y se revisa con Lighthouse en el navegador.
 */
async function violaciones(): Promise<string[]> {
  const resultado = await axe.run(document.body, {
    runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] },
    rules: { 'color-contrast': { enabled: false } },
  })
  return resultado.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).join(', ')})`)
}

describe('Accesibilidad automática (axe-core, WCAG 2.1 AA)', () => {
  it.each([
    ['Entrar', '/entrar', 'Entrar'],
    ['Crear cuenta', '/registro', 'Crear cuenta'],
    ['Recuperar contraseña', '/recuperar', 'Recuperar contraseña'],
  ])('%s, sin sesión', async (_nombre, ruta, titulo) => {
    backendFalso({})
    montarApp(crearServicioFalso(null).servicio, ruta)

    await screen.findByRole('heading', { level: 1, name: new RegExp(titulo, 'i') })

    expect(await violaciones()).toEqual([])
  })

  it.each([
    ['Recetario', '/recetas', 'Recetario'],
    ['Detalle de una receta', '/recetas/11', 'Arroz con pollo'],
    ['Despensa', '/despensa', 'Despensa'],
    ['Perfil', '/perfil', 'Perfil'],
  ])('%s, con sesión de usuaria', async (_nombre, ruta, titulo) => {
    backendFalso(rutasCocina())
    montarApp(crearServicioFalso(LUISA).servicio, ruta)

    await screen.findByRole('heading', { level: 1, name: new RegExp(titulo, 'i') })
    // Que termine de cargar lo que pide la pantalla.
    await new Promise((resolver) => setTimeout(resolver, 50))

    expect(await violaciones()).toEqual([])
  })
})
