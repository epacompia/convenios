package org.canalesCMAC.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.canalesCMAC.domain.model.Cabecera;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Persiste la operación y su detalle transaccional en las 12 tablas del modelo.
 */
@ApplicationScoped
public class AuditoriaRecaudacion {

    private static final Logger LOG = Logger.getLogger(AuditoriaRecaudacion.class);

    @Inject
    OperacionRecaudacionRepository operaciones;

    @Inject
    RecaudacionTransaccionRepository transacciones;

    @Inject
    RecaudacionClienteRepository clientes;

    @Inject
    RecaudacionDeudaRepository deudas;

    @Inject
    RecaudacionPagoRepository pagos;

    @Inject
    RecaudacionExtornoRepository extornos;

    @Inject
    ConvenioRepository convenios;

    @Inject
    SanitizadorDatos sanitizador;

    @ConfigProperty(name = "recaudacion.auditoria.enabled", defaultValue = "true")
    boolean habilitada;

    @ConfigProperty(name = "recaudacion.auditoria.fail-on-error", defaultValue = "false")
    boolean fallarSiError;

    @Transactional
    public void registrar(RecaudacionRequest solicitud, RecaudacionResponse respuesta, long duracionMs) {
        if (!habilitada) {
            return;
        }
        try {
            Cabecera cabecera = solicitud.cabecera();
            Optional<OperacionRecaudacion> existente =
                operaciones.porIdTransaccion(cabecera.idTransaccion());
            OperacionRecaudacion operacion = existente.orElseGet(OperacionRecaudacion::new);
            operacion.idTransaccion = cabecera.idTransaccion();
            operacion.convenioId = convenios.porCodigo(solicitud.institucion().name())
                .map(convenio -> convenio.id)
                .orElse(null);
            operacion.institucion = solicitud.institucion().name();
            operacion.operador = solicitud.operador().name();
            operacion.idSesion = cabecera.idSesion();
            operacion.codAgencia = cabecera.codAgencia();
            operacion.codUsuario = cabecera.codUsuario();
            operacion.ip = cabecera.ip();
            operacion.fechaPeticion = fecha(cabecera.fecha());
            operacion.idCanal = cabecera.idCanal();
            operacion.idAplicacion = cabecera.idAplicacion();
            operacion.codigo = respuesta.codigo();
            operacion.mensaje = respuesta.mensaje();
            operacion.estado = "00".equals(respuesta.codigo()) ? "OK" : "ERROR";
            operacion.duracionMs = duracionMs;
            operacion.requestJson = sanitizador.sanitizar(solicitud);
            operacion.responseJson = sanitizador.sanitizar(respuesta);
            operacion.fechaActualizacion = OffsetDateTime.now();
            if (existente.isEmpty()) {
                operacion.fechaRegistro = OffsetDateTime.now();
                operaciones.persist(operacion);
            } else {
                return;
            }

            guardarCliente(operacion, solicitud, respuesta);
            guardarTransacciones(operacion, solicitud, respuesta);
        } catch (RuntimeException e) {
            if (fallarSiError) {
                throw e;
            }
            LOG.errorf("no se pudo guardar la auditoría idTransaccion=%s: %s",
                solicitud.cabecera().idTransaccion(), e.getMessage());
        }
    }

    private void guardarCliente(OperacionRecaudacion operacion,
                                RecaudacionRequest solicitud,
                                RecaudacionResponse respuesta) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.putAll(solicitud.datos().getFiltro());
        datos.putAll(solicitud.datos().getCliente());
        datos.putAll(respuesta.cliente());

        RecaudacionCliente cliente = new RecaudacionCliente();
        cliente.operacionId = operacion.id;
        cliente.tipoDocumento = texto(datos, "tipoDocumentoCliente");
        cliente.numeroDocumento = texto(datos, "numeroDocumento");
        cliente.nombre = texto(datos, "nombreCliente");
        cliente.email = texto(datos, "email");
        cliente.direccion = texto(datos, "direccionCliente");
        cliente.distrito = texto(datos, "distritoCliente");
        cliente.codigoContribuyente = texto(datos, "codigoContribuyente");
        cliente.numeroSuministro = texto(datos, "numeroSuministro");
        if (tieneAlgunDato(cliente)) {
            clientes.persist(cliente);
        }
    }

    private void guardarTransacciones(OperacionRecaudacion operacion,
                                      RecaudacionRequest solicitud,
                                      RecaudacionResponse respuesta) {
        List<Map<String, Object>> respuestaTxs = respuesta.transacciones();
        List<Map<String, Object>> solicitudTxs = solicitud.datos().transacciones();

        if (respuestaTxs.isEmpty()) {
            Map<String, Object> unica = new LinkedHashMap<>(solicitud.datos().fusion());
            guardarTransaccion(operacion, solicitud, unica, solicitud.operador());
            return;
        }

        for (int i = 0; i < respuestaTxs.size(); i++) {
            Map<String, Object> respuestaTx = respuestaTxs.get(i);
            Map<String, Object> solicitudTx = i < solicitudTxs.size()
                ? solicitudTxs.get(i)
                : solicitud.datos().transaccionPrincipal();
            Map<String, Object> merged = new LinkedHashMap<>();
            merged.putAll(solicitud.datos().getFiltro());
            merged.putAll(solicitud.datos().getCliente());
            if (solicitudTx != null) {
                merged.putAll(solicitudTx);
            }
            if (respuestaTx != null) {
                merged.putAll(respuestaTx);
            }
            guardarTransaccion(operacion, solicitud, merged, solicitud.operador());
        }
    }

    private void guardarTransaccion(OperacionRecaudacion operacion,
                                    RecaudacionRequest solicitud,
                                    Map<String, Object> datos,
                                    Operador operador) {
        RecaudacionTransaccion transaccion = new RecaudacionTransaccion();
        transaccion.operacionId = operacion.id;
        transaccion.identificadorTransaccion = texto(datos, "identificadorTransaccion");
        transaccion.identificadorPago = texto(datos, "identificadorPago");
        transaccion.numeroOperacion = primero(texto(datos, "numeroOperacion"),
            texto(datos, "numOperacion"));
        transaccion.numeroComprobante = texto(datos, "numeroComprobante");
        transaccion.numeroTrace = texto(datos, "numeroTrace");
        transaccion.monto = decimal(datos, "monto");
        if (transaccion.monto == null) {
            transaccion.monto = decimal(datos, "montoPagar");
        }
        transaccion.moneda = texto(datos, "moneda");
        transaccion.tipoPago = texto(datos, "tipoPago");
        transaccion.estado = texto(datos, "estado");
        transaccion.codigoAprobacion = texto(datos, "codigoAprobacion");
        transaccion.fechaOperacion = fecha(datos, "fechaOperacion");
        transaccion.fechaRegistro = fecha(datos, "fechaRegistro");
        transaccion.referenciaProveedor = texto(datos, "referenciaProveedor");
        transaccion.referenciaOperador = texto(datos, "referenciaOperador");
        transacciones.persist(transaccion);

        guardarDeudas(transaccion, datos);
        guardarPago(transaccion, datos, operador);
        guardarExtorno(transaccion, datos, operador);
    }

    private void guardarDeudas(RecaudacionTransaccion transaccion, Map<String, Object> datos) {
        Object valor = datos.get("deudas");
        if (valor == null) {
            valor = datos.get("listaDeudas");
        }
        if (!(valor instanceof List<?> lista)) {
            return;
        }
        for (Object item : lista) {
            if (!(item instanceof Map<?, ?> mapa)) {
                continue;
            }
            Map<String, Object> deudaDatos = new LinkedHashMap<>();
            mapa.forEach((clave, contenido) -> deudaDatos.put(String.valueOf(clave), contenido));
            RecaudacionDeuda deuda = new RecaudacionDeuda();
            deuda.transaccionId = transaccion.id;
            deuda.numeroComprobante = texto(deudaDatos, "numeroComprobante");
            deuda.descripcion = texto(deudaDatos, "descripcion");
            deuda.monto = decimal(deudaDatos, "monto");
            deuda.importeDeudaMinima = decimal(deudaDatos, "importeDeudaMinima");
            deuda.fechaEmision = fechaLocal(deudaDatos, "fechaEmision");
            deuda.fechaVencimiento = fechaLocal(deudaDatos, "fechaVencimiento");
            deuda.periodo = texto(deudaDatos, "periodo");
            deuda.tipoIntegracion = texto(deudaDatos, "tipoIntegracion");
            deuda.indicadorRestriccionPago = texto(deudaDatos, "indicadorRestriccPago");
            deuda.cantidadSubconceptos = entero(deudaDatos, "cantidadSubconceptos");
            deuda.glosa = texto(deudaDatos, "glosa");
            deudas.persist(deuda);
        }
    }

    private void guardarPago(RecaudacionTransaccion transaccion,
                             Map<String, Object> datos,
                             Operador operador) {
        if (!esPago(operador, datos)) {
            return;
        }
        RecaudacionPago pago = new RecaudacionPago();
        pago.transaccionId = transaccion.id;
        pago.medioPago = texto(datos, "medioPago");
        pago.formaPago = texto(datos, "formaPago");
        pago.cuentaOrigen = texto(datos, "cuentaOrigen");
        pago.cuentaDestino = texto(datos, "cuentaDestino");
        pago.numeroTarjetaMask = mascara(texto(datos, "numeroTarjeta"));
        pago.binAdquiriente = texto(datos, "binAdquiriente");
        pago.codigoAutorizacion = primero(texto(datos, "codigoAprobacion"),
            texto(datos, "codigoAutorizacion"));
        pago.codigoMoneda = primero(texto(datos, "moneda"), texto(datos, "codigoMoneda"));
        pago.tipoCambio = decimal(datos, "tipoCambio");
        pago.nombreComercio = texto(datos, "nombreComercio");
        pago.numeroComercio = texto(datos, "numeroComercio");
        pago.numeroTerminal = texto(datos, "numeroTerminal");
        pago.fechaCaptura = fecha(datos, "fechaCaptura");
        pagos.persist(pago);
    }

    private void guardarExtorno(RecaudacionTransaccion transaccion,
                                Map<String, Object> datos,
                                Operador operador) {
        if (!esExtorno(operador, datos)) {
            return;
        }
        RecaudacionExtorno extorno = new RecaudacionExtorno();
        extorno.transaccionId = transaccion.id;
        extorno.identificadorPagoOriginal = texto(datos, "identificadorPago");
        extorno.numeroOperacionOriginal = texto(datos, "numeroOperacionOriginal");
        extorno.fechaOperacionOriginal = fecha(datos, "fechaOperacionOriginal");
        extorno.motivo = texto(datos, "motivoExtorno");
        extorno.estado = texto(datos, "estado");
        extorno.codigoAprobacion = texto(datos, "codigoAprobacion");
        extorno.fechaExtorno = OffsetDateTime.now();
        extornos.persist(extorno);
    }

    private boolean esPago(Operador operador, Map<String, Object> datos) {
        String nombre = operador.name();
        return nombre.contains("PAGO")
            || nombre.contains("RECARGA")
            || nombre.contains("VENTA")
            || datos.containsKey("numeroTarjeta")
            || datos.containsKey("medioPago")
            || datos.containsKey("formaPago");
    }

    private boolean esExtorno(Operador operador, Map<String, Object> datos) {
        String nombre = operador.name();
        return nombre.contains("EXTORNO")
            || nombre.contains("ANULACION")
            || nombre.contains("DEVOLUCION")
            || datos.containsKey("numeroOperacionOriginal")
            || datos.containsKey("motivoExtorno");
    }

    private boolean tieneAlgunDato(RecaudacionCliente cliente) {
        return cliente.tipoDocumento != null
            || cliente.numeroDocumento != null
            || cliente.nombre != null
            || cliente.email != null
            || cliente.direccion != null
            || cliente.distrito != null
            || cliente.codigoContribuyente != null
            || cliente.numeroSuministro != null;
    }

    private static String texto(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        return valor == null || valor.toString().isBlank() ? null : valor.toString().trim();
    }

    private static String primero(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private static BigDecimal decimal(Map<String, Object> datos, String clave) {
        String valor = texto(datos, clave);
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer entero(Map<String, Object> datos, String clave) {
        String valor = texto(datos, clave);
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static OffsetDateTime fecha(Map<String, Object> datos, String clave) {
        String valor = texto(datos, clave);
        if (valor == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(valor).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignoredLocal) {
                try {
                    return LocalDate.parse(valor).atStartOfDay().atOffset(ZoneOffset.UTC);
                } catch (DateTimeParseException ignoredDate) {
                    return null;
                }
            }
        }
    }

    private static LocalDate fechaLocal(Map<String, Object> datos, String clave) {
        OffsetDateTime fecha = fecha(datos, clave);
        return fecha == null ? null : fecha.toLocalDate();
    }

    private static String mascara(String tarjeta) {
        if (tarjeta == null || tarjeta.isBlank()) {
            return null;
        }
        if (tarjeta.contains("*") || tarjeta.contains("X") || tarjeta.contains("x")) {
            return tarjeta;
        }
        if (tarjeta.length() <= 4) {
            return "****";
        }
        return "*".repeat(tarjeta.length() - 4) + tarjeta.substring(tarjeta.length() - 4);
    }

    private OffsetDateTime fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return OffsetDateTime.now();
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.now();
        }
    }
}
