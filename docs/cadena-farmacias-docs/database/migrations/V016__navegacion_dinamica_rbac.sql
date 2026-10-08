-- Fuente: cadena_farmacias_postgresql18.sql
-- Migraci?n modular V016.

-- ============================================================================
-- 16. ESQUEMA: sch_app (Menú de Navegación, Notificaciones, Flags y Versiones)
-- ============================================================================

CREATE TABLE sch_app.menu_navegacion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico        UUID NOT NULL DEFAULT uuidv7(),
    modulo_id           BIGINT,
    menu_padre_id       BIGINT,
    codigo              VARCHAR(80) NOT NULL,
    aplicacion          VARCHAR(40) NOT NULL DEFAULT 'ERP_WEB',
    etiqueta            VARCHAR(120) NOT NULL,
    descripcion         VARCHAR(500),
    ruta                VARCHAR(240),
    icono               VARCHAR(80),
    tipo                VARCHAR(20) NOT NULL DEFAULT 'ITEM',
    modo_autorizacion   VARCHAR(20) NOT NULL DEFAULT 'TODOS',
    orden               INTEGER NOT NULL DEFAULT 0,
    es_visible          BOOLEAN NOT NULL DEFAULT TRUE,
    es_activo           CHAR(1) NOT NULL DEFAULT '1',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(15),
    CONSTRAINT pk_app_menu PRIMARY KEY (id),
    CONSTRAINT uk_app_menu_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_app_menu_modulo FOREIGN KEY (modulo_id) REFERENCES sch_seguridad.modulo_sistema(id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_menu_padre FOREIGN KEY (menu_padre_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_aplicacion CHECK (aplicacion IN ('ERP_WEB', 'POS_WEB', 'ECOMMERCE_WEB', 'MOBILE')),
    CONSTRAINT ck_app_menu_tipo CHECK (tipo IN ('GRUPO', 'ITEM', 'SEPARADOR')),
    CONSTRAINT ck_app_menu_auth CHECK (modo_autorizacion IN ('AUTENTICADO', 'CUALQUIERA', 'TODOS')),
    CONSTRAINT ck_app_menu_orden CHECK (orden >= 0),
    CONSTRAINT ck_app_menu_ruta CHECK (ruta IS NULL OR LEFT(ruta, 1) = '/'),
    CONSTRAINT ck_app_menu_tipo_ruta CHECK ((tipo = 'ITEM' AND ruta IS NOT NULL) OR (tipo IN ('GRUPO', 'SEPARADOR') AND ruta IS NULL)),
    CONSTRAINT ck_app_menu_padre CHECK (menu_padre_id IS NULL OR menu_padre_id <> id),
    CONSTRAINT ck_app_menu_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_app_menu_aplicacion_codigo ON sch_app.menu_navegacion(aplicacion, codigo) WHERE es_activo = '1';
CREATE UNIQUE INDEX uk_app_menu_ruta_activa ON sch_app.menu_navegacion(aplicacion, ruta) WHERE ruta IS NOT NULL AND es_activo = '1';
CREATE INDEX ix_app_menu_padre_orden ON sch_app.menu_navegacion(aplicacion, menu_padre_id, orden, etiqueta) WHERE es_activo = '1' AND es_visible = TRUE;

CREATE TABLE sch_app.menu_navegacion_permiso (
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    menu_id     BIGINT NOT NULL,
    permiso_id  BIGINT NOT NULL,
    es_activo   CHAR(1) NOT NULL DEFAULT '1',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(15) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_app_menu_permiso PRIMARY KEY (id),
    CONSTRAINT fk_app_menu_permiso_menu FOREIGN KEY (menu_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_menu_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES sch_seguridad.permiso(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_permiso_es_activo CHECK (es_activo IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_app_menu_permiso ON sch_app.menu_navegacion_permiso(menu_id, permiso_id) WHERE es_activo = '1';
CREATE INDEX ix_app_menu_permiso_busqueda ON sch_app.menu_navegacion_permiso(permiso_id) WHERE es_activo = '1';

CREATE OR REPLACE FUNCTION sch_app.fn_validar_menu_jerarquia()
RETURNS TRIGGER AS $$
DECLARE
    app_padre  VARCHAR(40);
    tipo_padre VARCHAR(20);
BEGIN
    IF NEW.id IS NOT NULL AND NEW.menu_padre_id = NEW.id THEN
        RAISE EXCEPTION 'Un menú no puede ser padre de sí mismo (ID: %)', NEW.id;
    END IF;

    IF NEW.menu_padre_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT aplicacion, tipo
      INTO app_padre, tipo_padre
      FROM sch_app.menu_navegacion
     WHERE id = NEW.menu_padre_id
       AND es_activo = '1';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El menú padre con ID % no existe o no está activo', NEW.menu_padre_id;
    END IF;

    IF app_padre <> NEW.aplicacion THEN
        RAISE EXCEPTION 'El menú padre e hijo deben pertenecer a la misma aplicación (Padre: %, Hijo: %)', app_padre, NEW.aplicacion;
    END IF;

    IF tipo_padre <> 'GRUPO' THEN
        RAISE EXCEPTION 'Solo un menú de tipo GRUPO puede contener sub-ítems (Tipo actual: %)', tipo_padre;
    END IF;

    IF EXISTS (
        WITH RECURSIVE arbol_ancestros AS (
            SELECT id, menu_padre_id
              FROM sch_app.menu_navegacion
             WHERE id = NEW.menu_padre_id
            UNION ALL
            SELECT p.id, p.menu_padre_id
              FROM sch_app.menu_navegacion p
              JOIN arbol_ancestros a ON p.id = a.menu_padre_id
        )
        SELECT 1 FROM arbol_ancestros WHERE id = NEW.id
    ) THEN
        RAISE EXCEPTION 'La relación de jerarquía genera un ciclo circular infinito';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_app_menu_jerarquia
    BEFORE INSERT OR UPDATE ON sch_app.menu_navegacion
    FOR EACH ROW EXECUTE FUNCTION sch_app.fn_validar_menu_jerarquia();

-- ============================================================================
-- 17. SEED DATA FUNDACIONAL (Módulos, Permisos y Menús Base)
-- ============================================================================

INSERT INTO sch_seguridad.modulo_sistema(codigo, nombre, descripcion, orden, es_activo) VALUES
('SEGURIDAD',        'Seguridad',               'Usuarios, roles, permisos y ámbitos.', 10, '1'),
('ORGANIZACION',     'Organización',            'Empresas, establecimientos, almacenes y terminales.', 20, '1'),
('CATALOGO',         'Catálogo',                'Productos regulatorios y SKU comerciales.', 30, '1'),
('COMPRAS',          'Compras',                 'Solicitudes, órdenes, recepción y proveedores.', 40, '1'),
('INVENTARIO',       'Inventario',              'Stock, lotes, reservas, transferencias y conteos.', 50, '1'),
('PRECIOS',          'Precios y Promociones',   'Listas de precios y promociones.', 60, '1'),
('VENTAS',           'Retail / POS',            'Caja, ventas, pagos y devoluciones.', 70, '1'),
('DISPENSACION',     'Dispensación',            'Prescripciones y dispensación farmacéutica.', 80, '1'),
('CONTROLADOS',      'Controlados',             'Recetas y balances de psicotrópicos/estupefacientes.', 90, '1'),
('FISCAL',           'Fiscal / CPE',            'Comprobantes electrónicos y notas de crédito.', 100, '1'),
('RECALL',           'Recall y Seguridad',      'Alertas sanitarias, inmovilización y retiros.', 110, '1'),
('FARMACOVIGILANCIA','Farmacovigilancia',       'Reportes y seguimientos de seguridad (RAMs).', 120, '1'),
('ERP',              'ERP Financiero',          'CxP/CxC, tesorería y posting contable.', 130, '1'),
('REPORTES',         'Reportes e Inteligencia', 'Observatorio de Precios DIGEMID y BI.', 140, '1')
ON CONFLICT (codigo) WHERE es_activo = '1' DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    orden = EXCLUDED.orden,
    es_activo = '1';

INSERT INTO sch_seguridad.permiso(modulo_id, codigo, recurso, accion, nombre, descripcion, es_activo)
SELECT m.id,
       m.codigo || ':MODULO:VER',
       'MODULO',
       'VER',
       'Ver ' || m.nombre,
       'Permiso de visibilidad y acceso básico del módulo.',
       '1'
  FROM sch_seguridad.modulo_sistema m
ON CONFLICT (codigo) WHERE es_activo = '1' DO NOTHING;

INSERT INTO sch_app.menu_navegacion(codigo, aplicacion, etiqueta, descripcion, tipo, modo_autorizacion, orden, es_visible, es_activo)
VALUES
('OPERACIONES',    'ERP_WEB', 'Operaciones',    'Operación comercial, inventario y farmacia.', 'GRUPO', 'AUTENTICADO', 10, TRUE, '1'),
('ADMINISTRACION', 'ERP_WEB', 'Administración', 'Configuración, seguridad y organización.',    'GRUPO', 'AUTENTICADO', 20, TRUE, '1')
ON CONFLICT (aplicacion, codigo) WHERE es_activo = '1' DO UPDATE
SET etiqueta = EXCLUDED.etiqueta,
    descripcion = EXCLUDED.descripcion,
    tipo = EXCLUDED.tipo,
    modo_autorizacion = EXCLUDED.modo_autorizacion,
    orden = EXCLUDED.orden,
    es_visible = TRUE,
    es_activo = '1';

WITH items(modulo_codigo, padre, codigo, etiqueta, descripcion, ruta, icono, orden) AS (
VALUES
(NULL,               'OPERACIONES',    'DASHBOARD',        'Resumen',          'Resumen operativo general.',          '/dashboard',        'LayoutDashboard',      10),
('CATALOGO',         'OPERACIONES',    'CATALOGO',         'Catálogo',         'Productos, SKUs y maestros.',         '/catalogo',         'PackageSearch',        20),
('INVENTARIO',       'OPERACIONES',    'INVENTARIO',       'Inventario',       'Stock por almacén, lotes y FEFO.',    '/inventario',       'Boxes',                30),
('COMPRAS',          'OPERACIONES',    'COMPRAS',          'Compras',          'Proveedores, órdenes y recepción.',   '/compras',          'ShoppingCart',         40),
('PRECIOS',          'OPERACIONES',    'PRECIOS',          'Precios',          'Listas de precio y promociones.',     '/precios',          'Tags',                 50),
('VENTAS',           'OPERACIONES',    'VENTAS',           'Ventas',           'Historial de ventas y devoluciones.', '/ventas',           'ReceiptText',          60),
('VENTAS',           'OPERACIONES',    'POS',              'Punto de Venta',   'Terminal de venta y caja rápida.',    '/pos',              'MonitorSmartphone',    70),
('DISPENSACION',     'OPERACIONES',    'DISPENSACION',     'Dispensación',     'Prescripción y atención clínica.',    '/dispensacion',     'Pill',                 80),
('CONTROLADOS',      'OPERACIONES',    'CONTROLADOS',      'Controlados',      'Recetas especiales y balances.',      '/controlados',      'ShieldAlert',          90),
('RECALL',           'OPERACIONES',    'RECALL',           'Recall Sanitario', 'Alertas y retiros de mercado.',       '/recall',           'Siren',                100),
('FARMACOVIGILANCIA','OPERACIONES',    'FARMACOVIGILANCIA','Farmacovigilancia','Reportes de seguridad y RAMs.',       '/farmacovigilancia','HeartPulse',           110),
('ERP',              'OPERACIONES',    'ERP',              'ERP Financiero',   'CxP, CxC y tesorería.',               '/erp',              'Landmark',             120),
('REPORTES',         'OPERACIONES',    'REPORTES',         'Reportes',         'Observatorio DIGEMID e informes.',    '/reportes',         'ChartNoAxesCombined',  130),
('SEGURIDAD',        'ADMINISTRACION', 'SEGURIDAD',        'Seguridad',        'Usuarios, roles y accesos.',          '/seguridad',        'ShieldCheck',          10),
('ORGANIZACION',     'ADMINISTRACION', 'ORGANIZACION',     'Organización',     'Empresas, boticas y almacenes.',      '/organizacion',     'Building2',            20)
)
INSERT INTO sch_app.menu_navegacion(
    modulo_id, menu_padre_id, codigo, aplicacion, etiqueta, descripcion, ruta, icono, tipo, modo_autorizacion, orden, es_visible, es_activo
)
SELECT m.id,
       p.id,
       i.codigo,
       'ERP_WEB',
       i.etiqueta,
       i.descripcion,
       i.ruta,
       i.icono,
       'ITEM',
       CASE WHEN i.modulo_codigo IS NULL THEN 'AUTENTICADO' ELSE 'TODOS' END,
       i.orden,
       TRUE,
       '1'
  FROM items i
  JOIN sch_app.menu_navegacion p ON p.aplicacion = 'ERP_WEB' AND p.codigo = i.padre
  LEFT JOIN sch_seguridad.modulo_sistema m ON m.codigo = i.modulo_codigo
ON CONFLICT (aplicacion, codigo) WHERE es_activo = '1' DO UPDATE
SET modulo_id = EXCLUDED.modulo_id,
    menu_padre_id = EXCLUDED.menu_padre_id,
    etiqueta = EXCLUDED.etiqueta,
    descripcion = EXCLUDED.descripcion,
    ruta = EXCLUDED.ruta,
    icono = EXCLUDED.icono,
    orden = EXCLUDED.orden,
    es_visible = TRUE,
    es_activo = '1';

INSERT INTO sch_app.menu_navegacion_permiso(menu_id, permiso_id, es_activo)
SELECT mn.id, p.id, '1'
  FROM sch_app.menu_navegacion mn
  JOIN sch_seguridad.modulo_sistema m ON m.id = mn.modulo_id
  JOIN sch_seguridad.permiso p ON p.modulo_id = m.id AND p.codigo = m.codigo || ':MODULO:VER'
 WHERE mn.aplicacion = 'ERP_WEB'
   AND mn.modulo_id IS NOT NULL
ON CONFLICT (menu_id, permiso_id) WHERE es_activo = '1' DO UPDATE
SET es_activo = '1';
