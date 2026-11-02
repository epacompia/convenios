package org.canalesCMAC.adapter.in.rest;

import org.canalesCMAC.domain.exception.OperacionEnProcesoException;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OperacionEnProcesoExceptionMapper
    implements ExceptionMapper<OperacionEnProcesoException> {

    private static final Logger LOG =
        Logger.getLogger(OperacionEnProcesoExceptionMapper.class);

    @Inject
    SolicitudContexto contexto;

    @Override
    public Response toResponse(OperacionEnProcesoException excepcion) {
        LOG.warnf("operacion en proceso mensaje=%s", excepcion.getMessage());
        return Response.status(Response.Status.CONFLICT)
            .entity(error(excepcion.getMessage()))
            .build();
    }

    private RecaudacionResponse error(String mensaje) {
        if (contexto == null) {
            return RecaudacionResponse.error(null, null, Cabecera.generar(), mensaje);
        }
        return RecaudacionResponse.error(contexto.institucion(), contexto.operador(),
            contexto.cabecera(), mensaje);
    }
}
