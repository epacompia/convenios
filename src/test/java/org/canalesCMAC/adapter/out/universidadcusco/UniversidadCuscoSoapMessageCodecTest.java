
package org.canalesCMAC.adapter.out.universidadcusco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

class UniversidadCuscoSoapMessageCodecTest {

    private UniversidadCuscoSoapMessageCodec codec;

    @BeforeEach
    void setUp() {
        codec = new UniversidadCuscoSoapMessageCodec();
    }

    // =======================================================
    // CONSTRUCCION DE PETICIONES SOAP
    // =======================================================

    @Test
    void construyeXmlParaOperacionAutoExtorno() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.AUTO_EXTORNO,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);
        assertEquals("text/xml; charset=utf-8", salida.contentType());

        assertTrue(salida.contenido().contains("<uac:AutoExtorno>"));
        assertTrue(salida.contenido().contains("</uac:AutoExtorno>"));

        assertTrue(salida.contenido().contains(
            "<numeroReferencialDeuda>REF-DEUDA-001</numeroReferencialDeuda>"
        ));

        assertTrue(salida.contenido().contains(
            "<numeroOperacionRecaudacion>OP-001</numeroOperacionRecaudacion>"
        ));
    }

    @Test
    void construyeXmlParaOperacionConsultaDeuda() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);

        assertTrue(salida.contenido().contains("<uac:ConsultarDeuda>"));
        assertTrue(salida.contenido().contains("</uac:ConsultarDeuda>"));

        assertTrue(salida.contenido().contains(
            "<numeroReferencialDeuda>REF-DEUDA-001</numeroReferencialDeuda>"
        ));
    }

    @Test
    void construyeXmlParaOperacionConsultaDeudaOrdinario() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA_ORDINARIO,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);

        assertTrue(salida.contenido().contains(
            "<uac:ConsultarDeudaOrdinario>"
        ));

        assertTrue(salida.contenido().contains(
            "</uac:ConsultarDeudaOrdinario>"
        ));

        assertTrue(salida.contenido().contains(
            "<tipoDocumentoCliente>DNI</tipoDocumentoCliente>"
        ));

        assertTrue(salida.contenido().contains(
            "<numeroDocumentoCliente>12345678</numeroDocumentoCliente>"
        ));

        assertTrue(salida.contenido().contains(
            "<codigoConcepto>CON001</codigoConcepto>"
        ));
    }

    @Test
    void construyeXmlParaOperacionNotificacionPago() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.NOTIFICACION_PAGO,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);

        assertTrue(salida.contenido().contains("<uac:NotificarPago>"));
        assertTrue(salida.contenido().contains("</uac:NotificarPago>"));

        assertTrue(salida.contenido().contains(
            "<numeroReferencialDeuda>REF-DEUDA-001</numeroReferencialDeuda>"
        ));

        assertTrue(salida.contenido().contains(
            "<numeroDocumento>12345678</numeroDocumento>"
        ));

        assertTrue(salida.contenido().contains(
            "<montoImporteDeudaPagada>150.50</montoImporteDeudaPagada>"
        ));

        assertTrue(salida.contenido().contains(
            "<formaPago>EFECTIVO</formaPago>"
        ));

        assertTrue(salida.contenido().contains(
            "<codigoMoneda>PEN</codigoMoneda>"
        ));
    }

    @Test
    void construyeXmlParaOperacionNotificacionPagoOrdinario() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.NOTIFICACION_PAGO_ORDINARIO,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertNotNull(salida);

        assertTrue(salida.contenido().contains(
            "<uac:NotificarPagoOrdinario>"
        ));

        assertTrue(salida.contenido().contains(
            "</uac:NotificarPagoOrdinario>"
        ));

        assertTrue(salida.contenido().contains(
            "<tipoDocumentoCliente>DNI</tipoDocumentoCliente>"
        ));

        assertTrue(salida.contenido().contains(
            "<numeroDocumentoCliente>12345678</numeroDocumentoCliente>"
        ));

        assertTrue(salida.contenido().contains(
            "<codigoConcepto>CON001</codigoConcepto>"
        ));
    }

    // =======================================================
    // VALIDACION DE ESTRUCTURA SOAP
    // =======================================================

    @Test
    void construyeXmlConEnvelopeHeaderYBody() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertTrue(salida.contenido().contains("<soapenv:Envelope"));
        assertTrue(salida.contenido().contains("<soapenv:Header/>"));
        assertTrue(salida.contenido().contains("<soapenv:Body>"));

        assertTrue(salida.contenido().contains("<uac:recaudosRq>"));
        assertTrue(salida.contenido().contains("<uac:cabecera>"));
        assertTrue(salida.contenido().contains("<uac:operacion>"));
        assertTrue(salida.contenido().contains("<uac:detalle>"));
        assertTrue(salida.contenido().contains("<uac:transaccion>"));

        assertTrue(salida.contenido().contains("</soapenv:Envelope>"));
    }

    @Test
    void construyeXmlConDatosDeCabecera() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        PeticionSalida salida = codec.construir(peticion);

        assertTrue(salida.contenido().contains(
            "<uac:codigoOperacion>COD001</uac:codigoOperacion>"
        ));

        assertTrue(salida.contenido().contains(
            "<uac:numeroOperacion>NUM001</uac:numeroOperacion>"
        ));

        assertTrue(salida.contenido().contains(
            "<uac:codigoBanco>BAN001</uac:codigoBanco>"
        ));

        assertTrue(salida.contenido().contains(
            "<uac:codigoConvenio>CONV001</uac:codigoConvenio>"
        ));

        assertTrue(salida.contenido().contains(
            "<uac:canalOperacion>APP</uac:canalOperacion>"
        ));

        assertTrue(salida.contenido().contains(
            "<uac:codigoOficina>OFI001</uac:codigoOficina>"
        ));

        assertTrue(salida.contenido().contains(
            "<uac:fechaOperacion>2026-11-10</uac:fechaOperacion>"
        ));

        assertTrue(salida.contenido().contains(
            "<uac:horaOperacion>15:30:00</uac:horaOperacion>"
        ));
    }

    @Test
    void omiteCamposNulosOVaciosAlConstruirXml() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            Map.of(
                Canonico.CODIGO_OPERACION, "COD001",
                Canonico.NUMERO_REFERENCIAL_DEUDA, "REF001",
                Canonico.CODIGO_BANCO, ""
            )
        );

        PeticionSalida salida = codec.construir(peticion);

        assertTrue(salida.contenido().contains(
            "<uac:codigoOperacion>COD001</uac:codigoOperacion>"
        ));

        assertTrue(salida.contenido().contains(
            "<numeroReferencialDeuda>REF001</numeroReferencialDeuda>"
        ));

        assertFalse(salida.contenido().contains("<uac:codigoBanco>"));
        assertFalse(salida.contenido().contains("<uac:numeroOperacion>"));
    }

    // =======================================================
    // INTERPRETACION DE RESPUESTAS SOAP
    // =======================================================

    @Test
    void interpretaRespuestaSoapConVariosDocumentos() {

        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope
                xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                xmlns:uac="http://www.uac.com.pe">

                <soapenv:Body>
                    <uac:ConsultarDeudaResponse>
                        <recaudosRs>
                            <detalle>
                                <transaccion>
                                    <listaDocumentos>
                                        <numero>DOC001</numero>
                                        <descripcion>Deuda mensual</descripcion>
                                        <fechaEmision>2026-01-01</fechaEmision>
                                        <fechaVencimiento>2026-11-30</fechaVencimiento>
                                        <importeDeuda>150.50</importeDeuda>
                                        <importeDeudaMinima>50.00</importeDeudaMinima>
                                    </listaDocumentos>
                                    <listaDocumentos>
                                        <numero>DOC002</numero>
                                        <descripcion>Deuda adicional</descripcion>
                                        <fechaEmision>2026-02-01</fechaEmision>
                                        <fechaVencimiento>2026-12-30</fechaVencimiento>
                                        <importeDeuda>200.00</importeDeuda>
                                        <importeDeudaMinima>100.00</importeDeudaMinima>
                                    </listaDocumentos>
                                    <tipoDocumentoCliente>DNI</tipoDocumentoCliente>
                                    <numeroDocumentoCliente>12345678</numeroDocumentoCliente>
                                    <nombreCliente>JUAN PEREZ</nombreCliente>
                                    <numeroReferencialDeuda>REF-DEUDA-001</numeroReferencialDeuda>
                                    <numeroOperacionEmpresa>OP-001</numeroOperacionEmpresa>
                                </transaccion>
                            </detalle>
                        </recaudosRs>
                    </uac:ConsultarDeudaResponse>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(
            respuesta,
            peticion
        );

        assertNotNull(resultado);

        assertEquals(Institucion.UNIVCUSCO, resultado.institucion());
        assertEquals(Operador.CONSULTA_DEUDA, resultado.operador());

        assertNotNull(resultado.cliente());
        assertEquals("DNI", resultado.cliente().get(Canonico.TIPO_DOCUMENTO_CLIENTE));
        assertEquals("12345678", resultado.cliente().get(Canonico.DOCUMENTO_CLIENTE));
        assertEquals("JUAN PEREZ", resultado.cliente().get(Canonico.NOMBRE_CLIENTE));

        assertNotNull(resultado.transacciones());
        assertEquals(1, resultado.transacciones().size());

        Map<String, Object> transaccion = resultado.transacciones().get(0);

        assertEquals("REF-DEUDA-001",
            transaccion.get(Canonico.NUMERO_REFERENCIAL_DEUDA));

        assertEquals("OP-001",
            transaccion.get(Canonico.NUMERO_OPERACION));

        assertEquals("DOC002",
            transaccion.get(Canonico.NUMERO_COMPROBANTE));

        assertEquals("Deuda adicional",
            transaccion.get(Canonico.DESCRIPCION));

        assertEquals("2026-02-01",
            transaccion.get(Canonico.FECHA_EMISION));

        assertEquals("2026-12-30",
            transaccion.get(Canonico.FECHA_VENCIMIENTO));

        assertEquals("200.00",
            transaccion.get(Canonico.IMPORTE_ORIGEN));

        assertEquals("100.00",
            transaccion.get(Canonico.IMPORTE_DEUDA_MINIMA));
    }

    @Test
    void interpretaRespuestaSoapConUnSoloDocumento() {

        String xml = """
            <Envelope>
                <Body>
                    <ConsultarDeudaResponse>
                        <recaudosRs>
                            <detalle>
                                <transaccion>
                                    <listaDocumentos>
                                        <documento>
                                            <numero>DOC001</numero>
                                            <descripcion>Deuda universitaria</descripcion>
                                            <fechaEmision>2026-01-01</fechaEmision>
                                            <fechaVencimiento>2026-12-31</fechaVencimiento>
                                            <importeDeuda>350.00</importeDeuda>
                                            <importeDeudaMinima>100.00</importeDeudaMinima>
                                        </documento>
                                    </listaDocumentos>
                                    <tipoDocumentoCliente>DNI</tipoDocumentoCliente>
                                    <numeroDocumentoCliente>12345678</numeroDocumentoCliente>
                                    <nombreCliente>JUAN PEREZ</nombreCliente>
                                    <numeroReferencialDeuda>REF001</numeroReferencialDeuda>
                                    <numeroOperacionEmpresa>OP001</numeroOperacionEmpresa>
                                </transaccion>
                            </detalle>
                        </recaudosRs>
                    </ConsultarDeudaResponse>
                </Body>
            </Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(
            respuesta,
            peticion
        );

        assertNotNull(resultado);
        assertEquals(1, resultado.transacciones().size());

        Map<String, Object> transaccion = resultado.transacciones().get(0);

        assertEquals("DOC001",
            transaccion.get(Canonico.NUMERO_COMPROBANTE));

        assertEquals("Deuda universitaria",
            transaccion.get(Canonico.DESCRIPCION));

        assertEquals("350.00",
            transaccion.get(Canonico.IMPORTE_ORIGEN));

        assertEquals("100.00",
            transaccion.get(Canonico.IMPORTE_DEUDA_MINIMA));
    }

    @Test
    void interpretaRespuestaSoapSinDocumentos() {

        String xml = """
            <Envelope>
                <Body>
                    <ConsultarDeudaResponse>
                        <recaudosRs>
                            <detalle>
                                <transaccion>
                                    <tipoDocumentoCliente>DNI</tipoDocumentoCliente>
                                    <numeroDocumentoCliente>12345678</numeroDocumentoCliente>
                                    <nombreCliente>JUAN PEREZ</nombreCliente>
                                    <numeroReferencialDeuda>REF001</numeroReferencialDeuda>
                                    <numeroOperacionEmpresa>OP001</numeroOperacionEmpresa>
                                </transaccion>
                            </detalle>
                        </recaudosRs>
                    </ConsultarDeudaResponse>
                </Body>
            </Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(
            respuesta,
            peticion
        );

        assertNotNull(resultado);

        assertEquals("JUAN PEREZ",
            resultado.cliente().get(Canonico.NOMBRE_CLIENTE));

        assertEquals(1, resultado.transacciones().size());

        Map<String, Object> transaccion = resultado.transacciones().get(0);

        assertEquals("REF001",
            transaccion.get(Canonico.NUMERO_REFERENCIAL_DEUDA));

        assertEquals("OP001",
            transaccion.get(Canonico.NUMERO_OPERACION));

        assertFalse(transaccion.containsKey(Canonico.NUMERO_COMPROBANTE));
    }

    @Test
    void interpretaRespuestaSoapSinDatosDeClienteNiTransaccion() {

        String xml = """
            <Envelope>
                <Body>
                    <ConsultarDeudaResponse>
                        <recaudosRs>
                            <detalle>
                                <transaccion/>
                            </detalle>
                        </recaudosRs>
                    </ConsultarDeudaResponse>
                </Body>
            </Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(
            respuesta,
            peticion
        );

        assertNotNull(resultado);
        assertTrue(resultado.cliente().isEmpty());
        assertTrue(resultado.transacciones().isEmpty());
    }

    @Test
    void interpretaRespuestaSoapConCamposVacios() {

        String xml = """
            <Envelope>
                <Body>
                    <ConsultarDeudaResponse>
                        <recaudosRs>
                            <detalle>
                                <transaccion>
                                    <tipoDocumentoCliente></tipoDocumentoCliente>
                                    <numeroDocumentoCliente></numeroDocumentoCliente>
                                    <nombreCliente>JUAN PEREZ</nombreCliente>
                                    <numeroReferencialDeuda></numeroReferencialDeuda>
                                    <numeroOperacionEmpresa>OP001</numeroOperacionEmpresa>
                                </transaccion>
                            </detalle>
                        </recaudosRs>
                    </ConsultarDeudaResponse>
                </Body>
            </Envelope>
            """;

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(200, xml);

        RecaudacionResponse resultado = codec.interpretar(
            respuesta,
            peticion
        );

        assertNotNull(resultado);

        assertFalse(resultado.cliente().containsKey(
            Canonico.TIPO_DOCUMENTO_CLIENTE
        ));

        assertFalse(resultado.cliente().containsKey(
            Canonico.DOCUMENTO_CLIENTE
        ));

        assertEquals("JUAN PEREZ",
            resultado.cliente().get(Canonico.NOMBRE_CLIENTE));

        assertEquals(1, resultado.transacciones().size());

        Map<String, Object> transaccion = resultado.transacciones().get(0);

        assertFalse(transaccion.containsKey(
            Canonico.NUMERO_REFERENCIAL_DEUDA
        ));

        assertEquals("OP001",
            transaccion.get(Canonico.NUMERO_OPERACION));
    }

    @Test
    void lanzaExcepcionCuandoLaRespuestaSoapEsInvalida() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.CONSULTA_DEUDA,
            datosCompletos()
        );

        RespuestaCruda respuesta = new RespuestaCruda(
            500,
            "<Envelope><Body><ConsultarDeudaResponse>"
        );

        IllegalStateException excepcion = assertThrows(
            IllegalStateException.class,
            () -> codec.interpretar(respuesta, peticion)
        );

        assertTrue(
            excepcion.getMessage().contains("Respuesta inválida")
        );
    }

    // =======================================================
    // VALIDACION DE OPERACIONES
    // =======================================================

    @Test
    void obtieneExcepcionCuandoElOperadorNoEstaSoportado() {

        RecaudacionRequest peticion = crearPeticion(
            Operador.VENTA,
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
            Institucion.UNIVCUSCO,
            operador,
            Cabecera.generar(),
            new Datos(datos, datos, null)
        );
    }

    private Map<String, Object> datosCompletos() {
        return Map.ofEntries(
            Map.entry(Canonico.CODIGO_OPERACION, "COD001"),
            Map.entry(Canonico.NUMERO_OPERACION, "NUM001"),
            Map.entry(Canonico.CODIGO_BANCO, "BAN001"),
            Map.entry(Canonico.CODIGO_CONVENIO, "CONV001"),
            Map.entry(Canonico.CANAL, "APP"),
            Map.entry(Canonico.CODIGO_OFICINA, "OFI001"),
            Map.entry(Canonico.FECHA_OPERACION, "2026-11-10"),
            Map.entry(Canonico.HORA_OPERACION, "15:30:00"),
            Map.entry(Canonico.NUMERO_REFERENCIAL_DEUDA, "REF-DEUDA-001"),
            Map.entry(Canonico.NUMERO_OPERACION_RECAUDACION, "OP-001"),
            Map.entry(Canonico.TIPO_DOCUMENTO_CLIENTE, "DNI"),
            Map.entry(Canonico.DOCUMENTO_CLIENTE, "12345678"),
            Map.entry(Canonico.CODIGO_CONCEPTO, "CON001"),
            Map.entry(Canonico.IMPORTE_ORIGEN, "150.50"),
            Map.entry(Canonico.FORMA_PAGO, "EFECTIVO"),
            Map.entry(Canonico.MONEDA, "PEN")
        );
    }
}
