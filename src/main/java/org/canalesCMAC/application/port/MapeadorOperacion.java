package org.canalesCMAC.application.port;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

public interface MapeadorOperacion {

    Institucion institucion();

    PeticionSalida construir(RecaudacionRequest peticion);

    RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion);
}
