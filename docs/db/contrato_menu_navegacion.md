# Contrato de navegación dinámica

La navegación visual y la autorización tienen responsabilidades distintas:

- `security.modulo_sistema` identifica módulos funcionales para RBAC.
- `security.permiso` controla recursos y acciones.
- `app.menu_navegacion` define jerarquía, etiquetas, rutas, iconos y orden.
- `app.menu_navegacion_permiso` relaciona opciones visuales con permisos.

Ocultar una opción no autoriza ni desautoriza una operación. Cada endpoint del backend debe
validar nuevamente el permiso y el alcance de empresa, sucursal, almacén o caja.

## Endpoint propuesto

```http
GET /api/v1/me/navigation?application=ERP_WEB
```

El backend obtiene la identidad desde la sesión. El cliente no envía `usuarioId`, roles ni
permisos en la consulta.

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
          "code": "DASHBOARD",
          "label": "Resumen",
          "type": "ITEM",
          "path": "/dashboard",
          "icon": "LayoutDashboard",
          "order": 10,
          "children": []
        },
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
    "tenantId": "empresa-actual",
    "branchId": "sucursal-actual"
  }
}
```

No se exponen `menu_id`, `permiso_id` ni la estructura de roles. Los identificadores
técnicos de base de datos no son parte del contrato público.

## Activación para roles

La migración crea los permisos de entrada, pero no los concede automáticamente. Un proceso
de bootstrap administrativo debe asociarlos mediante `security.rol_permiso` a los roles
correspondientes. Esta decisión evita elevar privilegios existentes de manera silenciosa.

`DASHBOARD` utiliza `AUTENTICADO` y puede mostrarse a cualquier sesión interna válida. Los
demás elementos requieren su permiso `*:MODULO:VER`.

## Reglas del backend

1. Seleccionar únicamente registros activos, visibles y no eliminados.
2. Incluir `AUTENTICADO` cuando existe una sesión válida.
3. Para `CUALQUIERA`, exigir al menos un permiso relacionado.
4. Para `TODOS`, exigir todos los permisos relacionados y activos.
5. Eliminar grupos que queden sin hijos visibles.
6. Ordenar siempre por `orden`, luego por `etiqueta` y finalmente por `codigo`.
7. Resolver permisos considerando los roles y alcances vigentes del usuario.
8. No construir SQL a partir del parámetro `application`; utilizar un valor validado.
9. Incorporar empresa y sucursal a la clave de caché cuando cambien el resultado.
10. La base impide ciclos, padres de otra aplicación y padres que no sean de tipo `GRUPO`.

## Reglas de `erp-web`

1. Mantener un mapa cerrado entre claves de icono y componentes de `lucide-react`.
2. Utilizar un icono de respaldo cuando la clave no sea reconocida.
3. No ejecutar HTML, imports dinámicos ni nombres de componentes recibidos del backend.
4. React Router continúa declarando las rutas válidas durante el build.
5. El menú recibido solo determina visibilidad y navegación, no autorización.
6. Al cambiar de sesión, empresa o sucursal se invalida la consulta de navegación.

## Evolución

El menú se crea inicialmente para `ERP_WEB`. Los valores `POS_WEB`, `ECOMMERCE_WEB` y
`MOBILE` están reservados para aplicaciones desplegables reales. No deben poblarse antes de
que esos consumidores existan.
