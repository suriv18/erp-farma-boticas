INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('catalogo.soporte.consultar', 'CATALOGO_SOPORTE', 'CONSULTAR', 'Consultar catalogos de soporte',
     'Permite consultar condicion de venta, forma farmaceutica, via de administracion, unidad de medida y clasificacion controlada.', FALSE),
    ('catalogo.soporte.gestionar', 'CATALOGO_SOPORTE', 'GESTIONAR', 'Gestionar catalogos de soporte',
     'Permite crear, editar y cambiar el estado de los catalogos de soporte regulatorio.', TRUE),
    ('catalogo.principios-activos.consultar', 'PRINCIPIO_ACTIVO', 'CONSULTAR', 'Consultar principios activos',
     'Permite consultar el catalogo de principios activos.', FALSE),
    ('catalogo.principios-activos.gestionar', 'PRINCIPIO_ACTIVO', 'GESTIONAR', 'Gestionar principios activos',
     'Permite crear, editar y cambiar el estado de principios activos.', TRUE),
    ('catalogo.marcas.consultar', 'MARCA', 'CONSULTAR', 'Consultar marcas',
     'Permite consultar las marcas registradas por el tenant.', FALSE),
    ('catalogo.marcas.gestionar', 'MARCA', 'GESTIONAR', 'Gestionar marcas',
     'Permite crear, editar y cambiar el estado de marcas.', TRUE),
    ('catalogo.categorias.consultar', 'CATEGORIA_PRODUCTO', 'CONSULTAR', 'Consultar categorias',
     'Permite consultar las categorias de producto del tenant.', FALSE),
    ('catalogo.categorias.gestionar', 'CATEGORIA_PRODUCTO', 'GESTIONAR', 'Gestionar categorias',
     'Permite crear, editar y cambiar el estado de categorias de producto.', TRUE),
    ('catalogo.productos-regulados.consultar', 'PRODUCTO_REGULADO', 'CONSULTAR', 'Consultar productos regulados',
     'Permite consultar la ficha regulatoria de productos.', FALSE),
    ('catalogo.productos-regulados.gestionar', 'PRODUCTO_REGULADO', 'GESTIONAR', 'Gestionar productos regulados',
     'Permite crear, editar y cambiar el estado de la ficha regulatoria de productos.', TRUE),
    ('catalogo.skus.consultar', 'SKU_COMERCIAL', 'CONSULTAR', 'Consultar SKU comerciales',
     'Permite consultar los SKU comerciales del tenant.', FALSE),
    ('catalogo.skus.gestionar', 'SKU_COMERCIAL', 'GESTIONAR', 'Gestionar SKU comerciales',
     'Permite crear, editar, cambiar el estado y administrar codigos de barra de SKU comerciales.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'CATALOGO'
ON CONFLICT (codigo) DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';
