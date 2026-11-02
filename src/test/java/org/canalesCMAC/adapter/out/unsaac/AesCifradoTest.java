package org.canalesCMAC.adapter.out.unsaac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AesCifradoTest {

    private static final String CLAVE_PRUEBAS = "_aaaae00th@h%h1234";

    @Test
    void descifraLosEjemplosDeLaColeccionPostman() {
        assertEquals("1760986", AesCifrado.descifrar(
            "U2FsdGVkX1+NNvaNFtZYXQOWpv/638O0lD5wIHwfVsw=", CLAVE_PRUEBAS));
        assertEquals("O-000000001760986", AesCifrado.descifrar(
            "U2FsdGVkX1/03HSmi2P+Uq1Jjq2YqqcRbHyZXAarXgVMqO1lSSmLYxlWZXIbch+1",
            CLAVE_PRUEBAS));
    }

    @Test
    void cifraYDescifraConLaClaveDeProduccion() {
        String original = "{\"codigo_transaccion\":\"O-000000001760986\",\"clave\":\"23300\"}";
        String cifrado = AesCifrado.cifrar(original, "xaadaippth@@%m5678");

        assertTrue(cifrado.startsWith("U2FsdGVkX1"));
        assertEquals(original,
            AesCifrado.descifrar(cifrado, "xaadaippth@@%m5678"));
    }
}
