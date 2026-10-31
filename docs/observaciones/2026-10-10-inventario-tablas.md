# Inventario de tablas de las migraciones

Fecha de revisión: 10 de octubre de 2026.

Se enumeran las 116 declaraciones `CREATE TABLE` encontradas en 38 migraciones versionadas. El inventario se extrajo de los archivos SQL locales; no se consultó una base desplegada. No incluye `flyway_schema_history`, creada por Flyway. Las migraciones posteriores pueden modificar columnas, restricciones e índices de las tablas enumeradas.

Las fuentes de ejecución son `classpath:migrations` y `classpath:db/migration`, según [application.yaml](../../service-botica/bootstrap-app/src/main/resources/application.yaml). Los SQL consolidados y las propuestas fuera de esas ubicaciones no deben sumarse como tablas adicionales.

Ver el [análisis por módulos](2026-10-10-revision-integral-modulos.md) y la [comparativa de mercado](2026-10-10-comparativa-mercado.md) para distinguir tablas diseñadas de procesos implementados.

## sch_admin — 1 tabla

| Tabla | Migración de creación |
| --- | --- |
| `sch_admin.tenant` | [V001__base_esquemas_tenant.sql](../../docs/cadena-farmacias-docs/database/migrations/V001__base_esquemas_tenant.sql) |

## sch_organizacion — 8 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_organizacion.empresa_operadora` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |
| `sch_organizacion.establecimiento_farmaceutico` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |
| `sch_organizacion.establecimiento_autorizacion_sanitaria` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |
| `sch_organizacion.almacen` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |
| `sch_organizacion.ubicacion_almacen` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |
| `sch_organizacion.terminal_pos` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |
| `sch_organizacion.profesional_farmaceutico` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |
| `sch_organizacion.asignacion_profesional` | [V002__organizacion_establecimientos.sql](../../docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql) |

## sch_catalogo — 14 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_catalogo.condicion_venta` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.forma_farmaceutica` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.via_administracion` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.unidad_medida` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.clasificacion_controlada` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.principio_activo` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.producto_regulado` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.producto_principio_activo` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.categoria_producto` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.marca` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.rubro_comercial` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.sku_comercial` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.sku_codigo_barra` | [V003__catalogo_farmaceutico.sql](../../docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql) |
| `sch_catalogo.tipo_documento_identidad` | [V018__catalogo_tipo_documento_identidad_sunat.sql](../../docs/cadena-farmacias-docs/database/migrations/V018__catalogo_tipo_documento_identidad_sunat.sql) |

## sch_abastecimiento — 7 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_abastecimiento.proveedor` | [V004__compras_proveedores.sql](../../docs/cadena-farmacias-docs/database/migrations/V004__compras_proveedores.sql) |
| `sch_abastecimiento.solicitud_compra` | [V004__compras_proveedores.sql](../../docs/cadena-farmacias-docs/database/migrations/V004__compras_proveedores.sql) |
| `sch_abastecimiento.solicitud_compra_linea` | [V004__compras_proveedores.sql](../../docs/cadena-farmacias-docs/database/migrations/V004__compras_proveedores.sql) |
| `sch_abastecimiento.orden_compra` | [V004__compras_proveedores.sql](../../docs/cadena-farmacias-docs/database/migrations/V004__compras_proveedores.sql) |
| `sch_abastecimiento.orden_compra_linea` | [V004__compras_proveedores.sql](../../docs/cadena-farmacias-docs/database/migrations/V004__compras_proveedores.sql) |
| `sch_abastecimiento.recepcion_compra` | [V004__compras_proveedores.sql](../../docs/cadena-farmacias-docs/database/migrations/V004__compras_proveedores.sql) |
| `sch_abastecimiento.recepcion_compra_linea` | [V004__compras_proveedores.sql](../../docs/cadena-farmacias-docs/database/migrations/V004__compras_proveedores.sql) |

## sch_inventario — 12 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_inventario.lote` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.posicion_inventario` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.movimiento_inventario` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.reserva_inventario` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.transferencia_inventario` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.transferencia_linea` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.transferencia_despacho` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.transferencia_despacho_linea` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.transferencia_recepcion` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.transferencia_recepcion_linea` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.conteo_inventario` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |
| `sch_inventario.conteo_inventario_linea` | [V005__inventario_lotes_transferencias.sql](../../docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql) |

## sch_precio — 6 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_precio.lista_precio` | [V006__precios_promociones.sql](../../docs/cadena-farmacias-docs/database/migrations/V006__precios_promociones.sql) |
| `sch_precio.lista_precio_version` | [V006__precios_promociones.sql](../../docs/cadena-farmacias-docs/database/migrations/V006__precios_promociones.sql) |
| `sch_precio.lista_precio_item` | [V006__precios_promociones.sql](../../docs/cadena-farmacias-docs/database/migrations/V006__precios_promociones.sql) |
| `sch_precio.promocion` | [V006__precios_promociones.sql](../../docs/cadena-farmacias-docs/database/migrations/V006__precios_promociones.sql) |
| `sch_precio.promocion_version` | [V006__precios_promociones.sql](../../docs/cadena-farmacias-docs/database/migrations/V006__precios_promociones.sql) |
| `sch_precio.promocion_objetivo` | [V006__precios_promociones.sql](../../docs/cadena-farmacias-docs/database/migrations/V006__precios_promociones.sql) |

## sch_venta — 13 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_venta.cliente` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.cliente_consentimiento` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.medio_pago` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.turno_caja` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.movimiento_caja` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.venta` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.venta_linea` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.venta_linea_lote` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.pago_venta` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.devolucion_comercial` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.devolucion_comercial_linea` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.devolucion_linea_lote` | [V007__retail_pos_ventas.sql](../../docs/cadena-farmacias-docs/database/migrations/V007__retail_pos_ventas.sql) |
| `sch_venta.secuencia_operacion` | [V033__ventas_turno_venta_base.sql](../../service-botica/bootstrap-app/src/main/resources/db/migration/V033__ventas_turno_venta_base.sql) |

## sch_dispensacion — 9 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_dispensacion.prescripcion` | [V008__prescripcion_dispensacion.sql](../../docs/cadena-farmacias-docs/database/migrations/V008__prescripcion_dispensacion.sql) |
| `sch_dispensacion.prescripcion_linea` | [V008__prescripcion_dispensacion.sql](../../docs/cadena-farmacias-docs/database/migrations/V008__prescripcion_dispensacion.sql) |
| `sch_dispensacion.dispensacion` | [V008__prescripcion_dispensacion.sql](../../docs/cadena-farmacias-docs/database/migrations/V008__prescripcion_dispensacion.sql) |
| `sch_dispensacion.dispensacion_linea` | [V008__prescripcion_dispensacion.sql](../../docs/cadena-farmacias-docs/database/migrations/V008__prescripcion_dispensacion.sql) |
| `sch_dispensacion.dispensacion_linea_lote` | [V008__prescripcion_dispensacion.sql](../../docs/cadena-farmacias-docs/database/migrations/V008__prescripcion_dispensacion.sql) |
| `sch_dispensacion.receta_controlada` | [V009__productos_controlados.sql](../../docs/cadena-farmacias-docs/database/migrations/V009__productos_controlados.sql) |
| `sch_dispensacion.movimiento_controlado` | [V009__productos_controlados.sql](../../docs/cadena-farmacias-docs/database/migrations/V009__productos_controlados.sql) |
| `sch_dispensacion.balance_controlado` | [V009__productos_controlados.sql](../../docs/cadena-farmacias-docs/database/migrations/V009__productos_controlados.sql) |
| `sch_dispensacion.balance_controlado_linea` | [V009__productos_controlados.sql](../../docs/cadena-farmacias-docs/database/migrations/V009__productos_controlados.sql) |

## sch_facturacion — 3 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_facturacion.comprobante_electronico` | [V010__fiscal_cpe.sql](../../docs/cadena-farmacias-docs/database/migrations/V010__fiscal_cpe.sql) |
| `sch_facturacion.cpe_envio_intento` | [V010__fiscal_cpe.sql](../../docs/cadena-farmacias-docs/database/migrations/V010__fiscal_cpe.sql) |
| `sch_facturacion.nota_credito_electronica` | [V010__fiscal_cpe.sql](../../docs/cadena-farmacias-docs/database/migrations/V010__fiscal_cpe.sql) |

## sch_vigilancia — 7 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_vigilancia.caso_recall` | [V011__recall_farmacovigilancia.sql](../../docs/cadena-farmacias-docs/database/migrations/V011__recall_farmacovigilancia.sql) |
| `sch_vigilancia.recall_producto_lote` | [V011__recall_farmacovigilancia.sql](../../docs/cadena-farmacias-docs/database/migrations/V011__recall_farmacovigilancia.sql) |
| `sch_vigilancia.recall_establecimiento` | [V011__recall_farmacovigilancia.sql](../../docs/cadena-farmacias-docs/database/migrations/V011__recall_farmacovigilancia.sql) |
| `sch_vigilancia.recall_accion` | [V011__recall_farmacovigilancia.sql](../../docs/cadena-farmacias-docs/database/migrations/V011__recall_farmacovigilancia.sql) |
| `sch_vigilancia.reporte_seguridad` | [V011__recall_farmacovigilancia.sql](../../docs/cadena-farmacias-docs/database/migrations/V011__recall_farmacovigilancia.sql) |
| `sch_vigilancia.reporte_seguridad_producto` | [V011__recall_farmacovigilancia.sql](../../docs/cadena-farmacias-docs/database/migrations/V011__recall_farmacovigilancia.sql) |
| `sch_vigilancia.reporte_seguridad_seguimiento` | [V011__recall_farmacovigilancia.sql](../../docs/cadena-farmacias-docs/database/migrations/V011__recall_farmacovigilancia.sql) |

## sch_finanzas — 8 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_finanzas.cuenta_bancaria_empresa` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |
| `sch_finanzas.factura_proveedor` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |
| `sch_finanzas.cuenta_por_pagar` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |
| `sch_finanzas.pago_cuenta_por_pagar` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |
| `sch_finanzas.cuenta_por_cobrar` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |
| `sch_finanzas.posting_retail` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |
| `sch_finanzas.reporte_mensual_precios` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |
| `sch_finanzas.reporte_mensual_precios_linea` | [V012__erp_observatorio.sql](../../docs/cadena-farmacias-docs/database/migrations/V012__erp_observatorio.sql) |

## sch_seguridad — 13 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_seguridad.identidad` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.membership` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.identidad_externa` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.modulo_sistema` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.rol` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.permiso` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.rol_permiso` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.credencial_local` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.token_refresh` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.token_recuperacion_password` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.dispositivo_tienda` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.usuario_rol_ambito` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |
| `sch_seguridad.sesion_usuario` | [V013__seguridad_iam.sql](../../docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql) |

## sch_auditoria — 3 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_auditoria.evento_auditoria` | [V014__auditoria.sql](../../docs/cadena-farmacias-docs/database/migrations/V014__auditoria.sql) |
| `sch_auditoria.acceso_dato_sensible` | [V014__auditoria.sql](../../docs/cadena-farmacias-docs/database/migrations/V014__auditoria.sql) |
| `sch_auditoria.exportacion_datos` | [V014__auditoria.sql](../../docs/cadena-farmacias-docs/database/migrations/V014__auditoria.sql) |

## sch_integracion — 5 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_integracion.servicio_externo` | [V015__integracion_outbox_inbox.sql](../../docs/cadena-farmacias-docs/database/migrations/V015__integracion_outbox_inbox.sql) |
| `sch_integracion.outbox_event` | [V015__integracion_outbox_inbox.sql](../../docs/cadena-farmacias-docs/database/migrations/V015__integracion_outbox_inbox.sql) |
| `sch_integracion.inbox_message` | [V015__integracion_outbox_inbox.sql](../../docs/cadena-farmacias-docs/database/migrations/V015__integracion_outbox_inbox.sql) |
| `sch_integracion.sync_checkpoint` | [V015__integracion_outbox_inbox.sql](../../docs/cadena-farmacias-docs/database/migrations/V015__integracion_outbox_inbox.sql) |
| `sch_integracion.integracion_intento` | [V015__integracion_outbox_inbox.sql](../../docs/cadena-farmacias-docs/database/migrations/V015__integracion_outbox_inbox.sql) |

## sch_app — 6 tablas

| Tabla | Migración de creación |
| --- | --- |
| `sch_app.menu_navegacion` | [V016__navegacion_dinamica_rbac.sql](../../docs/cadena-farmacias-docs/database/migrations/V016__navegacion_dinamica_rbac.sql) |
| `sch_app.menu_navegacion_permiso` | [V016__navegacion_dinamica_rbac.sql](../../docs/cadena-farmacias-docs/database/migrations/V016__navegacion_dinamica_rbac.sql) |
| `sch_app.plantilla_notificacion` | [V017__notificaciones_feature_flags.sql](../../docs/cadena-farmacias-docs/database/migrations/V017__notificaciones_feature_flags.sql) |
| `sch_app.notificacion` | [V017__notificaciones_feature_flags.sql](../../docs/cadena-farmacias-docs/database/migrations/V017__notificaciones_feature_flags.sql) |
| `sch_app.feature_flag` | [V017__notificaciones_feature_flags.sql](../../docs/cadena-farmacias-docs/database/migrations/V017__notificaciones_feature_flags.sql) |
| `sch_app.app_version` | [V017__notificaciones_feature_flags.sql](../../docs/cadena-farmacias-docs/database/migrations/V017__notificaciones_feature_flags.sql) |

## Sin esquema explícito — 1 tabla

| Tabla | Migración de creación |
| --- | --- |
| `event_publication` | [V020__create_modulith_event_publication.sql](../../service-botica/bootstrap-app/src/main/resources/db/migration/V020__create_modulith_event_publication.sql) |

## Observaciones sobre integridad y uso

- Las FK compuestas con tenant y ámbito organizacional aportan aislamiento estructural, pero no sustituyen la autorización del usuario sobre cada recurso.
- `posicion_inventario` y `movimiento_inventario` referencian SKU y lote por separado: falta imponer que el lote corresponda al SKU. Véase OBS-005 del [informe inicial](2026-10-10-revision-erp-botica.md).
- Los campos de costo del inventario tienen valores predeterminados en cero y el flujo de recepción revisado no los alimenta: OBS-007 del análisis integral.
- El esquema exige lote y vencimiento para flujos que el catálogo permite declarar sin ellos: OBS-010.
- Las tablas de precios, prescripción, controlados, CPE, vigilancia, finanzas, integración y notificaciones representan diseño de datos; no acreditan servicios operativos.
- La presencia de `event_publication` no acredita publicación, consumo ni reintentos de eventos de negocio. Su esquema efectivo depende del esquema de ejecución porque su DDL no lo califica.
- No se encontraron sentencias para activar RLS en las 38 migraciones enumeradas. Existen scripts de política fuera de las ubicaciones Flyway; no se puede afirmar que estén aplicados en una instalación real.
- Las migraciones V026 y V029–V038 agregan, entre otras cosas, unicidad de series, idempotencia, ampliación de actores, anulación y precio de referencia. No basta revisar solo las V001–V018 para reconstruir el modelo efectivo.

## Validación pendiente de base de datos

1. Ejecutar todas las migraciones en PostgreSQL 18 vacío y validar el esquema efectivo y el historial Flyway.
2. Probar actualización desde una versión anterior con datos representativos, sin modificar migraciones ya aplicadas.
3. Verificar las restricciones con intentos controlados de cruces de tenant, empresa, establecimiento, SKU y lote.
4. Conciliar cantidades y costos entre recepción, posiciones, movimientos, ventas, anulación y cierre de caja.
5. Evaluar planes de ejecución y latencia con un volumen representativo; la existencia de índices no demuestra rendimiento.
6. Comprobar roles de conexión y RLS realmente activos, copias de seguridad y restauración. No ejecutar cambios de políticas a ciegas sobre producción.

