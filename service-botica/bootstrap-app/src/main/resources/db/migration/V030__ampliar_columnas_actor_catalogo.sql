ALTER TABLE sch_catalogo.categoria_producto
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_catalogo.marca
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_catalogo.rubro_comercial
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_catalogo.sku_comercial
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_catalogo.sku_codigo_barra
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);
