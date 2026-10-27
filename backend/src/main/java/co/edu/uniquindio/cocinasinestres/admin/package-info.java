/**
 * Módulo <strong>admin</strong>: Gestión de cuentas y roles, y análisis del uso con los
 * indicadores del Plan de Gestión de la Calidad (KAN-54). Rol ADMIN.
 *
 * <p>Depende de: perfil. Un módulo nunca usa las entidades ni los repositorios
 * de otro: llama a su servicio (RNF-26). El análisis es un reporte: lee con SQL las
 * tablas de los demás módulos ({@code FuenteAnalisisJdbc}), solo para contar y promediar.</p>
 */
package co.edu.uniquindio.cocinasinestres.admin;
