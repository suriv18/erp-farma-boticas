-- IAM agnóstico de proveedor de identidad.
-- No fija todavía password local/JWT/OIDC; sí define usuarios, módulos, roles, permisos, ámbitos, sesiones y dispositivos.

CREATE TABLE sch_seguridad.usuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    tipo_documento VARCHAR(20),
    numero_documento VARCHAR(30),
    nombres VARCHAR(150),
    apellidos VARCHAR(180),
    username CITEXT,
    email CITEXT,
    telefono VARCHAR(40),
    nombre_mostrar VARCHAR(250),
    requiere_cambio_credencial BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_requerido BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login_at TIMESTAMPTZ,
    bloqueado_hasta TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_usuario PRIMARY KEY (id),
    CONSTRAINT uk_seg_usuario_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_usuario_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_seg_usuario_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_seg_usuario_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO','SUSPENDIDO'))
);

CREATE UNIQUE INDEX uk_seg_usuario_tenant_username ON sch_seguridad.usuario(tenant_id, username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX uk_seg_usuario_tenant_email ON sch_seguridad.usuario(tenant_id, email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_seg_usuario_tenant_documento ON sch_seguridad.usuario(tenant_id, tipo_documento, numero_documento) WHERE numero_documento IS NOT NULL;

CREATE TABLE sch_seguridad.identidad_externa (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    usuario_id BIGINT NOT NULL,
    provider VARCHAR(100) NOT NULL,
    subject VARCHAR(300) NOT NULL,
    issuer VARCHAR(500),
    email_claim CITEXT,
    ultimo_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_identidad_externa PRIMARY KEY (id),
    CONSTRAINT uk_seg_identidad_provider_subject UNIQUE (provider, subject),
    CONSTRAINT fk_seg_identidad_usuario FOREIGN KEY (usuario_id) REFERENCES sch_seguridad.usuario(id)
);

CREATE TABLE sch_seguridad.modulo_sistema (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    descripcion VARCHAR(500),
    orden INTEGER NOT NULL DEFAULT 0,
    es_activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_modulo PRIMARY KEY (id),
    CONSTRAINT uk_seg_modulo_codigo UNIQUE (codigo),
    CONSTRAINT ck_seg_modulo_orden CHECK (orden >= 0)
);

CREATE TABLE sch_seguridad.rol (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    tipo_rol VARCHAR(30) NOT NULL DEFAULT 'ESTABLECIMIENTO',
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_rol PRIMARY KEY (id),
    CONSTRAINT uk_seg_rol_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_rol_tenant_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT uk_seg_rol_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_seg_rol_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_seg_rol_tipo CHECK (tipo_rol IN ('GLOBAL','EMPRESA','ESTABLECIMIENTO','ALMACEN','TERMINAL')),
    CONSTRAINT ck_seg_rol_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_seguridad.permiso (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    modulo_id BIGINT NOT NULL,
    codigo VARCHAR(150) NOT NULL,
    recurso VARCHAR(100) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(500),
    es_critico BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_seg_permiso PRIMARY KEY (id),
    CONSTRAINT uk_seg_permiso_codigo UNIQUE (codigo),
    CONSTRAINT uk_seg_permiso_modulo_recurso_accion UNIQUE (modulo_id, recurso, accion),
    CONSTRAINT fk_seg_permiso_modulo FOREIGN KEY (modulo_id) REFERENCES sch_seguridad.modulo_sistema(id),
    CONSTRAINT ck_seg_permiso_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_seguridad.rol_permiso (
    tenant_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(100) NOT NULL,
    CONSTRAINT pk_seg_rol_permiso PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_seg_rol_permiso_rol FOREIGN KEY (tenant_id, rol_id) REFERENCES sch_seguridad.rol(tenant_id, id),
    CONSTRAINT fk_seg_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES sch_seguridad.permiso(id),
    CONSTRAINT ck_seg_rol_permiso_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_seguridad.usuario_rol_ambito (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    tipo_ambito VARCHAR(30) NOT NULL,
    empresa_id BIGINT,
    establecimiento_id BIGINT,
    almacen_id BIGINT,
    terminal_id BIGINT,
    vigente_desde TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vigente_hasta TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_usuario_rol_ambito PRIMARY KEY (id),
    CONSTRAINT uk_seg_usuario_rol_ambito_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_seg_ura_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT fk_seg_ura_rol FOREIGN KEY (tenant_id, rol_id) REFERENCES sch_seguridad.rol(tenant_id, id),
    CONSTRAINT fk_seg_ura_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_seg_ura_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_seg_ura_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_seg_ura_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_farmacia.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_seg_ura_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_seg_ura_estado CHECK (estado IN ('ACTIVO','INACTIVO','REVOCADO')),
    CONSTRAINT ck_seg_ura_scope CHECK (
        (tipo_ambito = 'GLOBAL' AND empresa_id IS NULL AND establecimiento_id IS NULL AND almacen_id IS NULL AND terminal_id IS NULL)
        OR (tipo_ambito = 'EMPRESA' AND empresa_id IS NOT NULL AND establecimiento_id IS NULL AND almacen_id IS NULL AND terminal_id IS NULL)
        OR (tipo_ambito = 'ESTABLECIMIENTO' AND empresa_id IS NOT NULL AND establecimiento_id IS NOT NULL AND almacen_id IS NULL AND terminal_id IS NULL)
        OR (tipo_ambito = 'ALMACEN' AND empresa_id IS NOT NULL AND establecimiento_id IS NOT NULL AND almacen_id IS NOT NULL AND terminal_id IS NULL)
        OR (tipo_ambito = 'TERMINAL' AND empresa_id IS NOT NULL AND establecimiento_id IS NOT NULL AND terminal_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_seg_ura_activa ON sch_seguridad.usuario_rol_ambito(
    tenant_id, usuario_id, rol_id, tipo_ambito,
    (COALESCE(empresa_id,0)), (COALESCE(establecimiento_id,0)), (COALESCE(almacen_id,0)), (COALESCE(terminal_id,0))
) WHERE estado = 'ACTIVO';

CREATE TABLE sch_seguridad.sesion_usuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_sesion UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    provider VARCHAR(100),
    auth_method VARCHAR(50),
    canal VARCHAR(30) NOT NULL DEFAULT 'WEB',
    ip_origen INET,
    user_agent VARCHAR(1000),
    dispositivo_ref UUID,
    login_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_uso_at TIMESTAMPTZ,
    expira_at TIMESTAMPTZ,
    logout_at TIMESTAMPTZ,
    revocado_at TIMESTAMPTZ,
    motivo_revocacion VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT pk_seg_sesion PRIMARY KEY (id),
    CONSTRAINT uk_seg_sesion_uuid UNIQUE (uuid_sesion),
    CONSTRAINT fk_seg_sesion_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_seg_sesion_canal CHECK (canal IN ('WEB','POS','MOBILE','API','BACKOFFICE','ECOMMERCE')),
    CONSTRAINT ck_seg_sesion_estado CHECK (estado IN ('ACTIVA','CERRADA','EXPIRADA','REVOCADA'))
);

CREATE TABLE sch_seguridad.dispositivo_tienda (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    terminal_id BIGINT,
    device_fingerprint_hash VARCHAR(300),
    certificado_thumbprint VARCHAR(300),
    version_agente VARCHAR(100),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    registrado_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_contacto_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_dispositivo_tienda PRIMARY KEY (id),
    CONSTRAINT uk_seg_dispositivo_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_seg_dispositivo_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_seg_dispositivo_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_farmacia.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_seg_dispositivo_estado CHECK (estado IN ('PENDIENTE','CONFIABLE','BLOQUEADO','REVOCADO'))
);

CREATE INDEX ix_seg_ura_usuario ON sch_seguridad.usuario_rol_ambito(tenant_id, usuario_id, estado);
CREATE INDEX ix_seg_sesion_usuario ON sch_seguridad.sesion_usuario(tenant_id, usuario_id, estado);
