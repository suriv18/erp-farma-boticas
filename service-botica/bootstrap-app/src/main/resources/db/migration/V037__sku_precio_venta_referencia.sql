ALTER TABLE sch_catalogo.sku_comercial
    ADD COLUMN precio_venta_referencia NUMERIC(18, 4),
    ADD CONSTRAINT ck_sku_precio_venta_referencia
        CHECK (precio_venta_referencia IS NULL OR precio_venta_referencia >= 0);
