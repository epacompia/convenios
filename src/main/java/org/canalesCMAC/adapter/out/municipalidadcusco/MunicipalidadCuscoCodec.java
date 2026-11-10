package org.canalesCMAC.adapter.out.municipalidadcusco;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MunicipalidadCuscoCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(MunicipalidadCuscoCodec.class);
    private static final String CONTENT_TYPE = "application/json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Institucion institucion() {
        return Institucion.MUNICIPALIDAD_CUSCO;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo REST MUNICIPALIDAD_CUSCO operador=%s", peticion.operador());
        Map<String, Object> request = new LinkedHashMap<>();
        String ruta;
        switch (peticion.operador()) {
            case RECUPERAR_ORDEN_PAGO -> {
                Mapas.poner(request, "ordenpago", peticion.datos().get(Canonico.NUMERO_ORDEN_PAGO));
                ruta = "/recuperar_orden_pago";
            }
            case VALIDAR_ORDEN_PAGO -> {
                Mapas.poner(request, "ordenpago", peticion.datos().get(Canonico.NUMERO_ORDEN_PAGO));
                Mapas.poner(request, "codcontribuyente",
                    peticion.datos().get(Canonico.CODIGO_CONTRIBUYENTE));
                Mapas.poner(request, "monto", peticion.datos().get(Canonico.MONTO));
                ruta = "/validar_orden_pago";
            }
            case ACTUALIZAR_ORDEN_PAGO -> {
                Mapas.poner(request, "ordenpago", peticion.datos().get(Canonico.NUMERO_ORDEN_PAGO));
                Mapas.poner(request, "nrooperacion", peticion.datos().get(Canonico.NUM_OPERACION));
                ruta = "/actualizar_orden_pago";
            }
            default -> throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por MUNICIPALIDAD_CUSCO");
        }
        return salida(request, ruta);
    }

    private PeticionSalida salida(Map<String, Object> request, String ruta) {
        try {
            return new PeticionSalida(objectMapper.writeValueAsString(request), ruta, CONTENT_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "Error serializando request de la Municipalidad de Cusco", e);
        }
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        try {
            JsonNode root = objectMapper.readTree(respuesta.cuerpo());
            String status = root.path("status").asText();
            String message = root.path("message").asText();
            JsonNode data = root.path("data");

            Map<String, Object> cliente = Mapas.nuevo();
            Map<String, Object> transaccion = Mapas.nuevo();
            if (data.isArray() && !data.isEmpty()) {
                JsonNode primera = data.get(0);
                Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, texto(primera, "contribuyente"));
                Mapas.poner(cliente, Canonico.CODIGO_CONTRIBUYENTE,
                    texto(primera, "codcontribuyente"));
                Mapas.poner(transaccion, Canonico.NUMERO_COMPROBANTE, texto(primera, "ordenpago"));
                Mapas.poner(transaccion, Canonico.MONTO, numero(primera, "monto"));
                Mapas.poner(transaccion, Canonico.ESTADO, texto(primera, "validacion"));
                Mapas.poner(transaccion, Canonico.GLOSA, texto(primera, "descripcion"));
                Map<String, Object> adicionales = Mapas.nuevo();
                adicionales.put("data", convertirData(data));
                transaccion.put(Canonico.DATOS_ADICIONALES, adicionales);
            } else if (data.isTextual() && !data.asText().isBlank()) {
                Mapas.poner(transaccion, Canonico.GLOSA, data.asText().trim());
            }

            return RecaudacionResponse.de(
                Institucion.MUNICIPALIDAD_CUSCO,
                peticion.operador(),
                peticion.cabecera(),
                normalizarCodigo(status),
                message,
                cliente,
                transaccion.isEmpty() ? List.of() : List.of(transaccion)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "Error deserializando respuesta de la Municipalidad de Cusco", e);
        }
    }

    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()
            ? nodo.get(campo).asText().trim()
            : null;
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "99";
        }
        return switch (codigo.trim().toLowerCase()) {
            case "ok", "00" -> "00";
            case "error" -> "01";
            default -> codigo;
        };
    }

    private static Object numero(JsonNode nodo, String campo) {
        if (!nodo.hasNonNull(campo)) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        return valor.isNumber() ? valor.decimalValue() : valor.asText();
    }

    private List<Map<String, Object>> convertirData(JsonNode data) {
        if (!data.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(
            data,
            objectMapper.getTypeFactory()
                .constructCollectionType(List.class, Map.class)
        );
    }
}
