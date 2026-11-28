CREATE TABLE recaudacion_operacion (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_transaccion NVARCHAR(20) NOT NULL UNIQUE,
    institucion NVARCHAR(50) NOT NULL,
    operador NVARCHAR(80) NOT NULL,
    id_sesion NVARCHAR(50),
    cod_agencia NVARCHAR(20),
    cod_usuario NVARCHAR(50),
    ip NVARCHAR(45),
    fecha_peticion DATETIMEOFFSET(6),
    id_canal NVARCHAR(20),
    id_aplicacion NVARCHAR(20),
    codigo NVARCHAR(10),
    mensaje NVARCHAR(500),
    estado NVARCHAR(20) NOT NULL,
    duracion_ms BIGINT,
    request_json NVARCHAR(MAX),
    response_json NVARCHAR(MAX),
    fecha_registro DATETIMEOFFSET(6) NOT NULL,
    fecha_actualizacion DATETIMEOFFSET(6)
);

CREATE INDEX idx_recaudacion_operacion_institucion
    ON recaudacion_operacion (institucion, operador);

CREATE INDEX idx_recaudacion_operacion_fecha
    ON recaudacion_operacion (fecha_registro);
