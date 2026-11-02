package org.canalesCMAC.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EsquemaTest {

    private static final Set<String> TABLAS = Set.of(
        "convenio",
        "convenio_operador",
        "recaudacion_operacion",
        "recaudacion_transaccion",
        "recaudacion_cliente",
        "recaudacion_deuda",
        "recaudacion_pago",
        "recaudacion_extorno",
        "recaudacion_intento_proveedor",
        "recaudacion_log",
        "recaudacion_idempotencia",
        "recaudacion_conciliacion");

    @Inject
    AgroalDataSource dataSource;

    @Test
    void existenLasDoceTablasDelModelo() throws Exception {
        Set<String> encontradas = new HashSet<>();
        try (Connection conexion = dataSource.getConnection();
             Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery(
                 "select table_name from information_schema.tables where table_schema = 'PUBLIC'")) {
            while (resultado.next()) {
                encontradas.add(resultado.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        assertTrue(encontradas.containsAll(TABLAS),
            "Faltan tablas: " + diferencia(TABLAS, encontradas));
    }

    private Set<String> diferencia(Set<String> esperadas, Set<String> encontradas) {
        Set<String> faltantes = new HashSet<>(esperadas);
        faltantes.removeAll(encontradas);
        return faltantes;
    }
}
