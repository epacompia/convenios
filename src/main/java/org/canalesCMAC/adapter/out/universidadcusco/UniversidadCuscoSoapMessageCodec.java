package org.canalesCMAC.adapter.out.universidadcusco;

import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UniversidadCuscoSoapMessageCodec implements MapeadorOperacion{

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String UAC_NS ="http://www.uac.com.pe";
    
    @Override
    public Institucion institucion() {
         return Institucion.UNIVCUSCO;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        UniversidadCuscoTag universidadCuscoTag = obtenerOperacion(operacion);
        StringBuilder xml = new StringBuilder();

        xml.append("<soapenv:Envelope")
           .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
           .append(" xmlns:uac=\"").append(UAC_NS).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<uac:").append(universidadCuscoTag.getTagOperacionRequest()).append(">");
        xml.append("<uac:recaudosRq>");
        xml.append("<uac:cabecera>");
        xml.append("<uac:operacion>");
        
        poner(xml, "uac:codigoOperacion", peticion, Canonico.CODIGO_OPERACION);
        poner(xml, "uac:numeroOperacion", peticion, Canonico.NUMERO_OPERACION);
        poner(xml, "uac:codigoBanco", peticion, Canonico.CODIGO_BANCO);
        poner(xml, "uac:codigoConvenio", peticion, Canonico.CODIGO_CONVENIO);
        poner(xml, "uac:canalOperacion", peticion, Canonico.CANAL);
        poner(xml, "uac:codigoOficina", peticion, Canonico.CODIGO_OFICINA);
        poner(xml, "uac:fechaOperacion", peticion, Canonico.FECHA_OPERACION);
        poner(xml, "uac:horaOperacion", peticion, Canonico.HORA_OPERACION);
        
        xml.append("</uac:operacion>");        
        xml.append("</uac:cabecera>");
        xml.append("<uac:detalle>");
        xml.append("<uac:transaccion>");

        if("AUTO_EXTORNO".equals(operacion)){
            poner(xml, "numeroReferencialDeuda", peticion, Canonico.NUMERO_REFERENCIAL_DEUDA);
            poner(xml, "numeroOperacionRecaudacion", peticion, Canonico.NUMERO_OPERACION_RECAUDACION);
        } else if("CONSULTA_DEUDA".equals(operacion)){
            poner(xml, "numeroReferencialDeuda", peticion, Canonico.NUMERO_REFERENCIAL_DEUDA);
        } else if("CONSULTA_DEUDA_ORDINARIO".equals(operacion)){
            poner(xml, "tipoDocumentoCliente", peticion, Canonico.TIPO_DOCUMENTO_CLIENTE);
            poner(xml, "numeroDocumentoCliente", peticion, Canonico.DOCUMENTO_CLIENTE);
            poner(xml, "codigoConcepto", peticion, Canonico.CODIGO_CONCEPTO);
        } else if("NOTIFICACION_PAGO".equals(operacion)){
            poner(xml, "numeroReferencialDeuda", peticion, Canonico.NUMERO_REFERENCIAL_DEUDA);
            poner(xml, "numeroDocumento", peticion, Canonico.DOCUMENTO_CLIENTE);
            poner(xml, "montoImporteDeudaPagada", peticion, Canonico.IMPORTE_ORIGEN);
            poner(xml, "numeroOperacionRecaudacion", peticion, Canonico.NUMERO_OPERACION_RECAUDACION);
            poner(xml, "formaPago", peticion, Canonico.FORMA_PAGO);
            poner(xml, "codigoMoneda", peticion, Canonico.MONEDA);
            poner(xml, "codigoEmpresa", peticion, Canonico.CODIGO_CONVENIO);
        } else if("NOTIFICACION_PAGO_ORDINARIO".equals(operacion)){
            poner(xml, "tipoDocumentoCliente", peticion, Canonico.TIPO_DOCUMENTO_CLIENTE);
            poner(xml, "numeroDocumentoCliente", peticion, Canonico.DOCUMENTO_CLIENTE);
            poner(xml, "codigoConcepto", peticion, Canonico.CODIGO_CONCEPTO);
            poner(xml, "montoImporteDeudaPagada", peticion, Canonico.IMPORTE_ORIGEN);
            poner(xml, "numeroOperacionRecaudacion", peticion, Canonico.NUMERO_OPERACION_RECAUDACION);
            poner(xml, "formaPago", peticion, Canonico.FORMA_PAGO);
            poner(xml, "codigoMoneda", peticion, Canonico.MONEDA);
            poner(xml, "codigoEmpresa", peticion, Canonico.CODIGO_CONVENIO);
        } else {
            throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por " + institucion());            
        }

        xml.append("</uac:transaccion>");
        xml.append("</uac:detalle>");
        xml.append("</uac:recaudosRq>");
        xml.append("</uac:").append(universidadCuscoTag.getTagOperacionRequest()).append(">");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");

        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        UniversidadCuscoTag universidadCuscoTag = obtenerOperacion(operacion);
        XmlMapper xmlMapper = new XmlMapper();
        String codigo = "00";
        String mensaje = "";
        Map<String, Object> cliente = Mapas.nuevo();
        Map<String, Object> transaccion = Mapas.nuevo();

        try {
            JsonNode root = xmlMapper.readTree(respuesta.cuerpo());
            JsonNode detalleNodo = root
                .path("Body")
                .path(universidadCuscoTag.getTagOperacionResponse())
                .path("recaudosRs")
                .path("detalle")
                .path("transaccion");
            JsonNode listaDocumentosNodo = detalleNodo.path("listaDocumentos");
            if (listaDocumentosNodo != null) {
                if(listaDocumentosNodo.isArray()){
                    for (JsonNode documentoNodo : listaDocumentosNodo) {
                        Mapas.poner(transaccion, Canonico.NUMERO_COMPROBANTE, texto(documentoNodo, "numero"));
                        Mapas.poner(transaccion, Canonico.DESCRIPCION, texto(documentoNodo, "descripcion"));
                        Mapas.poner(transaccion, Canonico.FECHA_EMISION, texto(documentoNodo, "fechaEmision"));
                        Mapas.poner(transaccion, Canonico.FECHA_VENCIMIENTO, texto(documentoNodo, "fechaVencimiento"));
                        Mapas.poner(transaccion, Canonico.IMPORTE_ORIGEN, texto(documentoNodo, "importeDeuda"));
                        Mapas.poner(transaccion, Canonico.IMPORTE_DEUDA_MINIMA, texto(documentoNodo, "importeDeudaMinima"));
                    }
                }else{
                    JsonNode documentoNodo = listaDocumentosNodo.path("documento");
                    Mapas.poner(transaccion, Canonico.NUMERO_COMPROBANTE, texto(documentoNodo, "numero"));
                    Mapas.poner(transaccion, Canonico.DESCRIPCION, texto(documentoNodo, "descripcion"));
                    Mapas.poner(transaccion, Canonico.FECHA_EMISION, texto(documentoNodo, "fechaEmision"));
                    Mapas.poner(transaccion, Canonico.FECHA_VENCIMIENTO, texto(documentoNodo, "fechaVencimiento"));
                    Mapas.poner(transaccion, Canonico.IMPORTE_ORIGEN, texto(documentoNodo, "importeDeuda"));
                    Mapas.poner(transaccion, Canonico.IMPORTE_DEUDA_MINIMA, texto(documentoNodo, "importeDeudaMinima"));
                }
            }
            Log.info(":::::" + detalleNodo);
            Mapas.poner(cliente, Canonico.TIPO_DOCUMENTO_CLIENTE, texto(detalleNodo, "tipoDocumentoCliente")); 
            Mapas.poner(cliente, Canonico.DOCUMENTO_CLIENTE, texto(detalleNodo, "numeroDocumentoCliente")); 
            Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, texto(detalleNodo, "nombreCliente"));  
            Mapas.poner(transaccion, Canonico.NUMERO_REFERENCIAL_DEUDA, texto(detalleNodo, "numeroReferencialDeuda"));
            Mapas.poner(transaccion, Canonico.NUMERO_OPERACION, texto(detalleNodo, "numeroOperacionEmpresa"));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta inválida de " + institucion(), e);
        }        
        return RecaudacionResponse.de(institucion(), peticion.operador(), peticion.cabecera(),
            codigo, mensaje, cliente,
            transaccion.isEmpty() ? List.of() : List.of(transaccion));
    }


    private void poner(StringBuilder xml, String nombre,
                       RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor != null && !valor.toString().isBlank()) {
            xml.append("<").append(nombre).append(">");
            xml.append(valor);
            xml.append("</").append(nombre).append(">");
        }
    } 
    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()
            ? nodo.get(campo).asText().trim()
            : null;
    }
    
    private UniversidadCuscoTag obtenerOperacion(String operador) {
        return switch (operador) {
            case "AUTO_EXTORNO" -> UniversidadCuscoTag.AUTO_EXTORNO;
            case "CONSULTA_DEUDA" -> UniversidadCuscoTag.CONSULTA_DEUDA;
            case "CONSULTA_DEUDA_ORDINARIO" -> UniversidadCuscoTag.CONSULTA_DEUDA_ORDINARIO;
            case "NOTIFICACION_PAGO" -> UniversidadCuscoTag.NOTIFICACION_PAGO;
            case "NOTIFICACION_PAGO_ORDINARIO" -> UniversidadCuscoTag.NOTIFICACION_PAGO_ORDINARIO;
            default -> throw new OperadorNoSoportadoException("Operación SOAP no soportada: " + operador);
        };
    }

    private enum UniversidadCuscoTag {
        AUTO_EXTORNO(
            "AutoExtorno", 
            "AutoExtornoResponse"),
        CONSULTA_DEUDA(
            "ConsultarDeuda", 
            "ConsultarDeudaResponse"),
        CONSULTA_DEUDA_ORDINARIO(
            "ConsultarDeudaOrdinario",
            "ConsultarDeudaOrdinarioResponse"),
        NOTIFICACION_PAGO(
            "NotificarPago",
            "NotificarPagoResponse"),
        NOTIFICACION_PAGO_ORDINARIO(
            "NotificarPagoOrdinario",
            "NotificarPagoOrdinarioResponse");

        private final String tagOperacionRequest;
        private final String tagOperacionResponse;

        UniversidadCuscoTag(String tagOperacionRequest, String tagOperacionResponse) {
            this.tagOperacionRequest = tagOperacionRequest;
            this.tagOperacionResponse = tagOperacionResponse;
        }

        public String getTagOperacionRequest() {
            return tagOperacionRequest;
        }

        public String getTagOperacionResponse() {
            return tagOperacionResponse;
        }
    }
    
}
