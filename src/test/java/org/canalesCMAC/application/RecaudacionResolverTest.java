package org.canalesCMAC.application;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecaudacionResolverTest {

    private final RecaudacionResolver resolver = new RecaudacionResolver(registro());

    private static ConvenioRegistro registro() {
        Map<Institucion, Set<Operador>> operadores = new EnumMap<>(Institucion.class);
        operadores.put(Institucion.ELSE,
            EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA, Operador.CONSULTA_PAGO, Operador.EXTORNO_PAGO));
        operadores.put(Institucion.ELECTRO_UCAYALI,
            EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA, Operador.EXTORNO_PAGO,
                Operador.EXTORNO_PAGO_AUTO, Operador.EXTORNO_AUTO));
        operadores.put(Institucion.SEAL,
            EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA, Operador.EXTORNO_PAGO, Operador.ANULACION));
        return new ConvenioRegistro(operadores, Map.of());
    }

    @Test
    void aceptaConsultaDeudaDeElse() {
        assertDoesNotThrow(() -> resolver.validar(Institucion.ELSE, Operador.CONSULTA_DEUDA));
    }

    @Test
    void aceptaExtornoAutomaticoDeElectroUcayali() {
        assertDoesNotThrow(() -> resolver.validar(Institucion.ELECTRO_UCAYALI, Operador.EXTORNO_AUTO));
    }

    @Test
    void rechazaConsultaPagoEnElectroUcayali() {
        assertThrows(OperadorNoSoportadoException.class,
            () -> resolver.validar(Institucion.ELECTRO_UCAYALI, Operador.CONSULTA_PAGO));
    }

    @Test
    void rechazaExternoAutomaticoEnElse() {
        assertThrows(OperadorNoSoportadoException.class,
            () -> resolver.validar(Institucion.ELSE, Operador.EXTORNO_PAGO_AUTO));
    }

    @Test
    void aceptaAnulacionDeSeal() {
        assertDoesNotThrow(() -> resolver.validar(Institucion.SEAL, Operador.ANULACION));
    }

    @Test
    void rechazaConsultaPagoEnSeal() {
        assertThrows(OperadorNoSoportadoException.class,
            () -> resolver.validar(Institucion.SEAL, Operador.CONSULTA_PAGO));
    }
}