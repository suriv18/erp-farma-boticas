-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V003.

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
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_condicion_venta PRIMARY KEY (codigo),
    CONSTRAINT ck_condicion_venta_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_condicion_venta_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.forma_farmaceutica (
    codigo          VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(200) NOT NULL,
    fuente          VARCHAR(300) DEFAULT 'DIGEMID',
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_forma_farmaceutica PRIMARY KEY (codigo),
    CONSTRAINT ck_forma_farmaceutica_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_forma_farmaceutica_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.via_administracion (
    codigo          VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(200) NOT NULL,
    fuente          VARCHAR(300) DEFAULT 'DIGEMID',
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_via_administracion PRIMARY KEY (codigo),
    CONSTRAINT ck_via_administracion_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_via_administracion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.unidad_medida (
    codigo          VARCHAR(30) NOT NULL,
    denominacion    VARCHAR(150) NOT NULL,
    simbolo         VARCHAR(30),
    permite_decimal BOOLEAN NOT NULL DEFAULT FALSE,
    fuente          VARCHAR(300) DEFAULT 'SUNAT_CAT03',
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_unidad_medida PRIMARY KEY (codigo),
    CONSTRAINT ck_unidad_medida_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_unidad_medida_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.clasificacion_controlada (
    codigo                   VARCHAR(40) NOT NULL,
    denominacion             VARCHAR(200) NOT NULL,
    norma_fuente             VARCHAR(300) DEFAULT 'D.S. 023-2001-SA',
    requiere_receta_especial BOOLEAN NOT NULL DEFAULT FALSE,
    retiene_receta           BOOLEAN NOT NULL DEFAULT FALSE,
    vigencia_receta_dias     INTEGER,
    estado                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo                CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_clasificacion_controlada PRIMARY KEY (codigo),
    CONSTRAINT ck_clasif_controlada_dias CHECK (vigencia_receta_dias IS NULL OR vigencia_receta_dias > 0),
    CONSTRAINT ck_clasif_controlada_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_clasif_controlada_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_catalogo.principio_activo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    codigo_fuente       VARCHAR(80),
    denominacion        VARCHAR(300) NOT NULL,
    nombre_normalizado  VARCHAR(300),
    fuente              VARCHAR(300) DEFAULT 'DIGEMID',
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    CONSTRAINT pk_principio_activo PRIMARY KEY (id),
    CONSTRAINT uk_principio_activo_uuid UNIQUE (uuid_publico),
    CONSTRAINT ck_principio_activo_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
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
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
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
    CONSTRAINT ck_categoria_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
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
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_marca PRIMARY KEY (id),
    CONSTRAINT uk_marca_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_marca_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_marca_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_marca_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
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
    CONSTRAINT ck_sku_estado CHECK (estado_comercial IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO', 'DESCONTINUADO')),
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
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo       CHAR(1) NOT NULL DEFAULT '1',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(15),
    CONSTRAINT pk_sku_codigo_barra PRIMARY KEY (id),
    CONSTRAINT fk_sku_codigo_barra FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_sku_barra_fechas CHECK (vigente_hasta IS NULL OR vigente_desde IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_sku_barra_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_sku_barra_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_sku_codigo_barra ON sch_catalogo.sku_codigo_barra(tenant_id, codigo_barra) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_sku_codigo_principal ON sch_catalogo.sku_codigo_barra(tenant_id, sku_id) WHERE es_activo = '1' AND es_principal = TRUE;
