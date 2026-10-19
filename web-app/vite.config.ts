/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // El backend permite este origen en CORS (cocina.cors.origenes).
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/prueba/configuracion.ts'],
    css: false,
    restoreMocks: true,
    // Los recorridos completos (perfil en 4 pasos, plan) hacen muchos clics: en el CI
    // pueden pasar de los 5 s por defecto sin que nada esté mal.
    testTimeout: 20000,
  },
})
