package org.canalesCMAC.infrastructure.camel;

import java.util.Locale;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ChoiceDefinition;
import org.canalesCMAC.application.ConvenioOrquestador;
import org.canalesCMAC.application.ConvenioRegistro;
import org.canalesCMAC.domain.exception.InstitucionNoSoportadaException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecaudacionRouteBuilder extends RouteBuilder {

    @Inject
    ConvenioRegistro registro;

    @Inject
    ConvenioOrquestador orquestador;

    @Override
    public void configure() {
        errorHandler(defaultErrorHandler()
            .maximumRedeliveries(0)
            .logExhausted(false));

        ChoiceDefinition choice = from("direct:recaudacion")
            .routeId("recaudacion.contentBasedRouter")
            .process(exchange -> {
                RecaudacionRequest request = exchange.getMessage().getBody(RecaudacionRequest.class);
                exchange.getMessage().setHeader("institucion", request.institucion().name());
            })
            .choice();

        for (Institucion institucion : registro.instituciones()) {
            choice.when(header("institucion").isEqualTo(institucion.name()))
                .to(registro.ruta(institucion));
        }

        choice.otherwise()
            .throwException(new InstitucionNoSoportadaException("Institución no soportada"))
            .end();

        for (Institucion institucion : registro.instituciones()) {
            from(registro.ruta(institucion))
                .routeId("recaudacion." + institucion.name().toLowerCase(Locale.ROOT) + "." + registro.protocolo(institucion))
                .bean(orquestador, "procesar");
        }
    }
}
