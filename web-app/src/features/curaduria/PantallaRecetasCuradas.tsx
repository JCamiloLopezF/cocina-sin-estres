import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { enPesos } from '../despensa/formato'
import { listarRecetasCuradas, mensajeCuraduria, type RecetaCuradaResumen } from './apiCuraduria'
import { comidasDe } from './recetaCurada'
import './curaduria.css'

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; recetas: RecetaCuradaResumen[] } | { tipo: 'error'; error: unknown }

/** Recetas del catálogo para el curador (RNF-28): buscar, crear y abrir para editar. */
export function PantallaRecetasCuradas() {
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [buscar, setBuscar] = useState('')
  const [verDesactivadas, setVerDesactivadas] = useState(false)
  const [aviso] = useState<string | undefined>(() => (ubicacion.state as { aviso?: string } | null)?.aviso)

  useEffect(() => {
    if ((ubicacion.state as { aviso?: string } | null)?.aviso) {
      navegar(ubicacion.pathname, { replace: true, state: null })
    }
  }, [ubicacion, navegar])

  useEffect(() => {
    const control = new AbortController()
    listarRecetasCuradas(control.signal)
      .then((recetas) => setEstado({ tipo: 'listo', recetas }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) setEstado({ tipo: 'error', error })
      })
    return () => control.abort()
  }, [version])

  const recetas = estado.tipo === 'listo' ? estado.recetas : []
  const texto = normalizar(buscar.trim())
  const desactivadas = recetas.filter((r) => !r.activa).length
  const visibles = recetas.filter((r) => (verDesactivadas || r.activa) && (!texto || normalizar(r.nombre).includes(texto)))

  return (
    <>
      <EncabezadoPagina
        titulo="Recetas"
        detalle="Crea y edita las recetas que ven todas las usuarias y que usa «Armar mi semana»."
        detalleAbajo
        volver={{ ruta: '/curaduria', etiqueta: 'Volver al catálogo' }}
      />
      {aviso && <Alerta tono="exito">{aviso}</Alerta>}

      <div className="curaduria__barra">
        <Link className="boton boton--secundario" to="/curaduria/recetas/videos">
          Videos y etiquetas
        </Link>
        <Link className="boton boton--primario" to="/curaduria/recetas/nueva">
          + Nueva receta
        </Link>
      </div>

      {estado.tipo === 'cargando' && <Cargando texto="Cargando las recetas…" />}
      {estado.tipo === 'error' && (
        <Alerta tono="error">
          {mensajeCuraduria(estado.error)}{' '}
          <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
            Reintentar
          </Boton>
        </Alerta>
      )}

      {estado.tipo === 'listo' && (
        <>
          <div className="curaduria__filtros curaduria__filtros--recetas">
            <label className="solo-lectores" htmlFor="buscar-receta-curada">
              Buscar receta
            </label>
            <input
              id="buscar-receta-curada"
              className="campo__control"
              type="search"
              placeholder="Buscar receta"
              value={buscar}
              onChange={(e) => setBuscar(e.target.value)}
            />
            {desactivadas > 0 && (
              <label className="casilla">
                <input type="checkbox" checked={verDesactivadas} onChange={(e) => setVerDesactivadas(e.target.checked)} />
                Ver desactivadas ({desactivadas})
              </label>
            )}
          </div>
          <p className="curaduria__conteo">
            {visibles.length} {visibles.length === 1 ? 'receta' : 'recetas'}
          </p>
          <ul className="filas">
            {visibles.map((receta) => (
              <li key={receta.id}>
                <Link className="fila" to={`/curaduria/recetas/${receta.id}`}>
                  <span className="fila__texto">
                    <span className="fila__nombre">
                      {receta.nombre}
                      {!receta.activa && ' · desactivada'}
                    </span>
                    <span className="fila__detalle">
                      {comidasDe(receta)} · {receta.tiempoMinutos} min · {receta.ingredientes} ingredientes
                      {receta.tieneVideo ? ' · con video' : ''}
                    </span>
                  </span>
                  <span className="curaduria__precio">
                    {enPesos(receta.costo)}
                    <span className="fila__detalle"> {receta.porciones} porc.</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}

function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}
