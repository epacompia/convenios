package org.canalesCMAC.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class SanitizadorDatosTest {

    private SanitizadorDatos sanitizador;

    @BeforeEach
    void setUp() {
        sanitizador = new SanitizadorDatos();
        sanitizador.objectMapper = new ObjectMapper();
    }

    @Test
    void enmascaraSensiblesAnidadosYEnListas() {
        Map<String, Object> payload = Map.of(
            "numeroTarjeta", "4111111111111111",
            "claveTpv", "secreto",
            "datos", List.of(Map.of(
                "pinblock", "9876",
                "track2Data", "T2",
                "monto", "10.00")),
            "cliente", Map.of(
                "nombreCliente", "JUAN",
                "email", "juan@example.com"));

        String json = sanitizador.sanitizar(payload);

        assertTrue(json.contains("\"numeroTarjeta\":\"****\""));
        assertTrue(json.contains("\"claveTpv\":\"****\""));
        assertTrue(json.contains("\"pinblock\":\"****\""));
        assertTrue(json.contains("\"track2Data\":\"****\""));
        assertFalse(json.contains("4111111111111111"));
        assertFalse(json.contains("secreto"));
        assertFalse(json.contains("9876"));
        assertTrue(json.contains("JUAN"));
    }

    @Test
    void enmascaraJsonTextoYNoRompeContenidoNoJson() {
        String json = sanitizador.sanitizarJsonTexto("{\"mac\":\"ABC123\",\"monto\":10}");
        assertTrue(json.contains("\"mac\":\"****\""));
        assertFalse(json.contains("ABC123"));

        assertNull(sanitizador.sanitizarJsonTexto("<xml>no es json</xml>"));
        assertEquals("{}", sanitizador.sanitizar(null));
    }
}
