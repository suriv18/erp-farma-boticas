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
