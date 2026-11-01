package org.canalesCMAC.infrastructure.cache;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Idempotencia de operaciones usando Redis.
 *
 * <p>Si está deshabilitada, el flujo continúa sin cachear ni bloquear.</p>
 */
@ApplicationScoped
public class IdempotenciaRedis {

    private static final Logger LOG = Logger.getLogger(IdempotenciaRedis.class);
    private static final String PREFIJO = "idem:";

    @Inject
    Instance<RedisClient> redisInstance;

    @ConfigProperty(name = "recaudacion.redis.enabled", defaultValue = "false")
    boolean habilitada;

    @ConfigProperty(name = "recaudacion.redis.idempotencia.ttl", defaultValue = "86400")
    long ttlSegundos;

    public boolean habilitada() {
        return habilitada;
    }

    public Optional<String> buscar(String idTransaccion) {
        if (!habilitada) {
            return Optional.empty();
        }
        RedisClient redis = redis();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            Response respuesta = redis.get(clave(idTransaccion));
            return respuesta == null ? Optional.empty() : Optional.of(respuesta.toString());
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo leer idempotencia idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
            return Optional.empty();
        }
    }

    public void guardar(String idTransaccion, String respuestaJson) {
        if (!habilitada) {
            return;
        }
        RedisClient redis = redis();
        if (redis == null) {
            return;
        }
        try {
            redis.setex(clave(idTransaccion), String.valueOf(ttlSegundos), respuestaJson);
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo guardar idempotencia idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
        }
    }

    private RedisClient redis() {
        if (redisInstance == null || redisInstance.isUnsatisfied()) {
            return null;
        }
        try {
            return redisInstance.get();
        } catch (RuntimeException e) {
            LOG.debugf("cliente Redis no activo: %s", e.getMessage());
            return null;
        }
    }

    private String clave(String idTransaccion) {
        return PREFIJO + idTransaccion;
    }
}
