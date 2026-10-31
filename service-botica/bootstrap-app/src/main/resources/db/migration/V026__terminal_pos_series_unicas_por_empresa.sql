CREATE UNIQUE INDEX uk_terminal_pos_serie_boleta
    ON sch_organizacion.terminal_pos (tenant_id, empresa_id, serie_boleta_defecto)
    WHERE es_activo = '1' AND serie_boleta_defecto IS NOT NULL;

CREATE UNIQUE INDEX uk_terminal_pos_serie_factura
    ON sch_organizacion.terminal_pos (tenant_id, empresa_id, serie_factura_defecto)
    WHERE es_activo = '1' AND serie_factura_defecto IS NOT NULL;
