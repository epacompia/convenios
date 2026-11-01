package org.canalesCMAC.adapter.out.seal;

import java.math.BigDecimal;
import java.util.Map;

import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SealIso8583CodecTest {

    @Inject
    SealIso8583Codec codec;

    @Test
    void construyeConsultaComoIso8583() {
        RecaudacionRequest peticion = new RecaudacionRequest(Institucion.SEAL, Operador.CONSULTA_DEUDA,
            Map.of("contrato", "98787", "identificadorTransaccion", "369571",
                "fechaOperacion", "2026-10-01T10:20:30"));
        String trama = codec.construir(peticion).contenido();
        assertTrue(trama.startsWith("0200F038048188E08000"));
        assertEquals("0000000000000080", trama.substring(20, 36));
        assertTrue(trama.contains("310000"));
        assertTrue(trama.contains("369571"));
        assertTrue(trama.contains("20261001"));
        assertTrue(trama.contains("102030"));
        assertTrue(trama.contains("INTERBA"));
    }

    @Test
    void construyeAnulacionConAprobacion() {
        RecaudacionRequest peticion = new RecaudacionRequest(Institucion.SEAL, Operador.ANULACION,
            Map.of("contrato", "98787", "codigoAprobacion", "00001"));
        String trama = codec.construir(peticion).contenido();
        assertTrue(trama.startsWith("0200F03804818CE08000"));
        assertTrue(trama.contains("220000"));
    }

    @Test
    void construyeExtornoConMtiDeReversa() {
        RecaudacionRequest peticion = new RecaudacionRequest(Institucion.SEAL, Operador.EXTORNO_PAGO,
            Map.of("contrato", "98787"));
        String trama = codec.construir(peticion).contenido();
        assertTrue(trama.startsWith("0400F038048188E08000"));
        assertTrue(trama.contains("210000"));
    }

    @Test
    void interpretaConsultaExitosa() {
        RecaudacionResponse respuesta = codec.interpretar(new RespuestaCruda(200, consultaResponse()),
            new RecaudacionRequest(Institucion.SEAL, Operador.CONSULTA_DEUDA, Map.of()));
        assertEquals("00", respuesta.codigo());
        assertEquals("TRANSACCION CORRECTA", respuesta.mensaje());
        assertEquals("98787", respuesta.transacciones().get(0).get("numeroComprobante"));
        assertEquals("DELGADO DE VIZCARRA NORA", respuesta.cliente().get("nombreCliente"));
        assertEquals("CALLE NICOLAS DE PIEROLA NRO 117-2", respuesta.cliente().get("direccionCliente"));
        assertEquals(0, new BigDecimal("1500.00").compareTo((BigDecimal) respuesta.transacciones().get(0).get("monto")));
    }

    private String consultaResponse() {
        String contenido = padRight("INTERBA", 7) + padRight("98787", 6) + padLeft("9999", 4)
            + padRight("DELGADO DE VIZCARRA NORA", 50) + padRight("CALLE NICOLAS DE PIEROLA NRO 117-2", 50);
        String campo121 = padLeft(String.valueOf(contenido.length() + 4), 4) + contenido;
        return "0210" + "F03804818E808000" + "0000000000000080"
            + padLeft("", 19) + "310000" + "000000150000" + "369571" + "123846" + "20100416"
            + "010" + "10" + "40001000" + "20013005" + "123456789012" + "000001" + "00"
            + "00000000" + "604" + campo121;
    }

    private String padLeft(String valor, int longitud) {
        return "0".repeat(Math.max(0, longitud - valor.length())) + valor;
    }

    private String padRight(String valor, int longitud) {
        return valor + " ".repeat(Math.max(0, longitud - valor.length()));
    }
}
