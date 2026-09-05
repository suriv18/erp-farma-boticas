-- ============================================================================
-- V2__menu_navegacion_rbac.sql
-- Navegación dinámica desacoplada del catálogo RBAC
-- Requiere: V1__init_erp_boticas_postgres_revisado.sql
-- Motor: PostgreSQL 16+
-- Fecha: 2026-08-09
-- ============================================================================

BEGIN;

-- ============================================================================
-- 1. MENÚ DE NAVEGACIÓN POR APLICACIÓN
-- ============================================================================
CREATE TABLE app.menu_navegacion (
    menu_id                 BIGSERIAL PRIMARY KEY,
    modulo_id               BIGINT REFERENCES security.modulo_sistema(modulo_id) ON DELETE RESTRICT,
    menu_padre_id           BIGINT REFERENCES app.menu_navegacion(menu_id) ON DELETE RESTRICT,
    codigo                  VARCHAR(80) NOT NULL,
    aplicacion              VARCHAR(40) NOT NULL DEFAULT 'ERP_WEB',
    etiqueta                VARCHAR(120) NOT NULL,
    descripcion             TEXT,
    ruta                    VARCHAR(240),
    icono                   VARCHAR(80),
    tipo                    VARCHAR(20) NOT NULL DEFAULT 'ITEM',
    modo_autorizacion       VARCHAR(20) NOT NULL DEFAULT 'TODOS',
    orden                   INTEGER NOT NULL DEFAULT 0,
    es_visible              BOOLEAN NOT NULL DEFAULT TRUE,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ,
    deleted_by              BIGINT,
    CONSTRAINT uq_menu_navegacion_aplicacion_codigo UNIQUE (aplicacion, codigo),
    CONSTRAINT ck_menu_navegacion_aplicacion CHECK (
        aplicacion IN ('ERP_WEB','POS_WEB','ECOMMERCE_WEB','MOBILE')
    ),
    CONSTRAINT ck_menu_navegacion_tipo CHECK (tipo IN ('GRUPO','ITEM','SEPARADOR')),
    CONSTRAINT ck_menu_navegacion_autorizacion CHECK (
        modo_autorizacion IN ('AUTENTICADO','CUALQUIERA','TODOS')
    ),
    CONSTRAINT ck_menu_navegacion_estado CHECK (estado IN ('A','I')),
    CONSTRAINT ck_menu_navegacion_orden CHECK (orden >= 0),
    CONSTRAINT ck_menu_navegacion_ruta CHECK (ruta IS NULL OR LEFT(ruta, 1) = '/'),
    CONSTRAINT ck_menu_navegacion_tipo_ruta CHECK (
        (tipo = 'ITEM' AND ruta IS NOT NULL)
        OR (tipo IN ('GRUPO','SEPARADOR') AND ruta IS NULL)
    ),
    CONSTRAINT ck_menu_navegacion_padre CHECK (
        menu_padre_id IS NULL OR menu_padre_id <> menu_id
    )
);

CREATE TABLE app.menu_navegacion_permiso (
    menu_permiso_id         BIGSERIAL PRIMARY KEY,
    menu_id                 BIGINT NOT NULL REFERENCES app.menu_navegacion(menu_id) ON DELETE CASCADE,
    permiso_id              BIGINT NOT NULL REFERENCES security.permiso(permiso_id) ON DELETE RESTRICT,
    estado                  CHAR(1) NOT NULL DEFAULT 'A',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ,
    updated_by              BIGINT,
    CONSTRAINT uq_menu_navegacion_permiso UNIQUE (menu_id, permiso_id),
    CONSTRAINT ck_menu_navegacion_permiso_estado CHECK (estado IN ('A','I'))
);

CREATE INDEX idx_menu_navegacion_padre_orden
    ON app.menu_navegacion(aplicacion, menu_padre_id, orden)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_menu_navegacion_modulo
    ON app.menu_navegacion(modulo_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_menu_navegacion_visible
    ON app.menu_navegacion(aplicacion, estado, es_visible)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_menu_navegacion_ruta_activa
    ON app.menu_navegacion(aplicacion, ruta)
    WHERE ruta IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_menu_navegacion_permiso_permiso
    ON app.menu_navegacion_permiso(permiso_id, estado);

CREATE OR REPLACE FUNCTION app.fn_validar_menu_navegacion_jerarquia()
RETURNS TRIGGER AS $$
DECLARE
    aplicacion_padre VARCHAR(40);
    tipo_padre       VARCHAR(20);
BEGIN
    IF NEW.tipo <> 'GRUPO' AND EXISTS (
        SELECT 1
        FROM app.menu_navegacion hijo
        WHERE hijo.menu_padre_id = NEW.menu_id
          AND hijo.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'El menú % tiene hijos activos y debe conservar el tipo GRUPO', NEW.codigo;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM app.menu_navegacion hijo
        WHERE hijo.menu_padre_id = NEW.menu_id
          AND hijo.aplicacion <> NEW.aplicacion
          AND hijo.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'El menú % no puede cambiar de aplicación mientras tenga hijos activos', NEW.codigo;
    END IF;

    IF NEW.menu_padre_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT padre.aplicacion, padre.tipo
    INTO aplicacion_padre, tipo_padre
    FROM app.menu_navegacion padre
    WHERE padre.menu_id = NEW.menu_padre_id
      AND padre.deleted_at IS NULL;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El menú padre % no existe o está eliminado', NEW.menu_padre_id;
    END IF;

    IF aplicacion_padre <> NEW.aplicacion THEN
        RAISE EXCEPTION 'El menú padre y el hijo deben pertenecer a la misma aplicación';
    END IF;

    IF tipo_padre <> 'GRUPO' THEN
        RAISE EXCEPTION 'Solo un menú de tipo GRUPO puede contener hijos';
    END IF;

    IF EXISTS (
        WITH RECURSIVE ancestro AS (
            SELECT menu_id, menu_padre_id
            FROM app.menu_navegacion
            WHERE menu_id = NEW.menu_padre_id

            UNION

            SELECT padre.menu_id, padre.menu_padre_id
            FROM app.menu_navegacion padre
            JOIN ancestro actual
                ON padre.menu_id = actual.menu_padre_id
        )
        SELECT 1
        FROM ancestro
        WHERE menu_id = NEW.menu_id
    ) THEN
        RAISE EXCEPTION 'La relación padre-hijo genera un ciclo para el menú %', NEW.codigo;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_menu_navegacion_jerarquia
    BEFORE INSERT OR UPDATE ON app.menu_navegacion
    FOR EACH ROW EXECUTE FUNCTION app.fn_validar_menu_navegacion_jerarquia();

DROP TRIGGER IF EXISTS trg_set_updated_at ON app.menu_navegacion;
CREATE TRIGGER trg_set_updated_at
    BEFORE UPDATE ON app.menu_navegacion
    FOR EACH ROW EXECUTE FUNCTION auditoria.fn_set_updated_at();

DROP TRIGGER IF EXISTS trg_set_updated_at ON app.menu_navegacion_permiso;
CREATE TRIGGER trg_set_updated_at
    BEFORE UPDATE ON app.menu_navegacion_permiso
    FOR EACH ROW EXECUTE FUNCTION auditoria.fn_set_updated_at();

-- ============================================================================
-- 2. MÓDULOS RBAC INICIALES
--    POS, caja y clientes son capacidades del módulo VENTAS; no se duplican
--    bounded contexts solo para representar opciones del menú.
-- ============================================================================
INSERT INTO security.modulo_sistema (
    codigo,
    nombre,
    descripcion,
    orden,
    es_activo
)
VALUES
    ('SEGURIDAD', 'Seguridad', 'Usuarios, roles, permisos y control de accesos.', 10, TRUE),
    ('ORGANIZACION', 'Organización', 'Empresas, sucursales, almacenes y cajas operativas.', 20, TRUE),
    ('CATALOGO', 'Catálogo', 'Productos, categorías, marcas y unidades de medida.', 30, TRUE),
    ('INVENTARIO', 'Inventario', 'Stock, lotes, vencimientos, reservas y kardex.', 40, TRUE),
    ('COMPRAS', 'Compras', 'Proveedores, órdenes de compra y recepción.', 50, TRUE),
    ('VENTAS', 'Ventas', 'Ventas, POS, caja, clientes, pagos y comprobantes.', 60, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    orden = EXCLUDED.orden,
    es_activo = EXCLUDED.es_activo,
    deleted_at = NULL,
    deleted_by = NULL;

-- ============================================================================
-- 3. PERMISOS DE ENTRADA A MÓDULO
--    Los endpoints deben exigir permisos más específicos por recurso y acción.
-- ============================================================================
WITH permiso_inicial (
    modulo_codigo,
    codigo,
    recurso,
    accion,
    nombre,
    descripcion
) AS (
    VALUES
        ('SEGURIDAD', 'SEGURIDAD:MODULO:VER', 'MODULO', 'VER', 'Acceder a Seguridad', 'Permite visualizar la navegación del módulo Seguridad.'),
        ('ORGANIZACION', 'ORGANIZACION:MODULO:VER', 'MODULO', 'VER', 'Acceder a Organización', 'Permite visualizar la navegación del módulo Organización.'),
        ('CATALOGO', 'CATALOGO:MODULO:VER', 'MODULO', 'VER', 'Acceder a Catálogo', 'Permite visualizar la navegación del módulo Catálogo.'),
        ('INVENTARIO', 'INVENTARIO:MODULO:VER', 'MODULO', 'VER', 'Acceder a Inventario', 'Permite visualizar la navegación del módulo Inventario.'),
        ('COMPRAS', 'COMPRAS:MODULO:VER', 'MODULO', 'VER', 'Acceder a Compras', 'Permite visualizar la navegación del módulo Compras.'),
        ('VENTAS', 'VENTAS:MODULO:VER', 'MODULO', 'VER', 'Acceder a Ventas', 'Permite visualizar la navegación de Ventas, POS, Caja y Clientes.')
)
INSERT INTO security.permiso (
    modulo_id,
    codigo,
    recurso,
    accion,
    nombre,
    descripcion,
    es_critico,
    estado
)
SELECT
    modulo.modulo_id,
    permiso.codigo,
    permiso.recurso,
    permiso.accion,
    permiso.nombre,
    permiso.descripcion,
    FALSE,
    'A'
FROM permiso_inicial permiso
JOIN security.modulo_sistema modulo
    ON modulo.codigo = permiso.modulo_codigo
ON CONFLICT (codigo) DO UPDATE
SET modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = EXCLUDED.estado,
    deleted_at = NULL,
    deleted_by = NULL;

-- ============================================================================
-- 4. ESTRUCTURA JERÁRQUICA DEL MENÚ ERP WEB
-- ============================================================================
INSERT INTO app.menu_navegacion (
    codigo,
    aplicacion,
    etiqueta,
    descripcion,
    tipo,
    modo_autorizacion,
    orden,
    es_visible,
    estado
)
VALUES
    ('OPERACIONES', 'ERP_WEB', 'Operaciones', 'Operación comercial y farmacéutica.', 'GRUPO', 'AUTENTICADO', 10, TRUE, 'A'),
    ('ADMINISTRACION', 'ERP_WEB', 'Administración', 'Configuración, seguridad y organización.', 'GRUPO', 'AUTENTICADO', 20, TRUE, 'A')
ON CONFLICT (aplicacion, codigo) DO UPDATE
SET etiqueta = EXCLUDED.etiqueta,
    descripcion = EXCLUDED.descripcion,
    tipo = EXCLUDED.tipo,
    modo_autorizacion = EXCLUDED.modo_autorizacion,
    orden = EXCLUDED.orden,
    es_visible = EXCLUDED.es_visible,
    estado = EXCLUDED.estado,
    deleted_at = NULL,
    deleted_by = NULL;

WITH menu_inicial (
    modulo_codigo,
    menu_padre_codigo,
    codigo,
    etiqueta,
    descripcion,
    ruta,
    icono,
    modo_autorizacion,
    orden
) AS (
    VALUES
        (NULL, 'OPERACIONES', 'DASHBOARD', 'Resumen', 'Resumen operativo de la botica.', '/dashboard', 'LayoutDashboard', 'AUTENTICADO', 10),
        ('CATALOGO', 'OPERACIONES', 'CATALOGO', 'Catálogo', 'Productos, categorías, marcas y laboratorios.', '/catalogo', 'PackageSearch', 'TODOS', 20),
        ('INVENTARIO', 'OPERACIONES', 'INVENTARIO', 'Inventario', 'Stock, lotes y vencimientos.', '/inventario', 'Boxes', 'TODOS', 30),
        ('COMPRAS', 'OPERACIONES', 'COMPRAS', 'Compras', 'Proveedores, órdenes y recepción.', '/compras', 'ShoppingCart', 'TODOS', 40),
        ('VENTAS', 'OPERACIONES', 'VENTAS', 'Ventas', 'Ventas y comprobantes.', '/ventas', 'ReceiptText', 'TODOS', 50),
        ('VENTAS', 'OPERACIONES', 'POS', 'Punto de venta', 'Venta rápida y emisión de comprobantes.', '/pos', 'MonitorSmartphone', 'TODOS', 60),
        ('VENTAS', 'OPERACIONES', 'CAJA', 'Caja', 'Aperturas, movimientos, arqueos y cierres.', '/caja', 'WalletCards', 'TODOS', 70),
        ('VENTAS', 'OPERACIONES', 'CLIENTES', 'Clientes', 'Datos e historial comercial de clientes.', '/clientes', 'Users', 'TODOS', 80),
        ('SEGURIDAD', 'ADMINISTRACION', 'SEGURIDAD', 'Seguridad', 'Usuarios, roles y permisos.', '/seguridad', 'ShieldCheck', 'TODOS', 10),
        ('ORGANIZACION', 'ADMINISTRACION', 'ORGANIZACION', 'Organización', 'Empresas, sucursales, almacenes y cajas.', '/organizacion', 'Building2', 'TODOS', 20)
)
INSERT INTO app.menu_navegacion (
    modulo_id,
    menu_padre_id,
    codigo,
    aplicacion,
    etiqueta,
    descripcion,
    ruta,
    icono,
    tipo,
    modo_autorizacion,
    orden,
    es_visible,
    estado
)
SELECT
    modulo.modulo_id,
    menu_padre.menu_id,
    menu.codigo,
    'ERP_WEB',
    menu.etiqueta,
    menu.descripcion,
    menu.ruta,
    menu.icono,
    'ITEM',
    menu.modo_autorizacion,
    menu.orden,
    TRUE,
    'A'
FROM menu_inicial menu
JOIN app.menu_navegacion menu_padre
    ON menu_padre.aplicacion = 'ERP_WEB'
   AND menu_padre.codigo = menu.menu_padre_codigo
LEFT JOIN security.modulo_sistema modulo
    ON modulo.codigo = menu.modulo_codigo
ON CONFLICT (aplicacion, codigo) DO UPDATE
SET modulo_id = EXCLUDED.modulo_id,
    menu_padre_id = EXCLUDED.menu_padre_id,
    etiqueta = EXCLUDED.etiqueta,
    descripcion = EXCLUDED.descripcion,
    ruta = EXCLUDED.ruta,
    icono = EXCLUDED.icono,
    tipo = EXCLUDED.tipo,
    modo_autorizacion = EXCLUDED.modo_autorizacion,
    orden = EXCLUDED.orden,
    es_visible = EXCLUDED.es_visible,
    estado = EXCLUDED.estado,
    deleted_at = NULL,
    deleted_by = NULL;

-- ============================================================================
-- 5. PERMISOS REQUERIDOS POR OPCIÓN DE MENÚ
-- ============================================================================
WITH menu_permiso_inicial (menu_codigo, permiso_codigo) AS (
    VALUES
        ('CATALOGO', 'CATALOGO:MODULO:VER'),
        ('INVENTARIO', 'INVENTARIO:MODULO:VER'),
        ('COMPRAS', 'COMPRAS:MODULO:VER'),
        ('VENTAS', 'VENTAS:MODULO:VER'),
        ('POS', 'VENTAS:MODULO:VER'),
        ('CAJA', 'VENTAS:MODULO:VER'),
        ('CLIENTES', 'VENTAS:MODULO:VER'),
        ('SEGURIDAD', 'SEGURIDAD:MODULO:VER'),
        ('ORGANIZACION', 'ORGANIZACION:MODULO:VER')
)
INSERT INTO app.menu_navegacion_permiso (
    menu_id,
    permiso_id,
    estado
)
SELECT
    menu.menu_id,
    permiso.permiso_id,
    'A'
FROM menu_permiso_inicial relacion
JOIN app.menu_navegacion menu
    ON menu.aplicacion = 'ERP_WEB'
   AND menu.codigo = relacion.menu_codigo
JOIN security.permiso permiso
    ON permiso.codigo = relacion.permiso_codigo
ON CONFLICT (menu_id, permiso_id) DO UPDATE
SET estado = EXCLUDED.estado;

COMMENT ON TABLE app.menu_navegacion IS
    'Árbol de navegación por aplicación. Contiene metadatos de presentación y referencia opcional al módulo RBAC.';
COMMENT ON TABLE app.menu_navegacion_permiso IS
    'Permisos RBAC requeridos para mostrar una opción de navegación.';
COMMENT ON COLUMN app.menu_navegacion.icono IS
    'Clave semántica incluida en una lista segura del frontend; nunca contiene HTML ni nombres de componentes dinámicos.';
COMMENT ON COLUMN app.menu_navegacion.modo_autorizacion IS
    'AUTENTICADO no exige permiso; CUALQUIERA exige al menos uno; TODOS exige todos los permisos activos relacionados.';
COMMENT ON COLUMN app.menu_navegacion.ruta IS
    'Ruta interna declarativa. La autorización real continúa aplicándose en los endpoints del backend.';

COMMIT;
