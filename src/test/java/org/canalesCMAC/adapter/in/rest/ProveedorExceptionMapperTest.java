package org.canalesCMAC.adapter.in.rest;

import org.canalesCMAC.domain.exception.ProveedorException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProveedorExceptionMapperTest {

    private final ProveedorExceptionMapper mapper = new ProveedorExceptionMapper();

    @Test
    void mapeaBadGateway() {
        int status = mapper.toResponse(
            new ProveedorException("falla", ProveedorException.BAD_GATEWAY, false, null)).getStatus();

        assertEquals(502, status);
    }

    @Test
    void mapeaGatewayTimeout() {
        int status = mapper.toResponse(
            new ProveedorException("timeout", ProveedorException.GATEWAY_TIMEOUT, true, null)).getStatus();

        assertEquals(504, status);
    }
}
