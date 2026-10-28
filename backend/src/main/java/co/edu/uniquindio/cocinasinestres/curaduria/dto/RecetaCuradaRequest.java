package co.edu.uniquindio.cocinasinestres.curaduria.dto;

import java.util.List;

/**
 * Una receta que el curador crea o edita desde su pantalla (RNF-28): todo lo que la
 * usuaria ve en el recetario y lo que el plan necesita para proponerla.
 *
 * @param nombre            entre 3 y 120 caracteres; no se repite en el catálogo
 * @param descripcion       opcional, hasta 500 caracteres
 * @param tiempoMinutos     tiempo total, de 1 a 600 minutos
 * @param porciones         para cuántas personas son las cantidades, de 1 a 20
 * @param pasos             en orden; cada uno dice como máximo un tiempo (Modo cocinar)
 * @param preparacionPrevia lo que se hace el día anterior (remojar los fríjoles); opcional
 * @param ingredientes      productos del catálogo con su cantidad en la unidad base
 * @param momentos          {@code DESAYUNO}, {@code ALMUERZO} y/o {@code CENA}
 * @param platoFuerte       es una comida; {@code false} para jugos y acompañamientos
 * @param gustaANinos       suele gustarles a los niños
 */
public record RecetaCuradaRequest(
    String nombre,
    String descripcion,
    Integer tiempoMinutos,
    Integer porciones,
    List<String> pasos,
    String preparacionPrevia,
    List<IngredienteCuradoRequest> ingredientes,
    List<String> momentos,
    Boolean platoFuerte,
    Boolean gustaANinos) {
}
