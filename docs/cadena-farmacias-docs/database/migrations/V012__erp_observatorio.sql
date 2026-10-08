-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V012.

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
