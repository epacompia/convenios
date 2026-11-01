package org.canalesCMAC.domain.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contrato único de entrada para los 10 convenios.
 */
public record RecaudacionRequest(
    Institucion institucion,
    Operador operador,
    Cabecera cabecera,
    Datos datos) {

    @JsonCreator
    public RecaudacionRequest(
        @JsonProperty("institucion") Institucion institucion,
        @JsonProperty("operador") Operador operador,
        @JsonProperty("cabecera") Cabecera cabecera,
        @JsonProperty("datos") Datos datos) {
        if (institucion == null || operador == null) {
            throw new IllegalArgumentException("institucion y operador son obligatorios");
        }
        this.institucion = institucion;
        this.operador = operador;
        this.cabecera = cabecera == null ? Cabecera.generar() : cabecera;
        this.datos = datos == null ? Datos.vacio() : datos;
    }

    /**
     * Constructor de compatibilidad para pruebas y clientes del contrato plano.
     */
    public RecaudacionRequest(Institucion institucion, Operador operador, Map<String, Object> datos) {
        this(institucion, operador, Cabecera.generar(), Datos.deFiltro(datos));
    }

    @JsonIgnore
    public Map<String, Object> campos() {
        return datos.fusion();
    }

    public RecaudacionRequest conTransaccion(Map<String, Object> transaccion) {
        return new RecaudacionRequest(institucion, operador, cabecera, datos.conTransaccion(transaccion));
    }
}
