package org.canalesCMAC.application;

import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.config.Config;
import org.canalesCMAC.application.port.Autenticacion;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConvenioRegistroTest {

    private final ConvenioRegistro registro = new ConvenioRegistro(
            Map.of(
                    Institucion.SEAL,
                    EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA)
            ),
            Map.of(Institucion.SEAL, "iso8583")
    );

    @Test
    void describeElConvenioSoportado() {
        var convenio = registro.convenio(Institucion.SEAL);

        assertNotNull(convenio);
        assertEquals(Institucion.SEAL, convenio.institucion());
        assertEquals("iso8583", convenio.protocolo());
    }

    @Test
    void devuelveNuloParaInstitucionNoRegistrada() {
        assertNull(registro.convenio(Institucion.ELSE));
    }

    @Test
    void devuelveInstitucionesRegistradas() {
        assertEquals(Set.of(Institucion.SEAL), registro.instituciones());
    }

    @Test
    void institucionesNoPermiteModificaciones() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> registro.instituciones().add(Institucion.ELSE)
        );
    }

    @Test
    void validaInstitucionSoportada() {
        assertTrue(registro.soporta(Institucion.SEAL));
    }

    @Test
    void validaInstitucionNoSoportada() {
        assertFalse(registro.soporta(Institucion.ELSE));
    }

    @Test
    void validaOperadorSoportado() {
        assertTrue(registro.soporta(
                Institucion.SEAL,
                Operador.CONSULTA_DEUDA
        ));
    }

    @Test
    void validaOperadorNoSoportado() {
        assertFalse(registro.soporta(
                Institucion.SEAL,
                Operador.EXTORNO_PAGO
        ));
    }

    @Test
    void devuelveOperadoresDeInstitucion() {
        assertEquals(
                EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA),
                registro.operadores(Institucion.SEAL)
        );
    }

    @Test
    void devuelveListaVaciaParaInstitucionSinOperadores() {
        assertTrue(registro.operadores(Institucion.ELSE).isEmpty());
    }

    @Test
    void devuelveProtocoloPorDefecto() {
        assertEquals("generic", registro.protocolo(Institucion.ELSE));
    }

    @Test
    void devuelveAutenticacionPorDefecto() {
        assertEquals("ninguna", registro.autenticacionTipo(Institucion.ELSE));
    }

    @Test
    void devuelveUrlVaciaPorDefecto() {
        assertEquals("", registro.url(Institucion.SEAL));
    }

    @Test
    void devuelveTokenVacioPorDefecto() {
        assertEquals("", registro.token(Institucion.SEAL));
    }

    @Test
    void devuelveApiKeyVaciaPorDefecto() {
        assertEquals("", registro.apiKey(Institucion.SEAL));
    }

    @Test
    void devuelveReintentosPorDefecto() {
        assertEquals(0, registro.reintentos(Institucion.SEAL));
    }

    @Test
    void devuelveDelayPorDefecto() {
        assertEquals(500L, registro.reintentosDelay(Institucion.SEAL));
    }

    @Test
    void devuelveTimeoutPorDefecto() {
        assertEquals(0L, registro.timeout(Institucion.SEAL));
    }

    @Test
    void devuelveAutenticacionNingunaPorDefecto() {
        assertEquals(
                Autenticacion.NINGUNA,
                registro.autenticacion(Institucion.SEAL)
        );
    }

    @Test
    void construyeRutaDeInstitucion() {
        assertEquals("direct:convenio.SEAL", registro.ruta(Institucion.SEAL));
    }

    @Test
    void cargaConfiguracionCompleta() {

        Map<String, Object> propiedades = new HashMap<>();

        propiedades.put("convenio.SEAL.operadores", "CONSULTA_DEUDA,PAGO_DEUDA");
        propiedades.put("convenio.SEAL.protocolo", "soap");
        propiedades.put("convenio.SEAL.auth", "basica");
        propiedades.put("convenio.SEAL.url", "http://localhost/servicio");
        propiedades.put("convenio.SEAL.token", "token-prueba");
        propiedades.put("convenio.SEAL.apikey", "api-prueba");
        propiedades.put("convenio.SEAL.reintentos", 3);
        propiedades.put("convenio.SEAL.reintentos.delay", 1000L);
        propiedades.put("convenio.SEAL.timeout", 5000L);

        propiedades.put("recaudacion.reintentos", 5);
        propiedades.put("recaudacion.reintentos.delay", 2000L);
        propiedades.put("recaudacion.timeout", 8000L);

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertTrue(configurado.soporta(Institucion.SEAL));
        assertTrue(configurado.soporta(
                Institucion.SEAL,
                Operador.CONSULTA_DEUDA
        ));

        assertEquals("soap", configurado.protocolo(Institucion.SEAL));
        assertEquals("basica", configurado.autenticacionTipo(Institucion.SEAL));
        assertEquals("http://localhost/servicio", configurado.url(Institucion.SEAL));
        assertEquals("token-prueba", configurado.token(Institucion.SEAL));
        assertEquals("api-prueba", configurado.apiKey(Institucion.SEAL));
        assertEquals(3, configurado.reintentos(Institucion.SEAL));
        assertEquals(1000L, configurado.reintentosDelay(Institucion.SEAL));
        assertEquals(5000L, configurado.timeout(Institucion.SEAL));
    }

    @Test
    void utilizaConfiguracionGlobalCuandoNoExisteConfiguracionPorInstitucion() {

        Map<String, Object> propiedades = new HashMap<>();

        propiedades.put("convenio.SEAL.operadores", "CONSULTA_DEUDA");
        propiedades.put("recaudacion.reintentos", 4);
        propiedades.put("recaudacion.reintentos.delay", 1500L);
        propiedades.put("recaudacion.timeout", 6000L);

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertEquals(4, configurado.reintentos(Institucion.SEAL));
        assertEquals(1500L, configurado.reintentosDelay(Institucion.SEAL));
        assertEquals(6000L, configurado.timeout(Institucion.SEAL));
    }

    @Test
    void utilizaValoresGlobalesPorDefecto() {

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("convenio.SEAL.operadores", "CONSULTA_DEUDA");

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertEquals(0, configurado.reintentos(Institucion.SEAL));
        assertEquals(500L, configurado.reintentosDelay(Institucion.SEAL));
        assertEquals(0L, configurado.timeout(Institucion.SEAL));
    }

    @Test
    void ignoraPropiedadesQueNoPertenecenAlConvenio() {

        Map<String, Object> propiedades = new HashMap<>();

        propiedades.put("otra.SEAL.operadores", "CONSULTA_DEUDA");
        propiedades.put("convenio.SEAL.operadores.extra", "PAGO_DEUDA");
        propiedades.put("convenio.INEXISTENTE.operadores", "CONSULTA_DEUDA");

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertFalse(configurado.soporta(Institucion.SEAL));
        assertTrue(configurado.instituciones().isEmpty());
    }

    @Test
    void ignoraValoresDeConfiguracionVacios() {

        Map<String, Object> propiedades = new HashMap<>();

        propiedades.put("convenio.SEAL.operadores", "CONSULTA_DEUDA");
        propiedades.put("convenio.SEAL.protocolo", " ");
        propiedades.put("convenio.SEAL.auth", "");
        propiedades.put("convenio.SEAL.url", " ");
        propiedades.put("convenio.SEAL.token", "");
        propiedades.put("convenio.SEAL.apikey", " ");

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertEquals("generic", configurado.protocolo(Institucion.SEAL));
        assertEquals("ninguna", configurado.autenticacionTipo(Institucion.SEAL));
        assertEquals("", configurado.url(Institucion.SEAL));
        assertEquals("", configurado.token(Institucion.SEAL));
        assertEquals("", configurado.apiKey(Institucion.SEAL));
    }

    @Test
    void permiteOperadoresSeparadosPorComasYEspacios() {

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(
                "convenio.SEAL.operadores",
                " CONSULTA_DEUDA , PAGO_DEUDA "
        );

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertTrue(configurado.soporta(Institucion.SEAL, Operador.CONSULTA_DEUDA));
        assertTrue(configurado.soporta(Institucion.SEAL, Operador.PAGO_DEUDA));
    }

    @Test
    void ignoraOperadoresVacios() {

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(
                "convenio.SEAL.operadores",
                "CONSULTA_DEUDA, ,PAGO_DEUDA"
        );

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertEquals(2, configurado.operadores(Institucion.SEAL).size());
    }

    @Test
    void fallaCuandoOperadorConfiguradoNoExiste() {

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("convenio.SEAL.operadores", "OPERADOR_INVALIDO");

        assertThrows(
                IllegalArgumentException.class,
                () -> crearRegistro(propiedades)
        );
    }

    @Test
    void utilizaProtocoloPorDefectoParaInstitucionSinProtocolo() {

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("convenio.SEAL.operadores", "CONSULTA_DEUDA");

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertEquals("generic", configurado.protocolo(Institucion.SEAL));
    }

    @Test
    void utilizaConfiguracionGlobalCuandoNoHayValorIndividual() {

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("convenio.SEAL.operadores", "CONSULTA_DEUDA");
        propiedades.put("recaudacion.reintentos", 2);
        propiedades.put("recaudacion.reintentos.delay", 700L);
        propiedades.put("recaudacion.timeout", 3000L);

        ConvenioRegistro configurado = crearRegistro(propiedades);

        assertEquals(2, configurado.reintentos(Institucion.SEAL));
        assertEquals(700L, configurado.reintentosDelay(Institucion.SEAL));
        assertEquals(3000L, configurado.timeout(Institucion.SEAL));
    }

    private ConvenioRegistro crearRegistro(Map<String, Object> propiedades) {

        Config config = (Config) Proxy.newProxyInstance(
                Config.class.getClassLoader(),
                new Class<?>[]{Config.class},
                (proxy, method, args) -> {

                    String nombreMetodo = method.getName();

                    if ("getPropertyNames".equals(nombreMetodo)) {
                        return propiedades.keySet();
                    }

                    if ("getOptionalValue".equals(nombreMetodo)) {
                        String nombre = (String) args[0];
                        Class<?> tipo = (Class<?>) args[1];

                        Object valor = propiedades.get(nombre);

                        if (valor == null) {
                            return Optional.empty();
                        }

                        return Optional.of(tipo.cast(valor));
                    }

                    if ("getValue".equals(nombreMetodo)) {
                        String nombre = (String) args[0];
                        Class<?> tipo = (Class<?>) args[1];

                        Object valor = propiedades.get(nombre);

                        if (valor == null) {
                            throw new IllegalArgumentException(
                                    "Propiedad no encontrada: " + nombre
                            );
                        }

                        return tipo.cast(valor);
                    }

                    if ("unwrap".equals(nombreMetodo)) {
                        return null;
                    }

                    if ("toString".equals(nombreMetodo)) {
                        return "ConfigTest";
                    }

                    if ("hashCode".equals(nombreMetodo)) {
                        return System.identityHashCode(proxy);
                    }

                    if ("equals".equals(nombreMetodo)) {
                        return proxy == args[0];
                    }

                    throw new UnsupportedOperationException(
                            "Método Config no implementado: " + nombreMetodo
                    );
                }
        );

        Instance<Autenticacion> autenticaciones = crearInstanciaVacia();

        return new ConvenioRegistro(config, autenticaciones);
    }

    private Instance<Autenticacion> crearInstanciaVacia() {

        return (Instance<Autenticacion>) Proxy.newProxyInstance(
                Instance.class.getClassLoader(),
                new Class<?>[]{Instance.class},
                (proxy, method, args) -> {

                    if ("iterator".equals(method.getName())) {
                        return List.<Autenticacion>of().iterator();
                    }

                    if ("isUnsatisfied".equals(method.getName())) {
                        return true;
                    }

                    if ("isAmbiguous".equals(method.getName())) {
                        return false;
                    }

                    if ("isResolvable".equals(method.getName())) {
                        return false;
                    }

                    if ("toString".equals(method.getName())) {
                        return "InstanceTest";
                    }

                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }

                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }

                    throw new UnsupportedOperationException(
                            "Método Instance no implementado: " + method.getName()
                    );
                }
        );
    }
}