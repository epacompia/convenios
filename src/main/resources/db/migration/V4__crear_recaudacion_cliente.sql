CREATE TABLE recaudacion_cliente (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    operacion_id BIGINT NOT NULL UNIQUE,
    tipo_documento NVARCHAR(10),
    numero_documento NVARCHAR(20),
    nombre NVARCHAR(200),
    email NVARCHAR(150),
    direccion NVARCHAR(300),
    distrito NVARCHAR(150),
    codigo_contribuyente NVARCHAR(20),
    numero_suministro NVARCHAR(50),
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_cliente_operacion FOREIGN KEY (operacion_id)
        REFERENCES recaudacion_operacion(id)
);

CREATE INDEX idx_recaudacion_cliente_documento ON recaudacion_cliente (numero_documento);
CREATE INDEX idx_recaudacion_cliente_contribuyente ON recaudacion_cliente (codigo_contribuyente);
CREATE INDEX idx_recaudacion_cliente_suministro ON recaudacion_cliente (numero_suministro);
