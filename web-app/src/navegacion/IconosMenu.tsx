/** Íconos del menú inferior. Decorativos: el texto de cada pestaña ya dice qué es. */

const trazo = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
}

export function IconoInicio() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" {...trazo}>
      <path d="M3 11 12 4l9 7" />
      <path d="M5 10v10h14V10" />
    </svg>
  )
}

export function IconoDespensa() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" {...trazo}>
      <rect x="4" y="3" width="16" height="18" rx="2" />
      <path d="M4 12h16M9 7.5h6M9 16.5h6" />
    </svg>
  )
}

export function IconoPlan() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" {...trazo}>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10h18M8 3v4M16 3v4" />
    </svg>
  )
}

export function IconoRecetas() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" {...trazo}>
      <path d="M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3z" />
      <path d="M5 17a3 3 0 0 1 3-3h11" />
    </svg>
  )
}

export function IconoPerfil() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" {...trazo}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </svg>
  )
}
