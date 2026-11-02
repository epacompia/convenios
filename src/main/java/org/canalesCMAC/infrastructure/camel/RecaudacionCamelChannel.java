package org.canalesCMAC.infrastructure.camel;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.application.port.RecaudacionChannel;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecaudacionCamelChannel implements RecaudacionChannel {

    private static final Logger LOG = Logger.getLogger(RecaudacionCamelChannel.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Override
    public RecaudacionResponse ejecutar(RecaudacionRequest solicitud) {
        LOG.debugf("ejecutando ruta camel institucion=%s operador=%s",
            solicitud.institucion(), solicitud.operador());
        try {
            return producerTemplate.requestBody("direct:recaudacion", solicitud, RecaudacionResponse.class);
        } catch (CamelExecutionException excepcion) {
            if (excepcion.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw excepcion;
        }
    }
}
