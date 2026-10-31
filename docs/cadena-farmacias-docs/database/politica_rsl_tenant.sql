-- ============================================================================
-- FUNCIÓN HELPER: Contexto de Sesión Multi-Tenant
-- ============================================================================

CREATE OR REPLACE FUNCTION sch_admin.fn_current_tenant_id()
RETURNS BIGINT
LANGUAGE sql
STABLE
PARALLEL SAFE
AS $$
    SELECT NULLIF(current_setting('app.current_tenant_id', true), '')::BIGINT;
$$;

CREATE OR REPLACE FUNCTION sch_admin.fn_is_superadmin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
PARALLEL SAFE
AS $$
    SELECT COALESCE(current_setting('app.is_superadmin', true), 'false')::BOOLEAN;
$$;

-- ============================================================================
-- ESQUEMA 1: sch_seguridad
-- ============================================================================

-- usuario
ALTER TABLE sch_seguridad.usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_seguridad.usuario FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_usuario_tenant ON sch_seguridad.usuario
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- identidad_externa
ALTER TABLE sch_seguridad.identidad_externa ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_seguridad.identidad_externa FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_identidad_externa_tenant ON sch_seguridad.identidad_externa
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- rol
ALTER TABLE sch_seguridad.rol ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_seguridad.rol FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_rol_tenant ON sch_seguridad.rol
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- rol_permiso
ALTER TABLE sch_seguridad.rol_permiso ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_seguridad.rol_permiso FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_rol_permiso_tenant ON sch_seguridad.rol_permiso
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- dispositivo_tienda
ALTER TABLE sch_seguridad.dispositivo_tienda ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_seguridad.dispositivo_tienda FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_dispositivo_tienda_tenant ON sch_seguridad.dispositivo_tienda
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- usuario_rol_ambito
ALTER TABLE sch_seguridad.usuario_rol_ambito ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_seguridad.usuario_rol_ambito FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_usuario_rol_ambito_tenant ON sch_seguridad.usuario_rol_ambito
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- sesion_usuario
ALTER TABLE sch_seguridad.sesion_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_seguridad.sesion_usuario FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_sesion_usuario_tenant ON sch_seguridad.sesion_usuario
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 2: sch_organizacion
-- ============================================================================

-- empresa_operadora
ALTER TABLE sch_organizacion.empresa_operadora ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.empresa_operadora FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_empresa_operadora_tenant ON sch_organizacion.empresa_operadora
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- establecimiento_farmaceutico
ALTER TABLE sch_organizacion.establecimiento_farmaceutico ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.establecimiento_farmaceutico FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_establecimiento_farmaceutico_tenant ON sch_organizacion.establecimiento_farmaceutico
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- establecimiento_autorizacion_sanitaria
ALTER TABLE sch_organizacion.establecimiento_autorizacion_sanitaria ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.establecimiento_autorizacion_sanitaria FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_est_autorizacion_sanitaria_tenant ON sch_organizacion.establecimiento_autorizacion_sanitaria
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- almacen
ALTER TABLE sch_organizacion.almacen ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.almacen FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_almacen_tenant ON sch_organizacion.almacen
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- ubicacion_almacen
ALTER TABLE sch_organizacion.ubicacion_almacen ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.ubicacion_almacen FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_ubicacion_almacen_tenant ON sch_organizacion.ubicacion_almacen
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- terminal_pos
ALTER TABLE sch_organizacion.terminal_pos ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.terminal_pos FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_terminal_pos_tenant ON sch_organizacion.terminal_pos
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- profesional_farmaceutico
ALTER TABLE sch_organizacion.profesional_farmaceutico ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.profesional_farmaceutico FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_profesional_farmaceutico_tenant ON sch_organizacion.profesional_farmaceutico
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- asignacion_profesional
ALTER TABLE sch_organizacion.asignacion_profesional ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_organizacion.asignacion_profesional FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_asignacion_profesional_tenant ON sch_organizacion.asignacion_profesional
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 3: sch_catalogo (Solo tablas comerciales privadas del tenant)
-- NOTA: Las tablas maestras normativas (producto_regulado, principio_activo, etc.)
-- son globales y compartidas para todos los tenants, por lo que no llevan RLS por tenant_id.
-- ============================================================================

-- categoria_producto
ALTER TABLE sch_catalogo.categoria_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_catalogo.categoria_producto FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_categoria_producto_tenant ON sch_catalogo.categoria_producto
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- marca
ALTER TABLE sch_catalogo.marca ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_catalogo.marca FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_marca_tenant ON sch_catalogo.marca
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- rubro_comercial
ALTER TABLE sch_catalogo.rubro_comercial ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_catalogo.rubro_comercial FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_rubro_comercial_tenant ON sch_catalogo.rubro_comercial
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- sku_comercial
ALTER TABLE sch_catalogo.sku_comercial ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_catalogo.sku_comercial FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_sku_comercial_tenant ON sch_catalogo.sku_comercial
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- sku_codigo_barra
ALTER TABLE sch_catalogo.sku_codigo_barra ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_catalogo.sku_codigo_barra FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_sku_codigo_barra_tenant ON sch_catalogo.sku_codigo_barra
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 4: sch_abastecimiento
-- ============================================================================

-- proveedor
ALTER TABLE sch_abastecimiento.proveedor ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_abastecimiento.proveedor FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_proveedor_tenant ON sch_abastecimiento.proveedor
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- solicitud_compra
ALTER TABLE sch_abastecimiento.solicitud_compra ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_abastecimiento.solicitud_compra FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_solicitud_compra_tenant ON sch_abastecimiento.solicitud_compra
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- solicitud_compra_linea
ALTER TABLE sch_abastecimiento.solicitud_compra_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_abastecimiento.solicitud_compra_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_solicitud_compra_linea_tenant ON sch_abastecimiento.solicitud_compra_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- orden_compra
ALTER TABLE sch_abastecimiento.orden_compra ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_abastecimiento.orden_compra FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_orden_compra_tenant ON sch_abastecimiento.orden_compra
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- orden_compra_linea
ALTER TABLE sch_abastecimiento.orden_compra_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_abastecimiento.orden_compra_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_orden_compra_linea_tenant ON sch_abastecimiento.orden_compra_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- recepcion_compra
ALTER TABLE sch_abastecimiento.recepcion_compra ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_abastecimiento.recepcion_compra FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_recepcion_compra_tenant ON sch_abastecimiento.recepcion_compra
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- recepcion_compra_linea
ALTER TABLE sch_abastecimiento.recepcion_compra_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_abastecimiento.recepcion_compra_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_recepcion_compra_linea_tenant ON sch_abastecimiento.recepcion_compra_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 5: sch_inventario
-- ============================================================================

-- lote
ALTER TABLE sch_inventario.lote ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.lote FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_lote_tenant ON sch_inventario.lote
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- posicion_inventario
ALTER TABLE sch_inventario.posicion_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.posicion_inventario FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_posicion_inventario_tenant ON sch_inventario.posicion_inventario
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- movimiento_inventario (Kardex)
ALTER TABLE sch_inventario.movimiento_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.movimiento_inventario FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_movimiento_inventario_tenant ON sch_inventario.movimiento_inventario
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- reserva_inventario
ALTER TABLE sch_inventario.reserva_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.reserva_inventario FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_reserva_inventario_tenant ON sch_inventario.reserva_inventario
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- transferencia_inventario
ALTER TABLE sch_inventario.transferencia_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.transferencia_inventario FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_transferencia_inventario_tenant ON sch_inventario.transferencia_inventario
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- transferencia_linea
ALTER TABLE sch_inventario.transferencia_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.transferencia_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_transferencia_linea_tenant ON sch_inventario.transferencia_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- transferencia_despacho
ALTER TABLE sch_inventario.transferencia_despacho ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.transferencia_despacho FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_transferencia_despacho_tenant ON sch_inventario.transferencia_despacho
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- transferencia_despacho_linea
ALTER TABLE sch_inventario.transferencia_despacho_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.transferencia_despacho_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_transferencia_despacho_linea_tenant ON sch_inventario.transferencia_despacho_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- transferencia_recepcion
ALTER TABLE sch_inventario.transferencia_recepcion ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.transferencia_recepcion FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_transferencia_recepcion_tenant ON sch_inventario.transferencia_recepcion
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- transferencia_recepcion_linea
ALTER TABLE sch_inventario.transferencia_recepcion_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.transferencia_recepcion_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_transferencia_recepcion_linea_tenant ON sch_inventario.transferencia_recepcion_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- conteo_inventario
ALTER TABLE sch_inventario.conteo_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.conteo_inventario FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_conteo_inventario_tenant ON sch_inventario.conteo_inventario
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- conteo_inventario_linea
ALTER TABLE sch_inventario.conteo_inventario_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_inventario.conteo_inventario_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_conteo_inventario_linea_tenant ON sch_inventario.conteo_inventario_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 6: sch_precio
-- ============================================================================

-- lista_precio
ALTER TABLE sch_precio.lista_precio ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_precio.lista_precio FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_lista_precio_tenant ON sch_precio.lista_precio
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- lista_precio_version
ALTER TABLE sch_precio.lista_precio_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_precio.lista_precio_version FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_lista_precio_version_tenant ON sch_precio.lista_precio_version
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- lista_precio_item
ALTER TABLE sch_precio.lista_precio_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_precio.lista_precio_item FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_lista_precio_item_tenant ON sch_precio.lista_precio_item
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- promocion
ALTER TABLE sch_precio.promocion ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_precio.promocion FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_promocion_tenant ON sch_precio.promocion
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- promocion_version
ALTER TABLE sch_precio.promocion_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_precio.promocion_version FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_promocion_version_tenant ON sch_precio.promocion_version
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- promocion_objetivo
ALTER TABLE sch_precio.promocion_objetivo ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_precio.promocion_objetivo FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_promocion_objetivo_tenant ON sch_precio.promocion_objetivo
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 7: sch_venta
-- ============================================================================

-- cliente
ALTER TABLE sch_venta.cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.cliente FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_cliente_tenant ON sch_venta.cliente
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- cliente_consentimiento
ALTER TABLE sch_venta.cliente_consentimiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.cliente_consentimiento FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_cliente_consentimiento_tenant ON sch_venta.cliente_consentimiento
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- medio_pago
ALTER TABLE sch_venta.medio_pago ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.medio_pago FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_medio_pago_tenant ON sch_venta.medio_pago
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- turno_caja
ALTER TABLE sch_venta.turno_caja ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.turno_caja FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_turno_caja_tenant ON sch_venta.turno_caja
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- movimiento_caja
ALTER TABLE sch_venta.movimiento_caja ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.movimiento_caja FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_movimiento_caja_tenant ON sch_venta.movimiento_caja
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- venta
ALTER TABLE sch_venta.venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.venta FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_venta_tenant ON sch_venta.venta
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- venta_linea
ALTER TABLE sch_venta.venta_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.venta_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_venta_linea_tenant ON sch_venta.venta_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- venta_linea_lote
ALTER TABLE sch_venta.venta_linea_lote ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.venta_linea_lote FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_venta_linea_lote_tenant ON sch_venta.venta_linea_lote
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- pago_venta
ALTER TABLE sch_venta.pago_venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.pago_venta FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_pago_venta_tenant ON sch_venta.pago_venta
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- devolucion_comercial
ALTER TABLE sch_venta.devolucion_comercial ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.devolucion_comercial FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_devolucion_comercial_tenant ON sch_venta.devolucion_comercial
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- devolucion_comercial_linea
ALTER TABLE sch_venta.devolucion_comercial_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.devolucion_comercial_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_devolucion_comercial_linea_tenant ON sch_venta.devolucion_comercial_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- devolucion_linea_lote
ALTER TABLE sch_venta.devolucion_linea_lote ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_venta.devolucion_linea_lote FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_devolucion_linea_lote_tenant ON sch_venta.devolucion_linea_lote
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 8: sch_dispensacion
-- ============================================================================

-- prescripcion
ALTER TABLE sch_dispensacion.prescripcion ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.prescripcion FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_prescripcion_tenant ON sch_dispensacion.prescripcion
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- prescripcion_linea
ALTER TABLE sch_dispensacion.prescripcion_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.prescripcion_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_prescripcion_linea_tenant ON sch_dispensacion.prescripcion_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- dispensacion
ALTER TABLE sch_dispensacion.dispensacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.dispensacion FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_dispensacion_tenant ON sch_dispensacion.dispensacion
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- dispensacion_linea
ALTER TABLE sch_dispensacion.dispensacion_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.dispensacion_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_dispensacion_linea_tenant ON sch_dispensacion.dispensacion_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- dispensacion_linea_lote
ALTER TABLE sch_dispensacion.dispensacion_linea_lote ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.dispensacion_linea_lote FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_dispensacion_linea_lote_tenant ON sch_dispensacion.dispensacion_linea_lote
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- receta_controlada
ALTER TABLE sch_dispensacion.receta_controlada ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.receta_controlada FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_receta_controlada_tenant ON sch_dispensacion.receta_controlada
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- movimiento_controlado (Libro Oficial)
ALTER TABLE sch_dispensacion.movimiento_controlado ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.movimiento_controlado FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_movimiento_controlado_tenant ON sch_dispensacion.movimiento_controlado
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- balance_controlado
ALTER TABLE sch_dispensacion.balance_controlado ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.balance_controlado FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_balance_controlado_tenant ON sch_dispensacion.balance_controlado
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- balance_controlado_linea
ALTER TABLE sch_dispensacion.balance_controlado_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_dispensacion.balance_controlado_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_balance_controlado_linea_tenant ON sch_dispensacion.balance_controlado_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 9: sch_facturacion
-- ============================================================================

-- comprobante_electronico
ALTER TABLE sch_facturacion.comprobante_electronico ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_facturacion.comprobante_electronico FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_comprobante_electronico_tenant ON sch_facturacion.comprobante_electronico
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- cpe_envio_intento
ALTER TABLE sch_facturacion.cpe_envio_intento ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_facturacion.cpe_envio_intento FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_cpe_envio_intento_tenant ON sch_facturacion.cpe_envio_intento
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- nota_credito_electronica
ALTER TABLE sch_facturacion.nota_credito_electronica ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_facturacion.nota_credito_electronica FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_nota_credito_electronica_tenant ON sch_facturacion.nota_credito_electronica
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 10: sch_vigilancia
-- ============================================================================

-- caso_recall
ALTER TABLE sch_vigilancia.caso_recall ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_vigilancia.caso_recall FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_caso_recall_tenant ON sch_vigilancia.caso_recall
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- recall_producto_lote
ALTER TABLE sch_vigilancia.recall_producto_lote ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_vigilancia.recall_producto_lote FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_recall_producto_lote_tenant ON sch_vigilancia.recall_producto_lote
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- recall_establecimiento
ALTER TABLE sch_vigilancia.recall_establecimiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_vigilancia.recall_establecimiento FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_recall_establecimiento_tenant ON sch_vigilancia.recall_establecimiento
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- recall_accion
ALTER TABLE sch_vigilancia.recall_accion ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_vigilancia.recall_accion FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_recall_accion_tenant ON sch_vigilancia.recall_accion
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- reporte_seguridad (Farmacovigilancia / Tecnovigilancia)
ALTER TABLE sch_vigilancia.reporte_seguridad ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_vigilancia.reporte_seguridad FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_reporte_seguridad_tenant ON sch_vigilancia.reporte_seguridad
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- reporte_seguridad_producto
ALTER TABLE sch_vigilancia.reporte_seguridad_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_vigilancia.reporte_seguridad_producto FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_reporte_seguridad_producto_tenant ON sch_vigilancia.reporte_seguridad_producto
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- reporte_seguridad_seguimiento
ALTER TABLE sch_vigilancia.reporte_seguridad_seguimiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_vigilancia.reporte_seguridad_seguimiento FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_reporte_seg_seguimiento_tenant ON sch_vigilancia.reporte_seguridad_seguimiento
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 11: sch_finanzas
-- ============================================================================

-- cuenta_bancaria_empresa
ALTER TABLE sch_finanzas.cuenta_bancaria_empresa ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.cuenta_bancaria_empresa FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_cuenta_bancaria_empresa_tenant ON sch_finanzas.cuenta_bancaria_empresa
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- factura_proveedor
ALTER TABLE sch_finanzas.factura_proveedor ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.factura_proveedor FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_factura_proveedor_tenant ON sch_finanzas.factura_proveedor
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- cuenta_por_pagar
ALTER TABLE sch_finanzas.cuenta_por_pagar ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.cuenta_por_pagar FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_cuenta_por_pagar_tenant ON sch_finanzas.cuenta_por_pagar
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- pago_cuenta_por_pagar
ALTER TABLE sch_finanzas.pago_cuenta_por_pagar ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.pago_cuenta_por_pagar FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_pago_cuenta_por_pagar_tenant ON sch_finanzas.pago_cuenta_por_pagar
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- cuenta_por_cobrar
ALTER TABLE sch_finanzas.cuenta_por_cobrar ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.cuenta_por_cobrar FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_cuenta_por_cobrar_tenant ON sch_finanzas.cuenta_por_cobrar
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- posting_retail (Outbox Contable)
ALTER TABLE sch_finanzas.posting_retail ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.posting_retail FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_posting_retail_tenant ON sch_finanzas.posting_retail
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- reporte_mensual_precios (Observatorio DIGEMID)
ALTER TABLE sch_finanzas.reporte_mensual_precios ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.reporte_mensual_precios FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_reporte_mensual_precios_tenant ON sch_finanzas.reporte_mensual_precios
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- reporte_mensual_precios_linea
ALTER TABLE sch_finanzas.reporte_mensual_precios_linea ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_finanzas.reporte_mensual_precios_linea FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_rep_mensual_precios_linea_tenant ON sch_finanzas.reporte_mensual_precios_linea
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 12: sch_auditoria
-- ============================================================================

-- evento_auditoria
ALTER TABLE sch_auditoria.evento_auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_auditoria.evento_auditoria FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_evento_auditoria_tenant ON sch_auditoria.evento_auditoria
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- acceso_dato_sensible
ALTER TABLE sch_auditoria.acceso_dato_sensible ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_auditoria.acceso_dato_sensible FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_acceso_dato_sensible_tenant ON sch_auditoria.acceso_dato_sensible
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- exportacion_datos
ALTER TABLE sch_auditoria.exportacion_datos ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_auditoria.exportacion_datos FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_exportacion_datos_tenant ON sch_auditoria.exportacion_datos
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 13: sch_integracion
-- ============================================================================

-- servicio_externo (TABLA HÍBRIDA: Conectores globales o del tenant)
ALTER TABLE sch_integracion.servicio_externo ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_integracion.servicio_externo FORCE ROW LEVEL SECURITY;

CREATE POLICY rls_servicio_externo_select ON sch_integracion.servicio_externo
    FOR SELECT
    USING (sch_admin.fn_is_superadmin() OR tenant_id IS NULL OR tenant_id = sch_admin.fn_current_tenant_id());

CREATE POLICY rls_servicio_externo_modify ON sch_integracion.servicio_externo
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- outbox_event
ALTER TABLE sch_integracion.outbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_integracion.outbox_event FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_outbox_event_tenant ON sch_integracion.outbox_event
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- inbox_message
ALTER TABLE sch_integracion.inbox_message ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_integracion.inbox_message FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_inbox_message_tenant ON sch_integracion.inbox_message
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- sync_checkpoint
ALTER TABLE sch_integracion.sync_checkpoint ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_integracion.sync_checkpoint FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_sync_checkpoint_tenant ON sch_integracion.sync_checkpoint
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- integracion_intento
ALTER TABLE sch_integracion.integracion_intento ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_integracion.integracion_intento FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_integracion_intento_tenant ON sch_integracion.integracion_intento
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());


-- ============================================================================
-- ESQUEMA 14: sch_app
-- ============================================================================

-- plantilla_notificacion (TABLA HÍBRIDA: Plantillas globales del SaaS o custom del tenant)
ALTER TABLE sch_app.plantilla_notificacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_app.plantilla_notificacion FORCE ROW LEVEL SECURITY;

CREATE POLICY rls_plantilla_notificacion_select ON sch_app.plantilla_notificacion
    FOR SELECT
    USING (sch_admin.fn_is_superadmin() OR tenant_id IS NULL OR tenant_id = sch_admin.fn_current_tenant_id());

CREATE POLICY rls_plantilla_notificacion_modify ON sch_app.plantilla_notificacion
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());

-- notificacion
ALTER TABLE sch_app.notificacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE sch_app.notificacion FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_notificacion_tenant ON sch_app.notificacion
    FOR ALL
    USING (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id())
    WITH CHECK (sch_admin.fn_is_superadmin() OR tenant_id = sch_admin.fn_current_tenant_id());