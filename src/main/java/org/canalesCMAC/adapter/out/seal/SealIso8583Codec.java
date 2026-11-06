package org.canalesCMAC.adapter.out.seal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.canalesCMAC.application.CatalogoCodigos;
import org.canalesCMAC.application.CatalogoOperaciones;
import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SealIso8583Codec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(SealIso8583Codec.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HHmmss");
    private static final String HEX = "0123456789ABCDEF";
    private static final Map<Integer, Integer> LONGITUDES = Map.ofEntries(
        Map.entry(2, 19), Map.entry(3, 6), Map.entry(4, 12), Map.entry(11, 6),
        Map.entry(12, 6), Map.entry(13, 8), Map.entry(22, 3), Map.entry(25, 2),
        Map.entry(32, 8), Map.entry(33, 8), Map.entry(37, 12), Map.entry(38, 6),
        Map.entry(39, 2), Map.entry(41, 8), Map.entry(42, 15), Map.entry(43, 40),
        Map.entry(49, 3));

    @ConfigProperty(name = "recaudacion.seal.usuario", defaultValue = "USUARIO")
    String usuarioBanco;

    @ConfigProperty(name = "recaudacion.seal.car", defaultValue = "9999")
    String car;

    @ConfigProperty(name = "recaudacion.seal.acquirer", defaultValue = "40001000")
    String acquirer;

    @ConfigProperty(name = "recaudacion.seal.forward", defaultValue = "20013005")
    String forward;

    @ConfigProperty(name = "recaudacion.seal.moneda", defaultValue = "604")
    String moneda;

    @ConfigProperty(name = "recaudacion.seal.agencia", defaultValue = "015")
    String agencia;

    @ConfigProperty(name = "recaudacion.seal.direccion", defaultValue = "")
    String direccion;

    @Inject
    CatalogoCodigos catalogo;

    @Inject
    CatalogoOperaciones catalogoOperaciones;

    @Override
    public Institucion institucion() {
        return Institucion.SEAL;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        CatalogoOperaciones.OperacionSpec operacion = catalogoOperaciones.operacion(Institucion.SEAL, peticion.operador());
        LocalDateTime fecha = fechaOperacion(peticion);
        String trace = trace(peticion);
        Map<Integer, String> campos = new LinkedHashMap<>();
        campos.put(2, padLeft("", 19));
        campos.put(3, operacion.processingCode());
        campos.put(4, operacion.conMonto() ? monto(peticion) : padLeft("", 12));
        campos.put(11, trace);
        campos.put(12, HORA.format(fecha));
        campos.put(13, FECHA.format(fecha));
        campos.put(22, "010");
        campos.put(25, "10");
        campos.put(32, padLeft(acquirer, 8));
        campos.put(33, padLeft(forward, 8));
        campos.put(37, padLeft(trace, 12));
        if (operacion.requiereAprobacion()) {
            campos.put(38, padLeft(aprobacion(peticion), 6));
        }
        campos.put(41, padLeft(car, 8));
        campos.put(42, padLeft(texto(peticion, Canonico.AGENCIA, agencia), 15));
        campos.put(43, padRight(direccion, 40));
        campos.put(49, padLeft(texto(peticion, Canonico.MONEDA, moneda), 3));
        campos.put(121, campo121(peticion));
        LOG.debugf("construyendo ISO8583 SEAL operador=%s mti=%s processingCode=%s",
            peticion.operador(), operacion.mti(), operacion.processingCode());
        return new PeticionSalida(ensamblar(operacion.mti(), campos), "text/plain; charset=ISO-8859-1");
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        Map<Integer, String> campos = parsear(respuesta.cuerpo());
        String codigo = campos.get(39);
        if (codigo == null || codigo.isBlank()) {
            codigo = "99";
        }
        String mensaje = catalogo.mensaje(Institucion.SEAL, codigo, "");
        Map<String, Object> cliente = Mapas.nuevo();
        Map<String, Object> transaccion = Mapas.nuevo();
        if ("00".equals(codigo)) {
            Map<String, String> privados = subcampos(campos.get(121));
            String contrato = privados.get("P02");
            Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, privados.get("P04"));
            Mapas.poner(cliente, Canonico.DIRECCION_CLIENTE, privados.get("P05"));
            Mapas.poner(cliente, Canonico.NUMERO_SUMINISTRO, contrato);
            Mapas.poner(transaccion, Canonico.IDENTIFICADOR_TRANSACCION, recortar(campos.get(11)));
            Mapas.poner(transaccion, Canonico.CODIGO_APROBACION, recortar(campos.get(38)));
            Mapas.poner(transaccion, Canonico.NUMERO_COMPROBANTE, contrato);
            Mapas.poner(transaccion, Canonico.MONTO, decimal(campos.get(4)));
        }
        LOG.debugf("ISO8583 SEAL interpretado operador=%s codigo=%s", peticion.operador(), codigo);
        return RecaudacionResponse.de(Institucion.SEAL, peticion.operador(), peticion.cabecera(),
            codigo, mensaje, cliente, transaccion.isEmpty() ? List.of() : List.of(transaccion));
    }

    private String ensamblar(String mti, Map<Integer, String> campos) {
        Set<Integer> bits = new TreeSet<>(campos.keySet());
        boolean secundario = bits.stream().anyMatch(b -> b > 64);
        Set<Integer> primarios = new TreeSet<>();
        for (int bit : bits) {
            if (bit <= 64) {
                primarios.add(bit);
            }
        }
        if (secundario) {
            primarios.add(1);
        }
        StringBuilder trama = new StringBuilder();
        trama.append(mti);
        trama.append(bitmap(primarios, 0));
        if (secundario) {
            trama.append(bitmap(bits, 64));
        }
        for (int bit : bits) {
            trama.append(campos.get(bit));
        }
        return trama.toString();
    }

    private Map<Integer, String> parsear(String trama) {
        Map<Integer, String> campos = new LinkedHashMap<>();
        int pos = 4;
        Set<Integer> bits = bits(trama.substring(pos, pos + 16), 0);
        pos += 16;
        if (bits.contains(1)) {
            bits.addAll(bits(trama.substring(pos, pos + 16), 64));
            pos += 16;
        }
        for (int bit : bits) {
            if (bit == 1) {
                continue;
            }
            if (bit == 121) {
                int longitud = Integer.parseInt(trama.substring(pos, pos + 4));
                campos.put(bit, trama.substring(pos, pos + longitud));
                pos += longitud;
            } else {
                int longitud = LONGITUDES.getOrDefault(bit, 0);
                campos.put(bit, trama.substring(pos, pos + longitud));
                pos += longitud;
            }
        }
        return campos;
    }

    private String bitmap(Set<Integer> bits, int offset) {
        StringBuilder hex = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            int valor = 0;
            for (int j = 0; j < 4; j++) {
                if (bits.contains(offset + i * 4 + j + 1)) {
                    valor |= (8 >> j);
                }
            }
            hex.append(HEX.charAt(valor));
        }
        return hex.toString();
    }

    private Set<Integer> bits(String hex, int offset) {
        Set<Integer> bits = new TreeSet<>();
        for (int i = 0; i < 16; i++) {
            int valor = Character.digit(hex.charAt(i), 16);
            for (int j = 0; j < 4; j++) {
                if ((valor & (8 >> j)) != 0) {
                    bits.add(offset + i * 4 + j + 1);
                }
            }
        }
        return bits;
    }

    private Map<String, String> subcampos(String campo121) {
        Map<String, String> privados = new LinkedHashMap<>();
        if (campo121 == null || campo121.length() < 4) {
            return privados;
        }
        String contenido = campo121.substring(4);
        int pos = 0;
        pos = agregar(privados, contenido, pos, "P01", 7);
        pos = agregar(privados, contenido, pos, "P02", 6);
        pos = agregar(privados, contenido, pos, "P03", 4);
        pos = agregar(privados, contenido, pos, "P04", 50);
        agregar(privados, contenido, pos, "P05", 50);
        return privados;
    }

    private int agregar(Map<String, String> privados, String contenido, int pos, String nombre, int longitud) {
        if (pos + longitud <= contenido.length()) {
            String valor = recortar(contenido.substring(pos, pos + longitud));
            if (!valor.isBlank()) {
                privados.put(nombre, valor);
            }
        }
        return pos + longitud;
    }

    private String campo121(RecaudacionRequest peticion) {
        StringBuilder contenido = new StringBuilder();
        contenido.append(padRight(usuarioBanco, 7));
        contenido.append(padRight(contrato(peticion), 6));
        contenido.append(padLeft(car, 4));
        contenido.append(tipoPago(peticion));
        int longitud = 4 + contenido.length();
        return padLeft(String.valueOf(longitud), 4) + contenido;
    }

    private String contrato(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.CONTRATO);
        if (valor == null || valor.toString().isBlank()) {
            valor = peticion.datos().get(Canonico.NUMERO_SUMINISTRO);
        }
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + Canonico.CONTRATO);
        }
        return valor.toString();
    }

    private String tipoPago(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.TIPO_PAGO);
        return valor == null || valor.toString().isBlank() ? "1" : valor.toString();
    }

    private String aprobacion(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.CODIGO_APROBACION);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + Canonico.CODIGO_APROBACION);
        }
        return valor.toString();
    }

    private String monto(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.MONTO);
        if (valor == null) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: monto");
        }
        return padLeft(new BigDecimal(valor.toString()).movePointRight(2).toBigInteger().toString(), 12);
    }

    private String trace(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.IDENTIFICADOR_TRANSACCION);
        String base = valor == null ? "" : valor.toString().replaceAll("\\D", "");
        if (base.length() > 6) {
            base = base.substring(base.length() - 6);
        }
        if (base.isBlank()) {
            base = String.valueOf(System.currentTimeMillis() % 1_000_000L);
        }
        return padLeft(base, 6);
    }

    private LocalDateTime fechaOperacion(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.FECHA_OPERACION);
        if (valor != null && !valor.toString().isBlank()) {
            return LocalDateTime.parse(valor.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    private String texto(RecaudacionRequest peticion, String clave, String porDefecto) {
        Object valor = peticion.datos().get(clave);
        return valor == null || valor.toString().isBlank() ? porDefecto : valor.toString();
    }

    private BigDecimal decimal(String valor) {
        return valor == null || valor.isBlank() ? null : new BigDecimal(valor).movePointLeft(2);
    }

    private void poner(Map<String, Object> destino, String clave, Object valor) {
        if (valor != null) {
            destino.put(clave, valor);
        }
    }

    private String recortar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String padLeft(String valor, int longitud) {
        String v = valor == null ? "" : valor;
        if (v.length() >= longitud) {
            return v.substring(v.length() - longitud);
        }
        return "0".repeat(longitud - v.length()) + v;
    }

    private String padRight(String valor, int longitud) {
        String v = valor == null ? "" : valor;
        if (v.length() >= longitud) {
            return v.substring(0, longitud);
        }
        return v + " ".repeat(longitud - v.length());
    }
}
