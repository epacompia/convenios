package org.canalesCMAC.adapter.in.rest;

import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProveedorExceptionMapper implements ExceptionMapper<ProveedorException> {

    private static final Logger LOG = Logger.getLogger(ProveedorExceptionMapper.class);

    @Inject
    SolicitudContexto contexto;

    @Override
    public Response toResponse(ProveedorException excepcion) {
        LOG.errorf("fallo de proveedor status=%d mensaje=%s", excepcion.status(), excepcion.getMessage());
        return Response.status(excepcion.status())
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
