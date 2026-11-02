package org.canalesCMAC.adapter.in.rest;

import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;

import jakarta.enterprise.context.RequestScoped;

/**
 * Contexto de la solicitud en curso para que los exception mappers puedan
 * devolver el mismo sobre (institucion, operador, cabecera) del contrato único.
 */
@RequestScoped
public class SolicitudContexto {

    private Institucion institucion;
    private Operador operador;
    private Cabecera cabecera;

    public void establecer(RecaudacionRequest solicitud) {
        if (solicitud == null) {
            return;
        }
        this.institucion = solicitud.institucion();
        this.operador = solicitud.operador();
        this.cabecera = solicitud.cabecera();
    }

    public Institucion institucion() {
        return institucion;
    }

    public Operador operador() {
        return operador;
    }

    public Cabecera cabecera() {
        return cabecera;
    }
}
