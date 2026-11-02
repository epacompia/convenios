package org.canalesCMAC.adapter.in.rest;

import java.time.LocalDate;

import org.canalesCMAC.application.ConciliacionService;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.infrastructure.persistence.RecaudacionConciliacion;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/conciliacion")
public class ConciliacionResource {

    @Inject
    ConciliacionService servicio;

    @POST
    @Path("/{fecha}/{institucion}")
    @Produces(MediaType.APPLICATION_JSON)
    public RecaudacionConciliacion cerrar(@PathParam("fecha") String fecha,
                                          @PathParam("institucion") String institucion) {
        try {
            return servicio.cerrar(LocalDate.parse(fecha), Institucion.desde(institucion));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Parámetros de conciliación inválidos", e);
        }
    }
}
