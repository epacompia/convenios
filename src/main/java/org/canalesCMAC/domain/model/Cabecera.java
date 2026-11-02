package org.canalesCMAC.domain.model;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Metadatos genéricos de la petición, comunes a todos los convenios.
 */
@Schema(name = "Cabecera", description = "Metadatos de correlación, trazabilidad y auditoría")
public record Cabecera(
    String idTransaccion,
    String idSesion,
    String codAgencia,
    String codUsuario,
    String ip,
    String fecha,
    String idCanal,
    String idAplicacion) {

    private static final AtomicLong SECUENCIA =
        new AtomicLong(System.nanoTime() % 10_000L);
    private static final DateTimeFormatter FORMATO_ID =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int CODIGO_NODO = codigoNodo();

    @JsonCreator
    public Cabecera(
        @JsonProperty("idTransaccion") String idTransaccion,
        @JsonProperty("idSesion") String idSesion,
        @JsonProperty("codAgencia") String codAgencia,
        @JsonProperty("codUsuario") String codUsuario,
        @JsonProperty("ip") String ip,
        @JsonProperty("fecha") String fecha,
        @JsonProperty("idCanal") String idCanal,
        @JsonProperty("idAplicacion") String idAplicacion) {
        this.idTransaccion = textoOGenerado(idTransaccion);
        this.idSesion = defecto(idSesion, "0");
        this.codAgencia = defecto(codAgencia, "0");
        this.codUsuario = defecto(codUsuario, "SIST");
        this.ip = defecto(ip, "127.0.0.1");
        this.fecha = defecto(fecha, OffsetDateTime.now().withNano(0)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        this.idCanal = defecto(idCanal, "0");
        this.idAplicacion = defecto(idAplicacion, "0");
    }

    public static Cabecera generar() {
        return new Cabecera(null, null, null, null, null, null, null, null);
    }

    public static String generarIdTransaccion() {
        long secuencia = Math.floorMod(SECUENCIA.incrementAndGet(), 10_000L);
        return OffsetDateTime.now().format(FORMATO_ID)
            + String.format("%02d%04d", CODIGO_NODO, secuencia);
    }

    private static int codigoNodo() {
        try {
            String nombre = InetAddress.getLocalHost().getHostName();
            return Math.floorMod(nombre.hashCode(), 100);
        } catch (Exception e) {
            return Math.floorMod((int) ProcessHandle.current().pid(), 100);
        }
    }

    private static String textoOGenerado(String valor) {
        return valor == null || valor.isBlank() ? generarIdTransaccion() : valor;
    }

    private static String defecto(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
