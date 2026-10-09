package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Calcula qué campos de un producto cambiaron entre dos instantáneas.
 *
 * <p>El resultado tiene el formato de {@code auditoria_catalogo.cambios}:
 * {@code {"campo": {"antes": ..., "despues": ...}}}. Si {@code antes} es
 * {@code null} (creación), todos los campos con valor aparecen como cambio.</p>
 *
 * <p>No utiliza Spring ni JPA porque pertenece al dominio.</p>
 */
public final class ComparadorProducto {

    private ComparadorProducto() {
    }

    public static Map<String, Map<String, Object>> comparar(
        InstantaneaProducto antes,
        InstantaneaProducto despues) {

        Objects.requireNonNull(despues, "despues");

        Map<String, Object> valoresAntes = valores(antes);
        Map<String, Object> valoresDespues = valores(despues);
        Map<String, Map<String, Object>> cambios = new LinkedHashMap<>();

        for (Map.Entry<String, Object> campo : valoresDespues.entrySet()) {
            Object valorAntes = valoresAntes.get(campo.getKey());
            Object valorDespues = campo.getValue();

            if (!iguales(valorAntes, valorDespues)) {
                Map<String, Object> par = new LinkedHashMap<>();
                par.put("antes", valorAntes);
                par.put("despues", valorDespues);
                cambios.put(campo.getKey(), par);
            }
        }

        return cambios;
    }

    private static Map<String, Object> valores(InstantaneaProducto p) {
        Map<String, Object> valores = new LinkedHashMap<>();

        valores.put("nombre", p == null ? null : p.nombre());
        valores.put("categoria", p == null ? null : p.categoria());
        valores.put("tipoCantidad", p == null ? null : nombre(p.tipoCantidad()));
        valores.put("unidadBase", p == null ? null : nombre(p.unidadBase()));
        valores.put("unidadCompra", p == null ? null : p.unidadCompra());
        valores.put("cantidadUnidadCompra", p == null ? null : p.cantidadUnidadCompra());
        valores.put("precioUnidadCompra", p == null ? null : p.precioUnidadCompra());
        valores.put("capacidadReferencia", p == null ? null : p.capacidadReferencia());
        valores.put("esBasico", p == null ? null : p.esBasico());
        valores.put("activo", p == null ? null : p.activo());

        return valores;
    }

    private static String nombre(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    /**
     * Los decimales se comparan por valor: 500 y 500.000 son iguales.
     */
    private static boolean iguales(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        return Objects.equals(a, b);
    }
}
