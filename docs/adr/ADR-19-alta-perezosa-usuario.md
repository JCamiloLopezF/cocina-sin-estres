# ADR-19: Alta perezosa del usuario local

- **Estado:** Abierta
- **Fecha:** 2026-09-30
- **Origen:** D7 · KAN-10, KAN-8

## Decisión

El registro ocurre solo en Firebase, desde el frontend. El backend crea el usuario en MySQL la primera vez que recibe una petición con un token válido de ese `firebase_uid` (buscar o crear). No hay endpoint de registro en el backend.

## Contexto

El ADR-05 no definía qué pasa si el registro en Firebase funciona pero el alta en MySQL falla. Con dos altas separadas, ese caso deja una cuenta que existe en Firebase y no en la base.

## Detalle

- `FirebaseTokenFilter` verifica el token y entrega el `uid` y el correo a `UsuarioActual`, que busca el usuario y lo crea si no existe.
- `usuario.firebase_uid` es `UNIQUE`. Si dos peticiones simultáneas intentan crearlo, la segunda captura el error de duplicado y vuelve a leer.
- Las cuentas nuevas nacen con rol `USUARIA` y `perfil_completo = false`; el frontend las lleva al onboarding.
- En el perfil `dev`, el filtro acepta el encabezado `X-Dev-User` para simular un usuario sin Firebase. Ese bean solo existe en `dev` y nunca se carga en producción.

## Alternativas

Endpoint de registro que cree el usuario en ambos lados; función de Firebase que notifique al backend.

## Justificación

No hay estado intermedio que reparar: si el alta falla, se reintenta sola en la siguiente petición. El modo `dev` permite trabajar antes de tener Firebase configurado.

## Consecuencias

- La métrica "perfiles completos / registros iniciados" se calcula sobre `usuario` y `perfil_completo`.
- Al eliminar una cuenta se borra primero en MySQL y después en Firebase con el Admin SDK.
- Ya está implementado en KAN-8 (esqueleto del backend).
