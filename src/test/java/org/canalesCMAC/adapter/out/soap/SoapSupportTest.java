package org.canalesCMAC.adapter.out.soap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoapSupportTest {

    private static final String SOAP_NS =
            "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String TEMPURI_NS =
            "http://tempuri.org/";

    @Test
    void construyeSobreConNamespaceYParametros() {

        String xml = SoapSupport.sobre(
                TEMPURI_NS,
                "ConsultaDeuda",
                Map.of("DatoConsulta", "0010681504")
        );

        assertNotNull(xml);
        assertTrue(xml.contains("<soap:Envelope"));
        assertTrue(xml.contains("<soap:Body"));
        assertTrue(xml.contains("<els:ConsultaDeuda>"));
        assertTrue(xml.contains(
                "<els:DatoConsulta>0010681504</els:DatoConsulta>"
        ));
        assertTrue(xml.contains("xmlns:els=\"" + TEMPURI_NS + "\""));
    }

    @Test
    void construyeSobreConMultiplesParametros() {

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("CodigoCliente", "12345");
        parametros.put("Monto", "150.50");
        parametros.put("Moneda", "PEN");

        String xml = SoapSupport.sobre(
                TEMPURI_NS,
                "RegistrarPago",
                parametros
        );

        assertTrue(xml.contains(
                "<els:CodigoCliente>12345</els:CodigoCliente>"
        ));
        assertTrue(xml.contains(
                "<els:Monto>150.50</els:Monto>"
        ));
        assertTrue(xml.contains(
                "<els:Moneda>PEN</els:Moneda>"
        ));
    }

    @Test
    void construyeSobreConParametrosNulos() {

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("CodigoCliente", null);

        String xml = SoapSupport.sobre(
                TEMPURI_NS,
                "ConsultaDeuda",
                parametros
        );

        assertTrue(xml.contains(
                "<els:CodigoCliente></els:CodigoCliente>"
        ));
    }

    @Test
    void construyeSobreSinParametros() {

        String xml = SoapSupport.sobre(
                TEMPURI_NS,
                "ConsultaDeuda",
                Map.of()
        );

        assertTrue(xml.contains("ConsultaDeuda"));
    }

    @Test
    void construyeSobreConCaracteresEspeciales() {

        Map<String, String> parametros = Map.of(
                "Descripcion",
                "Pago & consulta <cliente>"
        );

        String xml = SoapSupport.sobre(
                TEMPURI_NS,
                "RegistrarPago",
                parametros
        );

        assertTrue(xml.contains("&amp;"));
        assertTrue(xml.contains("&lt;"));
        
    }

    @Test
    void construyeSobreConCaracteresUnicode() {

        Map<String, String> parametros = Map.of(
                "Descripcion",
                "Operación válida"
        );

        String xml = SoapSupport.sobre(
                TEMPURI_NS,
                "RegistrarPago",
                parametros
        );

        assertTrue(xml.contains("Operación válida"));
    }

    @Test
    void lanzaIllegalStateExceptionCuandoFallaLaConstruccionDelSobre() {

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> SoapSupport.sobre(
                        null,
                        "ConsultaDeuda",
                        Map.of("Codigo", "001")
                )
        );

        assertEquals(
                "No se pudo construir el sobre SOAP",
                exception.getMessage()
        );

        assertNotNull(exception.getCause());
    }


    @Test
    void parseaCuerpoConNodosAnidados() {

        String xml = "<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\">"
                + "<soap:Body>"
                + "<Respuesta xmlns=\"http://proveedor\">"
                + "<Codigo>0</Codigo>"
                + "<Cliente>"
                + "<Nombre>JUAN PEREZ</Nombre>"
                + "<Documento>20123456789</Documento>"
                + "</Cliente>"
                + "</Respuesta>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals("0", raiz.textoDe("Codigo"));
        assertEquals(
                "JUAN PEREZ",
                raiz.hijo("Cliente").textoDe("Nombre")
        );
        assertEquals(
                "20123456789",
                raiz.hijo("Cliente").textoDe("Documento")
        );
    }

    @Test
    void buscaBodyDespuesDeEncontrarElementosPrevios() {

        String xml = "<Envelope>"
                + "<Header>"
                + "<Autenticacion>123</Autenticacion>"
                + "</Header>"
                + "<Body>"
                + "<Respuesta><Codigo>00</Codigo></Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals("00", raiz.textoDe("Codigo"));
    }

    @Test
    void parseaRespuestaConUnSoloNodo() {

        String xml = "<Envelope>"
                + "<Body>"
                + "<Respuesta>OK</Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals("OK", raiz.texto());
    }

    @Test
    void parseaRespuestaConMultiplesNiveles() {

        String xml = "<Envelope>"
                + "<Body>"
                + "<Respuesta>"
                + "<Operacion>"
                + "<Detalle>"
                + "<Codigo>00</Codigo>"
                + "</Detalle>"
                + "</Operacion>"
                + "</Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals(
                "00",
                raiz.hijo("Operacion")
                        .hijo("Detalle")
                        .textoDe("Codigo")
        );
    }

    @Test
    void parseaRespuestaConVariosHijos() {

        String xml = "<Envelope>"
                + "<Body>"
                + "<Respuesta>"
                + "<Codigo>00</Codigo>"
                + "<Mensaje>Correcto</Mensaje>"
                + "<Estado>APROBADO</Estado>"
                + "</Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("00", raiz.textoDe("Codigo"));
        assertEquals("Correcto", raiz.textoDe("Mensaje"));
        assertEquals("APROBADO", raiz.textoDe("Estado"));
    }

    @Test
    void parseaRespuestaConContenidoVacio() {

        String xml = "<Envelope>"
                + "<Body>"
                + "<Respuesta>"
                + "<Codigo></Codigo>"
                + "</Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("", raiz.textoDe("Codigo"));
    }

    @Test
    void parseaRespuestaConCdata() {

        String xml = "<Envelope>"
                + "<Body>"
                + "<Respuesta>"
                + "<Mensaje><![CDATA[Pago realizado correctamente]]></Mensaje>"
                + "</Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals(
                "Pago realizado correctamente",
                raiz.textoDe("Mensaje")
        );
    }

    @Test
    void parseaRespuestaSimpleYAlcanzaFinDeElemento() {

        String xml = "<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\">"
                + "<soap:Body>"
                + "<Respuesta>OK</Respuesta>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals("OK", raiz.texto());
    }

    @Test
    void parseaNodosAnidadosYAlcanzaFinDeCadaElemento() {

        String xml = "<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\">"
                + "<soap:Body>"
                + "<Respuesta>"
                + "<Cliente>"
                + "<Nombre>JUAN</Nombre>"
                + "</Cliente>"
                + "</Respuesta>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals(
                "JUAN",
                raiz.hijo("Cliente").textoDe("Nombre")
        );
    }

    @Test
    void finalizaLecturaDeNodosAnidados() {

        String xml = "<Envelope>"
                + "<Body>"
                + "<Respuesta>"
                + "<Operacion>"
                + "<Detalle>"
                + "<Codigo>00</Codigo>"
                + "</Detalle>"
                + "</Operacion>"
                + "</Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals(
                "00",
                raiz.hijo("Operacion")
                        .hijo("Detalle")
                        .textoDe("Codigo")
        );
    }

    @Test
    void finalizaLecturaCuandoBodyNoTieneOperacion() {

        String xml = "<Envelope>"
                + "<Body>"
                + "</Body>"
                + "</Envelope>";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SoapSupport.cuerpo(xml)
        );

        assertEquals(
                "SOAP sin operación de respuesta",
                exception.getMessage()
        );
    }

    @Test
    void lanzaExcepcionCuandoSoapNoTieneOperacion() {

        String xml = "<Envelope>"
                + "<Body></Body>"
                + "</Envelope>";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SoapSupport.cuerpo(xml)
        );

        assertEquals(
                "SOAP sin operación de respuesta",
                exception.getMessage()
        );
    }

    @Test
    void lanzaExcepcionCuandoXmlEsInvalido() {

        String xml = "<Envelope><Body><Respuesta>";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SoapSupport.cuerpo(xml)
        );

        assertTrue(
                exception.getMessage().startsWith(
                        "Respuesta SOAP no válida:"
                )
        );
    }

    @Test
    void lanzaExcepcionCuandoXmlEstaVacio() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SoapSupport.cuerpo("")
        );

        assertNotNull(exception);
    }

    @Test
    void noConfundeElementosFueraDelBodyConLaRespuesta() {

        String xml = "<Envelope>"
                + "<Header>"
                + "<Autenticacion>123</Autenticacion>"
                + "</Header>"
                + "<Body>"
                + "<Respuesta>"
                + "<Codigo>00</Codigo>"
                + "</Respuesta>"
                + "</Body>"
                + "</Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals("00", raiz.textoDe("Codigo"));
        assertFalse("Header".equals(raiz.nombre()));
    }
}
