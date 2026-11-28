CREATE TABLE recaudacion_conciliacion (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    fecha_conciliacion DATE NOT NULL,
    convenio_id BIGINT NOT NULL,
    total_operaciones BIGINT NOT NULL,
    total_aprobadas BIGINT NOT NULL,
    total_rechazadas BIGINT NOT NULL,
    monto_total NUMERIC(18,2) NOT NULL,
    moneda NVARCHAR(3) NOT NULL,
    estado NVARCHAR(20) NOT NULL,
    archivo_ref NVARCHAR(300),
    observacion NVARCHAR(MAX),
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT uq_conciliacion_fecha_convenio UNIQUE (fecha_conciliacion, convenio_id),
    CONSTRAINT fk_conciliacion_convenio FOREIGN KEY (convenio_id)
        REFERENCES convenio(id)
);

CREATE INDEX idx_conciliacion_estado ON recaudacion_conciliacion (estado);
