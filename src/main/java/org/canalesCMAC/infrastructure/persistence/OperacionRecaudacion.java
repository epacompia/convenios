package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recaudacion_operacion", indexes = {
    @Index(name = "idx_recaudacion_operacion_institucion", columnList = "institucion,operador"),
    @Index(name = "idx_recaudacion_operacion_fecha", columnList = "fecha_registro")
})
public class OperacionRecaudacion extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "id_transaccion", nullable = false, unique = true, length = 20)
    public String idTransaccion;

    @Column(name = "convenio_id")
    public Long convenioId;

    @Column(nullable = false, length = 50)
    public String institucion;

    @Column(nullable = false, length = 80)
    public String operador;

    @Column(name = "id_sesion", length = 50)
    public String idSesion;

    @Column(name = "cod_agencia", length = 20)
    public String codAgencia;

    @Column(name = "cod_usuario", length = 50)
    public String codUsuario;

    @Column(length = 45)
    public String ip;

    @Column(name = "fecha_peticion")
    public OffsetDateTime fechaPeticion;

    @Column(name = "id_canal", length = 20)
    public String idCanal;

    @Column(name = "id_aplicacion", length = 20)
    public String idAplicacion;

    @Column(length = 10)
    public String codigo;

    @Column(length = 500)
    public String mensaje;

    @Column(nullable = false, length = 20)
    public String estado;

    @Column(name = "duracion_ms")
    public Long duracionMs;

    @Column(name = "request_json", columnDefinition = "TEXT")
    public String requestJson;

    @Column(name = "response_json", columnDefinition = "TEXT")
    public String responseJson;

    @Column(name = "fecha_registro", nullable = false)
    public OffsetDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    public OffsetDateTime fechaActualizacion;
}
