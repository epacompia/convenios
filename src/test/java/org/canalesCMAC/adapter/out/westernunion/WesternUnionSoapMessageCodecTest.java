
package org.canalesCMAC.adapter.out.westernunion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
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

class WesternUnionSoapMessageCodecTest {

    private WesternUnionSoapMessageCodec codec;

    @BeforeEach
    void setUp() {
        codec = new WesternUnionSoapMessageCodec();
    }

    @Test
    void devuelveInstitucionWesternUnion() {
        assertEquals(Institucion.WESTERN_UNION, codec.institucion());
    }

    @Test
    void construyeXmlParaBuscarProducto() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "buscarProducto");

        assertTrue(resultado.contenido().contains("<idAgente>AGENTE01</idAgente>"));
        assertTrue(resultado.contenido().contains("<idLocacion>LOCACION01</idLocacion>"));
        assertTrue(resultado.contenido().contains("<caja>CAJA01</caja>"));
        assertTrue(resultado.contenido().contains("<operador>OP01</operador>"));
    }

    @Test
    void construyeXmlParaConsultarTransaccion() {
        RecaudacionRequest request = peticion("CONSULTAR_TRANSACCION");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "consultarTransaccion");

        verificarCamposComunes(resultado);
    }

    @Test
    void construyeXmlParaConsultarDatosAdicionales() {
        RecaudacionRequest request = peticion("CONSULTAR_DATOS_ADICIONALES");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "consultarDatosAdicionales");

        verificarCamposComunes(resultado);
        assertTrue(resultado.contenido().contains("<idTransaccion>TRANS001</idTransaccion>"));
        assertTrue(resultado.contenido().contains("<idProducto>PROD001</idProducto>"));
    }

    @Test
    void construyeXmlParaAgregarDatosAdicionales() {
        RecaudacionRequest request = peticion("AGREGAR_DATOS_ADICIONALES");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "agregarDatosAdicionales");

        verificarCamposComunes(resultado);
        assertTrue(resultado.contenido().contains("<fecha>2026-11-11</fecha>"));
        assertTrue(resultado.contenido().contains("<idTransaccion>TRANS001</idTransaccion>"));
        assertTrue(resultado.contenido().contains("<idProducto>PROD001</idProducto>"));
    }

    @Test
    void construyeXmlParaConfirmarLote() {
        RecaudacionRequest request = peticion("CONFIRMAR_LOTE");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "confirmarLote");

        verificarCamposComunes(resultado);
        assertTrue(resultado.contenido().contains("<fecha>2026-11-11</fecha>"));
    }

    @Test
    void construyeXmlParaConfirmarDirecto() {
        RecaudacionRequest request = peticion("CONFIRMAR_DIRECTO");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "confirmarDirecto");

        verificarCamposComunes(resultado);
        assertTrue(resultado.contenido().contains("<fecha>2026-11-11</fecha>"));
        assertTrue(resultado.contenido().contains("<idTransaccion>TRANS001</idTransaccion>"));
        assertTrue(resultado.contenido().contains("<idProducto>PROD001</idProducto>"));
    }

    @Test
    void construyeXmlParaConsultarLote() {
        RecaudacionRequest request = peticion("CONSULTAR_LOTE");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "consultarLote");

        verificarCamposComunes(resultado);
    }

    @Test
    void construyeXmlParaReimprimirTransaccion() {
        RecaudacionRequest request = peticion("REIMPRIMIR_TRANSACCION");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "reimprimirTransaccion");

        verificarCamposComunes(resultado);
        assertTrue(resultado.contenido().contains("<idLote>LOTE001</idLote>"));
    }

    @Test
    void construyeXmlParaAnularTransaccion() {
        RecaudacionRequest request = peticion("ANULAR_TRANSACCION");

        PeticionSalida resultado = codec.construir(request);

        verificarSobreSoap(resultado, "anularTransaccion");

        verificarCamposComunes(resultado);
    }

    @Test
    void construyeXmlConContentTypeCorrecto() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        PeticionSalida resultado = codec.construir(request);

        assertEquals("text/xml; charset=utf-8", resultado.contentType());
    }

    @Test
    void construyeXmlOmitiendoCamposNulosOVacios() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO", Map.of(
                Canonico.ID_AGENTE, "",
                Canonico.ID_LOCACION, " ",
                Canonico.CAJA, "CAJA01"));

        PeticionSalida resultado = codec.construir(request);

        assertFalse(resultado.contenido().contains("<idAgente>"));
        assertFalse(resultado.contenido().contains("<idLocacion>"));
        assertTrue(resultado.contenido().contains("<caja>CAJA01</caja>"));
    }

    @Test
    void lanzaExcepcionCuandoElOperadorNoEstaSoportado() {
        RecaudacionRequest request = peticion("CONSULTA_DEUDA");

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> codec.construir(request));
    }

    @Test
    void interpretaRespuestaSoapCorrectamente() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        String xml = """
                <Envelope>
                    <Body>
                        <buscarProductoResponse>
                            <response>
                                <idAgente>AGENTE_RESPUESTA</idAgente>
                                <idProducto>PRODUCTO_RESPUESTA</idProducto>
                            </response>
                        </buscarProductoResponse>
                    </Body>
                </Envelope>
                """;

        RecaudacionResponse resultado = codec.interpretar(
                new RespuestaCruda(200, xml),
                request);

        assertEquals(
                "AGENTE_RESPUESTA",
                resultado.cliente().get(Canonico.ID_AGENTE));

        assertEquals(1, resultado.transacciones().size());

        assertEquals(
                "PRODUCTO_RESPUESTA",
                resultado.transacciones().get(0).get(Canonico.ID_PRODUCTO));
    }

    @Test
    void interpretaRespuestaConCamposVacios() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        String xml = """
                <Envelope>
                    <Body>
                        <buscarProductoResponse>
                            <response>
                                <idAgente></idAgente>
                                <idProducto></idProducto>
                            </response>
                        </buscarProductoResponse>
                    </Body>
                </Envelope>
                """;

        RecaudacionResponse resultado = codec.interpretar(
                new RespuestaCruda(200, xml),
                request);

        assertTrue(resultado.cliente().isEmpty());
        assertTrue(resultado.transacciones().isEmpty());
    }

    @Test
    void interpretaRespuestaSinDatos() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        String xml = """
                <Envelope>
                    <Body>
                        <buscarProductoResponse>
                            <response/>
                        </buscarProductoResponse>
                    </Body>
                </Envelope>
                """;

        RecaudacionResponse resultado = codec.interpretar(
                new RespuestaCruda(200, xml),
                request);

        assertTrue(resultado.cliente().isEmpty());
        assertTrue(resultado.transacciones().isEmpty());
    }

    @Test
    void interpretaRespuestaConDatosSoloDelCliente() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        String xml = """
                <Envelope>
                    <Body>
                        <buscarProductoResponse>
                            <response>
                                <idAgente>AGENTE001</idAgente>
                            </response>
                        </buscarProductoResponse>
                    </Body>
                </Envelope>
                """;

        RecaudacionResponse resultado = codec.interpretar(
                new RespuestaCruda(200, xml),
                request);

        assertEquals("AGENTE001", resultado.cliente().get(Canonico.ID_AGENTE));
        assertTrue(resultado.transacciones().isEmpty());
    }

    @Test
    void interpretaRespuestaConDatosSoloDeTransaccion() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        String xml = """
                <Envelope>
                    <Body>
                        <buscarProductoResponse>
                            <response>
                                <idProducto>PROD001</idProducto>
                            </response>
                        </buscarProductoResponse>
                    </Body>
                </Envelope>
                """;

        RecaudacionResponse resultado = codec.interpretar(
                new RespuestaCruda(200, xml),
                request);

        assertTrue(resultado.cliente().isEmpty());
        assertEquals(1, resultado.transacciones().size());
        assertEquals("PROD001", resultado.transacciones().get(0).get(Canonico.ID_PRODUCTO));
    }

    @Test
    void lanzaExcepcionCuandoLaRespuestaXmlEsInvalida() {
        RecaudacionRequest request = peticion("BUSCAR_PRODUCTO");

        RespuestaCruda respuesta = new RespuestaCruda(
                200,
                "<Envelope><Body><respuesta></Body></Envelope>");

        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> codec.interpretar(respuesta, request));

        assertTrue(excepcion.getMessage().contains("Respuesta inválida de WESTERN_UNION"));
    }

    @Test
    void lanzaExcepcionCuandoElOperadorNoEstaSoportadoAlInterpretar() {
        RecaudacionRequest request = peticion("CONSULTA_DEUDA");

        RespuestaCruda respuesta = new RespuestaCruda(200, "<Envelope/>");

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> codec.interpretar(respuesta, request));
    }

    private void verificarSobreSoap(PeticionSalida resultado, String operacion) {
        String xml = resultado.contenido();

        assertTrue(xml.contains("<soapenv:Envelope"));
        assertTrue(xml.contains("xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""));
        assertTrue(xml.contains("xmlns:gat=\"http://gcr.sepsa.com.ar/wsdl/GatewayService/\""));
        assertTrue(xml.contains("<soapenv:Header/>"));
        assertTrue(xml.contains("<soapenv:Body>"));
        assertTrue(xml.contains("<gat:" + operacion + ">"));
        assertTrue(xml.contains("</gat:" + operacion + ">"));
        assertTrue(xml.contains("</soapenv:Body>"));
        assertTrue(xml.contains("</soapenv:Envelope>"));
    }

    private void verificarCamposComunes(PeticionSalida resultado) {
        String xml = resultado.contenido();

        assertTrue(xml.contains("<idAgente>AGENTE01</idAgente>"));
        assertTrue(xml.contains("<idLocacion>LOCACION01</idLocacion>"));
        assertTrue(xml.contains("<caja>CAJA01</caja>"));
        assertTrue(xml.contains("<operador>OP01</operador>"));
    }

    private RecaudacionRequest peticion(String operador) {
        return peticion(operador, datosCompletos());
    }

    private RecaudacionRequest peticion(
            String operador,
            Map<String, Object> datos) {

        return new RecaudacionRequest(
                Institucion.WESTERN_UNION,
                Operador.valueOf(operador),
                Cabecera.generar(),
                new Datos(datos, datos, null));
    }

    private Map<String, Object> datosCompletos() {
        Map<String, Object> datos = new HashMap<>();

        datos.put(Canonico.ID_AGENTE, "AGENTE01");
        datos.put(Canonico.ID_LOCACION, "LOCACION01");
        datos.put(Canonico.CAJA, "CAJA01");
        datos.put(Canonico.CODIGO_OPERACION, "OP01");
        datos.put(Canonico.ID_TRANSACCION, "TRANS001");
        datos.put(Canonico.ID_PRODUCTO, "PROD001");
        datos.put(Canonico.FECHA, "2026-11-11");
        datos.put(Canonico.ID_LOTE, "LOTE001");

        return datos;
    }
}
