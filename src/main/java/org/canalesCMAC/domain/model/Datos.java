package org.canalesCMAC.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Datos de la petición agrupados en filtro, cliente y transacción")
public class Datos {

    private static final Map<String, String> ALIAS = Map.ofEntries(
        Map.entry("ordenpago", Canonico.NUMERO_ORDEN_PAGO),
        Map.entry("codigocontrato", Canonico.CONTRATO),
        Map.entry("ncodigo_pago", Canonico.CODIGO_PAGO),
        Map.entry("cnro_documento", Canonico.DOCUMENTO_CLIENTE),
        Map.entry("cubicacion_canal", Canonico.UBICACION_CANAL),
        Map.entry("codcontribuyente", Canonico.CODIGO_CONTRIBUYENTE),
        Map.entry("nrooperacion", Canonico.NUMERO_OPERACION),
        Map.entry("nrosumin", Canonico.NUMERO_SUMINISTRO),
        Map.entry("numfactura", Canonico.NUMERO_COMPROBANTE),
        Map.entry("fechahoraoperacion", Canonico.FECHA_OPERACION),
        Map.entry("dfecha", Canonico.FECHA_OPERACION),
        Map.entry("canaloperacion", Canonico.CANAL),
        Map.entry("ccanal", Canonico.CANAL),
        Map.entry("importedeudapagada", Canonico.MONTO),
        Map.entry("importe", Canonico.MONTO),
        Map.entry("nimporte", Canonico.MONTO)
    );

    private Map<String, Object> filtro = new LinkedHashMap<>();
    private Map<String, Object> cliente = new LinkedHashMap<>();
    private List<Map<String, Object>> transaccion = new ArrayList<>();

    public Datos() {
    }

    public Datos(Map<String, Object> filtro, Map<String, Object> cliente,
                 List<Map<String, Object>> transaccion) {
        setFiltro(filtro);
        setCliente(cliente);
        setTransaccion(transaccion);
    }

    public static Datos vacio() {
        return new Datos(Map.of(), Map.of(), List.of());
    }

    public static Datos deFiltro(Map<String, Object> filtro) {
        return new Datos(filtro, Map.of(), List.of());
    }

    @Schema(description = "Criterios de consulta e identificación del servicio o deuda")
    public Map<String, Object> getFiltro() {
        return filtro;
    }

    public void setFiltro(Map<String, Object> filtro) {
        this.filtro = normalizar(filtro);
    }

    @Schema(description = "Datos del cliente que paga")
    public Map<String, Object> getCliente() {
        return cliente;
    }

    public void setCliente(Map<String, Object> cliente) {
        this.cliente = normalizar(cliente);
    }

    @Schema(description = "Datos de la operación; puede contener una o varias transacciones")
    public List<Map<String, Object>> getTransaccion() {
        return transaccion;
    }

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public void setTransaccion(List<Map<String, Object>> transaccion) {
        if (transaccion == null) {
            this.transaccion = new ArrayList<>();
            return;
        }
        List<Map<String, Object>> normalizadas = new ArrayList<>(transaccion.size());
        for (Map<String, Object> item : transaccion) {
            normalizadas.add(normalizar(item));
        }
        this.transaccion = normalizadas;
    }

    @JsonAnySetter
    public void campoAdicional(String nombre, Object valor) {
        String canonica = canonica(nombre);
        if (!filtro.containsKey(canonica) || filtro.get(canonica) == null) {
            filtro.put(canonica, valor);
        }
    }

    @JsonIgnore
    public Object get(String clave) {
        return fusion().get(clave);
    }

    @JsonIgnore
    public Object getOrDefault(String clave, Object porDefecto) {
        return fusion().getOrDefault(clave, porDefecto);
    }

    @JsonIgnore
    public boolean contiene(String clave) {
        return fusion().containsKey(clave);
    }

    @JsonIgnore
    public Map<String, Object> fusion() {
        Map<String, Object> fusion = new LinkedHashMap<>();
        fusionar(fusion, filtro);
        fusionar(fusion, cliente);
        if (!transaccion.isEmpty() && transaccion.get(0) != null) {
            fusionar(fusion, transaccion.get(0));
        }
        return fusion;
    }

    @JsonIgnore
    public List<Map<String, Object>> transacciones() {
        return transaccion;
    }

    @JsonIgnore
    public Map<String, Object> transaccionPrincipal() {
        return transaccion.isEmpty() || transaccion.get(0) == null ? Map.of() : transaccion.get(0);
    }

    public Datos conTransaccion(Map<String, Object> tx) {
        return new Datos(filtro, cliente, List.of(tx == null ? Map.of() : tx));
    }

    @JsonIgnore
    public static String canonica(String clave) {
        if (clave == null) {
            return null;
        }
        return ALIAS.getOrDefault(clave.trim().toLowerCase(Locale.ROOT), clave);
    }

    private static Map<String, Object> normalizar(Map<String, Object> origen) {
        Map<String, Object> destino = new LinkedHashMap<>();
        if (origen == null) {
            return destino;
        }
        origen.forEach((clave, valor) -> {
            String canonica = canonica(clave);
            if (!destino.containsKey(canonica) || destino.get(canonica) == null) {
                destino.put(canonica, valor);
            }
        });
        return destino;
    }

    private static void fusionar(Map<String, Object> destino, Map<String, Object> origen) {
        if (origen == null) {
            return;
        }
        origen.forEach((clave, valor) -> {
            if (valor != null) {
                destino.put(clave, valor);
            }
        });
    }
}
