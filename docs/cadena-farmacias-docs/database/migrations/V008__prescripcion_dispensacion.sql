-- Prescripción y dispensación farmacéutica.
-- El paciente puede no ser cliente comercial; se preserva snapshot mínimo de la prescripción.

CREATE TABLE sch_farmacia.prescripcion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    tipo_documento_prescripcion VARCHAR(40) NOT NULL,
    numero_documento VARCHAR(120),
    fecha_emision TIMESTAMPTZ NOT NULL,
    fecha_vencimiento TIMESTAMPTZ,
    paciente_ref UUID,
    paciente_tipo_documento VARCHAR(20),
    paciente_numero_documento VARCHAR(30),
    paciente_nombre_snapshot VARCHAR(300),
    prescriptor_nombre VARCHAR(300),
    prescriptor_tipo_identificador VARCHAR(30),
    prescriptor_identificador VARCHAR(100),
    prescriptor_especialidad VARCHAR(150),
    establecimiento_prescriptor VARCHAR(300),
    estado_validacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    validado_por_profesional_id BIGINT,
    validado_at TIMESTAMPTZ,
    motivo_invalidacion VARCHAR(1000),
    evidencia_uri TEXT,
    datos_minimos JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_prescripcion PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_prescripcion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_prescripcion_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_prescripcion_validador FOREIGN KEY (tenant_id, validado_por_profesional_id) REFERENCES sch_farmacia.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_prescripcion_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_emision),
    CONSTRAINT ck_prescripcion_estado CHECK (estado_validacion IN ('PENDIENTE','VALIDA','INVALIDA','VENCIDA','ANULADA','UTILIZADA_PARCIAL','UTILIZADA_TOTAL'))
);

CREATE TABLE sch_farmacia.prescripcion_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    prescripcion_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    producto_regulado_id BIGINT,
    descripcion_prescrita VARCHAR(500) NOT NULL,
    cantidad_prescrita NUMERIC(18,4),
    unidad_medida_codigo VARCHAR(30),
    dosis VARCHAR(200),
    frecuencia VARCHAR(200),
    duracion VARCHAR(200),
    via_administracion_codigo VARCHAR(30),
    indicaciones VARCHAR(1500),
    CONSTRAINT pk_prescripcion_linea PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_linea UNIQUE (prescripcion_id, numero_linea),
    CONSTRAINT uk_prescripcion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_prescripcion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, prescripcion_id, id),
    CONSTRAINT fk_prescripcion_linea_pres FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_farmacia.prescripcion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_prescripcion_linea_producto FOREIGN KEY (producto_regulado_id) REFERENCES sch_farmacia.producto_regulado(id),
    CONSTRAINT fk_prescripcion_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_farmacia.unidad_medida(codigo),
    CONSTRAINT fk_prescripcion_linea_via FOREIGN KEY (via_administracion_codigo) REFERENCES sch_farmacia.via_administracion(codigo),
    CONSTRAINT ck_prescripcion_linea_cantidad CHECK (cantidad_prescrita IS NULL OR cantidad_prescrita > 0)
);

CREATE TABLE sch_farmacia.dispensacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    prescripcion_id BIGINT,
    profesional_id BIGINT NOT NULL,
    fecha_dispensacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'EN_EVALUACION',
    decision_codigo VARCHAR(60),
    observacion VARCHAR(1500),
    informacion_brindada TEXT,
    advertencias_brindadas TEXT,
    evidencia_entrega JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_dispensacion PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_dispensacion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_est FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_farmacia.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_dispensacion_pres FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_farmacia.prescripcion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_prof FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_farmacia.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_dispensacion_estado CHECK (estado IN ('EN_EVALUACION','AUTORIZADA','RECHAZADA','PARCIAL','CONFIRMADA','ANULADA'))
);

CREATE TABLE sch_farmacia.dispensacion_linea (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    establecimiento_id BIGINT NOT NULL,
    dispensacion_id BIGINT NOT NULL,
    numero_linea INTEGER NOT NULL,
    prescripcion_linea_id BIGINT,
    sku_id BIGINT NOT NULL,
    cantidad_autorizada NUMERIC(18,4) NOT NULL,
    cantidad_entregada NUMERIC(18,4) NOT NULL DEFAULT 0,
    decision_linea VARCHAR(30) NOT NULL DEFAULT 'AUTORIZADA',
    motivo_sustitucion VARCHAR(1000),
    regulatory_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_dispensacion_linea PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea UNIQUE (dispensacion_id, numero_linea),
    CONSTRAINT uk_dispensacion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_dispensacion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, dispensacion_id, id),
    CONSTRAINT fk_dispensacion_linea_disp FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, dispensacion_id) REFERENCES sch_farmacia.dispensacion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_linea_pres_linea FOREIGN KEY (tenant_id, prescripcion_linea_id) REFERENCES sch_farmacia.prescripcion_linea(tenant_id, id),
    CONSTRAINT fk_dispensacion_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_farmacia.sku_comercial(tenant_id, id),
    CONSTRAINT ck_dispensacion_linea_cant CHECK (cantidad_autorizada > 0 AND cantidad_entregada >= 0 AND cantidad_entregada <= cantidad_autorizada),
    CONSTRAINT ck_dispensacion_linea_decision CHECK (decision_linea IN ('AUTORIZADA','RECHAZADA','SUSTITUIDA','PARCIAL'))
);

CREATE TABLE sch_farmacia.dispensacion_linea_lote (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id BIGINT NOT NULL,
    dispensacion_linea_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    cantidad_entregada NUMERIC(18,4) NOT NULL,
    CONSTRAINT pk_dispensacion_linea_lote PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea_lote UNIQUE (dispensacion_linea_id, lote_id),
    CONSTRAINT fk_disp_linea_lote_linea FOREIGN KEY (tenant_id, dispensacion_linea_id) REFERENCES sch_farmacia.dispensacion_linea(tenant_id, id),
    CONSTRAINT fk_disp_linea_lote_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_farmacia.lote(tenant_id, id),
    CONSTRAINT ck_disp_linea_lote_cant CHECK (cantidad_entregada > 0)
);

ALTER TABLE sch_farmacia.venta_linea
    ADD COLUMN dispensacion_linea_id BIGINT,
    ADD CONSTRAINT fk_venta_linea_dispensacion_linea FOREIGN KEY (tenant_id, dispensacion_linea_id) REFERENCES sch_farmacia.dispensacion_linea(tenant_id, id);

CREATE INDEX ix_prescripcion_estado ON sch_farmacia.prescripcion(tenant_id, establecimiento_id, estado_validacion, fecha_emision DESC);
CREATE INDEX ix_dispensacion_fecha ON sch_farmacia.dispensacion(tenant_id, establecimiento_id, fecha_dispensacion DESC);
