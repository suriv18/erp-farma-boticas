CREATE UNIQUE INDEX uk_movimiento_business_uuid
    ON sch_inventario.movimiento_inventario (tenant_id, business_uuid)
    WHERE business_uuid IS NOT NULL;
