package org.canalesCMAC.domain.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Institucion {
    ELSE,
    ELECTRO_UCAYALI,
    SEAL,
    CLARO,
    MUNICIPALIDAD_CUSCO,
    PAYTOPERU,
    UNIVCUSCO,
    YAGANASTE,
    WESTERN_UNION,
    UNIVCONTINENTAL_UNIVERSIDAD,
    UNIVCONTINENTAL_CIDIOMAS;

    @JsonCreator
    public static Institucion desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normal = valor.trim().toUpperCase(Locale.ROOT);
        if ("UNIVCONTINENTAL".equals(normal)) {
            return UNIVCONTINENTAL_UNIVERSIDAD;
        }
        return Institucion.valueOf(normal);
    }

    @JsonValue
    public String codigo() {
        return name();
    }
}
