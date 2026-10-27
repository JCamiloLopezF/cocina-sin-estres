# Medición con Lighthouse (26 de octubre de 2026)

Lighthouse 12 en modo **celular** (pantalla de teléfono, CPU 4 veces más lenta y red 4G lenta simuladas), sobre la compilación de producción del frontend (`npm run build` y `vite preview`). Las pantallas que necesitan sesión dependen del backend; aquí se midieron las públicas, que son las que carga primero quien llega a la aplicación.

| Pantalla | Rendimiento | Accesibilidad | Buenas prácticas | Primer contenido | Mayor contenido (LCP) | Bloqueo total | Cambio de diseño |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `/bienvenida` | 98 | 100 | 100 | 1,8 s | 1,8 s | 40 ms | 0 |
| `/privacidad` | 97 | 100 | 100 | 2,1 s | 2,1 s | 20 ms | 0,008 |

- **RNF-09** (carga en menos de 3 s con 4G simulada): la pantalla queda lista en unos 2 s.
- **RNF-12** (WCAG 2.1 AA): Lighthouse encontró un `aria-label` en un `<p>` del nombre de la marca (no permitido sin rol); se corrigió y la accesibilidad pasó de 94 a 100.
- El paquete de JavaScript pesa 581 kB (175 kB comprimido), casi todo React y Firebase. Cargar las pantallas por partes (`import()` por ruta) es la siguiente mejora si hiciera falta bajar el tiempo en celulares de gama baja.

Para repetirla:

```
cd web-app
VITE_API_URL=http://localhost:8080/api VITE_AUTH_MODO=dev npm run build
npx vite preview --port 4173
npx lighthouse http://localhost:4173/bienvenida --only-categories=performance,accessibility,best-practices --view
```
