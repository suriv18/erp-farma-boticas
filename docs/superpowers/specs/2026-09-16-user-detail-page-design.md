# Diseño: `UserDetailPage` (detalle de usuario — roles, identidades externas, credencial local)

Fecha: 2026-09-16
Estado: propuesto

## Contexto

Continuación de la spec `2026-09-15-frontend-seguridad-integracion-design.md`. Ya están implementadas las pantallas de Usuarios (listado + alta), Roles (listado + alta + detalle con checklist de permisos + cambio de estado) y Permisos (solo lectura). Falta la pieza más compleja del módulo `seguridad`: el detalle de un usuario, que según el backend real (`SecurityControlController`, `UsuarioController`, `LocalAuthController`) agrupa tres sub-recursos:

- Asignaciones de rol con ámbito organizacional (`GET/POST/DELETE .../asignaciones-rol` y `.../role-assignments`).
- Identidades externas / SSO (`GET/POST/DELETE .../identidades-externas`).
- Provisión de credencial local (`POST .../credencial-local`).

El `Link` de la columna "Nombre" en `UsersPage.tsx` ya apunta a `/seguridad/usuarios/:id`, pero esa ruta no existe todavía (404).

`cambiarEstadoUsuario` ya existe en `features/seguridad/api/usuarios.api.ts` pero no se usa en ninguna pantalla — esta página lo consume por primera vez.

`features/organizacion` ya expone `corporateStructureQuery` (`GET /estructura-corporativa`) con la jerarquía real empresa → establecimiento → almacenes/cajas, reutilizable para resolver los selects de ámbito sin inventar un catálogo nuevo. No existe un catálogo de "terminales" independiente de las cajas (`cashRegisters`); se asume `cashRegisters[].id` como el `terminalId` del ámbito TERMINAL.

## Alcance

Una sola pantalla `UserDetailPage` en `seguridad/usuarios/:userId`, con 4 secciones apiladas (sin tabs):

1. **Header**: nombre/email/username del usuario, `EstadoBadge`, botón activar/desactivar (usa `cambiarEstadoUsuario` existente + `ConfirmActionDialog`).
2. **Roles asignados**: tabla de asignaciones activas (rol, tipo de ámbito, alcance concreto, vigencia, estado) + botón "Asignar rol" + revocar por fila.
3. **Identidades externas**: tabla (provider, subject, último login) + botón "Vincular identidad" + desvincular por fila.
4. **Credencial local**: formulario de una sola acción (fijar contraseña inicial), sin listado.

Fuera de alcance: fechas de vigencia como filtro de listado (solo se capturan al crear), edición de una asignación existente (solo alta/revocación, como expone el backend), catálogo dedicado de terminales.

## Contratos backend confirmados

| Acción | Endpoint | Body/Query |
|---|---|---|
| Listar asignaciones | `GET /usuarios/{userId}/asignaciones-rol?tenantId` | — |
| Asignar rol | `POST /usuarios/{userId}/role-assignments` | `{tenantId, roleId, scopeType, companyId?, establishmentId?, warehouseId?, terminalId?, validFrom?, validUntil?}` |
| Revocar asignación | `DELETE /usuarios/{userId}/asignaciones-rol/{assignmentId}?tenantId` | — |
| Listar identidades | `GET /usuarios/{userId}/identidades-externas?tenantId` | — |
| Vincular identidad | `POST /usuarios/{userId}/identidades-externas?tenantId` | `{provider, subject, issuer?, emailClaim?}` |
| Desvincular identidad | `DELETE /usuarios/{userId}/identidades-externas?tenantId&provider&subject` | — |
| Provisionar credencial | `POST /usuarios/{userId}/credencial-local` | `{tenantId, password, requireChange?}` |
| Cambiar estado usuario | `PATCH /usuarios/{userId}/estado?tenantId` | `{status}` (ya implementado en `usuarios.api.ts`) |

Response de asignación (`AsignacionRolResponse`/`AssignmentView`): `id, tenantId, userId, roleId, scopeType, companyId, establishmentId, warehouseId, terminalId, validFrom, validUntil, status, createdBy, createdAt`. El backend no devuelve `roleCode`/`roleName` en el DTO de escritura pero sí en `AssignmentView` (lectura) — el tipo de listado incluye `roleCode`/`roleName`, el de creación no.

Response de identidad (`ExternalIdentityView`): `provider, subject, issuer, emailClaim, lastLoginAt, createdAt` (sin `id` propio — la clave natural es `provider`+`subject`, por eso el DELETE usa esos dos campos como query params).

## Cambios en `api/usuarios.api.ts` y `usuarios.types.ts`

Se extienden los archivos existentes (no se crean `asignaciones.api.ts` separados — son sub-recursos de usuario):

```ts
// usuarios.types.ts (agregar)
export type AsignacionRol = {
  id: string; roleId: string; roleCode: string; roleName: string; scopeType: string;
  companyId: string | null; establishmentId: string | null; warehouseId: string | null;
  terminalId: string | null; validFrom: string | null; validUntil: string | null;
  status: string; createdBy: string; createdAt: string;
};

export type AsignarRolPayload = {
  tenantId: string; roleId: string; scopeType: string;
  companyId?: string; establishmentId?: string; warehouseId?: string; terminalId?: string;
  validFrom?: string; validUntil?: string;
};

export type IdentidadExterna = {
  provider: string; subject: string; issuer: string | null;
  emailClaim: string | null; lastLoginAt: string | null; createdAt: string;
};

export type VincularIdentidadPayload = {
  provider: string; subject: string; issuer?: string; emailClaim?: string;
};

export type ProvisionarCredencialPayload = {
  tenantId: string; password: string; requireChange?: boolean;
};
```

```ts
// usuarios.api.ts (agregar)
export function usuarioAsignacionesQuery(userId: string, tenantId: string) { /* GET .../asignaciones-rol */ }
export function asignarRolUsuario(client, userId, payload: AsignarRolPayload) { /* POST .../role-assignments */ }
export function revocarAsignacionRol(client, userId, assignmentId, tenantId) { /* DELETE .../asignaciones-rol/{id} */ }

export function identidadesExternasQuery(userId: string, tenantId: string) { /* GET .../identidades-externas */ }
export function vincularIdentidadExterna(client, userId, tenantId, payload: VincularIdentidadPayload) { /* POST .../identidades-externas */ }
export function desvincularIdentidadExterna(client, userId, tenantId, provider, subject) { /* DELETE .../identidades-externas */ }

export function provisionarCredencialLocal(client, userId, payload: ProvisionarCredencialPayload) { /* POST .../credencial-local */ }
```

Todas siguen el patrón exacto de `fetchUsuarios`/`crearUsuario`/`cambiarEstadoUsuario` ya presentes: `queryOptions` con `queryKey: ['seguridad', 'usuarios', ...]` para las de lectura, funciones planas `(client, ...) => client.<method>(...)` para las mutaciones.

## Componentes nuevos

- **`AsignarRolDialog.tsx`** (`Modal` + form, `react-hook-form` + zod):
  - Select de Rol: usa `rolesQuery({tenantId, size: 100})` (igual que `RoleDetailPage`), muestra `name`/`code`.
  - Select de ámbito (`scopeType`): GLOBAL / EMPRESA / ESTABLECIMIENTO / ALMACEN / TERMINAL.
  - Selects encadenados condicionados por `scopeType`, poblados desde `corporateStructureQuery` (import público de `features/organizacion`):
    - EMPRESA → select de empresa (`companyId`).
    - ESTABLECIMIENTO → select de empresa → select de establecimiento (`establishmentId`).
    - ALMACEN → select de empresa → establecimiento → almacén (`warehouseId`).
    - TERMINAL → select de empresa → establecimiento → caja/terminal (`terminalId` = id de `cashRegisters`).
    - GLOBAL → sin selects adicionales.
  - Campos opcionales `validFrom`/`validUntil` (`<input type="datetime-local">`, convertidos a ISO string al enviar).
  - `schemas/asignacion-rol.schema.ts`: zod discriminado por `scopeType` (el id correspondiente es requerido solo si el scope lo exige; GLOBAL no requiere ninguno).

- **`IdentidadExternaForm.tsx`**:
  - Select de provider: `GOOGLE`, `MICROSOFT`, `SAML`, `Otro` (Otro habilita un input de texto libre para el valor real, validado igual que el backend: `NotBlank`, max 100).
  - `subject` (texto, requerido, max 300), `issuer` (opcional, max 500), `emailClaim` (opcional, formato email si se llena, max 254) — límites tomados literalmente de `VincularIdentidadExternaRequest`.
  - `schemas/identidad-externa.schema.ts`.

- **`CredencialLocalForm.tsx`**:
  - `password` + `confirmPassword`, `requireChange` (checkbox, default `true`).
  - `schemas/credencial-local.schema.ts`: min 12, max 128 (límite real del backend), requiere al menos 1 mayúscula, 1 minúscula, 1 número, 1 símbolo; `confirmPassword` debe coincidir (`.refine`).

Reutiliza sin cambios: `DataTable`, `EstadoBadge`, `ConfirmActionDialog`, `Modal`, `FormField`, `Button`/`Card` de `@boticas/ui-web`.

## Página `UserDetailPage.tsx`

Sigue el patrón de `RoleDetailPage.tsx`: obtiene el usuario vía `usuariosQuery` filtrando por `id` en memoria — se confirmó contra `UsuarioController.java` que el backend **no expone** `GET /usuarios/{id}` individual, solo el listado paginado. Cuatro `useQuery` (usuario vía lista, asignaciones, identidades) + 5 `useMutation` (cambiar estado, asignar rol, revocar asignación, vincular identidad, desvincular identidad, provisionar credencial — invalidando las queries correspondientes tras éxito).

## Testing

- `api/usuarios.api.test.ts` (extender): un caso por función nueva, verificando URL/método/query/payload — mismo estilo que los casos existentes de `crearUsuario`/`cambiarEstadoUsuario`.
- `pages/UserDetailPage.test.tsx`: carga con datos, listas vacías, error de backend, asignar rol (incluye selección de ámbito ESTABLECIMIENTO con selects encadenados), revocar asignación con confirmación, vincular identidad, desvincular identidad, provisionar credencial, y un caso 403 que verifica que las acciones de escritura se ocultan/deshabilitan sin el permiso (patrón `deniesIamAdministrationWithoutTheRequiredPermission`).
- `components/AsignarRolDialog.test.tsx`, `components/IdentidadExternaForm.test.tsx`, `components/CredencialLocalForm.test.tsx`: validación zod y callbacks `onSubmit`/`onCancel` aislados de la página, usando el handler MSW global de `/estructura-corporativa` ya presente en `test/mocks/handlers.ts` (no se duplica fixture).

## Manejo de errores

Igual que el resto del módulo: `ApiError`/`toApiError` de `@boticas/api-client`, mensaje `detail`/`title` mostrado inline en el formulario/diálogo. Sin mapeo custom de códigos de negocio en esta iteración.

## Fuera de alcance

- Edición de una asignación de rol existente (el backend solo permite crear/revocar).
- Catálogo dedicado de terminales distinto de `cashRegisters`.
- E2E Playwright (pendiente en la spec original, no se aborda aquí).
