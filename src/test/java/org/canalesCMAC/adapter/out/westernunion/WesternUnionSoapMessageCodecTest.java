package org.canalesCMAC.adapter.out.westernunion;

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

class WesternUnionSoapMessageCodecTest {

    private final WesternUnionSoapMessageCodec codec = new WesternUnionSoapMessageCodec();

    @Test
    void construyeConfirmarLoteConListaDeTransacciones() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.WESTERN_UNION, Operador.CONFIRMAR_LOTE, Cabecera.generar(),
            new Datos(Map.of(
                "idAgente", "77519999",
                "idLocacion", "91234",
                "caja", "C1234"), Map.of(), List.of(Map.of(
                    "transacciones", List.of(Map.of(
                        "idTransaccion", "1561471319936",
                        "moneda", "PEN",
                        "importe", "1000.00"))))));

        String xml = codec.construir(peticion).contenido();

        assertTrue(xml.contains("<ns2:confirmarLote>"));
        assertTrue(xml.contains("<listaTransacciones><transaccion>"));
        assertTrue(xml.contains("<idTransaccion>1561471319936</idTransaccion>"));
        assertTrue(xml.contains("</transaccion></listaTransacciones>"));
    }

    @Test
    void interpretaConfirmarLoteConEstadoDeLote() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<ns2:confirmarLoteResponse xmlns:ns2=\"http://gcr.sepsa.com.ar/wsdl/GatewayService/\">"
            + "<response><idAgente>77514287</idAgente><idLocacion>95221</idLocacion>"
            + "<caja>1</caja><operador>Test</operador>"
            + "<lote><idLote>9999999999999</idLote><estado>ACEPTADA</estado></lote>"
            + "<transacciones><transaccion><idTransaccion>1561471319936</idTransaccion>"
            + "<moneda>PEN</moneda><importe>1000.00</importe><estado>ACEPTADA</estado>"
            + "</transaccion></transacciones></response></ns2:confirmarLoteResponse>"
            + "</soap:Body></soap:Envelope>";

        RecaudacionResponse respuesta = codec.interpretar(new RespuestaCruda(200, xml),
            new RecaudacionRequest(Institucion.WESTERN_UNION, Operador.CONFIRMAR_LOTE,
                Cabecera.generar(), Datos.vacio()));

        assertEquals("00", respuesta.codigo());
        Map<String, Object> transaccion = respuesta.transacciones().get(0);
        assertEquals("9999999999999", transaccion.get("numeroLote"));
        assertEquals("ACEPTADA", transaccion.get("estado"));
        List<?> deudas = (List<?>) transaccion.get("deudas");
        assertEquals("1561471319936", ((Map<?, ?>) deudas.get(0)).get("numeroOperacion"));
    }
}
