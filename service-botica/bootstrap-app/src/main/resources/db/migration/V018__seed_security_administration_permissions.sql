INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('seguridad.usuarios.consultar', 'USUARIO', 'CONSULTAR', 'Consultar usuarios',
     'Permite consultar perfiles IAM locales.', FALSE),
    ('seguridad.usuarios.gestionar', 'USUARIO', 'GESTIONAR', 'Gestionar usuarios',
     'Permite crear y administrar perfiles IAM locales.', TRUE),
    ('seguridad.identidades.gestionar', 'IDENTIDAD_EXTERNA', 'GESTIONAR', 'Gestionar identidades externas',
     'Permite vincular y desvincular identidades externas de un usuario.', TRUE),
    ('seguridad.roles.consultar', 'ROL', 'CONSULTAR', 'Consultar roles',
     'Permite consultar roles y sus permisos.', FALSE),
    ('seguridad.roles.gestionar', 'ROL', 'GESTIONAR', 'Gestionar roles',
     'Permite crear y administrar roles.', TRUE),
    ('seguridad.roles.asignar', 'ROL', 'ASIGNAR', 'Asignar roles',
     'Permite asignar roles a usuarios dentro de un ambito.', TRUE),
    ('seguridad.permisos.consultar', 'PERMISO', 'CONSULTAR', 'Consultar permisos',
     'Permite consultar el catalogo de permisos.', FALSE),
    ('seguridad.permisos.asignar', 'PERMISO', 'ASIGNAR', 'Asignar permisos',
     'Permite reemplazar los permisos asociados a un rol.', TRUE),
    ('seguridad.modulos.consultar', 'MODULO', 'CONSULTAR', 'Consultar modulos',
     'Permite consultar los modulos que agrupan el catalogo de permisos.', FALSE),
    ('seguridad.sesiones.consultar', 'SESION', 'CONSULTAR', 'Consultar sesiones',
     'Permite consultar las sesiones locales de acceso.', TRUE),
    ('seguridad.sesiones.revocar', 'SESION', 'REVOCAR', 'Revocar sesiones',
     'Permite invalidar una sesion local activa.', TRUE),
    ('seguridad.dispositivos.consultar', 'DISPOSITIVO', 'CONSULTAR', 'Consultar dispositivos',
     'Permite consultar dispositivos registrados para operacion en tienda.', TRUE),
    ('seguridad.dispositivos.gestionar', 'DISPOSITIVO', 'GESTIONAR', 'Gestionar dispositivos',
     'Permite registrar y cambiar la confianza de dispositivos de tienda.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'SEGURIDAD'
ON CONFLICT (codigo) DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';
