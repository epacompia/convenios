package org.canalesCMAC.infrastructure.persistence;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Enmascara datos sensibles antes de persistir o cachear payloads.
 */
@ApplicationScoped
public class SanitizadorDatos {

    private static final Set<String> SENSIBLES = Set.of(
        "numeroTarjeta", "numero_tarjeta", "pinblock", "track2Data", "track2data",
        "mac", "claveTpv", "clave_tpv", "pin", "cvv", "cvv2", "clave");

    private static final String MASCARA = "****";

    @Inject
    ObjectMapper objectMapper;

    public String sanitizar(Object valor) {
        if (valor == null) {
            return "{}";
        }
        try {
            JsonNode nodo = objectMapper.valueToTree(valor);
            enmascarar(nodo);
            return objectMapper.writeValueAsString(nodo);
        } catch (Exception e) {
            return "{\"error\":\"no se pudo serializar el payload\"}";
        }
    }

    public String sanitizarJsonTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            JsonNode nodo = objectMapper.readTree(texto);
            enmascarar(nodo);
            return objectMapper.writeValueAsString(nodo);
        } catch (Exception e) {
            return null;
        }
    }

    private void enmascarar(JsonNode nodo) {
        if (nodo == null) {
            return;
        }
        if (nodo.isArray()) {
            for (JsonNode hijo : (ArrayNode) nodo) {
                enmascarar(hijo);
            }
            return;
        }
        if (!nodo.isObject()) {
            return;
        }
        ObjectNode objeto = (ObjectNode) nodo;
        Iterator<Map.Entry<String, JsonNode>> campos = objeto.fields();
        while (campos.hasNext()) {
            Map.Entry<String, JsonNode> campo = campos.next();
            if (SENSIBLES.contains(campo.getKey())) {
                objeto.put(campo.getKey(), MASCARA);
            } else {
                enmascarar(campo.getValue());
            }
        }
    }
}
