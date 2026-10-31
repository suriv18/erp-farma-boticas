-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V015.

-- ============================================================================
-- 15. ESQUEMA: sch_integracion (Outbox, Inbox, Store-Edge e Idempotencia)
-- ============================================================================

CREATE TABLE sch_integracion.servicio_externo (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT,
    codigo                      VARCHAR(80) NOT NULL,
    nombre                      VARCHAR(160) NOT NULL,
    tipo_servicio               VARCHAR(60) NOT NULL,
    base_url                    TEXT,
    requiere_auth               BOOLEAN NOT NULL DEFAULT TRUE,
    configuracion_no_secreta    JSONB NOT NULL DEFAULT '{}'::jsonb,
    secret_ref                  VARCHAR(300),
    timeout_ms                  INTEGER DEFAULT 5000,
    estado                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_int_servicio_externo PRIMARY KEY (id),
    CONSTRAINT uk_int_servicio_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_int_servicio_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_int_servicio_timeout CHECK (timeout_ms IS NULL OR timeout_ms > 0),
    CONSTRAINT ck_int_servicio_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'DEGRADADO')),
    CONSTRAINT ck_int_servicio_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_int_servicio_codigo ON sch_integracion.servicio_externo (tenant_id, codigo) NULLS NOT DISTINCT WHERE es_activo = '1';

CREATE TABLE sch_integracion.outbox_event (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    message_id      UUID NOT NULL DEFAULT uuidv7(),
    tenant_id       BIGINT NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_uuid  UUID NOT NULL,
    event_type      VARCHAR(150) NOT NULL,
    schema_version  INTEGER NOT NULL DEFAULT 1,
    payload         JSONB NOT NULL,
    headers         JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id  UUID,
    causation_id    UUID,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMPTZ,
    numero_intentos INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    estado          VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    ultimo_error    VARCHAR(1500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_int_outbox PRIMARY KEY (id),
    CONSTRAINT uk_int_outbox_message UNIQUE (message_id),
    CONSTRAINT fk_int_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_int_outbox_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'PUBLICADO', 'ERROR_REINTENTABLE', 'ERROR_DEFINITIVO'))
);

CREATE INDEX ix_int_outbox_worker ON sch_integracion.outbox_event(estado, next_attempt_at, occurred_at) WHERE estado IN ('PENDIENTE', 'ERROR_REINTENTABLE');
CREATE INDEX ix_int_outbox_aggregate ON sch_integracion.outbox_event(tenant_id, aggregate_type, aggregate_uuid, occurred_at DESC);

CREATE TABLE sch_integracion.inbox_message (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    message_id      UUID NOT NULL,
    tenant_id       BIGINT NOT NULL,
    source_system   VARCHAR(100) NOT NULL,
    message_type    VARCHAR(150) NOT NULL,
    schema_version  INTEGER NOT NULL DEFAULT 1,
    payload         JSONB NOT NULL,
    payload_hash    VARCHAR(200),
    received_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMPTZ,
    estado          VARCHAR(30) NOT NULL DEFAULT 'RECIBIDO',
    ultimo_error    VARCHAR(1500),
    CONSTRAINT pk_int_inbox PRIMARY KEY (id),
    CONSTRAINT uk_int_inbox_message UNIQUE (source_system, message_id),
    CONSTRAINT fk_int_inbox_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_int_inbox_estado CHECK (estado IN ('RECIBIDO', 'PROCESANDO', 'PROCESADO', 'IGNORADO_DUPLICADO', 'ERROR_REINTENTABLE', 'ERROR_DEFINITIVO'))
);

CREATE INDEX ix_int_inbox_worker ON sch_integracion.inbox_message(estado, received_at) WHERE estado IN ('RECIBIDO', 'ERROR_REINTENTABLE');

CREATE TABLE sch_integracion.sync_checkpoint (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    stream_name         VARCHAR(120) NOT NULL,
    last_sequence       BIGINT NOT NULL DEFAULT 0,
    last_message_id     UUID,
    last_synced_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_int_sync_checkpoint PRIMARY KEY (id),
    CONSTRAINT uk_int_sync_checkpoint UNIQUE (tenant_id, establecimiento_id, stream_name),
    CONSTRAINT fk_int_sync_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_int_sync_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id) ON DELETE CASCADE
);

CREATE TABLE sch_integracion.integracion_intento (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    servicio_externo_id BIGINT,
    sistema_destino     VARCHAR(80) NOT NULL,
    operacion           VARCHAR(120) NOT NULL,
    idempotency_key     VARCHAR(180) NOT NULL,
    correlation_id      UUID,
    request_ref         UUID,
    request_hash        VARCHAR(200),
    estado              VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    numero_intento      INTEGER NOT NULL DEFAULT 0,
    codigo_respuesta    VARCHAR(100),
    mensaje_respuesta   VARCHAR(1500),
    status_http         INTEGER,
    duracion_ms         INTEGER,
    next_attempt_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at        TIMESTAMPTZ,
    CONSTRAINT pk_int_integracion_intento PRIMARY KEY (id),
    CONSTRAINT uk_int_integracion_intento_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_int_integracion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_int_integracion_servicio FOREIGN KEY (servicio_externo_id) REFERENCES sch_integracion.servicio_externo(id),
    CONSTRAINT ck_int_integracion_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'CONFIRMADO', 'ERROR_REINTENTABLE', 'ERROR_DEFINITIVO'))
);

CREATE UNIQUE INDEX uk_int_integracion_idempotency ON sch_integracion.integracion_intento(tenant_id, sistema_destino, idempotency_key);
CREATE INDEX ix_int_integracion_reintento ON sch_integracion.integracion_intento(estado, next_attempt_at) WHERE estado IN ('PENDIENTE', 'ERROR_REINTENTABLE');
