package org.canalesCMAC.adapter.out.yaganaste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class YaGanasteSoapMessageCodecTest {

    private final YaGanasteSoapMessageCodec codec = new YaGanasteSoapMessageCodec();

    @Test
    void construyeConsultaConOperacionConsulta() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.YAGANASTE, Operador.CONSULTA, Cabecera.generar(),
            new Datos(Map.of("codigoProducto", "P1"), Map.of(), List.of(Map.of(
                "tpv", "1",
                "claveTpv", "clave",
                "referenciaOperacion", "REF-1",
                "fechaOperacion", "2026-10-14T10:30:00"))));

        String xml = codec.construir(peticion).contenido();

        assertTrue(xml.contains("<net:consulta>"));
        assertTrue(xml.contains("<codigoProducto>P1</codigoProducto>"));
        assertTrue(xml.contains("<fechaHoraOperacion>2026-10-14T10:30:00</fechaHoraOperacion>"));
    }

    @Test
    void interpretaRespuestaConNodoResponse() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body>"
            + "<ns2:consultaResponse xmlns:ns2=\"net.sigma.h2h.ws\"><response>"
            + "<responseCode>00</responseCode><refOperador>REF-OP-1</refOperador>"
            + "<referenciaSigma>REF-PROV-1</referenciaSigma><importe>10.00</importe>"
            + "<estadoVenta>OK</estadoVenta><parametros><indice>1</indice>"
            + "<valor>CONSUH2H</valor></parametros></response></ns2:consultaResponse>"
            + "</S:Body></S:Envelope>";

        RecaudacionResponse respuesta = codec.interpretar(new RespuestaCruda(200, xml),
            new RecaudacionRequest(Institucion.YAGANASTE, Operador.CONSULTA,
                Cabecera.generar(), Datos.vacio()));

        assertEquals("00", respuesta.codigo());
        Map<String, Object> transaccion = respuesta.transacciones().get(0);
        assertEquals("REF-PROV-1", transaccion.get("referenciaProveedor"));
        assertEquals("REF-OP-1", transaccion.get("referenciaOperador"));
        assertEquals("OK", transaccion.get("estado"));
    }

    @Test
    void noIncluyeProductoNiParametrosEnEcho() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.YAGANASTE, Operador.ECHO, Cabecera.generar(),
            new Datos(Map.of(), Map.of(), List.of(Map.of(
                "tpv", "1",
                "claveTpv", "clave",
                "codigoProducto", "P1",
                "referenciaOperacion", "16765",
                "fechaOperacion", "2026-10-14T10:30:00",
                "parametrosOperacion", List.of(Map.of("indice", 1, "valor", "x"))))));

        String xml = codec.construir(peticion).contenido();

        assertTrue(xml.contains("<net:echo>"));
        assertTrue(xml.contains("<referenciaOperacion>16765</referenciaOperacion>"));
        assertFalse(xml.contains("<codigoProducto>"));
        assertFalse(xml.contains("<parametrosOperacion>"));
    }

    @Test
    void incluyeReferenciaEnSaldoYNormalizaFecha() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.YAGANASTE, Operador.SALDO, Cabecera.generar(),
            new Datos(Map.of(), Map.of(), List.of(Map.of(
                "tpv", "1",
                "claveTpv", "clave",
                "referenciaOperacion", "16765",
                "fechaOperacion", "2026-10-14"))));

        String xml = codec.construir(peticion).contenido();

        assertTrue(xml.contains("<net:saldo>"));
        assertTrue(xml.contains("<referenciaOperacion>16765</referenciaOperacion>"));
        assertTrue(xml.contains(
            "<fechaHoraOperacion>2026-10-14T00:00:00</fechaHoraOperacion>"));
        assertFalse(xml.contains("<codigoProducto>"));
    }

    @Test
    void construyeConsultaEstadoVentaRefFc() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.YAGANASTE, Operador.CONSULTA_ESTADO_VENTA_REF_FC, Cabecera.generar(),
            new Datos(Map.of(), Map.of(), List.of(Map.of(
                "tpv", "1",
                "claveTpv", "clave",
                "referenciaOperacion", "16765"))));

        String xml = codec.construir(peticion).contenido();

        assertTrue(xml.contains("<net:consultaEstadoVentaRefFC>"));
        assertTrue(xml.contains("<referenciaOperacion>16765</referenciaOperacion>"));
        assertFalse(xml.contains("<codigoProducto>"));
    }

    @Test
    void construyeConsultaEstadoVentaRefH2H() {
        RecaudacionRequest peticion = new RecaudacionRequest(
            Institucion.YAGANASTE, Operador.CONSULTA_ESTADO_VENTA_REF_H2H, Cabecera.generar(),
            new Datos(Map.of(), Map.of(), List.of(Map.of(
                "tpv", "1",
                "claveTpv", "clave",
                "referenciaOperacion", "16765"))));

        String xml = codec.construir(peticion).contenido();

        assertTrue(xml.contains("<net:consultaEstadoVentaRefH2H>"));
        assertTrue(xml.contains("<referenciaOperacion>16765</referenciaOperacion>"));
    }
}
