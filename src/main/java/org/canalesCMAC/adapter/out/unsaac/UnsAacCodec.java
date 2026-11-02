package org.canalesCMAC.adapter.out.unsaac;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Convenio UNSAAC / Servicios Caja Cusco v2.
 *
 * <p>Endpoints HTTP POST con formulario urlencoded y payloads cifrados con
 * AES-256-CBC compatible con aes-everywhere.</p>
 */
@ApplicationScoped
public class UnsAacCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(UnsAacCodec.class);
    private static final String CONTENT_TYPE =
        "application/x-www-form-urlencoded; charset=UTF-8";
    private static final DateTimeFormatter FECHA_PROVEEDOR =
        DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "convenio.UNSAAC.claveCifrado",
        defaultValue = "xaadaippth@@%m5678")
    String claveCifrado;

    public UnsAacCodec() {
    }

    UnsAacCodec(String claveCifrado) {
        this.claveCifrado = claveCifrado;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Institucion institucion() {
        return Institucion.UNSAAC;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo request UNSAAC operador=%s", peticion.operador());
        Map<String, String> campos = new LinkedHashMap<>();
        String ruta;
        switch (peticion.operador()) {
            case CONSULTA_DEUDA -> {
                campos.put("codigo_transaccion",
                    cifrar(dato(peticion, Canonico.NUMERO_OPERACION)));
                ruta = "/SRVPPConsultaDetallePagoCC";
            }
            case PAGO_DEUDA -> {
                campos.put("pago", cifrar(jsonPago(peticion)));
                ruta = "/SRVPPGuardarPagoCC";
            }
            case VERIFICAR_PAGO -> {
                campos.put("ccodigos_transaccion", cifrar(listaOrdenes(peticion)));
                ruta = "/SRVPPVerificarPagoCC";
            }
            case CONFIRMAR_PAGO -> {
                campos.put("codigo_transaccion",
                    cifrar(dato(peticion, Canonico.NUMERO_OPERACION)));
                campos.put("exito", String.valueOf(exito(peticion)));
                campos.put("numero_operacion",
                    dato(peticion, Canonico.IDENTIFICADOR_PAGO));
                ruta = "/SRVPPConfirmarPagoCC";
            }
            default -> throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por UNSAAC");
        }
        return new PeticionSalida(formulario(campos), ruta, CONTENT_TYPE);
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        try {
            JsonNode raiz = objectMapper.readTree(respuesta.cuerpo());
            boolean exito = raiz.path("exito").asBoolean(false);
            String codresp = texto(raiz, "codresp");
            String mensaje = texto(raiz, "mensaje");
            if ("M01".equalsIgnoreCase(codresp)) {
                throw new ProveedorException(
                    "UNSAAC error interno: " + (mensaje == null ? "sin detalle" : mensaje),
                    ProveedorException.BAD_GATEWAY, false, null);
            }
            String codigo = exito ? "00" : ("M02".equalsIgnoreCase(codresp) ? "01" : "99");
            String objeto = texto(raiz, "objeto");
            Map<String, Object> cliente = new LinkedHashMap<>();
            Map<String, Object> transaccion = new LinkedHashMap<>();

            switch (peticion.operador()) {
                case CONSULTA_DEUDA -> {
                    if (exito) {
                        JsonNode datos = leerJson(objeto);
                        cliente.putAll(clienteConsulta(datos));
                        transaccion.putAll(transaccionConsulta(datos));
                    }
                }
                case PAGO_DEUDA -> {
                    if (exito) {
                        JsonNode datos = leerJson(objeto);
                        cliente.putAll(clientePago(peticion, datos));
                        transaccion.putAll(transaccionPago(peticion, datos));
                    }
                }
                case VERIFICAR_PAGO -> {
                    transaccion.putAll(transaccionVerificacion(exito, objeto, mensaje));
                }
                case CONFIRMAR_PAGO -> {
                    transaccion.putAll(transaccionConfirmacion(peticion, exito, objeto));
                }
                default -> throw new OperadorNoSoportadoException(
                    "Operador " + peticion.operador() + " no soportado por UNSAAC");
            }

            String mensajeFinal = mensaje;
            if (mensajeFinal == null || mensajeFinal.isBlank()) {
                mensajeFinal = exito ? "OK" : "ERROR UNSAAC";
            }
            LOG.debugf("respuesta UNSAAC interpretada operador=%s codigo=%s",
                peticion.operador(), codigo);
            return RecaudacionResponse.de(Institucion.UNSAAC, peticion.operador(),
                peticion.cabecera(), codigo, mensajeFinal, cliente,
                transaccion.isEmpty() ? List.of() : List.of(transaccion));
        } catch (ProveedorException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new ProveedorException(
                "Respuesta UNSAAC inválida: " + e.getMessage(),
                ProveedorException.BAD_GATEWAY, false, e);
        }
    }

    private String jsonPago(RecaudacionRequest peticion) {
        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("fecha", fechaProveedor(peticion));
        pago.put("nombre_tarjeta_habiente",
            texto(peticion, Canonico.NOMBRE_TARJETA_HABIENTE,
                texto(peticion, Canonico.NOMBRE_CLIENTE, "")));
        pago.put("importe_autorizado", monto(peticion));
        pago.put("codigo_transaccion", dato(peticion, Canonico.NUMERO_OPERACION));
        pago.put("numero_tarjeta", texto(peticion, Canonico.NUMERO_TARJETA, ""));
        pago.put("numero_pedido", dato(peticion, Canonico.NUMERO_PEDIDO));
        pago.put("forma_pago", dato(peticion, Canonico.FORMA_PAGO));
        pago.put("codigo_autorizacion", dato(peticion, Canonico.CODIGO_APROBACION));
        pago.put("email", texto(peticion, Canonico.EMAIL, ""));
        pago.put("proveedorNombre", texto(peticion, Canonico.PROVEEDOR_NOMBRE, ""));
        try {
            return objectMapper.writeValueAsString(pago);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el pago UNSAAC", e);
        }
    }

    private String listaOrdenes(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.TRANSACCIONES);
        if (!(valor instanceof List<?> lista) || lista.isEmpty()) {
            throw new IllegalArgumentException("Falta el campo obligatorio: transacciones");
        }
        List<String> ordenes = new ArrayList<>();
        for (Object item : lista) {
            if (!(item instanceof Map<?, ?> mapa)) {
                continue;
            }
            Object orden = mapa.get(Canonico.NUMERO_OPERACION);
            if (orden == null || orden.toString().isBlank()) {
                throw new IllegalArgumentException(
                    "Cada elemento de transacciones requiere numeroOperacion");
            }
            ordenes.add(orden.toString());
        }
        if (ordenes.isEmpty()) {
            throw new IllegalArgumentException("transacciones no contiene órdenes válidas");
        }
        return String.join("|", ordenes);
    }

    private Map<String, Object> clienteConsulta(JsonNode datos) {
        Map<String, Object> cliente = new LinkedHashMap<>();
        poner(cliente, Canonico.NOMBRE_CLIENTE, texto(datos, "nombres"));
        poner(cliente, Canonico.EMAIL, texto(datos, "email"));
        poner(cliente, Canonico.CODIGO_CLIENTE, texto(datos, "codigo_cliente"));
        return cliente;
    }

    private Map<String, Object> transaccionConsulta(JsonNode datos) {
        Map<String, Object> transaccion = new LinkedHashMap<>();
        poner(transaccion, Canonico.NUMERO_OPERACION, texto(datos, "codigo_transaccion"));
        poner(transaccion, Canonico.MONTO, numero(datos, "importe"));
        poner(transaccion, Canonico.MONEDA, moneda(datos));
        poner(transaccion, Canonico.CANTIDAD, numero(datos, "items"));
        String descripcion = primero(texto(datos, "descripción"), texto(datos, "descripcion"));
        poner(transaccion, Canonico.GLOSA, descripcion);
        poner(transaccion, "detalleConsulta", descripcion);
        poner(transaccion, Canonico.ESTADO, "OK");
        List<Map<String, Object>> deudas = conceptos(datos);
        if (!deudas.isEmpty()) {
            transaccion.put(Canonico.DEUDAS, deudas);
        }
        Map<String, Object> adicionales = new LinkedHashMap<>();
        poner(adicionales, "keymerchant", texto(datos, "keymerchant"));
        poner(adicionales, "ccp", texto(datos, "ccp"));
        poner(adicionales, "items", texto(datos, "items"));
        poner(adicionales, "monedaOriginal", texto(datos, "moneda"));
        if (!adicionales.isEmpty()) {
            transaccion.put(Canonico.DATOS_ADICIONALES, adicionales);
        }
        return transaccion;
    }

    private Map<String, Object> clientePago(RecaudacionRequest peticion, JsonNode datos) {
        Map<String, Object> cliente = new LinkedHashMap<>();
        poner(cliente, Canonico.NOMBRE_CLIENTE,
            primero(texto(peticion, Canonico.NOMBRE_TARJETA_HABIENTE, null),
                texto(peticion, Canonico.NOMBRE_CLIENTE, null)));
        poner(cliente, Canonico.EMAIL, texto(peticion, Canonico.EMAIL, null));
        return cliente;
    }

    private Map<String, Object> transaccionPago(RecaudacionRequest peticion, JsonNode datos) {
        Map<String, Object> transaccion = new LinkedHashMap<>();
        poner(transaccion, Canonico.NUMERO_OPERACION, texto(datos, "codigo_transaccion"));
        poner(transaccion, Canonico.IDENTIFICADOR_PAGO, texto(datos, "codigo_pago"));
        poner(transaccion, Canonico.CLAVE, texto(datos, "clave"));
        poner(transaccion, Canonico.ESTADO, texto(datos, "estado"));
        poner(transaccion, Canonico.OBSERVACION, texto(datos, "mensaje"));
        poner(transaccion, Canonico.GLOSA, texto(datos, "estado"));
        poner(transaccion, Canonico.MONTO, numeroPeticion(peticion, Canonico.MONTO));
        return transaccion;
    }

    private Map<String, Object> transaccionVerificacion(boolean exito,
                                                        String objeto,
                                                        String mensaje) {
        Map<String, Object> transaccion = new LinkedHashMap<>();
        if (exito) {
            transaccion.put(Canonico.ESTADO, "SIN_PAGOS");
            transaccion.put(Canonico.GLOSA,
                mensaje == null || mensaje.isBlank()
                    ? "NINGUNA ORDEN PAGADA" : mensaje);
            return transaccion;
        }
        if (objeto == null || objeto.isBlank()) {
            throw new ProveedorException(
                "UNSAAC no devolvió el detalle de órdenes pagadas",
                ProveedorException.BAD_GATEWAY, false, null);
        }
        String lista = descifrar(objeto);
        List<Map<String, Object>> deudas = new ArrayList<>();
        for (String orden : lista.split("\\|")) {
            if (orden.isBlank()) {
                continue;
            }
            Map<String, Object> deuda = new LinkedHashMap<>();
            deuda.put(Canonico.NUMERO_COMPROBANTE, orden.trim());
            deuda.put(Canonico.DESCRIPCION, "PAGO REGISTRADO");
            deuda.put(Canonico.GLOSA, "PAGO REGISTRADO");
            deudas.add(deuda);
        }
        transaccion.put(Canonico.ESTADO, "PAGADO");
        transaccion.put(Canonico.GLOSA,
            mensaje == null || mensaje.isBlank()
                ? "LAS SIGUIENTES ÓRDENES SE ENCUENTRAN COMO PAGO REGISTRADO" : mensaje);
        if (!deudas.isEmpty()) {
            transaccion.put(Canonico.DEUDAS, deudas);
        }
        Map<String, Object> adicionales = new LinkedHashMap<>();
        adicionales.put("ordenes", deudas.stream()
            .map(deuda -> deuda.get(Canonico.NUMERO_COMPROBANTE))
            .collect(Collectors.toList()));
        transaccion.put(Canonico.DATOS_ADICIONALES, adicionales);
        return transaccion;
    }

    private Map<String, Object> transaccionConfirmacion(RecaudacionRequest peticion,
                                                        boolean exito,
                                                        String objeto) {
        Map<String, Object> transaccion = new LinkedHashMap<>();
        poner(transaccion, Canonico.NUMERO_OPERACION,
            peticion.datos().get(Canonico.NUMERO_OPERACION));
        poner(transaccion, Canonico.IDENTIFICADOR_PAGO,
            peticion.datos().get(Canonico.IDENTIFICADOR_PAGO));
        transaccion.put(Canonico.ESTADO, exito ? "OK" : "ERROR");
        if (objeto != null && !objeto.isBlank()) {
            JsonNode datos = leerJson(objeto);
            poner(transaccion, Canonico.NUMERO_OPERACION,
                primero(texto(datos, "codigo_transaccion"),
                    valorTexto(transaccion.get(Canonico.NUMERO_OPERACION))));
            poner(transaccion, Canonico.IDENTIFICADOR_PAGO,
                primero(texto(datos, "codigo_pago"),
                    valorTexto(transaccion.get(Canonico.IDENTIFICADOR_PAGO))));
            poner(transaccion, Canonico.CLAVE, texto(datos, "clave"));
            poner(transaccion, Canonico.OBSERVACION, texto(datos, "mensaje"));
        }
        return transaccion;
    }

    private List<Map<String, Object>> conceptos(JsonNode datos) {
        List<Map<String, Object>> deudas = new ArrayList<>();
        JsonNode conceptos = datos.path("conceptos");
        if (!conceptos.isArray()) {
            return deudas;
        }
        for (JsonNode concepto : conceptos) {
            Map<String, Object> deuda = new LinkedHashMap<>();
            poner(deuda, Canonico.NUMERO_COMPROBANTE, texto(concepto, "codigo"));
            poner(deuda, Canonico.DESCRIPCION, texto(concepto, "descripcion"));
            poner(deuda, Canonico.MONTO, numero(concepto, "valor"));
            poner(deuda, Canonico.GLOSA, texto(concepto, "descripcion"));
            Map<String, Object> adicionales = new LinkedHashMap<>();
            poner(adicionales, "nro", texto(concepto, "nro"));
            poner(adicionales, "codigo", texto(concepto, "codigo"));
            if (!adicionales.isEmpty()) {
                deuda.put(Canonico.DATOS_ADICIONALES, adicionales);
            }
            deudas.add(deuda);
        }
        return deudas;
    }

    private JsonNode leerJson(String objeto) {
        if (objeto == null || objeto.isBlank()) {
            throw new ProveedorException(
                "UNSAAC no devolvió el objeto esperado",
                ProveedorException.BAD_GATEWAY, false, null);
        }
        try {
            return objectMapper.readTree(descifrar(objeto));
        } catch (JsonProcessingException e) {
            throw new ProveedorException(
                "UNSAAC devolvió un objeto cifrado inválido",
                ProveedorException.BAD_GATEWAY, false, e);
        }
    }

    private String descifrar(String objeto) {
        try {
            return AesCifrado.descifrar(objeto, claveCifrado);
        } catch (RuntimeException e) {
            throw new ProveedorException(
                "No se pudo descifrar la respuesta de UNSAAC",
                ProveedorException.BAD_GATEWAY, false, e);
        }
    }

    private String cifrar(String texto) {
        try {
            return AesCifrado.cifrar(texto, claveCifrado);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo cifrar el request UNSAAC", e);
        }
    }

    private String formulario(Map<String, String> campos) {
        return campos.entrySet().stream()
            .map(entrada -> codificar(entrada.getKey()) + "=" + codificar(entrada.getValue()))
            .collect(Collectors.joining("&"));
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }

    private String fechaProveedor(RecaudacionRequest peticion) {
        String valor = texto(peticion, Canonico.FECHA_OPERACION, null);
        if (valor == null) {
            return LocalDateTime.now().format(FECHA_PROVEEDOR);
        }
        if (valor.matches("\\d{14}")) {
            return valor;
        }
        try {
            return FECHA_PROVEEDOR.format(LocalDateTime.parse(valor,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (RuntimeException ignored) {
            try {
                return FECHA_PROVEEDOR.format(OffsetDateTime.parse(valor,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } catch (RuntimeException ignoredOffset) {
                try {
                    return FECHA_PROVEEDOR.format(LocalDate.parse(valor)
                        .atStartOfDay());
                } catch (RuntimeException ignoredDate) {
                    throw new IllegalArgumentException(
                        "fechaOperacion inválida para UNSAAC: " + valor);
                }
            }
        }
    }

    private String monto(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.MONTO);
        if (valor == null) {
            throw new IllegalArgumentException("Falta el campo obligatorio: monto");
        }
        return new BigDecimal(valor.toString()).setScale(2).toPlainString();
    }

    private boolean exito(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.EXITO);
        if (valor == null) {
            throw new IllegalArgumentException("Falta el campo obligatorio: exito");
        }
        if (valor instanceof Boolean booleano) {
            return booleano;
        }
        String texto = valor.toString().trim();
        return "true".equalsIgnoreCase(texto)
            || "1".equals(texto)
            || "SI".equalsIgnoreCase(texto);
    }

    private String dato(RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Falta el campo obligatorio: " + clave);
        }
        return valor.toString();
    }

    private String texto(RecaudacionRequest peticion, String clave, String porDefecto) {
        Object valor = peticion.datos().get(clave);
        return valor == null || valor.toString().isBlank()
            ? porDefecto
            : valor.toString().trim();
    }

    private String texto(JsonNode nodo, String campo) {
        if (nodo == null || !nodo.hasNonNull(campo)) {
            return null;
        }
        String valor = nodo.get(campo).asText();
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Object numero(JsonNode nodo, String campo) {
        if (nodo == null || !nodo.hasNonNull(campo)) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor.isNumber()) {
            return valor.decimalValue();
        }
        String texto = valor.asText();
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return texto;
        }
    }

    private String moneda(JsonNode datos) {
        String moneda = texto(datos, "moneda");
        if ("1".equals(moneda)) {
            return "PEN";
        }
        return moneda;
    }

    private Object numeroPeticion(RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor == null || valor instanceof Number) {
            return valor;
        }
        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException e) {
            return valor;
        }
    }

    private static String valorTexto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static String primero(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private static void poner(Map<String, Object> destino, String clave, Object valor) {
        if (valor != null) {
            destino.put(clave, valor);
        }
    }
}
