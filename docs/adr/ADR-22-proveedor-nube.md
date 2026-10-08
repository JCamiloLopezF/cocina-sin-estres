# ADR-22: Proveedor de nube

- **Estado:** Aprobada
- **Fecha:** 2026-09-30 (actualizada 2026-10-14)
- **Origen:** D10 · KAN-9, KAN-11, KAN-50

## Decisión

Todo se despliega en **Google Cloud, dentro del mismo proyecto de Firebase**, pagado con el crédito de la prueba gratuita:

| Componente | Servicio | Costo aproximado |
| --- | --- | --- |
| Frontend (SPA) | Firebase Hosting | Gratis |
| Identidad | Firebase Authentication (ADR-05) | Gratis |
| Backend | Cloud Run, imagen Docker desde GitHub Container Registry | Dentro de la capa gratuita mensual |
| Base de datos | Cloud SQL para MySQL 8.4, edición Enterprise, `db-f1-micro`, una instancia con las bases `cocina_desarrollo` y `cocina_produccion` | Unos USD 10 al mes, del crédito de USD 300 |

El backend sigue empaquetado como imagen Docker y el frontend como sitio estático, así que cambiar de proveedor no cambiaría el código.

## Contexto

- El profesor exige AWS, Google Cloud o Azure, y ningún documento definía el proveedor.
- El diagrama de contexto del C4 decía que Firebase provee hosting, identidad y base de datos. Pero Firebase no ofrece MySQL ni ejecuta un backend Java como contenedor, que es lo que piden el diagrama de contenedores y los ADR-02, ADR-03 y ADR-04.
- La primera propuesta fue Azure for Students. Se descartó porque Microsoft no reconoce el dominio `uqvirtual.edu.co` para verificar la condición de estudiante, y el equipo no tiene GitHub Student Developer Pack.

## Criterios

Costo cero o cubierto por créditos durante el semestre; MySQL administrado con respaldo diario automático (RNF-20); HTTPS; despliegue desde GitHub Actions; ambientes de desarrollo y producción separados (ADR-12); integración simple con Firebase.

## Alternativas

| Proveedor | Por qué no |
| --- | --- |
| Azure for Students | No verifica el dominio de la universidad. |
| Azure con cuenta gratuita | Pide tarjeta, y después de 30 días hay que pasar a pago por uso para conservar los servicios gratuitos de 12 meses: lo que exceda se cobra. |
| AWS | Pide tarjeta, y separa la identidad (Firebase) del resto de la infraestructura igual que Azure. |

## Justificación

- Un proyecto de Firebase **es** un proyecto de Google Cloud: identidad, hosting, backend y base de datos quedan en un solo lugar, con una sola facturación y los mismos permisos.
- Cloud Run se conecta a Cloud SQL con el conector oficial, sin abrir la base a internet.
- La prueba gratuita pide tarjeta solo para verificar la identidad y no cobra mientras no se active una cuenta paga.
- Corrige el diagrama de contexto con un cambio mínimo: "Firebase / Google Cloud" sigue siendo el sistema externo que aloja la aplicación.

## Consecuencias

- **La edición de Cloud SQL debe ser Enterprise.** Para MySQL 8.4, Google crea por defecto Enterprise Plus, que cuesta unas 25 veces más. El script de KAN-9 lo fija.
- Desarrollo y producción comparten la instancia; cada ambiente tiene su base y su usuario con permisos solo sobre ella (`docs/despliegue/base-de-datos.md`).
- `db-f1-micro` no está cubierta por el SLA de Cloud SQL; para el curso basta.
- El crédito y la prueba tienen fecha límite; la consola la muestra. Cubre el semestre, pero hay que revisar el saldo cada semana y tener un presupuesto con alertas.
- Hay que corregir el diagrama de contexto del C4: **"Firebase / Google Cloud: hosting, identidad, backend y base de datos MySQL administrada (Cloud SQL)"**.
- KAN-11 agrega al backend la dependencia del conector de Cloud SQL (`com.google.cloud.sql:mysql-socket-factory-connector-j-8`).
