package org.canalesCMAC.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionConciliacionRepository
    implements PanacheRepositoryBase<RecaudacionConciliacion, Long> {

    public Optional<RecaudacionConciliacion> porFechaYConvenio(LocalDate fecha, Long convenioId) {
        return find("fechaConciliacion = ?1 and convenioId = ?2", fecha, convenioId)
            .firstResultOptional();
    }
}
