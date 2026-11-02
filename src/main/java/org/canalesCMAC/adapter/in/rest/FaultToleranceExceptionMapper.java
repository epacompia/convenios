package org.canalesCMAC.adapter.in.rest;

import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FaultToleranceExceptionMapper
    implements ExceptionMapper<FaultToleranceException> {

    private static final Logger LOG =
        Logger.getLogger(FaultToleranceExceptionMapper.class);

    @Inject
    SolicitudContexto contexto;

    @Override
    public Response toResponse(FaultToleranceException excepcion) {
        int status = excepcion instanceof TimeoutException
            ? Response.Status.GATEWAY_TIMEOUT.getStatusCode()
            : excepcion instanceof CircuitBreakerOpenException
                ? Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
                : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        LOG.errorf("fallo de resiliencia status=%d mensaje=%s", status, excepcion.getMessage());
        return Response.status(status)
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
