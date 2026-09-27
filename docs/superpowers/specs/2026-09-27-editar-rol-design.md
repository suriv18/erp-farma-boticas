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
- **Roles de sistema (`systemRole == true`) no son editables**: la regla vive en el **agregado de dominio**, no solo en el handler. El módulo `security` no usa excepciones de dominio para rechazos de negocio esperables (`domain/exception/package-info.java` reserva las excepciones solo para estados imposibles); usa exclusivamente el Result Pattern, como ya hace `Rol.replacePermissions(...)`. Por tanto `Rol.updateDetails(...)` retorna `Result.failure(new ErrorDetail("SEC_ROL_SISTEMA_NO_EDITABLE", ...))` si `this.systemRole` es `true`, en vez de lanzar una excepción — mismo mecanismo que ya usa el método `invalid(field, message)` privado del agregado. Esto protege la invariante sin importar qué caso de uso futuro invoque el mutador. `systemRole` en sí no es editable por este endpoint (no forma parte del payload).
- Los campos `id`, `tenantId`, `systemRole`, `permissionCodes`, `status` del rol se preservan sin cambio al editar datos generales; se editan por los comandos ya existentes (`replacePermissions`, cambio de estado).

## Backend

### Dominio (`domain/model/Rol.java`)

Nuevo mutador, siguiendo exactamente el estilo de `replacePermissions(...)` (Result Pattern, sin excepciones):

```java
public Result<Rol, ErrorDetail> updateDetails(
        String code, String name, String description, String roleType, Instant updatedAt) {
    if (this.systemRole) {
        return Result.failure(new ErrorDetail(
                "SEC_ROL_SISTEMA_NO_EDITABLE",
                "Un rol de sistema no puede modificar sus datos generales.",
                Map.of("field", "systemRole")));
    }
    if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");

    var normalizedCode = normalize(code);
    normalizedCode = normalizedCode == null ? null : normalizedCode.toUpperCase(Locale.ROOT);
    if (normalizedCode == null || !CODE_PATTERN.matcher(normalizedCode).matches()) {
        return invalid("code", "El código debe usar entre 3 y 80 caracteres A-Z, 0-9 o guion bajo.");
    }

    var normalizedName = normalizeSpaces(name);
    if (normalizedName == null || normalizedName.length() < 2 || normalizedName.length() > 150) {
        return invalid("name", "El nombre debe tener entre 2 y 150 caracteres.");
    }

    var normalizedDescription = normalizeSpaces(description);
    if (normalizedDescription != null && normalizedDescription.length() > 500) {
        return invalid("description", "La descripción no debe exceder 500 caracteres.");
    }

    final TipoRol normalizedRoleType;
    try {
        normalizedRoleType = TipoRol.valueOf(roleType == null ? "" : roleType.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
        return invalid("roleType", "El tipo de rol no es válido.");
    }

    return Result.success(new Rol(
            id, tenantId, normalizedCode, normalizedName, normalizedDescription, normalizedRoleType,
            systemRole, permissionCodes, status, createdAt, updatedAt));
}
```

No requiere ninguna clase de excepción nueva — reutiliza el mismo método privado `invalid(field, message)` y el mismo `Map`/`ErrorDetail` ya presentes en el agregado. Firma alineada con `create(...)`: recibe `roleType` como `String` (no `TipoRol`), igual que `create`, para no imponerle al caller conversión de enum antes de invocar el dominio.

### Aplicación

Igual que el resto del módulo `security`, no se usan `Command`/`CommandHandler` genéricos de `shared-application` — cada caso de uso define su propia interfaz `@FunctionalInterface XxxUseCase` en `application/port/in/`, implementada por un `XxxHandler` en `application/usecase/{command,query}/`, registrado como `@Bean` en `SecurityModuleConfiguration` e inyectado directo en el controller (sin bus/dispatcher).

- `application/dto/command/ActualizarRolCommand.java`:
  ```java
  package com.softprimesolutions.security.application.dto.command;

  import java.util.UUID;

  public record ActualizarRolCommand(UUID roleId, String code, String name, String description, String roleType) {
  }
  ```
  No incluye `tenantId` (se resuelve desde el rol encontrado vía `writePort.findRole`) ni `systemRole` (no editable por este comando).

- `application/port/in/ActualizarRolUseCase.java`:
  ```java
  package com.softprimesolutions.security.application.port.in;

  import com.softprimesolutions.security.application.dto.command.ActualizarRolCommand;
  import com.softprimesolutions.security.application.dto.result.RolResult;
  import com.softprimesolutions.shared.application.error.ApplicationError;
  import com.softprimesolutions.shared.kernel.result.Result;

  @FunctionalInterface
  public interface ActualizarRolUseCase {
      Result<RolResult, ApplicationError> execute(ActualizarRolCommand command);
  }
  ```

- `application/usecase/command/ActualizarRolHandler.java` (mismo esqueleto que `ReemplazarPermisosRolHandler`):
  1. `writePort.findRole(command.roleId())` → si vacío: `Result.failure(new StandardApplicationError("SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND))`.
  2. Si `command.code()` (normalizado a mayúsculas) difiere del `code()` actual del rol encontrado: `writePort.existsActiveRoleWithCode(role.tenantId().value(), command.code(), command.roleId())` — nuevo método en `IamWritePort` (ver más abajo). Si `true`: `Result.failure(new StandardApplicationError("SEC_ROL_CODIGO_DUPLICADO", "Ya existe un rol con el código indicado.", ErrorCategory.CONFLICT))`.
  3. `role.get().updateDetails(command.code(), command.name(), command.description(), command.roleType(), clock.now())` → `.fold(updatedRole -> persist(updatedRole), this::validationFailure)`, donde `validationFailure` traduce el `ErrorDetail` a `ApplicationError` usando su propio `code()` como código y `ErrorCategory.CONFLICT` cuando el código es `SEC_ROL_SISTEMA_NO_EDITABLE`, o `ErrorCategory.VALIDATION` en cualquier otro caso (mismos códigos que ya devuelve `updateDetails`).
  4. `persist(role)` llama `writePort.save(role)` — reutiliza la rama UPDATE ya implementada en `IamJpaWriteAdapter.save(Rol)` — y retorna `Result.success(IamApplicationMapper.toResult(role))`.

- `application/port/out/IamWritePort.java` — nuevo método:
  ```java
  boolean existsActiveRoleWithCode(UUID tenantId, String code, UUID excludingRoleId);
  ```
  Implementado en `IamJpaWriteAdapter` sobre un nuevo método de `RolJpaRepository`:
  ```java
  boolean existsByTenantIdAndCodigoAndUuidPublicoNot(Long tenantId, String codigo, UUID uuidPublico);
  ```
  (`RolJpaRepository` ya tiene `existsByTenantIdAndCodigo(Long, String)`; el nuevo método sigue la misma convención de Spring Data JPA derived query, solo agregando la exclusión por `uuidPublico`.)

- `application/port/out/IamReadPort.java` — nuevo método:
  ```java
  java.util.Optional<RolResult> findRoleById(UUID tenantId, UUID roleId);
  ```
- `application/port/in/ObtenerRolUseCase.java` (mismo estilo `@FunctionalInterface` que `ActualizarRolUseCase`) + `application/dto/query/ObtenerRolQuery.java` (`record ObtenerRolQuery(UUID tenantId, UUID roleId)`) + `application/usecase/query/ObtenerRolHandler.java`: llama `readPort.findRoleById(query.tenantId(), query.roleId())`; si vacío, `Result.failure(new StandardApplicationError("SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND))`; si presente, `Result.success(...)`.

### API (`api/`)

- DTO request `ActualizarRolRequest(String code, String name, String description, String roleType)` en `api/dto/request/` (mismos tipos primitivos que `CrearRolRequest`, sin `tenantId`/`systemRole`).
- `IamApiMapper`: nuevo `toCommand(UUID roleId, ActualizarRolRequest request)` → `ActualizarRolCommand`.
- `RolController` (agrega dos endpoints a los ya existentes `POST /roles` y `GET /roles`):
  - `PUT /api/v1/roles/{roleId}` — `@PreAuthorize("hasAuthority('seguridad.roles.gestionar')")` (mismo authority que crear), body `ActualizarRolRequest`, responde `200 OK` + `RolResponse`. Errores de negocio (`SEC_ROL_NO_ENCONTRADO` → 404, `SEC_ROL_CODIGO_DUPLICADO` → 409, `SEC_ROL_SISTEMA_NO_EDITABLE` → 409) vía `ApplicationErrorHttpMapper.toProblemDetail(error)` ya establecido en `shared-web`.
  - `GET /api/v1/roles/{roleId}?tenantId=` — `@PreAuthorize("hasAuthority('seguridad.roles.consultar')")` (mismo authority que el listado), responde `200 OK` + `RolResponse` o `404` (`SEC_ROL_NO_ENCONTRADO`) si no existe.

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
export function fetchRolById(client: ApiClient, roleId: string, tenantId: string): Promise<Rol> {
  return client.get<Rol>(`/roles/${roleId}?tenantId=${encodeURIComponent(tenantId)}`);
}

export function rolQuery(roleId: string, tenantId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'roles', roleId],
    queryFn: () => fetchRolById(apiClient, roleId, tenantId)
  });
}

export function actualizarRol(client: ApiClient, roleId: string, payload: ActualizarRolPayload): Promise<Rol> {
  return client.put<Rol, ActualizarRolPayload>(`/roles/${roleId}`, payload);
}
```

Mismo estilo que `crearRol`/`cambiarEstadoRol` ya presentes: `queryOptions` para lectura, función plana `(client, ...) => client.<method>(...)` para mutación. `tenantId` va como query param en el `GET`, igual convención que `cambiarEstadoRol` (`PATCH /roles/{id}/estado?tenantId=`).

### `pages/RoleDetailPage.tsx`

- Reemplaza la carga actual (`rolesQuery({ size: 100 })` + `find()`) por `useQuery(rolQuery(roleId, tenantId))` — un solo rol, sin el límite arbitrario de 100.
- Agrega botón **"Editar rol"** junto al de activar/desactivar existente:
  - Oculto (no solo disabled, para evitar confusión) cuando `rol.systemRole === true`.
  - Abre `Modal` con `RolForm` pre-poblado (`defaultValues` = valores actuales del rol, `submitLabel="Guardar cambios"`).
  - `useMutation` → `actualizarRol`; al éxito invalida `rolQuery(roleId)` y la query de listado (`['seguridad', 'roles']`); cierra el modal.
  - Error 409 por `code` duplicado o rol de sistema: se muestra inline en el formulario/modal (mismo mecanismo `ApiError`/`toApiError` de `@boticas/api-client` ya usado en el resto del módulo — sin mapeo custom de código de negocio en esta iteración, se muestra el `detail`/`title` del `ProblemDetail`).

### Rutas

Sin cambios — se resuelve como modal dentro de `RoleDetailPage` (ruta ya existente `seguridad/roles/:roleId`), igual patrón que "Nuevo rol" en `RolesPage` (modal, no ruta propia).

## Testing

**Backend** (unitario, patrón `ReemplazarPermisosRolHandlerTest` con `StubWritePort`, sin Spring context):
- `RolTest`: `updateDetails` con datos válidos, `updateDetails` sobre rol de sistema retorna `Result.failure` con código `SEC_ROL_SISTEMA_NO_EDITABLE`, validaciones de formato de `code`/`name`/`description`/`roleType` iguales a `create`.
- `ActualizarRolHandlerTest`: éxito sin cambio de code, éxito con cambio de code sin duplicado, falla rol no encontrado, falla rol de sistema, falla code duplicado.
- `ObtenerRolHandlerTest`: éxito, no encontrado.
- Cobertura 100% líneas/ramas exigida para todo archivo nuevo (gate JaCoCo por clase).

**Frontend** (Vitest + Testing Library + MSW, patrón `RolesPage.test.tsx`/`roles.api.test.ts`):
- `api/roles.api.test.ts` (extender): casos para `actualizarRol` y `fetchRolById` (éxito y error), verificando URL/método/payload — mismo estilo `setupServer()` local ya presente en este archivo.
- `pages/RoleDetailPage.test.tsx` (nuevo — no existe hoy): carga vía GET individual, flujo completo editar rol (abrir modal → editar → submit → invalidación de queries), botón "Editar" ausente cuando `systemRole=true`, error 409 por code duplicado mostrado inline.
- Cobertura 100% para archivos nuevos o reescritos.

No existe en el repo ningún test de "acceso denegado por falta de permiso" en el frontend del módulo `seguridad` (verificado — no hay precedente que replicar); queda fuera de alcance de esta spec introducir ese patrón desde cero.

## Manejo de errores

Igual que el resto del módulo: `Result<T>` en application layer → `ProblemDetail` (RFC 9457) vía `GlobalExceptionHandler` de `shared-web` en el backend; `ApiError`/`toApiError` de `@boticas/api-client` mostrado inline en el frontend. Sin mapeo custom de códigos de negocio a mensajes específicos por campo en esta iteración (se muestra `detail`/`title` tal cual).

## Fuera de alcance

- Cambio de estado y reemplazo de permisos (ya implementados, no se tocan).
- Edición de `systemRole` (no editable por diseño).
- Edición de ámbitos de asignación usuario-rol (`usuario_rol_ambito`).
- Migraciones de base de datos (no se requieren).
- E2E Playwright.
