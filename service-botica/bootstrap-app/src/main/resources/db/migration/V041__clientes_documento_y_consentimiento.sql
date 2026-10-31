ALTER TABLE sch_venta.cliente
    ALTER COLUMN tipo_documento TYPE VARCHAR(1),
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36),
    ADD COLUMN motivo_bloqueo VARCHAR(300),
    ADD CONSTRAINT fk_cliente_tipo_documento FOREIGN KEY (tipo_documento)
        REFERENCES sch_catalogo.tipo_documento_identidad(codigo),
    ADD CONSTRAINT ck_cliente_documento_completo
        CHECK ((tipo_documento IS NULL) = (numero_documento IS NULL)),
    ADD CONSTRAINT ck_cliente_sin_identificar
        CHECK (tipo_cliente <> 'SIN_IDENTIFICAR' OR (tipo_documento IS NULL AND numero_documento IS NULL)),
    ADD CONSTRAINT ck_cliente_documento_dni
        CHECK (tipo_documento IS DISTINCT FROM '1' OR numero_documento ~ '^[0-9]{8}$'),
    ADD CONSTRAINT ck_cliente_documento_ruc
        CHECK (tipo_documento IS DISTINCT FROM '6' OR numero_documento ~ '^[0-9]{11}$'),
    ADD CONSTRAINT ck_cliente_motivo_bloqueo
        CHECK (estado <> 'BLOQUEADO' OR btrim(coalesce(motivo_bloqueo, '')) <> '');

CREATE TABLE sch_catalogo.finalidad_consentimiento (
    codigo          VARCHAR(50) NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     VARCHAR(400) NOT NULL,
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_finalidad_consentimiento PRIMARY KEY (codigo),
    CONSTRAINT ck_finalidad_consentimiento_codigo CHECK (codigo ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_finalidad_consentimiento_nombre CHECK (btrim(nombre) <> ''),
    CONSTRAINT ck_finalidad_consentimiento_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_finalidad_consentimiento_es_activo CHECK (es_activo IN ('0', '1'))
);

COMMENT ON TABLE sch_catalogo.finalidad_consentimiento IS
    'Finalidades de tratamiento de datos de clientes (ADR-012). Marketing y fidelización se consienten por separado; la base jurídica de cada finalidad está POR_VALIDAR.';

INSERT INTO sch_catalogo.finalidad_consentimiento (codigo, nombre, descripcion)
VALUES
    ('IDENTIFICACION_VENTA', 'Identificación en la venta',
     'Uso de los datos del cliente para identificarlo en una venta y en su comprobante.'),
    ('MARKETING', 'Marketing',
     'Envío de comunicaciones comerciales. No se infiere de una compra ni de una receta.'),
    ('FIDELIZACION', 'Fidelización',
     'Participación en programas de fidelización. Se consiente por separado del marketing.');

ALTER TABLE sch_venta.cliente_consentimiento
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ADD COLUMN texto_version VARCHAR(40) NOT NULL DEFAULT 'SIN_REGISTRO',
    ADD COLUMN revocado_by VARCHAR(36),
    DROP CONSTRAINT fk_cliente_consent_cliente,
    ADD CONSTRAINT fk_cliente_consent_cliente FOREIGN KEY (tenant_id, cliente_id)
        REFERENCES sch_venta.cliente(tenant_id, id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_cliente_consent_finalidad FOREIGN KEY (finalidad_codigo)
        REFERENCES sch_catalogo.finalidad_consentimiento(codigo),
    ADD CONSTRAINT ck_cliente_consent_revocacion
        CHECK ((estado = 'REVOCADO') = (revocado_at IS NOT NULL)),
    ADD CONSTRAINT ck_cliente_consent_revocado_por
        CHECK (estado <> 'REVOCADO' OR revocado_by IS NOT NULL);

ALTER TABLE sch_venta.cliente_consentimiento
    ALTER COLUMN texto_version DROP DEFAULT;
