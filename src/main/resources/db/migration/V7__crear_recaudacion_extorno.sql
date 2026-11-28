CREATE TABLE recaudacion_extorno (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaccion_id BIGINT NOT NULL UNIQUE,
    identificador_pago_original NVARCHAR(100),
    numero_operacion_original NVARCHAR(100),
    fecha_operacion_original DATETIMEOFFSET(6),
    motivo NVARCHAR(300),
    estado NVARCHAR(30),
    codigo_aprobacion NVARCHAR(30),
    fecha_extorno DATETIMEOFFSET(6),
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_extorno_transaccion FOREIGN KEY (transaccion_id)
        REFERENCES recaudacion_transaccion(id)
);

CREATE INDEX idx_recaudacion_extorno_operacion_original ON recaudacion_extorno (numero_operacion_original);
