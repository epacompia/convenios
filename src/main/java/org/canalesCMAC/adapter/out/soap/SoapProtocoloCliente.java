package org.canalesCMAC.adapter.out.soap;

import org.canalesCMAC.adapter.out.http.HttpProtocoloCliente;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SoapProtocoloCliente extends HttpProtocoloCliente {

    @Override
    public String protocolo() {
        return "soap";
    }
}
