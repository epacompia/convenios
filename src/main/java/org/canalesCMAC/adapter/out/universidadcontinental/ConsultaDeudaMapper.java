package org.canalesCMAC.adapter.out.universidadcontinental;

import java.util.LinkedHashMap;
import java.util.Map;

import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse.Respuesta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConsultaDeudaMapper {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static String generarPeticion(RecaudacionRequest peticion) 
        throws JsonProcessingException{
        Map<String, Object> request = new LinkedHashMap<>();

        poner(request,"codigocontrato",peticion,"codigocontrato");
        poner(request,"moneda",peticion,"moneda");
        poner(request,"canal",peticion,"canal");
        return objectMapper.writeValueAsString(request);
    }

    public static Respuesta generarRespuesta(RespuestaCruda respuesta) throws JsonMappingException, JsonProcessingException{
        JsonNode root = objectMapper.readTree(respuesta.cuerpo());

        Map<String, Object> datos =
            new LinkedHashMap<>();

        datos.put("mensaje", root.path("Mensaje").asText());
        datos.put("codigoContrato", root.path("CodigoContrato").asText());
        datos.put("cliente", root.path("Cliente").asText());

        return new Respuesta("00", "Consulta exitosa", datos, null);
    }

    public static void poner(
        Map<String, Object> request,
        String nombre,
        RecaudacionRequest peticion,
        String clave) {

        Object valor = peticion.datos().get(clave);

        if (valor != null) {
            request.put(nombre, valor);
        }
    }
}
