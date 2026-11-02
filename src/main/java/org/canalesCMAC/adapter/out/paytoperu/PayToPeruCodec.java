package org.canalesCMAC.adapter.out.paytoperu;

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
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PayToPeruCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(PayToPeruCodec.class);
    private static final String CONTENT_TYPE = "application/json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Institucion institucion() {
        return Institucion.PAYTOPERU;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo REST PAYTOPERU operador=%s", peticion.operador());
        Map<String, Object> request = new LinkedHashMap<>();
        String ruta;
        switch (peticion.operador()) {
            case CONSULTA_PAGO -> {
                Mapas.poner(request, "ncodigo_pago", peticion.datos().get(Canonico.CODIGO_PAGO));
                Mapas.poner(request, "cnro_documento", peticion.datos().get(Canonico.DOCUMENTO_CLIENTE));
                ruta = "/consulta_pago";
            }
            case NOTIFICACION_PAGO -> {
                Mapas.poner(request, "ncodigo_pago", peticion.datos().get(Canonico.CODIGO_PAGO));
                Mapas.poner(request, "ccanal", peticion.datos().get(Canonico.CANAL));
                Mapas.poner(request, "cubicacion_canal",
                    peticion.datos().get(Canonico.UBICACION_CANAL));
                Mapas.poner(request, "nimporte", peticion.datos().get(Canonico.MONTO));
                Mapas.poner(request, "nmoneda", peticion.datos().get(Canonico.MONEDA));
                Mapas.poner(request, "dfecha", peticion.datos().get(Canonico.FECHA_OPERACION));
                ruta = "/notificacion_pago";
            }
            case CONFIRMACION_PAGO -> {
                Mapas.poner(request, "ncodigo_pago", peticion.datos().get(Canonico.CODIGO_PAGO));
                ruta = "/confirmacion_pago";
            }
            case CONSULTA_TRANSACCIONES -> {
                Mapas.poner(request, "dfecha", peticion.datos().get(Canonico.FECHA_OPERACION));
                ruta = "/consulta_transacciones";
            }
            default -> throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por PAYTOPERU");
        }
        try {
            return new PeticionSalida(objectMapper.writeValueAsString(request), ruta, CONTENT_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando request de PayToPeru", e);
        }
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        try {
            JsonNode root = objectMapper.readTree(respuesta.cuerpo());
            if (root.isArray()) {
                List<Map<String, Object>> transacciones = new java.util.ArrayList<>();
                for (JsonNode item : root) {
                    transacciones.add(transaccion(item));
                }
                JsonNode primero = root.isEmpty() ? null : root.get(0);
                return RecaudacionResponse.de(
                    Institucion.PAYTOPERU,
                    peticion.operador(),
                    peticion.cabecera(),
                    "00",
                    "",
                    primero == null ? Map.of() : cliente(primero),
                    transacciones
                );
            }

            if (!root.isObject()
                || (!root.has("berror") && !root.has("cmensaje") && !root.has("ncodigo_pago"))) {
                throw new ProveedorException(
                    "Respuesta de PAYTOPERU sin estructura reconocible",
                    ProveedorException.BAD_GATEWAY, false, null);
            }
            String codigo = root.path("berror").asBoolean() ? "01" : "00";
            String mensaje = texto(root, "cmensaje");
            return RecaudacionResponse.de(
                Institucion.PAYTOPERU,
                peticion.operador(),
                peticion.cabecera(),
                codigo,
                mensaje,
                cliente(root),
                List.of(transaccion(root))
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error deserializando respuesta de PayToPeru", e);
        }
    }

    private Map<String, Object> cliente(JsonNode root) {
        Map<String, Object> cliente = Mapas.nuevo();
        Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, unir(
            texto(root, "cnombres"), texto(root, "capellidos")));
        Mapas.poner(cliente, Canonico.DOCUMENTO_CLIENTE, texto(root, "cnro_documento"));
        Mapas.poner(cliente, Canonico.EMAIL, texto(root, "cemail"));
        return cliente;
    }

    private Map<String, Object> transaccion(JsonNode root) {
        Map<String, Object> transaccion = Mapas.nuevo();
        Mapas.poner(transaccion, Canonico.NUM_OPERACION, numero(root, "ncodigo_pago"));
        Mapas.poner(transaccion, Canonico.MONTO, numero(root, "nimporte"));
        Mapas.poner(transaccion, Canonico.MONEDA, texto(root, "nmoneda"));
        Mapas.poner(transaccion, Canonico.GLOSA, texto(root, "cconcepto"));
        Mapas.poner(transaccion, Canonico.CANAL, texto(root, "ccanal"));
        Mapas.poner(transaccion, Canonico.FECHA_OPERACION, texto(root, "dfecha"));
        Mapas.poner(transaccion, Canonico.UBICACION_CANAL, texto(root, "cubicacion_canal"));
        Mapas.poner(transaccion, Canonico.CLAVE, texto(root, "cclave"));
        Mapas.poner(transaccion, Canonico.OBSERVACION, texto(root, "cobservacion"));
        return transaccion;
    }

    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()
            ? nodo.get(campo).asText().trim()
            : null;
    }

    private static Object numero(JsonNode nodo, String campo) {
        if (!nodo.hasNonNull(campo)) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        return valor.isNumber() ? valor.decimalValue() : valor.asText();
    }

    private static String unir(String primero, String segundo) {
        if (primero == null || primero.isBlank()) {
            return segundo;
        }
        if (segundo == null || segundo.isBlank()) {
            return primero;
        }
        return primero + " " + segundo;
    }
}
