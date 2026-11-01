package org.canalesCMAC.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionExtornoRepository implements PanacheRepositoryBase<RecaudacionExtorno, Long> {

    public Optional<RecaudacionExtorno> porTransaccion(Long transaccionId) {
        return find("transaccionId", transaccionId).firstResultOptional();
    }
}
