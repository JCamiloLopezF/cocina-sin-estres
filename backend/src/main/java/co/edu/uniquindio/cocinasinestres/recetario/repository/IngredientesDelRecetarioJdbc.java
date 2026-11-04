package co.edu.uniquindio.cocinasinestres.recetario.repository;

import co.edu.uniquindio.cocinasinestres.recetario.domain.IngredientesDelRecetario;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lee los productos de las recetas activas por JDBC, sin entidades de curaduría (RNF-26). */
@Repository
public class IngredientesDelRecetarioJdbc implements IngredientesDelRecetario {

    private final JdbcClient jdbc;

    public IngredientesDelRecetarioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Ingrediente> listar() {
        return jdbc.sql("""
                SELECT DISTINCT p.id, p.nombre, p.categoria
                FROM producto p
                JOIN ingrediente_receta ir ON ir.producto_id = p.id
                JOIN receta r ON r.id = ir.receta_id
                WHERE r.activa = TRUE AND p.hogar_id IS NULL AND p.activo = TRUE
                ORDER BY p.nombre
                """)
            .query((rs, fila) -> new Ingrediente(rs.getLong("id"), rs.getString("nombre"), rs.getString("categoria")))
            .list();
    }
}
