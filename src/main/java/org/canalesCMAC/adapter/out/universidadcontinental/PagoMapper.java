package org.canalesCMAC.adapter.out.universidadcontinental;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse.Respuesta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PagoMapper {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static String generarPeticion(RecaudacionRequest peticion)
            throws JsonProcessingException {

        Map<String, Object> request = new LinkedHashMap<>();

        poner(request, "CodigoContrato", peticion, "CodigoContrato");
        poner(request, "NumeroTrace", peticion, "NumeroTrace");
        poner(request, "Fecha", peticion, "Fecha");
        poner(request, "Canal", peticion, "Canal");

        Map<String, Object> datos = new LinkedHashMap<>();

        poner(datos, "FechaCreación", peticion, "FechaCreación");
        poner(datos, "FechaVencimiento", peticion, "FechaVencimiento");
        poner(datos, "Moneda", peticion, "Moneda");
        poner(datos, "Monto", peticion, "Monto");
        poner(datos, "NumeroDocumento", peticion, "NumeroDocumento");
        poner(datos, "Periodo", peticion, "Periodo");

        if (!datos.isEmpty()) {
            request.put("Datos", List.of(datos));
        }

        return objectMapper.writeValueAsString(request);
    }

    public static Respuesta generarRespuesta(RespuestaCruda respuesta) throws JsonMappingException, JsonProcessingException{
        JsonNode root = objectMapper.readTree(respuesta.cuerpo());

        Map<String, Object> datos =
            new LinkedHashMap<>();

        datos.put("mensaje", root.path("Mensaje").asText());
        datos.put("codigoContrato", root.path("CodigoContrato").asText());
        datos.put("cliente", root.path("Cliente").asText());

        return new Respuesta("00", "Transacción exitosa", datos, null);
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
