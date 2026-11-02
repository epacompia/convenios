package org.canalesCMAC.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionPagoRepository implements PanacheRepositoryBase<RecaudacionPago, Long> {

    public Optional<RecaudacionPago> porTransaccion(Long transaccionId) {
        return find("transaccionId", transaccionId).firstResultOptional();
    }
}
