-- Separa la tabla usuario (persona + relacion con tenant) en identidad (persona, global)
-- y membership (relacion identidad-tenant: rol, estado, credenciales).
-- No hay datos productivos que preservar: se recrea el esquema sin backfill.

CREATE TABLE sch_seguridad.identidad (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    email CITEXT NOT NULL,
    username CITEXT,
    tipo_documento VARCHAR(20),
    numero_documento VARCHAR(30),
    nombres VARCHAR(150),
    apellidos VARCHAR(180),
    telefono VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_identidad PRIMARY KEY (id),
    CONSTRAINT uk_seg_identidad_uuid UNIQUE (uuid_publico)
);

CREATE UNIQUE INDEX uk_seg_identidad_email ON sch_seguridad.identidad(email);
CREATE UNIQUE INDEX uk_seg_identidad_username ON sch_seguridad.identidad(username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX uk_seg_identidad_documento ON sch_seguridad.identidad(tipo_documento, numero_documento)
    WHERE numero_documento IS NOT NULL;

CREATE TABLE sch_seguridad.membership (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    identidad_id BIGINT NOT NULL,
    nombre_mostrar VARCHAR(250),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    bloqueado_hasta TIMESTAMPTZ,
    mfa_requerido BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_cambio_credencial BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_membership PRIMARY KEY (id),
    CONSTRAINT uk_seg_membership_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_membership_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_seg_membership_tenant_identidad UNIQUE (tenant_id, identidad_id),
    CONSTRAINT fk_seg_membership_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_seg_membership_identidad FOREIGN KEY (identidad_id) REFERENCES sch_seguridad.identidad(id),
    CONSTRAINT ck_seg_membership_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO','SUSPENDIDO'))
);

ALTER TABLE sch_seguridad.credencial_local DROP CONSTRAINT IF EXISTS fk_seg_credencial_usuario;
ALTER TABLE sch_seguridad.credencial_local RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.credencial_local
    ADD CONSTRAINT fk_seg_credencial_membership FOREIGN KEY (membership_id) REFERENCES sch_seguridad.membership(id);

ALTER TABLE sch_seguridad.usuario_rol_ambito DROP CONSTRAINT fk_seg_ura_usuario;
ALTER TABLE sch_seguridad.usuario_rol_ambito RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.usuario_rol_ambito
    ADD CONSTRAINT fk_seg_ura_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

DROP INDEX IF EXISTS sch_seguridad.ix_seg_ura_usuario;
CREATE INDEX ix_seg_ura_membership ON sch_seguridad.usuario_rol_ambito(tenant_id, membership_id, estado);

DROP INDEX IF EXISTS sch_seguridad.uk_seg_ura_activa;
CREATE UNIQUE INDEX uk_seg_ura_activa ON sch_seguridad.usuario_rol_ambito(
    tenant_id, membership_id, rol_id, tipo_ambito,
    COALESCE(empresa_id, 0), COALESCE(establecimiento_id, 0),
    COALESCE(almacen_id, 0), COALESCE(terminal_id, 0)
) WHERE estado = 'ACTIVO';

ALTER TABLE sch_seguridad.sesion_usuario DROP CONSTRAINT fk_seg_sesion_usuario;
ALTER TABLE sch_seguridad.sesion_usuario RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.sesion_usuario
    ADD CONSTRAINT fk_seg_sesion_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

DROP INDEX IF EXISTS sch_seguridad.ix_seg_sesion_usuario;
CREATE INDEX ix_seg_sesion_membership ON sch_seguridad.sesion_usuario(tenant_id, membership_id, estado);

ALTER TABLE sch_seguridad.identidad_externa DROP CONSTRAINT fk_seg_identidad_usuario;
ALTER TABLE sch_seguridad.identidad_externa RENAME COLUMN usuario_id TO identidad_id;
ALTER TABLE sch_seguridad.identidad_externa
    ADD CONSTRAINT fk_seg_identidad_externa_identidad
        FOREIGN KEY (identidad_id) REFERENCES sch_seguridad.identidad(id);

-- token_refresh y token_recuperacion_password (V020) tambien referencian usuario directamente;
-- el brief no las menciona explicitamente pero deben migrar igual que credencial_local/sesion_usuario
-- para poder eliminar sch_seguridad.usuario sin dejar objetos dependientes.
ALTER TABLE sch_seguridad.token_refresh DROP CONSTRAINT fk_seg_refresh_usuario;
ALTER TABLE sch_seguridad.token_refresh RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.token_refresh
    ADD CONSTRAINT fk_seg_refresh_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_seguridad.token_recuperacion_password DROP CONSTRAINT fk_seg_recuperacion_usuario;
ALTER TABLE sch_seguridad.token_recuperacion_password RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.token_recuperacion_password
    ADD CONSTRAINT fk_seg_recuperacion_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

DROP TABLE sch_seguridad.usuario;
