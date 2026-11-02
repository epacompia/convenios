package org.canalesCMAC.adapter.out.yaganaste;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.adapter.out.soap.NodoXml;
import org.canalesCMAC.adapter.out.soap.SoapSupport;
import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

@ApplicationScoped
public class YaGanasteSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(YaGanasteSoapMessageCodec.class);
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private static final String SOAP_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String SIGMA_NS = "net.sigma.h2h.ws";

    @Override
    public Institucion institucion() {
        return Institucion.YAGANASTE;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo SOAP YAGANASTE operador=%s", peticion.operador());
        String operacion = operacionSoap(peticion.operador());
        Map<String, Object> campos = campos(peticion);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
            .append(" xmlns:net=\"").append(SIGMA_NS).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<net:").append(operacion).append(">");

        agregar(xml, "tpv", campos.get(Canonico.TPV));
        agregar(xml, "claveTpv", campos.get(Canonico.CLAVE_TPV));
        if (!"saldo".equals(operacion)) {
            agregar(xml, "codigoProducto", campos.get(Canonico.CODIGO_PRODUCTO));
            agregar(xml, "referenciaOperacion", campos.get(Canonico.REFERENCIA_OPERACION));
        }
        agregar(xml, "fechaHoraOperacion", campos.get(Canonico.FECHA_OPERACION));
        agregarParametrosOperacion(xml, campos.get(Canonico.PARAMETROS_OPERACION));

        xml.append("</net:").append(operacion).append(">");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    private String operacionSoap(org.canalesCMAC.domain.model.Operador operador) {
        return switch (operador) {
            case ECHO -> "echo";
            case VENTA -> "venta";
            case DEVOLUCION -> "devolucion";
            case CONSULTA -> "consulta";
            case CONSULTA_PREVIA -> "consultaPrevia";
            case SALDO -> "saldo";
            default -> throw new org.canalesCMAC.domain.exception.OperadorNoSoportadoException(
                "Operador " + operador + " no soportado por YAGANASTE");
        };
    }

    private Map<String, Object> campos(RecaudacionRequest peticion) {
        Map<String, Object> campos = new LinkedHashMap<>();
        poner(campos, Canonico.TPV, peticion, Canonico.TPV);
        poner(campos, Canonico.CLAVE_TPV, peticion, Canonico.CLAVE_TPV);
        poner(campos, Canonico.CODIGO_PRODUCTO, peticion, Canonico.CODIGO_PRODUCTO);
        poner(campos, Canonico.REFERENCIA_OPERACION, peticion, Canonico.REFERENCIA_OPERACION);
        poner(campos, Canonico.FECHA_OPERACION, peticion, Canonico.FECHA_OPERACION);
        Object parametros = peticion.datos().get(Canonico.PARAMETROS_OPERACION);
        if (parametros != null) {
            campos.put(Canonico.PARAMETROS_OPERACION, parametros);
        }
        return campos;
    }

    private void poner(Map<String, Object> campos, String nombre,
                       RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor != null) {
            campos.put(nombre, valor);
        }
    }

    private void agregar(StringBuilder xml, String nombre, Object valor) {
        if (valor == null) {
            return;
        }
        String texto = valor.toString();
        if (texto.isBlank()) {
            return;
        }
        xml.append("<").append(nombre).append(">");
        xml.append(escapar(texto));
        xml.append("</").append(nombre).append(">");
    }

    @SuppressWarnings("unchecked")
    private void agregarParametrosOperacion(StringBuilder xml, Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return;
        }
        for (Object item : lista) {
            if (!(item instanceof Map<?, ?> parametro)) {
                continue;
            }
            xml.append("<parametrosOperacion>");
            Object indice = parametro.get("indice");
            Object valorParametro = parametro.get("valor");
            if (indice != null) {
                xml.append("<indice>").append(escapar(indice.toString())).append("</indice>");
            }
            if (valorParametro != null) {
                xml.append("<valor>").append(escapar(valorParametro.toString())).append("</valor>");
            }
            xml.append("</parametrosOperacion>");
        }
    }

    private String escapar(String valor) {
        return valor
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        LOG.debugf("respuesta YAGANASTE operador=%s", peticion.operador());
        NodoXml raiz = SoapSupport.cuerpo(respuesta.cuerpo());
        NodoXml response = raiz.hijo("response") == null ? raiz : raiz.hijo("response");

        String codigo = primero(texto(response, "responseCode"), texto(response, "codigo"), "");
        if (codigo == null || codigo.isBlank()) {
            throw new ProveedorException(
                "Respuesta de YAGANASTE sin código",
                ProveedorException.BAD_GATEWAY, false, null);
        }
        String mensaje = primero(texto(response, "errMessage"), texto(response, "mensaje"), "OK");

        Map<String, Object> cliente = Mapas.nuevo();
        Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, texto(response, "nombreCliente"));

        Map<String, Object> transaccion = Mapas.nuevo();
        Mapas.poner(transaccion, Canonico.ESTADO, texto(response, "estadoVenta"));
        Mapas.poner(transaccion, Canonico.MONTO, numero(response, "importe"));
        Mapas.poner(transaccion, Canonico.MONEDA, texto(response, "monedaOrigen"));
        Mapas.poner(transaccion, Canonico.FEE, numero(response, "fee"));
        Mapas.poner(transaccion, Canonico.IMPORTE_ORIGEN, numero(response, "importeOrigen"));
        Mapas.poner(transaccion, Canonico.SALDO, numero(response, "saldo"));
        Mapas.poner(transaccion, Canonico.REFERENCIA_PROVEEDOR, texto(response, "referenciaSigma"));
        Mapas.poner(transaccion, Canonico.REFERENCIA_OPERADOR, texto(response, "refOperador"));

        List<Map<String, Object>> parametros = parametros(response);
        if (!parametros.isEmpty()) {
            transaccion.put(Canonico.PARAMETROS, parametros);
        }

        return RecaudacionResponse.de(Institucion.YAGANASTE, peticion.operador(),
            peticion.cabecera(), codigo, mensaje, cliente,
            transaccion.isEmpty() ? List.of() : List.of(transaccion));
    }

    private static List<Map<String, Object>> parametros(NodoXml raiz) {
        List<Map<String, Object>> parametros = new ArrayList<>();
        for (String nombre : List.of("parametros", "parametro", "listaParametros")) {
            for (NodoXml nodo : raiz.hijos(nombre)) {
                Map<String, Object> parametro = Mapas.nuevo();
                Mapas.poner(parametro, "indice", texto(nodo, "indice"));
                Mapas.poner(parametro, "valor", texto(nodo, "valor"));
                if (!parametro.isEmpty()) {
                    parametros.add(parametro);
                }
            }
        }
        return parametros;
    }

    private static String texto(NodoXml nodo, String campo) {
        String valor = nodo.textoDe(campo);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static Object numero(NodoXml nodo, String campo) {
        String valor = texto(nodo, campo);
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return valor;
        }
    }

    private static String primero(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return "";
    }
}
