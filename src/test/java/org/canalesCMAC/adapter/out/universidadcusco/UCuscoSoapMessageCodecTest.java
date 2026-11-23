package org.canalesCMAC.adapter.out.universidadcusco;

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

class UCuscoSoapMessageCodecTest {

    private final UCuscoSoapMessageCodec codec = new UCuscoSoapMessageCodec();

    @Test
    void construyeConsultaConOperacionYCampoDelContrato() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.UNIVCUSCO, Operador.CONSULTA_DEUDA, Cabecera.generar(),
            new Datos(Map.of(
                "numeroReferencialDeuda", "123456",
                "codigoConvenio", "1223",
                "canal", "TF",
                "codigoOficina", "1241",
                "fechaOperacion", "2026-10-14T10:30:00"), Map.of(), List.of()));

        String xml = codec.construir(peticion).contenido();

        assertTrue(xml.contains("<uac:ConsultarDeuda>"));
        assertTrue(xml.contains("<uac:codigoOperacion>1010</uac:codigoOperacion>"));
        assertTrue(xml.contains("<uac:numeroReferenciaDeuda>123456</uac:numeroReferenciaDeuda>"));
        assertTrue(xml.contains("<uac:fechaOperacion>20261014</uac:fechaOperacion>"));
        assertTrue(xml.contains("<uac:horaOperacion>103000</uac:horaOperacion>"));
    }

    @Test
    void interpretaConsultaDelContrato() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<ConsultarDeudaResponse xmlns=\"http://www.uac.com.pe/\"><recaudosRs><detalle>"
            + "<respuesta><codigo>0001</codigo><descripcion>TRANSACCION REALIZADA CON EXITO</descripcion></respuesta>"
            + "<transaccion><numeroReferenciaDeuda>123456</numeroReferenciaDeuda>"
            + "<nombreCliente>ALUMNO UAC</nombreCliente><numeroOperacionEmpresa>220009</numeroOperacionEmpresa>"
            + "<cantidadDocsDeuda>1</cantidadDocsDeuda><listaDocumentos><documento>"
            + "<numero>T-012-145230</numero><descripcion>Pago unico</descripcion>"
            + "<fechaEmision>20250928</fechaEmision><fechaVencimiento>20250928</fechaVencimiento>"
            + "<importeDeuda>150.50</importeDeuda><importeDeudaMinima>50.00</importeDeudaMinima>"
            + "</documento></listaDocumentos></transaccion></detalle></recaudosRs></ConsultarDeudaResponse>"
            + "</soap:Body></soap:Envelope>";

        RecaudacionResponse respuesta = codec.interpretar(new RespuestaCruda(200, xml),
            new RecaudacionRequest(Institucion.UNIVCUSCO, Operador.CONSULTA_DEUDA,
                Cabecera.generar(), Datos.vacio()));

        assertEquals("00", respuesta.codigo());
        assertEquals("ALUMNO UAC", respuesta.cliente().get("nombreCliente"));
        Map<String, Object> transaccion = respuesta.transacciones().get(0);
        assertEquals("220009", transaccion.get("numeroOperacion"));
        assertEquals("123456", transaccion.get("numeroComprobante"));
        List<?> deudas = (List<?>) transaccion.get("deudas");
        assertEquals("T-012-145230", ((Map<?, ?>) deudas.get(0)).get("numeroComprobante"));
    }
}
