package org.canalesCMAC.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Datos del request agrupados en filtro, cliente y transacción.
 *
 * <p>Se acepta también el formato plano anterior: cualquier campo que no sea
 * {@code filtro}, {@code cliente} o {@code transaccion} se trata como parte de
 * {@code filtro}. Así los clientes existentes siguen funcionando durante la
 * transición.</p>
 */
public class Datos {

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

    public Map<String, Object> getFiltro() {
        return filtro;
    }

    public void setFiltro(Map<String, Object> filtro) {
        this.filtro = filtro == null ? new LinkedHashMap<>() : new LinkedHashMap<>(filtro);
    }

    public Map<String, Object> getCliente() {
        return cliente;
    }

    public void setCliente(Map<String, Object> cliente) {
        this.cliente = cliente == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cliente);
    }

    public List<Map<String, Object>> getTransaccion() {
        return transaccion;
    }

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public void setTransaccion(List<Map<String, Object>> transaccion) {
        this.transaccion = transaccion == null ? new ArrayList<>() : new ArrayList<>(transaccion);
    }

    @JsonAnySetter
    public void campoAdicional(String nombre, Object valor) {
        filtro.put(nombre, valor);
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
        fusion.putAll(filtro);
        fusion.putAll(cliente);
        if (!transaccion.isEmpty() && transaccion.get(0) != null) {
            fusion.putAll(transaccion.get(0));
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
}
