package org.canalesCMAC.application;

import java.util.EnumSet;
import java.util.Map;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConvenioRegistroTest {

    private final ConvenioRegistro registro = new ConvenioRegistro(
        Map.of(Institucion.SEAL, EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA)),
        Map.of(Institucion.SEAL, "iso8583"));

    @Test
    void describeElConvenioSoportado() {
        Convenio convenio = registro.convenio(Institucion.SEAL);

        assertEquals(Institucion.SEAL, convenio.institucion());
        assertEquals("iso8583", convenio.protocolo());
        assertEquals(EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA), convenio.operadores());
    }

    @Test
    void devuelveNuloParaInstitucionNoRegistrada() {
        assertNull(registro.convenio(Institucion.ELSE));
    }
}
