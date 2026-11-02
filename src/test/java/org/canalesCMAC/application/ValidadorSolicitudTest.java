package org.canalesCMAC.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.junit.jupiter.api.Test;

class ValidadorSolicitudTest {

    private final ValidadorSolicitud validador =
        new ValidadorSolicitud(new MatrizObligatoriedad());

    @Test
    void aceptaElGrupoAlternativoContratoONumeroSuministro() {
        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.SEAL, Operador.PAGO_DEUDA, Cabecera.generar(),
            new Datos(Map.of("contrato", "98787"), Map.of(),
                List.of(Map.of("monto", "10.00"))));

        assertDoesNotThrow(() -> validador.validar(solicitud));
    }

    @Test
    void rechazaListasVaciasCuandoElCampoEsObligatorio() {
        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.WESTERN_UNION, Operador.CONSULTAR_TRANSACCION, Cabecera.generar(),
            new Datos(Map.of("idProducto", "P1", "parametrosOperacion", List.of()), Map.of(),
                List.of()));

        assertThrows(IllegalArgumentException.class, () -> validador.validar(solicitud));
    }

    @Test
    void rechazaFechaDeCabeceraSinZonaHoraria() {
        Cabecera cabecera = new Cabecera("20260218000123000042", "0", "7", "SIST",
            "127.0.0.1", "2026-02-18 10:19:47", "21", "56");
        RecaudacionRequest solicitud = new RecaudacionRequest(
            Institucion.ELSE, Operador.CONSULTA_DEUDA, cabecera,
            new Datos(Map.of("datoConsulta", "0010681504"), Map.of(), List.of()));

        assertThrows(IllegalArgumentException.class, () -> validador.validar(solicitud));
    }
}
