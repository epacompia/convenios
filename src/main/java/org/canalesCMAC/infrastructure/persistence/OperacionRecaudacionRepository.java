package org.canalesCMAC.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OperacionRecaudacionRepository implements PanacheRepositoryBase<OperacionRecaudacion, Long> {

    public Optional<OperacionRecaudacion> porIdTransaccion(String idTransaccion) {
        return find("idTransaccion", idTransaccion).firstResultOptional();
    }
}
