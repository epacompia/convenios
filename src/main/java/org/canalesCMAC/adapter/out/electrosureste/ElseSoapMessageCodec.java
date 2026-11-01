package org.canalesCMAC.adapter.out.electrosureste;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.canalesCMAC.adapter.out.soap.NodoXml;
import org.canalesCMAC.adapter.out.soap.SoapSupport;
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
public class ElseSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(ElseSoapMessageCodec.class);
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final DateTimeFormatter FECHA_SOAP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @ConfigProperty(name = "recaudacion.electrosureste.usuario")
    String usuario;

    @ConfigProperty(name = "recaudacion.electrosureste.clave")
    String clave;

    @ConfigProperty(name = "recaudacion.electrosureste.soap.namespace", defaultValue = "http://tempuri.org/")
    String namespace;

    @Inject
    CatalogoCodigos catalogo;

    @Override
    public Institucion institucion() {
        return Institucion.ELSE;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        OperacionElse operacion = OperacionElse.de(peticion.operador());
        LOG.debugf("construyendo SOAP ELSE operador=%s soap=%s", peticion.operador(), operacion.soap());
        Map<String, String> campos = new LinkedHashMap<>();
        operacion.campos(campos, peticion, usuario, clave);
        return new PeticionSalida(SoapSupport.sobre(namespace, operacion.soap(), campos), CONTENT_TYPE);
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        OperacionElse operacion = OperacionElse.de(peticion.operador());
        NodoXml raiz = SoapSupport.cuerpo(respuesta.cuerpo());
        String codigo = mapeoCodigo(operacion, raiz);
        String mensaje = catalogo.mensaje(Institucion.ELSE, codigo, mapeoMensaje(operacion, raiz));
        Map<String, Object> cliente = Mapas.nuevo();
        List<Map<String, Object>> transaccion = List.of();
        if ("00".equals(codigo)) {
            Map<String, Object> datos = mapearDatos(operacion, raiz);
            cliente = extraerCliente(datos);
            transaccion = List.of(datos);
        }
        LOG.debugf("SOAP ELSE interpretado operador=%s codigo=%s", peticion.operador(), codigo);
        return RecaudacionResponse.de(Institucion.ELSE, peticion.operador(), peticion.cabecera(),
            codigo, mensaje, cliente, transaccion);
    }

    private String mapeoCodigo(OperacionElse operacion, NodoXml raiz) {
        String codigo = raiz.textoDe(operacion.campoCodigo());
        return (codigo == null || codigo.isBlank()) ? "99" : ("0".equals(codigo.trim()) ? "00" : codigo.trim());
    }

    private String mapeoMensaje(OperacionElse operacion, NodoXml raiz) {
        String mensaje = raiz.textoDe(operacion.campoMensaje());
        return mensaje == null ? "" : mensaje.trim();
    }

    private Map<String, Object> mapearDatos(OperacionElse operacion, NodoXml raiz) {
        Map<String, Object> datos = new LinkedHashMap<>();
        operacion.datos(datos, raiz);
        return datos;
    }

    private static Map<String, Object> extraerCliente(Map<String, Object> datos) {
        Map<String, Object> cliente = Mapas.nuevo();
        for (String clave : Set.of(Canonico.NOMBRE_CLIENTE, Canonico.DIRECCION_CLIENTE,
            Canonico.DISTRITO_CLIENTE, Canonico.DOCUMENTO_CLIENTE, Canonico.NUMERO_SUMINISTRO)) {
            if (datos.containsKey(clave)) {
                cliente.put(clave, datos.remove(clave));
            }
        }
        return cliente;
    }

    private static String texto(NodoXml raiz, String nombre) {
        return raiz.textoDe(nombre);
    }

    private static void poner(Map<String, Object> destino, String clave, Object valor) {
        if (valor != null) {
            destino.put(clave, valor);
        }
    }

    private static String datoObligatorio(RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + clave);
        }
        return valor.toString();
    }

    private static String monto(RecaudacionRequest peticion) {
        Object monto = peticion.datos().get(Canonico.MONTO);
        if (monto == null) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: monto");
        }
        return new BigDecimal(monto.toString()).setScale(2).toPlainString();
    }

    private static BigDecimal decimal(String valor) {
        return valor == null || valor.isBlank() ? null : new BigDecimal(valor);
    }

    private static String ahoraSoap() {
        return LocalDateTime.now().format(FECHA_SOAP);
    }

    private enum OperacionElse {
        CONSULTA_DEUDA {
            @Override
            String soap() {
                return "ConsultaDeuda";
            }

            @Override
            String campoCodigo() {
                return "CodigoMensajeRetornoConsulta";
            }

            @Override
            String campoMensaje() {
                return "MensajeRetornoConsulta";
            }

            @Override
            void campos(Map<String, String> p, RecaudacionRequest r, String usuario, String clave) {
                p.put("IdentificadorEntidadConsulta", UUID.randomUUID().toString());
                p.put("AgenciaEntidadConsulta", r.datos().getOrDefault(Canonico.AGENCIA, "Agencia Principal").toString());
                p.put("MedioPagoEntidadConsulta", r.datos().getOrDefault(Canonico.MEDIO_PAGO, "Ventanilla").toString());
                p.put("FechaConsulta", ahoraSoap());
                p.put("UsuarioConsulta", usuario);
                p.put("ClaveUsuario", clave);
                p.put("TipoConsulta", r.datos().getOrDefault(Canonico.TIPO_CONSULTA, "1").toString());
                p.put("DatoConsulta", datoObligatorio(r, Canonico.DATO_CONSULTA));
            }

            @Override
            void datos(Map<String, Object> d, NodoXml raiz) {
                poner(d, Canonico.IDENTIFICADOR_TRANSACCION, texto(raiz, "IdentificadorTransaccion"));
                poner(d, Canonico.NUMERO_COMPROBANTE, texto(raiz, "CodigoComprobante"));
                poner(d, Canonico.NUMERO_SUMINISTRO, texto(raiz, "CodigoSuministro"));
                poner(d, Canonico.NOMBRE_CLIENTE, texto(raiz, "NombreCliente"));
                poner(d, Canonico.DISTRITO_CLIENTE, texto(raiz, "DistritoCliente"));
                poner(d, Canonico.DIRECCION_CLIENTE, texto(raiz, "DireccionCliente"));
                poner(d, Canonico.DOCUMENTO_CLIENTE, texto(raiz, "NumeroDocumento"));
                poner(d, Canonico.MONTO, decimal(texto(raiz, "MontoAPagarConsulta")));
                poner(d, Canonico.FECHA_EMISION, texto(raiz, "FechaEmision"));
                poner(d, Canonico.FECHA_VENCIMIENTO, texto(raiz, "FechaVencimiento"));
                poner(d, Canonico.FECHA_VENCIMIENTO_RSP, texto(raiz, "FechaVencimientoRSP"));
                poner(d, Canonico.FECHA_REGISTRO, texto(raiz, "FechaRegistroConsulta"));
                poner(d, "detalleConsulta", texto(raiz, "DetalleConsulta"));
            }
        },
        PAGO_DEUDA {
            @Override
            String soap() {
                return "PagoDeuda";
            }

            @Override
            String campoCodigo() {
                return "CodigoMensajeRetornoPago";
            }

            @Override
            String campoMensaje() {
                return "MensajeRetornoPago";
            }

            @Override
            void campos(Map<String, String> p, RecaudacionRequest r, String usuario, String clave) {
                camposPago(p, r, usuario, clave);
            }

            @Override
            void datos(Map<String, Object> d, NodoXml raiz) {
                datosPago(d, raiz);
            }
        },
        CONSULTA_PAGO {
            @Override
            String soap() {
                return "ConsultaPago";
            }

            @Override
            String campoCodigo() {
                return "CodigoMensajeRetornoConsultaPago";
            }

            @Override
            String campoMensaje() {
                return "MensajeRetornoConsultaPago";
            }

            @Override
            void campos(Map<String, String> p, RecaudacionRequest r, String usuario, String clave) {
                camposPago(p, r, usuario, clave);
            }

            @Override
            void datos(Map<String, Object> d, NodoXml raiz) {
                datosPago(d, raiz);
            }
        },
        EXTORNO_PAGO {
            @Override
            String soap() {
                return "ExtornoPago";
            }

            @Override
            String campoCodigo() {
                return "CodigoMensajeRetornoExtorno";
            }

            @Override
            String campoMensaje() {
                return "MensajeRetornoExtorno";
            }

            @Override
            void campos(Map<String, String> p, RecaudacionRequest r, String usuario, String clave) {
                p.put("IdentificadorTransaccion", datoObligatorio(r, Canonico.IDENTIFICADOR_TRANSACCION));
                p.put("CodigoIdentificadorPago", datoObligatorio(r, Canonico.IDENTIFICADOR_PAGO));
                p.put("IdentificadorEntidadExtorno", UUID.randomUUID().toString());
                p.put("CodigoMotivoExtorno", r.datos().getOrDefault(Canonico.MOTIVO_EXSTORNO, "1").toString());
                p.put("AgenciaEntidadExtorno", r.datos().getOrDefault(Canonico.AGENCIA, "Agencia Principal").toString());
                p.put("MedioPagoEntidadExtorno", r.datos().getOrDefault(Canonico.MEDIO_PAGO, "Ventanilla").toString());
                p.put("FechaExtornoEntidad", ahoraSoap());
                p.put("UsuarioExtorno", usuario);
                p.put("ClaveUsuario", clave);
                p.put("CodigoComprobante", datoObligatorio(r, Canonico.NUMERO_COMPROBANTE));
                p.put("CodigoSuministro", datoObligatorio(r, Canonico.NUMERO_SUMINISTRO));
                p.put("MontoExtorno", monto(r));
            }

            @Override
            void datos(Map<String, Object> d, NodoXml raiz) {
                poner(d, Canonico.IDENTIFICADOR_TRANSACCION, texto(raiz, "IdentificadorTransaccion"));
                poner(d, Canonico.IDENTIFICADOR_PAGO, texto(raiz, "CodigoIdentificadorExtorno"));
                poner(d, Canonico.NUMERO_COMPROBANTE, texto(raiz, "CodigoComprobante"));
                poner(d, Canonico.NUMERO_SUMINISTRO, texto(raiz, "CodigoSuministro"));
                poner(d, Canonico.MONTO, decimal(texto(raiz, "MontoExtorno")));
                poner(d, Canonico.FECHA_REGISTRO, texto(raiz, "FechaRegistroExtorno"));
            }
        };

        abstract String soap();

        abstract String campoCodigo();

        abstract String campoMensaje();

        abstract void campos(Map<String, String> parametros, RecaudacionRequest peticion, String usuario, String clave);

        abstract void datos(Map<String, Object> datos, NodoXml raiz);

        static OperacionElse de(Operador operador) {
            for (OperacionElse operacion : values()) {
                if (operacion.name().equals(operador.name())) {
                    return operacion;
                }
            }
            throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por ELSE");
        }

        private static void camposPago(Map<String, String> p, RecaudacionRequest r, String usuario, String clave) {
            p.put("IdentificadorTransaccion", datoObligatorio(r, Canonico.IDENTIFICADOR_TRANSACCION));
            p.put("IdentificadorEntidadPago", UUID.randomUUID().toString());
            p.put("AgenciaEntidadPago", r.datos().getOrDefault(Canonico.AGENCIA, "Agencia Principal").toString());
            p.put("MedioPagoEntidadPago", r.datos().getOrDefault(Canonico.MEDIO_PAGO, "Ventanilla").toString());
            p.put("FechaPago", ahoraSoap());
            p.put("UsuarioPago", usuario);
            p.put("ClaveUsuario", clave);
            p.put("CodigoComprobante", datoObligatorio(r, Canonico.NUMERO_COMPROBANTE));
            p.put("CodigoSuministro", datoObligatorio(r, Canonico.NUMERO_SUMINISTRO));
            p.put("MontoPago", monto(r));
        }

        private static void datosPago(Map<String, Object> d, NodoXml raiz) {
            poner(d, Canonico.IDENTIFICADOR_TRANSACCION, texto(raiz, "IdentificadorTransaccion"));
            poner(d, Canonico.IDENTIFICADOR_PAGO, texto(raiz, "CodigoIdentificadorPago"));
            poner(d, Canonico.NUMERO_COMPROBANTE, texto(raiz, "CodigoComprobante"));
            poner(d, Canonico.NUMERO_SUMINISTRO, texto(raiz, "CodigoSuministro"));
            poner(d, Canonico.MONTO, decimal(texto(raiz, "MontoPago")));
            poner(d, Canonico.FECHA_REGISTRO, texto(raiz, "FechaRegistroPago"));
        }
    }
}
