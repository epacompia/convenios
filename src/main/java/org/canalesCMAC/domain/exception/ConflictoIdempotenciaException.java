package org.canalesCMAC.domain.exception;

public class ConflictoIdempotenciaException extends RecaudacionException {

    public ConflictoIdempotenciaException(String mensaje) {
        super(mensaje);
    }
}
