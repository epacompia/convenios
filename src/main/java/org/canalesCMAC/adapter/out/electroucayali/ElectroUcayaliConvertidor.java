package org.canalesCMAC.adapter.out.electroucayali;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.canalesCMAC.application.CatalogoCodigos;
import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Mapas;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ElectroUcayaliConvertidor implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(ElectroUcayaliConvertidor.class);
    private static final String CONTENT_TYPE = "application/json";
    private static final DateTimeFormatter FECHA_COMPACTA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HORA_COMPACTA = DateTimeFormatter.ofPattern("HHmmss");

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CatalogoCodigos catalogo;

    @ConfigProperty(name = "recaudacion.electroucayali.codempresa", defaultValue = "05")
    String codEmpresa;

    @Override
    public Institucion institucion() {
        return Institucion.ELECTRO_UCAYALI;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        OperacionEu operacion = OperacionEu.de(peticion.operador());
        LOG.debugf("construyendo REST Electro Ucayali operador=%s ruta=%s", peticion.operador(), operacion.ruta());
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        operacion.cuerpo(cuerpo, peticion, codEmpresa);
        return new PeticionSalida(jsonDe(cuerpo), operacion.ruta(), CONTENT_TYPE);
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        try {
            JsonNode nodo = objectMapper.readTree(respuesta.cuerpo());
            String codigo = nodo.path("codigo").asText("99");
            String mensaje = catalogo.mensaje(Institucion.ELECTRO_UCAYALI, codigo, nodo.path("mensaje").asText(""));
            Map<String, Object> cliente = Mapas.nuevo();
            Mapas.poner(cliente, Canonico.NUMERO_SUMINISTRO, texto(nodo, "nroSumin"));
            Mapas.poner(cliente, Canonico.NOMBRE_CLIENTE, texto(nodo, "nombreCliente"));
            Map<String, Object> transaccion = Mapas.nuevo();
            Mapas.poner(transaccion, Canonico.NUMERO_OPERACION, texto(nodo, "numOperacionEmpresa"));
            if (nodo.hasNonNull("lstdebt")) {
                List<Map<String, Object>> deudas = new ArrayList<>();
                for (JsonNode deuda : nodo.get("lstdebt")) {
                    Map<String, Object> item = Mapas.nuevo();
                    Mapas.poner(item, Canonico.NUMERO_COMPROBANTE, texto(deuda, "numFactura"));
                    if (deuda.hasNonNull("montoDeuda")) {
                        item.put(Canonico.MONTO, deuda.get("montoDeuda").decimalValue());
                    }
                    Mapas.poner(item, Canonico.FECHA_EMISION, texto(deuda, "fechaEmision"));
                    Mapas.poner(item, Canonico.FECHA_VENCIMIENTO, texto(deuda, "fechaVencimiento"));
                    Mapas.poner(item, Canonico.GLOSA, texto(deuda, "glosa"));
                    Mapas.poner(item, Canonico.TIPO_INTEGRACION, texto(deuda, "tipoIntegracion"));
                    deudas.add(item);
                }
                transaccion.put(Canonico.DEUDAS, deudas);
            }
            LOG.debugf("REST Electro Ucayali interpretado operador=%s codigo=%s", peticion.operador(), codigo);
            return RecaudacionResponse.de(Institucion.ELECTRO_UCAYALI, peticion.operador(),
                peticion.cabecera(), codigo, mensaje, cliente, List.of(transaccion));
        } catch (Exception e) {
            throw new IllegalArgumentException("Respuesta JSON no válida: " + e.getMessage(), e);
        }
    }

    private String jsonDe(Map<String, Object> cuerpo) {
        try {
            return objectMapper.writeValueAsString(cuerpo);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar la solicitud JSON", e);
        }
    }

    private static LocalDateTime fechaOperacion(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.FECHA_OPERACION);
        if (valor != null && !valor.toString().isBlank()) {
            return LocalDateTime.parse(valor.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    private static String dato(RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + clave);
        }
        return valor.toString();
    }

    private static BigDecimal monto(RecaudacionRequest peticion) {
        Object monto = peticion.datos().get(Canonico.MONTO);
        if (monto == null) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: monto");
        }
        return new BigDecimal(monto.toString()).setScale(2);
    }

    private static void extraerSiExiste(JsonNode nodo, Map<String, Object> destino, String origen, String destinoClave) {
        if (nodo.hasNonNull(origen) && !nodo.get(origen).asText().isBlank()) {
            destino.put(destinoClave, nodo.get(origen).asText());
        }
    }

    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()
            ? nodo.get(campo).asText().trim()
            : null;
    }

    private enum OperacionEu {
        CONSULTA_DEUDA {
            @Override
            String ruta() {
                return "/api/v2/electro/consulta";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("traceConsulta", p.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                c.put("fechaConsulta", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaConsulta", HORA_COMPACTA.format(fechaOperacion(p)));
                comunes(c, codEmpresa, p);
            }
        },
        PAGO_DEUDA {
            @Override
            String ruta() {
                return "/api/v1/electro/pago";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("traceConsulta", p.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa, p);
            }
        },
        EXTORNO_PAGO {
            @Override
            String ruta() {
                return "/api/v1/electro/extorno";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("traceConsulta", p.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa, p);
            }
        },
        EXTORNO_PAGO_AUTO {
            @Override
            String ruta() {
                return "/api/v1/electro/extorno_automatico_pago";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa, p);
            }
        },
        EXTORNO_AUTO {
            @Override
            String ruta() {
                return "/api/v1/electro/extorno_automatico";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("tracePago", p.datos().getOrDefault(Canonico.IDENTIFICADOR_PAGO, "").toString());
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa, p);
            }
        };

        abstract String ruta();

        abstract void cuerpo(Map<String, Object> cuerpo, RecaudacionRequest peticion, String codEmpresa);

        static OperacionEu de(Operador operador) {
            for (OperacionEu operacion : values()) {
                if (operacion.name().equals(operador.name())) {
                    return operacion;
                }
            }
            throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por Electro Ucayali");
        }

        private static void comunes(Map<String, Object> c, String codEmpresa, RecaudacionRequest p) {
            c.put("codEmpresa", codEmpresa);
            c.put("codServicio", p.datos().getOrDefault("codServicio", "0").toString());
            c.put("codAgencia", p.datos().getOrDefault(Canonico.AGENCIA, "0").toString());
            c.put("codCanal", p.datos().getOrDefault(Canonico.CANAL, "00").toString());
            c.put("terminal", p.datos().getOrDefault("terminal", "0").toString());
        }
    }
}
