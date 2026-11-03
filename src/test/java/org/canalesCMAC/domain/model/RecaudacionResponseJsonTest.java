package org.canalesCMAC.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class RecaudacionResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializaYDeserializaElContratoUnico() throws Exception {

        RecaudacionResponse original = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "TRANSACCION CORRECTA",
            Map.of("nombreCliente", "DELGADO DE VIZCARRA NORA"),
            List.of(Map.of(
                "monto", 1500.00,
                "numeroComprobante", "98787"
            ))
        );

        String json = objectMapper.writeValueAsString(original);

        RecaudacionResponse copia = objectMapper.readValue(
            json,
            RecaudacionResponse.class
        );

        assertEquals(original.codigo(), copia.codigo());
        assertEquals(original.mensaje(), copia.mensaje());
        assertEquals(original.institucion(), copia.institucion());
        assertEquals(original.operador(), copia.operador());

        assertEquals(
            original.cabecera().idTransaccion(),
            copia.cabecera().idTransaccion()
        );

        assertEquals(
            "DELGADO DE VIZCARRA NORA",
            copia.cliente().get("nombreCliente")
        );

        assertEquals(1, copia.transacciones().size());
    }

    @Test
    void creaRespuestaCorrectaConMetodoOk() {

        Map<String, Object> cliente = Map.of(
            "nombreCliente", "JUAN PEREZ"
        );

        List<Map<String, Object>> transacciones = List.of(
            Map.of("monto", 100.00)
        );

        RecaudacionResponse response = RecaudacionResponse.ok(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            cliente,
            transacciones
        );

        assertEquals("00", response.codigo());
        assertEquals("OK", response.mensaje());
        assertEquals(Institucion.SEAL, response.institucion());
        assertEquals(Operador.CONSULTA_DEUDA, response.operador());
        assertEquals(cliente, response.cliente());
        assertEquals(transacciones, response.transacciones());
    }

    @Test
    void creaRespuestaDeErrorConMetodoError() {

        Cabecera cabecera = Cabecera.generar();

        RecaudacionResponse response = RecaudacionResponse.error(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            cabecera,
            "ERROR EN LA OPERACION"
        );

        assertEquals("ER", response.codigo());
        assertEquals("ERROR EN LA OPERACION", response.mensaje());
        assertEquals(Institucion.SEAL, response.institucion());
        assertEquals(Operador.CONSULTA_DEUDA, response.operador());
        assertEquals(cabecera, response.cabecera());

        assertTrue(response.cliente().isEmpty());
        assertTrue(response.transacciones().isEmpty());
    }

    @Test
    void creaRespuestaConValoresPorDefectoCuandoRespuestaEsNull() {

        RecaudacionResponse response = new RecaudacionResponse(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            null
        );

        assertNotNull(response.respuesta());
        assertEquals("ER", response.codigo());
        assertEquals("", response.mensaje());
        assertTrue(response.cliente().isEmpty());
        assertTrue(response.transacciones().isEmpty());
    }

    @Test
    void creaRespuestaConClienteYTransaccionesNull() {

        RecaudacionResponse.Respuesta respuesta =
            new RecaudacionResponse.Respuesta(
                "00",
                "OK",
                null,
                null
            );

        assertEquals("00", respuesta.codigo());
        assertEquals("OK", respuesta.mensaje());
        assertNotNull(respuesta.cliente());
        assertNotNull(respuesta.transaccion());
        assertTrue(respuesta.cliente().isEmpty());
        assertTrue(respuesta.transaccion().isEmpty());
    }

    @Test
    void devuelveVistaPlanaCombinandoClienteYPrimeraTransaccion() {

        Map<String, Object> cliente = Map.of(
            "nombreCliente", "JUAN PEREZ",
            "documento", "12345678"
        );

        Map<String, Object> transaccion = Map.of(
            "monto", 250.00,
            "comprobante", "98765"
        );

        RecaudacionResponse response = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "OK",
            cliente,
            List.of(transaccion)
        );

        Map<String, Object> datos = response.datos();

        assertEquals(4, datos.size());
        assertEquals("JUAN PEREZ", datos.get("nombreCliente"));
        assertEquals("12345678", datos.get("documento"));
        assertEquals(250.00, datos.get("monto"));
        assertEquals("98765", datos.get("comprobante"));
    }

    @Test
    void datosDevuelveClienteCuandoNoHayTransacciones() {

        RecaudacionResponse response = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "OK",
            Map.of("nombreCliente", "JUAN PEREZ"),
            List.of()
        );

        Map<String, Object> datos = response.datos();

        assertEquals(1, datos.size());
        assertEquals("JUAN PEREZ", datos.get("nombreCliente"));
    }

    @Test
    void datosDevuelveTransaccionCuandoClienteEsNull() {

        RecaudacionResponse response = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "OK",
            null,
            List.of(Map.of("monto", 100.00))
        );

        Map<String, Object> datos = response.datos();

        assertEquals(1, datos.size());
        assertEquals(100.00, datos.get("monto"));
    }

    @Test
    void datosDevuelveMapaVacioCuandoClienteYTransaccionesEstanVacios() {

        RecaudacionResponse response = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "OK",
            null,
            null
        );

        assertNotNull(response.datos());
        assertTrue(response.datos().isEmpty());
    }

    @Test
    void datosIgnoraPrimeraTransaccionCuandoEsNull() {

        List<Map<String, Object>> transacciones = Arrays.asList(
            null,
            Map.of("monto", 200.00)
        );

        RecaudacionResponse response = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "OK",
            Map.of("nombreCliente", "JUAN PEREZ"),
            transacciones
        );

        Map<String, Object> datos = response.datos();

        assertEquals(1, datos.size());
        assertEquals("JUAN PEREZ", datos.get("nombreCliente"));
    }

    @Test
    void primeraTransaccionSobrescribeCamposDelCliente() {

        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("monto", 100.00);
        cliente.put("nombreCliente", "JUAN PEREZ");

        Map<String, Object> transaccion = Map.of(
            "monto", 250.00
        );

        RecaudacionResponse response = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "OK",
            cliente,
            List.of(transaccion)
        );

        assertEquals(250.00, response.datos().get("monto"));
        assertEquals("JUAN PEREZ", response.datos().get("nombreCliente"));
    }

    @Test
    void obtieneCodigoMensajeClienteYTransacciones() {

        Map<String, Object> cliente = Map.of(
            "nombreCliente", "JUAN PEREZ"
        );

        List<Map<String, Object>> transacciones = List.of(
            Map.of("monto", 300.00)
        );

        RecaudacionResponse response = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            "00",
            "OPERACION EXITOSA",
            cliente,
            transacciones
        );

        assertEquals("00", response.codigo());
        assertEquals("OPERACION EXITOSA", response.mensaje());
        assertEquals(cliente, response.cliente());
        assertEquals(transacciones, response.transacciones());
    }
}