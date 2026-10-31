-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V017.

CREATE TABLE sch_app.plantilla_notificacion (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT,
    codigo                      VARCHAR(80) NOT NULL,
    nombre                      VARCHAR(160) NOT NULL,
    canal                       VARCHAR(40) NOT NULL,
    asunto                      VARCHAR(250),
    cuerpo                      TEXT NOT NULL,
    variables                   JSONB NOT NULL DEFAULT '[]'::jsonb,
    contiene_datos_sensibles    BOOLEAN NOT NULL DEFAULT FALSE,
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_app_plantilla_notificacion PRIMARY KEY (id),
    CONSTRAINT uk_app_plantilla_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_app_plantilla_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_app_plantilla_canal CHECK (canal IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'IN_APP', 'WEBHOOK')),
    CONSTRAINT ck_app_plantilla_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_app_plantilla_codigo ON sch_app.plantilla_notificacion (tenant_id, codigo) NULLS NOT DISTINCT WHERE es_activo = '1';

CREATE TABLE sch_app.notificacion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    plantilla_id        BIGINT,
    destinatario_tipo   VARCHAR(30) NOT NULL,
    destinatario_id     BIGINT,
    canal               VARCHAR(40) NOT NULL,
    destinatario        VARCHAR(320) NOT NULL,
    asunto              VARCHAR(250),
    mensaje_sanitizado  TEXT,
    payload_cifrado     BYTEA,
    estado              VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada    TIMESTAMPTZ,
    fecha_envio         TIMESTAMPTZ,
    provider_message_id VARCHAR(200),
    numero_intentos     INTEGER NOT NULL DEFAULT 0,
    ultimo_error        VARCHAR(1500),
    metadata            JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_notificacion PRIMARY KEY (id),
    CONSTRAINT uk_app_notificacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_app_notificacion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_app_notificacion_plantilla FOREIGN KEY (plantilla_id) REFERENCES sch_app.plantilla_notificacion(id),
    CONSTRAINT ck_app_notificacion_canal CHECK (canal IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'IN_APP', 'WEBHOOK')),
    CONSTRAINT ck_app_notificacion_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'ENVIADA', 'ENTREGADA', 'LEIDA', 'ERROR_REINTENTABLE', 'ERROR_DEFINITIVO', 'CANCELADA'))
);

CREATE INDEX ix_app_notificacion_cola ON sch_app.notificacion(tenant_id, estado, fecha_programada) WHERE estado IN ('PENDIENTE', 'ERROR_REINTENTABLE');
CREATE INDEX ix_app_notificacion_destinatario ON sch_app.notificacion(tenant_id, destinatario, created_at DESC);

CREATE TABLE sch_app.feature_flag (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo              VARCHAR(80) NOT NULL,
    nombre              VARCHAR(160) NOT NULL,
    descripcion         VARCHAR(500),
    habilitado_default  BOOLEAN NOT NULL DEFAULT FALSE,
    aplicacion          VARCHAR(40),
    reglas              JSONB NOT NULL DEFAULT '{}'::jsonb,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_app_feature_flag PRIMARY KEY (id),
    CONSTRAINT ck_app_feature_flag_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_app_feature_flag_codigo ON sch_app.feature_flag(codigo) WHERE es_activo = '1';

CREATE TABLE sch_app.app_version (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    aplicacion          VARCHAR(40) NOT NULL,
    plataforma          VARCHAR(30) NOT NULL,
    version             VARCHAR(50) NOT NULL,
    build_number        VARCHAR(50),
    minima_compatible   VARCHAR(50) NOT NULL,
    es_obligatoria      BOOLEAN NOT NULL DEFAULT FALSE,
    url_descarga        TEXT,
    notas_version       TEXT,
    fecha_publicacion   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_app_version PRIMARY KEY (id),
    CONSTRAINT ck_app_version_plataforma CHECK (plataforma IN ('WEB', 'PWA', 'ANDROID', 'IOS', 'WINDOWS', 'LINUX')),
    CONSTRAINT ck_app_version_estado CHECK (estado IN ('ACTIVO', 'OBSOLETO', 'RETIRADO')),
    CONSTRAINT ck_app_version_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_app_version_registro ON sch_app.app_version (aplicacion, plataforma, version, build_number) NULLS NOT DISTINCT WHERE es_activo = '1';
