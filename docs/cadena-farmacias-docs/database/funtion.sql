-- ============================================================================
-- FUNCIÓN HELPER: Contexto del Usuario Activo en Sesión
-- ============================================================================
CREATE OR REPLACE FUNCTION sch_admin.fn_current_user_id()
RETURNS VARCHAR(15)
LANGUAGE sql
STABLE
PARALLEL SAFE
AS $$
    SELECT COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), 'SYSTEM')::VARCHAR(15);
$$;

-- ============================================================================
-- FUNCIÓN TRIGGER: Actualización Automática y Blindaje de Auditoría
-- ============================================================================
CREATE OR REPLACE FUNCTION sch_admin.fn_audit_updated_columns()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- 1. Actualización automática de timestamp y actor
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.updated_by = sch_admin.fn_current_user_id();

    -- 2. Blindaje: Garantizar que created_at y created_by permanezcan inmutables
    NEW.created_at = OLD.created_at;
    NEW.created_by = OLD.created_by;

    RETURN NEW;
END;
$$;

-- ============================================================================
-- ESQUEMA: sch_admin
-- ============================================================================
CREATE TRIGGER trg_audit_tenant
    BEFORE UPDATE ON sch_admin.tenant
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_seguridad
-- ============================================================================
CREATE TRIGGER trg_audit_usuario
    BEFORE UPDATE ON sch_seguridad.usuario
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_identidad_externa
    BEFORE UPDATE ON sch_seguridad.identidad_externa
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_modulo_sistema
    BEFORE UPDATE ON sch_seguridad.modulo_sistema
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_rol
    BEFORE UPDATE ON sch_seguridad.rol
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_permiso
    BEFORE UPDATE ON sch_seguridad.permiso
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_rol_permiso
    BEFORE UPDATE ON sch_seguridad.rol_permiso
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_dispositivo_tienda
    BEFORE UPDATE ON sch_seguridad.dispositivo_tienda
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_usuario_rol_ambito
    BEFORE UPDATE ON sch_seguridad.usuario_rol_ambito
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_sesion_usuario
    BEFORE UPDATE ON sch_seguridad.sesion_usuario
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_organizacion
-- ============================================================================
CREATE TRIGGER trg_audit_empresa_operadora
    BEFORE UPDATE ON sch_organizacion.empresa_operadora
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_establecimiento_farmaceutico
    BEFORE UPDATE ON sch_organizacion.establecimiento_farmaceutico
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_est_autorizacion_sanitaria
    BEFORE UPDATE ON sch_organizacion.establecimiento_autorizacion_sanitaria
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_almacen
    BEFORE UPDATE ON sch_organizacion.almacen
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_ubicacion_almacen
    BEFORE UPDATE ON sch_organizacion.ubicacion_almacen
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_terminal_pos
    BEFORE UPDATE ON sch_organizacion.terminal_pos
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_profesional_farmaceutico
    BEFORE UPDATE ON sch_organizacion.profesional_farmaceutico
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_asignacion_profesional
    BEFORE UPDATE ON sch_organizacion.asignacion_profesional
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_catalogo
-- ============================================================================
CREATE TRIGGER trg_audit_categoria_producto
    BEFORE UPDATE ON sch_catalogo.categoria_producto
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_marca
    BEFORE UPDATE ON sch_catalogo.marca
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_rubro_comercial
    BEFORE UPDATE ON sch_catalogo.rubro_comercial
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_sku_comercial
    BEFORE UPDATE ON sch_catalogo.sku_comercial
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_sku_codigo_barra
    BEFORE UPDATE ON sch_catalogo.sku_codigo_barra
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_abastecimiento
-- ============================================================================
CREATE TRIGGER trg_audit_proveedor
    BEFORE UPDATE ON sch_abastecimiento.proveedor
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_solicitud_compra
    BEFORE UPDATE ON sch_abastecimiento.solicitud_compra
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_solicitud_compra_linea
    BEFORE UPDATE ON sch_abastecimiento.solicitud_compra_linea
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_orden_compra
    BEFORE UPDATE ON sch_abastecimiento.orden_compra
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_orden_compra_linea
    BEFORE UPDATE ON sch_abastecimiento.orden_compra_linea
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_recepcion_compra
    BEFORE UPDATE ON sch_abastecimiento.recepcion_compra
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_recepcion_compra_linea
    BEFORE UPDATE ON sch_abastecimiento.recepcion_compra_linea
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_inventario
-- ============================================================================
CREATE TRIGGER trg_audit_lote
    BEFORE UPDATE ON sch_inventario.lote
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_posicion_inventario
    BEFORE UPDATE ON sch_inventario.posicion_inventario
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_transferencia_inventario
    BEFORE UPDATE ON sch_inventario.transferencia_inventario
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_transferencia_linea
    BEFORE UPDATE ON sch_inventario.transferencia_linea
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_transferencia_despacho
    BEFORE UPDATE ON sch_inventario.transferencia_despacho
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_transferencia_recepcion
    BEFORE UPDATE ON sch_inventario.transferencia_recepcion
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_conteo_inventario
    BEFORE UPDATE ON sch_inventario.conteo_inventario
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_precio
-- ============================================================================
CREATE TRIGGER trg_audit_lista_precio
    BEFORE UPDATE ON sch_precio.lista_precio
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_lista_precio_version
    BEFORE UPDATE ON sch_precio.lista_precio_version
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_lista_precio_item
    BEFORE UPDATE ON sch_precio.lista_precio_item
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_promocion
    BEFORE UPDATE ON sch_precio.promocion
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_promocion_version
    BEFORE UPDATE ON sch_precio.promocion_version
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_promocion_objetivo
    BEFORE UPDATE ON sch_precio.promocion_objetivo
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_venta
-- ============================================================================
CREATE TRIGGER trg_audit_cliente
    BEFORE UPDATE ON sch_venta.cliente
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_medio_pago
    BEFORE UPDATE ON sch_venta.medio_pago
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_turno_caja
    BEFORE UPDATE ON sch_venta.turno_caja
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_venta
    BEFORE UPDATE ON sch_venta.venta
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_venta_linea
    BEFORE UPDATE ON sch_venta.venta_linea
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_devolucion_comercial
    BEFORE UPDATE ON sch_venta.devolucion_comercial
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_dispensacion
-- ============================================================================
CREATE TRIGGER trg_audit_prescripcion
    BEFORE UPDATE ON sch_dispensacion.prescripcion
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_prescripcion_linea
    BEFORE UPDATE ON sch_dispensacion.prescripcion_linea
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_dispensacion
    BEFORE UPDATE ON sch_dispensacion.dispensacion
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_dispensacion_linea
    BEFORE UPDATE ON sch_dispensacion.dispensacion_linea
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_receta_controlada
    BEFORE UPDATE ON sch_dispensacion.receta_controlada
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_balance_controlado
    BEFORE UPDATE ON sch_dispensacion.balance_controlado
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_facturacion
-- ============================================================================
CREATE TRIGGER trg_audit_comprobante_electronico
    BEFORE UPDATE ON sch_facturacion.comprobante_electronico
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_nota_credito_electronica
    BEFORE UPDATE ON sch_facturacion.nota_credito_electronica
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_vigilancia
-- ============================================================================
CREATE TRIGGER trg_audit_caso_recall
    BEFORE UPDATE ON sch_vigilancia.caso_recall
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_recall_establecimiento
    BEFORE UPDATE ON sch_vigilancia.recall_establecimiento
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_reporte_seguridad
    BEFORE UPDATE ON sch_vigilancia.reporte_seguridad
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_finanzas
-- ============================================================================
CREATE TRIGGER trg_audit_cuenta_bancaria_empresa
    BEFORE UPDATE ON sch_finanzas.cuenta_bancaria_empresa
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_factura_proveedor
    BEFORE UPDATE ON sch_finanzas.factura_proveedor
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_cuenta_por_pagar
    BEFORE UPDATE ON sch_finanzas.cuenta_por_pagar
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_cuenta_por_cobrar
    BEFORE UPDATE ON sch_finanzas.cuenta_por_cobrar
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_reporte_mensual_precios
    BEFORE UPDATE ON sch_finanzas.reporte_mensual_precios
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_integracion
-- ============================================================================
CREATE TRIGGER trg_audit_servicio_externo
    BEFORE UPDATE ON sch_integracion.servicio_externo
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

-- ============================================================================
-- ESQUEMA: sch_app
-- ============================================================================
CREATE TRIGGER trg_audit_menu_navegacion
    BEFORE UPDATE ON sch_app.menu_navegacion
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_plantilla_notificacion
    BEFORE UPDATE ON sch_app.plantilla_notificacion
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_feature_flag
    BEFORE UPDATE ON sch_app.feature_flag
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

CREATE TRIGGER trg_audit_app_version
    BEFORE UPDATE ON sch_app.app_version
    FOR EACH ROW EXECUTE FUNCTION sch_admin.fn_audit_updated_columns();

    