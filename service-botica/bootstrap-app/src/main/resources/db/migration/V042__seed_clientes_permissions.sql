INSERT INTO sch_seguridad.modulo_sistema (codigo, nombre, descripcion, orden, es_activo)
VALUES ('CLIENTES', 'Clientes', 'Clientes retail y sus consentimientos de tratamiento de datos.', 75, '1')
ON CONFLICT (codigo) WHERE es_activo = '1' DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    orden = EXCLUDED.orden,
    es_activo = '1';

INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('clientes.clientes.consultar', 'CLIENTE', 'CONSULTAR', 'Consultar clientes',
     'Permite buscar y consultar clientes.', FALSE),
    ('clientes.clientes.registrar', 'CLIENTE', 'REGISTRAR', 'Registrar clientes',
     'Permite registrar clientes con sus datos mínimos.', FALSE),
    ('clientes.clientes.editar', 'CLIENTE', 'EDITAR', 'Editar clientes',
     'Permite corregir los datos de un cliente.', FALSE),
    ('clientes.clientes.bloquear', 'CLIENTE', 'BLOQUEAR', 'Bloquear clientes',
     'Permite bloquear y desbloquear clientes con su motivo.', TRUE),
    ('clientes.consentimientos.gestionar', 'CONSENTIMIENTO', 'GESTIONAR', 'Gestionar consentimientos',
     'Permite otorgar y revocar consentimientos de tratamiento de datos.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'CLIENTES' AND m.es_activo = '1'
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
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'clientes.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
