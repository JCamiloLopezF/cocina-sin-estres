import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach } from 'vitest'

// Las pantallas del plan esperan varias respuestas seguidas (plan, recetas, despensa,
// costo). En las máquinas del CI eso puede tardar más del segundo que da por defecto
// findBy*/waitFor, y la prueba fallaba de vez en cuando sin que hubiera un error real.
configure({ asyncUtilTimeout: 5000 })

afterEach(() => {
  cleanup()
  window.localStorage.clear()
})
