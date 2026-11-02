package org.canalesCMAC.infrastructure.persistence;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionTransaccionRepository implements PanacheRepositoryBase<RecaudacionTransaccion, Long> {

    public List<RecaudacionTransaccion> porOperacion(Long operacionId) {
        return list("operacionId", operacionId);
    }
}
