-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V004.

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
