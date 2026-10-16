# ADR-22: Proveedor de nube

- **Estado:** Aprobada
- **Fecha:** 2026-09-30 (actualizada 2026-10-15)
- **Origen:** D10 · KAN-9, KAN-11, KAN-50

## Decisión

El backend y la base de datos se despliegan en **AWS**, con el plan gratuito (créditos de USD 100 a 200 durante 6 meses). Firebase se queda con la identidad y el hosting del frontend, en su plan gratuito, sin facturación.

| Componente | Servicio | Costo aproximado |
| --- | --- | --- |
| Identidad | Firebase Authentication (ADR-05) | Gratis |
| Frontend (SPA) | Firebase Hosting | Gratis |
| Backend | AWS Elastic Beanstalk con una instancia Docker, detrás de CloudFront para HTTPS (se detalla en KAN-11) | Del crédito de AWS |
| Base de datos | Amazon RDS para MySQL 8.4, `db.t4g.micro`, una instancia con las bases `cocina_desarrollo` y `cocina_produccion` | Unos USD 18 al mes, del crédito |

El backend sigue empaquetado como imagen Docker y el frontend como sitio estático, así que cambiar de proveedor no cambia el código.

## Contexto

- El profesor exige AWS, Google Cloud o Azure, y ningún documento definía el proveedor.
- El diagrama de contexto del C4 decía que Firebase provee hosting, identidad y base de datos. Pero Firebase no ofrece MySQL ni ejecuta un backend Java como contenedor, que es lo que piden el diagrama de contenedores y los ADR-02, ADR-03 y ADR-04.
- Ningún integrante tiene GitHub Student Developer Pack.

## Criterios

Costo cero o cubierto por créditos durante el semestre, sin pagos anticipados; MySQL administrado con respaldo diario automático (RNF-20); HTTPS; despliegue desde GitHub Actions; ambientes de desarrollo y producción separados (ADR-12).

## Alternativas

| Proveedor | Por qué no |
| --- | --- |
| Azure for Students | Microsoft no reconoce el dominio `uqvirtual.edu.co` para verificar la condición de estudiante. |
| Azure con cuenta gratuita | Después de 30 días hay que pasar a pago por uso para conservar los servicios gratuitos; lo que exceda se cobra a la tarjeta. |
| Google Cloud (prueba de USD 300) | Era la opción más integrada con Firebase, pero la verificación exigió un prepago de COP 100.000 sin devolución garantizada, y las tarjetas de los demás integrantes no fueron aceptadas o no tienen. |

## Justificación

- El plan gratuito de AWS no cobra a la tarjeta: al acabarse el crédito o los 6 meses, la cuenta se cierra sola. Los 6 meses cubren el semestre.
- RDS incluye respaldo automático diario y restauración a cualquier minuto de los últimos días, que cumple el RNF-20 sin trabajo extra.
- Firebase Authentication no depende del proveedor: el backend verifica los tokens con la cuenta de servicio, guardada como secreto.

## Consecuencias

- El frontend (Firebase Hosting) y el backend (AWS) quedan en dominios distintos: hay que configurar CORS con `CORS_ORIGENES`, que el backend ya soporta.
- El HTTPS del backend necesita CloudFront delante de Elastic Beanstalk; se resuelve en KAN-11.
- Desarrollo y producción comparten la instancia de RDS; cada ambiente tiene su base y su usuario con permisos solo sobre ella (`docs/despliegue/base-de-datos.md`).
- El crédito se agota con el uso: hay que revisar el saldo cada semana y tener un presupuesto con alertas.
- Hay que corregir el diagrama de contexto del C4: **"Firebase: identidad y hosting del frontend"** y un sistema nuevo, **"AWS: backend (Elastic Beanstalk) y base de datos MySQL administrada (Amazon RDS)"**.
