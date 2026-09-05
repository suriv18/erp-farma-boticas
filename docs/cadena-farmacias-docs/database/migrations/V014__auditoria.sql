-- Auditoría funcional y de seguridad. Separada de logs técnicos; evita dumps indiscriminados de datos sensibles.

CREATE TABLE sch_auditoria.evento_auditoria (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT,
    establecimiento_id BIGINT,
    actor_usuario_uuid UUID,
    actor_subject VARCHAR(300),
    sesion_uuid UUID,
    actor_tipo VARCHAR(30) NOT NULL DEFAULT 'USUARIO',
    aplicacion VARCHAR(50),
    accion VARCHAR(120) NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    recurso_tipo VARCHAR(100),
    recurso_uuid UUID,
    resultado VARCHAR(20) NOT NULL,
    nivel_riesgo VARCHAR(20),
    ocurrido_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id UUID,
    trace_id VARCHAR(100),
    ip_origen INET,
    user_agent VARCHAR(1000),
    campos_modificados JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_aud_evento PRIMARY KEY (id),
    CONSTRAINT uk_aud_evento_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_aud_evento_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_aud_evento_resultado CHECK (resultado IN ('SUCCESS','DENIED','FAILURE')),
    CONSTRAINT ck_aud_evento_riesgo CHECK (nivel_riesgo IS NULL OR nivel_riesgo IN ('BAJO','MEDIO','ALTO','CRITICO'))
);

CREATE TABLE sch_auditoria.acceso_dato_sensible (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    evento_auditoria_id BIGINT NOT NULL,
    categoria_dato VARCHAR(50) NOT NULL,
    operacion VARCHAR(30) NOT NULL,
    sujeto_ref UUID,
    justificacion VARCHAR(1000),
    CONSTRAINT pk_aud_acceso_sensible PRIMARY KEY (id),
    CONSTRAINT fk_aud_acceso_evento FOREIGN KEY (evento_auditoria_id) REFERENCES sch_auditoria.evento_auditoria(id),
    CONSTRAINT ck_aud_acceso_operacion CHECK (operacion IN ('VER','DESCARGAR','IMPRIMIR','EXPORTAR','MODIFICAR'))
);

CREATE TABLE sch_auditoria.exportacion_datos (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    evento_auditoria_id BIGINT NOT NULL,
    tipo_exportacion VARCHAR(80) NOT NULL,
    formato VARCHAR(30),
    cantidad_registros BIGINT,
    filtro_resumen JSONB NOT NULL DEFAULT '{}'::jsonb,
    hash_archivo VARCHAR(200),
    estado VARCHAR(20) NOT NULL,
    generado_at TIMESTAMPTZ,
    entregado_at TIMESTAMPTZ,
    CONSTRAINT pk_aud_exportacion PRIMARY KEY (id),
    CONSTRAINT uk_aud_exportacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_aud_exportacion_evento FOREIGN KEY (evento_auditoria_id) REFERENCES sch_auditoria.evento_auditoria(id),
    CONSTRAINT ck_aud_exportacion_estado CHECK (estado IN ('SOLICITADA','GENERADA','ENTREGADA','DENEGADA','ERROR'))
);

CREATE INDEX ix_aud_evento_fecha ON sch_auditoria.evento_auditoria(tenant_id, ocurrido_at DESC);
CREATE INDEX ix_aud_evento_recurso ON sch_auditoria.evento_auditoria(tenant_id, recurso_tipo, recurso_uuid, ocurrido_at DESC);
CREATE INDEX ix_aud_evento_actor ON sch_auditoria.evento_auditoria(tenant_id, actor_usuario_uuid, ocurrido_at DESC);
