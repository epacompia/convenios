package org.canalesCMAC.application;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.canalesCMAC.domain.model.Institucion;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CatalogoCodigosTest {

    @Test
    void usaElMensajeConfigurado() {

        CatalogoCodigos catalogo = new CatalogoCodigos(
                Map.of(
                        Institucion.SEAL,
                        Map.of(
                                "07",
                                "CONTRATO NO ESTA DISPONIBLE PARA SER PAGADO"
                        )
                )
        );

        assertEquals(
                "CONTRATO NO ESTA DISPONIBLE PARA SER PAGADO",
                catalogo.mensaje(
                        Institucion.SEAL,
                        "07",
                        "mensaje del proveedor"
                )
        );
    }

    @Test
    void usaElMensajeDelProveedorPorDefecto() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals(
                "mensaje del proveedor",
                catalogo.mensaje(
                        Institucion.ELSE,
                        "00",
                        "mensaje del proveedor"
                )
        );
    }

    @Test
    void usaElMensajeDelProveedorCuandoCodigoNoEstaConfigurado() {

        CatalogoCodigos catalogo = new CatalogoCodigos(
                Map.of(
                        Institucion.SEAL,
                        Map.of("07", "MENSAJE CONFIGURADO")
                )
        );

        assertEquals(
                "mensaje del proveedor",
                catalogo.mensaje(
                        Institucion.SEAL,
                        "99",
                        "mensaje del proveedor"
                )
        );
    }

    @Test
    void devuelveMensajeProveedorNuloCuandoNoExisteConfiguracion() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertNull(
                catalogo.mensaje(
                        Institucion.SEAL,
                        "99",
                        null
                )
        );
    }

    @Test
    void normalizaCodigoCero() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("00", catalogo.normalizar("0"));
    }

    @Test
    void normalizaCodigoCuatroCeros() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("00", catalogo.normalizar("0000"));
    }

    @Test
    void normalizaCodigoCincoCeros() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("00", catalogo.normalizar("00000"));
    }

    @Test
    void devuelveCodigo99CuandoCodigoEsNulo() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("99", catalogo.normalizar(null));
    }

    @Test
    void devuelveCodigo99CuandoCodigoEstaVacio() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("99", catalogo.normalizar(""));
    }

    @Test
    void devuelveCodigo99CuandoCodigoContieneEspacios() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("99", catalogo.normalizar("   "));
    }

    @Test
    void eliminaEspaciosAlNormalizarCodigo() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("07", catalogo.normalizar(" 07 "));
    }

    @Test
    void conservaCodigoDiferenteDeCero() {

        CatalogoCodigos catalogo = new CatalogoCodigos(Map.of());

        assertEquals("12", catalogo.normalizar("12"));
    }

    @Test
    void cargaMensajesDesdeConfiguracion() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.codigo.07",
                "CONTRATO NO ESTA DISPONIBLE"
        );

        propiedades.put(
                "convenio.SEAL.codigo.08",
                "CONTRATO YA CANCELADO"
        );

        CatalogoCodigos catalogo = new CatalogoCodigos(
                crearConfig(propiedades)
        );

        assertEquals(
                "CONTRATO NO ESTA DISPONIBLE",
                catalogo.mensaje(Institucion.SEAL, "07", "mensaje proveedor")
        );

        assertEquals(
                "CONTRATO YA CANCELADO",
                catalogo.mensaje(Institucion.SEAL, "08", "mensaje proveedor")
        );
    }

    @Test
    void ignoraPropiedadesConPrefijoIncorrecto() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "otra.SEAL.codigo.07",
                "MENSAJE CONFIGURADO"
        );

        CatalogoCodigos catalogo = new CatalogoCodigos(
                crearConfig(propiedades)
        );

        assertEquals(
                "mensaje proveedor",
                catalogo.mensaje(
                        Institucion.SEAL,
                        "07",
                        "mensaje proveedor"
                )
        );
    }

    @Test
    void ignoraPropiedadesConCantidadIncorrectaDeSegmentos() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.codigo",
                "MENSAJE CONFIGURADO"
        );

        propiedades.put(
                "convenio.SEAL.codigo.07.extra",
                "MENSAJE CONFIGURADO"
        );

        CatalogoCodigos catalogo = new CatalogoCodigos(
                crearConfig(propiedades)
        );

        assertEquals(
                "mensaje proveedor",
                catalogo.mensaje(
                        Institucion.SEAL,
                        "07",
                        "mensaje proveedor"
                )
        );
    }

    @Test
    void ignoraPropiedadesConMarcadorIncorrecto() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.operacion.07",
                "MENSAJE CONFIGURADO"
        );

        CatalogoCodigos catalogo = new CatalogoCodigos(
                crearConfig(propiedades)
        );

        assertEquals(
                "mensaje proveedor",
                catalogo.mensaje(
                        Institucion.SEAL,
                        "07",
                        "mensaje proveedor"
                )
        );
    }

    @Test
    void ignoraInstitucionDesconocida() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.INEXISTENTE.codigo.07",
                "MENSAJE CONFIGURADO"
        );

        CatalogoCodigos catalogo = new CatalogoCodigos(
                crearConfig(propiedades)
        );

        assertEquals(
                "mensaje proveedor",
                catalogo.mensaje(
                        Institucion.SEAL,
                        "07",
                        "mensaje proveedor"
                )
        );
    }

    @Test
    void cargaInstitucionSinDiferenciarMayusculas() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.seal.codigo.07",
                "MENSAJE CONFIGURADO"
        );

        CatalogoCodigos catalogo = new CatalogoCodigos(
                crearConfig(propiedades)
        );

        assertEquals(
                "MENSAJE CONFIGURADO",
                catalogo.mensaje(
                        Institucion.SEAL,
                        "07",
                        "mensaje proveedor"
                )
        );
    }

    private Config crearConfig(Map<String, String> propiedades) {

        return (Config) Proxy.newProxyInstance(
                Config.class.getClassLoader(),
                new Class<?>[]{Config.class},
                (proxy, method, args) -> {

                    switch (method.getName()) {

                        case "getPropertyNames":
                            return propiedades.keySet();

                        case "getValue":

                            String nombre = (String) args[0];
                            Class<?> tipo = (Class<?>) args[1];

                            String valor = propiedades.get(nombre);

                            if (valor == null) {
                                throw new IllegalArgumentException(
                                        "Propiedad no encontrada: " + nombre
                                );
                            }

                            return tipo.cast(valor);

                        case "toString":
                            return "ConfigTest";

                        case "hashCode":
                            return System.identityHashCode(proxy);

                        case "equals":
                            return proxy == args[0];

                        default:
                            throw new UnsupportedOperationException(
                                    "Método Config no implementado: "
                                            + method.getName()
                            );
                    }
                }
        );
    }
}