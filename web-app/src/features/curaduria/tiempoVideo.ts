/** Tiempos del fragmento de video que escribe el curador. */

/** "1:30", "90" o "0:05" → segundos. `undefined` si no se entiende. */
export function segundosDe(texto: string): number | undefined {
  const limpio = texto.trim()
  const partes = /^(?:(\d{1,3}):)?(\d{1,4})$/.exec(limpio)
  if (!partes) return undefined
  const minutos = partes[1] ? Number(partes[1]) : 0
  const segundos = Number(partes[2])
  if (partes[1] && segundos >= 60) return undefined
  return minutos * 60 + segundos
}

/** Si el enlace trae `t=95` o `t=1m35s` (compartir desde un minuto), se usa como inicio. */
export function inicioDelEnlace(enlace: string): number | undefined {
  const t = /[?&#](?:t|start)=(?:(\d+)m)?(\d+)s?(?:&|$)/.exec(enlace)
  return t ? Number(t[1] ?? 0) * 60 + Number(t[2]) : undefined
}
