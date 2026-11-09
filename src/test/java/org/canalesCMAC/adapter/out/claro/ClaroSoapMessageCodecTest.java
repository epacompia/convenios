package org.canalesCMAC.adapter.out.claro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

class ClaroSoapMessageCodecTest {

    private final ClaroSoapMessageCodec codec = new ClaroSoapMessageCodec();

    @Test
    void rechazaRespuestaQueNoEsXml() {
        assertThrows(ProveedorException.class,
            () -> codec.interpretar(new RespuestaCruda(200, "no-es-xml"), peticion()));
    }

    @Test
    void rechazaRespuestaSinCodigo() {
        String xml = "<Envelope><Body><pagoResponse><return>"
            + "<codigoAutorizacion>AUTH-123</codigoAutorizacion>"
            + "</return></pagoResponse></Body></Envelope>";

        assertThrows(ProveedorException.class,
            () -> codec.interpretar(new RespuestaCruda(200, xml), peticion()));
    }

    @Test
    void interpretaRespuestaCorrecta() {
        String xml = "<Envelope><Body><pagoResponse><return>"
            + "<codigoRespuesta>00</codigoRespuesta>"
            + "<codigoAutorizacion>AUTH-123</codigoAutorizacion>"
            + "</return></pagoResponse></Body></Envelope>";

        RecaudacionResponse respuesta = codec.interpretar(new RespuestaCruda(200, xml), peticion());

        assertEquals("00", respuesta.codigo());
        assertEquals("AUTH-123", respuesta.transacciones().get(0).get("codigoAprobacion"));
    }

    private RecaudacionRequest peticion() {
        return new RecaudacionRequest(Institucion.CLARO, Operador.PAGO, Cabecera.generar(),
            new Datos(Map.of(), Map.of(), List.of(Map.of("monto", "10.00"))));
    }
}
