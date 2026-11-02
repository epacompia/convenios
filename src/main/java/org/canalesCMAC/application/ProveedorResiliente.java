package org.canalesCMAC.application;

import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.ProtocoloCliente;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.infrastructure.persistence.RegistroIntentoProveedor;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProveedorResiliente {

    private static final Logger LOG = Logger.getLogger(ProveedorResiliente.class);

    @Inject
    RegistroIntentoProveedor registro;

    @Inject
    ResilienciaConvenios resiliencia;

    @Inject
    MeterRegistry metricas;

    public RespuestaCruda ejecutar(ProtocoloCliente cliente,
                                   Convenio convenio,
                                   PeticionSalida peticion,
                                   RecaudacionRequest solicitud) {
        Institucion institucion = convenio.institucion();
        resiliencia.verificar(institucion);
        ResilienciaConvenios.Permiso permiso;
        try {
            permiso = resiliencia.entrar(institucion);
        } catch (RuntimeException e) {
            resiliencia.abortar(institucion);
            throw e;
        }
        try (ResilienciaConvenios.Permiso recurso = permiso) {
            long inicio = System.nanoTime();
            String endpoint = convenio.url() + peticion.ruta();
            Convenio efectivo = esIdempotente(solicitud.operador())
                ? convenio
                : convenio.sinReintentos();
            try {
                RespuestaCruda respuesta = cliente.ejecutar(efectivo, peticion);
                resiliencia.exito(institucion);
                registrarMetrica(institucion, solicitud, "ok", milisegundos(inicio));
                registro.registrar(solicitud.cabecera().idTransaccion(),
                    institucion.name(), solicitud.operador().name(),
                    convenio.protocolo(), endpoint, 1, respuesta.status(), null, null,
                    milisegundos(inicio), resiliencia.estado(institucion), solicitud,
                    respuesta.cuerpo());
                return respuesta;
            } catch (RuntimeException e) {
                resiliencia.fallo(institucion);
                registrarMetrica(institucion, solicitud, "error", milisegundos(inicio));
                int status = e instanceof ProveedorException proveedor
                    ? proveedor.status()
                    : 500;
                registro.registrar(solicitud.cabecera().idTransaccion(),
                    institucion.name(), solicitud.operador().name(),
                    convenio.protocolo(), endpoint, 1, status, String.valueOf(status),
                    e.getMessage(), milisegundos(inicio), resiliencia.estado(institucion),
                    solicitud, null);
                LOG.errorf("fallo de proveedor institucion=%s status=%d motivo=%s",
                    institucion, status, e.getMessage());
                throw e;
            }
        }
    }

    private static boolean esIdempotente(Operador operador) {
        String nombre = operador.name();
        return nombre.startsWith("CONSULTA")
            || nombre.startsWith("CONSULTAR")
            || nombre.startsWith("ECHO")
            || nombre.equals("SALDO")
            || nombre.equals("BUSCAR_PRODUCTO")
            || nombre.equals("RECUPERAR_ORDEN_PAGO")
            || nombre.equals("REIMPRIMIR_TRANSACCION")
            || nombre.equals("CONSULTAR_LOTE");
    }

    private void registrarMetrica(Institucion institucion,
                                  RecaudacionRequest solicitud,
                                  String resultado,
                                  long duracionMs) {
        metricas.counter("recaudacion.proveedor.llamadas",
            "institucion", institucion.name(),
            "operador", solicitud.operador().name(),
            "resultado", resultado).increment();
        metricas.timer("recaudacion.proveedor.duracion",
            "institucion", institucion.name(),
            "operador", solicitud.operador().name()).record(
                java.time.Duration.ofMillis(duracionMs));
    }

    private static long milisegundos(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }
}
