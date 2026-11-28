CREATE TABLE recaudacion_pago (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaccion_id BIGINT NOT NULL UNIQUE,
    medio_pago NVARCHAR(30),
    forma_pago NVARCHAR(30),
    cuenta_origen NVARCHAR(50),
    cuenta_destino NVARCHAR(50),
    numero_tarjeta_mask NVARCHAR(30),
    bin_adquiriente NVARCHAR(10),
    codigo_autorizacion NVARCHAR(30),
    codigo_moneda NVARCHAR(3),
    tipo_cambio NUMERIC(18,6),
    nombre_comercio NVARCHAR(100),
    numero_comercio NVARCHAR(30),
    numero_terminal NVARCHAR(30),
    fecha_captura DATETIMEOFFSET(6),
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_pago_transaccion FOREIGN KEY (transaccion_id)
        REFERENCES recaudacion_transaccion(id)
);

CREATE INDEX idx_recaudacion_pago_autorizacion ON recaudacion_pago (codigo_autorizacion);
