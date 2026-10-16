# ADR-20: Cifrado de los datos de salud

- **Estado:** Aprobada
- **Fecha:** 2026-09-30
- **Origen:** D8 · KAN-14, KAN-32

## Decisión

Las restricciones de salud del hogar se guardan cifradas con AES-256-GCM mediante un `AttributeConverter` de JPA (`CifradoConverter`). La clave está en una variable de entorno (`CIFRADO_CLAVE_V1`) o en el gestor de secretos del proveedor de nube, nunca en el repositorio.

## Contexto

El RNF-22 exige cifrar alergias y condiciones y que solo las vea la cuenta propietaria, pero no había diseño.

## Detalle

- Campos cifrados: `hogar.restricciones` (lista de códigos en JSON) y `hogar.otras_condiciones` (texto libre opcional).
- Cada valor usa un vector de inicialización aleatorio de 96 bits y se guarda como `v1:` + Base64(IV + texto cifrado + etiqueta). El prefijo de versión permite rotar la clave más adelante.
- Todas las consultas de datos del hogar se filtran por el hogar de `UsuarioActual`; no existe un endpoint que reciba un `hogar_id` desde el cliente.
- Estos campos se excluyen de `toString()` y de los logs.

## Alternativas

Cifrado de toda la base en reposo (lo da el proveedor, pero no protege contra accesos desde la aplicación); cifrado en MySQL con `AES_ENCRYPT`.

## Justificación

El converter es transparente para el resto del código y la clave nunca llega a la base de datos.

## Consecuencias

- No se puede filtrar por restricciones en SQL. No importa: el filtro del RNF-03 descifra las restricciones de un solo hogar y luego descarta recetas en el servicio.
- Si se pierde la clave, se pierden esos datos: hay que respaldarla en el gestor de secretos.
- Las pruebas del RNF-22 intentan leer el hogar de otra cuenta y esperan 404.
