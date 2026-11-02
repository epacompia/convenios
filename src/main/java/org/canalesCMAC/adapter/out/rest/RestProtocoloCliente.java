package org.canalesCMAC.adapter.out.rest;

import org.canalesCMAC.adapter.out.http.HttpProtocoloCliente;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RestProtocoloCliente extends HttpProtocoloCliente {

    @Override
    public String protocolo() {
        return "rest";
    }
}
