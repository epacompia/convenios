package org.canalesCMAC.adapter.out.electroucayali;

import java.util.Map;

import org.canalesCMAC.application.port.Autenticacion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AutenticacionBearer implements Autenticacion {

    @Inject
    ElectroUcayaliToken token;

    @Override
    public String tipo() {
        return "bearer";
    }

    @Override
    public Map<String, String> encabezados() {
        return Map.of("Authorization", "Bearer " + token.obtener());
    }
}
