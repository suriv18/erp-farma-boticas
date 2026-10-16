INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('catalogo.rubros-comerciales.consultar', 'RUBRO_COMERCIAL', 'CONSULTAR', 'Consultar rubros comerciales',
     'Permite consultar los rubros comerciales del tenant.', FALSE),
    ('catalogo.rubros-comerciales.gestionar', 'RUBRO_COMERCIAL', 'GESTIONAR', 'Gestionar rubros comerciales',
     'Permite crear, editar y cambiar el estado de rubros comerciales.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'CATALOGO'
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
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'catalogo.rubros-comerciales.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
