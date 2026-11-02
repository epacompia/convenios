package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import org.jboss.logging.Logger;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
            QuarkusTransaction.requiringNew().run(() -> persistir(nivel, logger, mensaje,
                traceId, idTransaccion, institucion, operador, contextoJson));
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo persistir log idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
        }
    }

    private void persistir(String nivel,
                           String logger,
                           String mensaje,
                           String traceId,
                           String idTransaccion,
                           String institucion,
                           String operador,
                           String contextoJson) {
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
    }
}
