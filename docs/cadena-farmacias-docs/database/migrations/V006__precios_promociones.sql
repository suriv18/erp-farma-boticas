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
