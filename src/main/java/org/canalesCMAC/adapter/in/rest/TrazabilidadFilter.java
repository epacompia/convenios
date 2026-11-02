package org.canalesCMAC.adapter.in.rest;

import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TrazabilidadFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(TrazabilidadFilter.class);
    private static final String CABECERA = "X-Request-Id";
    private static final String CLAVE_MDC = "recaudacion.traceId";

    @Override
    public void filter(ContainerRequestContext peticion) {
        String traceId = peticion.getHeaderString(CABECERA);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(CLAVE_MDC, traceId);
        LOG.debugf("solicitud entrante metodo=%s ruta=%s", peticion.getMethod(), peticion.getUriInfo().getPath());
    }

    @Override
    public void filter(ContainerRequestContext peticion, ContainerResponseContext respuesta) {
        Object traceId = MDC.get(CLAVE_MDC);
        if (traceId != null) {
            respuesta.getHeaders().add(CABECERA, traceId.toString());
        }
        MDC.remove(CLAVE_MDC);
    }
}
