package org.canalesCMAC.adapter.out.soap;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoapSupportTest {

    @Test
    void construyeSobreConNamespaceYParametros() {
        String xml = SoapSupport.sobre("http://tempuri.org/", "ConsultaDeuda", Map.of("DatoConsulta", "0010681504"));

        assertTrue(xml.contains("<els:ConsultaDeuda>"));
        assertTrue(xml.contains("<els:DatoConsulta>0010681504</els:DatoConsulta>"));
    }

    @Test
    void parseaCuerpoConNodosAnidados() {
        String xml = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<Respuesta xmlns=\"http://proveedor\"><Codigo>0</Codigo>"
            + "<Cliente><Nombre>JUAN PEREZ</Nombre><Documento>20123456789</Documento></Cliente>"
            + "</Respuesta></soap:Body></soap:Envelope>";

        NodoXml raiz = SoapSupport.cuerpo(xml);

        assertEquals("Respuesta", raiz.nombre());
        assertEquals("0", raiz.textoDe("Codigo"));
        assertEquals("JUAN PEREZ", raiz.hijo("Cliente").textoDe("Nombre"));
        assertEquals("20123456789", raiz.hijo("Cliente").textoDe("Documento"));
    }
}
