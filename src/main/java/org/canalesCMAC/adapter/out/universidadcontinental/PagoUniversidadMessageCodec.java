package org.canalesCMAC.adapter.out.universidadcontinental;

import org.canalesCMAC.domain.model.Institucion;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PagoUniversidadMessageCodec extends AbstractUnivContinentalCodec {

    @Override
    public Institucion institucion() {
        return Institucion.UNIVCONTINENTAL_UNIVERSIDAD;
    }

    @Override
    protected String rutaBase() {
        return "/api/pagouniversidad";
    }
}
