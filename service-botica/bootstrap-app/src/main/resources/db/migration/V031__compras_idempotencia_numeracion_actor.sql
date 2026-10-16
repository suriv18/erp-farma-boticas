CREATE SEQUENCE sch_abastecimiento.seq_orden_compra_numero;
CREATE SEQUENCE sch_abastecimiento.seq_recepcion_compra_numero;

ALTER TABLE sch_abastecimiento.recepcion_compra
    ADD COLUMN business_uuid UUID,
    ADD COLUMN huella_solicitud VARCHAR(64);

CREATE UNIQUE INDEX uk_recepcion_business_uuid
    ON sch_abastecimiento.recepcion_compra (tenant_id, business_uuid)
    WHERE business_uuid IS NOT NULL;

ALTER TABLE sch_abastecimiento.recepcion_compra_linea
    ADD COLUMN uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    ADD CONSTRAINT uk_recepcion_compra_linea_uuid UNIQUE (uuid_publico);

ALTER TABLE sch_abastecimiento.proveedor
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_abastecimiento.solicitud_compra
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_abastecimiento.solicitud_compra_linea
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_abastecimiento.orden_compra
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_abastecimiento.orden_compra_linea
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_abastecimiento.recepcion_compra
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_abastecimiento.recepcion_compra_linea
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);
