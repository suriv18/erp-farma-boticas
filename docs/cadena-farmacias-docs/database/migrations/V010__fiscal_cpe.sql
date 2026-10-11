-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V010.

-- ============================================================================
-- 11. ESQUEMA: sch_facturacion (CPE UBL 2.1 SUNAT / OSE / CDRs y Notas)
-- ============================================================================

CREATE TABLE sch_facturacion.comprobante_electronico (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    venta_id                    BIGINT NOT NULL,
    tipo_cpe                    VARCHAR(20) NOT NULL,
    codigo_tipo_sunat           VARCHAR(2) NOT NULL,
    serie                       VARCHAR(4) NOT NULL,
    numero                      BIGINT NOT NULL,
    fecha_emision               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_vencimiento           DATE,
    moneda                      CHAR(3) NOT NULL DEFAULT 'PEN',
    cliente_tipo_documento      VARCHAR(2),
    cliente_numero_documento    VARCHAR(15),
    cliente_denominacion        VARCHAR(300) NOT NULL,
    cliente_direccion           VARCHAR(500),
    total_gravado               NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_exonerado             NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_inafecto              NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_gratuito              NUMERIC(18, 2) NOT NULL DEFAULT 0,
    descuento_global            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    impuesto_igv                NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_icbper                NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total                       NUMERIC(18, 2) NOT NULL,
    estado_fiscal               VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_GENERACION',
    hash_cpe                    VARCHAR(200),
    firma_digital_valor         TEXT,
    xml_uri                     TEXT,
    pdf_uri                     TEXT,
    cdr_uri                     TEXT,
    codigo_respuesta_sunat      VARCHAR(10),
    mensaje_respuesta_sunat     VARCHAR(1500),
    enviado_at                  TIMESTAMPTZ,
    aceptado_at                 TIMESTAMPTZ,
    anulado_at                  TIMESTAMPTZ,
    motivo_anulacion            VARCHAR(500),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_comprobante_electronico PRIMARY KEY (id),
    CONSTRAINT uk_comprobante_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_comprobante_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_comprobante_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_venta.venta(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_comprobante_tipo CHECK (tipo_cpe IN ('BOLETA', 'FACTURA')),
    CONSTRAINT ck_comprobante_sunat CHECK (
        (tipo_cpe = 'FACTURA' AND codigo_tipo_sunat = '01' AND serie ~ '^F[A-Z0-9]{3}$') OR
        (tipo_cpe = 'BOLETA'  AND codigo_tipo_sunat = '03' AND serie ~ '^B[A-Z0-9]{3}$')
    ),
    CONSTRAINT ck_comprobante_estado CHECK (estado_fiscal IN ('PENDIENTE_GENERACION', 'GENERADO', 'PENDIENTE_ENVIO', 'ENVIADO', 'ACEPTADO', 'OBSERVADO', 'RECHAZADO', 'ANULADO', 'ERROR_TECNICO')),
    CONSTRAINT ck_comprobante_totales CHECK (
        total_gravado >= 0 AND total_exonerado >= 0 AND total_inafecto >= 0 AND total_gratuito >= 0 AND
        descuento_global >= 0 AND impuesto_igv >= 0 AND total_icbper >= 0 AND total >= 0
    ),
    CONSTRAINT ck_comprobante_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_comprobante_numeracion_sunat ON sch_facturacion.comprobante_electronico(tenant_id, empresa_id, codigo_tipo_sunat, serie, numero) WHERE es_activo = '1';
CREATE INDEX ix_comprobante_venta ON sch_facturacion.comprobante_electronico(tenant_id, venta_id) WHERE es_activo = '1';
CREATE INDEX ix_comprobante_estado ON sch_facturacion.comprobante_electronico(tenant_id, estado_fiscal, fecha_emision DESC) WHERE es_activo = '1';

CREATE TABLE sch_facturacion.cpe_envio_intento (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    comprobante_id      BIGINT NOT NULL,
    numero_intento      INTEGER NOT NULL,
    proveedor_servicio  VARCHAR(50) NOT NULL,
    idempotency_key     VARCHAR(160),
    enviado_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respondido_at       TIMESTAMPTZ,
    resultado           VARCHAR(30) NOT NULL,
    status_http         INTEGER,
    codigo_respuesta    VARCHAR(100),
    mensaje_respuesta   VARCHAR(1500),
    metadata_sanitizada JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cpe_envio_intento PRIMARY KEY (id),
    CONSTRAINT uk_cpe_envio_intento UNIQUE (comprobante_id, numero_intento),
    CONSTRAINT fk_cpe_intento_comprobante FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, comprobante_id) REFERENCES sch_facturacion.comprobante_electronico(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_cpe_envio_resultado CHECK (resultado IN ('PENDIENTE', 'ACEPTADO', 'OBSERVADO', 'RECHAZADO', 'ERROR_CONEXION'))
);

CREATE INDEX ix_cpe_envio_fallidos ON sch_facturacion.cpe_envio_intento(tenant_id, resultado) WHERE resultado IN ('ERROR_CONEXION', 'PENDIENTE');

CREATE TABLE sch_facturacion.nota_credito_electronica (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    comprobante_origen_id   BIGINT NOT NULL,
    devolucion_id           BIGINT,
    codigo_tipo_sunat       VARCHAR(2) NOT NULL DEFAULT '07',
    serie                   VARCHAR(4) NOT NULL,
    numero                  BIGINT NOT NULL,
    motivo_codigo_sunat     VARCHAR(2) NOT NULL,
    motivo_sustento         VARCHAR(500) NOT NULL,
    fecha_emision           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    moneda                  CHAR(3) NOT NULL DEFAULT 'PEN',
    total_gravado           NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_exonerado         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_inafecto          NUMERIC(18, 2) NOT NULL DEFAULT 0,
    impuesto_igv            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_icbper            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total                   NUMERIC(18, 2) NOT NULL,
    estado_fiscal           VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_GENERACION',
    hash_cpe                VARCHAR(200),
    firma_digital_valor     TEXT,
    xml_uri                 TEXT,
    pdf_uri                 TEXT,
    cdr_uri                 TEXT,
    codigo_respuesta_sunat  VARCHAR(10),
    mensaje_respuesta_sunat VARCHAR(1500),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_nota_credito PRIMARY KEY (id),
    CONSTRAINT uk_nota_credito_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_nota_credito_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_nc_comprobante_origen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, comprobante_origen_id) REFERENCES sch_facturacion.comprobante_electronico(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_nc_devolucion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, devolucion_id) REFERENCES sch_venta.devolucion_comercial(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_nc_serie CHECK (serie ~ '^[FB][A-Z0-9]{3}$'),
    CONSTRAINT ck_nc_motivo_sunat CHECK (motivo_codigo_sunat IN ('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13')),
    CONSTRAINT ck_nc_estado CHECK (estado_fiscal IN ('PENDIENTE_GENERACION', 'GENERADO', 'PENDIENTE_ENVIO', 'ENVIADO', 'ACEPTADO', 'OBSERVADO', 'RECHAZADO', 'ANULADO', 'ERROR_TECNICO')),
    CONSTRAINT ck_nc_totales CHECK (total_gravado >= 0 AND total_exonerado >= 0 AND total_inafecto >= 0 AND impuesto_igv >= 0 AND total_icbper >= 0 AND total >= 0),
    CONSTRAINT ck_nc_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_nota_credito_numeracion ON sch_facturacion.nota_credito_electronica(tenant_id, empresa_id, serie, numero) WHERE es_activo = '1';
CREATE INDEX ix_nota_credito_cpe_origen ON sch_facturacion.nota_credito_electronica(tenant_id, comprobante_origen_id) WHERE es_activo = '1';
