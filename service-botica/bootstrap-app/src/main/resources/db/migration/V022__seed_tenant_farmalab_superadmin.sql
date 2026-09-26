INSERT INTO sch_admin.tenant
    (codigo, nombre, slug)
VALUES
    ('FARMALAB', 'FarmaLab', 'farmalab');

INSERT INTO sch_seguridad.identidad
    (email, username, tipo_documento, numero_documento, nombres, apellidos, telefono)
VALUES
    ('wilton.sullcaray.r@gmail.com', 'wmsullcaray', '1', '48480505', 'Wilton Mijael', 'Sullcaray Riveros', '918215615');

INSERT INTO sch_seguridad.membership
    (tenant_id, identidad_id, nombre_mostrar)
SELECT t.id, i.id, 'Wilton Mijael Sullcaray Riveros'
FROM sch_admin.tenant t, sch_seguridad.identidad i
WHERE t.codigo = 'FARMALAB' AND i.email = 'wilton.sullcaray.r@gmail.com';

INSERT INTO sch_seguridad.credencial_local
    (membership_id, password_hash, requiere_cambio)
SELECT m.id, '$2a$12$FPwI8rBYBtpLRnVzvm.5iue01sOCd7Vau/Mi2Butz9aMUNjnl4ifq', FALSE
FROM sch_seguridad.membership m
JOIN sch_seguridad.identidad i ON i.id = m.identidad_id
WHERE i.email = 'wilton.sullcaray.r@gmail.com';

INSERT INTO sch_seguridad.rol
    (tenant_id, codigo, nombre, descripcion, tipo_rol, es_sistema)
SELECT t.id, 'ADMIN', 'Administrador global', 'Rol con acceso completo a todos los modulos del tenant.', 'GLOBAL', TRUE
FROM sch_admin.tenant t
WHERE t.codigo = 'FARMALAB';

INSERT INTO sch_seguridad.rol_permiso
    (tenant_id, rol_id, permiso_id)
SELECT r.tenant_id, r.id, p.id
FROM sch_seguridad.rol r
JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
CROSS JOIN sch_seguridad.permiso p
WHERE r.codigo = 'ADMIN' AND p.es_activo = '1';

INSERT INTO sch_seguridad.usuario_rol_ambito
    (tenant_id, membership_id, rol_id, tipo_ambito)
SELECT m.tenant_id, m.id, r.id, 'GLOBAL'
FROM sch_seguridad.membership m
JOIN sch_seguridad.identidad i ON i.id = m.identidad_id
JOIN sch_seguridad.rol r ON r.tenant_id = m.tenant_id AND r.codigo = 'ADMIN'
WHERE i.email = 'wilton.sullcaray.r@gmail.com';
