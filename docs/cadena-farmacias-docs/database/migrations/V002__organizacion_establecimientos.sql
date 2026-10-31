-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V002.

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
    CONSTRAINT uk_establecimiento_tenant_id UNIQUE (tenant_id, id),
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
