package org.canalesCMAC.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.canalesCMAC.domain.exception.ConflictoIdempotenciaException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class IdempotenciaDurableTest {

    @Inject
    IdempotenciaDurable idempotencia;

    @Test
    void reservaSoloUnaVezYDevuelveLaRespuestaGuardada() {
        String id = "20260218000123990001";

        assertTrue(idempotencia.reservar(id, "ELSE", "PAGO_DEUDA", "hash-1"));
        assertFalse(idempotencia.reservar(id, "ELSE", "PAGO_DEUDA", "hash-1"));
        assertTrue(idempotencia.buscar(id).isPresent());

        idempotencia.guardar(id, "ELSE", "PAGO_DEUDA", "COMPLETADA",
            "{\"respuesta\":{\"codigo\":\"00\"}}", "hash-1");

        assertEquals("COMPLETADA", idempotencia.buscar(id).orElseThrow().estado);
        assertEquals("{\"respuesta\":{\"codigo\":\"00\"}}",
            idempotencia.buscar(id).orElseThrow().responseJson);
    }

    @Test
    void rechazaReutilizarElIdConOtroPayload() {
        String id = "20260218000123990002";
        idempotencia.reservar(id, "ELSE", "PAGO_DEUDA", "hash-1");

        assertThrows(ConflictoIdempotenciaException.class,
            () -> idempotencia.reservar(id, "ELSE", "PAGO_DEUDA", "hash-2"));
    }

    @Test
    void permiteReintentarCuandoElEstadoEsError() {
        String id = "20260218000123990003";
        idempotencia.reservar(id, "ELSE", "PAGO_DEUDA", "hash-1");
        idempotencia.marcarError(id);

        assertTrue(idempotencia.reservar(id, "ELSE", "PAGO_DEUDA", "hash-1"));
    }
}
