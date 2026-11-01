package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LogPersistente {

    private static final Logger LOG = Logger.getLogger(LogPersistente.class);

    @Inject
    RecaudacionLogRepository repositorio;

    @Inject
    ConvenioRepository convenios;

    @ConfigProperty(name = "recaudacion.logs.enabled", defaultValue = "true")
    boolean habilitado;

    @Transactional
    public void registrar(String nivel,
                          String logger,
                          String mensaje,
                          String traceId,
                          String idTransaccion,
                          String institucion,
                          String operador,
                          String contextoJson) {
        if (!habilitado) {
            return;
        }
        try {
            RecaudacionLog entidad = new RecaudacionLog();
            entidad.nivel = nivel;
            entidad.logger = logger;
            entidad.mensaje = mensaje;
            entidad.traceId = traceId;
            entidad.idTransaccion = idTransaccion;
            entidad.convenioId = institucion == null ? null : convenios.porCodigo(institucion)
                .map(convenio -> convenio.id)
                .orElse(null);
            entidad.operador = operador;
            entidad.contextoJson = contextoJson;
            entidad.fechaLog = OffsetDateTime.now();
            repositorio.persist(entidad);
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo persistir log idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
        }
    }
}
