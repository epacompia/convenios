package org.canalesCMAC.adapter.out.universidadcontinental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import org.junit.jupiter.api.Test;

class PagoIdiomasMessageCodecTest {

    private final PagoIdiomasMessageCodec codec = new PagoIdiomasMessageCodec();

    @Test
    void construyePagoConRutaYCamposDelContrato() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.UNIVCONTINENTAL_CIDIOMAS,
            Operador.UNIVERSIDAD_PAGO_DEUDA,
            Cabecera.generar(),
            new Datos(
                Map.of("contrato", "12345678"),
                Map.of(),
                List.of(Map.of(
                    "numeroTrace", "000123",
                    "fechaOperacion", "2026-10-14",
                    "canal", "1",
                    "datos", "{\"detalle\":\"x\"}"
                ))
            )
        );

        var salida = codec.construir(peticion);

        assertEquals("/api/pagoidiomas/pago", salida.ruta());
        assertTrue(salida.contenido().contains("\"CodigoContrato\":\"12345678\""));
        assertTrue(salida.contenido().contains("\"NumeroTrace\":\"000123\""));
        assertTrue(salida.contenido().contains("\"Fecha\":\"2026-10-14\""));
        assertTrue(salida.contenido().contains("\"Canal\":\"1\""));
        assertTrue(salida.contenido().contains("\"Datos\":"));
    }

    @Test
    void interpretaConsultaConDocumentosDelContrato() {
        String json = "{\"Codigo\":\"00000\",\"Mensaje\":\"OK\",\"CodigoContrato\":\"12345678\","
            + "\"Cliente\":\"ALUMNO CONTINENTAL\",\"NumeroOperacion\":\"OP-1\",\"Datos\":["
            + "{\"FechaCreacion\":\"2026-09-01\",\"FechaVencimiento\":\"2026-10-01\","
            + "\"Moneda\":\"PEN\",\"Monto\":250.00,\"NumeroDocumento\":\"DOC-1\",\"Periodo\":\"2026-10\"}]}";

        RecaudacionResponse respuesta = codec.interpretar(
            new RespuestaCruda(200, json),
            new RecaudacionRequest(
                Institucion.UNIVCONTINENTAL_CIDIOMAS,
                Operador.UNIVERSIDAD_CONSULTA_DEUDA,
                Cabecera.generar(),
                Datos.vacio()
            )
        );

        assertEquals("00", respuesta.codigo());
        assertEquals("ALUMNO CONTINENTAL", respuesta.cliente().get("nombreCliente"));

        Map<String, Object> transaccion = respuesta.transacciones().get(0);
        assertEquals("OP-1", transaccion.get("numeroOperacion"));

        List<?> deudas = (List<?>) transaccion.get("deudas");
        Map<?, ?> deuda = (Map<?, ?>) deudas.get(0);

        assertEquals("DOC-1", deuda.get("numeroComprobante"));
        assertEquals("2026-10", deuda.get("periodo"));
    }
}