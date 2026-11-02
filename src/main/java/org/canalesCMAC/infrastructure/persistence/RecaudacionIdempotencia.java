package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_idempotencia", indexes = {
    @Index(name = "idx_idempotencia_expiracion", columnList = "expira_at")
})
public class RecaudacionIdempotencia extends EntidadBase {

    @Column(name = "id_transaccion", nullable = false, unique = true, length = 20)
    public String idTransaccion;

    @Column(name = "convenio_id")
    public Long convenioId;

    @Column(length = 80)
    public String operador;

    @Column(nullable = false, length = 20)
    public String estado;

    @Column(name = "response_json", columnDefinition = "TEXT")
    public String responseJson;

    @Column(name = "hash_request", length = 64)
    public String hashRequest;

    @Column(name = "expira_at", nullable = false)
    public OffsetDateTime expiraAt;
}
