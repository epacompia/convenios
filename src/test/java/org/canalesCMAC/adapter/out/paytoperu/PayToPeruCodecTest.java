package org.canalesCMAC.adapter.out.paytoperu;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.canalesCMAC.application.port.RespuestaCruda;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class PayToPeruCodecTest {

    private static final String RUTA_BASE = "/paytopperu";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TestPayToPeruCodec codec;

    @BeforeEach
    void setUp() {
        codec = new TestPayToPeruCodec();
    }


    @Test
    void construirConsultaPago() throws Exception {

        RecaudacionRequest request = peticion(
            "CONSULTA_PAGO",
            Map.of(
                Canonico.CODIGO_PAGO, "100200",
                Canonico.DOCUMENTO_CLIENTE, "12345678"
            )
        );

        var salida = codec.construir(request);

        JsonNode json = objectMapper.readTree(salida.contenido());

        assertEquals(RUTA_BASE + "/consulta_pago", salida.ruta());
        assertEquals("application/json", salida.contentType());

        assertEquals("100200", json.path("ncodigo_pago").asText());
        assertEquals("12345678", json.path("cnro_documento").asText());
    }

    @Test
    void construirNotificacionPago() throws Exception {

        RecaudacionRequest request = peticion(
            "NOTIFICACION_PAGO",
            Map.of(
                Canonico.CODIGO_PAGO, "100200",
                Canonico.CANAL, "01",
                Canonico.UBICACION_CANAL, "Av. de la Cultura",
                Canonico.MONTO, 55.0,
                Canonico.MONEDA, 1,
                Canonico.FECHA_OPERACION, "21-06-2019 13:42:05"
            )
        );

        var salida = codec.construir(request);

        JsonNode json = objectMapper.readTree(salida.contenido());

        assertEquals(RUTA_BASE + "/notificacion_pago", salida.ruta());

        assertEquals("100200", json.path("ncodigo_pago").asText());
        assertEquals("01", json.path("ccanal").asText());
        assertEquals("Av. de la Cultura", json.path("cubicacion_canal").asText());
        assertEquals(55.0, json.path("nimporte").asDouble());
        assertEquals(1, json.path("nmoneda").asInt());
        assertEquals("21-06-2019 13:42:05", json.path("dfecha").asText());
    }

    @Test
    void construirConfirmacionPago() throws Exception {

        RecaudacionRequest request = peticion(
            "CONFIRMACION_PAGO",
            Map.of(Canonico.CODIGO_PAGO, "100200")
        );

        var salida = codec.construir(request);

        JsonNode json = objectMapper.readTree(salida.contenido());

        assertEquals(RUTA_BASE + "/confirmacion_pago", salida.ruta());
        assertEquals("100200", json.path("ncodigo_pago").asText());
    }

    @Test
    void construirConsultaTransacciones() throws Exception {

        RecaudacionRequest request = peticion(
            "CONSULTA_TRANSACCIONES",
            Map.of(Canonico.FECHA_OPERACION, "21-06-2019")
        );

        var salida = codec.construir(request);

        JsonNode json = objectMapper.readTree(salida.contenido());

        assertEquals(RUTA_BASE + "/consulta_transacciones", salida.ruta());
        assertEquals("21-06-2019", json.path("dfecha").asText());
    }

    @Test
    void construirOperacionNoSoportada() {

        RecaudacionRequest request = peticion(
            "PAGO",
            Map.of()
        );

        assertThrows(
            OperadorNoSoportadoException.class,
            () -> codec.construir(request)
        );
    }


    @Test
    void interpretarRespuestaArray() {

        String json = """
            [
              {
                "ncodigo_pago": 100200,
                "ccanal": "01",
                "cubicacion_canal": "Av. de la Cultura Nro. 1624",
                "nimporte": 55.0,
                "nmoneda": 1,
                "dfecha": "21-06-2019 13:42:05",
                "cobservacion": "---"
              }
            ]
            """;

        RecaudacionRequest request = peticion(
            "CONSULTA_TRANSACCIONES",
            Map.of()
        );

        RecaudacionResponse response = codec.interpretar(
            respuesta(json),
            request
        );

        assertNotNull(response);
        assertEquals(1, response.transacciones().size());

        Map<String, Object> transaccion = response.transacciones().get(0);

        assertEquals("100200", transaccion.get(Canonico.CODIGO_PAGO));
        assertEquals("01", transaccion.get(Canonico.CANAL));
        assertEquals(
            "Av. de la Cultura Nro. 1624",
            transaccion.get(Canonico.UBICACION_CANAL)
        );
        assertEquals("55.0", transaccion.get(Canonico.MONTO));
        assertEquals("1", transaccion.get(Canonico.MONEDA));
        assertEquals(
            "21-06-2019 13:42:05",
            transaccion.get(Canonico.FECHA_OPERACION)
        );
        assertEquals("---", transaccion.get(Canonico.OBSERVACION));
    }

    @Test
    void interpretarRespuestaArrayVacio() {

        RecaudacionRequest request = peticion(
            "CONSULTA_TRANSACCIONES",
            Map.of()
        );

        RecaudacionResponse response = codec.interpretar(
            respuesta("[]"),
            request
        );

        assertNotNull(response);
        assertTrue(response.transacciones().isEmpty());
    }


    @Test
    void interpretarRespuestaObjeto() {

        String json = """
            {
              "ncodigo_pago": 100200,
              "berror": false,
              "cmensaje": "Notificación exitosa"
            }
            """;

        RecaudacionRequest request = peticion(
            "NOTIFICACION_PAGO",
            Map.of()
        );

        RecaudacionResponse response = codec.interpretar(
            respuesta(json),
            request
        );

        assertNotNull(response);
        assertEquals(1, response.transacciones().size());

        Map<String, Object> transaccion = response.transacciones().get(0);

        assertEquals("100200", transaccion.get(Canonico.CODIGO_PAGO));
    }

    @Test
    void interpretarRespuestaObjetoConDatosCliente() {

        String json = """
            {
              "ncodigo_pago": 100200,
              "cnro_documento": "12345678",
              "cnombres": "JUAN",
              "capellidos": "PEREZ",
              "cemail": "juan@gmail.com",
              "nimporte": 55.0,
              "nmoneda": 1,
              "cconcepto": "PENSION"
            }
            """;

        RecaudacionRequest request = peticion(
            "CONSULTA_PAGO",
            Map.of()
        );

        RecaudacionResponse response = codec.interpretar(
            respuesta(json),
            request
        );

        assertNotNull(response);

        assertEquals(
            "12345678",
            response.cliente().get(Canonico.DOCUMENTO_CLIENTE)
        );

        assertEquals(
            "JUAN PEREZ",
            response.cliente().get(Canonico.NOMBRE_CLIENTE)
        );

        assertEquals(
            "juan@gmail.com",
            response.cliente().get(Canonico.EMAIL)
        );

        assertEquals(1, response.transacciones().size());

        Map<String, Object> transaccion = response.transacciones().get(0);

        assertEquals("100200", transaccion.get(Canonico.CODIGO_PAGO));
        assertEquals("55.0", transaccion.get(Canonico.MONTO));
        assertEquals("1", transaccion.get(Canonico.MONEDA));
        assertEquals("PENSION", transaccion.get(Canonico.CODIGO_CONCEPTO));
    }


    @Test
    void interpretarRespuestaJsonInvalido() {

        RecaudacionRequest request = peticion(
            "CONSULTA_PAGO",
            Map.of()
        );

        assertThrows(
            IllegalStateException.class,
            () -> codec.interpretar(
                respuesta("{ json invalido"),
                request
            )
        );
    }


    private RecaudacionRequest peticion(
        String operacion,
        Map<String, Object> datos
    ) {
        return new RecaudacionRequest(
            Institucion.values()[0],
            Operador.valueOf(operacion),
            datos
        );
    }

    private RespuestaCruda respuesta(String cuerpo) {
        return new RespuestaCruda(200, cuerpo);
    }


    private static class TestPayToPeruCodec
        extends AbstractPayToPeruCodec {

        @Override
        protected String rutaBase() {
            return RUTA_BASE;
        }

        @Override
        public Institucion institucion() {
            return Institucion.PAYTOPERU;
        }
    }
}
