package org.canalesCMAC.adapter.out.westernunion;

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
public class WesternUnionSoapMessageCodec implements MapeadorOperacion {

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String GAT_NS ="http://gcr.sepsa.com.ar/wsdl/GatewayService/";

    @Override
    public Institucion institucion() {
         return Institucion.WESTERN_UNION;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        WesternUnionTag westernUnionTag = obtenerOperacion(operacion);
        StringBuilder xml = new StringBuilder();

        xml.append("<soapenv:Envelope")
           .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
           .append(" xmlns:gat=\"").append(GAT_NS).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<gat:").append(westernUnionTag.getTagOperacion()).append(">");

        if("BUSCAR_PRODUCTO".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION);                        
        } else if("CONSULTAR_TRANSACCION".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION);  
        } else if("CONSULTAR_DATOS_ADICIONALES".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION);  
            poner(xml, "idTransaccion", peticion, Canonico.ID_TRANSACCION); 
            poner(xml, "idProducto", peticion, Canonico.ID_PRODUCTO); 
        } else if("AGREGAR_DATOS_ADICIONALES".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION); 
            poner(xml, "fecha", peticion, Canonico.FECHA);   
            poner(xml, "idTransaccion", peticion, Canonico.ID_TRANSACCION); 
            poner(xml, "idProducto", peticion, Canonico.ID_PRODUCTO); 
        } else if("CONFIRMAR_LOTE".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION); 
            poner(xml, "fecha", peticion, Canonico.FECHA);  
        } else if("CONFIRMAR_DIRECTO".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION); 
            poner(xml, "fecha", peticion, Canonico.FECHA);   
            poner(xml, "idTransaccion", peticion, Canonico.ID_TRANSACCION); 
            poner(xml, "idProducto", peticion, Canonico.ID_PRODUCTO); 
        } else if("CONSULTAR_LOTE".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION);   
        } else if("REIMPRIMIR_TRANSACCION".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION);   
            poner(xml, "idLote", peticion, Canonico.ID_LOTE);
        } else if("ANULAR_TRANSACCION".equals(operacion)){
            poner(xml, "idAgente", peticion, Canonico.ID_AGENTE);
            poner(xml, "idLocacion", peticion, Canonico.ID_LOCACION);
            poner(xml, "caja", peticion, Canonico.CAJA);
            poner(xml, "operador", peticion, Canonico.CODIGO_OPERACION);                        
        } else {
            throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por " + institucion());            
        }

        xml.append("</gat:").append(westernUnionTag.getTagOperacion()).append(">");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");

        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        WesternUnionTag westernUnionTag = obtenerOperacion(operacion);
        XmlMapper xmlMapper = new XmlMapper();
        String codigo = "00";
        String mensaje = "";
        Map<String, Object> cliente = Mapas.nuevo();
        Map<String, Object> transaccion = Mapas.nuevo();
        try {
            JsonNode root = xmlMapper.readTree(respuesta.cuerpo());
            JsonNode response = root
                .path("Body")
                .path(westernUnionTag.getTagResponse())
                .path("response");
    
            Mapas.poner(cliente, Canonico.ID_AGENTE, texto(response, "idAgente"));    
            Mapas.poner(transaccion, Canonico.ID_PRODUCTO, texto(response, "idProducto"));             
              
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
    
    private WesternUnionTag obtenerOperacion(String operador) {
        return switch (operador) {
            case "BUSCAR_PRODUCTO" -> WesternUnionTag.BUSCAR_PRODUCTO;
            case "CONSULTAR_TRANSACCION" -> WesternUnionTag.CONSULTAR_TRANSACCION;
            case "CONSULTAR_DATOS_ADICIONALES" -> WesternUnionTag.CONSULTAR_DATOS_ADICIONALES;
            case "AGREGAR_DATOS_ADICIONALES" -> WesternUnionTag.AGREGAR_DATOS_ADICIONALES;
            case "CONFIRMAR_LOTE" -> WesternUnionTag.CONFIRMAR_LOTE;
            case "CONFIRMAR_DIRECTO" -> WesternUnionTag.CONFIRMAR_DIRECTO;
            case "CONSULTAR_LOTE" -> WesternUnionTag.CONSULTAR_LOTE;
            case "REIMPRIMIR_TRANSACCION" -> WesternUnionTag.REIMPRIMIR_TRANSACCION;
            case "ANULAR_TRANSACCION" -> WesternUnionTag.ANULAR_TRANSACCION;
            default -> throw new OperadorNoSoportadoException("Operación SOAP no soportada: " + operador);
        };
    }
    private enum WesternUnionTag {
        BUSCAR_PRODUCTO(
            "buscarProducto",
            "buscarProductoResponse"),
        CONSULTAR_TRANSACCION(
            "consultarTransaccion",
            "consultarTransaccionResponse"),
        CONSULTAR_DATOS_ADICIONALES(
            "consultarDatosAdicionales",
            "consultarDatosAdicionalesResponse"),
        AGREGAR_DATOS_ADICIONALES(
            "agregarDatosAdicionales",
            "agregarDatosAdicionalesResponse"),
        CONFIRMAR_LOTE(
            "confirmarLote",
            "confirmarLoteResponse"),
        CONFIRMAR_DIRECTO(
            "confirmarDirecto",
            "confirmarDirectoResponse"),
        CONSULTAR_LOTE(
            "consultarLote",
            "consultarLoteResponse"),
        REIMPRIMIR_TRANSACCION(
            "reimprimirTransaccion",
            "reimprimirTransaccionResponse"),
        ANULAR_TRANSACCION(
            "anularTransaccion",
            "anularTransaccionResponse");

        private final String tagOperacion;
        private final String tagResponse;

        WesternUnionTag(String tagOperacion, String tagResponse) {
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
