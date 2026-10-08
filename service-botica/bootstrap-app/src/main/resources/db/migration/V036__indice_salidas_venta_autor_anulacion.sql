CREATE INDEX ix_movimiento_salida_venta_documento
    ON sch_inventario.movimiento_inventario (tenant_id, documento_uuid)
    WHERE tipo_movimiento = 'SALIDA_VENTA' AND documento_tipo = 'VENTA';

ALTER TABLE sch_venta.venta
    ADD CONSTRAINT ck_venta_anulacion_autor
    CHECK (estado <> 'ANULADA' OR (anulada_por_usuario_id IS NOT NULL AND motivo_anulacion IS NOT NULL));
