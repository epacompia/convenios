package org.canalesCMAC.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionIdempotenciaRepository
    implements PanacheRepositoryBase<RecaudacionIdempotencia, Long> {

    public Optional<RecaudacionIdempotencia> porIdTransaccion(String idTransaccion) {
        return find("idTransaccion", idTransaccion).firstResultOptional();
    }
}
