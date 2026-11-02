package org.canalesCMAC.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_deuda", indexes = {
    @Index(name = "idx_recaudacion_deuda_transaccion", columnList = "transaccion_id"),
    @Index(name = "idx_recaudacion_deuda_comprobante", columnList = "numero_comprobante"),
    @Index(name = "idx_recaudacion_deuda_vencimiento", columnList = "fecha_vencimiento")
})
public class RecaudacionDeuda extends EntidadBase {

    @Column(name = "transaccion_id", nullable = false)
    public Long transaccionId;

    @Column(name = "numero_comprobante", length = 100)
    public String numeroComprobante;

    @Column(length = 300)
    public String descripcion;

    @Column(precision = 18, scale = 2)
    public BigDecimal monto;

    @Column(name = "importe_deuda_minima", precision = 18, scale = 2)
    public BigDecimal importeDeudaMinima;

    @Column(name = "fecha_emision")
    public LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    public LocalDate fechaVencimiento;

    @Column(length = 20)
    public String periodo;

    @Column(name = "tipo_integracion", length = 20)
    public String tipoIntegracion;

    @Column(name = "indicador_restriccion_pago", length = 5)
    public String indicadorRestriccionPago;

    @Column(name = "cantidad_subconceptos")
    public Integer cantidadSubconceptos;

    @Column(length = 300)
    public String glosa;
}
