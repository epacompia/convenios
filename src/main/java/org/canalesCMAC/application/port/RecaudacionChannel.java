package org.canalesCMAC.application.port;

import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

public interface RecaudacionChannel {

    RecaudacionResponse ejecutar(RecaudacionRequest solicitud);
}