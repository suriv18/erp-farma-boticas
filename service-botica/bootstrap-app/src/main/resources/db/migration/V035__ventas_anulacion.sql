ALTER TABLE sch_venta.venta
    ADD COLUMN anulada_at TIMESTAMPTZ,
    ADD COLUMN anulada_por_usuario_id BIGINT,
    ADD COLUMN motivo_anulacion VARCHAR(500),
    ADD CONSTRAINT ck_venta_anulacion CHECK ((estado = 'ANULADA') = (anulada_at IS NOT NULL));

INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('ventas.ventas.anular', 'VENTA', 'ANULAR', 'Anular ventas',
     'Permite anular una venta confirmada mientras su turno de caja sigue abierto y reintegrar el stock.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'VENTAS'
ON CONFLICT (codigo) WHERE es_activo = '1' DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';

INSERT INTO sch_seguridad.rol_permiso
    (tenant_id, rol_id, permiso_id)
SELECT r.tenant_id, r.id, p.id
FROM sch_seguridad.rol r
JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'ventas.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
