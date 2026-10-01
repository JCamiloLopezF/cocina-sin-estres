# Aplicación web

Interfaz de **Cocina sin estrés** en React con Vite. Es una SPA pensada para navegador móvil de gama baja (RNF-08: menos de 150 MB de memoria).

> **Estado:** el esqueleto del proyecto llega con KAN-8. Este README se completa en ese PR.

## Requisitos

- Node.js 22 (LTS) y npm

## Configuración

Copia `.env.example` a `.env.local` y completa los valores. La configuración web de Firebase no es secreta, pero igual se maneja por variables de entorno para separar desarrollo y producción.

## Ejecución

```bash
npm install
npm run dev      # http://localhost:5173
npm test         # Vitest
npm run build    # compilación de producción en dist/
```

## Estructura

```
src/
├── features/
│   ├── auth/           Registro, inicio de sesión y recuperación
│   ├── perfil/         Onboarding y perfil del hogar
│   ├── despensa/       Listado, alta y edición de productos
│   ├── recetario/      Catálogo, filtros y detalle de receta
│   ├── planificador/   Semana, cruce, costo y lista de compras
│   ├── avisos/         Centro de avisos
│   └── admin/          Curaduría del catálogo y gestión de cuentas
├── shared/             Componentes reutilizables (RNF-30)
├── api/                Cliente HTTP que adjunta el token de Firebase
└── config/             Firebase y variables de entorno
```

Las pantallas de referencia están en `docs/curso/` (Interfaces de Usuario, pantallas 13 a 26).
