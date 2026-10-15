# Propuesta — Arquitectura de Request Único

> **Estado:** propuesta (no se ha tocado código).
> **Alcance:** unificar **un solo request** para los **10 convenios**: ELSE, ELECTRO_UCAYALI, SEAL, CLARO, UNIVCUSCO, YAGANASTE, MUNICIPALIDAD_CUSCO, PAYTOPERU, WESTERN_UNION y UNIVCONTINENTAL (Universidad + Centro de Idiomas).
> **Objetivo:** que exista **un único contrato de entrada** (mismo request para todos los convenios y todas las operaciones), con un objeto `datos` **organizado en grupos** (`filtro`, `cliente`, `transaccion`) según la idea de Hildebrando, donde cada campo es **obligatorio u opcional** según el convenio y la operación.
> **Validado contra** los instructivos de `docs/` (uno por convenio) y el código actual.

---

## 0. Estructura general

El request mantiene su sobre (`institucion` + `operador`) y el objeto `datos` se **agrupa** en tres llaves:

```
{
  "institucion": "CLARO",          // obligatorio
  "operador": "ECHO_RECARGA",      // obligatorio
  "datos": {
    "filtro":      { ... },        // criterios de consulta / identificación del servicio
    "cliente":     { ... },        // identificación del cliente
    "transaccion": [ { ... } ]     // una o más operaciones (pago, extorno, anulación, recarga...)
  }
}
```

- **`filtro`**: datos para **buscar/identificar** el servicio o la deuda (consultas).
- **`cliente`**: datos del **cliente**.
- **`transaccion`**: arreglo con los datos de la **operación** (puede ser uno o varios).
- Los nombres de los campos **no cambian**; solo se **agrupan**.

---

## 1. El request único (completo)

```json
{
  "institucion": "SEAL",
  "operador": "ANULACION",
  "datos": {
    "filtro": {
      "numeroSuministro": null,
      "numeroComprobante": null,
      "contrato": "98787",
      "numeroOrdenPago": null,
      "numeroReferencialDeuda": null,
      "numeroOperacionRecaudacion": null,
      "codigoPago": null,
      "codigoProducto": null,
      "codigoOperacion": null,
      "codigoBanco": null,
      "codigoConvenio": null,
      "codigoOficina": null,
      "codigoConcepto": null,
      "tipoConsulta": null,
      "datoConsulta": null,
      "traceConsulta": null
    },
    "cliente": {
      "numeroDocumento": null,
      "tipoDocumentoCliente": null,
      "nombreCliente": null,
      "codigoContribuyente": null
    },
    "transaccion": [
      {
        "identificadorTransaccion": null,
        "identificadorPago": null,
        "numeroOperacion": null,
        "numeroTrace": null,
        "numeroOperacionOriginal": null,
        "monto": null,
        "fechaOperacion": null,
        "horaOperacion": null,
        "fechaOperacionOriginal": null,
        "tipoPago": null,
        "codigoAprobacion": "00001",
        "motivoExtorno": null,
        "referenciaOperacion": null,
        "parametrosOperacion": null,
        "agencia": null,
        "medioPago": null,
        "formaPago": null,
        "moneda": null,
        "canal": null,
        "ubicacionCanal": null,
        "tpv": null,
        "claveTpv": null,
        "idAgente": null,
        "idLocacion": null,
        "caja": null,
        "idProducto": null,
        "idTransaccion": null,
        "idLote": null,
        "cantidad": null,
        "serviceCharge": null,
        "datosAdicionales": null,
        "transacciones": null,
        "datos": null,
        "datosEmpresa": null,
        "otrosDatosEmpresa": null,
        "binAdquiriente": null,
        "binAdquirienteReenvia": null,
        "codigoMoneda": null,
        "codigoMonedaPago": null,
        "codigoFormato": null,
        "codigoMedioPago": null,
        "cuentaDestino": null,
        "cuentaOrigen": null,
        "fechaCaptura": null,
        "fechaTransaccion": null,
        "fechaExpiracionTarjeta": null,
        "mac": null,
        "nombreComercio": null,
        "numeroComercio": null,
        "numeroReferencia": null,
        "numeroTerminal": null,
        "numeroTarjeta": null,
        "pinblock": null,
        "track2Data": null,
        "acreedor": null,
        "ciudad": null,
        "datosTransaccion": null,
        "numeroIdentificacionDeudor": null,
        "numeroProductosPagados": null,
        "pagoTotal": null,
        "plaza": null,
        "procesador": null,
        "tipoCambio": null,
        "tipoIdentificacionDeudor": null
      }
    ]
  }
}
```

Reglas del contrato:
- `institucion` y `operador` son **obligatorios** (enum).
- `datos` es **siempre el mismo objeto** agrupado en `filtro`, `cliente` y `transaccion`; solo se envían los campos que aplican.
- La **obligatoriedad** de cada campo se determina por **convenio + operación** (sección 3).
- Los campos que no aplican al operador se **ignoran**.

---

## 2. Diccionario de campos (`datos`)

### 2.1 `datos.filtro` — criterios de consulta / identificación
| Campo | Tipo | Descripción | Convenios |
|---|---|---|---|
| `numeroSuministro` | string | Número de suministro/servicio. | ELSE, ELECTRO_UCAYALI, SEAL, CLARO |
| `numeroComprobante` | string | Número de recibo/factura. | ELSE, ELECTRO_UCAYALI |
| `contrato` | string | Código de contrato del cliente. | SEAL, UNIVCONTINENTAL |
| `numeroOrdenPago` | string | Número de orden de pago. | MUNICIPALIDAD_CUSCO |
| `numeroReferencialDeuda` | string | Número referencial de deuda. | UNIVCUSCO |
| `numeroOperacionRecaudacion` | string | Número de operación de recaudación. | UNIVCUSCO |
| `codigoPago` | string | Código de pago. | PAYTOPERU |
| `codigoProducto` | string | Código de producto. | YAGANASTE |
| `codigoOperacion` | string | Código de operación. | UNIVCUSCO |
| `codigoBanco` | string | Código de banco. | UNIVCUSCO |
| `codigoConvenio` | string | Código de convenio. | UNIVCUSCO |
| `codigoOficina` | string | Código de oficina. | UNIVCUSCO |
| `codigoConcepto` | string | Código de concepto. | UNIVCUSCO |
| `tipoConsulta` | string | Tipo de consulta. | ELSE |
| `datoConsulta` | string | Dato a consultar. | ELSE |
| `traceConsulta` | string | Referencia de la consulta previa. | ELECTRO_UCAYALI |

### 2.2 `datos.cliente` — identificación del cliente
| Campo | Tipo | Descripción | Convenios |
|---|---|---|---|
| `numeroDocumento` | string | Documento del cliente. | PAYTOPERU, UNIVCUSCO |
| `tipoDocumentoCliente` | string | Tipo de documento del cliente. | UNIVCUSCO |
| `nombreCliente` | string | Nombre del cliente. | UNIVCUSCO |
| `codigoContribuyente` | string | Código de contribuyente. | MUNICIPALIDAD_CUSCO |

### 2.3 `datos.transaccion[]` — datos de la operación
| Campo | Tipo | Descripción | Convenios |
|---|---|---|---|
| `identificadorTransaccion` | string | Identificador único de la transacción (trace). | ELSE, SEAL, CLARO |
| `identificadorPago` | string | Identificador del pago a extornar/anular. | ELSE, ELECTRO_UCAYALI |
| `numeroOperacion` | string | Número de operación. | UNIVCUSCO, MUNICIPALIDAD_CUSCO |
| `numeroTrace` | string | Número de trace de la operación. | UNIVCONTINENTAL |
| `numeroOperacionOriginal` | string | Número de operación original (para extorno). | UNIVCUSCO |
| `monto` | number | Importe a pagar o extornar. | ELSE, ELECTRO_UCAYALI, SEAL, CLARO, PAYTOPERU, UNIVCUSCO, MUNICIPALIDAD_CUSCO |
| `fechaOperacion` | string (date-time) | Fecha/hora de la operación. | ELECTRO_UCAYALI, SEAL, CLARO, UNIVCUSCO, YAGANASTE, PAYTOPERU, UNIVCONTINENTAL |
| `horaOperacion` | string | Hora de la operación. | UNIVCUSCO |
| `fechaOperacionOriginal` | string (date-time) | Fecha de la operación original (extorno). | UNIVCUSCO |
| `tipoPago` | string | Tipo de pago. | SEAL |
| `codigoAprobacion` | string | Código de aprobación. | SEAL |
| `motivoExtorno` | string | Motivo del extorno. | ELSE |
| `referenciaOperacion` | string | Referencia de la operación. | YAGANASTE |
| `parametrosOperacion` | array | Lista de parámetros `{ indice, valor }`. | YAGANASTE, WESTERN_UNION |
| `agencia` | string | Agencia que opera. | ELSE, CLARO, SEAL |
| `medioPago` | string | Medio/canal de pago. | ELSE |
| `formaPago` | string | Forma de pago. | UNIVCUSCO |
| `moneda` | string | Código de moneda. | UNIVCONTINENTAL, UNIVCUSCO, PAYTOPERU, WESTERN_UNION |
| `canal` | string | Canal de atención. | CLARO, UNIVCUSCO, UNIVCONTINENTAL, PAYTOPERU |
| `ubicacionCanal` | string | Ubicación del canal. | PAYTOPERU |
| `tpv` | string | Terminal punto de venta. | YAGANASTE |
| `claveTpv` | string | Clave del TPV. | YAGANASTE |
| `idAgente` | string | Identificador de agente. | WESTERN_UNION |
| `idLocacion` | string | Identificador de locación. | WESTERN_UNION |
| `caja` | string | Caja. | WESTERN_UNION |
| `idProducto` | string | Identificador de producto. | WESTERN_UNION |
| `idTransaccion` | string | Identificador de transacción del proveedor. | WESTERN_UNION |
| `idLote` | string | Identificador de lote. | WESTERN_UNION |
| `cantidad` | number | Cantidad. | UNIVCUSCO |
| `serviceCharge` | object | Cargo por servicio `{ moneda, importe }`. | WESTERN_UNION |
| `datosAdicionales` | array | Lista de datos adicionales `{ codigo, valor }`. | WESTERN_UNION |
| `transacciones` | array | Lista de transacciones para operaciones por lote. | WESTERN_UNION |
| `datos` | string | Datos de la operación (blob). | UNIVCONTINENTAL |
| `datosEmpresa` | string | Datos de la empresa. | UNIVCUSCO |
| `otrosDatosEmpresa` | string | Otros datos de la empresa. | UNIVCUSCO |

### 2.4 `datos.transaccion[]` — datos de pago / tarjeta (CLARO)
| Campo | Tipo | Descripción |
|---|---|---|
| `binAdquiriente` | string | Bin del adquiriente. |
| `binAdquirienteReenvia` | string | Bin del adquiriente que reenvía. |
| `codigoMoneda` | string | Código de moneda de la transacción. |
| `codigoMonedaPago` | string | Código de moneda del pago. |
| `codigoFormato` | string | Código de formato. |
| `codigoMedioPago` | string | Código de medio de pago. |
| `cuentaDestino` | string | Cuenta destino. |
| `cuentaOrigen` | string | Cuenta origen. |
| `fechaCaptura` | string (date-time) | Fecha de captura. |
| `fechaTransaccion` | string (date-time) | Fecha de transacción. |
| `fechaExpiracionTarjeta` | string | Fecha de expiración de la tarjeta. |
| `mac` | string | MAC de seguridad. |
| `nombreComercio` | string | Nombre del comercio. |
| `numeroComercio` | string | Número de comercio. |
| `numeroReferencia` | string | Número de referencia. |
| `numeroTerminal` | string | Número de terminal. |
| `numeroTarjeta` | string | Número de tarjeta. |
| `pinblock` | string | Pinblock. |
| `track2Data` | string | Track 2. |
| `acreedor` | string | Acreedor. |
| `ciudad` | string | Ciudad. |
| `datosTransaccion` | string | Datos de la transacción. |
| `numeroIdentificacionDeudor` | string | Identificación del deudor. |
| `numeroProductosPagados` | string | Número de productos pagados. |
| `pagoTotal` | string | Indicador de pago total. |
| `plaza` | string | Plaza. |
| `procesador` | string | Procesador. |
| `tipoCambio` | string | Tipo de cambio. |
| `tipoIdentificacionDeudor` | string | Tipo de identificación del deudor. |

---

## 3. Matriz de obligatoriedad por convenio y operación

Leyenda: **R** = obligatorio, **O** = opcional, **–** = no aplica. Los campos viven en el grupo indicado en la sección 2.

### ELSE
| Operador | Obligatorios | Opcionales |
|---|---|---|
| CONSULTA_DEUDA | `datoConsulta` | `tipoConsulta`, `agencia`, `medioPago` |
| PAGO_DEUDA | `identificadorTransaccion`, `numeroComprobante`, `numeroSuministro`, `monto` | `agencia`, `medioPago` |
| CONSULTA_PAGO | `identificadorTransaccion`, `numeroComprobante`, `numeroSuministro`, `monto` | `agencia`, `medioPago` |
| EXTORNO_PAGO | `identificadorTransaccion`, `identificadorPago`, `numeroComprobante`, `numeroSuministro`, `monto` | `motivoExtorno`, `agencia`, `medioPago` |

### ELECTRO_UCAYALI
| Operador | Obligatorios | Opcionales |
|---|---|---|
| CONSULTA_DEUDA | `numeroSuministro`, `traceConsulta`, `fechaOperacion` | – |
| PAGO_DEUDA | `numeroSuministro`, `numeroComprobante`, `monto`, `fechaOperacion` | `traceConsulta` |
| EXTORNO_PAGO | `numeroSuministro`, `numeroComprobante`, `monto`, `fechaOperacion` | `traceConsulta` |
| EXTORNO_PAGO_AUTO | `numeroSuministro`, `numeroComprobante`, `monto`, `fechaOperacion` | – |
| EXTORNO_AUTO | `numeroSuministro`, `numeroComprobante`, `monto`, `fechaOperacion` | `identificadorPago` |

### SEAL
| Operador | Obligatorios | Opcionales |
|---|---|---|
| CONSULTA_DEUDA | `contrato` (o `numeroSuministro`) | `identificadorTransaccion`, `tipoPago`, `fechaOperacion` |
| PAGO_DEUDA | `contrato` (o `numeroSuministro`), `monto` | `identificadorTransaccion`, `tipoPago`, `fechaOperacion` |
| EXTORNO_PAGO | `contrato` (o `numeroSuministro`) | `identificadorTransaccion`, `tipoPago`, `fechaOperacion` |
| ANULACION | `contrato` (o `numeroSuministro`), `codigoAprobacion` | `tipoPago`, `fechaOperacion` |

### CLARO
| Operador | Obligatorios | Opcionales |
|---|---|---|
| PAGO | `monto` | `identificadorTransaccion`, `codigoMoneda`, `numeroComercio`, `numeroTerminal` y demás campos de tarjeta/canal |
| RECARGA | `monto` | `identificadorTransaccion`, campos de tarjeta/canal |
| EXTORNO_PAGO | `monto`, `identificadorTransaccion` | campos de tarjeta/canal |
| EXTORNO_ANULACION_PAGO | `monto`, `identificadorTransaccion` | campos de tarjeta/canal |
| EXTORNO_ANULACION_RECARGA | `monto`, `identificadorTransaccion` | campos de tarjeta/canal |
| ANULACION_PAGO | `identificadorTransaccion` | campos de tarjeta/canal |
| ANULACION_RECARGA | `identificadorTransaccion` | campos de tarjeta/canal |
| CONSULTA_DEUDA | `numeroSuministro` | campos de canal |
| CONSULTA_SALDO | `numeroSuministro` | campos de canal |
| ECHO_PAGO | – | campos de tarjeta/canal |
| ECHO_RECARGA | – | campos de tarjeta/canal |

### UNIVCUSCO
| Operador | Obligatorios | Opcionales |
|---|---|---|
| CONSULTA_DEUDA | `numeroReferencialDeuda` | `codigoOperacion`, `numeroOperacion`, `codigoBanco`, `codigoConvenio`, `canal`, `codigoOficina`, `fechaOperacion`, `horaOperacion` |
| CONSULTA_DEUDA_ORDINARIO | `tipoDocumentoCliente`, `numeroDocumento`, `codigoConcepto` | `codigoOperacion`, `numeroOperacion`, `codigoBanco`, `codigoConvenio`, `canal`, `codigoOficina`, `fechaOperacion`, `horaOperacion` |
| NOTIFICACION_PAGO | `numeroReferencialDeuda`, `numeroDocumento`, `monto`, `numeroOperacionRecaudacion`, `formaPago`, `moneda` | `codigoOperacion`, `numeroOperacion`, `codigoBanco`, `codigoConvenio`, `canal`, `codigoOficina`, `fechaOperacion`, `horaOperacion`, `datosEmpresa`, `otrosDatosEmpresa` |
| NOTIFICACION_PAGO_ORDINARIO | `tipoDocumentoCliente`, `numeroDocumento`, `nombreCliente`, `codigoConcepto`, `numeroOperacionRecaudacion`, `cantidad`, `monto`, `formaPago`, `moneda` | `codigoOperacion`, `numeroOperacion`, `codigoBanco`, `codigoConvenio`, `canal`, `codigoOficina`, `fechaOperacion`, `horaOperacion`, `datosEmpresa`, `otrosDatosEmpresa` |
| AUTO_EXTORNO | `numeroReferencialDeuda`, `numeroDocumento`, `monto`, `numeroOperacionRecaudacion`, `numeroOperacionOriginal`, `fechaOperacionOriginal`, `formaPago`, `moneda` | `codigoOperacion`, `numeroOperacion`, `codigoBanco`, `codigoConvenio`, `canal`, `codigoOficina`, `datosEmpresa`, `otrosDatosEmpresa` |

### YAGANASTE
| Operador | Obligatorios | Opcionales |
|---|---|---|
| CONSULTA | `tpv`, `claveTpv`, `codigoProducto`, `referenciaOperacion` | `fechaOperacion`, `parametrosOperacion` |
| CONSULTA_PREVIA | `tpv`, `claveTpv`, `codigoProducto`, `referenciaOperacion` | `fechaOperacion`, `parametrosOperacion` |
| VENTA | `tpv`, `claveTpv`, `codigoProducto`, `referenciaOperacion` | `fechaOperacion`, `parametrosOperacion` |
| DEVOLUCION | `tpv`, `claveTpv`, `codigoProducto`, `referenciaOperacion` | `fechaOperacion`, `parametrosOperacion` |
| SALDO | `tpv`, `claveTpv` | `codigoProducto`, `referenciaOperacion`, `parametrosOperacion` |
| ECHO | – | `tpv`, `claveTpv`, `parametrosOperacion` |

### MUNICIPALIDAD_CUSCO
| Operador | Obligatorios | Opcionales |
|---|---|---|
| RECUPERAR_ORDEN_PAGO | `numeroOrdenPago` | – |
| VALIDAR_ORDEN_PAGO | `numeroOrdenPago`, `codigoContribuyente`, `monto` | – |
| ACTUALIZAR_ORDEN_PAGO | `numeroOrdenPago`, `numeroOperacion` | – |

### PAYTOPERU
| Operador | Obligatorios | Opcionales |
|---|---|---|
| CONSULTA_PAGO | `codigoPago`, `numeroDocumento` | – |
| NOTIFICACION_PAGO | `codigoPago`, `canal`, `ubicacionCanal`, `monto`, `moneda`, `fechaOperacion` | – |
| CONFIRMACION_PAGO | `codigoPago` | – |
| CONSULTA_TRANSACCIONES | `fechaOperacion` | – |

### WESTERN_UNION
| Operador | Obligatorios | Opcionales |
|---|---|---|
| BUSCAR_PRODUCTO | `idAgente`, `idLocacion`, `caja` | – |
| CONSULTAR_TRANSACCION | `idProducto`, `parametrosOperacion` | – |
| CONSULTAR_DATOS_ADICIONALES | `idProducto` | `idTransaccion` |
| AGREGAR_DATOS_ADICIONALES | `idProducto`, `idTransaccion`, `datosAdicionales` | – |
| CONFIRMAR_LOTE | `transacciones` | – |
| CONFIRMAR_DIRECTO | `idProducto`, `parametrosOperacion` | – |
| CONSULTAR_LOTE | – | `idLote` |
| REIMPRIMIR_TRANSACCION | `idLote` | `transacciones` |
| ANULAR_TRANSACCION | – | `idLote`, `transacciones` |

### UNIVCONTINENTAL (Universidad y Centro de Idiomas)
| Operador | Obligatorios | Opcionales |
|---|---|---|
| UNIVERSIDAD_CONSULTA_DEUDA | `contrato` | `moneda`, `canal` |
| UNIVERSIDAD_PAGO_DEUDA | `contrato`, `numeroTrace`, `fechaOperacion`, `datos` | `canal` |
| UNIVERSIDAD_EXTORNO_DEUDA | `contrato`, `numeroTrace`, `fechaOperacion` | – |
| CENTRO_IDIOMAS_CONSULTA_DEUDA | `contrato` | `moneda`, `canal` |
| CENTRO_IDIOMAS_PAGO_DEUDA | `contrato`, `numeroTrace`, `fechaOperacion`, `datos` | `canal` |
| CENTRO_IDIOMAS_EXTORNO_DEUDA | `contrato`, `numeroTrace`, `fechaOperacion` | – |

> **Nota:** la obligatoriedad se tomó de los instructivos (ELSE = `Convenio 1` y ELECTRO_UCAYALI = `Convenio 2` en la raíz; el resto en `docs/`). Para **CLARO** no hay instructivo; sus campos se extrajeron del código (`ClaroSoapMessageCodec`), que hoy trata todos los campos como opcionales.

---

## 4. Mapeo campo común → campo del proveedor

Cada mapeador (traductor) toma los campos del request único y los convierte al formato de su proveedor.

### 4.1 Convenios del primer grupo
| Campo común | ELSE | ELECTRO_UCAYALI | SEAL | MUNICIPALIDAD_CUSCO | UNIVCONTINENTAL |
|---|---|---|---|---|---|
| `numeroSuministro` | CodigoSuministro | `nroSumin` | contrato (respaldo) | – | – |
| `numeroComprobante` | CodigoComprobante | `numFactura` | – | – | – |
| `contrato` | – | – | campo privado 121 | – | `codigocontrato` |
| `numeroOrdenPago` | – | – | – | `ordenpago` | – |
| `identificadorTransaccion` | IdentificadorTransaccion | – | campo 11 (trace) | – | – |
| `identificadorPago` | CodigoIdentificadorPago | `tracePago` | – | – | – |
| `monto` | MontoPago / MontoExtorno | `montoDeuda` | campo 4 | `monto` | – |
| `fechaOperacion` | – | `fechaPago` / `fechaConsulta` | campos 12/13 | – | `Fecha` |
| `traceConsulta` | – | `traceConsulta` | – | – | – |
| `tipoConsulta` | TipoConsulta | – | – | – | – |
| `datoConsulta` | DatoConsulta | – | – | – | – |
| `tipoPago` | – | – | campo 121 (tipo pago) | – | – |
| `codigoAprobacion` | – | – | campo 38 | – | – |
| `motivoExtorno` | CodigoMotivoExtorno | – | – | – | – |
| `agencia` | Agencia | `codAgencia` | campo 42 | – | – |
| `medioPago` | MedioPago | – | – | – | – |
| `moneda` | – | – | campo 49 | – | `moneda` |
| `canal` | – | `codCanal` | – | – | `Canal` |
| `numeroTrace` | – | – | – | – | `NumeroTrace` |
| `datos` | – | – | – | – | `Datos` |
| `codigoContribuyente` | – | – | – | `codcontribuyente` | – |
| `numeroOperacion` | – | – | – | `nrooperacion` | – |

### 4.2 Convenios del segundo grupo
| Campo común | CLARO | UNIVCUSCO | YAGANASTE | PAYTOPERU | WESTERN_UNION |
|---|---|---|---|---|---|
| `monto` | `importePago` | `importeDeudaPagada` | – | `nimporte` | `importe` |
| `identificadorTransaccion` | `trace` | – | – | – | – |
| `canal` | `canal` | `canalOperacion` | – | `ccanal` | – |
| `ubicacionCanal` | – | – | – | `cubicacion_canal` | – |
| `moneda` | – | `codigoMoneda` | – | `nmoneda` | `moneda` |
| `fechaOperacion` | `fechaTransaccion` | `fechaOperacion` | `fechaHoraOperacion` | `dfecha` | – |
| `agencia` | `agencia` | – | – | – | – |
| `numeroSuministro` | `numeroSuministro` | – | – | – | – |
| `numeroReferencialDeuda` | – | `numeroReferenciaDeuda` | – | – | – |
| `numeroOperacionRecaudacion` | – | `numeroOperacionRecaudos` | – | – | – |
| `numeroOperacion` | – | `numeroOperacion` | – | – | – |
| `numeroOperacionOriginal` | – | `numeroOperacionOriginal` | – | – | – |
| `fechaOperacionOriginal` | – | `fechaOperacionOriginal` | – | – | – |
| `codigoOperacion` | – | `codigoOperacion` | – | – | – |
| `codigoBanco` | – | `codigoBanco` | – | – | – |
| `codigoConvenio` | – | `codigoConvenio` | – | – | – |
| `codigoOficina` | – | `codigoOficina` | – | – | – |
| `codigoConcepto` | – | `codigoConcepto` | – | – | – |
| `horaOperacion` | – | `horaOperacion` | – | – | – |
| `tipoDocumentoCliente` | – | `tipoDocumentoCliente` | – | – | – |
| `numeroDocumento` | – | `numeroDocumento` | – | `cnro_documento` | – |
| `nombreCliente` | – | `nombreCliente` | – | – | – |
| `formaPago` | – | `formaPago` | – | – | – |
| `cantidad` | – | `cantidad` | – | – | – |
| `datosEmpresa` | – | `datosEmpresa` | – | – | – |
| `otrosDatosEmpresa` | – | `otrosDatosEmpresa` | – | – | – |
| `tpv` | – | – | `tpv` | – | – |
| `claveTpv` | – | – | `claveTpv` | – | – |
| `codigoProducto` | – | – | `codigoProducto` | – | – |
| `referenciaOperacion` | – | – | `referenciaOperacion` | – | – |
| `parametrosOperacion` | – | – | `parametrosOperacion` | – | `listaParametros` |
| `codigoPago` | – | – | – | `ncodigo_pago` | – |
| `idAgente` | – | – | – | – | `idAgente` |
| `idLocacion` | – | – | – | – | `idLocacion` |
| `caja` | – | – | – | – | `caja` |
| `idProducto` | – | – | – | – | `idProducto` |
| `idTransaccion` | – | – | – | – | `idTransaccion` |
| `idLote` | – | – | – | – | `idLote` |
| `datosAdicionales` | – | – | – | – | `listaDatosAdicionales` |
| `transacciones` | – | – | – | – | `listaTransacciones` |
| `serviceCharge` | – | – | – | – | `serviceCharge` |
| campos de tarjeta | (mismo nombre) | – | – | – | – |
| `operador` (sobre) | – | – | – | – | `operador` |

---

## 5. Ejemplos del request único

**ELSE — consulta de deuda**
```json
{ "institucion": "ELSE", "operador": "CONSULTA_DEUDA",
  "datos": { "filtro": { "datoConsulta": "0010681504", "tipoConsulta": "1" },
             "cliente": {}, "transaccion": [] } }
```

**ELECTRO_UCAYALI — pago de deuda**
```json
{ "institucion": "ELECTRO_UCAYALI", "operador": "PAGO_DEUDA",
  "datos": { "filtro": { "numeroSuministro": "800006812", "numeroComprobante": "201800100000",
                         "traceConsulta": "TRC-001" },
             "cliente": {}, "transaccion": [ { "monto": 19423.00 } ] } }
```

**SEAL — anulación**
```json
{ "institucion": "SEAL", "operador": "ANULACION",
  "datos": { "filtro": { "contrato": "98787" },
             "cliente": {}, "transaccion": [ { "codigoAprobacion": "00001" } ] } }
```

**CLARO — pago**
```json
{ "institucion": "CLARO", "operador": "PAGO",
  "datos": { "filtro": {}, "cliente": {},
             "transaccion": [ { "monto": 10.00, "identificadorTransaccion": "TRC-001",
                                "numeroComercio": "123", "numeroTerminal": "001",
                                "codigoMoneda": "604" } ] } }
```

**UNIVCUSCO — notificar pago**
```json
{ "institucion": "UNIVCUSCO", "operador": "NOTIFICACION_PAGO",
  "datos": { "filtro": { "numeroReferencialDeuda": "123456", "numeroOperacionRecaudacion": "OP-1",
                         "codigoBanco": "003" },
             "cliente": { "numeroDocumento": "12345678" },
             "transaccion": [ { "monto": 250.00, "formaPago": "EFECTIVO", "moneda": "PEN" } ] } }
```

**YAGANASTE — consulta**
```json
{ "institucion": "YAGANASTE", "operador": "CONSULTA",
  "datos": { "filtro": { "codigoProducto": "P1" }, "cliente": {},
             "transaccion": [ { "tpv": "1", "claveTpv": "clave", "referenciaOperacion": "REF-1",
                                "fechaOperacion": "2026-10-14T10:30:00" } ] } }
```

**MUNICIPALIDAD_CUSCO — validar orden de pago**
```json
{ "institucion": "MUNICIPALIDAD_CUSCO", "operador": "VALIDAR_ORDEN_PAGO",
  "datos": { "filtro": { "numeroOrdenPago": "123" },
             "cliente": { "codigoContribuyente": "C-001" },
             "transaccion": [ { "monto": 150.00 } ] } }
```

**PAYTOPERU — notificación de pago**
```json
{ "institucion": "PAYTOPERU", "operador": "NOTIFICACION_PAGO",
  "datos": { "filtro": { "codigoPago": "123" },
             "cliente": {},
             "transaccion": [ { "canal": "01", "ubicacionCanal": "AGENCIA-01",
                                "monto": 120.00, "moneda": "1",
                                "fechaOperacion": "14-10-2026 10:30:00" } ] } }
```

**WESTERN_UNION — confirmar lote**
```json
{ "institucion": "WESTERN_UNION", "operador": "CONFIRMAR_LOTE",
  "datos": { "filtro": {}, "cliente": {},
             "transaccion": [ { "transacciones": [
                 { "idProducto": "P1", "idTransaccion": "T1", "moneda": "604", "monto": 100.00 } ] } ] } }
```

**UNIVCONTINENTAL — pago de deuda (Universidad)**
```json
{ "institucion": "UNIVCONTINENTAL_UNIVERSIDAD", "operador": "UNIVERSIDAD_PAGO_DEUDA",
  "datos": { "filtro": { "contrato": "C-123" }, "cliente": {},
             "transaccion": [ { "numeroTrace": "000123", "fechaOperacion": "2026-10-14",
                                "datos": "{\"detalle\":\"...\"}", "canal": "01" } ] } }
```

---

## 6. Reglas de validación propuestas

1. `institucion` y `operador` obligatorios; si no, error 400.
2. Si el convenio/operador no está soportado, error 400.
3. Validar los campos **obligatorios** de la matriz (sección 3) según `institucion` + `operador`; si falta alguno, error 400 con mensaje claro (ej. `"Falta el campo obligatorio: contrato"`).
4. Los campos **opcionales** ausentes no generan error (se usan valores por defecto del proveedor).
5. Los campos **no aplicables** se ignoran silenciosamente.
6. Nombres en **camelCase** y con el **vocabulario único** (sección 2).
7. `transaccion` puede traer **una o varias** operaciones; cada elemento se valida por separado.

---

## 7. Compatibilidad durante la transición (opcional)

Para no romper clientes actuales, se puede aceptar temporalmente **alias** y normalizarlos al campo común:

| Alias recibido | Campo común |
|---|---|
| `ordenpago` | `numeroOrdenPago` |
| `codigocontrato` | `contrato` |
| `ncodigo_pago` | `codigoPago` |
| `cnro_documento` | `numeroDocumento` |
| `cubicacion_canal` | `ubicacionCanal` |
| `codcontribuyente` | `codigoContribuyente` |
| `nrooperacion` | `numeroOperacion` |
| `nroSumin`, `numFactura` | `numeroSuministro` / `numeroComprobante` |
| `fechaHoraOperacion`, `dfecha` | `fechaOperacion` |
| `canalOperacion`, `ccanal` | `canal` |
| `importeDeudaPagada`, `importe`, `nimporte` | `monto` |

---

## 8. Justificación de la arquitectura

¿Por qué se toma esta decisión?

1. **Un solo punto de integración.** La entidad financiera (cliente) se integra **una sola vez** y no necesita conocer el formato de cada proveedor.
2. **El cliente no cambia por convenio.** El request de entrada es siempre el mismo; toda la diferencia se resuelve **dentro** del backend (en los mapeadores).
3. **Separa el contrato de entrada del transporte de salida.** El request único representa el "idioma de negocio"; por dentro se sigue traduciendo a SOAP, REST o ISO8583 según el proveedor.
4. **Obligatoriedad por convenio + operación, no por estructura distinta.** Un solo request y una **matriz** que dice qué campos son obligatorios u opcionales.
5. **Orden y semántica clara.** Agrupar en `filtro`, `cliente` y `transaccion` hace evidente qué dato es de búsqueda, cuál del cliente y cuál de la operación.
6. **Menos acoplamiento y más estabilidad.** Si un proveedor cambia su interfaz, el cliente **no se ve afectado**; solo se ajusta su mapeador.
7. **Validación y documentación centralizadas.** Un único contrato facilita las pruebas, la documentación y el manejo de errores.

En una frase: **se estandariza la "puerta de entrada" para todos, y se deja la traducción al proveedor como un detalle interno.**

---

## 9. ¿Es escalable?

Sí, con matices.

**Lo que la hace escalable:**
- El request **no cambia** al agregar convenios: se reutiliza el mismo objeto `datos`.
- La obligatoriedad de campos vive en **configuración (matriz)**, no en código.
- Los **protocolos de salida son plug-in** (SOAP, REST, ISO8583) y los **mapeadores son por convenio**.
- Si una operación nueva necesita un campo adicional, se **agrega como campo opcional al grupo correspondiente** (`filtro`, `cliente` o `transaccion`), sin romper a los clientes existentes.

**Riesgos a gobernar:**
- El objeto `datos` es un **superset** y tiende a **crecer**; hay que mantener un **vocabulario único** y evitar duplicados o sinónimos.
- Conviene **versionar el contrato** (ej. `v1`) para evolucionar sin romper integraciones.
- Toda regla de obligatoriedad debe quedar **documentada y centralizada**.

---

## 10. ¿Qué pasa cuando se integra un convenio nuevo?

El request de entrada **no se modifica** para el cliente. El proceso es:

1. **Registrar** la institución y sus operadores (enum/configuración).
2. **Definir en la matriz** qué campos son obligatorios u opcionales para cada operación.
3. **Crear el mapeador** (traductor) que convierte los campos comunes al formato del proveedor (SOAP, REST o ISO8583).
4. **Configurar** URL, protocolo y autenticación del proveedor.

Consecuencias:
- El **cliente sigue mandando el mismo request**; no renegocia nada.
- **No se toca** el endpoint ni los demás convenios.
- Si el proveedor necesita un dato que no existe, se **agrega un campo opcional** al grupo que corresponda (compatible hacia atrás) y se documenta.
- El costo de integración baja a **configurar + mapear**, no a rediseñar el contrato de entrada.

---

## 11. Rendimiento y concurrencia (picos de fin de mes)

**Escenario:** en el cierre de mes el volumen de consultas y pagos se multiplica y pueden llegar **miles de peticiones simultáneas**.

**Cómo responde hoy la arquitectura:**
- Quarkus sobre Vert.x (event loop no bloqueante) + Apache Camel con cliente HTTP sobre Vert.x.
- El endpoint y la ruta Camel (`direct:`) son **síncronos/bloqueantes**: cada petición ocupa un **hilo de trabajo (worker)** mientras espera la respuesta del proveedor.
- El tiempo lo domina la **latencia del proveedor** (SOAP/REST/ISO8583 externo), no el procesamiento propio, que es liviano (mapear JSON/XML/trama).
- Es **stateless** (sin estado compartido, salvo el cache del token Bearer), lo que permite **escalar horizontalmente**.

**El cuello de botella:**
- Con el modelo "un hilo por petición" y un pool de workers acotado, si los proveedores son lentos los hilos se agotan y las peticiones hacen **cola** → sube la latencia y aparecen **timeouts**.
- Cada llamada al proveedor reutiliza o abre conexión TCP y negocia TLS; si el pool de conexiones es pequeño, se vuelve el límite.
- Los timeouts por defecto (15 s) hacen que un proveedor degradado retenga un hilo hasta 15 s por petición.

**Qué ayuda (recomendaciones):**
1. **Hilos virtuales (Java 21):** ejecutar los endpoints/rutas en virtual threads para soportar miles de llamadas bloqueantes sin agotar hilos reales.
2. **Modelo reactivo/asíncrono:** usar el cliente HTTP reactivo de Vert.x para no ocupar un hilo por petición.
3. **Escalado horizontal:** al ser stateless, agregar réplicas (ya hay Dockerfiles) y balancear carga.
4. **Pool de conexiones HTTP:** keep-alive y tamaño de pool adecuado hacia cada proveedor.
5. **Timeouts + circuit breaker:** timeouts cortos por convenio y patrón *circuit breaker* (MicroProfile Fault Tolerance / Resilience4j).
6. **Caché del token:** ya existe para Electro Ucayali; evita autenticar en cada llamada.
7. **Aislamiento por convenio (bulkhead):** limitar la concurrencia por convenio.
8. **Pruebas de carga:** medir con k6/JMeter a la escala esperada de fin de mes.
9. **Observabilidad:** métricas por convenio (latencia, errores, colas) con Micrometer/OpenTelemetry.

**Conclusión:**
- Tal como está hoy (síncrono, un hilo por petición, pool por defecto) **no aguanta con holgura miles de conexiones simultáneas** en un pico; el límite no es la lógica de negocio (es barata), sino el **modelo de concurrencia** y la **latencia de los proveedores**.
- Con **hilos virtuales o reactivo + escalado horizontal + timeouts/circuit breaker + pruebas de carga**, la misma arquitectura de request único **sí puede** soportar los picos de fin de mes.

---

## 12. Arquitectura escalable, resiliencia y Redis (visión senior)

Esta sección define cómo evolucionar la arquitectura para que **no se degrade en picos de fin de mes**, sea **totalmente desacoplada**, **escalable horizontalmente** y permita **agregar convenios sin tocar el núcleo**.

### 12.1 Principios rectores
1. **No bloqueo (reactivo) de extremo a extremo:** el hilo del event loop nunca espera I/O de red.
2. **Stateless + estado compartido en Redis:** escala horizontal sin sesiones pegajosas.
3. **Aislamiento por convenio (bulkhead):** un proveedor lento no consume recursos de los demás.
4. **Resiliencia configurable por convenio** (timeout, retry, circuit breaker, bulkhead, fallback).
5. **Config-driven / plugin:** agregar convenio = configuración + mapeador.
6. **Backpressure y degradación controlada** antes que colapso.

### 12.2 Visión de capas (hexagonal)

```
   Entidad  ─►  adapter/in (REST reactivo)  ─►  application (orquestador Mutiny,
   Financiera        │                            registro de convenios, validación)
                     │                                     │ ports
                     │              adapter/out (mapeadores + clientes reactivos)
                     │                    │            │              │
                     │               Redis        Resilience      Métricas
                     │             (cache/lock/   (timeout,retry,  (Micrometer/
                     │              idempot.)      CB,bulkhead)     OTel)
```

- `domain` no depende de nada; `application` solo de `domain` y puertos; los adaptadores dependen del núcleo, **nunca al revés**.
- Redis, HTTP y resiliencia se inyectan **detrás de puertos**.

### 12.3 Flujo reactivo de una petición
```
POST /api/v1/operaciones  (Uni<Response>, no bloqueante)
 1. Trazabilidad (X-Request-Id → MDC/OTel)
 2. Validación contra la matriz (convenio + operación)
 3. Idempotencia en Redis (pagos): ¿ya procesado? → devolver resultado
 4. Orquestador reactivo: mapear → ejecutar [Bulkhead→CB→Timeout→Retry→Fallback] → interpretar
 5. (Opcional) cachear consulta en Redis
 6. Responder 200/4xx/5xx con contrato de error uniforme
```
Sin `block()` en el camino caliente.

### 12.4 Concurrencia, paralelismo y multihilos
- **I/O saliente reactivo** con el cliente HTTP de Vert.x (`WebClient`) vía Mutiny; pocos hilos atienden muchas peticiones.
- **CPU liviana** (mapear JSON/XML/trama) en el event loop; si es pesada, pool de workers acotado.
- **Hilos virtuales (Java 21)** solo para integraciones que exijan código bloqueante (`@RunOnVirtualThread`), nunca `new Thread()` ni pools sin límite.
- **Paralelismo de `transaccion[]`** con concurrencia acotada:
  ```
  Uni.combine().all().unis(transacciones.map(this::procesar).toList()).with(agregador)
  ```
- **Bulkhead por convenio:** cada convenio con su propio límite de concurrencia y pool de conexiones.

### 12.5 Resiliencia (configurable por convenio)
Se aplica en el borde de salida mediante un **registro de políticas** cargado de configuración (recomendado **Resilience4j programático**, un `Registry` por convenio).

| Política | Descripción | Config |
|---|---|---|
| **Timeout** | Corta la llamada (connect + read). | `convenio.<X>.timeout` |
| **Retry** | Reintenta solo operaciones **idempotentes** (consultas), con backoff + jitter. | `convenio.<X>.retry.*` |
| **Circuit Breaker** | Deja de llamar al proveedor caído. | `convenio.<X>.cb.*` |
| **Bulkhead** | Limita concurrencia por convenio. | `convenio.<X>.bulkhead.*` |
| **Rate Limiter** | Limita peticiones/segundo al proveedor. | `convenio.<X>.ratelimit.*` |
| **Fallback** | Respuesta alternativa (caché / error de negocio uniforme). | `convenio.<X>.fallback` |
| **Load shedding** | Si el sistema se satura, `429` con `Retry-After` en vez de colapsar. | global |

Reglas de oro:
- **Pagos/extornos NO se reintentan a ciegas** (riesgo de doble cobro): se combinan con **idempotencia en Redis**.
- **Consultas** sí se reintentan y se cachean.
- El **circuit breaker** evita acumular hilos esperando a un proveedor caído.

### 12.6 Redis (estado compartido y rendimiento)

| Uso | Clave | TTL | Propósito |
|---|---|---|---|
| **Caché de token** | `token:<convenio>` | según expiración | Comparte el token entre réplicas (hoy está en memoria). |
| **Caché de consultas** | `consulta:<convenio>:<clave>` | 10–60 s | Reduce llamadas en picos. |
| **Idempotencia de pagos** | `idem:<convenio>:<idempotencyKey>` | 24 h | Evita **doble pago**. |
| **Lock distribuido** | `lock:<convenio>:<claveNegocio>` | corto | Serializa operaciones sobre el mismo contrato/suministro. |
| **Rate limiting** | `rl:<convenio>:<ventana>` | ventana | Protege al proveedor y al sistema. |
| **Estado de circuit breaker** | `cb:<convenio>` | — | (Opcional) estado compartido entre réplicas. |
| **Contadores ligeros** | `cnt:<convenio>:<dia>` | 1–2 días | Métricas rápidas. |

Tecnología: **`quarkus-redis-client`** (Vert.x Redis, no bloqueante), **`quarkus-redis-cache`** para caché declarativa y **Redisson** para locks avanzados. Redis en modo **Sentinel/Cluster** para alta disponibilidad.

### 12.7 Escalabilidad y performance
- **Horizontal stateless:** N réplicas detrás de balanceador; el estado vive en Redis.
- **Pool de conexiones por proveedor** con keep-alive (evita costo de TLS por llamada).
- **Backpressure:** colas acotadas; al saturarse, **load shedding** (429).
- **Caché** para amortiguar picos.
- **Dimensionamiento por pruebas de carga** (k6/JMeter) a la escala de fin de mes.

### 12.8 Extensibilidad: agregar convenios sin tocar el núcleo
1. **Registro config-driven** (ya existe): `convenio.<X>.*` (operadores, protocolo, auth, url, timeout, retry, CB, bulkhead).
2. **Plugin de mapeador:** una clase `MapeadorOperacion` por convenio, descubierta por CDI; solo traduce campo común ↔ formato del proveedor.
3. **Descriptor declarativo (mejora propuesta):** para convenios simples, definir el mapeo de campos en configuración y evitar incluso la clase Java:
   ```yaml
   convenio:
     ELECTRO_NORTE:
       protocolo: rest
       auth: bearer
       mapeo: { numeroSuministro: nroSumin, numeroComprobante: numFactura, monto: montoDeuda }
       operaciones: { PAGO_DEUDA: { metodo: POST, ruta: /api/v1/pago } }
   ```
4. **Núcleo intacto:** el router/orquestador es genérico, sin `if/switch` por convenio.
5. **Contrato de entrada estable:** el request agrupado (`filtro`/`cliente`/`transaccion`) no cambia al sumar convenios.

### 12.9 Observabilidad
- **Métricas por convenio** (Micrometer/Prometheus): latencia p95/p99, errores, saturación de bulkhead, estado del CB, reintentos, cache hit ratio.
- **Tracing distribuido** (OpenTelemetry) con `traceId`/`X-Request-Id` de extremo a extremo, incluida la llamada al proveedor.
- **Logs estructurados** (JSON en prod), **health checks** (liveness/readiness, Redis, cada convenio) y **alertas de fin de mes**.

### 12.10 Roadmap por fases
| Fase | Entregable | Impacto |
|---|---|---|
| **1** | Request único agrupado + validación por matriz | Contrato estable |
| **2** | Cliente HTTP **reactivo** + orquestador `Uni` | Performance/concurrencia |
| **3** | **Resiliencia por convenio** (timeout, retry, CB, bulkhead, fallback) | Aislamiento y estabilidad |
| **4** | **Redis**: caché + **idempotencia de pagos** + locks | Escala horizontal, anti doble pago |
| **5** | **Bulkhead + rate limit + load shedding** | Resistencia a picos |
| **6** | **Observabilidad** + pruebas de carga | Visibilidad y dimensionamiento |
| **7** | **Descriptor declarativo** de convenios | Agregar convenios casi sin código |

### 12.11 Trade-offs y riesgos
- **Reactivo vs virtual threads:** reactivo es más eficiente pero más complejo; virtual threads son más simples. Recomendación: **reactivo en el I/O saliente**, virtual threads solo para código bloqueante inevitable.
- **Circuit breaker distribuido en Redis:** más consistente pero con latencia y dependencia; alternativa: CB **por instancia** (más simple). Decidir por criticidad.
- **Caché de consultas:** mejora performance pero puede servir datos "viejos"; TTL corto y clave por convenio+operación.
- **Reintentos en pagos:** riesgo de doble cobro → siempre con **idempotencia**.
- **Redis como dependencia crítica:** requiere alta disponibilidad y plan de degradación (modo sin caché).

---

## 13. Resumen para negocio

- **Un solo request** para los **10 convenios** y todas sus operaciones.
- El objeto `datos` está **ordenado en tres grupos**: `filtro` (qué buscar), `cliente` (quién paga) y `transaccion` (qué operación).
- Cada campo es **obligatorio u opcional según la operación**.
- El backend sigue traduciendo al formato de cada proveedor (SOAP, REST, ISO8583); el cliente solo conoce **un idioma**.
- Agregar un convenio nuevo = mapear los campos comunes a su formato, **sin cambiar el request de entrada**.
