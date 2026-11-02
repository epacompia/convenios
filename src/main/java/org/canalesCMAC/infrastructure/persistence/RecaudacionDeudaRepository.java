package org.canalesCMAC.infrastructure.persistence;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionDeudaRepository implements PanacheRepositoryBase<RecaudacionDeuda, Long> {

    public List<RecaudacionDeuda> porTransaccion(Long transaccionId) {
        return list("transaccionId", transaccionId);
    }
}
