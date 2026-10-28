import { pedir } from '../../api/cliente'
import type { DatosItem, Filtro, ItemDespensa, ProductoDisponible, ResumenDespensa } from './tipos'

/** Llamadas a `/api/items-despensa`. Todas actúan sobre el hogar de la usuaria que entra. */

function consulta(parametros: Record<string, string | undefined>): string {
  const limpios = Object.entries(parametros).filter((par): par is [string, string] => Boolean(par[1]?.trim()))
  return limpios.length ? `?${new URLSearchParams(limpios).toString()}` : ''
}

export function listarDespensa(filtro: Filtro, buscar: string, senal?: AbortSignal) {
  return pedir<ItemDespensa[]>(
    `/items-despensa${consulta({ filtro: filtro === 'TODO' ? undefined : filtro, buscar })}`,
    { senal, respaldo: true },
  )
}

export function resumenDespensa(senal?: AbortSignal) {
  return pedir<ResumenDespensa>('/items-despensa/resumen', { senal })
}

export function obtenerItem(id: number, senal?: AbortSignal) {
  return pedir<ItemDespensa>(`/items-despensa/${id}`, { senal })
}

export function catalogoDisponible(buscar: string, senal?: AbortSignal) {
  return pedir<ProductoDisponible[]>(`/items-despensa/catalogo${consulta({ buscar })}`, { senal, respaldo: true })
}

export function agregarItem(productoId: number, datos: DatosItem) {
  return pedir<ItemDespensa>('/items-despensa', { metodo: 'POST', cuerpo: { productoId, ...datos } })
}

export function actualizarItem(id: number, datos: DatosItem) {
  return pedir<ItemDespensa>(`/items-despensa/${id}`, { metodo: 'PUT', cuerpo: datos })
}

export function eliminarItem(id: number) {
  return pedir<void>(`/items-despensa/${id}`, { metodo: 'DELETE' })
}

/** Los datos editables de un ítem ya guardado, para volver a crearlo (deshacer) o editarlo. */
export function datosDe(item: ItemDespensa): DatosItem {
  return item.tipoCantidad === 'GRANEL'
    ? {
        nivel: item.nivel,
        nivelMinimo: item.nivelMinimo,
        fechaVencimiento: item.fechaVencimiento,
        ubicacion: item.ubicacion,
      }
    : {
        cantidad: item.cantidad,
        cantidadMinima: item.cantidadMinima,
        fechaVencimiento: item.fechaVencimiento,
        ubicacion: item.ubicacion,
      }
}
