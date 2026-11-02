package org.canalesCMAC.application;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.canalesCMAC.domain.model.Institucion;
import org.eclipse.microprofile.config.Config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CatalogoCodigos {

    private static final String PREFIJO = "convenio.";

    private final Map<Institucion, Map<String, String>> mensajes;

    @Inject
    public CatalogoCodigos(Config config) {
        this(parsear(config));
    }

    public CatalogoCodigos(Map<Institucion, Map<String, String>> mensajes) {
        this.mensajes = mensajes;
    }

    public String mensaje(Institucion institucion, String codigo, String mensajeProveedor) {
        Map<String, String> porCodigo = mensajes.get(institucion);
        if (porCodigo != null) {
            String mensaje = porCodigo.get(codigo);
            if (mensaje != null) {
                return mensaje;
            }
        }
        return mensajeProveedor;
    }

    public String normalizar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "99";
        }
        String limpio = codigo.trim();
        return switch (limpio) {
            case "0", "0000", "00000" -> "00";
            default -> limpio;
        };
    }

    private static Map<Institucion, Map<String, String>> parsear(Config config) {
        Map<Institucion, Map<String, String>> resultado = new EnumMap<>(Institucion.class);
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO)) {
                continue;
            }
            String[] partes = propiedad.split("\\.");
            if (partes.length != 4 || !"codigo".equals(partes[2])) {
                continue;
            }
            Institucion institucion = institucion(partes[1]);
            if (institucion == null) {
                continue;
            }
            resultado.computeIfAbsent(institucion, clave -> new HashMap<>())
                .put(partes[3], config.getValue(propiedad, String.class));
        }
        return resultado;
    }

    private static Institucion institucion(String nombre) {
        for (Institucion institucion : Institucion.values()) {
            if (institucion.name().equalsIgnoreCase(nombre)) {
                return institucion;
            }
        }
        return null;
    }
}
