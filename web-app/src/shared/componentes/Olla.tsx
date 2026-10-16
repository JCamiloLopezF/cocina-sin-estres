/** Ícono de la marca: una olla con vapor. Decorativo, por eso va oculto a lectores. */
export function Olla({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 64 64" aria-hidden="true" focusable="false">
      <rect width="64" height="64" rx="14" fill="var(--color-primario)" />
      <path d="M14 30h36v6a14 14 0 0 1-14 14h-8a14 14 0 0 1-14-14z" fill="var(--color-mazorca)" />
      <path d="M10 30h44" stroke="var(--color-mazorca)" strokeWidth="4" strokeLinecap="round" />
      <path
        d="M26 22c0-3 4-3 4-6M34 22c0-3 4-3 4-6"
        stroke="#fff"
        strokeWidth="3"
        strokeLinecap="round"
        fill="none"
      />
    </svg>
  )
}
