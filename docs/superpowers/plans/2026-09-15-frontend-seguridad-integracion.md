# Integración frontend del módulo seguridad — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el placeholder vacío de `features/seguridad/` por 5 pantallas CRUD (Usuarios, detalle de Usuario, Roles, Permisos, Sesiones, Dispositivos) conectadas al backend real `service-botica`/`security`, y crear la infraestructura Playwright base del proyecto (desktop/tablet/mobile).

**Architecture:** Cada recurso tiene un archivo `api/*.api.ts` (queryOptions + funciones de mutación sobre `apiClient` de `@boticas/api-client`) con sus tipos en `*.types.ts`, un `pages/*.tsx` que consume esos hooks vía TanStack Query, formularios en `components/` con `react-hook-form` + `zod`. Se sigue el patrón exacto ya usado en `features/auth/` y `features/organizacion/`. `AuthSessionProvider` se extiende para persistir `tenantId`/`userId` (hoy descartados), porque todas las pantallas de seguridad los necesitan.

**Tech Stack:** React 19, React Router 8, TanStack Query 5, react-hook-form 7 + zod 4, Tailwind CSS 4, Vitest 4 + Testing Library + MSW 2, Playwright (nuevo).

## Global Constraints

- Backend real: `service-botica` corriendo en `http://localhost:8080` vía Docker Compose (`docker compose up -d postgres` + `bootRun`), ya verificado en sesiones previas.
- Rutas backend confirmadas (todas bajo `/api/v1` salvo `/api/v1/auth/*` que también lleva el prefijo): `POST/GET /api/v1/usuarios`, `PATCH /usuarios/{id}/estado`, `POST /usuarios/{id}/role-assignments`, `GET/DELETE /usuarios/{id}/asignaciones-rol`, `GET/POST/DELETE /usuarios/{id}/identidades-externas`, `POST /usuarios/{id}/credencial-local`, `POST/GET /api/v1/roles`, `PATCH /roles/{id}/estado`, `PUT /roles/{id}/permissions`, `GET /api/v1/permisos`, `GET /sesiones`, `POST /sesiones/{id}/revocacion`, `GET/POST /dispositivos`, `PATCH /dispositivos/{id}/estado`.
- Paginación: solo Usuarios y Roles (`page`/`size`, `PaginaResponse<T>{items,page,size,totalElements}`). Permisos/Sesiones/Dispositivos/Asignaciones/Identidades son listas simples.
- `tenantId` va en el body al crear (usuario, rol, dispositivo, asignación de rol) y como query param obligatorio en listados y cambios de estado.
- `apiClient` (`apps/erp-web/src/app/api.ts`) ya inyecta `Authorization: Bearer <token>` y maneja refresh en 401 — nunca pasar el token a mano.
- Componentes UI reutilizables ya existentes en `@boticas/ui-web`: `Button` (`variant: 'primary'|'secondary'|'ghost'`, `size: 'sm'|'md'`), `Badge` (`tone: 'neutral'|'success'|'warning'|'danger'`), `Card`. No hay `Table`/`Modal`/`Input`/`Select` — se construyen en `features/seguridad/components/`.
- Todos los textos de UI en español, estilo ya usado en `LoginForm.tsx`/`OrganizationPage.tsx` (Tailwind utility classes inline, sin CSS modules).
- Tests co-ubicados: `Componente.tsx` + `Componente.test.tsx` en la misma carpeta.
- `pnpm --filter @boticas/erp-web dev` arranca el front en `http://localhost:5173`; proxy Vite ya reenvía `/api` a `:8080`.
- Ejecutar todos los comandos `pnpm exec vitest run <path>` de este plan **desde `frontend/` (la raíz del workspace pnpm), no desde `frontend/apps/erp-web`**. El `vitest.config.ts` que define `globals: true` vía `projects` vive en `frontend/`; `pnpm --filter @boticas/erp-web exec vitest run` (filtrando al subpaquete) NO carga esa config y falla con `ReferenceError: beforeAll is not defined` incluso en tests ya correctos — usar siempre `pnpm exec vitest run <path-relativo-a-frontend>` desde `frontend/`.

---

## Task 1: Persistir tenantId/userId en la sesión de autenticación

**Files:**
- Modify: `apps/erp-web/src/features/auth/model/auth-session.context.ts`
- Modify: `apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`
- Modify: `apps/erp-web/src/features/auth/model/AuthSessionProvider.test.tsx`

**Interfaces:**
- Produces: `AuthSession` type ahora expone `tenantId: string | null` y `userId: string | null`, leído vía `useAuthSession()`. Todas las tareas de páginas de seguridad consumen `useAuthSession().tenantId` como el `tenantId` a pasar en cada llamada a la API.

- [ ] **Step 1: Leer el archivo actual completo para confirmar el estado exacto antes de editar**

Ejecutar: `cat apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`

Confirmar que `authenticate` hoy hace:
```tsx
const authenticate = useCallback(async (credentials: LoginCredentials) => {
  const response = await loginRequest(apiClient, {
    login: credentials.email,
    password: credentials.password
  });
  setAccessToken(response.accessToken);
  saveRefreshToken(response.refreshToken);
}, []);
```
y que `doRefresh` hace:
```tsx
const response = await refreshRequest(refreshApiClient, storedRefreshToken);
setAccessToken(response.accessToken);
saveRefreshToken(response.refreshToken);
```
Ambas respuestas (`AuthTokenResponse`) ya incluyen `tenantId`/`userId` — solo hace falta guardarlos en estado.

- [ ] **Step 2: Escribir el test que falla en AuthSessionProvider.test.tsx**

Añadir al describe existente (ver el archivo para el `Probe`/setup ya presente; usar el mismo `setupServer()` local del archivo). Agregar este test nuevo:

```tsx
it('expone tenantId y userId tras autenticar', async () => {
  server.use(
    http.post('http://localhost/api/v1/auth/login', () =>
      HttpResponse.json({
        accessToken: 'access-1',
        refreshToken: 'refresh-1',
        tokenType: 'Bearer',
        accessExpiresAt: '2026-09-05T10:10:00Z',
        refreshExpiresAt: '2026-09-12T10:00:00Z',
        tenantId: 'tenant-abc',
        userId: 'user-xyz',
        sessionId: 'session-1',
        passwordChangeRequired: false
      })
    )
  );

  render(
    <AuthSessionProvider>
      <Probe />
    </AuthSessionProvider>
  );

  await act(async () => {
    await screen.getByText('authenticate').click();
  });

  expect(await screen.findByText('tenant-abc')).toBeInTheDocument();
  expect(screen.getByText('user-xyz')).toBeInTheDocument();
});
```

Si el `Probe` existente en el archivo no expone un botón "authenticate" ni renderiza `tenantId`/`userId`, ajustar el `Probe` para renderizarlos (leer el archivo primero y adaptar el `Probe` ya presente en vez de duplicarlo — mismo componente, agregar `<p>{session.tenantId}</p><p>{session.userId}</p>` a su JSX).

- [ ] **Step 3: Ejecutar el test y confirmar que falla**

Run: `pnpm exec vitest run src/features/auth/model/AuthSessionProvider.test.tsx`
Expected: FAIL — `tenantId`/`userId` no existen en `AuthSession` todavía (error de tipo o `undefined` en pantalla).

- [ ] **Step 4: Extender el tipo AuthSession**

En `apps/erp-web/src/features/auth/model/auth-session.context.ts`, reemplazar:

```ts
export type AuthSession = {
  authenticated: boolean;
  accessToken: string | null;
  authenticate: (credentials: LoginCredentials) => Promise<void>;
  signOut: () => Promise<void>;
};
```

por:

```ts
export type AuthSession = {
  authenticated: boolean;
  accessToken: string | null;
  tenantId: string | null;
  userId: string | null;
  authenticate: (credentials: LoginCredentials) => Promise<void>;
  signOut: () => Promise<void>;
};
```

- [ ] **Step 5: Guardar tenantId/userId en AuthSessionProvider**

En `apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`:

1. Agregar estado nuevo junto a `accessToken`:
```tsx
const [tenantId, setTenantId] = useState<string | null>(null);
const [userId, setUserId] = useState<string | null>(null);
```

2. En `doRefresh`, tras `setAccessToken(response.accessToken);`, agregar:
```tsx
setTenantId(response.tenantId);
setUserId(response.userId);
```
y en el bloque `catch` (donde hoy hace `setAccessToken(null)`), agregar también `setTenantId(null); setUserId(null);`.

3. En `authenticate`, tras `setAccessToken(response.accessToken);`, agregar:
```tsx
setTenantId(response.tenantId);
setUserId(response.userId);
```

4. En `signOut`, tras `setAccessToken(null);`, agregar `setTenantId(null); setUserId(null);`.

5. En el `useMemo` de `session`, agregar `tenantId` y `userId` al objeto devuelto y a las dependencias:
```tsx
const session = useMemo<AuthSession>(
  () => ({
    authenticated: accessToken !== null,
    accessToken,
    tenantId,
    userId,
    authenticate,
    signOut
  }),
  [accessToken, tenantId, userId, authenticate, signOut]
);
```

- [ ] **Step 6: Ejecutar el test y confirmar que pasa**

Run: `pnpm exec vitest run src/features/auth/model/AuthSessionProvider.test.tsx`
Expected: PASS

- [ ] **Step 7: Ejecutar toda la suite de auth para confirmar que nada se rompió**

Run: `pnpm exec vitest run src/features/auth`
Expected: PASS (todos los tests existentes de auth, incluido `LoginPage.test.tsx`, `auth.api.test.ts`, `session-storage.test.ts`)

- [ ] **Step 8: Commit**

```bash
git add apps/erp-web/src/features/auth
git commit -m "feat(auth): exponer tenantId y userId en la sesion autenticada"
```

---

## Task 2: Componentes UI compartidos de seguridad (Table, Modal, FormField, EstadoBadge, ConfirmActionDialog)

**Files:**
- Create: `apps/erp-web/src/features/seguridad/components/DataTable.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/DataTable.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/Modal.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/Modal.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/FormField.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/EstadoBadge.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/EstadoBadge.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/ConfirmActionDialog.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/ConfirmActionDialog.test.tsx`

**Interfaces:**
- Consumes: `Button`, `Card` de `@boticas/ui-web`.
- Produces:
  - `DataTable<T>({ columns, rows, rowKey, emptyMessage, isLoading, isError, errorMessage }): JSX.Element` — columns: `{ header: string; cell: (row: T) => ReactNode }[]`.
  - `Modal({ open, onClose, title, children }): JSX.Element | null`.
  - `FormField({ label, htmlFor, error, children }): JSX.Element` — wrapper de label + slot de input + mensaje de error accesible (mismo patrón visual que `LoginForm.tsx`).
  - `EstadoBadge({ status }: { status: string }): JSX.Element` — mapea `ACTIVO/ACTIVE→success`, `INACTIVO/INACTIVE/REVOCADO/REVOKED/BLOQUEADO/BLOCKED→danger`, `PENDIENTE/PENDING/SUSPENDIDO/SUSPENDED→warning`, cualquier otro valor→`neutral`, mostrando el texto tal cual (mayúsculas) devuelto por el backend.
  - `ConfirmActionDialog({ open, title, description, confirmLabel, tone, onConfirm, onCancel, isPending }): JSX.Element | null` — `tone: 'default' | 'danger'`.

- [ ] **Step 1: Escribir el test de EstadoBadge (el más simple, empieza por aquí)**

`apps/erp-web/src/features/seguridad/components/EstadoBadge.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { EstadoBadge } from './EstadoBadge';

describe('EstadoBadge', () => {
  it('muestra tono success para estados activos', () => {
    render(<EstadoBadge status="ACTIVO" />);
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
  });

  it('muestra tono danger para estados bloqueados o revocados', () => {
    render(<EstadoBadge status="BLOQUEADO" />);
    expect(screen.getByText('BLOQUEADO')).toBeInTheDocument();
  });

  it('muestra tono warning para estados pendientes', () => {
    render(<EstadoBadge status="PENDIENTE" />);
    expect(screen.getByText('PENDIENTE')).toBeInTheDocument();
  });

  it('muestra tono neutral para estados no reconocidos', () => {
    render(<EstadoBadge status="DESCONOCIDO" />);
    expect(screen.getByText('DESCONOCIDO')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/EstadoBadge.test.tsx`
Expected: FAIL — `./EstadoBadge` no existe.

- [ ] **Step 3: Implementar EstadoBadge**

`apps/erp-web/src/features/seguridad/components/EstadoBadge.tsx`:
```tsx
import { Badge } from '@boticas/ui-web';

const SUCCESS_STATUSES = new Set(['ACTIVO', 'ACTIVE', 'CONFIABLE', 'TRUSTED']);
const DANGER_STATUSES = new Set(['INACTIVO', 'INACTIVE', 'REVOCADO', 'REVOKED', 'BLOQUEADO', 'BLOCKED']);
const WARNING_STATUSES = new Set(['PENDIENTE', 'PENDING', 'SUSPENDIDO', 'SUSPENDED']);

function toneFor(status: string): 'success' | 'danger' | 'warning' | 'neutral' {
  const normalized = status.toUpperCase();
  if (SUCCESS_STATUSES.has(normalized)) return 'success';
  if (DANGER_STATUSES.has(normalized)) return 'danger';
  if (WARNING_STATUSES.has(normalized)) return 'warning';
  return 'neutral';
}

export function EstadoBadge({ status }: { status: string }) {
  return <Badge tone={toneFor(status)}>{status}</Badge>;
}
```

- [ ] **Step 4: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/EstadoBadge.test.tsx`
Expected: PASS

- [ ] **Step 5: Escribir el test de DataTable**

`apps/erp-web/src/features/seguridad/components/DataTable.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { DataTable } from './DataTable';

type Row = { id: string; name: string };

const columns = [
  { header: 'Nombre', cell: (row: Row) => row.name }
];

describe('DataTable', () => {
  it('renderiza una fila por cada elemento', () => {
    render(
      <DataTable
        columns={columns}
        rows={[{ id: '1', name: 'Ana' }, { id: '2', name: 'Luis' }]}
        rowKey={(row: Row) => row.id}
        emptyMessage="Sin registros"
      />
    );
    expect(screen.getByText('Ana')).toBeInTheDocument();
    expect(screen.getByText('Luis')).toBeInTheDocument();
  });

  it('muestra el mensaje vacio cuando no hay filas', () => {
    render(<DataTable columns={columns} rows={[]} rowKey={(row: Row) => row.id} emptyMessage="Sin registros" />);
    expect(screen.getByText('Sin registros')).toBeInTheDocument();
  });

  it('muestra un indicador de carga', () => {
    render(
      <DataTable columns={columns} rows={[]} rowKey={(row: Row) => row.id} emptyMessage="Sin registros" isLoading />
    );
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra un mensaje de error', () => {
    render(
      <DataTable
        columns={columns}
        rows={[]}
        rowKey={(row: Row) => row.id}
        emptyMessage="Sin registros"
        isError
        errorMessage="No se pudo cargar la información."
      />
    );
    expect(screen.getByText('No se pudo cargar la información.')).toBeInTheDocument();
  });
});
```

- [ ] **Step 6: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/DataTable.test.tsx`
Expected: FAIL

- [ ] **Step 7: Implementar DataTable**

`apps/erp-web/src/features/seguridad/components/DataTable.tsx`:
```tsx
import type { ReactNode } from 'react';
import { Card } from '@boticas/ui-web';

export type DataTableColumn<T> = {
  header: string;
  cell: (row: T) => ReactNode;
};

export type DataTableProps<T> = {
  columns: DataTableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  emptyMessage: string;
  isLoading?: boolean;
  isError?: boolean;
  errorMessage?: string;
};

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyMessage,
  isLoading = false,
  isError = false,
  errorMessage = 'No se pudo cargar la información.'
}: DataTableProps<T>) {
  return (
    <Card className="overflow-hidden">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-slate-100 bg-slate-50">
          <tr>
            {columns.map((column) => (
              <th key={column.header} className="px-4 py-3 font-semibold text-slate-600">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {isLoading ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-6 text-center text-slate-500">
                Cargando…
              </td>
            </tr>
          ) : null}
          {!isLoading && isError ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-6 text-center text-rose-700">
                {errorMessage}
              </td>
            </tr>
          ) : null}
          {!isLoading && !isError && rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-6 text-center text-slate-500">
                {emptyMessage}
              </td>
            </tr>
          ) : null}
          {!isLoading && !isError
            ? rows.map((row) => (
                <tr key={rowKey(row)} className="hover:bg-slate-50">
                  {columns.map((column) => (
                    <td key={column.header} className="px-4 py-3 text-slate-700">
                      {column.cell(row)}
                    </td>
                  ))}
                </tr>
              ))
            : null}
        </tbody>
      </table>
    </Card>
  );
}
```

- [ ] **Step 8: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/DataTable.test.tsx`
Expected: PASS

- [ ] **Step 9: Escribir el test de Modal**

`apps/erp-web/src/features/seguridad/components/Modal.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

describe('Modal', () => {
  it('no renderiza nada cuando open es false', () => {
    render(
      <Modal open={false} onClose={() => {}} title="Título">
        <p>Contenido</p>
      </Modal>
    );
    expect(screen.queryByText('Contenido')).not.toBeInTheDocument();
  });

  it('renderiza el título y el contenido cuando open es true', () => {
    render(
      <Modal open onClose={() => {}} title="Nuevo usuario">
        <p>Formulario</p>
      </Modal>
    );
    expect(screen.getByRole('heading', { name: 'Nuevo usuario' })).toBeInTheDocument();
    expect(screen.getByText('Formulario')).toBeInTheDocument();
  });

  it('llama a onClose al hacer clic en cerrar', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Nuevo usuario">
        <p>Formulario</p>
      </Modal>
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
```

- [ ] **Step 10: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/Modal.test.tsx`
Expected: FAIL

- [ ] **Step 11: Implementar Modal**

`apps/erp-web/src/features/seguridad/components/Modal.tsx`:
```tsx
import type { PropsWithChildren } from 'react';
import { X } from 'lucide-react';

export type ModalProps = PropsWithChildren<{
  open: boolean;
  onClose: () => void;
  title: string;
}>;

export function Modal({ open, onClose, title, children }: ModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
      >
        <div className="flex items-center justify-between">
          <h2 id="modal-title" className="text-lg font-bold text-slate-950">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700"
          >
            <X className="size-4.5" aria-hidden="true" />
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}
```

- [ ] **Step 12: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/Modal.test.tsx`
Expected: PASS

- [ ] **Step 13: Implementar FormField (sin test propio — es un wrapper visual puro, cubierto por los tests de los formularios que lo usan en tareas posteriores)**

`apps/erp-web/src/features/seguridad/components/FormField.tsx`:
```tsx
import type { PropsWithChildren, ReactNode } from 'react';

export type FormFieldProps = PropsWithChildren<{
  label: string;
  htmlFor: string;
  error?: string;
  hint?: ReactNode;
}>;

export function FormField({ label, htmlFor, error, hint, children }: FormFieldProps) {
  return (
    <div>
      <label htmlFor={htmlFor} className="text-sm font-semibold text-slate-700">
        {label}
      </label>
      <div className="mt-2">{children}</div>
      {error ? (
        <p id={`${htmlFor}-error`} role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
          {error}
        </p>
      ) : null}
      {!error && hint ? <p className="mt-1.5 text-xs text-slate-500">{hint}</p> : null}
    </div>
  );
}
```

- [ ] **Step 14: Escribir el test de ConfirmActionDialog**

`apps/erp-web/src/features/seguridad/components/ConfirmActionDialog.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfirmActionDialog } from './ConfirmActionDialog';

describe('ConfirmActionDialog', () => {
  it('no renderiza nada cuando open es false', () => {
    render(
      <ConfirmActionDialog
        open={false}
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={() => {}}
        onCancel={() => {}}
      />
    );
    expect(screen.queryByText('Revocar sesión')).not.toBeInTheDocument();
  });

  it('llama a onConfirm al confirmar', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(
      <ConfirmActionDialog
        open
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={onConfirm}
        onCancel={() => {}}
      />
    );
    await user.click(screen.getByRole('button', { name: 'Revocar' }));
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('llama a onCancel al cancelar', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <ConfirmActionDialog
        open
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={() => {}}
        onCancel={onCancel}
      />
    );
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('deshabilita el boton de confirmar cuando isPending es true', () => {
    render(
      <ConfirmActionDialog
        open
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={() => {}}
        onCancel={() => {}}
        isPending
      />
    );
    expect(screen.getByRole('button', { name: 'Revocar' })).toBeDisabled();
  });
});
```

- [ ] **Step 15: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/ConfirmActionDialog.test.tsx`
Expected: FAIL

- [ ] **Step 16: Implementar ConfirmActionDialog**

`apps/erp-web/src/features/seguridad/components/ConfirmActionDialog.tsx`:
```tsx
import { Button } from '@boticas/ui-web';
import { Modal } from './Modal';

export type ConfirmActionDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  tone?: 'default' | 'danger';
  isPending?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmActionDialog({
  open,
  title,
  description,
  confirmLabel,
  tone = 'default',
  isPending = false,
  onConfirm,
  onCancel
}: ConfirmActionDialogProps) {
  if (!open) return null;

  return (
    <Modal open={open} onClose={onCancel} title={title}>
      <p className="text-sm text-slate-600">{description}</p>
      <div className="mt-6 flex justify-end gap-3">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isPending}>
          Cancelar
        </Button>
        <Button
          type="button"
          variant={tone === 'danger' ? 'primary' : 'primary'}
          className={tone === 'danger' ? 'bg-rose-700 hover:bg-rose-800 focus-visible:outline-rose-700' : undefined}
          onClick={onConfirm}
          disabled={isPending}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
```

- [ ] **Step 17: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/ConfirmActionDialog.test.tsx`
Expected: PASS

- [ ] **Step 18: Ejecutar toda la carpeta de componentes de seguridad**

Run: `pnpm exec vitest run src/features/seguridad`
Expected: PASS (14 tests: 4 EstadoBadge + 4 DataTable + 3 Modal + 4 ConfirmActionDialog... ajustar conteo si difiere, todos deben pasar)

- [ ] **Step 19: Commit**

```bash
git add apps/erp-web/src/features/seguridad
git commit -m "feat(seguridad): componentes UI compartidos (DataTable, Modal, FormField, EstadoBadge, ConfirmActionDialog)"
```

---

## Task 3: Pantalla de Permisos (solo lectura)

**Files:**
- Create: `apps/erp-web/src/features/seguridad/api/permisos.types.ts`
- Create: `apps/erp-web/src/features/seguridad/api/permisos.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/permisos.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/pages/PermissionsPage.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/PermissionsPage.test.tsx`
- Modify: `apps/erp-web/src/features/seguridad/routes.tsx`

**Interfaces:**
- Consumes: `apiClient` de `apps/erp-web/src/app/api.ts` (`apiClient.get<T>(path)`).
- Produces:
  - `Permiso` type: `{ moduleCode: string; moduleName: string; code: string; resource: string; action: string; name: string; description: string | null; critical: boolean; status: string }`.
  - `permisosQuery(search?: string)` → `UseQueryOptions` para `useQuery`, queryKey `['seguridad', 'permisos', search ?? '']`.
  - `PermissionsPage` componente, ruta `seguridad/permisos`.

- [ ] **Step 1: Definir los tipos**

`apps/erp-web/src/features/seguridad/api/permisos.types.ts`:
```ts
export type Permiso = {
  moduleCode: string;
  moduleName: string;
  code: string;
  resource: string;
  action: string;
  name: string;
  description: string | null;
  critical: boolean;
  status: string;
};
```

- [ ] **Step 2: Escribir el test de permisos.api.ts (falla primero)**

`apps/erp-web/src/features/seguridad/api/permisos.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { fetchPermisos } from './permisos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('permisos.api', () => {
  it('fetchPermisos consulta /permisos con el filtro de busqueda', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/permisos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([
          {
            moduleCode: 'SEGURIDAD',
            moduleName: 'Seguridad',
            code: 'seguridad.usuarios.consultar',
            resource: 'USUARIO',
            action: 'CONSULTAR',
            name: 'Consultar usuarios',
            description: null,
            critical: false,
            status: 'ACTIVO'
          }
        ]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchPermisos(client, 'usuarios');

    expect(receivedUrl?.searchParams.get('search')).toBe('usuarios');
    expect(result).toHaveLength(1);
    expect(result[0].code).toBe('seguridad.usuarios.consultar');
  });

  it('fetchPermisos omite el parametro search cuando no se provee', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/permisos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await fetchPermisos(client);

    expect(receivedUrl?.searchParams.has('search')).toBe(false);
  });
});
```

- [ ] **Step 3: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/permisos.api.test.ts`
Expected: FAIL — `./permisos.api` no existe.

- [ ] **Step 4: Implementar permisos.api.ts**

`apps/erp-web/src/features/seguridad/api/permisos.api.ts`:
```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { Permiso } from './permisos.types';

export function fetchPermisos(client: ApiClient, search?: string): Promise<Permiso[]> {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  const query = params.toString();
  return client.get<Permiso[]>(`/permisos${query ? `?${query}` : ''}`);
}

export function permisosQuery(search?: string) {
  return queryOptions({
    queryKey: ['seguridad', 'permisos', search ?? ''],
    queryFn: () => fetchPermisos(apiClient, search)
  });
}
```

- [ ] **Step 5: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/permisos.api.test.ts`
Expected: PASS

- [ ] **Step 6: Escribir el test de PermissionsPage**

`apps/erp-web/src/features/seguridad/pages/PermissionsPage.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { PermissionsPage } from './PermissionsPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <PermissionsPage />
      </QueryClientProvider>
    )
  };
}

describe('PermissionsPage', () => {
  it('lista los permisos agrupados por modulo', async () => {
    server.use(
      http.get('*/api/v1/permisos', () =>
        HttpResponse.json([
          {
            moduleCode: 'SEGURIDAD',
            moduleName: 'Seguridad',
            code: 'seguridad.usuarios.consultar',
            resource: 'USUARIO',
            action: 'CONSULTAR',
            name: 'Consultar usuarios',
            description: null,
            critical: false,
            status: 'ACTIVO'
          }
        ])
      )
    );

    renderPage();

    expect(await screen.findByText('Consultar usuarios')).toBeInTheDocument();
    expect(screen.getByText('seguridad.usuarios.consultar')).toBeInTheDocument();
  });

  it('muestra un mensaje cuando no hay permisos', async () => {
    server.use(http.get('*/api/v1/permisos', () => HttpResponse.json([])));

    renderPage();

    expect(await screen.findByText('No se encontraron permisos.')).toBeInTheDocument();
  });

  it('filtra por texto de busqueda', async () => {
    server.use(
      http.get('*/api/v1/permisos', ({ request }) => {
        const url = new URL(request.url);
        const search = url.searchParams.get('search');
        if (search === 'roles') {
          return HttpResponse.json([
            {
              moduleCode: 'SEGURIDAD',
              moduleName: 'Seguridad',
              code: 'seguridad.roles.consultar',
              resource: 'ROL',
              action: 'CONSULTAR',
              name: 'Consultar roles',
              description: null,
              critical: false,
              status: 'ACTIVO'
            }
          ]);
        }
        return HttpResponse.json([]);
      })
    );

    const { user } = renderPage();
    await user.type(screen.getByLabelText('Buscar permiso'), 'roles');

    expect(await screen.findByText('Consultar roles')).toBeInTheDocument();
  });
});
```

- [ ] **Step 7: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/pages/PermissionsPage.test.tsx`
Expected: FAIL — `./PermissionsPage` no existe.

- [ ] **Step 8: Implementar PermissionsPage**

`apps/erp-web/src/features/seguridad/pages/PermissionsPage.tsx`:
```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { permisosQuery } from '../api/permisos.api';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';

export function PermissionsPage() {
  const [search, setSearch] = useState('');
  const { data, isPending, isError } = useQuery(permisosQuery(search || undefined));

  return (
    <div className="mx-auto max-w-7xl">
      <div>
        <p className="text-sm font-semibold text-teal-700">Seguridad / Permisos</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Permisos</h1>
        <p className="mt-2 text-sm text-slate-500">
          Catálogo de permisos disponibles para asignar a roles.
        </p>
      </div>

      <div className="mt-6">
        <label htmlFor="permisos-search" className="text-sm font-semibold text-slate-700">
          Buscar permiso
        </label>
        <input
          id="permisos-search"
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Código, nombre o módulo"
          className="mt-2 h-11 w-full max-w-md rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
        />
      </div>

      <div className="mt-6">
        <DataTable
          columns={[
            { header: 'Módulo', cell: (row) => row.moduleName },
            { header: 'Código', cell: (row) => row.code },
            { header: 'Nombre', cell: (row) => row.name },
            { header: 'Crítico', cell: (row) => (row.critical ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> }
          ]}
          rows={data ?? []}
          rowKey={(row) => row.code}
          emptyMessage="No se encontraron permisos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el catálogo de permisos."
        />
      </div>
    </div>
  );
}
```

- [ ] **Step 9: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/pages/PermissionsPage.test.tsx`
Expected: PASS

- [ ] **Step 10: Añadir la ruta**

Reemplazar el contenido completo de `apps/erp-web/src/features/seguridad/routes.tsx` (se irá extendiendo en tareas posteriores; por ahora solo agrega `seguridad/permisos` junto a la ruta índice existente):

```tsx
import type { RouteObject } from 'react-router';

export const securityRoutes = [
  {
    path: 'seguridad',
    lazy: async () => {
      const { SecurityPage } = await import('./pages/SecurityPage');
      return { Component: SecurityPage };
    }
  },
  {
    path: 'seguridad/permisos',
    lazy: async () => {
      const { PermissionsPage } = await import('./pages/PermissionsPage');
      return { Component: PermissionsPage };
    }
  }
] satisfies RouteObject[];
```

- [ ] **Step 11: Ejecutar toda la suite de seguridad**

Run: `pnpm exec vitest run src/features/seguridad`
Expected: PASS

- [ ] **Step 12: Commit**

```bash
git add apps/erp-web/src/features/seguridad
git commit -m "feat(seguridad): pantalla de permisos (solo lectura)"
```

---

## Task 4: Pantalla de Roles (listado paginado, alta, cambio de estado, reemplazo de permisos)

**Files:**
- Create: `apps/erp-web/src/features/seguridad/api/roles.types.ts`
- Create: `apps/erp-web/src/features/seguridad/api/roles.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/roles.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/schemas/rol.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/components/RolForm.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/RolForm.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/PermisosChecklist.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/PermisosChecklist.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/RolesPage.test.tsx`
- Modify: `apps/erp-web/src/features/seguridad/routes.tsx`

**Interfaces:**
- Consumes: `Permiso` de `../api/permisos.types`, `permisosQuery` de `../api/permisos.api`, `useAuthSession` de `../../auth`.
- Produces:
  - `Rol` type: `{ id: string; tenantId: string; code: string; name: string; description: string | null; roleType: string; systemRole: boolean; permissionCodes: string[]; status: string; createdAt: string; updatedAt: string | null }`.
  - `PaginaResponse<T>` type: `{ items: T[]; page: number; size: number; totalElements: number }` (definido aquí y reutilizado tal cual por Usuarios en la Task 5 — mismo shape exacto, no redefinir).
  - `rolesQuery({ tenantId, search, page, size })` → queryOptions.
  - `crearRol(client, payload: CrearRolPayload): Promise<Rol>`, `cambiarEstadoRol(client, roleId, tenantId, status): Promise<void>`, `reemplazarPermisosRol(client, roleId, permissionCodes: string[]): Promise<Rol>`.
  - `rolSchema` (zod) → `RolFormValues = { code: string; name: string; description?: string; roleType: string; systemRole: boolean }`.
  - `RolForm({ defaultValues, onSubmit, submitLabel, isSubmitting }): JSX.Element`.
  - `PermisosChecklist({ permisos, selectedCodes, onChange }): JSX.Element` — `permisos: Permiso[]`, `selectedCodes: Set<string>`, `onChange: (codes: Set<string>) => void`, agrupa por `moduleName`.

- [ ] **Step 1: Definir los tipos**

`apps/erp-web/src/features/seguridad/api/roles.types.ts`:
```ts
export type Rol = {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  description: string | null;
  roleType: string;
  systemRole: boolean;
  permissionCodes: string[];
  status: string;
  createdAt: string;
  updatedAt: string | null;
};

export type PaginaResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};

export type CrearRolPayload = {
  tenantId: string;
  code: string;
  name: string;
  description?: string;
  roleType: string;
  systemRole?: boolean;
};
```

- [ ] **Step 2: Escribir el test de roles.api.ts**

`apps/erp-web/src/features/seguridad/api/roles.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { cambiarEstadoRol, crearRol, fetchRoles, reemplazarPermisosRol } from './roles.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleRol = {
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
};

describe('roles.api', () => {
  it('fetchRoles consulta /roles con tenantId, search, page y size', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/roles', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleRol], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchRoles(client, { tenantId: 'tenant-1', search: 'admin', page: 0, size: 20 });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('search')).toBe('admin');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleRol]);
  });

  it('crearRol envia el payload y devuelve el rol creado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/roles', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleRol, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearRol(client, {
      tenantId: 'tenant-1',
      code: 'ADMIN_LOCAL',
      name: 'Administrador local',
      roleType: 'ESTABLECIMIENTO'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      code: 'ADMIN_LOCAL',
      name: 'Administrador local',
      roleType: 'ESTABLECIMIENTO'
    });
    expect(result).toEqual(sampleRol);
  });

  it('cambiarEstadoRol envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.patch('http://localhost/api/v1/roles/rol-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoRol(client, 'rol-1', 'tenant-1', 'INACTIVO');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });

  it('reemplazarPermisosRol envia PUT con la lista de codigos', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/roles/rol-1/permissions', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleRol, permissionCodes: ['seguridad.usuarios.consultar'] });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await reemplazarPermisosRol(client, 'rol-1', ['seguridad.usuarios.consultar']);

    expect(receivedBody).toEqual({ permissionCodes: ['seguridad.usuarios.consultar'] });
    expect(result.permissionCodes).toEqual(['seguridad.usuarios.consultar']);
  });
});
```

- [ ] **Step 3: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/roles.api.test.ts`
Expected: FAIL

- [ ] **Step 4: Implementar roles.api.ts**

`apps/erp-web/src/features/seguridad/api/roles.api.ts`:
```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { CrearRolPayload, PaginaResponse, Rol } from './roles.types';

export type FetchRolesParams = {
  tenantId: string;
  search?: string;
  page?: number;
  size?: number;
};

export function fetchRoles(client: ApiClient, params: FetchRolesParams): Promise<PaginaResponse<Rol>> {
  const query = new URLSearchParams({ tenantId: params.tenantId });
  if (params.search) query.set('search', params.search);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<Rol>>(`/roles?${query.toString()}`);
}

export function rolesQuery(params: FetchRolesParams) {
  return queryOptions({
    queryKey: ['seguridad', 'roles', params.tenantId, params.search ?? '', params.page ?? 0, params.size ?? 20],
    queryFn: () => fetchRoles(apiClient, params)
  });
}

export function crearRol(client: ApiClient, payload: CrearRolPayload): Promise<Rol> {
  return client.post<Rol, CrearRolPayload>('/roles', payload);
}

export function cambiarEstadoRol(
  client: ApiClient,
  roleId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/roles/${roleId}/estado?tenantId=${encodeURIComponent(tenantId)}`,
    { status }
  );
}

export function reemplazarPermisosRol(
  client: ApiClient,
  roleId: string,
  permissionCodes: string[]
): Promise<Rol> {
  return client.put<Rol, { permissionCodes: string[] }>(`/roles/${roleId}/permissions`, { permissionCodes });
}
```

- [ ] **Step 5: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/roles.api.test.ts`
Expected: PASS

- [ ] **Step 6: Definir el schema de zod para el formulario de rol**

`apps/erp-web/src/features/seguridad/schemas/rol.schema.ts`:
```ts
import { z } from 'zod';

export const rolSchema = z.object({
  code: z
    .string()
    .min(3, 'El código debe tener al menos 3 caracteres.')
    .max(80, 'El código no debe exceder 80 caracteres.'),
  name: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(150, 'El nombre no debe exceder 150 caracteres.'),
  description: z.string().max(500, 'La descripción no debe exceder 500 caracteres.').optional(),
  roleType: z.enum(['GLOBAL', 'EMPRESA', 'ESTABLECIMIENTO', 'ALMACEN', 'TERMINAL'], {
    message: 'Selecciona un tipo de rol válido.'
  }),
  systemRole: z.boolean()
});

export type RolFormValues = z.infer<typeof rolSchema>;
```

- [ ] **Step 7: Escribir el test de PermisosChecklist**

`apps/erp-web/src/features/seguridad/components/PermisosChecklist.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Permiso } from '../api/permisos.types';
import { PermisosChecklist } from './PermisosChecklist';

const permisos: Permiso[] = [
  {
    moduleCode: 'SEGURIDAD',
    moduleName: 'Seguridad',
    code: 'seguridad.usuarios.consultar',
    resource: 'USUARIO',
    action: 'CONSULTAR',
    name: 'Consultar usuarios',
    description: null,
    critical: false,
    status: 'ACTIVO'
  },
  {
    moduleCode: 'CATALOGO',
    moduleName: 'Catálogo',
    code: 'catalogo.skus.consultar',
    resource: 'SKU',
    action: 'CONSULTAR',
    name: 'Consultar SKU',
    description: null,
    critical: false,
    status: 'ACTIVO'
  }
];

describe('PermisosChecklist', () => {
  it('agrupa los permisos por modulo', () => {
    render(<PermisosChecklist permisos={permisos} selectedCodes={new Set()} onChange={() => {}} />);
    expect(screen.getByText('Seguridad')).toBeInTheDocument();
    expect(screen.getByText('Catálogo')).toBeInTheDocument();
  });

  it('marca los checkboxes de los codigos seleccionados', () => {
    render(
      <PermisosChecklist
        permisos={permisos}
        selectedCodes={new Set(['seguridad.usuarios.consultar'])}
        onChange={() => {}}
      />
    );
    expect(screen.getByLabelText('Consultar usuarios')).toBeChecked();
    expect(screen.getByLabelText('Consultar SKU')).not.toBeChecked();
  });

  it('llama a onChange con el nuevo set al marcar un permiso', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PermisosChecklist permisos={permisos} selectedCodes={new Set()} onChange={onChange} />);

    await user.click(screen.getByLabelText('Consultar usuarios'));

    expect(onChange).toHaveBeenCalledWith(new Set(['seguridad.usuarios.consultar']));
  });

  it('llama a onChange con el codigo removido al desmarcar un permiso', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <PermisosChecklist
        permisos={permisos}
        selectedCodes={new Set(['seguridad.usuarios.consultar'])}
        onChange={onChange}
      />
    );

    await user.click(screen.getByLabelText('Consultar usuarios'));

    expect(onChange).toHaveBeenCalledWith(new Set());
  });
});
```

- [ ] **Step 8: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/PermisosChecklist.test.tsx`
Expected: FAIL

- [ ] **Step 9: Implementar PermisosChecklist**

`apps/erp-web/src/features/seguridad/components/PermisosChecklist.tsx`:
```tsx
import type { Permiso } from '../api/permisos.types';

export type PermisosChecklistProps = {
  permisos: Permiso[];
  selectedCodes: Set<string>;
  onChange: (codes: Set<string>) => void;
};

function groupByModule(permisos: Permiso[]): Map<string, Permiso[]> {
  const groups = new Map<string, Permiso[]>();
  for (const permiso of permisos) {
    const existing = groups.get(permiso.moduleName) ?? [];
    existing.push(permiso);
    groups.set(permiso.moduleName, existing);
  }
  return groups;
}

export function PermisosChecklist({ permisos, selectedCodes, onChange }: PermisosChecklistProps) {
  const groups = groupByModule(permisos);

  function toggle(code: string) {
    const next = new Set(selectedCodes);
    if (next.has(code)) next.delete(code);
    else next.add(code);
    onChange(next);
  }

  return (
    <div className="max-h-80 space-y-5 overflow-y-auto rounded-xl border border-slate-200 p-4">
      {[...groups.entries()].map(([moduleName, modulePermisos]) => (
        <div key={moduleName}>
          <p className="text-xs font-bold tracking-wide text-slate-500 uppercase">{moduleName}</p>
          <div className="mt-2 space-y-2">
            {modulePermisos.map((permiso) => (
              <label key={permiso.code} className="flex items-center gap-2.5 text-sm text-slate-700">
                <input
                  type="checkbox"
                  aria-label={permiso.name}
                  checked={selectedCodes.has(permiso.code)}
                  onChange={() => toggle(permiso.code)}
                  className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
                />
                {permiso.name}
              </label>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 10: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/PermisosChecklist.test.tsx`
Expected: PASS

- [ ] **Step 11: Escribir el test de RolForm**

`apps/erp-web/src/features/seguridad/components/RolForm.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RolForm } from './RolForm';

describe('RolForm', () => {
  it('muestra errores de validacion cuando se envia vacio', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<RolForm onSubmit={onSubmit} submitLabel="Crear rol" />);

    await user.click(screen.getByRole('button', { name: 'Crear rol' }));

    expect(await screen.findByText('El código debe tener al menos 3 caracteres.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<RolForm onSubmit={onSubmit} submitLabel="Crear rol" />);

    await user.type(screen.getByLabelText('Código'), 'ADMIN_LOCAL');
    await user.type(screen.getByLabelText('Nombre'), 'Administrador local');
    await user.selectOptions(screen.getByLabelText('Tipo de rol'), 'ESTABLECIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Crear rol' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ code: 'ADMIN_LOCAL', name: 'Administrador local', roleType: 'ESTABLECIMIENTO' })
    );
  });

  it('precarga los valores por defecto al editar', () => {
    render(
      <RolForm
        defaultValues={{ code: 'ADMIN_LOCAL', name: 'Administrador local', roleType: 'ESTABLECIMIENTO', systemRole: false }}
        onSubmit={vi.fn()}
        submitLabel="Guardar cambios"
      />
    );

    expect(screen.getByLabelText('Código')).toHaveValue('ADMIN_LOCAL');
    expect(screen.getByLabelText('Nombre')).toHaveValue('Administrador local');
  });
});
```

- [ ] **Step 12: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/RolForm.test.tsx`
Expected: FAIL

- [ ] **Step 13: Implementar RolForm**

`apps/erp-web/src/features/seguridad/components/RolForm.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { rolSchema, type RolFormValues } from '../schemas/rol.schema';
import { FormField } from './FormField';

export type RolFormProps = {
  defaultValues?: RolFormValues;
  onSubmit: (values: RolFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

const ROLE_TYPES = ['GLOBAL', 'EMPRESA', 'ESTABLECIMIENTO', 'ALMACEN', 'TERMINAL'] as const;

export function RolForm({ defaultValues, onSubmit, submitLabel, isSubmitting = false }: RolFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<RolFormValues>({
    defaultValues: defaultValues ?? { code: '', name: '', description: '', roleType: 'ESTABLECIMIENTO', systemRole: false },
    mode: 'onTouched',
    resolver: zodResolver(rolSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(onSubmit)(event);
      }}
    >
      <FormField label="Código" htmlFor="rol-code" error={errors.code?.message}>
        <input
          id="rol-code"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('code')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="rol-name" error={errors.name?.message}>
        <input
          id="rol-name"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('name')}
        />
      </FormField>

      <FormField label="Descripción" htmlFor="rol-description" error={errors.description?.message}>
        <textarea
          id="rol-description"
          rows={2}
          className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('description')}
        />
      </FormField>

      <FormField label="Tipo de rol" htmlFor="rol-type" error={errors.roleType?.message}>
        <select
          id="rol-type"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('roleType')}
        >
          {ROLE_TYPES.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
          {...register('systemRole')}
        />
        Rol de sistema (no editable por usuarios finales)
      </label>

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 14: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/RolForm.test.tsx`
Expected: PASS

- [ ] **Step 15: Escribir el test de RolesPage**

`apps/erp-web/src/features/seguridad/pages/RolesPage.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { RolesPage } from './RolesPage';

const authenticatedSession = {
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <RolesPage />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

const sampleRol = {
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
};

describe('RolesPage', () => {
  it('lista los roles del tenant activo', async () => {
    server.use(
      http.get('*/api/v1/roles', () =>
        HttpResponse.json({ items: [sampleRol], page: 0, size: 20, totalElements: 1 })
      )
    );

    renderPage();

    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
  });

  it('crea un rol nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/roles', () =>
        HttpResponse.json({
          items: created ? [sampleRol] : [],
          page: 0,
          size: 20,
          totalElements: created ? 1 : 0
        })
      ),
      http.post('*/api/v1/roles', async () => {
        created = true;
        return HttpResponse.json(sampleRol, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron roles.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo rol' }));
    await user.type(screen.getByLabelText('Código'), 'ADMIN_LOCAL');
    await user.type(screen.getByLabelText('Nombre'), 'Administrador local');
    await user.selectOptions(screen.getByLabelText('Tipo de rol'), 'ESTABLECIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Crear rol' }));

    await waitFor(() => expect(screen.getByText('Administrador local')).toBeInTheDocument());
  });
});
```

- [ ] **Step 16: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/pages/RolesPage.test.tsx`
Expected: FAIL

- [ ] **Step 17: Implementar RolesPage**

`apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`:
```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearRol, rolesQuery } from '../api/roles.api';
import type { Rol } from '../api/roles.types';
import type { RolFormValues } from '../schemas/rol.schema';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';
import { Modal } from '../components/Modal';
import { RolForm } from '../components/RolForm';

export function RolesPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isPending, isError } = useQuery({
    ...rolesQuery({ tenantId: tenantId ?? '' }),
    enabled: Boolean(tenantId)
  });

  const createMutation = useMutation({
    mutationFn: (values: RolFormValues) =>
      crearRol(apiClient, {
        tenantId: tenantId ?? '',
        code: values.code,
        name: values.name,
        description: values.description,
        roleType: values.roleType,
        systemRole: values.systemRole
      }),
    onSuccess: () => {
      setCreateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Roles</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Roles</h1>
          <p className="mt-2 text-sm text-slate-500">
            Administra los roles y sus permisos asociados.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo rol</Button>
      </div>

      <div className="mt-6">
        <DataTable<Rol>
          columns={[
            {
              header: 'Código',
              cell: (row) => (
                <Link to={`/seguridad/roles/${row.id}`} className="font-semibold text-teal-700 hover:underline">
                  {row.code}
                </Link>
              )
            },
            { header: 'Nombre', cell: (row) => row.name },
            { header: 'Tipo', cell: (row) => row.roleType },
            { header: 'Permisos', cell: (row) => row.permissionCodes.length },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron roles."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de roles."
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo rol">
        <RolForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear rol"
          isSubmitting={createMutation.isPending}
        />
      </Modal>
    </div>
  );
}
```

Nota: el detalle de edición de un rol existente (cambio de estado, reemplazo de permisos vía `PermisosChecklist` + `reemplazarPermisosRol`) se agrega en el **Step 18** siguiente, como una página de detalle separada, para mantener este archivo enfocado en listado + alta.

- [ ] **Step 18: Crear la página de detalle de rol (edición + permisos)**

Crear `apps/erp-web/src/features/seguridad/pages/RoleDetailPage.tsx`:
```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { Button, Card } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { cambiarEstadoRol, reemplazarPermisosRol, rolesQuery } from '../api/roles.api';
import { permisosQuery } from '../api/permisos.api';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { EstadoBadge } from '../components/EstadoBadge';
import { PermisosChecklist } from '../components/PermisosChecklist';

export function RoleDetailPage() {
  const { roleId } = useParams<{ roleId: string }>();
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [confirmDeactivateOpen, setConfirmDeactivateOpen] = useState(false);

  const rolesResult = useQuery({
    ...rolesQuery({ tenantId: tenantId ?? '', size: 100 }),
    enabled: Boolean(tenantId)
  });
  const rol = rolesResult.data?.items.find((item) => item.id === roleId);

  const permisosResult = useQuery(permisosQuery());

  const [selectedCodes, setSelectedCodes] = useState<Set<string> | null>(null);
  const effectiveCodes = selectedCodes ?? new Set(rol?.permissionCodes ?? []);

  const savePermissionsMutation = useMutation({
    mutationFn: (codes: string[]) => reemplazarPermisosRol(apiClient, roleId ?? '', codes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (status: string) => cambiarEstadoRol(apiClient, roleId ?? '', tenantId ?? '', status),
    onSuccess: () => {
      setConfirmDeactivateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  if (!rol) {
    return <p className="text-sm text-slate-500">Cargando rol…</p>;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Roles</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">{rol.name}</h1>
          <p className="mt-1 text-sm text-slate-500">{rol.code}</p>
        </div>
        <EstadoBadge status={rol.status} />
      </div>

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-slate-950">Permisos</h2>
        <div className="mt-4">
          <PermisosChecklist
            permisos={permisosResult.data ?? []}
            selectedCodes={effectiveCodes}
            onChange={setSelectedCodes}
          />
        </div>
        <div className="mt-4 flex justify-end">
          <Button
            onClick={() => savePermissionsMutation.mutate([...effectiveCodes])}
            disabled={savePermissionsMutation.isPending}
          >
            Guardar permisos
          </Button>
        </div>
      </Card>

      <div className="mt-6 flex justify-end">
        <Button variant="secondary" onClick={() => setConfirmDeactivateOpen(true)}>
          {rol.status === 'ACTIVO' ? 'Desactivar rol' : 'Activar rol'}
        </Button>
      </div>

      <ConfirmActionDialog
        open={confirmDeactivateOpen}
        title={rol.status === 'ACTIVO' ? 'Desactivar rol' : 'Activar rol'}
        description={
          rol.status === 'ACTIVO'
            ? 'Los usuarios con este rol perderán los permisos asociados.'
            : 'El rol volverá a estar disponible para asignación.'
        }
        confirmLabel={rol.status === 'ACTIVO' ? 'Desactivar' : 'Activar'}
        tone={rol.status === 'ACTIVO' ? 'danger' : 'default'}
        isPending={toggleStatusMutation.isPending}
        onConfirm={() => toggleStatusMutation.mutate(rol.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')}
        onCancel={() => setConfirmDeactivateOpen(false)}
      />
    </div>
  );
}
```

- [ ] **Step 19: Ejecutar y confirmar que RolesPage.test.tsx pasa**

Run: `pnpm exec vitest run src/features/seguridad/pages/RolesPage.test.tsx`
Expected: PASS

- [ ] **Step 20: Añadir las rutas de roles**

Reemplazar `apps/erp-web/src/features/seguridad/routes.tsx`:
```tsx
import type { RouteObject } from 'react-router';

export const securityRoutes = [
  {
    path: 'seguridad',
    lazy: async () => {
      const { SecurityPage } = await import('./pages/SecurityPage');
      return { Component: SecurityPage };
    }
  },
  {
    path: 'seguridad/permisos',
    lazy: async () => {
      const { PermissionsPage } = await import('./pages/PermissionsPage');
      return { Component: PermissionsPage };
    }
  },
  {
    path: 'seguridad/roles',
    lazy: async () => {
      const { RolesPage } = await import('./pages/RolesPage');
      return { Component: RolesPage };
    }
  },
  {
    path: 'seguridad/roles/:roleId',
    lazy: async () => {
      const { RoleDetailPage } = await import('./pages/RoleDetailPage');
      return { Component: RoleDetailPage };
    }
  }
] satisfies RouteObject[];
```

- [ ] **Step 21: Ejecutar toda la suite de seguridad**

Run: `pnpm exec vitest run src/features/seguridad`
Expected: PASS

- [ ] **Step 22: Commit**

```bash
git add apps/erp-web/src/features/seguridad
git commit -m "feat(seguridad): pantalla de roles con alta, estado y reemplazo de permisos"
```

---

## Task 5: Pantalla de Usuarios (listado paginado, alta, cambio de estado)

**Files:**
- Create: `apps/erp-web/src/features/seguridad/api/usuarios.types.ts`
- Create: `apps/erp-web/src/features/seguridad/api/usuarios.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/schemas/usuario.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/components/UsuarioForm.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/UsuarioForm.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/UsersPage.test.tsx`
- Modify: `apps/erp-web/src/features/seguridad/routes.tsx`

**Interfaces:**
- Consumes: `PaginaResponse<T>` de `../api/roles.types` (mismo shape, reutilizado — no redefinir), `useAuthSession` de `../../auth`, `DataTable`, `EstadoBadge`, `Modal` de `../components`.
- Produces:
  - `Usuario` type: `{ id: string; tenantId: string; documentType: string | null; documentNumber: string | null; firstNames: string | null; lastNames: string | null; username: string | null; email: string | null; displayName: string | null; phone: string | null; credentialChangeRequired: boolean; mfaRequired: boolean; status: string; createdAt: string; updatedAt: string | null }`.
  - `usuariosQuery({ tenantId, search, page, size })` → queryOptions.
  - `crearUsuario(client, payload: CrearUsuarioPayload): Promise<Usuario>`, `cambiarEstadoUsuario(client, userId, tenantId, status): Promise<void>`.
  - `usuarioSchema` (zod) → `UsuarioFormValues`.
  - `UsuarioForm({ defaultValues, onSubmit, submitLabel, isSubmitting }): JSX.Element`.

- [ ] **Step 1: Definir los tipos**

`apps/erp-web/src/features/seguridad/api/usuarios.types.ts`:
```ts
export type Usuario = {
  id: string;
  tenantId: string;
  documentType: string | null;
  documentNumber: string | null;
  firstNames: string | null;
  lastNames: string | null;
  username: string | null;
  email: string | null;
  displayName: string | null;
  phone: string | null;
  credentialChangeRequired: boolean;
  mfaRequired: boolean;
  status: string;
  createdAt: string;
  updatedAt: string | null;
};

export type CrearUsuarioPayload = {
  tenantId: string;
  documentType?: string;
  documentNumber?: string;
  firstNames?: string;
  lastNames?: string;
  username?: string;
  email?: string;
  phone?: string;
  displayName?: string;
  credentialChangeRequired?: boolean;
  mfaRequired?: boolean;
};
```

- [ ] **Step 2: Escribir el test de usuarios.api.ts**

`apps/erp-web/src/features/seguridad/api/usuarios.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { cambiarEstadoUsuario, crearUsuario, fetchUsuarios } from './usuarios.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

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

describe('usuarios.api', () => {
  it('fetchUsuarios consulta /usuarios con tenantId, search, page y size', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/usuarios', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleUsuario], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchUsuarios(client, { tenantId: 'tenant-1', search: 'ada', page: 0, size: 20 });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('search')).toBe('ada');
    expect(result.items).toEqual([sampleUsuario]);
  });

  it('crearUsuario envia el payload y devuelve el usuario creado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/usuarios', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleUsuario, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearUsuario(client, {
      tenantId: 'tenant-1',
      email: 'ada@boticas.pe',
      displayName: 'Ada Lovelace'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      email: 'ada@boticas.pe',
      displayName: 'Ada Lovelace'
    });
    expect(result).toEqual(sampleUsuario);
  });

  it('cambiarEstadoUsuario envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.patch('http://localhost/api/v1/usuarios/user-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoUsuario(client, 'user-1', 'tenant-1', 'INACTIVO');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });
});
```

- [ ] **Step 3: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/usuarios.api.test.ts`
Expected: FAIL

- [ ] **Step 4: Implementar usuarios.api.ts**

`apps/erp-web/src/features/seguridad/api/usuarios.api.ts`:
```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './roles.types';
import type { CrearUsuarioPayload, Usuario } from './usuarios.types';

export type FetchUsuariosParams = {
  tenantId: string;
  search?: string;
  page?: number;
  size?: number;
};

export function fetchUsuarios(
  client: ApiClient,
  params: FetchUsuariosParams
): Promise<PaginaResponse<Usuario>> {
  const query = new URLSearchParams({ tenantId: params.tenantId });
  if (params.search) query.set('search', params.search);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<Usuario>>(`/usuarios?${query.toString()}`);
}

export function usuariosQuery(params: FetchUsuariosParams) {
  return queryOptions({
    queryKey: [
      'seguridad',
      'usuarios',
      params.tenantId,
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchUsuarios(apiClient, params)
  });
}

export function crearUsuario(client: ApiClient, payload: CrearUsuarioPayload): Promise<Usuario> {
  return client.post<Usuario, CrearUsuarioPayload>('/usuarios', payload);
}

export function cambiarEstadoUsuario(
  client: ApiClient,
  userId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/usuarios/${userId}/estado?tenantId=${encodeURIComponent(tenantId)}`,
    { status }
  );
}
```

- [ ] **Step 5: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/usuarios.api.test.ts`
Expected: PASS

- [ ] **Step 6: Definir el schema de zod para el formulario de usuario**

`apps/erp-web/src/features/seguridad/schemas/usuario.schema.ts`:
```ts
import { z } from 'zod';

export const usuarioSchema = z.object({
  firstNames: z.string().max(150, 'Los nombres no deben exceder 150 caracteres.').optional(),
  lastNames: z.string().max(180, 'Los apellidos no deben exceder 180 caracteres.').optional(),
  username: z.string().max(150, 'El usuario no debe exceder 150 caracteres.').optional(),
  email: z.email('Ingresa un correo electrónico válido.').optional().or(z.literal('')),
  phone: z.string().max(40, 'El teléfono no debe exceder 40 caracteres.').optional(),
  displayName: z.string().max(250, 'El nombre visible no debe exceder 250 caracteres.').optional(),
  mfaRequired: z.boolean()
});

export type UsuarioFormValues = z.infer<typeof usuarioSchema>;
```

- [ ] **Step 7: Escribir el test de UsuarioForm**

`apps/erp-web/src/features/seguridad/components/UsuarioForm.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UsuarioForm } from './UsuarioForm';

describe('UsuarioForm', () => {
  it('muestra un error cuando el correo es invalido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<UsuarioForm onSubmit={onSubmit} submitLabel="Crear usuario" />);

    await user.type(screen.getByLabelText('Correo'), 'no-es-un-correo');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Ingresa un correo electrónico válido.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<UsuarioForm onSubmit={onSubmit} submitLabel="Crear usuario" />);

    await user.type(screen.getByLabelText('Nombre visible'), 'Ada Lovelace');
    await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ displayName: 'Ada Lovelace', email: 'ada@boticas.pe' })
    );
  });
});
```

- [ ] **Step 8: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/UsuarioForm.test.tsx`
Expected: FAIL

- [ ] **Step 9: Implementar UsuarioForm**

`apps/erp-web/src/features/seguridad/components/UsuarioForm.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { usuarioSchema, type UsuarioFormValues } from '../schemas/usuario.schema';
import { FormField } from './FormField';

export type UsuarioFormProps = {
  defaultValues?: UsuarioFormValues;
  onSubmit: (values: UsuarioFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function UsuarioForm({ defaultValues, onSubmit, submitLabel, isSubmitting = false }: UsuarioFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<UsuarioFormValues>({
    defaultValues:
      defaultValues ?? {
        firstNames: '',
        lastNames: '',
        username: '',
        email: '',
        phone: '',
        displayName: '',
        mfaRequired: false
      },
    mode: 'onTouched',
    resolver: zodResolver(usuarioSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(onSubmit)(event);
      }}
    >
      <FormField label="Nombre visible" htmlFor="usuario-display-name" error={errors.displayName?.message}>
        <input
          id="usuario-display-name"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('displayName')}
        />
      </FormField>

      <div className="grid grid-cols-2 gap-4">
        <FormField label="Nombres" htmlFor="usuario-first-names" error={errors.firstNames?.message}>
          <input
            id="usuario-first-names"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('firstNames')}
          />
        </FormField>
        <FormField label="Apellidos" htmlFor="usuario-last-names" error={errors.lastNames?.message}>
          <input
            id="usuario-last-names"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('lastNames')}
          />
        </FormField>
      </div>

      <FormField label="Correo" htmlFor="usuario-email" error={errors.email?.message}>
        <input
          id="usuario-email"
          type="email"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('email')}
        />
      </FormField>

      <FormField label="Usuario" htmlFor="usuario-username" error={errors.username?.message}>
        <input
          id="usuario-username"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('username')}
        />
      </FormField>

      <FormField label="Teléfono" htmlFor="usuario-phone" error={errors.phone?.message}>
        <input
          id="usuario-phone"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('phone')}
        />
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
          {...register('mfaRequired')}
        />
        Requiere autenticación multifactor
      </label>

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 10: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/UsuarioForm.test.tsx`
Expected: PASS

- [ ] **Step 11: Escribir el test de UsersPage**

`apps/erp-web/src/features/seguridad/pages/UsersPage.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { UsersPage } from './UsersPage';

const authenticatedSession = {
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/', Component: UsersPage }]);
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <RouterProvider router={router} />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

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

describe('UsersPage', () => {
  it('lista los usuarios del tenant activo', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [sampleUsuario], page: 0, size: 20, totalElements: 1 })
      )
    );

    renderPage();

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
  });

  it('crea un usuario nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({
          items: created ? [sampleUsuario] : [],
          page: 0,
          size: 20,
          totalElements: created ? 1 : 0
        })
      ),
      http.post('*/api/v1/usuarios', async () => {
        created = true;
        return HttpResponse.json(sampleUsuario, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron usuarios.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo usuario' }));
    await user.type(screen.getByLabelText('Nombre visible'), 'Ada Lovelace');
    await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    await waitFor(() => expect(screen.getByText('Ada Lovelace')).toBeInTheDocument());
  });

  it('permite buscar por texto', async () => {
    server.use(
      http.get('*/api/v1/usuarios', ({ request }) => {
        const url = new URL(request.url);
        const search = url.searchParams.get('search');
        return HttpResponse.json({
          items: search === 'ada' ? [sampleUsuario] : [],
          page: 0,
          size: 20,
          totalElements: search === 'ada' ? 1 : 0
        });
      })
    );

    const { user } = renderPage();
    await user.type(screen.getByLabelText('Buscar usuario'), 'ada');

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
  });
});
```

- [ ] **Step 12: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/pages/UsersPage.test.tsx`
Expected: FAIL

- [ ] **Step 13: Implementar UsersPage**

`apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`:
```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearUsuario, usuariosQuery } from '../api/usuarios.api';
import type { Usuario } from '../api/usuarios.types';
import type { UsuarioFormValues } from '../schemas/usuario.schema';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';
import { Modal } from '../components/Modal';
import { UsuarioForm } from '../components/UsuarioForm';

export function UsersPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');

  const { data, isPending, isError } = useQuery({
    ...usuariosQuery({ tenantId: tenantId ?? '', search: search || undefined }),
    enabled: Boolean(tenantId)
  });

  const createMutation = useMutation({
    mutationFn: (values: UsuarioFormValues) =>
      crearUsuario(apiClient, {
        tenantId: tenantId ?? '',
        displayName: values.displayName || undefined,
        firstNames: values.firstNames || undefined,
        lastNames: values.lastNames || undefined,
        email: values.email || undefined,
        username: values.username || undefined,
        phone: values.phone || undefined,
        mfaRequired: values.mfaRequired
      }),
    onSuccess: () => {
      setCreateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Usuarios</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Usuarios</h1>
          <p className="mt-2 text-sm text-slate-500">
            Administra las cuentas de acceso al sistema.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo usuario</Button>
      </div>

      <div className="mt-6">
        <label htmlFor="usuarios-search" className="text-sm font-semibold text-slate-700">
          Buscar usuario
        </label>
        <input
          id="usuarios-search"
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Nombre, correo o documento"
          className="mt-2 h-11 w-full max-w-md rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
        />
      </div>

      <div className="mt-6">
        <DataTable<Usuario>
          columns={[
            {
              header: 'Nombre',
              cell: (row) => (
                <Link to={`/seguridad/usuarios/${row.id}`} className="font-semibold text-teal-700 hover:underline">
                  {row.displayName ?? row.email ?? row.username ?? row.id}
                </Link>
              )
            },
            { header: 'Correo', cell: (row) => row.email ?? '—' },
            { header: 'MFA', cell: (row) => (row.mfaRequired ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron usuarios."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de usuarios."
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo usuario">
        <UsuarioForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear usuario"
          isSubmitting={createMutation.isPending}
        />
      </Modal>
    </div>
  );
}
```

- [ ] **Step 14: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/pages/UsersPage.test.tsx`
Expected: PASS

- [ ] **Step 15: Añadir la ruta de usuarios**

Agregar a `apps/erp-web/src/features/seguridad/routes.tsx` (dentro del array `securityRoutes`, después de la entrada `seguridad/permisos`):
```tsx
  {
    path: 'seguridad/usuarios',
    lazy: async () => {
      const { UsersPage } = await import('./pages/UsersPage');
      return { Component: UsersPage };
    }
  },
```

- [ ] **Step 16: Ejecutar toda la suite de seguridad**

Run: `pnpm exec vitest run src/features/seguridad`
Expected: PASS

- [ ] **Step 17: Commit**

```bash
git add apps/erp-web/src/features/seguridad
git commit -m "feat(seguridad): pantalla de usuarios con listado, busqueda y alta"
```

---

## Task 6: Detalle de Usuario (estado, asignación de rol, identidades externas, credencial local)

**Files:**
- Create: `apps/erp-web/src/features/seguridad/api/asignaciones.types.ts`
- Create: `apps/erp-web/src/features/seguridad/api/asignaciones.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/asignaciones.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/api/identidades-externas.types.ts`
- Create: `apps/erp-web/src/features/seguridad/api/identidades-externas.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/identidades-externas.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/schemas/identidad-externa.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/schemas/credencial-local.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/api/credencial-local.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/credencial-local.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/AsignarRolDialog.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/CredencialLocalForm.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/UserDetailPage.test.tsx`
- Modify: `apps/erp-web/src/features/seguridad/routes.tsx`

**Interfaces:**
- Consumes: `Usuario` de `../api/usuarios.types`, `cambiarEstadoUsuario` de `../api/usuarios.api`, `Rol`/`rolesQuery` de `../api/roles.api`, `DataTable`/`EstadoBadge`/`Modal`/`ConfirmActionDialog`/`FormField` de `../components`.
- Produces:
  - `AsignacionRol` type: `{ id: string; roleId: string; roleCode: string; roleName: string; scopeType: string; companyId: string | null; establishmentId: string | null; warehouseId: string | null; terminalId: string | null; validFrom: string | null; validUntil: string | null; status: string; createdBy: string; createdAt: string }`.
  - `asignacionesQuery(tenantId, userId)` → queryOptions. `asignarRol(client, userId, payload): Promise<AsignacionRol>`. `eliminarAsignacion(client, userId, assignmentId, tenantId): Promise<void>`.
  - `IdentidadExterna` type: `{ provider: string; subject: string; issuer: string | null; emailClaim: string | null; lastLoginAt: string | null; createdAt: string }`.
  - `identidadesExternasQuery(tenantId, userId)` → queryOptions. `vincularIdentidadExterna(client, userId, tenantId, payload): Promise<void>`. `desvincularIdentidadExterna(client, userId, tenantId, provider, subject): Promise<void>`.
  - `provisionarCredencialLocal(client, userId, payload: { tenantId: string; password: string; requireChange?: boolean }): Promise<void>`.
  - `AsignarRolDialog({ open, tenantId, userId, onClose, onAssigned }): JSX.Element`.
  - `IdentidadExternaForm({ onSubmit, isSubmitting }): JSX.Element`.
  - `CredencialLocalForm({ onSubmit, isSubmitting }): JSX.Element`.

- [ ] **Step 1: Definir los tipos de asignaciones**

`apps/erp-web/src/features/seguridad/api/asignaciones.types.ts`:
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
  companyId?: string;
  establishmentId?: string;
  warehouseId?: string;
  terminalId?: string;
};
```

- [ ] **Step 2: Escribir el test de asignaciones.api.ts**

`apps/erp-web/src/features/seguridad/api/asignaciones.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { asignarRol, eliminarAsignacion, fetchAsignaciones } from './asignaciones.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleAsignacion = {
  id: 'assignment-1',
  roleId: 'rol-1',
  roleCode: 'ADMIN_LOCAL',
  roleName: 'Administrador local',
  scopeType: 'ESTABLECIMIENTO',
  companyId: 'company-1',
  establishmentId: 'establishment-1',
  warehouseId: null,
  terminalId: null,
  validFrom: null,
  validUntil: null,
  status: 'ACTIVO',
  createdBy: 'admin',
  createdAt: '2026-09-01T00:00:00Z'
};

describe('asignaciones.api', () => {
  it('fetchAsignaciones consulta las asignaciones de un usuario', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/usuarios/user-1/asignaciones-rol', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleAsignacion]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchAsignaciones(client, 'tenant-1', 'user-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual([sampleAsignacion]);
  });

  it('asignarRol envia el payload al endpoint de role-assignments', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/usuarios/user-1/role-assignments', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleAsignacion, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await asignarRol(client, 'user-1', {
      tenantId: 'tenant-1',
      roleId: 'rol-1',
      scopeType: 'ESTABLECIMIENTO',
      companyId: 'company-1',
      establishmentId: 'establishment-1'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      roleId: 'rol-1',
      scopeType: 'ESTABLECIMIENTO',
      companyId: 'company-1',
      establishmentId: 'establishment-1'
    });
    expect(result).toEqual(sampleAsignacion);
  });

  it('eliminarAsignacion envia DELETE con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.delete('http://localhost/api/v1/usuarios/user-1/asignaciones-rol/assignment-1', ({ request }) => {
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await eliminarAsignacion(client, 'user-1', 'assignment-1', 'tenant-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
  });
});
```

- [ ] **Step 3: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/asignaciones.api.test.ts`
Expected: FAIL

- [ ] **Step 4: Implementar asignaciones.api.ts**

`apps/erp-web/src/features/seguridad/api/asignaciones.api.ts`:
```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { AsignacionRol, AsignarRolPayload } from './asignaciones.types';

export function fetchAsignaciones(client: ApiClient, tenantId: string, userId: string): Promise<AsignacionRol[]> {
  return client.get<AsignacionRol[]>(
    `/usuarios/${userId}/asignaciones-rol?tenantId=${encodeURIComponent(tenantId)}`
  );
}

export function asignacionesQuery(tenantId: string, userId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'asignaciones', tenantId, userId],
    queryFn: () => fetchAsignaciones(apiClient, tenantId, userId)
  });
}

export function asignarRol(client: ApiClient, userId: string, payload: AsignarRolPayload): Promise<AsignacionRol> {
  return client.post<AsignacionRol, AsignarRolPayload>(`/usuarios/${userId}/role-assignments`, payload);
}

export function eliminarAsignacion(
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

- [ ] **Step 5: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/asignaciones.api.test.ts`
Expected: PASS

- [ ] **Step 6: Definir los tipos de identidades externas**

`apps/erp-web/src/features/seguridad/api/identidades-externas.types.ts`:
```ts
export type IdentidadExterna = {
  provider: string;
  subject: string;
  issuer: string | null;
  emailClaim: string | null;
  lastLoginAt: string | null;
  createdAt: string;
};

export type VincularIdentidadExternaPayload = {
  provider: string;
  subject: string;
  issuer?: string;
  emailClaim?: string;
};
```

- [ ] **Step 7: Escribir el test de identidades-externas.api.ts**

`apps/erp-web/src/features/seguridad/api/identidades-externas.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  desvincularIdentidadExterna,
  fetchIdentidadesExternas,
  vincularIdentidadExterna
} from './identidades-externas.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleIdentidad = {
  provider: 'google',
  subject: 'sub-123',
  issuer: 'https://accounts.google.com',
  emailClaim: 'ada@boticas.pe',
  lastLoginAt: null,
  createdAt: '2026-09-01T00:00:00Z'
};

describe('identidades-externas.api', () => {
  it('fetchIdentidadesExternas consulta el listado del usuario', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/usuarios/user-1/identidades-externas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleIdentidad]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchIdentidadesExternas(client, 'tenant-1', 'user-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual([sampleIdentidad]);
  });

  it('vincularIdentidadExterna envia POST con el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/usuarios/user-1/identidades-externas', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleIdentidad, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await vincularIdentidadExterna(client, 'user-1', 'tenant-1', { provider: 'google', subject: 'sub-123' });

    expect(receivedBody).toEqual({ provider: 'google', subject: 'sub-123' });
  });

  it('desvincularIdentidadExterna envia DELETE con provider y subject como query', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.delete('http://localhost/api/v1/usuarios/user-1/identidades-externas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await desvincularIdentidadExterna(client, 'user-1', 'tenant-1', 'google', 'sub-123');

    expect(receivedUrl?.searchParams.get('provider')).toBe('google');
    expect(receivedUrl?.searchParams.get('subject')).toBe('sub-123');
    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
  });
});
```

- [ ] **Step 8: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/identidades-externas.api.test.ts`
Expected: FAIL

- [ ] **Step 9: Implementar identidades-externas.api.ts**

`apps/erp-web/src/features/seguridad/api/identidades-externas.api.ts`:
```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { IdentidadExterna, VincularIdentidadExternaPayload } from './identidades-externas.types';

export function fetchIdentidadesExternas(
  client: ApiClient,
  tenantId: string,
  userId: string
): Promise<IdentidadExterna[]> {
  return client.get<IdentidadExterna[]>(
    `/usuarios/${userId}/identidades-externas?tenantId=${encodeURIComponent(tenantId)}`
  );
}

export function identidadesExternasQuery(tenantId: string, userId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'identidades-externas', tenantId, userId],
    queryFn: () => fetchIdentidadesExternas(apiClient, tenantId, userId)
  });
}

export function vincularIdentidadExterna(
  client: ApiClient,
  userId: string,
  tenantId: string,
  payload: VincularIdentidadExternaPayload
): Promise<void> {
  return client.post<void, VincularIdentidadExternaPayload>(
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

- [ ] **Step 10: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/identidades-externas.api.test.ts`
Expected: PASS

- [ ] **Step 11: Definir el schema de identidad externa**

`apps/erp-web/src/features/seguridad/schemas/identidad-externa.schema.ts`:
```ts
import { z } from 'zod';

export const identidadExternaSchema = z.object({
  provider: z.string().min(1, 'Selecciona un proveedor.').max(100),
  subject: z.string().min(1, 'El identificador es obligatorio.').max(300),
  issuer: z.string().max(500, 'El emisor no debe exceder 500 caracteres.').optional(),
  emailClaim: z.email('Ingresa un correo electrónico válido.').optional().or(z.literal(''))
});

export type IdentidadExternaFormValues = z.infer<typeof identidadExternaSchema>;
```

- [ ] **Step 12: Escribir el test de IdentidadExternaForm**

`apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { IdentidadExternaForm } from './IdentidadExternaForm';

describe('IdentidadExternaForm', () => {
  it('muestra errores cuando el proveedor y el identificador estan vacios', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<IdentidadExternaForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    expect(await screen.findByText('Selecciona un proveedor.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<IdentidadExternaForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Proveedor'), 'google');
    await user.type(screen.getByLabelText('Identificador (subject)'), 'sub-123');
    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ provider: 'google', subject: 'sub-123' }));
  });
});
```

- [ ] **Step 13: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/IdentidadExternaForm.test.tsx`
Expected: FAIL

- [ ] **Step 14: Implementar IdentidadExternaForm**

`apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { identidadExternaSchema, type IdentidadExternaFormValues } from '../schemas/identidad-externa.schema';
import { FormField } from './FormField';

export type IdentidadExternaFormProps = {
  onSubmit: (values: IdentidadExternaFormValues) => void;
  isSubmitting?: boolean;
};

export function IdentidadExternaForm({ onSubmit, isSubmitting = false }: IdentidadExternaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<IdentidadExternaFormValues>({
    defaultValues: { provider: '', subject: '', issuer: '', emailClaim: '' },
    mode: 'onTouched',
    resolver: zodResolver(identidadExternaSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(onSubmit)(event);
      }}
    >
      <FormField label="Proveedor" htmlFor="identidad-provider" error={errors.provider?.message}>
        <input
          id="identidad-provider"
          placeholder="google, azure-ad, okta…"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('provider')}
        />
      </FormField>

      <FormField label="Identificador (subject)" htmlFor="identidad-subject" error={errors.subject?.message}>
        <input
          id="identidad-subject"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('subject')}
        />
      </FormField>

      <FormField label="Emisor" htmlFor="identidad-issuer" error={errors.issuer?.message}>
        <input
          id="identidad-issuer"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('issuer')}
        />
      </FormField>

      <FormField label="Correo asociado" htmlFor="identidad-email-claim" error={errors.emailClaim?.message}>
        <input
          id="identidad-email-claim"
          type="email"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('emailClaim')}
        />
      </FormField>

      <Button type="submit" disabled={isSubmitting}>
        Vincular
      </Button>
    </form>
  );
}
```

- [ ] **Step 15: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/IdentidadExternaForm.test.tsx`
Expected: PASS

- [ ] **Step 16: Escribir el test de AsignarRolDialog**

`apps/erp-web/src/features/seguridad/components/AsignarRolDialog.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { AsignarRolDialog } from './AsignarRolDialog';

function renderDialog(onAssigned = vi.fn()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    onAssigned,
    ...render(
      <QueryClientProvider client={queryClient}>
        <AsignarRolDialog open tenantId="tenant-1" userId="user-1" onClose={() => {}} onAssigned={onAssigned} />
      </QueryClientProvider>
    )
  };
}

describe('AsignarRolDialog', () => {
  it('lista los roles disponibles y asigna el seleccionado', async () => {
    server.use(
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
      http.post('*/api/v1/usuarios/user-1/role-assignments', async ({ request }) => {
        const body = (await request.json()) as { roleId: string; scopeType: string };
        expect(body.roleId).toBe('rol-1');
        expect(body.scopeType).toBe('GLOBAL');
        return HttpResponse.json(
          {
            id: 'assignment-1',
            roleId: 'rol-1',
            roleCode: 'ADMIN_LOCAL',
            roleName: 'Administrador local',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'admin',
            createdAt: '2026-09-01T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user, onAssigned } = renderDialog();

    await user.selectOptions(await screen.findByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Asignar' }));

    await waitFor(() => expect(onAssigned).toHaveBeenCalled());
  });
});
```

- [ ] **Step 17: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/AsignarRolDialog.test.tsx`
Expected: FAIL — `./AsignarRolDialog` no existe.

- [ ] **Step 18: Implementar AsignarRolDialog**

`apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`:
```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { asignarRol } from '../api/asignaciones.api';
import { rolesQuery } from '../api/roles.api';
import { FormField } from './FormField';
import { Modal } from './Modal';

export type AsignarRolDialogProps = {
  open: boolean;
  tenantId: string;
  userId: string;
  onClose: () => void;
  onAssigned: () => void;
};

const SCOPE_TYPES = ['GLOBAL', 'EMPRESA', 'ESTABLECIMIENTO', 'ALMACEN', 'TERMINAL'] as const;

export function AsignarRolDialog({ open, tenantId, userId, onClose, onAssigned }: AsignarRolDialogProps) {
  const [roleId, setRoleId] = useState('');
  const [scopeType, setScopeType] = useState<(typeof SCOPE_TYPES)[number]>('GLOBAL');
  const queryClient = useQueryClient();

  const rolesResult = useQuery({ ...rolesQuery({ tenantId, size: 100 }), enabled: open });

  const assignMutation = useMutation({
    mutationFn: () => asignarRol(apiClient, userId, { tenantId, roleId, scopeType }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'asignaciones', tenantId, userId] });
      onAssigned();
    }
  });

  return (
    <Modal open={open} onClose={onClose} title="Asignar rol">
      <div className="space-y-4">
        <FormField label="Rol" htmlFor="asignar-rol-id">
          <select
            id="asignar-rol-id"
            value={roleId}
            onChange={(event) => setRoleId(event.target.value)}
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          >
            <option value="">Selecciona un rol</option>
            {(rolesResult.data?.items ?? []).map((rol) => (
              <option key={rol.id} value={rol.id}>
                {rol.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="Ámbito" htmlFor="asignar-rol-scope">
          <select
            id="asignar-rol-scope"
            value={scopeType}
            onChange={(event) => setScopeType(event.target.value as (typeof SCOPE_TYPES)[number])}
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          >
            {SCOPE_TYPES.map((scope) => (
              <option key={scope} value={scope}>
                {scope}
              </option>
            ))}
          </select>
        </FormField>

        <div className="flex justify-end">
          <Button
            type="button"
            onClick={() => assignMutation.mutate()}
            disabled={!roleId || assignMutation.isPending}
          >
            Asignar
          </Button>
        </div>
      </div>
    </Modal>
  );
}
```

- [ ] **Step 19: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/AsignarRolDialog.test.tsx`
Expected: PASS

- [ ] **Step 20: Definir el schema de credencial local**

`apps/erp-web/src/features/seguridad/schemas/credencial-local.schema.ts`:
```ts
import { z } from 'zod';

export const credencialLocalSchema = z.object({
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.').max(128),
  requireChange: z.boolean()
});

export type CredencialLocalFormValues = z.infer<typeof credencialLocalSchema>;
```

- [ ] **Step 21: Escribir el test de credencial-local.api.ts**

`apps/erp-web/src/features/seguridad/api/credencial-local.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { provisionarCredencialLocal } from './credencial-local.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('credencial-local.api', () => {
  it('provisionarCredencialLocal envia el payload al endpoint correcto', async () => {
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
      password: 'ContrasenaSegura1!',
      requireChange: true
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      password: 'ContrasenaSegura1!',
      requireChange: true
    });
  });
});
```

- [ ] **Step 22: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/credencial-local.api.test.ts`
Expected: FAIL

- [ ] **Step 23: Implementar credencial-local.api.ts**

`apps/erp-web/src/features/seguridad/api/credencial-local.api.ts`:
```ts
import type { ApiClient } from '@boticas/api-client';

export type ProvisionarCredencialLocalPayload = {
  tenantId: string;
  password: string;
  requireChange?: boolean;
};

export function provisionarCredencialLocal(
  client: ApiClient,
  userId: string,
  payload: ProvisionarCredencialLocalPayload
): Promise<void> {
  return client.post<void, ProvisionarCredencialLocalPayload>(`/usuarios/${userId}/credencial-local`, payload);
}
```

- [ ] **Step 24: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/credencial-local.api.test.ts`
Expected: PASS

- [ ] **Step 25: Escribir el test de CredencialLocalForm**

`apps/erp-web/src/features/seguridad/components/CredencialLocalForm.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CredencialLocalForm } from './CredencialLocalForm';

describe('CredencialLocalForm', () => {
  it('muestra un error cuando la contraseña es muy corta', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña inicial'), '123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(await screen.findByText('La contraseña debe tener al menos 8 caracteres.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña inicial'), 'ContrasenaSegura1!');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ password: 'ContrasenaSegura1!', requireChange: true })
    );
  });
});
```

- [ ] **Step 26: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/CredencialLocalForm.test.tsx`
Expected: FAIL

- [ ] **Step 27: Implementar CredencialLocalForm**

`apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { credencialLocalSchema, type CredencialLocalFormValues } from '../schemas/credencial-local.schema';
import { FormField } from './FormField';

export type CredencialLocalFormProps = {
  onSubmit: (values: CredencialLocalFormValues) => void;
  isSubmitting?: boolean;
};

export function CredencialLocalForm({ onSubmit, isSubmitting = false }: CredencialLocalFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<CredencialLocalFormValues>({
    defaultValues: { password: '', requireChange: true },
    mode: 'onTouched',
    resolver: zodResolver(credencialLocalSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(onSubmit)(event);
      }}
    >
      <FormField label="Contraseña inicial" htmlFor="credencial-password" error={errors.password?.message}>
        <input
          id="credencial-password"
          type="password"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('password')}
        />
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
          {...register('requireChange')}
        />
        Requerir cambio de contraseña en el próximo inicio de sesión
      </label>

      <Button type="submit" disabled={isSubmitting}>
        Fijar contraseña
      </Button>
    </form>
  );
}
```

- [ ] **Step 28: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/CredencialLocalForm.test.tsx`
Expected: PASS

- [ ] **Step 29: Escribir el test de UserDetailPage**

`apps/erp-web/src/features/seguridad/pages/UserDetailPage.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
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

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/seguridad/usuarios/:userId', Component: UserDetailPage }], {
    initialEntries: ['/seguridad/usuarios/user-1']
  });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <RouterProvider router={router} />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

describe('UserDetailPage', () => {
  it('muestra los datos del usuario y sus asignaciones de rol', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [sampleUsuario], page: 0, size: 100, totalElements: 1 })
      ),
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () =>
        HttpResponse.json([
          {
            id: 'assignment-1',
            roleId: 'rol-1',
            roleCode: 'ADMIN_LOCAL',
            roleName: 'Administrador local',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'admin',
            createdAt: '2026-09-01T00:00:00Z'
          }
        ])
      ),
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([]))
    );

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Ada Lovelace' })).toBeInTheDocument();
    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
  });

  it('cambia el estado del usuario tras confirmar', async () => {
    let currentStatus = 'ACTIVO';
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({
          items: [{ ...sampleUsuario, status: currentStatus }],
          page: 0,
          size: 100,
          totalElements: 1
        })
      ),
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () => HttpResponse.json([])),
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([])),
      http.patch('*/api/v1/usuarios/user-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentStatus = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByRole('heading', { name: 'Ada Lovelace' });
    await user.click(screen.getByRole('button', { name: 'Desactivar usuario' }));
    await user.click(screen.getByRole('button', { name: 'Desactivar' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });

  it('fija una contraseña inicial para el usuario', async () => {
    let provisioned = false;
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [sampleUsuario], page: 0, size: 100, totalElements: 1 })
      ),
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () => HttpResponse.json([])),
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([])),
      http.post('*/api/v1/usuarios/user-1/credencial-local', async ({ request }) => {
        const body = (await request.json()) as { password: string };
        provisioned = body.password === 'ContrasenaSegura1!';
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByRole('heading', { name: 'Ada Lovelace' });
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));
    await user.type(screen.getByLabelText('Contraseña inicial'), 'ContrasenaSegura1!');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    await waitFor(() => expect(provisioned).toBe(true));
  });
});
```

- [ ] **Step 30: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/pages/UserDetailPage.test.tsx`
Expected: FAIL

- [ ] **Step 31: Implementar UserDetailPage**

`apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`:
```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { Button, Card } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { asignacionesQuery, eliminarAsignacion } from '../api/asignaciones.api';
import { provisionarCredencialLocal } from '../api/credencial-local.api';
import {
  desvincularIdentidadExterna,
  identidadesExternasQuery,
  vincularIdentidadExterna
} from '../api/identidades-externas.api';
import { cambiarEstadoUsuario, usuariosQuery } from '../api/usuarios.api';
import { AsignarRolDialog } from '../components/AsignarRolDialog';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { CredencialLocalForm } from '../components/CredencialLocalForm';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';
import { IdentidadExternaForm } from '../components/IdentidadExternaForm';
import { Modal } from '../components/Modal';

export function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [assignRoleOpen, setAssignRoleOpen] = useState(false);
  const [linkIdentityOpen, setLinkIdentityOpen] = useState(false);
  const [setPasswordOpen, setSetPasswordOpen] = useState(false);
  const [confirmStatusOpen, setConfirmStatusOpen] = useState(false);

  const usuariosResult = useQuery({
    ...usuariosQuery({ tenantId: tenantId ?? '', size: 100 }),
    enabled: Boolean(tenantId)
  });
  const usuario = usuariosResult.data?.items.find((item) => item.id === userId);

  const asignacionesResult = useQuery({
    ...asignacionesQuery(tenantId ?? '', userId ?? ''),
    enabled: Boolean(tenantId && userId)
  });

  const identidadesResult = useQuery({
    ...identidadesExternasQuery(tenantId ?? '', userId ?? ''),
    enabled: Boolean(tenantId && userId)
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (status: string) => cambiarEstadoUsuario(apiClient, userId ?? '', tenantId ?? '', status),
    onSuccess: () => {
      setConfirmStatusOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
    }
  });

  const removeAssignmentMutation = useMutation({
    mutationFn: (assignmentId: string) => eliminarAsignacion(apiClient, userId ?? '', assignmentId, tenantId ?? ''),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'asignaciones', tenantId, userId] });
    }
  });

  const linkIdentityMutation = useMutation({
    mutationFn: (values: { provider: string; subject: string; issuer?: string; emailClaim?: string }) =>
      vincularIdentidadExterna(apiClient, userId ?? '', tenantId ?? '', values),
    onSuccess: () => {
      setLinkIdentityOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'identidades-externas', tenantId, userId] });
    }
  });

  const unlinkIdentityMutation = useMutation({
    mutationFn: (identity: { provider: string; subject: string }) =>
      desvincularIdentidadExterna(apiClient, userId ?? '', tenantId ?? '', identity.provider, identity.subject),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'identidades-externas', tenantId, userId] });
    }
  });

  const setPasswordMutation = useMutation({
    mutationFn: (values: { password: string; requireChange: boolean }) =>
      provisionarCredencialLocal(apiClient, userId ?? '', {
        tenantId: tenantId ?? '',
        password: values.password,
        requireChange: values.requireChange
      }),
    onSuccess: () => {
      setSetPasswordOpen(false);
    }
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
            {usuario.displayName ?? usuario.email ?? usuario.id}
          </h1>
          <p className="mt-1 text-sm text-slate-500">{usuario.email}</p>
        </div>
        <EstadoBadge status={usuario.status} />
      </div>

      <Card className="mt-6 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-bold text-slate-950">Roles asignados</h2>
          <Button size="sm" onClick={() => setAssignRoleOpen(true)}>
            Asignar rol
          </Button>
        </div>
        <div className="mt-4">
          <DataTable
            columns={[
              { header: 'Rol', cell: (row) => row.roleName },
              { header: 'Ámbito', cell: (row) => row.scopeType },
              { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
              {
                header: '',
                cell: (row) => (
                  <Button size="sm" variant="ghost" onClick={() => removeAssignmentMutation.mutate(row.id)}>
                    Quitar
                  </Button>
                )
              }
            ]}
            rows={asignacionesResult.data ?? []}
            rowKey={(row) => row.id}
            emptyMessage="Este usuario no tiene roles asignados."
            isLoading={asignacionesResult.isPending}
            isError={asignacionesResult.isError}
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
          <DataTable
            columns={[
              { header: 'Proveedor', cell: (row) => row.provider },
              { header: 'Identificador', cell: (row) => row.subject },
              { header: 'Correo', cell: (row) => row.emailClaim ?? '—' },
              {
                header: '',
                cell: (row) => (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => unlinkIdentityMutation.mutate({ provider: row.provider, subject: row.subject })}
                  >
                    Desvincular
                  </Button>
                )
              }
            ]}
            rows={identidadesResult.data ?? []}
            rowKey={(row) => `${row.provider}:${row.subject}`}
            emptyMessage="Este usuario no tiene identidades externas vinculadas."
            isLoading={identidadesResult.isPending}
            isError={identidadesResult.isError}
          />
        </div>
      </Card>

      <div className="mt-6 flex justify-end gap-3">
        <Button variant="secondary" onClick={() => setSetPasswordOpen(true)}>
          Fijar contraseña
        </Button>
        <Button variant="secondary" onClick={() => setConfirmStatusOpen(true)}>
          {usuario.status === 'ACTIVO' ? 'Desactivar usuario' : 'Activar usuario'}
        </Button>
      </div>

      <AsignarRolDialog
        open={assignRoleOpen}
        tenantId={tenantId ?? ''}
        userId={userId ?? ''}
        onClose={() => setAssignRoleOpen(false)}
        onAssigned={() => setAssignRoleOpen(false)}
      />

      <Modal open={linkIdentityOpen} onClose={() => setLinkIdentityOpen(false)} title="Vincular identidad externa">
        <IdentidadExternaForm
          onSubmit={(values) =>
            linkIdentityMutation.mutate({
              provider: values.provider,
              subject: values.subject,
              issuer: values.issuer || undefined,
              emailClaim: values.emailClaim || undefined
            })
          }
          isSubmitting={linkIdentityMutation.isPending}
        />
      </Modal>

      <Modal open={setPasswordOpen} onClose={() => setSetPasswordOpen(false)} title="Fijar contraseña inicial">
        <CredencialLocalForm
          onSubmit={(values) => setPasswordMutation.mutate(values)}
          isSubmitting={setPasswordMutation.isPending}
        />
      </Modal>

      <ConfirmActionDialog
        open={confirmStatusOpen}
        title={usuario.status === 'ACTIVO' ? 'Desactivar usuario' : 'Activar usuario'}
        description={
          usuario.status === 'ACTIVO'
            ? 'El usuario perderá acceso al sistema hasta que se reactive.'
            : 'El usuario podrá volver a iniciar sesión.'
        }
        confirmLabel={usuario.status === 'ACTIVO' ? 'Desactivar' : 'Activar'}
        tone={usuario.status === 'ACTIVO' ? 'danger' : 'default'}
        isPending={toggleStatusMutation.isPending}
        onConfirm={() => toggleStatusMutation.mutate(usuario.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')}
        onCancel={() => setConfirmStatusOpen(false)}
      />
    </div>
  );
}
```

- [ ] **Step 32: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/pages/UserDetailPage.test.tsx`
Expected: PASS

- [ ] **Step 33: Añadir la ruta de detalle de usuario**

Agregar a `apps/erp-web/src/features/seguridad/routes.tsx` (después de la entrada `seguridad/usuarios`):
```tsx
  {
    path: 'seguridad/usuarios/:userId',
    lazy: async () => {
      const { UserDetailPage } = await import('./pages/UserDetailPage');
      return { Component: UserDetailPage };
    }
  },
```

- [ ] **Step 34: Ejecutar toda la suite de seguridad**

Run: `pnpm exec vitest run src/features/seguridad`
Expected: PASS

- [ ] **Step 35: Commit**

```bash
git add apps/erp-web/src/features/seguridad
git commit -m "feat(seguridad): detalle de usuario con roles, identidades externas y cambio de estado"
```

---

## Task 7: Pantallas de Sesiones y Dispositivos

**Files:**
- Create: `apps/erp-web/src/features/seguridad/api/sesiones.types.ts`
- Create: `apps/erp-web/src/features/seguridad/api/sesiones.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/sesiones.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/pages/SessionsPage.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/SessionsPage.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/api/dispositivos.types.ts`
- Create: `apps/erp-web/src/features/seguridad/api/dispositivos.api.ts`
- Create: `apps/erp-web/src/features/seguridad/api/dispositivos.api.test.ts`
- Create: `apps/erp-web/src/features/seguridad/schemas/dispositivo.schema.ts`
- Create: `apps/erp-web/src/features/seguridad/components/DispositivoForm.tsx`
- Create: `apps/erp-web/src/features/seguridad/components/DispositivoForm.test.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/DevicesPage.tsx`
- Create: `apps/erp-web/src/features/seguridad/pages/DevicesPage.test.tsx`
- Modify: `apps/erp-web/src/features/seguridad/routes.tsx`

**Interfaces:**
- Consumes: `useAuthSession`, `DataTable`, `EstadoBadge`, `Modal`, `ConfirmActionDialog`, `FormField` de sus respectivas ubicaciones ya creadas.
- Produces:
  - `Sesion` type: `{ id: string; userId: string; provider: string | null; authMethod: string | null; channel: string; ipAddress: string | null; userAgent: string | null; deviceId: string | null; loginAt: string; lastUsedAt: string | null; expiresAt: string | null; logoutAt: string | null; revokedAt: string | null; revocationReason: string | null; status: string }`.
  - `sesionesQuery(tenantId, userId?)` → queryOptions. `revocarSesion(client, sessionId, tenantId, reason?): Promise<void>`.
  - `Dispositivo` type: `{ id: string; companyId: string; establishmentId: string; terminalId: string | null; fingerprintHash: string | null; certificateThumbprint: string | null; agentVersion: string | null; status: string; registeredAt: string; lastContactAt: string | null }`.
  - `dispositivosQuery(tenantId)` → queryOptions. `registrarDispositivo(client, payload): Promise<Dispositivo>`. `cambiarEstadoDispositivo(client, deviceId, tenantId, status): Promise<void>`.
  - `dispositivoSchema` (zod) → `DispositivoFormValues = { companyId: string; establishmentId: string; terminalId?: string; fingerprintHash?: string; certificateThumbprint?: string; agentVersion?: string }`.

- [ ] **Step 1: Definir los tipos de sesiones**

`apps/erp-web/src/features/seguridad/api/sesiones.types.ts`:
```ts
export type Sesion = {
  id: string;
  userId: string;
  provider: string | null;
  authMethod: string | null;
  channel: string;
  ipAddress: string | null;
  userAgent: string | null;
  deviceId: string | null;
  loginAt: string;
  lastUsedAt: string | null;
  expiresAt: string | null;
  logoutAt: string | null;
  revokedAt: string | null;
  revocationReason: string | null;
  status: string;
};
```

- [ ] **Step 2: Escribir el test de sesiones.api.ts**

`apps/erp-web/src/features/seguridad/api/sesiones.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { fetchSesiones, revocarSesion } from './sesiones.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleSesion = {
  id: 'session-1',
  userId: 'user-1',
  provider: 'local',
  authMethod: 'PASSWORD',
  channel: 'WEB',
  ipAddress: '10.0.0.1',
  userAgent: 'Mozilla/5.0',
  deviceId: null,
  loginAt: '2026-09-01T10:00:00Z',
  lastUsedAt: '2026-09-01T10:05:00Z',
  expiresAt: '2026-09-01T10:10:00Z',
  logoutAt: null,
  revokedAt: null,
  revocationReason: null,
  status: 'ACTIVA'
};

describe('sesiones.api', () => {
  it('fetchSesiones consulta /sesiones con tenantId y userId opcional', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/sesiones', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleSesion]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchSesiones(client, 'tenant-1', 'user-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('userId')).toBe('user-1');
    expect(result).toEqual([sampleSesion]);
  });

  it('fetchSesiones omite userId cuando no se provee', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/sesiones', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await fetchSesiones(client, 'tenant-1');

    expect(receivedUrl?.searchParams.has('userId')).toBe(false);
  });

  it('revocarSesion envia POST con el motivo', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.post('http://localhost/api/v1/sesiones/session-1/revocacion', async ({ request }) => {
        receivedBody = await request.json();
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await revocarSesion(client, 'session-1', 'tenant-1', 'Cierre administrativo');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ reason: 'Cierre administrativo' });
  });
});
```

- [ ] **Step 3: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/sesiones.api.test.ts`
Expected: FAIL

- [ ] **Step 4: Implementar sesiones.api.ts**

`apps/erp-web/src/features/seguridad/api/sesiones.api.ts`:
```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { Sesion } from './sesiones.types';

export function fetchSesiones(client: ApiClient, tenantId: string, userId?: string): Promise<Sesion[]> {
  const query = new URLSearchParams({ tenantId });
  if (userId) query.set('userId', userId);
  return client.get<Sesion[]>(`/sesiones?${query.toString()}`);
}

export function sesionesQuery(tenantId: string, userId?: string) {
  return queryOptions({
    queryKey: ['seguridad', 'sesiones', tenantId, userId ?? ''],
    queryFn: () => fetchSesiones(apiClient, tenantId, userId)
  });
}

export function revocarSesion(
  client: ApiClient,
  sessionId: string,
  tenantId: string,
  reason?: string
): Promise<void> {
  return client.post<void, { reason?: string }>(
    `/sesiones/${sessionId}/revocacion?tenantId=${encodeURIComponent(tenantId)}`,
    { reason }
  );
}
```

- [ ] **Step 5: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/sesiones.api.test.ts`
Expected: PASS

- [ ] **Step 6: Escribir el test de SessionsPage**

`apps/erp-web/src/features/seguridad/pages/SessionsPage.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { SessionsPage } from './SessionsPage';

const authenticatedSession = {
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <SessionsPage />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

const sampleSesion = {
  id: 'session-1',
  userId: 'user-1',
  provider: 'local',
  authMethod: 'PASSWORD',
  channel: 'WEB',
  ipAddress: '10.0.0.1',
  userAgent: 'Mozilla/5.0',
  deviceId: null,
  loginAt: '2026-09-01T10:00:00Z',
  lastUsedAt: '2026-09-01T10:05:00Z',
  expiresAt: '2026-09-01T10:10:00Z',
  logoutAt: null,
  revokedAt: null,
  revocationReason: null,
  status: 'ACTIVA'
};

describe('SessionsPage', () => {
  it('lista las sesiones del tenant activo', async () => {
    server.use(http.get('*/api/v1/sesiones', () => HttpResponse.json([sampleSesion])));

    renderPage();

    expect(await screen.findByText('WEB')).toBeInTheDocument();
    expect(screen.getByText('10.0.0.1')).toBeInTheDocument();
  });

  it('revoca una sesion tras confirmar', async () => {
    let revoked = false;
    server.use(
      http.get('*/api/v1/sesiones', () =>
        HttpResponse.json([{ ...sampleSesion, status: revoked ? 'REVOCADA' : 'ACTIVA' }])
      ),
      http.post('*/api/v1/sesiones/session-1/revocacion', () => {
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByText('WEB');
    await user.click(screen.getByRole('button', { name: 'Revocar' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar revocación' }));

    await waitFor(() => expect(screen.getByText('REVOCADA')).toBeInTheDocument());
  });
});
```

- [ ] **Step 7: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/pages/SessionsPage.test.tsx`
Expected: FAIL

- [ ] **Step 8: Implementar SessionsPage**

`apps/erp-web/src/features/seguridad/pages/SessionsPage.tsx`:
```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { revocarSesion, sesionesQuery } from '../api/sesiones.api';
import type { Sesion } from '../api/sesiones.types';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';

function formatDate(value: string | null) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
}

export function SessionsPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [sessionToRevoke, setSessionToRevoke] = useState<Sesion | null>(null);

  const { data, isPending, isError } = useQuery({
    ...sesionesQuery(tenantId ?? ''),
    enabled: Boolean(tenantId)
  });

  const revokeMutation = useMutation({
    mutationFn: () => revocarSesion(apiClient, sessionToRevoke?.id ?? '', tenantId ?? ''),
    onSuccess: () => {
      setSessionToRevoke(null);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'sesiones'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div>
        <p className="text-sm font-semibold text-teal-700">Seguridad / Sesiones</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Sesiones</h1>
        <p className="mt-2 text-sm text-slate-500">Sesiones activas e históricas del tenant.</p>
      </div>

      <div className="mt-6">
        <DataTable<Sesion>
          columns={[
            { header: 'Canal', cell: (row) => row.channel },
            { header: 'IP', cell: (row) => row.ipAddress ?? '—' },
            { header: 'Inicio', cell: (row) => formatDate(row.loginAt) },
            { header: 'Último uso', cell: (row) => formatDate(row.lastUsedAt) },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
            {
              header: '',
              cell: (row) =>
                row.status === 'ACTIVA' ? (
                  <button
                    type="button"
                    onClick={() => setSessionToRevoke(row)}
                    className="text-xs font-semibold text-rose-700 hover:underline"
                  >
                    Revocar
                  </button>
                ) : null
            }
          ]}
          rows={data ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No hay sesiones registradas."
          isLoading={isPending}
          isError={isError}
        />
      </div>

      <ConfirmActionDialog
        open={sessionToRevoke !== null}
        title="Revocar sesión"
        description="El usuario deberá volver a iniciar sesión en ese dispositivo."
        confirmLabel="Confirmar revocación"
        tone="danger"
        isPending={revokeMutation.isPending}
        onConfirm={() => revokeMutation.mutate()}
        onCancel={() => setSessionToRevoke(null)}
      />
    </div>
  );
}
```

- [ ] **Step 9: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/pages/SessionsPage.test.tsx`
Expected: PASS

- [ ] **Step 10: Definir los tipos de dispositivos**

`apps/erp-web/src/features/seguridad/api/dispositivos.types.ts`:
```ts
export type Dispositivo = {
  id: string;
  companyId: string;
  establishmentId: string;
  terminalId: string | null;
  fingerprintHash: string | null;
  certificateThumbprint: string | null;
  agentVersion: string | null;
  status: string;
  registeredAt: string;
  lastContactAt: string | null;
};

export type RegistrarDispositivoPayload = {
  tenantId: string;
  companyId: string;
  establishmentId: string;
  terminalId?: string;
  fingerprintHash?: string;
  certificateThumbprint?: string;
  agentVersion?: string;
};
```

- [ ] **Step 11: Escribir el test de dispositivos.api.ts**

`apps/erp-web/src/features/seguridad/api/dispositivos.api.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { cambiarEstadoDispositivo, fetchDispositivos, registrarDispositivo } from './dispositivos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleDispositivo = {
  id: 'device-1',
  companyId: 'company-1',
  establishmentId: 'establishment-1',
  terminalId: null,
  fingerprintHash: 'hash-abc',
  certificateThumbprint: null,
  agentVersion: '1.0.0',
  status: 'PENDIENTE',
  registeredAt: '2026-09-01T00:00:00Z',
  lastContactAt: null
};

describe('dispositivos.api', () => {
  it('fetchDispositivos consulta /dispositivos con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/dispositivos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleDispositivo]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchDispositivos(client, 'tenant-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual([sampleDispositivo]);
  });

  it('registrarDispositivo envia el payload y devuelve el dispositivo', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/dispositivos', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleDispositivo, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await registrarDispositivo(client, {
      tenantId: 'tenant-1',
      companyId: 'company-1',
      establishmentId: 'establishment-1',
      fingerprintHash: 'hash-abc',
      agentVersion: '1.0.0'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      companyId: 'company-1',
      establishmentId: 'establishment-1',
      fingerprintHash: 'hash-abc',
      agentVersion: '1.0.0'
    });
    expect(result).toEqual(sampleDispositivo);
  });

  it('cambiarEstadoDispositivo envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch('http://localhost/api/v1/dispositivos/device-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoDispositivo(client, 'device-1', 'tenant-1', 'CONFIABLE');

    expect(receivedBody).toEqual({ status: 'CONFIABLE' });
  });
});
```

- [ ] **Step 12: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/api/dispositivos.api.test.ts`
Expected: FAIL

- [ ] **Step 13: Implementar dispositivos.api.ts**

`apps/erp-web/src/features/seguridad/api/dispositivos.api.ts`:
```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { Dispositivo, RegistrarDispositivoPayload } from './dispositivos.types';

export function fetchDispositivos(client: ApiClient, tenantId: string): Promise<Dispositivo[]> {
  return client.get<Dispositivo[]>(`/dispositivos?tenantId=${encodeURIComponent(tenantId)}`);
}

export function dispositivosQuery(tenantId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'dispositivos', tenantId],
    queryFn: () => fetchDispositivos(apiClient, tenantId)
  });
}

export function registrarDispositivo(
  client: ApiClient,
  payload: RegistrarDispositivoPayload
): Promise<Dispositivo> {
  return client.post<Dispositivo, RegistrarDispositivoPayload>('/dispositivos', payload);
}

export function cambiarEstadoDispositivo(
  client: ApiClient,
  deviceId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/dispositivos/${deviceId}/estado?tenantId=${encodeURIComponent(tenantId)}`,
    { status }
  );
}
```

- [ ] **Step 14: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/api/dispositivos.api.test.ts`
Expected: PASS

- [ ] **Step 15: Definir el schema de dispositivo**

`apps/erp-web/src/features/seguridad/schemas/dispositivo.schema.ts`:
```ts
import { z } from 'zod';

export const dispositivoSchema = z.object({
  companyId: z.uuid('Selecciona una empresa válida.'),
  establishmentId: z.uuid('Selecciona un establecimiento válido.'),
  terminalId: z.string().optional(),
  fingerprintHash: z.string().max(300, 'La huella no debe exceder 300 caracteres.').optional(),
  certificateThumbprint: z.string().max(300, 'El certificado no debe exceder 300 caracteres.').optional(),
  agentVersion: z.string().max(100, 'La versión no debe exceder 100 caracteres.').optional()
});

export type DispositivoFormValues = z.infer<typeof dispositivoSchema>;
```

- [ ] **Step 16: Escribir el test de DispositivoForm**

`apps/erp-web/src/features/seguridad/components/DispositivoForm.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DispositivoForm } from './DispositivoForm';

const VALID_UUID = '11111111-1111-1111-1111-111111111111';

describe('DispositivoForm', () => {
  it('muestra errores cuando companyId y establishmentId son invalidos', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<DispositivoForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('ID de empresa'), 'no-es-uuid');
    await user.click(screen.getByRole('button', { name: 'Registrar dispositivo' }));

    expect(await screen.findByText('Selecciona una empresa válida.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<DispositivoForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('ID de empresa'), VALID_UUID);
    await user.type(screen.getByLabelText('ID de establecimiento'), VALID_UUID);
    await user.click(screen.getByRole('button', { name: 'Registrar dispositivo' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ companyId: VALID_UUID, establishmentId: VALID_UUID })
    );
  });
});
```

- [ ] **Step 17: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/components/DispositivoForm.test.tsx`
Expected: FAIL

- [ ] **Step 18: Implementar DispositivoForm**

`apps/erp-web/src/features/seguridad/components/DispositivoForm.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { dispositivoSchema, type DispositivoFormValues } from '../schemas/dispositivo.schema';
import { FormField } from './FormField';

export type DispositivoFormProps = {
  onSubmit: (values: DispositivoFormValues) => void;
  isSubmitting?: boolean;
};

export function DispositivoForm({ onSubmit, isSubmitting = false }: DispositivoFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<DispositivoFormValues>({
    defaultValues: {
      companyId: '',
      establishmentId: '',
      terminalId: '',
      fingerprintHash: '',
      certificateThumbprint: '',
      agentVersion: ''
    },
    mode: 'onTouched',
    resolver: zodResolver(dispositivoSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(onSubmit)(event);
      }}
    >
      <FormField label="ID de empresa" htmlFor="dispositivo-company-id" error={errors.companyId?.message}>
        <input
          id="dispositivo-company-id"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('companyId')}
        />
      </FormField>

      <FormField
        label="ID de establecimiento"
        htmlFor="dispositivo-establishment-id"
        error={errors.establishmentId?.message}
      >
        <input
          id="dispositivo-establishment-id"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('establishmentId')}
        />
      </FormField>

      <FormField label="ID de terminal (opcional)" htmlFor="dispositivo-terminal-id" error={errors.terminalId?.message}>
        <input
          id="dispositivo-terminal-id"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('terminalId')}
        />
      </FormField>

      <FormField label="Huella del dispositivo" htmlFor="dispositivo-fingerprint" error={errors.fingerprintHash?.message}>
        <input
          id="dispositivo-fingerprint"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('fingerprintHash')}
        />
      </FormField>

      <FormField label="Versión del agente" htmlFor="dispositivo-agent-version" error={errors.agentVersion?.message}>
        <input
          id="dispositivo-agent-version"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('agentVersion')}
        />
      </FormField>

      <Button type="submit" disabled={isSubmitting}>
        Registrar dispositivo
      </Button>
    </form>
  );
}
```

- [ ] **Step 19: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/components/DispositivoForm.test.tsx`
Expected: PASS

- [ ] **Step 20: Escribir el test de DevicesPage**

`apps/erp-web/src/features/seguridad/pages/DevicesPage.test.tsx`:
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { DevicesPage } from './DevicesPage';

const authenticatedSession = {
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

const VALID_UUID = '11111111-1111-1111-1111-111111111111';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <DevicesPage />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

const sampleDispositivo = {
  id: 'device-1',
  companyId: VALID_UUID,
  establishmentId: VALID_UUID,
  terminalId: null,
  fingerprintHash: 'hash-abc',
  certificateThumbprint: null,
  agentVersion: '1.0.0',
  status: 'PENDIENTE',
  registeredAt: '2026-09-01T00:00:00Z',
  lastContactAt: null
};

describe('DevicesPage', () => {
  it('lista los dispositivos del tenant activo', async () => {
    server.use(http.get('*/api/v1/dispositivos', () => HttpResponse.json([sampleDispositivo])));

    renderPage();

    expect(await screen.findByText('1.0.0')).toBeInTheDocument();
  });

  it('registra un dispositivo nuevo y refresca el listado', async () => {
    let registered = false;
    server.use(
      http.get('*/api/v1/dispositivos', () => HttpResponse.json(registered ? [sampleDispositivo] : [])),
      http.post('*/api/v1/dispositivos', async () => {
        registered = true;
        return HttpResponse.json(sampleDispositivo, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No hay dispositivos registrados.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Registrar dispositivo' }));
    await user.type(screen.getByLabelText('ID de empresa'), VALID_UUID);
    await user.type(screen.getByLabelText('ID de establecimiento'), VALID_UUID);
    await user.click(screen.getByRole('button', { name: 'Registrar dispositivo' }));

    await waitFor(() => expect(screen.getByText('1.0.0')).toBeInTheDocument());
  });

  it('cambia el estado de un dispositivo a confiable', async () => {
    let status = 'PENDIENTE';
    server.use(
      http.get('*/api/v1/dispositivos', () => HttpResponse.json([{ ...sampleDispositivo, status }])),
      http.patch('*/api/v1/dispositivos/device-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        status = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByText('1.0.0');
    await user.click(screen.getByRole('button', { name: 'Marcar como confiable' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar' }));

    await waitFor(() => expect(screen.getByText('CONFIABLE')).toBeInTheDocument());
  });
});
```

- [ ] **Step 21: Ejecutar y confirmar que falla**

Run: `pnpm exec vitest run src/features/seguridad/pages/DevicesPage.test.tsx`
Expected: FAIL

- [ ] **Step 22: Implementar DevicesPage**

`apps/erp-web/src/features/seguridad/pages/DevicesPage.tsx`:
```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { cambiarEstadoDispositivo, dispositivosQuery, registrarDispositivo } from '../api/dispositivos.api';
import type { Dispositivo } from '../api/dispositivos.types';
import type { DispositivoFormValues } from '../schemas/dispositivo.schema';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { DataTable } from '../components/DataTable';
import { DispositivoForm } from '../components/DispositivoForm';
import { EstadoBadge } from '../components/EstadoBadge';
import { Modal } from '../components/Modal';

export function DevicesPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [registerOpen, setRegisterOpen] = useState(false);
  const [deviceToTrust, setDeviceToTrust] = useState<Dispositivo | null>(null);

  const { data, isPending, isError } = useQuery({
    ...dispositivosQuery(tenantId ?? ''),
    enabled: Boolean(tenantId)
  });

  const registerMutation = useMutation({
    mutationFn: (values: DispositivoFormValues) =>
      registrarDispositivo(apiClient, {
        tenantId: tenantId ?? '',
        companyId: values.companyId,
        establishmentId: values.establishmentId,
        terminalId: values.terminalId || undefined,
        fingerprintHash: values.fingerprintHash || undefined,
        certificateThumbprint: values.certificateThumbprint || undefined,
        agentVersion: values.agentVersion || undefined
      }),
    onSuccess: () => {
      setRegisterOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'dispositivos'] });
    }
  });

  const trustMutation = useMutation({
    mutationFn: () => cambiarEstadoDispositivo(apiClient, deviceToTrust?.id ?? '', tenantId ?? '', 'CONFIABLE'),
    onSuccess: () => {
      setDeviceToTrust(null);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'dispositivos'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Dispositivos</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Dispositivos</h1>
          <p className="mt-2 text-sm text-slate-500">
            Dispositivos autorizados para operar en tienda.
          </p>
        </div>
        <Button onClick={() => setRegisterOpen(true)}>Registrar dispositivo</Button>
      </div>

      <div className="mt-6">
        <DataTable<Dispositivo>
          columns={[
            { header: 'Versión de agente', cell: (row) => row.agentVersion ?? '—' },
            { header: 'Huella', cell: (row) => row.fingerprintHash ?? '—' },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
            {
              header: '',
              cell: (row) =>
                row.status === 'PENDIENTE' ? (
                  <button
                    type="button"
                    onClick={() => setDeviceToTrust(row)}
                    className="text-xs font-semibold text-teal-700 hover:underline"
                  >
                    Marcar como confiable
                  </button>
                ) : null
            }
          ]}
          rows={data ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No hay dispositivos registrados."
          isLoading={isPending}
          isError={isError}
        />
      </div>

      <Modal open={registerOpen} onClose={() => setRegisterOpen(false)} title="Registrar dispositivo">
        <DispositivoForm
          onSubmit={(values) => registerMutation.mutate(values)}
          isSubmitting={registerMutation.isPending}
        />
      </Modal>

      <ConfirmActionDialog
        open={deviceToTrust !== null}
        title="Marcar dispositivo como confiable"
        description="El dispositivo podrá usarse para operaciones en tienda sin restricciones adicionales."
        confirmLabel="Confirmar"
        isPending={trustMutation.isPending}
        onConfirm={() => trustMutation.mutate()}
        onCancel={() => setDeviceToTrust(null)}
      />
    </div>
  );
}
```

- [ ] **Step 23: Ejecutar y confirmar que pasa**

Run: `pnpm exec vitest run src/features/seguridad/pages/DevicesPage.test.tsx`
Expected: PASS

- [ ] **Step 24: Añadir las rutas de sesiones y dispositivos**

Agregar a `apps/erp-web/src/features/seguridad/routes.tsx` (después de la entrada `seguridad/usuarios/:userId`):
```tsx
  {
    path: 'seguridad/sesiones',
    lazy: async () => {
      const { SessionsPage } = await import('./pages/SessionsPage');
      return { Component: SessionsPage };
    }
  },
  {
    path: 'seguridad/dispositivos',
    lazy: async () => {
      const { DevicesPage } = await import('./pages/DevicesPage');
      return { Component: DevicesPage };
    }
  },
```

- [ ] **Step 25: Reemplazar SecurityPage por un panel de navegación a las 5 pantallas**

Reemplazar `apps/erp-web/src/features/seguridad/pages/SecurityPage.tsx` (ya no debe usar `ModulePlaceholderPage`):
```tsx
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

const SECTIONS = [
  { to: '/seguridad/usuarios', title: 'Usuarios', description: 'Cuentas de acceso al sistema.' },
  { to: '/seguridad/roles', title: 'Roles', description: 'Roles y permisos asociados.' },
  { to: '/seguridad/permisos', title: 'Permisos', description: 'Catálogo de permisos del sistema.' },
  { to: '/seguridad/sesiones', title: 'Sesiones', description: 'Sesiones activas e históricas.' },
  { to: '/seguridad/dispositivos', title: 'Dispositivos', description: 'Dispositivos autorizados en tienda.' }
];

export function SecurityPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <div>
        <p className="text-sm font-semibold text-teal-700">Foundation / Core maestro</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Seguridad</h1>
        <p className="mt-2 text-sm text-slate-500">Usuarios, roles, permisos y control de accesos.</p>
      </div>

      <div className="mt-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {SECTIONS.map((section) => (
          <Link key={section.to} to={section.to}>
            <Card className="h-full p-5 transition hover:border-teal-300 hover:shadow-md">
              <h2 className="font-bold text-slate-950">{section.title}</h2>
              <p className="mt-1 text-sm text-slate-500">{section.description}</p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 26: Ejecutar toda la suite de seguridad**

Run: `pnpm exec vitest run src/features/seguridad`
Expected: PASS

- [ ] **Step 27: Ejecutar toda la suite de erp-web para confirmar que nada se rompió fuera de seguridad**

Run: `pnpm exec vitest run`
Expected: PASS

- [ ] **Step 28: Commit**

```bash
git add apps/erp-web/src/features/seguridad
git commit -m "feat(seguridad): pantallas de sesiones y dispositivos, panel de navegacion del modulo"
```

---

## Task 8: Infraestructura Playwright (desktop/tablet/mobile) y specs E2E de seguridad

**Files:**
- Create: `frontend/playwright.config.ts`
- Create: `frontend/e2e/fixtures/seed.ts`
- Create: `frontend/e2e/fixtures/test-tenant.ts`
- Create: `frontend/e2e/seguridad/login.spec.ts`
- Create: `frontend/e2e/seguridad/usuarios-roles.spec.ts`
- Create: `frontend/e2e/seguridad/sesiones.spec.ts`
- Create: `frontend/e2e/README.md`
- Modify: `frontend/package.json`
- Modify: `frontend/.gitignore` (si existe; si no, crear la entrada en el `.gitignore` raíz del repo)

**Interfaces:**
- Consumes: backend real en `http://localhost:8080` (`service-botica`, Docker Compose ya configurado en sesiones previas de este mismo proyecto), frontend real en `http://localhost:5173` vía `webServer` de Playwright.
- Produces: `seedTenantWithAdminUser(): Promise<{ tenantId: string; adminEmail: string; adminPassword: string }>` — sembrado idempotente vía HTTP contra el backend real, usado por los 3 specs.

- [ ] **Step 1: Instalar Playwright**

Run: `cd frontend && pnpm add -D -w @playwright/test`
Expected: agrega `@playwright/test` a `devDependencies` del `package.json` raíz de `frontend/`.

Run: `pnpm exec playwright install --with-deps chromium webkit`
Expected: descarga los navegadores necesarios (Chromium para desktop/tablet, WebKit para mobile).

- [ ] **Step 2: Crear la configuración de Playwright con 3 proyectos de viewport**

`frontend/playwright.config.ts`:
```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry'
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } }
    },
    {
      name: 'tablet',
      use: { ...devices['iPad (gen 7)'], viewport: { width: 768, height: 1024 } }
    },
    {
      name: 'mobile',
      use: { ...devices['iPhone 13'], viewport: { width: 390, height: 844 } }
    }
  ],
  webServer: {
    command: 'pnpm --filter @boticas/erp-web dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
    env: {
      VITE_API_MODE: 'http'
    }
  }
});
```

Nota crítica: `env: { VITE_API_MODE: 'http' }` fuerza que el servidor de desarrollo levantado por Playwright NO use MSW (que por defecto se activa en `.env.development`) — los E2E deben golpear el backend real a través del proxy Vite (`/api` → `:8080`), nunca los mocks.

- [ ] **Step 3: Crear el helper de sembrado de datos de prueba**

`frontend/e2e/fixtures/test-tenant.ts`:
```ts
export type TestTenantSeed = {
  tenantId: string;
  tenantUuid: string;
  adminEmail: string;
  adminPassword: string;
  userId: string;
};

export const BACKEND_BASE_URL = process.env.E2E_BACKEND_URL ?? 'http://localhost:8080';
```

`frontend/e2e/fixtures/seed.ts`:
```ts
import { randomUUID } from 'node:crypto';
import { BACKEND_BASE_URL, type TestTenantSeed } from './test-tenant';

async function post<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${BACKEND_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!response.ok && response.status !== 204) {
    throw new Error(`Seed request failed: POST ${path} -> ${response.status} ${await response.text()}`);
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

// Sembrado vía los mismos endpoints que usa la aplicación (no SQL directo), para
// ejercitar el contrato real de creación de tenant/usuario/credencial que los E2E consumen.
// Requiere que exista un endpoint de creación de tenant accesible sin autenticación previa
// para pruebas, O que se use un tenant fijo ya sembrado por las migraciones de desarrollo.
//
// Nota de implementación: `service-botica` no expone hoy un endpoint público de creación de
// tenant (los tenants se administran fuera del alcance del módulo `security`). Por lo tanto
// este seed asume un tenant de desarrollo YA EXISTENTE (creado manualmente una vez, o por una
// migración Flyway de datos de desarrollo) y solo crea el usuario administrador de prueba sobre
// ese tenant. El id de ese tenant de desarrollo se configura vía la variable de entorno
// E2E_TENANT_ID (ver frontend/e2e/README.md para cómo obtenerlo).
export async function seedTenantWithAdminUser(): Promise<TestTenantSeed> {
  const tenantUuid = process.env.E2E_TENANT_ID;
  if (!tenantUuid) {
    throw new Error(
      'E2E_TENANT_ID no está definido. Ver frontend/e2e/README.md para sembrar un tenant de desarrollo.'
    );
  }

  const unique = randomUUID().slice(0, 8);
  const adminEmail = `e2e-admin-${unique}@boticas.pe`;
  const adminPassword = 'PlaywrightE2E!2026';

  const usuario = await post<{ id: string }>('/api/v1/usuarios', {
    tenantId: tenantUuid,
    email: adminEmail,
    displayName: `E2E Admin ${unique}`
  });

  await post<void>(`/usuarios/${usuario.id}/credencial-local`, {
    tenantId: tenantUuid,
    password: adminPassword,
    requireChange: false
  });

  return { tenantId: tenantUuid, tenantUuid, adminEmail, adminPassword, userId: usuario.id };
}
```

- [ ] **Step 4: Crear el README de e2e con instrucciones de setup**

`frontend/e2e/README.md`:
```markdown
# Tests E2E (Playwright)

Estos tests corren contra el **backend real** de `service-botica`, no contra mocks.

## Prerrequisitos

1. Levantar Postgres y el backend:
   ```bash
   cd ../service-botica
   docker compose up -d postgres
   ./gradlew.bat :bootstrap-app:bootRun
   ```
2. Confirmar que responde: `curl http://localhost:8080/actuator/health` debe devolver `{"status":"UP"}`.
3. Sembrar (una sola vez) un tenant de desarrollo y exportar su UUID:
   ```bash
   export E2E_TENANT_ID=<uuid-del-tenant-de-desarrollo>
   ```
   Si no existe un tenant de desarrollo todavía, créalo insertando directamente en `sch_admin.tenant`
   vía `psql` (ver `service-botica/bootstrap-app/src/test/java/.../IamApiIntegrationTest.java` para
   el INSERT de referencia con `slug` obligatorio) y usa el `uuid_publico` generado.

## Ejecutar

```bash
pnpm test:e2e            # los 3 proyectos (desktop, tablet, mobile)
pnpm test:e2e -- --project=desktop   # solo un viewport
pnpm test:e2e -- --ui                 # modo interactivo
```

## Qué cubren los specs de `seguridad/`

- `login.spec.ts`: login real con credenciales sembradas, logout.
- `usuarios-roles.spec.ts`: crear un usuario, asignarle un rol con ámbito ESTABLECIMIENTO, verificar que aparece en el listado, cambiar su estado.
- `sesiones.spec.ts`: iniciar sesión, verificar que aparece en el listado de sesiones propio, revocarla.
```

- [ ] **Step 5: Escribir el spec de login**

`frontend/e2e/seguridad/login.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { seedTenantWithAdminUser } from '../fixtures/seed';

test.describe('Login', () => {
  test('permite iniciar sesión con credenciales válidas y cerrar sesión', async ({ page }) => {
    const seed = await seedTenantWithAdminUser();

    await page.goto('/login');
    await page.getByLabel('Correo corporativo').fill(seed.adminEmail);
    await page.getByLabel('Contraseña').fill(seed.adminPassword);
    await page.getByRole('button', { name: 'Iniciar Sesión' }).click();

    await expect(page).toHaveURL(/\/dashboard/);

    await page.getByRole('button', { name: /cerrar sesión/i }).click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('muestra un error con credenciales inválidas', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Correo corporativo').fill('no-existe@boticas.pe');
    await page.getByLabel('Contraseña').fill('ContrasenaIncorrecta1!');
    await page.getByRole('button', { name: 'Iniciar Sesión' }).click();

    await expect(page.getByText('Credenciales incorrectas o cuenta bloqueada.')).toBeVisible();
  });
});
```

Nota: el botón "cerrar sesión" se asume existente en `AppShell` (layout autenticado) con ese texto accesible — si al ejecutar este spec el selector no encuentra el botón, ajustar el `name` del `getByRole` al texto real presente en `apps/erp-web/src/shared/layout/AppShell.tsx` (revisar ese archivo si este paso falla; no está cubierto por las tareas anteriores de este plan porque `AppShell` ya existe y no se modifica aquí).

- [ ] **Step 6: Escribir el spec de usuarios y roles**

`frontend/e2e/seguridad/usuarios-roles.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { seedTenantWithAdminUser } from '../fixtures/seed';

test.describe('Usuarios y roles', () => {
  test('crea un usuario, le asigna un rol y cambia su estado', async ({ page }) => {
    const seed = await seedTenantWithAdminUser();

    await page.goto('/login');
    await page.getByLabel('Correo corporativo').fill(seed.adminEmail);
    await page.getByLabel('Contraseña').fill(seed.adminPassword);
    await page.getByRole('button', { name: 'Iniciar Sesión' }).click();
    await expect(page).toHaveURL(/\/dashboard/);

    await page.goto('/seguridad/usuarios');
    await page.getByRole('button', { name: 'Nuevo usuario' }).click();

    const displayName = `Usuario E2E ${Date.now()}`;
    await page.getByLabel('Nombre visible').fill(displayName);
    await page.getByLabel('Correo').fill(`e2e-${Date.now()}@boticas.pe`);
    await page.getByRole('button', { name: 'Crear usuario' }).click();

    await expect(page.getByRole('link', { name: displayName })).toBeVisible();

    await page.getByRole('link', { name: displayName }).click();
    await page.getByRole('button', { name: 'Asignar rol' }).click();

    const roleSelect = page.getByLabel('Rol');
    await expect(roleSelect).toBeVisible();
    const firstRoleValue = await roleSelect.locator('option').nth(1).getAttribute('value');
    if (firstRoleValue) {
      await roleSelect.selectOption(firstRoleValue);
    }
    await page.getByLabel('Ámbito').selectOption('ESTABLECIMIENTO');
    await page.getByRole('button', { name: 'Asignar' }).click();

    await page.getByRole('button', { name: 'Desactivar usuario' }).click();
    await page.getByRole('button', { name: 'Desactivar' }).click();
    await expect(page.getByText('INACTIVO')).toBeVisible();
  });
});
```

- [ ] **Step 7: Escribir el spec de sesiones**

`frontend/e2e/seguridad/sesiones.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { seedTenantWithAdminUser } from '../fixtures/seed';

test.describe('Sesiones', () => {
  test('la sesion propia aparece en el listado y puede revocarse', async ({ page }) => {
    const seed = await seedTenantWithAdminUser();

    await page.goto('/login');
    await page.getByLabel('Correo corporativo').fill(seed.adminEmail);
    await page.getByLabel('Contraseña').fill(seed.adminPassword);
    await page.getByRole('button', { name: 'Iniciar Sesión' }).click();
    await expect(page).toHaveURL(/\/dashboard/);

    await page.goto('/seguridad/sesiones');
    await expect(page.getByText('WEB')).toBeVisible();

    await page.getByRole('button', { name: 'Revocar' }).first().click();
    await page.getByRole('button', { name: 'Confirmar revocación' }).click();

    await expect(page.getByText('REVOCADA')).toBeVisible();
  });
});
```

- [ ] **Step 8: Agregar el script de e2e al package.json raíz**

En `frontend/package.json`, dentro de `"scripts"`, agregar (después de `"test:watch": "vitest",`):
```json
    "test:e2e": "playwright test",
```

- [ ] **Step 9: Ignorar los artefactos de Playwright en git**

Verificar si existe `frontend/.gitignore`; si existe, agregar estas líneas al final (si no existe el archivo, crear `frontend/.gitignore` con este contenido):
```
/test-results/
/playwright-report/
/blob-report/
/playwright/.cache/
```

- [ ] **Step 10: Verificar que el backend está corriendo y sembrar el tenant de desarrollo**

Run: `curl http://localhost:8080/actuator/health`
Expected: `{"status":"UP",...}`. Si no responde, levantar el stack primero:
```bash
cd service-botica
docker compose up -d postgres
./gradlew.bat :bootstrap-app:bootRun
```
(esperar a que el log muestre `Started ServiceBoticaApplication`).

Sembrar el tenant de desarrollo una sola vez (si `E2E_TENANT_ID` no está ya definido de una sesión anterior) insertando vía `psql` contra el contenedor de Postgres, replicando el INSERT usado en `IamApiIntegrationTest.java`:
```bash
docker exec -it $(docker compose -f ../service-botica/docker-compose.yml ps -q postgres 2>/dev/null || echo erp-botica-postgres-1) \
  psql -U postgres -d erp_botica -c "INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by) VALUES (gen_random_uuid(), 'E2E', 'Tenant E2E', 'tenant-e2e', 'e2e') RETURNING uuid_publico;"
```
Copiar el `uuid_publico` devuelto y exportarlo: `export E2E_TENANT_ID=<uuid-devuelto>`.

- [ ] **Step 11: Ejecutar los specs de seguridad en el proyecto desktop**

Run: `cd frontend && pnpm test:e2e -- --project=desktop e2e/seguridad`
Expected: PASS — los 4 tests (login × 2, usuarios-roles × 1, sesiones × 1) pasan contra el backend y frontend reales.

Si algún test falla por un selector no encontrado (por ejemplo el botón "cerrar sesión" del Step 5), inspeccionar el HTML real renderizado (`pnpm test:e2e -- --project=desktop e2e/seguridad --debug`) y ajustar el selector en el spec correspondiente para que coincida con el texto/rol accesible real — no cambiar el código de la aplicación para acomodar el test.

- [ ] **Step 12: Ejecutar los mismos specs en tablet y mobile**

Run: `pnpm test:e2e -- --project=tablet e2e/seguridad`
Run: `pnpm test:e2e -- --project=mobile e2e/seguridad`
Expected: PASS en ambos. Si algún elemento no es visible/clickeable en mobile (por ejemplo por un menú colapsado en `AppShell` que no se abre automáticamente), documentar el hallazgo — no es responsabilidad de esta tarea rediseñar la navegación responsive de `AppShell`, que está fuera de alcance de este plan.

- [ ] **Step 13: Commit**

```bash
git add frontend/playwright.config.ts frontend/e2e frontend/package.json frontend/pnpm-lock.yaml frontend/.gitignore
git commit -m "test(e2e): infraestructura Playwright (desktop/tablet/mobile) y specs de seguridad"
```

---

## Task 9: Verificación final del módulo completo

**Files:** Ninguno nuevo — solo verificación.

- [ ] **Step 1: Ejecutar el pipeline completo de calidad del frontend**

Run: `cd frontend && pnpm check`
Expected: PASS — `lint && typecheck && test && build` todos en verde. Esto ejercita las 9 tareas anteriores juntas: si algún tipo quedó inconsistente entre archivos (por ejemplo `PaginaResponse<T>` importado desde dos lugares distintos), `typecheck` lo revela aquí.

- [ ] **Step 2: Si `pnpm check` falla, diagnosticar por capa**

Si falla en `lint`: `pnpm lint` sin el resto, corregir los errores reportados (típicamente imports no usados o reglas de `eslint-plugin-react-hooks`).
Si falla en `typecheck`: `pnpm typecheck`, revisar el archivo y línea exactos del error — la causa más probable es un tipo definido en una tarea (ej. `Rol` en Task 4) usado con un campo distinto en otra (ej. `RoleDetailPage` de Task 4 usando `rol.status` que sí existe, pero verificar cualquier renombre accidental).
Si falla en `test`: `pnpm test`, aislar el archivo de test que falla con `pnpm exec vitest run <ruta>`.
Si falla en `build`: `pnpm build`, revisar el error de Vite/Rollup (típicamente un import faltante o circular).

- [ ] **Step 3: Ejecutar la suite completa de Playwright una vez más tras cualquier fix**

Run: `pnpm test:e2e`
Expected: PASS en los 3 proyectos.

- [ ] **Step 4: Confirmar el estado final con git**

Run: `git status`
Expected: working tree limpio (todo commiteado en las tareas anteriores) salvo archivos explícitamente fuera de alcance de este plan (otras features del monorepo).

No se requiere un commit adicional en esta tarea — es solo verificación de que las Tasks 1-8 dejaron el repositorio en un estado consistente y verificado de punta a punta.
