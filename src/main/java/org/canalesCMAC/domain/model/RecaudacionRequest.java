package org.canalesCMAC.domain.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Contrato único de entrada para los 10 convenios.
 */
@Schema(name = "RecaudacionRequest", description = "Contrato único de entrada de recaudación")
public record RecaudacionRequest(
    @Schema(description = "Convenio que procesa la operación", example = "SEAL")
    Institucion institucion,
    @Schema(description = "Operación a ejecutar", example = "CONSULTA_DEUDA")
    Operador operador,
    @Schema(description = "Metadatos de correlación y auditoría")
    Cabecera cabecera,
    @Schema(description = "Datos agrupados de la petición")
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
