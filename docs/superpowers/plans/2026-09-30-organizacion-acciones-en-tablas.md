# Organización: columna «Acciones» en empresas y establecimientos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que en las tablas de empresas y de establecimientos el nombre sea texto plano y exista una columna «Acciones» con un ojo («Ver detalle», navega) y un lápiz («Editar», abre un modal sin salir de la página), igual que en el resto de la app.

**Architecture:** El modal de edición, hoy incrustado en las páginas de detalle, se extrae a dos diálogos autónomos (`EmpresaEditarDialog`, `EstablecimientoEditarDialog`) que cargan su propia mutación y se montan solo cuando están abiertos; los usan la lista y el detalle. Un componente `AccionesFila` encapsula el par ojo + lápiz para no duplicarlo entre las dos tablas.

**Tech Stack:** React 19, TanStack Query, react-hook-form + zod 4, `@boticas/ui-web` (`IconButton`, `iconButtonClassName`), lucide-react, Vitest + Testing Library + MSW, Playwright (desktop/tablet/móvil), pnpm.

## Global Constraints

Tomadas del spec `docs/superpowers/specs/2026-09-30-organizacion-acciones-en-tablas-design.md`:

- En empresas (razón social) y en establecimientos (nombre) el nombre deja de ser enlace y pasa a texto plano.
- Columna «Acciones» con dos iconos del tamaño predeterminado (el mismo que Seguridad y Catálogo): ojo `Ver detalle de {nombre}` (un `Link` al detalle con `aria-label` y `title`) y lápiz `Editar {nombre}` (abre el modal de edición sobre la tabla).
- Los diálogos de edición los comparten la lista y el detalle: no debe quedar lógica de edición duplicada.
- «Cambiar estado» sigue solo en el detalle. El árbol del resumen de `/organizacion` conserva sus enlaces. No se toca el backend.
- Todo archivo **nuevo** debe alcanzar 100% de cobertura de líneas y ramas (gate por archivo de Vitest) y los modificados que el plan lista también; no relajar umbrales ni tocar `coverage-baseline.txt`.
- Sin comentarios explicativos en el código; sin código duplicado; estructura por features (nada fuera de `features/organizacion/`, `test/` y `e2e/`).
- No reintroducir `forwardRef` ni patrones de React Router 8 bloqueados por ESLint.
- Mensajes en español.

## Convenciones de este plan

Rutas relativas a `frontend/`. `FE` = `apps/erp-web/src/features/organizacion`. Comandos en PowerShell desde `frontend`, con `$env:CI='true'` para que pnpm no pida confirmación sin terminal (excepto Playwright, que se ejecuta sin `CI`). Plantilla de ejecución con cobertura de archivos concretos (sustituir `<tests>` y `<archivos>`):

```powershell
$env:CI='true'; pnpm.cmd exec vitest run <tests> --project erp-web --coverage --coverage.include="<archivos>" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100
```

Tras crear o editar archivos, formatear con `pnpm.cmd exec prettier --write <archivos>` y comprobar con `pnpm.cmd exec eslint <archivos>`. Si una corrida con cobertura de toda la feature agota los 5 s de Vitest en tests ajenos por la carga de la máquina, repetir con `--testTimeout=60000` o los archivos aislados; no modificar esos tests.

## Mapa de archivos

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `FE/components/AccionesFila.tsx` (+test) | Crear | Ojo («Ver detalle») + lápiz («Editar») de una fila |
| `FE/components/EmpresaEditarDialog.tsx` (+test) | Crear | Modal de edición de empresa con su mutación |
| `FE/components/EstablecimientoEditarDialog.tsx` (+test) | Crear | Modal de edición de establecimiento con su mutación |
| `FE/pages/EmpresaDetailPage.tsx` | Modificar | Usar `EmpresaEditarDialog` |
| `FE/pages/EstablecimientoDetailPage.tsx` | Modificar | Usar `EstablecimientoEditarDialog` |
| `FE/pages/EmpresasPage.tsx` (+test) | Modificar | Nombre plano + columna «Acciones» + edición desde la lista |
| `FE/components/EstablecimientosSection.tsx` (+test) | Modificar | Nombre plano + columna «Acciones» + edición desde la lista |
| `FE/pages/EmpresaDetailPage.test.tsx` | Modificar | El establecimiento deja de ser un enlace |
| `e2e/organizacion.spec.ts` | Modificar | Ojo y lápiz en tres viewports |

---

### Task 1: Componente `AccionesFila`

**Files:**
- Create: `FE/components/AccionesFila.tsx`, `FE/components/AccionesFila.test.tsx`

**Interfaces:**
- Consumes: `IconButton` e `iconButtonClassName` de `@boticas/ui-web`; `renderRoute` de `apps/erp-web/src/test/render-route.tsx`.
- Produces: `AccionesFila({ nombre, detalleHref, onEditar })` con `nombre: string`, `detalleHref: string`, `onEditar: () => void`. Renderiza un enlace accesible `Ver detalle de {nombre}` y un botón `Editar {nombre}`.

- [ ] **Step 1: Escribir el test que falla**

`FE/components/AccionesFila.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { renderRoute } from '../../../test/render-route';
import { AccionesFila } from './AccionesFila';

function renderAcciones(onEditar = vi.fn()) {
  const result = renderRoute(
    '/lista',
    () => (
      <AccionesFila
        nombre="Boticas SAC"
        detalleHref="/organizacion/empresas/empresa-1"
        onEditar={onEditar}
      />
    ),
    '/lista'
  );
  return { onEditar, ...result };
}

describe('AccionesFila', () => {
  it('muestra un enlace accesible al detalle', () => {
    renderAcciones();

    const enlace = screen.getByRole('link', { name: 'Ver detalle de Boticas SAC' });
    expect(enlace).toHaveAttribute('href', '/organizacion/empresas/empresa-1');
    expect(enlace).toHaveAttribute('title', 'Ver detalle de Boticas SAC');
  });

  it('muestra un botón de editar que llama a onEditar sin navegar', async () => {
    const { onEditar, user, router } = renderAcciones();

    await user.click(screen.getByRole('button', { name: 'Editar Boticas SAC' }));

    expect(onEditar).toHaveBeenCalledOnce();
    expect(router.state.location.pathname).toBe('/lista');
  });
});
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/AccionesFila.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./AccionesFila"`).

- [ ] **Step 2: Implementar**

`FE/components/AccionesFila.tsx`:

```tsx
import { Eye, Pencil } from 'lucide-react';
import { Link } from 'react-router';
import { IconButton, iconButtonClassName } from '@boticas/ui-web';

type AccionesFilaProps = {
  nombre: string;
  detalleHref: string;
  onEditar: () => void;
};

export function AccionesFila({ nombre, detalleHref, onEditar }: AccionesFilaProps) {
  return (
    <div className="flex items-center gap-1">
      <Link
        to={detalleHref}
        aria-label={`Ver detalle de ${nombre}`}
        title={`Ver detalle de ${nombre}`}
        className={iconButtonClassName()}
      >
        <Eye className="size-4.5" aria-hidden="true" />
      </Link>
      <IconButton icon={Pencil} label={`Editar ${nombre}`} onClick={onEditar} />
    </div>
  );
}
```

- [ ] **Step 3: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion/components/AccionesFila.test.tsx`, `<archivos>` = `apps/erp-web/src/features/organizacion/components/AccionesFila.tsx`.
Expected: 2 tests PASS y 100% en `AccionesFila.tsx`.

- [ ] **Step 4: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): agregar acciones de fila con ojo y lapiz

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: `EmpresaEditarDialog` y su uso en el detalle de empresa

**Files:**
- Create: `FE/components/EmpresaEditarDialog.tsx`, `FE/components/EmpresaEditarDialog.test.tsx`
- Modify: `FE/pages/EmpresaDetailPage.tsx`

**Interfaces:**
- Consumes: `actualizarEmpresa(client, empresaId, tenantId, payload)` de `FE/api/empresas.api`, `toActualizarEmpresaPayload`, `toEmpresaFormValues`, `EmpresaForm` (con `isEdit`), `empresaEdicionSchema` ya usado por el formulario, `invalidateOrganizacion`, `describeApiError`, `useTenantId`, `Empresa`; `sampleEmpresa` y `renderRoute` para los tests.
- Produces: `EmpresaEditarDialog({ empresa, onClose })` con `empresa: Empresa` y `onClose: () => void`. Se monta solo cuando está abierto; al guardar con éxito invalida los datos de Organización y llama a `onClose`; al cerrar (botón «Cerrar») llama a `onClose` y, al desmontarse, descarta el error del servidor.

- [ ] **Step 1: Escribir el test que falla**

`FE/components/EmpresaEditarDialog.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEmpresa } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EmpresaEditarDialog } from './EmpresaEditarDialog';

const itemUrl = '*/api/v1/organizacion/empresas/empresa-1';

function renderDialog(empresa = sampleEmpresa) {
  const onClose = vi.fn();
  const result = renderRoute(
    '/lista',
    () => <EmpresaEditarDialog empresa={empresa} onClose={onClose} />,
    '/lista'
  );
  return { onClose, ...result };
}

describe('EmpresaEditarDialog', () => {
  it('precarga los datos y deja el RUC de solo lectura', () => {
    renderDialog();

    expect(screen.getByRole('heading', { name: 'Editar empresa' })).toBeInTheDocument();
    expect(screen.getByLabelText('RUC')).toHaveValue('20123456786');
    expect(screen.getByLabelText('RUC')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Razón social')).toHaveValue('Boticas SAC');
  });

  it('guarda sin enviar el RUC, con el tenant en la consulta, y se cierra', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEmpresa, razonSocial: 'Boticas del Perú SAC' });
      })
    );
    const { onClose, user } = renderDialog();

    const razonSocial = screen.getByLabelText('Razón social');
    await user.clear(razonSocial);
    await user.type(razonSocial, 'Boticas del Perú SAC');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ razonSocial: 'Boticas del Perú SAC', monedaFuncional: 'PEN' });
    expect('ruc' in body).toBe(false);
  });

  it('permite guardar una empresa cuyo RUC guardado no cumple el dígito verificador', async () => {
    server.use(http.put(itemUrl, () => HttpResponse.json(sampleEmpresa)));
    const { onClose, user } = renderDialog({ ...sampleEmpresa, ruc: '20123456789' });

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(screen.queryByText('El RUC no es válido: el dígito verificador no coincide.')).toBeNull();
  });

  it('muestra el error del servidor y no se cierra', async () => {
    server.use(
      http.put(itemUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'La razón social no es válida.' },
          { status: 400 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('La razón social no es válida.');
    expect(onClose).not.toHaveBeenCalled();
  });

  it('llama a onClose desde el botón cerrar', async () => {
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledOnce();
  });
});
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EmpresaEditarDialog.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./EmpresaEditarDialog"`).

- [ ] **Step 2: Implementar el diálogo**

`FE/components/EmpresaEditarDialog.tsx`:

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarEmpresa } from '../api/empresas.api';
import type { Empresa } from '../api/empresas.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { describeApiError } from '../lib/describe-api-error';
import { toEmpresaFormValues } from '../lib/form-defaults';
import { toActualizarEmpresaPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { EmpresaFormValues } from '../schemas/empresa.schema';
import { EmpresaForm } from './EmpresaForm';

type EmpresaEditarDialogProps = {
  empresa: Empresa;
  onClose: () => void;
};

export function EmpresaEditarDialog({ empresa, onClose }: EmpresaEditarDialogProps) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (values: EmpresaFormValues) =>
      actualizarEmpresa(apiClient, empresa.id, tenantId, toActualizarEmpresaPayload(values)),
    onSuccess: () => {
      onClose();
      void invalidateOrganizacion(queryClient);
    }
  });

  return (
    <Modal open onClose={onClose} title="Editar empresa" size="lg">
      <EmpresaForm
        isEdit
        defaultValues={toEmpresaFormValues(empresa)}
        submitLabel="Guardar cambios"
        isSubmitting={mutation.isPending}
        error={mutation.isError ? describeApiError(mutation.error) : null}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </Modal>
  );
}
```

- [ ] **Step 3: Usarlo en `EmpresaDetailPage`**

En `FE/pages/EmpresaDetailPage.tsx`:

1. Cambiar el import `import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';` por `import { Button, Card, EstadoBadge, PageHeader } from '@boticas/ui-web';`.
2. Cambiar `import { actualizarEmpresa, cambiarEstadoEmpresa, empresaQuery } from '../api/empresas.api';` por `import { cambiarEstadoEmpresa, empresaQuery } from '../api/empresas.api';`.
3. Eliminar los imports de `EmpresaForm`, `toEmpresaFormValues`, `toActualizarEmpresaPayload` y `type EmpresaFormValues`; añadir `import { EmpresaEditarDialog } from '../components/EmpresaEditarDialog';` (en orden alfabético, antes de `EstablecimientosSection`).
4. Eliminar el bloque completo `const updateMutation = useMutation({ ... });` y la función `closeEdit`.
5. Reemplazar el bloque:

```tsx
      <Modal open={editOpen} onClose={closeEdit} title="Editar empresa" size="lg">
        <EmpresaForm
          isEdit
          defaultValues={toEmpresaFormValues(empresa)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
          onSubmit={(values) => updateMutation.mutate(values)}
        />
      </Modal>
```

por:

```tsx
      {editOpen ? (
        <EmpresaEditarDialog empresa={empresa} onClose={() => setEditOpen(false)} />
      ) : null}
```

- [ ] **Step 4: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion/components/EmpresaEditarDialog.test.tsx apps/erp-web/src/features/organizacion/pages/EmpresaDetailPage.test.tsx`, `<archivos>` = `apps/erp-web/src/features/organizacion/{components/EmpresaEditarDialog.tsx,pages/EmpresaDetailPage.tsx}`.
Expected: todos PASS (los tests de edición que ya tiene `EmpresaDetailPage.test.tsx` pasan sin cambios) y 100% en los dos archivos.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps
git commit -m "refactor(organizacion): extraer el dialogo de edicion de empresa

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `EstablecimientoEditarDialog` y su uso en el detalle de establecimiento

**Files:**
- Create: `FE/components/EstablecimientoEditarDialog.tsx`, `FE/components/EstablecimientoEditarDialog.test.tsx`
- Modify: `FE/pages/EstablecimientoDetailPage.tsx`

**Interfaces:**
- Consumes: `actualizarEstablecimiento(client, establecimientoId, tenantId, payload)` de `FE/api/establecimientos.api`, `toActualizarEstablecimientoPayload`, `toEstablecimientoFormValues`, `EstablecimientoForm` (con `isEdit`), `invalidateOrganizacion`, `describeApiError`, `useTenantId`, `Establecimiento`; `sampleEstablecimiento` y `renderRoute` para los tests.
- Produces: `EstablecimientoEditarDialog({ establecimiento, onClose })` con `establecimiento: Establecimiento` y `onClose: () => void`, con el mismo contrato que `EmpresaEditarDialog`.

- [ ] **Step 1: Escribir el test que falla**

`FE/components/EstablecimientoEditarDialog.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstablecimiento } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EstablecimientoEditarDialog } from './EstablecimientoEditarDialog';

const itemUrl = '*/api/v1/organizacion/establecimientos/est-1';

function renderDialog() {
  const onClose = vi.fn();
  const result = renderRoute(
    '/lista',
    () => <EstablecimientoEditarDialog establecimiento={sampleEstablecimiento} onClose={onClose} />,
    '/lista'
  );
  return { onClose, ...result };
}

describe('EstablecimientoEditarDialog', () => {
  it('precarga los datos y deja el código de solo lectura', () => {
    renderDialog();

    expect(screen.getByRole('heading', { name: 'Editar establecimiento' })).toBeInTheDocument();
    expect(screen.getByLabelText('Código', { exact: true })).toHaveValue('EST001');
    expect(screen.getByLabelText('Código', { exact: true })).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Nombre', { exact: true })).toHaveValue('Botica Central');
  });

  it('guarda sin enviar el código, con el tenant en la consulta, y se cierra', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEstablecimiento, nombre: 'Botica Principal' });
      })
    );
    const { onClose, user } = renderDialog();

    const nombre = screen.getByLabelText('Nombre', { exact: true });
    await user.clear(nombre);
    await user.type(nombre, 'Botica Principal');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ nombre: 'Botica Principal', perfilOperacion: 'ONLINE' });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor y no se cierra', async () => {
    server.use(
      http.put(itemUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'El nombre no es válido.' },
          { status: 400 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El nombre no es válido.');
    expect(onClose).not.toHaveBeenCalled();
  });

  it('llama a onClose desde el botón cerrar', async () => {
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledOnce();
  });
});
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EstablecimientoEditarDialog.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./EstablecimientoEditarDialog"`).

- [ ] **Step 2: Implementar el diálogo**

`FE/components/EstablecimientoEditarDialog.tsx`:

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarEstablecimiento } from '../api/establecimientos.api';
import type { Establecimiento } from '../api/establecimientos.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { describeApiError } from '../lib/describe-api-error';
import { toEstablecimientoFormValues } from '../lib/form-defaults';
import { toActualizarEstablecimientoPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import { EstablecimientoForm } from './EstablecimientoForm';

type EstablecimientoEditarDialogProps = {
  establecimiento: Establecimiento;
  onClose: () => void;
};

export function EstablecimientoEditarDialog({
  establecimiento,
  onClose
}: EstablecimientoEditarDialogProps) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (values: EstablecimientoFormValues) =>
      actualizarEstablecimiento(
        apiClient,
        establecimiento.id,
        tenantId,
        toActualizarEstablecimientoPayload(values)
      ),
    onSuccess: () => {
      onClose();
      void invalidateOrganizacion(queryClient);
    }
  });

  return (
    <Modal open onClose={onClose} title="Editar establecimiento" size="lg">
      <EstablecimientoForm
        isEdit
        defaultValues={toEstablecimientoFormValues(establecimiento)}
        submitLabel="Guardar cambios"
        isSubmitting={mutation.isPending}
        error={mutation.isError ? describeApiError(mutation.error) : null}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </Modal>
  );
}
```

- [ ] **Step 3: Usarlo en `EstablecimientoDetailPage`**

En `FE/pages/EstablecimientoDetailPage.tsx`:

1. Cambiar `import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';` por `import { Button, Card, EstadoBadge, PageHeader } from '@boticas/ui-web';`.
2. Cambiar el import de `../api/establecimientos.api` para dejar solo `cambiarEstadoEstablecimiento` y `establecimientoQuery` (se elimina `actualizarEstablecimiento`).
3. Eliminar los imports de `EstablecimientoForm`, `toEstablecimientoFormValues`, `toActualizarEstablecimientoPayload` y `type EstablecimientoFormValues`; añadir `import { EstablecimientoEditarDialog } from '../components/EstablecimientoEditarDialog';` (orden alfabético, tras `DatoItem`).
4. Eliminar el bloque `const updateMutation = useMutation({ ... });` y la función `closeEdit`.
5. Reemplazar el bloque:

```tsx
      <Modal open={editOpen} onClose={closeEdit} title="Editar establecimiento" size="lg">
        <EstablecimientoForm
          isEdit
          defaultValues={toEstablecimientoFormValues(establecimiento)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
          onSubmit={(values) => updateMutation.mutate(values)}
        />
      </Modal>
```

por:

```tsx
      {editOpen ? (
        <EstablecimientoEditarDialog
          establecimiento={establecimiento}
          onClose={() => setEditOpen(false)}
        />
      ) : null}
```

- [ ] **Step 4: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion/components/EstablecimientoEditarDialog.test.tsx apps/erp-web/src/features/organizacion/pages/EstablecimientoDetailPage.test.tsx`, `<archivos>` = `apps/erp-web/src/features/organizacion/{components/EstablecimientoEditarDialog.tsx,pages/EstablecimientoDetailPage.tsx}`.
Expected: todos PASS (los tests de edición existentes del detalle siguen válidos) y 100% en los dos archivos.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps
git commit -m "refactor(organizacion): extraer el dialogo de edicion de establecimiento

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Tabla de empresas con columna «Acciones»

**Files:**
- Modify: `FE/pages/EmpresasPage.tsx`, `FE/pages/EmpresasPage.test.tsx`

**Interfaces:**
- Consumes: `AccionesFila` (Task 1), `EmpresaEditarDialog` (Task 2).
- Produces: `EmpresasPage` con la razón social en texto plano y la columna «Acciones» (`Ver detalle de {razonSocial}`, `Editar {razonSocial}`).

- [ ] **Step 1: Actualizar y ampliar los tests (fallan)**

En `FE/pages/EmpresasPage.test.tsx`:

1. En `lista las empresas con enlace al detalle, nombre comercial y estado` (renombrar a `lista las empresas con acciones, nombre comercial y estado`), reemplazar:

```tsx
    const link = await screen.findByRole('link', { name: 'Boticas SAC' });
    expect(link).toHaveAttribute('href', '/organizacion/empresas/empresa-1');
    expect(screen.getByRole('link', { name: 'Inversiones Andinas SAC' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-2'
    );
```

por:

```tsx
    expect(await screen.findByText('Boticas SAC')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Boticas SAC' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver detalle de Boticas SAC' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-1'
    );
    expect(
      screen.getByRole('link', { name: 'Ver detalle de Inversiones Andinas SAC' })
    ).toHaveAttribute('href', '/organizacion/empresas/empresa-2');
    expect(screen.getByRole('button', { name: 'Editar Boticas SAC' })).toBeInTheDocument();
```

2. En `crea una empresa y refresca el listado`, reemplazar `expect(await screen.findByRole('link', { name: 'Boticas SAC' })).toBeInTheDocument();` por `expect(await screen.findByText('Boticas SAC')).toBeInTheDocument();`.

3. Añadir dentro del `describe`:

```tsx
  it('edita una empresa desde la lista sin salir de la página', async () => {
    let razonSocial = 'Boticas SAC';
    let body: Record<string, unknown> = {};
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([{ ...sampleEmpresa, razonSocial }]))),
      http.put(`${listUrl}/empresa-1`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        razonSocial = String(body.razonSocial);
        return HttpResponse.json({ ...sampleEmpresa, razonSocial });
      })
    );
    const { user, router } = renderPage();
    await screen.findByText('Boticas SAC');

    await user.click(screen.getByRole('button', { name: 'Editar Boticas SAC' }));
    expect(screen.getByRole('heading', { name: 'Editar empresa' })).toBeInTheDocument();
    expect(screen.getByLabelText('RUC')).toHaveAttribute('readonly');
    const razon = screen.getByLabelText('Razón social');
    await user.clear(razon);
    await user.type(razon, 'Boticas del Perú SAC');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Boticas del Perú SAC')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(body).toMatchObject({ razonSocial: 'Boticas del Perú SAC' });
    expect(router.state.location.pathname).toBe('/organizacion/empresas');
  });

  it('cierra el modal de edición con Cerrar sin guardar', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([sampleEmpresa]))));
    const { user } = renderPage();
    await screen.findByText('Boticas SAC');

    await user.click(screen.getByRole('button', { name: 'Editar Boticas SAC' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/EmpresasPage.test.tsx --project erp-web`
Expected: FAIL (la razón social sigue siendo un enlace y no hay columna «Acciones»).

- [ ] **Step 2: Implementar**

En `FE/pages/EmpresasPage.tsx`:

1. Eliminar la línea `import { Link } from 'react-router';`.
2. Añadir los imports `import { AccionesFila } from '../components/AccionesFila';` y `import { EmpresaEditarDialog } from '../components/EmpresaEditarDialog';` (orden alfabético, junto a `EmpresaForm`; `EmpresaForm` sigue usándose en el modal de alta).
3. Añadir el estado junto a los demás: `const [editing, setEditing] = useState<Empresa | null>(null);`.
4. Reemplazar la columna de razón social:

```tsx
            {
              header: 'Razón social',
              cell: (row) => (
                <Link
                  className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  to={`/organizacion/empresas/${row.id}`}
                >
                  {row.razonSocial}
                </Link>
              )
            },
```

por:

```tsx
            { header: 'Razón social', cell: (row) => row.razonSocial },
```

y, tras la columna `Estado`, añadir:

```tsx
            {
              header: 'Acciones',
              cell: (row) => (
                <AccionesFila
                  nombre={row.razonSocial}
                  detalleHref={`/organizacion/empresas/${row.id}`}
                  onEditar={() => setEditing(row)}
                />
              )
            }
```

5. Justo antes de cerrar el `<div className="mx-auto max-w-7xl">` (después del `Modal` de alta) añadir:

```tsx
      {editing ? <EmpresaEditarDialog empresa={editing} onClose={() => setEditing(null)} /> : null}
```

- [ ] **Step 3: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion/pages/EmpresasPage.test.tsx`, `<archivos>` = `apps/erp-web/src/features/organizacion/pages/EmpresasPage.tsx`.
Expected: todos PASS y 100% en `EmpresasPage.tsx`.

- [ ] **Step 4: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): columna de acciones con ver detalle y editar en empresas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Tabla de establecimientos con columna «Acciones»

**Files:**
- Modify: `FE/components/EstablecimientosSection.tsx`, `FE/components/EstablecimientosSection.test.tsx`, `FE/pages/EmpresaDetailPage.test.tsx`

**Interfaces:**
- Consumes: `AccionesFila` (Task 1), `EstablecimientoEditarDialog` (Task 3).
- Produces: `EstablecimientosSection` con el nombre en texto plano y la columna «Acciones» (`Ver detalle de {nombre}`, `Editar {nombre}`).

- [ ] **Step 1: Actualizar y ampliar los tests (fallan)**

En `FE/components/EstablecimientosSection.test.tsx`:

1. En `lista los establecimientos de la empresa con enlace al detalle` (renombrar a `lista los establecimientos de la empresa con acciones`), reemplazar:

```tsx
    const link = await screen.findByRole('link', { name: 'Botica Central' });
    expect(link).toHaveAttribute('href', '/organizacion/establecimientos/est-1');
```

por:

```tsx
    expect(await screen.findByText('Botica Central')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Botica Central' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver detalle de Botica Central' })).toHaveAttribute(
      'href',
      '/organizacion/establecimientos/est-1'
    );
    expect(screen.getByRole('button', { name: 'Editar Botica Central' })).toBeInTheDocument();
```

2. En `crea un establecimiento de la empresa y refresca el listado`, reemplazar `expect(await screen.findByRole('link', { name: 'Botica Central' })).toBeInTheDocument();` por `expect(await screen.findByText('Botica Central')).toBeInTheDocument();`.

3. Añadir dentro del `describe`:

```tsx
  it('edita un establecimiento desde la lista sin salir de la página', async () => {
    let nombre = 'Botica Central';
    let body: Record<string, unknown> = {};
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([{ ...sampleEstablecimiento, nombre }]))),
      http.put(`${listUrl}/est-1`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        nombre = String(body.nombre);
        return HttpResponse.json({ ...sampleEstablecimiento, nombre });
      })
    );
    const { user, router } = renderSection();
    await screen.findByText('Botica Central');

    await user.click(screen.getByRole('button', { name: 'Editar Botica Central' }));
    expect(screen.getByRole('heading', { name: 'Editar establecimiento' })).toBeInTheDocument();
    expect(screen.getByLabelText('Código', { exact: true })).toHaveAttribute('readonly');
    const campoNombre = screen.getByLabelText('Nombre', { exact: true });
    await user.clear(campoNombre);
    await user.type(campoNombre, 'Botica Principal');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Botica Principal')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(body).toMatchObject({ nombre: 'Botica Principal' });
    expect(router.state.location.pathname).toBe('/empresa');
  });

  it('cierra el modal de edición con Cerrar sin guardar', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([sampleEstablecimiento]))));
    const { user } = renderSection();
    await screen.findByText('Botica Central');

    await user.click(screen.getByRole('button', { name: 'Editar Botica Central' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
```

En `FE/pages/EmpresaDetailPage.test.tsx`, en `muestra los datos de la empresa y sus establecimientos`, reemplazar `expect(await screen.findByRole('link', { name: 'Botica Central' })).toBeInTheDocument();` por `expect(await screen.findByText('Botica Central')).toBeInTheDocument();`.

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EstablecimientosSection.test.tsx apps/erp-web/src/features/organizacion/pages/EmpresaDetailPage.test.tsx --project erp-web`
Expected: FAIL (el nombre sigue siendo un enlace).

- [ ] **Step 2: Implementar**

En `FE/components/EstablecimientosSection.tsx`:

1. Eliminar la línea `import { Link } from 'react-router';`.
2. Añadir `import { AccionesFila } from './AccionesFila';` y `import { EstablecimientoEditarDialog } from './EstablecimientoEditarDialog';` junto a `import { EstablecimientoForm } from './EstablecimientoForm';` (orden alfabético).
3. Añadir el estado `const [editing, setEditing] = useState<Establecimiento | null>(null);` tras `createOpen`.
4. Reemplazar la columna de nombre:

```tsx
            {
              header: 'Nombre',
              cell: (row) => (
                <Link
                  className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  to={`/organizacion/establecimientos/${row.id}`}
                >
                  {row.nombre}
                </Link>
              )
            },
```

por:

```tsx
            { header: 'Nombre', cell: (row) => row.nombre },
```

y, tras la columna `Estado`, añadir:

```tsx
            {
              header: 'Acciones',
              cell: (row) => (
                <AccionesFila
                  nombre={row.nombre}
                  detalleHref={`/organizacion/establecimientos/${row.id}`}
                  onEditar={() => setEditing(row)}
                />
              )
            }
```

5. Después del `Modal` de alta, antes del cierre de `</section>`, añadir:

```tsx
      {editing ? (
        <EstablecimientoEditarDialog
          establecimiento={editing}
          onClose={() => setEditing(null)}
        />
      ) : null}
```

- [ ] **Step 3: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion`, `<archivos>` = `apps/erp-web/src/features/organizacion/components/EstablecimientosSection.tsx`.
Expected: todos PASS en la feature y 100% en `EstablecimientosSection.tsx`.

- [ ] **Step 4: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): columna de acciones con ver detalle y editar en establecimientos

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Playwright en tres viewports y verificación final

**Files:**
- Modify: `e2e/organizacion.spec.ts`

**Interfaces:**
- Consumes: todo lo producido por las Tasks 1 a 5. Ya existen en el spec `abrirSesionEn`, `crearEmpresa` y `expectNoHorizontalOverflow`. El backend falso simula `PUT` de empresas y establecimientos.
- Produces: cobertura e2e del ojo y del lápiz en desktop, tablet y móvil.

- [ ] **Step 1: Actualizar los localizadores que dependían del enlace por nombre**

En `e2e/organizacion.spec.ts`:

1. En los dos lugares donde se espera visible el enlace tras crear la empresa (`crea una empresa, filtra el listado y rechaza un RUC duplicado`), reemplazar `page.getByRole('link', { name: 'PRUEBA UI Boticas SAC' })` por `page.getByRole('link', { name: 'Ver detalle de PRUEBA UI Boticas SAC' })`.
2. En el flujo completo, reemplazar `await page.getByRole('link', { name: 'PRUEBA UI Boticas SAC' }).click();` por `await page.getByRole('link', { name: 'Ver detalle de PRUEBA UI Boticas SAC' }).click();`.
3. En el flujo completo, reemplazar la línea `await page.getByRole('link', { name: 'Botica UI Central' }).click();` por este bloque (edita desde la tabla con el lápiz y luego entra con el ojo):

```ts
    await page.getByRole('button', { name: 'Editar Botica UI Central' }).click();
    const editarEstablecimiento = page.getByRole('dialog');
    await expect(
      editarEstablecimiento.getByRole('heading', { name: 'Editar establecimiento' })
    ).toBeVisible();
    await expect(editarEstablecimiento.getByLabel('Código', { exact: true })).toHaveAttribute(
      'readonly',
      ''
    );
    await editarEstablecimiento.getByLabel('Referencia').fill('Frente al parque');
    await editarEstablecimiento.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page).toHaveURL(/\/organizacion\/empresas\/[^/]+$/);
    await page.getByRole('link', { name: 'Ver detalle de Botica UI Central' }).click();
```

- [ ] **Step 2: Añadir el caso de edición desde la lista de empresas**

Añadir dentro del `describe('Organización', ...)`, después de `cancela la creación de una empresa sin guardarla`:

```ts
  test('edita una empresa desde la lista sin salir de la página', async ({ page }) => {
    await abrirSesionEn(page, '/organizacion/empresas');
    await crearEmpresa(page, '20999999990', 'PRUEBA UI Boticas SAC');
    await expect(page.getByRole('dialog')).toBeHidden();

    await page.getByRole('button', { name: 'Editar PRUEBA UI Boticas SAC' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: 'Editar empresa' })).toBeVisible();
    await expect(dialog.getByLabel('RUC')).toHaveAttribute('readonly', '');
    await dialog.getByLabel('Nombre comercial').fill('UI Boticas');
    await dialog.getByRole('button', { name: 'Guardar cambios' }).click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('UI Boticas', { exact: true })).toBeVisible();
    await expect(page).toHaveURL(/\/organizacion\/empresas$/);
    await expectNoHorizontalOverflow(page);
  });
```

- [ ] **Step 3: Ejecutar Playwright en los tres viewports**

Si hay un servidor Vite escuchando en el puerto 3000 en modo `http`, Playwright lo reutiliza (no matarlo); si el puerto está libre, levanta el suyo. Sin la variable `CI`:

Run: `Remove-Item Env:CI -ErrorAction SilentlyContinue; pnpm.cmd exec playwright test --reporter=line`
Expected: todos los casos PASS en `desktop`, `tablet` y `mobile`. Si algún caso falla solo por tiempo bajo carga, repetir con `--workers=1` antes de concluir nada.

- [ ] **Step 4: Gate completo del frontend**

Run: `$env:CI='true'; pnpm.cmd lint; pnpm.cmd typecheck; pnpm.cmd build`
Expected: lint sin errores (las advertencias existentes no cuentan), typecheck y build sin errores.

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion --project erp-web --testTimeout=60000`
Expected: todos los tests de la feature PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/e2e
git commit -m "test(e2e): cubrir el ojo y el lapiz de las tablas de organizacion

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-review

**Cobertura del spec:**
- Nombre como texto plano en empresas y establecimientos → Tasks 4 y 5.
- Columna «Acciones» con ojo y lápiz, mismo estilo que Seguridad y Catálogo → Task 1 (`AccionesFila`), usada en las Tasks 4 y 5.
- Diálogos de edición compartidos sin duplicación → Tasks 2 y 3, con el uso en el detalle y en la lista.
- «Cambiar estado» solo en el detalle, árbol del resumen sin cambios, backend sin cambios → ninguna tarea los toca (por diseño).
- Pruebas: tests unitarios al 100%, actualización de los tests que buscaban el enlace por nombre (Tasks 4 y 5), e2e en tres viewports (Task 6).

**Consistencia de nombres:** el enlace se llama `Ver detalle de {nombre}` y el botón `Editar {nombre}` en `AccionesFila`, en los tests de las dos tablas y en el e2e; los diálogos reciben `empresa`/`establecimiento` y `onClose`; el estado de las tablas se llama `editing` en ambas.

**Riesgos conocidos:**
- Los tests de edición del detalle (`EmpresaDetailPage.test.tsx`, `EstablecimientoDetailPage.test.tsx`) no cambian: validan el comportamiento del diálogo extraído, que debe ser idéntico al anterior (el error del servidor se limpia al cerrar porque el diálogo se desmonta).
- Al editar desde la lista una empresa con teléfono o sitio web antiguos inválidos, el modal pide corregirlos, igual que el detalle (decisión del spec).
