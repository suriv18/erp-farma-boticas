INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('inventario.posiciones.consultar', 'POSICION_INVENTARIO', 'CONSULTAR', 'Consultar posiciones de inventario',
     'Permite consultar el stock por almacen, SKU y lote.', FALSE),
    ('inventario.lotes.consultar', 'LOTE', 'CONSULTAR', 'Consultar lotes',
     'Permite consultar el detalle y la vendibilidad de un lote.', FALSE),
    ('inventario.lotes.bloquear', 'LOTE', 'BLOQUEAR', 'Bloquear y desbloquear lotes',
     'Permite bloquear un lote para impedir su venta y levantar el bloqueo.', TRUE),
    ('inventario.movimientos.registrar', 'MOVIMIENTO_INVENTARIO', 'REGISTRAR', 'Registrar movimientos de inventario',
     'Permite registrar ingresos y salidas de ajuste que actualizan el stock y el kardex.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'INVENTARIO'
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
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'inventario.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
