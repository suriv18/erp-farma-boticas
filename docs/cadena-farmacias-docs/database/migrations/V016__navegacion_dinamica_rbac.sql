-- Navegación dinámica desacoplada de la autorización real.
-- Adaptado del V2 aportado: el menú determina visibilidad/navegación; cada endpoint vuelve a autorizar.

CREATE TABLE sch_app.menu_navegacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    modulo_id BIGINT,
    menu_padre_id BIGINT,
    codigo VARCHAR(80) NOT NULL,
    aplicacion VARCHAR(40) NOT NULL DEFAULT 'ERP_WEB',
    etiqueta VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    ruta VARCHAR(240),
    icono VARCHAR(80),
    tipo VARCHAR(20) NOT NULL DEFAULT 'ITEM',
    modo_autorizacion VARCHAR(20) NOT NULL DEFAULT 'TODOS',
    orden INTEGER NOT NULL DEFAULT 0,
    es_visible BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_app_menu PRIMARY KEY (id),
    CONSTRAINT uk_app_menu_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_app_menu_aplicacion_codigo UNIQUE (aplicacion, codigo),
    CONSTRAINT fk_app_menu_modulo FOREIGN KEY (modulo_id) REFERENCES sch_seguridad.modulo_sistema(id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_menu_padre FOREIGN KEY (menu_padre_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_aplicacion CHECK (aplicacion IN ('ERP_WEB','POS_WEB','ECOMMERCE_WEB','MOBILE')),
    CONSTRAINT ck_app_menu_tipo CHECK (tipo IN ('GRUPO','ITEM','SEPARADOR')),
    CONSTRAINT ck_app_menu_auth CHECK (modo_autorizacion IN ('AUTENTICADO','CUALQUIERA','TODOS')),
    CONSTRAINT ck_app_menu_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT ck_app_menu_orden CHECK (orden >= 0),
    CONSTRAINT ck_app_menu_ruta CHECK (ruta IS NULL OR LEFT(ruta,1) = '/'),
    CONSTRAINT ck_app_menu_tipo_ruta CHECK ((tipo='ITEM' AND ruta IS NOT NULL) OR (tipo IN ('GRUPO','SEPARADOR') AND ruta IS NULL)),
    CONSTRAINT ck_app_menu_padre CHECK (menu_padre_id IS NULL OR menu_padre_id <> id)
);

CREATE TABLE sch_app.menu_navegacion_permiso (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    menu_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_menu_permiso PRIMARY KEY (id),
    CONSTRAINT uk_app_menu_permiso UNIQUE (menu_id, permiso_id),
    CONSTRAINT fk_app_menu_permiso_menu FOREIGN KEY (menu_id) REFERENCES sch_app.menu_navegacion(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_menu_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES sch_seguridad.permiso(id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_menu_permiso_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE OR REPLACE FUNCTION sch_app.fn_validar_menu_jerarquia()
RETURNS TRIGGER AS $$
DECLARE
    app_padre VARCHAR(40);
    tipo_padre VARCHAR(20);
BEGIN
    IF NEW.id IS NOT NULL AND NEW.menu_padre_id = NEW.id THEN
        RAISE EXCEPTION 'Un menú no puede ser padre de sí mismo';
    END IF;
    IF NEW.menu_padre_id IS NULL THEN RETURN NEW; END IF;
    SELECT aplicacion, tipo INTO app_padre, tipo_padre FROM sch_app.menu_navegacion WHERE id = NEW.menu_padre_id AND estado='ACTIVO';
    IF NOT FOUND THEN RAISE EXCEPTION 'El menú padre no existe o no está activo'; END IF;
    IF app_padre <> NEW.aplicacion THEN RAISE EXCEPTION 'Padre e hijo deben pertenecer a la misma aplicación'; END IF;
    IF tipo_padre <> 'GRUPO' THEN RAISE EXCEPTION 'Solo un GRUPO puede contener hijos'; END IF;
    IF EXISTS (
        WITH RECURSIVE anc AS (
            SELECT id, menu_padre_id FROM sch_app.menu_navegacion WHERE id = NEW.menu_padre_id
            UNION ALL
            SELECT p.id, p.menu_padre_id FROM sch_app.menu_navegacion p JOIN anc a ON p.id = a.menu_padre_id
        ) SELECT 1 FROM anc WHERE id = NEW.id
    ) THEN RAISE EXCEPTION 'La relación genera un ciclo'; END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_app_menu_jerarquia BEFORE INSERT OR UPDATE ON sch_app.menu_navegacion FOR EACH ROW EXECUTE FUNCTION sch_app.fn_validar_menu_jerarquia();

CREATE INDEX ix_app_menu_padre_orden ON sch_app.menu_navegacion(aplicacion, menu_padre_id, orden, etiqueta) WHERE estado='ACTIVO' AND es_visible;
CREATE INDEX ix_app_menu_permiso ON sch_app.menu_navegacion_permiso(permiso_id, estado);
CREATE UNIQUE INDEX uk_app_menu_ruta_activa ON sch_app.menu_navegacion(aplicacion, ruta) WHERE ruta IS NOT NULL AND estado='ACTIVO';

-- Módulos base: no crean roles ni conceden permisos automáticamente.
INSERT INTO sch_seguridad.modulo_sistema(codigo,nombre,descripcion,orden) VALUES
('SEGURIDAD','Seguridad','Usuarios, roles, permisos y ámbitos.',10),
('ORGANIZACION','Organización','Empresas, establecimientos, almacenes y terminales.',20),
('CATALOGO','Catálogo','Productos regulatorios y SKU comerciales.',30),
('COMPRAS','Compras','Solicitudes, órdenes, recepción y proveedores.',40),
('INVENTARIO','Inventario','Stock, lotes, reservas, transferencias y conteos.',50),
('PRECIOS','Precios y promociones','Listas de precios y promociones.',60),
('VENTAS','Retail / POS','Caja, ventas, pagos y devoluciones.',70),
('DISPENSACION','Dispensación','Prescripciones y dispensación farmacéutica.',80),
('CONTROLADOS','Controlados','Recetas y movimientos de productos controlados.',90),
('FISCAL','Fiscal / CPE','Comprobantes electrónicos y notas de crédito.',100),
('RECALL','Recall','Alertas, inmovilización y retiros.',110),
('FARMACOVIGILANCIA','Farmacovigilancia','Reportes y seguimientos de seguridad.',120),
('ERP','ERP financiero','CxP/CxC, tesorería y posting.',130),
('REPORTES','Reportes','Observatorio, BI y reportes.',140)
ON CONFLICT (codigo) DO UPDATE SET nombre=EXCLUDED.nombre, descripcion=EXCLUDED.descripcion, orden=EXCLUDED.orden, es_activo=TRUE;

INSERT INTO sch_seguridad.permiso(modulo_id,codigo,recurso,accion,nombre,descripcion)
SELECT m.id, m.codigo || ':MODULO:VER', 'MODULO', 'VER', 'Ver ' || m.nombre, 'Permiso de entrada/visibilidad del módulo.'
FROM sch_seguridad.modulo_sistema m
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO sch_app.menu_navegacion(codigo,aplicacion,etiqueta,descripcion,tipo,modo_autorizacion,orden)
VALUES
('OPERACIONES','ERP_WEB','Operaciones','Operación comercial y farmacéutica.','GRUPO','AUTENTICADO',10),
('ADMINISTRACION','ERP_WEB','Administración','Configuración, seguridad y organización.','GRUPO','AUTENTICADO',20)
ON CONFLICT (aplicacion,codigo) DO UPDATE SET etiqueta=EXCLUDED.etiqueta, descripcion=EXCLUDED.descripcion, tipo=EXCLUDED.tipo, modo_autorizacion=EXCLUDED.modo_autorizacion, orden=EXCLUDED.orden, es_visible=TRUE, estado='ACTIVO';

WITH items(modulo_codigo,padre,codigo,etiqueta,descripcion,ruta,icono,orden) AS (
VALUES
(NULL,'OPERACIONES','DASHBOARD','Resumen','Resumen operativo.','/dashboard','LayoutDashboard',10),
('CATALOGO','OPERACIONES','CATALOGO','Catálogo','Productos y maestros.','/catalogo','PackageSearch',20),
('INVENTARIO','OPERACIONES','INVENTARIO','Inventario','Stock, lotes y vencimientos.','/inventario','Boxes',30),
('COMPRAS','OPERACIONES','COMPRAS','Compras','Proveedores, órdenes y recepción.','/compras','ShoppingCart',40),
('PRECIOS','OPERACIONES','PRECIOS','Precios','Listas y promociones.','/precios','Tags',50),
('VENTAS','OPERACIONES','VENTAS','Ventas','Ventas y devoluciones.','/ventas','ReceiptText',60),
('VENTAS','OPERACIONES','POS','Punto de venta','Venta rápida.','/pos','MonitorSmartphone',70),
('DISPENSACION','OPERACIONES','DISPENSACION','Dispensación','Prescripción y dispensación.','/dispensacion','Pill',80),
('CONTROLADOS','OPERACIONES','CONTROLADOS','Controlados','Recetas y balances.','/controlados','ShieldAlert',90),
('RECALL','OPERACIONES','RECALL','Recall','Alertas y retiros.','/recall','Siren',100),
('FARMACOVIGILANCIA','OPERACIONES','FARMACOVIGILANCIA','Farmacovigilancia','Reportes de seguridad.','/farmacovigilancia','HeartPulse',110),
('ERP','OPERACIONES','ERP','ERP financiero','Posting y finanzas.','/erp','Landmark',120),
('REPORTES','OPERACIONES','REPORTES','Reportes','Observatorio y BI.','/reportes','ChartNoAxesCombined',130),
('SEGURIDAD','ADMINISTRACION','SEGURIDAD','Seguridad','Usuarios, roles y permisos.','/seguridad','ShieldCheck',10),
('ORGANIZACION','ADMINISTRACION','ORGANIZACION','Organización','Empresas y establecimientos.','/organizacion','Building2',20)
)
INSERT INTO sch_app.menu_navegacion(modulo_id,menu_padre_id,codigo,aplicacion,etiqueta,descripcion,ruta,icono,tipo,modo_autorizacion,orden)
SELECT m.id,p.id,i.codigo,'ERP_WEB',i.etiqueta,i.descripcion,i.ruta,i.icono,'ITEM',CASE WHEN i.modulo_codigo IS NULL THEN 'AUTENTICADO' ELSE 'TODOS' END,i.orden
FROM items i
JOIN sch_app.menu_navegacion p ON p.aplicacion='ERP_WEB' AND p.codigo=i.padre
LEFT JOIN sch_seguridad.modulo_sistema m ON m.codigo=i.modulo_codigo
ON CONFLICT (aplicacion,codigo) DO UPDATE SET modulo_id=EXCLUDED.modulo_id,menu_padre_id=EXCLUDED.menu_padre_id,etiqueta=EXCLUDED.etiqueta,descripcion=EXCLUDED.descripcion,ruta=EXCLUDED.ruta,icono=EXCLUDED.icono,orden=EXCLUDED.orden,estado='ACTIVO',es_visible=TRUE;

INSERT INTO sch_app.menu_navegacion_permiso(menu_id,permiso_id)
SELECT mn.id,p.id
FROM sch_app.menu_navegacion mn
JOIN sch_seguridad.modulo_sistema m ON m.id=mn.modulo_id
JOIN sch_seguridad.permiso p ON p.modulo_id=m.id AND p.codigo=m.codigo || ':MODULO:VER'
WHERE mn.aplicacion='ERP_WEB' AND mn.modulo_id IS NOT NULL
ON CONFLICT (menu_id,permiso_id) DO UPDATE SET estado='ACTIVO';
