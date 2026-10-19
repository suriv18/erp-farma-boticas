-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V008.

-- ============================================================================
-- 10. ESQUEMA: sch_dispensacion (Prescripción, Atención Clínica y Controlados)
-- ============================================================================

CREATE TABLE sch_dispensacion.prescripcion (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                       BIGINT NOT NULL,
    empresa_id                      BIGINT NOT NULL,
    establecimiento_id              BIGINT NOT NULL,
    tipo_receta                     VARCHAR(40) NOT NULL DEFAULT 'SIMPLE',
    numero_receta                   VARCHAR(120),
    fecha_emision                   DATE NOT NULL,
    fecha_vencimiento               DATE,
    paciente_tipo_documento         VARCHAR(2) DEFAULT '1',
    paciente_numero_documento       VARCHAR(15),
    paciente_nombre_completo        VARCHAR(300) NOT NULL,
    paciente_edad                   SMALLINT,
    paciente_diagnostico_cie10      VARCHAR(10),
    prescriptor_nombre              VARCHAR(300) NOT NULL,
    prescriptor_colegio             VARCHAR(10) NOT NULL DEFAULT 'CMP',
    prescriptor_colegiatura         VARCHAR(30) NOT NULL,
    prescriptor_especialidad        VARCHAR(150),
    establecimiento_salud_origen    VARCHAR(300),
    estado_validacion               VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    validado_por_profesional_id     BIGINT,
    validado_at                     TIMESTAMPTZ,
    motivo_invalidacion             VARCHAR(1000),
    receta_digital_uri              TEXT,
    datos_adicionales               JSONB NOT NULL DEFAULT '{}'::jsonb,
    es_activo                       CHAR(1) NOT NULL DEFAULT '1',
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(15),
    CONSTRAINT pk_prescripcion PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_prescripcion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_prescripcion_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_prescripcion_validador FOREIGN KEY (tenant_id, validado_por_profesional_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_prescripcion_tipo CHECK (tipo_receta IN ('SIMPLE', 'RETENIDA', 'ESPECIAL_ESTUPEFACIENTES', 'ELECTRONICA')),
    CONSTRAINT ck_prescripcion_colegio CHECK (prescriptor_colegio IN ('CMP', 'COP', 'CMVP', 'OTRO')),
    CONSTRAINT ck_prescripcion_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_emision),
    CONSTRAINT ck_prescripcion_estado CHECK (estado_validacion IN ('PENDIENTE', 'VALIDA', 'INVALIDA', 'VENCIDA', 'ATENDIDA_TOTAL', 'ATENDIDA_PARCIAL', 'ANULADA')),
    CONSTRAINT ck_prescripcion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_prescripcion_busqueda ON sch_dispensacion.prescripcion(tenant_id, establecimiento_id, estado_validacion, fecha_emision DESC) WHERE es_activo = '1';
CREATE INDEX ix_prescripcion_paciente ON sch_dispensacion.prescripcion(tenant_id, paciente_numero_documento) WHERE es_activo = '1' AND paciente_numero_documento IS NOT NULL;

CREATE TABLE sch_dispensacion.prescripcion_linea (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    prescripcion_id             BIGINT NOT NULL,
    numero_linea                INTEGER NOT NULL,
    producto_regulado_id        BIGINT,
    principio_activo_dci        VARCHAR(300),
    descripcion_prescrita       VARCHAR(500) NOT NULL,
    cantidad_prescrita          NUMERIC(18, 4) NOT NULL,
    unidad_medida_codigo        VARCHAR(30) NOT NULL,
    dosis                       VARCHAR(200),
    frecuencia                  VARCHAR(200),
    duracion                    VARCHAR(200),
    via_administracion_codigo   VARCHAR(30),
    indicaciones                VARCHAR(1500),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_prescripcion_linea PRIMARY KEY (id),
    CONSTRAINT uk_prescripcion_linea UNIQUE (prescripcion_id, numero_linea),
    CONSTRAINT uk_prescripcion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_prescripcion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, prescripcion_id, id),
    CONSTRAINT fk_pres_linea_prescripcion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_dispensacion.prescripcion(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_pres_linea_producto_regulado FOREIGN KEY (producto_regulado_id) REFERENCES sch_catalogo.producto_regulado(id),
    CONSTRAINT fk_pres_linea_unidad FOREIGN KEY (unidad_medida_codigo) REFERENCES sch_catalogo.unidad_medida(codigo),
    CONSTRAINT fk_pres_linea_via FOREIGN KEY (via_administracion_codigo) REFERENCES sch_catalogo.via_administracion(codigo),
    CONSTRAINT ck_pres_linea_cantidad CHECK (cantidad_prescrita > 0),
    CONSTRAINT ck_pres_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE TABLE sch_dispensacion.dispensacion (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico                UUID NOT NULL DEFAULT uuidv7(),
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    prescripcion_id             BIGINT,
    profesional_id              BIGINT NOT NULL,
    fecha_dispensacion          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADA',
    decision_farmaceutica       VARCHAR(60) NOT NULL DEFAULT 'CONFORME',
    informacion_brindada        TEXT,
    advertencias_brindadas      TEXT,
    evidencia_entrega           JSONB NOT NULL DEFAULT '{}'::jsonb,
    observacion                 VARCHAR(1500),
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_dispensacion PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_dispensacion_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_establecimiento FOREIGN KEY (tenant_id, empresa_id, establecimiento_id) REFERENCES sch_organizacion.establecimiento_farmaceutico(tenant_id, empresa_id, id),
    CONSTRAINT fk_dispensacion_prescripcion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, prescripcion_id) REFERENCES sch_dispensacion.prescripcion(tenant_id, empresa_id, establecimiento_id, id),
    CONSTRAINT fk_dispensacion_profesional FOREIGN KEY (tenant_id, profesional_id) REFERENCES sch_organizacion.profesional_farmaceutico(tenant_id, id),
    CONSTRAINT ck_dispensacion_estado CHECK (estado IN ('EN_EVALUACION', 'AUTORIZADA', 'CONFIRMADA', 'RECHAZADA', 'ANULADA')),
    CONSTRAINT ck_dispensacion_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_dispensacion_fecha ON sch_dispensacion.dispensacion(tenant_id, establecimiento_id, fecha_dispensacion DESC) WHERE es_activo = '1';
CREATE INDEX ix_dispensacion_profesional ON sch_dispensacion.dispensacion(tenant_id, profesional_id, fecha_dispensacion DESC) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.dispensacion_linea (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id                   BIGINT NOT NULL,
    empresa_id                  BIGINT NOT NULL,
    establecimiento_id          BIGINT NOT NULL,
    dispensacion_id             BIGINT NOT NULL,
    numero_linea                INTEGER NOT NULL,
    prescripcion_linea_id       BIGINT,
    sku_id                      BIGINT NOT NULL,
    cantidad_autorizada         NUMERIC(18, 4) NOT NULL,
    cantidad_entregada          NUMERIC(18, 4) NOT NULL DEFAULT 0,
    decision_linea              VARCHAR(30) NOT NULL DEFAULT 'AUTORIZADA',
    motivo_sustitucion          VARCHAR(1000),
    regulatory_snapshot         JSONB NOT NULL DEFAULT '{}'::jsonb,
    es_activo                   CHAR(1) NOT NULL DEFAULT '1',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(15),
    CONSTRAINT pk_dispensacion_linea PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea UNIQUE (dispensacion_id, numero_linea),
    CONSTRAINT uk_dispensacion_linea_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_dispensacion_linea_scope_id UNIQUE (tenant_id, empresa_id, establecimiento_id, dispensacion_id, id),
    CONSTRAINT fk_disp_linea_dispensacion FOREIGN KEY (tenant_id, empresa_id, establecimiento_id, dispensacion_id) REFERENCES sch_dispensacion.dispensacion(tenant_id, empresa_id, establecimiento_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_disp_linea_prescripcion_linea FOREIGN KEY (tenant_id, prescripcion_linea_id) REFERENCES sch_dispensacion.prescripcion_linea(tenant_id, id),
    CONSTRAINT fk_disp_linea_sku FOREIGN KEY (tenant_id, sku_id) REFERENCES sch_catalogo.sku_comercial(tenant_id, id),
    CONSTRAINT ck_disp_linea_cantidades CHECK (cantidad_autorizada > 0 AND cantidad_entregada >= 0 AND cantidad_entregada <= cantidad_autorizada),
    CONSTRAINT ck_disp_linea_decision CHECK (decision_linea IN ('AUTORIZADA', 'SUSTITUIDA_GENERICO', 'RECHAZADA', 'PARCIAL')),
    CONSTRAINT ck_disp_linea_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE INDEX ix_disp_linea_sku ON sch_dispensacion.dispensacion_linea(tenant_id, sku_id) WHERE es_activo = '1';

CREATE TABLE sch_dispensacion.dispensacion_linea_lote (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    tenant_id               BIGINT NOT NULL,
    dispensacion_linea_id   BIGINT NOT NULL,
    lote_id                 BIGINT NOT NULL,
    cantidad                NUMERIC(18, 4) NOT NULL,
    CONSTRAINT pk_dispensacion_linea_lote PRIMARY KEY (id),
    CONSTRAINT uk_dispensacion_linea_lote UNIQUE (dispensacion_linea_id, lote_id),
    CONSTRAINT fk_disp_lote_linea FOREIGN KEY (tenant_id, dispensacion_linea_id) REFERENCES sch_dispensacion.dispensacion_linea(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_disp_lote_lote FOREIGN KEY (tenant_id, lote_id) REFERENCES sch_inventario.lote(tenant_id, id),
    CONSTRAINT ck_disp_lote_cantidad CHECK (cantidad > 0)
);

CREATE INDEX ix_disp_lote_lote ON sch_dispensacion.dispensacion_linea_lote(tenant_id, lote_id);

ALTER TABLE sch_venta.venta_linea
    ADD CONSTRAINT fk_venta_linea_dispensacion_linea
    FOREIGN KEY (tenant_id, dispensacion_linea_id)
    REFERENCES sch_dispensacion.dispensacion_linea(tenant_id, id);
