-- ============================================================================
-- V1__init_erp_boticas_postgres_revisado.sql
-- Modelo relacional PostgreSQL para ERP de cadena de boticas en Perú
-- Incluye: Core botica, ERP, CRM, CMR, app web, app móvil, RBAC, trabajadores y auditoría básica
-- Motor recomendado: PostgreSQL 16+
-- Autor: ChatGPT
-- Fecha: 2026-05-27
-- ============================================================================

BEGIN;

-- ==========================================================================
-- 1. EXTENSIONES
-- ==========================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

-- ==========================================================================
-- 2. ESQUEMAS
-- ==========================================================================
CREATE SCHEMA IF NOT EXISTS organizacion;
CREATE SCHEMA IF NOT EXISTS security;
CREATE SCHEMA IF NOT EXISTS auditoria;
CREATE SCHEMA IF NOT EXISTS catalogo;
CREATE SCHEMA IF NOT EXISTS farmacia;
CREATE SCHEMA IF NOT EXISTS proveedores;
CREATE SCHEMA IF NOT EXISTS compras;
CREATE SCHEMA IF NOT EXISTS inventario;
CREATE SCHEMA IF NOT EXISTS ventas;
CREATE SCHEMA IF NOT EXISTS finanzas;
CREATE SCHEMA IF NOT EXISTS rrhh;
CREATE SCHEMA IF NOT EXISTS crm;
CREATE SCHEMA IF NOT EXISTS cmr;
CREATE SCHEMA IF NOT EXISTS erp;
CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS logistica;
CREATE SCHEMA IF NOT EXISTS pagos;
CREATE SCHEMA IF NOT EXISTS notificaciones;
CREATE SCHEMA IF NOT EXISTS integracion;

-- ==========================================================================
-- 3. FUNCIÓN GENERAL PARA UPDATED_AT
--    created_by y updated_by son BIGINT para evitar dependencia circular durante la instalación.
--    deleted_at y deleted_by SOLO aparecen en tablas maestras/configurables donde aplica baja lógica.
--    Tablas transaccionales, históricas, kardex, pagos, comprobantes, asistencias y movimientos no se eliminan: se anulan/cambian de estado.
-- ==========================================================================
CREATE OR REPLACE FUNCTION auditoria.fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================
-- 4. ORGANIZACIÓN: EMPRESA, SUCURSALES, ALMACENES Y CAJAS
-- ==========================================================================
CREATE TABLE IF NOT EXISTS organizacion.empresa (
    empresa_id              BIGSERIAL PRIMARY KEY,
    uuid_publico            UUID NOT NULL DEFAULT gen_random_uuid(),
    ruc                     VARCHAR(11) NOT NULL,
    razon_social            VARCHAR(250) NOT NULL,
    nombre_comercial        VARCHAR(200) NOT NULL,
    direccion_fiscal        VARCHAR(300),
    ubigeo                  VARCHAR(6),
    telefono                VARCHAR(30),
    email                   CITEXT,
    logo_url                TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_empresa_ruc UNIQUE (ruc),
    CONSTRAINT ck_empresa_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_empresa_ruc_len CHECK (char_length(ruc) = 11)

);

CREATE TABLE IF NOT EXISTS organizacion.sucursal (
    sucursal_id             BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(30) NOT NULL,
    nombre                  VARCHAR(180) NOT NULL,
    tipo_sucursal           VARCHAR(30) NOT NULL DEFAULT 'BOTICA',
    direccion               VARCHAR(300) NOT NULL,
    ubigeo                  VARCHAR(6),
    latitud                 NUMERIC(10,7),
    longitud                NUMERIC(10,7),
    telefono                VARCHAR(30),
    email                   CITEXT,
    es_principal            BOOLEAN NOT NULL DEFAULT FALSE,
    permite_venta_online    BOOLEAN NOT NULL DEFAULT TRUE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_sucursal_empresa_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_sucursal_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_sucursal_tipo CHECK (tipo_sucursal IN ('BOTICA','ALMACEN_CENTRAL','OFICINA','ECOMMERCE','OTRO'))

);

CREATE TABLE IF NOT EXISTS organizacion.almacen (
    almacen_id              BIGSERIAL PRIMARY KEY,
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    codigo                  VARCHAR(30) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    tipo_almacen            VARCHAR(40) NOT NULL DEFAULT 'VENTA',
    permite_lotes           BOOLEAN NOT NULL DEFAULT TRUE,
    permite_vencimiento     BOOLEAN NOT NULL DEFAULT TRUE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_almacen_sucursal_codigo UNIQUE (sucursal_id, codigo),
    CONSTRAINT ck_almacen_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_almacen_tipo CHECK (tipo_almacen IN ('VENTA','TRASTIENDA','CENTRAL','MERMA','CUARENTENA','DEVOLUCION','OTRO'))

);

CREATE TABLE IF NOT EXISTS organizacion.caja (
    caja_id                 BIGSERIAL PRIMARY KEY,
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    codigo                  VARCHAR(30) NOT NULL,
    nombre                  VARCHAR(120) NOT NULL,
    serie_impresora         VARCHAR(80),
    ip_equipo               VARCHAR(60),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_caja_sucursal_codigo UNIQUE (sucursal_id, codigo),
    CONSTRAINT ck_caja_estado CHECK (estado IN ('A','I'))

);

-- ==========================================================================
-- 5. SEGURIDAD RBAC
-- ==========================================================================
CREATE TABLE IF NOT EXISTS security.usuario (
    usuario_id                  BIGSERIAL PRIMARY KEY,
    uuid_publico                UUID NOT NULL DEFAULT gen_random_uuid(),
    tipo_documento              VARCHAR(10) NOT NULL DEFAULT 'DNI',
    numero_documento            VARCHAR(20) NOT NULL,
    nombres                     VARCHAR(120) NOT NULL,
    apellidos                   VARCHAR(150) NOT NULL,
    username                    CITEXT NOT NULL,
    email                       CITEXT,
    telefono                    VARCHAR(30),
    password_hash               VARCHAR(255) NOT NULL,
    requiere_cambio_password    BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_enabled                 BOOLEAN NOT NULL DEFAULT FALSE,
    estado                      CHAR(1) NOT NULL DEFAULT 'A',
    ultimo_login_at             TIMESTAMPTZ,
    intentos_fallidos           INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta             TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ,
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ,
    deleted_by                  BIGINT,
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT uq_usuario_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('A','I','B'))

);

CREATE TABLE IF NOT EXISTS security.modulo_sistema (
    modulo_id               BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(80) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    orden                   INTEGER NOT NULL DEFAULT 0,
    es_activo               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_modulo_codigo UNIQUE (codigo)

);

CREATE TABLE IF NOT EXISTS security.rol (
    rol_id                  BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(80) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    tipo_rol                VARCHAR(40) NOT NULL DEFAULT 'SUCURSAL',
    es_sistema              BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_rol_codigo UNIQUE (codigo),
    CONSTRAINT ck_rol_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_rol_tipo CHECK (tipo_rol IN ('GLOBAL','EMPRESA','SUCURSAL','ALMACEN','CAJA'))

);

CREATE TABLE IF NOT EXISTS security.permiso (
    permiso_id              BIGSERIAL PRIMARY KEY,
    modulo_id               BIGINT NOT NULL REFERENCES security.modulo_sistema(modulo_id),
    codigo                  VARCHAR(120) NOT NULL,
    recurso                 VARCHAR(100) NOT NULL,
    accion                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(180) NOT NULL,
    descripcion             TEXT,
    es_critico              BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_permiso_codigo UNIQUE (codigo),
    CONSTRAINT uq_permiso_modulo_recurso_accion UNIQUE (modulo_id, recurso, accion),
    CONSTRAINT ck_permiso_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_permiso_accion CHECK (accion IN ('VER','CREAR','EDITAR','ELIMINAR','ANULAR','APROBAR','EXPORTAR','IMPORTAR','IMPRIMIR','ADMINISTRAR','VALIDAR','DESPACHAR'))

);

CREATE TABLE IF NOT EXISTS security.rol_permiso (
    rol_permiso_id          BIGSERIAL PRIMARY KEY,
    rol_id                  BIGINT NOT NULL REFERENCES security.rol(rol_id),
    permiso_id              BIGINT NOT NULL REFERENCES security.permiso(permiso_id),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_rol_permiso UNIQUE (rol_id, permiso_id),
    CONSTRAINT ck_rol_permiso_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS security.usuario_rol_sucursal (
    usuario_rol_sucursal_id BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT NOT NULL REFERENCES security.usuario(usuario_id),
    rol_id                  BIGINT NOT NULL REFERENCES security.rol(rol_id),
    empresa_id              BIGINT REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id              BIGINT REFERENCES organizacion.almacen(almacen_id),
    caja_id                 BIGINT REFERENCES organizacion.caja(caja_id),
    fecha_inicio            DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin               DATE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_usuario_rol_scope UNIQUE (usuario_id, rol_id, empresa_id, sucursal_id, almacen_id, caja_id),
    CONSTRAINT ck_usuario_rol_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_usuario_rol_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)

);

CREATE TABLE IF NOT EXISTS security.sesion_usuario (
    sesion_usuario_id       BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT NOT NULL REFERENCES security.usuario(usuario_id),
    token_id                UUID NOT NULL DEFAULT gen_random_uuid(),
    refresh_token_hash      VARCHAR(255),
    ip_origen               INET,
    user_agent              TEXT,
    dispositivo             VARCHAR(150),
    canal                   VARCHAR(30) NOT NULL DEFAULT 'WEB',
    login_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    logout_at               TIMESTAMPTZ,
    expira_at               TIMESTAMPTZ,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT uq_sesion_token UNIQUE (token_id),
    CONSTRAINT ck_sesion_canal CHECK (canal IN ('WEB','POS','MOBILE','API','BACKOFFICE')),
    CONSTRAINT ck_sesion_estado CHECK (estado IN ('ACTIVA','CERRADA','EXPIRADA','REVOCADA'))

);

-- ==========================================================================
-- 6. AUDITORÍA DE EVENTOS
-- ==========================================================================
CREATE TABLE IF NOT EXISTS auditoria.auditoria_evento (
    auditoria_evento_id     BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT REFERENCES security.usuario(usuario_id),
    empresa_id              BIGINT REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    modulo                  VARCHAR(80) NOT NULL,
    recurso                 VARCHAR(120) NOT NULL,
    accion                  VARCHAR(80) NOT NULL,
    entidad                 VARCHAR(120),
    entidad_id              VARCHAR(80),
    descripcion             TEXT,
    valor_anterior          JSONB,
    valor_nuevo             JSONB,
    ip_origen               INET,
    user_agent              TEXT,
    resultado               VARCHAR(20) NOT NULL DEFAULT 'OK',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_auditoria_resultado CHECK (resultado IN ('OK','ERROR','DENEGADO'))

);

-- ==========================================================================
-- 7. CATÁLOGO COMERCIAL GENERAL
-- ==========================================================================
CREATE TABLE IF NOT EXISTS catalogo.categoria_producto (
    categoria_id            BIGSERIAL PRIMARY KEY,
    categoria_padre_id      BIGINT REFERENCES catalogo.categoria_producto(categoria_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    nivel                   INTEGER NOT NULL DEFAULT 1,
    orden                   INTEGER NOT NULL DEFAULT 0,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_categoria_codigo UNIQUE (codigo),
    CONSTRAINT ck_categoria_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_categoria_nivel CHECK (nivel >= 1)

);

CREATE TABLE IF NOT EXISTS catalogo.marca (
    marca_id                BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_marca_codigo UNIQUE (codigo),
    CONSTRAINT uq_marca_nombre UNIQUE (nombre),
    CONSTRAINT ck_marca_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS catalogo.unidad_medida (
    unidad_medida_id        BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(20) NOT NULL,
    nombre                  VARCHAR(100) NOT NULL,
    simbolo                 VARCHAR(20),
    permite_decimal         BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_unidad_codigo UNIQUE (codigo),
    CONSTRAINT ck_unidad_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS catalogo.producto_base (
    producto_base_id        BIGSERIAL PRIMARY KEY,
    categoria_id            BIGINT NOT NULL REFERENCES catalogo.categoria_producto(categoria_id),
    marca_id                BIGINT REFERENCES catalogo.marca(marca_id),
    codigo                  VARCHAR(60) NOT NULL,
    nombre                  VARCHAR(250) NOT NULL,
    descripcion             TEXT,
    tipo_producto           VARCHAR(50) NOT NULL DEFAULT 'COMERCIAL',
    es_farmaceutico         BOOLEAN NOT NULL DEFAULT FALSE,
    es_controlado           BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_receta         BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_lote           BOOLEAN NOT NULL DEFAULT TRUE,
    requiere_vencimiento    BOOLEAN NOT NULL DEFAULT TRUE,
    afecto_igv              BOOLEAN NOT NULL DEFAULT TRUE,
    imagen_url              TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_producto_base_codigo UNIQUE (codigo),
    CONSTRAINT ck_producto_base_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_producto_base_tipo CHECK (tipo_producto IN ('FARMACEUTICO','DISPOSITIVO_MEDICO','PRODUCTO_SANITARIO','COSMETICO','PERFUMERIA','CUIDADO_PERSONAL','PRENDA','ALIMENTO','COMERCIAL','OTRO'))

);

CREATE TABLE IF NOT EXISTS catalogo.producto_sku (
    producto_sku_id         BIGSERIAL PRIMARY KEY,
    producto_base_id        BIGINT NOT NULL REFERENCES catalogo.producto_base(producto_base_id),
    unidad_medida_id        BIGINT NOT NULL REFERENCES catalogo.unidad_medida(unidad_medida_id),
    sku                     VARCHAR(80) NOT NULL,
    nombre_comercial        VARCHAR(250) NOT NULL,
    presentacion            VARCHAR(180),
    contenido               NUMERIC(14,4),
    unidad_contenido        VARCHAR(30),
    peso_gramos             NUMERIC(14,4),
    alto_cm                 NUMERIC(10,2),
    ancho_cm                NUMERIC(10,2),
    largo_cm                NUMERIC(10,2),
    permite_venta_fraccion  BOOLEAN NOT NULL DEFAULT FALSE,
    factor_fraccion         NUMERIC(14,4),
    stock_minimo_default    NUMERIC(14,4) NOT NULL DEFAULT 0,
    stock_maximo_default    NUMERIC(14,4),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_producto_sku UNIQUE (sku),
    CONSTRAINT ck_producto_sku_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_producto_sku_factor CHECK (factor_fraccion IS NULL OR factor_fraccion > 0)

);

CREATE TABLE IF NOT EXISTS catalogo.codigo_barra (
    codigo_barra_id         BIGSERIAL PRIMARY KEY,
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    codigo_barra            VARCHAR(80) NOT NULL,
    tipo_codigo             VARCHAR(30) NOT NULL DEFAULT 'EAN13',
    es_principal            BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_codigo_barra UNIQUE (codigo_barra),
    CONSTRAINT ck_codigo_barra_estado CHECK (estado IN ('A','I'))

);

-- ==========================================================================
-- 8. EXTENSIÓN FARMACÉUTICA / SANITARIA
-- ==========================================================================
CREATE TABLE IF NOT EXISTS farmacia.laboratorio (
    laboratorio_id          BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(60) NOT NULL,
    nombre                  VARCHAR(200) NOT NULL,
    pais_origen             VARCHAR(80),
    ruc                     VARCHAR(11),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_laboratorio_codigo UNIQUE (codigo),
    CONSTRAINT ck_laboratorio_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS farmacia.forma_farmaceutica (
    forma_farmaceutica_id   BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(120) NOT NULL,
    descripcion             TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_forma_farmaceutica_codigo UNIQUE (codigo),
    CONSTRAINT ck_forma_farmaceutica_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS farmacia.condicion_venta (
    condicion_venta_id      BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    requiere_receta         BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_validacion_qf  BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_condicion_venta_codigo UNIQUE (codigo),
    CONSTRAINT ck_condicion_venta_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS farmacia.principio_activo (
    principio_activo_id     BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(60) NOT NULL,
    nombre                  VARCHAR(200) NOT NULL,
    descripcion             TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_principio_activo_codigo UNIQUE (codigo),
    CONSTRAINT ck_principio_activo_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS farmacia.producto_farmaceutico (
    producto_farmaceutico_id BIGSERIAL PRIMARY KEY,
    producto_base_id         BIGINT NOT NULL REFERENCES catalogo.producto_base(producto_base_id),
    laboratorio_id           BIGINT REFERENCES farmacia.laboratorio(laboratorio_id),
    forma_farmaceutica_id    BIGINT REFERENCES farmacia.forma_farmaceutica(forma_farmaceutica_id),
    condicion_venta_id       BIGINT REFERENCES farmacia.condicion_venta(condicion_venta_id),
    registro_sanitario       VARCHAR(80),
    codigo_digemid           VARCHAR(80),
    concentracion            VARCHAR(120),
    via_administracion       VARCHAR(120),
    indicaciones             TEXT,
    contraindicaciones       TEXT,
    requiere_cadena_frio     BOOLEAN NOT NULL DEFAULT FALSE,
    temperatura_minima       NUMERIC(5,2),
    temperatura_maxima       NUMERIC(5,2),
    estado                   CHAR(1) NOT NULL DEFAULT 'A',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_at               TIMESTAMPTZ,
    updated_by               BIGINT,
    deleted_at               TIMESTAMPTZ,
    deleted_by               BIGINT,
    CONSTRAINT uq_producto_farm_producto UNIQUE (producto_base_id),
    CONSTRAINT uq_producto_farm_registro UNIQUE (registro_sanitario),
    CONSTRAINT ck_producto_farm_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_producto_farm_temp CHECK (temperatura_minima IS NULL OR temperatura_maxima IS NULL OR temperatura_maxima >= temperatura_minima)

);

CREATE TABLE IF NOT EXISTS farmacia.producto_principio_activo (
    producto_principio_activo_id BIGSERIAL PRIMARY KEY,
    producto_farmaceutico_id     BIGINT NOT NULL REFERENCES farmacia.producto_farmaceutico(producto_farmaceutico_id),
    principio_activo_id          BIGINT NOT NULL REFERENCES farmacia.principio_activo(principio_activo_id),
    dosis                        VARCHAR(120),
    es_principal                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                   BIGINT,
    CONSTRAINT uq_producto_principio UNIQUE (producto_farmaceutico_id, principio_activo_id)

);

-- ==========================================================================
-- 9. PROVEEDORES
-- ==========================================================================
CREATE TABLE IF NOT EXISTS proveedores.proveedor (
    proveedor_id            BIGSERIAL PRIMARY KEY,
    uuid_publico            UUID NOT NULL DEFAULT gen_random_uuid(),
    tipo_documento          VARCHAR(10) NOT NULL DEFAULT 'RUC',
    numero_documento        VARCHAR(20) NOT NULL,
    razon_social            VARCHAR(250) NOT NULL,
    nombre_comercial        VARCHAR(200),
    direccion               VARCHAR(300),
    ubigeo                  VARCHAR(6),
    telefono                VARCHAR(30),
    email                   CITEXT,
    contacto_nombre         VARCHAR(160),
    contacto_telefono       VARCHAR(30),
    contacto_email          CITEXT,
    es_laboratorio          BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_proveedor_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_proveedor_estado CHECK (estado IN ('A','I'))

);

-- ==========================================================================
-- 10. CLIENTES BASE PARA VENTAS, CRM, CMR Y APP
-- ==========================================================================
CREATE TABLE IF NOT EXISTS ventas.cliente (
    cliente_id              BIGSERIAL PRIMARY KEY,
    uuid_publico            UUID NOT NULL DEFAULT gen_random_uuid(),
    tipo_cliente            VARCHAR(30) NOT NULL DEFAULT 'NATURAL',
    tipo_documento          VARCHAR(10),
    numero_documento        VARCHAR(20),
    nombres                 VARCHAR(120),
    apellidos               VARCHAR(150),
    razon_social            VARCHAR(250),
    email                   CITEXT,
    telefono                VARCHAR(30),
    fecha_nacimiento        DATE,
    genero                  VARCHAR(20),
    acepta_marketing        BOOLEAN NOT NULL DEFAULT FALSE,
    acepta_tratamiento_datos BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_cliente_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_cliente_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_cliente_tipo CHECK (tipo_cliente IN ('NATURAL','JURIDICO','SIN_IDENTIFICAR'))

);

-- ==========================================================================
-- 11. APP WEB / APP MÓVIL: CANALES, CUENTAS Y DIRECCIONES
-- ==========================================================================
CREATE TABLE IF NOT EXISTS app.canal (
    canal_id                BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(120) NOT NULL,
    descripcion             TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_canal_codigo UNIQUE (codigo),
    CONSTRAINT ck_canal_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS app.cliente_cuenta (
    cliente_cuenta_id       BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    email                   CITEXT NOT NULL,
    telefono                VARCHAR(30),
    password_hash           VARCHAR(255) NOT NULL,
    email_verificado        BOOLEAN NOT NULL DEFAULT FALSE,
    telefono_verificado     BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login_at         TIMESTAMPTZ,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_cliente_cuenta_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_cliente_cuenta_email UNIQUE (email),
    CONSTRAINT ck_cliente_cuenta_estado CHECK (estado IN ('A','I','B'))

);

CREATE TABLE IF NOT EXISTS app.dispositivo_cliente (
    dispositivo_cliente_id  BIGSERIAL PRIMARY KEY,
    cliente_cuenta_id       BIGINT NOT NULL REFERENCES app.cliente_cuenta(cliente_cuenta_id),
    plataforma              VARCHAR(30) NOT NULL,
    token_push              TEXT,
    identificador_dispositivo VARCHAR(180),
    modelo                  VARCHAR(120),
    version_so              VARCHAR(80),
    app_version             VARCHAR(50),
    ultimo_acceso_at        TIMESTAMPTZ,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    revocado_at             TIMESTAMPTZ,
    revocado_by             BIGINT,
    CONSTRAINT ck_dispositivo_plataforma CHECK (plataforma IN ('ANDROID','IOS','WEB','PWA')),
    CONSTRAINT ck_dispositivo_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS app.sesion_cliente (
    sesion_cliente_id       BIGSERIAL PRIMARY KEY,
    cliente_cuenta_id       BIGINT NOT NULL REFERENCES app.cliente_cuenta(cliente_cuenta_id),
    dispositivo_cliente_id  BIGINT REFERENCES app.dispositivo_cliente(dispositivo_cliente_id),
    token_id                UUID NOT NULL DEFAULT gen_random_uuid(),
    ip_origen               INET,
    user_agent              TEXT,
    login_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    logout_at               TIMESTAMPTZ,
    expira_at               TIMESTAMPTZ,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT uq_sesion_cliente_token UNIQUE (token_id),
    CONSTRAINT ck_sesion_cliente_estado CHECK (estado IN ('ACTIVA','CERRADA','EXPIRADA','REVOCADA'))

);

CREATE TABLE IF NOT EXISTS app.direccion_cliente (
    direccion_cliente_id    BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    alias                   VARCHAR(80),
    direccion               VARCHAR(300) NOT NULL,
    referencia              VARCHAR(300),
    ubigeo                  VARCHAR(6),
    latitud                 NUMERIC(10,7),
    longitud                NUMERIC(10,7),
    es_principal            BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT ck_direccion_cliente_estado CHECK (estado IN ('A','I'))

);

-- ==========================================================================
-- 12. COMPRAS
-- ==========================================================================
CREATE TABLE IF NOT EXISTS compras.orden_compra (
    orden_compra_id         BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    proveedor_id            BIGINT NOT NULL REFERENCES proveedores.proveedor(proveedor_id),
    numero_orden            VARCHAR(50) NOT NULL,
    fecha_emision           DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_entrega_estimada  DATE,
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    subtotal                NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv                     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_orden_compra_empresa_numero UNIQUE (empresa_id, numero_orden),
    CONSTRAINT ck_orden_compra_estado CHECK (estado IN ('BORRADOR','EMITIDA','APROBADA','RECIBIDA_PARCIAL','RECIBIDA_TOTAL','ANULADA')),
    CONSTRAINT ck_orden_compra_total CHECK (subtotal >= 0 AND igv >= 0 AND total >= 0)

);

CREATE TABLE IF NOT EXISTS compras.orden_compra_detalle (
    orden_compra_detalle_id BIGSERIAL PRIMARY KEY,
    orden_compra_id         BIGINT NOT NULL REFERENCES compras.orden_compra(orden_compra_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    cantidad                NUMERIC(14,4) NOT NULL,
    precio_unitario         NUMERIC(14,4) NOT NULL,
    descuento               NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv                     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_oc_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_oc_detalle_importes CHECK (precio_unitario >= 0 AND descuento >= 0 AND igv >= 0 AND total >= 0)

);

CREATE TABLE IF NOT EXISTS compras.compra (
    compra_id               BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id              BIGINT NOT NULL REFERENCES organizacion.almacen(almacen_id),
    proveedor_id            BIGINT NOT NULL REFERENCES proveedores.proveedor(proveedor_id),
    orden_compra_id         BIGINT REFERENCES compras.orden_compra(orden_compra_id),
    tipo_comprobante        VARCHAR(20) NOT NULL,
    serie                   VARCHAR(10),
    numero                  VARCHAR(30),
    fecha_emision           DATE NOT NULL,
    fecha_recepcion         DATE NOT NULL DEFAULT CURRENT_DATE,
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    subtotal                NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv                     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_compra_comprobante UNIQUE (proveedor_id, tipo_comprobante, serie, numero),
    CONSTRAINT ck_compra_estado CHECK (estado IN ('REGISTRADA','CONFIRMADA','ANULADA')),
    CONSTRAINT ck_compra_importes CHECK (subtotal >= 0 AND igv >= 0 AND total >= 0)

);

CREATE TABLE IF NOT EXISTS compras.compra_detalle (
    compra_detalle_id       BIGSERIAL PRIMARY KEY,
    compra_id               BIGINT NOT NULL REFERENCES compras.compra(compra_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    cantidad                NUMERIC(14,4) NOT NULL,
    precio_unitario         NUMERIC(14,4) NOT NULL,
    descuento               NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv                     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_compra_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_compra_detalle_importes CHECK (precio_unitario >= 0 AND descuento >= 0 AND igv >= 0 AND total >= 0)

);

-- ==========================================================================
-- 13. INVENTARIO Y KARDEX
-- ==========================================================================
CREATE TABLE IF NOT EXISTS inventario.tipo_movimiento_inventario (
    tipo_movimiento_id      BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(120) NOT NULL,
    naturaleza              CHAR(1) NOT NULL,
    descripcion             TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_tipo_movimiento_codigo UNIQUE (codigo),
    CONSTRAINT ck_tipo_movimiento_naturaleza CHECK (naturaleza IN ('E','S')),
    CONSTRAINT ck_tipo_movimiento_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS inventario.lote_producto (
    lote_producto_id        BIGSERIAL PRIMARY KEY,
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    proveedor_id            BIGINT REFERENCES proveedores.proveedor(proveedor_id),
    compra_detalle_id       BIGINT REFERENCES compras.compra_detalle(compra_detalle_id),
    numero_lote             VARCHAR(100) NOT NULL,
    fecha_fabricacion       DATE,
    fecha_vencimiento       DATE,
    registro_sanitario      VARCHAR(80),
    estado_lote             VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_lote_producto UNIQUE (producto_sku_id, numero_lote, fecha_vencimiento),
    CONSTRAINT ck_lote_producto_estado CHECK (estado_lote IN ('DISPONIBLE','CUARENTENA','BLOQUEADO','VENCIDO','AGOTADO','RETIRADO')),
    CONSTRAINT ck_lote_producto_fechas CHECK (fecha_vencimiento IS NULL OR fecha_fabricacion IS NULL OR fecha_vencimiento >= fecha_fabricacion)

);

CREATE TABLE IF NOT EXISTS inventario.inventario_lote (
    inventario_lote_id      BIGSERIAL PRIMARY KEY,
    almacen_id              BIGINT NOT NULL REFERENCES organizacion.almacen(almacen_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    lote_producto_id        BIGINT REFERENCES inventario.lote_producto(lote_producto_id),
    stock_actual            NUMERIC(14,4) NOT NULL DEFAULT 0,
    stock_reservado         NUMERIC(14,4) NOT NULL DEFAULT 0,
    stock_disponible        NUMERIC(14,4) GENERATED ALWAYS AS (stock_actual - stock_reservado) STORED,
    costo_promedio          NUMERIC(14,4) NOT NULL DEFAULT 0,
    ubicacion_fisica        VARCHAR(100),
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_inventario_lote UNIQUE (almacen_id, producto_sku_id, lote_producto_id),
    CONSTRAINT ck_inventario_stock CHECK (stock_actual >= 0 AND stock_reservado >= 0 AND stock_reservado <= stock_actual),
    CONSTRAINT ck_inventario_costo CHECK (costo_promedio >= 0)

);

CREATE TABLE IF NOT EXISTS inventario.kardex_movimiento (
    kardex_movimiento_id    BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id              BIGINT NOT NULL REFERENCES organizacion.almacen(almacen_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    lote_producto_id        BIGINT REFERENCES inventario.lote_producto(lote_producto_id),
    tipo_movimiento_id      BIGINT NOT NULL REFERENCES inventario.tipo_movimiento_inventario(tipo_movimiento_id),
    fecha_movimiento        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    documento_tipo          VARCHAR(50),
    documento_id            BIGINT,
    cantidad                NUMERIC(14,4) NOT NULL,
    costo_unitario          NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_total             NUMERIC(14,2) NOT NULL DEFAULT 0,
    stock_anterior          NUMERIC(14,4),
    stock_posterior         NUMERIC(14,4),
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_kardex_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_kardex_costo CHECK (costo_unitario >= 0 AND costo_total >= 0)

);

CREATE TABLE IF NOT EXISTS inventario.transferencia_stock (
    transferencia_stock_id  BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_origen_id      BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    almacen_origen_id       BIGINT NOT NULL REFERENCES organizacion.almacen(almacen_id),
    sucursal_destino_id     BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    almacen_destino_id      BIGINT NOT NULL REFERENCES organizacion.almacen(almacen_id),
    numero_transferencia    VARCHAR(50) NOT NULL,
    fecha_emision           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_recepcion         TIMESTAMPTZ,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'EMITIDA',
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_transferencia_numero UNIQUE (empresa_id, numero_transferencia),
    CONSTRAINT ck_transferencia_estado CHECK (estado IN ('EMITIDA','EN_TRANSITO','RECIBIDA','ANULADA','RECHAZADA'))

);

CREATE TABLE IF NOT EXISTS inventario.transferencia_stock_detalle (
    transferencia_stock_detalle_id BIGSERIAL PRIMARY KEY,
    transferencia_stock_id         BIGINT NOT NULL REFERENCES inventario.transferencia_stock(transferencia_stock_id),
    producto_sku_id                BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    lote_producto_id               BIGINT REFERENCES inventario.lote_producto(lote_producto_id),
    cantidad                       NUMERIC(14,4) NOT NULL,
    cantidad_recibida              NUMERIC(14,4) NOT NULL DEFAULT 0,
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                     BIGINT,
    CONSTRAINT ck_transferencia_detalle_cantidad CHECK (cantidad > 0 AND cantidad_recibida >= 0)

);

CREATE TABLE IF NOT EXISTS inventario.ajuste_inventario (
    ajuste_inventario_id    BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id              BIGINT NOT NULL REFERENCES organizacion.almacen(almacen_id),
    numero_ajuste           VARCHAR(50) NOT NULL,
    motivo                  VARCHAR(160) NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    observacion             TEXT,
    fecha_ajuste            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_ajuste_numero UNIQUE (empresa_id, numero_ajuste),
    CONSTRAINT ck_ajuste_estado CHECK (estado IN ('BORRADOR','APROBADO','ANULADO'))

);

CREATE TABLE IF NOT EXISTS inventario.ajuste_inventario_detalle (
    ajuste_inventario_detalle_id BIGSERIAL PRIMARY KEY,
    ajuste_inventario_id         BIGINT NOT NULL REFERENCES inventario.ajuste_inventario(ajuste_inventario_id),
    producto_sku_id              BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    lote_producto_id             BIGINT REFERENCES inventario.lote_producto(lote_producto_id),
    stock_sistema                NUMERIC(14,4) NOT NULL,
    stock_fisico                 NUMERIC(14,4) NOT NULL,
    diferencia                   NUMERIC(14,4) GENERATED ALWAYS AS (stock_fisico - stock_sistema) STORED,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                   BIGINT,
    CONSTRAINT ck_ajuste_detalle_stock CHECK (stock_sistema >= 0 AND stock_fisico >= 0)

);

-- ==========================================================================
-- 14. VENTAS, POS, PRECIOS, COMPROBANTES Y DEVOLUCIONES
-- ==========================================================================
CREATE TABLE IF NOT EXISTS ventas.lista_precio (
    lista_precio_id         BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_lista_precio_empresa_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_lista_precio_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS ventas.precio_producto (
    precio_producto_id      BIGSERIAL PRIMARY KEY,
    lista_precio_id         BIGINT NOT NULL REFERENCES ventas.lista_precio(lista_precio_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    precio_venta            NUMERIC(14,4) NOT NULL,
    precio_oferta           NUMERIC(14,4),
    fecha_inicio            DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin               DATE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_precio_producto UNIQUE (lista_precio_id, producto_sku_id, sucursal_id, fecha_inicio),
    CONSTRAINT ck_precio_producto_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_precio_producto_precio CHECK (precio_venta >= 0 AND (precio_oferta IS NULL OR precio_oferta >= 0)),
    CONSTRAINT ck_precio_producto_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)

);

CREATE TABLE IF NOT EXISTS ventas.promocion (
    promocion_id            BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(180) NOT NULL,
    descripcion             TEXT,
    tipo_promocion          VARCHAR(40) NOT NULL,
    fecha_inicio            TIMESTAMPTZ NOT NULL,
    fecha_fin               TIMESTAMPTZ NOT NULL,
    canal                   VARCHAR(30) NOT NULL DEFAULT 'TODOS',
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_promocion_empresa_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_promocion_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_promocion_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_promocion_tipo CHECK (tipo_promocion IN ('DESCUENTO_PORCENTAJE','DESCUENTO_MONTO','2X1','COMBO','CUPON','PUNTOS','OTRO'))

);

CREATE TABLE IF NOT EXISTS ventas.promocion_detalle (
    promocion_detalle_id    BIGSERIAL PRIMARY KEY,
    promocion_id            BIGINT NOT NULL REFERENCES ventas.promocion(promocion_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    cantidad_minima         NUMERIC(14,4) NOT NULL DEFAULT 1,
    valor_descuento         NUMERIC(14,4) NOT NULL DEFAULT 0,
    precio_promocional      NUMERIC(14,4),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_promocion_producto UNIQUE (promocion_id, producto_sku_id),
    CONSTRAINT ck_promocion_detalle_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_promocion_detalle_valores CHECK (cantidad_minima > 0 AND valor_descuento >= 0 AND (precio_promocional IS NULL OR precio_promocional >= 0))

);

CREATE TABLE IF NOT EXISTS ventas.medio_pago (
    medio_pago_id           BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(120) NOT NULL,
    requiere_referencia     BOOLEAN NOT NULL DEFAULT FALSE,
    permite_vuelto          BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_medio_pago_codigo UNIQUE (codigo),
    CONSTRAINT ck_medio_pago_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS ventas.tipo_comprobante (
    tipo_comprobante_id     BIGSERIAL PRIMARY KEY,
    codigo_sunat            VARCHAR(10) NOT NULL,
    codigo_interno          VARCHAR(30) NOT NULL,
    nombre                  VARCHAR(120) NOT NULL,
    requiere_cliente_doc    BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_tipo_comprobante_codigo_sunat UNIQUE (codigo_sunat),
    CONSTRAINT uq_tipo_comprobante_codigo_interno UNIQUE (codigo_interno),
    CONSTRAINT ck_tipo_comprobante_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS ventas.serie_comprobante (
    serie_comprobante_id    BIGSERIAL PRIMARY KEY,
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    caja_id                 BIGINT REFERENCES organizacion.caja(caja_id),
    tipo_comprobante_id     BIGINT NOT NULL REFERENCES ventas.tipo_comprobante(tipo_comprobante_id),
    serie                   VARCHAR(10) NOT NULL,
    correlativo_actual      BIGINT NOT NULL DEFAULT 0,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_serie_comprobante UNIQUE (sucursal_id, tipo_comprobante_id, serie),
    CONSTRAINT ck_serie_comprobante_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS ventas.turno_caja (
    turno_caja_id           BIGSERIAL PRIMARY KEY,
    caja_id                 BIGINT NOT NULL REFERENCES organizacion.caja(caja_id),
    usuario_id              BIGINT NOT NULL REFERENCES security.usuario(usuario_id),
    fecha_apertura          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_cierre            TIMESTAMPTZ,
    monto_apertura          NUMERIC(14,2) NOT NULL DEFAULT 0,
    monto_cierre            NUMERIC(14,2),
    monto_sistema           NUMERIC(14,2),
    diferencia              NUMERIC(14,2),
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_turno_caja_estado CHECK (estado IN ('ABIERTO','CERRADO','ANULADO')),
    CONSTRAINT ck_turno_caja_montos CHECK (monto_apertura >= 0)

);

CREATE TABLE IF NOT EXISTS ventas.carrito (
    carrito_id              BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    cliente_cuenta_id       BIGINT REFERENCES app.cliente_cuenta(cliente_cuenta_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    canal_id                BIGINT REFERENCES app.canal(canal_id),
    estado                  VARCHAR(30) NOT NULL DEFAULT 'ABIERTO',
    subtotal                NUMERIC(14,2) NOT NULL DEFAULT 0,
    descuento_total         NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv_total               NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_carrito_estado CHECK (estado IN ('ABIERTO','CONVERTIDO','ABANDONADO','CANCELADO'))

);

CREATE TABLE IF NOT EXISTS ventas.carrito_detalle (
    carrito_detalle_id      BIGSERIAL PRIMARY KEY,
    carrito_id              BIGINT NOT NULL REFERENCES ventas.carrito(carrito_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    cantidad                NUMERIC(14,4) NOT NULL,
    precio_unitario         NUMERIC(14,4) NOT NULL,
    descuento               NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_carrito_producto UNIQUE (carrito_id, producto_sku_id),
    CONSTRAINT ck_carrito_detalle_cantidad CHECK (cantidad > 0)

);

CREATE TABLE IF NOT EXISTS ventas.pedido_digital (
    pedido_digital_id       BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    cliente_cuenta_id       BIGINT REFERENCES app.cliente_cuenta(cliente_cuenta_id),
    carrito_id              BIGINT REFERENCES ventas.carrito(carrito_id),
    direccion_cliente_id    BIGINT REFERENCES app.direccion_cliente(direccion_cliente_id),
    canal_id                BIGINT REFERENCES app.canal(canal_id),
    numero_pedido           VARCHAR(50) NOT NULL,
    tipo_entrega            VARCHAR(30) NOT NULL DEFAULT 'DELIVERY',
    estado                  VARCHAR(40) NOT NULL DEFAULT 'CREADO',
    subtotal                NUMERIC(14,2) NOT NULL DEFAULT 0,
    descuento_total         NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv_total               NUMERIC(14,2) NOT NULL DEFAULT 0,
    costo_delivery          NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL DEFAULT 0,
    fecha_programada        TIMESTAMPTZ,
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_pedido_digital_numero UNIQUE (empresa_id, numero_pedido),
    CONSTRAINT ck_pedido_digital_tipo_entrega CHECK (tipo_entrega IN ('DELIVERY','RECOJO_TIENDA')),
    CONSTRAINT ck_pedido_digital_estado CHECK (estado IN ('CREADO','PENDIENTE_PAGO','PAGADO','CONFIRMADO','PREPARANDO','LISTO_RECOJO','EN_RUTA','ENTREGADO','CANCELADO','RECHAZADO'))

);

CREATE TABLE IF NOT EXISTS ventas.pedido_digital_detalle (
    pedido_digital_detalle_id BIGSERIAL PRIMARY KEY,
    pedido_digital_id         BIGINT NOT NULL REFERENCES ventas.pedido_digital(pedido_digital_id),
    producto_sku_id           BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    cantidad                  NUMERIC(14,4) NOT NULL,
    precio_unitario           NUMERIC(14,4) NOT NULL,
    descuento                 NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv                       NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                     NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                BIGINT,
    CONSTRAINT ck_pedido_detalle_cantidad CHECK (cantidad > 0)

);

CREATE TABLE IF NOT EXISTS inventario.reserva_stock (
    reserva_stock_id        BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id              BIGINT NOT NULL REFERENCES organizacion.almacen(almacen_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    lote_producto_id        BIGINT REFERENCES inventario.lote_producto(lote_producto_id),
    pedido_digital_id       BIGINT REFERENCES ventas.pedido_digital(pedido_digital_id),
    cantidad                NUMERIC(14,4) NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
    expira_at               TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_reserva_stock_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_reserva_stock_estado CHECK (estado IN ('ACTIVA','CONSUMIDA','EXPIRADA','CANCELADA'))

);

CREATE TABLE IF NOT EXISTS ventas.receta_digital (
    receta_digital_id       BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    pedido_digital_id       BIGINT REFERENCES ventas.pedido_digital(pedido_digital_id),
    archivo_url             TEXT,
    medico_nombre           VARCHAR(180),
    medico_cmp              VARCHAR(30),
    fecha_emision           DATE,
    estado_validacion       VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    validado_por            BIGINT REFERENCES security.usuario(usuario_id),
    validado_at             TIMESTAMPTZ,
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_receta_estado CHECK (estado_validacion IN ('PENDIENTE','VALIDADA','RECHAZADA','OBSERVADA'))

);

CREATE TABLE IF NOT EXISTS ventas.venta (
    venta_id                BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    caja_id                 BIGINT REFERENCES organizacion.caja(caja_id),
    turno_caja_id           BIGINT REFERENCES ventas.turno_caja(turno_caja_id),
    usuario_vendedor_id     BIGINT REFERENCES security.usuario(usuario_id),
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    pedido_digital_id       BIGINT REFERENCES ventas.pedido_digital(pedido_digital_id),
    canal_id                BIGINT REFERENCES app.canal(canal_id),
    numero_operacion        VARCHAR(60) NOT NULL,
    fecha_venta             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tipo_venta              VARCHAR(30) NOT NULL DEFAULT 'PRESENCIAL',
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    subtotal                NUMERIC(14,2) NOT NULL DEFAULT 0,
    descuento_total         NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv_total               NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_venta_operacion UNIQUE (empresa_id, numero_operacion),
    CONSTRAINT ck_venta_tipo CHECK (tipo_venta IN ('PRESENCIAL','WEB','MOBILE','CALL_CENTER','MARKETPLACE')),
    CONSTRAINT ck_venta_estado CHECK (estado IN ('REGISTRADA','PAGADA','FACTURADA','ANULADA','DEVUELTA_PARCIAL','DEVUELTA_TOTAL')),
    CONSTRAINT ck_venta_importes CHECK (subtotal >= 0 AND descuento_total >= 0 AND igv_total >= 0 AND total >= 0)

);

CREATE TABLE IF NOT EXISTS ventas.venta_detalle (
    venta_detalle_id        BIGSERIAL PRIMARY KEY,
    venta_id                BIGINT NOT NULL REFERENCES ventas.venta(venta_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    lote_producto_id        BIGINT REFERENCES inventario.lote_producto(lote_producto_id),
    cantidad                NUMERIC(14,4) NOT NULL,
    precio_unitario         NUMERIC(14,4) NOT NULL,
    descuento               NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv                     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(14,2) NOT NULL,
    receta_digital_id       BIGINT REFERENCES ventas.receta_digital(receta_digital_id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_venta_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_venta_detalle_importes CHECK (precio_unitario >= 0 AND descuento >= 0 AND igv >= 0 AND total >= 0)

);

CREATE TABLE IF NOT EXISTS ventas.pago_venta (
    pago_venta_id           BIGSERIAL PRIMARY KEY,
    venta_id                BIGINT NOT NULL REFERENCES ventas.venta(venta_id),
    medio_pago_id           BIGINT NOT NULL REFERENCES ventas.medio_pago(medio_pago_id),
    monto                   NUMERIC(14,2) NOT NULL,
    referencia              VARCHAR(120),
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    estado                  VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    fecha_pago              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_pago_venta_monto CHECK (monto > 0),
    CONSTRAINT ck_pago_venta_estado CHECK (estado IN ('REGISTRADO','CONFIRMADO','ANULADO','EXTORNADO'))

);

CREATE TABLE IF NOT EXISTS ventas.comprobante_electronico (
    comprobante_electronico_id BIGSERIAL PRIMARY KEY,
    venta_id                   BIGINT NOT NULL REFERENCES ventas.venta(venta_id),
    tipo_comprobante_id        BIGINT NOT NULL REFERENCES ventas.tipo_comprobante(tipo_comprobante_id),
    serie_comprobante_id       BIGINT REFERENCES ventas.serie_comprobante(serie_comprobante_id),
    serie                      VARCHAR(10) NOT NULL,
    numero                     BIGINT NOT NULL,
    fecha_emision              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cliente_tipo_documento     VARCHAR(10),
    cliente_numero_documento   VARCHAR(20),
    cliente_nombre             VARCHAR(250),
    subtotal                   NUMERIC(14,2) NOT NULL DEFAULT 0,
    igv                        NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                      NUMERIC(14,2) NOT NULL DEFAULT 0,
    hash_cpe                   VARCHAR(255),
    xml_url                    TEXT,
    pdf_url                    TEXT,
    cdr_url                    TEXT,
    estado_sunat               VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_sunat              TEXT,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                 BIGINT,
    updated_at                 TIMESTAMPTZ,
    updated_by                 BIGINT,
    CONSTRAINT uq_comprobante_serie_numero UNIQUE (tipo_comprobante_id, serie, numero),
    CONSTRAINT ck_comprobante_estado_sunat CHECK (estado_sunat IN ('PENDIENTE','ENVIADO','ACEPTADO','RECHAZADO','ANULADO','ERROR'))

);

CREATE TABLE IF NOT EXISTS ventas.devolucion_venta (
    devolucion_venta_id     BIGSERIAL PRIMARY KEY,
    venta_id                BIGINT NOT NULL REFERENCES ventas.venta(venta_id),
    numero_devolucion       VARCHAR(60) NOT NULL,
    fecha_devolucion        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    motivo                  VARCHAR(200) NOT NULL,
    total_devuelto          NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_devolucion_numero UNIQUE (numero_devolucion),
    CONSTRAINT ck_devolucion_estado CHECK (estado IN ('REGISTRADA','APROBADA','ANULADA'))

);

CREATE TABLE IF NOT EXISTS ventas.devolucion_venta_detalle (
    devolucion_venta_detalle_id BIGSERIAL PRIMARY KEY,
    devolucion_venta_id         BIGINT NOT NULL REFERENCES ventas.devolucion_venta(devolucion_venta_id),
    venta_detalle_id            BIGINT NOT NULL REFERENCES ventas.venta_detalle(venta_detalle_id),
    cantidad                    NUMERIC(14,4) NOT NULL,
    monto_devuelto              NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  BIGINT,
    CONSTRAINT ck_devolucion_detalle_cantidad CHECK (cantidad > 0 AND monto_devuelto >= 0)

);

-- ==========================================================================
-- 15. FINANZAS Y CONTABILIDAD BÁSICA
-- ==========================================================================
CREATE TABLE IF NOT EXISTS finanzas.cuenta_bancaria (
    cuenta_bancaria_id      BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    banco                   VARCHAR(120) NOT NULL,
    numero_cuenta           VARCHAR(80) NOT NULL,
    cci                     VARCHAR(80),
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_cuenta_bancaria UNIQUE (empresa_id, banco, numero_cuenta),
    CONSTRAINT ck_cuenta_bancaria_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS finanzas.caja_financiera (
    caja_financiera_id      BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_caja_financiera_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_caja_financiera_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS finanzas.movimiento_caja (
    movimiento_caja_id      BIGSERIAL PRIMARY KEY,
    caja_financiera_id      BIGINT NOT NULL REFERENCES finanzas.caja_financiera(caja_financiera_id),
    fecha_movimiento        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tipo_movimiento         CHAR(1) NOT NULL,
    concepto                VARCHAR(180) NOT NULL,
    monto                   NUMERIC(14,2) NOT NULL,
    referencia_tipo         VARCHAR(60),
    referencia_id           BIGINT,
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_movimiento_caja_tipo CHECK (tipo_movimiento IN ('I','E')),
    CONSTRAINT ck_movimiento_caja_monto CHECK (monto > 0)

);

CREATE TABLE IF NOT EXISTS finanzas.cuenta_por_cobrar (
    cuenta_por_cobrar_id    BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    venta_id                BIGINT REFERENCES ventas.venta(venta_id),
    comprobante_electronico_id BIGINT REFERENCES ventas.comprobante_electronico(comprobante_electronico_id),
    fecha_emision           DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento       DATE,
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    monto_total             NUMERIC(14,2) NOT NULL,
    monto_pagado            NUMERIC(14,2) NOT NULL DEFAULT 0,
    saldo                   NUMERIC(14,2) GENERATED ALWAYS AS (monto_total - monto_pagado) STORED,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_cxc_montos CHECK (monto_total >= 0 AND monto_pagado >= 0 AND monto_pagado <= monto_total),
    CONSTRAINT ck_cxc_estado CHECK (estado IN ('PENDIENTE','PARCIAL','PAGADA','ANULADA','VENCIDA'))

);

CREATE TABLE IF NOT EXISTS finanzas.cuenta_por_pagar (
    cuenta_por_pagar_id     BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    proveedor_id            BIGINT NOT NULL REFERENCES proveedores.proveedor(proveedor_id),
    compra_id               BIGINT REFERENCES compras.compra(compra_id),
    fecha_emision           DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento       DATE,
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    monto_total             NUMERIC(14,2) NOT NULL,
    monto_pagado            NUMERIC(14,2) NOT NULL DEFAULT 0,
    saldo                   NUMERIC(14,2) GENERATED ALWAYS AS (monto_total - monto_pagado) STORED,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_cxp_montos CHECK (monto_total >= 0 AND monto_pagado >= 0 AND monto_pagado <= monto_total),
    CONSTRAINT ck_cxp_estado CHECK (estado IN ('PENDIENTE','PARCIAL','PAGADA','ANULADA','VENCIDA'))

);

CREATE TABLE IF NOT EXISTS finanzas.plan_cuenta (
    plan_cuenta_id          BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(40) NOT NULL,
    nombre                  VARCHAR(180) NOT NULL,
    tipo_cuenta             VARCHAR(40) NOT NULL,
    nivel                   INTEGER NOT NULL DEFAULT 1,
    cuenta_padre_id         BIGINT REFERENCES finanzas.plan_cuenta(plan_cuenta_id),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_plan_cuenta_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_plan_cuenta_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_plan_cuenta_tipo CHECK (tipo_cuenta IN ('ACTIVO','PASIVO','PATRIMONIO','INGRESO','GASTO','COSTO','ORDEN'))

);

CREATE TABLE IF NOT EXISTS finanzas.asiento_contable (
    asiento_contable_id     BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    numero_asiento          VARCHAR(60) NOT NULL,
    fecha_asiento           DATE NOT NULL DEFAULT CURRENT_DATE,
    glosa                   VARCHAR(300) NOT NULL,
    referencia_tipo         VARCHAR(60),
    referencia_id           BIGINT,
    total_debe              NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_haber             NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_asiento_numero UNIQUE (empresa_id, numero_asiento),
    CONSTRAINT ck_asiento_estado CHECK (estado IN ('BORRADOR','CONTABILIZADO','ANULADO')),
    CONSTRAINT ck_asiento_totales CHECK (total_debe >= 0 AND total_haber >= 0)

);

CREATE TABLE IF NOT EXISTS finanzas.asiento_contable_detalle (
    asiento_contable_detalle_id BIGSERIAL PRIMARY KEY,
    asiento_contable_id         BIGINT NOT NULL REFERENCES finanzas.asiento_contable(asiento_contable_id),
    plan_cuenta_id              BIGINT NOT NULL REFERENCES finanzas.plan_cuenta(plan_cuenta_id),
    debe                        NUMERIC(14,2) NOT NULL DEFAULT 0,
    haber                       NUMERIC(14,2) NOT NULL DEFAULT 0,
    descripcion                 VARCHAR(250),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  BIGINT,
    CONSTRAINT ck_asiento_detalle_importes CHECK (debe >= 0 AND haber >= 0 AND NOT (debe > 0 AND haber > 0))

);

-- ==========================================================================
-- 16. RR. HH.
-- ==========================================================================
CREATE TABLE IF NOT EXISTS rrhh.area (
    area_id                 BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_area_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_area_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS rrhh.cargo (
    cargo_id                BIGSERIAL PRIMARY KEY,
    area_id                 BIGINT NOT NULL REFERENCES rrhh.area(area_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_cargo_codigo UNIQUE (area_id, codigo),
    CONSTRAINT ck_cargo_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS rrhh.trabajador (
    trabajador_id           BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT UNIQUE REFERENCES security.usuario(usuario_id),
    cargo_id                BIGINT REFERENCES rrhh.cargo(cargo_id),
    codigo_trabajador       VARCHAR(40),
    tipo_documento          VARCHAR(10) NOT NULL DEFAULT 'DNI',
    numero_documento        VARCHAR(20) NOT NULL,
    nombres                 VARCHAR(120) NOT NULL,
    apellidos               VARCHAR(150) NOT NULL,
    email                   CITEXT,
    telefono                VARCHAR(30),
    direccion               VARCHAR(300),
    ubigeo                  VARCHAR(6),
    fecha_nacimiento        DATE,
    fecha_ingreso           DATE NOT NULL,
    fecha_cese              DATE,
    tipo_trabajador         VARCHAR(40) NOT NULL DEFAULT 'DEPENDIENTE',
    estado_laboral          VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_trabajador_codigo UNIQUE (codigo_trabajador),
    CONSTRAINT uq_trabajador_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_trabajador_tipo CHECK (tipo_trabajador IN ('DEPENDIENTE','PRACTICANTE','TERCERO','TEMPORAL','OTRO')),
    CONSTRAINT ck_trabajador_estado CHECK (estado_laboral IN ('ACTIVO','CESADO','SUSPENDIDO','VACACIONES','LICENCIA')),
    CONSTRAINT ck_trabajador_fechas CHECK (fecha_cese IS NULL OR fecha_cese >= fecha_ingreso)

);

CREATE TABLE IF NOT EXISTS rrhh.contrato_trabajador (
    contrato_trabajador_id  BIGSERIAL PRIMARY KEY,
    trabajador_id           BIGINT NOT NULL REFERENCES rrhh.trabajador(trabajador_id),
    tipo_contrato           VARCHAR(40) NOT NULL DEFAULT 'INDETERMINADO',
    fecha_inicio            DATE NOT NULL,
    fecha_fin               DATE,
    sueldo_base             NUMERIC(14,2),
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    jornada                 VARCHAR(40) NOT NULL DEFAULT 'TIEMPO_COMPLETO',
    horas_semanales         NUMERIC(5,2),
    estado                  VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_contrato_trabajador_inicio UNIQUE (trabajador_id, fecha_inicio),
    CONSTRAINT ck_contrato_tipo CHECK (tipo_contrato IN ('INDETERMINADO','PLAZO_FIJO','PRACTICAS','LOCACION','TERCERO','OTRO')),
    CONSTRAINT ck_contrato_jornada CHECK (jornada IN ('TIEMPO_COMPLETO','TIEMPO_PARCIAL','POR_TURNOS','OTRO')),
    CONSTRAINT ck_contrato_estado CHECK (estado IN ('VIGENTE','FINALIZADO','SUSPENDIDO','ANULADO')),
    CONSTRAINT ck_contrato_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT ck_contrato_sueldo CHECK (sueldo_base IS NULL OR sueldo_base >= 0)

);

CREATE TABLE IF NOT EXISTS rrhh.trabajador_sucursal (
    trabajador_sucursal_id  BIGSERIAL PRIMARY KEY,
    trabajador_id           BIGINT NOT NULL REFERENCES rrhh.trabajador(trabajador_id),
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id              BIGINT REFERENCES organizacion.almacen(almacen_id),
    caja_id                 BIGINT REFERENCES organizacion.caja(caja_id),
    fecha_inicio            DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin               DATE,
    es_principal            BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_trabajador_sucursal_inicio UNIQUE (trabajador_id, sucursal_id, fecha_inicio),
    CONSTRAINT ck_trabajador_sucursal_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_trabajador_sucursal_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)

);

CREATE TABLE IF NOT EXISTS rrhh.turno_laboral (
    turno_laboral_id        BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(120) NOT NULL,
    hora_inicio             TIME NOT NULL,
    hora_fin                TIME NOT NULL,
    tolerancia_minutos      INTEGER NOT NULL DEFAULT 0,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_turno_laboral_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_turno_laboral_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS rrhh.programacion_turno (
    programacion_turno_id   BIGSERIAL PRIMARY KEY,
    trabajador_id          BIGINT NOT NULL REFERENCES rrhh.trabajador(trabajador_id),
    sucursal_id             BIGINT NOT NULL REFERENCES organizacion.sucursal(sucursal_id),
    turno_laboral_id        BIGINT NOT NULL REFERENCES rrhh.turno_laboral(turno_laboral_id),
    fecha                   DATE NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADO',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_programacion_trabajador_fecha UNIQUE (trabajador_id, fecha),
    CONSTRAINT ck_programacion_estado CHECK (estado IN ('PROGRAMADO','ASISTIO','FALTO','DESCANSO','VACACIONES','PERMISO'))

);

CREATE TABLE IF NOT EXISTS rrhh.asistencia (
    asistencia_id           BIGSERIAL PRIMARY KEY,
    programacion_turno_id   BIGINT REFERENCES rrhh.programacion_turno(programacion_turno_id),
    trabajador_id          BIGINT NOT NULL REFERENCES rrhh.trabajador(trabajador_id),
    fecha                   DATE NOT NULL,
    hora_entrada            TIMESTAMPTZ,
    hora_salida             TIMESTAMPTZ,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_asistencia_trabajador_fecha UNIQUE (trabajador_id, fecha),
    CONSTRAINT ck_asistencia_estado CHECK (estado IN ('REGISTRADA','TARDANZA','COMPLETA','INCOMPLETA','FALTA','JUSTIFICADA'))

);

-- ==========================================================================
-- 17. CRM
-- ==========================================================================
CREATE TABLE IF NOT EXISTS crm.segmento_cliente (
    segmento_cliente_id     BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    criterio_json           JSONB,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_segmento_cliente_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_segmento_cliente_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS crm.cliente_segmento (
    cliente_segmento_id     BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    segmento_cliente_id     BIGINT NOT NULL REFERENCES crm.segmento_cliente(segmento_cliente_id),
    fecha_asignacion        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_cliente_segmento UNIQUE (cliente_id, segmento_cliente_id),
    CONSTRAINT ck_cliente_segmento_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS crm.consentimiento_cliente (
    consentimiento_cliente_id BIGSERIAL PRIMARY KEY,
    cliente_id                BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    tipo_consentimiento       VARCHAR(80) NOT NULL,
    aceptado                  BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_respuesta           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    canal                     VARCHAR(40),
    version_texto             VARCHAR(50),
    evidencia_url             TEXT,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                BIGINT,
    CONSTRAINT uq_consentimiento_cliente UNIQUE (cliente_id, tipo_consentimiento, version_texto)

);

CREATE TABLE IF NOT EXISTS crm.preferencia_cliente (
    preferencia_cliente_id  BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    categoria_id            BIGINT REFERENCES catalogo.categoria_producto(categoria_id),
    tipo_preferencia        VARCHAR(60) NOT NULL,
    valor                   VARCHAR(200),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT ck_preferencia_cliente_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS crm.interaccion_cliente (
    interaccion_cliente_id  BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    usuario_id              BIGINT REFERENCES security.usuario(usuario_id),
    canal                   VARCHAR(40) NOT NULL,
    tipo_interaccion        VARCHAR(80) NOT NULL,
    asunto                  VARCHAR(200),
    descripcion             TEXT,
    resultado               VARCHAR(80),
    fecha_interaccion       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT

);

CREATE TABLE IF NOT EXISTS crm.campania_marketing (
    campania_marketing_id   BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(50) NOT NULL,
    nombre                  VARCHAR(180) NOT NULL,
    descripcion             TEXT,
    tipo_campania           VARCHAR(60) NOT NULL,
    canal                   VARCHAR(40) NOT NULL,
    fecha_inicio            TIMESTAMPTZ NOT NULL,
    fecha_fin               TIMESTAMPTZ,
    presupuesto             NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_campania_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_campania_estado CHECK (estado IN ('BORRADOR','ACTIVA','PAUSADA','FINALIZADA','CANCELADA'))

);

CREATE TABLE IF NOT EXISTS crm.campania_segmento (
    campania_segmento_id    BIGSERIAL PRIMARY KEY,
    campania_marketing_id   BIGINT NOT NULL REFERENCES crm.campania_marketing(campania_marketing_id),
    segmento_cliente_id     BIGINT NOT NULL REFERENCES crm.segmento_cliente(segmento_cliente_id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT uq_campania_segmento UNIQUE (campania_marketing_id, segmento_cliente_id)

);

CREATE TABLE IF NOT EXISTS crm.cupon (
    cupon_id                BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    campania_marketing_id   BIGINT REFERENCES crm.campania_marketing(campania_marketing_id),
    codigo                  VARCHAR(80) NOT NULL,
    descripcion             TEXT,
    tipo_descuento          VARCHAR(30) NOT NULL,
    valor_descuento         NUMERIC(14,4) NOT NULL,
    monto_minimo            NUMERIC(14,2) NOT NULL DEFAULT 0,
    fecha_inicio            TIMESTAMPTZ NOT NULL,
    fecha_fin               TIMESTAMPTZ NOT NULL,
    uso_maximo              INTEGER,
    uso_actual              INTEGER NOT NULL DEFAULT 0,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_cupon_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_cupon_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_cupon_tipo CHECK (tipo_descuento IN ('PORCENTAJE','MONTO'))

);

CREATE TABLE IF NOT EXISTS crm.cupon_cliente (
    cupon_cliente_id        BIGSERIAL PRIMARY KEY,
    cupon_id                BIGINT NOT NULL REFERENCES crm.cupon(cupon_id),
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    estado                  VARCHAR(30) NOT NULL DEFAULT 'ASIGNADO',
    venta_id                BIGINT REFERENCES ventas.venta(venta_id),
    fecha_uso               TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT uq_cupon_cliente UNIQUE (cupon_id, cliente_id),
    CONSTRAINT ck_cupon_cliente_estado CHECK (estado IN ('ASIGNADO','USADO','VENCIDO','ANULADO'))

);

CREATE TABLE IF NOT EXISTS crm.cuenta_fidelizacion (
    cuenta_fidelizacion_id  BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    codigo                  VARCHAR(80) NOT NULL,
    puntos_actuales         NUMERIC(14,2) NOT NULL DEFAULT 0,
    puntos_acumulados       NUMERIC(14,2) NOT NULL DEFAULT 0,
    nivel                   VARCHAR(50) DEFAULT 'BASICO',
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_fidelizacion_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_fidelizacion_codigo UNIQUE (codigo),
    CONSTRAINT ck_fidelizacion_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS crm.movimiento_puntos (
    movimiento_puntos_id    BIGSERIAL PRIMARY KEY,
    cuenta_fidelizacion_id  BIGINT NOT NULL REFERENCES crm.cuenta_fidelizacion(cuenta_fidelizacion_id),
    venta_id                BIGINT REFERENCES ventas.venta(venta_id),
    tipo_movimiento         CHAR(1) NOT NULL,
    puntos                  NUMERIC(14,2) NOT NULL,
    concepto                VARCHAR(180) NOT NULL,
    fecha_movimiento        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_movimiento_puntos_tipo CHECK (tipo_movimiento IN ('E','S')),
    CONSTRAINT ck_movimiento_puntos_valor CHECK (puntos > 0)

);

CREATE TABLE IF NOT EXISTS crm.reclamo (
    reclamo_id              BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    venta_id                BIGINT REFERENCES ventas.venta(venta_id),
    numero_reclamo          VARCHAR(80) NOT NULL,
    tipo_reclamo            VARCHAR(60) NOT NULL,
    descripcion             TEXT NOT NULL,
    canal                   VARCHAR(40) NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    respuesta               TEXT,
    fecha_respuesta         TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_reclamo_numero UNIQUE (empresa_id, numero_reclamo),
    CONSTRAINT ck_reclamo_estado CHECK (estado IN ('REGISTRADO','EN_REVISION','RESPONDIDO','CERRADO','ANULADO'))

);

-- ==========================================================================
-- 18. CMR: AUTOGESTIÓN Y GESTIÓN DE RELACIÓN POR EL CLIENTE
-- ==========================================================================
CREATE TABLE IF NOT EXISTS cmr.perfil_autogestion (
    perfil_autogestion_id   BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    permite_historial_compras BOOLEAN NOT NULL DEFAULT TRUE,
    permite_recomendaciones BOOLEAN NOT NULL DEFAULT TRUE,
    canal_preferido         VARCHAR(40),
    frecuencia_contacto     VARCHAR(40),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_perfil_autogestion_cliente UNIQUE (cliente_id),
    CONSTRAINT ck_perfil_autogestion_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS cmr.solicitud_cliente (
    solicitud_cliente_id    BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    tipo_solicitud          VARCHAR(80) NOT NULL,
    asunto                  VARCHAR(200) NOT NULL,
    descripcion             TEXT,
    canal                   VARCHAR(40) NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    respuesta               TEXT,
    fecha_respuesta         TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_solicitud_cliente_estado CHECK (estado IN ('REGISTRADA','EN_ATENCION','ATENDIDA','RECHAZADA','CERRADA'))

);

CREATE TABLE IF NOT EXISTS cmr.historial_privacidad (
    historial_privacidad_id BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT NOT NULL REFERENCES ventas.cliente(cliente_id),
    accion                  VARCHAR(80) NOT NULL,
    detalle                 JSONB,
    canal                   VARCHAR(40),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT

);

CREATE TABLE IF NOT EXISTS cmr.indicador_retail (
    indicador_retail_id     BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    codigo                  VARCHAR(80) NOT NULL,
    nombre                  VARCHAR(180) NOT NULL,
    descripcion             TEXT,
    unidad_medida           VARCHAR(40),
    frecuencia              VARCHAR(40) NOT NULL DEFAULT 'MENSUAL',
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_indicador_retail_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_indicador_retail_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS cmr.valor_indicador (
    valor_indicador_id      BIGSERIAL PRIMARY KEY,
    indicador_retail_id     BIGINT NOT NULL REFERENCES cmr.indicador_retail(indicador_retail_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    periodo                 VARCHAR(20) NOT NULL,
    valor                   NUMERIC(18,4) NOT NULL,
    fuente                  VARCHAR(120),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT uq_valor_indicador UNIQUE (indicador_retail_id, sucursal_id, periodo)

);

CREATE TABLE IF NOT EXISTS cmr.meta_indicador (
    meta_indicador_id       BIGSERIAL PRIMARY KEY,
    indicador_retail_id     BIGINT NOT NULL REFERENCES cmr.indicador_retail(indicador_retail_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    periodo                 VARCHAR(20) NOT NULL,
    valor_meta              NUMERIC(18,4) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT uq_meta_indicador UNIQUE (indicador_retail_id, sucursal_id, periodo)

);

CREATE TABLE IF NOT EXISTS cmr.alerta_gestion (
    alerta_gestion_id       BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    tipo_alerta             VARCHAR(80) NOT NULL,
    titulo                  VARCHAR(180) NOT NULL,
    descripcion             TEXT,
    severidad               VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    estado                  VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_alerta_severidad CHECK (severidad IN ('BAJA','MEDIA','ALTA','CRITICA')),
    CONSTRAINT ck_alerta_estado CHECK (estado IN ('ABIERTA','EN_REVISION','CERRADA','DESCARTADA'))

);

-- ==========================================================================
-- 19. ERP: REPOSICIÓN Y GESTIÓN OPERATIVA
-- ==========================================================================
CREATE TABLE IF NOT EXISTS erp.politica_reposicion (
    politica_reposicion_id  BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id             BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id              BIGINT REFERENCES organizacion.almacen(almacen_id),
    producto_sku_id         BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    stock_minimo            NUMERIC(14,4) NOT NULL DEFAULT 0,
    stock_maximo            NUMERIC(14,4),
    punto_reorden           NUMERIC(14,4),
    dias_cobertura          INTEGER,
    proveedor_preferido_id  BIGINT REFERENCES proveedores.proveedor(proveedor_id),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_politica_reposicion UNIQUE (empresa_id, sucursal_id, almacen_id, producto_sku_id),
    CONSTRAINT ck_politica_reposicion_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_politica_reposicion_stock CHECK (stock_minimo >= 0 AND (stock_maximo IS NULL OR stock_maximo >= stock_minimo))

);

CREATE TABLE IF NOT EXISTS erp.sugerencia_reposicion (
    sugerencia_reposicion_id BIGSERIAL PRIMARY KEY,
    politica_reposicion_id   BIGINT REFERENCES erp.politica_reposicion(politica_reposicion_id),
    empresa_id               BIGINT NOT NULL REFERENCES organizacion.empresa(empresa_id),
    sucursal_id              BIGINT REFERENCES organizacion.sucursal(sucursal_id),
    almacen_id               BIGINT REFERENCES organizacion.almacen(almacen_id),
    producto_sku_id          BIGINT NOT NULL REFERENCES catalogo.producto_sku(producto_sku_id),
    stock_actual             NUMERIC(14,4) NOT NULL DEFAULT 0,
    cantidad_sugerida        NUMERIC(14,4) NOT NULL,
    motivo                   VARCHAR(200),
    estado                   VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_at               TIMESTAMPTZ,
    updated_by               BIGINT,
    CONSTRAINT ck_sugerencia_reposicion_cantidad CHECK (cantidad_sugerida > 0),
    CONSTRAINT ck_sugerencia_reposicion_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA','CONVERTIDA_OC'))

);

-- ==========================================================================
-- 20. LOGÍSTICA Y DELIVERY
-- ==========================================================================
CREATE TABLE IF NOT EXISTS logistica.entrega_pedido (
    entrega_pedido_id       BIGSERIAL PRIMARY KEY,
    pedido_digital_id       BIGINT NOT NULL REFERENCES ventas.pedido_digital(pedido_digital_id),
    repartidor_usuario_id   BIGINT REFERENCES security.usuario(usuario_id),
    tipo_entrega            VARCHAR(30) NOT NULL DEFAULT 'DELIVERY',
    estado                  VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE',
    direccion_entrega       VARCHAR(300),
    latitud                 NUMERIC(10,7),
    longitud                NUMERIC(10,7),
    fecha_asignacion        TIMESTAMPTZ,
    fecha_salida            TIMESTAMPTZ,
    fecha_entrega           TIMESTAMPTZ,
    evidencia_url           TEXT,
    observacion             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_entrega_pedido UNIQUE (pedido_digital_id),
    CONSTRAINT ck_entrega_pedido_tipo CHECK (tipo_entrega IN ('DELIVERY','RECOJO_TIENDA')),
    CONSTRAINT ck_entrega_pedido_estado CHECK (estado IN ('PENDIENTE','ASIGNADO','EN_PREPARACION','EN_RUTA','ENTREGADO','FALLIDO','CANCELADO'))

);

CREATE TABLE IF NOT EXISTS logistica.entrega_evento (
    entrega_evento_id       BIGSERIAL PRIMARY KEY,
    entrega_pedido_id       BIGINT NOT NULL REFERENCES logistica.entrega_pedido(entrega_pedido_id),
    estado                  VARCHAR(40) NOT NULL,
    descripcion             TEXT,
    latitud                 NUMERIC(10,7),
    longitud                NUMERIC(10,7),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT

);

-- ==========================================================================
-- 21. PAGOS DIGITALES
-- ==========================================================================
CREATE TABLE IF NOT EXISTS pagos.intento_pago (
    intento_pago_id         BIGSERIAL PRIMARY KEY,
    pedido_digital_id       BIGINT REFERENCES ventas.pedido_digital(pedido_digital_id),
    venta_id                BIGINT REFERENCES ventas.venta(venta_id),
    pasarela                VARCHAR(80) NOT NULL,
    moneda                  VARCHAR(10) NOT NULL DEFAULT 'PEN',
    monto                   NUMERIC(14,2) NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'CREADO',
    referencia_externa      VARCHAR(180),
    payload_request         JSONB,
    payload_response        JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_intento_pago_monto CHECK (monto > 0),
    CONSTRAINT ck_intento_pago_estado CHECK (estado IN ('CREADO','PENDIENTE','APROBADO','RECHAZADO','EXPIRADO','CANCELADO','EXTORNADO'))

);

CREATE TABLE IF NOT EXISTS pagos.transaccion_pago (
    transaccion_pago_id     BIGSERIAL PRIMARY KEY,
    intento_pago_id         BIGINT NOT NULL REFERENCES pagos.intento_pago(intento_pago_id),
    codigo_transaccion      VARCHAR(180),
    autorizacion            VARCHAR(120),
    estado                  VARCHAR(30) NOT NULL,
    monto                   NUMERIC(14,2) NOT NULL,
    fecha_transaccion       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata                JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_transaccion_pago_monto CHECK (monto > 0)

);

-- ==========================================================================
-- 22. NOTIFICACIONES WEB, EMAIL, SMS Y PUSH
-- ==========================================================================
CREATE TABLE IF NOT EXISTS notificaciones.plantilla_notificacion (
    plantilla_notificacion_id BIGSERIAL PRIMARY KEY,
    empresa_id                BIGINT REFERENCES organizacion.empresa(empresa_id),
    codigo                    VARCHAR(80) NOT NULL,
    nombre                    VARCHAR(160) NOT NULL,
    canal                     VARCHAR(40) NOT NULL,
    asunto                    VARCHAR(250),
    cuerpo                    TEXT NOT NULL,
    cuerpo_html               TEXT,
    variables                 JSONB,
    estado                    CHAR(1) NOT NULL DEFAULT 'A',
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                BIGINT,
    updated_at                TIMESTAMPTZ,
    updated_by                BIGINT,
    deleted_at                TIMESTAMPTZ,
    deleted_by                BIGINT,
    CONSTRAINT uq_plantilla_notificacion_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT ck_plantilla_notificacion_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS notificaciones.notificacion (
    notificacion_id         BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT REFERENCES ventas.cliente(cliente_id),
    usuario_id              BIGINT REFERENCES security.usuario(usuario_id),
    plantilla_notificacion_id BIGINT REFERENCES notificaciones.plantilla_notificacion(plantilla_notificacion_id),
    canal                   VARCHAR(40) NOT NULL,
    destinatario            VARCHAR(250) NOT NULL,
    asunto                  VARCHAR(250),
    mensaje                 TEXT NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada        TIMESTAMPTZ,
    fecha_envio             TIMESTAMPTZ,
    error_mensaje           TEXT,
    metadata                JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_notificacion_estado CHECK (estado IN ('PENDIENTE','ENVIADA','LEIDA','ERROR','CANCELADA'))

);

-- ==========================================================================
-- 23. SINCRONIZACIÓN MÓVIL, FEATURE FLAGS Y VERSIONES
-- ==========================================================================
CREATE TABLE IF NOT EXISTS app.sincronizacion_lote (
    sincronizacion_lote_id  BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT REFERENCES security.usuario(usuario_id),
    dispositivo_cliente_id  BIGINT REFERENCES app.dispositivo_cliente(dispositivo_cliente_id),
    modulo                  VARCHAR(80) NOT NULL,
    tipo_sincronizacion     VARCHAR(40) NOT NULL,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    total_registros         INTEGER NOT NULL DEFAULT 0,
    registros_exitosos      INTEGER NOT NULL DEFAULT 0,
    registros_error         INTEGER NOT NULL DEFAULT 0,
    started_at              TIMESTAMPTZ,
    finished_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT ck_sync_tipo CHECK (tipo_sincronizacion IN ('FULL','DELTA','UPLOAD','DOWNLOAD')),
    CONSTRAINT ck_sync_estado CHECK (estado IN ('PENDIENTE','EN_PROCESO','COMPLETADO','ERROR','CANCELADO'))

);

CREATE TABLE IF NOT EXISTS app.sincronizacion_conflicto (
    sincronizacion_conflicto_id BIGSERIAL PRIMARY KEY,
    sincronizacion_lote_id      BIGINT NOT NULL REFERENCES app.sincronizacion_lote(sincronizacion_lote_id),
    entidad                     VARCHAR(120) NOT NULL,
    entidad_id                  VARCHAR(120),
    dato_local                  JSONB,
    dato_servidor               JSONB,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    resolucion                  TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ,
    updated_by                  BIGINT,
    CONSTRAINT ck_sync_conflicto_estado CHECK (estado IN ('PENDIENTE','RESUELTO_LOCAL','RESUELTO_SERVIDOR','RESUELTO_MANUAL','IGNORADO'))

);

CREATE TABLE IF NOT EXISTS app.feature_flag (
    feature_flag_id         BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(80) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    descripcion             TEXT,
    habilitado              BOOLEAN NOT NULL DEFAULT FALSE,
    canal                   VARCHAR(40),
    reglas                  JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_feature_flag_codigo UNIQUE (codigo)

);

CREATE TABLE IF NOT EXISTS app.app_version (
    app_version_id          BIGSERIAL PRIMARY KEY,
    plataforma              VARCHAR(30) NOT NULL,
    version                 VARCHAR(50) NOT NULL,
    build_number            VARCHAR(50),
    es_obligatoria          BOOLEAN NOT NULL DEFAULT FALSE,
    url_descarga            TEXT,
    notas                   TEXT,
    fecha_publicacion       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_app_version UNIQUE (plataforma, version, build_number),
    CONSTRAINT ck_app_version_plataforma CHECK (plataforma IN ('ANDROID','IOS','WEB','PWA')),
    CONSTRAINT ck_app_version_estado CHECK (estado IN ('A','I'))

);

-- ==========================================================================
-- 24. INTEGRACIONES EXTERNAS: SUNAT, DIGEMID, PASARELAS, ETC.
-- ==========================================================================
CREATE TABLE IF NOT EXISTS integracion.servicio_externo (
    servicio_externo_id     BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(80) NOT NULL,
    nombre                  VARCHAR(160) NOT NULL,
    tipo_servicio           VARCHAR(60) NOT NULL,
    base_url                TEXT,
    requiere_auth           BOOLEAN NOT NULL DEFAULT TRUE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_servicio_externo_codigo UNIQUE (codigo),
    CONSTRAINT ck_servicio_externo_estado CHECK (estado IN ('A','I'))

);

CREATE TABLE IF NOT EXISTS integracion.log_integracion (
    log_integracion_id      BIGSERIAL PRIMARY KEY,
    servicio_externo_id     BIGINT REFERENCES integracion.servicio_externo(servicio_externo_id),
    correlacion_id          UUID NOT NULL DEFAULT gen_random_uuid(),
    metodo                  VARCHAR(20),
    endpoint                TEXT,
    request_payload         JSONB,
    response_payload        JSONB,
    status_code             INTEGER,
    estado                  VARCHAR(30) NOT NULL DEFAULT 'OK',
    duracion_ms             INTEGER,
    error_mensaje           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    CONSTRAINT ck_log_integracion_estado CHECK (estado IN ('OK','ERROR','TIMEOUT','REINTENTO'))

);

-- ==========================================================================
-- 25. ÍNDICES RECOMENDADOS
-- ==========================================================================
-- Organización
CREATE INDEX IF NOT EXISTS idx_sucursal_empresa ON organizacion.sucursal(empresa_id);
CREATE INDEX IF NOT EXISTS idx_almacen_sucursal ON organizacion.almacen(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_caja_sucursal ON organizacion.caja(sucursal_id);

-- Seguridad
CREATE INDEX IF NOT EXISTS idx_usuario_estado ON security.usuario(estado);
CREATE INDEX IF NOT EXISTS idx_usuario_rol_usuario ON security.usuario_rol_sucursal(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_rol_sucursal ON security.usuario_rol_sucursal(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_permiso_modulo ON security.permiso(modulo_id);
CREATE INDEX IF NOT EXISTS idx_sesion_usuario ON security.sesion_usuario(usuario_id, estado);

-- Catálogo
CREATE INDEX IF NOT EXISTS idx_producto_base_categoria ON catalogo.producto_base(categoria_id);
CREATE INDEX IF NOT EXISTS idx_producto_base_nombre ON catalogo.producto_base USING gin (to_tsvector('spanish', coalesce(nombre,'') || ' ' || coalesce(descripcion,'')));
CREATE INDEX IF NOT EXISTS idx_producto_sku_producto ON catalogo.producto_sku(producto_base_id);
CREATE INDEX IF NOT EXISTS idx_producto_sku_nombre ON catalogo.producto_sku USING gin (to_tsvector('spanish', coalesce(nombre_comercial,'') || ' ' || coalesce(presentacion,'')));
CREATE INDEX IF NOT EXISTS idx_codigo_barra_producto ON catalogo.codigo_barra(producto_sku_id);

-- Farmacia
CREATE INDEX IF NOT EXISTS idx_producto_farm_registro ON farmacia.producto_farmaceutico(registro_sanitario);
CREATE INDEX IF NOT EXISTS idx_producto_principio_activo ON farmacia.producto_principio_activo(principio_activo_id);

-- Inventario
CREATE INDEX IF NOT EXISTS idx_lote_producto_sku ON inventario.lote_producto(producto_sku_id);
CREATE INDEX IF NOT EXISTS idx_lote_producto_vencimiento ON inventario.lote_producto(fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_inventario_almacen_sku ON inventario.inventario_lote(almacen_id, producto_sku_id);
CREATE INDEX IF NOT EXISTS idx_kardex_fecha ON inventario.kardex_movimiento(fecha_movimiento);
CREATE INDEX IF NOT EXISTS idx_kardex_sku ON inventario.kardex_movimiento(producto_sku_id);
CREATE INDEX IF NOT EXISTS idx_reserva_stock_estado ON inventario.reserva_stock(estado, expira_at);

-- Compras
CREATE INDEX IF NOT EXISTS idx_compra_proveedor ON compras.compra(proveedor_id);
CREATE INDEX IF NOT EXISTS idx_compra_fecha ON compras.compra(fecha_recepcion);
CREATE INDEX IF NOT EXISTS idx_compra_detalle_sku ON compras.compra_detalle(producto_sku_id);

-- Ventas
CREATE INDEX IF NOT EXISTS idx_cliente_documento ON ventas.cliente(tipo_documento, numero_documento);
CREATE INDEX IF NOT EXISTS idx_cliente_nombre ON ventas.cliente USING gin (to_tsvector('spanish', coalesce(nombres,'') || ' ' || coalesce(apellidos,'') || ' ' || coalesce(razon_social,'')));
CREATE INDEX IF NOT EXISTS idx_venta_fecha ON ventas.venta(fecha_venta);
CREATE INDEX IF NOT EXISTS idx_venta_sucursal_fecha ON ventas.venta(sucursal_id, fecha_venta);
CREATE INDEX IF NOT EXISTS idx_venta_cliente ON ventas.venta(cliente_id);
CREATE INDEX IF NOT EXISTS idx_venta_detalle_sku ON ventas.venta_detalle(producto_sku_id);
CREATE INDEX IF NOT EXISTS idx_comprobante_estado_sunat ON ventas.comprobante_electronico(estado_sunat);
CREATE INDEX IF NOT EXISTS idx_pedido_digital_estado ON ventas.pedido_digital(estado);
CREATE INDEX IF NOT EXISTS idx_pedido_digital_cliente ON ventas.pedido_digital(cliente_id);

-- Finanzas
CREATE INDEX IF NOT EXISTS idx_cxc_estado ON finanzas.cuenta_por_cobrar(estado, fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_cxp_estado ON finanzas.cuenta_por_pagar(estado, fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_movimiento_caja_fecha ON finanzas.movimiento_caja(fecha_movimiento);


-- RR. HH.
CREATE INDEX IF NOT EXISTS idx_trabajador_usuario ON rrhh.trabajador(usuario_id);
CREATE INDEX IF NOT EXISTS idx_trabajador_documento ON rrhh.trabajador(tipo_documento, numero_documento);
CREATE INDEX IF NOT EXISTS idx_trabajador_estado ON rrhh.trabajador(estado_laboral);
CREATE INDEX IF NOT EXISTS idx_trabajador_sucursal ON rrhh.trabajador_sucursal(sucursal_id, estado);
CREATE INDEX IF NOT EXISTS idx_contrato_trabajador ON rrhh.contrato_trabajador(trabajador_id, estado);
CREATE INDEX IF NOT EXISTS idx_programacion_trabajador_fecha ON rrhh.programacion_turno(trabajador_id, fecha);
CREATE INDEX IF NOT EXISTS idx_asistencia_trabajador_fecha ON rrhh.asistencia(trabajador_id, fecha);

-- CRM/CMR
CREATE INDEX IF NOT EXISTS idx_interaccion_cliente ON crm.interaccion_cliente(cliente_id, fecha_interaccion);
CREATE INDEX IF NOT EXISTS idx_reclamo_estado ON crm.reclamo(estado);
CREATE INDEX IF NOT EXISTS idx_solicitud_cliente_estado ON cmr.solicitud_cliente(estado);
CREATE INDEX IF NOT EXISTS idx_valor_indicador_periodo ON cmr.valor_indicador(periodo);

-- Logística, pagos, notificaciones e integración
CREATE INDEX IF NOT EXISTS idx_entrega_pedido_estado ON logistica.entrega_pedido(estado);
CREATE INDEX IF NOT EXISTS idx_intento_pago_estado ON pagos.intento_pago(estado);
CREATE INDEX IF NOT EXISTS idx_notificacion_estado ON notificaciones.notificacion(estado, fecha_programada);
CREATE INDEX IF NOT EXISTS idx_log_integracion_created ON integracion.log_integracion(created_at);

-- ==========================================================================
-- 26. TRIGGERS UPDATED_AT PARA TABLAS CON COLUMNA updated_at
-- ==========================================================================
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT table_schema, table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_at'
          AND table_schema IN (
              'organizacion','security','auditoria','catalogo','farmacia','proveedores',
              'compras','inventario','ventas','finanzas','rrhh','crm','cmr','erp',
              'app','logistica','pagos','notificaciones','integracion'
          )
        GROUP BY table_schema, table_name
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_set_updated_at ON %I.%I', r.table_schema, r.table_name);
        EXECUTE format(
            'CREATE TRIGGER trg_set_updated_at BEFORE UPDATE ON %I.%I FOR EACH ROW EXECUTE FUNCTION auditoria.fn_set_updated_at()',
            r.table_schema, r.table_name
        );
    END LOOP;
END $$;

-- ==========================================================================
-- 27. COMENTARIOS GENERALES
-- ==========================================================================
COMMENT ON SCHEMA security IS 'Seguridad RBAC: usuarios, roles, permisos, sesiones y accesos por sucursal/almacén/caja.';
COMMENT ON SCHEMA auditoria IS 'Auditoría funcional y técnica de eventos críticos.';
COMMENT ON SCHEMA catalogo IS 'Catálogo comercial genérico para productos farmacéuticos y no farmacéuticos.';
COMMENT ON SCHEMA farmacia IS 'Extensión sanitaria/farmacéutica: DIGEMID, registro sanitario, laboratorio, forma y condición de venta.';
COMMENT ON SCHEMA inventario IS 'Inventario por sucursal, almacén, lote, vencimiento, reservas y kardex.';
COMMENT ON SCHEMA ventas IS 'Ventas POS, ecommerce, app móvil, pedidos, comprobantes y devoluciones.';
COMMENT ON SCHEMA rrhh IS 'RR. HH.: trabajadores, cargos, contratos, asignaciones por sucursal/caja, turnos y asistencia.';
COMMENT ON SCHEMA crm IS 'CRM: segmentación, campañas, cupones, fidelización, reclamos e interacciones.';
COMMENT ON SCHEMA cmr IS 'CMR: autogestión, privacidad, solicitudes e indicadores gerenciales.';
COMMENT ON SCHEMA app IS 'Soporte para app web, app móvil, cuentas digitales, dispositivos, sesiones, sincronización y feature flags.';


COMMENT ON TABLE rrhh.trabajador IS 'Tabla principal de trabajadores/empleados. No todo trabajador tiene usuario del sistema; usuario_id se llena solo si requiere login.';
COMMENT ON TABLE rrhh.contrato_trabajador IS 'Historial contractual del trabajador. No se elimina lógicamente; se finaliza o anula por estado.';
COMMENT ON TABLE rrhh.trabajador_sucursal IS 'Asignación operativa del trabajador a sucursal, almacén o caja con vigencia.';
COMMENT ON COLUMN rrhh.trabajador.usuario_id IS 'Relación opcional con security.usuario para trabajadores que acceden al ERP/POS/app interna.';

COMMIT;
