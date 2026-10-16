INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('organizacion.empresas.consultar', 'EMPRESA_OPERADORA', 'CONSULTAR', 'Consultar empresas operadoras',
     'Permite consultar las empresas operadoras del tenant.', FALSE),
    ('organizacion.empresas.gestionar', 'EMPRESA_OPERADORA', 'GESTIONAR', 'Gestionar empresas operadoras',
     'Permite crear, editar y cambiar el estado de empresas operadoras.', TRUE),
    ('organizacion.establecimientos.consultar', 'ESTABLECIMIENTO', 'CONSULTAR', 'Consultar establecimientos',
     'Permite consultar los establecimientos farmaceuticos del tenant.', FALSE),
    ('organizacion.establecimientos.gestionar', 'ESTABLECIMIENTO', 'GESTIONAR', 'Gestionar establecimientos',
     'Permite crear, editar y cambiar el estado de establecimientos farmaceuticos.', TRUE),
    ('organizacion.almacenes.consultar', 'ALMACEN', 'CONSULTAR', 'Consultar almacenes',
     'Permite consultar los almacenes del tenant.', FALSE),
    ('organizacion.almacenes.gestionar', 'ALMACEN', 'GESTIONAR', 'Gestionar almacenes',
     'Permite crear, editar y cambiar el estado de almacenes.', TRUE),
    ('organizacion.terminales-pos.consultar', 'TERMINAL_POS', 'CONSULTAR', 'Consultar terminales POS',
     'Permite consultar los terminales de punto de venta del tenant.', FALSE),
    ('organizacion.terminales-pos.gestionar', 'TERMINAL_POS', 'GESTIONAR', 'Gestionar terminales POS',
     'Permite crear, editar y cambiar el estado de terminales de punto de venta.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'ORGANIZACION'
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
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'organizacion.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
