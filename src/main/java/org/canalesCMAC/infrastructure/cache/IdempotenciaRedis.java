package org.canalesCMAC.infrastructure.cache;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    public Optional<String> buscar(String clave) {
        if (!habilitada) {
            return Optional.empty();
        }
        RedisClient redis = redis();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            Response respuesta = redis.get(clave(clave));
            return respuesta == null ? Optional.empty() : Optional.of(respuesta.toString());
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo leer idempotencia clave=%s: %s", clave, e.getMessage());
            return Optional.empty();
        }
    }

    public void guardar(String clave, String respuestaJson) {
        if (!habilitada || respuestaJson == null) {
            return;
        }
        RedisClient redis = redis();
        if (redis == null) {
            return;
        }
        try {
            redis.setex(clave(clave), String.valueOf(ttlSegundos), respuestaJson);
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo guardar idempotencia clave=%s: %s", clave, e.getMessage());
        }
    }

    public void eliminar(String clave) {
        if (!habilitada) {
            return;
        }
        RedisClient redis = redis();
        if (redis == null) {
            return;
        }
        try {
            redis.del(java.util.List.of(clave(clave)));
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo eliminar idempotencia clave=%s: %s", clave, e.getMessage());
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

    private String clave(String clave) {
        return PREFIJO + clave;
    }
}
