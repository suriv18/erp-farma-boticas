-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V005.

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
