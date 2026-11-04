/**
 * Tamaño de letra y alto contraste (recomendación final de la auditoría externa del 4 de
 * noviembre). Es una preferencia de cada navegador: se guarda en localStorage y se aplica
 * como atributo en <html>, así que todas las pantallas (también las del curador y del
 * administrador) la respetan sin tocar sus estilos. Los tamaños de la app van en rem, así
 * que basta con cambiar el tamaño base.
 */

export type TamanoLetra = 'normal' | 'grande' | 'muy-grande'

export interface PreferenciasVisuales {
  letra: TamanoLetra
  altoContraste: boolean
}

const CLAVE = 'cocina.preferencias-visuales'
const PREDETERMINADAS: PreferenciasVisuales = { letra: 'normal', altoContraste: false }
const TAMANOS: TamanoLetra[] = ['normal', 'grande', 'muy-grande']

export function leerPreferencias(): PreferenciasVisuales {
  try {
    const guardadas = JSON.parse(localStorage.getItem(CLAVE) ?? 'null') as Partial<PreferenciasVisuales> | null
    return {
      letra: TAMANOS.includes(guardadas?.letra as TamanoLetra) ? (guardadas?.letra as TamanoLetra) : 'normal',
      altoContraste: guardadas?.altoContraste === true,
    }
  } catch {
    return PREDETERMINADAS
  }
}

/** Pone los atributos en <html>; el CSS de base.css hace el resto. */
export function aplicarPreferencias(preferencias: PreferenciasVisuales, raiz: HTMLElement = document.documentElement): void {
  if (preferencias.letra === 'normal') delete raiz.dataset.letra
  else raiz.dataset.letra = preferencias.letra
  if (preferencias.altoContraste) raiz.dataset.contraste = 'alto'
  else delete raiz.dataset.contraste
}

export function guardarPreferencias(preferencias: PreferenciasVisuales): void {
  aplicarPreferencias(preferencias)
  try {
    localStorage.setItem(CLAVE, JSON.stringify(preferencias))
  } catch {
    // Sin almacenamiento (modo privado): vale mientras la página siga abierta.
  }
}
