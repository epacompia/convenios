package org.canalesCMAC.application;

import java.lang.reflect.Array;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.RecaudacionRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ValidadorSolicitud {

    private static final String FORMATO_ID_TRANSACCION = "^\\d{20}$";

    private final MatrizObligatoriedad matriz;

    @Inject
    public ValidadorSolicitud(MatrizObligatoriedad matriz) {
        this.matriz = matriz;
    }

    public void validar(RecaudacionRequest solicitud) {
        validarCabecera(solicitud.cabecera());

        List<Map<String, Object>> transacciones = solicitud.datos().transacciones();
        if (transacciones.isEmpty()) {
            validarCampos(solicitud, solicitud.datos().fusion());
            return;
        }
        for (Map<String, Object> transaccion : transacciones) {
            Map<String, Object> campos = new LinkedHashMap<>(solicitud.datos().getFiltro());
            campos.putAll(solicitud.datos().getCliente());
            if (transaccion != null) {
                campos.putAll(transaccion);
            }
            validarCampos(solicitud, campos);
        }
    }

    private void validarCabecera(Cabecera cabecera) {
        if (cabecera == null) {
            throw new IllegalArgumentException("La cabecera es obligatoria");
        }
        if (cabecera.idTransaccion() == null || !cabecera.idTransaccion().matches(FORMATO_ID_TRANSACCION)) {
            throw new IllegalArgumentException(
                "cabecera.idTransaccion inválido: se esperaba el formato yyyyMMddHHmmss + 6 dígitos (^\\d{20}$)");
        }
        try {
            OffsetDateTime.parse(cabecera.fecha());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "cabecera.fecha inválida: se esperaba una fecha ISO-8601 con zona horaria");
        }
    }

    private void validarCampos(RecaudacionRequest solicitud, Map<String, Object> campos) {
        for (Set<String> grupo : matriz.requeridos(solicitud.institucion(), solicitud.operador())) {
            boolean presente = grupo.stream().anyMatch(campo -> presente(campos.get(campo)));
            if (!presente) {
                throw new IllegalArgumentException(
                    "Falta el campo obligatorio: " + String.join(" o ", grupo));
            }
        }
    }

    private boolean presente(Object valor) {
        if (valor == null) {
            return false;
        }
        if (valor instanceof CharSequence texto) {
            return !texto.toString().isBlank();
        }
        if (valor instanceof Collection<?> coleccion) {
            return !coleccion.isEmpty();
        }
        if (valor instanceof Map<?, ?> mapa) {
            return !mapa.isEmpty();
        }
        if (valor.getClass().isArray()) {
            return Array.getLength(valor) > 0;
        }
        return true;
    }
}
