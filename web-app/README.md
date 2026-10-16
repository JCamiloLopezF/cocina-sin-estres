# Aplicación web

Interfaz de **Cocina sin estrés** en React con Vite y TypeScript. Es una SPA pensada para navegador móvil de gama baja (RNF-08: menos de 150 MB de memoria).

## Requisitos

- Node.js 22 (LTS) y npm

## Configuración

Copia `.env.example` a `.env.local` y completa los valores. El `.env.local` nunca se sube (está en `.gitignore`).

| Variable | Para qué |
| --- | --- |
| `VITE_API_URL` | URL del backend terminada en `/api`, por ejemplo `http://localhost:8080/api` |
| `VITE_AUTH_MODO` | `firebase` (por defecto) o `dev` |
| `VITE_FIREBASE_*` | Configuración web de Firebase. Solo en modo `firebase` |

La configuración web de Firebase **no es secreta**: termina en el navegador de cualquier usuaria. Igual va por variables para separar desarrollo y producción. Lo que sí es secreto (la *service account*) vive solo en el backend.

Si falta una variable, la aplicación lo dice en pantalla en lugar de quedar en blanco.

### Dos formas de entrar

- **`firebase`**: registro, inicio de sesión y recuperación de contraseña con Firebase Auth. La contraseña va directo a Firebase; el backend solo recibe el token (RNF-21). La sesión se guarda en el navegador y no pide credenciales al volver (RNF-19). La recuperación manda un enlace al correo registrado (RNF-25).
- **`dev`**: sin Firebase, para trabajar con el backend en perfil `dev` (ADR-19). Escribes un nombre de prueba, por ejemplo `luisa`, y la aplicación lo manda como `X-Dev-User`; el backend crea la cuenta `luisa@dev.local`. En producción ese encabezado no hace nada.

Para probar el rol de curador o administrador en modo `dev`, cambia el rol de esa cuenta en tu base local: `UPDATE usuario SET rol = 'CURADOR' WHERE firebase_uid = 'luisa';`.

## Ejecución

```bash
npm install
npm run dev      # http://localhost:5173 (el origen que permite el CORS del backend)
npm test         # Vitest, una pasada
npm run lint     # oxlint
npm run build    # compilación de producción en dist/
```

El CI (`ci-frontend`) corre `npm ci`, `npm test` y `npm run build` en cada PR que toque `web-app/`.

## Estructura

```
src/
├── features/
│   ├── auth/           Entrar, crear cuenta, recuperar contraseña y la sesión
│   ├── inicio/         Pantalla de inicio de la usuaria
│   ├── perfil/         Cuenta y perfil del hogar
│   ├── despensa/       Despensa virtual
│   ├── recetario/      Catálogo de recetas
│   ├── planificador/   Plan semanal y lista de compras
│   ├── curaduria/      Catálogo para el rol CURADOR
│   └── admin/          Cuentas y roles para el rol ADMIN
├── navegacion/         Rutas, guardas por sesión y rol, y los marcos con el menú
├── shared/             Componentes reutilizables (RNF-30) y estilos base
├── api/                Cliente HTTP que adjunta el token y lee los errores del backend
├── config/             Variables de entorno
└── prueba/             Utilidades de las pruebas
```

### Navegación por rol (ADR-16)

| Rol | Entra a | Menú |
| --- | --- | --- |
| `USUARIA` | `/` | Inicio, Despensa, Plan, Recetas y Perfil |
| `CURADOR` | `/curaduria` | Sin menú de hogar |
| `ADMIN` | `/admin` | Sin menú de hogar |

Si alguien abre una pantalla que no es de su rol, la aplicación lo lleva a su propio inicio. Esto ordena la navegación, pero **no es la barrera de seguridad**: el backend revisa el rol en cada petición (RNF-23).

### Errores

`api/cliente.ts` convierte cualquier error en `ErrorApi` con el `codigo` estable del backend (CONTRIBUTING.md §7). El texto que ve la usuaria sale de `api/mensajesError.ts` según ese código, nunca del `detail` del servidor. Los errores de Firebase se traducen en `features/auth/mensajesAuth.ts`. Correo inexistente y contraseña equivocada dan el mismo mensaje, para no revelar qué correos tienen cuenta.

## Componentes compartidos

Antes de crear un componente en un módulo, revisa `shared/componentes/index.ts`: `Boton`, `CampoTexto`, `Alerta`, `EstadoVacio`, `EncabezadoPagina`, `Cargando`, `EnConstruccion`, `MarcaCocina` y `Olla`.

## Accesibilidad (RNF-12, WCAG 2.1 AA)

- Cada campo tiene etiqueta visible; la ayuda y el error quedan asociados con `aria-describedby`.
- Al enviar con errores, el foco va al primer campo con error.
- Los errores se anuncian con `role="alert"` y las confirmaciones con `role="status"`.
- Contraste AA en todos los textos, foco visible en todo lo que se puede tocar, objetivos táctiles de al menos 44 px y enlace "Saltar al contenido".
- La tipografía es Atkinson Hyperlegible Next, diseñada para lectura con baja visión, servida desde el propio sitio.

Las pantallas de referencia están en `docs/curso/` (Interfaces de Usuario, pantallas 13 a 26).
