package org.canalesCMAC.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DatosTest {

    @Test
    void normalizaAliasDelContratoPlanoAnterior() {
        Datos datos = new Datos(
            Map.of("ordenpago", "123", "nroSumin", "S1", "dfecha", "2026-10-14"),
            Map.of("cnro_documento", "87654321"),
            List.of(Map.of("nimporte", 10.5)));

        assertEquals("123", datos.get("numeroOrdenPago"));
        assertEquals("S1", datos.get("numeroSuministro"));
        assertEquals("2026-10-14", datos.get("fechaOperacion"));
        assertEquals("87654321", datos.get("numeroDocumento"));
        assertEquals(10.5, datos.get("monto"));
    }

    @Test
    void losValoresNulosNoSobrescribenCamposValidos() {
        Map<String, Object> transaccion = new LinkedHashMap<>();
        transaccion.put("numeroSuministro", null);
        transaccion.put("monto", null);

        Datos datos = new Datos(
            Map.of("numeroSuministro", "S1"),
            Map.of(),
            List.of(transaccion));

        assertEquals("S1", datos.get("numeroSuministro"));
        assertNull(datos.get("monto"));
    }
}
