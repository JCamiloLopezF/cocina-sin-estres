# ADR-16: Tres roles con mínimo privilegio

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D4 · KAN-12, KAN-32 · Amplía ADR-06

## Decisión

El sistema tiene tres roles, `USUARIA`, `CURADOR` y `ADMIN`, guardados en `usuario.rol` (un rol por cuenta) y aplicados con Spring Security en el backend. Esto amplía el ADR-06, que definía dos.

## Contexto

La Definición describe tres actores; el ADR-06, dos. El RNF-24 exige que cada cambio del catálogo se asocie a un curador identificable, y el RNF-22 exige que los datos de salud solo los vea la cuenta propietaria.

| Capacidad | USUARIA | CURADOR | ADMIN |
| --- | --- | --- | --- |
| Perfil, despensa, plan y avisos de su propio hogar | Sí | No | No |
| Consultar el catálogo de productos y recetas | Sí | Sí | Sí |
| Crear y editar productos, precios, recetas y restricciones | No | Sí | No |
| Listar cuentas, cambiar roles y desactivar cuentas | No | No | Sí |
| Ver datos de salud de otros hogares | No | No | No |
| Rutas del backend | `/api/**` | `/api/curaduria/**` | `/api/admin/**` |

## Detalle

- Toda cuenta nueva nace como `USUARIA`. Solo un `ADMIN` cambia roles.
- El primer `ADMIN` se define con la variable de entorno `ADMIN_INICIAL_CORREO`: cuando esa persona inicia sesión por primera vez, su cuenta se crea como `ADMIN`. No se escribe en el código ni en las migraciones.
- Las cuentas `CURADOR` y `ADMIN` no tienen hogar ni pasan por el onboarding del perfil.

## Alternativas

Dos roles con el administrador editando el catálogo; el rol en los *custom claims* del token de Firebase.

## Justificación

Separar curaduría y administración cumple el RNF-24 y limita lo que cada cuenta puede dañar. El `ADMIN` no ve restricciones de salud, lo que protege el RNF-22.

## Consecuencias

- Las pruebas del RNF-23 (KAN-32) recorren la matriz completa: tres roles por cada grupo de rutas, esperando 403 donde corresponde.
- La interfaz oculta opciones por rol, pero eso no es control de acceso.
- `usuario.activo` permite al `ADMIN` desactivar una cuenta sin borrarla.
