package org.canalesCMAC.adapter.out.universidadcontinental;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base para Universidad Continental (Universidad y Centro de Idiomas).
 *
 * <p>Contrato tomado de {@code docs/SERVICIO WEB PARA LA COBRANZA EN LINEA.docx}:
 * consulta, pago y extorno sobre {@code /api/pagouniversidad} y
 * {@code /api/pagoidiomas}, autenticado con header {@code ApiKey}.</p>
 */
abstract class AbstractUnivContinentalCodec implements MapeadorOperacion {

    private static final String CONTENT_TYPE = "application/json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected abstract String rutaBase();

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        String operacion = peticion.operador().name();
        Map<String, Object> request = new LinkedHashMap<>();
        String ruta;
        if (operacion.endsWith("_CONSULTA_DEUDA")) {
            request.put("codigocontrato", contrato(peticion));
            poner(request, "moneda", peticion, Canonico.MONEDA);
            poner(request, "canal", peticion, Canonico.CANAL);
            ruta = rutaBase() + "/consulta";
        } else if (operacion.endsWith("_PAGO_DEUDA")) {
            request.put("CodigoContrato", contrato(peticion));
            poner(request, "NumeroTrace", peticion, Canonico.NUMERO_TRACE);
            poner(request, "Fecha", peticion, Canonico.FECHA_OPERACION);
            poner(request, "Canal", peticion, Canonico.CANAL);
            poner(request, "Datos", peticion, Canonico.DATOS);
            ruta = rutaBase() + "/pago";
        } else if (operacion.endsWith("_EXTORNO_DEUDA")) {
            request.put("CodigoContrato", contrato(peticion));
            poner(request, "NumeroTrace", peticion, Canonico.NUMERO_TRACE);
            poner(request, "Fecha", peticion, Canonico.FECHA_OPERACION);
            ruta = rutaBase() + "/extorno";
        } else {
            throw new OperadorNoSoportadoException(
                "Operador " + peticion.operador() + " no soportado por " + institucion());
        }
        try {
            return new PeticionSalida(objectMapper.writeValueAsString(request), ruta, CONTENT_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error construyendo request de " + institucion(), e);
        }
    }

    private String contrato(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.CONTRATO);
        if (valor == null || valor.toString().isBlank()) {
            valor = peticion.datos().get("codigocontrato");
        }
        return valor == null ? null : valor.toString();
    }

    private void poner(Map<String, Object> request, String nombre,
                       RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor != null && !valor.toString().isBlank()) {
            request.put(nombre, valor);
        }
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        String codigo = "00";
        String mensaje = "";
        Map<String, Object> cliente = Mapas.nuevo();
        Map<String, Object> transaccion = Mapas.nuevo();
        try {
            JsonNode root = objectMapper.readTree(respuesta.cuerpo());
            codigo = normalizarCodigo(texto(root, "Codigo"));
            mensaje = texto(root, "Mensaje");
            Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, texto(root, "Cliente"));
            Mapas.poner(transaccion, Canonico.NUM_OPERACION, texto(root, "NumeroOperacion"));
            Mapas.poner(transaccion, Canonico.NUMERO_COMPROBANTE, texto(root, "CodigoContrato"));

            JsonNode datos = root.get("Datos");
            if (datos != null && datos.isArray()) {
                List<Map<String, Object>> deudas = new ArrayList<>();
                for (JsonNode documento : datos) {
                    Map<String, Object> deuda = Mapas.nuevo();
                    Mapas.poner(deuda, Canonico.FECHA_EMISION, texto(documento, "FechaCreacion"));
                    Mapas.poner(deuda, Canonico.FECHA_VENCIMIENTO,
                        texto(documento, "FechaVencimiento"));
                    Mapas.poner(deuda, Canonico.MONEDA, texto(documento, "Moneda"));
                    Mapas.poner(deuda, Canonico.MONTO, numero(documento, "Monto"));
                    Mapas.poner(deuda, Canonico.NUMERO_COMPROBANTE,
                        texto(documento, "NumeroDocumento"));
                    Mapas.poner(deuda, Canonico.PERIODO, texto(documento, "Periodo"));
                    if (!deuda.isEmpty()) {
                        deudas.add(deuda);
                    }
                }
                if (!deudas.isEmpty()) {
                    transaccion.put(Canonico.DEUDAS, deudas);
                    Mapas.poner(transaccion, Canonico.MONTO, deudas.get(0).get(Canonico.MONTO));
                    Mapas.poner(transaccion, Canonico.MONEDA, deudas.get(0).get(Canonico.MONEDA));
                }
            } else if (datos != null && !datos.isNull()) {
                transaccion.put(Canonico.DATOS_ADICIONALES,
                    objectMapper.convertValue(datos, Map.class));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta inválida de " + institucion(), e);
        }

        return RecaudacionResponse.de(institucion(), peticion.operador(), peticion.cabecera(),
            codigo, mensaje, cliente,
            transaccion.isEmpty() ? List.of() : List.of(transaccion));
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "00";
        }
        return "00000".equals(codigo) || "00".equals(codigo) ? "00" : codigo;
    }

    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()
            ? nodo.get(campo).asText().trim()
            : null;
    }

    private static Object numero(JsonNode nodo, String campo) {
        if (!nodo.hasNonNull(campo)) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        return valor.isNumber() ? valor.decimalValue() : new BigDecimal(valor.asText());
    }
}
