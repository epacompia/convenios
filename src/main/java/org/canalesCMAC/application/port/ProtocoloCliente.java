package org.canalesCMAC.application.port;

import org.canalesCMAC.application.Convenio;

public interface ProtocoloCliente {

    String protocolo();

    RespuestaCruda ejecutar(Convenio convenio, PeticionSalida peticion);
}
