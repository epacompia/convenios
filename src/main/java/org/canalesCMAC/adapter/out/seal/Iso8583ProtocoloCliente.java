package org.canalesCMAC.adapter.out.seal;

import org.canalesCMAC.adapter.out.http.HttpProtocoloCliente;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Iso8583ProtocoloCliente extends HttpProtocoloCliente {

    @Override
    public String protocolo() {
        return "iso8583";
    }
}
