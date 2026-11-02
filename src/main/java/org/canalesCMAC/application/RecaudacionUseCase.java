package org.canalesCMAC.application;

import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

public interface RecaudacionUseCase {

    RecaudacionResponse procesar(RecaudacionRequest solicitud);
}