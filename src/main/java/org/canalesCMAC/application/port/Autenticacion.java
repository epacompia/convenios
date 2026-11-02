package org.canalesCMAC.application.port;

import java.util.Map;

public interface Autenticacion {

    Autenticacion NINGUNA = new Autenticacion() {
        @Override
        public String tipo() {
            return "ninguna";
        }

        @Override
        public Map<String, String> encabezados() {
            return Map.of();
        }
    };

    String tipo();

    Map<String, String> encabezados();
}
