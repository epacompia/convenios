package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_intento_proveedor", indexes = {
    @Index(name = "idx_intento_proveedor_transaccion", columnList = "id_transaccion"),
    @Index(name = "idx_intento_proveedor_fecha", columnList = "fecha_intento")
})
public class RecaudacionIntentoProveedor extends EntidadBase {

    @Column(name = "operacion_id")
    public Long operacionId;

    @Column(name = "id_transaccion", length = 20)
    public String idTransaccion;

    @Column(name = "convenio_id")
    public Long convenioId;

    @Column(length = 80)
    public String operador;

    @Column(length = 20)
    public String protocolo;

    @Column(length = 500)
    public String endpoint;

    @Column(length = 10)
    public String metodo;

    @Column
    public Integer intento;

    @Column(name = "http_status")
    public Integer httpStatus;

    @Column(name = "codigo_error", length = 50)
    public String codigoError;

    @Column(name = "mensaje_error", length = 500)
    public String mensajeError;

    @Column(name = "duracion_ms")
    public Long duracionMs;

    @Column(name = "circuit_breaker_estado", length = 20)
    public String circuitBreakerEstado;

    @Column(name = "request_json", columnDefinition = "TEXT")
    public String requestJson;

    @Column(name = "response_json", columnDefinition = "TEXT")
    public String responseJson;

    @Column(name = "fecha_intento")
    public OffsetDateTime fechaIntento;
}
