package org.canalesCMAC.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_transaccion", indexes = {
    @Index(name = "idx_recaudacion_transaccion_operacion", columnList = "operacion_id"),
    @Index(name = "idx_recaudacion_transaccion_identificador", columnList = "identificador_transaccion"),
    @Index(name = "idx_recaudacion_transaccion_comprobante", columnList = "numero_comprobante")
})
public class RecaudacionTransaccion extends EntidadBase {

    @Column(name = "operacion_id", nullable = false)
    public Long operacionId;

    @Column(name = "identificador_transaccion", length = 100)
    public String identificadorTransaccion;

    @Column(name = "identificador_pago", length = 100)
    public String identificadorPago;

    @Column(name = "numero_operacion", length = 100)
    public String numeroOperacion;

    @Column(name = "numero_comprobante", length = 100)
    public String numeroComprobante;

    @Column(name = "numero_trace", length = 50)
    public String numeroTrace;

    @Column(precision = 18, scale = 2)
    public BigDecimal monto;

    @Column(length = 3)
    public String moneda;

    @Column(name = "tipo_pago", length = 30)
    public String tipoPago;

    @Column(length = 30)
    public String estado;

    @Column(name = "codigo_aprobacion", length = 30)
    public String codigoAprobacion;

    @Column(name = "fecha_operacion")
    public OffsetDateTime fechaOperacion;

    @Column(name = "fecha_registro")
    public OffsetDateTime fechaRegistro;

    @Column(name = "referencia_proveedor", length = 100)
    public String referenciaProveedor;

    @Column(name = "referencia_operador", length = 100)
    public String referenciaOperador;
}
