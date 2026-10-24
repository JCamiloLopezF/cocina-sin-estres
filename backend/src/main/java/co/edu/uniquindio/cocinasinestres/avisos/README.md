# Avisos

Motor de reglas y centro de avisos (Proceso 4, ADR-17, KAN-27).

## Cómo funciona

1. Al abrir la app, el frontend pide `GET /api/avisos`.
2. `AvisosService` lee la despensa, el plan y las preferencias del hogar (`FuenteAvisos`) y corre las cinco reglas de `domain/ReglasAviso`:
   - `BAJO_MINIMO`: productos en su mínimo o por debajo.
   - `POR_VENCER`: vencen hoy o en los próximos 3 días.
   - `PREPARACION_PREVIA`: una comida de mañana pide alistar algo desde hoy.
   - `SEMANA_SIN_PLANIFICAR`: desde el viernes, la semana siguiente sin comidas. Antes de avisar, `BorradorSemanaService` (planificador) intenta dejarla armada como borrador.
   - `APROVECHAR_PRODUCTO`: la primera opción del modo rescate que salva algo por vencer.
3. Cada aviso se guarda con su clave de idempotencia (`AlmacenAvisos`); se devuelven hasta 5 sin leer.

Las reglas no usan Spring ni base de datos: se prueban en `ReglasAvisoTest`. El servicio no es transaccional a propósito: si el borrador o el modo rescate fallan, los demás avisos igual llegan.

## Reglas de módulo (RNF-26)

No usa entidades ni repositorios de otros módulos. Lee las tablas de despensa y plan con consultas de solo lectura (`FuenteAvisosJdbc`) y llama a los servicios del planificador para el borrador y el modo rescate.

## Endpoints

| Endpoint | Qué hace |
| --- | --- |
| `GET /api/avisos` | Evalúa las reglas y devuelve hasta 5 avisos sin leer. |
| `GET /api/avisos/recientes` | Centro de avisos: últimos 30 días. |
| `PUT /api/avisos/{id}/leido` | Marca uno como leído. |
| `PUT /api/avisos/leidos` | Marca todos como leídos. |
| `GET /api/avisos/preferencias` | Tipos de aviso y si están encendidos. |
| `PUT /api/avisos/preferencias/{tipo}` | `{ "activo": false }` lo apaga. |
