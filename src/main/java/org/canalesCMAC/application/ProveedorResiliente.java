package org.canalesCMAC.application;

import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.ProtocoloCliente;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.infrastructure.persistence.RegistroIntentoProveedor;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Borde resiliente de salida hacia proveedores.
 *
 * <p>Equivalente Quarkus a Spring Cloud Circuit Breaker: SmallRye Fault
 * Tolerance con circuit breaker y timeout.</p>
 */
@ApplicationScoped
public class ProveedorResiliente {

    @Inject
    RegistroIntentoProveedor registro;

    @CircuitBreaker(requestVolumeThreshold = 20, failureRatio = 0.5, delay = 5000)
    @Timeout(15000)
    public RespuestaCruda ejecutar(ProtocoloCliente cliente,
                                   Convenio convenio,
                                   PeticionSalida peticion,
                                   RecaudacionRequest solicitud) {
        long inicio = System.nanoTime();
        String endpoint = convenio.url() + peticion.ruta();
        try {
            RespuestaCruda respuesta = cliente.ejecutar(convenio, peticion);
            registro.registrar(solicitud.cabecera().idTransaccion(),
                convenio.institucion().name(), solicitud.operador().name(),
                convenio.protocolo(), endpoint, 1, respuesta.status(), null, null,
                milisegundos(inicio), "CERRADO", solicitud, respuesta.cuerpo());
            return respuesta;
        } catch (RuntimeException e) {
            int status = e instanceof ProveedorException proveedor
                ? proveedor.status()
                : 500;
            registro.registrar(solicitud.cabecera().idTransaccion(),
                convenio.institucion().name(), solicitud.operador().name(),
                convenio.protocolo(), endpoint, 1, status, String.valueOf(status), e.getMessage(),
                milisegundos(inicio), "FALLO", solicitud, null);
            throw e;
        }
    }

    private static long milisegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }
}
