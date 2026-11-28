CREATE TABLE recaudacion_deuda (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaccion_id BIGINT NOT NULL,
    numero_comprobante NVARCHAR(100),
    descripcion NVARCHAR(300),
    monto NUMERIC(18,2),
    importe_deuda_minima NUMERIC(18,2),
    fecha_emision DATE,
    fecha_vencimiento DATE,
    periodo NVARCHAR(20),
    tipo_integracion NVARCHAR(20),
    indicador_restriccion_pago NVARCHAR(5),
    cantidad_subconceptos INT,
    glosa NVARCHAR(300),
    created_at DATETIMEOFFSET(6) NOT NULL,
    updated_at DATETIMEOFFSET(6),
    CONSTRAINT fk_deuda_transaccion FOREIGN KEY (transaccion_id)
        REFERENCES recaudacion_transaccion(id)
);

CREATE INDEX idx_recaudacion_deuda_transaccion ON recaudacion_deuda (transaccion_id);
CREATE INDEX idx_recaudacion_deuda_comprobante ON recaudacion_deuda (numero_comprobante);
CREATE INDEX idx_recaudacion_deuda_vencimiento ON recaudacion_deuda (fecha_vencimiento);
