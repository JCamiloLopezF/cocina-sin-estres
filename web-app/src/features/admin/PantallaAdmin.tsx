import { useEffect, useState } from 'react'
import { ErrorApi, pedir } from '../../api/cliente'
import { mensajeDeError } from '../../api/mensajesError'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { useCuenta, type Rol } from '../auth/contextoSesion'
import { NavAdmin } from './NavAdmin'
import './admin.css'

/** `GET /api/admin/cuentas` (CuentaResponse). */
export interface CuentaAdmin {
  id: number
  correo: string
  rol: Rol
  activo: boolean
  perfilCompleto: boolean
}

const ROLES: { valor: Rol; texto: string }[] = [
  { valor: 'USUARIA', texto: 'Usuaria' },
  { valor: 'CURADOR', texto: 'Curador de contenido' },
  { valor: 'ADMIN', texto: 'Administrador' },
]

const MENSAJES: Record<string, string> = {
  ADMIN_CUENTA_PROPIA: 'No puedes cambiar tu propio rol ni desactivar tu cuenta. Pídeselo a otro administrador.',
  ADMIN_ROL_INVALIDO: 'Elige un rol de la lista.',
  RECURSO_NO_ENCONTRADO: 'Esa cuenta ya no existe. Recarga la lista.',
}

function mensajeAdmin(error: unknown): string {
  return error instanceof ErrorApi && MENSAJES[error.codigo] ? MENSAJES[error.codigo] : mensajeDeError(error)
}

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; cuentas: CuentaAdmin[] } | { tipo: 'error'; error: unknown }

/**
 * Gestión de cuentas y roles (proceso 1, rol ADMIN): ver las cuentas, asignar el rol
 * de curador o administrador y desactivar o reactivar cuentas. El backend vuelve a
 * revisar el rol en cada petición (RNF-23).
 */
export function PantallaAdmin() {
  const yo = useCuenta()
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [buscar, setBuscar] = useState('')
  const [ocupada, setOcupada] = useState<number>()
  const [aviso, setAviso] = useState<{ tono: 'exito' | 'error'; texto: string }>()

  useEffect(() => {
    const control = new AbortController()
    pedir<CuentaAdmin[]>('/admin/cuentas', { senal: control.signal })
      .then((cuentas) => setEstado({ tipo: 'listo', cuentas }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [version])

  async function cambiar(cuenta: CuentaAdmin, cambio: { rol?: Rol; activo?: boolean }, texto: string) {
    setOcupada(cuenta.id)
    setAviso(undefined)
    try {
      const nueva = await pedir<CuentaAdmin>(`/admin/cuentas/${cuenta.id}`, { metodo: 'PUT', cuerpo: cambio })
      setEstado((e) => (e.tipo === 'listo' ? { tipo: 'listo', cuentas: e.cuentas.map((c) => (c.id === nueva.id ? nueva : c)) } : e))
      setAviso({ tono: 'exito', texto })
    } catch (error) {
      setAviso({ tono: 'error', texto: mensajeAdmin(error) })
    } finally {
      setOcupada(undefined)
    }
  }

  if (estado.tipo !== 'listo') {
    return (
      <>
        <NavAdmin />
        <EncabezadoPagina titulo="Cuentas" />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Cargando cuentas…" />
        ) : (
          <Alerta tono="error">
            {mensajeAdmin(estado.error)}{' '}
            <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
              Reintentar
            </Boton>
          </Alerta>
        )}
      </>
    )
  }

  const texto = buscar.trim().toLowerCase()
  const visibles = estado.cuentas.filter((c) => c.correo.toLowerCase().includes(texto))
  const contar = (rol: Rol) => estado.cuentas.filter((c) => c.rol === rol && c.activo).length

  return (
    <>
      <NavAdmin />
      <EncabezadoPagina
        titulo="Cuentas"
        detalle={[
          cantidad(contar('USUARIA'), 'usuaria', 'usuarias'),
          cantidad(contar('CURADOR'), 'curador', 'curadores'),
          cantidad(contar('ADMIN'), 'administrador', 'administradores'),
        ].join(' · ')}
        detalleAbajo
      />
      {aviso && <Alerta tono={aviso.tono}>{aviso.texto}</Alerta>}

      <div className="admin__busqueda">
        <label className="solo-lectores" htmlFor="buscar-cuenta">
          Buscar por correo
        </label>
        <input
          id="buscar-cuenta"
          className="campo__control"
          type="search"
          placeholder="Buscar por correo"
          value={buscar}
          onChange={(e) => setBuscar(e.target.value)}
        />
      </div>

      <ul className="cuentas">
        {visibles.map((cuenta) => {
          const propia = cuenta.id === yo.id
          return (
            <li key={cuenta.id} className={cuenta.activo ? 'cuenta' : 'cuenta cuenta--inactiva'}>
              <div className="cuenta__cabecera">
                <strong className="cuenta__correo">{cuenta.correo}</strong>
                <span className="cuenta__estado">
                  {propia ? 'tu cuenta' : cuenta.activo ? 'activa' : 'desactivada'}
                </span>
              </div>
              <div className="cuenta__acciones">
                <label className="solo-lectores" htmlFor={`rol-${cuenta.id}`}>
                  Rol de {cuenta.correo}
                </label>
                <select
                  id={`rol-${cuenta.id}`}
                  className="campo__control campo__control--lista"
                  value={cuenta.rol}
                  disabled={propia || ocupada === cuenta.id}
                  onChange={(e) => {
                    const rol = e.target.value as Rol
                    const nombre = ROLES.find((r) => r.valor === rol)?.texto.toLowerCase()
                    void cambiar(cuenta, { rol }, `${cuenta.correo} ahora es ${nombre}.`)
                  }}
                >
                  {ROLES.map((r) => (
                    <option key={r.valor} value={r.valor}>
                      {r.texto}
                    </option>
                  ))}
                </select>
                {!propia && (
                  <Boton
                    variante="secundario"
                    cargando={ocupada === cuenta.id}
                    aria-label={`${cuenta.activo ? 'Desactivar' : 'Reactivar'} ${cuenta.correo}`}
                    onClick={() =>
                      void cambiar(
                        cuenta,
                        { activo: !cuenta.activo },
                        cuenta.activo ? `Desactivamos ${cuenta.correo}: ya no puede entrar.` : `${cuenta.correo} puede volver a entrar.`,
                      )
                    }
                  >
                    {cuenta.activo ? 'Desactivar' : 'Reactivar'}
                  </Boton>
                )}
              </div>
            </li>
          )
        })}
      </ul>
      {visibles.length === 0 && <p className="admin__vacio">Ninguna cuenta coincide con "{buscar}".</p>}
      <p className="admin__nota">
        Una cuenta nueva aparece aquí la primera vez que la persona entra. Para crear un curador, pídele que se registre y
        cámbiale el rol.
      </p>
    </>
  )
}

/** "1 curador", "3 curadores". */
function cantidad(n: number, singular: string, plural: string): string {
  return `${n} ${n === 1 ? singular : plural}`
}
