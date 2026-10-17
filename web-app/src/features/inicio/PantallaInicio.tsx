import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { EncabezadoPagina } from '../../shared/componentes'
import { useCuenta, useSesion } from '../auth/contextoSesion'
import { resumenDespensa } from '../despensa/apiDespensa'
import type { ResumenDespensa } from '../despensa/tipos'
import './inicio.css'

/** Pantalla 13: saludo y accesos a lo principal del día. */
export function PantallaInicio() {
  const { estado } = useSesion()
  const cuenta = useCuenta()
  const nombre = estado.tipo === 'con-sesion' ? estado.sesion.nombre : null
  const [resumen, setResumen] = useState<ResumenDespensa>()
  useEffect(() => {
    if (!cuenta.perfilCompleto) {
      return
    }
    const control = new AbortController()
    // Si falla, la tarjeta se queda con el texto general: no vale la pena un error aquí.
    resumenDespensa(control.signal).then(setResumen).catch(() => undefined)
    return () => control.abort()
  }, [cuenta.perfilCompleto])

  const [hoy] = useState(() =>
    new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date()),
  )

  return (
    <>
      <EncabezadoPagina previo={hoy.charAt(0).toUpperCase() + hoy.slice(1)} titulo={nombre ? `Hola, ${nombre}` : 'Hola'} />
      <div className="inicio">
        {!cuenta.perfilCompleto && (
          <Link className="aviso-inicio" to="/perfil/hogar">
            <span className="aviso-inicio__titulo">Falta el perfil de tu hogar</span>
            <span>Cuéntanos quiénes comen en casa y tu presupuesto · Completar perfil</span>
          </Link>
        )}
        {resumen && resumen.porVencer > 0 && (
          <Link className="aviso-inicio" to="/despensa?filtro=POR_VENCER">
            <span className="aviso-inicio__titulo">
              {resumen.porVencer === 1 ? '1 producto está por vencer' : `${resumen.porVencer} productos están por vencer`}
            </span>
            <span>Ver en la despensa</span>
          </Link>
        )}
        <ul className="mosaicos">
          <li>
            <Link className="mosaico" to="/despensa">
              <span className="rotulo">Despensa</span>
              <span className="mosaico__cifra">{resumen ? resumen.total : '—'}</span>
              <span className="mosaico__detalle">{textoDespensa(resumen)}</span>
            </Link>
          </li>
          <li>
            <Link className="mosaico" to="/plan">
              <span className="rotulo">Semana</span>
              <span className="mosaico__cifra mosaico__cifra--texto">Plan</span>
              <span className="mosaico__detalle">Qué cocinar y cuánto cuesta</span>
            </Link>
          </li>
          <li className="mosaicos__ancho">
            <Link className="mosaico mosaico--fila" to="/recetas">
              <span>
                <span className="rotulo">Recetario</span>
                <span className="mosaico__detalle">Recetas sencillas con precios de referencia</span>
              </span>
              <span className="fila__flecha" aria-hidden="true">
                ›
              </span>
            </Link>
          </li>
        </ul>
      </div>
    </>
  )
}

function textoDespensa(resumen?: ResumenDespensa): string {
  if (!resumen) {
    return 'Lo que tienes en casa'
  }
  if (resumen.total === 0) {
    return 'Todavía está vacía'
  }
  if (resumen.bajoMinimo > 0) {
    return `${resumen.bajoMinimo} bajo el mínimo`
  }
  return resumen.total === 1 ? 'producto en casa' : 'productos en casa'
}
