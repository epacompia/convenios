package org.canalesCMAC.adapter.out.universidadcusco;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.canalesCMAC.adapter.out.soap.NodoXml;
import org.canalesCMAC.adapter.out.soap.SoapSupport;
import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

/**
 * Convenio Universidad del Cusco (UAC) - recaudo interconectado SOAP.
 *
 * <p>Contrato tomado de {@code docs/SERVICIO DE RECAUDO INTERCONECTADO - CAJAS.docx}.
 * Operaciones: ConsultarDeuda, ConsultarDeudaOrdinario, NotificarPago,
 * NotificarPagoOrdinario y ExtornarPago.</p>
 */
@ApplicationScoped
public class UCuscoSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(UCuscoSoapMessageCodec.class);
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String UAC_NS = "http://www.uac.com.pe/";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HHmmss");

    @ConfigProperty(name = "convenio.UNIVCUSCO.codigoBanco", defaultValue = "11")
    String codigoBanco;

    @ConfigProperty(name = "convenio.UNIVCUSCO.codigoConvenio", defaultValue = "1223")
    String codigoConvenio;

    @ConfigProperty(name = "convenio.UNIVCUSCO.canal", defaultValue = "TF")
    String canal;

    @ConfigProperty(name = "convenio.UNIVCUSCO.codigoOficina", defaultValue = "1241")
    String codigoOficina;

    @ConfigProperty(name = "convenio.UNIVCUSCO.moneda", defaultValue = "PEN")
    String moneda;

    @ConfigProperty(name = "convenio.UNIVCUSCO.formaPago", defaultValue = "EF")
    String formaPago;

    @Override
    public Institucion institucion() {
        return Institucion.UNIVCUSCO;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo SOAP UNIVCUSCO operador=%s", peticion.operador());
        String operacion = operacionSoap(peticion.operador());
        String codigoOperacion = texto(peticion, Canonico.CODIGO_OPERACION,
            codigoOperacionPorDefecto(peticion.operador()));
        String numeroOperacion = texto(peticion, Canonico.NUM_OPERACION,
            ultimos(peticion.cabecera().idTransaccion(), 9));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
            .append(" xmlns:uac=\"").append(UAC_NS).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<uac:").append(operacion).append(">");
        xml.append("<uac:recaudosRq>");
        xml.append("<uac:cabecera>");
        xml.append("<uac:operacion>");
        agregar(xml, "codigoOperacion", codigoOperacion);
        agregar(xml, "numeroOperacion", numeroOperacion);
        agregar(xml, "codigoBanco", texto(peticion, Canonico.CODIGO_BANCO, codigoBanco));
        agregar(xml, "codigoConvenio", texto(peticion, Canonico.CODIGO_CONVENIO, codigoConvenio));
        agregar(xml, "canalOperacion", texto(peticion, Canonico.CANAL, canal));
        agregar(xml, "codigoOficina", texto(peticion, Canonico.CODIGO_OFICINA, codigoOficina));
        agregar(xml, "fechaOperacion", fechaCompacta(peticion, Canonico.FECHA_OPERACION));
        agregar(xml, "horaOperacion", horaCompacta(peticion));
        xml.append("</uac:operacion>");
        xml.append("</uac:cabecera>");

        xml.append("<uac:detalle>");
        xml.append("<uac:transaccion>");
        detalle(peticion, xml);
        xml.append("</uac:transaccion>");
        xml.append("</uac:detalle>");

        xml.append("</uac:recaudosRq>");
        xml.append("</uac:").append(operacion).append(">");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    private void detalle(RecaudacionRequest peticion, StringBuilder xml) {
        switch (peticion.operador()) {
            case CONSULTA_DEUDA -> agregar(xml, "numeroReferenciaDeuda",
                texto(peticion, Canonico.NUMERO_REFERENCIAL_DEUDA, null));
            case CONSULTA_DEUDA_ORDINARIO -> {
                agregar(xml, "tipoDocumentoCliente",
                    texto(peticion, Canonico.TIPO_DOCUMENTO_CLIENTE, null));
                agregar(xml, "numeroDocumentoCliente",
                    texto(peticion, Canonico.DOCUMENTO_CLIENTE, null));
                agregar(xml, "codigoConcepto",
                    texto(peticion, Canonico.CODIGO_CONCEPTO, null));
            }
            case NOTIFICACION_PAGO -> {
                agregar(xml, "numeroReferenciaDeuda",
                    texto(peticion, Canonico.NUMERO_REFERENCIAL_DEUDA, null));
                agregar(xml, "numeroDocumento",
                    texto(peticion, Canonico.DOCUMENTO_CLIENTE, null));
                agregar(xml, "importeDeudaPagada",
                    texto(peticion, Canonico.MONTO, null));
                agregar(xml, "numeroOperacionRecaudos",
                    texto(peticion, Canonico.NUMERO_OPERACION_RECAUDACION, null));
                agregar(xml, "formaPago", texto(peticion, Canonico.FORMA_PAGO, formaPago));
                agregar(xml, "codigoMoneda", texto(peticion, Canonico.MONEDA, moneda));
                agregar(xml, "datosEmpresa", texto(peticion, Canonico.DATOS_EMPRESA, null));
                agregar(xml, "otrosDatosEmpresa",
                    texto(peticion, Canonico.OTROS_DATOS_EMPRESA, null));
            }
            case NOTIFICACION_PAGO_ORDINARIO -> {
                agregar(xml, "tipoDocumentoCliente",
                    texto(peticion, Canonico.TIPO_DOCUMENTO_CLIENTE, null));
                agregar(xml, "numeroDocumentoCliente",
                    texto(peticion, Canonico.DOCUMENTO_CLIENTE, null));
                agregar(xml, "nombreCliente",
                    texto(peticion, Canonico.NOMBRE_CLIENTE, null));
                agregar(xml, "codigoConcepto",
                    texto(peticion, Canonico.CODIGO_CONCEPTO, null));
                agregar(xml, "numeroOperacionRecaudos",
                    texto(peticion, Canonico.NUMERO_OPERACION_RECAUDACION, null));
                agregar(xml, "cantidad", texto(peticion, Canonico.CANTIDAD, null));
                agregar(xml, "importeDeudaPagada",
                    texto(peticion, Canonico.MONTO, null));
                agregar(xml, "formaPago", texto(peticion, Canonico.FORMA_PAGO, formaPago));
                agregar(xml, "codigoMoneda", texto(peticion, Canonico.MONEDA, moneda));
                agregar(xml, "datosEmpresa", texto(peticion, Canonico.DATOS_EMPRESA, null));
                agregar(xml, "otrosDatosEmpresa",
                    texto(peticion, Canonico.OTROS_DATOS_EMPRESA, null));
                agregar(xml, "numeroDocumento",
                    texto(peticion, Canonico.NUMERO_COMPROBANTE, null));
            }
            case AUTO_EXTORNO -> {
                agregar(xml, "numeroReferenciaDeuda",
                    texto(peticion, Canonico.NUMERO_REFERENCIAL_DEUDA, null));
                agregar(xml, "numeroDocumento",
                    texto(peticion, Canonico.DOCUMENTO_CLIENTE, null));
                agregar(xml, "importeDeudaPagada",
                    texto(peticion, Canonico.MONTO, null));
                agregar(xml, "numeroOperacionRecaudos",
                    texto(peticion, Canonico.NUMERO_OPERACION_RECAUDACION, null));
                agregar(xml, "numeroOperacionOriginal",
                    texto(peticion, Canonico.NUMERO_OPERACION_ORIGINAL, null));
                agregar(xml, "fechaOperacionOriginal",
                    fechaCompacta(peticion, Canonico.FECHA_OPERACION_ORIGINAL));
                agregar(xml, "formaPago", texto(peticion, Canonico.FORMA_PAGO, formaPago));
                agregar(xml, "codigoMoneda", texto(peticion, Canonico.MONEDA, moneda));
                agregar(xml, "datosEmpresa", texto(peticion, Canonico.DATOS_EMPRESA, null));
                agregar(xml, "otrosDatosEmpresa",
                    texto(peticion, Canonico.OTROS_DATOS_EMPRESA, null));
            }
            default -> throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por UNIVCUSCO");
        }
    }

    private String operacionSoap(Operador operador) {
        return switch (operador) {
            case CONSULTA_DEUDA -> "ConsultarDeuda";
            case CONSULTA_DEUDA_ORDINARIO -> "ConsultarDeudaOrdinario";
            case NOTIFICACION_PAGO -> "NotificarPago";
            case NOTIFICACION_PAGO_ORDINARIO -> "NotificarPagoOrdinario";
            case AUTO_EXTORNO -> "ExtornarPago";
            default -> throw new OperadorNoSoportadoException(
                "Operador " + operador + " no soportado por UNIVCUSCO");
        };
    }

    private String codigoOperacionPorDefecto(Operador operador) {
        return operador == Operador.AUTO_EXTORNO ? "4010" : "1010";
    }

    private void agregar(StringBuilder xml, String nombre, String valor) {
        if (valor != null && !valor.isBlank()) {
            xml.append("<uac:").append(nombre).append(">");
            xml.append(escapar(valor));
            xml.append("</uac:").append(nombre).append(">");
        }
    }

    private String texto(RecaudacionRequest peticion, String clave, String porDefecto) {
        Object valor = peticion.datos().get(clave);
        return valor == null || valor.toString().isBlank() ? porDefecto : valor.toString().trim();
    }

    private String fechaCompacta(RecaudacionRequest peticion, String clave) {
        String valor = texto(peticion, clave, null);
        if (valor == null) {
            return FECHA.format(LocalDate.now());
        }
        if (valor.matches("\\d{8}")) {
            return valor;
        }
        try {
            return FECHA.format(LocalDateTime.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (RuntimeException ignored) {
            try {
                return FECHA.format(OffsetDateTime.parse(valor, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } catch (RuntimeException ignoredOffset) {
                return FECHA.format(LocalDate.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE));
            }
        }
    }

    private String horaCompacta(RecaudacionRequest peticion) {
        String hora = texto(peticion, Canonico.HORA_OPERACION, null);
        if (hora != null && hora.matches("\\d{6}")) {
            return hora;
        }
        String fecha = texto(peticion, Canonico.FECHA_OPERACION, null);
        if (fecha != null && fecha.length() >= 19) {
            try {
                return HORA.format(LocalDateTime.parse(fecha.substring(0, 19),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (RuntimeException ignored) {
                // se usa la hora actual
            }
        }
        return HORA.format(LocalDateTime.now());
    }

    private static String ultimos(String valor, int longitud) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return valor.length() <= longitud ? valor : valor.substring(valor.length() - longitud);
    }

    private String escapar(String valor) {
        return valor
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        LOG.debugf("respuesta UNIVCUSCO operador=%s", peticion.operador());
        NodoXml raiz = SoapSupport.cuerpo(respuesta.cuerpo());
        NodoXml recaudos = raiz.hijo("recaudosRs");
        if (recaudos == null) {
            recaudos = raiz.hijo("recaudosRq");
        }
        NodoXml detalle = recaudos == null ? null : recaudos.hijo("detalle");
        NodoXml respuestaNodo = detalle == null ? raiz.hijo("resultado") : detalle.hijo("respuesta");
        if (respuestaNodo == null) {
            respuestaNodo = detalle;
        }
        NodoXml transaccion = detalle == null ? null : detalle.hijo("transaccion");

        String codigo = normalizarCodigo(texto(respuestaNodo, "codigo"));
        String mensaje = texto(respuestaNodo, "descripcion");
        if (mensaje == null) {
            mensaje = texto(respuestaNodo, "mensaje");
        }

        Map<String, Object> cliente = Mapas.nuevo();
        Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, texto(transaccion, "nombreCliente"));

        Map<String, Object> tx = Mapas.nuevo();
        Mapas.poner(tx, Canonico.NUM_OPERACION, texto(transaccion, "numeroOperacionEmpresa"));
        Mapas.poner(tx, Canonico.NUMERO_COMPROBANTE, texto(transaccion, "numeroReferenciaDeuda"));
        Mapas.poner(tx, Canonico.IND_MAS_DEUDA, texto(transaccion, "indMasDeuda"));
        Mapas.poner(tx, Canonico.CANTIDAD_DOCS_DEUDA, numero(transaccion, "cantidadDocsDeuda"));
        Mapas.poner(tx, Canonico.DATOS_EMPRESA, texto(transaccion, "datosEmpresa"));

        List<Map<String, Object>> deudas = deudas(transaccion);
        if (!deudas.isEmpty()) {
            tx.put(Canonico.DEUDAS, deudas);
            Mapas.poner(tx, Canonico.MONTO, deudas.get(0).get(Canonico.MONTO));
            Mapas.poner(tx, Canonico.MONEDA, deudas.get(0).get(Canonico.MONEDA));
        }

        return RecaudacionResponse.de(Institucion.UNIVCUSCO, peticion.operador(),
            peticion.cabecera(), codigo, mensaje, cliente,
            tx.isEmpty() ? List.of() : List.of(tx));
    }

    private List<Map<String, Object>> deudas(NodoXml transaccion) {
        if (transaccion == null) {
            return List.of();
        }
        List<Map<String, Object>> deudas = new ArrayList<>();
        for (NodoXml lista : transaccion.hijos("listaDocumentos")) {
            for (NodoXml documento : lista.hijos("documento")) {
                agregarDeuda(deudas, documento);
            }
        }
        if (deudas.isEmpty()) {
            for (NodoXml documento : transaccion.hijos("documento")) {
                agregarDeuda(deudas, documento);
            }
        }
        return deudas;
    }

    private void agregarDeuda(List<Map<String, Object>> deudas, NodoXml documento) {
        Map<String, Object> deuda = Mapas.nuevo();
        Mapas.poner(deuda, Canonico.NUMERO_COMPROBANTE, texto(documento, "numero"));
        Mapas.poner(deuda, Canonico.DESCRIPCION, texto(documento, "descripcion"));
        Mapas.poner(deuda, Canonico.FECHA_EMISION, texto(documento, "fechaEmision"));
        Mapas.poner(deuda, Canonico.FECHA_VENCIMIENTO, texto(documento, "fechaVencimiento"));
        Mapas.poner(deuda, Canonico.MONTO, numero(documento, "importeDeuda"));
        Mapas.poner(deuda, Canonico.IMPORTE_DEUDA_MINIMA,
            numero(documento, "importeDeudaMinima"));
        Mapas.poner(deuda, Canonico.MONEDA, texto(documento, "moneda"));
        Mapas.poner(deuda, Canonico.INDICADOR_RESTRICCION_PAGO,
            texto(documento, "indicadorRestriccPago"));
        Mapas.poner(deuda, Canonico.CANTIDAD_SUBCONCEPTOS,
            numero(documento, "cantidadSubconceptos"));
        if (!deuda.isEmpty()) {
            deudas.add(deuda);
        }
    }

    private static String texto(NodoXml nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        String valor = nodo.textoDe(campo);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static Object numero(NodoXml nodo, String campo) {
        String valor = texto(nodo, campo);
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return valor;
        }
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "99";
        }
        return switch (codigo) {
            case "0000", "0001", "00" -> "00";
            default -> codigo;
        };
    }
}
