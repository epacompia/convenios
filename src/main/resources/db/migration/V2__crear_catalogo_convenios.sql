CREATE TABLE convenio (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    codigo NVARCHAR(50) NOT NULL UNIQUE,
    nombre NVARCHAR(100) NOT NULL,
    protocolo NVARCHAR(20) NOT NULL,
    url_base NVARCHAR(500),
    auth_tipo NVARCHAR(30),
    activo BIT NOT NULL DEFAULT 1,
    timeout_ms INT,
    reintentos INT,
    reintentos_delay_ms INT,
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6)
);

CREATE TABLE convenio_operador (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    convenio_id BIGINT NOT NULL,
    operador NVARCHAR(80) NOT NULL,
    ruta NVARCHAR(300),
    metodo_http NVARCHAR(10),
    mti NVARCHAR(4),
    processing_code NVARCHAR(6),
    con_monto BIT,
    requiere_aprobacion BIT,
    timeout_ms INT,
    reintentos INT,
    cb_umbral INT,
    cb_espera_ms INT,
    activo BIT NOT NULL DEFAULT 1,
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT uq_convenio_operador UNIQUE (convenio_id, operador),
    CONSTRAINT fk_convenio_operador_convenio FOREIGN KEY (convenio_id) REFERENCES convenio(id)
);

CREATE INDEX idx_convenio_operador_convenio ON convenio_operador (convenio_id, activo);

ALTER TABLE recaudacion_operacion ADD convenio_id BIGINT;
ALTER TABLE recaudacion_operacion
    ADD CONSTRAINT fk_recaudacion_operacion_convenio
    FOREIGN KEY (convenio_id) REFERENCES convenio(id);
CREATE INDEX idx_recaudacion_operacion_convenio ON recaudacion_operacion (convenio_id);
