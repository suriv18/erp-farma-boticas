CREATE INDEX ix_venta_turno_caja ON sch_venta.venta(tenant_id, turno_caja_id) WHERE es_activo = '1';
