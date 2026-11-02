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
    UNIVCONTINENTAL_CIDIOMAS,
    UNSAAC;

    @JsonCreator
    public static Institucion desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normal = valor.trim().toUpperCase(Locale.ROOT);
        if ("UNIVCONTINENTAL".equals(normal)) {
            return UNIVCONTINENTAL_UNIVERSIDAD;
        }
        if ("SERVICIOS_CAJACUSCO_V2".equals(normal)
            || "CAJA_CUSCO_V2".equals(normal)
            || "CAJACUSCO_V2".equals(normal)
            || "SERVICIOS_CAJA_CUSCO_V2".equals(normal)) {
            return UNSAAC;
        }
        return Institucion.valueOf(normal);
    }

    @JsonValue
    public String codigo() {
        return name();
    }
}
