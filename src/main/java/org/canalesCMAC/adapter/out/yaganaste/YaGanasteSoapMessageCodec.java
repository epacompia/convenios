package org.canalesCMAC.adapter.out.yaganaste;

import java.util.LinkedHashMap;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;

@ApplicationScoped
public class YaGanasteSoapMessageCodec implements MapeadorOperacion {

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SIGMA_NS ="net.sigma.h2h.ws";

    @Override
    public Institucion institucion() {
         return Institucion.YAGANASTE;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        YaGanasteTag yaGanasteTag = obtenerOperacion(operacion);
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soapenv:Envelope")
           .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
           .append(" xmlns:net=\"").append(SIGMA_NS).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<net:").append(yaGanasteTag.getTagOperacion()).append(">");

        if("ECHO".equals(operacion)){
            poner(xml, "tpv", peticion, Canonico.TPV);
            poner(xml, "claveTpv", peticion, Canonico.CLAVE_TPV);
            poner(xml, "referenciaOperacion", peticion, Canonico.REFERENCIA_OPERACION);
            poner(xml, "fechaHoraOperacion", peticion, Canonico.FECHA_OPERACION);
        } else if("DEVOLUCION".equals(operacion)){
            poner(xml, "tpv", peticion, Canonico.TPV);
            poner(xml, "claveTpv", peticion, Canonico.CLAVE_TPV);
            poner(xml, "codigoProducto", peticion, Canonico.CODIGO_PRODUCTO);
            poner(xml, "referenciaOperacion", peticion, Canonico.REFERENCIA_OPERACION);
            poner(xml, "fechaHoraOperacion", peticion, Canonico.FECHA_OPERACION);
        } else if("VENTA".equals(operacion)){
            poner(xml, "tpv", peticion, Canonico.TPV);
            poner(xml, "claveTpv", peticion, Canonico.CLAVE_TPV);
            poner(xml, "codigoProducto", peticion, Canonico.CODIGO_PRODUCTO);
            poner(xml, "referenciaOperacion", peticion, Canonico.REFERENCIA_OPERACION);
            poner(xml, "fechaHoraOperacion", peticion, Canonico.FECHA_OPERACION);
        } else if("CONSULTA".equals(operacion)){
            poner(xml, "tpv", peticion, Canonico.TPV);
            poner(xml, "claveTpv", peticion, Canonico.CLAVE_TPV);
            poner(xml, "codigoProducto", peticion, Canonico.CODIGO_PRODUCTO);
            poner(xml, "referenciaOperacion", peticion, Canonico.REFERENCIA_OPERACION);
            poner(xml, "fechaHoraOperacion", peticion, Canonico.FECHA_OPERACION);
        } else if("CONSULTA_PREVIA".equals(operacion)){
            poner(xml, "tpv", peticion, Canonico.TPV);
            poner(xml, "claveTpv", peticion, Canonico.CLAVE_TPV);
            poner(xml, "codigoProducto", peticion, Canonico.CODIGO_PRODUCTO);
            poner(xml, "referenciaOperacion", peticion, Canonico.REFERENCIA_OPERACION);
            poner(xml, "fechaHoraOperacion", peticion, Canonico.FECHA_OPERACION);
        } else if("SALDO".equals(operacion)){
            poner(xml, "tpv", peticion, Canonico.TPV);
            poner(xml, "claveTpv", peticion, Canonico.CLAVE_TPV);
            poner(xml, "codigoProducto", peticion, Canonico.CODIGO_PRODUCTO);
            poner(xml, "referenciaOperacion", peticion, Canonico.REFERENCIA_OPERACION);
            poner(xml, "fechaHoraOperacion", peticion, Canonico.FECHA_OPERACION);
        } else {
            throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por " + institucion());            
        }

        xml.append("</net:").append(yaGanasteTag.getTagOperacion()).append(">");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");

        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        YaGanasteTag yaGanasteTag = obtenerOperacion(operacion);
        XmlMapper xmlMapper = new XmlMapper();
        String codigo = "00";
        String mensaje = "";
        Map<String, Object> cliente = Mapas.nuevo();
        Map<String, Object> transaccion = Mapas.nuevo();
        try {
            JsonNode root = xmlMapper.readTree(respuesta.cuerpo());
            JsonNode response = root
                .path("Body")
                .path(yaGanasteTag.getTagResponse())
                .path("response");
    
            Mapas.poner(transaccion, Canonico.REFERENCIA_OPERADOR, texto(response, "refOperador"));    
            Mapas.poner(transaccion, Canonico.IMPORTE_ORIGEN, texto(response, "importe"));  
            Mapas.poner(transaccion, "referenciaSigma", texto(response, "referenciaSigma"));
            Mapas.poner(transaccion, Canonico.SALDO, texto(response, "saldo"));              
              
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
    private YaGanasteTag obtenerOperacion(String operador) {
        return switch (operador) {
            case "ECHO" -> YaGanasteTag.ECHO;
            case "DEVOLUCION" -> YaGanasteTag.DEVOLUCION;
            case "VENTA" -> YaGanasteTag.VENTA;
            case "CONSULTA_PREVIA" -> YaGanasteTag.CONSULTA_PREVIA;
            case "CONSULTA" -> YaGanasteTag.CONSULTA;
            case "SALDO" -> YaGanasteTag.SALDO;
            default -> throw new OperadorNoSoportadoException("Operación SOAP no soportada: " + operador);
        };
    }

    private enum YaGanasteTag {
        ECHO("echo", "echoResponse"),
        DEVOLUCION("devolucion", "devolucionResponse"),
        VENTA("venta","ventaResponse"),
        CONSULTA_PREVIA("consultaPrevia","consultaPreviaResponse"),
        CONSULTA("consulta","consultaResponse"),
        SALDO("saldo","saldoResponse");

        private final String tagOperacion;
        private final String tagResponse;

        YaGanasteTag(String tagOperacion, String tagResponse) {
            this.tagOperacion = tagOperacion;
            this.tagResponse = tagResponse;
        }

        public String getTagOperacion() {
            return tagOperacion;
        }

        public String getTagResponse() {
            return tagResponse;
        }
    }
}
