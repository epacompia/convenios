package org.canalesCMAC.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuditoriaRecaudacionTest {

    @Inject
    AuditoriaRecaudacion auditoria;

    @Inject
    OperacionRecaudacionRepository repositorio;

    @Inject
    RecaudacionTransaccionRepository transacciones;

    @Inject
    RecaudacionPagoRepository pagos;

    @Inject
    RecaudacionClienteRepository clientes;

    @Test
    void guardaLaOperacionYEnmascaraDatosSensibles() {
        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.CLARO, Operador.PAGO, Cabecera.generar(),
            new Datos(
                Map.of("numeroTarjeta", "4111111111111111", "numeroDocumento", "12345678"),
                Map.of("nombreCliente", "JUAN PEREZ"),
                List.of(Map.of("pinblock", "9876", "monto", "10.00"))));
        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.CLARO, Operador.PAGO, solicitud.cabecera(),
            "00", "OK", Map.of("nombreCliente", "JUAN PEREZ"),
            List.of(Map.of("codigoAprobacion", "AUTH-123", "monto", 10.00)));

        auditoria.registrar(solicitud, respuesta, 12L);

        Optional<OperacionRecaudacion> guardada =
            repositorio.porIdTransaccion(solicitud.cabecera().idTransaccion());
        assertTrue(guardada.isPresent());
        assertTrue(guardada.get().requestJson.contains("****"));
        assertFalse(guardada.get().requestJson.contains("4111111111111111"));
        assertTrue(guardada.get().requestJson.contains("\"pinblock\":\"****\""));
        assertFalse(guardada.get().requestJson.contains("9876"));

        List<RecaudacionTransaccion> detalle = transacciones.porOperacion(guardada.get().id);
        assertFalse(detalle.isEmpty());
        assertTrue(pagos.porTransaccion(detalle.get(0).id).isPresent());
        assertTrue(clientes.porOperacion(guardada.get().id).isPresent());
    }

    @Test
    void unaConsultaDePagoNoGeneraRegistroDePago() {
        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.PAYTOPERU, Operador.CONSULTA_PAGO, Cabecera.generar(),
            new Datos(Map.of("codigoPago", "123"), Map.of(), List.of()));
        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.PAYTOPERU, Operador.CONSULTA_PAGO, solicitud.cabecera(),
            "00", "OK", Map.of(), List.of(Map.of("monto", 10.00)));

        auditoria.registrar(solicitud, respuesta, 5L);

        OperacionRecaudacion operacion = repositorio
            .porIdTransaccion(solicitud.cabecera().idTransaccion()).orElseThrow();
        List<RecaudacionTransaccion> detalle = transacciones.porOperacion(operacion.id);
        assertFalse(detalle.isEmpty());
        assertTrue(pagos.porTransaccion(detalle.get(0).id).isEmpty());
    }
}
