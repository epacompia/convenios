package org.canalesCMAC.infrastructure.persistence;

import java.util.Set;
import java.util.stream.Collectors;

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
            desactivarNoConfigurados();
            LOG.infof("catálogo de convenios sincronizado instituciones=%d",
                registro.instituciones().size());
        } catch (RuntimeException e) {
            LOG.errorf("no se pudo sincronizar el catálogo de convenios: %s", e.getMessage());
        }
    }

    private void desactivarNoConfigurados() {
        Set<String> configurados = registro.instituciones().stream()
            .map(Institucion::name)
            .collect(Collectors.toSet());
        for (Convenio convenio : convenios.listAll()) {
            if (!configurados.contains(convenio.codigo)) {
                convenio.activo = false;
                continue;
            }
            Institucion institucion;
            try {
                institucion = Institucion.desde(convenio.codigo);
            } catch (RuntimeException e) {
                convenio.activo = false;
                continue;
            }
            if (institucion == null) {
                convenio.activo = false;
                continue;
            }
            Set<String> operadoresConfigurados = registro
                .operadores(institucion).stream()
                .map(Operador::name)
                .collect(Collectors.toSet());
            for (ConvenioOperador relacion : operadores.porConvenio(convenio.id)) {
                if (!operadoresConfigurados.contains(relacion.operador)) {
                    relacion.activo = false;
                }
            }
        }
    }
}
