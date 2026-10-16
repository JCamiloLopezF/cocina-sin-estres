# ADR-23: Trazabilidad del catálogo

- **Estado:** Abierta
- **Fecha:** 2026-09-30
- **Origen:** D11 · KAN-12, KAN-17

## Decisión

Cada cambio a productos, precios, recetas o restricciones del catálogo se registra en la tabla `auditoria_catalogo`, en la misma transacción que el cambio. Además, `producto` y `receta` llevan las columnas `modificado_por` y `modificado_en`. Los productos y recetas no se borran: se desactivan.

## Contexto

El RNF-24 exige que el 100 % de los cambios del catálogo se asocien a un curador identificable, pero no había diseño.

## Detalle

- `auditoria_catalogo`: id, entidad (`PRODUCTO`, `RECETA`, `INGREDIENTE_RECETA` o `RESTRICCION`), id de la entidad, acción (`CREAR`, `EDITAR` o `DESACTIVAR`), `usuario_id` (`NOT NULL`), fecha y los campos cambiados con su valor anterior y nuevo (JSON).
- La escribe un servicio del módulo de curaduría, no un trigger, porque solo la aplicación sabe qué curador hizo el cambio.
- `producto.activo` y `receta.activa` reemplazan el borrado, porque recetas, despensas y planes antiguos siguen apuntando a esos registros.

## Alternativas

Solo las columnas `modificado_por` y `modificado_en`; triggers de MySQL; librería de auditoría (Hibernate Envers).

## Justificación

Una tabla propia es simple de consultar en la sustentación y deja, sin trabajo extra, el historial de precios de referencia.

## Consecuencias

- La verificación del RNF-24 se reduce a comprobar que no hay cambios sin `usuario_id`, lo que garantiza la restricción `NOT NULL`.
- Todo producto del catálogo global exige `modificado_por` (`ck_producto_catalogo`), y toda receta también (`NOT NULL`).

## Carga inicial del catálogo (usuario de sistema)

Las migraciones que cargan datos del catálogo (`V2__catalogo_inicial.sql` y siguientes) no tienen un curador real, porque los usuarios se crean al iniciar sesión con Firebase. Esas cargas se atribuyen a un **usuario de sistema** que crea la propia migración:

```sql
-- No puede iniciar sesión: ningún usuario de Firebase tiene este uid y la cuenta está inactiva.
INSERT INTO usuario (firebase_uid, correo, rol, perfil_completo, activo)
VALUES ('sistema-carga-inicial', 'sistema@cocinasinestres.local', 'CURADOR', TRUE, FALSE);

SET @sistema = (SELECT id FROM usuario WHERE firebase_uid = 'sistema-carga-inicial');
```

- Cada producto o receta de la carga usa `@sistema` en `modificado_por`.
- Al final se registra un `CREAR` por cada fila cargada en `auditoria_catalogo`, con `usuario_id = @sistema`.
- Una migración posterior que cargue más datos (por ejemplo, recetas) reutiliza el mismo usuario con el `SET @sistema = (SELECT ...)`; no lo vuelve a crear.
