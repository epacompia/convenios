package org.canalesCMAC.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionClienteRepository implements PanacheRepositoryBase<RecaudacionCliente, Long> {

    public Optional<RecaudacionCliente> porOperacion(Long operacionId) {
        return find("operacionId", operacionId).firstResultOptional();
    }
}
