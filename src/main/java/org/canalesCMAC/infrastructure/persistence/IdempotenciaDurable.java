package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class IdempotenciaDurable {

    @Inject
    RecaudacionIdempotenciaRepository repositorio;

    @Inject
    ConvenioRepository convenios;

    @ConfigProperty(name = "recaudacion.idempotencia.durable.enabled", defaultValue = "true")
    boolean habilitada;

    @ConfigProperty(name = "recaudacion.idempotencia.durable.ttl", defaultValue = "86400")
    long ttlSegundos;

    @Transactional
    public Optional<RecaudacionIdempotencia> buscar(String idTransaccion) {
        if (!habilitada) {
            return Optional.empty();
        }
        return repositorio.porIdTransaccion(idTransaccion)
            .filter(registro -> registro.expiraAt == null
                || registro.expiraAt.isAfter(OffsetDateTime.now()));
    }

    @Transactional
    public void guardar(String idTransaccion,
                        String institucion,
                        String operador,
                        String estado,
                        String responseJson,
                        String hashRequest) {
        if (!habilitada) {
            return;
        }
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
    }
}
