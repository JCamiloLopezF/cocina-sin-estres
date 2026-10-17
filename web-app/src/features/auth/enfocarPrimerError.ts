/**
 * Lleva el foco al primer campo con error para que quien usa teclado o lector de
 * pantalla sepa qué corregir (WCAG 3.3.1). Espera un cuadro para que React pinte el error.
 */
export function enfocarPrimerError(formulario: HTMLFormElement | null): void {
  requestAnimationFrame(() => {
    formulario?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
  })
}
