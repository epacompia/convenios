package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_extorno", indexes = {
    @Index(name = "idx_recaudacion_extorno_operacion_original", columnList = "numero_operacion_original")
})
public class RecaudacionExtorno extends EntidadBase {

    @Column(name = "transaccion_id", nullable = false, unique = true)
    public Long transaccionId;

    @Column(name = "identificador_pago_original", length = 100)
    public String identificadorPagoOriginal;

    @Column(name = "numero_operacion_original", length = 100)
    public String numeroOperacionOriginal;

    @Column(name = "fecha_operacion_original")
    public OffsetDateTime fechaOperacionOriginal;

    @Column(length = 300)
    public String motivo;

    @Column(length = 30)
    public String estado;

    @Column(name = "codigo_aprobacion", length = 30)
    public String codigoAprobacion;

    @Column(name = "fecha_extorno")
    public OffsetDateTime fechaExtorno;
}
