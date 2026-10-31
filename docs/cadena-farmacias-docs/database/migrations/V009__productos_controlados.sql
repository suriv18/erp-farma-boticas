-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V009.

CREATE TABLE sch_dispensacion.receta_controlada (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    prescripcion_id                 BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    tipo_receta_especial            VARCHAR(50) NOT NULL,
    numero_receta                   VARCHAR(120) NOT NULL,
    fecha_expedicion                TIMESTAMPTZ NOT NULL,
    fecha_limite_at                 TIMESTAMPTZ NOT NULL,
    estado_validacion               VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    motivo_invalidacion             VARCHAR(1000),
    retenida                        BOOLEAN NOT NULL DEFAULT TRUE,
    retenida_at                     TIMESTAMPTZ,
    archivada_at                    TIMESTAMPTZ,
    conservar_hasta                 DATE,
    profesional_validador_id        BIGINT NOT NULL,
    evidencia_uri                   TEXT,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(15),
    CONSTRAINT pk_receta_controlada PRIMARY KEY (id),
    CONSTRAINT uk_receta_controlada_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_receta_controlada_pres UNIQUE (prescripcion_id),
    CONSTRAINT uk_receta_controlada_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_rec_ctrl_prescripcion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_dispensacion.prescripcion(tenant_id, empresa_id, establecimiento_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_rec_ctrl_clasificacion FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_catalogo.clasificacion_controlada(codigo),
    CONSTRAINT fk_rec_ctrl_profesional FOREIGN KEY (tenant_id, profesional_validador_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_rec_ctrl_fechas CHECK (fecha_limite_at >= fecha_expedicion),
    CONSTRAINT ck_rec_ctrl_estado CHECK (estado_validacion IN ('PENDIENTE', 'VALIDA', 'INVALIDA', 'VENCIDA', 'ADULTERADA', 'ATENDIDA', 'ANULADA')),
    CONSTRAINT ck_rec_ctrl_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_rec_ctrl_establecimiento ON sch_dispensacion.receta_controlada(tenant_id, establecimiento_id, estado_validacion) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.movimiento_controlado (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    sku_id                          BIGINT NOT NULL,
    lote_id                         BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    tipo_movimiento                 VARCHAR(30) NOT NULL,
    cantidad                        NUMERIC(18, 4) NOT NULL,
    saldo_anterior                  NUMERIC(18, 4) NOT NULL,
    saldo_posterior                 NUMERIC(18, 4) NOT NULL,
    documento_tipo                  VARCHAR(50) NOT NULL,
    documento_uuid                  UUID,
    receta_controlada_id            BIGINT,
    profesional_id                  BIGINT NOT NULL,
    fecha_movimiento                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion                     VARCHAR(1000),
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_movimiento_controlado PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_controlado_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_mov_ctrl_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT fk_mov_ctrl_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT fk_mov_ctrl_clasificacion FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_catalogo.clasificacion_controlada(codigo),
    CONSTRAINT fk_mov_ctrl_profesional FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT fk_mov_ctrl_receta FOREIGN KEY (receta_controlada_id) REFERENCES sch_dispensacion.receta_controlada(id),
    CONSTRAINT ck_mov_ctrl_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_mov_ctrl_saldos CHECK (saldo_anterior >= 0 AND saldo_posterior >= 0),
    CONSTRAINT ck_mov_ctrl_tipo CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO', 'DEVOLUCION', 'DISPOSICION'))
);

CREATE INDEX ix_mov_ctrl_libro ON sch_dispensacion.movimiento_controlado(tenant_id, establecimiento_id, sku_id, fecha_movimiento DESC);

CREATE TABLE sch_dispensacion.balance_controlado (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    clasificacion_controlada_codigo VARCHAR(40) NOT NULL,
    periodo_desde                   DATE NOT NULL,
    periodo_hasta                   DATE NOT NULL,
    fecha_limite_presentacion       DATE,
    estado                          VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    quimico_responsable_id          BIGINT NOT NULL,
    generado_at                     TIMESTAMPTZ,
    validado_at                     TIMESTAMPTZ,
    presentado_at                   TIMESTAMPTZ,
    identificador_presentacion      VARCHAR(150),
    evidencia_uri                   TEXT,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(15),
    CONSTRAINT pk_balance_controlado PRIMARY KEY (id),
    CONSTRAINT uk_balance_controlado_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_balance_controlado_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_bal_ctrl_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_bal_ctrl_clasificacion FOREIGN KEY (clasificacion_controlada_codigo) REFERENCES sch_catalogo.clasificacion_controlada(codigo),
    CONSTRAINT fk_bal_ctrl_quimico FOREIGN KEY (tenant_id, quimico_responsable_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_bal_ctrl_fechas CHECK (periodo_hasta >= periodo_desde),
    CONSTRAINT ck_bal_ctrl_estado CHECK (estado IN ('BORRADOR', 'GENERADO', 'VALIDADO', 'PRESENTADO', 'OBSERVADO', 'RECTIFICADO')),
    CONSTRAINT ck_bal_ctrl_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_balance_controlado_periodo ON sch_dispensacion.balance_controlado(tenant_id, establecimiento_id, clasificacion_controlada_codigo, periodo_desde, periodo_hasta) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.balance_controlado_linea (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    balance_id              BIGINT NOT NULL,
    sku_id                  BIGINT NOT NULL,
    existencia_inicial      NUMERIC(18, 4) NOT NULL DEFAULT 0,
    entradas                NUMERIC(18, 4) NOT NULL DEFAULT 0,
    salidas                 NUMERIC(18, 4) NOT NULL DEFAULT 0,
    ajustes_positivos       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    ajustes_negativos       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    existencia_final        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    CONSTRAINT pk_balance_controlado_linea PRIMARY KEY (id),
    CONSTRAINT uk_bal_ctrl_linea UNIQUE (balance_id, sku_id),
    CONSTRAINT fk_bal_ctrl_linea_balance FOREIGN KEY (tenant_id, balance_id) REFERENCES sch_dispensacion.balance_controlado(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_bal_ctrl_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT ck_bal_linea_cantidades CHECK (existencia_inicial >= 0 AND entradas >= 0 AND salidas >= 0 AND ajustes_positivos >= 0 AND ajustes_negativos >= 0 AND existencia_final >= 0),
    CONSTRAINT ck_bal_linea_cuadre CHECK (existencia_final = (existencia_inicial + entradas - salidas + ajustes_positivos - ajustes_negativos))
);

CREATE INDEX ix_bal_ctrl_linea_sku ON sch_dispensacion.balance_controlado_linea(tenant_id, sku_id);
