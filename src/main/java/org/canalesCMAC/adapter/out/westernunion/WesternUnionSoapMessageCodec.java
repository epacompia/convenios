package org.canalesCMAC.adapter.out.westernunion;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@ApplicationScoped
public class WesternUnionSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG =
        Logger.getLogger(WesternUnionSoapMessageCodec.class);

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private static final String SOAP_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String SERVICE_NS =
        "http://gcr.sepsa.com.ar/wsdl/GatewayService/";

    @Override
    public Institucion institucion() {
        return Institucion.WESTERN_UNION;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {

        LOG.debugf(
            "construyendo SOAP SEPSA operador=%s",
            peticion.operador()
        );

        Map<String, String> campos = campos(peticion);

        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"")
            .append(SOAP_NS)
            .append("\"")
            .append(" xmlns:ns2=\"")
            .append(SERVICE_NS)
            .append("\">");

        xml.append("<soapenv:Header/>");

        xml.append("<soapenv:Body>");

        xml.append("<ns2:buscarProducto>");

        xml.append("<request>");

        campos.forEach((campo, valor) -> {
            if (valor != null) {
                xml.append("<")
                    .append(campo)
                    .append(">");

                xml.append(escapar(valor));

                xml.append("</")
                    .append(campo)
                    .append(">");
            }
        });

        xml.append("</request>");

        xml.append("</ns2:buscarProducto>");

        xml.append("</soapenv:Body>");

        xml.append("</soapenv:Envelope>");

        return new PeticionSalida(
            xml.toString(),
            CONTENT_TYPE
        );
    }

    private Map<String, String> campos(
        RecaudacionRequest peticion
    ) {

        Map<String, String> campos = new LinkedHashMap<>();

        poner(
            campos,
            "idAgente",
            peticion,
            "idAgente"
        );

        poner(
            campos,
            "idLocacion",
            peticion,
            "idLocacion"
        );

        poner(
            campos,
            "caja",
            peticion,
            "caja"
        );

        poner(
            campos,
            "operador",
            peticion,
            "operador"
        );

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
            campos.put(
                nombre,
                valor.toString()
            );
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
    public RecaudacionResponse interpretar(
        RespuestaCruda respuesta,
        RecaudacionRequest peticion
    ) {

        XmlMapper xmlMapper = new XmlMapper();

        String codigoRespuesta = "";
        String codigoAutorizacion = "";

        try {

            JsonNode root =
                xmlMapper.readTree(respuesta.cuerpo());

            JsonNode returnNode = root
                .path("Body")
                .path("buscarProductoResponse")
                .path("return");

            codigoRespuesta =
                returnNode.path("codigoRespuesta").asText();

            codigoAutorizacion =
                returnNode.path("codigoAutorizacion").asText();

            LOG.debugf(
                "respuesta SEPSA codigoRespuesta=%s",
                codigoRespuesta
            );

        } catch (JsonProcessingException e) {

            LOG.errorf(
                "respuesta SEPSA inválida: %s",
                e.getMessage()
            );
        }

        return new RecaudacionResponse(
            Institucion.WESTERN_UNION,
            peticion.operador(),
            codigoRespuesta,
            codigoAutorizacion,
            Map.of()
        );
    }
}
