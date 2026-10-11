-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V011.

-- ============================================================================
-- 12. ESQUEMA: sch_vigilancia (Alertas Recall, Cuarentena y RAMs)
-- ============================================================================

CREATE TABLE sch_vigilancia.caso_recall (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    tenant_id           BIGINT NOT NULL,
    codigo_caso         VARCHAR(80) NOT NULL,
    tipo_alerta         VARCHAR(40) NOT NULL DEFAULT 'RECALL',
    autoridad_fuente    VARCHAR(150) NOT NULL DEFAULT 'DIGEMID',
    fuente              VARCHAR(300) NOT NULL,
    referencia_externa  VARCHAR(250),
    fecha_publicacion   TIMESTAMPTZ,
    fecha_alerta        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo              VARCHAR(2000) NOT NULL,
    nivel_riesgo        VARCHAR(30) NOT NULL DEFAULT 'CLASE_II',
    instruccion_oficial TEXT NOT NULL,
    estado              VARCHAR(30) NOT NULL DEFAULT 'ABIERTO',
    evidencia_uri       TEXT,
    closed_at           TIMESTAMPTZ,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_caso_recall PRIMARY KEY (id),
    CONSTRAINT uk_caso_recall_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_caso_recall_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_caso_recall_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT ck_caso_recall_riesgo CHECK (nivel_riesgo IN ('CLASE_I', 'CLASE_II', 'CLASE_III', 'INFORMATIVO')),
    CONSTRAINT ck_caso_recall_estado CHECK (estado IN ('ABIERTO', 'EN_EJECUCION', 'CONCILIACION', 'CERRADO', 'CANCELADO')),
    CONSTRAINT ck_caso_recall_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_caso_recall_codigo ON sch_vigilancia.caso_recall(tenant_id, codigo_caso) WHERE es_activo = '1';
CREATE INDEX ix_recall_estado ON sch_vigilancia.caso_recall(tenant_id, estado, fecha_alerta DESC) WHERE es_activo = '1';

CREATE TABLE sch_vigilancia.recall_producto_lote (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    caso_recall_id              BIGINT NOT NULL,
    producto_regulado_id        BIGINT,
    sku_id                      BIGINT,
    lote_id                     BIGINT,
    registro_sanitario_snapshot VARCHAR(120),
    denominacion_snapshot       VARCHAR(500) NOT NULL,
    numero_lote_snapshot        VARCHAR(120) NOT NULL,
    fecha_vencimiento_snapshot  DATE,
    alcance                     VARCHAR(1000),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_recall_producto_lote PRIMARY KEY (id),
    CONSTRAINT fk_rec_prod_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_vigilancia.caso_recall(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_prod_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_rec_prod_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_rec_prod_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_rec_prod_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_recall_producto_lote_natural ON sch_vigilancia.recall_producto_lote(
    caso_recall_id, (COALESCE(producto_regulado_id, 0)), (COALESCE(sku_id, 0)), (COALESCE(lote_id, 0)), numero_lote_snapshot
) WHERE es_activo = '1';

CREATE TABLE sch_vigilancia.recall_establecimiento (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    empresa_id              BIGINT NOT NULL,
    caso_recall_id          BIGINT NOT NULL,
    establecimiento_id      BIGINT NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    cantidad_identificada   NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_inmovilizada   NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_retirada       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cantidad_dispuesta      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    confirmado_at           TIMESTAMPTZ,
    confirmado_por          VARCHAR(15),
    observacion             VARCHAR(1000),
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(15),
    CONSTRAINT pk_recall_establecimiento PRIMARY KEY (id),
    CONSTRAINT uk_recall_establecimiento UNIQUE (caso_recall_id, establecimiento_id),
    CONSTRAINT fk_rec_est_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_vigilancia.caso_recall(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_est_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT ck_recall_est_cantidades CHECK (cantidad_identificada >= 0 AND cantidad_inmovilizada >= 0 AND cantidad_retirada >= 0 AND cantidad_dispuesta >= 0),
    CONSTRAINT ck_recall_est_estado CHECK (estado IN ('PENDIENTE', 'INMOVILIZADO', 'EN_RETIRO', 'CONCILIADO', 'CERRADO')),
    CONSTRAINT ck_recall_est_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_vigilancia.recall_accion (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id       BIGINT NOT NULL,
    caso_recall_id  BIGINT NOT NULL,
    tipo_accion     VARCHAR(40) NOT NULL,
    descripcion     VARCHAR(1500) NOT NULL,
    actor           VARCHAR(15) NOT NULL,
    ejecutado_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evidencia_uri   TEXT,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recall_accion PRIMARY KEY (id),
    CONSTRAINT fk_rec_accion_caso FOREIGN KEY (tenant_id, caso_recall_id) REFERENCES sch_vigilancia.caso_recall(tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE sch_vigilancia.reporte_seguridad (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT,
    establecimiento_id          BIGINT,
    tipo_reporte                VARCHAR(30) NOT NULL,
    origen_reporte              VARCHAR(30) NOT NULL,
    venta_id                    BIGINT,
    lote_id                     BIGINT,
    paciente_cliente_id         BIGINT,
    profesional_notificador_id  BIGINT,
    descripcion_evento          TEXT NOT NULL,
    gravedad_codigo             VARCHAR(50) NOT NULL DEFAULT 'MODERADA',
    desenlace_codigo            VARCHAR(50) NOT NULL DEFAULT 'RECUPERADO',
    ocurrido_at                 TIMESTAMPTZ,
    conocido_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    datos_contexto              JSONB NOT NULL DEFAULT '{}'::jsonb,
    datos_sensibles_cifrado     BYTEA,
    notificacion_oficial_at     TIMESTAMPTZ,
    notificacion_identificador  VARCHAR(150),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_reporte_seguridad PRIMARY KEY (id),
    CONSTRAINT uk_reporte_seguridad_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_reporte_seguridad_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_rep_seg_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_rep_seg_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_rep_seg_venta FOREIGN KEY (tenant_id, venta_id) REFERENCES sch_venta.venta(tenant_id, id),
    CONSTRAINT fk_rep_seg_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT fk_rep_seg_cliente FOREIGN KEY (tenant_id, paciente_cliente_id) REFERENCES sch_venta.cliente(tenant_id, id),
    CONSTRAINT fk_rep_seg_profesional FOREIGN KEY (tenant_id, profesional_notificador_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_rep_seg_tipo CHECK (tipo_reporte IN ('FARMACOVIGILANCIA', 'TECNOVIGILANCIA')),
    CONSTRAINT ck_rep_seg_origen CHECK (origen_reporte IN ('PACIENTE', 'MEDICO', 'QUIMICO_FARMACEUTICO', 'INTERNO', 'OTRO')),
    CONSTRAINT ck_rep_seg_gravedad CHECK (gravedad_codigo IN ('LEVE', 'MODERADA', 'GRAVE_HOSPITALIZACION', 'GRAVE_INCAPACIDAD', 'GRAVE_MUERTE')),
    CONSTRAINT ck_rep_seg_estado CHECK (estado IN ('REGISTRADO', 'EN_EVALUACION', 'NOTIFICADO_DIGEMID', 'EN_SEGUIMIENTO', 'CERRADO')),
    CONSTRAINT ck_rep_seg_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_rep_seg_estado ON sch_vigilancia.reporte_seguridad(tenant_id, estado, conocido_at DESC) WHERE es_activo = '1';
CREATE INDEX ix_rep_seg_lote ON sch_vigilancia.reporte_seguridad(tenant_id, lote_id) WHERE es_activo = '1' AND lote_id IS NOT NULL;

CREATE TABLE sch_vigilancia.reporte_seguridad_producto (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    reporte_id              BIGINT NOT NULL,
    producto_regulado_id    BIGINT,
    sku_id                  BIGINT,
    lote_id                 BIGINT,
    sospechoso              BOOLEAN NOT NULL DEFAULT TRUE,
    descripcion_producto    VARCHAR(500) NOT NULL,
    dosis_exposicion        VARCHAR(300),
    fecha_inicio_uso        TIMESTAMPTZ,
    fecha_fin_uso           TIMESTAMPTZ,
    es_activo               CHAR(1) NOT NULL DEFAULT '1',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_reporte_seguridad_producto PRIMARY KEY (id),
    CONSTRAINT fk_rep_prod_reporte FOREIGN KEY (tenant_id, reporte_id) REFERENCES sch_vigilancia.reporte_seguridad(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_rep_prod_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_rep_prod_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_rep_prod_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_rep_prod_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_vigilancia.reporte_seguridad_seguimiento (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id           BIGINT NOT NULL,
    reporte_id          BIGINT NOT NULL,
    fecha               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_seguimiento    VARCHAR(50) NOT NULL,
    descripcion         TEXT NOT NULL,
    actor               VARCHAR(15) NOT NULL,
    evidencia_uri       TEXT,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_reporte_seguridad_seguimiento PRIMARY KEY (id),
    CONSTRAINT fk_rep_seg_reporte FOREIGN KEY (tenant_id, reporte_id) REFERENCES sch_vigilancia.reporte_seguridad(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_rep_seg_es_activo CHECK (es_activo IN ('0', '1'))
);
