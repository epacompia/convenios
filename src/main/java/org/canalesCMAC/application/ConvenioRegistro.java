package org.canalesCMAC.application;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.canalesCMAC.application.port.Autenticacion;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConvenioRegistro {

    private static final Logger LOG = Logger.getLogger(ConvenioRegistro.class);
    private static final String PREFIJO = "convenio.";
    private static final String AUTENTICACION_POR_DEFECTO = "ninguna";
    private static final String PROTOCOLO_POR_DEFECTO = "generic";
    private static final int REINTENTOS_POR_DEFECTO = 0;
    private static final long REINTENTOS_DELAY_POR_DEFECTO = 500L;
    private static final long TIMEOUT_POR_DEFECTO = 0L;

    private final Map<Institucion, Set<Operador>> operadores;
    private final Map<Institucion, String> protocolos;
    private final Map<Institucion, String> autenticaciones;
    private final Map<Institucion, String> urls;
    private final Map<Institucion, String> tokens;
    private final Map<Institucion, String> apiKeys;
    private final Map<Institucion, Integer> reintentos;
    private final Map<Institucion, Long> reintentosDelay;
    private final Map<Institucion, Long> timeouts;
    private final int reintentosGlobal;
    private final long reintentosDelayGlobal;
    private final long timeoutGlobal;
    private final Map<String, Autenticacion> autenticacionesPorTipo;

    @Inject
    public ConvenioRegistro(Config config, Instance<Autenticacion> autenticaciones) {
        this.operadores = parsearOperadores(config);
        this.protocolos = parsearValores(config, "protocolo");
        this.autenticaciones = parsearValores(config, "auth");
        this.urls = parsearValores(config, "url");
        this.tokens = parsearValores(config, "token");
        this.apiKeys = parsearValores(config, "apikey");
        this.reintentos = parsearEnteros(config, "reintentos");
        this.reintentosDelay = parsearLargos(config, "reintentos.delay");
        this.timeouts = parsearLargos(config, "timeout");
        this.reintentosGlobal = config.getOptionalValue("recaudacion.reintentos", Integer.class)
            .orElse(REINTENTOS_POR_DEFECTO);
        this.reintentosDelayGlobal = config.getOptionalValue("recaudacion.reintentos.delay", Long.class)
            .orElse(REINTENTOS_DELAY_POR_DEFECTO);
        this.timeoutGlobal = config.getOptionalValue("recaudacion.timeout", Long.class)
            .orElse(TIMEOUT_POR_DEFECTO);
        this.autenticacionesPorTipo = porTipo(autenticaciones);
        LOG.infof("convenios cargados=%d %s", operadores.size(), operadores.keySet());
    }

    public ConvenioRegistro(Map<Institucion, Set<Operador>> operadores, Map<Institucion, String> protocolos) {
        this.operadores = operadores;
        this.protocolos = protocolos;
        this.autenticaciones = Map.of();
        this.urls = Map.of();
        this.tokens = Map.of();
        this.apiKeys = Map.of();
        this.reintentos = Map.of();
        this.reintentosDelay = Map.of();
        this.timeouts = Map.of();
        this.reintentosGlobal = REINTENTOS_POR_DEFECTO;
        this.reintentosDelayGlobal = REINTENTOS_DELAY_POR_DEFECTO;
        this.timeoutGlobal = TIMEOUT_POR_DEFECTO;
        this.autenticacionesPorTipo = Map.of();
    }

    public Set<Institucion> instituciones() {
        return Collections.unmodifiableSet(operadores.keySet());
    }

    public boolean soporta(Institucion institucion) {
        return operadores.containsKey(institucion);
    }

    public boolean soporta(Institucion institucion, Operador operador) {
        Set<Operador> soportados = operadores.get(institucion);
        return soportados != null && soportados.contains(operador);
    }

    public Set<Operador> operadores(Institucion institucion) {
        return operadores.getOrDefault(institucion, Set.of());
    }

    public String protocolo(Institucion institucion) {
        return protocolos.getOrDefault(institucion, PROTOCOLO_POR_DEFECTO);
    }

    public String autenticacionTipo(Institucion institucion) {
        return autenticaciones.getOrDefault(institucion, AUTENTICACION_POR_DEFECTO);
    }

    public String url(Institucion institucion) {
        return urls.getOrDefault(institucion, "");
    }

    public String token(Institucion institucion) {
        return tokens.getOrDefault(institucion, "");
    }

    public String apiKey(Institucion institucion) {
        return apiKeys.getOrDefault(institucion, "");
    }

    public int reintentos(Institucion institucion) {
        return reintentos.getOrDefault(institucion, reintentosGlobal);
    }

    public long reintentosDelay(Institucion institucion) {
        return reintentosDelay.getOrDefault(institucion, reintentosDelayGlobal);
    }

    public long timeout(Institucion institucion) {
        return timeouts.getOrDefault(institucion, timeoutGlobal);
    }

    public Convenio convenio(Institucion institucion) {
        if (!operadores.containsKey(institucion)) {
            return null;
        }
        return new Convenio(institucion, protocolo(institucion), url(institucion),
            operadores(institucion), reintentos(institucion), reintentosDelay(institucion), timeout(institucion));
    }

    public Autenticacion autenticacion(Institucion institucion) {
        Autenticacion autenticacion = autenticacionesPorTipo.get(
            autenticaciones.getOrDefault(institucion, AUTENTICACION_POR_DEFECTO));
        return autenticacion == null ? Autenticacion.NINGUNA : autenticacion;
    }

    public String ruta(Institucion institucion) {
        return "direct:convenio." + institucion.name();
    }

    private static Map<Institucion, Set<Operador>> parsearOperadores(Config config) {
        Map<Institucion, Set<Operador>> resultado = new EnumMap<>(Institucion.class);
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO) || !propiedad.endsWith(".operadores")) {
                continue;
            }
            Institucion institucion = institucion(propiedad);
            if (institucion == null) {
                continue;
            }
            Set<Operador> soportados = EnumSet.noneOf(Operador.class);
            for (String operador : config.getValue(propiedad, String.class).split(",")) {
                if (!operador.isBlank()) {
                    soportados.add(Operador.valueOf(operador.trim()));
                }
            }
            resultado.put(institucion, soportados);
        }
        return resultado;
    }

    private static Map<Institucion, String> parsearValores(Config config, String sufijo) {
        Map<Institucion, String> resultado = new EnumMap<>(Institucion.class);
        String terminacion = "." + sufijo;
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO) || !propiedad.endsWith(terminacion)) {
                continue;
            }
            Institucion institucion = institucion(propiedad);
            if (institucion != null) {
                config.getOptionalValue(propiedad, String.class)
                    .filter(valor -> !valor.isBlank())
                    .ifPresent(valor -> resultado.put(institucion, valor));
            }
        }
        return resultado;
    }

    private static Map<Institucion, Integer> parsearEnteros(Config config, String sufijo) {
        Map<Institucion, Integer> resultado = new EnumMap<>(Institucion.class);
        String terminacion = "." + sufijo;
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO) || !propiedad.endsWith(terminacion)) {
                continue;
            }
            Institucion institucion = institucion(propiedad);
            if (institucion != null) {
                resultado.put(institucion, config.getValue(propiedad, Integer.class));
            }
        }
        return resultado;
    }

    private static Map<Institucion, Long> parsearLargos(Config config, String sufijo) {
        Map<Institucion, Long> resultado = new EnumMap<>(Institucion.class);
        String terminacion = "." + sufijo;
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO) || !propiedad.endsWith(terminacion)) {
                continue;
            }
            Institucion institucion = institucion(propiedad);
            if (institucion != null) {
                resultado.put(institucion, config.getValue(propiedad, Long.class));
            }
        }
        return resultado;
    }

    private static Map<String, Autenticacion> porTipo(Instance<Autenticacion> autenticaciones) {
        Map<String, Autenticacion> resultado = new HashMap<>();
        for (Autenticacion autenticacion : autenticaciones) {
            resultado.put(autenticacion.tipo(), autenticacion);
        }
        return resultado;
    }

    private static Institucion institucion(String propiedad) {
        String[] partes = propiedad.split("\\.");
        if (partes.length < 2) {
            return null;
        }
        for (Institucion institucion : Institucion.values()) {
            if (institucion.name().equalsIgnoreCase(partes[1])) {
                return institucion;
            }
        }
        return null;
    }
}
