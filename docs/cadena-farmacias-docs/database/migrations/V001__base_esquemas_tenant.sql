-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V001.

-- ============================================================================
-- SCRIPT MAESTRO DDL: ERP CORE FARMACÉUTICO (PERÚ / DIGEMID / SUNAT)
-- MOTOR: PostgreSQL 18.x
-- INCLUYE: sch_catalogo.rubro_comercial + sku_comercial adaptado para Retail
-- CONVENCIONES:
--   - Claves primarias técnicas: BIGINT GENERATED ALWAYS AS IDENTITY
--   - Claves públicas de negocio: UUIDv7
--   - Auditoría: created_by VARCHAR(15) DEFAULT 'SYSTEM', updated_by VARCHAR(15)
--   - Borrado Lógico: es_activo CHAR(1) DEFAULT '1' CHECK (es_activo IN ('0', '1'))
--   - Aislamiento SaaS: Multi-tenant explícito (tenant_id) con claves compuestas
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. EXTENSIONES Y ESQUEMAS
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE SCHEMA IF NOT EXISTS sch_admin;
CREATE SCHEMA IF NOT EXISTS sch_seguridad;
CREATE SCHEMA IF NOT EXISTS sch_organizacion;
CREATE SCHEMA IF NOT EXISTS sch_catalogo;
CREATE SCHEMA IF NOT EXISTS sch_abastecimiento;
CREATE SCHEMA IF NOT EXISTS sch_inventario;
CREATE SCHEMA IF NOT EXISTS sch_precio;
CREATE SCHEMA IF NOT EXISTS sch_venta;
CREATE SCHEMA IF NOT EXISTS sch_dispensacion;
CREATE SCHEMA IF NOT EXISTS sch_facturacion;
CREATE SCHEMA IF NOT EXISTS sch_vigilancia;
CREATE SCHEMA IF NOT EXISTS sch_finanzas;
CREATE SCHEMA IF NOT EXISTS sch_auditoria;
CREATE SCHEMA IF NOT EXISTS sch_integracion;
CREATE SCHEMA IF NOT EXISTS sch_app;

-- ============================================================================
-- 1. ESQUEMA: sch_admin (Gobernanza SaaS Multi-Tenant)
-- ============================================================================

CREATE TABLE sch_admin.tenant (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    codigo              VARCHAR(30) NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    slug                VARCHAR(80) NOT NULL,
    tipo_suscripcion    VARCHAR(30) NOT NULL DEFAULT 'PRODUCCION',
    timezone            VARCHAR(80) NOT NULL DEFAULT 'America/Lima',
    locale              VARCHAR(20) NOT NULL DEFAULT 'es-PE',
    default_currency    CHAR(3) NOT NULL DEFAULT 'PEN',
    settings            JSONB NOT NULL DEFAULT '{}'::jsonb,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_tenant PRIMARY KEY (id),
    CONSTRAINT uk_tenant_uuid UNIQUE (uuid_publico),
    CONSTRAINT ck_tenant_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'BLOQUEADO')),
    CONSTRAINT ck_tenant_es_activo CHECK (es_activo IN ('0', '1')),
    CONSTRAINT ck_tenant_currency CHECK (default_currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX uk_tenant_codigo ON sch_admin.tenant(codigo) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_tenant_slug ON sch_admin.tenant(slug) WHERE es_activo = '1';
CREATE INDEX ix_tenant_activo ON sch_admin.tenant(id, estado) WHERE es_activo = '1';
