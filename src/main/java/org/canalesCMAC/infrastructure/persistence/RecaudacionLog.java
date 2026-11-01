package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_log", indexes = {
    @Index(name = "idx_recaudacion_log_fecha", columnList = "fecha_log"),
    @Index(name = "idx_recaudacion_log_trace", columnList = "trace_id"),
    @Index(name = "idx_recaudacion_log_transaccion", columnList = "id_transaccion")
})
public class RecaudacionLog extends EntidadBase {

    @Column(nullable = false, length = 10)
    public String nivel;

    @Column(length = 200)
    public String logger;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String mensaje;

    @Column(name = "trace_id", length = 64)
    public String traceId;

    @Column(name = "id_transaccion", length = 20)
    public String idTransaccion;

    @Column(name = "convenio_id")
    public Long convenioId;

    @Column(length = 80)
    public String operador;

    @Column(name = "contexto_json", columnDefinition = "TEXT")
    public String contextoJson;

    @Column(name = "fecha_log", nullable = false)
    public OffsetDateTime fechaLog;
}
