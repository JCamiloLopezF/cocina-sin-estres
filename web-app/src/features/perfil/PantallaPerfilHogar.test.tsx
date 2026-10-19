import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso, CUENTA_USUARIA } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'

describe('Perfil del hogar (asistente de 4 pasos)', () => {
  it('valida cada paso y guarda el perfil al final', async () => {
    let perfilCompleto = false
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson({ ...CUENTA_USUARIA, perfilCompleto }),
      'PUT /perfil/hogar': (cuerpo) => {
        perfilCompleto = true
        return respuestaJson({ id: 1, ...(cuerpo as object) })
      },
    })
    const { servicio } = crearServicioFalso(LUISA)
    const { router } = montarApp(servicio, '/perfil/hogar')
    const usuario = userEvent.setup()
    const continuar = () => usuario.click(screen.getByRole('button', { name: 'Continuar' }))

    // Paso 1: personas y edades
    expect(await screen.findByRole('heading', { level: 1, name: 'Tu hogar · paso 1 de 4' })).toBeInTheDocument()
    await continuar()
    expect(screen.getByText('Edad entre 0 y 120.')).toBeInTheDocument()
    const tu = () => within(screen.getByRole('group', { name: 'Tú' }))
    await usuario.type(tu().getByLabelText('Nombre (opcional)'), 'Luisa')
    await usuario.type(tu().getByLabelText('Edad'), '34')
    await usuario.click(screen.getByRole('button', { name: '+ Agregar persona' }))
    const persona2 = within(screen.getByRole('group', { name: 'Persona 2' }))
    await usuario.type(persona2.getByLabelText('Nombre (opcional)'), 'Samuel')
    await usuario.type(persona2.getByLabelText('Edad'), '9')
    await continuar()

    // Paso 2: alergias y condiciones (datos de salud, con autorización)
    expect(screen.getByRole('heading', { level: 1, name: 'Tu hogar · paso 2 de 4' })).toBeInTheDocument()
    await continuar()
    expect(screen.getByText('Marca las restricciones del hogar o indica que nadie tiene.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('checkbox', { name: 'Lactosa' }))
    await usuario.click(screen.getByRole('checkbox', { name: 'Hipertensión' }))
    await continuar()
    expect(screen.getByText('Para guardar datos de salud necesitamos tu autorización.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('checkbox', { name: /Autorizo guardar estos datos de salud/ }))
    await continuar()

    // Paso 3: presupuesto y ciclo
    expect(screen.getByRole('heading', { level: 1, name: 'Tu hogar · paso 3 de 4' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('radio', { name: 'Quincenal' }))
    await usuario.type(screen.getByLabelText('Presupuesto quincenal de alimentación'), '390000')
    await usuario.type(screen.getByLabelText('Primer día de ingreso'), '15')
    await usuario.type(screen.getByLabelText('Segundo día de ingreso'), '10')
    await continuar()
    expect(screen.getByText('Escribe el segundo día de pago, mayor que el primero.')).toBeInTheDocument()
    await usuario.clear(screen.getByLabelText('Segundo día de ingreso'))
    await usuario.type(screen.getByLabelText('Segundo día de ingreso'), '30')
    await continuar()

    // Paso 4: días sin tiempo
    expect(screen.getByRole('heading', { level: 1, name: 'Tu hogar · paso 4 de 4' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('checkbox', { name: 'Miércoles' }))
    await usuario.click(screen.getByRole('checkbox', { name: 'Lunes' }))
    expect(llamadas.some((l) => l.metodo === 'PUT')).toBe(false)
    await usuario.click(screen.getByRole('button', { name: 'Guardar perfil' }))

    // Primera vez: sigue la carga rápida de la despensa (wireframe 11).
    expect(await screen.findByRole('heading', { level: 1, name: 'Carga rápida' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/despensa/carga-rapida')
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({
      presupuestoMonto: 390000,
      periodicidadIngreso: 'QUINCENAL',
      diaPago1: 15,
      diaPago2: 30,
      diasSinTiempo: [1, 3],
      restricciones: ['lactosa', 'hipertension'],
      sinRestricciones: false,
      otrasCondiciones: null,
      consentimientoDatos: true,
      personas: [
        { nombre: 'Luisa', edad: 34 },
        { nombre: 'Samuel', edad: 9 },
      ],
    })
  })

  it('"Atrás" vuelve al paso anterior sin perder lo escrito', async () => {
    backendFalso({ 'GET /yo': () => respuestaJson(CUENTA_USUARIA) })
    const { servicio } = crearServicioFalso(LUISA)
    montarApp(servicio, '/perfil/hogar')
    const usuario = userEvent.setup()

    const tu = within(await screen.findByRole('group', { name: 'Tú' }))
    await usuario.type(tu.getByLabelText('Edad'), '34')
    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))
    await usuario.click(screen.getByRole('checkbox', { name: /Nadie en casa/ }))
    await usuario.click(screen.getByRole('button', { name: 'Atrás' }))

    expect(screen.getByRole('heading', { level: 1, name: 'Tu hogar · paso 1 de 4' })).toBeInTheDocument()
    expect(within(screen.getByRole('group', { name: 'Tú' })).getByLabelText('Edad')).toHaveValue('34')
  })

  it('carga el perfil guardado para editarlo', async () => {
    backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /perfil/hogar': () =>
        respuestaJson({
          id: 1,
          presupuestoMonto: 800000,
          periodicidadIngreso: 'MENSUAL',
          diaPago1: 30,
          diasSinTiempo: [],
          restricciones: [],
          sinRestricciones: true,
          personas: [
            { id: 1, nombre: 'Luisa', edad: 34 },
            { id: 2, nombre: null, edad: 6 },
          ],
        }),
    })
    const { servicio } = crearServicioFalso(LUISA)
    montarApp(servicio, '/perfil/hogar')
    const usuario = userEvent.setup()

    expect(within(await screen.findByRole('group', { name: 'Tú' })).getByLabelText('Nombre (opcional)')).toHaveValue('Luisa')
    expect(within(screen.getByRole('group', { name: 'Persona 2' })).getByLabelText('Nombre (opcional)')).toHaveValue('')
    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByRole('checkbox', { name: /Nadie en casa/ })).toBeChecked()
    await usuario.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(screen.getByDisplayValue('800000')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'Mensual' })).toBeChecked()
  })
})
