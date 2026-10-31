-- ============================================================================
-- SCRIPT MAESTRO DDL: ERP CORE FARMACÉUTICO (PERÚ / DIGEMID / SUNAT)
-- MOTOR: PostgreSQL 18.x
-- INCLUYE: sch_catalogo.rubro_comercial + sku_comercial adaptado para Retail
-- CONVENCIONES:
--   - Claves primarias técnicas: BIGINT GENERATED ALWAYS AS IDENTITY
--   - Claves públicas de negocio: UUIDv7
--   - Auditoría: created_by VARCHAR(15) DEFAULT 'SYSTEM', updated_by VARCHAR(15)
--   - Borrado Lógico: es_activo CHAR(1) DEFAULT '1' CHECK (es_activo IN ('0', '1'))
--   - Aislamiento SaaS: Multi-tenant explícito (tenant_id) con claves compuestas
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. EXTENSIONES Y ESQUEMAS
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE SCHEMA IF NOT EXISTS sch_admin;
CREATE SCHEMA IF NOT EXISTS sch_seguridad;
CREATE SCHEMA IF NOT EXISTS sch_organizacion;
CREATE SCHEMA IF NOT EXISTS sch_catalogo;
CREATE SCHEMA IF NOT EXISTS sch_abastecimiento;
CREATE SCHEMA IF NOT EXISTS sch_inventario;
CREATE SCHEMA IF NOT EXISTS sch_precio;
CREATE SCHEMA IF NOT EXISTS sch_venta;
CREATE SCHEMA IF NOT EXISTS sch_dispensacion;
CREATE SCHEMA IF NOT EXISTS sch_facturacion;
CREATE SCHEMA IF NOT EXISTS sch_vigilancia;
CREATE SCHEMA IF NOT EXISTS sch_finanzas;
CREATE SCHEMA IF NOT EXISTS sch_auditoria;
CREATE SCHEMA IF NOT EXISTS sch_integracion;
CREATE SCHEMA IF NOT EXISTS sch_app;

-- ============================================================================
-- 1. ESQUEMA: sch_admin (Gobernanza SaaS Multi-Tenant)
-- ============================================================================

CREATE TABLE sch_admin.tenant (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    codigo              VARCHAR(30) NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    slug                VARCHAR(80) NOT NULL,
    tipo_suscripcion    VARCHAR(30) NOT NULL DEFAULT 'PRODUCCION',
    timezone            VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    locale              VARCHAR(20) NOT NULL DEFAULT 'es-PE',
    default_currency    CHAR(3) NOT NULL DEFAULT 'PEN',
    settings            JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_tenant PRIMARY KEY (id),
    CONSTRAINT uk_tenant_uuid UNIQUE (uuid_publico),
    CONSTRAINT ck_tenant_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'BLOQUEADO')),
    CONSTRAINT ck_tenant_es_activo CHECK (es_activo IN ('0', '1')),
    CONSTRAINT ck_tenant_currency CHECK (default_currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX uk_tenant_codigo ON sch_admin.tenant(codigo) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_tenant_slug ON sch_admin.tenant(slug) WHERE es_activo = '1';
CREATE INDEX ix_tenant_activo ON sch_admin.tenant(id, estado) WHERE es_activo = '1';

-- ============================================================================
-- 2. ESQUEMA: sch_seguridad (IAM Base: Usuario, Módulo, Rol, Permiso)
-- ============================================================================

CREATE TABLE sch_seguridad.usuario (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    tipo_documento              VARCHAR(2),      -- Catálogo 06 SUNAT: '1'=DNI, '6'=RUC, '4'=CE, '7'=PAS
    numero_documento            VARCHAR(15),
    nombres                     VARCHAR(150),
    apellidos                   VARCHAR(180),
    username                    CITEXT,
    email                       CITEXT,
    telefono                    VARCHAR(40),
    nombre_alias                VARCHAR(150),
    requiere_cambio_clave       BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_requerido               BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_acceso_en            TIMESTAMPTZ,
    bloqueado_hasta             TIMESTAMPTZ,
    estado                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_seg_usuario PRIMARY KEY (id),
    CONSTRAINT uk_seg_usuario_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_usuario_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_seg_usuario_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_seg_usuario_estado CHECK (estado IN ('ACTIVO', 'BLOQUEADO', 'SUSPENDIDO')),
    CONSTRAINT ck_seg_usuario_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_usuario_tenant_username ON sch_seguridad.usuario(tenant_id, username) WHERE es_activo = '1' AND username IS NOT NULL;
CREATE UNIQUE INDEX uk_seg_usuario_tenant_email ON sch_seguridad.usuario(tenant_id, email) WHERE es_activo = '1' AND email IS NOT NULL;
CREATE UNIQUE INDEX uk_seg_usuario_tenant_documento ON sch_seguridad.usuario(tenant_id, tipo_documento, numero_documento) WHERE es_activo = '1' AND numero_documento IS NOT NULL;

CREATE TABLE sch_seguridad.identidad_externa (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    usuario_id          BIGINT NOT NULL,
    provider            VARCHAR(100) NOT NULL,
    subject             VARCHAR(300) NOT NULL,
    issuer              VARCHAR(500),
    email_claim         CITEXT,
    ultimo_acceso_en    TIMESTAMPTZ,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_seg_identidad_externa PRIMARY KEY (id),
    CONSTRAINT fk_seg_identidad_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_seg_identidad_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_identidad_provider_subject ON sch_seguridad.identidad_externa(provider, subject) WHERE es_activo = '1';
CREATE INDEX ix_seg_identidad_usuario ON sch_seguridad.identidad_externa(tenant_id, usuario_id) WHERE es_activo = '1';

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
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(15),
    CONSTRAINT pk_seg_rol PRIMARY KEY (id),
    CONSTRAINT uk_seg_rol_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_rol_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_seg_rol_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_seg_rol_tipo CHECK (tipo_rol IN ('GLOBAL','EMPRESA','ESTABLECIMIENTO','ALMACEN','TERMINAL')),
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
    es_activo   CHAR(1) NOT NULL DEFAULT '1',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(15),
    CONSTRAINT pk_seg_permiso PRIMARY KEY (id),
    CONSTRAINT fk_seg_permiso_modulo FOREIGN KEY (modulo_id) REFERENCES sch_seguridad.modulo_sistema(id),
    CONSTRAINT ck_seg_permiso_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_seg_permiso_codigo ON sch_seguridad.permiso(codigo) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_seg_permiso_modulo_recurso_accion ON sch_seguridad.permiso(modulo_id, recurso, accion) WHERE es_activo = '1';
CREATE INDEX ix_seg_permiso_modulo ON sch_seguridad.permiso(modulo_id) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.rol_permiso (
    tenant_id   BIGINT NOT NULL,
    rol_id      BIGINT NOT NULL,
    permiso_id  BIGINT NOT NULL,
    es_activo   CHAR(1) NOT NULL DEFAULT '1',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(15),
    CONSTRAINT pk_seg_rol_permiso PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_seg_rol_permiso_rol FOREIGN KEY (tenant_id, rol_id) REFERENCES sch_seguridad.rol(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_seg_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES sch_seguridad.permiso(id),
    CONSTRAINT ck_seg_rol_permiso_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_seg_rol_permiso_permiso ON sch_seguridad.rol_permiso(permiso_id) WHERE es_activo = '1';

-- ============================================================================
-- 3. ESQUEMA: sch_organizacion (Empresas, Sedes, Almacenes, POS y Regentes)
-- ============================================================================

CREATE TABLE sch_organizacion.empresa_operadora (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    ruc                     VARCHAR(11) NOT NULL,
    razon_social            VARCHAR(300) NOT NULL,
    nombre_comercial        VARCHAR(300),
    direccion_fiscal        VARCHAR(500),
    ubigeo_fiscal           VARCHAR(6),
    telefono                VARCHAR(40),
    email                   CITEXT,
    sitio_web               VARCHAR(300),
    logo_uri                TEXT,
    moneda_funcional        CHAR(3) NOT NULL DEFAULT 'PEN',
    zona_horaria            VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    permite_venta_online    BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_empresa_operadora PRIMARY KEY (id),
    CONSTRAINT uk_empresa_operadora_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_empresa_operadora_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_empresa_operadora_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_empresa_operadora_ruc CHECK (ruc ~ '^(10|20)[0-9]{9}$'),
    CONSTRAINT ck_empresa_operadora_ubigeo CHECK (ubigeo_fiscal IS NULL OR ubigeo_fiscal ~ '^[0-9]{6}$'),
    CONSTRAINT ck_empresa_operadora_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'BLOQUEADO')),
    CONSTRAINT ck_empresa_operadora_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_empresa_tenant_ruc ON sch_organizacion.empresa_operadora(tenant_id, ruc) WHERE es_activo = '1';

CREATE TABLE sch_organizacion.establecimiento_farmaceutico (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    codigo                      VARCHAR(40) NOT NULL,
    nombre                      VARCHAR(250) NOT NULL,
    tipo_establecimiento        VARCHAR(40) NOT NULL DEFAULT 'BOTICA',
    categoria_regulatoria_codigo VARCHAR(50),
    codigo_anexo_sunat          VARCHAR(4) NOT NULL DEFAULT '0000',
    codigo_digemid              VARCHAR(10),
    direccion                   VARCHAR(500),
    ubigeo                      VARCHAR(6),
    referencia                  VARCHAR(300),
    latitud                     NUMERIC(10, 7),
    longitud                    NUMERIC(10, 7),
    telefono                    VARCHAR(40),
    email                       CITEXT,
    es_principal                BOOLEAN NOT NULL DEFAULT FALSE,
    permite_venta_online        BOOLEAN NOT NULL DEFAULT FALSE,
    permite_delivery            BOOLEAN NOT NULL DEFAULT FALSE,
    perfil_operacion            VARCHAR(30) NOT NULL DEFAULT 'ONLINE',
    zona_horaria                VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    estado_operativo            VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_establecimiento_farmaceutico PRIMARY KEY (id),
    CONSTRAINT uk_establecimiento_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_establecimiento_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_establecimiento_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_establecimiento_ubigeo CHECK (ubigeo IS NULL OR ubigeo ~ '^[0-9]{6}$'),
    CONSTRAINT ck_establecimiento_codigo_sunat CHECK (codigo_anexo_sunat ~ '^[0-9]{4}$'),
    CONSTRAINT ck_establecimiento_perfil CHECK (perfil_operacion IN ('ONLINE', 'STORE_EDGE')),
    CONSTRAINT ck_establecimiento_estado CHECK (estado_operativo IN ('ACTIVO', 'SUSPENDIDO', 'CLAUSURADO', 'REMODELACION')),
    CONSTRAINT ck_establecimiento_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_establecimiento_tenant_codigo ON sch_organizacion.establecimiento_farmaceutico(tenant_id, codigo) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_establecimiento_codigo_digemid ON sch_organizacion.establecimiento_farmaceutico(tenant_id, codigo_digemid) WHERE es_activo = '1' AND codigo_digemid IS NOT NULL;
CREATE INDEX ix_establecimiento_empresa ON sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id) WHERE es_activo = '1';

CREATE TABLE sch_organizacion.establecimiento_autorizacion_sanitaria (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    tipo_autorizacion   VARCHAR(50) NOT NULL,
    numero_autorizacion VARCHAR(100) NOT NULL,
    entidad_emisora     VARCHAR(150),
    fecha_emision       DATE,
    vigente_desde       DATE,
    vigente_hasta       DATE,
    estado              VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
    observacion         VARCHAR(1000),
    evidencia_uri       TEXT,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_establecimiento_autorizacion PRIMARY KEY (id),
    CONSTRAINT uk_establecimiento_autorizacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_establecimiento_autorizacion_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_est_aut_fechas CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_est_aut_estado CHECK (estado IN ('VIGENTE', 'VENCIDA', 'SUSPENDIDA', 'REVOCADA')),
    CONSTRAINT ck_est_aut_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_est_autorizacion_numero ON sch_organizacion.establecimiento_autorizacion_sanitaria(tenant_id, establecimiento_id, tipo_autorizacion, numero_autorizacion) WHERE es_activo = '1';

CREATE TABLE sch_organizacion.almacen (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    codigo              VARCHAR(40) NOT NULL,
    nombre              VARCHAR(150) NOT NULL,
    tipo                VARCHAR(30) NOT NULL DEFAULT 'VENTA',
    permite_lotes       BOOLEAN NOT NULL DEFAULT TRUE,
    permite_vencimiento BOOLEAN NOT NULL DEFAULT TRUE,
    permite_venta       BOOLEAN NOT NULL DEFAULT FALSE,
    permite_despacho    BOOLEAN NOT NULL DEFAULT TRUE,
    control_temperatura BOOLEAN NOT NULL DEFAULT FALSE,
    temperatura_min_c   NUMERIC(6, 2),
    temperatura_max_c   NUMERIC(6, 2),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_almacen PRIMARY KEY (id),
    CONSTRAINT uk_almacen_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_almacen_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_almacen_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_almacen_temperatura CHECK (temperatura_max_c IS NULL OR temperatura_min_c IS NULL OR temperatura_max_c >= temperatura_min_c),
    CONSTRAINT ck_almacen_tipo CHECK (tipo IN ('VENTA', 'GENERAL', 'CUARENTENA', 'REFRIGERADO', 'PSICOTROPICO', 'MERMA')),
    CONSTRAINT ck_almacen_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_almacen_est_codigo ON sch_organizacion.almacen(tenant_id, establecimiento_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_organizacion.ubicacion_almacen (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    almacen_id          BIGINT NOT NULL,
    codigo              VARCHAR(50) NOT NULL,
    zona                VARCHAR(80),
    pasillo             VARCHAR(40),
    rack                VARCHAR(40),
    nivel               VARCHAR(40),
    posicion            VARCHAR(40),
    descripcion         VARCHAR(200),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_ubicacion_almacen PRIMARY KEY (id),
    CONSTRAINT uk_ubicacion_almacen_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_ubicacion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, almacen_id, id),
    CONSTRAINT fk_ubicacion_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_ubicacion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ubicacion_almacen_codigo ON sch_organizacion.ubicacion_almacen(tenant_id, almacen_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_organizacion.terminal_pos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    codigo                  VARCHAR(40) NOT NULL,
    nombre                  VARCHAR(120),
    serie_boleta_defecto    VARCHAR(4),
    serie_factura_defecto   VARCHAR(4),
    numero_serie_equipo     VARCHAR(120),
    hostname                VARCHAR(150),
    ip_equipo               INET,
    impresora_codigo        VARCHAR(100),
    store_edge_habilitado   BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_heartbeat_at     TIMESTAMPTZ,
    version_app             VARCHAR(80),
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_terminal_pos PRIMARY KEY (id),
    CONSTRAINT uk_terminal_pos_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_terminal_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_terminal_pos_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_terminal_series CHECK (
        (serie_boleta_defecto IS NULL OR serie_boleta_defecto ~ '^B[A-Z0-9]{3}$') AND
        (serie_factura_defecto IS NULL OR serie_factura_defecto ~ '^F[A-Z0-9]{3}$')
    ),
    CONSTRAINT ck_terminal_pos_estado CHECK (estado IN ('ACTIVO', 'BLOQUEADO', 'MANTENIMIENTO')),
    CONSTRAINT ck_terminal_pos_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_terminal_pos_codigo ON sch_organizacion.terminal_pos(tenant_id, establecimiento_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_organizacion.profesional_farmaceutico (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    tipo_documento      VARCHAR(2) NOT NULL,
    numero_documento    VARCHAR(15) NOT NULL,
    nombres             VARCHAR(150) NOT NULL,
    apellidos           VARCHAR(150) NOT NULL,
    email               CITEXT,
    telefono            VARCHAR(40),
    colegio_profesional VARCHAR(100) NOT NULL DEFAULT 'COLEGIO QUIMICO FARMACEUTICO DEL PERU',
    numero_colegiatura  VARCHAR(50) NOT NULL,
    especialidad        VARCHAR(150),
    estado_colegiatura  VARCHAR(30) NOT NULL DEFAULT 'HABILITADO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_profesional_farmaceutico PRIMARY KEY (id),
    CONSTRAINT uk_profesional_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_profesional_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_profesional_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_profesional_estado_colegiatura CHECK (estado_colegiatura IN ('HABILITADO', 'NO_HABILITADO', 'SUSPENDIDO')),
    CONSTRAINT ck_profesional_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_profesional_doc ON sch_organizacion.profesional_farmaceutico(tenant_id, tipo_documento, numero_documento) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_profesional_colegiatura ON sch_organizacion.profesional_farmaceutico(tenant_id, numero_colegiatura) WHERE es_activo = '1';

CREATE TABLE sch_organizacion.asignacion_profesional (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    profesional_id      BIGINT NOT NULL,
    funcion             VARCHAR(40) NOT NULL DEFAULT 'DIRECTOR_TECNICO',
    es_principal        BOOLEAN NOT NULL DEFAULT FALSE,
    horario_inicio      TIME,
    horario_fin         TIME,
    vigente_desde       DATE NOT NULL,
    vigente_hasta       DATE,
    evidencia_uri       TEXT,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_asignacion_profesional PRIMARY KEY (id),
    CONSTRAINT uk_asignacion_profesional_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_asignacion_prof_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_asignacion_prof_prof FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_asignacion_prof_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_asignacion_funcion CHECK (funcion IN ('DIRECTOR_TECNICO', 'QUIMICO_ASISTENTE')),
    CONSTRAINT ck_asignacion_prof_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_un_solo_director_tecnico_principal ON sch_organizacion.asignacion_profesional(tenant_id, establecimiento_id) WHERE es_activo = '1' AND es_principal = TRUE AND vigente_hasta IS NULL;
CREATE INDEX ix_asignacion_prof_est ON sch_organizacion.asignacion_profesional(tenant_id, establecimiento_id) WHERE es_activo = '1';

-- ============================================================================
-- 4. ESQUEMA: sch_seguridad (Continuación: Dispositivos, Ámbitos y Sesiones)
-- ============================================================================

CREATE TABLE sch_seguridad.dispositivo_tienda (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    terminal_id         BIGINT,
    device_fingerprint  VARCHAR(300),
    cert_thumbprint     VARCHAR(300),
    agent_version       VARCHAR(100),
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    ultimo_contacto_en  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
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
    usuario_id          BIGINT NOT NULL,
    rol_id              BIGINT NOT NULL,
    tipo_ambito         VARCHAR(30) NOT NULL,
    empresa_id          BIGINT,
    establecimiento_id  BIGINT,
    almacen_id          BIGINT,
    terminal_id         BIGINT,
    vigente_desde       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vigente_hasta       TIMESTAMPTZ,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_seg_usuario_rol_ambito PRIMARY KEY (id),
    CONSTRAINT uk_seg_usuario_rol_ambito_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_seg_ura_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_seg_ura_rol FOREIGN KEY (tenant_id, rol_id) REFERENCES sch_seguridad.rol(tenant_id, id),
    CONSTRAINT fk_seg_ura_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_seg_ura_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_seg_ura_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_seg_ura_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_organizacion.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_seg_ura_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
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
    tenant_id, usuario_id, rol_id, tipo_ambito,
    (COALESCE(empresa_id, 0)), (COALESCE(establecimiento_id, 0)), 
    (COALESCE(almacen_id, 0)), (COALESCE(terminal_id, 0))
) WHERE es_activo = '1';

CREATE INDEX ix_seg_ura_usuario ON sch_seguridad.usuario_rol_ambito(tenant_id, usuario_id) WHERE es_activo = '1';
CREATE INDEX ix_seg_ura_rol ON sch_seguridad.usuario_rol_ambito(tenant_id, rol_id) WHERE es_activo = '1';

CREATE TABLE sch_seguridad.sesion_usuario (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_sesion         UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    usuario_id          BIGINT NOT NULL,
    dispositivo_id      BIGINT,
    provider            VARCHAR(100),
    auth_method         VARCHAR(50),
    channel             VARCHAR(30) NOT NULL DEFAULT 'WEB',
    ip_address          INET,
    user_agent          VARCHAR(1000),
    ultimo_uso_en       TIMESTAMPTZ,
    expira_en           TIMESTAMPTZ,
    cerrado_en          TIMESTAMPTZ,
    revocado_en         TIMESTAMPTZ,
    motivo_revocacion   VARCHAR(500),
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_seg_sesion PRIMARY KEY (id),
    CONSTRAINT uk_seg_sesion_uuid UNIQUE (uuid_sesion),
    CONSTRAINT fk_seg_sesion_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_seg_sesion_dispositivo FOREIGN KEY (tenant_id, dispositivo_id) REFERENCES sch_seguridad.dispositivo_tienda(tenant_id, id),
    CONSTRAINT ck_seg_sesion_channel CHECK (channel IN ('WEB','POS','MOBILE','API','BACKOFFICE')),
    CONSTRAINT ck_seg_sesion_estado CHECK (estado IN ('ACTIVA', 'CERRADA', 'EXPIRADA', 'REVOCADA')),
    CONSTRAINT ck_seg_sesion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_seg_sesion_usuario ON sch_seguridad.sesion_usuario(tenant_id, usuario_id, estado) WHERE es_activo = '1';
CREATE INDEX ix_seg_sesion_expiracion ON sch_seguridad.sesion_usuario(expira_en) WHERE es_activo = '1' AND estado = 'ACTIVA';

-- ============================================================================
-- 5. ESQUEMA: sch_catalogo (Catálogo Regulado, Rubros y SKUs de Retail/Farma)
-- ============================================================================

CREATE TABLE sch_catalogo.condicion_venta (
    codigo              VARCHAR(30) NOT NULL,
    denominacion        VARCHAR(200) NOT NULL,
    requiere_receta     BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_retencion  BOOLEAN NOT NULL DEFAULT FALSE,
    fuente              VARCHAR(300) DEFAULT 'DIGEMID',
    version_fuente      VARCHAR(100),
    vigente_desde       DATE,
    vigente_hasta       DATE,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_condicion_venta PRIMARY KEY (codigo),
    CONSTRAINT ck_condicion_venta_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.forma_farmaceutica (
    codigo          VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(200) NOT NULL,
    fuente          VARCHAR(300) DEFAULT 'DIGEMID',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_forma_farmaceutica PRIMARY KEY (codigo),
    CONSTRAINT ck_forma_farmaceutica_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.via_administracion (
    codigo          VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(200) NOT NULL,
    fuente          VARCHAR(300) DEFAULT 'DIGEMID',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_via_administracion PRIMARY KEY (codigo),
    CONSTRAINT ck_via_administracion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.unidad_medida (
    codigo          VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(150) NOT NULL,
    simbolo         VARCHAR(30),
    permite_decimal BOOLEAN NOT NULL DEFAULT FALSE,
    fuente          VARCHAR(300) DEFAULT 'SUNAT_CAT03',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_unidad_medida PRIMARY KEY (codigo),
    CONSTRAINT ck_unidad_medida_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.clasificacion_controlada (
    codigo                   VARCHAR(40) NOT NULL,
    denominacion             VARCHAR(200) NOT NULL,
    norma_fuente             VARCHAR(300) DEFAULT 'D.S. 023-2001-SA',
    requiere_receta_especial BOOLEAN NOT NULL DEFAULT FALSE,
    retiene_receta           BOOLEAN NOT NULL DEFAULT FALSE,
    vigencia_receta_dias     INTEGER,
    es_activo                CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_clasificacion_controlada PRIMARY KEY (codigo),
    CONSTRAINT ck_clasif_controlada_dias CHECK (vigencia_receta_dias IS NULL OR vigencia_receta_dias > 0),
    CONSTRAINT ck_clasif_controlada_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.principio_activo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    codigo_fuente       VARCHAR(80),
    denominacion        VARCHAR(300) NOT NULL,
    nombre_normalizado  VARCHAR(300),
    fuente              VARCHAR(300) DEFAULT 'DIGEMID',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_principio_activo PRIMARY KEY (id),
    CONSTRAINT uk_principio_activo_uuid UNIQUE (uuid_publico),
    CONSTRAINT ck_principio_activo_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_principio_activo_nombre ON sch_catalogo.principio_activo(denominacion) WHERE es_activo = '1';

CREATE TABLE sch_catalogo.producto_regulado (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tipo_producto                   VARCHAR(40) NOT NULL,
    rubro_codigo                    VARCHAR(50),
    tipo_registro                   VARCHAR(40),
    numero_registro                 VARCHAR(100),
    denominacion                    VARCHAR(500) NOT NULL,
    concentracion_texto             VARCHAR(300),
    presentacion_regulatoria        VARCHAR(500),
    forma_farmaceutica_codigo       VARCHAR(30),
    via_administracion_codigo       VARCHAR(30),
    unidad_medida_codigo            VARCHAR(30),
    condicion_venta_codigo          VARCHAR(30),
    clasificacion_atc               VARCHAR(30),
    clasificacion_controlada_codigo VARCHAR(40),
    tipo_liberacion                 VARCHAR(40),
    origen_fabricacion              VARCHAR(40),
    pais_origen                     VARCHAR(100),
    subpartida_nacional             VARCHAR(30),
    titular_registro                VARCHAR(300),
    fabricante                      VARCHAR(300),
    importador                      VARCHAR(300),
    establecimiento_expendio        VARCHAR(200),
    vigente_desde                   DATE,
    vigente_hasta                   DATE,
    estado_regulatorio              VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    fuente                          VARCHAR(300) DEFAULT 'DIGEMID',
    version_fuente                  VARCHAR(100),
    updated_source_at               TIMESTAMPTZ,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMPTZ,
    CONSTRAINT pk_producto_regulado PRIMARY KEY (id),
    CONSTRAINT uk_producto_regulado_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_prod_reg_forma FOREIGN KEY (forma_farmaceutica_codigo) REFERENCES sch_catalogo.forma_farmaceutica(codigo),
    CONSTRAINT fk_prod_reg_via FOREIGN KEY (via_administracion_codigo) REFERENCES sch_catalogo.via_administracion(codigo),
    CONSTRAINT fk_prod_reg_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT fk_prod_reg_condicion FOREIGN KEY (condicion_venta_codigo) REFERENCES sch_catalogo.condicion_venta(codigo),
    CONSTRAINT fk_prod_reg_controlada FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_catalogo.clasificacion_controlada(codigo),
    CONSTRAINT ck_prod_reg_registro CHECK ((numero_registro IS NULL AND tipo_registro IS NULL) OR (numero_registro IS NOT NULL AND tipo_registro IS NOT NULL)),
    CONSTRAINT ck_prod_reg_vigencia CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_prod_reg_estado CHECK (estado_regulatorio IN ('VIGENTE', 'VENCIDO', 'SUSPENDIDO', 'CANCELADO', 'POR_VALIDAR')),
    CONSTRAINT ck_prod_reg_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_producto_regulado_registro ON sch_catalogo.producto_regulado(tipo_registro, numero_registro) WHERE es_activo = '1' AND numero_registro IS NOT NULL;
CREATE INDEX ix_producto_condicion ON sch_catalogo.producto_regulado(condicion_venta_codigo);
CREATE INDEX ix_producto_nombre_search ON sch_catalogo.producto_regulado USING gin (
    to_tsvector('spanish', coalesce(denominacion,'') || ' ' || coalesce(concentracion_texto,'') || ' ' || coalesce(fabricante,''))
);

CREATE TABLE sch_catalogo.producto_principio_activo (
    producto_regulado_id    BIGINT NOT NULL,
    principio_activo_id     BIGINT NOT NULL,
    concentracion_texto     VARCHAR(200),
    cantidad                NUMERIC(18, 6),
    unidad_medida_codigo    VARCHAR(30),
    es_principal            BOOLEAN NOT NULL DEFAULT TRUE,
    orden                   SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_producto_principio_activo PRIMARY KEY (producto_regulado_id, principio_activo_id),
    CONSTRAINT fk_prod_pa_producto FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id) ON DELETE CASCADE,
    CONSTRAINT fk_prod_pa_principio FOREIGN KEY (principio_activo_id) REFERENCES sch_catalogo.principio_activo(id),
    CONSTRAINT fk_prod_pa_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT ck_producto_pa_cantidad CHECK (cantidad IS NULL OR cantidad > 0)
);

CREATE TABLE sch_catalogo.categoria_producto (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    categoria_padre_id  BIGINT,
    codigo              VARCHAR(50) NOT NULL,
    nombre              VARCHAR(180) NOT NULL,
    descripcion         VARCHAR(500),
    nivel               INTEGER NOT NULL DEFAULT 1,
    orden               INTEGER NOT NULL DEFAULT 0,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_categoria_producto PRIMARY KEY (id),
    CONSTRAINT uk_categoria_producto_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_categoria_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_categoria_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_categoria_padre FOREIGN KEY (tenant_id, categoria_padre_id) REFERENCES sch_catalogo.categoria_producto(tenant_id, id),
    CONSTRAINT ck_categoria_nivel CHECK (nivel >= 1),
    CONSTRAINT ck_categoria_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_categoria_tenant_codigo ON sch_catalogo.categoria_producto(tenant_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_catalogo.marca (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    codigo              VARCHAR(50) NOT NULL,
    nombre              VARCHAR(180) NOT NULL,
    descripcion         VARCHAR(500),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_marca PRIMARY KEY (id),
    CONSTRAINT uk_marca_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_marca_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_marca_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_marca_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_marca_tenant_codigo ON sch_catalogo.marca(tenant_id, codigo) WHERE es_activo = '1';

-- ============================================================================
-- 5.1 NOVEDAD: RUBRO COMERCIAL (Macro-Líneas: Farma, Bebé, Cosméticos, Retail)
-- ============================================================================
CREATE TABLE sch_catalogo.rubro_comercial (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    codigo              VARCHAR(50) NOT NULL,
    nombre              VARCHAR(120) NOT NULL,
    descripcion         VARCHAR(300),
    es_farmaceutico     BOOLEAN NOT NULL DEFAULT FALSE,
    orden               INTEGER NOT NULL DEFAULT 0,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_rubro_comercial PRIMARY KEY (id),
    CONSTRAINT uk_rubro_comercial_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_rubro_comercial_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_rubro_comercial_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_rubro_comercial_orden CHECK (orden >= 0),
    CONSTRAINT ck_rubro_comercial_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_rubro_comercial_codigo ON sch_catalogo.rubro_comercial(tenant_id, codigo) WHERE es_activo = '1';
CREATE INDEX ix_rubro_comercial_tipo ON sch_catalogo.rubro_comercial(tenant_id, es_farmaceutico) WHERE es_activo = '1';

-- ============================================================================
-- 5.2 SKU COMERCIAL (Con enlace formal a rubro_comercial y atributos Retail)
-- ============================================================================
CREATE TABLE sch_catalogo.sku_comercial (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    producto_regulado_id        BIGINT,
    rubro_id                    BIGINT,
    categoria_id                BIGINT,
    marca_id                    BIGINT,
    tipo_sku                    VARCHAR(30) NOT NULL DEFAULT 'REGULADO',
    codigo_interno              VARCHAR(60) NOT NULL,
    codigo_nso                  VARCHAR(100),
    codigo_estandar_sunat       VARCHAR(20),
    descripcion_comercial       VARCHAR(500) NOT NULL,
    nombre_corto                VARCHAR(200),
    presentacion_comercial      VARCHAR(300),
    atributos_variante          JSONB NOT NULL DEFAULT '{}'::jsonb,
    unidad_venta_codigo         VARCHAR(30) NOT NULL,
    unidad_fraccion_codigo      VARCHAR(30),
    permite_venta_fraccion      BOOLEAN NOT NULL DEFAULT FALSE,
    factor_fraccion             NUMERIC(18, 4),
    contenido                   NUMERIC(18, 4),
    unidad_contenido_codigo     VARCHAR(30),
    peso_gramos                 NUMERIC(18, 4),
    alto_cm                     NUMERIC(10, 2),
    ancho_cm                    NUMERIC(10, 2),
    largo_cm                    NUMERIC(10, 2),
    requiere_lote               BOOLEAN NOT NULL DEFAULT TRUE,
    requiere_vencimiento        BOOLEAN NOT NULL DEFAULT TRUE,
    afecto_igv                  BOOLEAN NOT NULL DEFAULT TRUE,
    stock_minimo_default        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    stock_maximo_default        NUMERIC(18, 4),
    imagen_uri                  TEXT,
    estado_comercial            VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_sku_comercial PRIMARY KEY (id),
    CONSTRAINT uk_sku_comercial_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_sku_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_sku_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_sku_producto_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_sku_rubro FOREIGN KEY (tenant_id, rubro_id) REFERENCES sch_catalogo.rubro_comercial(tenant_id, id),
    CONSTRAINT fk_sku_categoria FOREIGN KEY (tenant_id, categoria_id) REFERENCES sch_catalogo.categoria_producto(tenant_id, id),
    CONSTRAINT fk_sku_marca FOREIGN KEY (tenant_id, marca_id) REFERENCES sch_catalogo.marca(tenant_id, id),
    CONSTRAINT fk_sku_unidad_venta FOREIGN KEY (unidad_venta_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT fk_sku_unidad_fraccion FOREIGN KEY (unidad_fraccion_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT fk_sku_unidad_contenido FOREIGN KEY (unidad_contenido_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT ck_sku_tipo CHECK (tipo_sku IN ('REGULADO', 'NO_REGULADO')),
    CONSTRAINT ck_sku_tipo_producto CHECK ((tipo_sku = 'REGULADO' AND producto_regulado_id IS NOT NULL) OR (tipo_sku = 'NO_REGULADO')),
    CONSTRAINT ck_sku_factor CHECK (
        (permite_venta_fraccion = FALSE AND factor_fraccion IS NULL AND unidad_fraccion_codigo IS NULL) OR 
        (permite_venta_fraccion = TRUE AND factor_fraccion > 0 AND unidad_fraccion_codigo IS NOT NULL)
    ),
    CONSTRAINT ck_sku_stock_default CHECK (stock_minimo_default >= 0 AND (stock_maximo_default IS NULL OR stock_maximo_default >= stock_minimo_default)),
    CONSTRAINT ck_sku_estado CHECK (estado_comercial IN ('ACTIVO', 'SUSPENDIDO', 'DESCONTINUADO')),
    CONSTRAINT ck_sku_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_sku_tenant_codigo ON sch_catalogo.sku_comercial(tenant_id, codigo_interno) WHERE es_activo = '1';
CREATE INDEX ix_sku_producto ON sch_catalogo.sku_comercial(producto_regulado_id);
CREATE INDEX ix_sku_rubro ON sch_catalogo.sku_comercial(tenant_id, rubro_id) WHERE es_activo = '1';
CREATE INDEX ix_sku_categoria ON sch_catalogo.sku_comercial(tenant_id, categoria_id) WHERE es_activo = '1';

CREATE TABLE sch_catalogo.sku_codigo_barra (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id       BIGINT NOT NULL,
    sku_id          BIGINT NOT NULL,
    tipo_codigo     VARCHAR(30) NOT NULL DEFAULT 'EAN13',
    codigo_barra    VARCHAR(80) NOT NULL,
    es_principal    BOOLEAN NOT NULL DEFAULT FALSE,
    vigente_desde   DATE,
    vigente_hasta   DATE,
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(15),
    CONSTRAINT pk_sku_codigo_barra PRIMARY KEY (id),
    CONSTRAINT fk_sku_codigo_barra FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_sku_barra_fechas CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_sku_barra_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_sku_codigo_barra ON sch_catalogo.sku_codigo_barra(tenant_id, codigo_barra) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_sku_codigo_principal ON sch_catalogo.sku_codigo_barra(tenant_id, sku_id) WHERE es_activo = '1' AND es_principal = TRUE;

-- ============================================================================
-- 6. ESQUEMA: sch_abastecimiento (Proveedores, Compras y Recepción BPA)
-- ============================================================================

CREATE TABLE sch_abastecimiento.proveedor (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    tipo_documento          VARCHAR(2) NOT NULL DEFAULT '6',
    numero_documento        VARCHAR(15) NOT NULL,
    razon_social            VARCHAR(300) NOT NULL,
    nombre_comercial        VARCHAR(300),
    direccion               VARCHAR(500),
    ubigeo                  VARCHAR(6),
    telefono                VARCHAR(40),
    email                   CITEXT,
    sitio_web               VARCHAR(300),
    contacto_nombre         VARCHAR(180),
    contacto_cargo          VARCHAR(120),
    contacto_telefono       VARCHAR(40),
    contacto_email          CITEXT,
    condicion_pago_default  VARCHAR(80) DEFAULT 'CONTADO',
    dias_credito_default    INTEGER DEFAULT 0,
    moneda_default          CHAR(3) NOT NULL DEFAULT 'PEN',
    es_laboratorio          BOOLEAN NOT NULL DEFAULT FALSE,
    es_importador           BOOLEAN NOT NULL DEFAULT FALSE,
    es_distribuidor         BOOLEAN NOT NULL DEFAULT TRUE,
    calificacion            VARCHAR(30) DEFAULT 'CONFIABLE',
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_proveedor PRIMARY KEY (id),
    CONSTRAINT uk_proveedor_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_proveedor_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_proveedor_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_proveedor_ruc CHECK ((tipo_documento = '6' AND numero_documento ~ '^(10|20)[0-9]{9}$') OR (tipo_documento <> '6')),
    CONSTRAINT ck_proveedor_ubigeo CHECK (ubigeo IS NULL OR ubigeo ~ '^[0-9]{6}$'),
    CONSTRAINT ck_proveedor_dias_credito CHECK (dias_credito_default IS NULL OR dias_credito_default >= 0),
    CONSTRAINT ck_proveedor_estado CHECK (estado IN ('ACTIVO', 'BLOQUEADO', 'SUSPENDIDO')),
    CONSTRAINT ck_proveedor_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_proveedor_documento ON sch_abastecimiento.proveedor(tenant_id, tipo_documento, numero_documento) WHERE es_activo = '1';
CREATE INDEX ix_proveedor_nombre ON sch_abastecimiento.proveedor(tenant_id, razon_social) WHERE es_activo = '1';

CREATE TABLE sch_abastecimiento.solicitud_compra (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_solicitante_id  BIGINT NOT NULL,
    almacen_solicitante_id          BIGINT,
    numero                          VARCHAR(50) NOT NULL,
    fecha_solicitud                 DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_requerida                 DATE,
    prioridad                       VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    estado                          VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    motivo                          VARCHAR(1000),
    solicitante_usuario_id          BIGINT,
    aprobado_por_usuario_id         BIGINT,
    aprobado_at                     TIMESTAMPTZ,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(15),
    CONSTRAINT pk_solicitud_compra PRIMARY KEY (id),
    CONSTRAINT uk_solicitud_compra_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_solicitud_compra_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_solicitud_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_solicitud_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_solicitante_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_solicitud_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_solicitante_id, almacen_solicitante_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_solicitud_usuario_solicita FOREIGN KEY (tenant_id, solicitante_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT fk_solicitud_usuario_aprueba FOREIGN KEY (tenant_id, aprobado_por_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_solicitud_fechas CHECK (fecha_requerida IS NULL OR fecha_requerida >= fecha_solicitud),
    CONSTRAINT ck_solicitud_prioridad CHECK (prioridad IN ('BAJA', 'NORMAL', 'ALTA', 'URGENTE')),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('BORRADOR', 'EN_APROBACION', 'APROBADA', 'RECHAZADA', 'CANCELADA', 'ATENDIDA')),
    CONSTRAINT ck_solicitud_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_solicitud_compra_numero ON sch_abastecimiento.solicitud_compra(tenant_id, numero) WHERE es_activo = '1';

CREATE TABLE sch_abastecimiento.solicitud_compra_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    solicitud_id            BIGINT NOT NULL,
    numero_linea            INTEGER NOT NULL,
    sku_id                  BIGINT NOT NULL,
    cantidad_solicitada     NUMERIC(18, 4) NOT NULL,
    unidad_medida_codigo    VARCHAR(30) NOT NULL,
    stock_actual_snapshot   NUMERIC(18, 4),
    stock_minimo_snapshot   NUMERIC(18, 4),
    observacion             VARCHAR(500),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_solicitud_compra_linea PRIMARY KEY (id),
    CONSTRAINT uk_solicitud_linea UNIQUE (solicitud_id, numero_linea),
    CONSTRAINT fk_sol_linea_solicitud FOREIGN KEY (tenant_id, empresa_id, solicitud_id) REFERENCES sch_abastecimiento.solicitud_compra(tenant_id, empresa_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_sol_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_sol_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT ck_sol_linea_cantidad CHECK (cantidad_solicitada > 0),
    CONSTRAINT ck_sol_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_sol_linea_sku ON sch_abastecimiento.solicitud_compra_linea(tenant_id, sku_id) WHERE es_activo = '1';

CREATE TABLE sch_abastecimiento.orden_compra (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    proveedor_id                BIGINT NOT NULL,
    solicitud_id                BIGINT,
    establecimiento_destino_id  BIGINT NOT NULL,
    numero                      VARCHAR(50) NOT NULL,
    fecha_emision               DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_entrega_estimada      DATE,
    moneda                      CHAR(3) NOT NULL DEFAULT 'PEN',
    tipo_cambio                 NUMERIC(18, 6) DEFAULT 1.000000,
    condicion_pago              VARCHAR(80) DEFAULT 'CONTADO',
    dias_credito                INTEGER DEFAULT 0,
    subtotal                    NUMERIC(18, 2) NOT NULL DEFAULT 0,
    descuento_total             NUMERIC(18, 2) NOT NULL DEFAULT 0,
    impuesto_total              NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total                       NUMERIC(18, 2) NOT NULL DEFAULT 0,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    observacion                 VARCHAR(1500),
    aprobado_por_usuario_id     BIGINT,
    aprobado_at                 TIMESTAMPTZ,
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_orden_compra PRIMARY KEY (id),
    CONSTRAINT uk_orden_compra_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_orden_compra_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_oc_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_oc_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_abastecimiento.proveedor(tenant_id, id),
    CONSTRAINT fk_oc_solicitud FOREIGN KEY (tenant_id, empresa_id, solicitud_id) REFERENCES sch_abastecimiento.solicitud_compra(tenant_id, empresa_id, id),
    CONSTRAINT fk_oc_establecimiento_destino FOREIGN KEY (tenant_id, empresa_id, establecimiento_destino_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_oc_usuario_aprueba FOREIGN KEY (tenant_id, aprobado_por_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_oc_fechas CHECK (fecha_entrega_estimada IS NULL OR fecha_entrega_estimada >= fecha_emision),
    CONSTRAINT ck_oc_credito CHECK (dias_credito IS NULL OR dias_credito >= 0),
    CONSTRAINT ck_oc_estado CHECK (estado IN ('BORRADOR', 'EN_APROBACION', 'APROBADA', 'EMITIDA', 'PARCIALMENTE_RECIBIDA', 'RECIBIDA', 'CANCELADA', 'CERRADA')),
    CONSTRAINT ck_oc_totales CHECK (subtotal >= 0 AND descuento_total >= 0 AND impuesto_total >= 0 AND total >= 0),
    CONSTRAINT ck_oc_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_orden_compra_numero ON sch_abastecimiento.orden_compra(tenant_id, numero) WHERE es_activo = '1';
CREATE INDEX ix_oc_proveedor ON sch_abastecimiento.orden_compra(tenant_id, proveedor_id, estado) WHERE es_activo = '1';

CREATE TABLE sch_abastecimiento.orden_compra_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    orden_compra_id         BIGINT NOT NULL,
    numero_linea            INTEGER NOT NULL,
    sku_id                  BIGINT NOT NULL,
    descripcion_snapshot    VARCHAR(500),
    cantidad                NUMERIC(18, 4) NOT NULL,
    unidad_medida_codigo    VARCHAR(30) NOT NULL,
    precio_unitario         NUMERIC(18, 6) NOT NULL,
    descuento               NUMERIC(18, 2) NOT NULL DEFAULT 0,
    impuesto                NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_linea             NUMERIC(18, 2) NOT NULL,
    tolerancia_exceso_pct   NUMERIC(7, 4) DEFAULT 0,
    tolerancia_defecto_pct  NUMERIC(7, 4) DEFAULT 0,
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_orden_compra_linea PRIMARY KEY (id),
    CONSTRAINT uk_orden_compra_linea UNIQUE (orden_compra_id, numero_linea),
    CONSTRAINT uk_orden_compra_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_oc_linea_orden FOREIGN KEY (tenant_id, empresa_id, orden_compra_id) REFERENCES sch_abastecimiento.orden_compra(tenant_id, empresa_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_oc_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_oc_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT ck_oc_linea_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_oc_linea_importes CHECK (precio_unitario >= 0 AND descuento >= 0 AND impuesto >= 0 AND total_linea >= 0),
    CONSTRAINT ck_oc_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_oc_linea_sku ON sch_abastecimiento.orden_compra_linea(tenant_id, sku_id) WHERE es_activo = '1';

CREATE TABLE sch_abastecimiento.recepcion_compra (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    almacen_id                  BIGINT NOT NULL,
    orden_compra_id             BIGINT,
    proveedor_id                BIGINT NOT NULL,
    numero                      VARCHAR(50) NOT NULL,
    guia_remision_remitente     VARCHAR(80),
    guia_remision_transportista VARCHAR(80),
    documento_proveedor_tipo    VARCHAR(2) DEFAULT '01',
    documento_proveedor_serie   VARCHAR(20),
    documento_proveedor_numero  VARCHAR(40),
    fecha_recepcion             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    temperatura_recepcion_c     NUMERIC(6, 2),
    humedad_relativa_pct        NUMERIC(5, 2),
    condicion_transporte        VARCHAR(500),
    recibido_por_usuario_id     BIGINT,
    estado                      VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    observacion                 VARCHAR(1000),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_recepcion_compra PRIMARY KEY (id),
    CONSTRAINT uk_recepcion_compra_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_recepcion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_recepcion_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_recepcion_orden_compra FOREIGN KEY (tenant_id, empresa_id, orden_compra_id) REFERENCES sch_abastecimiento.orden_compra(tenant_id, empresa_id, id),
    CONSTRAINT fk_recepcion_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_abastecimiento.proveedor(tenant_id, id),
    CONSTRAINT fk_recepcion_usuario_recibe FOREIGN KEY (tenant_id, recibido_por_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_recepcion_estado CHECK (estado IN ('BORRADOR', 'EN_INSPECCION', 'CONFIRMADA', 'OBSERVADA', 'ANULADA')),
    CONSTRAINT ck_recepcion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_recepcion_compra_numero ON sch_abastecimiento.recepcion_compra(tenant_id, numero) WHERE es_activo = '1';
CREATE INDEX ix_recepcion_oc ON sch_abastecimiento.recepcion_compra(tenant_id, orden_compra_id) WHERE es_activo = '1';

CREATE TABLE sch_abastecimiento.recepcion_compra_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    recepcion_id            BIGINT NOT NULL,
    numero_linea            INTEGER NOT NULL,
    orden_compra_linea_id   BIGINT,
    sku_id                  BIGINT NOT NULL,
    numero_lote             VARCHAR(120) NOT NULL,
    fecha_fabricacion       DATE,
    fecha_vencimiento       DATE NOT NULL,
    cantidad_recibida       NUMERIC(18, 4) NOT NULL,
    cantidad_aceptada       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_cuarentena     NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_rechazada      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    costo_unitario          NUMERIC(18, 6),
    decision_calidad        VARCHAR(30) NOT NULL DEFAULT 'ACEPTADO',
    motivo_decision         VARCHAR(1000),
    observacion             VARCHAR(500),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_recepcion_compra_linea PRIMARY KEY (id),
    CONSTRAINT uk_recepcion_compra_linea UNIQUE (recepcion_id, numero_linea),
    CONSTRAINT uk_recepcion_compra_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_rec_linea_recepcion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, recepcion_id) REFERENCES sch_abastecimiento.recepcion_compra(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_linea_oc_linea FOREIGN KEY (tenant_id, orden_compra_linea_id) REFERENCES sch_abastecimiento.orden_compra_linea(tenant_id, id),
    CONSTRAINT fk_rec_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT ck_rec_linea_fechas CHECK (fecha_vencimiento IS NULL OR fecha_fabricacion IS NULL OR fecha_vencimiento >= fecha_fabricacion),
    CONSTRAINT ck_rec_linea_cantidades CHECK (
        cantidad_recibida > 0 AND cantidad_aceptada >= 0 AND cantidad_cuarentena >= 0 AND cantidad_rechazada >= 0 AND 
        (cantidad_aceptada + cantidad_cuarentena + cantidad_rechazada) <= cantidad_recibida
    ),
    CONSTRAINT ck_rec_linea_calidad CHECK (decision_calidad IN ('ACEPTADO', 'CUARENTENA', 'RECHAZADO', 'ACEPTADO_PARCIAL')),
    CONSTRAINT ck_rec_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_rec_linea_sku_lote ON sch_abastecimiento.recepcion_compra_linea(tenant_id, sku_id, numero_lote) WHERE es_activo = '1';

-- ============================================================================
-- 7. ESQUEMA: sch_inventario (Lotes, Stock WMS, Kardex y Transferencias)
-- ============================================================================

CREATE TABLE sch_inventario.lote (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    sku_id                      BIGINT NOT NULL,
    proveedor_id                BIGINT,
    numero_lote                 VARCHAR(120) NOT NULL,
    fecha_fabricacion           DATE,
    fecha_vencimiento           DATE NOT NULL,
    registro_sanitario_snapshot VARCHAR(100),
    fabricante_snapshot         VARCHAR(300),
    origen_recepcion_linea_id   BIGINT,
    estado_lote                 VARCHAR(30) NOT NULL DEFAULT 'HABILITADO',
    motivo_estado               VARCHAR(1000),
    bloqueado_at                TIMESTAMPTZ,
    bloqueado_por               VARCHAR(15),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_lote PRIMARY KEY (id),
    CONSTRAINT uk_lote_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_lote_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_lote_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_lote_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_lote_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_abastecimiento.proveedor(tenant_id, id),
    CONSTRAINT fk_lote_origen_recepcion FOREIGN KEY (tenant_id, origen_recepcion_linea_id) REFERENCES sch_abastecimiento.recepcion_compra_linea(tenant_id, id),
    CONSTRAINT ck_lote_fechas CHECK (fecha_fabricacion IS NULL OR fecha_vencimiento >= fecha_fabricacion),
    CONSTRAINT ck_lote_estado CHECK (estado_lote IN ('HABILITADO', 'CUARENTENA', 'BLOQUEADO', 'INMOVILIZADO_RECALL', 'VENCIDO', 'BAJA_DESTRUIDO')),
    CONSTRAINT ck_lote_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_lote_tenant_sku_numero_vcto ON sch_inventario.lote(tenant_id, sku_id, numero_lote, fecha_vencimiento) WHERE es_activo = '1';
CREATE INDEX ix_lote_fefo ON sch_inventario.lote(tenant_id, sku_id, fecha_vencimiento ASC) WHERE es_activo = '1' AND estado_lote = 'HABILITADO';

CREATE TABLE sch_inventario.posicion_inventario (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    almacen_id              BIGINT NOT NULL,
    ubicacion_id            BIGINT,
    sku_id                  BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    estado_inventario       VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    cantidad_fisica         NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_reservada      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_disponible     NUMERIC(18, 4) GENERATED ALWAYS AS (cantidad_fisica - cantidad_reservada) STORED,
    costo_promedio          NUMERIC(18, 6) NOT NULL DEFAULT 0,
    stock_minimo            NUMERIC(18, 4),
    stock_maximo            NUMERIC(18, 4),
    version_lock            BIGINT NOT NULL DEFAULT 0,
    ultimo_movimiento_at    TIMESTAMPTZ,
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_posicion_inventario PRIMARY KEY (id),
    CONSTRAINT uk_posicion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_posicion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_posicion_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_posicion_ubicacion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id, ubicacion_id) REFERENCES sch_organizacion.ubicacion_almacen(tenant_id, empresa_id, establecimiento_id, almacen_id, id),
    CONSTRAINT fk_posicion_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_posicion_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_posicion_cantidades CHECK (cantidad_fisica >= 0 AND cantidad_reservada >= 0 AND cantidad_reservada <= cantidad_fisica),
    CONSTRAINT ck_posicion_costos CHECK (costo_promedio >= 0 AND (stock_minimo IS NULL OR stock_minimo >= 0) AND (stock_maximo IS NULL OR stock_maximo >= COALESCE(stock_minimo, 0))),
    CONSTRAINT ck_posicion_estado CHECK (estado_inventario IN ('DISPONIBLE', 'CUARENTENA', 'BLOQUEADO', 'DANADO', 'VENCIDO', 'RECALL', 'TRANSITO')),
    CONSTRAINT ck_posicion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_posicion_natural ON sch_inventario.posicion_inventario(
    tenant_id, establecimiento_id, almacen_id, (COALESCE(ubicacion_id, 0)), sku_id, lote_id, estado_inventario
) WHERE es_activo = '1';
CREATE INDEX ix_posicion_stock_lookup ON sch_inventario.posicion_inventario(tenant_id, establecimiento_id, almacen_id, sku_id, estado_inventario) WHERE es_activo = '1';

CREATE TABLE sch_inventario.movimiento_inventario (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    sku_id                  BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    tipo_movimiento         VARCHAR(50) NOT NULL,
    tipo_operacion_sunat    VARCHAR(2) DEFAULT '01',
    naturaleza              CHAR(1) NOT NULL,
    cantidad                NUMERIC(18, 4) NOT NULL,
    costo_unitario          NUMERIC(18, 6) NOT NULL DEFAULT 0,
    costo_total             NUMERIC(18, 2) NOT NULL DEFAULT 0,
    almacen_origen_id       BIGINT,
    ubicacion_origen_id     BIGINT,
    almacen_destino_id      BIGINT,
    ubicacion_destino_id    BIGINT,
    stock_anterior          NUMERIC(18, 4) NOT NULL,
    stock_posterior         NUMERIC(18, 4) NOT NULL,
    documento_tipo          VARCHAR(50),
    documento_id            BIGINT,
    documento_uuid          UUID,
    business_uuid           UUID,
    correlation_id          UUID,
    fecha_negocio           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor                   VARCHAR(15) NOT NULL,
    observacion             VARCHAR(1000),
    metadata                JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_movimiento_inventario PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_inventario_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_movimiento_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_movimiento_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT fk_movimiento_alm_origen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_origen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_movimiento_alm_destino FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_destino_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_movimiento_naturaleza CHECK (naturaleza IN ('E', 'S')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_movimiento_costo CHECK (costo_unitario >= 0 AND costo_total >= 0)
);

CREATE INDEX ix_movimiento_kardex ON sch_inventario.movimiento_inventario(tenant_id, establecimiento_id, sku_id, fecha_negocio DESC);
CREATE INDEX ix_movimiento_lote ON sch_inventario.movimiento_inventario(tenant_id, lote_id, fecha_negocio DESC);

CREATE TABLE sch_inventario.reserva_inventario (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    almacen_id          BIGINT NOT NULL,
    sku_id              BIGINT NOT NULL,
    lote_id             BIGINT,
    tipo_origen         VARCHAR(30) NOT NULL,
    origen_uuid         UUID NOT NULL,
    idempotency_key     VARCHAR(150),
    cantidad            NUMERIC(18, 4) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    expira_at           TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    released_at         TIMESTAMPTZ,
    released_reason     VARCHAR(500),
    CONSTRAINT pk_reserva_inventario PRIMARY KEY (id),
    CONSTRAINT uk_reserva_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_reserva_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_reserva_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_reserva_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_reserva_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('ACTIVA', 'CONSUMIDA', 'LIBERADA', 'EXPIRADA', 'CANCELADA'))
);

CREATE UNIQUE INDEX uk_reserva_origen ON sch_inventario.reserva_inventario(tenant_id, tipo_origen, origen_uuid, sku_id, (COALESCE(lote_id, 0))) WHERE estado = 'ACTIVA';
CREATE INDEX ix_reserva_limpieza_expirados ON sch_inventario.reserva_inventario(expira_at) WHERE estado = 'ACTIVA';

CREATE TABLE sch_inventario.transferencia_inventario (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_origen_id   BIGINT NOT NULL,
    almacen_origen_id           BIGINT NOT NULL,
    establecimiento_destino_id  BIGINT NOT NULL,
    almacen_destino_id          BIGINT NOT NULL,
    numero                      VARCHAR(50) NOT NULL,
    motivo                      VARCHAR(500),
    prioridad                   VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    fecha_solicitud             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aprobado_at                 TIMESTAMPTZ,
    aprobado_por                VARCHAR(15),
    despachado_at               TIMESTAMPTZ,
    recibido_at                 TIMESTAMPTZ,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'SOLICITADA',
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_transferencia PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_transferencia_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_transferencia_alm_origen FOREIGN KEY (tenant_id, empresa_id, establecimiento_origen_id, almacen_origen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_transferencia_alm_destino FOREIGN KEY (tenant_id, empresa_id, establecimiento_destino_id, almacen_destino_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_transferencia_distintos CHECK (almacen_origen_id <> almacen_destino_id OR establecimiento_origen_id <> establecimiento_destino_id),
    CONSTRAINT ck_transferencia_prioridad CHECK (prioridad IN ('BAJA', 'NORMAL', 'ALTA', 'URGENTE')),
    CONSTRAINT ck_transferencia_estado CHECK (estado IN ('SOLICITADA', 'APROBADA', 'DESPACHADA', 'PARCIALMENTE_RECIBIDA', 'RECIBIDA', 'CERRADA', 'CANCELADA')),
    CONSTRAINT ck_transferencia_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_transferencia_numero ON sch_inventario.transferencia_inventario(tenant_id, numero) WHERE es_activo = '1';

CREATE TABLE sch_inventario.transferencia_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    transferencia_id        BIGINT NOT NULL,
    numero_linea            INTEGER NOT NULL,
    sku_id                  BIGINT NOT NULL,
    cantidad_solicitada     NUMERIC(18, 4) NOT NULL,
    cantidad_aprobada       NUMERIC(18, 4),
    observacion             VARCHAR(500),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_transferencia_linea PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_linea UNIQUE (transferencia_id, numero_linea),
    CONSTRAINT uk_transferencia_linea_scope_id UNIQUE (tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_linea_trans FOREIGN KEY (tenant_id, empresa_id, transferencia_id) REFERENCES sch_inventario.transferencia_inventario(tenant_id, empresa_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_trans_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT ck_trans_linea_cantidad CHECK (cantidad_solicitada > 0 AND (cantidad_aprobada IS NULL OR (cantidad_aprobada >= 0 AND cantidad_aprobada <= cantidad_solicitada))),
    CONSTRAINT ck_trans_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_inventario.transferencia_despacho (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    transferencia_id    BIGINT NOT NULL,
    guia_remision       VARCHAR(80),
    fecha_despacho      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    actor               VARCHAR(15) NOT NULL,
    observacion         VARCHAR(1000),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_transferencia_despacho PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_despacho_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_transferencia_despacho_scope_id UNIQUE (tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_despacho_trans FOREIGN KEY (tenant_id, empresa_id, transferencia_id) REFERENCES sch_inventario.transferencia_inventario(tenant_id, empresa_id, id),
    CONSTRAINT ck_trans_despacho_estado CHECK (estado IN ('CONFIRMADO', 'ANULADO')),
    CONSTRAINT ck_trans_despacho_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_inventario.transferencia_despacho_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    transferencia_id        BIGINT NOT NULL,
    despacho_id             BIGINT NOT NULL,
    transferencia_linea_id  BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    cantidad_despachada     NUMERIC(18, 4) NOT NULL,
    CONSTRAINT pk_transferencia_despacho_linea PRIMARY KEY (id),
    CONSTRAINT fk_trans_desp_linea_desp FOREIGN KEY (tenant_id, empresa_id, transferencia_id, despacho_id) REFERENCES sch_inventario.transferencia_despacho(tenant_id, empresa_id, transferencia_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_trans_desp_linea_linea FOREIGN KEY (tenant_id, empresa_id, transferencia_id, transferencia_linea_id) REFERENCES sch_inventario.transferencia_linea(tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_desp_linea_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_trans_desp_linea_cantidad CHECK (cantidad_despachada > 0)
);

CREATE TABLE sch_inventario.transferencia_recepcion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    transferencia_id    BIGINT NOT NULL,
    fecha_recepcion     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor               VARCHAR(15) NOT NULL,
    observacion         VARCHAR(1000),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_transferencia_recepcion PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_recepcion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_transferencia_recepcion_scope_id UNIQUE (tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_recepcion_trans FOREIGN KEY (tenant_id, empresa_id, transferencia_id) REFERENCES sch_inventario.transferencia_inventario(tenant_id, empresa_id, id),
    CONSTRAINT ck_trans_recepcion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_inventario.transferencia_recepcion_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    transferencia_id        BIGINT NOT NULL,
    recepcion_id            BIGINT NOT NULL,
    transferencia_linea_id  BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    cantidad_recibida       NUMERIC(18, 4) NOT NULL,
    diferencia_motivo       VARCHAR(500),
    CONSTRAINT pk_transferencia_recepcion_linea PRIMARY KEY (id),
    CONSTRAINT fk_trans_rec_linea_rec FOREIGN KEY (tenant_id, empresa_id, transferencia_id, recepcion_id) REFERENCES sch_inventario.transferencia_recepcion(tenant_id, empresa_id, transferencia_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_trans_rec_linea_linea FOREIGN KEY (tenant_id, empresa_id, transferencia_id, transferencia_linea_id) REFERENCES sch_inventario.transferencia_linea(tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_rec_linea_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_trans_rec_linea_cantidad CHECK (cantidad_recibida >= 0)
);

CREATE TABLE sch_inventario.conteo_inventario (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    almacen_id          BIGINT NOT NULL,
    numero              VARCHAR(50) NOT NULL,
    tipo_conteo         VARCHAR(20) NOT NULL,
    conteo_ciego        BOOLEAN NOT NULL DEFAULT TRUE,
    motivo              VARCHAR(500),
    estado              VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    iniciado_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cerrado_at          TIMESTAMPTZ,
    aprobado_at         TIMESTAMPTZ,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_conteo_inventario PRIMARY KEY (id),
    CONSTRAINT uk_conteo_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_conteo_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_conteo_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_organizacion.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_conteo_tipo CHECK (tipo_conteo IN ('TOTAL', 'CICLICO', 'SELECTIVO')),
    CONSTRAINT ck_conteo_estado CHECK (estado IN ('ABIERTO', 'EN_CONTEO', 'CONCILIADO', 'APROBADO', 'CERRADO', 'CANCELADO')),
    CONSTRAINT ck_conteo_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_conteo_numero ON sch_inventario.conteo_inventario(tenant_id, numero) WHERE es_activo = '1';

CREATE TABLE sch_inventario.conteo_inventario_linea (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    conteo_id           BIGINT NOT NULL,
    sku_id              BIGINT NOT NULL,
    lote_id             BIGINT,
    cantidad_sistema    NUMERIC(18, 4) NOT NULL,
    cantidad_contada    NUMERIC(18, 4),
    diferencia          NUMERIC(18, 4) GENERATED ALWAYS AS (COALESCE(cantidad_contada, cantidad_sistema) - cantidad_sistema) STORED,
    observacion         VARCHAR(500),
    CONSTRAINT pk_conteo_inventario_linea PRIMARY KEY (id),
    CONSTRAINT fk_conteo_linea_conteo FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, conteo_id) REFERENCES sch_inventario.conteo_inventario(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_conteo_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_conteo_linea_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_conteo_linea_cant CHECK (cantidad_sistema >= 0 AND (cantidad_contada IS NULL OR cantidad_contada >= 0))
);

-- ============================================================================
-- 8. ESQUEMA: sch_precio (Tarifarios, Precios de Fracción y Promociones)
-- ============================================================================

CREATE TABLE sch_precio.lista_precio (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT,
    codigo              VARCHAR(50) NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    descripcion         VARCHAR(500),
    moneda              CHAR(3) NOT NULL DEFAULT 'PEN',
    tipo_lista          VARCHAR(30) NOT NULL DEFAULT 'VENTA',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_lista_precio PRIMARY KEY (id),
    CONSTRAINT uk_lista_precio_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_lista_precio_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_lista_precio_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_lista_precio_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_lista_precio_tipo CHECK (tipo_lista IN ('VENTA', 'MAYORISTA', 'CONVENIO', 'ECOMMERCE', 'INSTITUCIONAL')),
    CONSTRAINT ck_lista_precio_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_lista_precio_codigo ON sch_precio.lista_precio(tenant_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_precio.lista_precio_version (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    lista_precio_id     BIGINT NOT NULL,
    numero_version      INTEGER NOT NULL,
    vigente_desde       DATE NOT NULL,
    vigente_hasta       DATE,
    periodo_vigencia    DATERANGE GENERATED ALWAYS AS (daterange(vigente_desde, vigente_hasta, '[]')) STORED,
    estado_publicacion  VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    motivo_cambio       VARCHAR(1000),
    publicado_at        TIMESTAMPTZ,
    publicado_por       VARCHAR(15),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_lista_precio_version PRIMARY KEY (id),
    CONSTRAINT uk_lista_precio_version_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_lista_precio_version_num UNIQUE (lista_precio_id, numero_version),
    CONSTRAINT uk_lista_precio_version_scope_id UNIQUE (tenant_id, lista_precio_id, id),
    CONSTRAINT fk_lista_precio_version_lista FOREIGN KEY (tenant_id, lista_precio_id) REFERENCES sch_precio.lista_precio(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_lista_precio_version_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_lista_precio_version_estado CHECK (estado_publicacion IN ('BORRADOR', 'PUBLICADO', 'RETIRADO')),
    CONSTRAINT ck_lista_precio_version_es_activo CHECK (es_activo IN ('0', '1')),
    CONSTRAINT ex_lista_precio_publicada_no_overlap EXCLUDE USING gist (lista_precio_id WITH =, periodo_vigencia WITH &&) WHERE (estado_publicacion = 'PUBLICADO' AND es_activo = '1')
);

CREATE TABLE sch_precio.lista_precio_item (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    lista_precio_id         BIGINT NOT NULL,
    version_id              BIGINT NOT NULL,
    sku_id                  BIGINT NOT NULL,
    establecimiento_id      BIGINT,
    canal                   VARCHAR(30) NOT NULL DEFAULT 'POS',
    precio                  NUMERIC(18, 4) NOT NULL,
    precio_fraccion         NUMERIC(18, 4),
    precio_minimo           NUMERIC(18, 4),
    margen_referencia       NUMERIC(10, 4),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_lista_precio_item PRIMARY KEY (id),
    CONSTRAINT fk_lp_item_version FOREIGN KEY (tenant_id, lista_precio_id, version_id) REFERENCES sch_precio.lista_precio_version(tenant_id, lista_precio_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_lp_item_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_lp_item_establecimiento FOREIGN KEY (tenant_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, id),
    CONSTRAINT ck_lp_item_precio CHECK (precio >= 0 AND (precio_fraccion IS NULL OR precio_fraccion >= 0) AND (precio_minimo IS NULL OR (precio_minimo >= 0 AND precio_minimo <= precio))),
    CONSTRAINT ck_lp_item_canal CHECK (canal IN ('POS', 'WEB', 'DELIVERY', 'CALL_CENTER', 'CONVENIO', 'TODOS')),
    CONSTRAINT ck_lp_item_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_lista_precio_item_natural ON sch_precio.lista_precio_item(version_id, sku_id, (COALESCE(establecimiento_id, 0)), canal) WHERE es_activo = '1';
CREATE INDEX ix_lista_precio_item_sku ON sch_precio.lista_precio_item(tenant_id, sku_id) WHERE es_activo = '1';

CREATE TABLE sch_precio.promocion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT,
    codigo              VARCHAR(50) NOT NULL,
    nombre              VARCHAR(250) NOT NULL,
    descripcion         VARCHAR(1000),
    tipo_promocion      VARCHAR(40) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_promocion PRIMARY KEY (id),
    CONSTRAINT uk_promocion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_promocion_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_promocion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_promocion_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_promocion_tipo CHECK (tipo_promocion IN ('DESCUENTO_PORCENTAJE', 'DESCUENTO_MONTO', 'PRECIO_ESPECIAL', 'NXM', 'COMBO', 'CUPON', 'PUNTOS_FIDELIDAD')),
    CONSTRAINT ck_promocion_estado CHECK (estado IN ('BORRADOR', 'ACTIVA', 'PAUSADA', 'CERRADA')),
    CONSTRAINT ck_promocion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_promocion_codigo ON sch_precio.promocion(tenant_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_precio.promocion_version (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    promocion_id        BIGINT NOT NULL,
    numero_version      INTEGER NOT NULL,
    vigente_desde       TIMESTAMPTZ NOT NULL,
    vigente_hasta       TIMESTAMPTZ,
    periodo_vigencia    TSTZRANGE GENERATED ALWAYS AS (tstzrange(vigente_desde, vigente_hasta, '[]')) STORED,
    canal               VARCHAR(30) NOT NULL DEFAULT 'TODOS',
    condiciones         JSONB NOT NULL DEFAULT '{}'::jsonb,
    beneficio           JSONB NOT NULL DEFAULT '{}'::jsonb,
    prioridad           INTEGER NOT NULL DEFAULT 100,
    combinable          BOOLEAN NOT NULL DEFAULT FALSE,
    max_usos_total      BIGINT,
    max_usos_cliente    INTEGER,
    estado_publicacion  VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_promocion_version PRIMARY KEY (id),
    CONSTRAINT uk_promocion_version_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_promocion_version_num UNIQUE (promocion_id, numero_version),
    CONSTRAINT uk_promocion_version_scope_id UNIQUE (tenant_id, promocion_id, id),
    CONSTRAINT fk_promocion_version_promocion FOREIGN KEY (tenant_id, promocion_id) REFERENCES sch_precio.promocion(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_promocion_version_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_promocion_version_limites CHECK ((max_usos_total IS NULL OR max_usos_total > 0) AND (max_usos_cliente IS NULL OR max_usos_cliente > 0)),
    CONSTRAINT ck_promocion_version_estado CHECK (estado_publicacion IN ('BORRADOR', 'PUBLICADO', 'RETIRADO')),
    CONSTRAINT ck_promocion_version_es_activo CHECK (es_activo IN ('0', '1')),
    CONSTRAINT ex_promocion_publicada_no_overlap EXCLUDE USING gist (promocion_id WITH =, periodo_vigencia WITH &&) WHERE (estado_publicacion = 'PUBLICADO' AND es_activo = '1')
);

CREATE INDEX ix_promocion_vigente ON sch_precio.promocion_version USING gist (tenant_id, periodo_vigencia) WHERE estado_publicacion = 'PUBLICADO' AND es_activo = '1';

CREATE TABLE sch_precio.promocion_objetivo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    promocion_id        BIGINT NOT NULL,
    version_id          BIGINT NOT NULL,
    tipo_objetivo       VARCHAR(30) NOT NULL,
    sku_id              BIGINT,
    categoria_id        BIGINT,
    establecimiento_id  BIGINT,
    marca_id            BIGINT,
    valor_texto         VARCHAR(200),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_promocion_objetivo PRIMARY KEY (id),
    CONSTRAINT fk_prom_obj_version FOREIGN KEY (tenant_id, promocion_id, version_id) REFERENCES sch_precio.promocion_version(tenant_id, promocion_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_prom_obj_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_prom_obj_categoria FOREIGN KEY (tenant_id, categoria_id) REFERENCES sch_catalogo.categoria_producto(tenant_id, id),
    CONSTRAINT fk_prom_obj_marca FOREIGN KEY (tenant_id, marca_id) REFERENCES sch_catalogo.marca(tenant_id, id),
    CONSTRAINT fk_prom_obj_establecimiento FOREIGN KEY (tenant_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, id),
    CONSTRAINT ck_prom_obj_tipo CHECK (tipo_objetivo IN ('SKU', 'CATEGORIA', 'ESTABLECIMIENTO', 'MARCA', 'SEGMENTO_CLIENTE')),
    CONSTRAINT ck_prom_obj_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_prom_obj_lookup ON sch_precio.promocion_objetivo(tenant_id, version_id, tipo_objetivo) WHERE es_activo = '1';

-- ============================================================================
-- 9. ESQUEMA: sch_venta (Punto de Venta POS, Clientes, Turnos y Pagos)
-- ============================================================================

CREATE TABLE sch_venta.cliente (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    tipo_cliente        VARCHAR(30) NOT NULL DEFAULT 'NATURAL',
    tipo_documento      VARCHAR(2),
    numero_documento    VARCHAR(15),
    nombres             VARCHAR(150),
    apellidos           VARCHAR(180),
    razon_social        VARCHAR(300),
    email               CITEXT,
    telefono            VARCHAR(40),
    fecha_nacimiento    DATE,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_cliente PRIMARY KEY (id),
    CONSTRAINT uk_cliente_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cliente_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_cliente_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_cliente_tipo CHECK (tipo_cliente IN ('NATURAL', 'JURIDICO', 'SIN_IDENTIFICAR')),
    CONSTRAINT ck_cliente_estado CHECK (estado IN ('ACTIVO', 'BLOQUEADO')),
    CONSTRAINT ck_cliente_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_cliente_documento ON sch_venta.cliente(tenant_id, tipo_documento, numero_documento) WHERE es_activo = '1' AND numero_documento IS NOT NULL;
CREATE INDEX ix_cliente_nombre ON sch_venta.cliente(tenant_id, apellidos, nombres) WHERE es_activo = '1';

CREATE TABLE sch_venta.cliente_consentimiento (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    cliente_id          BIGINT NOT NULL,
    finalidad_codigo    VARCHAR(50) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'OTORGADO',
    otorgado_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revocado_at         TIMESTAMPTZ,
    evidencia_uri       TEXT,
    fuente              VARCHAR(80) NOT NULL DEFAULT 'POS',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_cliente_consentimiento PRIMARY KEY (id),
    CONSTRAINT uk_cliente_consentimiento_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_cliente_consent_cliente FOREIGN KEY (tenant_id, cliente_id) REFERENCES sch_venta.cliente(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_cliente_consent_estado CHECK (estado IN ('OTORGADO', 'REVOCADO', 'NO_OTORGADO')),
    CONSTRAINT ck_cliente_consent_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_cliente_finalidad_activa ON sch_venta.cliente_consentimiento(tenant_id, cliente_id, finalidad_codigo) WHERE es_activo = '1' AND estado = 'OTORGADO';

CREATE TABLE sch_venta.medio_pago (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    codigo              VARCHAR(50) NOT NULL,
    nombre              VARCHAR(120) NOT NULL,
    tipo                VARCHAR(30) NOT NULL,
    requiere_referencia BOOLEAN NOT NULL DEFAULT FALSE,
    permite_vuelto      BOOLEAN NOT NULL DEFAULT FALSE,
    proveedor_default   VARCHAR(80),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_medio_pago PRIMARY KEY (id),
    CONSTRAINT uk_medio_pago_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_medio_pago_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_medio_pago_tipo CHECK (tipo IN ('EFECTIVO', 'TARJETA', 'BILLETERA_DIGITAL', 'TRANSFERENCIA', 'CREDITO', 'VALE_CONSUMO', 'OTRO')),
    CONSTRAINT ck_medio_pago_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_medio_pago_codigo ON sch_venta.medio_pago(tenant_id, codigo) WHERE es_activo = '1';

CREATE TABLE sch_venta.turno_caja (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    terminal_id             BIGINT NOT NULL,
    cajero_usuario_id       BIGINT NOT NULL,
    apertura_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fondo_inicial           NUMERIC(18, 2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    cierre_at               TIMESTAMPTZ,
    total_ventas_sistema    NUMERIC(18, 2) DEFAULT 0,
    total_ingresos_sistema  NUMERIC(18, 2) DEFAULT 0,
    total_retiros_sistema   NUMERIC(18, 2) DEFAULT 0,
    total_sistema           NUMERIC(18, 2) DEFAULT 0,
    total_declarado         NUMERIC(18, 2),
    diferencia              NUMERIC(18, 2),
    observacion_cierre      VARCHAR(1000),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_turno_caja PRIMARY KEY (id),
    CONSTRAINT uk_turno_caja_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_turno_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_turno_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_organizacion.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_turno_cajero FOREIGN KEY (tenant_id, cajero_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_turno_caja_estado CHECK (estado IN ('ABIERTO', 'EN_ARQUEO', 'CERRADO', 'ANULADO')),
    CONSTRAINT ck_turno_fondo CHECK (fondo_inicial >= 0),
    CONSTRAINT ck_turno_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_turno_terminal_abierto ON sch_venta.turno_caja(tenant_id, terminal_id) WHERE es_activo = '1' AND estado IN ('ABIERTO', 'EN_ARQUEO');

CREATE TABLE sch_venta.movimiento_caja (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    turno_caja_id       BIGINT NOT NULL,
    tipo                VARCHAR(30) NOT NULL,
    concepto            VARCHAR(180) NOT NULL,
    monto               NUMERIC(18, 2) NOT NULL,
    moneda              CHAR(3) NOT NULL DEFAULT 'PEN',
    referencia_tipo     VARCHAR(60),
    referencia_uuid     UUID,
    motivo              VARCHAR(500),
    actor_usuario_id    BIGINT NOT NULL,
    ocurrido_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_movimiento_caja PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_caja_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_movimiento_caja_turno FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, turno_caja_id) REFERENCES sch_venta.turno_caja(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_movimiento_caja_actor FOREIGN KEY (tenant_id, actor_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_movimiento_caja_tipo CHECK (tipo IN ('INGRESO_MANUAL', 'RETIRO_SEGURIDAD', 'AJUSTE_SENCILLO')),
    CONSTRAINT ck_movimiento_caja_monto CHECK (monto > 0)
);

CREATE TABLE sch_venta.venta (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    terminal_id             BIGINT NOT NULL,
    turno_caja_id           BIGINT NOT NULL,
    cliente_id              BIGINT,
    vendedor_usuario_id     BIGINT NOT NULL,
    business_uuid           UUID NOT NULL DEFAULT uuidv7(),
    numero_operacion        VARCHAR(60) NOT NULL,
    idempotency_key         VARCHAR(160) NOT NULL,
    origen_operacion        VARCHAR(20) NOT NULL DEFAULT 'CENTRAL',
    store_sequence          BIGINT,
    canal                   VARCHAR(30) NOT NULL DEFAULT 'TIENDA',
    tipo_venta              VARCHAR(30) NOT NULL DEFAULT 'PRESENCIAL',
    pedido_externo_ref      VARCHAR(150),
    fecha_venta             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    moneda                  CHAR(3) NOT NULL DEFAULT 'PEN',
    subtotal                NUMERIC(18, 2) NOT NULL,
    descuento_total         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    impuesto_total          NUMERIC(18, 2) NOT NULL DEFAULT 0,
    redondeo                NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total                   NUMERIC(18, 2) NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADA',
    version_lock            BIGINT NOT NULL DEFAULT 0,
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_venta PRIMARY KEY (id),
    CONSTRAINT uk_venta_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_venta_business_uuid UNIQUE (tenant_id, business_uuid),
    CONSTRAINT uk_venta_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_venta_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_venta_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_organizacion.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_venta_turno FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, turno_caja_id) REFERENCES sch_venta.turno_caja(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_venta_cliente FOREIGN KEY (tenant_id, cliente_id) REFERENCES sch_venta.cliente(tenant_id, id),
    CONSTRAINT fk_venta_vendedor FOREIGN KEY (tenant_id, vendedor_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_venta_origen CHECK (origen_operacion IN ('CENTRAL', 'STORE_EDGE')),
    CONSTRAINT ck_venta_tipo CHECK (tipo_venta IN ('PRESENCIAL', 'DELIVERY', 'RECOJO_TIENDA', 'CALL_CENTER')),
    CONSTRAINT ck_venta_estado CHECK (estado IN ('CONFIRMADA', 'ANULADA', 'PARCIALMENTE_DEVUELTA', 'DEVUELTA')),
    CONSTRAINT ck_venta_totales CHECK (subtotal >= 0 AND descuento_total >= 0 AND impuesto_total >= 0 AND total >= 0),
    CONSTRAINT ck_venta_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_venta_numero_operacion ON sch_venta.venta(tenant_id, empresa_id, numero_operacion) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_venta_idempotency ON sch_venta.venta(tenant_id, idempotency_key) WHERE es_activo = '1';
CREATE INDEX ix_venta_fecha ON sch_venta.venta(tenant_id, establecimiento_id, fecha_venta DESC) WHERE es_activo = '1';
CREATE INDEX ix_venta_cliente ON sch_venta.venta(tenant_id, cliente_id, fecha_venta DESC) WHERE es_activo = '1';

CREATE TABLE sch_venta.venta_linea (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    venta_id                    BIGINT NOT NULL,
    numero_linea                INTEGER NOT NULL,
    sku_id                      BIGINT NOT NULL,
    descripcion_snapshot        VARCHAR(500) NOT NULL,
    unidad_venta_codigo         VARCHAR(30) NOT NULL,
    es_fraccion                 BOOLEAN NOT NULL DEFAULT FALSE,
    cantidad                    NUMERIC(18, 4) NOT NULL,
    precio_lista                NUMERIC(18, 4),
    precio_unitario             NUMERIC(18, 4) NOT NULL,
    descuento                   NUMERIC(18, 2) NOT NULL DEFAULT 0,
    impuesto                    NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_linea                 NUMERIC(18, 2) NOT NULL,
    requiere_dispensacion       BOOLEAN NOT NULL DEFAULT FALSE,
    dispensacion_linea_id       BIGINT, -- Se enlazará por FK tras crear sch_dispensacion
    promocion_snapshot          JSONB NOT NULL DEFAULT '{}'::jsonb,
    price_decision_snapshot     JSONB NOT NULL DEFAULT '{}'::jsonb,
    regulatory_snapshot         JSONB NOT NULL DEFAULT '{}'::jsonb,
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_venta_linea PRIMARY KEY (id),
    CONSTRAINT uk_venta_linea UNIQUE (venta_id, numero_linea),
    CONSTRAINT uk_venta_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_venta_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, venta_id, id),
    CONSTRAINT fk_venta_linea_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_venta.venta(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_venta_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_venta_linea_unidad FOREIGN KEY (unidad_venta_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT ck_venta_linea_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_venta_linea_importes CHECK ((precio_lista IS NULL OR precio_lista >= 0) AND precio_unitario >= 0 AND descuento >= 0 AND impuesto >= 0 AND total_linea >= 0),
    CONSTRAINT ck_venta_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_venta_linea_sku ON sch_venta.venta_linea(tenant_id, sku_id) WHERE es_activo = '1';

CREATE TABLE sch_venta.venta_linea_lote (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    venta_linea_id          BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    cantidad                NUMERIC(18, 4) NOT NULL,
    costo_unitario_snapshot NUMERIC(18, 6),
    CONSTRAINT pk_venta_linea_lote PRIMARY KEY (id),
    CONSTRAINT uk_venta_linea_lote UNIQUE (venta_linea_id, lote_id),
    CONSTRAINT fk_vll_linea FOREIGN KEY (tenant_id, venta_linea_id) REFERENCES sch_venta.venta_linea(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_vll_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_vll_cantidad CHECK (cantidad > 0)
);

CREATE INDEX ix_vll_lote ON sch_venta.venta_linea_lote(tenant_id, lote_id);

CREATE TABLE sch_venta.pago_venta (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    venta_id            BIGINT NOT NULL,
    medio_pago_id       BIGINT NOT NULL,
    monto               NUMERIC(18, 2) NOT NULL,
    monto_recibido      NUMERIC(18, 2),
    vuelto              NUMERIC(18, 2) DEFAULT 0,
    moneda              CHAR(3) NOT NULL DEFAULT 'PEN',
    proveedor_pago      VARCHAR(80),
    referencia_externa  VARCHAR(200),
    codigo_autorizacion VARCHAR(120),
    idempotency_key     VARCHAR(160),
    estado              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    pagado_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata            JSONB NOT NULL DEFAULT '{}'::jsonb,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_pago_venta PRIMARY KEY (id),
    CONSTRAINT uk_pago_venta_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_pago_venta_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_venta.venta(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_pago_venta_medio FOREIGN KEY (tenant_id, medio_pago_id) REFERENCES sch_venta.medio_pago(tenant_id, id),
    CONSTRAINT ck_pago_venta_monto CHECK (monto > 0),
    CONSTRAINT ck_pago_venta_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'RECHAZADO', 'REVERSADO', 'EXTORNADO')),
    CONSTRAINT ck_pago_venta_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_pago_venta_venta ON sch_venta.pago_venta(tenant_id, venta_id) WHERE es_activo = '1';

CREATE TABLE sch_venta.devolucion_comercial (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    venta_id                    BIGINT NOT NULL,
    numero                      VARCHAR(50) NOT NULL,
    motivo_codigo               VARCHAR(50),
    motivo                      VARCHAR(1000) NOT NULL,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    total_reembolso             NUMERIC(18, 2) NOT NULL DEFAULT 0,
    aprobado_por_usuario_id     BIGINT,
    aprobado_at                 TIMESTAMPTZ,
    reembolsado_at              TIMESTAMPTZ,
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_devolucion_comercial PRIMARY KEY (id),
    CONSTRAINT uk_devolucion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_devolucion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_devolucion_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_venta.venta(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_devolucion_aprobador FOREIGN KEY (tenant_id, aprobado_por_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_devolucion_estado CHECK (estado IN ('REGISTRADA', 'EN_REVISION', 'APROBADA', 'RECHAZADA', 'REEMBOLSADA', 'CERRADA')),
    CONSTRAINT ck_devolucion_total CHECK (total_reembolso >= 0),
    CONSTRAINT ck_devolucion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_devolucion_numero ON sch_venta.devolucion_comercial(tenant_id, numero) WHERE es_activo = '1';

CREATE TABLE sch_venta.devolucion_comercial_linea (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    devolucion_id               BIGINT NOT NULL,
    venta_linea_id              BIGINT NOT NULL,
    cantidad                    NUMERIC(18, 4) NOT NULL,
    monto_devuelto              NUMERIC(18, 2) NOT NULL DEFAULT 0,
    disposicion_inventario      VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_EVALUACION',
    evaluado_por_usuario_id     BIGINT,
    evaluado_at                 TIMESTAMPTZ,
    observacion                 VARCHAR(1000),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_devolucion_linea PRIMARY KEY (id),
    CONSTRAINT uk_devolucion_linea UNIQUE (devolucion_id, venta_linea_id),
    CONSTRAINT uk_devolucion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_dev_linea_devolucion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, devolucion_id) REFERENCES sch_venta.devolucion_comercial(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_dev_linea_venta_linea FOREIGN KEY (tenant_id, venta_linea_id) REFERENCES sch_venta.venta_linea(tenant_id, id),
    CONSTRAINT fk_dev_linea_evaluador FOREIGN KEY (tenant_id, evaluado_por_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
    CONSTRAINT ck_devolucion_linea_cantidad CHECK (cantidad > 0 AND monto_devuelto >= 0),
    CONSTRAINT ck_devolucion_disposicion CHECK (disposicion_inventario IN ('PENDIENTE_EVALUACION', 'REINTEGRO_DISPONIBLE', 'CUARENTENA', 'MERMA_BAJA', 'DISPOSICION_FINAL')),
    CONSTRAINT ck_devolucion_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_venta.devolucion_linea_lote (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    devolucion_linea_id     BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    cantidad                NUMERIC(18, 4) NOT NULL,
    CONSTRAINT pk_devolucion_linea_lote PRIMARY KEY (id),
    CONSTRAINT fk_dev_lote_linea FOREIGN KEY (tenant_id, devolucion_linea_id) REFERENCES sch_venta.devolucion_comercial_linea(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_dev_lote_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_dev_lote_cantidad CHECK (cantidad > 0)
);

-- ============================================================================
-- 10. ESQUEMA: sch_dispensacion (Prescripción, Atención Clínica y Controlados)
-- ============================================================================

CREATE TABLE sch_dispensacion.prescripcion (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    tipo_receta                     VARCHAR(40) NOT NULL DEFAULT 'SIMPLE',
    numero_receta                   VARCHAR(120),
    fecha_emision                   DATE NOT NULL,
    fecha_vencimiento               DATE,
    paciente_tipo_documento         VARCHAR(2) DEFAULT '1',
    paciente_numero_documento       VARCHAR(15),
    paciente_nombre_completo        VARCHAR(300) NOT NULL,
    paciente_edad                   SMALLINT,
    paciente_diagnostico_cie10      VARCHAR(10),
    prescriptor_nombre              VARCHAR(300) NOT NULL,
    prescriptor_colegio             VARCHAR(10) NOT NULL DEFAULT 'CMP',
    prescriptor_colegiatura         VARCHAR(30) NOT NULL,
    prescriptor_especialidad        VARCHAR(150),
    establecimiento_salud_origen    VARCHAR(300),
    estado_validacion               VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    validado_por_profesional_id     BIGINT,
    validado_at                     TIMESTAMPTZ,
    motivo_invalidacion             VARCHAR(1000),
    receta_digital_uri              TEXT,
    datos_adicionales               JSONB NOT NULL DEFAULT '{}'::jsonb,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(15),
    CONSTRAINT pk_prescripcion PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_prescripcion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_prescripcion_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_prescripcion_validador FOREIGN KEY (tenant_id, validado_por_profesional_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_prescripcion_tipo CHECK (tipo_receta IN ('SIMPLE', 'RETENIDA', 'ESPECIAL_ESTUPEFACIENTES', 'ELECTRONICA')),
    CONSTRAINT ck_prescripcion_colegio CHECK (prescriptor_colegio IN ('CMP', 'COP', 'CMVP', 'OTRO')),
    CONSTRAINT ck_prescripcion_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_emision),
    CONSTRAINT ck_prescripcion_estado CHECK (estado_validacion IN ('PENDIENTE', 'VALIDA', 'INVALIDA', 'VENCIDA', 'ATENDIDA_TOTAL', 'ATENDIDA_PARCIAL', 'ANULADA')),
    CONSTRAINT ck_prescripcion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_prescripcion_busqueda ON sch_dispensacion.prescripcion(tenant_id, establecimiento_id, estado_validacion, fecha_emision DESC) WHERE es_activo = '1';
CREATE INDEX ix_prescripcion_paciente ON sch_dispensacion.prescripcion(tenant_id, paciente_numero_documento) WHERE es_activo = '1' AND paciente_numero_documento IS NOT NULL;

CREATE TABLE sch_dispensacion.prescripcion_linea (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    prescripcion_id             BIGINT NOT NULL,
    numero_linea                INTEGER NOT NULL,
    producto_regulado_id        BIGINT,
    principio_activo_dci        VARCHAR(300),
    descripcion_prescrita       VARCHAR(500) NOT NULL,
    cantidad_prescrita          NUMERIC(18, 4) NOT NULL,
    unidad_medida_codigo        VARCHAR(30) NOT NULL,
    dosis                       VARCHAR(200),
    frecuencia                  VARCHAR(200),
    duracion                    VARCHAR(200),
    via_administracion_codigo   VARCHAR(30),
    indicaciones                VARCHAR(1500),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_prescripcion_linea PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_linea UNIQUE (prescripcion_id, numero_linea),
    CONSTRAINT uk_prescripcion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_prescripcion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, prescripcion_id, id),
    CONSTRAINT fk_pres_linea_prescripcion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_dispensacion.prescripcion(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_pres_linea_producto_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_pres_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT fk_pres_linea_via FOREIGN KEY (via_administracion_codigo) REFERENCES sch_catalogo.via_administracion(codigo),
    CONSTRAINT ck_pres_linea_cantidad CHECK (cantidad_prescrita > 0),
    CONSTRAINT ck_pres_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_dispensacion.dispensacion (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    prescripcion_id             BIGINT,
    profesional_id              BIGINT NOT NULL,
    fecha_dispensacion          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADA',
    decision_farmaceutica       VARCHAR(60) NOT NULL DEFAULT 'CONFORME',
    informacion_brindada        TEXT,
    advertencias_brindadas      TEXT,
    evidencia_entrega           JSONB NOT NULL DEFAULT '{}'::jsonb,
    observacion                 VARCHAR(1500),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_dispensacion PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_dispensacion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_dispensacion_prescripcion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_dispensacion.prescripcion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_profesional FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_dispensacion_estado CHECK (estado IN ('EN_EVALUACION', 'AUTORIZADA', 'CONFIRMADA', 'RECHAZADA', 'ANULADA')),
    CONSTRAINT ck_dispensacion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_dispensacion_fecha ON sch_dispensacion.dispensacion(tenant_id, establecimiento_id, fecha_dispensacion DESC) WHERE es_activo = '1';
CREATE INDEX ix_dispensacion_profesional ON sch_dispensacion.dispensacion(tenant_id, profesional_id, fecha_dispensacion DESC) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.dispensacion_linea (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    dispensacion_id             BIGINT NOT NULL,
    numero_linea                INTEGER NOT NULL,
    prescripcion_linea_id       BIGINT,
    sku_id                      BIGINT NOT NULL,
    cantidad_autorizada         NUMERIC(18, 4) NOT NULL,
    cantidad_entregada          NUMERIC(18, 4) NOT NULL DEFAULT 0,
    decision_linea              VARCHAR(30) NOT NULL DEFAULT 'AUTORIZADA',
    motivo_sustitucion          VARCHAR(1000),
    regulatory_snapshot         JSONB NOT NULL DEFAULT '{}'::jsonb,
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_dispensacion_linea PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea UNIQUE (dispensacion_id, numero_linea),
    CONSTRAINT uk_dispensacion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_dispensacion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, dispensacion_id, id),
    CONSTRAINT fk_disp_linea_dispensacion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, dispensacion_id) REFERENCES sch_dispensacion.dispensacion(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_disp_linea_prescripcion_linea FOREIGN KEY (tenant_id, prescripcion_linea_id) REFERENCES sch_dispensacion.prescripcion_linea(tenant_id, id),
    CONSTRAINT fk_disp_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT ck_disp_linea_cantidades CHECK (cantidad_autorizada > 0 AND cantidad_entregada >= 0 AND cantidad_entregada <= cantidad_autorizada),
    CONSTRAINT ck_disp_linea_decision CHECK (decision_linea IN ('AUTORIZADA', 'SUSTITUIDA_GENERICO', 'RECHAZADA', 'PARCIAL')),
    CONSTRAINT ck_disp_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_disp_linea_sku ON sch_dispensacion.dispensacion_linea(tenant_id, sku_id) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.dispensacion_linea_lote (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    dispensacion_linea_id   BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    cantidad                NUMERIC(18, 4) NOT NULL,
    CONSTRAINT pk_dispensacion_linea_lote PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea_lote UNIQUE (dispensacion_linea_id, lote_id),
    CONSTRAINT fk_disp_lote_linea FOREIGN KEY (tenant_id, dispensacion_linea_id) REFERENCES sch_dispensacion.dispensacion_linea(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_disp_lote_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_disp_lote_cantidad CHECK (cantidad > 0)
);

CREATE INDEX ix_disp_lote_lote ON sch_dispensacion.dispensacion_linea_lote(tenant_id, lote_id);

ALTER TABLE sch_venta.venta_linea
    ADD CONSTRAINT fk_venta_linea_dispensacion_linea 
    FOREIGN KEY (tenant_id, dispensacion_linea_id) 
    REFERENCES sch_dispensacion.dispensacion_linea(tenant_id, id);

CREATE TABLE sch_dispensacion.receta_controlada (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    prescripcion_id                 BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    tipo_receta_especial            VARCHAR(50) NOT NULL,
    numero_receta                   VARCHAR(120) NOT NULL,
    fecha_expedicion                TIMESTAMPTZ NOT NULL,
    fecha_limite_at                 TIMESTAMPTZ NOT NULL,
    estado_validacion               VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    motivo_invalidacion             VARCHAR(1000),
    retenida                        BOOLEAN NOT NULL DEFAULT TRUE,
    retenida_at                     TIMESTAMPTZ,
    archivada_at                    TIMESTAMPTZ,
    conservar_hasta                 DATE,
    profesional_validador_id        BIGINT NOT NULL,
    evidencia_uri                   TEXT,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(15),
    CONSTRAINT pk_receta_controlada PRIMARY KEY (id),
    CONSTRAINT uk_receta_controlada_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_receta_controlada_pres UNIQUE (prescripcion_id),
    CONSTRAINT uk_receta_controlada_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_rec_ctrl_prescripcion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_dispensacion.prescripcion(tenant_id, empresa_id, establecimiento_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_rec_ctrl_clasificacion FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_catalogo.clasificacion_controlada(codigo),
    CONSTRAINT fk_rec_ctrl_profesional FOREIGN KEY (tenant_id, profesional_validador_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_rec_ctrl_fechas CHECK (fecha_limite_at >= fecha_expedicion),
    CONSTRAINT ck_rec_ctrl_estado CHECK (estado_validacion IN ('PENDIENTE', 'VALIDA', 'INVALIDA', 'VENCIDA', 'ADULTERADA', 'ATENDIDA', 'ANULADA')),
    CONSTRAINT ck_rec_ctrl_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_rec_ctrl_establecimiento ON sch_dispensacion.receta_controlada(tenant_id, establecimiento_id, estado_validacion) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.movimiento_controlado (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    sku_id                          BIGINT NOT NULL,
    lote_id                         BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    tipo_movimiento                 VARCHAR(30) NOT NULL,
    cantidad                        NUMERIC(18, 4) NOT NULL,
    saldo_anterior                  NUMERIC(18, 4) NOT NULL,
    saldo_posterior                 NUMERIC(18, 4) NOT NULL,
    documento_tipo                  VARCHAR(50) NOT NULL,
    documento_uuid                  UUID,
    receta_controlada_id            BIGINT,
    profesional_id                  BIGINT NOT NULL,
    fecha_movimiento                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion                     VARCHAR(1000),
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_movimiento_controlado PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_controlado_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_mov_ctrl_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_mov_ctrl_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT fk_mov_ctrl_clasificacion FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_catalogo.clasificacion_controlada(codigo),
    CONSTRAINT fk_mov_ctrl_profesional FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT fk_mov_ctrl_receta FOREIGN KEY (receta_controlada_id) REFERENCES sch_dispensacion.receta_controlada(id),
    CONSTRAINT ck_mov_ctrl_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_mov_ctrl_saldos CHECK (saldo_anterior >= 0 AND saldo_posterior >= 0),
    CONSTRAINT ck_mov_ctrl_tipo CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO', 'DEVOLUCION', 'DISPOSICION'))
);

CREATE INDEX ix_mov_ctrl_libro ON sch_dispensacion.movimiento_controlado(tenant_id, establecimiento_id, sku_id, fecha_movimiento DESC);

CREATE TABLE sch_dispensacion.balance_controlado (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    periodo_desde                   DATE NOT NULL,
    periodo_hasta                   DATE NOT NULL,
    fecha_limite_presentacion       DATE,
    estado                          VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    quimico_responsable_id          BIGINT NOT NULL,
    generado_at                     TIMESTAMPTZ,
    validado_at                     TIMESTAMPTZ,
    presentado_at                   TIMESTAMPTZ,
    identificador_presentacion      VARCHAR(150),
    evidencia_uri                   TEXT,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(15),
    CONSTRAINT pk_balance_controlado PRIMARY KEY (id),
    CONSTRAINT uk_balance_controlado_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_balance_controlado_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_bal_ctrl_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_bal_ctrl_clasificacion FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_catalogo.clasificacion_controlada(codigo),
    CONSTRAINT fk_bal_ctrl_quimico FOREIGN KEY (tenant_id, quimico_responsable_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_bal_ctrl_fechas CHECK (periodo_hasta >= periodo_desde),
    CONSTRAINT ck_bal_ctrl_estado CHECK (estado IN ('BORRADOR', 'GENERADO', 'VALIDADO', 'PRESENTADO', 'OBSERVADO', 'RECTIFICADO')),
    CONSTRAINT ck_bal_ctrl_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_balance_controlado_periodo ON sch_dispensacion.balance_controlado(tenant_id, establecimiento_id, clasificacion_controlada_codigo, periodo_desde, periodo_hasta) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.balance_controlado_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    balance_id              BIGINT NOT NULL,
    sku_id                  BIGINT NOT NULL,
    existencia_inicial      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    entradas                NUMERIC(18, 4) NOT NULL DEFAULT 0,
    salidas                 NUMERIC(18, 4) NOT NULL DEFAULT 0,
    ajustes_positivos       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    ajustes_negativos       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    existencia_final        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    CONSTRAINT pk_balance_controlado_linea PRIMARY KEY (id),
    CONSTRAINT uk_bal_ctrl_linea UNIQUE (balance_id, sku_id),
    CONSTRAINT fk_bal_ctrl_linea_balance FOREIGN KEY (tenant_id, balance_id) REFERENCES sch_dispensacion.balance_controlado(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_bal_ctrl_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT ck_bal_linea_cantidades CHECK (existencia_inicial >= 0 AND entradas >= 0 AND salidas >= 0 AND ajustes_positivos >= 0 AND ajustes_negativos >= 0 AND existencia_final >= 0),
    CONSTRAINT ck_bal_linea_cuadre CHECK (existencia_final = (existencia_inicial + entradas - salidas + ajustes_positivos - ajustes_negativos))
);

CREATE INDEX ix_bal_ctrl_linea_sku ON sch_dispensacion.balance_controlado_linea(tenant_id, sku_id);

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

-- ============================================================================
-- 12. ESQUEMA: sch_vigilancia (Alertas Recall, Cuarentena y RAMs)
-- ============================================================================

CREATE TABLE sch_vigilancia.caso_recall (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    codigo_caso         VARCHAR(80) NOT NULL,
    tipo_alerta         VARCHAR(40) NOT NULL DEFAULT 'RECALL',
    autoridad_fuente    VARCHAR(150) NOT NULL DEFAULT 'DIGEMID',
    fuente              VARCHAR(300) NOT NULL,
    referencia_externa  VARCHAR(250),
    fecha_publicacion   TIMESTAMPTZ,
    fecha_alerta        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo              VARCHAR(2000) NOT NULL,
    nivel_riesgo        VARCHAR(30) NOT NULL DEFAULT 'CLASE_II',
    instruccion_oficial TEXT NOT NULL,
    estado              VARCHAR(30) NOT NULL DEFAULT 'ABIERTO',
    evidencia_uri       TEXT,
    closed_at           TIMESTAMPTZ,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_caso_recall PRIMARY KEY (id),
    CONSTRAINT uk_caso_recall_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_caso_recall_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_caso_recall_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_caso_recall_riesgo CHECK (nivel_riesgo IN ('CLASE_I', 'CLASE_II', 'CLASE_III', 'INFORMATIVO')),
    CONSTRAINT ck_caso_recall_estado CHECK (estado IN ('ABIERTO', 'EN_EJECUCION', 'CONCILIACION', 'CERRADO', 'CANCELADO')),
    CONSTRAINT ck_caso_recall_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_caso_recall_codigo ON sch_vigilancia.caso_recall(tenant_id, codigo_caso) WHERE es_activo = '1';
CREATE INDEX ix_recall_estado ON sch_vigilancia.caso_recall(tenant_id, estado, fecha_alerta DESC) WHERE es_activo = '1';

CREATE TABLE sch_vigilancia.recall_producto_lote (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    caso_recall_id              BIGINT NOT NULL,
    producto_regulado_id        BIGINT,
    sku_id                      BIGINT,
    lote_id                     BIGINT,
    registro_sanitario_snapshot VARCHAR(120),
    denominacion_snapshot       VARCHAR(500) NOT NULL,
    numero_lote_snapshot        VARCHAR(120) NOT NULL,
    fecha_vencimiento_snapshot  DATE,
    alcance                     VARCHAR(1000),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_recall_producto_lote PRIMARY KEY (id),
    CONSTRAINT fk_rec_prod_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_vigilancia.caso_recall(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_prod_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_rec_prod_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_rec_prod_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_rec_prod_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_recall_producto_lote_natural ON sch_vigilancia.recall_producto_lote(
    caso_recall_id, (COALESCE(producto_regulado_id, 0)), (COALESCE(sku_id, 0)), (COALESCE(lote_id, 0)), numero_lote_snapshot
) WHERE es_activo = '1';

CREATE TABLE sch_vigilancia.recall_establecimiento (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    caso_recall_id          BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    cantidad_identificada   NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_inmovilizada   NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_retirada       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_dispuesta      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    confirmado_at           TIMESTAMPTZ,
    confirmado_por          VARCHAR(15),
    observacion             VARCHAR(1000),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_recall_establecimiento PRIMARY KEY (id),
    CONSTRAINT uk_recall_establecimiento UNIQUE (caso_recall_id, establecimiento_id),
    CONSTRAINT fk_rec_est_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_vigilancia.caso_recall(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_est_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_recall_est_cantidades CHECK (cantidad_identificada >= 0 AND cantidad_inmovilizada >= 0 AND cantidad_retirada >= 0 AND cantidad_dispuesta >= 0),
    CONSTRAINT ck_recall_est_estado CHECK (estado IN ('PENDIENTE', 'INMOVILIZADO', 'EN_RETIRO', 'CONCILIADO', 'CERRADO')),
    CONSTRAINT ck_recall_est_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_vigilancia.recall_accion (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id       BIGINT NOT NULL,
    caso_recall_id  BIGINT NOT NULL,
    tipo_accion     VARCHAR(40) NOT NULL,
    descripcion     VARCHAR(1500) NOT NULL,
    actor           VARCHAR(15) NOT NULL,
    ejecutado_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evidencia_uri   TEXT,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recall_accion PRIMARY KEY (id),
    CONSTRAINT fk_rec_accion_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_vigilancia.caso_recall(tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE sch_vigilancia.reporte_seguridad (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT,
    establecimiento_id          BIGINT,
    tipo_reporte                VARCHAR(30) NOT NULL,
    origen_reporte              VARCHAR(30) NOT NULL,
    venta_id                    BIGINT,
    lote_id                     BIGINT,
    paciente_cliente_id         BIGINT,
    profesional_notificador_id  BIGINT,
    descripcion_evento          TEXT NOT NULL,
    gravedad_codigo             VARCHAR(50) NOT NULL DEFAULT 'MODERADA',
    desenlace_codigo            VARCHAR(50) NOT NULL DEFAULT 'RECUPERADO',
    ocurrido_at                 TIMESTAMPTZ,
    conocido_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    datos_contexto              JSONB NOT NULL DEFAULT '{}'::jsonb,
    datos_sensibles_cifrado     BYTEA,
    notificacion_oficial_at     TIMESTAMPTZ,
    notificacion_identificador  VARCHAR(150),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_reporte_seguridad PRIMARY KEY (id),
    CONSTRAINT uk_reporte_seguridad_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_reporte_seguridad_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_rep_seg_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_rep_seg_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_rep_seg_venta FOREIGN KEY (tenant_id, venta_id) REFERENCES sch_venta.venta(tenant_id, id),
    CONSTRAINT fk_rep_seg_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT fk_rep_seg_cliente FOREIGN KEY (tenant_id, paciente_cliente_id) REFERENCES sch_venta.cliente(tenant_id, id),
    CONSTRAINT fk_rep_seg_profesional FOREIGN KEY (tenant_id, profesional_notificador_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_rep_seg_tipo CHECK (tipo_reporte IN ('FARMACOVIGILANCIA', 'TECNOVIGILANCIA')),
    CONSTRAINT ck_rep_seg_origen CHECK (origen_reporte IN ('PACIENTE', 'MEDICO', 'QUIMICO_FARMACEUTICO', 'INTERNO', 'OTRO')),
    CONSTRAINT ck_rep_seg_gravedad CHECK (gravedad_codigo IN ('LEVE', 'MODERADA', 'GRAVE_HOSPITALIZACION', 'GRAVE_INCAPACIDAD', 'GRAVE_MUERTE')),
    CONSTRAINT ck_rep_seg_estado CHECK (estado IN ('REGISTRADO', 'EN_EVALUACION', 'NOTIFICADO_DIGEMID', 'EN_SEGUIMIENTO', 'CERRADO')),
    CONSTRAINT ck_rep_seg_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_rep_seg_estado ON sch_vigilancia.reporte_seguridad(tenant_id, estado, conocido_at DESC) WHERE es_activo = '1';
CREATE INDEX ix_rep_seg_lote ON sch_vigilancia.reporte_seguridad(tenant_id, lote_id) WHERE es_activo = '1' AND lote_id IS NOT NULL;

CREATE TABLE sch_vigilancia.reporte_seguridad_producto (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    reporte_id              BIGINT NOT NULL,
    producto_regulado_id    BIGINT,
    sku_id                  BIGINT,
    lote_id                 BIGINT,
    sospechoso              BOOLEAN NOT NULL DEFAULT TRUE,
    descripcion_producto    VARCHAR(500) NOT NULL,
    dosis_exposicion        VARCHAR(300),
    fecha_inicio_uso        TIMESTAMPTZ,
    fecha_fin_uso           TIMESTAMPTZ,
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_reporte_seguridad_producto PRIMARY KEY (id),
    CONSTRAINT fk_rep_prod_reporte FOREIGN KEY (tenant_id, reporte_id) REFERENCES sch_vigilancia.reporte_seguridad(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rep_prod_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_rep_prod_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_rep_prod_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_rep_prod_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_vigilancia.reporte_seguridad_seguimiento (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    reporte_id          BIGINT NOT NULL,
    fecha               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_seguimiento    VARCHAR(50) NOT NULL,
    descripcion         TEXT NOT NULL,
    actor               VARCHAR(15) NOT NULL,
    evidencia_uri       TEXT,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_reporte_seguridad_seguimiento PRIMARY KEY (id),
    CONSTRAINT fk_rep_seg_reporte FOREIGN KEY (tenant_id, reporte_id) REFERENCES sch_vigilancia.reporte_seguridad(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_rep_seg_es_activo CHECK (es_activo IN ('0', '1'))
);

-- ============================================================================
-- 13. ESQUEMA: sch_finanzas (Tesorería, CxP, CxC y Observatorio DIGEMID)
-- ============================================================================

CREATE TABLE sch_finanzas.cuenta_bancaria_empresa (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    banco               VARCHAR(150) NOT NULL,
    tipo_cuenta         VARCHAR(30) NOT NULL DEFAULT 'CORRIENTE',
    numero_cuenta       VARCHAR(100) NOT NULL,
    cci                 VARCHAR(20),
    moneda              CHAR(3) NOT NULL DEFAULT 'PEN',
    alias               VARCHAR(100),
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_cuenta_bancaria_empresa PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_bancaria_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cuenta_bancaria_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_cuenta_bancaria_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_cuenta_bancaria_tipo CHECK (tipo_cuenta IN ('CORRIENTE', 'AHORROS', 'DETRACCIONES', 'ROTATIVA')),
    CONSTRAINT ck_cuenta_bancaria_cci CHECK (cci IS NULL OR cci ~ '^[0-9]{20}$'),
    CONSTRAINT ck_cuenta_bancaria_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_cuenta_bancaria_numero ON sch_finanzas.cuenta_bancaria_empresa(tenant_id, empresa_id, banco, numero_cuenta) WHERE es_activo = '1';

CREATE TABLE sch_finanzas.factura_proveedor (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    proveedor_id            BIGINT NOT NULL,
    orden_compra_id         BIGINT,
    tipo_documento_sunat    VARCHAR(2) NOT NULL DEFAULT '01',
    serie                   VARCHAR(20) NOT NULL,
    numero                  VARCHAR(30) NOT NULL,
    fecha_emision           DATE NOT NULL,
    fecha_recepcion         DATE NOT NULL DEFAULT CURRENT_DATE,
    moneda                  CHAR(3) NOT NULL DEFAULT 'PEN',
    tipo_cambio             NUMERIC(18, 6) DEFAULT 1.000000,
    subtotal                NUMERIC(18, 2) NOT NULL DEFAULT 0,
    descuento_total         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    impuesto_total          NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total                   NUMERIC(18, 2) NOT NULL,
    fecha_vencimiento       DATE,
    estado_matching         VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    diferencia_matching     JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_factura_proveedor PRIMARY KEY (id),
    CONSTRAINT uk_factura_proveedor_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_factura_proveedor_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_factura_prov_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_abastecimiento.proveedor(tenant_id, id),
    CONSTRAINT fk_factura_prov_oc FOREIGN KEY (tenant_id, empresa_id, orden_compra_id) REFERENCES sch_abastecimiento.orden_compra(tenant_id, empresa_id, id),
    CONSTRAINT ck_factura_prov_totales CHECK (subtotal >= 0 AND descuento_total >= 0 AND impuesto_total >= 0 AND total >= 0),
    CONSTRAINT ck_factura_matching CHECK (estado_matching IN ('PENDIENTE', 'COINCIDENTE', 'CON_DIFERENCIAS', 'APROBADO_EXCEPCION')),
    CONSTRAINT ck_factura_prov_estado CHECK (estado IN ('REGISTRADA', 'APROBADA', 'OBSERVADA', 'ANULADA')),
    CONSTRAINT ck_factura_prov_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_factura_proveedor_doc ON sch_finanzas.factura_proveedor(tenant_id, proveedor_id, tipo_documento_sunat, serie, numero) WHERE es_activo = '1';
CREATE INDEX ix_factura_prov_oc ON sch_finanzas.factura_proveedor(tenant_id, orden_compra_id) WHERE es_activo = '1' AND orden_compra_id IS NOT NULL;

CREATE TABLE sch_finanzas.cuenta_por_pagar (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    factura_proveedor_id    BIGINT NOT NULL,
    fecha_emision           DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento       DATE,
    moneda                  CHAR(3) NOT NULL DEFAULT 'PEN',
    monto_total             NUMERIC(18, 2) NOT NULL,
    monto_pagado            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    saldo                   NUMERIC(18, 2) GENERATED ALWAYS AS (monto_total - monto_pagado) STORED,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    erp_external_id         VARCHAR(150),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_cuenta_por_pagar PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_por_pagar_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cuenta_por_pagar_factura UNIQUE (factura_proveedor_id),
    CONSTRAINT fk_cxp_factura FOREIGN KEY (tenant_id, empresa_id, factura_proveedor_id) REFERENCES sch_finanzas.factura_proveedor(tenant_id, empresa_id, id),
    CONSTRAINT ck_cxp_montos CHECK (monto_total >= 0 AND monto_pagado >= 0 AND monto_pagado <= monto_total),
    CONSTRAINT ck_cxp_estado CHECK (estado IN ('PENDIENTE', 'PARCIAL', 'PAGADA', 'BLOQUEADA', 'ANULADA')),
    CONSTRAINT ck_cxp_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_cxp_vencimiento ON sch_finanzas.cuenta_por_pagar(tenant_id, estado, fecha_vencimiento) WHERE es_activo = '1' AND estado IN ('PENDIENTE', 'PARCIAL');

CREATE TABLE sch_finanzas.pago_cuenta_por_pagar (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    cuenta_por_pagar_id BIGINT NOT NULL,
    cuenta_bancaria_id  BIGINT,
    fecha_pago          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto               NUMERIC(18, 2) NOT NULL,
    moneda              CHAR(3) NOT NULL DEFAULT 'PEN',
    tipo_medio_pago     VARCHAR(30) NOT NULL DEFAULT 'TRANSFERENCIA',
    referencia_operacion VARCHAR(150),
    estado              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_pago_cxp PRIMARY KEY (id),
    CONSTRAINT uk_pago_cxp_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_pago_cxp_cuenta FOREIGN KEY (cuenta_por_pagar_id) REFERENCES sch_finanzas.cuenta_por_pagar(id),
    CONSTRAINT fk_pago_cxp_banco FOREIGN KEY (tenant_id, cuenta_bancaria_id) REFERENCES sch_finanzas.cuenta_bancaria_empresa(tenant_id, id),
    CONSTRAINT ck_pago_cxp_monto CHECK (monto > 0),
    CONSTRAINT ck_pago_cxp_estado CHECK (estado IN ('REGISTRADO', 'CONFIRMADO', 'ANULADO', 'EXTORNADO')),
    CONSTRAINT ck_pago_cxp_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_pago_cxp_cuenta ON sch_finanzas.pago_cuenta_por_pagar(tenant_id, cuenta_por_pagar_id) WHERE es_activo = '1';

CREATE TABLE sch_finanzas.cuenta_por_cobrar (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    cliente_id          BIGINT NOT NULL,
    venta_id            BIGINT,
    comprobante_id      BIGINT,
    fecha_emision       DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento   DATE,
    moneda              CHAR(3) NOT NULL DEFAULT 'PEN',
    monto_total         NUMERIC(18, 2) NOT NULL,
    monto_cobrado       NUMERIC(18, 2) NOT NULL DEFAULT 0,
    saldo               NUMERIC(18, 2) GENERATED ALWAYS AS (monto_total - monto_cobrado) STORED,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_cuenta_por_cobrar PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_por_cobrar_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_cxc_cliente FOREIGN KEY (tenant_id, cliente_id) REFERENCES sch_venta.cliente(tenant_id, id),
    CONSTRAINT fk_cxc_venta FOREIGN KEY (tenant_id, venta_id) REFERENCES sch_venta.venta(tenant_id, id),
    CONSTRAINT fk_cxc_cpe FOREIGN KEY (comprobante_id) REFERENCES sch_facturacion.comprobante_electronico(id),
    CONSTRAINT ck_cxc_montos CHECK (monto_total >= 0 AND monto_cobrado >= 0 AND monto_cobrado <= monto_total),
    CONSTRAINT ck_cxc_estado CHECK (estado IN ('PENDIENTE', 'PARCIAL', 'COBRADA', 'VENCIDA', 'ANULADA')),
    CONSTRAINT ck_cxc_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_cxc_estado ON sch_finanzas.cuenta_por_cobrar(tenant_id, estado, fecha_vencimiento) WHERE es_activo = '1' AND estado IN ('PENDIENTE', 'PARCIAL');

CREATE TABLE sch_finanzas.posting_retail (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    tipo_origen         VARCHAR(30) NOT NULL,
    origen_uuid         UUID NOT NULL,
    idempotency_key     VARCHAR(150) NOT NULL,
    fecha_contable      DATE NOT NULL,
    payload_posting     JSONB NOT NULL,
    estado              VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    external_id         VARCHAR(150),
    numero_intentos     INTEGER NOT NULL DEFAULT 0,
    ultimo_error        VARCHAR(1500),
    confirmado_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_posting_retail PRIMARY KEY (id),
    CONSTRAINT uk_posting_retail_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_posting_retail_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT ck_posting_retail_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'CONFIRMADO', 'ERROR_REINTENTABLE', 'ERROR_DEFINITIVO', 'REVERSADO'))
);

CREATE INDEX ix_posting_cola ON sch_finanzas.posting_retail(tenant_id, estado, created_at) WHERE estado IN ('PENDIENTE', 'ERROR_REINTENTABLE');

CREATE TABLE sch_finanzas.reporte_mensual_precios (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    periodo             DATE NOT NULL,
    estado              VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    generado_at         TIMESTAMPTZ,
    validado_at         TIMESTAMPTZ,
    enviado_at          TIMESTAMPTZ,
    mecanismo_envio     VARCHAR(80) DEFAULT 'WEB_SERVICE_DIGEMID',
    identificador_envio VARCHAR(150),
    evidencia_uri       TEXT,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_reporte_mensual_precios PRIMARY KEY (id),
    CONSTRAINT uk_reporte_mensual_precios_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_reporte_mensual_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_reporte_mensual_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_organizacion.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_reporte_mensual_estado CHECK (estado IN ('BORRADOR', 'GENERADO', 'VALIDADO', 'ENVIADO', 'OBSERVADO', 'RECTIFICADO')),
    CONSTRAINT ck_reporte_mensual_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_reporte_mensual_periodo ON sch_finanzas.reporte_mensual_precios(tenant_id, empresa_id, periodo) WHERE es_activo = '1';

CREATE TABLE sch_finanzas.reporte_mensual_precios_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    reporte_id              BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    producto_regulado_id    BIGINT,
    sku_id                  BIGINT NOT NULL,
    precio_unidad           NUMERIC(18, 4) NOT NULL,
    precio_fraccion         NUMERIC(18, 4),
    moneda                  CHAR(3) NOT NULL DEFAULT 'PEN',
    fuente_precio           VARCHAR(100) NOT NULL DEFAULT 'LISTA_PRECIO_ACTIVA',
    version_precio          VARCHAR(100),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_reporte_mensual_linea PRIMARY KEY (id),
    CONSTRAINT uk_reporte_mensual_linea UNIQUE (reporte_id, establecimiento_id, sku_id),
    CONSTRAINT fk_rep_linea_reporte FOREIGN KEY (tenant_id, empresa_id, reporte_id) REFERENCES sch_finanzas.reporte_mensual_precios(tenant_id, empresa_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rep_linea_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_rep_linea_producto_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_rep_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT ck_rep_linea_precios CHECK (precio_unidad >= 0 AND (precio_fraccion IS NULL OR precio_fraccion >= 0)),
    CONSTRAINT ck_rep_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_rep_linea_est ON sch_finanzas.reporte_mensual_precios_linea(tenant_id, establecimiento_id, sku_id) WHERE es_activo = '1';

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
    CONSTRAINT fk_aud_evento_usuario FOREIGN KEY (tenant_id, actor_usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id),
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

CREATE UNIQUE INDEX uk_int_servicio_codigo ON sch_integracion.servicio_externo UNIQUE NULLS NOT DISTINCT (tenant_id, codigo) WHERE es_activo = '1';

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

-- ============================================================================
-- 16. ESQUEMA: sch_app (Menú de Navegación, Notificaciones, Flags y Versiones)
-- ============================================================================

CREATE TABLE sch_app.menu_navegacion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    modulo_id           BIGINT,
    menu_padre_id       BIGINT,
    codigo              VARCHAR(80) NOT NULL,
    aplicacion          VARCHAR(40) NOT NULL DEFAULT 'ERP_WEB',
    etiqueta            VARCHAR(120) NOT NULL,
    descripcion         VARCHAR(500),
    ruta                VARCHAR(240),
    icono               VARCHAR(80),
    tipo                VARCHAR(20) NOT NULL DEFAULT 'ITEM',
    modo_autorizacion   VARCHAR(20) NOT NULL DEFAULT 'TODOS',
    orden               INTEGER NOT NULL DEFAULT 0,
    es_visible          BOOLEAN NOT NULL DEFAULT TRUE,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_app_menu PRIMARY KEY (id),
    CONSTRAINT uk_app_menu_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_app_menu_modulo FOREIGN KEY (modulo_id) REFERENCES sch_seguridad.modulo_sistema(id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_menu_padre FOREIGN KEY (menu_padre_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_aplicacion CHECK (aplicacion IN ('ERP_WEB', 'POS_WEB', 'ECOMMERCE_WEB', 'MOBILE')),
    CONSTRAINT ck_app_menu_tipo CHECK (tipo IN ('GRUPO', 'ITEM', 'SEPARADOR')),
    CONSTRAINT ck_app_menu_auth CHECK (modo_autorizacion IN ('AUTENTICADO', 'CUALQUIERA', 'TODOS')),
    CONSTRAINT ck_app_menu_orden CHECK (orden >= 0),
    CONSTRAINT ck_app_menu_ruta CHECK (ruta IS NULL OR LEFT(ruta, 1) = '/'),
    CONSTRAINT ck_app_menu_tipo_ruta CHECK ((tipo = 'ITEM' AND ruta IS NOT NULL) OR (tipo IN ('GRUPO', 'SEPARADOR') AND ruta IS NULL)),
    CONSTRAINT ck_app_menu_padre CHECK (menu_padre_id IS NULL OR menu_padre_id <> id),
    CONSTRAINT ck_app_menu_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_app_menu_aplicacion_codigo ON sch_app.menu_navegacion(aplicacion, codigo) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_app_menu_ruta_activa ON sch_app.menu_navegacion(aplicacion, ruta) WHERE ruta IS NOT NULL AND es_activo = '1';
CREATE INDEX ix_app_menu_padre_orden ON sch_app.menu_navegacion(aplicacion, menu_padre_id, orden, etiqueta) WHERE es_activo = '1' AND es_visible = TRUE;

CREATE TABLE sch_app.menu_navegacion_permiso (
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    menu_id     BIGINT NOT NULL,
    permiso_id  BIGINT NOT NULL,
    es_activo   CHAR(1) NOT NULL DEFAULT '1',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_app_menu_permiso PRIMARY KEY (id),
    CONSTRAINT fk_app_menu_permiso_menu FOREIGN KEY (menu_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_menu_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES sch_seguridad.permiso(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_permiso_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_app_menu_permiso ON sch_app.menu_navegacion_permiso(menu_id, permiso_id) WHERE es_activo = '1';
CREATE INDEX ix_app_menu_permiso_busqueda ON sch_app.menu_navegacion_permiso(permiso_id) WHERE es_activo = '1';

CREATE OR REPLACE FUNCTION sch_app.fn_validar_menu_jerarquia()
RETURNS TRIGGER AS $$
DECLARE
    app_padre  VARCHAR(40);
    tipo_padre VARCHAR(20);
BEGIN
    IF NEW.id IS NOT NULL AND NEW.menu_padre_id = NEW.id THEN
        RAISE EXCEPTION 'Un menú no puede ser padre de sí mismo (ID: %)', NEW.id;
    END IF;

    IF NEW.menu_padre_id IS NULL THEN 
        RETURN NEW; 
    END IF;

    SELECT aplicacion, tipo 
      INTO app_padre, tipo_padre 
      FROM sch_app.menu_navegacion 
     WHERE id = NEW.menu_padre_id 
       AND es_activo = '1';

    IF NOT FOUND THEN 
        RAISE EXCEPTION 'El menú padre con ID % no existe o no está activo', NEW.menu_padre_id; 
    END IF;

    IF app_padre <> NEW.aplicacion THEN 
        RAISE EXCEPTION 'El menú padre e hijo deben pertenecer a la misma aplicación (Padre: %, Hijo: %)', app_padre, NEW.aplicacion; 
    END IF;

    IF tipo_padre <> 'GRUPO' THEN 
        RAISE EXCEPTION 'Solo un menú de tipo GRUPO puede contener sub-ítems (Tipo actual: %)', tipo_padre; 
    END IF;

    IF EXISTS (
        WITH RECURSIVE arbol_ancestros AS (
            SELECT id, menu_padre_id 
              FROM sch_app.menu_navegacion 
             WHERE id = NEW.menu_padre_id
            UNION ALL
            SELECT p.id, p.menu_padre_id 
              FROM sch_app.menu_navegacion p 
              JOIN arbol_ancestros a ON p.id = a.menu_padre_id
        ) 
        SELECT 1 FROM arbol_ancestros WHERE id = NEW.id
    ) THEN 
        RAISE EXCEPTION 'La relación de jerarquía genera un ciclo circular infinito'; 
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_app_menu_jerarquia 
    BEFORE INSERT OR UPDATE ON sch_app.menu_navegacion 
    FOR EACH ROW EXECUTE FUNCTION sch_app.fn_validar_menu_jerarquia();

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

CREATE UNIQUE INDEX uk_app_plantilla_codigo ON sch_app.plantilla_notificacion UNIQUE NULLS NOT DISTINCT (tenant_id, codigo) WHERE es_activo = '1';

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

CREATE UNIQUE INDEX uk_app_version_registro ON sch_app.app_version UNIQUE NULLS NOT DISTINCT (aplicacion, plataforma, version, build_number) WHERE es_activo = '1';

-- ============================================================================
-- 17. SEED DATA FUNDACIONAL (Módulos, Permisos y Menús Base)
-- ============================================================================

INSERT INTO sch_seguridad.modulo_sistema(codigo, nombre, descripcion, orden, es_activo) VALUES
('SEGURIDAD',        'Seguridad',               'Usuarios, roles, permisos y ámbitos.', 10, '1'),
('ORGANIZACION',     'Organización',            'Empresas, establecimientos, almacenes y terminales.', 20, '1'),
('CATALOGO',         'Catálogo',                'Productos regulatorios y SKU comerciales.', 30, '1'),
('COMPRAS',          'Compras',                 'Solicitudes, órdenes, recepción y proveedores.', 40, '1'),
('INVENTARIO',       'Inventario',              'Stock, lotes, reservas, transferencias y conteos.', 50, '1'),
('PRECIOS',          'Precios y Promociones',   'Listas de precios y promociones.', 60, '1'),
('VENTAS',           'Retail / POS',            'Caja, ventas, pagos y devoluciones.', 70, '1'),
('DISPENSACION',     'Dispensación',            'Prescripciones y dispensación farmacéutica.', 80, '1'),
('CONTROLADOS',      'Controlados',             'Recetas y balances de psicotrópicos/estupefacientes.', 90, '1'),
('FISCAL',           'Fiscal / CPE',            'Comprobantes electrónicos y notas de crédito.', 100, '1'),
('RECALL',           'Recall y Seguridad',      'Alertas sanitarias, inmovilización y retiros.', 110, '1'),
('FARMACOVIGILANCIA','Farmacovigilancia',       'Reportes y seguimientos de seguridad (RAMs).', 120, '1'),
('ERP',              'ERP Financiero',          'CxP/CxC, tesorería y posting contable.', 130, '1'),
('REPORTES',         'Reportes e Inteligencia', 'Observatorio de Precios DIGEMID y BI.', 140, '1')
ON CONFLICT (codigo) DO UPDATE 
SET nombre = EXCLUDED.nombre, 
    descripcion = EXCLUDED.descripcion, 
    orden = EXCLUDED.orden, 
    es_activo = '1';

INSERT INTO sch_seguridad.permiso(modulo_id, codigo, recurso, accion, nombre, descripcion, es_activo)
SELECT m.id, 
       m.codigo || ':MODULO:VER', 
       'MODULO', 
       'VER', 
       'Ver ' || m.nombre, 
       'Permiso de visibilidad y acceso básico del módulo.',
       '1'
  FROM sch_seguridad.modulo_sistema m
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO sch_app.menu_navegacion(codigo, aplicacion, etiqueta, descripcion, tipo, modo_autorizacion, orden, es_visible, es_activo)
VALUES
('OPERACIONES',    'ERP_WEB', 'Operaciones',    'Operación comercial, inventario y farmacia.', 'GRUPO', 'AUTENTICADO', 10, TRUE, '1'),
('ADMINISTRACION', 'ERP_WEB', 'Administración', 'Configuración, seguridad y organización.',    'GRUPO', 'AUTENTICADO', 20, TRUE, '1')
ON CONFLICT (aplicacion, codigo) DO UPDATE 
SET etiqueta = EXCLUDED.etiqueta, 
    descripcion = EXCLUDED.descripcion, 
    tipo = EXCLUDED.tipo, 
    modo_autorizacion = EXCLUDED.modo_autorizacion, 
    orden = EXCLUDED.orden, 
    es_visible = TRUE, 
    es_activo = '1';

WITH items(modulo_codigo, padre, codigo, etiqueta, descripcion, ruta, icono, orden) AS (
VALUES
(NULL,               'OPERACIONES',    'DASHBOARD',        'Resumen',          'Resumen operativo general.',          '/dashboard',        'LayoutDashboard',      10),
('CATALOGO',         'OPERACIONES',    'CATALOGO',         'Catálogo',         'Productos, SKUs y maestros.',         '/catalogo',         'PackageSearch',        20),
('INVENTARIO',       'OPERACIONES',    'INVENTARIO',       'Inventario',       'Stock por almacén, lotes y FEFO.',    '/inventario',       'Boxes',                30),
('COMPRAS',          'OPERACIONES',    'COMPRAS',          'Compras',          'Proveedores, órdenes y recepción.',   '/compras',          'ShoppingCart',         40),
('PRECIOS',          'OPERACIONES',    'PRECIOS',          'Precios',          'Listas de precio y promociones.',     '/precios',          'Tags',                 50),
('VENTAS',           'OPERACIONES',    'VENTAS',           'Ventas',           'Historial de ventas y devoluciones.', '/ventas',           'ReceiptText',          60),
('VENTAS',           'OPERACIONES',    'POS',              'Punto de Venta',   'Terminal de venta y caja rápida.',    '/pos',              'MonitorSmartphone',    70),
('DISPENSACION',     'OPERACIONES',    'DISPENSACION',     'Dispensación',     'Prescripción y atención clínica.',    '/dispensacion',     'Pill',                 80),
('CONTROLADOS',      'OPERACIONES',    'CONTROLADOS',      'Controlados',      'Recetas especiales y balances.',      '/controlados',      'ShieldAlert',          90),
('RECALL',           'OPERACIONES',    'RECALL',           'Recall Sanitario', 'Alertas y retiros de mercado.',       '/recall',           'Siren',                100),
('FARMACOVIGILANCIA','OPERACIONES',    'FARMACOVIGILANCIA','Farmacovigilancia','Reportes de seguridad y RAMs.',       '/farmacovigilancia','HeartPulse',           110),
('ERP',              'OPERACIONES',    'ERP',              'ERP Financiero',   'CxP, CxC y tesorería.',               '/erp',              'Landmark',             120),
('REPORTES',         'OPERACIONES',    'REPORTES',         'Reportes',         'Observatorio DIGEMID e informes.',    '/reportes',         'ChartNoAxesCombined',  130),
('SEGURIDAD',        'ADMINISTRACION', 'SEGURIDAD',        'Seguridad',        'Usuarios, roles y accesos.',          '/seguridad',        'ShieldCheck',          10),
('ORGANIZACION',     'ADMINISTRACION', 'ORGANIZACION',     'Organización',     'Empresas, boticas y almacenes.',      '/organizacion',     'Building2',            20)
)
INSERT INTO sch_app.menu_navegacion(
    modulo_id, menu_padre_id, codigo, aplicacion, etiqueta, descripcion, ruta, icono, tipo, modo_autorizacion, orden, es_visible, es_activo
)
SELECT m.id,
       p.id,
       i.codigo,
       'ERP_WEB',
       i.etiqueta,
       i.descripcion,
       i.ruta,
       i.icono,
       'ITEM',
       CASE WHEN i.modulo_codigo IS NULL THEN 'AUTENTICADO' ELSE 'TODOS' END,
       i.orden,
       TRUE,
       '1'
  FROM items i
  JOIN sch_app.menu_navegacion p ON p.aplicacion = 'ERP_WEB' AND p.codigo = i.padre
  LEFT JOIN sch_seguridad.modulo_sistema m ON m.codigo = i.modulo_codigo
ON CONFLICT (aplicacion, codigo) DO UPDATE 
SET modulo_id = EXCLUDED.modulo_id,
    menu_padre_id = EXCLUDED.menu_padre_id,
    etiqueta = EXCLUDED.etiqueta,
    descripcion = EXCLUDED.descripcion,
    ruta = EXCLUDED.ruta,
    icono = EXCLUDED.icono,
    orden = EXCLUDED.orden,
    es_visible = TRUE,
    es_activo = '1';

INSERT INTO sch_app.menu_navegacion_permiso(menu_id, permiso_id, es_activo)
SELECT mn.id, p.id, '1'
  FROM sch_app.menu_navegacion mn
  JOIN sch_seguridad.modulo_sistema m ON m.id = mn.modulo_id
  JOIN sch_seguridad.permiso p ON p.modulo_id = m.id AND p.codigo = m.codigo || ':MODULO:VER'
 WHERE mn.aplicacion = 'ERP_WEB' 
   AND mn.modulo_id IS NOT NULL
ON CONFLICT (menu_id, permiso_id) DO UPDATE 
SET es_activo = '1';

-- Catálogo SUNAT 06: Código de tipo de documento de identidad.
-- Fuente: https://cpe.sunat.gob.pe/guias-y-manuales
-- Reglas de validación actualizado al 26.08.2026, hoja Catálogos, A86:B98.
-- Catálogo global: compartido por todos los tenants.

CREATE TABLE sch_catalogo.tipo_documento_identidad (
    codigo          VARCHAR(2) NOT NULL,
    sigla           VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(200) NOT NULL,
    max             SMALLINT,
    min             SMALLINT,
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_tipo_documento_identidad PRIMARY KEY (codigo),
    CONSTRAINT ck_tipo_documento_identidad_sigla CHECK (btrim(sigla) <> ''),
    CONSTRAINT ck_tipo_documento_identidad_max CHECK (max IS NULL OR max > 0),
    CONSTRAINT ck_tipo_documento_identidad_min CHECK (min IS NULL OR min > 0),
    CONSTRAINT ck_tipo_documento_identidad_rango CHECK (min IS NULL OR max IS NULL OR min <= max),
    CONSTRAINT ck_tipo_documento_identidad_codigo CHECK (codigo ~ '^[0-9A-Z]$'),
    CONSTRAINT ck_tipo_documento_identidad_denominacion CHECK (btrim(denominacion) <> ''),
    CONSTRAINT ck_tipo_documento_identidad_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_tipo_documento_identidad_es_activo CHECK (es_activo IN ('0', '1'))
);

COMMENT ON TABLE sch_catalogo.tipo_documento_identidad IS
    'Catálogo SUNAT 06 global. La inclusión de un código no implica su aceptación en todos los CPE; validar según comprobante y operación.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.codigo IS
    'Código alfanumérico SUNAT sin relleno: DNI=1, RUC=6. El código 0 corresponde a DOC.TRIB.NO.DOM.SIN.RUC; no es un valor genérico para clientes sin identificar.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.sigla IS
    'Abreviatura de presentación definida por la aplicación; no es un código SUNAT adicional.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.max IS
    'Máximo de caracteres del número de documento; NULL indica sin límite general configurado. Aplicar además las reglas del comprobante y operación.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.min IS
    'Mínimo de caracteres del número de documento; NULL indica sin límite general configurado, no que se permita un documento vacío.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.denominacion IS
    'Descripción de la hoja oficial Catálogos, conservando su redacción; espacios finales eliminados.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.estado IS
    'Estado de disponibilidad local del catálogo (ACTIVO/INACTIVO), fuente de verdad para la aplicación. No sustituye las reglas SUNAT por operación.';
COMMENT ON COLUMN sch_catalogo.tipo_documento_identidad.es_activo IS
    'Disponibilidad local del catálogo replicada en formato legado: 1 activo, 0 inactivo. No sustituye las reglas SUNAT por operación.';

INSERT INTO sch_catalogo.tipo_documento_identidad
    (codigo, sigla, denominacion, max, min, es_activo)
VALUES
    ('0', 'DTSR', 'DOC.TRIB.NO.DOM.SIN.RUC', NULL, NULL, '1'),
    ('1', 'DNI', 'Documento Nacional de Identidad', 8, 8, '1'),
    ('4', 'CE', 'Carnet de extranjería', NULL, NULL, '1'),
    ('6', 'RUC', 'Registro Unico de Contributentes', 11, 11, '1'),
    ('7', 'PAS', 'Pasaporte', NULL, NULL, '1'),
    ('A', 'CDI', 'Cédula Diplomática de identidad', NULL, NULL, '1'),
    ('B', 'DIPR', 'DOC.IDENT.PAIS.RESIDENCIA-NO.D', NULL, NULL, '1'),
    ('C', 'TIN', 'Tax Identification Number - TIN – Doc Trib PP.NN', NULL, NULL, '1'),
    ('D', 'IN', 'Identification Number - IN – Doc Trib PP. JJ', NULL, NULL, '1'),
    ('E', 'TAM', 'TAM- Tarjeta Andina de Migración', NULL, NULL, '1'),
    ('F', 'PTP', 'Permiso Temporal de Permanencia - PTP', NULL, NULL, '1'),
    ('G', 'SC', 'Salvoconducto', NULL, NULL, '1'),
    ('H', 'CPP', 'Carné Permiso Temp.Perman. - CPP', NULL, NULL, '1');
