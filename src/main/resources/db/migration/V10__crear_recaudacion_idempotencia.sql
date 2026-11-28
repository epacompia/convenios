CREATE TABLE recaudacion_idempotencia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_transaccion NVARCHAR(20) NOT NULL UNIQUE,
    convenio_id BIGINT,
    operador NVARCHAR(80),
    estado NVARCHAR(20) NOT NULL,
    response_json NVARCHAR(MAX),
    hash_request NVARCHAR(64),
    expira_at DATETIMEOFFSET(6) NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_idempotencia_convenio FOREIGN KEY (convenio_id)
        REFERENCES convenio(id)
);

CREATE INDEX idx_idempotencia_expiracion ON recaudacion_idempotencia (expira_at);
