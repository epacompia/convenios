package org.canalesCMAC.application;

import java.util.Map;

import org.canalesCMAC.domain.model.Institucion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogoCodigosTest {

    @Test
    void usaElMensajeConfigurado() {
        CatalogoCodigos catalogo = new CatalogoCodigos(
            Map.of(Institucion.SEAL, Map.of("07", "CONTRATO NO ESTA DISPONIBLE PARA SER PAGADO")));
        assertEquals("CONTRATO NO ESTA DISPONIBLE PARA SER PAGADO",
            catalogo.mensaje(Institucion.SEAL, "07", "mensaje del proveedor"));
    }

    @Test
    void usaElMensajeDelProveedorPorDefecto() {
        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());
        assertEquals("mensaje del proveedor",
            catalogo.mensaje(Institucion.ELSE, "00", "mensaje del proveedor"));
    }
}
