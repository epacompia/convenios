# Propuesta — Arquitectura de Request Único

> **Estado:** implementado en la rama `eduardo-dev` (contrato único request/response, validación por matriz y multi-transacción).
>
> **Nota de implementación:** el contrato único ya está en código (`Cabecera`, `Datos`, `RecaudacionRequest`, `RecaudacionResponse`), con validación por convenio+operación (`MatrizObligatoriedad`, `ValidadorSolicitud`), eco de `cabecera`, respuesta agrupada (`respuesta.cliente` / `respuesta.transaccion[]`) y soporte de varias transacciones con resultado por transacción. Se mantiene compatibilidad de entrada con el formato plano anterior y se normalizan los alias de la sección 8. Los mapeadores internos se alinearon con los contratos de `docs/`: ELSE, ELECTRO_UCAYALI, SEAL, MUNICIPALIDAD_CUSCO, PAYTOPERU, UNIVCUSCO (UAC), YAGANASTE, WESTERN_UNION y UNIVCONTINENTAL (Universidad/Centro de Idiomas), incluyendo rutas, nombres de operación, campos y auth (`ApiKey`/Bearer) según cada instructivo. CLARO conserva el contrato de tarjeta existente porque el `webService_Pit` de `docs/` es otro contrato de recargas (WSPIT) no representado por el bloque CLARO del request único.
>
> **Correcciones aplicadas:** idempotencia atómica por `idTransaccion` (reserva previa en base de datos, hash del payload y TTL de proceso), respuestas de proveedor inválidas con fallo cerrado (HTTP 502/`ER`), mapeo de excepciones de resiliencia al contrato, alias del §8, matriz de obligatoriedad configurable (`convenio.<X>.requerido.<OPERADOR>`), resiliencia por convenio (circuito, bulkhead y rate limit), reintentos solo para operaciones idempotentes, auditoría aislada en su propia transacción y documentación OpenAPI (`/q/openapi`, `/q/swagger-ui`). Quedan como roadmap el cliente reactivo, los hilos virtuales y el caché de consultas.

> **Alcance:** unificar **un solo request** para los **10 convenios**: ELSE, ELECTRO_UCAYALI, SEAL, CLARO, UNIVCUSCO, YAGANASTE, MUNICIPALIDAD_CUSCO, PAYTOPERU, WESTERN_UNION y UNIVCONTINENTAL (Universidad + Centro de Idiomas).
> **Objetivo:** que exista **un único contrato de entrada** (mismo request para todos los convenios y todas las operaciones), con un objeto `datos` **organizado en grupos** (`filtro`, `cliente`, `transaccion`) según la idea de Hildebrando, donde cada campo es **obligatorio u opcional** según el convenio y la operación.
> **Validado contra** los instructivos de `docs/` (uno por convenio) y el código actual.

---

## 0. Estructura general

El request mantiene su sobre (`institucion` + `operador`), agrega una **`cabecera`** con metadatos genéricos de la petición, y el objeto `datos` se **agrupa** en tres llaves:

```
{
  "institucion": "CLARO",          // obligatorio
  "operador": "ECHO_RECARGA",      // obligatorio
  "cabecera": {                     // metadatos genéricos (comunes a todos los convenios)
    "idTransaccion": "20260218000123000042",
    "idSesion": "0",
    "codAgencia": "7",
    "codUsuario": "SIST",
    "ip": "127.0.0.1",
    "fecha": "2026-02-18T10:19:47-05:00",
    "idCanal": "21",
    "idAplicacion": "56"
  },
  "datos": {
    "filtro":      { ... },        // criterios de consulta / identificación del servicio
    "cliente":     { ... },        // identificación del cliente
    "transaccion": [ { ... } ]     // una o más operaciones (pago, extorno, anulación, recarga...)
  }
}
```

- **`institucion` / `operador`**: obligatorios, definen el convenio y la operación.
- **`cabecera`**: metadatos **genéricos** (mismos para todos los convenios): identificador de correlación (`idTransaccion`), sesión, agencia, usuario, IP, fecha, canal y aplicación que originan la petición. Se agrupan aparte para mantener `datos` limpio y concentrar la trazabilidad/auditoría en un solo bloque.
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
  "cabecera": {
    "idTransaccion": "20260218000123000042",
    "idSesion": "0",
    "codAgencia": "7",
    "codUsuario": "SIST",
    "ip": "127.0.0.1",
    "fecha": "2026-02-18T10:19:47-05:00",
    "idCanal": "21",
    "idAplicacion": "56"
  },
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

## 2. Diccionario de campos

### 2.0 `cabecera` — metadatos genéricos (mismos para todos los convenios)
| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `idTransaccion` | string | Sí | Identificador único de la petición: `yyyyMMddHHmmss` + secuencia de 6 dígitos (20 en total). Lo genera el cliente y **se devuelve idéntico** en el `cabecera` del response. |
| `idSesion` | string | Sí | Identificador de sesión de la petición. |
| `codAgencia` | string | Sí | Código de agencia que origina la petición. |
| `codUsuario` | string | Sí | Código de usuario que origina la petición. |
| `ip` | string | Sí | IP de origen de la petición. |
| `fecha` | string (date-time) | Sí | Fecha/hora de la petición (con zona horaria). |
| `idCanal` | string | Sí | Canal de atención. |
| `idAplicacion` | string | Sí | Aplicación que origina la petición. |

> La `cabecera` es **común a los 10 convenios** y viaja en todas las peticiones; el backend la usa para trazabilidad/auditoría y puede mapearla a campos equivalentes del proveedor cuando aplique.
>
> **`idTransaccion` — identificador de correlación request ↔ response:**
> - **Formato:** `yyyyMMddHHmmss` (fecha-hora al segundo) + secuencia de 6 dígitos → **20 dígitos**. Ej.: `20260218000123000042`.
> - **Generación:** consecutivo atómico en **Redis (`INCR`)** para garantizar unicidad entre réplicas durante los picos; alternativa sin Redis: `AtomicLong` por pod + código de pod.
> - **Regex:** `^\d{20}$`.
> - **Eco:** el valor recibido en el request se **devuelve sin cambios** en `respuesta.cabecera.idTransaccion` (ver secc. 6.4).
> - **Uso:** es la **llave única de correlación** para unir request ↔ response en los logs (trazabilidad de extremo a extremo); además, cada operación del arreglo conserva su `identificadorTransaccion` (trace de negocio).
> - **No confundir:** `cabecera.idTransaccion` = una por **petición** (correlación request ↔ response); `transaccion[].identificadorTransaccion` = trace por **operación**; `transaccion[].idTransaccion` (WESTERN_UNION) = identificador de transacción **del proveedor** (campo preexistente, secc. 2.3).

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

### 2.5 Justificación de los campos del request

> La **sección 3** dice **cuándo** un campo es obligatorio u opcional (según convenio + operación); esta sección dice **por qué el campo existe**: qué necesidad de negocio o de integración cubre y de qué proveedor nace.

**`cabecera` — contexto y trazabilidad de la petición**
| Campo | Por qué está (justificación) |
|---|---|
| `idTransaccion` | Llave única de correlación request ↔ response; permite unir la petición con su respuesta en millones de logs. |
| `idSesion` | Agrupa varias operaciones de una misma sesión de atención; trazabilidad por sesión. |
| `codAgencia` | Identifica la agencia/punto que origina la operación; auditoría, enrutamiento y reportes por agencia. |
| `codUsuario` | Identifica al cajero/operador responsable; control de acceso y auditoría de quién operó. |
| `ip` | Origen de red de la petición; seguridad y auditoría forense. |
| `fecha` | Momento de la petición; control de vigencia, orden temporal y auditoría. |
| `idCanal` | Canal de atención (ventanilla, app, agente, corresponsal); afecta reglas, tarifas y reportes. |
| `idAplicacion` | Sistema/aplicación origen; trazabilidad técnica y diagnóstico de integraciones. |

**`datos.filtro` — qué se consulta / identifica**
| Campo | Por qué está (justificación) |
|---|---|
| `numeroSuministro` | Identifica el **servicio** (luz/agua/telecom) sobre el que se consulta o paga; llave del cliente en los convenios de servicios. |
| `numeroComprobante` | Identifica el **recibo/factura** específico a pagar o extornar. |
| `contrato` | Identifica el contrato cuando el proveedor no usa número de suministro (SEAL, universidades). |
| `numeroOrdenPago` | Llave de la **orden de pago** municipal sobre la que se opera. |
| `numeroReferencialDeuda` | Referencia de la deuda en UNIVCUSCO; ubica la deuda a pagar. |
| `numeroOperacionRecaudacion` | Enlaza el pago con la **operación de recaudación**; conciliación banco–universidad. |
| `codigoPago` | Llave del pago en PAYTOPERU. |
| `codigoProducto` | Producto/servicio de YAGANASTE sobre el que se opera. |
| `codigoOperacion` | Clasifica la operación frente al proveedor (UNIVCUSCO). |
| `codigoBanco` | Identifica el banco en la recaudación interbancaria (UNIVCUSCO). |
| `codigoConvenio` | Identifica el convenio dentro del esquema del proveedor (UNIVCUSCO). |
| `codigoOficina` | Oficina que atiende la recaudación; traza del punto de atención (UNIVCUSCO). |
| `codigoConcepto` | Concepto de cobro (matrícula, pensión, subconcepto) (UNIVCUSCO). |
| `tipoConsulta` | Discrimina la modalidad de consulta en ELSE. |
| `datoConsulta` | Dato libre (documento, suministro…) con el que ELSE ubica la deuda. |
| `traceConsulta` | Referencia de la consulta previa; garantiza que el pago corresponda a una consulta válida (ELECTRO_UCAYALI). |

**`datos.cliente` — quién es el cliente**
| Campo | Por qué está (justificación) |
|---|---|
| `numeroDocumento` | Identifica al cliente/pagador para consultar o registrar la operación. |
| `tipoDocumentoCliente` | Discrimina el tipo de documento (DNI/RUC/CE) para validar y mostrar. |
| `nombreCliente` | Nombre del cliente; requerido en los flujos ordinarios para validar e imprimir el comprobante. |
| `codigoContribuyente` | Llave del contribuyente en la municipalidad. |

**`datos.transaccion[]` — qué operación se ejecuta**
| Campo | Por qué está (justificación) |
|---|---|
| `identificadorTransaccion` | Trace de la operación; permite referenciarla (idempotencia, extorno, soporte). |
| `identificadorPago` | Identifica el **pago a extornar/anular**. |
| `numeroOperacion` | Número de operación (actualizar orden municipal, recaudos UNIVCUSCO). |
| `numeroTrace` | Trace exigido por UNIVCONTINENTAL. |
| `numeroOperacionOriginal` | Referencia la operación original en un extorno. |
| `monto` | **Importe** a pagar o extornar; dato financiero central. |
| `fechaOperacion` | Fecha/hora de la operación; control temporal y conciliación. |
| `horaOperacion` | Hora de la operación cuando el proveedor la pide separada de la fecha (UNIVCUSCO). |
| `fechaOperacionOriginal` | Fecha de la operación original para el extorno. |
| `tipoPago` | Clasifica el pago (SEAL). |
| `codigoAprobacion` | Código de aprobación/autorización en la anulación (SEAL). |
| `motivoExtorno` | Razón del extorno (ELSE); registro y validación. |
| `referenciaOperacion` | Referencia del operador para la operación (YAGANASTE). |
| `parametrosOperacion` | Parámetros variables `{indice, valor}` que exige el proveedor sin añadir campos fijos. |
| `agencia` | Agencia que ejecuta la operación (cuando el proveedor lo pide dentro de la transacción). |
| `medioPago` | Medio/canal del pago (ELSE). |
| `formaPago` | Forma de pago (UNIVCUSCO). |
| `moneda` | Moneda del importe; evita la ambigüedad del monto. |
| `canal` | Canal de atención específico de la operación. |
| `ubicacionCanal` | Ubicación física del canal (PAYTOPERU); auditoría. |
| `tpv` | Terminal punto de venta que origina la operación (YAGANASTE). |
| `claveTpv` | Credencial que autentica la TPV (YAGANASTE). |
| `idAgente` | Agente que realiza la operación (WESTERN_UNION). |
| `idLocacion` | Locación del agente (WESTERN_UNION). |
| `caja` | Caja donde se opera (WESTERN_UNION). |
| `idProducto` | Producto sobre el que se opera (WESTERN_UNION). |
| `idTransaccion` | Identificador de la transacción en el proveedor (WESTERN_UNION). |
| `idLote` | Lote para operaciones agrupadas (WESTERN_UNION). |
| `cantidad` | Cantidad de ítems/documentos (UNIVCUSCO ordinario). |
| `serviceCharge` | Cargo por servicio `{moneda, importe}` exigido por WESTERN_UNION. |
| `datosAdicionales` | Pares `{codigo, valor}` específicos del proveedor (WESTERN_UNION). |
| `transacciones` | Lista de transacciones para operaciones por lote (WESTERN_UNION). |
| `datos` | Blob con el detalle de la operación (UNIVCONTINENTAL). |
| `datosEmpresa` / `otrosDatosEmpresa` | Datos de la empresa (universidad) requeridos por UNIVCUSCO. |

**`datos.transaccion[]` — datos de pago / tarjeta (CLARO)**
| Campo | Por qué está (justificación) |
|---|---|
| `binAdquiriente` / `binAdquirienteReenvia` | Identifican al adquiriente y enrutan el pago con tarjeta. |
| `codigoMoneda` / `codigoMonedaPago` | Monedas de la transacción y del pago. |
| `codigoFormato` | Formato del mensaje intercambiado. |
| `codigoMedioPago` | Medio de pago (tarjeta/efectivo). |
| `cuentaDestino` / `cuentaOrigen` | Cuentas afectadas por el movimiento. |
| `fechaCaptura` / `fechaTransaccion` | Momento de captura y de proceso de la transacción. |
| `fechaExpiracionTarjeta` | Validez de la tarjeta; validación. |
| `mac` | Código de autenticación de la trama (**seguridad**). |
| `nombreComercio` / `numeroComercio` | Identifican al comercio. |
| `numeroReferencia` / `numeroTerminal` | Referencia y terminal del pago. |
| `numeroTarjeta` | PAN de la tarjeta. **Dato sensible (PCI-DSS).** |
| `pinblock` | PIN cifrado. **Dato sensible (PCI-DSS).** |
| `track2Data` | Banda 2 de la tarjeta. **Dato sensible (PCI-DSS).** |
| `acreedor` / `ciudad` / `plaza` | Identifican al acreedor y la ubicación. |
| `datosTransaccion` | Datos adicionales de la transacción. |
| `numeroIdentificacionDeudor` / `tipoIdentificacionDeudor` | Identificación del deudor (documento y tipo). |
| `numeroProductosPagados` | Cuántos productos se pagan. |
| `pagoTotal` | Indica si es pago total. |
| `procesador` | Procesador de la transacción. |
| `tipoCambio` | Tipo de cambio aplicado. |

> **Nota de seguridad:** `numeroTarjeta`, `pinblock`, `track2Data` y `mac` son datos de tarjeta: deben tratarse conforme a **PCI-DSS** (no persistir ni loguear; cifrado en tránsito).

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

> Los ejemplos omiten el bloque `cabecera` por brevedad; en toda petición real es **obligatorio** (con `idTransaccion`, ver secc. 2.0).

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

## 6. Contrato de salida (response único)

El response es **autocontenido**: devuelve el **resultado de la operación** y el contexto que el cliente necesita para **imprimir el comprobante y conciliar**, aunque algunos datos coincidan con el request. Este es el patrón habitual en banca (ISO8583 y REST devuelven el registro completo de la operación). Conserva el mismo sobre (`institucion`, `operador`, `cabecera`) y la misma agrupación (`cliente`, `transaccion`), de modo que el cliente recibe **siempre la misma estructura** para los 10 convenios y todas sus operaciones. Hay campos que **no deben eliminarse** (ver secc. 6.6). Toda la traducción ocurre en `MapeadorOperacion.interpretar()`; lo muy específico de un proveedor va en `datosAdicionales`.

### 6.1 Estructura del response

```json
{
  "institucion": "SEAL",
  "operador": "CONSULTA_DEUDA",
  "cabecera": {
    "idTransaccion": "20260218000123000042",
    "idSesion": "0",
    "codAgencia": "7",
    "codUsuario": "SIST",
    "ip": "127.0.0.1",
    "fecha": "2026-02-18T10:19:47-05:00",
    "idCanal": "21",
    "idAplicacion": "56"
  },
  "respuesta": {
    "codigo": "00",
    "mensaje": "TRANSACCION CORRECTA",
    "cliente": {
      "tipoDocumentoCliente": "DNI",
      "numeroDocumento": "23917462",
      "nombreCliente": "DELGADO DE VIZCARRA NORA",
      "direccionCliente": "CALLE NICOLAS DE PIEROLA NRO 117-2",
      "distritoCliente": "CUSCO-PARURO-PARURO",
      "email": "cliente@correo.com",
      "codigoContribuyente": "C-001",
      "numeroSuministro": "10010681504"
    },
    "transaccion": [
      {
        "identificadorTransaccion": "369571",
        "identificadorPago": "20180425001000003",
        "numeroOperacion": "654321",
        "numeroComprobante": "98787",
        "codigoAprobacion": "00001",
        "monto": 1500.00,
        "moneda": "PEN",
        "fechaOperacion": "2026-02-18T10:19:47-05:00",
        "fechaRegistro": "2026-02-18T10:19:47-05:00",
        "fechaVencimiento": "2026-03-01T00:00:00",
        "fechaVencimientoRsp": "2026-03-01T00:00:00",
        "fechaEmision": "2026-01-01T00:00:00",
        "tipoIntegracion": "Online",
        "detalleConsulta": "RECIBO DE ENERGIA",
        "estado": "OK",
        "glosa": "RECIBO",
        "saldo": 0.00,
        "indMasDeuda": "NO",
        "cantidadDocsDeuda": 1,
        "datosEmpresa": "",
        "fee": 0.00,
        "importeOrigen": 0.00,
        "monedaOrigen": "PEN",
        "ubicacionCanal": "AGENCIA-01",
        "referenciaProveedor": "REF-PROV-1",
        "referenciaOperador": "REF-OP-1",
        "numeroLote": "LOTE-01",
        "clave": "AB123",
        "observacion": "",
        "deudas": [
          {
            "numeroComprobante": "201800100000",
            "descripcion": "RECIBO LUZ",
            "monto": 1500.00,
            "importeDeudaMinima": 1500.00,
            "fechaEmision": "2026-01-01",
            "fechaVencimiento": "2026-02-01",
            "periodo": "202601",
            "numeroDocumento": "23917462",
            "tipoIntegracion": "Online",
            "indicadorRestriccPago": "N",
            "cantidadSubconceptos": 0,
            "glosa": "RECIBO"
          }
        ],
        "parametros": [
          { "indice": "1", "valor": "123456" }
        ],
        "datosAdicionales": {}
      }
    ]
  }
}
```

### 6.2 Diccionario del response

**`respuesta`**
| Campo | Tipo | Descripción |
|---|---|---|
| `codigo` | string | Código canónico normalizado (catálogo). |
| `mensaje` | string | Mensaje estándar. |

**`respuesta.cliente`**
| Campo | Tipo | Descripción |
|---|---|---|
| `tipoDocumentoCliente` | string | Tipo de documento del cliente. |
| `numeroDocumento` | string | Documento del cliente. |
| `nombreCliente` | string | Nombre del cliente. |
| `direccionCliente` | string | Dirección del cliente. |
| `distritoCliente` | string | Distrito del cliente. |
| `email` | string | Correo del cliente. |
| `codigoContribuyente` | string | Código de contribuyente. |
| `numeroSuministro` | string | Número de suministro/servicio. |

**`respuesta.transaccion[]`**
| Campo | Tipo | Descripción |
|---|---|---|
| `identificadorTransaccion` | string | Trace / identificador de la transacción. |
| `identificadorPago` | string | Identificador de pago. |
| `numeroOperacion` | string | Número de operación del proveedor. |
| `numeroComprobante` | string | Comprobante / contrato / orden. |
| `codigoAprobacion` | string | Código de aprobación / autorización. |
| `monto` | number | Importe. |
| `moneda` | string | Moneda. |
| `fechaOperacion` | date-time | Fecha de la operación. |
| `fechaRegistro` | date-time | Fecha de registro en el proveedor. |
| `fechaVencimiento` | date-time | Fecha de vencimiento. |
| `fechaVencimientoRsp` | date-time | Fecha de vencimiento del comprobante (ELSE). |
| `fechaEmision` | date-time | Fecha de emisión. |
| `tipoIntegracion` | string | Tipo de integración (Online). |
| `detalleConsulta` | string | Detalle de la deuda (ELSE). |
| `estado` | string | Estado (OK / KO / estado de lote). |
| `glosa` | string | Glosa informativa. |
| `saldo` | number | Saldo (cuando aplica). |
| `indMasDeuda` | string | Indicador de más deudas (UNIVCUSCO). |
| `cantidadDocsDeuda` | number | Cantidad de documentos de deuda (UNIVCUSCO). |
| `datosEmpresa` | string | Datos de la empresa (UNIVCUSCO). |
| `fee` | number | Comisión (YAGANASTE). |
| `importeOrigen` | number | Importe de origen (YAGANASTE). |
| `monedaOrigen` | string | Moneda de origen (YAGANASTE). |
| `ubicacionCanal` | string | Ubicación del canal (PAYTOPERU). |
| `referenciaProveedor` | string | Referencia del proveedor. |
| `referenciaOperador` | string | Referencia del operador. |
| `numeroLote` | string | Identificador de lote. |
| `clave` | string | Clave de confirmación. |
| `observacion` | string | Observación. |
| `deudas[]` | array | Deudas (consultas). |
| `parametros[]` | array | Parámetros indexados `{ indice, valor }`. |
| `datosAdicionales` | object | Bloque de extensión para lo específico del proveedor. |

**`respuesta.transaccion[].deudas[]`**
| Campo | Tipo | Descripción |
|---|---|---|
| `numeroComprobante` | string | Comprobante / documento. |
| `descripcion` | string | Descripción. |
| `monto` | number | Importe de la deuda. |
| `importeDeudaMinima` | number | Importe mínimo (UNIVCUSCO). |
| `fechaEmision` | date-time | Fecha de emisión. |
| `fechaVencimiento` | date-time | Fecha de vencimiento. |
| `periodo` | string | Periodo (UNIVCONTINENTAL). |
| `numeroDocumento` | string | Documento (UNIVCONTINENTAL). |
| `tipoIntegracion` | string | Tipo de integración (ELECTRO_UCAYALI). |
| `indicadorRestriccPago` | string | Indicador de restricción de pago (UNIVCUSCO). |
| `cantidadSubconceptos` | number | Cantidad de subconceptos (UNIVCUSCO). |
| `glosa` | string | Glosa. |

### 6.3 Mapeo response general ← proveedor

| Campo general | ELSE | ELECTRO_UCAYALI | SEAL | CLARO |
|---|---|---|---|---|
| `codigo` | CodigoMensajeRetorno* | codigo | campo 39 | codigoRespuesta |
| `mensaje` | MensajeRetorno* | mensaje | catálogo(39) | — |
| `cliente.nombreCliente` | NombreCliente | nombreCliente | P04 | — |
| `cliente.numeroDocumento` | NumeroDocumento | — | — | — |
| `cliente.direccionCliente` | DireccionCliente | — | P05 | — |
| `cliente.distritoCliente` | DistritoCliente | — | — | — |
| `cliente.numeroSuministro` | CodigoSuministro | nroSumin | P02 | — |
| `transaccion.identificadorTransaccion` | IdentificadorTransaccion | — | campo 11 | — |
| `transaccion.identificadorPago` | CodigoIdentificadorPago | — | — | — |
| `transaccion.numeroOperacion` | — | numOperacionEmpresa | — | — |
| `transaccion.numeroComprobante` | CodigoComprobante | numFactura | P02 (contrato) | — |
| `transaccion.codigoAprobacion` | — | — | campo 38 | codigoAutorizacion |
| `transaccion.monto` | MontoAPagar/MontoPago/MontoExtorno | montoDeuda | campo 4 | — |
| `transaccion.fechaRegistro` | FechaRegistro* | — | — | — |
| `transaccion.fechaEmision` | FechaEmision | — | — | — |
| `transaccion.fechaVencimientoRsp` | FechaVencimientoRSP | — | — | — |
| `transaccion.detalleConsulta` | DetalleConsulta | — | — | — |
| `transaccion.glosa` | — | glosa | — | — |
| `transaccion.tipoIntegracion` | — | tipointegracion | — | — |
| `transaccion.deudas[]` | (consulta) | lstdebt[] | — | — |

| Campo general | UNIVCUSCO | YAGANASTE | MUNICIPALIDAD_CUSCO | PAYTOPERU |
|---|---|---|---|---|
| `codigo` | codigo | responseCode | status | berror→00/01 |
| `mensaje` | descripcion | errMessage | message | cmensaje |
| `cliente.nombreCliente` | nombreCliente | — | data.contribuyente | cnombres+capellidos |
| `cliente.numeroDocumento` | — | — | — | cnro_documento |
| `cliente.email` | — | — | — | cemail |
| `cliente.codigoContribuyente` | — | — | data.codcontribuyente | — |
| `transaccion.numeroOperacion` | numeroOperacionEmpresa | — | — | ncodigo_pago |
| `transaccion.numeroComprobante` | numeroReferenciaDeuda | — | data.ordenpago | — |
| `transaccion.monto` | importeDeuda | importe | data.monto | nimporte |
| `transaccion.moneda` | codigoMoneda | monedaOrigen | — | nmoneda |
| `transaccion.fechaOperacion` | — | — | — | dfecha |
| `transaccion.ubicacionCanal` | — | — | — | cubicacion_canal |
| `transaccion.estado` | — | estadoVenta | data.validacion | — |
| `transaccion.glosa` | — | — | data.descripcion | cconcepto |
| `transaccion.indMasDeuda` | indMasDeuda | — | — | — |
| `transaccion.cantidadDocsDeuda` | cantidadDocsDeuda | — | — | — |
| `transaccion.datosEmpresa` | datosEmpresa | — | — | — |
| `transaccion.fee` | — | fee | — | — |
| `transaccion.importeOrigen` | — | importeOrigen | — | — |
| `transaccion.referenciaProveedor` | — | referenciaSigma | — | — |
| `transaccion.referenciaOperador` | — | refOperador | — | — |
| `transaccion.saldo` | — | saldo | — | — |
| `transaccion.clave` | — | — | — | cclave |
| `transaccion.observacion` | — | — | — | cobservacion |
| `transaccion.parametros[]` | — | parametros[] | — | — |
| `transaccion.deudas[]` | listaDocumentos[] | — | — | — |

| Campo general | WESTERN_UNION | UNIVCONTINENTAL |
|---|---|---|
| `codigo` | codigoRespuesta / estado | Codigo |
| `mensaje` | error.mensaje | Mensaje |
| `cliente.nombreCliente` | descripcionCliente | Cliente |
| `transaccion.numeroOperacion` | — | NumeroOperacion |
| `transaccion.numeroComprobante` | — | CodigoContrato |
| `transaccion.numeroLote` | lote.idLote | — |
| `transaccion.estado` | lote.estado / transaccion.estado | — |
| `transaccion.monto` | importe | — |
| `transaccion.moneda` | moneda | — |
| `transaccion.codigoAprobacion` | comprobante | — |
| `transaccion.parametros[]` | listaParametros[] | — |
| `transaccion.deudas[]` | listaTransacciones[] | Datos[] |
| `datosAdicionales` | catálogo de productos/zonas/locaciones, serviceCharge, importeMinimo/Maximo, cantidades y totales de lote | — |

### 6.4 Regla de normalización
1. El mapeador (`interpretar`) traduce la respuesta del proveedor a este contrato.
2. `respuesta.cabecera.idTransaccion` se copia **idéntico** del `cabecera` del request (llave de correlación request ↔ response); si el request no lo trae, el backend genera uno y lo devuelve.
3. `codigo` y `mensaje` se normalizan con el catálogo de códigos del convenio.
4. Los campos comunes se llenan con lo que el proveedor devuelva; los ausentes se omiten o van nulos.
5. Lo que no encaje en los campos comunes va en `respuesta.transaccion[].datosAdicionales` (bloque de extensión), sin romper el contrato.
6. Ante error, `respuesta.codigo = "ER"` y `respuesta.mensaje` con el detalle (HTTP 400/502/504).

### 6.5 Justificación de los campos del response

Cada campo del response existe por una razón concreta: porque **al menos un convenio lo devuelve** y/o porque el cliente (la entidad financiera) **lo necesita para operar** (imprimir el comprobante, confirmar el importe, reintentar, extornar o conciliar). Se agrupan por función y se normalizan a un único vocabulario.

**Categorías de justificación (por qué está cada campo):**
- **Control del resultado:** expresa el estado de la operación de forma única (`codigo`, `mensaje`).
- **Correlación:** llaves para referenciar y volver a operar sobre la transacción (`identificadorTransaccion`, `identificadorPago`, `numeroOperacion`, `numeroLote`).
- **Enriquecimiento:** datos que el proveedor aporta y el cliente **no envió** (`nombreCliente`, `direccionCliente`, `email`, `deudas[]`).
- **Confirmación:** valores que el proveedor **asigna o ajusta** (`monto`, `moneda`, `codigoAprobacion`, `fechaOperacion`, `estado`, `saldo`).
- **Resultado específico:** datos propios de algunos convenios, expuestos en campos comunes o en `datosAdicionales` (`fee`, `importeOrigen`, `referenciaProveedor`, `clave`…).

**`respuesta` — control del resultado**
| Campo | Origen (convenios) | Por qué se puso (justificación) |
|---|---|---|
| `codigo` | Todos (secc. 6.3: `CodigoMensajeRetorno`, `codigo`, campo 39, `codigoRespuesta`, `status`, `estado`, `Codigo`…) | Da un **estado único y normalizado** para los 10 convenios. El cliente no necesita conocer el código crudo de cada proveedor: ramifica OK/KO con una sola regla. |
| `mensaje` | Todos (catálogo) | Traduce el código a **texto legible y homogéneo**. Evita que el cliente reciba mensajes distintos (o vacíos) de cada proveedor. |

**`respuesta.cliente` — quién paga (para comprobante e identificación)**
| Campo | Origen (convenios) | Por qué se puso (justificación) |
|---|---|---|
| `tipoDocumentoCliente` | UNIVCUSCO | Identifica el tipo de documento (DNI/RUC) para validar y mostrar. |
| `numeroDocumento` | ELSE, PAYTOPERU, UNIVCUSCO | Identificación del pagador en el comprobante y para conciliación. |
| `nombreCliente` | ELSE, ELECTRO_UCAYALI, SEAL, UNIVCUSCO, MUNICIPALIDAD_CUSCO, PAYTOPERU, WESTERN_UNION, UNIVCONTINENTAL | El usuario debe ver a nombre de quién está la deuda/pago; obligatorio para emitir el recibo. |
| `direccionCliente` | ELSE, SEAL | Dato de domicilio del servicio que se imprime en el comprobante. |
| `distritoCliente` | ELSE | Ubicación del suministro; complementa el recibo. |
| `email` | PAYTOPERU | Canal para **enviar confirmación/notificación** al cliente. |
| `codigoContribuyente` | MUNICIPALIDAD_CUSCO | Llave del contribuyente municipal en la orden de pago. |
| `numeroSuministro` | ELSE, ELECTRO_UCAYALI, SEAL | Identificador del servicio; el cliente lo usa para reintentar o pagar. |

**`respuesta.transaccion[]` — llaves de negocio (correlación, extorno, conciliación)**
| Campo | Origen (convenios) | Por qué se puso (justificación) |
|---|---|---|
| `identificadorTransaccion` | ELSE, SEAL | Trace de la operación; permite al cliente **referenciarla** luego (extorno, reimpresión, soporte). |
| `identificadorPago` | ELSE | Identificador del pago; **requisito para extornar/anular** esa operación. |
| `numeroOperacion` | ELECTRO_UCAYALI, UNIVCUSCO, PAYTOPERU, MUNICIPALIDAD_CUSCO, UNIVCONTINENTAL | Número de operación del **proveedor**; base de la conciliación y de las consultas de soporte. |
| `numeroComprobante` | ELSE, ELECTRO_UCAYALI, SEAL, UNIVCUSCO, UNIVCONTINENTAL, MUNICIPALIDAD_CUSCO | Recibo/factura/orden pagada; identifica el **documento** para el usuario y la conciliación. |
| `codigoAprobacion` | SEAL, CLARO, WESTERN_UNION | Código de **autorización** que acredita la operación; obligatorio en tarjeta e ISO8583. |
| `numeroLote` | WESTERN_UNION | Agrupa varias operaciones en un **lote**; necesario para confirmar/reimprimir sin operar una por una. |
| `referenciaProveedor` | YAGANASTE | Referencia del proveedor para **cuadrar** contra la caja. |
| `referenciaOperador` | YAGANASTE | Referencia del operador (caja) para conciliación bidireccional. |
| `clave` | PAYTOPERU | Clave de confirmación del pago; el cliente la entrega como constancia. |
| `parametros[]` | YAGANASTE, WESTERN_UNION | Bolsa de parámetros indexados `{indice, valor}`; permite devolver **datos específicos sin crear campos nuevos**. |

**`respuesta.transaccion[]` — resultado financiero (importes y moneda)**
| Campo | Origen (convenios) | Por qué se puso (justificación) |
|---|---|---|
| `monto` | Todos | Importe efectivamente **cotizado/pagado**; es el dato central de la operación. |
| `moneda` | PAYTOPERU, UNIVCUSCO, YAGANASTE, WESTERN_UNION | Desambigua el `monto` (PEN/USD); sin ella el importe es ambiguo. |
| `saldo` | YAGANASTE | Saldo resultante tras la operación (cuando aplica). |
| `fee` | YAGANASTE | Comisión/cargo por el servicio; el cliente debe conocer el costo total. |
| `importeOrigen` | YAGANASTE | Importe en la **moneda de origen** antes de la conversión. |
| `monedaOrigen` | YAGANASTE | Moneda correspondiente al `importeOrigen`. |

**`respuesta.transaccion[]` — fechas y estado**
| Campo | Origen (convenios) | Por qué se puso (justificación) |
|---|---|---|
| `fechaOperacion` | PAYTOPERU (y consultas) | Fecha en que se realizó la operación; para conciliación y constancia. |
| `fechaRegistro` | ELSE | Fecha en que el proveedor **registró** la operación; puede diferir de la de ejecución. |
| `fechaEmision` | ELSE | Fecha de emisión de la deuda/comprobante. |
| `fechaVencimiento` | Genérico (consultas) | Vigencia para pagar; el cliente muestra el vencimiento de la deuda. |
| `fechaVencimientoRsp` | ELSE | Vencimiento del comprobante según ELSE (dato propio de ese convenio). |
| `tipoIntegracion` | ELECTRO_UCAYALI | Indica la modalidad (Online/Offline) de la operación; trazabilidad de integración. |
| `estado` | YAGANASTE, MUNICIPALIDAD_CUSCO, WESTERN_UNION | Estado de **negocio** del proveedor (vendida, validada, estado de lote). |
| `glosa` | ELECTRO_UCAYALI, MUNICIPALIDAD_CUSCO, PAYTOPERU | Descripción corta del concepto/deuda; se usa en el comprobante. |
| `detalleConsulta` | ELSE | Detalle textual que ELSE devuelve en la consulta. |
| `ubicacionCanal` | PAYTOPERU | Ubicación física donde se operó; auditoría del canal. |
| `observacion` | PAYTOPERU | Observación libre del proveedor sobre el pago. |

**`respuesta.transaccion[]` — específicos de empresa y detalle de deuda**
| Campo | Origen (convenios) | Por qué se puso (justificación) |
|---|---|---|
| `indMasDeuda` | UNIVCUSCO | Indica si el cliente **tiene más deudas**; el cliente decide si continuar pagando. |
| `cantidadDocsDeuda` | UNIVCUSCO | Cuántos documentos de deuda existen; dimensiona el pago. |
| `datosEmpresa` | UNIVCUSCO | Datos de la empresa (universidad) que se imprimen en el comprobante. |
| `deudas[]` | ELSE, ELECTRO_UCAYALI, UNIVCUSCO, WESTERN_UNION, UNIVCONTINENTAL | En **consultas**, devuelve el detalle de cada deuda; es el resultado principal de un `CONSULTA_DEUDA`. |
| `datosAdicionales` | WESTERN_UNION (catálogo, serviceCharge, lote) | Bloque de **extensión**: lo muy específico del proveedor se aísla aquí sin romper el contrato (regla 5, secc. 6.4). |

**`respuesta.transaccion[].deudas[]` — detalle de cada deuda**
| Campo | Origen (convenios) | Por qué se puso (justificación) |
|---|---|---|
| `numeroComprobante` | Genérico | Identifica cada documento de deuda. |
| `descripcion` | Genérico | Descripción del concepto de la deuda. |
| `monto` | Genérico | Importe de la deuda. |
| `importeDeudaMinima` | UNIVCUSCO | Monto **mínimo** permitido para pago parcial. |
| `fechaEmision` | Genérico | Fecha de emisión de la deuda. |
| `fechaVencimiento` | Genérico | Vencimiento de la deuda. |
| `periodo` | UNIVCONTINENTAL | Periodo académico/cobro al que corresponde la deuda. |
| `numeroDocumento` | UNIVCONTINENTAL | Documento asociado a la deuda. |
| `tipoIntegracion` | ELECTRO_UCAYALI | Modalidad (Online/Offline) por deuda. |
| `indicadorRestriccPago` | UNIVCUSCO | Indica si la deuda **tiene restricción de pago**. |
| `cantidadSubconceptos` | UNIVCUSCO | Número de subconceptos que componen la deuda. |
| `glosa` | Genérico | Glosa informativa de la deuda. |

> **Criterio:** se incluye un campo en el contrato de salida solo si **al menos un convenio lo produce** y aporta valor al cliente. Lo que no encaja en el vocabulario común se envía en `datosAdicionales` o en `parametros[]`, preservando un contrato estable.

### 6.6 Campos que NO deben eliminarse del response

**Principio:** el response es **autocontenido** (patrón de banca: ISO8583 y REST devuelven el registro completo de la operación). Por eso conserva datos que también vienen en el request; sin embargo, hay campos **intocables** porque **solo el proveedor los produce** y el cliente **no los puede deducir ni reconstruir**. La regla antes de eliminar un campo es: **¿el proveedor lo devuelve y el cliente no lo tiene? Si es así, no se elimina.**

**Campos que NO deben eliminarse**
| Campo | Grupo | Por qué no puede eliminarse |
|---|---|---|
| `idTransaccion` | `cabecera` | **Llave de correlación** request ↔ response; sin él no se puede unir la petición con su respuesta en los logs. |
| `codigo`, `mensaje` | `respuesta` | **Estado** de la operación; es el resultado. |
| `identificadorTransaccion`, `identificadorPago` | `transaccion` | Traces que permiten **extornar/anular/reimprimir**. |
| `numeroOperacion`, `numeroComprobante`, `codigoAprobacion`, `numeroLote`, `clave` | `transaccion` | Los **asigna/confirma** el proveedor; base de la conciliación y de la constancia. |
| `monto`, `moneda` | `transaccion` | **Importe final** cobrado (puede incluir comisión/ajuste). |
| `fechaVencimientoRsp` | `transaccion` | **Solo ELSE** lo devuelve; es distinto de `fechaVencimiento`. |
| `fee`, `importeOrigen`, `monedaOrigen`, `saldo` | `transaccion` | **Datos financieros** que aporta el proveedor (YAGANASTE). |
| `estado`, `glosa`, `detalleConsulta`, `tipoIntegracion` | `transaccion` | **Estado/descripción** que entrega el proveedor. |
| `indMasDeuda`, `cantidadDocsDeuda`, `datosEmpresa` | `transaccion` | Datos propios de **UNIVCUSCO**; no vienen en el request. |
| `referenciaProveedor`, `referenciaOperador`, `observacion`, `parametros[]`, `datosAdicionales` | `transaccion` | Datos de **conciliación/confirmación** del proveedor. |
| `cliente.direccionCliente`, `cliente.distritoCliente`, `cliente.email` | `cliente` | **No vienen en el request**; solo los entrega el proveedor. |
| `cliente.nombreCliente` | `cliente` | El proveedor lo devuelve; en varios convenios el cliente **no lo envía**. |
| `deudas[]` y sus campos (`numeroComprobante`, `descripcion`, `monto`, `importeDeudaMinima`, `fechaEmision`, `fechaVencimiento`, `periodo`, `numeroDocumento`, `tipoIntegracion`, `indicadorRestriccPago`, `cantidadSubconceptos`, `glosa`) | `transaccion` | **Resultado principal de una consulta**; no existe en el request. |

**Campos de *identificación* de la entrada que SÍ se repiten (a propósito):**
`tipoDocumentoCliente`, `numeroDocumento`, `codigoContribuyente`, `numeroSuministro`, `numeroComprobante`, `datosEmpresa`, `ubicacionCanal`. Se conservan porque el response se **imprime y almacena por sí solo** (constancia/comprobante) y no debe depender de tener el request a mano. Los demás campos que coinciden con el request (`monto`, `moneda`, `fechaOperacion`, `identificadorTransaccion`, `identificadorPago`, `numeroOperacion`, `codigoAprobacion`…) se justifican como **correlación/confirmación** en la tabla anterior.

**Justificación**
- **Autocontenido (ISO8583/REST):** en banca el mensaje de respuesta es el **registro completo** de la operación, necesario para constancia, conciliación y contracargos.
- **Sin pérdida de información:** los campos de la tabla vienen del proveedor y no se pueden reconstruir; eliminarlos dejaría huecos en el comprobante y en la conciliación.
- **Trazabilidad:** `idTransaccion` garantiza que request y response se puedan unir aunque otros campos coincidan.

**Regla de oro:** solo se elimina un campo del response si el cliente **ya lo envió** en el request **y** el proveedor **no lo modifica ni asigna** **y** no es llave de correlación. Todo lo demás se mantiene.

---

## 7. Reglas de validación propuestas

1. `institucion` y `operador` son obligatorios; si no, error 400. La `cabecera` puede omitirse: el backend genera una con valores por defecto (§6.4). Si se envía, se validan sus campos.
2. `cabecera.idTransaccion`, cuando se envía, debe cumplir el formato `^\d{20}$` (`yyyyMMddHHmmss` + secuencia de 6). Si se omite, el backend lo genera. La unicidad lógica se garantiza con la reserva atómica de idempotencia por `idTransaccion`; reutilizarlo con otro convenio, operador o payload devuelve HTTP 409.
3. Si el convenio/operador no está soportado, error 400.
4. Validar los campos **obligatorios** de la matriz (sección 3) según `institucion` + `operador`; si falta alguno, error 400 con mensaje claro (ej. `"Falta el campo obligatorio: contrato"`).
5. Los campos **opcionales** ausentes no generan error (se usan valores por defecto del proveedor).
6. Los campos **no aplicables** se ignoran silenciosamente.
7. Nombres en **camelCase** y con el **vocabulario único** (sección 2).
8. `transaccion` puede traer **una o varias** operaciones; cada elemento se valida por separado.

---

## 8. Compatibilidad durante la transición (opcional)

Para no romper clientes actuales, se aceptan **alias** y se normalizan al campo común antes de validar y mapear (implementado en `Datos`):

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

## 9. Justificación de la arquitectura

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

## 10. ¿Es escalable?

Sí, con matices.

**Lo que la hace escalable:**
- El request **no cambia** al agregar convenios: se reutiliza el mismo objeto `datos`.
- La obligatoriedad de campos vive en **configuración** (`convenio.<X>.requerido.<OPERADOR>`), con valores por defecto seguros en código.
- Los **protocolos de salida son plug-in** (SOAP, REST, ISO8583) y los **mapeadores son por convenio**.
- Si una operación nueva necesita un campo adicional, se **agrega como campo opcional al grupo correspondiente** (`filtro`, `cliente` o `transaccion`), sin romper a los clientes existentes.

**Riesgos a gobernar:**
- El objeto `datos` es un **superset** y tiende a **crecer**; hay que mantener un **vocabulario único** y evitar duplicados o sinónimos.
- Conviene **versionar el contrato** (ej. `v1`) para evolucionar sin romper integraciones.
- Toda regla de obligatoriedad debe quedar **documentada y centralizada**.

---

## 11. ¿Qué pasa cuando se integra un convenio nuevo?

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

## 12. Rendimiento y concurrencia (picos de fin de mes)

**Escenario:** en el cierre de mes el volumen de consultas y pagos se multiplica y pueden llegar **miles de peticiones simultáneas**.

**Cómo responde hoy la arquitectura:**
- Quarkus sobre Vert.x (event loop no bloqueante) + Apache Camel con cliente HTTP **bloqueante** (`camel-quarkus-http`, Apache HttpClient) en las rutas `direct:`.
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

## 13. Arquitectura escalable, resiliencia y Redis (visión senior)

Esta sección define cómo evolucionar la arquitectura para que **no se degrade en picos de fin de mes**, sea **totalmente desacoplada**, **escalable horizontalmente** y permita **agregar convenios sin tocar el núcleo**.

### 13.1 Principios rectores
1. **No bloqueo (reactivo) de extremo a extremo:** el hilo del event loop nunca espera I/O de red.
2. **Stateless + estado compartido en Redis:** escala horizontal sin sesiones pegajosas.
3. **Aislamiento por convenio (bulkhead):** un proveedor lento no consume recursos de los demás.
4. **Resiliencia configurable por convenio** (timeout, retry, circuit breaker, bulkhead, fallback).
5. **Config-driven / plugin:** agregar convenio = configuración + mapeador.
6. **Backpressure y degradación controlada** antes que colapso.

### 13.2 Visión de capas (hexagonal)

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

### 13.3 Flujo reactivo de una petición
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

### 13.4 Concurrencia, paralelismo y multihilos
- **I/O saliente reactivo** con el cliente HTTP de Vert.x (`WebClient`) vía Mutiny; pocos hilos atienden muchas peticiones.
- **CPU liviana** (mapear JSON/XML/trama) en el event loop; si es pesada, pool de workers acotado.
- **Hilos virtuales (Java 21)** solo para integraciones que exijan código bloqueante (`@RunOnVirtualThread`), nunca `new Thread()` ni pools sin límite.
- **Paralelismo de `transaccion[]`** con concurrencia acotada:
  ```
  Uni.combine().all().unis(transacciones.map(this::procesar).toList()).with(agregador)
  ```
- **Bulkhead por convenio:** cada convenio con su propio límite de concurrencia y pool de conexiones.

### 13.5 Resiliencia (configurable por convenio)
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

### 13.6 Redis (estado compartido y rendimiento)

| Uso | Clave | TTL | Propósito |
|---|---|---|---|
| **Caché de token** | `token:<convenio>` | según expiración | Comparte el token entre réplicas (hoy está en memoria). |
| **Caché de consultas** | `consulta:<convenio>:<clave>` | 10–60 s | Reduce llamadas en picos. |
| **Idempotencia de pagos** | `idem:<convenio>:<idTransaccion>` | 24 h | Evita **doble pago**; usa `cabecera.idTransaccion` como llave. |
| **Lock distribuido** | `lock:<convenio>:<claveNegocio>` | corto | Serializa operaciones sobre el mismo contrato/suministro. |
| **Rate limiting** | `rl:<convenio>:<ventana>` | ventana | Protege al proveedor y al sistema. |
| **Estado de circuit breaker** | `cb:<convenio>` | — | (Opcional) estado compartido entre réplicas. |
| **Contadores ligeros** | `cnt:<convenio>:<dia>` | 1–2 días | Métricas rápidas. |

Tecnología: **`quarkus-redis-client`** (Vert.x Redis, no bloqueante), **`quarkus-redis-cache`** para caché declarativa y **Redisson** para locks avanzados. Redis en modo **Sentinel/Cluster** para alta disponibilidad.

### 13.7 Escalabilidad y performance
- **Horizontal stateless:** N réplicas detrás de balanceador; el estado vive en Redis.
- **Pool de conexiones por proveedor** con keep-alive (evita costo de TLS por llamada).
- **Backpressure:** colas acotadas; al saturarse, **load shedding** (429).
- **Caché** para amortiguar picos.
- **Dimensionamiento por pruebas de carga** (k6/JMeter) a la escala de fin de mes.

### 13.8 Extensibilidad: agregar convenios sin tocar el núcleo
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

### 13.9 Observabilidad
- **Métricas por convenio** (Micrometer/Prometheus): latencia p95/p99, errores, saturación de bulkhead, estado del CB, reintentos, cache hit ratio.
- **Tracing distribuido** (OpenTelemetry) con `traceId`/`X-Request-Id` de extremo a extremo, incluida la llamada al proveedor.
- **Logs estructurados** (JSON en prod), **health checks** (liveness/readiness, Redis, cada convenio) y **alertas de fin de mes**.

### 13.10 Roadmap por fases
| Fase | Entregable | Impacto |
|---|---|---|
| **1** | Request único agrupado + validación por matriz | Contrato estable |
| **2** | Cliente HTTP **reactivo** + orquestador `Uni` | Performance/concurrencia |
| **3** | **Resiliencia por convenio** (timeout, retry, CB, bulkhead, fallback) | Aislamiento y estabilidad |
| **4** | **Redis**: caché + **idempotencia de pagos** + locks | Escala horizontal, anti doble pago |
| **5** | **Bulkhead + rate limit + load shedding** | Resistencia a picos |
| **6** | **Observabilidad** + pruebas de carga | Visibilidad y dimensionamiento |
| **7** | **Descriptor declarativo** de convenios | Agregar convenios casi sin código |

> **Estado actual:** Fase 1 implementada. Fase 3 implementada por convenio (circuito, bulkhead y rate limit). Fase 4 parcial: idempotencia durable atómica por `idTransaccion` y caché de token en Redis; quedan el caché de consultas y los locks distribuidos. Fases 2, 5, 6 y 7 pendientes.

### 13.11 Trade-offs y riesgos
- **Reactivo vs virtual threads:** reactivo es más eficiente pero más complejo; virtual threads son más simples. Recomendación: **reactivo en el I/O saliente**, virtual threads solo para código bloqueante inevitable.
- **Circuit breaker distribuido en Redis:** más consistente pero con latencia y dependencia; alternativa: CB **por instancia** (más simple). Decidir por criticidad.
- **Caché de consultas:** mejora performance pero puede servir datos "viejos"; TTL corto y clave por convenio+operación.
- **Reintentos en pagos:** riesgo de doble cobro → siempre con **idempotencia**.
- **Redis como dependencia crítica:** requiere alta disponibilidad y plan de degradación (modo sin caché).

---

## 14. Resumen para negocio

- **Un solo request** para los **10 convenios** y todas sus operaciones.
- El objeto `datos` está **ordenado en tres grupos**: `filtro` (qué buscar), `cliente` (quién paga) y `transaccion` (qué operación).
- Cada campo es **obligatorio u opcional según la operación**.
- El backend sigue traduciendo al formato de cada proveedor (SOAP, REST, ISO8583); el cliente solo conoce **un idioma**.
- Agregar un convenio nuevo = mapear los campos comunes a su formato, **sin cambiar el request de entrada**.
