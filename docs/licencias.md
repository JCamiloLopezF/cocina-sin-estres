# Licencias

Auditoría externa del 4 de noviembre: «las dependencias tienen licencias permisivas, pero el repositorio no tiene archivo LICENSE ni registro de licencias».

## Licencia del proyecto

El código de Cocina sin estrés se publica con la **licencia MIT** (archivo `LICENSE` en la raíz): cualquiera puede usarlo, copiarlo y modificarlo si conserva el aviso de autoría. Es la licencia más común en proyectos académicos y es compatible con todas las dependencias de abajo.

## Dependencias directas

Las versiones están fijadas en `web-app/package.json` y en el BOM de Spring Boot (`backend/build.gradle`).

### Backend (Java)

| Dependencia | Para qué | Licencia |
| --- | --- | --- |
| Spring Boot 4.1 (web, security, data-jpa, validation, flyway) | Framework de la API | Apache 2.0 |
| Hibernate ORM 7 (vía Spring Data JPA) | Persistencia | Apache 2.0 |
| Jackson 3 (vía Spring Boot) | JSON | Apache 2.0 |
| Flyway y `flyway-mysql` | Migraciones del esquema (ADR-07) | Apache 2.0 |
| Firebase Admin SDK 9.9 | Verificar los tokens de Firebase Auth (ADR-05) | Apache 2.0 |
| MySQL Connector/J | Controlador JDBC (solo en ejecución) | GPL 2.0 con *Universal FOSS Exception* (se puede usar desde software con licencia MIT) |
| Spring Boot Test, Spring Security Test | Pruebas | Apache 2.0 |
| JUnit 5 | Pruebas | EPL 2.0 |
| AssertJ | Pruebas | Apache 2.0 |
| Mockito | Pruebas | MIT |
| Testcontainers (MySQL) | Pruebas de integración con MySQL real | MIT |

### Frontend (web-app)

| Dependencia | Para qué | Licencia |
| --- | --- | --- |
| React y React DOM 19 | Interfaz | MIT |
| React Router 8 | Rutas | MIT |
| Firebase JS SDK 13 | Inicio de sesión | Apache 2.0 |
| Atkinson Hyperlegible Next (Fontsource) | Tipografía para baja visión | SIL Open Font License 1.1 |
| Vite, Vitest, jsdom, oxlint, @vitejs/plugin-react | Compilar, probar y revisar el código (solo desarrollo) | MIT |
| TypeScript | Tipos (solo desarrollo) | Apache 2.0 |
| Testing Library (react, user-event, jest-dom) | Pruebas de pantallas (solo desarrollo) | MIT |
| axe-core | Pruebas automáticas de accesibilidad (solo desarrollo) | MPL 2.0 |

### Todas las dependencias del frontend

Contadas desde `web-app/package-lock.json` el 4 de noviembre de 2026:

| Licencia | Lo que va al navegador (95 paquetes) | Con las de desarrollo (259 paquetes) |
| --- | --- | --- |
| Apache 2.0 | 53 | 78 |
| MIT y MIT-0 | 25 | 143 |
| BSD (2 y 3 cláusulas), 0BSD, ISC | 16 | 22 |
| SIL OFL 1.1 (tipografía) | 1 | 1 |
| MPL 2.0 | 0 | 13 |
| BlueOak 1.0, CC0 | 0 | 2 |

Todas son permisivas o de copyleft débil por archivo (MPL 2.0, solo en herramientas de desarrollo que no se distribuyen). Ninguna obliga a publicar el código de la aplicación con otra licencia.

## Servicios externos

| Servicio | Uso | Condiciones |
| --- | --- | --- |
| Firebase Auth y Hosting | Cuentas y sitio web | Plan gratuito Spark, términos de Google Firebase |
| AWS (Elastic Beanstalk, RDS, CloudFront) | API y base de datos | Capa gratuita de AWS |
| YouTube | Videos de las recetas (enlace, sin descargar el video) | Términos de servicio de YouTube; el video se muestra con su reproductor oficial |

## Cómo mantenerla

Al agregar una dependencia, en el mismo PR se agrega su fila aquí. No se agregan dependencias con licencia GPL o AGPL sin excepción para enlazarlas desde software MIT.
