package org.canalesCMAC.application;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Datos;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MatrizObligatoriedad {

    private static final Logger LOG = Logger.getLogger(MatrizObligatoriedad.class);
    private static final String PREFIJO = "convenio.";
    private static final String MARCADOR = "requerido";

    private static final Map<Institucion, Map<Operador, List<Set<String>>>> POR_DEFECTO =
        construir();

    private final Map<Institucion, Map<Operador, List<Set<String>>>> matriz;

    public MatrizObligatoriedad() {
        this.matriz = copiar(POR_DEFECTO);
    }

    @Inject
    public MatrizObligatoriedad(Config config) {
        this.matriz = conOverrides(POR_DEFECTO, config);
        LOG.debugf("matriz de obligatoriedad cargada convenios=%d", matriz.size());
    }

    public List<Set<String>> requeridos(Institucion institucion, Operador operador) {
        Map<Operador, List<Set<String>>> porOperador = matriz.get(institucion);
        if (porOperador == null) {
            return List.of();
        }
        return porOperador.getOrDefault(operador, List.of());
    }

    private static Map<Institucion, Map<Operador, List<Set<String>>>> conOverrides(
        Map<Institucion, Map<Operador, List<Set<String>>>> base, Config config) {
        Map<Institucion, Map<Operador, List<Set<String>>>> resultado = copiar(base);
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO)) {
                continue;
            }
            String[] partes = propiedad.split("\\.");
            if (partes.length != 4 || !MARCADOR.equals(partes[2])) {
                continue;
            }
            Institucion institucion = Institucion.desde(partes[1]);
            if (institucion == null) {
                continue;
            }
            Operador operador;
            try {
                operador = Operador.valueOf(partes[3]);
            } catch (IllegalArgumentException e) {
                LOG.warnf("operador inválido en matriz propiedad=%s", propiedad);
                continue;
            }
            String valor = config.getOptionalValue(propiedad, String.class).orElse("");
            List<Set<String>> grupos = grupos(valor);
            if (!grupos.isEmpty()) {
                resultado.computeIfAbsent(institucion, clave -> new EnumMap<>(Operador.class))
                    .put(operador, grupos);
            }
        }
        return resultado;
    }

    private static List<Set<String>> grupos(String valor) {
        List<Set<String>> grupos = new ArrayList<>();
        for (String grupo : valor.split("\\|")) {
            Set<String> campos = new LinkedHashSet<>();
            for (String campo : grupo.split(",")) {
                String limpio = campo.trim();
                if (!limpio.isBlank()) {
                    campos.add(Datos.canonica(limpio));
                }
            }
            if (!campos.isEmpty()) {
                grupos.add(Set.copyOf(campos));
            }
        }
        return List.copyOf(grupos);
    }

    private static Map<Institucion, Map<Operador, List<Set<String>>>> copiar(
        Map<Institucion, Map<Operador, List<Set<String>>>> origen) {
        Map<Institucion, Map<Operador, List<Set<String>>>> copia = new EnumMap<>(Institucion.class);
        origen.forEach((institucion, porOperador) -> {
            Map<Operador, List<Set<String>>> operadores = new EnumMap<>(Operador.class);
            porOperador.forEach((operador, grupos) -> operadores.put(operador, List.copyOf(grupos)));
            copia.put(institucion, operadores);
        });
        return copia;
    }

    private static Map<Institucion, Map<Operador, List<Set<String>>>> construir() {
        return Map.ofEntries(
            Map.entry(Institucion.ELSE, Map.ofEntries(
                Map.entry(Operador.CONSULTA_DEUDA, req(Canonico.DATO_CONSULTA)),
                Map.entry(Operador.PAGO_DEUDA, req(Canonico.IDENTIFICADOR_TRANSACCION,
                    Canonico.NUMERO_COMPROBANTE, Canonico.NUMERO_SUMINISTRO, Canonico.MONTO)),
                Map.entry(Operador.CONSULTA_PAGO, req(Canonico.IDENTIFICADOR_TRANSACCION,
                    Canonico.NUMERO_COMPROBANTE, Canonico.NUMERO_SUMINISTRO, Canonico.MONTO)),
                Map.entry(Operador.EXTORNO_PAGO, req(Canonico.IDENTIFICADOR_TRANSACCION,
                    Canonico.IDENTIFICADOR_PAGO, Canonico.NUMERO_COMPROBANTE,
                    Canonico.NUMERO_SUMINISTRO, Canonico.MONTO))
            )),
            Map.entry(Institucion.ELECTRO_UCAYALI, Map.ofEntries(
                Map.entry(Operador.CONSULTA_DEUDA, req(Canonico.NUMERO_SUMINISTRO,
                    Canonico.TRACE_CONSULTA, Canonico.FECHA_OPERACION)),
                Map.entry(Operador.PAGO_DEUDA, req(Canonico.NUMERO_SUMINISTRO,
                    Canonico.NUMERO_COMPROBANTE, Canonico.MONTO, Canonico.FECHA_OPERACION)),
                Map.entry(Operador.EXTORNO_PAGO, req(Canonico.NUMERO_SUMINISTRO,
                    Canonico.NUMERO_COMPROBANTE, Canonico.MONTO, Canonico.FECHA_OPERACION)),
                Map.entry(Operador.EXTORNO_PAGO_AUTO, req(Canonico.NUMERO_SUMINISTRO,
                    Canonico.NUMERO_COMPROBANTE, Canonico.MONTO, Canonico.FECHA_OPERACION)),
                Map.entry(Operador.EXTORNO_AUTO, req(Canonico.NUMERO_SUMINISTRO,
                    Canonico.NUMERO_COMPROBANTE, Canonico.MONTO, Canonico.FECHA_OPERACION))
            )),
            Map.entry(Institucion.SEAL, Map.ofEntries(
                Map.entry(Operador.CONSULTA_DEUDA, unoDe(Canonico.CONTRATO, Canonico.NUMERO_SUMINISTRO)),
                Map.entry(Operador.PAGO_DEUDA, mas(
                    unoDe(Canonico.CONTRATO, Canonico.NUMERO_SUMINISTRO), req(Canonico.MONTO))),
                Map.entry(Operador.EXTORNO_PAGO, unoDe(Canonico.CONTRATO, Canonico.NUMERO_SUMINISTRO)),
                Map.entry(Operador.ANULACION, mas(
                    unoDe(Canonico.CONTRATO, Canonico.NUMERO_SUMINISTRO), req(Canonico.CODIGO_APROBACION)))
            )),
            Map.entry(Institucion.CLARO, Map.ofEntries(
                Map.entry(Operador.PAGO, req(Canonico.MONTO)),
                Map.entry(Operador.RECARGA, req(Canonico.MONTO)),
                Map.entry(Operador.EXTORNO_PAGO, req(Canonico.MONTO, Canonico.IDENTIFICADOR_TRANSACCION)),
                Map.entry(Operador.EXTORNO_RECARGA, req(Canonico.MONTO, Canonico.IDENTIFICADOR_TRANSACCION)),
                Map.entry(Operador.EXTORNO_ANULACION_PAGO, req(Canonico.MONTO, Canonico.IDENTIFICADOR_TRANSACCION)),
                Map.entry(Operador.EXTORNO_ANULACION_RECARGA, req(Canonico.MONTO, Canonico.IDENTIFICADOR_TRANSACCION)),
                Map.entry(Operador.ANULACION_PAGO, req(Canonico.IDENTIFICADOR_TRANSACCION)),
                Map.entry(Operador.ANULACION_RECARGA, req(Canonico.IDENTIFICADOR_TRANSACCION)),
                Map.entry(Operador.CONSULTA_DEUDA, req(Canonico.NUMERO_SUMINISTRO)),
                Map.entry(Operador.CONSULTA_SALDO, req(Canonico.NUMERO_SUMINISTRO)),
                Map.entry(Operador.ECHO_PAGO, List.of()),
                Map.entry(Operador.ECHO_RECARGA, List.of())
            )),
            Map.entry(Institucion.UNIVCUSCO, Map.ofEntries(
                Map.entry(Operador.CONSULTA_DEUDA, req(Canonico.NUMERO_REFERENCIAL_DEUDA)),
                Map.entry(Operador.CONSULTA_DEUDA_ORDINARIO, req(Canonico.TIPO_DOCUMENTO_CLIENTE,
                    Canonico.DOCUMENTO_CLIENTE, Canonico.CODIGO_CONCEPTO)),
                Map.entry(Operador.NOTIFICACION_PAGO, req(Canonico.NUMERO_REFERENCIAL_DEUDA,
                    Canonico.DOCUMENTO_CLIENTE, Canonico.MONTO, Canonico.NUMERO_OPERACION_RECAUDACION,
                    Canonico.FORMA_PAGO, Canonico.MONEDA)),
                Map.entry(Operador.NOTIFICACION_PAGO_ORDINARIO, req(Canonico.TIPO_DOCUMENTO_CLIENTE,
                    Canonico.DOCUMENTO_CLIENTE, Canonico.NOMBRE_CLIENTE, Canonico.CODIGO_CONCEPTO,
                    Canonico.NUMERO_OPERACION_RECAUDACION, Canonico.CANTIDAD, Canonico.MONTO,
                    Canonico.FORMA_PAGO, Canonico.MONEDA)),
                Map.entry(Operador.AUTO_EXTORNO, req(Canonico.NUMERO_REFERENCIAL_DEUDA,
                    Canonico.DOCUMENTO_CLIENTE, Canonico.MONTO, Canonico.NUMERO_OPERACION_RECAUDACION,
                    Canonico.NUMERO_OPERACION_ORIGINAL, Canonico.FECHA_OPERACION_ORIGINAL,
                    Canonico.FORMA_PAGO, Canonico.MONEDA))
            )),
            Map.entry(Institucion.YAGANASTE, Map.ofEntries(
                Map.entry(Operador.CONSULTA, req(Canonico.TPV, Canonico.CLAVE_TPV,
                    Canonico.CODIGO_PRODUCTO, Canonico.REFERENCIA_OPERACION)),
                Map.entry(Operador.CONSULTA_PREVIA, req(Canonico.TPV, Canonico.CLAVE_TPV,
                    Canonico.CODIGO_PRODUCTO, Canonico.REFERENCIA_OPERACION)),
                Map.entry(Operador.VENTA, req(Canonico.TPV, Canonico.CLAVE_TPV,
                    Canonico.CODIGO_PRODUCTO, Canonico.REFERENCIA_OPERACION)),
                Map.entry(Operador.DEVOLUCION, req(Canonico.TPV, Canonico.CLAVE_TPV,
                    Canonico.CODIGO_PRODUCTO, Canonico.REFERENCIA_OPERACION)),
                Map.entry(Operador.SALDO, req(Canonico.TPV, Canonico.CLAVE_TPV)),
                Map.entry(Operador.ECHO, List.of())
            )),
            Map.entry(Institucion.MUNICIPALIDAD_CUSCO, Map.ofEntries(
                Map.entry(Operador.RECUPERAR_ORDEN_PAGO, req(Canonico.NUMERO_ORDEN_PAGO)),
                Map.entry(Operador.VALIDAR_ORDEN_PAGO, req(Canonico.NUMERO_ORDEN_PAGO,
                    Canonico.CODIGO_CONTRIBUYENTE, Canonico.MONTO)),
                Map.entry(Operador.ACTUALIZAR_ORDEN_PAGO, req(Canonico.NUMERO_ORDEN_PAGO,
                    Canonico.NUM_OPERACION))
            )),
            Map.entry(Institucion.PAYTOPERU, Map.ofEntries(
                Map.entry(Operador.CONSULTA_PAGO, req(Canonico.CODIGO_PAGO, Canonico.DOCUMENTO_CLIENTE)),
                Map.entry(Operador.NOTIFICACION_PAGO, req(Canonico.CODIGO_PAGO, Canonico.CANAL,
                    Canonico.UBICACION_CANAL, Canonico.MONTO, Canonico.MONEDA, Canonico.FECHA_OPERACION)),
                Map.entry(Operador.CONFIRMACION_PAGO, req(Canonico.CODIGO_PAGO)),
                Map.entry(Operador.CONSULTA_TRANSACCIONES, req(Canonico.FECHA_OPERACION))
            )),
            Map.entry(Institucion.WESTERN_UNION, Map.ofEntries(
                Map.entry(Operador.BUSCAR_PRODUCTO, req(Canonico.ID_AGENTE,
                    Canonico.ID_LOCACION, Canonico.CAJA)),
                Map.entry(Operador.CONSULTAR_TRANSACCION, req(Canonico.ID_PRODUCTO,
                    Canonico.PARAMETROS_OPERACION)),
                Map.entry(Operador.CONSULTAR_DATOS_ADICIONALES, req(Canonico.ID_PRODUCTO)),
                Map.entry(Operador.AGREGAR_DATOS_ADICIONALES, req(Canonico.ID_PRODUCTO,
                    Canonico.ID_TRANSACCION, Canonico.DATOS_ADICIONALES)),
                Map.entry(Operador.CONFIRMAR_LOTE, req(Canonico.TRANSACCIONES)),
                Map.entry(Operador.CONFIRMAR_DIRECTO, req(Canonico.ID_PRODUCTO,
                    Canonico.PARAMETROS_OPERACION)),
                Map.entry(Operador.CONSULTAR_LOTE, List.of()),
                Map.entry(Operador.REIMPRIMIR_TRANSACCION, req(Canonico.ID_LOTE)),
                Map.entry(Operador.ANULAR_TRANSACCION, List.of())
            )),
            Map.entry(Institucion.UNIVCONTINENTAL, Map.ofEntries(
                Map.entry(Operador.UNIVERSIDAD_CONSULTA_DEUDA, req(Canonico.CONTRATO)),
                Map.entry(Operador.UNIVERSIDAD_PAGO_DEUDA, req(Canonico.CONTRATO,
                    Canonico.NUMERO_TRACE, Canonico.FECHA_OPERACION, Canonico.DATOS)),
                Map.entry(Operador.UNIVERSIDAD_EXTORNO_DEUDA, req(Canonico.CONTRATO,
                    Canonico.NUMERO_TRACE, Canonico.FECHA_OPERACION)),
                Map.entry(Operador.CENTRO_IDIOMAS_CONSULTA_DEUDA, req(Canonico.CONTRATO)),
                Map.entry(Operador.CENTRO_IDIOMAS_PAGO_DEUDA, req(Canonico.CONTRATO,
                    Canonico.NUMERO_TRACE, Canonico.FECHA_OPERACION, Canonico.DATOS)),
                Map.entry(Operador.CENTRO_IDIOMAS_EXTORNO_DEUDA, req(Canonico.CONTRATO,
                    Canonico.NUMERO_TRACE, Canonico.FECHA_OPERACION))
            )),

            Map.entry(Institucion.UNSAAC, Map.ofEntries(
                Map.entry(Operador.CONSULTA_DEUDA, req(Canonico.NUMERO_OPERACION)),
                Map.entry(Operador.PAGO_DEUDA, mas(req(Canonico.NUMERO_OPERACION,
                    Canonico.MONTO, Canonico.FECHA_OPERACION, Canonico.NUMERO_TARJETA,
                    Canonico.CODIGO_APROBACION, Canonico.NUMERO_PEDIDO,
                    Canonico.FORMA_PAGO), unoDe(Canonico.NOMBRE_TARJETA_HABIENTE,
                    Canonico.NOMBRE_CLIENTE))),
                Map.entry(Operador.VERIFICAR_PAGO, req(Canonico.TRANSACCIONES)),
                Map.entry(Operador.CONFIRMAR_PAGO, req(Canonico.NUMERO_OPERACION,
                    Canonico.IDENTIFICADOR_PAGO, Canonico.EXITO))
            ))
        );
    }

    private static List<Set<String>> req(String... campos) {
        List<Set<String>> grupos = new ArrayList<>();
        for (String campo : campos) {
            grupos.add(Set.of(campo));
        }
        return List.copyOf(grupos);
    }

    private static List<Set<String>> unoDe(String... campos) {
        return List.of(Set.of(campos));
    }

    private static List<Set<String>> mas(List<Set<String>> base, List<Set<String>> extra) {
        List<Set<String>> grupos = new ArrayList<>(base);
        grupos.addAll(extra);
        return List.copyOf(grupos);
    }
}
