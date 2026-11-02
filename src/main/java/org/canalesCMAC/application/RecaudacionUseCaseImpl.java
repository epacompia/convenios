package org.canalesCMAC.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import org.canalesCMAC.application.port.RecaudacionChannel;
import org.canalesCMAC.domain.exception.ConflictoIdempotenciaException;
import org.canalesCMAC.domain.exception.OperacionEnProcesoException;
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

            String hash = hash(solicitud);
            String clave = clave(solicitud, hash);
            Optional<String> cache = idempotencia.buscar(clave);
            if (cache.isPresent()) {
                RecaudacionResponse recuperada = leer(cache.get());
                if (recuperada != null) {
                    LOG.infof("recaudacion idempotente en redis idTransaccion=%s", idTransaccion);
                    return recuperada;
                }
            }

            Optional<RecaudacionIdempotencia> durable =
                idempotenciaDurable.buscar(idTransaccion);
            if (durable.isPresent()) {
                RecaudacionIdempotencia registro = durable.get();
                if (registro.hashRequest != null && !registro.hashRequest.equals(hash)) {
                    throw new ConflictoIdempotenciaException(
                        "idTransaccion ya fue usada con otro payload");
                }
                if ("COMPLETADA".equals(registro.estado) && registro.responseJson != null) {
                    RecaudacionResponse recuperada = leer(registro.responseJson);
                    if (recuperada != null) {
                        idempotencia.guardar(clave, registro.responseJson);
                        LOG.infof("recaudacion idempotente durable idTransaccion=%s", idTransaccion);
                        return recuperada;
                    }
                    idempotenciaDurable.marcarError(idTransaccion);
                } else if ("EN_PROCESO".equals(registro.estado)) {
                    throw new OperacionEnProcesoException(
                        "La operación con idTransaccion " + idTransaccion + " está en proceso");
                }
            }

            boolean reservado = idempotenciaDurable.reservar(
                idTransaccion, institucion, operador, hash);
            if (!reservado) {
                Optional<RecaudacionIdempotencia> otra =
                    idempotenciaDurable.buscar(idTransaccion);
                if (otra.isPresent() && "COMPLETADA".equals(otra.get().estado)
                    && otra.get().responseJson != null) {
                    RecaudacionResponse recuperada = leer(otra.get().responseJson);
                    if (recuperada != null) {
                        idempotencia.guardar(clave, otra.get().responseJson);
                        return recuperada;
                    }
                }
                throw new OperacionEnProcesoException(
                    "La operación con idTransaccion " + idTransaccion + " está en proceso");
            }

            try {
                RecaudacionResponse respuesta = canal.ejecutar(solicitud);
                String responseJson = serializar(respuesta);
                if (responseJson == null) {
                    responseJson = serializar(RecaudacionResponse.error(
                        solicitud.institucion(), solicitud.operador(), solicitud.cabecera(),
                        "No se pudo serializar la respuesta del proveedor"));
                }

                idempotenciaDurable.guardar(idTransaccion, institucion, operador,
                    "COMPLETADA", responseJson, hash);
                idempotencia.guardar(clave, responseJson);

                long duracionMs = milisegundos(inicio);
                auditoria.registrar(solicitud, respuesta, duracionMs);
                logs.registrar("INFO", RecaudacionUseCaseImpl.class.getName(),
                    "recaudacion completada codigo=" + respuesta.codigo(),
                    idTransaccion, idTransaccion, institucion, operador, null);
                LOG.infof("recaudacion completada institucion=%s operador=%s idTransaccion=%s codigo=%s duracionMs=%d",
                    institucion, operador, idTransaccion, respuesta.codigo(), duracionMs);
                return respuesta;
            } catch (RuntimeException e) {
                idempotenciaDurable.marcarError(idTransaccion);
                idempotencia.eliminar(clave);
                throw e;
            }
        } catch (RuntimeException e) {
            auditarError(solicitud, e, milisegundos(inicio));
            logs.registrar("ERROR", RecaudacionUseCaseImpl.class.getName(),
                e.getMessage(), idTransaccion, idTransaccion, institucion, operador, null);
            throw e;
        }
    }

    private void auditarError(RecaudacionRequest solicitud, RuntimeException error, long duracionMs) {
        try {
            RecaudacionResponse respuesta = RecaudacionResponse.error(
                solicitud.institucion(), solicitud.operador(), solicitud.cabecera(),
                error.getMessage() == null ? "Error en la operación" : error.getMessage());
            auditoria.registrar(solicitud, respuesta, duracionMs);
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo auditar el error idTransaccion=%s: %s",
                solicitud.cabecera().idTransaccion(), e.getMessage());
        }
    }

    private RecaudacionResponse leer(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RecaudacionResponse.class);
        } catch (JsonProcessingException e) {
            LOG.warnf("respuesta idempotente inválida: %s", e.getMessage());
            return null;
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

    private String clave(RecaudacionRequest solicitud, String hash) {
        return solicitud.institucion().name() + ":" + solicitud.operador().name()
            + ":" + solicitud.cabecera().idTransaccion() + ":" + hash;
    }

    private String hash(RecaudacionRequest solicitud) {
        try {
            Map<String, Object> contenido = Map.of(
                "institucion", solicitud.institucion().name(),
                "operador", solicitud.operador().name(),
                "datos", solicitud.datos());
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                .digest(objectMapper.writer()
                    .with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(contenido)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la solicitud", e);
        }
    }

    private static long milisegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }
}
