# Crear Usuario — Página Completa con Asignación de Rol — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convertir el modal "Nuevo usuario" en una página completa (`/seguridad/usuarios/nuevo`) que incluye, en el mismo flujo, la asignación opcional de un rol con su ámbito (incluida sucursal cuando aplique), orquestando dos llamadas HTTP secuenciales con manejo explícito de fallo parcial.

**Architecture:** React 19 + TanStack Query + react-hook-form/zod, siguiendo los patrones ya establecidos en `features/seguridad`. Extracción de un componente reutilizable (`AsignacionRolForm`) desde el modal existente `AsignarRolDialog`, sin cambiar su comportamiento actual en `UserDetailPage`. Primera página de creación con ruta propia del proyecto (hasta hoy todas las creaciones son modales).

**Tech Stack:** TypeScript, React 19, React Router 8, TanStack Query 5, react-hook-form + zod, Vitest + Testing Library + MSW.

## Global Constraints

- Todo archivo fuente nuevo o reescrito debe alcanzar 100% de cobertura de líneas y ramas (gate Vitest por archivo) — CLAUDE.md, "Cobertura de tests".
- Sin comentarios explicativos en el código salvo decisión no obvia — CLAUDE.md, "Estándares de implementación".
- El router y las features no importan archivos internos de otra feature, solo su `index.ts` — CLAUDE.md, "Frontend: pnpm workspace".
- Comando de test correcto en este repo: `cd frontend && npx vitest run <ruta>` (NO `pnpm --filter ... test`, no funciona). Lint: `cd frontend && npx eslint apps packages`. Typecheck: `cd frontend/apps/erp-web && npx tsc -b --pretty false`.
- No existe backend que una crear-usuario y asignar-rol en una transacción — son 2 llamadas HTTP independientes (`POST /usuarios`, `POST /usuarios/{id}/role-assignments`), sin cambios de backend en este plan.
- No existe tabla ni concepto de "sucursal" independiente del rol — el ámbito de una asignación de rol (`scopeType` + `companyId`/`establishmentId`/`warehouseId`/`terminalId`) es la única forma de asociar un usuario a una sucursal, y siempre va junto con un `roleId`.

---

## Task 1: Extraer `AsignacionRolForm` reutilizable desde `AsignarRolDialog`

**Files:**
- Create: `frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.tsx`
- Create: `frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.test.tsx`
- Modify: `frontend/apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`
- Modify: `frontend/apps/erp-web/src/features/seguridad/components/AsignarRolDialog.test.tsx`

**Interfaces:**
- Produces: `AsignacionRolForm` — componente con props `{ tenantId: string; onSubmit: (values: AsignacionRolFormValues) => void; onCancel: () => void; isSubmitting?: boolean; errorMessage?: string | undefined; formId?: string | undefined }`. Usado por `AsignarRolDialog` (Task 1) y por `NewUserPage` (Task 3).

- [ ] **Step 1: Leer el contenido actual completo de `AsignarRolDialog.tsx` para confirmar que no cambió**

Ya verificado en este plan — el archivo actual (233 líneas) tiene el `<form>` en las líneas 79-230, envuelto por `<Modal open={open} onClose={onCancel} title="Asignar rol">` (línea 78) y su cierre (línea 231). El resto del componente (imports, queries `rolesQuery`/`corporateStructureQuery`, `useForm`, los tres `useEffect` de reseteo en cascada) no depende de `Modal` en absoluto.

- [ ] **Step 2: Crear `AsignacionRolForm.tsx` con el form extraído**

Crear `frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.tsx`:

```tsx
import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Button, FormField } from '@boticas/ui-web';
import { rolesQuery } from '../api/roles.api';
import { corporateStructureQuery } from '../../organizacion';
import {
  asignacionRolSchema,
  SCOPE_TYPES,
  type AsignacionRolFormValues
} from '../schemas/asignacion-rol.schema';

export type AsignacionRolFormProps = {
  tenantId: string;
  onSubmit: (values: AsignacionRolFormValues) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  errorMessage?: string | undefined;
  formId?: string | undefined;
};

export function AsignacionRolForm({
  tenantId,
  onSubmit,
  onCancel,
  isSubmitting = false,
  errorMessage,
  formId
}: AsignacionRolFormProps) {
  const rolesResult = useQuery(rolesQuery({ tenantId, size: 100 }));
  const structureResult = useQuery(corporateStructureQuery);

  const {
    formState: { errors },
    handleSubmit,
    register,
    setValue,
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
  const selectedEstablishment = establishments.find(
    (establishment) => establishment.id === establishmentId
  );

  useEffect(() => {
    setValue('companyId', '');
    setValue('establishmentId', '');
    setValue('warehouseId', '');
    setValue('terminalId', '');
  }, [scopeType, setValue]);

  useEffect(() => {
    setValue('establishmentId', '');
    setValue('warehouseId', '');
    setValue('terminalId', '');
  }, [companyId, setValue]);

  useEffect(() => {
    setValue('warehouseId', '');
    setValue('terminalId', '');
  }, [establishmentId, setValue]);

  return (
    <form
      id={formId}
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {errorMessage ? (
        <p role="alert" className="text-danger-700 dark:text-danger-400 text-sm font-medium">
          {errorMessage}
        </p>
      ) : null}
      <FormField label="Rol" htmlFor="asignacion-role" error={errors.roleId?.message}>
        <select
          id="asignacion-role"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
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
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
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
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
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
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
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
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
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
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
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
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('validFrom')}
        />
      </FormField>

      <FormField label="Vigente hasta (opcional)" htmlFor="asignacion-valid-until">
        <input
          id="asignacion-valid-until"
          type="datetime-local"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('validUntil')}
        />
      </FormField>

      {formId ? null : (
        <div className="flex justify-end gap-3">
          <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            Asignar rol
          </Button>
        </div>
      )}
    </form>
  );
}
```

Nota de diseño: la prop `formId` es la única adición respecto al form original. Cuando se pasa (caso `NewUserPage`, Task 3), el `<form>` recibe ese `id` y **no** renderiza sus propios botones Cancelar/Asignar — la página externa dispara el submit vía `document.getElementById(formId).requestSubmit()`. Cuando `formId` es `undefined` (caso `AsignarRolDialog`, uso actual sin cambios), el form se comporta exactamente igual que hoy, con sus propios botones.

- [ ] **Step 2b: Escribir `AsignacionRolForm.test.tsx` (mueve la cobertura de `AsignarRolDialog.test.tsx`)**

Crear `frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { AsignacionRolForm } from './AsignacionRolForm';

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

function renderForm(props: Partial<Parameters<typeof AsignacionRolForm>[0]> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const onSubmit = props.onSubmit ?? vi.fn();
  const onCancel = props.onCancel ?? vi.fn();
  return {
    onSubmit,
    onCancel,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AsignacionRolForm
          tenantId="tenant-1"
          onSubmit={onSubmit}
          onCancel={onCancel}
          errorMessage={props.errorMessage}
          formId={props.formId}
        />
      </QueryClientProvider>
    )
  };
}

describe('AsignacionRolForm', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleStructure))
    );
  });

  it('exige seleccionar un establecimiento cuando el ambito es ESTABLECIMIENTO', async () => {
    const { user, onSubmit } = renderForm();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    expect(await screen.findByText('Selecciona un establecimiento.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia el payload completo cuando el ambito ESTABLECIMIENTO tiene establecimiento seleccionado', async () => {
    const { user, onSubmit } = renderForm();

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
    const { user, onSubmit } = renderForm();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ roleId: 'rol-1', scopeType: 'GLOBAL' }))
    );
  });

  it('resetea empresa y establecimiento seleccionados al cambiar el tipo de ambito', async () => {
    const { user } = renderForm();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.selectOptions(await screen.findByLabelText('Empresa'), 'company-1');
    await user.selectOptions(await screen.findByLabelText('Establecimiento'), 'est-1');

    expect(screen.getByLabelText('Establecimiento')).toHaveValue('est-1');

    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');

    expect(await screen.findByLabelText('Empresa')).toHaveValue('');
    expect(screen.getByLabelText('Establecimiento')).toHaveValue('');
  });

  it('muestra el mensaje de error cuando errorMessage esta presente', async () => {
    renderForm({ errorMessage: 'No se pudo asignar el rol.' });

    await screen.findByText('Administrador local');
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo asignar el rol.');
  });

  it('no renderiza ningun mensaje de error cuando errorMessage no esta presente', async () => {
    renderForm();

    await screen.findByText('Administrador local');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('no renderiza botones propios cuando se provee formId', async () => {
    renderForm({ formId: 'external-form' });

    await screen.findByText('Administrador local');
    expect(screen.queryByRole('button', { name: 'Asignar rol' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();
    expect(document.getElementById('external-form')).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Ejecutar el test nuevo para verificar que pasa**

Run: `cd frontend && npx vitest run src/features/seguridad/components/AsignacionRolForm.test.tsx`
Expected: `Test Files 1 passed (1)`, `Tests 7 passed (7)`.

- [ ] **Step 4: Reescribir `AsignarRolDialog.tsx` como wrapper delgado**

Reemplazar el contenido completo de `frontend/apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`:

```tsx
import { Modal } from '@boticas/ui-web';
import type { AsignacionRolFormValues } from '../schemas/asignacion-rol.schema';
import { AsignacionRolForm } from './AsignacionRolForm';

export type AsignarRolDialogProps = {
  open: boolean;
  tenantId: string;
  onSubmit: (values: AsignacionRolFormValues) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  errorMessage?: string | undefined;
};

export function AsignarRolDialog({
  open,
  tenantId,
  onSubmit,
  onCancel,
  isSubmitting = false,
  errorMessage
}: AsignarRolDialogProps) {
  if (!open) return null;

  return (
    <Modal open={open} onClose={onCancel} title="Asignar rol">
      <AsignacionRolForm
        tenantId={tenantId}
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={isSubmitting}
        errorMessage={errorMessage}
      />
    </Modal>
  );
}
```

- [ ] **Step 5: Ejecutar `AsignarRolDialog.test.tsx` sin modificar — debe seguir pasando tal cual**

Run: `cd frontend && npx vitest run src/features/seguridad/components/AsignarRolDialog.test.tsx`
Expected: `Test Files 1 passed (1)`, `Tests 6 passed (6)` — sin cambiar el archivo de test, el wrapper debe comportarse idéntico (mismos labels, mismo `role="dialog"` provisto por `Modal`, mismo `role="alert"`).

- [ ] **Step 6: Ejecutar el resto de la suite de seguridad para detectar regresión en `UserDetailPage`**

Run: `cd frontend && npx vitest run src/features/seguridad/pages/UserDetailPage.test.tsx`
Expected: `Test Files 1 passed (1)` — `UserDetailPage` sigue usando `AsignarRolDialog` exactamente igual, sin cambios de comportamiento.

- [ ] **Step 7: Lint y typecheck**

Run: `cd frontend && npx eslint apps/erp-web/src/features/seguridad/components/AsignacionRolForm.tsx apps/erp-web/src/features/seguridad/components/AsignacionRolForm.test.tsx apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`
Expected: sin errores.

Run: `cd frontend/apps/erp-web && npx tsc -b --pretty false`
Expected: sin salida (sin errores).

- [ ] **Step 8: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.tsx frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.test.tsx frontend/apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx
git commit -m "$(cat <<'EOF'
refactor(seguridad): extraer AsignacionRolForm reutilizable de AsignarRolDialog

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Agregar `buttonClassName` a `@boticas/ui-web` para el link "Nuevo usuario"

**Files:**
- Modify: `frontend/packages/ui-web/src/button/Button.tsx`
- Modify: `frontend/packages/ui-web/src/button/Button.test.tsx` (si existe; si no existe, crear con el caso nuevo únicamente)
- Modify: `frontend/packages/ui-web/src/index.ts`

**Interfaces:**
- Consumes: nada nuevo.
- Produces: `buttonClassName(variant?: ButtonVariant, size?: ButtonSize): string`, exportado desde `@boticas/ui-web`. Usado por `UsersPage.tsx` (Task 3) para el `<Link>` "Nuevo usuario" con apariencia de botón primario.

- [ ] **Step 1: Verificar si existe `Button.test.tsx`**

Run: `ls frontend/packages/ui-web/src/button/`
Si existe `Button.test.tsx`, leerlo completo antes de modificar. Si no existe, se crea uno nuevo solo con el test de `buttonClassName` (Step 3).

- [ ] **Step 2: Escribir el test que falla**

Si `Button.test.tsx` ya existe, añadir al final del `describe` existente; si no existe, crear `frontend/packages/ui-web/src/button/Button.test.tsx` con:

```tsx
import { render, screen } from '@testing-library/react';
import { buttonClassName } from './Button';

describe('buttonClassName', () => {
  it('devuelve las clases del boton primario por defecto', () => {
    const className = buttonClassName();
    expect(className).toContain('bg-primary-600');
    expect(className).toContain('h-11');
  });

  it('devuelve las clases de la variante y tamano solicitados', () => {
    const className = buttonClassName('secondary', 'sm');
    expect(className).toContain('border-neutral-200');
    expect(className).toContain('h-9');
  });

  it('produce las mismas clases base que usa el componente Button', () => {
    render(<a className={buttonClassName()} href="/destino">Ir</a>);
    const link = screen.getByRole('link', { name: 'Ir' });
    expect(link.className).toContain('rounded-xl');
    expect(link.className).toContain('font-semibold');
  });
});
```

- [ ] **Step 2b: Ejecutar el test para verificar que falla**

Run: `cd frontend && npx vitest run packages/ui-web/src/button/Button.test.tsx`
Expected: FAIL — `buttonClassName is not exported` / `has no exported member 'buttonClassName'`.

- [ ] **Step 3: Modificar `Button.tsx` para extraer y exportar `buttonClassName`**

Reemplazar el contenido completo de `frontend/packages/ui-web/src/button/Button.tsx`:

```tsx
import type { ComponentPropsWithRef } from 'react';
import { cn } from '../lib/cn';

type ButtonVariant = 'primary' | 'secondary' | 'ghost';
type ButtonSize = 'sm' | 'md';

export type ButtonProps = ComponentPropsWithRef<'button'> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

const variants: Record<ButtonVariant, string> = {
  primary:
    'bg-primary-600 text-white shadow-sm hover:bg-primary-700 focus-visible:outline-primary-600 disabled:bg-neutral-300 dark:disabled:bg-neutral-700',
  secondary:
    'border border-neutral-200 bg-white text-neutral-700 shadow-sm hover:border-neutral-300 hover:bg-neutral-50 focus-visible:outline-neutral-500 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800',
  ghost:
    'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900 focus-visible:outline-neutral-500 dark:text-neutral-300 dark:hover:bg-neutral-800 dark:hover:text-white'
};

const sizes: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 text-sm',
  md: 'h-11 px-4 text-sm'
};

export function buttonClassName(variant: ButtonVariant = 'primary', size: ButtonSize = 'md'): string {
  return cn(
    'inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed',
    variants[variant],
    sizes[size]
  );
}

export function Button({
  className,
  ref,
  size = 'md',
  type = 'button',
  variant = 'primary',
  ...props
}: ButtonProps) {
  return (
    <button
      ref={ref}
      type={type}
      className={cn(buttonClassName(variant, size), className)}
      {...props}
    />
  );
}
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

Run: `cd frontend && npx vitest run packages/ui-web/src/button/Button.test.tsx`
Expected: `Test Files 1 passed (1)`, `Tests 3 passed (3)`.

- [ ] **Step 5: Exportar `buttonClassName` desde `index.ts`**

En `frontend/packages/ui-web/src/index.ts`, cambiar la línea:

```ts
export { Button, type ButtonProps } from './button/Button';
```

por:

```ts
export { Button, buttonClassName, type ButtonProps } from './button/Button';
```

- [ ] **Step 6: Ejecutar la suite completa de `ui-web` para detectar regresión**

Run: `cd frontend && npx vitest run packages/ui-web`
Expected: todos los test files existentes de `ui-web` pasan, incluido el nuevo.

- [ ] **Step 7: Lint y typecheck**

Run: `cd frontend && npx eslint packages/ui-web/src/button`
Expected: sin errores.

Run: `cd frontend/apps/erp-web && npx tsc -b --pretty false`
Expected: sin salida.

- [ ] **Step 8: Commit**

```bash
git add frontend/packages/ui-web/src/button/Button.tsx frontend/packages/ui-web/src/button/Button.test.tsx frontend/packages/ui-web/src/index.ts
git commit -m "$(cat <<'EOF'
feat(ui-web): exportar buttonClassName para botones-link

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Crear `NewUserPage.tsx` con orquestación de creación + asignación de rol

**Files:**
- Create: `frontend/apps/erp-web/src/features/seguridad/pages/NewUserPage.tsx`
- Create: `frontend/apps/erp-web/src/features/seguridad/pages/NewUserPage.test.tsx`

**Interfaces:**
- Consumes: `UsuarioForm` (existente, sin cambios), `AsignacionRolForm` (Task 1, prop `formId`), `crearUsuario`/`asignarRolUsuario` de `api/usuarios.api.ts` (existentes), `useAuthSession` de `features/auth`, `PageHeader`/`Button`/`Card` de `@boticas/ui-web`.
- Produces: `NewUserPage` — componente de página, exportado para uso en `routes.tsx` (Task 4).

- [ ] **Step 1: Escribir el test que falla (los 4 casos del spec)**

Crear `frontend/apps/erp-web/src/features/seguridad/pages/NewUserPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { NewUserPage } from './NewUserPage';

const authenticatedSession = {
  status: 'authenticated' as const,
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

const sampleUsuarioCreado = {
  id: 'user-nuevo',
  tenantId: 'tenant-1',
  documentType: '1',
  documentNumber: '45678912',
  firstNames: null,
  lastNames: null,
  username: null,
  email: 'ada@boticas.pe',
  displayName: null,
  phone: null,
  credentialChangeRequired: false,
  mfaRequired: false,
  status: 'ACTIVO',
  createdAt: '2026-09-27T00:00:00Z',
  updatedAt: null
};

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

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([
    { path: '/seguridad/usuarios/nuevo', Component: NewUserPage },
    { path: '/seguridad/usuarios/:userId', Component: () => <h1>Detalle de usuario</h1> }
  ], { initialEntries: ['/seguridad/usuarios/nuevo'] });
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

async function fillRequiredUserFields(user: ReturnType<typeof userEvent.setup>) {
  await screen.findByRole('option', { name: 'DNI' });
  await user.type(screen.getByLabelText('Número de documento'), '45678912');
  await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
}

describe('NewUserPage', () => {
  it('crea el usuario sin rol cuando el toggle de asignar rol esta desactivado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleUsuarioCreado, { status: 201 });
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(receivedBody).toEqual(
      expect.objectContaining({ tenantId: 'tenant-1', documentType: '1', documentNumber: '45678912', email: 'ada@boticas.pe' })
    );
  });

  it('crea el usuario y asigna el rol cuando el toggle esta activado y el bloque es valido', async () => {
    let roleAssignmentBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', async ({ request }) => {
        roleAssignmentBody = await request.json();
        return HttpResponse.json(
          {
            id: 'assign-1',
            tenantId: 'tenant-1',
            userId: 'user-nuevo',
            roleId: 'rol-1',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'wilton.sullcaray.r@gmail.com',
            createdAt: '2026-09-27T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(roleAssignmentBody).toEqual(
      expect.objectContaining({ tenantId: 'tenant-1', roleId: 'rol-1', scopeType: 'GLOBAL' })
    );
  });

  it('muestra fallo parcial cuando el usuario se crea pero la asignacion de rol falla, y permite reintentar', async () => {
    let attempts = 0;
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.json({ detail: 'El rol indicado no existe.' }, { status: 404 });
        }
        return HttpResponse.json(
          {
            id: 'assign-1',
            tenantId: 'tenant-1',
            userId: 'user-nuevo',
            roleId: 'rol-1',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'wilton.sullcaray.r@gmail.com',
            createdAt: '2026-09-27T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(
      await screen.findByText('Usuario creado, pero no se pudo asignar el rol: El rol indicado no existe.')
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Detalle de usuario' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Reintentar asignación' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
  });

  it('muestra el error inline y no crea ninguna asignacion cuando falla la creacion del usuario', async () => {
    server.use(
      http.post('*/api/v1/usuarios', () =>
        HttpResponse.json({ detail: 'Ya existe un usuario con ese correo.' }, { status: 409 })
      )
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un usuario con ese correo.');
    expect(screen.queryByRole('heading', { name: 'Detalle de usuario' })).not.toBeInTheDocument();
  });

  it('el enlace ir al detalle desde el fallo parcial navega sin reintentar la asignacion', async () => {
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () =>
        HttpResponse.json({ detail: 'El rol indicado no existe.' }, { status: 404 })
      )
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    await screen.findByText('Usuario creado, pero no se pudo asignar el rol: El rol indicado no existe.');
    await user.click(screen.getByRole('link', { name: 'Ir al detalle del usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `cd frontend && npx vitest run src/features/seguridad/pages/NewUserPage.test.tsx`
Expected: FAIL — `Cannot find module './NewUserPage'`.

- [ ] **Step 3: Crear `NewUserPage.tsx`**

Crear `frontend/apps/erp-web/src/features/seguridad/pages/NewUserPage.tsx`:

```tsx
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { ApiError } from '@boticas/api-client';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearUsuario, asignarRolUsuario } from '../api/usuarios.api';
import type { UsuarioFormValues } from '../schemas/usuario.schema';
import type { AsignacionRolFormValues } from '../schemas/asignacion-rol.schema';
import { UsuarioForm } from '../components/UsuarioForm';
import { AsignacionRolForm } from '../components/AsignacionRolForm';

const ROLE_FORM_ID = 'new-user-role-assignment-form';

type Phase = 'idle' | 'creating' | 'assigning' | 'partial-failure';

export function NewUserPage() {
  const { tenantId } = useAuthSession();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [assignRoleEnabled, setAssignRoleEnabled] = useState(false);
  const [phase, setPhase] = useState<Phase>('idle');
  const [createError, setCreateError] = useState<string | null>(null);
  const [assignError, setAssignError] = useState<string | null>(null);
  const [createdUserId, setCreatedUserId] = useState<string | null>(null);
  const [pendingRoleValues, setPendingRoleValues] = useState<AsignacionRolFormValues | null>(null);

  const createMutation = useMutation({
    mutationFn: (values: UsuarioFormValues) =>
      crearUsuario(apiClient, {
        tenantId: tenantId ?? '',
        documentType: values.documentType || undefined,
        documentNumber: values.documentNumber || undefined,
        displayName: values.displayName || undefined,
        firstNames: values.firstNames || undefined,
        lastNames: values.lastNames || undefined,
        email: values.email || undefined,
        username: values.username || undefined,
        phone: values.phone || undefined,
        mfaRequired: values.mfaRequired
      })
  });

  const assignRoleMutation = useMutation({
    mutationFn: ({ userId, values }: { userId: string; values: AsignacionRolFormValues }) =>
      asignarRolUsuario(apiClient, userId, {
        tenantId: tenantId ?? '',
        roleId: values.roleId,
        scopeType: values.scopeType,
        companyId: values.companyId || undefined,
        establishmentId: values.establishmentId || undefined,
        warehouseId: values.warehouseId || undefined,
        terminalId: values.terminalId || undefined,
        validFrom: values.validFrom || undefined,
        validUntil: values.validUntil || undefined
      })
  });

  const goToDetail = (userId: string) => {
    void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
    navigate(`/seguridad/usuarios/${userId}`);
  };

  const handleCreateUser = (values: UsuarioFormValues) => {
    setCreateError(null);
    setPhase('creating');
    createMutation.mutate(values, {
      onError: (error: unknown) => {
        setCreateError(error instanceof ApiError ? error.message : 'No se pudo crear el usuario.');
        setPhase('idle');
      },
      onSuccess: (usuario) => {
        setCreatedUserId(usuario.id);
        if (!assignRoleEnabled) {
          goToDetail(usuario.id);
          return;
        }
        const roleForm = document.getElementById(ROLE_FORM_ID) as HTMLFormElement | null;
        roleForm?.requestSubmit();
      }
    });
  };

  const handleAssignRole = (values: AsignacionRolFormValues) => {
    if (!createdUserId) return;
    setAssignError(null);
    setPendingRoleValues(values);
    setPhase('assigning');
    assignRoleMutation.mutate(
      { userId: createdUserId, values },
      {
        onError: (error: unknown) => {
          setAssignError(error instanceof ApiError ? error.message : 'No se pudo asignar el rol.');
          setPhase('partial-failure');
        },
        onSuccess: () => {
          goToDetail(createdUserId);
        }
      }
    );
  };

  const handleRetryAssignment = () => {
    if (!createdUserId || !pendingRoleValues) return;
    handleAssignRole(pendingRoleValues);
  };

  if (phase === 'partial-failure' && createdUserId) {
    return (
      <div className="mx-auto max-w-3xl">
        <PageHeader
          title="Nuevo usuario"
          context="Seguridad / Usuarios / Nuevo usuario"
          description="El usuario fue creado, pero la asignación de rol no se pudo completar."
        />
        <Card className="mt-6 p-5">
          <p className="text-danger-700 dark:text-danger-400 text-sm font-medium">
            Usuario creado, pero no se pudo asignar el rol: {assignError}
          </p>
          <div className="mt-4 flex items-center gap-3">
            <Button onClick={handleRetryAssignment} disabled={assignRoleMutation.isPending}>
              Reintentar asignación
            </Button>
            <Link
              to={`/seguridad/usuarios/${createdUserId}`}
              className="text-primary-700 dark:text-primary-400 text-sm font-semibold"
            >
              Ir al detalle del usuario
            </Link>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        title="Nuevo usuario"
        context="Seguridad / Usuarios / Nuevo usuario"
        description="Crea una cuenta de acceso al sistema y, opcionalmente, asígnale un rol."
      />

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-neutral-950 dark:text-white">Datos del usuario</h2>
        <div className="mt-4">
          <UsuarioForm
            onSubmit={handleCreateUser}
            submitLabel="Crear usuario"
            isSubmitting={phase === 'creating' || phase === 'assigning'}
          />
          {createError && (
            <p className="mt-3 text-sm text-red-600 dark:text-red-400" role="alert">
              {createError}
            </p>
          )}
        </div>
      </Card>

      <Card className="mt-6 p-5">
        <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm font-semibold text-neutral-700 dark:text-neutral-200">
          <input
            type="checkbox"
            className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
            checked={assignRoleEnabled}
            onChange={(event) => setAssignRoleEnabled(event.target.checked)}
          />
          Asignar rol ahora
        </label>
        {assignRoleEnabled && (
          <div className="mt-4">
            <AsignacionRolForm
              formId={ROLE_FORM_ID}
              tenantId={tenantId ?? ''}
              onSubmit={handleAssignRole}
              onCancel={() => setAssignRoleEnabled(false)}
              isSubmitting={phase === 'assigning'}
            />
          </div>
        )}
      </Card>
    </div>
  );
}
```

Nota de orquestación: `UsuarioForm` conserva su propio botón submit interno ("Crear usuario"), que es el único disparador visible del flujo completo. Al enviarse, `handleCreateUser` llama a `crearUsuario`; si el toggle "Asignar rol ahora" está activo, dispara programáticamente el submit del `<form id={ROLE_FORM_ID}>` (renderizado por `AsignacionRolForm` con `formId`) vía `HTMLFormElement.requestSubmit()`, lo que ejecuta la validación zod de ese formulario y, si es válida, invoca `handleAssignRole`. Si el toggle está desactivado, se navega directo al detalle.

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

Run: `cd frontend && npx vitest run src/features/seguridad/pages/NewUserPage.test.tsx`
Expected: `Test Files 1 passed (1)`, `Tests 5 passed (5)`.

- [ ] **Step 5: Lint y typecheck**

Run: `cd frontend && npx eslint apps/erp-web/src/features/seguridad/pages/NewUserPage.tsx apps/erp-web/src/features/seguridad/pages/NewUserPage.test.tsx`
Expected: sin errores.

Run: `cd frontend/apps/erp-web && npx tsc -b --pretty false`
Expected: sin salida.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/pages/NewUserPage.tsx frontend/apps/erp-web/src/features/seguridad/pages/NewUserPage.test.tsx
git commit -m "$(cat <<'EOF'
feat(seguridad): agregar NewUserPage con creación de usuario y asignación de rol opcional

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Registrar la ruta y actualizar `UsersPage` para enlazar en vez de abrir modal

**Files:**
- Modify: `frontend/apps/erp-web/src/features/seguridad/routes.tsx`
- Modify: `frontend/apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`
- Modify: `frontend/apps/erp-web/src/features/seguridad/pages/UsersPage.test.tsx`

**Interfaces:**
- Consumes: `NewUserPage` (Task 3, vía import lazy en `routes.tsx`), `buttonClassName` (Task 2).
- Produces: ninguna interfaz nueva — es el punto de entrada de la feature.

- [ ] **Step 1: Agregar la ruta nueva en `routes.tsx`, antes de `:userId`**

En `frontend/apps/erp-web/src/features/seguridad/routes.tsx`, insertar el bloque nuevo entre `seguridad/usuarios` y `seguridad/usuarios/:userId`:

```tsx
  {
    path: 'seguridad/usuarios',
    lazy: async () => {
      const { UsersPage } = await import('./pages/UsersPage');
      return { Component: UsersPage };
    }
  },
  {
    path: 'seguridad/usuarios/nuevo',
    lazy: async () => {
      const { NewUserPage } = await import('./pages/NewUserPage');
      return { Component: NewUserPage };
    }
  },
  {
    path: 'seguridad/usuarios/:userId',
    lazy: async () => {
      const { UserDetailPage } = await import('./pages/UserDetailPage');
      return { Component: UserDetailPage };
    }
  },
```

- [ ] **Step 2: Escribir el test que falla para `UsersPage` (el link, no el modal)**

En `frontend/apps/erp-web/src/features/seguridad/pages/UsersPage.test.tsx`, reemplazar el test `'crea un usuario nuevo y refresca el listado'` (líneas 67-95) y el test `'muestra el error del servidor cuando la creacion falla'` (líneas 97-117) por:

```tsx
  it('el boton nuevo usuario enlaza a la pagina de creacion', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 })
      )
    );

    renderPage();
    await screen.findByText('No se encontraron usuarios.');

    const link = screen.getByRole('link', { name: 'Nuevo usuario' });
    expect(link).toHaveAttribute('href', '/seguridad/usuarios/nuevo');
  });
```

Archivo completo resultante (`UsersPage.test.tsx`):

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { UsersPage } from './UsersPage';

const authenticatedSession = {
  status: 'authenticated' as const,
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

  it('el boton nuevo usuario enlaza a la pagina de creacion', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 })
      )
    );

    renderPage();
    await screen.findByText('No se encontraron usuarios.');

    const link = screen.getByRole('link', { name: 'Nuevo usuario' });
    expect(link).toHaveAttribute('href', '/seguridad/usuarios/nuevo');
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

  it('navega al detalle del usuario al usar el boton de accion', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [sampleUsuario], page: 0, size: 20, totalElements: 1 })
      )
    );

    const { user } = renderPage();
    await screen.findByText('Ada Lovelace');

    const link = screen.getByRole('link', { name: 'Ver detalle de Ada Lovelace' });
    expect(link).toHaveAttribute('href', '/seguridad/usuarios/user-1');
    await user.click(link);
  });
});
```

- [ ] **Step 3: Ejecutar el test para verificar que falla**

Run: `cd frontend && npx vitest run src/features/seguridad/pages/UsersPage.test.tsx`
Expected: FAIL — el link "Nuevo usuario" no existe todavía (sigue siendo un `<button>` que abre modal).

- [ ] **Step 4: Reescribir `UsersPage.tsx`**

Reemplazar el contenido completo de `frontend/apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`:

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import {
  DataTable,
  EstadoBadge,
  PageHeader,
  ListFilters,
  buttonClassName,
  iconButtonClassName
} from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { usuariosQuery } from '../api/usuarios.api';
import type { Usuario } from '../api/usuarios.types';

export function UsersPage() {
  const { tenantId } = useAuthSession();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState('');

  const { data, isPending, isError } = useQuery({
    ...usuariosQuery({ tenantId: tenantId ?? '', search: search || undefined, page, size }),
    enabled: Boolean(tenantId)
  });

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Usuarios"
        context="Seguridad / Usuarios"
        description="Administra las cuentas de acceso al sistema."
        actions={
          <Link to="/seguridad/usuarios/nuevo" className={buttonClassName()}>
            Nuevo usuario
          </Link>
        }
      />

      <ListFilters
        label="Buscar usuario"
        placeholder="Nombre, correo o documento"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      <div className="mt-6">
        <DataTable<Usuario>
          columns={[
            {
              header: 'Nombre',
              cell: (row) => row.displayName ?? row.email ?? row.username ?? row.id
            },
            { header: 'Correo', cell: (row) => row.email ?? '—' },
            { header: 'MFA', cell: (row) => (row.mfaRequired ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <Link
                  to={`/seguridad/usuarios/${row.id}`}
                  aria-label={`Ver detalle de ${row.displayName ?? row.email ?? row.username ?? row.id}`}
                  title={`Ver detalle de ${row.displayName ?? row.email ?? row.username ?? row.id}`}
                  className={iconButtonClassName()}
                >
                  <Eye className="size-4.5" aria-hidden="true" />
                </Link>
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron usuarios."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de usuarios."
          startIndex={page * size}
          pagination={{
            page,
            size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>
    </div>
  );
}
```

- [ ] **Step 5: Ejecutar el test para verificar que pasa**

Run: `cd frontend && npx vitest run src/features/seguridad/pages/UsersPage.test.tsx`
Expected: `Test Files 1 passed (1)`, `Tests 4 passed (4)`.

- [ ] **Step 6: Ejecutar la suite completa de la feature seguridad para detectar regresiones**

Run: `cd frontend && npx vitest run src/features/seguridad`
Expected: todos los test files pasan, sin regresiones.

- [ ] **Step 7: Ejecutar la suite completa del workspace y typecheck**

Run: `cd frontend && npx vitest run`
Expected: todos los test files pasan.

Run: `cd frontend/apps/erp-web && npx tsc -b --pretty false`
Expected: sin salida.

- [ ] **Step 8: Lint completo**

Run: `cd frontend && npx eslint apps packages`
Expected: solo los warnings preexistentes ya conocidos (`AsignarRolDialog.tsx` fue reescrito en Task 1 y ya no tiene el `watch` que generaba warning — verificar; `IdentidadExternaForm.tsx`, `IconButton.test.tsx`, `IconButton.tsx` siguen con sus warnings/error preexistentes no relacionados a este plan). Ningún error nuevo.

- [ ] **Step 9: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/routes.tsx frontend/apps/erp-web/src/features/seguridad/pages/UsersPage.tsx frontend/apps/erp-web/src/features/seguridad/pages/UsersPage.test.tsx
git commit -m "$(cat <<'EOF'
feat(seguridad): enlazar Nuevo usuario a la página de creación completa

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Verificación end-to-end en el navegador

**Files:** ninguno nuevo.

- [ ] **Step 1: Confirmar que el backend local está corriendo**

Run: `curl -s http://localhost:8085/actuator/health`
Expected: `{"groups":["liveness","readiness"],"status":"UP"}`. Si no responde, no continuar con este paso — reportar y saltar a Step 5.

- [ ] **Step 2: Navegar a `/seguridad/usuarios`, iniciar sesión si hace falta, y hacer clic en "Nuevo usuario"**

Usar las herramientas de navegador disponibles. Verificar que navega a `/seguridad/usuarios/nuevo` (no abre modal) y que la página muestra: sección "Datos del usuario" con `UsuarioForm` (tipo de documento preseleccionado DNI), y sección con el checkbox "Asignar rol ahora" (desmarcado, sin el formulario de rol visible).

- [ ] **Step 3: Crear un usuario SIN marcar "Asignar rol ahora"**

Llenar número de documento y correo únicos, hacer clic en "Crear usuario". Verificar que navega a `/seguridad/usuarios/{id}` (detalle del usuario recién creado, sin roles asignados).

- [ ] **Step 4: Crear un usuario CON "Asignar rol ahora" marcado**

Repetir con otro correo/documento únicos, marcar el checkbox, verificar que aparece el bloque de `AsignacionRolForm` (select de Rol poblado, select de Tipo de ámbito), completar con un rol y ámbito GLOBAL (para evitar depender de estructura organizacional real), hacer clic en "Crear usuario". Verificar que navega al detalle del usuario y que en la sección "Roles asignados" aparece el rol recién asignado.

- [ ] **Step 5: Reportar resultado**

Si el backend local no estaba disponible (Step 1), indicarlo explícitamente y no reportar Steps 2-4 como verificados — decirlo así, no asumir éxito.

---

## Self-Review (completado durante la escritura de este plan)

**Cobertura del spec:**
- Extracción de `AsignacionRolForm` sin romper `AsignarRolDialog`/`UserDetailPage` → Task 1. ✓
- Botón "Nuevo usuario" con apariencia de botón pero como link → Task 2 (`buttonClassName`) + Task 4 (uso real). ✓
- Página completa con datos personales + bloque opcional de rol → Task 3. ✓
- Orquestación de 2 llamadas HTTP con manejo de fallo parcial (mensaje + reintento + enlace a detalle) → Task 3, casos de test 3 y 5. ✓
- Ruta nueva antes de `:userId` → Task 4. ✓
- `UsersPage` deja de tener modal de creación → Task 4. ✓
- Fuera de alcance (múltiples roles, campo de sucursal independiente, transacción de backend): no se tocan en ninguna tarea. ✓

**Escaneo de placeholders:** sin "TBD"/"TODO"; todo código mostrado es completo y compilable con imports reales verificados contra el código fuente vigente al momento de escribir este plan.

**Consistencia de tipos:** `AsignacionRolFormValues` (de `schemas/asignacion-rol.schema.ts`, sin cambios) se usa idéntico en `AsignacionRolForm` (Task 1) y `NewUserPage` (Task 3). `formId` se define en `AsignacionRolFormProps` (Task 1) y se consume con el mismo nombre en `NewUserPage` (Task 3, constante `ROLE_FORM_ID`). `buttonClassName()` se define en Task 2 y se usa con la misma firma (sin argumentos, variante `primary` por defecto) en Task 4.
