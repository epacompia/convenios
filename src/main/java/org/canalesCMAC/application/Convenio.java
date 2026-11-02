package org.canalesCMAC.application;

import java.util.Set;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

public record Convenio(
    Institucion institucion,
    String protocolo,
    String url,
    Set<Operador> operadores,
    int reintentos,
    long reintentosDelay,
    long timeout) {

    public Convenio sinReintentos() {
        return new Convenio(institucion, protocolo, url, operadores, 0, reintentosDelay, timeout);
    }
}
