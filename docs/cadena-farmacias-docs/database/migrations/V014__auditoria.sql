-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V014.

-- ============================================================================
-- 14. ESQUEMA: sch_auditoria (Bitácora Inmutable y Habeas Data Ley 29733)
-- ============================================================================

CREATE TABLE sch_auditoria.evento_auditoria (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT,
    establecimiento_id  BIGINT,
    actor_usuario_id    BIGINT,
    sesion_usuario_id   BIGINT,
    actor_subject       VARCHAR(300),
    actor_tipo          VARCHAR(30) NOT NULL DEFAULT 'USUARIO',
    aplicacion          VARCHAR(50) NOT NULL DEFAULT 'POS_RETAIL',
    modulo              VARCHAR(80) NOT NULL,
    accion              VARCHAR(120) NOT NULL,
    recurso_tipo        VARCHAR(100) NOT NULL,
    recurso_id          BIGINT,
    recurso_uuid        UUID,
    resultado           VARCHAR(20) NOT NULL,
    nivel_riesgo        VARCHAR(20) NOT NULL DEFAULT 'BAJO',
    ocurrido_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id      UUID,
    trace_id            VARCHAR(100),
    ip_origen           INET,
    user_agent          VARCHAR(1000),
    campos_modificados  JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata            JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_aud_evento PRIMARY KEY (id),
    CONSTRAINT uk_aud_evento_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_aud_evento_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_aud_evento_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_aud_evento_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_aud_evento_usuario FOREIGN KEY (tenant_id, actor_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id),
    CONSTRAINT fk_aud_evento_sesion FOREIGN KEY (sesion_usuario_id) REFERENCES sch_seguridad.sesion_usuario(id),
    CONSTRAINT ck_aud_evento_actor_tipo CHECK (actor_tipo IN ('USUARIO', 'SISTEMA', 'INTEGRACION_API', 'JOB_BATCH')),
    CONSTRAINT ck_aud_evento_resultado CHECK (resultado IN ('SUCCESS', 'DENIED', 'FAILURE')),
    CONSTRAINT ck_aud_evento_riesgo CHECK (nivel_riesgo IN ('BAJO', 'MEDIO', 'ALTO', 'CRITICO'))
);

CREATE INDEX ix_aud_evento_fecha ON sch_auditoria.evento_auditoria(tenant_id, ocurrido_at DESC);
CREATE INDEX ix_aud_evento_recurso ON sch_auditoria.evento_auditoria(tenant_id, recurso_tipo, recurso_uuid, ocurrido_at DESC);
CREATE INDEX ix_aud_evento_actor ON sch_auditoria.evento_auditoria(tenant_id, actor_usuario_id, ocurrido_at DESC);
CREATE INDEX ix_aud_evento_riesgo ON sch_auditoria.evento_auditoria(tenant_id, nivel_riesgo, ocurrido_at DESC) WHERE nivel_riesgo IN ('ALTO', 'CRITICO');

CREATE TABLE sch_auditoria.acceso_dato_sensible (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    evento_auditoria_id BIGINT NOT NULL,
    categoria_dato      VARCHAR(50) NOT NULL,
    operacion           VARCHAR(30) NOT NULL,
    sujeto_tipo         VARCHAR(40) NOT NULL DEFAULT 'PACIENTE',
    sujeto_id           BIGINT,
    justificacion       VARCHAR(1000) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_aud_acceso_sensible PRIMARY KEY (id),
    CONSTRAINT uk_aud_acceso_sensible_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_aud_acceso_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_aud_acceso_evento FOREIGN KEY (evento_auditoria_id) REFERENCES sch_auditoria.evento_auditoria(id) ON DELETE CASCADE,
    CONSTRAINT ck_aud_acceso_operacion CHECK (operacion IN ('VER', 'DESCARGAR', 'IMPRIMIR', 'EXPORTAR', 'MODIFICAR'))
);

CREATE INDEX ix_aud_acceso_sujeto ON sch_auditoria.acceso_dato_sensible(tenant_id, sujeto_id, created_at DESC);

CREATE TABLE sch_auditoria.exportacion_datos (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    evento_auditoria_id BIGINT NOT NULL,
    tipo_exportacion    VARCHAR(80) NOT NULL,
    formato             VARCHAR(30) NOT NULL DEFAULT 'EXCEL',
    cantidad_registros  BIGINT NOT NULL,
    filtro_resumen      JSONB NOT NULL DEFAULT '{}'::jsonb,
    hash_archivo        VARCHAR(200),
    estado              VARCHAR(20) NOT NULL DEFAULT 'SOLICITADA',
    generado_at         TIMESTAMPTZ,
    entregado_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_aud_exportacion PRIMARY KEY (id),
    CONSTRAINT uk_aud_exportacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_aud_exportacion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_aud_exportacion_evento FOREIGN KEY (evento_auditoria_id) REFERENCES sch_auditoria.evento_auditoria(id) ON DELETE CASCADE,
    CONSTRAINT ck_aud_exportacion_estado CHECK (estado IN ('SOLICITADA', 'GENERADA', 'ENTREGADA', 'DENEGADA', 'ERROR'))
);

CREATE INDEX ix_aud_exportacion_tenant ON sch_auditoria.exportacion_datos(tenant_id, created_at DESC);
