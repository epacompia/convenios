package org.canalesCMAC.adapter.in.rest;

import org.canalesCMAC.application.RecaudacionUseCase;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/operaciones")
public class OperacionesResource {

    @Inject
    RecaudacionUseCase useCase;

    @Inject
    SolicitudContexto contexto;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RecaudacionResponse procesar(RecaudacionRequest solicitud) {
        contexto.establecer(solicitud);
        return useCase.procesar(solicitud);
    }
}
