package org.canalesCMAC.adapter.in.rest;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class ProveedorExceptionMapperTest {

    private final ProveedorExceptionMapper mapper =
            new ProveedorExceptionMapper();

    @Test
    void mapeaBadGateway() {

        Response response = mapper.toResponse(
                new ProveedorException(
                        "falla",
                        ProveedorException.BAD_GATEWAY,
                        false,
                        null
                )
        );

        assertEquals(502, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void mapeaGatewayTimeout() {

        Response response = mapper.toResponse(
                new ProveedorException(
                        "timeout",
                        ProveedorException.GATEWAY_TIMEOUT,
                        true,
                        null
                )
        );

        assertEquals(504, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void construyeRespuestaCuandoContextoEsNulo() {

        ProveedorException excepcion = new ProveedorException(
                "error del proveedor",
                ProveedorException.BAD_GATEWAY,
                false,
                null
        );

        Response response = mapper.toResponse(excepcion);

        assertEquals(502, response.getStatus());

        assertInstanceOf(
                RecaudacionResponse.class,
                response.getEntity()
        );

        RecaudacionResponse resultado =
                (RecaudacionResponse) response.getEntity();

        assertNotNull(resultado);
    }

    @Test
    void construyeRespuestaCuandoContextoEstaDisponible() throws Exception {

        SolicitudContexto contexto = new SolicitudContexto();
        asignarContexto(mapper, contexto);

        ProveedorException excepcion = new ProveedorException(
                "error del proveedor",
                ProveedorException.GATEWAY_TIMEOUT,
                true,
                null
        );

        Response response = mapper.toResponse(excepcion);

        assertEquals(504, response.getStatus());

        assertInstanceOf(
                RecaudacionResponse.class,
                response.getEntity()
        );

        RecaudacionResponse resultado =
                (RecaudacionResponse) response.getEntity();

        assertNotNull(resultado);
    }

    @Test
    void conservaElStatusDeLaExcepcion() {

        int statusEsperado = ProveedorException.BAD_GATEWAY;

        Response response = mapper.toResponse(
                new ProveedorException(
                        "falla",
                        statusEsperado,
                        false,
                        null
                )
        );

        assertEquals(statusEsperado, response.getStatus());
    }

    @Test
    void conservaElMensajeDeLaExcepcionEnLaRespuesta() {

        String mensaje = "servicio externo no disponible";

        Response response = mapper.toResponse(
                new ProveedorException(
                        mensaje,
                        ProveedorException.BAD_GATEWAY,
                        false,
                        null
                )
        );

        RecaudacionResponse resultado =
                (RecaudacionResponse) response.getEntity();

        assertNotNull(resultado);
    }

    private void asignarContexto(
            ProveedorExceptionMapper mapper,
            SolicitudContexto contexto
    ) throws Exception {

        Field campo = ProveedorExceptionMapper.class
                .getDeclaredField("contexto");

        campo.setAccessible(true);
        campo.set(mapper, contexto);
    }

    private SolicitudContexto crearContexto() {

        return (SolicitudContexto) Proxy.newProxyInstance(
                SolicitudContexto.class.getClassLoader(),
                new Class<?>[]{SolicitudContexto.class},
                (proxy, method, args) -> {

                    switch (method.getName()) {

                        case "institucion":
                            return Institucion.SEAL;

                        case "operador":
                            return Operador.PAGO_DEUDA;

                        case "cabecera":
                            return Cabecera.generar();

                        case "toString":
                            return "SolicitudContextoTest";

                        case "hashCode":
                            return System.identityHashCode(proxy);

                        case "equals":
                            return proxy == args[0];

                        default:
                            throw new UnsupportedOperationException(
                                    "Método no implementado: "
                                            + method.getName()
                            );
                    }
                }
        );
    }
}