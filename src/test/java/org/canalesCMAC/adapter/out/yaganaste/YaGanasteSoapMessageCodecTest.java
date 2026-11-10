
package org.canalesCMAC.adapter.out.yaganaste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

class YaGanasteSoapMessageCodecTest {

    private YaGanasteSoapMessageCodec codec;

    @BeforeEach
    void setUp() {
        codec = new YaGanasteSoapMessageCodec();
    }

    // =======================================================
    // CONSTRUCCION DE PETICIONES SOAP
    // =======================================================

    @Test
    void construyeXmlParaOperacionEcho() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.ECHO,
            Map.of(
                Canonico.TPV, "TPV001",
                Canonico.CLAVE_TPV, "CLAVE123",
                Canonico.REFERENCIA_OPERACION, "REF001",
                Canonico.FECHA_OPERACION, "2026-11-09"
            )
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);
        assertEquals("text/xml; charset=utf-8", salida.contentType());

        assertTrue(salida.contenido().contains("<net:echo>"));
        assertTrue(salida.contenido().contains("</net:echo>"));
        assertTrue(salida.contenido().contains("<tpv>TPV001</tpv>"));
        assertTrue(salida.contenido().contains("<claveTpv>CLAVE123</claveTpv>"));
        assertTrue(salida.contenido().contains("<referenciaOperacion>REF001</referenciaOperacion>"));
        assertTrue(salida.contenido().contains("<fechaHoraOperacion>2026-11-09</fechaHoraOperacion>"));
    }

    @Test
    void construyeXmlParaOperacionDevolucion() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.DEVOLUCION,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);
        assertTrue(salida.contenido().contains("<net:devolucion>"));
        assertTrue(salida.contenido().contains("</net:devolucion>"));
        assertTrue(salida.contenido().contains("<codigoProducto>PROD001</codigoProducto>"));
    }

    @Test
    void construyeXmlParaOperacionVenta() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);
        assertTrue(salida.contenido().contains("<net:venta>"));
        assertTrue(salida.contenido().contains("</net:venta>"));
    }

    @Test
    void construyeXmlParaOperacionConsulta() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);
        assertTrue(salida.contenido().contains("<net:consulta>"));
        assertTrue(salida.contenido().contains("</net:consulta>"));
    }

    @Test
    void construyeXmlParaOperacionConsultaPrevia() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_PREVIA,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);
        assertTrue(salida.contenido().contains("<net:consultaPrevia>"));
        assertTrue(salida.contenido().contains("</net:consultaPrevia>"));
    }

    @Test
    void construyeXmlParaOperacionSaldo() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.SALDO,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);
        assertTrue(salida.contenido().contains("<net:saldo>"));
        assertTrue(salida.contenido().contains("</net:saldo>"));
    }

    @Test
    void construyeXmlConHeaderYEnvelopeSoap() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertTrue(salida.contenido().contains("<soapenv:Envelope"));
        assertTrue(salida.contenido().contains("<soapenv:Header/>"));
        assertTrue(salida.contenido().contains("<soapenv:Body>"));
        assertTrue(salida.contenido().contains("</soapenv:Envelope>"));
    }

    @Test
    void omiteCamposNulosOVaciosAlConstruirXml() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
            Map.of(
                Canonico.TPV, "TPV001",
                Canonico.CLAVE_TPV, "",
                Canonico.CODIGO_PRODUCTO, "PROD001"
            )
        );

        PeticionSalida salida = codec.construir(peticion);

        assertTrue(salida.contenido().contains("<tpv>TPV001</tpv>"));
        assertFalse(salida.contenido().contains("<claveTpv>"));
        assertFalse(salida.contenido().contains("<referenciaOperacion>"));
        assertFalse(salida.contenido().contains("<fechaHoraOperacion>"));
    }

    // =======================================================
    // INTERPRETACION DE RESPUESTAS SOAP
    // =======================================================

    @Test
    void interpretaRespuestaSoapCorrectamente() {

        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope
                xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                xmlns:net="net.sigma.h2h.ws">
                <soapenv:Body>
                    <net:ventaResponse>
                        <response>
                            <refOperador>REF-123</refOperador>
                            <importe>150.50</importe>
                            <referenciaSigma>SIGMA-001</referenciaSigma>
                            <saldo>500.00</saldo>
                        </response>
                    </net:ventaResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(respuesta, peticion);

        assertNotNull(resultado);
        assertEquals(Institucion.YAGANASTE, resultado.institucion());
        assertEquals(Operador.VENTA, resultado.operador());

        assertNotNull(resultado.transacciones());
        assertEquals(1, resultado.transacciones().size());

        Map<String, Object> transaccion = resultado.transacciones().get(0);

        assertEquals("REF-123", transaccion.get(Canonico.REFERENCIA_OPERADOR));
        assertEquals("150.50", transaccion.get(Canonico.IMPORTE_ORIGEN));
        assertEquals("SIGMA-001", transaccion.get("referenciaSigma"));
        assertEquals("500.00", transaccion.get(Canonico.SALDO));
    }

    @Test
    void interpretaRespuestaSoapSinDatosDeTransaccion() {

        String xml = """
            <Envelope>
                <Body>
                    <ventaResponse>
                        <response/>
                    </ventaResponse>
                </Body>
            </Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(respuesta, peticion);

        assertNotNull(resultado);
        assertEquals(Institucion.YAGANASTE, resultado.institucion());
        assertTrue(resultado.transacciones().isEmpty());
    }

    @Test
    void interpretaRespuestaSoapConCamposNulos() {

        String xml = """
            <Envelope>
                <Body>
                    <ventaResponse>
                        <response>
                            <refOperador></refOperador>
                            <importe></importe>
                            <referenciaSigma>SIGMA-001</referenciaSigma>
                        </response>
                    </ventaResponse>
                </Body>
            </Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(respuesta, peticion);

        assertNotNull(resultado);
        assertEquals(1, resultado.transacciones().size());

        Map<String, Object> transaccion = resultado.transacciones().get(0);

        assertFalse(transaccion.containsKey(Canonico.REFERENCIA_OPERADOR));
        assertFalse(transaccion.containsKey(Canonico.IMPORTE_ORIGEN));
        assertEquals("SIGMA-001", transaccion.get("referenciaSigma"));
    }

    @Test
    void lanzaExcepcionCuandoLaRespuestaSoapEsInvalida() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(
            500,
            "<Envelope><Body><ventaResponse>"
        );

        IllegalStateException excepcion = assertThrows(
            IllegalStateException.class,
            () -> codec.interpretar(respuesta, peticion)
        );

        assertTrue(excepcion.getMessage().contains("Respuesta inválida"));
    }

    // =======================================================
    // VALIDACION DE OPERACIONES
    // =======================================================

    @Test
    void obtieneExcepcionCuandoElOperadorNoEstaSoportado() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        assertThrows(
            OperadorNoSoportadoException.class,
            () -> codec.construir(peticion)
        );
    }

    // =======================================================
    // METODOS AUXILIARES
    // =======================================================

    private RecaudacionRequest crearPeticion(
        Operador operador,
        Map<String, Object> datos
    ) {
        return new RecaudacionRequest(
            Institucion.YAGANASTE,
            operador,
            Cabecera.generar(),
            new Datos(datos, datos, null)
        );
    }

    private Map<String, Object> datosCompletos() {
        return Map.of(
            Canonico.TPV, "TPV001",
            Canonico.CLAVE_TPV, "CLAVE123",
            Canonico.CODIGO_PRODUCTO, "PROD001",
            Canonico.REFERENCIA_OPERACION, "REF001",
            Canonico.FECHA_OPERACION, "2026-11-09"
        );
    }
}
