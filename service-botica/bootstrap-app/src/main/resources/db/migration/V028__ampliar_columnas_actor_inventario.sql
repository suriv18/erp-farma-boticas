ALTER TABLE sch_inventario.lote
    ALTER COLUMN bloqueado_por TYPE VARCHAR(36),
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_inventario.posicion_inventario
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_inventario.movimiento_inventario
    ALTER COLUMN actor TYPE VARCHAR(36);

ALTER TABLE sch_inventario.reserva_inventario
    ALTER COLUMN created_by TYPE VARCHAR(36);

ALTER TABLE sch_inventario.transferencia_inventario
    ALTER COLUMN aprobado_por TYPE VARCHAR(36),
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_inventario.transferencia_linea
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_inventario.transferencia_despacho
    ALTER COLUMN actor TYPE VARCHAR(36),
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_inventario.transferencia_recepcion
    ALTER COLUMN actor TYPE VARCHAR(36),
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_inventario.conteo_inventario
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);
