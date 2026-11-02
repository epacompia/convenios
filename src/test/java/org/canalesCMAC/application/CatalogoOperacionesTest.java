package org.canalesCMAC.application;

import java.util.Map;

import org.canalesCMAC.application.CatalogoOperaciones.OperacionSpec;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoOperacionesTest {

    @Test
    void devuelveLaOperacionConfigurada() {
        CatalogoOperaciones catalogo = new CatalogoOperaciones(Map.of(
            Institucion.SEAL, Map.of(Operador.PAGO_DEUDA, new OperacionSpec("0200", "210000", true, false))));

        OperacionSpec spec = catalogo.operacion(Institucion.SEAL, Operador.PAGO_DEUDA);

        assertEquals("0200", spec.mti());
        assertEquals("210000", spec.processingCode());
        assertTrue(spec.conMonto());
    }

    @Test
    void rechazaOperacionNoConfigurada() {
        CatalogoOperaciones catalogo = new CatalogoOperaciones(Map.of());

        assertThrows(OperadorNoSoportadoException.class,
            () -> catalogo.operacion(Institucion.SEAL, Operador.ANULACION));
    }
}
