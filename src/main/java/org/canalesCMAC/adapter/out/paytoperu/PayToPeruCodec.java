package org.canalesCMAC.adapter.out.paytoperu;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.domain.model.Institucion;

@ApplicationScoped
public class PayToPeruCodec extends AbstractPayToPeruCodec {

    @Override
    public Institucion institucion() {
        return Institucion.PAYTOPERU;
    }

    @Override
    protected String rutaBase() {
        return "/cmac";
    }



}