package org.canalesCMAC.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.canalesCMAC.application.ConciliacionService;
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
class ModeloDatosTest {

    @Inject
    ConvenioRepository convenios;

    @Inject
    ConvenioOperadorRepository convenioOperadores;

    @Inject
    AuditoriaRecaudacion auditoria;

    @Inject
    ConciliacionService conciliacion;

    @Inject
    IdempotenciaDurable idempotenciaDurable;

    @Inject
    RecaudacionIdempotenciaRepository idempotencias;

    @Inject
    LogPersistente logs;

    @Inject
    RecaudacionLogRepository logsRepositorio;

    @Inject
    RegistroIntentoProveedor intentos;

    @Inject
    RecaudacionIntentoProveedorRepository intentosRepositorio;

    @Test
    void sincronizaElCatalogoDeConveniosYOperadores() {
        Optional<Convenio> claro = convenios.porCodigo("CLARO");
        assertTrue(claro.isPresent());
        assertEquals(12, convenioOperadores.porConvenio(claro.get().id).size());
    }

    @Test
    void persisteOperacionTransaccionClientePagoYConciliacion() {
        RecaudacionRequest solicitud = solicitud();
        RecaudacionResponse respuesta = respuesta(solicitud);

        auditoria.registrar(solicitud, respuesta, 10L);

        RecaudacionConciliacion cierre = conciliacion.cerrar(LocalDate.now(), Institucion.CLARO);
        assertTrue(cierre.totalOperaciones >= 1);
        assertTrue(cierre.montoTotal.signum() >= 0);
        assertEquals("CUADRADO", cierre.estado);
    }

    @Test
    void guardaIdempotenciaDurable() {
        String id = Cabecera.generar().idTransaccion();

        idempotenciaDurable.guardar(id, "CLARO", "PAGO", "OK", "{}", null);

        Optional<RecaudacionIdempotencia> registro = idempotencias.porIdTransaccion(id);
        assertTrue(registro.isPresent());
        assertEquals("OK", registro.get().estado);
    }

    @Test
    void persisteLogsEIntentosDeProveedor() {
        RecaudacionRequest solicitud = solicitud();
        String id = solicitud.cabecera().idTransaccion();

        logs.registrar("INFO", "test", "mensaje", id, id, "CLARO", "PAGO", null);
        intentos.registrar(id, "CLARO", "PAGO", "soap", "http://localhost",
            1, 200, null, null, 5L, "CERRADO", solicitud, "{}");

        assertFalse(logsRepositorio.porIdTransaccion(id).isEmpty());
        assertFalse(intentosRepositorio.porIdTransaccion(id).isEmpty());
    }

    private RecaudacionRequest solicitud() {
        return new RecaudacionRequest(
            Institucion.CLARO, Operador.PAGO, Cabecera.generar(),
            new Datos(
                Map.of("numeroTarjeta", "4111111111111111", "numeroDocumento", "12345678"),
                Map.of("nombreCliente", "JUAN PEREZ"),
                List.of(Map.of("pinblock", "9876", "monto", "10.00"))));
    }

    private RecaudacionResponse respuesta(RecaudacionRequest solicitud) {
        return RecaudacionResponse.de(
            Institucion.CLARO, Operador.PAGO, solicitud.cabecera(),
            "00", "OK", Map.of("nombreCliente", "JUAN PEREZ"),
            List.of(Map.of("codigoAprobacion", "AUTH-123", "monto", 10.00)));
    }
}
