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
