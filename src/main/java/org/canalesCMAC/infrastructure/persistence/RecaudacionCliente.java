package org.canalesCMAC.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_cliente", indexes = {
    @Index(name = "idx_recaudacion_cliente_documento", columnList = "numero_documento"),
    @Index(name = "idx_recaudacion_cliente_contribuyente", columnList = "codigo_contribuyente"),
    @Index(name = "idx_recaudacion_cliente_suministro", columnList = "numero_suministro")
})
public class RecaudacionCliente extends EntidadBase {

    @Column(name = "operacion_id", nullable = false, unique = true)
    public Long operacionId;

    @Column(name = "tipo_documento", length = 10)
    public String tipoDocumento;

    @Column(name = "numero_documento", length = 20)
    public String numeroDocumento;

    @Column(length = 200)
    public String nombre;

    @Column(length = 150)
    public String email;

    @Column(length = 300)
    public String direccion;

    @Column(length = 150)
    public String distrito;

    @Column(name = "codigo_contribuyente", length = 20)
    public String codigoContribuyente;

    @Column(name = "numero_suministro", length = 50)
    public String numeroSuministro;
}
