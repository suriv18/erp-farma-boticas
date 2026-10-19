ALTER TABLE sch_inventario.lote
    ADD CONSTRAINT uk_lote_tenant_sku_id UNIQUE (tenant_id, sku_id, id);

ALTER TABLE sch_inventario.posicion_inventario
    ADD CONSTRAINT fk_posicion_lote_sku FOREIGN KEY (tenant_id, sku_id, lote_id)
        REFERENCES sch_inventario.lote(tenant_id, sku_id, id),
    DROP CONSTRAINT fk_posicion_lote;

ALTER TABLE sch_inventario.movimiento_inventario
    ADD CONSTRAINT fk_movimiento_lote_sku FOREIGN KEY (tenant_id, sku_id, lote_id)
        REFERENCES sch_inventario.lote(tenant_id, sku_id, id),
    DROP CONSTRAINT fk_movimiento_lote;

ALTER TABLE sch_inventario.reserva_inventario
    ADD CONSTRAINT fk_reserva_lote_sku FOREIGN KEY (tenant_id, sku_id, lote_id)
        REFERENCES sch_inventario.lote(tenant_id, sku_id, id),
    DROP CONSTRAINT fk_reserva_lote;

ALTER TABLE sch_inventario.conteo_inventario_linea
    ADD CONSTRAINT fk_conteo_linea_lote_sku FOREIGN KEY (tenant_id, sku_id, lote_id)
        REFERENCES sch_inventario.lote(tenant_id, sku_id, id),
    DROP CONSTRAINT fk_conteo_linea_lote;

ALTER TABLE sch_dispensacion.movimiento_controlado
    ADD CONSTRAINT fk_mov_ctrl_lote_sku FOREIGN KEY (tenant_id, sku_id, lote_id)
        REFERENCES sch_inventario.lote(tenant_id, sku_id, id),
    DROP CONSTRAINT fk_mov_ctrl_lote;
