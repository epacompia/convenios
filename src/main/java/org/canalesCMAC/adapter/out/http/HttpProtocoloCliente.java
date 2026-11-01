package org.canalesCMAC.adapter.out.http;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.canalesCMAC.application.Convenio;
import org.canalesCMAC.application.ConvenioRegistro;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.ProtocoloCliente;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;

public abstract class HttpProtocoloCliente implements ProtocoloCliente {

    private static final Logger LOG = Logger.getLogger(HttpProtocoloCliente.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConvenioRegistro registro;

    @Override
    public RespuestaCruda ejecutar(Convenio convenio, PeticionSalida peticion) {
        Map<String, Object> encabezados = new HashMap<>(registro.autenticacion(convenio.institucion()).encabezados());
        String apiKey = registro.apiKey(convenio.institucion());
        if (apiKey != null && !apiKey.isBlank()) {
            encabezados.put("ApiKey", apiKey);
        }
        String token = registro.token(convenio.institucion());
        if (token != null && !token.isBlank()) {
            encabezados.put("Authorization", "Bearer " + token);
        }
        encabezados.put(Exchange.CONTENT_TYPE, peticion.contentType());
        encabezados.putAll(encabezadosPropios(convenio, peticion));
        String url = endpoint(convenio, peticion);
        LOG.debugf("envio a proveedor institucion=%s protocolo=%s url=%s bytesSolicitud=%d",
            convenio.institucion(), protocolo(), url, longitud(peticion.contenido()));
        String cuerpo = enviar(convenio, url, peticion.contenido(), encabezados);
        return new RespuestaCruda(200, cuerpo);
    }

    protected Map<String, Object> encabezadosPropios(Convenio convenio, PeticionSalida peticion) {
        return Map.of();
    }

    protected String endpoint(Convenio convenio, PeticionSalida peticion) {
        String base = convenio.url() + peticion.ruta();
        long timeout = convenio.timeout();
        if (timeout <= 0) {
            return base;
        }
        return base + (base.contains("?") ? "&" : "?") + "connectTimeout=" + timeout + "&soTimeout=" + timeout;
    }

    private String enviar(Convenio convenio, String url, String contenido, Map<String, Object> encabezados) {
        int totalIntentos = convenio.reintentos() + 1;
        long delay = convenio.reintentosDelay();
        for (int intento = 1; ; intento++) {
            long inicio = System.nanoTime();
            try {
                String respuesta = producerTemplate.requestBodyAndHeaders(url, contenido, encabezados, String.class);
                LOG.debugf("respuesta de proveedor institucion=%s intento=%d duracionMs=%d bytesRespuesta=%d",
                    convenio.institucion(), intento, milisegundos(inicio), longitud(respuesta));
                return respuesta;
            } catch (CamelExecutionException excepcion) {
                ProveedorException falla = traducir(convenio, excepcion);
                if (intento >= totalIntentos || !falla.reintentable()) {
                    LOG.errorf("fallo de proveedor institucion=%s status=%d intento=%d duracionMs=%d motivo=%s",
                        convenio.institucion(), falla.status(), intento, milisegundos(inicio), falla.getMessage());
                    throw falla;
                }
                LOG.warnf("reintentando proveedor institucion=%s intento=%d/%d duracionMs=%d motivo=%s",
                    convenio.institucion(), intento, totalIntentos, milisegundos(inicio), falla.getMessage());
                esperar(delay);
            }
        }
    }

    private ProveedorException traducir(Convenio convenio, CamelExecutionException excepcion) {
        Throwable causa = excepcion.getCause() == null ? excepcion : excepcion.getCause();
        if (esTimeout(causa)) {
            return new ProveedorException(
                "Tiempo de espera agotado con " + convenio.institucion(),
                ProveedorException.GATEWAY_TIMEOUT, true, causa);
        }
        if (causa instanceof HttpOperationFailedException http) {
            return new ProveedorException(
                "El proveedor " + convenio.institucion() + " respondió con estado " + http.getStatusCode(),
                ProveedorException.BAD_GATEWAY, false, causa);
        }
        return new ProveedorException(
            "Error de comunicación con " + convenio.institucion() + ": " + causa.getMessage(),
            ProveedorException.BAD_GATEWAY, false, causa);
    }

    private boolean esTimeout(Throwable causa) {
        Throwable actual = causa;
        while (actual != null) {
            if (actual instanceof java.net.SocketTimeoutException
                || actual instanceof java.util.concurrent.TimeoutException
                || actual.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private void esperar(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            throw new ProveedorException(
                "Reintento interrumpido", ProveedorException.BAD_GATEWAY, false, interrupcion);
        }
    }

    private static long milisegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }

    private static int longitud(String valor) {
        return valor == null ? 0 : valor.length();
    }
}
