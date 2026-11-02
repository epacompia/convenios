package org.canalesCMAC.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Contrato único de salida para los 10 convenios.
 *
 * <pre>
 * {
 *   "institucion": "SEAL",
 *   "operador": "CONSULTA_DEUDA",
 *   "cabecera": { ... },
 *   "respuesta": {
 *     "codigo": "00",
 *     "mensaje": "...",
 *     "cliente": { ... },
 *     "transaccion": [ { ... } ]
 *   }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RecaudacionResponse", description = "Contrato único de salida de recaudación")
public record RecaudacionResponse(
    Institucion institucion,
    Operador operador,
    Cabecera cabecera,
    Respuesta respuesta) {

    public RecaudacionResponse {
        respuesta = respuesta == null
            ? new Respuesta("ER", "", Map.of(), List.of())
            : respuesta;
    }

    public static RecaudacionResponse de(
        Institucion institucion,
        Operador operador,
        Cabecera cabecera,
        String codigo,
        String mensaje,
        Map<String, Object> cliente,
        List<Map<String, Object>> transaccion) {
        return new RecaudacionResponse(institucion, operador, cabecera,
            new Respuesta(codigo, mensaje, cliente, transaccion));
    }

    public static RecaudacionResponse ok(
        Institucion institucion,
        Operador operador,
        Cabecera cabecera,
        Map<String, Object> cliente,
        List<Map<String, Object>> transaccion) {
        return de(institucion, operador, cabecera, "00", "OK", cliente, transaccion);
    }

    public static RecaudacionResponse error(
        Institucion institucion,
        Operador operador,
        Cabecera cabecera,
        String mensaje) {
        return de(institucion, operador, cabecera, "ER", mensaje, Map.of(), List.of());
    }

    @JsonIgnore
    public String codigo() {
        return respuesta.codigo();
    }

    @JsonIgnore
    public String mensaje() {
        return respuesta.mensaje();
    }

    @JsonIgnore
    public Map<String, Object> cliente() {
        return respuesta.cliente();
    }

    @JsonIgnore
    public List<Map<String, Object>> transacciones() {
        return respuesta.transaccion();
    }

    /**
     * Vista plana de compatibilidad: cliente + primera transacción.
     */
    @JsonIgnore
    public Map<String, Object> datos() {
        Map<String, Object> datos = new LinkedHashMap<>();
        if (respuesta.cliente() != null) {
            datos.putAll(respuesta.cliente());
        }
        if (respuesta.transaccion() != null && !respuesta.transaccion().isEmpty()) {
            Map<String, Object> primera = respuesta.transaccion().get(0);
            if (primera != null) {
                datos.putAll(primera);
            }
        }
        return datos;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Respuesta(
        String codigo,
        String mensaje,
        Map<String, Object> cliente,
        List<Map<String, Object>> transaccion) {

        public Respuesta {
            cliente = cliente == null ? Map.of() : cliente;
            transaccion = transaccion == null ? List.of() : transaccion;
        }
    }
}
