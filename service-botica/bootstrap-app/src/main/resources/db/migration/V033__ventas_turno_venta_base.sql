ALTER TABLE sch_venta.turno_caja
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_venta.venta
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36),
    ADD COLUMN huella_solicitud VARCHAR(64);

ALTER TABLE sch_venta.venta_linea
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36),
    ADD COLUMN uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    ADD CONSTRAINT uk_venta_linea_uuid UNIQUE (uuid_publico);

ALTER TABLE sch_venta.medio_pago
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

CREATE TABLE sch_venta.secuencia_operacion (
    tenant_id       BIGINT NOT NULL,
    terminal_id     BIGINT NOT NULL,
    ultimo_numero   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_secuencia_operacion PRIMARY KEY (tenant_id, terminal_id),
    CONSTRAINT fk_secuencia_operacion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_secuencia_operacion_terminal FOREIGN KEY (terminal_id) REFERENCES sch_organizacion.terminal_pos(id),
    CONSTRAINT ck_secuencia_operacion_ultimo CHECK (ultimo_numero >= 0)
);
