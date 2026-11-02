package org.canalesCMAC.domain.model;

/**
 * Vocabulario único del contrato de entrada/salida.
 *
 * <p>Todos los mapeadores deben leer y escribir estos nombres, sin importar
 * el nombre que use el proveedor. El mapeo al campo del proveedor ocurre
 * únicamente dentro de cada {@code MapeadorOperacion}.</p>
 */
public final class Canonico {

    public static final String ID_SESION = "idSesion";
    public static final String COD_AGENCIA = "codAgencia";
    public static final String COD_USUARIO = "codUsuario";
    public static final String IP = "ip";
    public static final String FECHA = "fecha";
    public static final String ID_CANAL = "idCanal";
    public static final String ID_APLICACION = "idAplicacion";

    public static final String NUMERO_SUMINISTRO = "numeroSuministro";
    public static final String NUMERO_COMPROBANTE = "numeroComprobante";
    public static final String CONTRATO = "contrato";
    public static final String NUMERO_ORDEN_PAGO = "numeroOrdenPago";
    public static final String NUMERO_REFERENCIAL_DEUDA = "numeroReferencialDeuda";
    public static final String NUMERO_OPERACION_RECAUDACION = "numeroOperacionRecaudacion";
    public static final String CODIGO_PAGO = "codigoPago";
    public static final String CODIGO_PRODUCTO = "codigoProducto";
    public static final String CODIGO_OPERACION = "codigoOperacion";
    public static final String CODIGO_BANCO = "codigoBanco";
    public static final String CODIGO_CONVENIO = "codigoConvenio";
    public static final String CODIGO_OFICINA = "codigoOficina";
    public static final String CODIGO_CONCEPTO = "codigoConcepto";
    public static final String TIPO_CONSULTA = "tipoConsulta";
    public static final String DATO_CONSULTA = "datoConsulta";
    public static final String TRACE_CONSULTA = "traceConsulta";

    public static final String TIPO_DOCUMENTO_CLIENTE = "tipoDocumentoCliente";
    public static final String DOCUMENTO_CLIENTE = "numeroDocumento";
    public static final String NOMBRE_CLIENTE = "nombreCliente";
    public static final String CODIGO_CONTRIBUYENTE = "codigoContribuyente";
    public static final String CODIGO_CLIENTE = "codigoCliente";
    public static final String DIRECCION_CLIENTE = "direccionCliente";
    public static final String DISTRITO_CLIENTE = "distritoCliente";
    public static final String EMAIL = "email";

    public static final String IDENTIFICADOR_TRANSACCION = "identificadorTransaccion";
    public static final String IDENTIFICADOR_PAGO = "identificadorPago";
    public static final String NUM_OPERACION = "numeroOperacion";
    public static final String NUMERO_OPERACION = "numeroOperacion";
    public static final String NUMERO_TRACE = "numeroTrace";
    public static final String NUMERO_PEDIDO = "numeroPedido";
    public static final String NUMERO_TARJETA = "numeroTarjeta";
    public static final String NOMBRE_TARJETA_HABIENTE = "nombreTarjetaHabiente";
    public static final String PROVEEDOR_NOMBRE = "proveedorNombre";
    public static final String EXITO = "exito";
    public static final String NUMERO_OPERACION_ORIGINAL = "numeroOperacionOriginal";
    public static final String MONTO = "monto";
    public static final String FECHA_OPERACION = "fechaOperacion";
    public static final String HORA_OPERACION = "horaOperacion";
    public static final String FECHA_OPERACION_ORIGINAL = "fechaOperacionOriginal";
    public static final String TIPO_PAGO = "tipoPago";
    public static final String CODIGO_APROBACION = "codigoAprobacion";
    public static final String MOTIVO_EXSTORNO = "motivoExtorno";
    public static final String MOTIVO_EXTORNO = "motivoExtorno";
    public static final String REFERENCIA_OPERACION = "referenciaOperacion";
    public static final String PARAMETROS_OPERACION = "parametrosOperacion";
    public static final String AGENCIA = "agencia";
    public static final String MEDIO_PAGO = "medioPago";
    public static final String FORMA_PAGO = "formaPago";
    public static final String MONEDA = "moneda";
    public static final String CANAL = "canal";
    public static final String UBICACION_CANAL = "ubicacionCanal";
    public static final String TPV = "tpv";
    public static final String CLAVE_TPV = "claveTpv";
    public static final String ID_AGENTE = "idAgente";
    public static final String ID_LOCACION = "idLocacion";
    public static final String CAJA = "caja";
    public static final String ID_PRODUCTO = "idProducto";
    public static final String ID_TRANSACCION = "idTransaccion";
    public static final String ID_LOTE = "idLote";
    public static final String CANTIDAD = "cantidad";
    public static final String SERVICE_CHARGE = "serviceCharge";
    public static final String DATOS_ADICIONALES = "datosAdicionales";
    public static final String TRANSACCIONES = "transacciones";
    public static final String DATOS = "datos";
    public static final String DATOS_EMPRESA = "datosEmpresa";
    public static final String OTROS_DATOS_EMPRESA = "otrosDatosEmpresa";

    public static final String CODIGO_RESPUESTA = "codigoRespuesta";
    public static final String ESTADO = "estado";
    public static final String GLOSA = "glosa";
    public static final String SALDO = "saldo";
    public static final String FEE = "fee";
    public static final String IMPORTE_ORIGEN = "importeOrigen";
    public static final String MONEDA_ORIGEN = "monedaOrigen";
    public static final String REFERENCIA_PROVEEDOR = "referenciaProveedor";
    public static final String REFERENCIA_OPERADOR = "referenciaOperador";
    public static final String NUMERO_LOTE = "numeroLote";
    public static final String CLAVE = "clave";
    public static final String OBSERVACION = "observacion";
    public static final String DEUDAS = "deudas";
    public static final String PARAMETROS = "parametros";
    public static final String IND_MAS_DEUDA = "indMasDeuda";
    public static final String CANTIDAD_DOCS_DEUDA = "cantidadDocsDeuda";
    public static final String TIPO_INTEGRACION = "tipoIntegracion";
    public static final String DETALLE_CONSULTA = "detalleConsulta";
    public static final String FECHA_REGISTRO = "fechaRegistro";
    public static final String FECHA_EMISION = "fechaEmision";
    public static final String FECHA_VENCIMIENTO = "fechaVencimiento";
    public static final String FECHA_VENCIMIENTO_RSP = "fechaVencimientoRsp";
    public static final String DESCRIPCION = "descripcion";
    public static final String IMPORTE_DEUDA_MINIMA = "importeDeudaMinima";
    public static final String PERIODO = "periodo";
    public static final String INDICADOR_RESTRICCION_PAGO = "indicadorRestriccPago";
    public static final String CANTIDAD_SUBCONCEPTOS = "cantidadSubconceptos";

    private Canonico() {
    }
}
