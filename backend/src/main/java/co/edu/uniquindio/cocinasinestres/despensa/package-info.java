/**
 * Módulo <strong>despensa</strong>: Productos del hogar, cantidades, vencimientos y mínimos.
 *
 * <p>Depende de: perfil (el hogar de la usuaria) y del catálogo de productos. Un módulo nunca
 * usa las entidades ni los repositorios de otro: llama a su servicio (RNF-26). Mientras esos
 * servicios no existan, la despensa los consulta a través de los puertos
 * {@code HogarDeUsuaria} y {@code CatalogoProductos}, definidos en {@code domain/}.</p>
 */
package co.edu.uniquindio.cocinasinestres.despensa;
