# UserDetailPage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el 404 actual de `/seguridad/usuarios/:userId` por una pantalla de detalle de usuario que permite ver/asignar/revocar roles con ámbito organizacional, vincular/desvincular identidades externas SSO, provisionar una credencial local, y cambiar el estado del usuario.

**Architecture:** Extiende los archivos existentes de `features/seguridad` (`api/usuarios.api.ts`, `usuarios.types.ts`) en vez de crear módulos nuevos por sub-recurso, siguiendo el patrón ya usado por `RoleDetailPage.tsx`/`roles.api.ts`. Los selects de ámbito organizacional reutilizan `corporateStructureQuery` de `features/organizacion` (ya público vía su `index.ts`). Todos los componentes nuevos reutilizan `DataTable`, `Modal`, `ConfirmActionDialog`, `EstadoBadge`, `FormField` ya existentes en `features/seguridad/components/`.

**Tech Stack:** React 19, React Router 8, TanStack Query (`queryOptions` + `useQuery`/`useMutation`), react-hook-form + zod + `@hookform/resolvers/zod`, Vitest + Testing Library + MSW, `@boticas/api-client` (`ApiClient.get/post/put/patch/delete`), `@boticas/ui-web` (`Button`, `Card`, `Badge`).

## Global Constraints

- Nunca importar archivos internos de otra feature — solo su `index.ts` público (`features/organizacion` se consume así).
- Todo tipo de request/response debe reflejar exactamente los DTOs Java confirmados — sin campos inventados ni omitidos.
- `tenantId` sale de `useAuthSession()` (contexto `AuthSessionContext`, ya expone `tenantId`/`userId`).
- Los formularios usan `react-hook-form` con `resolver: zodResolver(schema)`, `mode: 'onTouched'`, y `noValidate` en el `<form>`, igual que `RolForm.tsx`/`UsuarioForm.tsx`.
- Cada `api/*.api.ts` con mutaciones lleva su `*.api.test.ts` con casos que verifican URL, método, query params y payload contra un `ApiClient` real (`createApiClient({baseUrl: 'http://localhost/api/v1'})`) y `msw/node`, patrón exacto de `usuarios.api.test.ts`.
- Cada página nueva lleva su `*.test.tsx` que monta con `QueryClientProvider` + `AuthSessionContext.Provider` (sesión autenticada fija) + `MemoryRouter`, y usa `server.use(...)` del `test/mocks/server` global — patrón exacto de `RolesPage.test.tsx`.
- No se implementa lógica de permisos/403 en la UI en esta iteración — ninguna página existente del módulo lo hace hoy (`UsersPage`, `RolesPage`, `PermissionsPage` no ocultan acciones por permiso); los errores de backend (incluido 403) se muestran igual que cualquier otro `ApiError` vía el manejo de errores estándar de cada mutación.
- Comandos de verificación: `pnpm.cmd --filter @boticas/erp-web test -- --run` (todos los tests) o con `-- --run <ruta>` para un archivo específico, desde `C:\ambiente-dev\PROYECTOS-2026\erp-botica\frontend`.

---

## File Structure

```
apps/erp-web/src/features/seguridad/
├── api/
│   ├── usuarios.api.ts          # MODIFICAR: agregar 7 funciones nuevas
│   ├── usuarios.api.test.ts     # MODIFICAR: agregar 7 casos nuevos
│   └── usuarios.types.ts        # MODIFICAR: agregar 5 tipos nuevos
├── schemas/
│   ├── asignacion-rol.schema.ts       # NUEVO
│   ├── identidad-externa.schema.ts    # NUEVO
│   └── credencial-local.schema.ts     # NUEVO
├── components/
│   ├── AsignarRolDialog.tsx + .test.tsx        # NUEVO
│   ├── IdentidadExternaForm.tsx + .test.tsx    # NUEVO
│   └── CredencialLocalForm.tsx + .test.tsx     # NUEVO
├── pages/
│   ├── UserDetailPage.tsx + .test.tsx  # NUEVO
├── routes.tsx                    # MODIFICAR: agregar ruta seguridad/usuarios/:userId
```

---

### Task 1: Tipos y funciones de API para asignaciones de rol

**Files:**
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.types.ts`
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.api.ts`
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`

**Interfaces:**
- Consumes: `ApiClient` de `@boticas/api-client` (`get`, `post`, `delete`); `apiClient` de `../../../app/api`.
- Produces: `AsignacionRol`, `AsignacionRolCreada`, `AsignarRolPayload` (types); `usuarioAsignacionesQuery(userId, tenantId)`, `asignarRolUsuario(client, userId, payload)` (devuelve `Promise<AsignacionRolCreada>`), `revocarAsignacionRol(client, userId, assignmentId, tenantId)` — usados por Task 4 y Task 7.

**Corrección post-revisión:** `POST /role-assignments` devuelve `AsignacionRolResponse` (Java), cuyo shape real es `id, tenantId, userId, roleId, scopeType, companyId, establishmentId, warehouseId, terminalId, validFrom, validUntil, status, createdBy, createdAt` (confirmado en `IamApiMapper.toResponse(AsignacionRolResult)`) — **sin** `roleCode`/`roleName` y **con** `tenantId`/`userId`, a diferencia de `AsignacionRol` (que modela `AssignmentView`, la respuesta del GET de listado, con `roleCode`/`roleName` y sin `tenantId`/`userId`). Son shapes distintos del backend; no reutilizar el mismo tipo TS para ambos.

- [ ] **Step 1: Agregar los tipos a `usuarios.types.ts`**

Añadir al final del archivo:

```ts
export type AsignacionRol = {
  id: string;
  roleId: string;
  roleCode: string;
  roleName: string;
  scopeType: string;
  companyId: string | null;
  establishmentId: string | null;
  warehouseId: string | null;
  terminalId: string | null;
  validFrom: string | null;
  validUntil: string | null;
  status: string;
  createdBy: string;
  createdAt: string;
};

export type AsignarRolPayload = {
  tenantId: string;
  roleId: string;
  scopeType: string;
  companyId?: string | undefined;
  establishmentId?: string | undefined;
  warehouseId?: string | undefined;
  terminalId?: string | undefined;
  validFrom?: string | undefined;
  validUntil?: string | undefined;
};

export type AsignacionRolCreada = {
  id: string;
  tenantId: string;
  userId: string;
  roleId: string;
  scopeType: string;
  companyId: string | null;
  establishmentId: string | null;
  warehouseId: string | null;
  terminalId: string | null;
  validFrom: string | null;
  validUntil: string | null;
  status: string;
  createdBy: string;
  createdAt: string;
};
```

- [ ] **Step 2: Escribir los tests que fallan en `usuarios.api.test.ts`**

Agregar estos `import`s al inicio (reemplazando la línea de import existente de `./usuarios.api`):

```ts
import {
  asignarRolUsuario,
  cambiarEstadoUsuario,
  crearUsuario,
  fetchUsuarios,
  revocarAsignacionRol,
  usuarioAsignacionesQuery
} from './usuarios.api';
import { QueryClient } from '@tanstack/react-query';
```

Agregar estos casos dentro de `describe('usuarios.api', ...)`, después del test de `cambiarEstadoUsuario`:

```ts
  const sampleAsignacion = {
    id: 'assign-1',
    roleId: 'rol-1',
    roleCode: 'ADMIN_LOCAL',
    roleName: 'Administrador local',
    scopeType: 'ESTABLECIMIENTO',
    companyId: null,
    establishmentId: 'est-1',
    warehouseId: null,
    terminalId: null,
    validFrom: null,
    validUntil: null,
    status: 'ACTIVO',
    createdBy: 'user-admin',
    createdAt: '2026-09-01T00:00:00Z'
  };

  it('usuarioAsignacionesQuery consulta /usuarios/{userId}/asignaciones-rol con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/usuarios/user-1/asignaciones-rol', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleAsignacion]);
      })
    );

    const queryClient = new QueryClient();
    const result = await queryClient.fetchQuery(usuarioAsignacionesQuery('user-1', 'tenant-1'));

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual([sampleAsignacion]);
  });

  const sampleAsignacionCreada = {
    id: 'assign-1',
    tenantId: 'tenant-1',
    userId: 'user-1',
    roleId: 'rol-1',
    scopeType: 'ESTABLECIMIENTO',
    companyId: null,
    establishmentId: 'est-1',
    warehouseId: null,
    terminalId: null,
    validFrom: null,
    validUntil: null,
    status: 'ACTIVO',
    createdBy: 'admin-1',
    createdAt: '2026-09-01T00:00:00Z'
  };

  it('asignarRolUsuario envia POST con el payload de asignacion', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/usuarios/user-1/role-assignments', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleAsignacionCreada, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await asignarRolUsuario(client, 'user-1', {
      tenantId: 'tenant-1',
      roleId: 'rol-1',
      scopeType: 'ESTABLECIMIENTO',
      establishmentId: 'est-1'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      roleId: 'rol-1',
      scopeType: 'ESTABLECIMIENTO',
      establishmentId: 'est-1'
    });
    expect(result).toEqual(sampleAsignacionCreada);
  });

  it('revocarAsignacionRol envia DELETE con tenantId como query param', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.delete('http://localhost/api/v1/usuarios/user-1/asignaciones-rol/assign-1', ({ request }) => {
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await revocarAsignacionRol(client, 'user-1', 'assign-1', 'tenant-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
  });
```

- [ ] **Step 3: Ejecutar los tests y verificar que fallan**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`
Expected: FAIL — `usuarioAsignacionesQuery`, `asignarRolUsuario`, `revocarAsignacionRol` no están exportados por `./usuarios.api`.

- [ ] **Step 4: Implementar las funciones en `usuarios.api.ts`**

Agregar al final del archivo (después de `cambiarEstadoUsuario`), junto con el import adicional:

```ts
import type { AsignacionRol, AsignacionRolCreada, AsignarRolPayload, CrearUsuarioPayload, Usuario } from './usuarios.types';
```

(reemplaza el import existente de tipos, que solo traía `CrearUsuarioPayload, Usuario`)

```ts
export function fetchUsuarioAsignaciones(
  client: ApiClient,
  userId: string,
  tenantId: string
): Promise<AsignacionRol[]> {
  return client.get<AsignacionRol[]>(
    `/usuarios/${userId}/asignaciones-rol?tenantId=${encodeURIComponent(tenantId)}`
  );
}

export function usuarioAsignacionesQuery(userId: string, tenantId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'usuarios', userId, 'asignaciones-rol', tenantId],
    queryFn: () => fetchUsuarioAsignaciones(apiClient, userId, tenantId)
  });
}

export function asignarRolUsuario(
  client: ApiClient,
  userId: string,
  payload: AsignarRolPayload
): Promise<AsignacionRolCreada> {
  return client.post<AsignacionRolCreada, AsignarRolPayload>(`/usuarios/${userId}/role-assignments`, payload);
}

export function revocarAsignacionRol(
  client: ApiClient,
  userId: string,
  assignmentId: string,
  tenantId: string
): Promise<void> {
  return client.delete<void>(
    `/usuarios/${userId}/asignaciones-rol/${assignmentId}?tenantId=${encodeURIComponent(tenantId)}`
  );
}
```

- [ ] **Step 5: Ejecutar los tests y verificar que pasan**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`
Expected: PASS (6 tests)

- [ ] **Step 6: Commit**

```bash
git add apps/erp-web/src/features/seguridad/api/usuarios.types.ts apps/erp-web/src/features/seguridad/api/usuarios.api.ts apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts
git commit -m "feat(seguridad): agregar API de asignaciones de rol de usuario"
```

---

### Task 2: Tipos y funciones de API para identidades externas

**Files:**
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.types.ts`
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.api.ts`
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`

**Interfaces:**
- Consumes: mismo `ApiClient`/`apiClient` de Task 1.
- Produces: `IdentidadExterna`, `VincularIdentidadPayload` (types); `identidadesExternasQuery(userId, tenantId)`, `vincularIdentidadExterna(client, userId, tenantId, payload)`, `desvincularIdentidadExterna(client, userId, tenantId, provider, subject)` — usados por Task 5 y Task 7.

- [ ] **Step 1: Agregar los tipos a `usuarios.types.ts`**

```ts
export type IdentidadExterna = {
  provider: string;
  subject: string;
  issuer: string | null;
  emailClaim: string | null;
  lastLoginAt: string | null;
  createdAt: string;
};

export type VincularIdentidadPayload = {
  provider: string;
  subject: string;
  issuer?: string | undefined;
  emailClaim?: string | undefined;
};
```

- [ ] **Step 2: Escribir los tests que fallan**

Actualizar el import de `./usuarios.api` en `usuarios.api.test.ts` agregando:

```ts
  desvincularIdentidadExterna,
  identidadesExternasQuery,
  vincularIdentidadExterna,
```

Agregar estos casos al final de `describe('usuarios.api', ...)`:

```ts
  const sampleIdentidad = {
    provider: 'GOOGLE',
    subject: 'google-oauth2|123',
    issuer: 'https://accounts.google.com',
    emailClaim: 'ada@boticas.pe',
    lastLoginAt: null,
    createdAt: '2026-09-01T00:00:00Z'
  };

  it('identidadesExternasQuery consulta /usuarios/{userId}/identidades-externas con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/usuarios/user-1/identidades-externas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleIdentidad]);
      })
    );

    const queryClient = new QueryClient();
    const result = await queryClient.fetchQuery(identidadesExternasQuery('user-1', 'tenant-1'));

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual([sampleIdentidad]);
  });

  it('vincularIdentidadExterna envia POST con tenantId en query y el payload en el body', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.post('http://localhost/api/v1/usuarios/user-1/identidades-externas', async ({ request }) => {
        receivedBody = await request.json();
        receivedUrl = new URL(request.url);
        return HttpResponse.json(sampleIdentidad, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await vincularIdentidadExterna(client, 'user-1', 'tenant-1', {
      provider: 'GOOGLE',
      subject: 'google-oauth2|123',
      issuer: 'https://accounts.google.com',
      emailClaim: 'ada@boticas.pe'
    });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({
      provider: 'GOOGLE',
      subject: 'google-oauth2|123',
      issuer: 'https://accounts.google.com',
      emailClaim: 'ada@boticas.pe'
    });
    expect(result).toEqual(sampleIdentidad);
  });

  it('desvincularIdentidadExterna envia DELETE con tenantId, provider y subject en query', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.delete('http://localhost/api/v1/usuarios/user-1/identidades-externas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await desvincularIdentidadExterna(client, 'user-1', 'tenant-1', 'GOOGLE', 'google-oauth2|123');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('provider')).toBe('GOOGLE');
    expect(receivedUrl?.searchParams.get('subject')).toBe('google-oauth2|123');
  });
```

- [ ] **Step 3: Ejecutar los tests y verificar que fallan**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`
Expected: FAIL — funciones de identidades no exportadas.

- [ ] **Step 4: Implementar las funciones en `usuarios.api.ts`**

Actualizar el import de tipos para incluir `IdentidadExterna, VincularIdentidadPayload`. Agregar al final del archivo:

```ts
export function fetchIdentidadesExternas(
  client: ApiClient,
  userId: string,
  tenantId: string
): Promise<IdentidadExterna[]> {
  return client.get<IdentidadExterna[]>(
    `/usuarios/${userId}/identidades-externas?tenantId=${encodeURIComponent(tenantId)}`
  );
}

export function identidadesExternasQuery(userId: string, tenantId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'usuarios', userId, 'identidades-externas', tenantId],
    queryFn: () => fetchIdentidadesExternas(apiClient, userId, tenantId)
  });
}

export function vincularIdentidadExterna(
  client: ApiClient,
  userId: string,
  tenantId: string,
  payload: VincularIdentidadPayload
): Promise<IdentidadExterna> {
  return client.post<IdentidadExterna, VincularIdentidadPayload>(
    `/usuarios/${userId}/identidades-externas?tenantId=${encodeURIComponent(tenantId)}`,
    payload
  );
}

export function desvincularIdentidadExterna(
  client: ApiClient,
  userId: string,
  tenantId: string,
  provider: string,
  subject: string
): Promise<void> {
  const query = new URLSearchParams({ tenantId, provider, subject });
  return client.delete<void>(`/usuarios/${userId}/identidades-externas?${query.toString()}`);
}
```

- [ ] **Step 5: Ejecutar los tests y verificar que pasan**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`
Expected: PASS (9 tests)

- [ ] **Step 6: Commit**

```bash
git add apps/erp-web/src/features/seguridad/api/usuarios.types.ts apps/erp-web/src/features/seguridad/api/usuarios.api.ts apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts
git commit -m "feat(seguridad): agregar API de identidades externas de usuario"
```

---

### Task 3: Tipos y función de API para credencial local

**Files:**
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.types.ts`
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.api.ts`
- Modify: `apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`

**Interfaces:**
- Consumes: mismo `ApiClient`/`apiClient`.
- Produces: `ProvisionarCredencialPayload` (type); `provisionarCredencialLocal(client, userId, payload)` — usado por Task 6 y Task 7.

- [ ] **Step 1: Agregar el tipo a `usuarios.types.ts`**

```ts
export type ProvisionarCredencialPayload = {
  tenantId: string;
  password: string;
  requireChange?: boolean | undefined;
};
```

- [ ] **Step 2: Escribir el test que falla**

Agregar `provisionarCredencialLocal` al import de `./usuarios.api` en el test. Agregar al final de `describe('usuarios.api', ...)`:

```ts
  it('provisionarCredencialLocal envia POST con tenantId, password y requireChange', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/usuarios/user-1/credencial-local', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await provisionarCredencialLocal(client, 'user-1', {
      tenantId: 'tenant-1',
      password: 'Sup3r$eguro123',
      requireChange: true
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      password: 'Sup3r$eguro123',
      requireChange: true
    });
  });
```

- [ ] **Step 3: Ejecutar el test y verificar que falla**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`
Expected: FAIL — `provisionarCredencialLocal` no exportada.

- [ ] **Step 4: Implementar la función en `usuarios.api.ts`**

Actualizar el import de tipos para incluir `ProvisionarCredencialPayload`. Agregar al final:

```ts
export function provisionarCredencialLocal(
  client: ApiClient,
  userId: string,
  payload: ProvisionarCredencialPayload
): Promise<void> {
  return client.post<void, ProvisionarCredencialPayload>(`/usuarios/${userId}/credencial-local`, payload);
}
```

- [ ] **Step 5: Ejecutar los tests y verificar que pasan**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`
Expected: PASS (10 tests)

- [ ] **Step 6: Commit**

```bash
git add apps/erp-web/src/features/seguridad/api/usuarios.types.ts apps/erp-web/src/features/seguridad/api/usuarios.api.ts apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts
git commit -m "feat(seguridad): agregar API de provision de credencial local"
```

---

### Task 4: `AsignarRolDialog` — schema y componente

**Files:**
- Create: `apps/erp-web/src/features/seguridad/schemas/asignacion-rol.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/AsignarRolDialog.test.tsx`

**Interfaces:**
- Consumes: `rolesQuery` de `../api/roles.api` (`Rol` type de `../api/roles.types`); `corporateStructureQuery`, `CorporateStructure` de `../../organizacion` (import público); `Modal`, `FormField` de `./Modal`/`./FormField`; `Button` de `@boticas/ui-web`.
- Produces: `AsignacionRolFormValues` (type, exportado desde el schema); componente `AsignarRolDialog` con props `{ open: boolean; tenantId: string; onSubmit: (values: AsignacionRolFormValues) => void; onCancel: () => void; isSubmitting?: boolean }` — usado por Task 7.

- [ ] **Step 1: Verificar que `features/organizacion` exporta lo necesario**

Leer `apps/erp-web/src/features/organizacion/index.ts` y confirmar que exporta `corporateStructureQuery` y los tipos `CorporateStructure`/`CompanyStructure`/`EstablishmentStructure`/`OrganizationalNode`. Si no los exporta todos, agregarlos al `index.ts` (sin modificar `organization.api.ts`, que ya los define) antes de continuar. Este paso no lleva test propio — es una verificación de precondición.

- [ ] **Step 2: Escribir el schema `asignacion-rol.schema.ts`**

```ts
import { z } from 'zod';

const SCOPE_TYPES = ['GLOBAL', 'EMPRESA', 'ESTABLECIMIENTO', 'ALMACEN', 'TERMINAL'] as const;

export const asignacionRolSchema = z
  .object({
    roleId: z.string().min(1, 'Selecciona un rol.'),
    scopeType: z.enum(SCOPE_TYPES, { message: 'Selecciona un tipo de ámbito válido.' }),
    companyId: z.string().optional(),
    establishmentId: z.string().optional(),
    warehouseId: z.string().optional(),
    terminalId: z.string().optional(),
    validFrom: z.string().optional(),
    validUntil: z.string().optional()
  })
  .refine((values) => values.scopeType !== 'EMPRESA' || Boolean(values.companyId), {
    message: 'Selecciona una empresa.',
    path: ['companyId']
  })
  .refine((values) => values.scopeType !== 'ESTABLECIMIENTO' || Boolean(values.establishmentId), {
    message: 'Selecciona un establecimiento.',
    path: ['establishmentId']
  })
  .refine((values) => values.scopeType !== 'ALMACEN' || Boolean(values.warehouseId), {
    message: 'Selecciona un almacén.',
    path: ['warehouseId']
  })
  .refine((values) => values.scopeType !== 'TERMINAL' || Boolean(values.terminalId), {
    message: 'Selecciona un terminal.',
    path: ['terminalId']
  });

export type AsignacionRolFormValues = z.infer<typeof asignacionRolSchema>;

export { SCOPE_TYPES };
```

- [ ] **Step 3: Escribir el test que falla para `AsignarRolDialog`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { AsignarRolDialog } from './AsignarRolDialog';

const sampleRolesPage = {
  items: [
    {
      id: 'rol-1',
      tenantId: 'tenant-1',
      code: 'ADMIN_LOCAL',
      name: 'Administrador local',
      description: null,
      roleType: 'ESTABLECIMIENTO',
      systemRole: false,
      permissionCodes: [],
      status: 'ACTIVO',
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: null
    }
  ],
  page: 0,
  size: 100,
  totalElements: 1
};

const sampleStructure = {
  asOf: '2026-09-01T00:00:00Z',
  companies: [
    {
      id: 'company-1',
      legalName: 'Boticas SAC',
      tradeName: 'Boticas',
      status: 'ACTIVE' as const,
      establishments: [
        {
          id: 'est-1',
          code: 'EST-01',
          name: 'Sede Central',
          status: 'ACTIVE' as const,
          timeZone: 'America/Lima',
          warehouses: [{ id: 'wh-1', code: 'WH-01', name: 'Almacén central', status: 'ACTIVE' as const }],
          cashRegisters: [{ id: 'cr-1', code: 'CR-01', name: 'Caja 1', status: 'ACTIVE' as const }]
        }
      ]
    }
  ]
};

function renderDialog(onSubmit = vi.fn(), onCancel = vi.fn()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    onSubmit,
    onCancel,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AsignarRolDialog open tenantId="tenant-1" onSubmit={onSubmit} onCancel={onCancel} />
      </QueryClientProvider>
    )
  };
}

describe('AsignarRolDialog', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleStructure))
    );
  });

  it('exige seleccionar un establecimiento cuando el ambito es ESTABLECIMIENTO', async () => {
    const { user, onSubmit } = renderDialog();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    expect(await screen.findByText('Selecciona un establecimiento.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia el payload completo cuando el ambito ESTABLECIMIENTO tiene establecimiento seleccionado', async () => {
    const { user, onSubmit } = renderDialog();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.selectOptions(await screen.findByLabelText('Empresa'), 'company-1');
    await user.selectOptions(await screen.findByLabelText('Establecimiento'), 'est-1');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({ roleId: 'rol-1', scopeType: 'ESTABLECIMIENTO', establishmentId: 'est-1' })
      )
    );
  });

  it('no exige seleccion adicional cuando el ambito es GLOBAL', async () => {
    const { user, onSubmit } = renderDialog();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ roleId: 'rol-1', scopeType: 'GLOBAL' }))
    );
  });
});
```

- [ ] **Step 4: Ejecutar el test y verificar que falla**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/components/AsignarRolDialog.test.tsx`
Expected: FAIL — `./AsignarRolDialog` no existe.

- [ ] **Step 5: Implementar `AsignarRolDialog.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { rolesQuery } from '../api/roles.api';
import { corporateStructureQuery } from '../../organizacion';
import { asignacionRolSchema, SCOPE_TYPES, type AsignacionRolFormValues } from '../schemas/asignacion-rol.schema';
import { FormField } from './FormField';
import { Modal } from './Modal';

export type AsignarRolDialogProps = {
  open: boolean;
  tenantId: string;
  onSubmit: (values: AsignacionRolFormValues) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
};

export function AsignarRolDialog({
  open,
  tenantId,
  onSubmit,
  onCancel,
  isSubmitting = false
}: AsignarRolDialogProps) {
  const rolesResult = useQuery({ ...rolesQuery({ tenantId, size: 100 }), enabled: open });
  const structureResult = useQuery({ ...corporateStructureQuery, enabled: open });

  const {
    formState: { errors },
    handleSubmit,
    register,
    watch
  } = useForm<AsignacionRolFormValues>({
    defaultValues: { roleId: '', scopeType: 'ESTABLECIMIENTO' },
    mode: 'onTouched',
    resolver: zodResolver(asignacionRolSchema)
  });

  const scopeType = watch('scopeType');
  const companyId = watch('companyId');
  const establishmentId = watch('establishmentId');

  const companies = structureResult.data?.companies ?? [];
  const selectedCompany = companies.find((company) => company.id === companyId);
  const establishments = selectedCompany?.establishments ?? [];
  const selectedEstablishment = establishments.find((establishment) => establishment.id === establishmentId);

  if (!open) return null;

  return (
    <Modal open={open} onClose={onCancel} title="Asignar rol">
      <form
        className="space-y-4"
        noValidate
        onSubmit={(event) => {
          void handleSubmit((values) => onSubmit(values))(event);
        }}
      >
        <FormField label="Rol" htmlFor="asignacion-role" error={errors.roleId?.message}>
          <select
            id="asignacion-role"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('roleId')}
          >
            <option value="">Selecciona un rol</option>
            {(rolesResult.data?.items ?? []).map((rol) => (
              <option key={rol.id} value={rol.id}>
                {rol.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Tipo de ámbito" htmlFor="asignacion-scope" error={errors.scopeType?.message}>
          <select
            id="asignacion-scope"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('scopeType')}
          >
            {SCOPE_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </FormField>

        {scopeType !== 'GLOBAL' ? (
          <FormField label="Empresa" htmlFor="asignacion-company" error={errors.companyId?.message}>
            <select
              id="asignacion-company"
              className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
              {...register('companyId')}
            >
              <option value="">Selecciona una empresa</option>
              {companies.map((company) => (
                <option key={company.id} value={company.id}>
                  {company.tradeName ?? company.legalName}
                </option>
              ))}
            </select>
          </FormField>
        ) : null}

        {scopeType === 'ESTABLECIMIENTO' || scopeType === 'ALMACEN' || scopeType === 'TERMINAL' ? (
          <FormField
            label="Establecimiento"
            htmlFor="asignacion-establishment"
            error={errors.establishmentId?.message}
          >
            <select
              id="asignacion-establishment"
              className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
              {...register('establishmentId')}
            >
              <option value="">Selecciona un establecimiento</option>
              {establishments.map((establishment) => (
                <option key={establishment.id} value={establishment.id}>
                  {establishment.name}
                </option>
              ))}
            </select>
          </FormField>
        ) : null}

        {scopeType === 'ALMACEN' ? (
          <FormField label="Almacén" htmlFor="asignacion-warehouse" error={errors.warehouseId?.message}>
            <select
              id="asignacion-warehouse"
              className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
              {...register('warehouseId')}
            >
              <option value="">Selecciona un almacén</option>
              {(selectedEstablishment?.warehouses ?? []).map((warehouse) => (
                <option key={warehouse.id} value={warehouse.id}>
                  {warehouse.name}
                </option>
              ))}
            </select>
          </FormField>
        ) : null}

        {scopeType === 'TERMINAL' ? (
          <FormField label="Terminal" htmlFor="asignacion-terminal" error={errors.terminalId?.message}>
            <select
              id="asignacion-terminal"
              className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
              {...register('terminalId')}
            >
              <option value="">Selecciona un terminal</option>
              {(selectedEstablishment?.cashRegisters ?? []).map((register_) => (
                <option key={register_.id} value={register_.id}>
                  {register_.name}
                </option>
              ))}
            </select>
          </FormField>
        ) : null}

        <FormField label="Vigente desde (opcional)" htmlFor="asignacion-valid-from">
          <input
            id="asignacion-valid-from"
            type="datetime-local"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('validFrom')}
          />
        </FormField>

        <FormField label="Vigente hasta (opcional)" htmlFor="asignacion-valid-until">
          <input
            id="asignacion-valid-until"
            type="datetime-local"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('validUntil')}
          />
        </FormField>

        <div className="flex justify-end gap-3">
          <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            Asignar rol
          </Button>
        </div>
      </form>
    </Modal>
  );
}
```

Nota: la variable `register_` evita colisión con la función `register` de `react-hook-form` dentro del `.map`.

- [ ] **Step 6: Ejecutar el test y verificar que pasa**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/components/AsignarRolDialog.test.tsx`
Expected: PASS (3 tests)

- [ ] **Step 7: Commit**

```bash
git add apps/erp-web/src/features/seguridad/schemas/asignacion-rol.schema.ts apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx apps/erp-web/src/features/seguridad/components/AsignarRolDialog.test.tsx apps/erp-web/src/features/organizacion/index.ts
git commit -m "feat(seguridad): dialogo de asignacion de rol con ambito organizacional"
```

---

### Task 5: `IdentidadExternaForm` — schema y componente

**Files:**
- Create: `apps/erp-web/src/features/seguridad/schemas/identidad-externa.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.test.tsx`

**Interfaces:**
- Consumes: `FormField` de `./FormField`; `Button` de `@boticas/ui-web`.
- Produces: `IdentidadExternaFormValues` (type); componente `IdentidadExternaForm` con props `{ onSubmit: (values: VincularIdentidadPayload) => void; onCancel: () => void; isSubmitting?: boolean }` — usado por Task 7. Nota: el form produce directamente el shape de `VincularIdentidadPayload` (`../api/usuarios.types`), no un tipo intermedio, para no duplicar el mapeo en `UserDetailPage`.

- [ ] **Step 1: Escribir el schema `identidad-externa.schema.ts`**

```ts
import { z } from 'zod';

const KNOWN_PROVIDERS = ['GOOGLE', 'MICROSOFT', 'SAML'] as const;

export const identidadExternaSchema = z
  .object({
    providerOption: z.enum([...KNOWN_PROVIDERS, 'OTRO'], { message: 'Selecciona un proveedor.' }),
    providerCustom: z.string().max(100, 'El proveedor no debe exceder 100 caracteres.').optional(),
    subject: z
      .string()
      .min(1, 'El identificador del sujeto es obligatorio.')
      .max(300, 'El sujeto no debe exceder 300 caracteres.'),
    issuer: z.string().max(500, 'El emisor no debe exceder 500 caracteres.').optional(),
    emailClaim: z.email('Ingresa un correo electrónico válido.').max(254).optional().or(z.literal(''))
  })
  .refine((values) => values.providerOption !== 'OTRO' || Boolean(values.providerCustom?.trim()), {
    message: 'Indica el nombre del proveedor.',
    path: ['providerCustom']
  });

export type IdentidadExternaFormValues = z.infer<typeof identidadExternaSchema>;

export { KNOWN_PROVIDERS };
```

- [ ] **Step 2: Escribir el test que falla**

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { IdentidadExternaForm } from './IdentidadExternaForm';

describe('IdentidadExternaForm', () => {
  it('exige el nombre del proveedor cuando se elige Otro', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<IdentidadExternaForm onSubmit={onSubmit} onCancel={vi.fn()} submitLabel="Vincular" />);

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'OTRO');
    await user.type(screen.getByLabelText('Identificador (subject)'), 'sub-123');
    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    expect(await screen.findByText('Indica el nombre del proveedor.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia provider, subject, issuer y emailClaim cuando el proveedor es conocido', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<IdentidadExternaForm onSubmit={onSubmit} onCancel={vi.fn()} submitLabel="Vincular" />);

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'GOOGLE');
    await user.type(screen.getByLabelText('Identificador (subject)'), 'google-oauth2|123');
    await user.type(screen.getByLabelText('Emisor (opcional)'), 'https://accounts.google.com');
    await user.type(screen.getByLabelText('Correo asociado (opcional)'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    expect(onSubmit).toHaveBeenCalledWith({
      provider: 'GOOGLE',
      subject: 'google-oauth2|123',
      issuer: 'https://accounts.google.com',
      emailClaim: 'ada@boticas.pe'
    });
  });
});
```

- [ ] **Step 3: Ejecutar el test y verificar que falla**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.test.tsx`
Expected: FAIL — `./IdentidadExternaForm` no existe.

- [ ] **Step 4: Implementar `IdentidadExternaForm.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import type { VincularIdentidadPayload } from '../api/usuarios.types';
import {
  identidadExternaSchema,
  KNOWN_PROVIDERS,
  type IdentidadExternaFormValues
} from '../schemas/identidad-externa.schema';
import { FormField } from './FormField';

export type IdentidadExternaFormProps = {
  onSubmit: (payload: VincularIdentidadPayload) => void;
  onCancel: () => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function IdentidadExternaForm({
  onSubmit,
  onCancel,
  submitLabel,
  isSubmitting = false
}: IdentidadExternaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    watch
  } = useForm<IdentidadExternaFormValues>({
    defaultValues: { providerOption: 'GOOGLE', providerCustom: '', subject: '', issuer: '', emailClaim: '' },
    mode: 'onTouched',
    resolver: zodResolver(identidadExternaSchema)
  });

  const providerOption = watch('providerOption');

  function submit(values: IdentidadExternaFormValues) {
    onSubmit({
      provider: values.providerOption === 'OTRO' ? (values.providerCustom ?? '').trim() : values.providerOption,
      subject: values.subject,
      issuer: values.issuer || undefined,
      emailClaim: values.emailClaim || undefined
    });
  }

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(submit)(event);
      }}
    >
      <FormField label="Proveedor" htmlFor="identidad-provider" error={errors.providerOption?.message}>
        <select
          id="identidad-provider"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('providerOption')}
        >
          {KNOWN_PROVIDERS.map((provider) => (
            <option key={provider} value={provider}>
              {provider}
            </option>
          ))}
          <option value="OTRO">Otro</option>
        </select>
      </FormField>

      {providerOption === 'OTRO' ? (
        <FormField label="Nombre del proveedor" htmlFor="identidad-provider-custom" error={errors.providerCustom?.message}>
          <input
            id="identidad-provider-custom"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('providerCustom')}
          />
        </FormField>
      ) : null}

      <FormField label="Identificador (subject)" htmlFor="identidad-subject" error={errors.subject?.message}>
        <input
          id="identidad-subject"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('subject')}
        />
      </FormField>

      <FormField label="Emisor (opcional)" htmlFor="identidad-issuer" error={errors.issuer?.message}>
        <input
          id="identidad-issuer"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('issuer')}
        />
      </FormField>

      <FormField label="Correo asociado (opcional)" htmlFor="identidad-email" error={errors.emailClaim?.message}>
        <input
          id="identidad-email"
          type="email"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('emailClaim')}
        />
      </FormField>

      <div className="flex justify-end gap-3">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
```

- [ ] **Step 5: Ejecutar el test y verificar que pasa**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.test.tsx`
Expected: PASS (2 tests)

- [ ] **Step 6: Commit**

```bash
git add apps/erp-web/src/features/seguridad/schemas/identidad-externa.schema.ts apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.tsx apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.test.tsx
git commit -m "feat(seguridad): formulario de vinculacion de identidad externa"
```

---

### Task 6: `CredencialLocalForm` — schema y componente

**Files:**
- Create: `apps/erp-web/src/features/seguridad/schemas/credencial-local.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/CredencialLocalForm.test.tsx`

**Interfaces:**
- Consumes: `FormField` de `./FormField`; `Button` de `@boticas/ui-web`.
- Produces: `CredencialLocalFormValues` (type); componente `CredencialLocalForm` con props `{ onSubmit: (values: { password: string; requireChange: boolean }) => void; isSubmitting?: boolean }` — usado por Task 7.

- [ ] **Step 1: Escribir el schema `credencial-local.schema.ts`**

```ts
import { z } from 'zod';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/;

export const credencialLocalSchema = z
  .object({
    password: z
      .string()
      .min(12, 'La contraseña debe tener al menos 12 caracteres.')
      .max(128, 'La contraseña no debe exceder 128 caracteres.')
      .regex(PASSWORD_REGEX, 'Debe incluir mayúscula, minúscula, número y símbolo.'),
    confirmPassword: z.string(),
    requireChange: z.boolean()
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Las contraseñas no coinciden.',
    path: ['confirmPassword']
  });

export type CredencialLocalFormValues = z.infer<typeof credencialLocalSchema>;
```

- [ ] **Step 2: Escribir el test que falla**

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CredencialLocalForm } from './CredencialLocalForm';

describe('CredencialLocalForm', () => {
  it('rechaza una contraseña que no cumple la politica de complejidad', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña'), 'simple123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'simple123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(await screen.findByText('Debe incluir mayúscula, minúscula, número y símbolo.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('rechaza cuando la confirmacion no coincide', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña'), 'Sup3r$eguro123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'Otra$eguro123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(await screen.findByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia password y requireChange cuando la contraseña es valida', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña'), 'Sup3r$eguro123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'Sup3r$eguro123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(onSubmit).toHaveBeenCalledWith({ password: 'Sup3r$eguro123', requireChange: true });
  });
});
```

- [ ] **Step 3: Ejecutar el test y verificar que falla**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/components/CredencialLocalForm.test.tsx`
Expected: FAIL — `./CredencialLocalForm` no existe.

- [ ] **Step 4: Implementar `CredencialLocalForm.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { credencialLocalSchema, type CredencialLocalFormValues } from '../schemas/credencial-local.schema';
import { FormField } from './FormField';

export type CredencialLocalFormProps = {
  onSubmit: (values: { password: string; requireChange: boolean }) => void;
  isSubmitting?: boolean;
};

export function CredencialLocalForm({ onSubmit, isSubmitting = false }: CredencialLocalFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    reset
  } = useForm<CredencialLocalFormValues>({
    defaultValues: { password: '', confirmPassword: '', requireChange: true },
    mode: 'onTouched',
    resolver: zodResolver(credencialLocalSchema)
  });

  function submit(values: CredencialLocalFormValues) {
    onSubmit({ password: values.password, requireChange: values.requireChange });
    reset();
  }

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(submit)(event);
      }}
    >
      <FormField label="Contraseña" htmlFor="credencial-password" error={errors.password?.message}>
        <input
          id="credencial-password"
          type="password"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('password')}
        />
      </FormField>

      <FormField
        label="Confirmar contraseña"
        htmlFor="credencial-confirm"
        error={errors.confirmPassword?.message}
      >
        <input
          id="credencial-confirm"
          type="password"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('confirmPassword')}
        />
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
          {...register('requireChange')}
        />
        Exigir cambio de contraseña en el próximo inicio de sesión
      </label>

      <Button type="submit" disabled={isSubmitting}>
        Fijar contraseña
      </Button>
    </form>
  );
}
```

- [ ] **Step 5: Ejecutar el test y verificar que pasa**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/components/CredencialLocalForm.test.tsx`
Expected: PASS (3 tests)

- [ ] **Step 6: Commit**

```bash
git add apps/erp-web/src/features/seguridad/schemas/credencial-local.schema.ts apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx apps/erp-web/src/features/seguridad/components/CredencialLocalForm.test.tsx
git commit -m "feat(seguridad): formulario de provision de credencial local"
```

---

### Task 7: Página `UserDetailPage` y ruta

**Files:**
- Create: `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/UserDetailPage.test.tsx`
- Modify: `apps/erp-web/src/features/seguridad/routes.tsx`

**Interfaces:**
- Consumes: todo lo producido en Tasks 1-6 (`usuariosQuery`, `cambiarEstadoUsuario`, `usuarioAsignacionesQuery`, `asignarRolUsuario`, `revocarAsignacionRol`, `identidadesExternasQuery`, `vincularIdentidadExterna`, `desvincularIdentidadExterna`, `provisionarCredencialLocal`, `AsignarRolDialog`, `IdentidadExternaForm`, `CredencialLocalForm`, `DataTable`, `EstadoBadge`, `Modal`, `ConfirmActionDialog`, `useAuthSession`).
- Produces: componente `UserDetailPage` montado en la ruta `seguridad/usuarios/:userId`.

- [ ] **Step 1: Escribir el test que falla para `UserDetailPage`**

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { UserDetailPage } from './UserDetailPage';

const authenticatedSession = {
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'admin-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

const sampleUsuario = {
  id: 'user-1',
  tenantId: 'tenant-1',
  documentType: null,
  documentNumber: null,
  firstNames: 'Ada',
  lastNames: 'Lovelace',
  username: null,
  email: 'ada@boticas.pe',
  displayName: 'Ada Lovelace',
  phone: null,
  credentialChangeRequired: false,
  mfaRequired: false,
  status: 'ACTIVO',
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: null
};

const sampleAsignacion = {
  id: 'assign-1',
  roleId: 'rol-1',
  roleCode: 'ADMIN_LOCAL',
  roleName: 'Administrador local',
  scopeType: 'ESTABLECIMIENTO',
  companyId: null,
  establishmentId: 'est-1',
  warehouseId: null,
  terminalId: null,
  validFrom: null,
  validUntil: null,
  status: 'ACTIVO',
  createdBy: 'admin-1',
  createdAt: '2026-09-01T00:00:00Z'
};

const sampleIdentidad = {
  provider: 'GOOGLE',
  subject: 'google-oauth2|123',
  issuer: 'https://accounts.google.com',
  emailClaim: 'ada@boticas.pe',
  lastLoginAt: null,
  createdAt: '2026-09-01T00:00:00Z'
};

function renderPage(userId = 'user-1') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <MemoryRouter initialEntries={[`/seguridad/usuarios/${userId}`]}>
            <Routes>
              <Route path="/seguridad/usuarios/:userId" element={<UserDetailPage />} />
            </Routes>
          </MemoryRouter>
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

function mockBaseHandlers() {
  server.use(
    http.get('*/api/v1/usuarios', () =>
      HttpResponse.json({ items: [sampleUsuario], page: 0, size: 20, totalElements: 1 })
    ),
    http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () => HttpResponse.json([sampleAsignacion])),
    http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([sampleIdentidad])),
    http.get('*/api/v1/roles', () =>
      HttpResponse.json({
        items: [
          {
            id: 'rol-1',
            tenantId: 'tenant-1',
            code: 'ADMIN_LOCAL',
            name: 'Administrador local',
            description: null,
            roleType: 'ESTABLECIMIENTO',
            systemRole: false,
            permissionCodes: [],
            status: 'ACTIVO',
            createdAt: '2026-09-01T00:00:00Z',
            updatedAt: null
          }
        ],
        page: 0,
        size: 100,
        totalElements: 1
      })
    ),
    http.get('*/api/v1/estructura-corporativa', () =>
      HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
    )
  );
}

describe('UserDetailPage', () => {
  it('muestra los datos del usuario, sus roles asignados e identidades externas', async () => {
    mockBaseHandlers();
    renderPage();

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
    expect(await screen.findByText('google-oauth2|123')).toBeInTheDocument();
  });

  it('revoca una asignacion de rol tras confirmar', async () => {
    mockBaseHandlers();
    let revoked = false;
    server.use(
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () =>
        HttpResponse.json(revoked ? [] : [sampleAsignacion])
      ),
      http.delete('*/api/v1/usuarios/user-1/asignaciones-rol/assign-1', () => {
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Revocar' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar revocación' }));

    await waitFor(() => expect(screen.getByText('Sin roles asignados.')).toBeInTheDocument());
  });

  it('vincula una identidad externa nueva', async () => {
    mockBaseHandlers();
    let linked = false;
    server.use(
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () =>
        HttpResponse.json(linked ? [sampleIdentidad] : [])
      ),
      http.post('*/api/v1/usuarios/user-1/identidades-externas', () => {
        linked = true;
        return HttpResponse.json(sampleIdentidad, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('Sin identidades externas vinculadas.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Vincular identidad' }));
    await user.selectOptions(screen.getByLabelText('Proveedor'), 'GOOGLE');
    await user.type(screen.getByLabelText('Identificador (subject)'), 'google-oauth2|123');
    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    await waitFor(() => expect(screen.getByText('google-oauth2|123')).toBeInTheDocument());
  });

  it('provisiona una credencial local', async () => {
    mockBaseHandlers();
    let receivedBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios/user-1/credencial-local', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByText('Ada Lovelace');
    await user.type(screen.getByLabelText('Contraseña'), 'Sup3r$eguro123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'Sup3r$eguro123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    await waitFor(() =>
      expect(receivedBody).toEqual({ tenantId: 'tenant-1', password: 'Sup3r$eguro123', requireChange: true })
    );
  });

  it('cambia el estado del usuario tras confirmar', async () => {
    mockBaseHandlers();
    let currentStatus = 'ACTIVO';
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({
          items: [{ ...sampleUsuario, status: currentStatus }],
          page: 0,
          size: 20,
          totalElements: 1
        })
      ),
      http.patch('*/api/v1/usuarios/user-1/estado', () => {
        currentStatus = 'INACTIVO';
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByText('Ada Lovelace');
    await user.click(screen.getByRole('button', { name: 'Desactivar usuario' }));
    await user.click(screen.getByRole('button', { name: 'Desactivar' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });

  it('muestra un estado de error si el backend falla', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () => HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 })),
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () =>
        HttpResponse.json({ title: 'Error interno', status: 500 }, { status: 500 })
      ),
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([])),
      http.get('*/api/v1/roles', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      )
    );

    renderPage();

    expect(await screen.findByText('No se pudo cargar la información.')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar el test y verificar que falla**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/pages/UserDetailPage.test.tsx`
Expected: FAIL — `./UserDetailPage` no existe.

- [ ] **Step 3: Implementar `UserDetailPage.tsx`**

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { Button, Card } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import {
  asignarRolUsuario,
  cambiarEstadoUsuario,
  desvincularIdentidadExterna,
  identidadesExternasQuery,
  provisionarCredencialLocal,
  revocarAsignacionRol,
  usuarioAsignacionesQuery,
  usuariosQuery,
  vincularIdentidadExterna
} from '../api/usuarios.api';
import type { AsignacionRol, IdentidadExterna, VincularIdentidadPayload } from '../api/usuarios.types';
import { AsignarRolDialog } from '../components/AsignarRolDialog';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { CredencialLocalForm } from '../components/CredencialLocalForm';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';
import { IdentidadExternaForm } from '../components/IdentidadExternaForm';
import { Modal } from '../components/Modal';
import type { AsignacionRolFormValues } from '../schemas/asignacion-rol.schema';

export function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();

  const [confirmStatusOpen, setConfirmStatusOpen] = useState(false);
  const [assignRoleOpen, setAssignRoleOpen] = useState(false);
  const [revokeAssignmentId, setRevokeAssignmentId] = useState<string | null>(null);
  const [linkIdentityOpen, setLinkIdentityOpen] = useState(false);
  const [unlinkIdentity, setUnlinkIdentity] = useState<IdentidadExterna | null>(null);

  const usuariosResult = useQuery({
    ...usuariosQuery({ tenantId: tenantId ?? '', size: 100 }),
    enabled: Boolean(tenantId)
  });
  const usuario = usuariosResult.data?.items.find((item) => item.id === userId);

  const asignacionesResult = useQuery({
    ...usuarioAsignacionesQuery(userId ?? '', tenantId ?? ''),
    enabled: Boolean(userId && tenantId)
  });

  const identidadesResult = useQuery({
    ...identidadesExternasQuery(userId ?? '', tenantId ?? ''),
    enabled: Boolean(userId && tenantId)
  });

  const invalidateAsignaciones = () =>
    queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios', userId, 'asignaciones-rol'] });
  const invalidateIdentidades = () =>
    queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios', userId, 'identidades-externas'] });

  const toggleStatusMutation = useMutation({
    mutationFn: (status: string) => cambiarEstadoUsuario(apiClient, userId ?? '', tenantId ?? '', status),
    onSuccess: () => {
      setConfirmStatusOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
    }
  });

  const assignRoleMutation = useMutation({
    mutationFn: (values: AsignacionRolFormValues) =>
      asignarRolUsuario(apiClient, userId ?? '', {
        tenantId: tenantId ?? '',
        roleId: values.roleId,
        scopeType: values.scopeType,
        companyId: values.companyId || undefined,
        establishmentId: values.establishmentId || undefined,
        warehouseId: values.warehouseId || undefined,
        terminalId: values.terminalId || undefined,
        validFrom: values.validFrom || undefined,
        validUntil: values.validUntil || undefined
      }),
    onSuccess: () => {
      setAssignRoleOpen(false);
      void invalidateAsignaciones();
    }
  });

  const revokeAssignmentMutation = useMutation({
    mutationFn: (assignmentId: string) => revocarAsignacionRol(apiClient, userId ?? '', assignmentId, tenantId ?? ''),
    onSuccess: () => {
      setRevokeAssignmentId(null);
      void invalidateAsignaciones();
    }
  });

  const linkIdentityMutation = useMutation({
    mutationFn: (payload: VincularIdentidadPayload) =>
      vincularIdentidadExterna(apiClient, userId ?? '', tenantId ?? '', payload),
    onSuccess: () => {
      setLinkIdentityOpen(false);
      void invalidateIdentidades();
    }
  });

  const unlinkIdentityMutation = useMutation({
    mutationFn: (identity: IdentidadExterna) =>
      desvincularIdentidadExterna(apiClient, userId ?? '', tenantId ?? '', identity.provider, identity.subject),
    onSuccess: () => {
      setUnlinkIdentity(null);
      void invalidateIdentidades();
    }
  });

  const provisionCredentialMutation = useMutation({
    mutationFn: (values: { password: string; requireChange: boolean }) =>
      provisionarCredencialLocal(apiClient, userId ?? '', {
        tenantId: tenantId ?? '',
        password: values.password,
        requireChange: values.requireChange
      })
  });

  if (!usuario) {
    return <p className="text-sm text-slate-500">Cargando usuario…</p>;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Usuarios</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">
            {usuario.displayName ?? usuario.email ?? usuario.username ?? usuario.id}
          </h1>
          <p className="mt-1 text-sm text-slate-500">{usuario.email}</p>
        </div>
        <EstadoBadge status={usuario.status} />
      </div>

      <div className="mt-4 flex justify-end">
        <Button variant="secondary" onClick={() => setConfirmStatusOpen(true)}>
          {usuario.status === 'ACTIVO' ? 'Desactivar usuario' : 'Activar usuario'}
        </Button>
      </div>

      <Card className="mt-6 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-bold text-slate-950">Roles asignados</h2>
          <Button size="sm" onClick={() => setAssignRoleOpen(true)}>
            Asignar rol
          </Button>
        </div>
        <div className="mt-4">
          <DataTable<AsignacionRol>
            columns={[
              { header: 'Rol', cell: (row) => row.roleName },
              { header: 'Ámbito', cell: (row) => row.scopeType },
              { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
              {
                header: '',
                cell: (row) => (
                  <Button size="sm" variant="secondary" onClick={() => setRevokeAssignmentId(row.id)}>
                    Revocar
                  </Button>
                )
              }
            ]}
            rows={asignacionesResult.data ?? []}
            rowKey={(row) => row.id}
            emptyMessage="Sin roles asignados."
            isLoading={asignacionesResult.isPending}
            isError={asignacionesResult.isError}
            errorMessage="No se pudo cargar la información."
          />
        </div>
      </Card>

      <Card className="mt-6 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-bold text-slate-950">Identidades externas</h2>
          <Button size="sm" onClick={() => setLinkIdentityOpen(true)}>
            Vincular identidad
          </Button>
        </div>
        <div className="mt-4">
          <DataTable<IdentidadExterna>
            columns={[
              { header: 'Proveedor', cell: (row) => row.provider },
              { header: 'Identificador', cell: (row) => row.subject },
              {
                header: '',
                cell: (row) => (
                  <Button size="sm" variant="secondary" onClick={() => setUnlinkIdentity(row)}>
                    Desvincular
                  </Button>
                )
              }
            ]}
            rows={identidadesResult.data ?? []}
            rowKey={(row) => `${row.provider}:${row.subject}`}
            emptyMessage="Sin identidades externas vinculadas."
            isLoading={identidadesResult.isPending}
            isError={identidadesResult.isError}
            errorMessage="No se pudo cargar la información."
          />
        </div>
      </Card>

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-slate-950">Credencial local</h2>
        <p className="mt-1 text-sm text-slate-500">Fija una contraseña inicial para el acceso local del usuario.</p>
        <div className="mt-4">
          <CredencialLocalForm
            onSubmit={(values) => provisionCredentialMutation.mutate(values)}
            isSubmitting={provisionCredentialMutation.isPending}
          />
        </div>
      </Card>

      <ConfirmActionDialog
        open={confirmStatusOpen}
        title={usuario.status === 'ACTIVO' ? 'Desactivar usuario' : 'Activar usuario'}
        description={
          usuario.status === 'ACTIVO'
            ? 'El usuario perderá acceso al sistema.'
            : 'El usuario recuperará acceso al sistema.'
        }
        confirmLabel={usuario.status === 'ACTIVO' ? 'Desactivar' : 'Activar'}
        tone={usuario.status === 'ACTIVO' ? 'danger' : 'default'}
        isPending={toggleStatusMutation.isPending}
        onConfirm={() => toggleStatusMutation.mutate(usuario.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')}
        onCancel={() => setConfirmStatusOpen(false)}
      />

      <AsignarRolDialog
        open={assignRoleOpen}
        tenantId={tenantId ?? ''}
        isSubmitting={assignRoleMutation.isPending}
        onSubmit={(values) => assignRoleMutation.mutate(values)}
        onCancel={() => setAssignRoleOpen(false)}
      />

      <ConfirmActionDialog
        open={revokeAssignmentId !== null}
        title="Revocar asignación de rol"
        description="El usuario perderá los permisos asociados a este rol en este ámbito."
        confirmLabel="Confirmar revocación"
        tone="danger"
        isPending={revokeAssignmentMutation.isPending}
        onConfirm={() => revokeAssignmentMutation.mutate(revokeAssignmentId ?? '')}
        onCancel={() => setRevokeAssignmentId(null)}
      />

      <Modal open={linkIdentityOpen} onClose={() => setLinkIdentityOpen(false)} title="Vincular identidad externa">
        <IdentidadExternaForm
          submitLabel="Vincular"
          isSubmitting={linkIdentityMutation.isPending}
          onSubmit={(payload) => linkIdentityMutation.mutate(payload)}
          onCancel={() => setLinkIdentityOpen(false)}
        />
      </Modal>

      <ConfirmActionDialog
        open={unlinkIdentity !== null}
        title="Desvincular identidad externa"
        description="El usuario ya no podrá iniciar sesión con este proveedor."
        confirmLabel="Desvincular"
        tone="danger"
        isPending={unlinkIdentityMutation.isPending}
        onConfirm={() => {
          if (unlinkIdentity) unlinkIdentityMutation.mutate(unlinkIdentity);
        }}
        onCancel={() => setUnlinkIdentity(null)}
      />
    </div>
  );
}
```

- [ ] **Step 4: Agregar la ruta en `routes.tsx`**

Agregar este objeto al array `securityRoutes`, después de la entrada de `seguridad/usuarios`:

```ts
  {
    path: 'seguridad/usuarios/:userId',
    lazy: async () => {
      const { UserDetailPage } = await import('./pages/UserDetailPage');
      return { Component: UserDetailPage };
    }
  },
```

- [ ] **Step 5: Ejecutar el test y verificar que pasa**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run apps/erp-web/src/features/seguridad/pages/UserDetailPage.test.tsx`
Expected: PASS (6 tests)

- [ ] **Step 6: Ejecutar toda la suite del frontend para verificar que no hay regresiones**

Run: `pnpm.cmd --filter @boticas/erp-web test -- --run`
Expected: PASS (todos los tests, incluidos los de las Tasks 1-6)

- [ ] **Step 7: Ejecutar `pnpm check` para lint/typecheck/build**

Run: `pnpm.cmd check`
Expected: PASS sin errores de lint ni de tipos

- [ ] **Step 8: Commit**

```bash
git add apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx apps/erp-web/src/features/seguridad/pages/UserDetailPage.test.tsx apps/erp-web/src/features/seguridad/routes.tsx
git commit -m "feat(seguridad): pantalla de detalle de usuario con roles, identidades y credencial local"
```

---

## Self-Review Notes

- **Cobertura del spec:** header+estado (Task 7), roles asignados + asignar + revocar (Tasks 1, 4, 7), identidades externas + vincular + desvincular (Tasks 2, 5, 7), credencial local (Tasks 3, 6, 7), selects encadenados desde `corporateStructureQuery` (Task 4), ruta `seguridad/usuarios/:userId` (Task 7). Los 3 sub-recursos y las 4 secciones de la spec `2026-09-16-user-detail-page-design.md` están cubiertos.
- **Placeholders:** ninguno — cada step tiene código completo y ejecutable.
- **Consistencia de tipos:** `AsignacionRolFormValues` (Task 4) se mapea explícitamente a `AsignarRolPayload` (Task 1) dentro de `UserDetailPage` (Task 7, `assignRoleMutation`), no se asume que son el mismo shape. `VincularIdentidadPayload` (Task 1) es producido directamente por `IdentidadExternaForm` (Task 5) sin tipo intermedio. `{ password, requireChange }` de `CredencialLocalForm` (Task 6) se mapea a `ProvisionarCredencialPayload` (Task 3) agregando `tenantId` en `UserDetailPage`.
- **Fuera de alcance confirmado:** no se agrega lógica de permisos/403 en la UI porque ninguna pantalla existente del módulo lo hace; no se edita una asignación existente (solo alta/revocación, como expone el backend); Playwright queda para la spec original, no se toca aquí.
