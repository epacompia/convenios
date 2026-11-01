package org.canalesCMAC.infrastructure.persistence;

import java.time.OffsetDateTime;

import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RegistroIntentoProveedor {

    private static final Logger LOG = Logger.getLogger(RegistroIntentoProveedor.class);

    @Inject
    RecaudacionIntentoProveedorRepository repositorio;

    @Inject
    ConvenioRepository convenios;

    @Inject
    SanitizadorDatos sanitizador;

    @ConfigProperty(name = "recaudacion.intentos.enabled", defaultValue = "true")
    boolean habilitado;

    @Transactional
    public void registrar(String idTransaccion,
                          String institucion,
                          String operador,
                          String protocolo,
                          String endpoint,
                          int intento,
                          Integer httpStatus,
                          String codigoError,
                          String mensajeError,
                          long duracionMs,
                          String circuitBreakerEstado,
                          RecaudacionRequest solicitud,
                          String responseJson) {
        if (!habilitado) {
            return;
        }
        try {
            RecaudacionIntentoProveedor entidad = new RecaudacionIntentoProveedor();
            entidad.idTransaccion = idTransaccion;
            entidad.convenioId = convenios.porCodigo(institucion)
                .map(convenio -> convenio.id)
                .orElse(null);
            entidad.operador = operador;
            entidad.protocolo = protocolo;
            entidad.endpoint = endpoint;
            entidad.metodo = "POST";
            entidad.intento = intento;
            entidad.httpStatus = httpStatus;
            entidad.codigoError = codigoError;
            entidad.mensajeError = mensajeError;
            entidad.duracionMs = duracionMs;
            entidad.circuitBreakerEstado = circuitBreakerEstado;
            entidad.requestJson = sanitizador.sanitizar(solicitud);
            entidad.responseJson = sanitizador.sanitizarJsonTexto(responseJson);
            entidad.fechaIntento = OffsetDateTime.now();
            repositorio.persist(entidad);
        } catch (RuntimeException e) {
            LOG.warnf("no se pudo registrar intento de proveedor idTransaccion=%s: %s",
                idTransaccion, e.getMessage());
        }
    }
}
