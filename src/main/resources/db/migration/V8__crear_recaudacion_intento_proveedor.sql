CREATE TABLE recaudacion_intento_proveedor (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    operacion_id BIGINT,
    id_transaccion NVARCHAR(20),
    convenio_id BIGINT,
    operador NVARCHAR(80),
    protocolo NVARCHAR(20),
    endpoint NVARCHAR(500),
    metodo NVARCHAR(10),
    intento INT,
    http_status INT,
    codigo_error NVARCHAR(50),
    mensaje_error NVARCHAR(500),
    duracion_ms BIGINT,
    circuit_breaker_estado NVARCHAR(20),
    request_json NVARCHAR(MAX),
    response_json NVARCHAR(MAX),
    fecha_intento DATETIMEOFFSET(6),
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_intento_operacion FOREIGN KEY (operacion_id)
        REFERENCES recaudacion_operacion(id),
    CONSTRAINT fk_intento_convenio FOREIGN KEY (convenio_id)
        REFERENCES convenio(id)
);

CREATE INDEX idx_intento_proveedor_transaccion ON recaudacion_intento_proveedor (id_transaccion);
CREATE INDEX idx_intento_proveedor_fecha ON recaudacion_intento_proveedor (fecha_intento);
