import { Link } from 'react-router'
import { ErrorApi } from '../../api/cliente'
import { mensajeDeError } from '../../api/mensajesError'
import { useConsulta } from '../../api/useConsulta'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { useCuenta, useSesion } from '../auth/contextoSesion'
import './perfil.css'

/** Respuesta de `GET /api/perfil/hogar` (PerfilHogarResponse); solo lo que se muestra aquí. */
interface PerfilHogar {
  presupuestoMonto: number | null
  periodicidadIngreso: 'SEMANAL' | 'QUINCENAL' | 'MENSUAL' | null
  personas: { id: number; nombre: string | null; edad: number | null }[]
}

const PERIODICIDAD: Record<string, string> = {
  SEMANAL: 'cada semana',
  QUINCENAL: 'cada quincena',
  MENSUAL: 'cada mes',
}

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

/** Cuenta, resumen del hogar y salida. El formulario del perfil del hogar llega con su propia tarea. */
export function PantallaPerfil() {
  const { estado, salir } = useSesion()
  const cuenta = useCuenta()
  const nombre = estado.tipo === 'con-sesion' ? estado.sesion.nombre : null
  const hogar = useConsulta<PerfilHogar>('/perfil/hogar')

  return (
    <>
      <EncabezadoPagina titulo="Perfil" detalle={nombre ?? undefined} />

      <section className="perfil__bloque" aria-labelledby="titulo-cuenta">
        <h2 id="titulo-cuenta" className="perfil__subtitulo">
          Tu cuenta
        </h2>
        <dl className="perfil__datos">
          <dt>Correo</dt>
          <dd>{cuenta.correo}</dd>
        </dl>
      </section>

      <section className="perfil__bloque" aria-labelledby="titulo-hogar">
        <h2 id="titulo-hogar" className="perfil__subtitulo">
          Tu hogar
        </h2>
        {hogar.estado.tipo === 'cargando' && <Cargando texto="Cargando tu hogar…" />}
        {hogar.estado.tipo === 'error' &&
          (hogar.estado.error instanceof ErrorApi && hogar.estado.error.status === 404 ? (
            <EstadoVacio
              titulo="Todavía no has llenado el perfil del hogar"
              texto="Con las personas que comen en casa, sus restricciones y tu presupuesto podremos sugerirte recetas que te sirvan."
              acciones={
                <Link className="boton boton--primario boton--ancho" to="/perfil/hogar">
                  Completar perfil del hogar
                </Link>
              }
            />
          ) : (
            <Alerta tono="error">
              {mensajeDeError(hogar.estado.error)}{' '}
              <Boton variante="texto" onClick={hogar.recargar}>
                Reintentar
              </Boton>
            </Alerta>
          ))}
        {hogar.estado.tipo === 'listo' && (
          <dl className="perfil__datos">
            <dt>Personas</dt>
            <dd>
              {describirPersonas(hogar.estado.datos.personas)}
            </dd>
            <dt>Presupuesto</dt>
            <dd>
              {hogar.estado.datos.presupuestoMonto == null
                ? 'Sin registrar'
                : `${pesos.format(hogar.estado.datos.presupuestoMonto)} ${
                    PERIODICIDAD[hogar.estado.datos.periodicidadIngreso ?? ''] ?? ''
                  }`.trim()}
            </dd>
          </dl>
        )}
        {hogar.estado.tipo === 'listo' && (
          <Link className="boton boton--secundario boton--ancho" to="/perfil/hogar">
            Editar perfil del hogar
          </Link>
        )}
      </section>

      <Boton variante="secundario" ancho onClick={() => void salir()}>
        Cerrar sesión
      </Boton>
    </>
  )
}

function describirPersonas(personas: PerfilHogar['personas']): string {
  if (personas.length === 0) {
    return 'Sin registrar'
  }
  const nombres = personas.map((p) => p.nombre).filter(Boolean)
  const cuantas = `${personas.length} ${personas.length === 1 ? 'persona' : 'personas'}`
  return nombres.length ? `${cuantas}: ${nombres.join(', ')}` : cuantas
}
