package org.canalesCMAC.infrastructure.persistence;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionLogRepository implements PanacheRepositoryBase<RecaudacionLog, Long> {

    public List<RecaudacionLog> porIdTransaccion(String idTransaccion) {
        return list("idTransaccion", idTransaccion);
    }
}
