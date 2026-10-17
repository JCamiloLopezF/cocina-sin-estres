package co.edu.uniquindio.cocinasinestres.despensa.repository;

import co.edu.uniquindio.cocinasinestres.despensa.domain.HogarDeUsuaria;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Busca el hogar de una usuaria en la tabla {@code hogar}, sin usar la entidad de perfil
 * (RNF-26).
 *
 * <p>Es una solución provisional: cuando perfil publique un servicio que entregue el hogar
 * de una usuaria, esta clase se reemplaza por una que lo llame.</p>
 */
@Repository
public class HogarDeUsuariaJdbc implements HogarDeUsuaria {

    private final JdbcClient jdbc;

    public HogarDeUsuariaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Long> hogarIdDe(Long usuarioId) {
        return jdbc.sql("SELECT id FROM hogar WHERE usuario_id = :usuarioId")
            .param("usuarioId", usuarioId)
            .query(Long.class)
            .optional();
    }
}
