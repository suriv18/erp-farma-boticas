# Diseño: Editar rol (backend `PUT /roles/{id}` + `GET /roles/{id}`, frontend `RoleDetailPage`)

Fecha: 2026-09-27
Estado: propuesto

## Contexto

El módulo `security` (IAM) ya soporta, para el agregado `Rol`, tres operaciones de escritura independientes:

- **Crear** (`POST /api/v1/roles`, `CrearRolHandler`).
- **Reemplazar permisos** (`PUT /api/v1/roles/{roleId}/permissions`, `ReemplazarPermisosRolHandler`).
- **Cambiar estado** (`PATCH /api/v1/roles/{roleId}/estado`, resuelto en `SecurityControlController`/`SecurityControlService`, no en `RolController`).

No existe ninguna operación para editar los **datos generales** de un rol ya creado (`code`, `name`, `description`, `roleType`). El agregado `Rol` (`domain/model/Rol.java`) solo expone `create(...)` y `replacePermissions(...)` como mutadores — no hay `updateDetails`.

Dato clave ya verificado en el código: `IamJpaWriteAdapter.save(Rol)` (`infrastructure/persistence/write/adapter/`) **ya implementa la rama UPDATE completa** (si el rol existe por `uuid_publico`, hace `UPDATE nombre/descripcion/tipo_rol/es_sistema/estado`). Es decir, la persistencia de escritura ya soporta editar un rol completo; solo falta el caso de uso de dominio/aplicación y el endpoint que la invoquen.

Tampoco existe `GET /api/v1/roles/{roleId}` (solo el listado paginado `GET /api/v1/roles`). El frontend (`RoleDetailPage.tsx`) hoy resuelve el detalle cargando `rolesQuery({ size: 100 })` y haciendo `find()` en memoria — un workaround que esta spec elimina.

En frontend, `RolForm.tsx` ya soporta `defaultValues`/`submitLabel` y está listo para reutilizarse en edición sin cambios. No existe ningún precedente de "editar entidad completa" en el módulo `seguridad` (ni para roles ni para usuarios) — todas las mutaciones existentes son creación, cambio de estado, o reemplazo de subcolección. Esta es la primera vez que se introduce ese patrón.

## Alcance

1. Nuevo mutador de dominio `Rol.updateDetails(code, name, description, roleType, updatedAt)`.
2. Nuevo caso de uso `ActualizarRolUseCase`/`ActualizarRolHandler` + endpoint `PUT /api/v1/roles/{roleId}`.
3. Nuevo caso de uso de lectura `ObtenerRolUseCase`/`ObtenerRolHandler` + endpoint `GET /api/v1/roles/{roleId}`.
4. Frontend: `actualizarRol` + `fetchRolById`/`rolQuery` en `roles.api.ts`, botón "Editar rol" en `RoleDetailPage.tsx` reutilizando `RolForm`, migración de `RoleDetailPage` del workaround `find()` al nuevo `GET` individual.

**Explícitamente fuera de alcance** (ya existen como operaciones separadas y no se tocan ni se fusionan en el nuevo endpoint):
- Cambio de estado activo/inactivo (`cambiarEstadoRol`, `PATCH /roles/{id}/estado`) — ya implementado.
- Reemplazo de permisos (`reemplazarPermisosRol`, `PUT /roles/{id}/permissions`) — ya implementado.
- Edición del ámbito organizacional de asignaciones usuario-rol (`usuario_rol_ambito`) — es un concepto distinto al `roleType`/`TipoRol` del propio rol y no aplica aquí.
- Migración de base de datos: la tabla `sch_seguridad.rol` (`V013__seguridad_iam.sql`) ya tiene todas las columnas necesarias (`codigo`, `nombre`, `descripcion`, `tipo_rol`, `es_sistema`, `estado`); no se requiere ninguna migración nueva.

## Reglas de negocio

- **Código editable pero validado contra duplicados**: si el nuevo `code` difiere del actual, se valida que no exista otro rol activo del mismo tenant con ese `code` (misma regla que ya aplica en creación, condicionada por la unique constraint `uk_seg_rol_tenant_codigo ON (tenant_id, codigo) WHERE es_activo='1'`). Si hay colisión, se rechaza con error de negocio `SEC_ROL_CODIGO_DUPLICADO` (409 Conflict) — no se deja que la constraint de BD falle con un error genérico de integridad.
- **Roles de sistema (`systemRole == true`) no son editables**: la regla vive en el **agregado de dominio**, no solo en el handler — `Rol.updateDetails(...)` lanza una excepción de dominio (`RolSistemaNoEditableException`, mapeada a `ProblemDetail` con código `SEC_ROL_SISTEMA_NO_EDITABLE`, 409 Conflict) si `this.systemRole` es `true`. Esto protege la invariante sin importar qué caso de uso futuro invoque el mutador. `systemRole` en sí no es editable por este endpoint (no forma parte del payload).
- Los campos `id`, `tenantId`, `systemRole`, `permissionCodes`, `status` del rol se preservan sin cambio al editar datos generales; se editan por los comandos ya existentes (`replacePermissions`, cambio de estado).

## Backend

### Dominio (`domain/model/Rol.java`)

```java
public Rol updateDetails(String code, String name, String description, TipoRol roleType, Instant updatedAt) {
    if (this.systemRole) {
        throw new RolSistemaNoEditableException(this.id);
    }
    // mismas validaciones de formato que create(): code (regex [A-Z][A-Z0-9_]{2,79}),
    // name (2-150), description (<=500), roleType no nulo
    return new Rol(this.id, this.tenantId, code, name, description, roleType,
        this.systemRole, this.permissionCodes, this.status, this.createdAt, updatedAt);
}
```

Nueva excepción `domain/exception/RolSistemaNoEditableException.java`, siguiendo el patrón de excepciones de dominio ya existentes en el módulo.

### Aplicación

- `application/dto/command/ActualizarRolCommand.java` — `record ActualizarRolCommand(UUID roleId, String code, String name, String description, TipoRol roleType) implements Command<RolResult>`. No incluye `tenantId` (se resuelve desde el rol encontrado) ni `systemRole` (no editable).
- `application/port/in/ActualizarRolUseCase.java` — `CommandHandler<ActualizarRolCommand, RolResult>`.
- `application/usecase/command/ActualizarRolHandler.java`:
  1. `writePort.findRole(roleId)` → si no existe: `Result.failure(SEC_ROL_NO_ENCONTRADO)`.
  2. Si `code` cambió respecto al actual: validar duplicado contra otros roles activos del mismo tenant (nuevo método, p.ej. `IamWritePort.existsActiveRoleWithCode(tenantId, code, excludingRoleId)` implementado en `IamJpaWriteAdapter` sobre `RolJpaRepository`, análogo a `existsByTenantIdAndCodigo` ya existente). Si hay colisión: `Result.failure(SEC_ROL_CODIGO_DUPLICADO)`.
  3. Invoca `rol.updateDetails(code, name, description, roleType, Instant.now())`; captura `RolSistemaNoEditableException` y la traduce a `Result.failure(SEC_ROL_SISTEMA_NO_EDITABLE)` (mismo patrón de traducción de excepción de dominio → `Result.failure` que usa `ReemplazarPermisosRolHandler`).
  4. `writePort.save(rolActualizado)` — reutiliza la rama UPDATE ya implementada en `IamJpaWriteAdapter.save(Rol)`.
  5. Retorna `RolResult` vía `IamApplicationMapper`.
- `application/port/in/ObtenerRolUseCase.java` + `application/usecase/query/ObtenerRolHandler.java`: nueva query `ObtenerRolQuery(UUID roleId)` → `IamReadPort.findRoleById(UUID roleId)` (nueva proyección de lectura, análoga a `findRoles` pero para un solo id), retorna `Optional<RolResult>`.

### API (`api/`)

- DTO request `ActualizarRolRequest(String code, String name, String description, TipoRol roleType)` en `api/dto/request/`.
- `IamApiMapper`: `toCommand(UUID roleId, ActualizarRolRequest request)` → `ActualizarRolCommand`.
- `RolController`:
  - `PUT /api/v1/roles/{roleId}` — `@PreAuthorize("hasAuthority('seguridad.roles.gestionar')")` (mismo authority que crear), body `ActualizarRolRequest`, responde `200 OK` + `RolResponse`. Errores de negocio (`SEC_ROL_NO_ENCONTRADO` → 404, `SEC_ROL_CODIGO_DUPLICADO` → 409, `SEC_ROL_SISTEMA_NO_EDITABLE` → 409) vía el mapeo de `Result.failure` a `ProblemDetail` ya establecido en `shared-web`.
  - `GET /api/v1/roles/{roleId}` — `@PreAuthorize("hasAuthority('seguridad.roles.consultar')")` (mismo authority que el listado), responde `200 OK` + `RolResponse` o `404` si no existe.

## Frontend

### `api/roles.types.ts`

```ts
export type ActualizarRolPayload = {
  code: string;
  name: string;
  description?: string;
  roleType: RoleType;
};
```

### `api/roles.api.ts`

```ts
export function rolQuery(roleId: string) { /* queryKey: ['seguridad', 'roles', roleId], GET /roles/{roleId} */ }
export function actualizarRol(client, roleId: string, payload: ActualizarRolPayload) {
  return client.put<RolResponse, ActualizarRolPayload>(`/roles/${roleId}`, payload);
}
```

Mismo estilo que `crearRol`/`cambiarEstadoRol` ya presentes: `queryOptions` para lectura, función plana `(client, ...) => client.<method>(...)` para mutación.

### `pages/RoleDetailPage.tsx`

- Reemplaza la carga actual (`rolesQuery({ size: 100 })` + `find()`) por `useQuery(rolQuery(roleId))` — un solo rol, sin el límite arbitrario de 100.
- Agrega botón **"Editar rol"** junto al de activar/desactivar existente:
  - Oculto (no solo disabled, para evitar confusión) cuando `rol.systemRole === true`.
  - Abre `Modal` con `RolForm` pre-poblado (`defaultValues` = valores actuales del rol, `submitLabel="Guardar cambios"`).
  - `useMutation` → `actualizarRol`; al éxito invalida `rolQuery(roleId)` y la query de listado (`['seguridad', 'roles']`); cierra el modal.
  - Error 409 por `code` duplicado o rol de sistema: se muestra inline en el formulario/modal (mismo mecanismo `ApiError`/`toApiError` de `@boticas/api-client` ya usado en el resto del módulo — sin mapeo custom de código de negocio en esta iteración, se muestra el `detail`/`title` del `ProblemDetail`).

### Rutas

Sin cambios — se resuelve como modal dentro de `RoleDetailPage` (ruta ya existente `seguridad/roles/:roleId`), igual patrón que "Nuevo rol" en `RolesPage` (modal, no ruta propia).

## Testing

**Backend** (unitario, patrón `ReemplazarPermisosRolHandlerTest` con `StubWritePort`, sin Spring context):
- `RolTest`: `updateDetails` con datos válidos, `updateDetails` sobre rol de sistema lanza `RolSistemaNoEditableException`, validaciones de formato de `code`/`name`/`description`/`roleType` iguales a `create`.
- `ActualizarRolHandlerTest`: éxito sin cambio de code, éxito con cambio de code sin duplicado, falla rol no encontrado, falla rol de sistema, falla code duplicado.
- `ObtenerRolHandlerTest`: éxito, no encontrado.
- Cobertura 100% líneas/ramas exigida para todo archivo nuevo (gate JaCoCo por clase).

**Frontend** (Vitest + Testing Library + MSW, patrón `RolesPage.test.tsx`):
- `api/roles.api.test.ts` (extender): casos para `actualizarRol` y `rolQuery`/fetch individual (éxito y error), verificando URL/método/payload.
- `pages/RoleDetailPage.test.tsx` (nuevo — no existe hoy): carga vía GET individual, flujo completo editar rol (abrir modal → editar → submit → invalidación de queries), botón "Editar" ausente cuando `systemRole=true`, error 409 por code duplicado mostrado inline, caso 403 sin permiso `seguridad.roles.gestionar` (acción oculta/deshabilitada, patrón `deniesIamAdministrationWithoutTheRequiredPermission` ya usado en el módulo).
- Cobertura 100% para archivos nuevos o reescritos.

## Manejo de errores

Igual que el resto del módulo: `Result<T>` en application layer → `ProblemDetail` (RFC 9457) vía `GlobalExceptionHandler` de `shared-web` en el backend; `ApiError`/`toApiError` de `@boticas/api-client` mostrado inline en el frontend. Sin mapeo custom de códigos de negocio a mensajes específicos por campo en esta iteración (se muestra `detail`/`title` tal cual).

## Fuera de alcance

- Cambio de estado y reemplazo de permisos (ya implementados, no se tocan).
- Edición de `systemRole` (no editable por diseño).
- Edición de ámbitos de asignación usuario-rol (`usuario_rol_ambito`).
- Migraciones de base de datos (no se requieren).
- E2E Playwright.
