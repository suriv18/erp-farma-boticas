INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('ventas.turnos.abrir', 'TURNO_CAJA', 'ABRIR', 'Abrir turno de caja',
     'Permite abrir un turno de caja en una terminal POS.', FALSE),
    ('ventas.turnos.cerrar', 'TURNO_CAJA', 'CERRAR', 'Cerrar turno de caja',
     'Permite cerrar un turno de caja declarando el efectivo contado.', TRUE),
    ('ventas.turnos.consultar', 'TURNO_CAJA', 'CONSULTAR', 'Consultar turnos de caja',
     'Permite consultar el turno actual y el detalle de un turno.', FALSE),
    ('ventas.ventas.registrar', 'VENTA', 'REGISTRAR', 'Registrar ventas',
     'Permite registrar ventas presenciales y descontar stock.', TRUE),
    ('ventas.ventas.consultar', 'VENTA', 'CONSULTAR', 'Consultar ventas',
     'Permite consultar y listar ventas.', FALSE)
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
