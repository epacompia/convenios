package org.canalesCMAC.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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
            Institucion.CLARO,
            Operador.PAGO,
            Cabecera.generar(),
            new Datos(
                Map.of(
                    "numeroTarjeta", "4111111111111111",
                    "numeroDocumento", "12345678"
                ),
                Map.of("nombreCliente", "JUAN PEREZ"),
                List.of(Map.of(
                    "pinblock", "9876",
                    "monto", "10.00"
                ))
            )
        );

        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.CLARO,
            Operador.PAGO,
            solicitud.cabecera(),
            "00",
            "OK",
            Map.of("nombreCliente", "JUAN PEREZ"),
            List.of(Map.of(
                "codigoAprobacion", "AUTH-123",
                "monto", 10.00
            ))
        );

        auditoria.registrar(solicitud, respuesta, 12L);

        Optional<OperacionRecaudacion> guardada =
            repositorio.porIdTransaccion(
                solicitud.cabecera().idTransaccion()
            );

        assertTrue(guardada.isPresent());
        assertTrue(guardada.get().requestJson.contains("****"));
        assertFalse(
            guardada.get().requestJson.contains("4111111111111111")
        );
        assertTrue(
            guardada.get().requestJson.contains("\"pinblock\":\"****\"")
        );
        assertFalse(guardada.get().requestJson.contains("9876"));

        List<RecaudacionTransaccion> detalle =
            transacciones.porOperacion(guardada.get().id);

        assertFalse(detalle.isEmpty());
        assertTrue(
            pagos.porTransaccion(detalle.get(0).id).isPresent()
        );
        assertTrue(
            clientes.porOperacion(guardada.get().id).isPresent()
        );
    }

    @Test
    void unaConsultaDePagoNoGeneraRegistroDePago() {

        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.PAYTOPERU,
            Operador.CONSULTA_PAGO,
            Cabecera.generar(),
            new Datos(
                Map.of("codigoPago", "123"),
                Map.of(),
                List.of()
            )
        );

        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.PAYTOPERU,
            Operador.CONSULTA_PAGO,
            solicitud.cabecera(),
            "00",
            "OK",
            Map.of(),
            List.of(Map.of("monto", 10.00))
        );

        auditoria.registrar(solicitud, respuesta, 5L);

        OperacionRecaudacion operacion = repositorio
            .porIdTransaccion(solicitud.cabecera().idTransaccion())
            .orElseThrow();

        List<RecaudacionTransaccion> detalle =
            transacciones.porOperacion(operacion.id);

        assertFalse(detalle.isEmpty());

        assertTrue(
            pagos.porTransaccion(detalle.get(0).id).isEmpty()
        );
    }

    @Test
    void registraTransaccionDesdeSolicitudCuandoRespuestaNoTieneTransacciones() {

        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            new Datos(
                Map.of("codigoSuministro", "123456"),
                Map.of("nombreCliente", "JUAN PEREZ"),
                List.of(Map.of(
                    "numeroOperacion", "OP-001",
                    "monto", "150.00"
                ))
            )
        );

        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            solicitud.cabecera(),
            "00",
            "OK",
            Map.of("nombreCliente", "JUAN PEREZ"),
            List.of()
        );

        auditoria.registrar(solicitud, respuesta, 8L);

        OperacionRecaudacion operacion = repositorio
            .porIdTransaccion(solicitud.cabecera().idTransaccion())
            .orElseThrow();

        List<RecaudacionTransaccion> detalle =
            transacciones.porOperacion(operacion.id);

        assertFalse(detalle.isEmpty());
    }

    @Test
    void registraVariasTransaccionesYProcesaDeudas() {

        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            Cabecera.generar(),
            new Datos(
                Map.of("codigoSuministro", "123456"),
                Map.of("nombreCliente", "JUAN PEREZ"),
                List.of(
                    Map.of("numeroOperacion", "OP-001"),
                    Map.of("numeroOperacion", "OP-002")
                )
            )
        );

        List<Map<String, Object>> respuestaTransacciones = List.of(
            Map.of(
                "numeroComprobante", "COMP-001",
                "monto", "100.00",
                "deudas", Arrays.asList(
                    Map.of(
                        "numeroComprobante", "DEU-001",
                        "descripcion", "Deuda de prueba",
                        "monto", "100.00",
                        "importeDeudaMinima", "50.00",
                        "periodo", "202610",
                        "cantidadSubconceptos", "2"
                    ),
                    "elemento-no-valido"
                )
            ),
            Map.of(
                "numeroComprobante", "COMP-002",
                "monto", "200.00"
            )
        );

        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.SEAL,
            Operador.CONSULTA_DEUDA,
            solicitud.cabecera(),
            "00",
            "OK",
            Map.of(),
            respuestaTransacciones
        );

        auditoria.registrar(solicitud, respuesta, 15L);

        OperacionRecaudacion operacion = repositorio
            .porIdTransaccion(solicitud.cabecera().idTransaccion())
            .orElseThrow();

        List<RecaudacionTransaccion> detalle =
            transacciones.porOperacion(operacion.id);

        assertEquals(2, detalle.size());
    }

    @Test
    void registraExtornoCuandoLosDatosContienenMotivoExtorno() {

        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.CLARO,
            Operador.CONSULTA_PAGO,
            Cabecera.generar(),
            new Datos(
                Map.of(
                    "identificadorPago", "PAGO-001",
                    "numeroOperacionOriginal", "OP-001",
                    "motivoExtorno", "ANULACION DE OPERACION"
                ),
                Map.of(),
                List.of()
            )
        );

        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.CLARO,
            Operador.CONSULTA_PAGO,
            solicitud.cabecera(),
            "00",
            "OK",
            Map.of(),
            List.of(Map.of(
                "numeroOperacionOriginal", "OP-001",
                "motivoExtorno", "ANULACION DE OPERACION",
                "estado", "PROCESADO"
            ))
        );

        auditoria.registrar(solicitud, respuesta, 10L);

        OperacionRecaudacion operacion = repositorio
            .porIdTransaccion(solicitud.cabecera().idTransaccion())
            .orElseThrow();

        List<RecaudacionTransaccion> detalle =
            transacciones.porOperacion(operacion.id);

        assertFalse(detalle.isEmpty());
    }

    @Test
    void noDuplicaOperacionCuandoLaTransaccionYaExiste() {

        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.CLARO,
            Operador.PAGO,
            Cabecera.generar(),
            new Datos(
                Map.of("numeroTarjeta", "4111111111111111"),
                Map.of("nombreCliente", "JUAN PEREZ"),
                List.of(Map.of("monto", "50.00"))
            )
        );

        RecaudacionResponse respuesta = RecaudacionResponse.de(
            Institucion.CLARO,
            Operador.PAGO,
            solicitud.cabecera(),
            "00",
            "OK",
            Map.of("nombreCliente", "JUAN PEREZ"),
            List.of(Map.of("monto", "50.00"))
        );

        // Primera ejecución: registra la operación.
        auditoria.registrar(solicitud, respuesta, 10L);

        OperacionRecaudacion operacion = repositorio
            .porIdTransaccion(solicitud.cabecera().idTransaccion())
            .orElseThrow();

        List<RecaudacionTransaccion> detalleInicial =
            transacciones.porOperacion(operacion.id);

        // Segunda ejecución: encuentra la operación existente.
        auditoria.registrar(solicitud, respuesta, 20L);

        List<RecaudacionTransaccion> detalleFinal =
            transacciones.porOperacion(operacion.id);

        assertEquals(detalleInicial.size(), detalleFinal.size());
    }
}