package org.canalesCMAC.application;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.ProtocoloCliente;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.InstitucionNoSoportadaException;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConvenioOrquestador {

    private static final Logger LOG = Logger.getLogger(ConvenioOrquestador.class);

    @Inject
    ConvenioRegistro registro;

    @Inject
    Instance<MapeadorOperacion> mapeadores;

    @Inject
    Instance<ProtocoloCliente> clientes;

    @Inject
    ProveedorResiliente resiliencia;

    @Inject
    CatalogoCodigos catalogo;

    private Map<Institucion, MapeadorOperacion> mapeadoresPorInstitucion;
    private Map<String, ProtocoloCliente> clientesPorProtocolo;

    @PostConstruct
    void inicializar() {
        Map<Institucion, MapeadorOperacion> porInstitucion = new EnumMap<>(Institucion.class);
        for (MapeadorOperacion mapeador : mapeadores) {
            porInstitucion.put(mapeador.institucion(), mapeador);
        }
        this.mapeadoresPorInstitucion = porInstitucion;

        Map<String, ProtocoloCliente> porProtocolo = new HashMap<>();
        for (ProtocoloCliente cliente : clientes) {
            porProtocolo.put(cliente.protocolo(), cliente);
        }
        this.clientesPorProtocolo = porProtocolo;
        LOG.infof("orquestador inicializado mapeadores=%s protocolos=%s",
            mapeadoresPorInstitucion.keySet(), clientesPorProtocolo.keySet());
    }

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        if (peticion.datos().transacciones().size() > 1) {
            List<RecaudacionResponse> respuestas = new ArrayList<>();
            for (Map<String, Object> transaccion : peticion.datos().transacciones()) {
                try {
                    respuestas.add(procesarUna(peticion.conTransaccion(transaccion)));
                } catch (RuntimeException e) {
                    respuestas.add(errorDeTransaccion(peticion, transaccion, e));
                }
            }
            return combinar(peticion, respuestas);
        }
        return procesarUna(peticion);
    }

    private RecaudacionResponse procesarUna(RecaudacionRequest peticion) {
        Convenio convenio = registro.convenio(peticion.institucion());
        if (convenio == null) {
            throw new InstitucionNoSoportadaException("Institución no soportada: " + peticion.institucion());
        }
        MapeadorOperacion mapeador = mapeadoresPorInstitucion.get(peticion.institucion());
        if (mapeador == null) {
            throw new InstitucionNoSoportadaException("Sin mapeador configurado para " + peticion.institucion());
        }
        ProtocoloCliente cliente = clientesPorProtocolo.get(convenio.protocolo());
        if (cliente == null) {
            throw new InstitucionNoSoportadaException(
                "Sin cliente para el protocolo " + convenio.protocolo() + " de " + peticion.institucion());
        }
        LOG.debugf("orquestando institucion=%s operador=%s protocolo=%s idTransaccion=%s",
            peticion.institucion(), peticion.operador(), convenio.protocolo(),
            peticion.cabecera().idTransaccion());
        PeticionSalida salida = mapeador.construir(peticion);
        RespuestaCruda respuesta = resiliencia.ejecutar(cliente, convenio, salida, peticion);
        try {
            RecaudacionResponse interpretada = mapeador.interpretar(respuesta, peticion);
            return normalizar(conCabecera(interpretada, peticion));
        } catch (ProveedorException excepcion) {
            throw excepcion;
        } catch (RuntimeException excepcion) {
            throw new ProveedorException(
                "Respuesta inválida de " + peticion.institucion() + ": " + excepcion.getMessage(),
                ProveedorException.BAD_GATEWAY, false, excepcion);
        }
    }

    private RecaudacionResponse conCabecera(RecaudacionResponse respuesta, RecaudacionRequest peticion) {
        if (respuesta.cabecera() != null) {
            return respuesta;
        }
        return new RecaudacionResponse(respuesta.institucion(), respuesta.operador(),
            peticion.cabecera(), respuesta.respuesta());
    }

    private RecaudacionResponse normalizar(RecaudacionResponse respuesta) {
        String codigo = catalogo.normalizar(respuesta.codigo());
        String mensaje = respuesta.mensaje();
        if ("00".equals(codigo) && (mensaje == null || mensaje.isBlank())) {
            mensaje = "OK";
        }
        if (codigo.equals(respuesta.codigo())
            && java.util.Objects.equals(mensaje, respuesta.mensaje())) {
            return respuesta;
        }
        return new RecaudacionResponse(respuesta.institucion(), respuesta.operador(),
            respuesta.cabecera(), new RecaudacionResponse.Respuesta(
                codigo, mensaje, respuesta.cliente(), respuesta.transacciones()));
    }

    private RecaudacionResponse errorDeTransaccion(RecaudacionRequest peticion,
                                                   Map<String, Object> transaccion,
                                                   RuntimeException error) {
        int status = error instanceof ProveedorException proveedor ? proveedor.status() : 500;
        String mensaje = error.getMessage() == null ? "Error en la operación" : error.getMessage();
        Map<String, Object> detalle = new LinkedHashMap<>();
        if (transaccion != null) {
            detalle.putAll(transaccion);
        }
        detalle.put("estado", "ERROR");
        detalle.put("glosa", mensaje);
        Map<String, Object> adicionales = new LinkedHashMap<>();
        adicionales.put("codigo", "ER");
        adicionales.put("mensaje", mensaje);
        adicionales.put("httpStatus", status);
        detalle.put("datosAdicionales", adicionales);
        return RecaudacionResponse.de(peticion.institucion(), peticion.operador(),
            peticion.cabecera(), "ER", mensaje, Map.of(), List.of(detalle));
    }

    private RecaudacionResponse combinar(RecaudacionRequest peticion,
                                         List<RecaudacionResponse> respuestas) {
        String codigo = respuestas.stream()
            .map(RecaudacionResponse::codigo)
            .filter(codigoRespuesta -> !"00".equals(codigoRespuesta))
            .findFirst()
            .orElse("00");
        String mensaje = respuestas.stream()
            .filter(respuesta -> !"00".equals(respuesta.codigo()))
            .map(RecaudacionResponse::mensaje)
            .findFirst()
            .orElse("OK");
        Map<String, Object> cliente = respuestas.stream()
            .map(RecaudacionResponse::cliente)
            .filter(datosCliente -> datosCliente != null && !datosCliente.isEmpty())
            .findFirst()
            .orElse(Map.of());

        List<Map<String, Object>> transacciones = new ArrayList<>();
        for (RecaudacionResponse respuesta : respuestas) {
            boolean correcta = "00".equals(respuesta.codigo());
            List<Map<String, Object>> propias = respuesta.transacciones();
            if (propias.isEmpty()) {
                Map<String, Object> detalle = new LinkedHashMap<>();
                detalle.put("estado", correcta ? "OK" : "ERROR");
                detalle.put("glosa", respuesta.mensaje());
                Map<String, Object> adicionales = new LinkedHashMap<>();
                adicionales.put("codigo", respuesta.codigo());
                adicionales.put("mensaje", respuesta.mensaje());
                detalle.put("datosAdicionales", adicionales);
                transacciones.add(detalle);
                continue;
            }
            for (Map<String, Object> original : propias) {
                Map<String, Object> detalle = new LinkedHashMap<>(
                    original == null ? Map.of() : original);
                detalle.putIfAbsent("estado", correcta ? "OK" : "ERROR");
                if (!correcta) {
                    Map<String, Object> adicionales = new LinkedHashMap<>();
                    Object existente = detalle.get("datosAdicionales");
                    if (existente instanceof Map<?, ?> mapa) {
                        mapa.forEach((clave, valor) ->
                            adicionales.put(String.valueOf(clave), valor));
                    }
                    adicionales.putIfAbsent("codigo", respuesta.codigo());
                    adicionales.putIfAbsent("mensaje", respuesta.mensaje());
                    detalle.put("datosAdicionales", adicionales);
                }
                transacciones.add(detalle);
            }
        }
        return RecaudacionResponse.de(peticion.institucion(), peticion.operador(),
            peticion.cabecera(), codigo, mensaje, cliente, transacciones);
    }
}
