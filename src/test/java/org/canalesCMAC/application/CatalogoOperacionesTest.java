package org.canalesCMAC.application;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.canalesCMAC.application.CatalogoOperaciones.OperacionSpec;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoOperacionesTest {

    @Test
    void devuelveLaOperacionConfigurada() {

        CatalogoOperaciones catalogo = new CatalogoOperaciones(Map.of(
                Institucion.SEAL,
                Map.of(
                        Operador.PAGO_DEUDA,
                        new OperacionSpec("0200", "210000", true, false)
                )
        ));

        OperacionSpec spec = catalogo.operacion(
                Institucion.SEAL,
                Operador.PAGO_DEUDA
        );

        assertEquals("0200", spec.mti());
        assertEquals("210000", spec.processingCode());
        assertTrue(spec.conMonto());
        assertFalse(spec.requiereAprobacion());
    }

    @Test
    void devuelveOperacionConAprobacionRequerida() {

        CatalogoOperaciones catalogo = new CatalogoOperaciones(Map.of(
                Institucion.SEAL,
                Map.of(
                        Operador.PAGO_DEUDA,
                        new OperacionSpec("0200", "210000", true, true)
                )
        ));

        OperacionSpec spec = catalogo.operacion(
                Institucion.SEAL,
                Operador.PAGO_DEUDA
        );

        assertTrue(spec.conMonto());
        assertTrue(spec.requiereAprobacion());
    }

    @Test
    void devuelveOperacionSinMontoNiAprobacion() {

        CatalogoOperaciones catalogo = new CatalogoOperaciones(Map.of(
                Institucion.SEAL,
                Map.of(
                        Operador.PAGO_DEUDA,
                        new OperacionSpec("0200", "210000", false, false)
                )
        ));

        OperacionSpec spec = catalogo.operacion(
                Institucion.SEAL,
                Operador.PAGO_DEUDA
        );

        assertFalse(spec.conMonto());
        assertFalse(spec.requiereAprobacion());
    }

    @Test
    void rechazaOperacionNoConfigurada() {

        CatalogoOperaciones catalogo = new CatalogoOperaciones(Map.of());

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> catalogo.operacion(
                        Institucion.SEAL,
                        Operador.ANULACION
                )
        );
    }

    @Test
    void rechazaOperadorNoConfiguradoParaInstitucion() {

        CatalogoOperaciones catalogo = new CatalogoOperaciones(Map.of(
                Institucion.SEAL,
                Map.of(
                        Operador.PAGO_DEUDA,
                        new OperacionSpec("0200", "210000", true, false)
                )
        ));

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> catalogo.operacion(
                        Institucion.SEAL,
                        Operador.ANULACION
                )
        );
    }

    @Test
    void cargaOperacionDesdeConfiguracion() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.mti",
                "0200"
        );
        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.processingCode",
                "210000"
        );
        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.conMonto",
                "true"
        );
        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.requiereAprobacion",
                "true"
        );

        CatalogoOperaciones catalogo = new CatalogoOperaciones(
                crearConfig(propiedades)
        );

        OperacionSpec spec = catalogo.operacion(
                Institucion.SEAL,
                Operador.PAGO_DEUDA
        );

        assertEquals("0200", spec.mti());
        assertEquals("210000", spec.processingCode());
        assertTrue(spec.conMonto());
        assertTrue(spec.requiereAprobacion());
    }

    @Test
    void utilizaValoresBooleanosPorDefecto() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.mti",
                "0200"
        );
        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.processingCode",
                "210000"
        );

        CatalogoOperaciones catalogo = new CatalogoOperaciones(
                crearConfig(propiedades)
        );

        OperacionSpec spec = catalogo.operacion(
                Institucion.SEAL,
                Operador.PAGO_DEUDA
        );

        assertFalse(spec.conMonto());
        assertFalse(spec.requiereAprobacion());
    }

    @Test
    void interpretaBooleanosComoFalseCuandoElValorNoEsTrue() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.mti",
                "0200"
        );
        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.processingCode",
                "210000"
        );
        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.conMonto",
                "valor-invalido"
        );
        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.requiereAprobacion",
                "false"
        );

        CatalogoOperaciones catalogo = new CatalogoOperaciones(
                crearConfig(propiedades)
        );

        OperacionSpec spec = catalogo.operacion(
                Institucion.SEAL,
                Operador.PAGO_DEUDA
        );

        assertFalse(spec.conMonto());
        assertFalse(spec.requiereAprobacion());
    }

    @Test
    void ignoraPropiedadesConPrefijoIncorrecto() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "otra.SEAL.operacion.PAGO_DEUDA.mti",
                "0200"
        );
        propiedades.put(
                "otra.SEAL.operacion.PAGO_DEUDA.processingCode",
                "210000"
        );

        CatalogoOperaciones catalogo = new CatalogoOperaciones(
                crearConfig(propiedades)
        );

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> catalogo.operacion(
                        Institucion.SEAL,
                        Operador.PAGO_DEUDA
                )
        );
    }

    @Test
    void ignoraPropiedadesConCantidadIncorrectaDeSegmentos() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.PAGO_DEUDA.mti",
                "0200"
        );
        propiedades.put(
                "convenio.SEAL.PAGO_DEUDA.processingCode",
                "210000"
        );

        CatalogoOperaciones catalogo = new CatalogoOperaciones(
                crearConfig(propiedades)
        );

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> catalogo.operacion(
                        Institucion.SEAL,
                        Operador.PAGO_DEUDA
                )
        );
    }

    @Test
    void ignoraPropiedadesConMarcadorIncorrecto() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.config.PAGO_DEUDA.mti",
                "0200"
        );
        propiedades.put(
                "convenio.SEAL.config.PAGO_DEUDA.processingCode",
                "210000"
        );

        CatalogoOperaciones catalogo = new CatalogoOperaciones(
                crearConfig(propiedades)
        );

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> catalogo.operacion(
                        Institucion.SEAL,
                        Operador.PAGO_DEUDA
                )
        );
    }

    @Test
    void ignoraInstitucionDesconocida() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.INEXISTENTE.operacion.PAGO_DEUDA.mti",
                "0200"
        );
        propiedades.put(
                "convenio.INEXISTENTE.operacion.PAGO_DEUDA.processingCode",
                "210000"
        );

        CatalogoOperaciones catalogo = new CatalogoOperaciones(
                crearConfig(propiedades)
        );

        assertThrows(
                OperadorNoSoportadoException.class,
                () -> catalogo.operacion(
                        Institucion.SEAL,
                        Operador.PAGO_DEUDA
                )
        );
    }

    @Test
    void fallaCuandoNoExisteMti() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.processingCode",
                "210000"
        );

        assertThrows(
                IllegalStateException.class,
                () -> new CatalogoOperaciones(crearConfig(propiedades))
        );
    }

    @Test
    void fallaCuandoNoExisteProcessingCode() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.operacion.PAGO_DEUDA.mti",
                "0200"
        );

        assertThrows(
                IllegalStateException.class,
                () -> new CatalogoOperaciones(crearConfig(propiedades))
        );
    }

    @Test
    void fallaCuandoOperadorConfiguradoNoExiste() {

        Map<String, String> propiedades = new HashMap<>();

        propiedades.put(
                "convenio.SEAL.operacion.OPERADOR_INVALIDO.mti",
                "0200"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CatalogoOperaciones(crearConfig(propiedades))
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

                        case "getOptionalValue":
                            String propiedad = (String) args[0];
                            Class<?> tipoOpcional = (Class<?>) args[1];

                            String valorOpcional = propiedades.get(propiedad);

                            if (valorOpcional == null) {
                                return java.util.Optional.empty();
                            }

                            return java.util.Optional.of(
                                    tipoOpcional.cast(valorOpcional)
                            );

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