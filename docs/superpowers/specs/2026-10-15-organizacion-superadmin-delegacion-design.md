# Organización, superadmin y delegación segura de usuarios — diseño

Fecha: 2026-10-15. Estado: aprobado por el dueño del producto.
Alcance: subproyectos **A** (endurecimiento del IAM) y **B1** (modelo de organización, superadmin, administrador de organización y delegación).
Insumos: `docs/observaciones/2026-10-15-analisis-dueno-y-administrador-de-organizacion.md`.

## 1. Contexto y decisiones

- `wilton.sullcaray.r@gmail.com` es el **dueño del sistema (superadmin)**.
- Una **organización** es el cliente y **administra sus empresas**. Hay **un solo tenant** para todos los clientes.
- La organización crea los usuarios de sus establecimientos, que trabajan **por turnos** (turnos rotativos por calendario: subproyecto C, fuera de este diseño).
- Aislamiento por fases: este diseño cubre IAM y organización (B1). El aislamiento en inventario, compras, catálogo y ventas es B2. **No se incorpora un segundo cliente real hasta cerrar B2.**

Estado actual verificado (BD y código):

- Jerarquía: `tenant → empresa_operadora → establecimiento_farmaceutico → almacen / terminal_pos (caja) → turno_caja`. No existe entidad organización ni grupo.
- Los permisos efectivos son la unión de todas las asignaciones del usuario y **no consideran el ámbito**; solo `ventas` valida ámbito (`AutorizacionAmbito`).
- Quien tenga `seguridad.roles.asignar` puede asignar `ADMIN` con ámbito `GLOBAL`; quien tenga `seguridad.credenciales.gestionar` puede fijar la contraseña de cualquier usuario del tenant; `ReemplazarPermisosRolHandler` no protege roles de sistema; `CrearRolRequest` acepta `systemRole`.
- Los controladores de `security` toman `tenantId` del request sin compararlo con el `tid` del JWT.

## 2. Modelo de datos

- Tabla nueva `sch_organizacion.organizacion`: `id`, `uuid_publico`, `tenant_id`, `codigo`, `nombre`, `estado`, columnas de auditoría. Único `(tenant_id, codigo)`.
- `empresa_operadora.organizacion_id` obligatorio, FK compuesta con `tenant_id`.
- `usuario_rol_ambito`: nuevo `tipo_ambito = 'ORGANIZACION'` y columna `organizacion_id`; la restricción de coherencia (`ck_seg_ura_scope`) se amplía. Los ámbitos inferiores derivan su organización por la empresa.
- `membership.organizacion_id`: nulo = personal de la plataforma; con valor = usuario de esa organización. Se hereda del actor al crear y no cambia.
- `rol.tipo_rol` admite `ORGANIZACION`.

Jerarquía resultante:

```
plataforma (tenant, superadmin)
└─ organización (cliente)
   └─ empresa_operadora
      └─ establecimiento
         ├─ almacén
         └─ terminal POS (caja) → turno_caja
```

Rango (mayor a menor): `GLOBAL > ORGANIZACION > EMPRESA > ESTABLECIMIENTO > ALMACEN > TERMINAL`. `GLOBAL` pasa a significar plataforma.

## 3. Roles y reglas de autorización

Roles:

- **Superadmin**: asignación `ADMIN` con ámbito `GLOBAL` (el código `ADMIN` no se renombra, para no romper semillas y tests). Único que crea organizaciones.
- **`ADMIN_ORGANIZACION`** (rol de sistema, tipo `ORGANIZACION`): crea y administra usuarios de su organización, asigna roles estándar, fija contraseñas, gestiona empresas y establecimientos de su organización. **No** crea ni edita roles ni asigna permisos.
- **`CAJERO`**: pasa a rol de sistema sembrado (hoy existe como rol manual con 9 permisos).

**`ActorScope`** (API pública del módulo `security`, mismo patrón que `AutorizacionAmbito`): dado el actor, calcula desde sus asignaciones vigentes si es global, su organización, empresas y establecimientos, y su rango más alto.

Reglas:

1. El `tenantId` sale siempre del token.
2. **Asignar rol:** el ámbito pedido debe estar contenido en el del actor; `GLOBAL` solo lo asigna un actor `GLOBAL`; los permisos del rol deben ser subconjunto de los del actor; los roles de sistema o `GLOBAL` solo los asigna un actor `GLOBAL`.
3. **Operar sobre un usuario** (contraseña, estado, sesiones, revocar asignaciones): el objetivo debe pertenecer a la organización del actor y tener rango inferior.
4. **Listado de usuarios:** filtrado por `membership.organizacion_id`; el superadmin ve todos.
5. **Organizaciones:** solo `GLOBAL` crea y modifica. **Empresas:** `GLOBAL`, o el administrador dentro de su organización.
6. **Roles de sistema** inmutables por API; el cliente no puede enviar `systemRole`.

## 4. Backend

### 4.1 Endurecimiento (A)

- Controladores de `security` (`UsuarioController`, `RolController`, `SecurityControlController`, aprovisionamiento de credencial): aceptan `tenantId` por compatibilidad, pero si difiere del `tid` del JWT responden 403 `SEC_TENANT_NO_COINCIDE`.
- `ReemplazarPermisosRolHandler` y el cambio de estado de rol rechazan roles `es_sistema` con `SEC_ROL_SISTEMA_NO_EDITABLE`.
- `CrearRolRequest.systemRole` se ignora: por API los roles se crean siempre no-sistema.
- `CrearUsuarioHandler` deriva `nombre_mostrar` de nombres + apellidos cuando viene vacío.

### 4.2 Organización (B1)

- `GET/POST/PUT /api/v1/organizacion/organizaciones` y `PATCH …/{id}/estado`. Permisos nuevos `organizacion.organizaciones.consultar` y `organizacion.organizaciones.gestionar`.
- Crear empresa recibe `organizacionId` y lo valida contra el alcance del actor.
- `GET /api/v1/estructura-corporativa` y los listados de empresas se filtran por alcance.
- `organizacion` consume `security.api.ActorScope`: se declara en `@ApplicationModule(allowedDependencies)` y en `build.gradle`.

### 4.3 Usuarios y roles (B1)

- Crear usuario fija `membership.organizacion_id` con la del actor; un actor `GLOBAL` puede indicarla o dejarla vacía (personal de plataforma).
- `GET /api/v1/roles` devuelve a un actor no global solo los roles asignables por él (permisos ⊆ los suyos, sin roles de sistema globales).
- Asignación de roles, credenciales, estado, sesiones y listado de usuarios aplican las reglas 2–4 mediante `ActorScope`.
- `GET /api/v1/auth/me` no cambia en este bloque.

### 4.4 Migraciones (desde V041)

- **V041:** tabla `organizacion`; `empresa_operadora.organizacion_id` y `membership.organizacion_id`; ámbito `ORGANIZACION` y `organizacion_id` en `usuario_rol_ambito` con su restricción; `rol.tipo_rol`.
- **Backfill:** una "Organización de prueba" recibe las 5 empresas actuales y los usuarios que no tienen asignación `GLOBAL` (los 12 de turno, `ana.torres…`, etc.). `wilton` y `e2e.inventario` quedan como personal de plataforma (`organizacion_id` nulo).
- **V042:** permisos `organizacion.organizaciones.*` (concedidos a `ADMIN`); roles de sistema `ADMIN_ORGANIZACION` y `CAJERO` (actualiza el `CAJERO` existente con `ON CONFLICT`, sin duplicarlo).
- Los scripts siguen la numeración de `bootstrap-app/src/main/resources/db/migration`; antes de escribirlos se revisa también `docs/cadena-farmacias-docs/database/migrations` para no duplicar tablas.

## 5. Frontend (mínimo)

- Pantalla **Organizaciones** (listado, alta, detalle y cambio de estado), visible para el superadmin; sigue el patrón de `features/organizacion`.
- Formulario de empresa con selector de organización (fijo para el administrador de organización).
- Alta de usuario con roles y ámbitos acotados a lo que devuelve el backend (`/roles` filtrado y estructura corporativa filtrada); para el superadmin, selector opcional de organización.
- El filtrado de menú y rutas por permiso es el subproyecto E; mientras tanto el backend bloquea lo no permitido.

## 6. Pruebas

- **Unitarias** (gate de cobertura del 100 % en clases nuevas): reglas de dominio, `ActorScope`, handlers de organización y las nuevas validaciones.
- **Integración con login real y PostgreSQL (Testcontainers)**, casos negativos del administrador de organización: no asigna `GLOBAL` ni `ADMIN`; no fija la contraseña de `wilton`; no ve usuarios ni empresas de otra organización; no edita el rol `ADMIN`; no crea empresas fuera de su organización. Casos positivos: crea cajeros en sus establecimientos y les asigna `CAJERO` con ámbito de establecimiento.
- Un `CAJERO` recibe 403 en los endpoints de usuarios; un `tenantId` distinto al del token recibe 403.
- Frontend: tests de componentes y de API con MSW para pantallas y formularios nuevos.
- **Verificación manual final:** `wilton` crea una organización y su administrador → entra como ese administrador → crea los usuarios de sus establecimientos.

## 7. Orden de entrega

1. A: endurecimiento del IAM (independiente).
2. Migraciones V041/V042 y modelo de organización.
3. `ActorScope` y reglas de delegación en `security`.
4. Endpoints de organización y filtros por alcance.
5. Frontend mínimo.
6. Verificación manual extremo a extremo.

## 8. Fuera de alcance

- **B2:** aislamiento por organización en inventario, compras, catálogo y ventas, y RLS como defensa en profundidad.
- **C:** turnos laborales rotativos por calendario (definición de turnos, roster y su efecto en caja y acceso).
- **D:** endpoint "mi puesto de trabajo" para que Caja y POS funcionen con permisos mínimos.
- **E:** menú y rutas por permiso, página 403 y asistente de alta de usuarios con plantilla de turnos.
- Legibilidad de `created_by`/`granted_by` en auditoría (hoy guardan el UUID del actor).

## 9. Riesgos

- Sin B2, un usuario con permisos de inventario o compras puede acceder a datos de otra organización si conoce los identificadores. Mitigación: un solo cliente real hasta cerrar B2.
- Calcular el alcance en cada petición agrega consultas; se mide y, si hace falta, se cachea por petición.
- Cambiar el significado de `GLOBAL` (de "todo el tenant" a "plataforma") exige revisar los usos actuales del ámbito `GLOBAL` en tests y seeds.
- La restricción `ck_seg_ura_scope` y la FK obligatoria `empresa.organizacion_id` requieren backfill correcto; las migraciones deben ejecutarse contra una copia de la BD actual antes de integrarse.
