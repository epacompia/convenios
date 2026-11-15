package org.canalesCMAC.infrastructure.logging;

import java.util.regex.Pattern;

import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.canalesCMAC.infrastructure.persistence.SanitizadorDatos;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Deja en el log de transacciones el request y el response de cada operacion,
 * incluyendo el idTransaccion en todas las lineas y enmascarando datos sensibles.
 */
@ApplicationScoped
public class RegistroTransacciones {

    private static final Logger LOG = Logger.getLogger("convenios.transaccion");

    private static final Pattern XML_SENSIBLE = Pattern.compile(
        "(?is)<(numeroTarjeta|pinblock|track2Data|mac|claveTpv|clave|cvv|cvv2|pin)>(.*?)</\\1>");

    private static final Pattern FORM_SENSIBLE = Pattern.compile(
        "(?i)(numeroTarjeta|pinblock|track2Data|mac|claveTpv|clave|cvv|cvv2|pin)=([^&]*)");

    @Inject
    SanitizadorDatos sanitizador;

    public void inicio(RecaudacionRequest solicitud) {
        LOG.infof("TRANSACCION INICIO convenio=%s operacion=%s idTransaccion=%s traceId=%s",
            institucion(solicitud), operador(solicitud), idTransaccion(solicitud), traceId());
    }

    public void solicitud(RecaudacionRequest solicitud) {
        LOG.infof("TRANSACCION REQUEST convenio=%s operacion=%s idTransaccion=%s payload=%s",
            institucion(solicitud), operador(solicitud), idTransaccion(solicitud),
            sanitizador.sanitizar(solicitud));
    }

    public void respuesta(RecaudacionRequest solicitud, RecaudacionResponse respuesta,
                          long duracionMs) {
        LOG.infof("TRANSACCION RESPONSE convenio=%s operacion=%s idTransaccion=%s codigo=%s duracionMs=%d payload=%s",
            institucion(solicitud), operador(solicitud), idTransaccion(solicitud),
            respuesta == null ? "" : respuesta.codigo(), duracionMs,
            sanitizador.sanitizar(respuesta));
    }

    public void error(RecaudacionRequest solicitud, RecaudacionResponse respuesta,
                      RuntimeException error, long duracionMs) {
        LOG.errorf("TRANSACCION ERROR convenio=%s operacion=%s idTransaccion=%s duracionMs=%d mensaje=%s payload=%s",
            institucion(solicitud), operador(solicitud), idTransaccion(solicitud), duracionMs,
            error == null ? "" : error.getMessage(), sanitizador.sanitizar(respuesta));
    }

    public void proveedorSolicitud(RecaudacionRequest solicitud, PeticionSalida peticion,
                                   String url) {
        LOG.infof("PROVEEDOR REQUEST convenio=%s operacion=%s idTransaccion=%s url=%s ruta=%s contentType=%s payload=%s",
            institucion(solicitud), operador(solicitud), idTransaccion(solicitud),
            url, peticion.ruta(), peticion.contentType(),
            sanitizar(peticion.contenido()));
    }

    public void proveedorRespuesta(RecaudacionRequest solicitud, RespuestaCruda respuesta) {
        LOG.infof("PROVEEDOR RESPONSE convenio=%s operacion=%s idTransaccion=%s status=%d payload=%s",
            institucion(solicitud), operador(solicitud), idTransaccion(solicitud),
            respuesta.status(), sanitizar(respuesta.cuerpo()));
    }

    public void proveedorError(RecaudacionRequest solicitud, RuntimeException error) {
        LOG.errorf("PROVEEDOR ERROR convenio=%s operacion=%s idTransaccion=%s mensaje=%s",
            institucion(solicitud), operador(solicitud), idTransaccion(solicitud),
            error == null ? "" : error.getMessage());
    }

    public void proveedorErrorHttp(String convenio, int status, String cuerpo,
                                   RuntimeException error) {
        LOG.errorf("PROVEEDOR RESPONSE ERROR convenio=%s operacion=%s idTransaccion=%s status=%d mensaje=%s payload=%s",
            convenio, operacionMdc(), idTransaccionMdc(), status,
            error == null ? "" : error.getMessage(), sanitizar(cuerpo));
    }

    private static String traceId() {
        Object traceId = org.jboss.logging.MDC.get("recaudacion.traceId");
        return traceId == null ? "" : traceId.toString();
    }

    private static String operacionMdc() {
        Object operacion = org.jboss.logging.MDC.get("recaudacion.operacion");
        return operacion == null ? "" : operacion.toString();
    }

    private static String idTransaccionMdc() {
        Object id = org.jboss.logging.MDC.get("recaudacion.idTransaccion");
        return id == null ? "" : id.toString();
    }

    private String sanitizar(String payload) {
        if (payload == null || payload.isBlank()) {
            return "";
        }
        String json = sanitizador.sanitizarJsonTexto(payload);
        if (json != null) {
            return json;
        }
        String xml = XML_SENSIBLE.matcher(payload).replaceAll("<$1>****</$1>");
        if (!xml.equals(payload)) {
            return xml;
        }
        String form = FORM_SENSIBLE.matcher(payload).replaceAll("$1=****");
        return form.equals(payload) ? payload : form;
    }

    private static String idTransaccion(RecaudacionRequest solicitud) {
        return solicitud == null || solicitud.cabecera() == null
            ? ""
            : solicitud.cabecera().idTransaccion();
    }

    private static String institucion(RecaudacionRequest solicitud) {
        return solicitud == null || solicitud.institucion() == null
            ? ""
            : solicitud.institucion().name();
    }

    private static String operador(RecaudacionRequest solicitud) {
        return solicitud == null || solicitud.operador() == null
            ? ""
            : solicitud.operador().name();
    }
}
