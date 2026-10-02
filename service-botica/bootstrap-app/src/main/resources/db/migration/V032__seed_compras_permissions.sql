INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('compras.proveedores.consultar', 'PROVEEDOR', 'CONSULTAR', 'Consultar proveedores',
     'Permite consultar y listar proveedores.', FALSE),
    ('compras.proveedores.gestionar', 'PROVEEDOR', 'GESTIONAR', 'Gestionar proveedores',
     'Permite crear, actualizar y cambiar el estado de proveedores.', TRUE),
    ('compras.ordenes.consultar', 'ORDEN_COMPRA', 'CONSULTAR', 'Consultar ordenes de compra',
     'Permite consultar y listar ordenes de compra con su avance de recepcion.', FALSE),
    ('compras.ordenes.crear', 'ORDEN_COMPRA', 'CREAR', 'Crear ordenes de compra',
     'Permite registrar ordenes de compra en borrador.', FALSE),
    ('compras.ordenes.aprobar', 'ORDEN_COMPRA', 'APROBAR', 'Aprobar y emitir ordenes de compra',
     'Permite aprobar y emitir ordenes de compra.', TRUE),
    ('compras.ordenes.anular', 'ORDEN_COMPRA', 'ANULAR', 'Anular ordenes de compra',
     'Permite anular ordenes de compra que aun no tienen recepciones.', TRUE),
    ('compras.recepciones.consultar', 'RECEPCION_COMPRA', 'CONSULTAR', 'Consultar recepciones de compra',
     'Permite consultar recepciones de compra y sus lotes.', FALSE),
    ('compras.recepciones.registrar', 'RECEPCION_COMPRA', 'REGISTRAR', 'Registrar recepciones de compra',
     'Permite recibir mercaderia contra una orden de compra y generar el ingreso de stock.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'COMPRAS'
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
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'compras.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
