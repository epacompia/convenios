package org.canalesCMAC.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "convenio_operador", indexes = {
    @Index(name = "idx_convenio_operador_convenio", columnList = "convenio_id,activo")
})
public class ConvenioOperador extends EntidadBase {

    @Column(name = "convenio_id", nullable = false)
    public Long convenioId;

    @Column(nullable = false, length = 80)
    public String operador;

    @Column(length = 300)
    public String ruta;

    @Column(name = "metodo_http", length = 10)
    public String metodoHttp;

    @Column(length = 4)
    public String mti;

    @Column(name = "processing_code", length = 6)
    public String processingCode;

    @Column(name = "con_monto")
    public Boolean conMonto;

    @Column(name = "requiere_aprobacion")
    public Boolean requiereAprobacion;

    @Column(name = "timeout_ms")
    public Integer timeoutMs;

    @Column
    public Integer reintentos;

    @Column(name = "cb_umbral")
    public Integer cbUmbral;

    @Column(name = "cb_espera_ms")
    public Integer cbEsperaMs;

    @Column(nullable = false)
    public boolean activo = true;
}
