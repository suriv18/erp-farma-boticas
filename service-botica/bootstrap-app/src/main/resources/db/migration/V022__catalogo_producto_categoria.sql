CREATE SCHEMA IF NOT EXISTS sch_catalogo;

CREATE TABLE sch_catalogo.categoria (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id       BIGINT NOT NULL REFERENCES sch_farmacia.tenant(id),
    nombre          VARCHAR(100) NOT NULL,
    descripcion     VARCHAR(500),
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT pk_cat_categoria PRIMARY KEY (id),
    CONSTRAINT uk_cat_categoria_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cat_categoria_tenant_nombre UNIQUE (tenant_id, nombre),
    CONSTRAINT ck_cat_categoria_estado CHECK (estado IN ('ACTIVA','INACTIVA'))
);

CREATE TABLE sch_catalogo.producto (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL REFERENCES sch_farmacia.tenant(id),
    categoria_id            BIGINT NOT NULL,
    nombre                  VARCHAR(200) NOT NULL,
    tipo                    VARCHAR(30) NOT NULL,
    laboratorio             VARCHAR(150),
    unidad_medida           VARCHAR(30) NOT NULL,
    presentacion            VARCHAR(150),
    unidades_por_paquete    INTEGER NOT NULL DEFAULT 1,
    codigo_barras           VARCHAR(40),
    precio_venta            NUMERIC(12,2) NOT NULL,
    condicion_venta         VARCHAR(20),
    es_generico             BOOLEAN NOT NULL DEFAULT FALSE,
    es_generico_esencial    BOOLEAN NOT NULL DEFAULT FALSE,
    grupo_terapeutico       VARCHAR(150),
    codigo_digemid          VARCHAR(40),
    principio_activo        VARCHAR(200),
    concentracion           VARCHAR(60),
    requiere_lote           BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_vencimiento    BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ,
    CONSTRAINT pk_cat_producto PRIMARY KEY (id),
    CONSTRAINT uk_cat_producto_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_cat_producto_categoria FOREIGN KEY (categoria_id) REFERENCES sch_catalogo.categoria(id),
    CONSTRAINT uk_cat_producto_tenant_codigo_barras UNIQUE (tenant_id, codigo_barras),
    CONSTRAINT ck_cat_producto_tipo CHECK (tipo IN (
        'MEDICAMENTO','DISPOSITIVO_MEDICO','PRODUCTO_SANITARIO','SUPLEMENTO_ALIMENTO','ARTICULO_NO_SANITARIO')),
    CONSTRAINT ck_cat_producto_condicion_venta CHECK (
        condicion_venta IS NULL OR condicion_venta IN ('SIN_RECETA','CON_RECETA','RECETA_RETENIDA')),
    CONSTRAINT ck_cat_producto_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT ck_cat_producto_precio_venta CHECK (precio_venta > 0),
    CONSTRAINT ck_cat_producto_medicamento_requiere_datos CHECK (
        tipo <> 'MEDICAMENTO' OR (principio_activo IS NOT NULL AND condicion_venta IS NOT NULL))
);

CREATE INDEX ix_cat_producto_tenant_categoria ON sch_catalogo.producto(tenant_id, categoria_id);
CREATE INDEX ix_cat_producto_tenant_estado ON sch_catalogo.producto(tenant_id, estado);

INSERT INTO sch_seguridad.modulo_sistema (codigo, nombre, orden)
VALUES ('CATALOGO', 'Catálogo', 20)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.categorias.gestionar', 'CATEGORIA', 'GESTIONAR',
       'Gestionar categorías', 'Crear, actualizar y cambiar el estado de categorías de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.categorias.consultar', 'CATEGORIA', 'CONSULTAR',
       'Consultar categorías', 'Listar categorías de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.productos.gestionar', 'PRODUCTO', 'GESTIONAR',
       'Gestionar productos', 'Crear, actualizar y cambiar el estado de productos de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.productos.consultar', 'PRODUCTO', 'CONSULTAR',
       'Consultar productos', 'Consultar y listar productos de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';
