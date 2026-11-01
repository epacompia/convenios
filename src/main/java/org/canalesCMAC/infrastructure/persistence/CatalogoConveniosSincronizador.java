package org.canalesCMAC.infrastructure.persistence;

import org.canalesCMAC.application.ConvenioRegistro;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Sincroniza el catálogo configurado en application.properties hacia las
 * tablas convenio y convenio_operador.
 */
@ApplicationScoped
public class CatalogoConveniosSincronizador {

    private static final Logger LOG = Logger.getLogger(CatalogoConveniosSincronizador.class);

    @Inject
    ConvenioRegistro registro;

    @Inject
    ConvenioRepository convenios;

    @Inject
    ConvenioOperadorRepository operadores;

    @Transactional
    public void onStart(@Observes StartupEvent event) {
        try {
            for (Institucion institucion : registro.instituciones()) {
                Convenio convenio = convenios.porCodigo(institucion.name())
                    .orElseGet(Convenio::new);
                convenio.codigo = institucion.name();
                convenio.nombre = institucion.name();
                convenio.protocolo = registro.protocolo(institucion);
                convenio.urlBase = registro.url(institucion);
                convenio.authTipo = registro.autenticacionTipo(institucion);
                convenio.activo = true;
                convenio.timeoutMs = (int) registro.timeout(institucion);
                convenio.reintentos = registro.reintentos(institucion);
                convenio.reintentosDelayMs = (int) registro.reintentosDelay(institucion);
                if (convenio.id == null) {
                    convenios.persist(convenio);
                }

                for (Operador operador : registro.operadores(institucion)) {
                    ConvenioOperador relacion = operadores
                        .porConvenioYOperador(convenio.id, operador.name())
                        .orElseGet(ConvenioOperador::new);
                    relacion.convenioId = convenio.id;
                    relacion.operador = operador.name();
                    relacion.activo = true;
                    relacion.timeoutMs = (int) registro.timeout(institucion);
                    relacion.reintentos = registro.reintentos(institucion);
                    if (relacion.id == null) {
                        operadores.persist(relacion);
                    }
                }
            }
            LOG.infof("catálogo de convenios sincronizado instituciones=%d",
                registro.instituciones().size());
        } catch (RuntimeException e) {
            LOG.errorf("no se pudo sincronizar el catálogo de convenios: %s", e.getMessage());
        }
    }
}
