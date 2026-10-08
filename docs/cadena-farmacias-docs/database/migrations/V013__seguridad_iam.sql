-- Fuente: cadena_farmacias_postgresql18.sql
-- Migracion modular V013.

-- ============================================================================
-- 2. ESQUEMA: sch_seguridad (IAM Base: Identidad, Membership, Modulo, Rol, Permiso)
-- ============================================================================

-- Identidad: persona global, independiente de cualquier tenant. Una persona
-- tiene una unica identidad aunque tenga cuentas (membership) en varios tenants.
CREATE TABLE sch_seguridad.identidad (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    email               CITEXT NOT NULL,
    username            CITEXT,
    tipo_documento      VARCHAR(20),
    numero_documento    VARCHAR(30),
    nombres             VARCHAR(150),
    apellidos           VARCHAR(180),
    telefono            VARCHAR(40),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_seg_identidad PRIMARY KEY (id),
    CONSTRAINT uk_seg_identidad_uuid UNIQUE (uuid_publico),
    CONSTRAINT ck_seg_identidad_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_identidad_email ON sch_seguridad.identidad(email) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_seg_identidad_username ON sch_seguridad.identidad(username) WHERE username IS NOT NULL AND es_activo = '1';
CREATE UNIQUE INDEX uk_seg_identidad_documento ON sch_seguridad.identidad(tipo_documento, numero_documento)
    WHERE numero_documento IS NOT NULL AND es_activo = '1';

-- Membership: la cuenta de una identidad dentro de un tenant especifico
-- (rol de acceso, estado, MFA, bloqueo, etc. son todos por tenant).
CREATE TABLE sch_seguridad.membership (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    identidad_id                BIGINT NOT NULL,
    nombre_mostrar              VARCHAR(250),
    estado                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    bloqueado_hasta             TIMESTAMPTZ,
    mfa_requerido                BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_cambio_credencial  BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login_at             TIMESTAMPTZ,
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ,
    CONSTRAINT pk_seg_membership PRIMARY KEY (id),
    CONSTRAINT uk_seg_membership_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_membership_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_seg_membership_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_seg_membership_identidad FOREIGN KEY (identidad_id) REFERENCES sch_seguridad.identidad(id),
    CONSTRAINT ck_seg_membership_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO','SUSPENDIDO')),
    CONSTRAINT ck_seg_membership_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_membership_tenant_identidad ON sch_seguridad.membership(tenant_id, identidad_id) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.identidad_externa (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    identidad_id        BIGINT NOT NULL,
    provider            VARCHAR(100) NOT NULL,
    subject             VARCHAR(300) NOT NULL,
    issuer              VARCHAR(500),
    email_claim         CITEXT,
    ultimo_login_at     TIMESTAMPTZ,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_identidad_externa PRIMARY KEY (id),
    CONSTRAINT fk_seg_identidad_externa_identidad FOREIGN KEY (identidad_id) REFERENCES sch_seguridad.identidad(id) ON DELETE CASCADE,
    CONSTRAINT ck_seg_identidad_externa_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_identidad_externa_provider_subject ON sch_seguridad.identidad_externa(provider, subject) WHERE es_activo = '1';
CREATE INDEX ix_seg_identidad_externa_identidad ON sch_seguridad.identidad_externa(identidad_id) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.modulo_sistema (
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo      VARCHAR(80) NOT NULL,
    nombre      VARCHAR(160) NOT NULL,
    descripcion VARCHAR(500),
    orden       INTEGER NOT NULL DEFAULT 0,
    es_activo   CHAR(1) NOT NULL DEFAULT '1',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(15),
    CONSTRAINT pk_seg_modulo PRIMARY KEY (id),
    CONSTRAINT ck_seg_modulo_orden CHECK (orden >= 0),
    CONSTRAINT ck_seg_modulo_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_modulo_codigo ON sch_seguridad.modulo_sistema(codigo) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.rol (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id       BIGINT NOT NULL,
    codigo          VARCHAR(80) NOT NULL,
    nombre          VARCHAR(150) NOT NULL,
    descripcion     VARCHAR(500),
    tipo_rol        VARCHAR(30) NOT NULL DEFAULT 'ESTABLECIMIENTO',
    es_sistema      BOOLEAN NOT NULL DEFAULT FALSE,
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT pk_seg_rol PRIMARY KEY (id),
    CONSTRAINT uk_seg_rol_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_rol_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_seg_rol_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_seg_rol_tipo CHECK (tipo_rol IN ('GLOBAL','EMPRESA','ESTABLECIMIENTO','ALMACEN','TERMINAL')),
    CONSTRAINT ck_seg_rol_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT ck_seg_rol_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_rol_tenant_codigo ON sch_seguridad.rol(tenant_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.permiso (
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    modulo_id   BIGINT NOT NULL,
    codigo      VARCHAR(150) NOT NULL,
    recurso     VARCHAR(100) NOT NULL,
    accion      VARCHAR(50) NOT NULL,
    nombre      VARCHAR(180) NOT NULL,
    descripcion VARCHAR(500),
    es_critico  BOOLEAN NOT NULL DEFAULT FALSE,
    estado      VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo   CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_seg_permiso PRIMARY KEY (id),
    CONSTRAINT fk_seg_permiso_modulo FOREIGN KEY (modulo_id) REFERENCES sch_seguridad.modulo_sistema(id),
    CONSTRAINT ck_seg_permiso_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT ck_seg_permiso_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_permiso_codigo ON sch_seguridad.permiso(codigo) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_seg_permiso_modulo_recurso_accion ON sch_seguridad.permiso(modulo_id, recurso, accion) WHERE es_activo = '1';
CREATE INDEX ix_seg_permiso_modulo ON sch_seguridad.permiso(modulo_id) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.rol_permiso (
    tenant_id   BIGINT NOT NULL,
    rol_id      BIGINT NOT NULL,
    permiso_id  BIGINT NOT NULL,
    estado      VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo   CHAR(1) NOT NULL DEFAULT '1',
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_seg_rol_permiso PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_seg_rol_permiso_rol FOREIGN KEY (tenant_id, rol_id) REFERENCES sch_seguridad.rol(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_seg_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES sch_seguridad.permiso(id),
    CONSTRAINT ck_seg_rol_permiso_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT ck_seg_rol_permiso_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_seg_rol_permiso_permiso ON sch_seguridad.rol_permiso(permiso_id) WHERE es_activo = '1';

-- ============================================================================
-- 3. ESQUEMA: sch_seguridad (Autenticacion local: credenciales, refresh, recuperacion)
-- ============================================================================

CREATE TABLE sch_seguridad.credencial_local (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    membership_id           BIGINT NOT NULL,
    password_hash           VARCHAR(500) NOT NULL,
    intentos_fallidos       INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta         TIMESTAMPTZ,
    requiere_cambio         BOOLEAN NOT NULL DEFAULT TRUE,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    password_changed_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ,
    CONSTRAINT pk_seg_credencial_local PRIMARY KEY (id),
    CONSTRAINT uk_seg_credencial_membership UNIQUE (membership_id),
    CONSTRAINT fk_seg_credencial_membership FOREIGN KEY (membership_id) REFERENCES sch_seguridad.membership(id),
    CONSTRAINT ck_seg_credencial_intentos CHECK (intentos_fallidos >= 0),
    CONSTRAINT ck_seg_credencial_estado CHECK (estado IN ('ACTIVA','INACTIVA','REVOCADA'))
);

CREATE TABLE sch_seguridad.token_refresh (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    membership_id       BIGINT NOT NULL,
    sesion_uuid         UUID NOT NULL,
    familia_uuid        UUID NOT NULL,
    token_hash          CHAR(64) NOT NULL,
    expira_at           TIMESTAMPTZ NOT NULL,
    usado_at            TIMESTAMPTZ,
    revocado_at         TIMESTAMPTZ,
    reemplazado_por     UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_token_refresh PRIMARY KEY (id),
    CONSTRAINT uk_seg_token_refresh_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_token_refresh_hash UNIQUE (token_hash),
    CONSTRAINT fk_seg_refresh_membership FOREIGN KEY (tenant_id, membership_id)
        REFERENCES sch_seguridad.membership(tenant_id, id),
    CONSTRAINT ck_seg_refresh_expira CHECK (expira_at > created_at)
);

CREATE INDEX ix_seg_refresh_sesion ON sch_seguridad.token_refresh(sesion_uuid, revocado_at, expira_at);

CREATE TABLE sch_seguridad.token_recuperacion_password (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    membership_id       BIGINT NOT NULL,
    token_hash          CHAR(64) NOT NULL,
    expira_at           TIMESTAMPTZ NOT NULL,
    consumido_at        TIMESTAMPTZ,
    revocado_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_token_recuperacion PRIMARY KEY (id),
    CONSTRAINT uk_seg_token_recuperacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_token_recuperacion_hash UNIQUE (token_hash),
    CONSTRAINT fk_seg_recuperacion_membership FOREIGN KEY (tenant_id, membership_id)
        REFERENCES sch_seguridad.membership(tenant_id, id),
    CONSTRAINT ck_seg_recuperacion_expira CHECK (expira_at > created_at)
);

CREATE INDEX ix_seg_recuperacion_membership ON sch_seguridad.token_recuperacion_password(tenant_id, membership_id, expira_at);

-- ============================================================================
-- 4. ESQUEMA: sch_seguridad (Continuacion: Dispositivos, Ambitos y Sesiones)
-- ============================================================================

CREATE TABLE sch_seguridad.dispositivo_tienda (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    terminal_id                 BIGINT,
    device_fingerprint_hash     VARCHAR(300),
    certificado_thumbprint      VARCHAR(300),
    version_agente               VARCHAR(100),
    estado                       VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    es_activo                    CHAR(1) NOT NULL DEFAULT '1',
    registrado_at                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_contacto_at           TIMESTAMPTZ,
    CONSTRAINT pk_seg_dispositivo_tienda PRIMARY KEY (id),
    CONSTRAINT uk_seg_dispositivo_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_dispositivo_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_seg_dispositivo_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_seg_dispositivo_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_organizacion.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_seg_dispositivo_estado CHECK (estado IN ('PENDIENTE', 'CONFIABLE', 'BLOQUEADO', 'REVOCADO')),
    CONSTRAINT ck_seg_dispositivo_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_seg_dispositivo_busqueda ON sch_seguridad.dispositivo_tienda(tenant_id, empresa_id, establecimiento_id) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.usuario_rol_ambito (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    membership_id       BIGINT NOT NULL,
    rol_id              BIGINT NOT NULL,
    tipo_ambito         VARCHAR(30) NOT NULL,
    empresa_id          BIGINT,
    establecimiento_id  BIGINT,
    almacen_id          BIGINT,
    terminal_id         BIGINT,
    vigente_desde       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vigente_hasta       TIMESTAMPTZ,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_by          VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_seg_usuario_rol_ambito PRIMARY KEY (id),
    CONSTRAINT uk_seg_usuario_rol_ambito_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_seg_ura_membership FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_seg_ura_rol FOREIGN KEY (tenant_id, rol_id) REFERENCES sch_seguridad.rol(tenant_id, id),
    CONSTRAINT fk_seg_ura_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_seg_ura_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_seg_ura_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_seg_ura_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_organizacion.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_seg_ura_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_seg_ura_estado CHECK (estado IN ('ACTIVO', 'REVOCADO')),
    CONSTRAINT ck_seg_ura_es_activo CHECK (es_activo IN ('0', '1')),
    CONSTRAINT ck_seg_ura_scope CHECK (
        (tipo_ambito = 'GLOBAL'          AND empresa_id IS NULL     AND establecimiento_id IS NULL     AND almacen_id IS NULL     AND terminal_id IS NULL)
        OR (tipo_ambito = 'EMPRESA'      AND empresa_id IS NOT NULL AND establecimiento_id IS NULL     AND almacen_id IS NULL     AND terminal_id IS NULL)
        OR (tipo_ambito = 'ESTABLECIMIENTO' AND empresa_id IS NOT NULL AND establecimiento_id IS NOT NULL AND almacen_id IS NULL     AND terminal_id IS NULL)
        OR (tipo_ambito = 'ALMACEN'      AND empresa_id IS NOT NULL AND establecimiento_id IS NOT NULL AND almacen_id IS NOT NULL AND terminal_id IS NULL)
        OR (tipo_ambito = 'TERMINAL'     AND empresa_id IS NOT NULL AND establecimiento_id IS NOT NULL AND almacen_id IS NULL     AND terminal_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_seg_ura_activa ON sch_seguridad.usuario_rol_ambito(
    tenant_id, membership_id, rol_id, tipo_ambito,
    (COALESCE(empresa_id, 0)), (COALESCE(establecimiento_id, 0)),
    (COALESCE(almacen_id, 0)), (COALESCE(terminal_id, 0))
) WHERE estado = 'ACTIVO' AND es_activo = '1';

CREATE INDEX ix_seg_ura_membership ON sch_seguridad.usuario_rol_ambito(tenant_id, membership_id, estado) WHERE es_activo = '1';
CREATE INDEX ix_seg_ura_rol ON sch_seguridad.usuario_rol_ambito(tenant_id, rol_id) WHERE estado = 'ACTIVO' AND es_activo = '1';

CREATE TABLE sch_seguridad.sesion_usuario (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_sesion         UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    membership_id       BIGINT NOT NULL,
    dispositivo_ref     UUID,
    provider            VARCHAR(100),
    auth_method         VARCHAR(50),
    canal               VARCHAR(30) NOT NULL DEFAULT 'WEB',
    ip_origen           INET,
    user_agent          VARCHAR(1000),
    login_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_uso_at       TIMESTAMPTZ,
    expira_at           TIMESTAMPTZ,
    logout_at           TIMESTAMPTZ,
    revocado_at         TIMESTAMPTZ,
    motivo_revocacion   VARCHAR(500),
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_seg_sesion PRIMARY KEY (id),
    CONSTRAINT uk_seg_sesion_uuid UNIQUE (uuid_sesion),
    CONSTRAINT fk_seg_sesion_membership FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_seg_sesion_canal CHECK (canal IN ('WEB','POS','MOBILE','API','BACKOFFICE')),
    CONSTRAINT ck_seg_sesion_estado CHECK (estado IN ('ACTIVA', 'CERRADA', 'EXPIRADA', 'REVOCADA')),
    CONSTRAINT ck_seg_sesion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_seg_sesion_membership ON sch_seguridad.sesion_usuario(tenant_id, membership_id, estado) WHERE es_activo = '1';
CREATE INDEX ix_seg_sesion_expiracion ON sch_seguridad.sesion_usuario(expira_at) WHERE estado = 'ACTIVA' AND es_activo = '1';

-- Claves foraneas de compras y ventas: se agregan despues de crear IAM.
-- Referencian membership (cuenta por tenant), no identidad (persona global).
ALTER TABLE sch_abastecimiento.solicitud_compra
    ADD CONSTRAINT fk_solicitud_usuario_solicita FOREIGN KEY (tenant_id, solicitante_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_abastecimiento.solicitud_compra
    ADD CONSTRAINT fk_solicitud_usuario_aprueba FOREIGN KEY (tenant_id, aprobado_por_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_abastecimiento.orden_compra
    ADD CONSTRAINT fk_oc_usuario_aprueba FOREIGN KEY (tenant_id, aprobado_por_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_abastecimiento.recepcion_compra
    ADD CONSTRAINT fk_recepcion_usuario_recibe FOREIGN KEY (tenant_id, recibido_por_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_venta.turno_caja
    ADD CONSTRAINT fk_turno_cajero FOREIGN KEY (tenant_id, cajero_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_venta.movimiento_caja
    ADD CONSTRAINT fk_movimiento_caja_actor FOREIGN KEY (tenant_id, actor_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_venta.venta
    ADD CONSTRAINT fk_venta_vendedor FOREIGN KEY (tenant_id, vendedor_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_venta.devolucion_comercial
    ADD CONSTRAINT fk_devolucion_aprobador FOREIGN KEY (tenant_id, aprobado_por_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_venta.devolucion_comercial_linea
    ADD CONSTRAINT fk_dev_linea_evaluador FOREIGN KEY (tenant_id, evaluado_por_usuario_id) REFERENCES sch_seguridad.membership(tenant_id, id);
