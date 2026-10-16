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
  personas: { id: number; nombre: string; edad: number | null }[]
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
              {hogar.estado.datos.personas.length === 0
                ? 'Sin registrar'
                : hogar.estado.datos.personas.map((p) => p.nombre).join(', ')}
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
      </section>

      <Boton variante="secundario" ancho onClick={() => void salir()}>
        Cerrar sesión
      </Boton>
    </>
  )
}
