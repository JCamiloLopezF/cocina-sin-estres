import { useState } from 'react'
import { Link } from 'react-router'
import { Alerta, EncabezadoPagina } from '../../shared/componentes'
import { useCuenta, useSesion } from '../auth/contextoSesion'
import './inicio.css'

/** Pantalla 13: saludo y accesos a lo principal del día. */
export function PantallaInicio() {
  const { estado } = useSesion()
  const cuenta = useCuenta()
  const nombre = estado.tipo === 'con-sesion' ? estado.sesion.nombre : null
  const [hoy] = useState(() =>
    new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date()),
  )

  return (
    <>
      <EncabezadoPagina titulo={nombre ? `Hola, ${nombre}` : 'Hola'} detalle={hoy.charAt(0).toUpperCase() + hoy.slice(1)} />
      {!cuenta.perfilCompleto && (
        <Alerta tono="info" titulo="Falta el perfil de tu hogar">
          Cuéntanos quiénes comen en casa y cuál es tu presupuesto para sugerirte recetas que te sirvan.{' '}
          <Link to="/perfil">Completar perfil</Link>
        </Alerta>
      )}
      <ul className="accesos">
        <li>
          <Link className="acceso-rapido" to="/despensa">
            <span className="acceso-rapido__titulo">Mi despensa</span>
            <span className="acceso-rapido__texto">Lo que tienes en casa y lo que está por vencer.</span>
          </Link>
        </li>
        <li>
          <Link className="acceso-rapido" to="/plan">
            <span className="acceso-rapido__titulo">Plan de la semana</span>
            <span className="acceso-rapido__texto">Qué vas a cocinar cada día y cuánto cuesta.</span>
          </Link>
        </li>
        <li>
          <Link className="acceso-rapido" to="/recetas">
            <span className="acceso-rapido__titulo">Recetario</span>
            <span className="acceso-rapido__texto">Recetas sencillas con precios de referencia.</span>
          </Link>
        </li>
      </ul>
    </>
  )
}
