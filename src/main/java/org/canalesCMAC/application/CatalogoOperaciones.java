package org.canalesCMAC.application;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CatalogoOperaciones {

    private static final Logger LOG = Logger.getLogger(CatalogoOperaciones.class);
    private static final String PREFIJO = "convenio.";
    private static final String MARCADOR = "operacion";

    private final Map<Institucion, Map<Operador, OperacionSpec>> operaciones;

    @Inject
    public CatalogoOperaciones(Config config) {
        this(parsear(config));
    }

    public CatalogoOperaciones(Map<Institucion, Map<Operador, OperacionSpec>> operaciones) {
        this.operaciones = operaciones;
    }

    public OperacionSpec operacion(Institucion institucion, Operador operador) {
        Map<Operador, OperacionSpec> porOperador = operaciones.get(institucion);
        OperacionSpec spec = porOperador == null ? null : porOperador.get(operador);
        if (spec == null) {
            throw new OperadorNoSoportadoException(
                "Operador " + operador + " no tiene operación configurada para " + institucion);
        }
        return spec;
    }

    private static Map<Institucion, Map<Operador, OperacionSpec>> parsear(Config config) {
        Map<Institucion, Map<Operador, Map<String, String>>> crudos = new EnumMap<>(Institucion.class);
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO)) {
                continue;
            }
            String[] partes = propiedad.split("\\.");
            if (partes.length != 5 || !MARCADOR.equals(partes[2])) {
                continue;
            }
            Institucion institucion = institucion(partes[1]);
            if (institucion == null) {
                continue;
            }
            Operador operador = Operador.valueOf(partes[3]);
            crudos.computeIfAbsent(institucion, clave -> new EnumMap<>(Operador.class))
                .computeIfAbsent(operador, clave -> new HashMap<>())
                .put(partes[4], config.getValue(propiedad, String.class));
        }
        Map<Institucion, Map<Operador, OperacionSpec>> resultado = new EnumMap<>(Institucion.class);
        crudos.forEach((institucion, porOperador) -> {
            Map<Operador, OperacionSpec> specs = new EnumMap<>(Operador.class);
            porOperador.forEach((operador, atributos) -> specs.put(operador, spec(institucion, operador, atributos)));
            resultado.put(institucion, specs);
        });
        LOG.debugf("catalogo de operaciones cargado instituciones=%s", resultado.keySet());
        return resultado;
    }

    private static OperacionSpec spec(Institucion institucion, Operador operador, Map<String, String> atributos) {
        String mti = atributos.get("mti");
        String processingCode = atributos.get("processingCode");
        if (mti == null || processingCode == null) {
            throw new IllegalStateException(
                "Operación " + operador + " de " + institucion + " requiere mti y processingCode");
        }
        return new OperacionSpec(
            mti,
            processingCode,
            Boolean.parseBoolean(atributos.getOrDefault("conMonto", "false")),
            Boolean.parseBoolean(atributos.getOrDefault("requiereAprobacion", "false")));
    }

    private static Institucion institucion(String nombre) {
        for (Institucion institucion : Institucion.values()) {
            if (institucion.name().equalsIgnoreCase(nombre)) {
                return institucion;
            }
        }
        return null;
    }

    public record OperacionSpec(String mti, String processingCode, boolean conMonto, boolean requiereAprobacion) {
    }
}
