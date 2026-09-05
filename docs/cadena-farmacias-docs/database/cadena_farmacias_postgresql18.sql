-- =============================================================
-- CADENA DE FARMACIAS - CORE CENTRAL - POSTGRESQL 18.x
-- Baseline consolidado v0.3.
-- FUENTE DE VERDAD: database/migrations/V001..V017.
-- Este archivo se genera como conveniencia; no mantenerlo manualmente en paralelo.
-- Los scripts históricos V1/V2 aportados fueron consolidados y NO deben ejecutarse además de este baseline.
-- =============================================================

-- =============================================================
-- V001__base_esquemas_tenant.sql
-- =============================================================
-- PostgreSQL 18.x
-- Cadena de Farmacias - Baseline consolidado v0.3
-- Convenciones: BIGINT IDENTITY interno + UUIDv7 público; timestamps con zona; multi-tenant explícito.

CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE SCHEMA IF NOT EXISTS sch_farmacia;
CREATE SCHEMA IF NOT EXISTS sch_seguridad;
CREATE SCHEMA IF NOT EXISTS sch_auditoria;
CREATE SCHEMA IF NOT EXISTS sch_integracion;
CREATE SCHEMA IF NOT EXISTS sch_app;

CREATE TABLE sch_farmacia.tenant (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    codigo              VARCHAR(30) NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    slug                VARCHAR(80),
    zona_horaria        VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    locale              VARCHAR(20) NOT NULL DEFAULT 'es-PE',
    moneda_default      CHAR(3) NOT NULL DEFAULT 'PEN',
    configuracion       JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_tenant PRIMARY KEY (id),
    CONSTRAINT uk_tenant_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_tenant_codigo UNIQUE (codigo),
    CONSTRAINT uk_tenant_slug UNIQUE (slug),
    CONSTRAINT ck_tenant_estado CHECK (estado IN ('ACTIVO','INACTIVO','SUSPENDIDO'))
);

-- =============================================================
-- V002__organizacion_establecimientos.sql
-- =============================================================
-- Organización, establecimientos, almacenes, POS y profesionales.
-- Se enriquecen datos fiscales, operativos y sanitarios sin mezclar autorización con el establecimiento.

CREATE TABLE sch_farmacia.empresa_operadora (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    ruc                 VARCHAR(11) NOT NULL,
    razon_social        VARCHAR(300) NOT NULL,
    nombre_comercial    VARCHAR(300),
    direccion_fiscal    VARCHAR(500),
    ubigeo_fiscal       VARCHAR(10),
    telefono            VARCHAR(40),
    email               CITEXT,
    sitio_web           VARCHAR(300),
    logo_uri            TEXT,
    moneda_funcional    CHAR(3) NOT NULL DEFAULT 'PEN',
    zona_horaria        VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    permite_venta_online BOOLEAN NOT NULL DEFAULT FALSE,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_empresa_operadora PRIMARY KEY (id),
    CONSTRAINT uk_empresa_operadora_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_empresa_operadora_tenant_ruc UNIQUE (tenant_id, ruc),
    CONSTRAINT uk_empresa_operadora_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_empresa_operadora_tenant FOREIGN KEY (tenant_id)
        REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_empresa_operadora_ruc CHECK (char_length(ruc) = 11),
    CONSTRAINT ck_empresa_operadora_estado CHECK (estado IN ('ACTIVO','INACTIVO','SUSPENDIDO'))
);

CREATE TABLE sch_farmacia.establecimiento_farmaceutico (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    codigo              VARCHAR(40) NOT NULL,
    nombre              VARCHAR(250) NOT NULL,
    tipo_establecimiento VARCHAR(40) NOT NULL,
    categoria_regulatoria_codigo VARCHAR(50),
    direccion           VARCHAR(500),
    ubigeo              VARCHAR(10),
    referencia          VARCHAR(300),
    latitud             NUMERIC(10,7),
    longitud            NUMERIC(10,7),
    telefono            VARCHAR(40),
    email               CITEXT,
    es_principal        BOOLEAN NOT NULL DEFAULT FALSE,
    permite_venta_online BOOLEAN NOT NULL DEFAULT FALSE,
    permite_delivery    BOOLEAN NOT NULL DEFAULT FALSE,
    perfil_operacion    VARCHAR(30) NOT NULL DEFAULT 'ONLINE',
    zona_horaria        VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    estado_operativo    VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_establecimiento_farmaceutico PRIMARY KEY (id),
    CONSTRAINT uk_establecimiento_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_establecimiento_tenant_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT uk_establecimiento_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_establecimiento_empresa FOREIGN KEY (tenant_id, empresa_id)
        REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_establecimiento_perfil CHECK (perfil_operacion IN ('ONLINE','STORE_EDGE')),
    CONSTRAINT ck_establecimiento_estado CHECK (estado_operativo IN ('ACTIVO','INACTIVO','SUSPENDIDO','CERRADO'))
);

CREATE TABLE sch_farmacia.establecimiento_autorizacion_sanitaria (
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
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_establecimiento_autorizacion PRIMARY KEY (id),
    CONSTRAINT uk_establecimiento_autorizacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_est_autorizacion_numero UNIQUE (tenant_id, establecimiento_id, tipo_autorizacion, numero_autorizacion),
    CONSTRAINT fk_establecimiento_autorizacion_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id)
        REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_establecimiento_autorizacion_fechas CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_establecimiento_autorizacion_estado CHECK (estado IN ('VIGENTE','VENCIDA','SUSPENDIDA','REVOCADA'))
);

CREATE TABLE sch_farmacia.almacen (
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
    temperatura_min_c   NUMERIC(6,2),
    temperatura_max_c   NUMERIC(6,2),
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_almacen PRIMARY KEY (id),
    CONSTRAINT uk_almacen_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_almacen_est_codigo UNIQUE (tenant_id, establecimiento_id, codigo),
    CONSTRAINT uk_almacen_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_almacen_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id)
        REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_almacen_temperatura CHECK (temperatura_max_c IS NULL OR temperatura_min_c IS NULL OR temperatura_max_c >= temperatura_min_c),
    CONSTRAINT ck_almacen_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.ubicacion_almacen (
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
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ubicacion_almacen PRIMARY KEY (id),
    CONSTRAINT uk_ubicacion_almacen_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_ubicacion_almacen_codigo UNIQUE (tenant_id, almacen_id, codigo),
    CONSTRAINT uk_ubicacion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, almacen_id, id),
    CONSTRAINT fk_ubicacion_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id)
        REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_ubicacion_almacen_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.terminal_pos (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    codigo              VARCHAR(40) NOT NULL,
    nombre              VARCHAR(120),
    numero_serie_equipo VARCHAR(120),
    hostname            VARCHAR(150),
    ip_equipo           INET,
    impresora_codigo    VARCHAR(100),
    store_edge_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_heartbeat_at TIMESTAMPTZ,
    version_app         VARCHAR(80),
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_terminal_pos PRIMARY KEY (id),
    CONSTRAINT uk_terminal_pos_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_terminal_pos_codigo UNIQUE (tenant_id, establecimiento_id, codigo),
    CONSTRAINT uk_terminal_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_terminal_pos_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id)
        REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_terminal_pos_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO','MANTENIMIENTO'))
);

CREATE TABLE sch_farmacia.profesional_farmaceutico (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    tipo_documento      VARCHAR(20) NOT NULL,
    numero_documento    VARCHAR(30) NOT NULL,
    nombres             VARCHAR(150) NOT NULL,
    apellidos           VARCHAR(150) NOT NULL,
    email               CITEXT,
    telefono            VARCHAR(40),
    colegio_profesional VARCHAR(100),
    numero_colegiatura  VARCHAR(50),
    especialidad        VARCHAR(150),
    estado_colegiatura  VARCHAR(30),
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_profesional_farmaceutico PRIMARY KEY (id),
    CONSTRAINT uk_profesional_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_profesional_doc UNIQUE (tenant_id, tipo_documento, numero_documento),
    CONSTRAINT uk_profesional_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_profesional_tenant FOREIGN KEY (tenant_id)
        REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_profesional_estado CHECK (estado IN ('ACTIVO','INACTIVO','SUSPENDIDO'))
);

CREATE TABLE sch_farmacia.asignacion_profesional (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    empresa_id          BIGINT NOT NULL,
    establecimiento_id  BIGINT NOT NULL,
    profesional_id      BIGINT NOT NULL,
    funcion             VARCHAR(40) NOT NULL,
    es_principal        BOOLEAN NOT NULL DEFAULT FALSE,
    vigente_desde       DATE NOT NULL,
    vigente_hasta       DATE,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    evidencia_uri       TEXT,
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_asignacion_profesional PRIMARY KEY (id),
    CONSTRAINT uk_asignacion_profesional_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_asignacion_prof_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id)
        REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_asignacion_prof_prof FOREIGN KEY (tenant_id, profesional_id)
        REFERENCES sch_farmacia.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_asignacion_prof_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_asignacion_prof_estado CHECK (estado IN ('ACTIVO','INACTIVO','SUSPENDIDO'))
);

CREATE INDEX ix_establecimiento_empresa ON sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id);
CREATE INDEX ix_asignacion_prof_est ON sch_farmacia.asignacion_profesional(tenant_id, establecimiento_id, estado);

-- =============================================================
-- V003__catalogo_farmaceutico.sql
-- =============================================================
-- Catálogo regulatorio global + catálogo comercial por tenant.
-- Campos regulatorios contrastados con consultas/estándares DIGEMID; categoría/marca son conceptos comerciales.

CREATE TABLE sch_farmacia.condicion_venta (
    codigo              VARCHAR(30) NOT NULL,
    denominacion        VARCHAR(200) NOT NULL,
    requiere_receta     BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_retencion  BOOLEAN NOT NULL DEFAULT FALSE,
    fuente              VARCHAR(300),
    version_fuente      VARCHAR(100),
    vigente_desde       DATE,
    vigente_hasta       DATE,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_condicion_venta PRIMARY KEY (codigo),
    CONSTRAINT ck_condicion_venta_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.forma_farmaceutica (
    codigo VARCHAR(30) NOT NULL,
    denominacion VARCHAR(200) NOT NULL,
    fuente VARCHAR(300),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_forma_farmaceutica PRIMARY KEY (codigo)
);

CREATE TABLE sch_farmacia.via_administracion (
    codigo VARCHAR(30) NOT NULL,
    denominacion VARCHAR(200) NOT NULL,
    fuente VARCHAR(300),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_via_administracion PRIMARY KEY (codigo)
);

CREATE TABLE sch_farmacia.unidad_medida (
    codigo VARCHAR(30) NOT NULL,
    denominacion VARCHAR(150) NOT NULL,
    simbolo VARCHAR(30),
    permite_decimal BOOLEAN NOT NULL DEFAULT FALSE,
    fuente VARCHAR(300),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_unidad_medida PRIMARY KEY (codigo)
);

CREATE TABLE sch_farmacia.clasificacion_controlada (
    codigo VARCHAR(40) NOT NULL,
    denominacion VARCHAR(200) NOT NULL,
    norma_fuente VARCHAR(300),
    requiere_receta_especial BOOLEAN NOT NULL DEFAULT FALSE,
    retiene_receta BOOLEAN NOT NULL DEFAULT FALSE,
    vigencia_receta_dias INTEGER,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_clasificacion_controlada PRIMARY KEY (codigo),
    CONSTRAINT ck_clasificacion_controlada_dias CHECK (vigencia_receta_dias IS NULL OR vigencia_receta_dias > 0)
);

CREATE TABLE sch_farmacia.principio_activo (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    codigo_fuente VARCHAR(80),
    denominacion VARCHAR(300) NOT NULL,
    nombre_normalizado VARCHAR(300),
    fuente VARCHAR(300),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_principio_activo PRIMARY KEY (id),
    CONSTRAINT uk_principio_activo_uuid UNIQUE (uuid_publico)
);

CREATE TABLE sch_farmacia.categoria_producto (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    categoria_padre_id BIGINT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(500),
    nivel INTEGER NOT NULL DEFAULT 1,
    orden INTEGER NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_categoria_producto PRIMARY KEY (id),
    CONSTRAINT uk_categoria_producto_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_categoria_tenant_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT uk_categoria_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_categoria_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_categoria_padre FOREIGN KEY (tenant_id, categoria_padre_id) REFERENCES sch_farmacia.categoria_producto(tenant_id, id),
    CONSTRAINT ck_categoria_nivel CHECK (nivel >= 1),
    CONSTRAINT ck_categoria_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.marca (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_marca PRIMARY KEY (id),
    CONSTRAINT uk_marca_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_marca_tenant_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT uk_marca_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_marca_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_marca_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.producto_regulado (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tipo_producto VARCHAR(40) NOT NULL,
    rubro_codigo VARCHAR(50),
    tipo_registro VARCHAR(40),
    numero_registro VARCHAR(100),
    denominacion VARCHAR(500) NOT NULL,
    concentracion_texto VARCHAR(300),
    presentacion_regulatoria VARCHAR(500),
    forma_farmaceutica_codigo VARCHAR(30),
    via_administracion_codigo VARCHAR(30),
    unidad_medida_codigo VARCHAR(30),
    condicion_venta_codigo VARCHAR(30),
    clasificacion_atc VARCHAR(30),
    clasificacion_controlada_codigo VARCHAR(40),
    tipo_liberacion VARCHAR(40),
    origen_fabricacion VARCHAR(40),
    pais_origen VARCHAR(100),
    subpartida_nacional VARCHAR(30),
    titular_registro VARCHAR(300),
    fabricante VARCHAR(300),
    importador VARCHAR(300),
    establecimiento_expendio VARCHAR(200),
    vigente_desde DATE,
    vigente_hasta DATE,
    estado_regulatorio VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    fuente VARCHAR(300),
    version_fuente VARCHAR(100),
    updated_source_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_producto_regulado PRIMARY KEY (id),
    CONSTRAINT uk_producto_regulado_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_producto_regulado_forma FOREIGN KEY (forma_farmaceutica_codigo) REFERENCES sch_farmacia.forma_farmaceutica(codigo),
    CONSTRAINT fk_producto_regulado_via FOREIGN KEY (via_administracion_codigo) REFERENCES sch_farmacia.via_administracion(codigo),
    CONSTRAINT fk_producto_regulado_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT fk_producto_regulado_condicion FOREIGN KEY (condicion_venta_codigo) REFERENCES sch_farmacia.condicion_venta(codigo),
    CONSTRAINT fk_producto_regulado_controlada FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_farmacia.clasificacion_controlada(codigo),
    CONSTRAINT ck_producto_regulado_registro CHECK ((numero_registro IS NULL AND tipo_registro IS NULL) OR (numero_registro IS NOT NULL AND tipo_registro IS NOT NULL)),
    CONSTRAINT ck_producto_regulado_vigencia CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_producto_regulado_estado CHECK (estado_regulatorio IN ('VIGENTE','VENCIDO','SUSPENDIDO','CANCELADO','POR_VALIDAR'))
);

CREATE UNIQUE INDEX uk_producto_regulado_registro ON sch_farmacia.producto_regulado(tipo_registro, numero_registro) WHERE numero_registro IS NOT NULL;

CREATE TABLE sch_farmacia.producto_principio_activo (
    producto_regulado_id BIGINT NOT NULL,
    principio_activo_id BIGINT NOT NULL,
    concentracion_texto VARCHAR(200),
    cantidad NUMERIC(18,6),
    unidad_medida_codigo VARCHAR(30),
    es_principal BOOLEAN NOT NULL DEFAULT TRUE,
    orden SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_producto_principio_activo PRIMARY KEY (producto_regulado_id, principio_activo_id),
    CONSTRAINT fk_producto_pa_producto FOREIGN KEY (producto_regulado_id) REFERENCES sch_farmacia.producto_regulado(id),
    CONSTRAINT fk_producto_pa_principio FOREIGN KEY (principio_activo_id) REFERENCES sch_farmacia.principio_activo(id),
    CONSTRAINT fk_producto_pa_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT ck_producto_pa_cantidad CHECK (cantidad IS NULL OR cantidad > 0)
);

CREATE TABLE sch_farmacia.sku_comercial (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    producto_regulado_id BIGINT,
    categoria_id BIGINT,
    marca_id BIGINT,
    tipo_sku VARCHAR(30) NOT NULL DEFAULT 'REGULADO',
    codigo_interno VARCHAR(60) NOT NULL,
    descripcion_comercial VARCHAR(500) NOT NULL,
    nombre_corto VARCHAR(200),
    presentacion_comercial VARCHAR(300),
    unidad_venta_codigo VARCHAR(30),
    contenido NUMERIC(18,4),
    unidad_contenido_codigo VARCHAR(30),
    peso_gramos NUMERIC(18,4),
    alto_cm NUMERIC(10,2),
    ancho_cm NUMERIC(10,2),
    largo_cm NUMERIC(10,2),
    permite_venta_fraccion BOOLEAN NOT NULL DEFAULT FALSE,
    factor_fraccion NUMERIC(18,4),
    requiere_lote BOOLEAN NOT NULL DEFAULT TRUE,
    requiere_vencimiento BOOLEAN NOT NULL DEFAULT TRUE,
    afecto_igv BOOLEAN NOT NULL DEFAULT TRUE,
    stock_minimo_default NUMERIC(18,4) NOT NULL DEFAULT 0,
    stock_maximo_default NUMERIC(18,4),
    imagen_uri TEXT,
    estado_comercial VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_sku_comercial PRIMARY KEY (id),
    CONSTRAINT uk_sku_comercial_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_sku_tenant_codigo UNIQUE (tenant_id, codigo_interno),
    CONSTRAINT uk_sku_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_sku_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_sku_producto FOREIGN KEY (producto_regulado_id) REFERENCES sch_farmacia.producto_regulado(id),
    CONSTRAINT fk_sku_categoria FOREIGN KEY (tenant_id, categoria_id) REFERENCES sch_farmacia.categoria_producto(tenant_id, id),
    CONSTRAINT fk_sku_marca FOREIGN KEY (tenant_id, marca_id) REFERENCES sch_farmacia.marca(tenant_id, id),
    CONSTRAINT fk_sku_unidad FOREIGN KEY (unidad_venta_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT fk_sku_unidad_contenido FOREIGN KEY (unidad_contenido_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT ck_sku_tipo CHECK (tipo_sku IN ('REGULADO','NO_REGULADO')),
    CONSTRAINT ck_sku_tipo_producto CHECK ((tipo_sku = 'REGULADO' AND producto_regulado_id IS NOT NULL) OR tipo_sku = 'NO_REGULADO'),
    CONSTRAINT ck_sku_factor CHECK ((permite_venta_fraccion = FALSE AND factor_fraccion IS NULL) OR (permite_venta_fraccion = TRUE AND factor_fraccion > 0)),
    CONSTRAINT ck_sku_stock_default CHECK (stock_minimo_default >= 0 AND (stock_maximo_default IS NULL OR stock_maximo_default >= stock_minimo_default)),
    CONSTRAINT ck_sku_estado CHECK (estado_comercial IN ('ACTIVO','INACTIVO','BLOQUEADO','DESCONTINUADO'))
);

CREATE TABLE sch_farmacia.sku_codigo_barra (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    tipo_codigo VARCHAR(30) NOT NULL DEFAULT 'EAN13',
    codigo_barra VARCHAR(80) NOT NULL,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    vigente_desde DATE,
    vigente_hasta DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_sku_codigo_barra PRIMARY KEY (id),
    CONSTRAINT uk_sku_codigo_barra UNIQUE (tenant_id, codigo_barra),
    CONSTRAINT fk_sku_codigo_barra FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_sku_barra_fechas CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde)
);

CREATE UNIQUE INDEX uk_sku_codigo_principal ON sch_farmacia.sku_codigo_barra(tenant_id, sku_id) WHERE es_principal AND estado = 'ACTIVO';
CREATE INDEX ix_sku_producto ON sch_farmacia.sku_comercial(producto_regulado_id);
CREATE INDEX ix_sku_categoria ON sch_farmacia.sku_comercial(tenant_id, categoria_id);
CREATE INDEX ix_producto_condicion ON sch_farmacia.producto_regulado(condicion_venta_codigo);
CREATE INDEX ix_producto_nombre_search ON sch_farmacia.producto_regulado USING gin (to_tsvector('spanish', coalesce(denominacion,'') || ' ' || coalesce(concentracion_texto,'') || ' ' || coalesce(fabricante,'')));

-- =============================================================
-- V004__compras_proveedores.sql
-- =============================================================
-- Proveedores, solicitudes, órdenes y recepción física.
-- Factura/CxP se mantienen en V012 para preservar el boundary ERP.

CREATE TABLE sch_farmacia.proveedor (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    tipo_documento VARCHAR(20) NOT NULL DEFAULT 'RUC',
    numero_documento VARCHAR(20) NOT NULL,
    razon_social VARCHAR(300) NOT NULL,
    nombre_comercial VARCHAR(300),
    direccion VARCHAR(500),
    ubigeo VARCHAR(10),
    telefono VARCHAR(40),
    email CITEXT,
    sitio_web VARCHAR(300),
    contacto_nombre VARCHAR(180),
    contacto_cargo VARCHAR(120),
    contacto_telefono VARCHAR(40),
    contacto_email CITEXT,
    condicion_pago_default VARCHAR(80),
    dias_credito_default INTEGER,
    moneda_default CHAR(3) NOT NULL DEFAULT 'PEN',
    es_laboratorio BOOLEAN NOT NULL DEFAULT FALSE,
    es_importador BOOLEAN NOT NULL DEFAULT FALSE,
    es_distribuidor BOOLEAN NOT NULL DEFAULT TRUE,
    calificacion VARCHAR(30),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_proveedor PRIMARY KEY (id),
    CONSTRAINT uk_proveedor_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_proveedor_documento UNIQUE (tenant_id, tipo_documento, numero_documento),
    CONSTRAINT uk_proveedor_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_proveedor_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_proveedor_dias_credito CHECK (dias_credito_default IS NULL OR dias_credito_default >= 0),
    CONSTRAINT ck_proveedor_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO','OBSERVADO'))
);

CREATE TABLE sch_farmacia.solicitud_compra (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_solicitante_id BIGINT,
    almacen_solicitante_id BIGINT,
    numero VARCHAR(50) NOT NULL,
    fecha_solicitud DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_requerida DATE,
    prioridad VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    motivo VARCHAR(1000),
    solicitante_usuario_uuid UUID,
    aprobado_por_usuario_uuid UUID,
    aprobado_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_solicitud_compra PRIMARY KEY (id),
    CONSTRAINT uk_solicitud_compra_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_solicitud_compra_numero UNIQUE (tenant_id, numero),
    CONSTRAINT uk_solicitud_compra_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_solicitud_compra_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_solicitud_compra_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_solicitante_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_solicitud_compra_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_solicitante_id, almacen_solicitante_id) REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_solicitud_compra_fechas CHECK (fecha_requerida IS NULL OR fecha_requerida >= fecha_solicitud),
    CONSTRAINT ck_solicitud_compra_prioridad CHECK (prioridad IN ('BAJA','NORMAL','ALTA','URGENTE')),
    CONSTRAINT ck_solicitud_compra_estado CHECK (estado IN ('BORRADOR','EN_APROBACION','APROBADA','RECHAZADA','CANCELADA','ATENDIDA'))
);

CREATE TABLE sch_farmacia.solicitud_compra_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    solicitud_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    sku_id BIGINT NOT NULL,
    cantidad_solicitada NUMERIC(18,4) NOT NULL,
    unidad_medida_codigo VARCHAR(30),
    stock_actual_snapshot NUMERIC(18,4),
    stock_minimo_snapshot NUMERIC(18,4),
    observacion VARCHAR(500),
    CONSTRAINT pk_solicitud_compra_linea PRIMARY KEY (id),
    CONSTRAINT uk_solicitud_linea UNIQUE (solicitud_id, numero_linea),
    CONSTRAINT fk_solicitud_linea_solicitud FOREIGN KEY (tenant_id, empresa_id, solicitud_id) REFERENCES sch_farmacia.solicitud_compra(tenant_id, empresa_id, id),
    CONSTRAINT fk_solicitud_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_solicitud_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT ck_solicitud_linea_cantidad CHECK (cantidad_solicitada > 0)
);

CREATE TABLE sch_farmacia.orden_compra (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    solicitud_id BIGINT,
    establecimiento_destino_id BIGINT,
    numero VARCHAR(50) NOT NULL,
    fecha_emision DATE NOT NULL,
    fecha_entrega_estimada DATE,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    tipo_cambio NUMERIC(18,6),
    condicion_pago VARCHAR(80),
    dias_credito INTEGER,
    subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
    descuento_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuesto_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    observacion VARCHAR(1500),
    aprobado_por_usuario_uuid UUID,
    aprobado_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_orden_compra PRIMARY KEY (id),
    CONSTRAINT uk_orden_compra_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_orden_compra_numero UNIQUE (tenant_id, numero),
    CONSTRAINT uk_orden_compra_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_orden_compra_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT fk_orden_compra_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_farmacia.proveedor(tenant_id, id),
    CONSTRAINT fk_orden_compra_solicitud FOREIGN KEY (tenant_id, empresa_id, solicitud_id) REFERENCES sch_farmacia.solicitud_compra(tenant_id, empresa_id, id),
    CONSTRAINT fk_orden_compra_est_destino FOREIGN KEY (tenant_id, empresa_id, establecimiento_destino_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_orden_compra_fechas CHECK (fecha_entrega_estimada IS NULL OR fecha_entrega_estimada >= fecha_emision),
    CONSTRAINT ck_orden_compra_credito CHECK (dias_credito IS NULL OR dias_credito >= 0),
    CONSTRAINT ck_orden_compra_estado CHECK (estado IN ('BORRADOR','EN_APROBACION','APROBADA','EMITIDA','PARCIALMENTE_RECIBIDA','RECIBIDA','CANCELADA','CERRADA')),
    CONSTRAINT ck_orden_compra_totales CHECK (subtotal >= 0 AND descuento_total >= 0 AND impuesto_total >= 0 AND total >= 0)
);

CREATE TABLE sch_farmacia.orden_compra_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    orden_compra_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    sku_id BIGINT NOT NULL,
    descripcion_snapshot VARCHAR(500),
    cantidad NUMERIC(18,4) NOT NULL,
    unidad_medida_codigo VARCHAR(30),
    precio_unitario NUMERIC(18,6) NOT NULL,
    descuento NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuesto NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_linea NUMERIC(18,2) NOT NULL,
    tolerancia_exceso_pct NUMERIC(7,4),
    tolerancia_defecto_pct NUMERIC(7,4),
    CONSTRAINT pk_orden_compra_linea PRIMARY KEY (id),
    CONSTRAINT uk_orden_compra_linea UNIQUE (orden_compra_id, numero_linea),
    CONSTRAINT uk_orden_compra_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_oc_linea_oc FOREIGN KEY (tenant_id, empresa_id, orden_compra_id) REFERENCES sch_farmacia.orden_compra(tenant_id, empresa_id, id),
    CONSTRAINT fk_oc_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_oc_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT ck_oc_linea_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_oc_linea_importes CHECK (precio_unitario >= 0 AND descuento >= 0 AND impuesto >= 0 AND total_linea >= 0)
);

CREATE TABLE sch_farmacia.recepcion_compra (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    orden_compra_id BIGINT,
    proveedor_id BIGINT,
    numero VARCHAR(50) NOT NULL,
    guia_remision VARCHAR(80),
    documento_proveedor_tipo VARCHAR(30),
    documento_proveedor_serie VARCHAR(20),
    documento_proveedor_numero VARCHAR(40),
    fecha_recepcion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    temperatura_recepcion_c NUMERIC(6,2),
    condicion_transporte VARCHAR(500),
    recibido_por_usuario_uuid UUID,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    observacion VARCHAR(1000),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_recepcion_compra PRIMARY KEY (id),
    CONSTRAINT uk_recepcion_compra_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_recepcion_compra_numero UNIQUE (tenant_id, numero),
    CONSTRAINT uk_recepcion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_recepcion_compra_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_recepcion_compra_oc FOREIGN KEY (tenant_id, empresa_id, orden_compra_id) REFERENCES sch_farmacia.orden_compra(tenant_id, empresa_id, id),
    CONSTRAINT fk_recepcion_compra_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_farmacia.proveedor(tenant_id, id),
    CONSTRAINT ck_recepcion_compra_estado CHECK (estado IN ('BORRADOR','EN_INSPECCION','CONFIRMADA','OBSERVADA','ANULADA'))
);

CREATE TABLE sch_farmacia.recepcion_compra_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    recepcion_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    orden_compra_linea_id BIGINT,
    sku_id BIGINT NOT NULL,
    numero_lote VARCHAR(120),
    fecha_fabricacion DATE,
    fecha_vencimiento DATE,
    cantidad_recibida NUMERIC(18,4) NOT NULL,
    cantidad_aceptada NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_cuarentena NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_rechazada NUMERIC(18,4) NOT NULL DEFAULT 0,
    costo_unitario NUMERIC(18,6),
    decision_calidad VARCHAR(30) NOT NULL DEFAULT 'ACEPTADO',
    motivo_decision VARCHAR(1000),
    observacion VARCHAR(500),
    CONSTRAINT pk_recepcion_compra_linea PRIMARY KEY (id),
    CONSTRAINT uk_recepcion_compra_linea UNIQUE (recepcion_id, numero_linea),
    CONSTRAINT uk_recepcion_compra_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_recepcion_linea_rec FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, recepcion_id) REFERENCES sch_farmacia.recepcion_compra(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_recepcion_linea_oc_linea FOREIGN KEY (tenant_id, orden_compra_linea_id) REFERENCES sch_farmacia.orden_compra_linea(tenant_id, id),
    CONSTRAINT fk_recepcion_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_recepcion_linea_fechas CHECK (fecha_vencimiento IS NULL OR fecha_fabricacion IS NULL OR fecha_vencimiento >= fecha_fabricacion),
    CONSTRAINT ck_recepcion_linea_cantidad CHECK (cantidad_recibida > 0 AND cantidad_aceptada >= 0 AND cantidad_cuarentena >= 0 AND cantidad_rechazada >= 0 AND cantidad_aceptada + cantidad_cuarentena + cantidad_rechazada <= cantidad_recibida),
    CONSTRAINT ck_recepcion_linea_calidad CHECK (decision_calidad IN ('ACEPTADO','CUARENTENA','RECHAZADO','ACEPTADO_PARCIAL'))
);

CREATE INDEX ix_oc_proveedor ON sch_farmacia.orden_compra(tenant_id, proveedor_id, estado);
CREATE INDEX ix_recepcion_oc ON sch_farmacia.recepcion_compra(tenant_id, orden_compra_id);

-- =============================================================
-- V005__inventario_lotes_transferencias.sql
-- =============================================================
-- Lotes, posiciones, movimientos, reservas, transferencias y conteos.
-- El lote conserva identidad/trazabilidad; el vencimiento se deriva de fecha_vencimiento y no de un estado manual exclusivo.

CREATE TABLE sch_farmacia.lote (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    proveedor_id BIGINT,
    numero_lote VARCHAR(120) NOT NULL,
    fecha_fabricacion DATE,
    fecha_vencimiento DATE,
    registro_sanitario_snapshot VARCHAR(100),
    fabricante_snapshot VARCHAR(300),
    origen_recepcion_linea_id BIGINT,
    estado_lote VARCHAR(30) NOT NULL DEFAULT 'HABILITADO',
    motivo_estado VARCHAR(1000),
    bloqueado_at TIMESTAMPTZ,
    bloqueado_por VARCHAR(100),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_lote PRIMARY KEY (id),
    CONSTRAINT uk_lote_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_lote_tenant_sku_numero_vcto UNIQUE NULLS NOT DISTINCT (tenant_id, sku_id, numero_lote, fecha_vencimiento),
    CONSTRAINT uk_lote_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_lote_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_lote_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_farmacia.proveedor(tenant_id, id),
    CONSTRAINT fk_lote_origen_recepcion FOREIGN KEY (tenant_id, origen_recepcion_linea_id) REFERENCES sch_farmacia.recepcion_compra_linea(tenant_id, id),
    CONSTRAINT ck_lote_fechas CHECK (fecha_vencimiento IS NULL OR fecha_fabricacion IS NULL OR fecha_vencimiento >= fecha_fabricacion),
    CONSTRAINT ck_lote_estado CHECK (estado_lote IN ('HABILITADO','CUARENTENA','BLOQUEADO','INMOVILIZADO_RECALL','DISPOSICION_FINAL'))
);

CREATE TABLE sch_farmacia.posicion_inventario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    ubicacion_id BIGINT,
    sku_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    estado_inventario VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    cantidad_fisica NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_reservada NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_disponible NUMERIC(18,4) GENERATED ALWAYS AS (cantidad_fisica - cantidad_reservada) STORED,
    costo_promedio NUMERIC(18,6) NOT NULL DEFAULT 0,
    stock_minimo NUMERIC(18,4),
    stock_maximo NUMERIC(18,4),
    version_lock BIGINT NOT NULL DEFAULT 0,
    ultimo_movimiento_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT pk_posicion_inventario PRIMARY KEY (id),
    CONSTRAINT uk_posicion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_posicion_natural UNIQUE NULLS NOT DISTINCT (tenant_id, establecimiento_id, almacen_id, ubicacion_id, sku_id, lote_id, estado_inventario),
    CONSTRAINT uk_posicion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_posicion_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_posicion_ubicacion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id, ubicacion_id) REFERENCES sch_farmacia.ubicacion_almacen(tenant_id, empresa_id, establecimiento_id, almacen_id, id),
    CONSTRAINT fk_posicion_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_posicion_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_posicion_cantidades CHECK (cantidad_fisica >= 0 AND cantidad_reservada >= 0 AND cantidad_reservada <= cantidad_fisica),
    CONSTRAINT ck_posicion_costos CHECK (costo_promedio >= 0 AND (stock_minimo IS NULL OR stock_minimo >= 0) AND (stock_maximo IS NULL OR stock_maximo >= COALESCE(stock_minimo,0))),
    CONSTRAINT ck_posicion_estado CHECK (estado_inventario IN ('DISPONIBLE','CUARENTENA','BLOQUEADO','DANADO','VENCIDO','RECALL','TRANSITO'))
);

CREATE TABLE sch_farmacia.movimiento_inventario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    tipo_movimiento VARCHAR(50) NOT NULL,
    naturaleza CHAR(1) NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario NUMERIC(18,6) NOT NULL DEFAULT 0,
    costo_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    almacen_origen_id BIGINT,
    ubicacion_origen_id BIGINT,
    almacen_destino_id BIGINT,
    ubicacion_destino_id BIGINT,
    stock_anterior NUMERIC(18,4),
    stock_posterior NUMERIC(18,4),
    documento_tipo VARCHAR(50),
    documento_id BIGINT,
    documento_uuid UUID,
    business_uuid UUID,
    correlation_id UUID,
    fecha_negocio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(100) NOT NULL,
    observacion VARCHAR(1000),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_movimiento_inventario PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_inventario_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_movimiento_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_movimiento_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_movimiento_naturaleza CHECK (naturaleza IN ('E','S')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_movimiento_costo CHECK (costo_unitario >= 0 AND costo_total >= 0)
);

CREATE TABLE sch_farmacia.reserva_inventario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    almacen_id BIGINT,
    sku_id BIGINT NOT NULL,
    lote_id BIGINT,
    tipo_origen VARCHAR(30) NOT NULL,
    origen_uuid UUID NOT NULL,
    idempotency_key VARCHAR(150),
    cantidad NUMERIC(18,4) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    expira_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMPTZ,
    released_reason VARCHAR(500),
    CONSTRAINT pk_reserva_inventario PRIMARY KEY (id),
    CONSTRAINT uk_reserva_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_reserva_origen UNIQUE NULLS NOT DISTINCT (tenant_id, tipo_origen, origen_uuid, sku_id, lote_id),
    CONSTRAINT fk_reserva_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_reserva_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_reserva_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('ACTIVA','CONSUMIDA','LIBERADA','EXPIRADA','CANCELADA'))
);

CREATE TABLE sch_farmacia.transferencia_inventario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_origen_id BIGINT NOT NULL,
    almacen_origen_id BIGINT NOT NULL,
    establecimiento_destino_id BIGINT NOT NULL,
    almacen_destino_id BIGINT NOT NULL,
    numero VARCHAR(50) NOT NULL,
    motivo VARCHAR(500),
    prioridad VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    fecha_solicitud TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aprobado_at TIMESTAMPTZ,
    despachado_at TIMESTAMPTZ,
    recibido_at TIMESTAMPTZ,
    estado VARCHAR(30) NOT NULL DEFAULT 'SOLICITADA',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_transferencia PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_transferencia_numero UNIQUE (tenant_id, numero),
    CONSTRAINT uk_transferencia_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_transferencia_alm_origen FOREIGN KEY (tenant_id, empresa_id, establecimiento_origen_id, almacen_origen_id) REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_transferencia_alm_destino FOREIGN KEY (tenant_id, empresa_id, establecimiento_destino_id, almacen_destino_id) REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_transferencia_distintos CHECK (almacen_origen_id <> almacen_destino_id OR establecimiento_origen_id <> establecimiento_destino_id),
    CONSTRAINT ck_transferencia_prioridad CHECK (prioridad IN ('BAJA','NORMAL','ALTA','URGENTE')),
    CONSTRAINT ck_transferencia_estado CHECK (estado IN ('SOLICITADA','APROBADA','DESPACHADA','PARCIALMENTE_RECIBIDA','RECIBIDA','CERRADA','CANCELADA'))
);

CREATE TABLE sch_farmacia.transferencia_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    transferencia_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    sku_id BIGINT NOT NULL,
    cantidad_solicitada NUMERIC(18,4) NOT NULL,
    cantidad_aprobada NUMERIC(18,4),
    observacion VARCHAR(500),
    CONSTRAINT pk_transferencia_linea PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_linea UNIQUE (transferencia_id, numero_linea),
    CONSTRAINT uk_transferencia_linea_scope_id UNIQUE (tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_transferencia_linea_trans FOREIGN KEY (tenant_id, empresa_id, transferencia_id) REFERENCES sch_farmacia.transferencia_inventario(tenant_id, empresa_id, id),
    CONSTRAINT fk_transferencia_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_transferencia_linea_cantidad CHECK (cantidad_solicitada > 0 AND (cantidad_aprobada IS NULL OR (cantidad_aprobada >= 0 AND cantidad_aprobada <= cantidad_solicitada)))
);

CREATE TABLE sch_farmacia.transferencia_despacho (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    transferencia_id BIGINT NOT NULL,
    guia_remision VARCHAR(80),
    fecha_despacho TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    actor VARCHAR(100) NOT NULL,
    observacion VARCHAR(1000),
    CONSTRAINT pk_transferencia_despacho PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_despacho_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_transferencia_despacho_scope_id UNIQUE (tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_transferencia_despacho_trans FOREIGN KEY (tenant_id, empresa_id, transferencia_id) REFERENCES sch_farmacia.transferencia_inventario(tenant_id, empresa_id, id),
    CONSTRAINT ck_transferencia_despacho_estado CHECK (estado IN ('CONFIRMADO','ANULADO'))
);

CREATE TABLE sch_farmacia.transferencia_despacho_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    transferencia_id BIGINT NOT NULL,
    despacho_id BIGINT NOT NULL,
    transferencia_linea_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    cantidad_despachada NUMERIC(18,4) NOT NULL,
    CONSTRAINT pk_transferencia_despacho_linea PRIMARY KEY (id),
    CONSTRAINT fk_trans_desp_linea_desp FOREIGN KEY (tenant_id, empresa_id, transferencia_id, despacho_id) REFERENCES sch_farmacia.transferencia_despacho(tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_desp_linea_trans_linea FOREIGN KEY (tenant_id, empresa_id, transferencia_id, transferencia_linea_id) REFERENCES sch_farmacia.transferencia_linea(tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_desp_linea_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_trans_desp_linea_cantidad CHECK (cantidad_despachada > 0)
);

CREATE TABLE sch_farmacia.transferencia_recepcion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    transferencia_id BIGINT NOT NULL,
    fecha_recepcion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(100) NOT NULL,
    observacion VARCHAR(1000),
    CONSTRAINT pk_transferencia_recepcion PRIMARY KEY (id),
    CONSTRAINT uk_transferencia_recepcion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_transferencia_recepcion_scope_id UNIQUE (tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_transferencia_recepcion_trans FOREIGN KEY (tenant_id, empresa_id, transferencia_id) REFERENCES sch_farmacia.transferencia_inventario(tenant_id, empresa_id, id)
);

CREATE TABLE sch_farmacia.transferencia_recepcion_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    transferencia_id BIGINT NOT NULL,
    recepcion_id BIGINT NOT NULL,
    transferencia_linea_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    cantidad_recibida NUMERIC(18,4) NOT NULL,
    diferencia_motivo VARCHAR(500),
    CONSTRAINT pk_transferencia_recepcion_linea PRIMARY KEY (id),
    CONSTRAINT fk_trans_rec_linea_rec FOREIGN KEY (tenant_id, empresa_id, transferencia_id, recepcion_id) REFERENCES sch_farmacia.transferencia_recepcion(tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_rec_linea_trans_linea FOREIGN KEY (tenant_id, empresa_id, transferencia_id, transferencia_linea_id) REFERENCES sch_farmacia.transferencia_linea(tenant_id, empresa_id, transferencia_id, id),
    CONSTRAINT fk_trans_rec_linea_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_trans_rec_linea_cantidad CHECK (cantidad_recibida >= 0)
);

CREATE TABLE sch_farmacia.conteo_inventario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    numero VARCHAR(50),
    tipo_conteo VARCHAR(20) NOT NULL,
    conteo_ciego BOOLEAN NOT NULL DEFAULT TRUE,
    motivo VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    iniciado_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cerrado_at TIMESTAMPTZ,
    aprobado_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT pk_conteo_inventario PRIMARY KEY (id),
    CONSTRAINT uk_conteo_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_conteo_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_conteo_almacen FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, almacen_id) REFERENCES sch_farmacia.almacen(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_conteo_tipo CHECK (tipo_conteo IN ('TOTAL','CICLICO','SELECTIVO')),
    CONSTRAINT ck_conteo_estado CHECK (estado IN ('ABIERTO','EN_CONTEO','CONCILIADO','APROBADO','CERRADO','CANCELADO'))
);

CREATE TABLE sch_farmacia.conteo_inventario_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    conteo_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    lote_id BIGINT,
    cantidad_sistema NUMERIC(18,4) NOT NULL,
    cantidad_contada NUMERIC(18,4),
    diferencia NUMERIC(18,4) GENERATED ALWAYS AS (COALESCE(cantidad_contada, cantidad_sistema) - cantidad_sistema) STORED,
    observacion VARCHAR(500),
    CONSTRAINT pk_conteo_inventario_linea PRIMARY KEY (id),
    CONSTRAINT fk_conteo_linea_conteo FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, conteo_id) REFERENCES sch_farmacia.conteo_inventario(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_conteo_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_conteo_linea_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_conteo_linea_cant CHECK (cantidad_sistema >= 0 AND (cantidad_contada IS NULL OR cantidad_contada >= 0))
);

CREATE INDEX ix_lote_vencimiento ON sch_farmacia.lote(tenant_id, fecha_vencimiento) WHERE estado_lote = 'HABILITADO';
CREATE INDEX ix_posicion_stock_lookup ON sch_farmacia.posicion_inventario(tenant_id, establecimiento_id, sku_id, estado_inventario);
CREATE INDEX ix_movimiento_inventario_fecha ON sch_farmacia.movimiento_inventario(tenant_id, establecimiento_id, fecha_negocio DESC);
CREATE INDEX ix_reserva_activa ON sch_farmacia.reserva_inventario(tenant_id, establecimiento_id, sku_id) WHERE estado = 'ACTIVA';

-- =============================================================
-- V006__precios_promociones.sql
-- =============================================================
-- Listas de precios y promociones versionadas.
-- La lógica compleja permanece en policies del dominio; la BD conserva vigencia, alcance y trazabilidad.

CREATE TABLE sch_farmacia.lista_precio (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500),
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    tipo_lista VARCHAR(30) NOT NULL DEFAULT 'VENTA',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_lista_precio PRIMARY KEY (id),
    CONSTRAINT uk_lista_precio_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_lista_precio_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT uk_lista_precio_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_lista_precio_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_lista_precio_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_lista_precio_tipo CHECK (tipo_lista IN ('VENTA','MAYORISTA','CONVENIO','ECOMMERCE','OTRA')),
    CONSTRAINT ck_lista_precio_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.lista_precio_version (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    lista_precio_id BIGINT NOT NULL,
    numero_version INTEGER NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE,
    periodo_vigencia DATERANGE GENERATED ALWAYS AS (daterange(vigente_desde, vigente_hasta, '[]')) STORED,
    estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    motivo_cambio VARCHAR(1000),
    publicado_at TIMESTAMPTZ,
    publicado_por VARCHAR(100),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_lista_precio_version PRIMARY KEY (id),
    CONSTRAINT uk_lista_precio_version_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_lista_precio_version_num UNIQUE (lista_precio_id, numero_version),
    CONSTRAINT uk_lista_precio_version_scope_id UNIQUE (tenant_id, lista_precio_id, id),
    CONSTRAINT fk_lista_precio_version_lista FOREIGN KEY (tenant_id, lista_precio_id) REFERENCES sch_farmacia.lista_precio(tenant_id, id),
    CONSTRAINT ck_lista_precio_version_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_lista_precio_version_estado CHECK (estado_publicacion IN ('BORRADOR','PUBLICADO','RETIRADO')),
    CONSTRAINT ex_lista_precio_publicada_no_overlap EXCLUDE USING gist (lista_precio_id WITH =, periodo_vigencia WITH &&) WHERE (estado_publicacion = 'PUBLICADO')
);

CREATE TABLE sch_farmacia.lista_precio_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    lista_precio_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    establecimiento_id BIGINT,
    canal VARCHAR(30) NOT NULL DEFAULT 'TIENDA',
    precio NUMERIC(18,4) NOT NULL,
    precio_minimo NUMERIC(18,4),
    margen_referencia NUMERIC(10,4),
    CONSTRAINT pk_lista_precio_item PRIMARY KEY (id),
    CONSTRAINT uk_lista_precio_item UNIQUE NULLS NOT DISTINCT (version_id, sku_id, establecimiento_id, canal),
    CONSTRAINT fk_lista_precio_item_version FOREIGN KEY (tenant_id, lista_precio_id, version_id) REFERENCES sch_farmacia.lista_precio_version(tenant_id, lista_precio_id, id),
    CONSTRAINT fk_lista_precio_item_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_lista_precio_item_precio CHECK (precio >= 0 AND (precio_minimo IS NULL OR (precio_minimo >= 0 AND precio_minimo <= precio)))
);

CREATE TABLE sch_farmacia.promocion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(250) NOT NULL,
    descripcion VARCHAR(1000),
    tipo_promocion VARCHAR(40) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_promocion PRIMARY KEY (id),
    CONSTRAINT uk_promocion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_promocion_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT uk_promocion_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_promocion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_promocion_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_promocion_tipo CHECK (tipo_promocion IN ('DESCUENTO_PORCENTAJE','DESCUENTO_MONTO','PRECIO_ESPECIAL','NXM','COMBO','CUPON','PUNTOS','OTRA')),
    CONSTRAINT ck_promocion_estado CHECK (estado IN ('BORRADOR','ACTIVA','INACTIVA','CERRADA'))
);

CREATE TABLE sch_farmacia.promocion_version (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    promocion_id BIGINT NOT NULL,
    numero_version INTEGER NOT NULL,
    vigente_desde TIMESTAMPTZ NOT NULL,
    vigente_hasta TIMESTAMPTZ,
    canal VARCHAR(30) NOT NULL DEFAULT 'TODOS',
    condiciones JSONB NOT NULL DEFAULT '{}'::jsonb,
    beneficio JSONB NOT NULL DEFAULT '{}'::jsonb,
    prioridad INTEGER NOT NULL DEFAULT 100,
    combinable BOOLEAN NOT NULL DEFAULT FALSE,
    max_usos_total BIGINT,
    max_usos_cliente INTEGER,
    estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_promocion_version PRIMARY KEY (id),
    CONSTRAINT uk_promocion_version_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_promocion_version_num UNIQUE (promocion_id, numero_version),
    CONSTRAINT uk_promocion_version_scope_id UNIQUE (tenant_id, promocion_id, id),
    CONSTRAINT fk_promocion_version_promocion FOREIGN KEY (tenant_id, promocion_id) REFERENCES sch_farmacia.promocion(tenant_id, id),
    CONSTRAINT ck_promocion_version_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_promocion_version_limites CHECK ((max_usos_total IS NULL OR max_usos_total > 0) AND (max_usos_cliente IS NULL OR max_usos_cliente > 0)),
    CONSTRAINT ck_promocion_version_estado CHECK (estado_publicacion IN ('BORRADOR','PUBLICADO','RETIRADO'))
);

CREATE TABLE sch_farmacia.promocion_objetivo (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    promocion_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    tipo_objetivo VARCHAR(30) NOT NULL,
    sku_id BIGINT,
    categoria_id BIGINT,
    establecimiento_id BIGINT,
    valor_texto VARCHAR(200),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_promocion_objetivo PRIMARY KEY (id),
    CONSTRAINT fk_prom_obj_version FOREIGN KEY (tenant_id, promocion_id, version_id) REFERENCES sch_farmacia.promocion_version(tenant_id, promocion_id, id),
    CONSTRAINT fk_prom_obj_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_prom_obj_categoria FOREIGN KEY (tenant_id, categoria_id) REFERENCES sch_farmacia.categoria_producto(tenant_id, id),
    CONSTRAINT ck_prom_obj_tipo CHECK (tipo_objetivo IN ('SKU','CATEGORIA','ESTABLECIMIENTO','SEGMENTO','OTRO'))
);

CREATE INDEX ix_lista_precio_item_sku ON sch_farmacia.lista_precio_item(tenant_id, sku_id);
CREATE INDEX ix_promocion_vigente ON sch_farmacia.promocion_version(tenant_id, vigente_desde, vigente_hasta) WHERE estado_publicacion = 'PUBLICADO';

-- =============================================================
-- V007__retail_pos_ventas.sql
-- =============================================================
-- Clientes, POS/caja, ventas, pagos y devoluciones comerciales.
-- Venta, dispensación y CPE permanecen separados.

CREATE TABLE sch_farmacia.cliente (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    tipo_cliente VARCHAR(30) NOT NULL DEFAULT 'NATURAL',
    tipo_documento VARCHAR(20),
    numero_documento VARCHAR(30),
    nombres VARCHAR(150),
    apellidos VARCHAR(180),
    razon_social VARCHAR(300),
    email CITEXT,
    telefono VARCHAR(40),
    fecha_nacimiento DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_cliente PRIMARY KEY (id),
    CONSTRAINT uk_cliente_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cliente_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_cliente_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_cliente_tipo CHECK (tipo_cliente IN ('NATURAL','JURIDICO','SIN_IDENTIFICAR')),
    CONSTRAINT ck_cliente_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO'))
);

CREATE UNIQUE INDEX uk_cliente_documento ON sch_farmacia.cliente(tenant_id, tipo_documento, numero_documento) WHERE numero_documento IS NOT NULL;

CREATE TABLE sch_farmacia.cliente_consentimiento (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    finalidad_codigo VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    otorgado_at TIMESTAMPTZ,
    revocado_at TIMESTAMPTZ,
    evidencia_uri TEXT,
    fuente VARCHAR(80),
    CONSTRAINT pk_cliente_consentimiento PRIMARY KEY (id),
    CONSTRAINT uk_cliente_consentimiento_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_cliente_consent_cliente FOREIGN KEY (tenant_id, cliente_id) REFERENCES sch_farmacia.cliente(tenant_id, id),
    CONSTRAINT ck_cliente_consent_estado CHECK (estado IN ('OTORGADO','REVOCADO','NO_OTORGADO'))
);

CREATE TABLE sch_farmacia.medio_pago (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    requiere_referencia BOOLEAN NOT NULL DEFAULT FALSE,
    permite_vuelto BOOLEAN NOT NULL DEFAULT FALSE,
    proveedor_default VARCHAR(80),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_medio_pago PRIMARY KEY (id),
    CONSTRAINT uk_medio_pago_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT uk_medio_pago_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_medio_pago_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_medio_pago_tipo CHECK (tipo IN ('EFECTIVO','TARJETA','TRANSFERENCIA','BILLETERA','CREDITO','OTRO')),
    CONSTRAINT ck_medio_pago_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.turno_caja (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    terminal_id BIGINT NOT NULL,
    cajero_usuario_uuid UUID NOT NULL,
    apertura_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fondo_inicial NUMERIC(18,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    cierre_at TIMESTAMPTZ,
    total_ventas_sistema NUMERIC(18,2),
    total_ingresos_sistema NUMERIC(18,2),
    total_retiros_sistema NUMERIC(18,2),
    total_sistema NUMERIC(18,2),
    total_declarado NUMERIC(18,2),
    diferencia NUMERIC(18,2),
    observacion_cierre VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_turno_caja PRIMARY KEY (id),
    CONSTRAINT uk_turno_caja_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_turno_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_turno_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_farmacia.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_turno_caja_estado CHECK (estado IN ('ABIERTO','EN_ARQUEO','CERRADO','ANULADO')),
    CONSTRAINT ck_turno_fondo CHECK (fondo_inicial >= 0)
);

CREATE UNIQUE INDEX uk_turno_terminal_abierto ON sch_farmacia.turno_caja(tenant_id, terminal_id) WHERE estado IN ('ABIERTO','EN_ARQUEO');

CREATE TABLE sch_farmacia.movimiento_caja (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    turno_caja_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    concepto VARCHAR(180),
    monto NUMERIC(18,2) NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    referencia_tipo VARCHAR(60),
    referencia_uuid UUID,
    motivo VARCHAR(500),
    actor_usuario_uuid UUID NOT NULL,
    ocurrido_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_movimiento_caja PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_caja_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_movimiento_caja_turno FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, turno_caja_id) REFERENCES sch_farmacia.turno_caja(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_movimiento_caja_tipo CHECK (tipo IN ('INGRESO','RETIRO','AJUSTE')),
    CONSTRAINT ck_movimiento_caja_monto CHECK (monto > 0)
);

CREATE TABLE sch_farmacia.venta (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    terminal_id BIGINT NOT NULL,
    turno_caja_id BIGINT NOT NULL,
    cliente_id BIGINT,
    vendedor_usuario_uuid UUID,
    business_uuid UUID NOT NULL DEFAULT uuidv7(),
    numero_operacion VARCHAR(60) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    origen_operacion VARCHAR(20) NOT NULL DEFAULT 'CENTRAL',
    store_sequence BIGINT,
    canal VARCHAR(30) NOT NULL DEFAULT 'TIENDA',
    tipo_venta VARCHAR(30) NOT NULL DEFAULT 'PRESENCIAL',
    pedido_externo_ref VARCHAR(150),
    fecha_venta TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    subtotal NUMERIC(18,2) NOT NULL,
    descuento_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuesto_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    redondeo NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADA',
    version_lock BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_venta PRIMARY KEY (id),
    CONSTRAINT uk_venta_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_venta_business_uuid UNIQUE (tenant_id, business_uuid),
    CONSTRAINT uk_venta_numero_operacion UNIQUE (tenant_id, empresa_id, numero_operacion),
    CONSTRAINT uk_venta_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_venta_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT uk_venta_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_venta_terminal FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, terminal_id) REFERENCES sch_farmacia.terminal_pos(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_venta_turno FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, turno_caja_id) REFERENCES sch_farmacia.turno_caja(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_venta_cliente FOREIGN KEY (tenant_id, cliente_id) REFERENCES sch_farmacia.cliente(tenant_id, id),
    CONSTRAINT ck_venta_origen CHECK (origen_operacion IN ('CENTRAL','STORE_EDGE')),
    CONSTRAINT ck_venta_tipo CHECK (tipo_venta IN ('PRESENCIAL','WEB','MOBILE','CALL_CENTER','MARKETPLACE')),
    CONSTRAINT ck_venta_estado CHECK (estado IN ('CONFIRMADA','ANULADA','PARCIALMENTE_DEVUELTA','DEVUELTA')),
    CONSTRAINT ck_venta_totales CHECK (subtotal >= 0 AND descuento_total >= 0 AND impuesto_total >= 0 AND total >= 0)
);

CREATE TABLE sch_farmacia.venta_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    venta_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    sku_id BIGINT NOT NULL,
    descripcion_snapshot VARCHAR(500) NOT NULL,
    unidad_venta_codigo VARCHAR(30),
    cantidad NUMERIC(18,4) NOT NULL,
    precio_lista NUMERIC(18,4),
    precio_unitario NUMERIC(18,4) NOT NULL,
    descuento NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuesto NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_linea NUMERIC(18,2) NOT NULL,
    requiere_dispensacion BOOLEAN NOT NULL DEFAULT FALSE,
    promocion_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    price_decision_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    regulatory_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_venta_linea PRIMARY KEY (id),
    CONSTRAINT uk_venta_linea UNIQUE (venta_id, numero_linea),
    CONSTRAINT uk_venta_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_venta_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, venta_id, id),
    CONSTRAINT fk_venta_linea_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_farmacia.venta(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_venta_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_venta_linea_unidad FOREIGN KEY (unidad_venta_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT ck_venta_linea_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_venta_linea_importes CHECK ((precio_lista IS NULL OR precio_lista >= 0) AND precio_unitario >= 0 AND descuento >= 0 AND impuesto >= 0 AND total_linea >= 0)
);

CREATE TABLE sch_farmacia.venta_linea_lote (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    venta_linea_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario_snapshot NUMERIC(18,6),
    CONSTRAINT pk_venta_linea_lote PRIMARY KEY (id),
    CONSTRAINT uk_venta_linea_lote UNIQUE (venta_linea_id, lote_id),
    CONSTRAINT fk_venta_linea_lote_linea FOREIGN KEY (tenant_id, venta_linea_id) REFERENCES sch_farmacia.venta_linea(tenant_id, id),
    CONSTRAINT fk_venta_linea_lote_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_venta_linea_lote_cantidad CHECK (cantidad > 0)
);

CREATE TABLE sch_farmacia.pago_venta (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    venta_id BIGINT NOT NULL,
    medio_pago_id BIGINT NOT NULL,
    monto NUMERIC(18,2) NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    proveedor_pago VARCHAR(80),
    referencia_externa VARCHAR(200),
    codigo_autorizacion VARCHAR(120),
    idempotency_key VARCHAR(160),
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    pagado_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_pago_venta PRIMARY KEY (id),
    CONSTRAINT uk_pago_venta_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_pago_venta_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_farmacia.venta(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_pago_venta_medio FOREIGN KEY (tenant_id, medio_pago_id) REFERENCES sch_farmacia.medio_pago(tenant_id, id),
    CONSTRAINT ck_pago_venta_monto CHECK (monto > 0),
    CONSTRAINT ck_pago_venta_estado CHECK (estado IN ('PENDIENTE','CONFIRMADO','RECHAZADO','REVERSADO','EXTORNADO'))
);

CREATE TABLE sch_farmacia.devolucion_comercial (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    venta_id BIGINT NOT NULL,
    numero VARCHAR(50) NOT NULL,
    motivo_codigo VARCHAR(50),
    motivo VARCHAR(1000) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    total_reembolso NUMERIC(18,2) NOT NULL DEFAULT 0,
    aprobado_por_usuario_uuid UUID,
    aprobado_at TIMESTAMPTZ,
    reembolsado_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_devolucion_comercial PRIMARY KEY (id),
    CONSTRAINT uk_devolucion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_devolucion_numero UNIQUE (tenant_id, numero),
    CONSTRAINT uk_devolucion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_devolucion_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_farmacia.venta(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_devolucion_estado CHECK (estado IN ('REGISTRADA','EN_REVISION','APROBADA','RECHAZADA','REEMBOLSADA','CERRADA')),
    CONSTRAINT ck_devolucion_total CHECK (total_reembolso >= 0)
);

CREATE TABLE sch_farmacia.devolucion_comercial_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    devolucion_id BIGINT NOT NULL,
    venta_linea_id BIGINT NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    monto_devuelto NUMERIC(18,2) NOT NULL DEFAULT 0,
    disposicion_inventario VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_EVALUACION',
    evaluado_por VARCHAR(100),
    evaluado_at TIMESTAMPTZ,
    observacion VARCHAR(1000),
    CONSTRAINT pk_devolucion_linea PRIMARY KEY (id),
    CONSTRAINT uk_devolucion_linea UNIQUE (devolucion_id, venta_linea_id),
    CONSTRAINT uk_devolucion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_devolucion_linea_dev FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, devolucion_id) REFERENCES sch_farmacia.devolucion_comercial(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_devolucion_linea_venta_linea FOREIGN KEY (tenant_id, venta_linea_id) REFERENCES sch_farmacia.venta_linea(tenant_id, id),
    CONSTRAINT ck_devolucion_linea_cantidad CHECK (cantidad > 0 AND monto_devuelto >= 0),
    CONSTRAINT ck_devolucion_disposicion CHECK (disposicion_inventario IN ('PENDIENTE_EVALUACION','NO_VENDIBLE','CUARENTENA','REINTEGRO_AUTORIZADO','DISPOSICION_FINAL'))
);

CREATE TABLE sch_farmacia.devolucion_linea_lote (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    devolucion_linea_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    CONSTRAINT pk_devolucion_linea_lote PRIMARY KEY (id),
    CONSTRAINT fk_dev_lote_linea FOREIGN KEY (tenant_id, devolucion_linea_id) REFERENCES sch_farmacia.devolucion_comercial_linea(tenant_id, id),
    CONSTRAINT fk_dev_lote_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_dev_lote_cantidad CHECK (cantidad > 0)
);

CREATE INDEX ix_venta_fecha ON sch_farmacia.venta(tenant_id, establecimiento_id, fecha_venta DESC);
CREATE INDEX ix_venta_cliente ON sch_farmacia.venta(tenant_id, cliente_id, fecha_venta DESC);
CREATE INDEX ix_venta_store_seq ON sch_farmacia.venta(tenant_id, establecimiento_id, terminal_id, store_sequence) WHERE origen_operacion = 'STORE_EDGE';
CREATE INDEX ix_pago_venta ON sch_farmacia.pago_venta(tenant_id, venta_id);

-- =============================================================
-- V008__prescripcion_dispensacion.sql
-- =============================================================
-- Prescripción y dispensación farmacéutica.
-- El paciente puede no ser cliente comercial; se preserva snapshot mínimo de la prescripción.

CREATE TABLE sch_farmacia.prescripcion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    tipo_documento_prescripcion VARCHAR(40) NOT NULL,
    numero_documento VARCHAR(120),
    fecha_emision TIMESTAMPTZ NOT NULL,
    fecha_vencimiento TIMESTAMPTZ,
    paciente_ref UUID,
    paciente_tipo_documento VARCHAR(20),
    paciente_numero_documento VARCHAR(30),
    paciente_nombre_snapshot VARCHAR(300),
    prescriptor_nombre VARCHAR(300),
    prescriptor_tipo_identificador VARCHAR(30),
    prescriptor_identificador VARCHAR(100),
    prescriptor_especialidad VARCHAR(150),
    establecimiento_prescriptor VARCHAR(300),
    estado_validacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    validado_por_profesional_id BIGINT,
    validado_at TIMESTAMPTZ,
    motivo_invalidacion VARCHAR(1000),
    evidencia_uri TEXT,
    datos_minimos JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_prescripcion PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_prescripcion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_prescripcion_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_prescripcion_validador FOREIGN KEY (tenant_id, validado_por_profesional_id) REFERENCES sch_farmacia.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_prescripcion_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_emision),
    CONSTRAINT ck_prescripcion_estado CHECK (estado_validacion IN ('PENDIENTE','VALIDA','INVALIDA','VENCIDA','ANULADA','UTILIZADA_PARCIAL','UTILIZADA_TOTAL'))
);

CREATE TABLE sch_farmacia.prescripcion_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    prescripcion_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    producto_regulado_id BIGINT,
    descripcion_prescrita VARCHAR(500) NOT NULL,
    cantidad_prescrita NUMERIC(18,4),
    unidad_medida_codigo VARCHAR(30),
    dosis VARCHAR(200),
    frecuencia VARCHAR(200),
    duracion VARCHAR(200),
    via_administracion_codigo VARCHAR(30),
    indicaciones VARCHAR(1500),
    CONSTRAINT pk_prescripcion_linea PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_linea UNIQUE (prescripcion_id, numero_linea),
    CONSTRAINT uk_prescripcion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_prescripcion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, prescripcion_id, id),
    CONSTRAINT fk_prescripcion_linea_pres FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_farmacia.prescripcion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_prescripcion_linea_producto FOREIGN KEY (producto_regulado_id) REFERENCES sch_farmacia.producto_regulado(id),
    CONSTRAINT fk_prescripcion_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT fk_prescripcion_linea_via FOREIGN KEY (via_administracion_codigo) REFERENCES sch_farmacia.via_administracion(codigo),
    CONSTRAINT ck_prescripcion_linea_cantidad CHECK (cantidad_prescrita IS NULL OR cantidad_prescrita > 0)
);

CREATE TABLE sch_farmacia.dispensacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    prescripcion_id BIGINT,
    profesional_id BIGINT NOT NULL,
    fecha_dispensacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'EN_EVALUACION',
    decision_codigo VARCHAR(60),
    observacion VARCHAR(1500),
    informacion_brindada TEXT,
    advertencias_brindadas TEXT,
    evidencia_entrega JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_dispensacion PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_dispensacion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_dispensacion_pres FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_farmacia.prescripcion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_prof FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_farmacia.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_dispensacion_estado CHECK (estado IN ('EN_EVALUACION','AUTORIZADA','RECHAZADA','PARCIAL','CONFIRMADA','ANULADA'))
);

CREATE TABLE sch_farmacia.dispensacion_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    dispensacion_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    prescripcion_linea_id BIGINT,
    sku_id BIGINT NOT NULL,
    cantidad_autorizada NUMERIC(18,4) NOT NULL,
    cantidad_entregada NUMERIC(18,4) NOT NULL DEFAULT 0,
    decision_linea VARCHAR(30) NOT NULL DEFAULT 'AUTORIZADA',
    motivo_sustitucion VARCHAR(1000),
    regulatory_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_dispensacion_linea PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea UNIQUE (dispensacion_id, numero_linea),
    CONSTRAINT uk_dispensacion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_dispensacion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, dispensacion_id, id),
    CONSTRAINT fk_dispensacion_linea_disp FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, dispensacion_id) REFERENCES sch_farmacia.dispensacion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_linea_pres_linea FOREIGN KEY (tenant_id, prescripcion_linea_id) REFERENCES sch_farmacia.prescripcion_linea(tenant_id, id),
    CONSTRAINT fk_dispensacion_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_dispensacion_linea_cant CHECK (cantidad_autorizada > 0 AND cantidad_entregada >= 0 AND cantidad_entregada <= cantidad_autorizada),
    CONSTRAINT ck_dispensacion_linea_decision CHECK (decision_linea IN ('AUTORIZADA','RECHAZADA','SUSTITUIDA','PARCIAL'))
);

CREATE TABLE sch_farmacia.dispensacion_linea_lote (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    dispensacion_linea_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    cantidad_entregada NUMERIC(18,4) NOT NULL,
    CONSTRAINT pk_dispensacion_linea_lote PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea_lote UNIQUE (dispensacion_linea_id, lote_id),
    CONSTRAINT fk_disp_linea_lote_linea FOREIGN KEY (tenant_id, dispensacion_linea_id) REFERENCES sch_farmacia.dispensacion_linea(tenant_id, id),
    CONSTRAINT fk_disp_linea_lote_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_disp_linea_lote_cant CHECK (cantidad_entregada > 0)
);

ALTER TABLE sch_farmacia.venta_linea
    ADD COLUMN dispensacion_linea_id BIGINT,
    ADD CONSTRAINT fk_venta_linea_dispensacion_linea FOREIGN KEY (tenant_id, dispensacion_linea_id) REFERENCES sch_farmacia.dispensacion_linea(tenant_id, id);

CREATE INDEX ix_prescripcion_estado ON sch_farmacia.prescripcion(tenant_id, establecimiento_id, estado_validacion, fecha_emision DESC);
CREATE INDEX ix_dispensacion_fecha ON sch_farmacia.dispensacion(tenant_id, establecimiento_id, fecha_dispensacion DESC);

-- =============================================================
-- V009__productos_controlados.sql
-- =============================================================
-- Productos controlados: receta especial, movimientos y balances.

CREATE TABLE sch_farmacia.receta_controlada (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    prescripcion_id BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    tipo_receta VARCHAR(50) NOT NULL,
    numero_receta VARCHAR(120),
    fecha_expedicion TIMESTAMPTZ NOT NULL,
    fecha_limite_at TIMESTAMPTZ,
    regla_vigencia_codigo VARCHAR(80),
    estado_validacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    motivo_invalidacion VARCHAR(1000),
    retenida BOOLEAN NOT NULL DEFAULT FALSE,
    retenida_at TIMESTAMPTZ,
    archivada_at TIMESTAMPTZ,
    conservar_hasta DATE,
    profesional_validador_id BIGINT,
    evidencia_uri TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_receta_controlada PRIMARY KEY (id),
    CONSTRAINT uk_receta_controlada_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_receta_controlada_pres UNIQUE (prescripcion_id),
    CONSTRAINT uk_receta_controlada_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_receta_controlada_pres FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_farmacia.prescripcion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_receta_controlada_clasif FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_farmacia.clasificacion_controlada(codigo),
    CONSTRAINT fk_receta_controlada_prof FOREIGN KEY (tenant_id, profesional_validador_id) REFERENCES sch_farmacia.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_receta_controlada_fechas CHECK (fecha_limite_at IS NULL OR fecha_limite_at >= fecha_expedicion),
    CONSTRAINT ck_receta_controlada_estado CHECK (estado_validacion IN ('PENDIENTE','VALIDA','INVALIDA','VENCIDA','ADULTERADA','ATENDIDA','ANULADA'))
);

CREATE TABLE sch_farmacia.movimiento_controlado (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    lote_id BIGINT,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    cantidad NUMERIC(18,4) NOT NULL,
    saldo_anterior NUMERIC(18,4),
    saldo_posterior NUMERIC(18,4),
    documento_tipo VARCHAR(50) NOT NULL,
    documento_uuid UUID,
    receta_controlada_id BIGINT,
    fecha_movimiento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    profesional_id BIGINT,
    observacion VARCHAR(1000),
    CONSTRAINT pk_movimiento_controlado PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_controlado_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_mov_controlado_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_mov_controlado_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT fk_mov_controlado_clasif FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_farmacia.clasificacion_controlada(codigo),
    CONSTRAINT fk_mov_controlado_prof FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_farmacia.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT fk_mov_controlado_receta FOREIGN KEY (receta_controlada_id) REFERENCES sch_farmacia.receta_controlada(id),
    CONSTRAINT ck_mov_controlado_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_mov_controlado_saldo CHECK (saldo_anterior IS NULL OR saldo_anterior >= 0) ,
    CONSTRAINT ck_mov_controlado_tipo CHECK (tipo_movimiento IN ('ENTRADA','SALIDA','AJUSTE_POSITIVO','AJUSTE_NEGATIVO','DEVOLUCION','DISPOSICION'))
);

CREATE TABLE sch_farmacia.balance_controlado (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    periodo_desde DATE NOT NULL,
    periodo_hasta DATE NOT NULL,
    fecha_limite_presentacion DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    generado_at TIMESTAMPTZ,
    validado_at TIMESTAMPTZ,
    presentado_at TIMESTAMPTZ,
    identificador_presentacion VARCHAR(150),
    evidencia_uri TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_balance_controlado PRIMARY KEY (id),
    CONSTRAINT uk_balance_controlado_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_balance_controlado_periodo UNIQUE (tenant_id, establecimiento_id, clasificacion_controlada_codigo, periodo_desde, periodo_hasta),
    CONSTRAINT uk_balance_controlado_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_balance_controlado_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_balance_controlado_clasif FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_farmacia.clasificacion_controlada(codigo),
    CONSTRAINT ck_balance_controlado_fechas CHECK (periodo_hasta >= periodo_desde),
    CONSTRAINT ck_balance_controlado_estado CHECK (estado IN ('BORRADOR','GENERADO','VALIDADO','PRESENTADO','OBSERVADO','RECTIFICADO'))
);

CREATE TABLE sch_farmacia.balance_controlado_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    balance_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    existencia_inicial NUMERIC(18,4) NOT NULL DEFAULT 0,
    entradas NUMERIC(18,4) NOT NULL DEFAULT 0,
    salidas NUMERIC(18,4) NOT NULL DEFAULT 0,
    ajustes_positivos NUMERIC(18,4) NOT NULL DEFAULT 0,
    ajustes_negativos NUMERIC(18,4) NOT NULL DEFAULT 0,
    existencia_final NUMERIC(18,4) NOT NULL DEFAULT 0,
    CONSTRAINT pk_balance_controlado_linea PRIMARY KEY (id),
    CONSTRAINT uk_balance_controlado_linea UNIQUE (balance_id, sku_id),
    CONSTRAINT fk_balance_controlado_linea_balance FOREIGN KEY (tenant_id, balance_id) REFERENCES sch_farmacia.balance_controlado(tenant_id, id),
    CONSTRAINT fk_balance_controlado_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_balance_linea_cant CHECK (existencia_inicial >= 0 AND entradas >= 0 AND salidas >= 0 AND ajustes_positivos >= 0 AND ajustes_negativos >= 0 AND existencia_final >= 0)
);

CREATE INDEX ix_mov_controlado_fecha ON sch_farmacia.movimiento_controlado(tenant_id, establecimiento_id, fecha_movimiento DESC);

-- =============================================================
-- V010__fiscal_cpe.sql
-- =============================================================
-- Comprobantes electrónicos y notas de crédito. La modalidad SUNAT/PSE/OSE queda detrás del FiscalPort.

CREATE TABLE sch_farmacia.comprobante_electronico (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    venta_id BIGINT NOT NULL,
    tipo_cpe VARCHAR(20) NOT NULL,
    codigo_tipo_sunat VARCHAR(10),
    serie VARCHAR(20),
    numero BIGINT,
    fecha_emision TIMESTAMPTZ NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    cliente_tipo_documento VARCHAR(20),
    cliente_numero_documento VARCHAR(30),
    cliente_nombre VARCHAR(300),
    cliente_direccion VARCHAR(500),
    total_gravado NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_exonerado NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_inafecto NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_gratuito NUMERIC(18,2) NOT NULL DEFAULT 0,
    descuento_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuesto_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL,
    estado_fiscal VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_GENERACION',
    payload_version INTEGER NOT NULL DEFAULT 1,
    payload_interno JSONB NOT NULL DEFAULT '{}'::jsonb,
    hash_documento VARCHAR(200),
    xml_uri TEXT,
    pdf_uri TEXT,
    cdr_uri TEXT,
    codigo_respuesta VARCHAR(100),
    mensaje_respuesta VARCHAR(1500),
    enviado_at TIMESTAMPTZ,
    aceptado_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_comprobante_electronico PRIMARY KEY (id),
    CONSTRAINT uk_comprobante_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_comprobante_venta_tipo UNIQUE (tenant_id, venta_id, tipo_cpe),
    CONSTRAINT uk_comprobante_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_comprobante_venta FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, venta_id) REFERENCES sch_farmacia.venta(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_comprobante_tipo CHECK (tipo_cpe IN ('BOLETA','FACTURA')),
    CONSTRAINT ck_comprobante_estado CHECK (estado_fiscal IN ('PENDIENTE_GENERACION','GENERADO','PENDIENTE_ENVIO','ENVIADO','ACEPTADO','OBSERVADO','RECHAZADO','ANULADO','ERROR_TECNICO')),
    CONSTRAINT ck_comprobante_totales CHECK (total_gravado >= 0 AND total_exonerado >= 0 AND total_inafecto >= 0 AND total_gratuito >= 0 AND descuento_total >= 0 AND impuesto_total >= 0 AND total >= 0)
);

CREATE UNIQUE INDEX uk_comprobante_numeracion ON sch_farmacia.comprobante_electronico(tenant_id, empresa_id, tipo_cpe, serie, numero) WHERE serie IS NOT NULL AND numero IS NOT NULL;

CREATE TABLE sch_farmacia.cpe_envio_intento (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    comprobante_id BIGINT NOT NULL,
    numero_intento INTEGER NOT NULL,
    provider VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(160),
    enviado_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respondido_at TIMESTAMPTZ,
    resultado VARCHAR(30) NOT NULL,
    status_http INTEGER,
    codigo_respuesta VARCHAR(100),
    mensaje_respuesta VARCHAR(1500),
    metadata_sanitizada JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_cpe_envio_intento PRIMARY KEY (id),
    CONSTRAINT uk_cpe_envio_intento UNIQUE (comprobante_id, numero_intento),
    CONSTRAINT fk_cpe_envio_intento_comprobante FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, comprobante_id) REFERENCES sch_farmacia.comprobante_electronico(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_cpe_envio_resultado CHECK (resultado IN ('PENDIENTE','ACEPTADO','OBSERVADO','RECHAZADO','ERROR_TECNICO'))
);

CREATE TABLE sch_farmacia.nota_credito_electronica (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    comprobante_origen_id BIGINT NOT NULL,
    devolucion_id BIGINT,
    codigo_tipo_sunat VARCHAR(10),
    serie VARCHAR(20),
    numero BIGINT,
    motivo_codigo VARCHAR(30) NOT NULL,
    motivo_descripcion VARCHAR(500),
    fecha_emision TIMESTAMPTZ NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    total_impuesto NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL,
    estado_fiscal VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_GENERACION',
    payload_interno JSONB NOT NULL DEFAULT '{}'::jsonb,
    xml_uri TEXT,
    pdf_uri TEXT,
    cdr_uri TEXT,
    codigo_respuesta VARCHAR(100),
    mensaje_respuesta VARCHAR(1500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_nota_credito PRIMARY KEY (id),
    CONSTRAINT uk_nota_credito_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_nota_credito_cpe FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, comprobante_origen_id) REFERENCES sch_farmacia.comprobante_electronico(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_nota_credito_devolucion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, devolucion_id) REFERENCES sch_farmacia.devolucion_comercial(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT ck_nota_credito_total CHECK (total >= 0 AND total_impuesto >= 0),
    CONSTRAINT ck_nota_credito_estado CHECK (estado_fiscal IN ('PENDIENTE_GENERACION','GENERADO','PENDIENTE_ENVIO','ENVIADO','ACEPTADO','OBSERVADO','RECHAZADO','ANULADO','ERROR_TECNICO'))
);

CREATE UNIQUE INDEX uk_nota_credito_numeracion ON sch_farmacia.nota_credito_electronica(tenant_id, empresa_id, serie, numero) WHERE serie IS NOT NULL AND numero IS NOT NULL;

-- =============================================================
-- V011__recall_farmacovigilancia.sql
-- =============================================================
-- Seguridad de producto: recall + farmacovigilancia/tecnovigilancia.

CREATE TABLE sch_farmacia.caso_recall (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    codigo_caso VARCHAR(80) NOT NULL,
    tipo_alerta VARCHAR(40) NOT NULL DEFAULT 'RECALL',
    autoridad_fuente VARCHAR(150),
    fuente VARCHAR(300) NOT NULL,
    referencia_externa VARCHAR(250),
    fecha_publicacion TIMESTAMPTZ,
    fecha_alerta TIMESTAMPTZ NOT NULL,
    motivo VARCHAR(2000) NOT NULL,
    nivel_riesgo VARCHAR(30),
    instruccion_oficial TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTO',
    evidencia_uri TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    CONSTRAINT pk_caso_recall PRIMARY KEY (id),
    CONSTRAINT uk_caso_recall_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_caso_recall_codigo UNIQUE (tenant_id, codigo_caso),
    CONSTRAINT uk_caso_recall_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_caso_recall_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_caso_recall_estado CHECK (estado IN ('ABIERTO','EN_EJECUCION','CONCILIACION','CERRADO','CANCELADO'))
);

CREATE TABLE sch_farmacia.recall_producto_lote (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    caso_recall_id BIGINT NOT NULL,
    producto_regulado_id BIGINT,
    sku_id BIGINT,
    lote_id BIGINT,
    registro_sanitario_snapshot VARCHAR(120),
    denominacion_snapshot VARCHAR(500),
    numero_lote_snapshot VARCHAR(120),
    fecha_vencimiento_snapshot DATE,
    alcance VARCHAR(1000),
    CONSTRAINT pk_recall_producto_lote PRIMARY KEY (id),
    CONSTRAINT uk_recall_producto_lote UNIQUE NULLS NOT DISTINCT (caso_recall_id, producto_regulado_id, sku_id, lote_id, numero_lote_snapshot),
    CONSTRAINT fk_recall_producto_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_farmacia.caso_recall(tenant_id, id),
    CONSTRAINT fk_recall_producto_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_farmacia.producto_regulado(id),
    CONSTRAINT fk_recall_producto_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_recall_producto_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id)
);

CREATE TABLE sch_farmacia.recall_establecimiento (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    caso_recall_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    cantidad_identificada NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_inmovilizada NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_retirada NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_dispuesta NUMERIC(18,4) NOT NULL DEFAULT 0,
    confirmado_at TIMESTAMPTZ,
    confirmado_por VARCHAR(100),
    observacion VARCHAR(1000),
    CONSTRAINT pk_recall_establecimiento PRIMARY KEY (id),
    CONSTRAINT uk_recall_establecimiento UNIQUE (caso_recall_id, establecimiento_id),
    CONSTRAINT fk_recall_est_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_farmacia.caso_recall(tenant_id, id),
    CONSTRAINT fk_recall_est_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_recall_est_cant CHECK (cantidad_identificada >= 0 AND cantidad_inmovilizada >= 0 AND cantidad_retirada >= 0 AND cantidad_dispuesta >= 0),
    CONSTRAINT ck_recall_est_estado CHECK (estado IN ('PENDIENTE','INMOVILIZADO','EN_RETIRO','CONCILIADO','CERRADO'))
);

CREATE TABLE sch_farmacia.recall_accion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    caso_recall_id BIGINT NOT NULL,
    tipo_accion VARCHAR(40) NOT NULL,
    descripcion VARCHAR(1500) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    ejecutado_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evidencia_uri TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_recall_accion PRIMARY KEY (id),
    CONSTRAINT fk_recall_accion_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_farmacia.caso_recall(tenant_id, id)
);

CREATE TABLE sch_farmacia.reporte_seguridad (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT,
    establecimiento_id BIGINT,
    tipo_reporte VARCHAR(30) NOT NULL,
    origen_reporte VARCHAR(30) NOT NULL,
    venta_id BIGINT,
    lote_id BIGINT,
    paciente_ref UUID,
    reportante_ref UUID,
    descripcion_evento TEXT NOT NULL,
    gravedad_codigo VARCHAR(50),
    desenlace_codigo VARCHAR(50),
    ocurrido_at TIMESTAMPTZ,
    conocido_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    datos_contexto JSONB NOT NULL DEFAULT '{}'::jsonb,
    datos_sensibles_cifrado BYTEA,
    notificacion_oficial_at TIMESTAMPTZ,
    notificacion_canal VARCHAR(80),
    notificacion_identificador VARCHAR(150),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_reporte_seguridad PRIMARY KEY (id),
    CONSTRAINT uk_reporte_seguridad_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_reporte_seguridad_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_reporte_seguridad_venta FOREIGN KEY (tenant_id, venta_id) REFERENCES sch_farmacia.venta(tenant_id, id),
    CONSTRAINT fk_reporte_seguridad_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_reporte_seguridad_tipo CHECK (tipo_reporte IN ('FARMACOVIGILANCIA','TECNOVIGILANCIA')),
    CONSTRAINT ck_reporte_seguridad_origen CHECK (origen_reporte IN ('CLIENTE','PROFESIONAL','INTERNO','EXTERNO')),
    CONSTRAINT ck_reporte_seguridad_estado CHECK (estado IN ('REGISTRADO','EN_EVALUACION','NOTIFICADO','EN_SEGUIMIENTO','CERRADO'))
);

CREATE TABLE sch_farmacia.reporte_seguridad_producto (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    reporte_id BIGINT NOT NULL,
    producto_regulado_id BIGINT,
    sku_id BIGINT,
    lote_id BIGINT,
    sospechoso BOOLEAN NOT NULL DEFAULT TRUE,
    descripcion_producto VARCHAR(500),
    dosis_exposicion VARCHAR(300),
    fecha_inicio_uso TIMESTAMPTZ,
    fecha_fin_uso TIMESTAMPTZ,
    CONSTRAINT pk_reporte_seguridad_producto PRIMARY KEY (id),
    CONSTRAINT fk_reporte_seg_producto_reporte FOREIGN KEY (tenant_id, reporte_id) REFERENCES sch_farmacia.reporte_seguridad(tenant_id, id),
    CONSTRAINT fk_reporte_seg_producto_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_farmacia.producto_regulado(id),
    CONSTRAINT fk_reporte_seg_producto_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT fk_reporte_seg_producto_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id)
);

CREATE TABLE sch_farmacia.reporte_seguridad_seguimiento (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    reporte_id BIGINT NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_seguimiento VARCHAR(50),
    descripcion TEXT NOT NULL,
    actor VARCHAR(100) NOT NULL,
    evidencia_uri TEXT,
    CONSTRAINT pk_reporte_seguridad_seguimiento PRIMARY KEY (id),
    CONSTRAINT fk_reporte_seg_seg_reporte FOREIGN KEY (tenant_id, reporte_id) REFERENCES sch_farmacia.reporte_seguridad(tenant_id, id)
);

CREATE INDEX ix_recall_estado ON sch_farmacia.caso_recall(tenant_id, estado, fecha_alerta DESC);
CREATE INDEX ix_fvg_estado ON sch_farmacia.reporte_seguridad(tenant_id, estado, conocido_at DESC);

-- =============================================================
-- V012__erp_observatorio.sql
-- =============================================================
-- ERP boundary: documentos proveedor, tesorería/CxP/CxC, posting retail y Observatorio.

CREATE TABLE sch_farmacia.cuenta_bancaria_empresa (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    banco VARCHAR(150) NOT NULL,
    tipo_cuenta VARCHAR(30),
    numero_cuenta VARCHAR(100) NOT NULL,
    cci VARCHAR(100),
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    alias VARCHAR(100),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cuenta_bancaria_empresa PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_bancaria_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cuenta_bancaria UNIQUE (tenant_id, empresa_id, banco, numero_cuenta),
    CONSTRAINT fk_cuenta_bancaria_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_cuenta_bancaria_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_farmacia.factura_proveedor (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    orden_compra_id BIGINT,
    tipo_documento VARCHAR(20) NOT NULL,
    serie VARCHAR(20) NOT NULL,
    numero VARCHAR(30) NOT NULL,
    fecha_emision DATE NOT NULL,
    fecha_recepcion DATE,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    tipo_cambio NUMERIC(18,6),
    subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
    descuento_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuesto_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL,
    fecha_vencimiento DATE,
    estado_matching VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    diferencia_matching JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_factura_proveedor PRIMARY KEY (id),
    CONSTRAINT uk_factura_proveedor_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_factura_proveedor_doc UNIQUE (tenant_id, proveedor_id, tipo_documento, serie, numero),
    CONSTRAINT uk_factura_proveedor_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_factura_proveedor_proveedor FOREIGN KEY (tenant_id, proveedor_id) REFERENCES sch_farmacia.proveedor(tenant_id, id),
    CONSTRAINT fk_factura_proveedor_oc FOREIGN KEY (tenant_id, empresa_id, orden_compra_id) REFERENCES sch_farmacia.orden_compra(tenant_id, empresa_id, id),
    CONSTRAINT ck_factura_proveedor_total CHECK (subtotal >= 0 AND descuento_total >= 0 AND impuesto_total >= 0 AND total >= 0),
    CONSTRAINT ck_factura_matching CHECK (estado_matching IN ('PENDIENTE','COINCIDENTE','CON_DIFERENCIAS','APROBADO_EXCEPCION')),
    CONSTRAINT ck_factura_proveedor_estado CHECK (estado IN ('REGISTRADA','APROBADA','OBSERVADA','ANULADA'))
);

CREATE TABLE sch_farmacia.cuenta_por_pagar (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    factura_proveedor_id BIGINT NOT NULL,
    fecha_emision DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento DATE,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    monto_total NUMERIC(18,2) NOT NULL,
    monto_pagado NUMERIC(18,2) NOT NULL DEFAULT 0,
    saldo NUMERIC(18,2) GENERATED ALWAYS AS (monto_total - monto_pagado) STORED,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    erp_external_id VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cuenta_por_pagar PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_por_pagar_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cuenta_por_pagar_factura UNIQUE (factura_proveedor_id),
    CONSTRAINT fk_cuenta_por_pagar_factura FOREIGN KEY (tenant_id, empresa_id, factura_proveedor_id) REFERENCES sch_farmacia.factura_proveedor(tenant_id, empresa_id, id),
    CONSTRAINT ck_cuenta_por_pagar_montos CHECK (monto_total >= 0 AND monto_pagado >= 0 AND monto_pagado <= monto_total),
    CONSTRAINT ck_cuenta_por_pagar_estado CHECK (estado IN ('PENDIENTE','PARCIAL','PAGADA','BLOQUEADA','ANULADA'))
);

CREATE TABLE sch_farmacia.pago_cuenta_por_pagar (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    cuenta_por_pagar_id BIGINT NOT NULL,
    cuenta_bancaria_id BIGINT,
    fecha_pago TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto NUMERIC(18,2) NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    referencia VARCHAR(150),
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADO',
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT pk_pago_cxp PRIMARY KEY (id),
    CONSTRAINT uk_pago_cxp_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_pago_cxp_cuenta FOREIGN KEY (cuenta_por_pagar_id) REFERENCES sch_farmacia.cuenta_por_pagar(id),
    CONSTRAINT fk_pago_cxp_banco FOREIGN KEY (cuenta_bancaria_id) REFERENCES sch_farmacia.cuenta_bancaria_empresa(id),
    CONSTRAINT ck_pago_cxp_monto CHECK (monto > 0),
    CONSTRAINT ck_pago_cxp_estado CHECK (estado IN ('REGISTRADO','CONFIRMADO','ANULADO','EXTORNADO'))
);

CREATE TABLE sch_farmacia.cuenta_por_cobrar (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT,
    venta_id BIGINT,
    comprobante_id BIGINT,
    fecha_emision DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento DATE,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    monto_total NUMERIC(18,2) NOT NULL,
    monto_cobrado NUMERIC(18,2) NOT NULL DEFAULT 0,
    saldo NUMERIC(18,2) GENERATED ALWAYS AS (monto_total - monto_cobrado) STORED,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cuenta_por_cobrar PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_por_cobrar_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_cxc_cliente FOREIGN KEY (tenant_id, cliente_id) REFERENCES sch_farmacia.cliente(tenant_id, id),
    CONSTRAINT fk_cxc_venta FOREIGN KEY (tenant_id, venta_id) REFERENCES sch_farmacia.venta(tenant_id, id),
    CONSTRAINT fk_cxc_cpe FOREIGN KEY (comprobante_id) REFERENCES sch_farmacia.comprobante_electronico(id),
    CONSTRAINT ck_cxc_montos CHECK (monto_total >= 0 AND monto_cobrado >= 0 AND monto_cobrado <= monto_total),
    CONSTRAINT ck_cxc_estado CHECK (estado IN ('PENDIENTE','PARCIAL','COBRADA','VENCIDA','ANULADA'))
);

CREATE TABLE sch_farmacia.posting_retail (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    tipo_origen VARCHAR(30) NOT NULL,
    origen_uuid UUID NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL,
    fecha_contable DATE NOT NULL,
    payload_posting JSONB NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    external_id VARCHAR(150),
    numero_intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(1500),
    confirmado_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_posting_retail PRIMARY KEY (id),
    CONSTRAINT uk_posting_retail_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_posting_retail_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT ck_posting_retail_estado CHECK (estado IN ('PENDIENTE','EN_PROCESO','CONFIRMADO','ERROR_REINTENTABLE','ERROR_DEFINITIVO','REVERSADO'))
);

CREATE TABLE sch_farmacia.reporte_mensual_precios (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    periodo DATE NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    generado_at TIMESTAMPTZ,
    validado_at TIMESTAMPTZ,
    enviado_at TIMESTAMPTZ,
    mecanismo VARCHAR(80),
    identificador_envio VARCHAR(150),
    evidencia_uri TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reporte_mensual_precios PRIMARY KEY (id),
    CONSTRAINT uk_reporte_mensual_precios_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_reporte_mensual_periodo UNIQUE (tenant_id, empresa_id, periodo),
    CONSTRAINT uk_reporte_mensual_scope_id UNIQUE (tenant_id, empresa_id, id),
    CONSTRAINT fk_reporte_mensual_empresa FOREIGN KEY (tenant_id, empresa_id) REFERENCES sch_farmacia.empresa_operadora(tenant_id, id),
    CONSTRAINT ck_reporte_mensual_estado CHECK (estado IN ('BORRADOR','GENERADO','VALIDADO','ENVIADO','OBSERVADO','RECTIFICADO'))
);

CREATE TABLE sch_farmacia.reporte_mensual_precios_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    reporte_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    producto_regulado_id BIGINT,
    sku_id BIGINT NOT NULL,
    precio NUMERIC(18,4) NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PEN',
    fuente_precio VARCHAR(100) NOT NULL,
    version_precio VARCHAR(100),
    CONSTRAINT pk_reporte_mensual_precios_linea PRIMARY KEY (id),
    CONSTRAINT uk_reporte_mensual_precios_linea UNIQUE (reporte_id, establecimiento_id, sku_id),
    CONSTRAINT fk_reporte_mensual_linea_reporte FOREIGN KEY (tenant_id, empresa_id, reporte_id) REFERENCES sch_farmacia.reporte_mensual_precios(tenant_id, empresa_id, id),
    CONSTRAINT fk_reporte_mensual_linea_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_reporte_mensual_linea_producto FOREIGN KEY (producto_regulado_id) REFERENCES sch_farmacia.producto_regulado(id),
    CONSTRAINT fk_reporte_mensual_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_reporte_mensual_precio CHECK (precio >= 0)
);

CREATE INDEX ix_cxp_estado ON sch_farmacia.cuenta_por_pagar(tenant_id, estado, fecha_vencimiento);
CREATE INDEX ix_cxc_estado ON sch_farmacia.cuenta_por_cobrar(tenant_id, estado, fecha_vencimiento);
CREATE INDEX ix_posting_estado ON sch_farmacia.posting_retail(tenant_id, estado, created_at);

-- =============================================================
-- V013__seguridad_iam.sql
-- =============================================================
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

-- =============================================================
-- V014__auditoria.sql
-- =============================================================
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

-- =============================================================
-- V015__integracion_outbox_inbox.sql
-- =============================================================
-- Integración fiable y sincronización: catálogo de servicios + Outbox/Inbox/idempotencia.

CREATE TABLE sch_integracion.servicio_externo (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    tipo_servicio VARCHAR(60) NOT NULL,
    base_url TEXT,
    requiere_auth BOOLEAN NOT NULL DEFAULT TRUE,
    configuracion_no_secreta JSONB NOT NULL DEFAULT '{}'::jsonb,
    secret_ref VARCHAR(300),
    timeout_ms INTEGER,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_int_servicio_externo PRIMARY KEY (id),
    CONSTRAINT uk_int_servicio_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_int_servicio_codigo UNIQUE NULLS NOT DISTINCT (tenant_id, codigo),
    CONSTRAINT fk_int_servicio_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_int_servicio_timeout CHECK (timeout_ms IS NULL OR timeout_ms > 0),
    CONSTRAINT ck_int_servicio_estado CHECK (estado IN ('ACTIVO','INACTIVO','DEGRADADO'))
);

CREATE TABLE sch_integracion.outbox_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    message_id UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_uuid UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1,
    payload JSONB NOT NULL,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id UUID,
    causation_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    numero_intentos INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    ultimo_error VARCHAR(1500),
    CONSTRAINT pk_int_outbox PRIMARY KEY (id),
    CONSTRAINT uk_int_outbox_message UNIQUE (message_id),
    CONSTRAINT fk_int_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_int_outbox_estado CHECK (estado IN ('PENDIENTE','EN_PROCESO','PUBLICADO','ERROR_REINTENTABLE','ERROR_DEFINITIVO'))
);

CREATE TABLE sch_integracion.inbox_message (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    message_id UUID NOT NULL,
    tenant_id BIGINT NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    message_type VARCHAR(150) NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1,
    payload_hash VARCHAR(200),
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    estado VARCHAR(30) NOT NULL DEFAULT 'RECIBIDO',
    ultimo_error VARCHAR(1500),
    CONSTRAINT pk_int_inbox PRIMARY KEY (id),
    CONSTRAINT uk_int_inbox_message UNIQUE (source_system, message_id),
    CONSTRAINT fk_int_inbox_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_int_inbox_estado CHECK (estado IN ('RECIBIDO','PROCESANDO','PROCESADO','IGNORADO_DUPLICADO','ERROR_REINTENTABLE','ERROR_DEFINITIVO'))
);

CREATE TABLE sch_integracion.sync_checkpoint (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    stream_name VARCHAR(120) NOT NULL,
    last_sequence BIGINT,
    last_message_id UUID,
    last_synced_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_int_sync_checkpoint PRIMARY KEY (id),
    CONSTRAINT uk_int_sync_checkpoint UNIQUE (tenant_id, establecimiento_id, stream_name),
    CONSTRAINT fk_int_sync_checkpoint_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_int_sync_checkpoint_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id)
);

CREATE TABLE sch_integracion.integracion_intento (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    servicio_externo_id BIGINT,
    sistema_destino VARCHAR(80) NOT NULL,
    operacion VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(180),
    correlation_id UUID,
    request_ref UUID,
    request_hash VARCHAR(200),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    numero_intento INTEGER NOT NULL DEFAULT 0,
    codigo_respuesta VARCHAR(100),
    mensaje_respuesta VARCHAR(1500),
    status_http INTEGER,
    duracion_ms INTEGER,
    next_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    CONSTRAINT pk_int_integracion_intento PRIMARY KEY (id),
    CONSTRAINT uk_int_integracion_intento_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_int_integracion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_int_integracion_servicio FOREIGN KEY (servicio_externo_id) REFERENCES sch_integracion.servicio_externo(id),
    CONSTRAINT ck_int_integracion_estado CHECK (estado IN ('PENDIENTE','EN_PROCESO','CONFIRMADO','ERROR_REINTENTABLE','ERROR_DEFINITIVO'))
);

CREATE UNIQUE INDEX uk_int_integracion_idempotency ON sch_integracion.integracion_intento(tenant_id, sistema_destino, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ix_int_outbox_pendiente ON sch_integracion.outbox_event(estado, next_attempt_at, occurred_at) WHERE estado IN ('PENDIENTE','ERROR_REINTENTABLE');
CREATE INDEX ix_int_inbox_pendiente ON sch_integracion.inbox_message(estado, received_at) WHERE estado IN ('RECIBIDO','ERROR_REINTENTABLE');

-- =============================================================
-- V016__navegacion_dinamica_rbac.sql
-- =============================================================
-- Navegación dinámica desacoplada de la autorización real.
-- Adaptado del V2 aportado: el menú determina visibilidad/navegación; cada endpoint vuelve a autorizar.

CREATE TABLE sch_app.menu_navegacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    modulo_id BIGINT,
    menu_padre_id BIGINT,
    codigo VARCHAR(80) NOT NULL,
    aplicacion VARCHAR(40) NOT NULL DEFAULT 'ERP_WEB',
    etiqueta VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    ruta VARCHAR(240),
    icono VARCHAR(80),
    tipo VARCHAR(20) NOT NULL DEFAULT 'ITEM',
    modo_autorizacion VARCHAR(20) NOT NULL DEFAULT 'TODOS',
    orden INTEGER NOT NULL DEFAULT 0,
    es_visible BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_app_menu PRIMARY KEY (id),
    CONSTRAINT uk_app_menu_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_app_menu_aplicacion_codigo UNIQUE (aplicacion, codigo),
    CONSTRAINT fk_app_menu_modulo FOREIGN KEY (modulo_id) REFERENCES sch_seguridad.modulo_sistema(id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_menu_padre FOREIGN KEY (menu_padre_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_aplicacion CHECK (aplicacion IN ('ERP_WEB','POS_WEB','ECOMMERCE_WEB','MOBILE')),
    CONSTRAINT ck_app_menu_tipo CHECK (tipo IN ('GRUPO','ITEM','SEPARADOR')),
    CONSTRAINT ck_app_menu_auth CHECK (modo_autorizacion IN ('AUTENTICADO','CUALQUIERA','TODOS')),
    CONSTRAINT ck_app_menu_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT ck_app_menu_orden CHECK (orden >= 0),
    CONSTRAINT ck_app_menu_ruta CHECK (ruta IS NULL OR LEFT(ruta,1) = '/'),
    CONSTRAINT ck_app_menu_tipo_ruta CHECK ((tipo='ITEM' AND ruta IS NOT NULL) OR (tipo IN ('GRUPO','SEPARADOR') AND ruta IS NULL)),
    CONSTRAINT ck_app_menu_padre CHECK (menu_padre_id IS NULL OR menu_padre_id <> id)
);

CREATE TABLE sch_app.menu_navegacion_permiso (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    menu_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_menu_permiso PRIMARY KEY (id),
    CONSTRAINT uk_app_menu_permiso UNIQUE (menu_id, permiso_id),
    CONSTRAINT fk_app_menu_permiso_menu FOREIGN KEY (menu_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_menu_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES sch_seguridad.permiso(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_permiso_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE OR REPLACE FUNCTION sch_app.fn_validar_menu_jerarquia()
RETURNS TRIGGER AS $$
DECLARE
    app_padre VARCHAR(40);
    tipo_padre VARCHAR(20);
BEGIN
    IF NEW.id IS NOT NULL AND NEW.menu_padre_id = NEW.id THEN
        RAISE EXCEPTION 'Un menú no puede ser padre de sí mismo';
    END IF;
    IF NEW.menu_padre_id IS NULL THEN RETURN NEW; END IF;
    SELECT aplicacion, tipo INTO app_padre, tipo_padre FROM sch_app.menu_navegacion WHERE id = NEW.menu_padre_id AND estado='ACTIVO';
    IF NOT FOUND THEN RAISE EXCEPTION 'El menú padre no existe o no está activo'; END IF;
    IF app_padre <> NEW.aplicacion THEN RAISE EXCEPTION 'Padre e hijo deben pertenecer a la misma aplicación'; END IF;
    IF tipo_padre <> 'GRUPO' THEN RAISE EXCEPTION 'Solo un GRUPO puede contener hijos'; END IF;
    IF EXISTS (
        WITH RECURSIVE anc AS (
            SELECT id, menu_padre_id FROM sch_app.menu_navegacion WHERE id = NEW.menu_padre_id
            UNION ALL
            SELECT p.id, p.menu_padre_id FROM sch_app.menu_navegacion p JOIN anc a ON p.id = a.menu_padre_id
        ) SELECT 1 FROM anc WHERE id = NEW.id
    ) THEN RAISE EXCEPTION 'La relación genera un ciclo'; END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_app_menu_jerarquia BEFORE INSERT OR UPDATE ON sch_app.menu_navegacion FOR EACH ROW EXECUTE FUNCTION sch_app.fn_validar_menu_jerarquia();

CREATE INDEX ix_app_menu_padre_orden ON sch_app.menu_navegacion(aplicacion, menu_padre_id, orden, etiqueta) WHERE estado='ACTIVO' AND es_visible;
CREATE INDEX ix_app_menu_permiso ON sch_app.menu_navegacion_permiso(permiso_id, estado);
CREATE UNIQUE INDEX uk_app_menu_ruta_activa ON sch_app.menu_navegacion(aplicacion, ruta) WHERE ruta IS NOT NULL AND estado='ACTIVO';

-- Módulos base: no crean roles ni conceden permisos automáticamente.
INSERT INTO sch_seguridad.modulo_sistema(codigo,nombre,descripcion,orden) VALUES
('SEGURIDAD','Seguridad','Usuarios, roles, permisos y ámbitos.',10),
('ORGANIZACION','Organización','Empresas, establecimientos, almacenes y terminales.',20),
('CATALOGO','Catálogo','Productos regulatorios y SKU comerciales.',30),
('COMPRAS','Compras','Solicitudes, órdenes, recepción y proveedores.',40),
('INVENTARIO','Inventario','Stock, lotes, reservas, transferencias y conteos.',50),
('PRECIOS','Precios y promociones','Listas de precios y promociones.',60),
('VENTAS','Retail / POS','Caja, ventas, pagos y devoluciones.',70),
('DISPENSACION','Dispensación','Prescripciones y dispensación farmacéutica.',80),
('CONTROLADOS','Controlados','Recetas y movimientos de productos controlados.',90),
('FISCAL','Fiscal / CPE','Comprobantes electrónicos y notas de crédito.',100),
('RECALL','Recall','Alertas, inmovilización y retiros.',110),
('FARMACOVIGILANCIA','Farmacovigilancia','Reportes y seguimientos de seguridad.',120),
('ERP','ERP financiero','CxP/CxC, tesorería y posting.',130),
('REPORTES','Reportes','Observatorio, BI y reportes.',140)
ON CONFLICT (codigo) DO UPDATE SET nombre=EXCLUDED.nombre, descripcion=EXCLUDED.descripcion, orden=EXCLUDED.orden, es_activo=TRUE;

INSERT INTO sch_seguridad.permiso(modulo_id,codigo,recurso,accion,nombre,descripcion)
SELECT m.id, m.codigo || ':MODULO:VER', 'MODULO', 'VER', 'Ver ' || m.nombre, 'Permiso de entrada/visibilidad del módulo.'
FROM sch_seguridad.modulo_sistema m
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO sch_app.menu_navegacion(codigo,aplicacion,etiqueta,descripcion,tipo,modo_autorizacion,orden)
VALUES
('OPERACIONES','ERP_WEB','Operaciones','Operación comercial y farmacéutica.','GRUPO','AUTENTICADO',10),
('ADMINISTRACION','ERP_WEB','Administración','Configuración, seguridad y organización.','GRUPO','AUTENTICADO',20)
ON CONFLICT (aplicacion,codigo) DO UPDATE SET etiqueta=EXCLUDED.etiqueta, descripcion=EXCLUDED.descripcion, tipo=EXCLUDED.tipo, modo_autorizacion=EXCLUDED.modo_autorizacion, orden=EXCLUDED.orden, es_visible=TRUE, estado='ACTIVO';

WITH items(modulo_codigo,padre,codigo,etiqueta,descripcion,ruta,icono,orden) AS (
VALUES
(NULL,'OPERACIONES','DASHBOARD','Resumen','Resumen operativo.','/dashboard','LayoutDashboard',10),
('CATALOGO','OPERACIONES','CATALOGO','Catálogo','Productos y maestros.','/catalogo','PackageSearch',20),
('INVENTARIO','OPERACIONES','INVENTARIO','Inventario','Stock, lotes y vencimientos.','/inventario','Boxes',30),
('COMPRAS','OPERACIONES','COMPRAS','Compras','Proveedores, órdenes y recepción.','/compras','ShoppingCart',40),
('PRECIOS','OPERACIONES','PRECIOS','Precios','Listas y promociones.','/precios','Tags',50),
('VENTAS','OPERACIONES','VENTAS','Ventas','Ventas y devoluciones.','/ventas','ReceiptText',60),
('VENTAS','OPERACIONES','POS','Punto de venta','Venta rápida.','/pos','MonitorSmartphone',70),
('DISPENSACION','OPERACIONES','DISPENSACION','Dispensación','Prescripción y dispensación.','/dispensacion','Pill',80),
('CONTROLADOS','OPERACIONES','CONTROLADOS','Controlados','Recetas y balances.','/controlados','ShieldAlert',90),
('RECALL','OPERACIONES','RECALL','Recall','Alertas y retiros.','/recall','Siren',100),
('FARMACOVIGILANCIA','OPERACIONES','FARMACOVIGILANCIA','Farmacovigilancia','Reportes de seguridad.','/farmacovigilancia','HeartPulse',110),
('ERP','OPERACIONES','ERP','ERP financiero','Posting y finanzas.','/erp','Landmark',120),
('REPORTES','OPERACIONES','REPORTES','Reportes','Observatorio y BI.','/reportes','ChartNoAxesCombined',130),
('SEGURIDAD','ADMINISTRACION','SEGURIDAD','Seguridad','Usuarios, roles y permisos.','/seguridad','ShieldCheck',10),
('ORGANIZACION','ADMINISTRACION','ORGANIZACION','Organización','Empresas y establecimientos.','/organizacion','Building2',20)
)
INSERT INTO sch_app.menu_navegacion(modulo_id,menu_padre_id,codigo,aplicacion,etiqueta,descripcion,ruta,icono,tipo,modo_autorizacion,orden)
SELECT m.id,p.id,i.codigo,'ERP_WEB',i.etiqueta,i.descripcion,i.ruta,i.icono,'ITEM',CASE WHEN i.modulo_codigo IS NULL THEN 'AUTENTICADO' ELSE 'TODOS' END,i.orden
FROM items i
JOIN sch_app.menu_navegacion p ON p.aplicacion='ERP_WEB' AND p.codigo=i.padre
LEFT JOIN sch_seguridad.modulo_sistema m ON m.codigo=i.modulo_codigo
ON CONFLICT (aplicacion,codigo) DO UPDATE SET modulo_id=EXCLUDED.modulo_id,menu_padre_id=EXCLUDED.menu_padre_id,etiqueta=EXCLUDED.etiqueta,descripcion=EXCLUDED.descripcion,ruta=EXCLUDED.ruta,icono=EXCLUDED.icono,orden=EXCLUDED.orden,estado='ACTIVO',es_visible=TRUE;

INSERT INTO sch_app.menu_navegacion_permiso(menu_id,permiso_id)
SELECT mn.id,p.id
FROM sch_app.menu_navegacion mn
JOIN sch_seguridad.modulo_sistema m ON m.id=mn.modulo_id
JOIN sch_seguridad.permiso p ON p.modulo_id=m.id AND p.codigo=m.codigo || ':MODULO:VER'
WHERE mn.aplicacion='ERP_WEB' AND mn.modulo_id IS NOT NULL
ON CONFLICT (menu_id,permiso_id) DO UPDATE SET estado='ACTIVO';

-- =============================================================
-- V017__notificaciones_feature_flags.sql
-- =============================================================
-- Capacidades transversales: notificaciones, feature flags y versiones de aplicaciones.

CREATE TABLE sch_app.plantilla_notificacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    canal VARCHAR(40) NOT NULL,
    asunto VARCHAR(250),
    cuerpo TEXT NOT NULL,
    variables JSONB NOT NULL DEFAULT '[]'::jsonb,
    contiene_datos_sensibles BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_plantilla_notificacion PRIMARY KEY (id),
    CONSTRAINT uk_app_plantilla_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_app_plantilla_codigo UNIQUE NULLS NOT DISTINCT (tenant_id, codigo),
    CONSTRAINT fk_app_plantilla_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT ck_app_plantilla_canal CHECK (canal IN ('EMAIL','SMS','PUSH','WHATSAPP','IN_APP','WEBHOOK')),
    CONSTRAINT ck_app_plantilla_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_app.notificacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    plantilla_id BIGINT,
    destinatario_tipo VARCHAR(30) NOT NULL,
    destinatario_ref UUID,
    canal VARCHAR(40) NOT NULL,
    destinatario VARCHAR(320),
    asunto VARCHAR(250),
    mensaje_sanitizado TEXT,
    payload_cifrado BYTEA,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada TIMESTAMPTZ,
    fecha_envio TIMESTAMPTZ,
    provider_message_id VARCHAR(200),
    numero_intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(1500),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_notificacion PRIMARY KEY (id),
    CONSTRAINT uk_app_notificacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_app_notificacion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_app_notificacion_plantilla FOREIGN KEY (plantilla_id) REFERENCES sch_app.plantilla_notificacion(id),
    CONSTRAINT ck_app_notificacion_canal CHECK (canal IN ('EMAIL','SMS','PUSH','WHATSAPP','IN_APP','WEBHOOK')),
    CONSTRAINT ck_app_notificacion_estado CHECK (estado IN ('PENDIENTE','EN_PROCESO','ENVIADA','ENTREGADA','LEIDA','ERROR_REINTENTABLE','ERROR_DEFINITIVO','CANCELADA'))
);

CREATE TABLE sch_app.feature_flag (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    descripcion VARCHAR(500),
    habilitado_default BOOLEAN NOT NULL DEFAULT FALSE,
    aplicacion VARCHAR(40),
    reglas JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_app_feature_flag PRIMARY KEY (id),
    CONSTRAINT uk_app_feature_flag_codigo UNIQUE (codigo),
    CONSTRAINT ck_app_feature_flag_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE sch_app.app_version (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    aplicacion VARCHAR(40) NOT NULL,
    plataforma VARCHAR(30) NOT NULL,
    version VARCHAR(50) NOT NULL,
    build_number VARCHAR(50),
    minima_compatible VARCHAR(50),
    es_obligatoria BOOLEAN NOT NULL DEFAULT FALSE,
    notas TEXT,
    fecha_publicacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_app_version PRIMARY KEY (id),
    CONSTRAINT uk_app_version UNIQUE NULLS NOT DISTINCT (aplicacion, plataforma, version, build_number),
    CONSTRAINT ck_app_version_plataforma CHECK (plataforma IN ('WEB','PWA','ANDROID','IOS','WINDOWS','LINUX')),
    CONSTRAINT ck_app_version_estado CHECK (estado IN ('ACTIVO','INACTIVO','RETIRADO'))
);

CREATE INDEX ix_app_notificacion_pendiente ON sch_app.notificacion(tenant_id, estado, fecha_programada) WHERE estado IN ('PENDIENTE','ERROR_REINTENTABLE');

