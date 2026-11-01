package org.canalesCMAC.adapter.out.claro;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@ApplicationScoped
public class ClaroSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(ClaroSoapMessageCodec.class);
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private static final String SOAP_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String SERVICE_NS =
        "http://servicio.interfazST.webservice.novatronic.com";

    private static final String XSD_NS =
        "http://entidades.interfazST.webservice.novatronic.com/xsd";

    @Override
    public Institucion institucion() {
        return Institucion.CLARO;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo SOAP CLARO operador=%s", peticion.operador());
        Map<String, String> campos = campos(peticion);
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
            .append(" xmlns:ser=\"").append(SERVICE_NS).append("\"")
            .append(" xmlns:xsd=\"").append(XSD_NS).append("\">");

        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<ser:pago>");
        xml.append("<ser:pagoRequest>");

        campos.forEach((campo, valor) -> {
                if (valor != null) {
                    xml.append("<xsd:").append(campo).append(">");
                    xml.append(escapar(valor));
                    xml.append("</xsd:").append(campo).append(">");
                }
            }
        );

        xml.append("</ser:pagoRequest>");
        xml.append("</ser:pago>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");

        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    private Map<String, String> campos(RecaudacionRequest peticion) {

        Map<String, String> campos = new LinkedHashMap<>();

        poner(campos, "binAdquiriente", peticion, "binAdquiriente");
        poner(campos, "binAdquirienteReenvia", peticion, "binAdquirienteReenvia");
        poner(campos, "canal", peticion, Canonico.CANAL);
        poner(campos, "codigoMoneda", peticion, "codigoMoneda");
        poner(campos, "fechaCaptura", peticion, "fechaCaptura");
        poner(campos, "fechaTransaccion", peticion, Canonico.FECHA_OPERACION);
        poner(campos, "mac", peticion, "mac");
        poner(campos, "nombreComercio", peticion, "nombreComercio");
        poner(campos, "numeroComercio", peticion, "numeroComercio");
        poner(campos, "numeroReferencia", peticion, "numeroReferencia");
        poner(campos, "numeroTerminal", peticion, "numeroTerminal");
        poner(campos, "trace", peticion, Canonico.IDENTIFICADOR_TRANSACCION);
        poner(campos, "numeroSuministro", peticion, Canonico.NUMERO_SUMINISTRO);
        poner(campos, "cuentaDestino", peticion, "cuentaDestino");
        poner(campos, "cuentaOrigen", peticion, "cuentaOrigen");
        poner(campos, "fechaExpiracionTarjeta", peticion, "fechaExpiracionTarjeta");
        poner(campos, "numeroTarjeta", peticion, "numeroTarjeta");
        poner(campos, "pinblock", peticion, "pinblock");
        poner(campos, "track2Data", peticion, "track2Data");
        poner(campos, "acreedor", peticion, "acreedor");
        poner(campos, "agencia", peticion, Canonico.AGENCIA);
        poner(campos, "ciudad", peticion, "ciudad");
        poner(campos, "codigoFormato", peticion, "codigoFormato");
        poner(campos, "codigoMedioPago", peticion, "codigoMedioPago");
        poner(campos, "codigoMonedaPago", peticion, "codigoMonedaPago");
        poner(campos, "datosTransaccion", peticion, "datosTransaccion");
        poner(campos, "importePago", peticion, Canonico.MONTO);
        poner(campos, "numeroIdentificacionDeudor",
            peticion, "numeroIdentificacionDeudor");
        poner(campos, "numeroProductosPagados",
            peticion, "numeroProductosPagados");
        poner(campos, "pagoTotal", peticion, "pagoTotal");
        poner(campos, "plaza", peticion, "plaza");
        poner(campos, "procesador", peticion, "procesador");
        poner(campos, "tipoCambio", peticion, "tipoCambio");
        poner(campos, "tipoIdentificacionDeudor",
            peticion, "tipoIdentificacionDeudor");

        return campos;
    }

    private void poner(
        Map<String, String> campos,
        String nombre,
        RecaudacionRequest peticion,
        String clave
    ) {
        Object valor = peticion.datos().get(clave);

        if (valor != null && !valor.toString().isBlank()) {
            campos.put(nombre, valor.toString());
        }
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
        XmlMapper xmlMapper = new XmlMapper();
        String codigoRespuesta = "";
        String codigoAutorizacion = "";
        try {
            JsonNode root = xmlMapper.readTree(respuesta.cuerpo());
            JsonNode returnNode = root
            .path("Body")
            .path("pagoResponse")
            .path("return");
            codigoRespuesta =
                returnNode.path("codigoRespuesta").asText();
            codigoAutorizacion =
                returnNode.path("codigoAutorizacion").asText();
            LOG.debugf("respuesta CLARO codigoRespuesta=%s", codigoRespuesta);
        } catch (JsonProcessingException e) {
            LOG.errorf("respuesta CLARO inválida: %s", e.getMessage());
        }

        Map<String, Object> cliente = Mapas.nuevo();
        Map<String, Object> transaccion = Mapas.nuevo();
        Mapas.poner(transaccion, Canonico.CODIGO_APROBACION, codigoAutorizacion);
        String mensaje = "00".equals(codigoRespuesta) ? "OK" : codigoRespuesta;

        return RecaudacionResponse.de(
            Institucion.CLARO,
            peticion.operador(),
            peticion.cabecera(),
            codigoRespuesta,
            mensaje,
            cliente,
            transaccion.isEmpty() ? List.of() : List.of(transaccion)
        );
    }
}
