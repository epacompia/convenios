CREATE TABLE recaudacion_transaccion (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    operacion_id BIGINT NOT NULL,
    identificador_transaccion NVARCHAR(100),
    identificador_pago NVARCHAR(100),
    numero_operacion NVARCHAR(100),
    numero_comprobante NVARCHAR(100),
    numero_trace NVARCHAR(50),
    monto NUMERIC(18,2),
    moneda NVARCHAR(3),
    tipo_pago NVARCHAR(30),
    estado NVARCHAR(30),
    codigo_aprobacion NVARCHAR(30),
    fecha_operacion DATETIMEOFFSET(6),
    fecha_registro DATETIMEOFFSET(6),
    referencia_proveedor NVARCHAR(100),
    referencia_operador NVARCHAR(100),
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_transaccion_operacion FOREIGN KEY (operacion_id)
        REFERENCES recaudacion_operacion(id)
);

CREATE INDEX idx_recaudacion_transaccion_operacion ON recaudacion_transaccion (operacion_id);
CREATE INDEX idx_recaudacion_transaccion_identificador ON recaudacion_transaccion (identificador_transaccion);
CREATE INDEX idx_recaudacion_transaccion_comprobante ON recaudacion_transaccion (numero_comprobante);
CREATE INDEX idx_recaudacion_transaccion_fecha ON recaudacion_transaccion (fecha_operacion);
