package org.canalesCMAC.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConvenioOperadorRepository implements PanacheRepositoryBase<ConvenioOperador, Long> {

    public List<ConvenioOperador> porConvenio(Long convenioId) {
        return list("convenioId", convenioId);
    }

    public Optional<ConvenioOperador> porConvenioYOperador(Long convenioId, String operador) {
        return find("convenioId = ?1 and operador = ?2", convenioId, operador).firstResultOptional();
    }
}
