package org.canalesCMAC.adapter.out.universidadcontinental;

import org.canalesCMAC.domain.model.Institucion;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PagoIdiomasMessageCodec extends AbstractUnivContinentalCodec {

    @Override
    public Institucion institucion() {
        return Institucion.UNIVCONTINENTAL_CIDIOMAS;
    }

    @Override
    protected String rutaBase() {
        return "/api/pagoidiomas";
    }
}
