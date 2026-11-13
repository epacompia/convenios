package org.canalesCMAC.adapter.out.unsaac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class UnsAacCodecTest {

    private static final String CLAVE_PRUEBAS = "_aaaae00th@h%h1234";

    private final UnsAacCodec codec = new UnsAacCodec(CLAVE_PRUEBAS);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void construyeConsultaCifrandoElCodigoDeTransaccion() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.CONSULTA_DEUDA, Cabecera.generar(),
            new Datos(Map.of("numeroOperacion", "1760986"), Map.of(), List.of()));

        var salida = codec.construir(peticion);

        assertEquals("/SRVPPConsultaDetallePagoCC", salida.ruta());
        String cuerpo = salida.contenido();
        String cifrado = valor(cuerpo, "codigo_transaccion");
        assertEquals("1760986", AesCifrado.descifrar(cifrado, CLAVE_PRUEBAS));
    }

    @Test
    void construyePagoConElJsonCifradoDelContrato() throws Exception {
        Map<String, Object> transaccion = new LinkedHashMap<>();
        transaccion.put("numeroOperacion", "O-000000001760986");
        transaccion.put("monto", "8.00");
        transaccion.put("fechaOperacion", "2026-10-31T18:00:00");
        transaccion.put("numeroTarjeta", "000000XXXXX00000");
        transaccion.put("nombreTarjetaHabiente", "QUISPE MAMANI THALIA");
        transaccion.put("codigoAprobacion", "010735");
        transaccion.put("numeroPedido", "123456");
        transaccion.put("formaPago", "VENTANILLA CAJA CUSCO");
        transaccion.put("email", "171879@unsaac.edu.pe");
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.PAGO_DEUDA, Cabecera.generar(),
            new Datos(Map.of(), Map.of(), List.of(transaccion)));

        var salida = codec.construir(peticion);

        assertEquals("/SRVPPGuardarPagoCC", salida.ruta());
        String json = AesCifrado.descifrar(valor(salida.contenido(), "pago"), CLAVE_PRUEBAS);
        assertEquals("O-000000001760986", mapper.readTree(json).path("codigo_transaccion").asText());
        assertEquals("8.00", mapper.readTree(json).path("importe_autorizado").asText());
        assertEquals("010735", mapper.readTree(json).path("codigo_autorizacion").asText());
    }

    @Test
    void construyeVerificacionConListaDeOrdenes() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.VERIFICAR_PAGO, Cabecera.generar(),
            new Datos(Map.of("transacciones", List.of(
                Map.of("numeroOperacion", "O-1"),
                Map.of("numeroOperacion", "O-2"))), Map.of(), List.of()));

        var salida = codec.construir(peticion);

        assertEquals("/SRVPPVerificarPagoCC", salida.ruta());
        assertEquals("O-1|O-2",
            AesCifrado.descifrar(valor(salida.contenido(), "ccodigos_transaccion"), CLAVE_PRUEBAS));
    }

    @Test
    void construyeConfirmacionConExitoYNumeroDeOperacion() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.CONFIRMAR_PAGO, Cabecera.generar(),
            new Datos(Map.of("numeroOperacion", "O-000000001760986",
                "identificadorPago", "V-000000000913999",
                "exito", true), Map.of(), List.of()));

        var salida = codec.construir(peticion);

        assertEquals("/SRVPPConfirmarPagoCC", salida.ruta());
        assertEquals("O-000000001760986",
            AesCifrado.descifrar(valor(salida.contenido(), "codigo_transaccion"), CLAVE_PRUEBAS));
        assertEquals("true", valor(salida.contenido(), "exito"));
        assertEquals("V-000000000913999", valor(salida.contenido(), "numero_operacion"));
    }

    @Test
    void interpretaConsultaExitosa() throws Exception {
        String objeto = "{\"codigo_transaccion\":\"O-000000001760986\","
            + "\"keymerchant\":\"20172474501\",\"importe\":\"300.50\",\"items\":2,"
            + "\"moneda\":\"1\",\"email\":\"171879@unsaac.edu.pe\","
            + "\"codigo_cliente\":\"435623\",\"nombres\":\"CASTRO-REYNA-DAVID\","
            + "\"descripción\":\"Matricula pregrado 2022-2\",\"ccp\":\"10012\","
            + "\"conceptos\":[{\"nro\":1,\"codigo\":\"116\","
            + "\"descripcion\":\"Certificacion\",\"valor\":\"20.00\"}]}";
        String respuesta = envelope(true, null, "M02", AesCifrado.cifrar(objeto, CLAVE_PRUEBAS));

        RecaudacionResponse salida = codec.interpretar(new RespuestaCruda(200, respuesta),
            peticion(Operador.CONSULTA_DEUDA, Map.of("numeroOperacion", "1760986")));

        assertEquals("00", salida.codigo());
        assertEquals("CASTRO-REYNA-DAVID", salida.cliente().get("nombreCliente"));
        assertEquals("435623", salida.cliente().get("codigoCliente"));
        Map<String, Object> tx = salida.transacciones().get(0);
        assertEquals("O-000000001760986", tx.get("numeroOperacion"));
        assertEquals("PEN", tx.get("moneda"));
        assertEquals(1, ((List<?>) tx.get("deudas")).size());
    }

    @Test
    void interpretaPagoExitoso() throws Exception {
        String objeto = "{\"codigo_transaccion\":\"O-000000001760986\","
            + "\"codigo_pago\":\"V-000000000208456\",\"clave\":\"23300\","
            + "\"estado\":\"correcto\",\"mensaje\":\"\"}";
        String respuesta = envelope(true, null, "M02", AesCifrado.cifrar(objeto, CLAVE_PRUEBAS));

        RecaudacionResponse salida = codec.interpretar(new RespuestaCruda(200, respuesta),
            peticion(Operador.PAGO_DEUDA, Map.of("numeroOperacion", "O-000000001760986")));

        assertEquals("00", salida.codigo());
        Map<String, Object> tx = salida.transacciones().get(0);
        assertEquals("V-000000000208456", tx.get("identificadorPago"));
        assertEquals("23300", tx.get("clave"));
    }

    @Test
    void interpretaVerificacionConOrdenesPagadas() throws Exception {
        String respuesta = envelope(false,
            "las siguientes órdenes se encuentran como pago registrado", "M02",
            AesCifrado.cifrar("O-1|O-2", CLAVE_PRUEBAS));

        RecaudacionResponse salida = codec.interpretar(new RespuestaCruda(200, respuesta),
            peticion(Operador.VERIFICAR_PAGO, Map.of("transacciones", List.of())));

        assertEquals("01", salida.codigo());
        Map<String, Object> tx = salida.transacciones().get(0);
        assertEquals("PAGADO", tx.get("estado"));
        assertEquals(2, ((List<?>) tx.get("deudas")).size());
    }

    @Test
    void interpretaConfirmacionExitosa() throws Exception {
        String respuesta = envelope(true, null, "M02", null);
        RecaudacionResponse salida = codec.interpretar(new RespuestaCruda(200, respuesta),
            peticion(Operador.CONFIRMAR_PAGO, Map.of(
                "numeroOperacion", "O-000000001760986",
                "identificadorPago", "V-000000000913999",
                "exito", true)));

        assertEquals("00", salida.codigo());
        Map<String, Object> tx = salida.transacciones().get(0);
        assertEquals("OK", tx.get("estado"));
        assertEquals("V-000000000913999", tx.get("identificadorPago"));
    }

    @Test
    void interpretaErrorInternoComoFalloDelProveedor() throws Exception {
        String respuesta = envelope(false, "ERROR INTERNO", "M01", null);

        assertThrows(ProveedorException.class,
            () -> codec.interpretar(new RespuestaCruda(200, respuesta),
                peticion(Operador.CONSULTA_DEUDA, Map.of("numeroOperacion", "1760986"))));
    }

    private String envelope(boolean exito, String mensaje, String codresp, String objeto)
        throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("exito", exito);
        datos.put("mensaje", mensaje);
        datos.put("codresp", codresp);
        datos.put("objeto", objeto);
        return mapper.writeValueAsString(datos);
    }

    private RecaudacionRequest peticion(Operador operador, Map<String, Object> datos) {
        return new RecaudacionRequest(Institucion.UNSAAC, operador, Cabecera.generar(),
            new Datos(datos, Map.of(), List.of()));
    }

    private String valor(String formulario, String clave) {
        for (String par : formulario.split("&")) {
            String[] partes = par.split("=", 2);
            if (partes.length == 2 && clave.equals(partes[0])) {
                return URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
            }
        }
        assertTrue(false, "No se encontró el campo " + clave);
        return null;
    }
}
