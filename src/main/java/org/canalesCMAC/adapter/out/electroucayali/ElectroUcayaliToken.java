package org.canalesCMAC.adapter.out.electroucayali;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.application.ConvenioRegistro;
import org.canalesCMAC.domain.model.Institucion;
import org.jboss.logging.Logger;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ElectroUcayaliToken {

    private static final Logger LOG = Logger.getLogger(ElectroUcayaliToken.class);
    private static final Duration TIEMPO_MAXIMO = Duration.ofHours(24);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ConvenioRegistro registro;

    @ConfigProperty(name = "recaudacion.electroucayali.usuario")
    String usuario;

    @ConfigProperty(name = "recaudacion.electroucayali.clave")
    String clave;

    @Inject
    Instance<RedisClient> redisInstance;

    @ConfigProperty(name = "recaudacion.redis.enabled", defaultValue = "false")
    boolean redisHabilitado;

    private static final String CLAVE_REDIS = "token:ELECTRO_UCAYALI";

    private volatile String token;
    private volatile Instant vence;

    public String obtener() {
        String actual = token;
        if (actual != null && Instant.now().isBefore(vence)) {
            return actual;
        }
        synchronized (this) {
            if (token != null && Instant.now().isBefore(vence)) {
                return token;
            }
            if (desdeRedis()) {
                return token;
            }
            autenticar();
            return token;
        }
    }

    private boolean desdeRedis() {
        RedisClient redis = redis();
        if (redis == null) {
            return false;
        }
        try {
            Response respuesta = redis.get(CLAVE_REDIS);
            if (respuesta == null) {
                return false;
            }
            token = respuesta.toString();
            vence = Instant.now().plusSeconds(300);
            return token != null && !token.isBlank();
        } catch (RuntimeException e) {
            LOG.debugf("no se pudo leer token de Redis: %s", e.getMessage());
            return false;
        }
    }

    private RedisClient redis() {
        if (!redisHabilitado || redisInstance == null || redisInstance.isUnsatisfied()) {
            return null;
        }
        try {
            return redisInstance.get();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void autenticar() {
        LOG.debug("renovando token de Electro Ucayali");
        try {
            String cuerpo = objectMapper.writeValueAsString(Map.of("Usuario", usuario, "Clave", clave));
            String respuesta = producerTemplate.requestBodyAndHeaders(
                registro.url(Institucion.ELECTRO_UCAYALI) + "/api/v1/electro/auth",
                cuerpo,
                Map.of(Exchange.CONTENT_TYPE, "application/json"),
                String.class);
            JsonNode nodo = objectMapper.readTree(respuesta);
            String nuevoToken = nodo.path("token").asText();
            if (nuevoToken.isBlank()) {
                throw new IllegalStateException("Electro Ucayali no otorgó token: " + respuesta);
            }
            token = nuevoToken;
            long expira = nodo.hasNonNull("Expires_in") ? Long.parseLong(nodo.get("Expires_in").asText()) : TIEMPO_MAXIMO.toSeconds();
            vence = Instant.now().plus(Duration.ofSeconds(expira));
            guardarEnRedis(expira);
            LOG.debugf("token de Electro Ucayali renovado venceEn=%s", vence);
        } catch (Exception e) {
            LOG.errorf("no se pudo autenticar en Electro Ucayali: %s", e.getMessage());
            throw new IllegalStateException("No se pudo autenticar en Electro Ucayali: " + e.getMessage(), e);
        }
    }

    private void guardarEnRedis(long expira) {
        RedisClient redis = redis();
        if (redis == null) {
            return;
        }
        try {
            redis.setex(CLAVE_REDIS, String.valueOf(expira), token);
        } catch (RuntimeException e) {
            LOG.debugf("no se pudo guardar token en Redis: %s", e.getMessage());
        }
    }
}