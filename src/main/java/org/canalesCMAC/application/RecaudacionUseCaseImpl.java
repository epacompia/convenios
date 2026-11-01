package org.canalesCMAC.application;

import java.util.Optional;

import org.canalesCMAC.application.port.RecaudacionChannel;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.canalesCMAC.infrastructure.cache.IdempotenciaRedis;
import org.canalesCMAC.infrastructure.persistence.AuditoriaRecaudacion;
import org.canalesCMAC.infrastructure.persistence.IdempotenciaDurable;
import org.canalesCMAC.infrastructure.persistence.LogPersistente;
import org.canalesCMAC.infrastructure.persistence.RecaudacionIdempotencia;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Inject
    IdempotenciaRedis idempotencia;

    @Inject
    IdempotenciaDurable idempotenciaDurable;

    @Inject
    AuditoriaRecaudacion auditoria;

    @Inject
    LogPersistente logs;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public RecaudacionResponse procesar(RecaudacionRequest solicitud) {
        long inicio = System.nanoTime();
        String idTransaccion = solicitud.cabecera().idTransaccion();
        String institucion = solicitud.institucion().name();
        String operador = solicitud.operador().name();
        LOG.debugf("inicio de recaudacion institucion=%s operador=%s idTransaccion=%s",
            institucion, operador, idTransaccion);
        logs.registrar("INFO", RecaudacionUseCaseImpl.class.getName(),
            "inicio de recaudacion", idTransaccion, idTransaccion, institucion, operador, null);

        try {
            resolver.validar(solicitud.institucion(), solicitud.operador());
            validador.validar(solicitud);

            if (idempotencia.habilitada()) {
                Optional<String> cache = idempotencia.buscar(idTransaccion);
                if (cache.isPresent()) {
                    try {
                        LOG.infof("recaudacion idempotente idTransaccion=%s", idTransaccion);
                        return objectMapper.readValue(cache.get(), RecaudacionResponse.class);
                    } catch (JsonProcessingException e) {
                        LOG.warnf("cache de idempotencia inválida idTransaccion=%s: %s",
                            idTransaccion, e.getMessage());
                    }
                }
            }

            Optional<RecaudacionIdempotencia> durable = idempotenciaDurable.buscar(idTransaccion);
            if (durable.isPresent()
                && "OK".equals(durable.get().estado)
                && durable.get().responseJson != null) {
                try {
                    return objectMapper.readValue(durable.get().responseJson, RecaudacionResponse.class);
                } catch (JsonProcessingException e) {
                    LOG.warnf("idempotencia durable inválida idTransaccion=%s: %s",
                        idTransaccion, e.getMessage());
                }
            }

            RecaudacionResponse respuesta = canal.ejecutar(solicitud);
            String responseJson = serializar(respuesta);

            if (idempotencia.habilitada()) {
                idempotencia.guardar(idTransaccion, responseJson);
            }
            idempotenciaDurable.guardar(idTransaccion, institucion, operador, "OK", responseJson, null);

            long duracionMs = milisegundos(inicio);
            auditoria.registrar(solicitud, respuesta, duracionMs);
            logs.registrar("INFO", RecaudacionUseCaseImpl.class.getName(),
                "recaudacion completada codigo=" + respuesta.codigo(),
                idTransaccion, idTransaccion, institucion, operador, null);
            LOG.infof("recaudacion completada institucion=%s operador=%s idTransaccion=%s codigo=%s duracionMs=%d",
                institucion, operador, idTransaccion, respuesta.codigo(), duracionMs);
            return respuesta;
        } catch (RuntimeException e) {
            idempotenciaDurable.guardar(idTransaccion, institucion, operador, "ERROR", null, null);
            logs.registrar("ERROR", RecaudacionUseCaseImpl.class.getName(),
                e.getMessage(), idTransaccion, idTransaccion, institucion, operador, null);
            throw e;
        }
    }

    private String serializar(RecaudacionResponse respuesta) {
        try {
            return objectMapper.writeValueAsString(respuesta);
        } catch (JsonProcessingException e) {
            LOG.warnf("no se pudo serializar respuesta: %s", e.getMessage());
            return null;
        }
    }

    private static long milisegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }
}
