package org.canalesCMAC.adapter.in.rest;

import org.canalesCMAC.application.RecaudacionUseCase;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/operaciones")
@Tag(name = "Recaudación", description = "Contrato único para los convenios de recaudación")
public class OperacionesResource {

    @Inject
    RecaudacionUseCase useCase;

    @Inject
    SolicitudContexto contexto;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Ejecuta una operación de recaudación",
        description = "Recibe el contrato único agrupado en filtro, cliente y transacción. "
            + "La obligatoriedad de campos depende del convenio y del operador."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Operación procesada",
            content = @Content(schema = @Schema(implementation = RecaudacionResponse.class))),
        @APIResponse(responseCode = "400", description = "Solicitud inválida"),
        @APIResponse(responseCode = "409", description = "idTransaccion en proceso o reutilizado"),
        @APIResponse(responseCode = "502", description = "Error del proveedor"),
        @APIResponse(responseCode = "503", description = "Proveedor saturado o circuito abierto"),
        @APIResponse(responseCode = "504", description = "Timeout del proveedor")
    })
    public RecaudacionResponse procesar(
        @RequestBody(required = true, description = "Contrato único de recaudación",
            content = @Content(schema = @Schema(implementation = RecaudacionRequest.class)))
        RecaudacionRequest solicitud) {
        contexto.establecer(solicitud);
        return useCase.procesar(solicitud);
    }
}
