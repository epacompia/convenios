package org.canalesCMAC.infrastructure.persistence;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionIntentoProveedorRepository
    implements PanacheRepositoryBase<RecaudacionIntentoProveedor, Long> {

    public List<RecaudacionIntentoProveedor> porIdTransaccion(String idTransaccion) {
        return list("idTransaccion", idTransaccion);
    }
}
