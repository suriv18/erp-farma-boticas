CREATE TABLE sch_seguridad.credencial_local (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    usuario_id BIGINT NOT NULL,
    password_hash VARCHAR(500) NOT NULL,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMPTZ,
    requiere_cambio BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_credencial_local PRIMARY KEY (id),
    CONSTRAINT uk_seg_credencial_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_seg_credencial_usuario FOREIGN KEY (usuario_id) REFERENCES sch_seguridad.usuario(id),
    CONSTRAINT ck_seg_credencial_intentos CHECK (intentos_fallidos >= 0),
    CONSTRAINT ck_seg_credencial_estado CHECK (estado IN ('ACTIVA','INACTIVA','REVOCADA'))
);

CREATE TABLE sch_seguridad.token_refresh (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    sesion_uuid UUID NOT NULL,
    familia_uuid UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expira_at TIMESTAMPTZ NOT NULL,
    usado_at TIMESTAMPTZ,
    revocado_at TIMESTAMPTZ,
    reemplazado_por UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_token_refresh PRIMARY KEY (id),
    CONSTRAINT uk_seg_token_refresh_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_token_refresh_hash UNIQUE (token_hash),
    CONSTRAINT fk_seg_refresh_usuario FOREIGN KEY (tenant_id, usuario_id)
        REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT fk_seg_refresh_sesion FOREIGN KEY (sesion_uuid)
        REFERENCES sch_seguridad.sesion_usuario(uuid_sesion),
    CONSTRAINT ck_seg_refresh_expira CHECK (expira_at > created_at)
);

CREATE TABLE sch_seguridad.token_recuperacion_password (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expira_at TIMESTAMPTZ NOT NULL,
    consumido_at TIMESTAMPTZ,
    revocado_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_token_recuperacion PRIMARY KEY (id),
    CONSTRAINT uk_seg_token_recuperacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_token_recuperacion_hash UNIQUE (token_hash),
    CONSTRAINT fk_seg_recuperacion_usuario FOREIGN KEY (tenant_id, usuario_id)
        REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_seg_recuperacion_expira CHECK (expira_at > created_at)
);

CREATE INDEX ix_seg_refresh_sesion
    ON sch_seguridad.token_refresh(sesion_uuid, revocado_at, expira_at);
CREATE INDEX ix_seg_recuperacion_usuario
    ON sch_seguridad.token_recuperacion_password(tenant_id, usuario_id, expira_at);

INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, 'seguridad.credenciales.gestionar', 'CREDENCIAL_LOCAL', 'GESTIONAR',
       'Gestionar credenciales locales',
       'Permite crear o reemplazar la credencial local de un usuario.', TRUE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema m
 WHERE m.codigo = 'SEGURIDAD'
ON CONFLICT (codigo) DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';
