CREATE TABLE recaudacion_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nivel NVARCHAR(10) NOT NULL,
    logger NVARCHAR(200),
    mensaje NVARCHAR(MAX) NOT NULL,
    trace_id NVARCHAR(64),
    id_transaccion NVARCHAR(20),
    convenio_id BIGINT,
    operador NVARCHAR(80),
    contexto_json NVARCHAR(MAX),
    fecha_log DATETIMEOFFSET(6) NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_log_convenio FOREIGN KEY (convenio_id)
        REFERENCES convenio(id)
);

CREATE INDEX idx_recaudacion_log_fecha ON recaudacion_log (fecha_log);
CREATE INDEX idx_recaudacion_log_trace ON recaudacion_log (trace_id);
CREATE INDEX idx_recaudacion_log_transaccion ON recaudacion_log (id_transaccion);
