package org.canalesCMAC.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilidades para construir mapas de request/response sin incluir nulos.
 */
public final class Mapas {

    private Mapas() {
    }

    public static Map<String, Object> nuevo() {
        return new LinkedHashMap<>();
    }

    public static void poner(Map<String, Object> destino, String clave, Object valor) {
        if (valor != null) {
            destino.put(clave, valor);
        }
    }

    public static void ponerTexto(Map<String, Object> destino, String clave, String valor) {
        if (valor != null && !valor.isBlank()) {
            destino.put(clave, valor.trim());
        }
    }
}
