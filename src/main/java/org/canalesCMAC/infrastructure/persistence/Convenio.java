package org.canalesCMAC.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "convenio")
public class Convenio extends EntidadBase {

    @Column(nullable = false, unique = true, length = 50)
    public String codigo;

    @Column(nullable = false, length = 100)
    public String nombre;

    @Column(nullable = false, length = 20)
    public String protocolo;

    @Column(name = "url_base", length = 500)
    public String urlBase;

    @Column(name = "auth_tipo", length = 30)
    public String authTipo;

    @Column(nullable = false)
    public boolean activo = true;

    @Column(name = "timeout_ms")
    public Integer timeoutMs;

    @Column
    public Integer reintentos;

    @Column(name = "reintentos_delay_ms")
    public Integer reintentosDelayMs;
}
