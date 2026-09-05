# API-FAR-007 — Contrato de navegación dinámica

## Principio

Navegación visual y autorización son responsabilidades distintas:

- `sch_seguridad.modulo_sistema`: capacidades funcionales.
- `sch_seguridad.permiso`: recursos/acciones.
- `sch_app.menu_navegacion`: jerarquía, etiqueta, ruta, icono y orden.
- `sch_app.menu_navegacion_permiso`: visibilidad según permisos.

Ocultar una opción **no autoriza ni desautoriza** una operación. El backend vuelve a validar permiso, tenant, empresa, establecimiento, almacén/terminal y reglas de competencia profesional.

## Endpoint candidato

```http
GET /api/v1/me/navigation?application=ERP_WEB
```

La identidad se obtiene de la sesión/token validado. El cliente no envía `usuarioId`, roles ni permisos.

## Respuesta

```json
{
  "data": [
    {
      "code": "OPERACIONES",
      "label": "Operaciones",
      "type": "GROUP",
      "order": 10,
      "children": [
        {
          "code": "INVENTARIO",
          "label": "Inventario",
          "type": "ITEM",
          "path": "/inventario",
          "icon": "Boxes",
          "order": 30,
          "children": []
        }
      ]
    }
  ],
  "meta": {
    "application": "ERP_WEB",
    "tenantId": "tenant-actual",
    "companyId": "empresa-actual",
    "branchId": "establecimiento-actual"
  }
}
```

No se exponen IDs internos de menú, permiso o rol.

## Resolución

1. Solo activos y visibles.
2. `AUTENTICADO`: sesión válida.
3. `CUALQUIERA`: al menos un permiso relacionado.
4. `TODOS`: todos los permisos relacionados.
5. Eliminar grupos sin hijos visibles.
6. Ordenar por `orden`, `etiqueta`, `codigo`.
7. Resolver roles y ámbitos vigentes.
8. Validar `application` contra allow-list.
9. Cachear, si se usa, incluyendo tenant/empresa/establecimiento/usuario y una versión de permisos.
10. El backend autoriza nuevamente cada endpoint.

## Frontend

- usar registro cerrado de iconos;
- nunca ejecutar HTML/imports recibidos desde backend;
- las rutas válidas se declaran en el build/router;
- el menú solo decide visibilidad y navegación;
- invalidar navegación al cambiar sesión o ámbito.
