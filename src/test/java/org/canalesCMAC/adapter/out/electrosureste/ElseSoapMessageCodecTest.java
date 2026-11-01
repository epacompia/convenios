package org.canalesCMAC.adapter.out.electrosureste;

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
class ElseSoapMessageCodecTest {

    @Inject
    ElseSoapMessageCodec codec;

    @Test
    void serializaConsultaDeudaComoSoap() {
        RecaudacionRequest peticion = new RecaudacionRequest(Institucion.ELSE, Operador.CONSULTA_DEUDA,
            Map.of("datoConsulta", "0010681504", "tipoConsulta", "1"));
        String soap = codec.construir(peticion).contenido();
        assertTrue(soap.contains("ConsultaDeuda"));
        assertTrue(soap.contains("<els:DatoConsulta>0010681504</els:DatoConsulta>"));
        assertTrue(soap.contains("<els:TipoConsulta>1</els:TipoConsulta>"));
        assertTrue(soap.contains("<els:UsuarioConsulta>usuarioentidadfinan</els:UsuarioConsulta>"));
    }

    @Test
    void deserializaConsultaDeudaExitosa() {
        String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>" +
            "<ConsultaDeudaResponse xmlns=\"http://tempuri.org/\">" +
            "<CodigoMensajeRetornoConsulta>0</CodigoMensajeRetornoConsulta>" +
            "<MensajeRetornoConsulta>CORRECTO</MensajeRetornoConsulta>" +
            "<IdentificadorTransaccion>647A5DEC-242A-4FC9-83A0-775A096080B1</IdentificadorTransaccion>" +
            "<CodigoComprobante>2018001000000294383</CodigoComprobante>" +
            "<CodigoSuministro>10010681504</CodigoSuministro>" +
            "<NombreCliente>PAUCAR LOAYZA ANA</NombreCliente>" +
            "<MontoAPagarConsulta>23.10</MontoAPagarConsulta>" +
            "<FechaVencimiento>2018-03-27T00:00:00</FechaVencimiento>" +
            "</ConsultaDeudaResponse></soap:Body></soap:Envelope>";
        RecaudacionResponse respuesta = codec.interpretar(new RespuestaCruda(200, soap),
            new RecaudacionRequest(Institucion.ELSE, Operador.CONSULTA_DEUDA, Map.of()));
        assertEquals("00", respuesta.codigo());
        assertEquals("CORRECTO", respuesta.mensaje());
        assertEquals("647A5DEC-242A-4FC9-83A0-775A096080B1", respuesta.transacciones().get(0).get("identificadorTransaccion"));
        assertEquals("PAUCAR LOAYZA ANA", respuesta.cliente().get("nombreCliente"));
        assertEquals(0, new BigDecimal("23.10").compareTo((BigDecimal) respuesta.transacciones().get(0).get("monto")));
    }
}