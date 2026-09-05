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
