package org.canalesCMAC.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConvenioRepository implements PanacheRepositoryBase<Convenio, Long> {

    public Optional<Convenio> porCodigo(String codigo) {
        return find("codigo", codigo).firstResultOptional();
    }
}
