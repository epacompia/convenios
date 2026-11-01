package org.canalesCMAC.application;

import org.canalesCMAC.application.port.RecaudacionChannel;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecaudacionUseCaseImpl implements RecaudacionUseCase {

    private static final Logger LOG = Logger.getLogger(RecaudacionUseCaseImpl.class);

    @Inject
    RecaudacionResolver resolver;

    @Inject
    ValidadorSolicitud validador;

    @Inject
    RecaudacionChannel canal;

    @Override
    public RecaudacionResponse procesar(RecaudacionRequest solicitud) {
        long inicio = System.nanoTime();
        LOG.debugf("inicio de recaudacion institucion=%s operador=%s idTransaccion=%s",
            solicitud.institucion(), solicitud.operador(), solicitud.cabecera().idTransaccion());
        resolver.validar(solicitud.institucion(), solicitud.operador());
        validador.validar(solicitud);
        RecaudacionResponse respuesta = canal.ejecutar(solicitud);
        LOG.infof("recaudacion completada institucion=%s operador=%s idTransaccion=%s codigo=%s duracionMs=%d",
            solicitud.institucion(), solicitud.operador(), solicitud.cabecera().idTransaccion(),
            respuesta.codigo(), milisegundos(inicio));
        return respuesta;
    }

    private static long milisegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }
}
