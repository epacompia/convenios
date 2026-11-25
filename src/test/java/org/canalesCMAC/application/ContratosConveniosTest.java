package org.canalesCMAC.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.EnumSet;
import java.util.Set;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ContratosConveniosTest {

    @Inject
    ConvenioRegistro registro;

    @Inject
    Instance<MapeadorOperacion> mapeadores;

    @Inject
    MatrizObligatoriedad matriz;

    @Test
    void cadaConvenioConfiguradoTieneMapper() {
        Set<Institucion> conMapper = EnumSet.noneOf(Institucion.class);
        for (MapeadorOperacion mapeador : mapeadores) {
            conMapper.add(mapeador.institucion());
        }

        assertEquals(12, registro.instituciones().size());
        assertEquals(registro.instituciones(), conMapper);
    }

    @Test
    void cadaOperadorConfiguradoTieneContratoEnLaMatriz() {
        for (Institucion institucion : registro.instituciones()) {
            Set<Operador> operadores = registro.operadores(institucion);
            assertFalse(operadores.isEmpty(),
                "El convenio " + institucion + " no tiene operadores configurados");
            for (Operador operador : operadores) {
                assertNotNull(matriz.requeridos(institucion, operador),
                    "Falta la matriz para " + institucion + "/" + operador);
            }
        }
    }
}
