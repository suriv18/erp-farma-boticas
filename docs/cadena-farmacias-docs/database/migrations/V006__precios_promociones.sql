-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V006.

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
