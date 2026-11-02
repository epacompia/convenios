package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.canalesCMAC.domain.exception.ConflictoIdempotenciaException;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.jboss.logging.Logger;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IdempotenciaDurable {

    private static final Logger LOG = Logger.getLogger(IdempotenciaDurable.class);

    @Inject
    RecaudacionIdempotenciaRepository repositorio;

    @Inject
    ConvenioRepository convenios;

    @ConfigProperty(name = "recaudacion.idempotencia.durable.enabled", defaultValue = "true")
    boolean habilitada;

    @ConfigProperty(name = "recaudacion.idempotencia.durable.ttl", defaultValue = "86400")
    long ttlSegundos;

    @ConfigProperty(name = "recaudacion.idempotencia.durable.ttlProceso", defaultValue = "300")
    long ttlProcesoSegundos;

    public Optional<RecaudacionIdempotencia> buscar(String idTransaccion) {
        if (!habilitada) {
            return Optional.empty();
        }
        try {
            return QuarkusTransaction.requiringNew().call(() -> repositorio.porIdTransaccion(idTransaccion)
                .filter(registro -> registro.expiraAt == null
                    || registro.expiraAt.isAfter(OffsetDateTime.now())));
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo leer idempotencia durable idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean reservar(String idTransaccion,
                            String institucion,
                            String operador,
                            String hashRequest) {
        if (!habilitada) {
            return true;
        }
        try {
            return QuarkusTransaction.requiringNew().call(() -> {
                Optional<RecaudacionIdempotencia> existente =
                    repositorio.porIdTransaccion(idTransaccion);
                if (existente.isPresent()) {
                    RecaudacionIdempotencia registro = existente.get();
                    if (!compatible(registro, operador, hashRequest)) {
                        throw new ConflictoIdempotenciaException(
                            "idTransaccion ya fue usada con otro convenio, operador o payload");
                    }
                    boolean expirado = registro.expiraAt != null
                        && registro.expiraAt.isBefore(OffsetDateTime.now());
                    if ("ERROR".equals(registro.estado) || expirado) {
                        registro.estado = "EN_PROCESO";
                        registro.responseJson = null;
                        registro.hashRequest = hashRequest;
                        registro.expiraAt = OffsetDateTime.now().plusSeconds(ttlProcesoSegundos);
                        return true;
                    }
                    return false;
                }
                RecaudacionIdempotencia registro = new RecaudacionIdempotencia();
                registro.idTransaccion = idTransaccion;
                registro.convenioId = convenios.porCodigo(institucion)
                    .map(convenio -> convenio.id)
                    .orElse(null);
                registro.operador = operador;
                registro.estado = "EN_PROCESO";
                registro.hashRequest = hashRequest;
                registro.expiraAt = OffsetDateTime.now().plusSeconds(ttlProcesoSegundos);
                repositorio.persist(registro);
                return true;
            });
        } catch (ConflictoIdempotenciaException e) {
            throw e;
        } catch (RuntimeException e) {
            if (esConflictoUnico(e)) {
                return false;
            }
            LOG.errorf("no se pudo reservar idempotencia idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
            throw new ProveedorException(
                "No se pudo reservar la idempotencia de la operación",
                ProveedorException.SERVICE_UNAVAILABLE, false, e);
        }
    }

    public void guardar(String idTransaccion,
                        String institucion,
                        String operador,
                        String estado,
                        String responseJson,
                        String hashRequest) {
        if (!habilitada) {
            return;
        }
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                RecaudacionIdempotencia registro = repositorio.porIdTransaccion(idTransaccion)
                    .orElseGet(RecaudacionIdempotencia::new);
                registro.idTransaccion = idTransaccion;
                registro.convenioId = convenios.porCodigo(institucion)
                    .map(convenio -> convenio.id)
                    .orElse(null);
                registro.operador = operador;
                registro.estado = estado;
                registro.responseJson = responseJson;
                registro.hashRequest = hashRequest;
                registro.expiraAt = OffsetDateTime.now().plusSeconds(ttlSegundos);
                if (registro.id == null) {
                    repositorio.persist(registro);
                }
            });
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo guardar idempotencia idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
        }
    }

    public void marcarError(String idTransaccion) {
        if (!habilitada) {
            return;
        }
        try {
            QuarkusTransaction.requiringNew().run(() -> repositorio.porIdTransaccion(idTransaccion)
                .ifPresent(registro -> {
                    registro.estado = "ERROR";
                    registro.responseJson = null;
                    registro.expiraAt = OffsetDateTime.now().plusSeconds(ttlProcesoSegundos);
                }));
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo marcar error de idempotencia idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
        }
    }

    private static boolean compatible(RecaudacionIdempotencia registro,
                                      String operador,
                                      String hashRequest) {
        if (registro.operador != null && !registro.operador.equals(operador)) {
            return false;
        }
        return registro.hashRequest == null || registro.hashRequest.equals(hashRequest);
    }

    private static boolean esConflictoUnico(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            String nombre = actual.getClass().getSimpleName().toLowerCase();
            String mensaje = actual.getMessage() == null ? "" : actual.getMessage().toLowerCase();
            if (nombre.contains("constraint") || nombre.contains("integrity")
                || mensaje.contains("unique") || mensaje.contains("duplicate")
                || mensaje.contains("constraint")) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
