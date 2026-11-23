package org.canalesCMAC.adapter.out.unsaac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Valida que la coleccion de contrato de UNSAAC y el codec compartan
 * rutas, nombres de campos y cifrado AES Everywhere.
 */
class ContratoDocsUnsAacTest {

    private static final String CLAVE = "_aaaae00th@h%h1234";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void laColeccionYElCodecCompartenRutasCamposYCifrado() throws Exception {
        JsonNode raiz = MAPPER.readTree(
            Files.readString(Path.of("docs/UNSAAC.postman_collection.json")));
        Map<String, Set<String>> camposPorRuta = new LinkedHashMap<>();
        Map<String, String> valores = new LinkedHashMap<>();
        for (JsonNode item : raiz.path("item")) {
            JsonNode solicitud = item.path("request");
            JsonNode ruta = solicitud.path("url").path("path");
            String ultimo = ruta.get(ruta.size() - 1).asText();
            Set<String> campos = new TreeSet<>();
            for (JsonNode campo : solicitud.path("body").path("urlencoded")) {
                String nombre = campo.path("key").asText();
                campos.add(nombre);
                valores.put(nombre, campo.path("value").asText());
            }
            camposPorRuta.put(ultimo, campos);
        }

        assertTrue(camposPorRuta.containsKey("SRVPPConsultaDetallePagoCC"));
        assertTrue(camposPorRuta.containsKey("SRVPPGuardarPagoCC"));
        assertTrue(camposPorRuta.containsKey("SRVPPVerificarPagoCC"));
        assertTrue(camposPorRuta.containsKey("SRVPPConfirmarPagoCC"));

        UnsAacCodec codec = new UnsAacCodec(CLAVE);

        RecaudacionRequest consulta = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.CONSULTA_DEUDA, Cabecera.generar(),
            new Datos(Map.of("numeroOperacion", "1760986"), Map.of(), List.of()));
        PeticionSalida salidaConsulta = codec.construir(consulta);
        assertEquals("/SRVPPConsultaDetallePagoCC", salidaConsulta.ruta());
        assertEquals(Set.of("codigo_transaccion"), clavesFormulario(salidaConsulta));
        assertEquals("1760986", AesCifrado.descifrar(
            decodificar(valorFormulario(salidaConsulta, "codigo_transaccion")), CLAVE));

        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("numeroOperacion", "O-000000001760986");
        pago.put("monto", "8.00");
        pago.put("fechaOperacion", "31102026180000");
        pago.put("numeroTarjeta", "000000XXXXX00000");
        pago.put("codigoAprobacion", "010735");
        pago.put("numeroPedido", "123456");
        pago.put("formaPago", "VENTANILLA CAJA CUSCO");
        pago.put("nombreTarjetaHabiente", "QUISPE MAMANI THALIA");
        pago.put("email", "171879@unsaac.edu.pe");
        pago.put("proveedorNombre", "");
        RecaudacionRequest solicitudPago = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.PAGO_DEUDA, Cabecera.generar(),
            new Datos(Map.of(), Map.of(), List.of(pago)));
        PeticionSalida salidaPago = codec.construir(solicitudPago);
        assertEquals("/SRVPPGuardarPagoCC", salidaPago.ruta());
        assertEquals(Set.of("pago"), clavesFormulario(salidaPago));
        JsonNode pagoCodec = MAPPER.readTree(
            AesCifrado.descifrar(decodificar(valorFormulario(salidaPago, "pago")), CLAVE));
        assertEquals(Set.of("fecha", "nombre_tarjeta_habiente", "importe_autorizado",
            "codigo_transaccion", "numero_tarjeta", "numero_pedido", "forma_pago",
            "codigo_autorizacion", "email", "proveedorNombre"), campos(pagoCodec));

        RecaudacionRequest verificar = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.VERIFICAR_PAGO, Cabecera.generar(),
            new Datos(Map.of("transacciones",
                List.of(Map.of("numeroOperacion", "O-000000001760986"))), Map.of(), List.of()));
        PeticionSalida salidaVerificar = codec.construir(verificar);
        assertEquals("/SRVPPVerificarPagoCC", salidaVerificar.ruta());
        assertEquals(Set.of("ccodigos_transaccion"), clavesFormulario(salidaVerificar));
        assertEquals("O-000000001760986", AesCifrado.descifrar(
            decodificar(valorFormulario(salidaVerificar, "ccodigos_transaccion")), CLAVE));

        RecaudacionRequest confirmar = new RecaudacionRequest(
            Institucion.UNSAAC, Operador.CONFIRMAR_PAGO, Cabecera.generar(),
            new Datos(Map.of("numeroOperacion", "O-000000001760986",
                "identificadorPago", "V-000000000913999", "exito", true),
                Map.of(), List.of()));
        PeticionSalida salidaConfirmar = codec.construir(confirmar);
        assertEquals("/SRVPPConfirmarPagoCC", salidaConfirmar.ruta());
        assertEquals(Set.of("codigo_transaccion", "exito", "numero_operacion"),
            clavesFormulario(salidaConfirmar));
        assertEquals("O-000000001760986", AesCifrado.descifrar(
            decodificar(valorFormulario(salidaConfirmar, "codigo_transaccion")), CLAVE));
        assertEquals("true", decodificar(valorFormulario(salidaConfirmar, "exito")).trim());
        assertEquals("V-000000000913999",
            decodificar(valorFormulario(salidaConfirmar, "numero_operacion")).trim());
    }

    private static Set<String> clavesFormulario(PeticionSalida salida) {
        Set<String> claves = new TreeSet<>();
        for (String par : salida.contenido().split("&")) {
            if (!par.isBlank()) {
                claves.add(par.substring(0, par.indexOf('=')));
            }
        }
        return claves;
    }

    private static String valorFormulario(PeticionSalida salida, String clave) {
        for (String par : salida.contenido().split("&")) {
            int indice = par.indexOf('=');
            if (indice > 0 && par.substring(0, indice).equals(clave)) {
                return par.substring(indice + 1);
            }
        }
        return null;
    }

    private static String decodificar(String valor) {
        return valor == null ? null : URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    private static Set<String> campos(JsonNode nodo) {
        Set<String> campos = new TreeSet<>();
        nodo.fieldNames().forEachRemaining(campos::add);
        return campos;
    }
}
