package org.canalesCMAC.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_conciliacion", indexes = {
    @Index(name = "idx_conciliacion_estado", columnList = "estado")
})
public class RecaudacionConciliacion extends EntidadBase {

    @Column(name = "fecha_conciliacion", nullable = false)
    public LocalDate fechaConciliacion;

    @Column(name = "convenio_id", nullable = false)
    public Long convenioId;

    @Column(name = "total_operaciones", nullable = false)
    public long totalOperaciones;

    @Column(name = "total_aprobadas", nullable = false)
    public long totalAprobadas;

    @Column(name = "total_rechazadas", nullable = false)
    public long totalRechazadas;

    @Column(name = "monto_total", nullable = false, precision = 18, scale = 2)
    public BigDecimal montoTotal;

    @Column(nullable = false, length = 3)
    public String moneda;

    @Column(nullable = false, length = 20)
    public String estado;

    @Column(name = "archivo_ref", length = 300)
    public String archivoRef;

    @Column(columnDefinition = "TEXT")
    public String observacion;
}
