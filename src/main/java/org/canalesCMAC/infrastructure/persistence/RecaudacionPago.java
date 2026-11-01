package org.canalesCMAC.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_pago", indexes = {
    @Index(name = "idx_recaudacion_pago_autorizacion", columnList = "codigo_autorizacion")
})
public class RecaudacionPago extends EntidadBase {

    @Column(name = "transaccion_id", nullable = false, unique = true)
    public Long transaccionId;

    @Column(name = "medio_pago", length = 30)
    public String medioPago;

    @Column(name = "forma_pago", length = 30)
    public String formaPago;

    @Column(name = "cuenta_origen", length = 50)
    public String cuentaOrigen;

    @Column(name = "cuenta_destino", length = 50)
    public String cuentaDestino;

    @Column(name = "numero_tarjeta_mask", length = 30)
    public String numeroTarjetaMask;

    @Column(name = "bin_adquiriente", length = 10)
    public String binAdquiriente;

    @Column(name = "codigo_autorizacion", length = 30)
    public String codigoAutorizacion;

    @Column(name = "codigo_moneda", length = 3)
    public String codigoMoneda;

    @Column(name = "tipo_cambio", precision = 18, scale = 6)
    public BigDecimal tipoCambio;

    @Column(name = "nombre_comercio", length = 100)
    public String nombreComercio;

    @Column(name = "numero_comercio", length = 30)
    public String numeroComercio;

    @Column(name = "numero_terminal", length = 30)
    public String numeroTerminal;

    @Column(name = "fecha_captura")
    public OffsetDateTime fechaCaptura;
}
