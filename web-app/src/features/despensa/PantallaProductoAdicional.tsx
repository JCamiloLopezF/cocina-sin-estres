import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { pedir } from '../../api/cliente'
import { Boton, CampoTexto, EncabezadoPagina, GrupoOpciones } from '../../shared/componentes'
import { nombreCategoria } from './formato'
import { FormularioItem } from './FormularioItem'
import type { AvisoDespensa } from './PantallaDespensa'
import type { DatosItem, ItemDespensa, TipoCantidad, UnidadBase } from './tipos'
import './despensa.css'

type Medida = 'UNIDADES' | 'GRAMOS' | 'MILILITROS'

const MEDIDAS: Record<Medida, { tipoCantidad: TipoCantidad; unidadBase: UnidadBase }> = {
  UNIDADES: { tipoCantidad: 'CONTABLE', unidadBase: 'unidad' },
  GRAMOS: { tipoCantidad: 'GRANEL', unidadBase: 'g' },
  MILILITROS: { tipoCantidad: 'GRANEL', unidadBase: 'ml' },
}

const CATEGORIAS = ['OTROS', 'GRANOS', 'PROTEINAS', 'LACTEOS', 'VERDURAS', 'FRUTAS', 'ACEITES', 'CONDIMENTOS']

/**
 * Registrar un producto que no está en el catálogo (proceso 2, ADR-18). Solo lo ve este
 * hogar y no tiene precio de referencia. Primero se nombra y se dice cómo se mide;
 * después se registra cuánto hay con el mismo formulario de siempre (pantallas 17 y 18).
 */
export function PantallaProductoAdicional() {
  const navegar = useNavigate()
  const [parametros] = useSearchParams()
  const [nombre, setNombre] = useState(parametros.get('nombre') ?? '')
  const [medida, setMedida] = useState<Medida>()
  const [categoria, setCategoria] = useState('OTROS')
  const [errores, setErrores] = useState<{ nombre?: string; medida?: string }>({})
  const [listo, setListo] = useState(false)

  function continuar(evento: FormEvent) {
    evento.preventDefault()
    const nuevos = {
      nombre: nombre.trim() ? (nombre.trim().length > 120 ? 'Usa máximo 120 caracteres.' : undefined) : 'Escribe el nombre del producto.',
      medida: medida ? undefined : 'Elige cómo lo mides.',
    }
    setErrores(nuevos)
    if (!nuevos.nombre && !nuevos.medida) {
      setListo(true)
    }
  }

  if (listo && medida) {
    const producto = { nombre: nombre.trim(), ...MEDIDAS[medida] }
    return (
      <>
        <EncabezadoPagina titulo={producto.nombre} detalle="Producto propio de tu hogar" detalleAbajo />
        <FormularioItem
          producto={producto}
          textoEnviar="Guardar"
          alCancelar={() => setListo(false)}
          alEnviar={async (datos: DatosItem) => {
            await pedir<ItemDespensa>('/items-despensa/adicional', {
              metodo: 'POST',
              cuerpo: { nombre: producto.nombre, categoria, ...MEDIDAS[medida], ...datos },
            })
            const aviso: AvisoDespensa = { texto: `Registraste ${producto.nombre} y quedó en tu despensa.` }
            navegar('/despensa', { state: { aviso } })
          }}
        />
      </>
    )
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Producto propio"
        detalle={<Link to="/despensa/agregar">Cerrar</Link>}
      />
      <p className="carga__intro">Para lo que no encuentras en el catálogo. Solo lo verá tu hogar.</p>
      <form className="formulario-item" onSubmit={continuar} noValidate>
        <CampoTexto
          etiqueta="Nombre del producto"
          name="nombre"
          autoComplete="off"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          error={errores.nombre}
        />
        <GrupoOpciones<Medida>
          leyenda="¿Cómo lo mides?"
          opciones={[
            { valor: 'UNIDADES', texto: 'Por unidades' },
            { valor: 'GRAMOS', texto: 'A granel (gramos)' },
            { valor: 'MILILITROS', texto: 'A granel (líquido)' },
          ]}
          valor={medida}
          alCambiar={setMedida}
          error={errores.medida}
        />
        <div className="campo">
          <label className="campo__etiqueta" htmlFor="categoria-adicional">
            Categoría
          </label>
          <select
            id="categoria-adicional"
            className="campo__control campo__control--lista"
            value={categoria}
            onChange={(e) => setCategoria(e.target.value)}
          >
            {CATEGORIAS.map((c) => (
              <option key={c} value={c}>
                {nombreCategoria(c)}
              </option>
            ))}
          </select>
        </div>
        <Boton type="submit" ancho>
          Continuar
        </Boton>
      </form>
    </>
  )
}
