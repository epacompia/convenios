package org.canalesCMAC.adapter.out.westernunion;

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
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WesternUnionSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG =
        Logger.getLogger(WesternUnionSoapMessageCodec.class);

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private static final String SOAP_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String SERVICE_NS =
        "http://gcr.sepsa.com.ar/wsdl/GatewayService/";

    @Override
    public Institucion institucion() {
        return Institucion.WESTERN_UNION;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo SOAP SEPSA operador=%s", peticion.operador());
        Map<String, String> campos = campos(peticion);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
            .append(" xmlns:ns2=\"").append(SERVICE_NS).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<ns2:").append(operacionSoap(peticion.operador())).append(">");
        xml.append("<request>");

        campos.forEach((campo, valor) -> {
            if (valor != null && !valor.isBlank()) {
                xml.append("<").append(campo).append(">");
                xml.append(escapar(valor));
                xml.append("</").append(campo).append(">");
            }
        });
        agregarLista(xml, "listaParametros", "parametro",
            peticion.datos().get(Canonico.PARAMETROS_OPERACION));
        agregarLista(xml, "datosAdicionales", "datoAdicional",
            peticion.datos().get(Canonico.DATOS_ADICIONALES));
        agregarLista(xml, "listaTransacciones", "transaccion",
            peticion.datos().get(Canonico.TRANSACCIONES));

        xml.append("</request>");
        xml.append("</ns2:").append(operacionSoap(peticion.operador())).append(">");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    private Map<String, String> campos(RecaudacionRequest peticion) {
        Map<String, String> campos = new LinkedHashMap<>();
        poner(campos, Canonico.ID_AGENTE, peticion, Canonico.ID_AGENTE);
        poner(campos, Canonico.ID_LOCACION, peticion, Canonico.ID_LOCACION);
        poner(campos, Canonico.CAJA, peticion, Canonico.CAJA);
        poner(campos, Canonico.ID_PRODUCTO, peticion, Canonico.ID_PRODUCTO);
        poner(campos, Canonico.ID_TRANSACCION, peticion, Canonico.ID_TRANSACCION);
        poner(campos, Canonico.ID_LOTE, peticion, Canonico.ID_LOTE);
        campos.put("operador", peticion.operador().name());
        return campos;
    }

    private void poner(Map<String, String> campos, String nombre,
                       RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor != null && !valor.toString().isBlank()) {
            campos.put(nombre, valor.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void agregarLista(StringBuilder xml, String contenedor, String elemento, Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return;
        }
        xml.append("<").append(contenedor).append(">");
        for (Object item : lista) {
            if (!(item instanceof Map<?, ?> mapa)) {
                continue;
            }
            xml.append("<").append(elemento).append(">");
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                String clave = String.valueOf(entrada.getKey());
                if ("listaParametros".equals(contenedor) && "indice".equals(clave)) {
                    clave = "ordinal";
                }
                if ("datosAdicionales".equals(contenedor) && "codigo".equals(clave)) {
                    clave = "descripcion";
                }
                Object valorCampo = entrada.getValue();
                if (valorCampo != null) {
                    xml.append("<").append(clave).append(">");
                    xml.append(escapar(valorCampo.toString()));
                    xml.append("</").append(clave).append(">");
                }
            }
            xml.append("</").append(elemento).append(">");
        }
        xml.append("</").append(contenedor).append(">");
    }

    private String operacionSoap(Operador operador) {
        return switch (operador) {
            case BUSCAR_PRODUCTO -> "buscarProducto";
            case CONSULTAR_TRANSACCION -> "consultarTransaccion";
            case CONSULTAR_DATOS_ADICIONALES -> "consultarDatosAdicionales";
            case AGREGAR_DATOS_ADICIONALES -> "agregarDatosAdicionales";
            case CONFIRMAR_LOTE -> "confirmarLote";
            case CONFIRMAR_DIRECTO -> "confirmarDirecto";
            case CONSULTAR_LOTE -> "consultarLote";
            case REIMPRIMIR_TRANSACCION -> "reimprimirTransaccion";
            case ANULAR_TRANSACCION -> "anularTransaccion";
            default -> throw new OperadorNoSoportadoException(
                "Operador " + operador + " no soportado por WESTERN_UNION");
        };
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
        LOG.debugf("respuesta SEPSA operador=%s", peticion.operador());
        NodoXml raiz = SoapSupport.cuerpo(respuesta.cuerpo());
        NodoXml respuestaNodo = raiz.hijo("response");
        if (respuestaNodo == null) {
            respuestaNodo = raiz.hijo("return");
        }
        if (respuestaNodo == null) {
            respuestaNodo = raiz;
        }

        NodoXml error = respuestaNodo.hijo("error");
        String codigo = error != null ? "01" : primero(
            texto(respuestaNodo, "codigoRespuesta"), texto(respuestaNodo, "codigo"), "");
        if (codigo == null || codigo.isBlank()) {
            NodoXml loteEstado = respuestaNodo.hijo("lote");
            String estado = loteEstado == null
                ? texto(respuestaNodo, "estado")
                : texto(loteEstado, "estado");
            if (estado == null || estado.isBlank()) {
                throw new ProveedorException(
                    "Respuesta de WESTERN_UNION sin código ni estado",
                    ProveedorException.BAD_GATEWAY, false, null);
            }
            codigo = estadoCorrecto(estado) ? "00" : "01";
        }
        String mensaje = primero(error == null ? null : texto(error, "mensaje"),
            texto(respuestaNodo, "mensaje"), "OK");

        Map<String, Object> cliente = Mapas.nuevo();
        Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, texto(respuestaNodo, "descripcionCliente"));

        Map<String, Object> transaccion = Mapas.nuevo();
        NodoXml lote = respuestaNodo.hijo("lote");
        Mapas.poner(transaccion, Canonico.NUMERO_LOTE,
            lote == null ? texto(respuestaNodo, "idLote") : texto(lote, "idLote"));
        Mapas.poner(transaccion, Canonico.ESTADO,
            lote == null ? texto(respuestaNodo, "estado") : texto(lote, "estado"));
        Mapas.poner(transaccion, Canonico.MONTO, numero(respuestaNodo, "importe"));
        Mapas.poner(transaccion, Canonico.MONEDA, texto(respuestaNodo, "moneda"));
        Mapas.poner(transaccion, Canonico.CODIGO_APROBACION, texto(respuestaNodo, "comprobante"));

        List<Map<String, Object>> parametros = lista(respuestaNodo, "listaParametros", "parametro");
        for (Map<String, Object> parametro : parametros) {
            if (parametro.containsKey("ordinal")) {
                parametro.put("indice", parametro.remove("ordinal"));
            }
        }
        if (!parametros.isEmpty()) {
            transaccion.put(Canonico.PARAMETROS, parametros);
        }

        List<Map<String, Object>> transacciones = lista(
            respuestaNodo, "listaTransacciones", "transaccion");
        if (transacciones.isEmpty()) {
            transacciones = lista(respuestaNodo, "transacciones", "transaccion");
        }
        if (!transacciones.isEmpty()) {
            List<Map<String, Object>> deudas = new ArrayList<>();
            for (Map<String, Object> item : transacciones) {
                Map<String, Object> deuda = Mapas.nuevo();
                Mapas.poner(deuda, Canonico.NUM_OPERACION, item.get("idTransaccion"));
                Mapas.poner(deuda, Canonico.MONTO, item.get("importe"));
                Mapas.poner(deuda, Canonico.MONEDA, item.get("moneda"));
                Mapas.poner(deuda, Canonico.ESTADO, item.get("estado"));
                Mapas.poner(deuda, Canonico.CODIGO_APROBACION, item.get("comprobante"));
                deudas.add(deuda);
            }
            transaccion.put(Canonico.DEUDAS, deudas);
        }

        List<Map<String, Object>> datosAdicionales = lista(
            respuestaNodo, "datosAdicionales", "datoAdicional");
        if (!datosAdicionales.isEmpty()) {
            Map<String, Object> adicionales = Mapas.nuevo();
            adicionales.put("datosAdicionales", datosAdicionales);
            transaccion.put(Canonico.DATOS_ADICIONALES, adicionales);
        } else {
            Map<String, Object> adicionales = escalares(respuestaNodo);
            if (!adicionales.isEmpty()) {
                transaccion.put(Canonico.DATOS_ADICIONALES, adicionales);
            }
        }

        return RecaudacionResponse.de(Institucion.WESTERN_UNION, peticion.operador(),
            peticion.cabecera(), codigo, mensaje, cliente,
            transaccion.isEmpty() ? List.of() : List.of(transaccion));
    }

    private static List<Map<String, Object>> lista(NodoXml raiz, String contenedor, String elemento) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (NodoXml lista : raiz.hijos(contenedor)) {
            for (NodoXml nodo : lista.hijos(elemento)) {
                Map<String, Object> item = escalares(nodo);
                if (!item.isEmpty()) {
                    resultado.add(item);
                }
            }
        }
        return resultado;
    }

    private static Map<String, Object> escalares(NodoXml nodo) {
        Map<String, Object> valores = Mapas.nuevo();
        for (NodoXml hijo : nodo.hijos()) {
            if (hijo.hijos().isEmpty() && hijo.texto() != null && !hijo.texto().isBlank()) {
                Mapas.poner(valores, hijo.nombre(), hijo.texto().trim());
            }
        }
        return valores;
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

    private static boolean estadoCorrecto(String estado) {
        String normal = estado.trim().toUpperCase(java.util.Locale.ROOT);
        return "ACEPTADA".equals(normal)
            || "ACEPTADO".equals(normal)
            || "OK".equals(normal)
            || "PROCESADA".equals(normal)
            || "PROCESADO".equals(normal)
            || "CONFIRMADA".equals(normal)
            || "CONFIRMADO".equals(normal)
            || "EXITOSA".equals(normal)
            || "EXITOSO".equals(normal);
    }
}
