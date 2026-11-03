package org.canalesCMAC.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class RecaudacionResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializaYDeserializaElContratoUnico() throws Exception {
        RecaudacionResponse original = RecaudacionResponse.de(
            Institucion.SEAL, Operador.CONSULTA_DEUDA, Cabecera.generar(),
            "00", "TRANSACCION CORRECTA",
            Map.of("nombreCliente", "DELGADO DE VIZCARRA NORA"),
            List.of(Map.of("monto", 1500.00, "numeroComprobante", "98787")));

        String json = objectMapper.writeValueAsString(original);
        RecaudacionResponse copia = objectMapper.readValue(json, RecaudacionResponse.class);

        assertEquals(original.codigo(), copia.codigo());
        assertEquals(original.mensaje(), copia.mensaje());
        assertEquals(original.cabecera().idTransaccion(), copia.cabecera().idTransaccion());
        assertEquals("DELGADO DE VIZCARRA NORA", copia.cliente().get("nombreCliente"));
        assertEquals(1, copia.transacciones().size());
    }
}
