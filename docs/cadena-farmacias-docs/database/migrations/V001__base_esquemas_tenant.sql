-- PostgreSQL 18.x
-- Cadena de Farmacias - Baseline consolidado v0.3
-- Convenciones: BIGINT IDENTITY interno + UUIDv7 público; timestamps con zona; multi-tenant explícito.

CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE SCHEMA IF NOT EXISTS sch_farmacia;
CREATE SCHEMA IF NOT EXISTS sch_seguridad;
CREATE SCHEMA IF NOT EXISTS sch_auditoria;
CREATE SCHEMA IF NOT EXISTS sch_integracion;
CREATE SCHEMA IF NOT EXISTS sch_app;

CREATE TABLE sch_farmacia.tenant (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    codigo              VARCHAR(30) NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    slug                VARCHAR(80),
    zona_horaria        VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    locale              VARCHAR(20) NOT NULL DEFAULT 'es-PE',
    moneda_default      CHAR(3) NOT NULL DEFAULT 'PEN',
    configuracion       JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT pk_tenant PRIMARY KEY (id),
    CONSTRAINT uk_tenant_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_tenant_codigo UNIQUE (codigo),
    CONSTRAINT uk_tenant_slug UNIQUE (slug),
    CONSTRAINT ck_tenant_estado CHECK (estado IN ('ACTIVO','INACTIVO','SUSPENDIDO'))
);
