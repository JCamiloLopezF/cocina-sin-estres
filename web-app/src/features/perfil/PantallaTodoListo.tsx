import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { pedir } from '../../api/cliente'
import { Olla } from '../../shared/componentes'
import { resumenDespensa } from '../despensa/apiDespensa'
import { enPesos } from '../despensa/formato'
import './perfil.css'

interface Resumen {
  personas?: number
  presupuesto?: string
  productos?: number
}

const CICLO = { SEMANAL: 'ingreso semanal', QUINCENAL: 'ingreso quincenal', MENSUAL: 'ingreso mensual' } as const

/** Pantalla 12: cierre del primer ingreso con el resumen del hogar. */
export function PantallaTodoListo() {
  const [resumen, setResumen] = useState<Resumen>({})

  useEffect(() => {
    const control = new AbortController()
    // Si algo falla, se muestra sin ese dato: no vale la pena un error en esta pantalla.
    pedir<{ personas: unknown[]; presupuestoMonto: number; periodicidadIngreso: keyof typeof CICLO }>('/perfil/hogar', {
      senal: control.signal,
    })
      .then((perfil) =>
        setResumen((r) => ({
          ...r,
          personas: perfil.personas.length,
          presupuesto: `${enPesos(perfil.presupuestoMonto)} · ${CICLO[perfil.periodicidadIngreso]}`,
        })),
      )
      .catch(() => undefined)
    resumenDespensa(control.signal)
      .then((d) => setResumen((r) => ({ ...r, productos: d.total })))
      .catch(() => undefined)
    return () => control.abort()
  }, [])

  return (
    <div className="listo">
      <div className="listo__ilustracion" aria-hidden="true">
        <Olla />
      </div>
      <h1 className="listo__titulo">Tu cocina ya está organizada</h1>
      <ul className="listo__datos">
        {resumen.personas !== undefined && (
          <li>
            {resumen.personas} {resumen.personas === 1 ? 'persona' : 'personas'} en casa
          </li>
        )}
        {resumen.presupuesto && <li>{resumen.presupuesto}</li>}
        {resumen.productos !== undefined && (
          <li>
            {resumen.productos} {resumen.productos === 1 ? 'producto' : 'productos'} en la despensa
          </li>
        )}
      </ul>
      <Link className="boton boton--primario boton--ancho" to="/plan">
        Armar mi primera semana
      </Link>
      <Link className="listo__inicio" to="/">
        Ir al inicio
      </Link>
    </div>
  )
}
