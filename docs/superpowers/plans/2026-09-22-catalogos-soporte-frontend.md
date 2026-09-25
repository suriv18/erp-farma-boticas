# Catálogos de Soporte en el Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrar en `frontend/apps/erp-web/src/features/catalogo/` los 6 catálogos de soporte que el backend ya expone vía REST (Rubro Comercial, Condición de Venta, Forma Farmacéutica, Vía de Administración, Unidad de Medida, Clasificación Controlada), reutilizando/extendiendo el patrón existente de Categoría/Marca.

**Architecture:** Rubro Comercial es tenant-scoped (como Marca) y se implementa como slice independiente calcando `marcas.api.ts`/`MarcaForm.tsx`/`MarcasPage.tsx`. Los otros 5 catálogos son globales por `codigo: string`, sin paginación server-side, y comparten exactamente la misma forma de contrato HTTP — se implementan sobre un módulo genérico nuevo `features/catalogo/support-catalog/` (factory de API, formulario dirigido por configuración, página genérica con paginación/búsqueda en cliente) para evitar 5 slices casi idénticos, cumpliendo la regla de `CLAUDE.md` de no duplicar código. Cada catálogo de soporte real es solo un archivo de configuración sobre ese módulo genérico.

**Tech Stack:** React 19.2.8, TanStack Query 5, react-hook-form + `@hookform/resolvers/zod` + zod, `@boticas/ui-web` (`DataTable`, `Modal`, `Card`, `Button`, `FormField`, `EstadoBadge`), `@boticas/api-client` (`ApiClient.get/post/put/patch`), MSW (mocks), Vitest + Testing Library.

## Global Constraints

- 100% de cobertura de tests en todo archivo nuevo o modificado (`CLAUDE.md` § Cobertura de tests) — cada rama de código debe tener un test que la ejercite.
- Prohibido código duplicado — patrones repetidos se extraen a abstracciones reutilizables (`CLAUDE.md` § Estándares de implementación). Por eso los 5 catálogos globales comparten `support-catalog/` en vez de 5 slices independientes.
- Sin comentarios explicativos en el código salvo decisiones no obvias.
- Un módulo/feature no importa archivos internos de otra feature, solo su `index.ts`.
- `pnpm check` (lint + typecheck + test + build) debe pasar en verde al final de cada tarea que toque código.
- En Windows PowerShell, si `pnpm.ps1` está bloqueado por policy, usar `pnpm.cmd`.
- Todas las rutas HTTP nuevas van bajo `/catalogo/<resource>` (el `apiClient` ya antepone `/api/v1`).
- Los DTOs de request del backend son exactos y ya confirmados contra el código Java real (ver cada tarea) — no inventar campos.

---

## File Structure

**Nuevos — módulo genérico `support-catalog/` (Task 1):**
- `features/catalogo/support-catalog/support-catalog.types.ts` — tipos genéricos `SupportCatalogItem`, `FieldDef`.
- `features/catalogo/support-catalog/support-catalog.api.ts` — factory `createSupportCatalogApi`.
- `features/catalogo/support-catalog/support-catalog.api.test.ts`
- `features/catalogo/support-catalog/SupportCatalogForm.tsx`
- `features/catalogo/support-catalog/SupportCatalogForm.test.tsx`
- `features/catalogo/support-catalog/SupportCatalogPage.tsx`
- `features/catalogo/support-catalog/SupportCatalogPage.test.tsx`
- `features/catalogo/support-catalog/index.ts`

**Nuevos — Rubro Comercial (Task 2):**
- `features/catalogo/api/rubros-comerciales.types.ts`
- `features/catalogo/api/rubros-comerciales.api.ts`
- `features/catalogo/api/rubros-comerciales.api.test.ts`
- `features/catalogo/schemas/rubro-comercial.schema.ts`
- `features/catalogo/components/RubroComercialForm.tsx`
- `features/catalogo/pages/RubrosComercialesPage.tsx`
- `features/catalogo/pages/RubrosComercialesPage.test.tsx`

**Nuevos — configs de catálogos de soporte (Tasks 3-7), uno por catálogo:**
- `features/catalogo/support-catalog/configs/condiciones-venta.config.ts` (+ `.test.ts`)
- `features/catalogo/support-catalog/configs/formas-farmaceuticas.config.ts` (+ `.test.ts`)
- `features/catalogo/support-catalog/configs/vias-administracion.config.ts` (+ `.test.ts`)
- `features/catalogo/support-catalog/configs/unidades-medida.config.ts` (+ `.test.ts`)
- `features/catalogo/support-catalog/configs/clasificaciones-controladas.config.ts` (+ `.test.ts`)
- Una página delgada por catálogo en `features/catalogo/pages/`: `CondicionesVentaPage.tsx`, `FormasFarmaceuticasPage.tsx`, `ViasAdministracionPage.tsx`, `UnidadesMedidaPage.tsx`, `ClasificacionesControladasPage.tsx` (cada una solo importa la config y `SupportCatalogPage`).

**Modificados (Task 8):**
- `features/catalogo/routes.tsx` — 6 rutas lazy nuevas.
- `features/catalogo/pages/CatalogPage.tsx` — 6 cards nuevas.
- `test/mocks/handlers.ts` — handlers de los 6 catálogos nuevos + handlers faltantes de categorías/marcas.
- `app/feature-routes.test.ts` — lista de paths esperados actualizada.

---

### Task 1: Módulo genérico `support-catalog/` (API factory + Form + Page)

**Files:**
- Create: `apps/erp-web/src/features/catalogo/support-catalog/support-catalog.types.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/support-catalog.api.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/support-catalog.api.test.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogForm.tsx`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogForm.test.tsx`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogPage.tsx`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogPage.test.tsx`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/index.ts`

**Interfaces:**
- Consumes: `ApiClient` de `@boticas/api-client`; `apiClient` singleton de `../../../app/api`; `queryOptions` de `@tanstack/react-query`; `Button`, `DataTable`, `EstadoBadge`, `FormField`, `Modal` de `@boticas/ui-web`; `zodResolver` de `@hookform/resolvers/zod`; `useForm` de `react-hook-form`; `Pagination` de `../components/Pagination`; `z.ZodType` de `zod`.
- Produces:
  - `SupportCatalogItem = { codigo: string; estado: string }` (tipo base, cada catálogo lo extiende).
  - `FieldDef = { name: string; label: string; type: 'text' | 'textarea' | 'number' | 'checkbox' }`.
  - `createSupportCatalogApi<TItem extends SupportCatalogItem, TRequest>(resource: string): SupportCatalogApi<TItem, TRequest>` con `SupportCatalogApi<TItem, TRequest> = { fetchList(client, estado?): Promise<TItem[]>; fetchOne(client, codigo): Promise<TItem>; create(client, payload: TRequest): Promise<TItem>; update(client, codigo, payload: TRequest): Promise<TItem>; changeStatus(client, codigo, status): Promise<void>; listQuery(estado?): UseQueryOptions }`.
  - `SupportCatalogForm<TValues extends Record<string, unknown>>({ fields, schema, defaultValues, onSubmit, submitLabel, isSubmitting }: SupportCatalogFormProps<TValues>)`.
  - `SupportCatalogPage<TItem extends SupportCatalogItem, TRequest>({ title, description, resourceLabel, api, fields, schema, columns, searchableFields, toRequest, toDefaultValues }: SupportCatalogPageProps<TItem, TRequest>)`.
  - Todos consumidos por Tasks 3-7 (cada `<slug>.config.ts` los instancia).

- [ ] **Step 1: Crear los tipos base**

Crear `apps/erp-web/src/features/catalogo/support-catalog/support-catalog.types.ts`:

```ts
export type SupportCatalogItem = {
  codigo: string;
  estado: string;
};

export type FieldDef = {
  name: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'checkbox';
};
```

- [ ] **Step 2: Escribir el test de la factory de API (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/support-catalog.api.test.ts`:

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { createSupportCatalogApi } from './support-catalog.api';
import type { SupportCatalogItem } from './support-catalog.types';

type SampleItem = SupportCatalogItem & { denominacion: string };
type SampleRequest = { codigo: string; denominacion: string };

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleItem: SampleItem = { codigo: 'ORAL', denominacion: 'Vía oral', estado: 'ACTIVO' };

describe('createSupportCatalogApi', () => {
  const api = createSupportCatalogApi<SampleItem, SampleRequest>('vias-administracion');

  it('fetchList consulta el recurso sin filtro de estado', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/vias-administracion', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleItem]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.fetchList(client);

    expect(receivedUrl?.searchParams.get('estado')).toBeNull();
    expect(result).toEqual([sampleItem]);
  });

  it('fetchList envia el filtro de estado cuando se especifica', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/vias-administracion', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleItem]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await api.fetchList(client, 'ACTIVO');

    expect(receivedUrl?.searchParams.get('estado')).toBe('ACTIVO');
  });

  it('fetchOne consulta el recurso por codigo', async () => {
    server.use(
      http.get('http://localhost/api/v1/catalogo/vias-administracion/ORAL', () =>
        HttpResponse.json(sampleItem)
      )
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.fetchOne(client, 'ORAL');

    expect(result).toEqual(sampleItem);
  });

  it('create envia POST con el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/catalogo/vias-administracion', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleItem, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.create(client, { codigo: 'ORAL', denominacion: 'Vía oral' });

    expect(receivedBody).toEqual({ codigo: 'ORAL', denominacion: 'Vía oral' });
    expect(result).toEqual(sampleItem);
  });

  it('update envia PUT con el payload al codigo indicado', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/catalogo/vias-administracion/ORAL', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleItem, denominacion: 'Vía oral estricta' });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.update(client, 'ORAL', { codigo: 'ORAL', denominacion: 'Vía oral estricta' });

    expect(receivedBody).toEqual({ codigo: 'ORAL', denominacion: 'Vía oral estricta' });
    expect(result.denominacion).toBe('Vía oral estricta');
  });

  it('changeStatus envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch('http://localhost/api/v1/catalogo/vias-administracion/ORAL/estado', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await api.changeStatus(client, 'ORAL', 'INACTIVO');

    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });

  it('listQuery arma una queryKey que incluye el resource y el estado', () => {
    const options = api.listQuery('ACTIVO');
    expect(options.queryKey).toEqual(['catalogo', 'vias-administracion', 'ACTIVO']);
  });
});
```

- [ ] **Step 3: Ejecutar el test para confirmar que falla**

Run (desde `frontend/`): `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/support-catalog.api.test.ts`
Expected: FAIL — `Cannot find module './support-catalog.api'`.

- [ ] **Step 4: Implementar `support-catalog.api.ts`**

Crear `apps/erp-web/src/features/catalogo/support-catalog/support-catalog.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { SupportCatalogItem } from './support-catalog.types';

export type SupportCatalogApi<TItem extends SupportCatalogItem, TRequest> = {
  fetchList: (client: ApiClient, estado?: string) => Promise<TItem[]>;
  fetchOne: (client: ApiClient, codigo: string) => Promise<TItem>;
  create: (client: ApiClient, payload: TRequest) => Promise<TItem>;
  update: (client: ApiClient, codigo: string, payload: TRequest) => Promise<TItem>;
  changeStatus: (client: ApiClient, codigo: string, status: string) => Promise<void>;
  listQuery: (estado?: string) => ReturnType<typeof queryOptions<TItem[]>>;
};

export function createSupportCatalogApi<TItem extends SupportCatalogItem, TRequest>(
  resource: string
): SupportCatalogApi<TItem, TRequest> {
  function fetchList(client: ApiClient, estado?: string): Promise<TItem[]> {
    const query = new URLSearchParams();
    if (estado) query.set('estado', estado);
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return client.get<TItem[]>(`/catalogo/${resource}${suffix}`);
  }

  function fetchOne(client: ApiClient, codigo: string): Promise<TItem> {
    return client.get<TItem>(`/catalogo/${resource}/${codigo}`);
  }

  function create(client: ApiClient, payload: TRequest): Promise<TItem> {
    return client.post<TItem, TRequest>(`/catalogo/${resource}`, payload);
  }

  function update(client: ApiClient, codigo: string, payload: TRequest): Promise<TItem> {
    return client.put<TItem, TRequest>(`/catalogo/${resource}/${codigo}`, payload);
  }

  function changeStatus(client: ApiClient, codigo: string, status: string): Promise<void> {
    return client.patch<void, { status: string }>(`/catalogo/${resource}/${codigo}/estado`, { status });
  }

  function listQuery(estado?: string) {
    return queryOptions({
      queryKey: ['catalogo', resource, estado ?? ''],
      queryFn: () => fetchList(apiClient, estado)
    });
  }

  return { fetchList, fetchOne, create, update, changeStatus, listQuery };
}
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/support-catalog.api.test.ts`
Expected: PASS — 7 tests.

- [ ] **Step 6: Escribir el test de `SupportCatalogForm` (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogForm.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { z } from 'zod';
import { SupportCatalogForm } from './SupportCatalogForm';
import type { FieldDef } from './support-catalog.types';

const fields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'notas', label: 'Notas', type: 'textarea' },
  { name: 'orden', label: 'Orden', type: 'number' },
  { name: 'activo', label: 'Activo', type: 'checkbox' }
];

const schema = z.object({
  codigo: z.string().min(1, 'Requerido'),
  denominacion: z.string().min(1, 'Requerido'),
  notas: z.string().optional(),
  orden: z.coerce.number().optional(),
  activo: z.boolean().optional()
});

type FormValues = z.infer<typeof schema>;

describe('SupportCatalogForm', () => {
  it('renderiza un input por cada FieldDef segun su type', () => {
    render(
      <SupportCatalogForm<FormValues>
        fields={fields}
        schema={schema}
        onSubmit={() => {}}
        submitLabel="Guardar"
      />
    );

    expect(screen.getByLabelText('Código')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Denominación')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Notas').tagName).toBe('TEXTAREA');
    expect(screen.getByLabelText('Orden')).toHaveAttribute('type', 'number');
    expect(screen.getByLabelText('Activo')).toHaveAttribute('type', 'checkbox');
  });

  it('muestra errores de validacion y no llama onSubmit si el schema falla', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <SupportCatalogForm<FormValues>
        fields={fields}
        schema={schema}
        onSubmit={onSubmit}
        submitLabel="Guardar"
      />
    );

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findAllByText('Requerido')).toHaveLength(2);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('llama onSubmit con los valores del formulario cuando es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(
      <SupportCatalogForm<FormValues>
        fields={fields}
        schema={schema}
        onSubmit={onSubmit}
        submitLabel="Guardar"
      />
    );

    await user.type(screen.getByLabelText('Código'), 'ORAL');
    await user.type(screen.getByLabelText('Denominación'), 'Vía oral');
    await user.click(screen.getByLabelText('Activo'));
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'ORAL', denominacion: 'Vía oral', activo: true })
    );
  });
});
```

- [ ] **Step 7: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/SupportCatalogForm.test.tsx`
Expected: FAIL — `Cannot find module './SupportCatalogForm'`.

- [ ] **Step 8: Implementar `SupportCatalogForm.tsx`**

Crear `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogForm.tsx`:

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, type FieldValues, type Path } from 'react-hook-form';
import type { z } from 'zod';
import { Button, FormField } from '@boticas/ui-web';
import type { FieldDef } from './support-catalog.types';

const inputClassName =
  'focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100';

export type SupportCatalogFormProps<TValues extends FieldValues> = {
  fields: FieldDef[];
  schema: z.ZodType<TValues>;
  defaultValues?: TValues;
  onSubmit: (values: TValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function SupportCatalogForm<TValues extends FieldValues>({
  fields,
  schema,
  defaultValues,
  onSubmit,
  submitLabel,
  isSubmitting = false
}: SupportCatalogFormProps<TValues>) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<TValues>({
    ...(defaultValues ? { defaultValues } : {}),
    mode: 'onTouched',
    resolver: zodResolver(schema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {fields.map((field) => {
        const fieldName = field.name as Path<TValues>;
        const fieldError = errors[field.name]?.message as string | undefined;
        const htmlId = `support-catalog-${field.name}`;

        if (field.type === 'checkbox') {
          return (
            <label
              key={field.name}
              htmlFor={htmlId}
              className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300"
            >
              <input
                id={htmlId}
                type="checkbox"
                className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
                {...register(fieldName)}
              />
              {field.label}
            </label>
          );
        }

        if (field.type === 'textarea') {
          return (
            <FormField key={field.name} label={field.label} htmlFor={htmlId} error={fieldError}>
              <textarea
                id={htmlId}
                rows={3}
                className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
                {...register(fieldName)}
              />
            </FormField>
          );
        }

        return (
          <FormField key={field.name} label={field.label} htmlFor={htmlId} error={fieldError}>
            <input
              id={htmlId}
              type={field.type === 'number' ? 'number' : 'text'}
              className={inputClassName}
              {...register(fieldName, field.type === 'number' ? { valueAsNumber: true } : {})}
            />
          </FormField>
        );
      })}

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 9: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/SupportCatalogForm.test.tsx`
Expected: PASS — 3 tests.

- [ ] **Step 10: Escribir el test de `SupportCatalogPage` (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { z } from 'zod';
import { server } from '../../../test/mocks/server';
import { createSupportCatalogApi } from './support-catalog.api';
import { SupportCatalogPage } from './SupportCatalogPage';
import type { FieldDef, SupportCatalogItem } from './support-catalog.types';

type SampleItem = SupportCatalogItem & { denominacion: string };
type SampleRequest = { codigo: string; denominacion: string };

const api = createSupportCatalogApi<SampleItem, SampleRequest>('vias-administracion');

const fields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' }
];

const schema = z.object({
  codigo: z.string().min(1, 'Requerido'),
  denominacion: z.string().min(1, 'Requerido')
});

function renderPage(pageSize = 20) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage<SampleItem, SampleRequest>
          title="Vías de administración"
          description="Administra las vías de administración del catálogo."
          resourceLabel="vía"
          api={api}
          fields={fields}
          schema={schema}
          columns={[
            { header: 'Código', cell: (row: SampleItem) => row.codigo },
            { header: 'Denominación', cell: (row: SampleItem) => row.denominacion }
          ]}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({ codigo: item.codigo, denominacion: item.denominacion })}
          pageSize={pageSize}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem: SampleItem = { codigo: 'ORAL', denominacion: 'Vía oral', estado: 'ACTIVO' };

describe('SupportCatalogPage', () => {
  it('lista los items del catalogo', async () => {
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () => HttpResponse.json([sampleItem]))
    );

    renderPage();

    expect(await screen.findByText('Vía oral')).toBeInTheDocument();
  });

  it('filtra en cliente por texto de busqueda', async () => {
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json([
          sampleItem,
          { codigo: 'IV', denominacion: 'Vía intravenosa', estado: 'ACTIVO' }
        ])
      )
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');
    expect(screen.getByText('Vía intravenosa')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/Buscar/), 'intraven');

    await waitFor(() => expect(screen.queryByText('Vía oral')).not.toBeInTheDocument());
    expect(screen.getByText('Vía intravenosa')).toBeInTheDocument();
  });

  it('pagina en cliente cuando hay mas items que el tamano de pagina', async () => {
    const items: SampleItem[] = Array.from({ length: 3 }, (_, index) => ({
      codigo: `COD-${index}`,
      denominacion: `Vía ${index}`,
      estado: 'ACTIVO'
    }));
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () => HttpResponse.json(items))
    );

    renderPage(2);

    await screen.findByText('Vía 0');
    expect(screen.getByText('Vía 1')).toBeInTheDocument();
    expect(screen.queryByText('Vía 2')).not.toBeInTheDocument();
  });

  it('crea un item nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/vias-administracion', () => {
        created = true;
        return HttpResponse.json(sampleItem, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'ORAL');
    await user.type(screen.getByLabelText('Denominación'), 'Vía oral');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Vía oral')).toBeInTheDocument());
  });

  it('edita un item existente', async () => {
    let currentDenominacion = 'Vía oral';
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json([{ ...sampleItem, denominacion: currentDenominacion }])
      ),
      http.put('*/api/v1/catalogo/vias-administracion/ORAL', async ({ request }) => {
        const body = (await request.json()) as SampleRequest;
        currentDenominacion = body.denominacion;
        return HttpResponse.json({ ...sampleItem, denominacion: currentDenominacion });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');

    await user.click(screen.getByRole('button', { name: 'Editar ORAL' }));
    const denominacionInput = screen.getByLabelText('Denominación');
    await user.clear(denominacionInput);
    await user.type(denominacionInput, 'Vía oral estricta');
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(screen.getByText('Vía oral estricta')).toBeInTheDocument());
  });

  it('cambia el estado de un item activo a inactivo', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/vias-administracion/ORAL/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');

    await user.click(screen.getByRole('button', { name: 'Desactivar ORAL' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });

  it('filtra por estado contra el backend', async () => {
    let receivedEstado: string | null = null;
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', ({ request }) => {
        receivedEstado = new URL(request.url).searchParams.get('estado');
        return HttpResponse.json(receivedEstado === 'INACTIVO' ? [] : [sampleItem]);
      })
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');

    await user.selectOptions(screen.getByLabelText('Estado'), 'INACTIVO');

    await waitFor(() => expect(receivedEstado).toBe('INACTIVO'));
    expect(screen.getByText('No se encontraron registros.')).toBeInTheDocument();
  });
});
```

- [ ] **Step 11: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/SupportCatalogPage.test.tsx`
Expected: FAIL — `Cannot find module './SupportCatalogPage'`.

- [ ] **Step 12: Implementar `SupportCatalogPage.tsx`**

Crear `apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogPage.tsx`:

```tsx
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FieldValues } from 'react-hook-form';
import type { z } from 'zod';
import { Button, DataTable, EstadoBadge, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { Pagination } from '../components/Pagination';
import { SupportCatalogForm } from './SupportCatalogForm';
import type { SupportCatalogApi } from './support-catalog.api';
import type { FieldDef, SupportCatalogItem } from './support-catalog.types';

export type SupportCatalogColumn<TItem> = {
  header: string;
  cell: (row: TItem) => React.ReactNode;
};

export type SupportCatalogPageProps<
  TItem extends SupportCatalogItem,
  TRequest,
  TValues extends FieldValues = TRequest & FieldValues
> = {
  title: string;
  description: string;
  resourceLabel: string;
  api: SupportCatalogApi<TItem, TRequest>;
  fields: FieldDef[];
  schema: z.ZodType<TValues>;
  columns: SupportCatalogColumn<TItem>[];
  searchableFields: (keyof TItem)[];
  toRequest: (values: TValues) => TRequest;
  toDefaultValues: (item: TItem) => TValues;
  pageSize?: number;
};

export function SupportCatalogPage<
  TItem extends SupportCatalogItem,
  TRequest,
  TValues extends FieldValues = TRequest & FieldValues
>({
  title,
  description,
  resourceLabel,
  api,
  fields,
  schema,
  columns,
  searchableFields,
  toRequest,
  toDefaultValues,
  pageSize = 20
}: SupportCatalogPageProps<TItem, TRequest, TValues>) {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<TItem | null>(null);
  const [search, setSearch] = useState('');
  const [estadoFilter, setEstadoFilter] = useState('');
  const [page, setPage] = useState(0);

  const { data, isPending, isError } = useQuery(api.listQuery(estadoFilter || undefined));

  const filtered = useMemo(() => {
    const items = data ?? [];
    if (!search) return items;
    const term = search.toLowerCase();
    return items.filter((item) =>
      searchableFields.some((field) => String(item[field]).toLowerCase().includes(term))
    );
  }, [data, search, searchableFields]);

  const paged = filtered.slice(page * pageSize, (page + 1) * pageSize);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo'] });

  const createMutation = useMutation({
    mutationFn: (values: TValues) => api.create(apiClient, toRequest(values)),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidate();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: TValues) => api.update(apiClient, editing!.codigo, toRequest(values)),
    onSuccess: () => {
      setEditing(null);
      void invalidate();
    }
  });

  const statusMutation = useMutation({
    mutationFn: (item: TItem) =>
      api.changeStatus(apiClient, item.codigo, item.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'),
    onSuccess: () => invalidate()
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Catálogo / {title}
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            {title}
          </h1>
          <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">{description}</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo {resourceLabel}</Button>
      </div>

      <div className="mt-6 flex flex-wrap items-end gap-4">
        <div>
          <label
            htmlFor="support-catalog-search"
            className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
          >
            Buscar {resourceLabel}
          </label>
          <input
            id="support-catalog-search"
            type="search"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Código o denominación"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 mt-2 h-11 w-full max-w-md rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          />
        </div>
        <div>
          <label
            htmlFor="support-catalog-estado"
            className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
          >
            Estado
          </label>
          <select
            id="support-catalog-estado"
            value={estadoFilter}
            onChange={(event) => {
              setEstadoFilter(event.target.value);
              setPage(0);
            }}
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 mt-2 h-11 rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          >
            <option value="">Todos</option>
            <option value="ACTIVO">Activo</option>
            <option value="INACTIVO">Inactivo</option>
          </select>
        </div>
      </div>

      <div className="mt-6">
        <DataTable<TItem>
          columns={[
            ...columns,
            { header: 'Estado', cell: (row: TItem) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row: TItem) => (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditing(row)}
                    className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  >
                    Editar {row.codigo}
                  </button>
                  <button
                    type="button"
                    onClick={() => statusMutation.mutate(row)}
                    className="font-semibold text-neutral-600 hover:underline dark:text-neutral-300"
                  >
                    {row.estado === 'ACTIVO' ? `Desactivar ${row.codigo}` : `Activar ${row.codigo}`}
                  </button>
                </div>
              )
            }
          ]}
          rows={paged}
          rowKey={(row) => row.codigo}
          emptyMessage="No se encontraron registros."
          isLoading={isPending}
          isError={isError}
          errorMessage={`No se pudo cargar el listado de ${resourceLabel}s.`}
          startIndex={page * pageSize}
        />
        <Pagination
          page={page}
          size={pageSize}
          totalElements={filtered.length}
          onPageChange={setPage}
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title={`Nuevo ${resourceLabel}`}>
        <SupportCatalogForm<TValues>
          fields={fields}
          schema={schema}
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear"
          isSubmitting={createMutation.isPending}
        />
      </Modal>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title={`Editar ${resourceLabel}`}>
        {editing ? (
          <SupportCatalogForm<TValues>
            fields={fields}
            schema={schema}
            defaultValues={toDefaultValues(editing)}
            onSubmit={(values) => updateMutation.mutate(values)}
            submitLabel="Guardar"
            isSubmitting={updateMutation.isPending}
          />
        ) : null}
      </Modal>
    </div>
  );
}
```

- [ ] **Step 13: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/SupportCatalogPage.test.tsx`
Expected: PASS — 7 tests.

- [ ] **Step 14: Crear el `index.ts` del módulo genérico**

Crear `apps/erp-web/src/features/catalogo/support-catalog/index.ts`:

```ts
export { createSupportCatalogApi } from './support-catalog.api';
export { SupportCatalogForm } from './SupportCatalogForm';
export { SupportCatalogPage } from './SupportCatalogPage';
export type { FieldDef, SupportCatalogItem } from './support-catalog.types';
export type { SupportCatalogApi } from './support-catalog.api';
export type { SupportCatalogColumn, SupportCatalogPageProps } from './SupportCatalogPage';
```

- [ ] **Step 15: Ejecutar toda la suite de `erp-web` para confirmar que nada se rompió**

Run: `pnpm vitest run --project erp-web`
Expected: PASS — todos los tests existentes más los nuevos de `support-catalog/`.

- [ ] **Step 16: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 17: Commit**

```bash
git add apps/erp-web/src/features/catalogo/support-catalog
git commit -m "feat(catalogo): agregar modulo generico support-catalog (api, form, page)"
```

---

### Task 2: Rubro Comercial (slice propio, tenant-scoped)

**Files:**
- Create: `apps/erp-web/src/features/catalogo/api/rubros-comerciales.types.ts`
- Create: `apps/erp-web/src/features/catalogo/api/rubros-comerciales.api.ts`
- Create: `apps/erp-web/src/features/catalogo/api/rubros-comerciales.api.test.ts`
- Create: `apps/erp-web/src/features/catalogo/schemas/rubro-comercial.schema.ts`
- Create: `apps/erp-web/src/features/catalogo/components/RubroComercialForm.tsx`
- Create: `apps/erp-web/src/features/catalogo/pages/RubrosComercialesPage.tsx`
- Create: `apps/erp-web/src/features/catalogo/pages/RubrosComercialesPage.test.tsx`

**Interfaces:**
- Consumes: mismo patrón exacto que `features/catalogo/api/marcas.api.ts`, `schemas/marca.schema.ts`, `components/MarcaForm.tsx`, `pages/MarcasPage.tsx`; `useAuthSession` de `../../auth`.
- Produces: `RubroComercial`, `CrearRubroComercialPayload`, `ActualizarRubroComercialPayload`, `fetchRubrosComerciales`, `rubrosComercialesQuery`, `crearRubroComercial`, `actualizarRubroComercial`, `cambiarEstadoRubroComercial`, `rubroComercialSchema`, `RubroComercialFormValues`, `RubroComercialForm`, `RubrosComercialesPage` — consumidos por Task 8 (rutas y `CatalogPage`).

- [ ] **Step 1: Crear los tipos**

Crear `apps/erp-web/src/features/catalogo/api/rubros-comerciales.types.ts`:

```ts
export type RubroComercial = {
  id: string;
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  esFarmaceutico: boolean;
  orden: number;
  estado: string;
};

export type CrearRubroComercialPayload = {
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
  esFarmaceutico: boolean;
  orden: number;
};

export type ActualizarRubroComercialPayload = {
  tenantId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | undefined;
  esFarmaceutico: boolean;
  orden: number;
};
```

- [ ] **Step 2: Escribir el test de la API (falla primero)**

Crear `apps/erp-web/src/features/catalogo/api/rubros-comerciales.api.test.ts`:

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  actualizarRubroComercial,
  cambiarEstadoRubroComercial,
  crearRubroComercial,
  fetchRubrosComerciales
} from './rubros-comerciales.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleRubro = {
  id: 'rubro-1',
  tenantId: 'tenant-1',
  codigo: 'FARMA',
  nombre: 'Farmacéutico',
  descripcion: null,
  esFarmaceutico: true,
  orden: 1,
  estado: 'ACTIVO'
};

describe('rubros-comerciales.api', () => {
  it('fetchRubrosComerciales consulta /catalogo/rubros-comerciales con tenantId, page y size por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/rubros-comerciales', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleRubro], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchRubrosComerciales(client, { tenantId: 'tenant-1' });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleRubro]);
  });

  it('fetchRubrosComerciales envia q, esFarmaceutico, page y size cuando se especifican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/rubros-comerciales', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleRubro], page: 1, size: 10, totalElements: 15 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchRubrosComerciales(client, {
      tenantId: 'tenant-1',
      q: 'farma',
      esFarmaceutico: true,
      page: 1,
      size: 10
    });

    expect(receivedUrl?.searchParams.get('q')).toBe('farma');
    expect(receivedUrl?.searchParams.get('esFarmaceutico')).toBe('true');
    expect(receivedUrl?.searchParams.get('page')).toBe('1');
    expect(result.totalElements).toBe(15);
  });

  it('crearRubroComercial envia el payload y devuelve el rubro creado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/catalogo/rubros-comerciales', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleRubro, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearRubroComercial(client, {
      tenantId: 'tenant-1',
      codigo: 'FARMA',
      nombre: 'Farmacéutico',
      esFarmaceutico: true,
      orden: 1
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      codigo: 'FARMA',
      nombre: 'Farmacéutico',
      esFarmaceutico: true,
      orden: 1
    });
    expect(result).toEqual(sampleRubro);
  });

  it('actualizarRubroComercial envia PUT con el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/catalogo/rubros-comerciales/rubro-1', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleRubro, nombre: 'Farmacéutico y afines' });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await actualizarRubroComercial(client, 'rubro-1', {
      tenantId: 'tenant-1',
      codigo: 'FARMA',
      nombre: 'Farmacéutico y afines',
      esFarmaceutico: true,
      orden: 1
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      codigo: 'FARMA',
      nombre: 'Farmacéutico y afines',
      esFarmaceutico: true,
      orden: 1
    });
    expect(result.nombre).toBe('Farmacéutico y afines');
  });

  it('cambiarEstadoRubroComercial envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch('http://localhost/api/v1/catalogo/rubros-comerciales/rubro-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoRubroComercial(client, 'rubro-1', 'tenant-1', 'INACTIVO');

    expect(receivedBody).toEqual({ tenantId: 'tenant-1', status: 'INACTIVO' });
  });
});
```

- [ ] **Step 3: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/api/rubros-comerciales.api.test.ts`
Expected: FAIL — `Cannot find module './rubros-comerciales.api'`.

- [ ] **Step 4: Implementar `rubros-comerciales.api.ts`**

Crear `apps/erp-web/src/features/catalogo/api/rubros-comerciales.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './marcas.types';
import type {
  ActualizarRubroComercialPayload,
  CrearRubroComercialPayload,
  RubroComercial
} from './rubros-comerciales.types';

export type FetchRubrosComercialesParams = {
  tenantId: string;
  q?: string | undefined;
  esFarmaceutico?: boolean | undefined;
  estado?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchRubrosComerciales(
  client: ApiClient,
  params: FetchRubrosComercialesParams
): Promise<PaginaResponse<RubroComercial>> {
  const query = new URLSearchParams({ tenantId: params.tenantId });
  if (params.q) query.set('q', params.q);
  if (params.esFarmaceutico !== undefined) query.set('esFarmaceutico', String(params.esFarmaceutico));
  if (params.estado) query.set('estado', params.estado);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<RubroComercial>>(`/catalogo/rubros-comerciales?${query.toString()}`);
}

export function rubrosComercialesQuery(params: FetchRubrosComercialesParams) {
  return queryOptions({
    queryKey: [
      'catalogo',
      'rubros-comerciales',
      params.tenantId,
      params.q ?? '',
      params.esFarmaceutico ?? '',
      params.estado ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchRubrosComerciales(apiClient, params)
  });
}

export function crearRubroComercial(
  client: ApiClient,
  payload: CrearRubroComercialPayload
): Promise<RubroComercial> {
  return client.post<RubroComercial, CrearRubroComercialPayload>('/catalogo/rubros-comerciales', payload);
}

export function actualizarRubroComercial(
  client: ApiClient,
  rubroComercialId: string,
  payload: ActualizarRubroComercialPayload
): Promise<RubroComercial> {
  return client.put<RubroComercial, ActualizarRubroComercialPayload>(
    `/catalogo/rubros-comerciales/${rubroComercialId}`,
    payload
  );
}

export function cambiarEstadoRubroComercial(
  client: ApiClient,
  rubroComercialId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { tenantId: string; status: string }>(
    `/catalogo/rubros-comerciales/${rubroComercialId}/estado`,
    { tenantId, status }
  );
}
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/api/rubros-comerciales.api.test.ts`
Expected: PASS — 5 tests.

- [ ] **Step 6: Crear el schema zod**

Crear `apps/erp-web/src/features/catalogo/schemas/rubro-comercial.schema.ts`:

```ts
import { z } from 'zod';

export const rubroComercialSchema = z.object({
  codigo: z
    .string()
    .min(2, 'El código debe tener al menos 2 caracteres.')
    .max(50, 'El código no debe exceder 50 caracteres.'),
  nombre: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(120, 'El nombre no debe exceder 120 caracteres.'),
  descripcion: z.string().max(300, 'La descripción no debe exceder 300 caracteres.').optional(),
  esFarmaceutico: z.boolean(),
  orden: z.coerce.number().int().min(0, 'El orden no puede ser negativo.')
});

export type RubroComercialFormValues = z.infer<typeof rubroComercialSchema>;
```

- [ ] **Step 7: Crear `RubroComercialForm.tsx`**

Crear `apps/erp-web/src/features/catalogo/components/RubroComercialForm.tsx`:

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button, FormField } from '@boticas/ui-web';
import { rubroComercialSchema, type RubroComercialFormValues } from '../schemas/rubro-comercial.schema';

export type RubroComercialFormProps = {
  defaultValues?: RubroComercialFormValues;
  onSubmit: (values: RubroComercialFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function RubroComercialForm({
  defaultValues,
  onSubmit,
  submitLabel,
  isSubmitting = false
}: RubroComercialFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<RubroComercialFormValues>({
    defaultValues: defaultValues ?? {
      codigo: '',
      nombre: '',
      descripcion: '',
      esFarmaceutico: false,
      orden: 0
    },
    mode: 'onTouched',
    resolver: zodResolver(rubroComercialSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormField label="Código" htmlFor="rubro-codigo" error={errors.codigo?.message}>
        <input
          id="rubro-codigo"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="rubro-nombre" error={errors.nombre?.message}>
        <input
          id="rubro-nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('nombre')}
        />
      </FormField>

      <FormField label="Descripción" htmlFor="rubro-descripcion" error={errors.descripcion?.message}>
        <textarea
          id="rubro-descripcion"
          rows={3}
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('descripcion')}
        />
      </FormField>

      <FormField label="Orden" htmlFor="rubro-orden" error={errors.orden?.message}>
        <input
          id="rubro-orden"
          type="number"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('orden', { valueAsNumber: true })}
        />
      </FormField>

      <label
        htmlFor="rubro-es-farmaceutico"
        className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300"
      >
        <input
          id="rubro-es-farmaceutico"
          type="checkbox"
          className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
          {...register('esFarmaceutico')}
        />
        Es farmacéutico
      </label>

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 8: Escribir el test de `RubrosComercialesPage` (falla primero)**

Crear `apps/erp-web/src/features/catalogo/pages/RubrosComercialesPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { RubrosComercialesPage } from './RubrosComercialesPage';

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
  const router = createMemoryRouter([{ path: '/', Component: RubrosComercialesPage }]);
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

const sampleRubro = {
  id: 'rubro-1',
  tenantId: 'tenant-1',
  codigo: 'FARMA',
  nombre: 'Farmacéutico',
  descripcion: null,
  esFarmaceutico: true,
  orden: 1,
  estado: 'ACTIVO'
};

function paginaResponse(items: unknown[], overrides: Partial<{ page: number; size: number; totalElements: number }> = {}) {
  return { items, page: 0, size: 20, totalElements: items.length, ...overrides };
}

describe('RubrosComercialesPage', () => {
  it('lista los rubros comerciales del tenant activo', async () => {
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () => HttpResponse.json(paginaResponse([sampleRubro])))
    );

    renderPage();

    expect(await screen.findByText('Farmacéutico')).toBeInTheDocument();
  });

  it('crea un rubro comercial nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse(created ? [sampleRubro] : []))
      ),
      http.post('*/api/v1/catalogo/rubros-comerciales', () => {
        created = true;
        return HttpResponse.json(sampleRubro, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron rubros comerciales.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo rubro comercial' }));
    await user.type(screen.getByLabelText('Código'), 'FARMA');
    await user.type(screen.getByLabelText('Nombre'), 'Farmacéutico');
    await user.click(screen.getByRole('button', { name: 'Crear rubro comercial' }));

    await waitFor(() => expect(screen.getByText('Farmacéutico')).toBeInTheDocument());
  });

  it('edita un rubro comercial existente', async () => {
    let currentNombre = 'Farmacéutico';
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse([{ ...sampleRubro, nombre: currentNombre }]))
      ),
      http.put('*/api/v1/catalogo/rubros-comerciales/rubro-1', async ({ request }) => {
        const body = (await request.json()) as { nombre: string };
        currentNombre = body.nombre;
        return HttpResponse.json({ ...sampleRubro, nombre: currentNombre });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Farmacéutico');

    await user.click(screen.getByRole('button', { name: 'Editar Farmacéutico' }));
    const nombreInput = screen.getByLabelText('Nombre');
    await user.clear(nombreInput);
    await user.type(nombreInput, 'Farmacéutico y afines');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.getByText('Farmacéutico y afines')).toBeInTheDocument());
  });

  it('cambia el estado de un rubro comercial activo a inactivo', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse([{ ...sampleRubro, estado: currentEstado }]))
      ),
      http.patch('*/api/v1/catalogo/rubros-comerciales/rubro-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Farmacéutico');

    await user.click(screen.getByRole('button', { name: 'Desactivar Farmacéutico' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
```

- [ ] **Step 9: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/pages/RubrosComercialesPage.test.tsx`
Expected: FAIL — `Cannot find module './RubrosComercialesPage'`.

- [ ] **Step 10: Implementar `RubrosComercialesPage.tsx`**

Crear `apps/erp-web/src/features/catalogo/pages/RubrosComercialesPage.tsx` (calca `MarcasPage.tsx` — ver `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx` como referencia exacta de estructura):

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, Modal } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import {
  actualizarRubroComercial,
  cambiarEstadoRubroComercial,
  crearRubroComercial,
  rubrosComercialesQuery
} from '../api/rubros-comerciales.api';
import type { RubroComercial } from '../api/rubros-comerciales.types';
import type { RubroComercialFormValues } from '../schemas/rubro-comercial.schema';
import { RubroComercialForm } from '../components/RubroComercialForm';
import { Pagination } from '../components/Pagination';

const PAGE_SIZE = 20;

export function RubrosComercialesPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<RubroComercial | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const { data, isPending, isError } = useQuery({
    ...rubrosComercialesQuery({ tenantId: tenantId ?? '', q: search || undefined, page, size: PAGE_SIZE }),
    enabled: Boolean(tenantId)
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo', 'rubros-comerciales'] });

  const createMutation = useMutation({
    mutationFn: (values: RubroComercialFormValues) =>
      crearRubroComercial(apiClient, {
        tenantId: tenantId ?? '',
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined,
        esFarmaceutico: values.esFarmaceutico,
        orden: values.orden
      }),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidate();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: RubroComercialFormValues) =>
      actualizarRubroComercial(apiClient, editing!.id, {
        tenantId: tenantId ?? '',
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined,
        esFarmaceutico: values.esFarmaceutico,
        orden: values.orden
      }),
    onSuccess: () => {
      setEditing(null);
      void invalidate();
    }
  });

  const statusMutation = useMutation({
    mutationFn: (rubro: RubroComercial) =>
      cambiarEstadoRubroComercial(
        apiClient,
        rubro.id,
        tenantId ?? '',
        rubro.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
      ),
    onSuccess: () => invalidate()
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Catálogo / Rubros comerciales
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            Rubros comerciales
          </h1>
          <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
            Administra los rubros comerciales del catálogo.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo rubro comercial</Button>
      </div>

      <div className="mt-6">
        <label
          htmlFor="rubros-search"
          className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
        >
          Buscar rubro comercial
        </label>
        <input
          id="rubros-search"
          type="search"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
          placeholder="Código o nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 mt-2 h-11 w-full max-w-md rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
        />
      </div>

      <div className="mt-6">
        <DataTable<RubroComercial>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Farmacéutico', cell: (row) => (row.esFarmaceutico ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditing(row)}
                    className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  >
                    Editar {row.nombre}
                  </button>
                  <button
                    type="button"
                    onClick={() => statusMutation.mutate(row)}
                    className="font-semibold text-neutral-600 hover:underline dark:text-neutral-300"
                  >
                    {row.estado === 'ACTIVO' ? `Desactivar ${row.nombre}` : `Activar ${row.nombre}`}
                  </button>
                </div>
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron rubros comerciales."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de rubros comerciales."
          startIndex={page * PAGE_SIZE}
        />
        <Pagination
          page={data?.page ?? page}
          size={data?.size ?? PAGE_SIZE}
          totalElements={data?.totalElements ?? 0}
          onPageChange={setPage}
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo rubro comercial">
        <RubroComercialForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear rubro comercial"
          isSubmitting={createMutation.isPending}
        />
      </Modal>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title="Editar rubro comercial">
        {editing ? (
          <RubroComercialForm
            defaultValues={{
              codigo: editing.codigo,
              nombre: editing.nombre,
              descripcion: editing.descripcion ?? '',
              esFarmaceutico: editing.esFarmaceutico,
              orden: editing.orden
            }}
            onSubmit={(values) => updateMutation.mutate(values)}
            submitLabel="Guardar cambios"
            isSubmitting={updateMutation.isPending}
          />
        ) : null}
      </Modal>
    </div>
  );
}
```

- [ ] **Step 11: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/pages/RubrosComercialesPage.test.tsx`
Expected: PASS — 4 tests.

- [ ] **Step 12: Ejecutar toda la suite de `erp-web`, typecheck y build**

Run: `pnpm vitest run --project erp-web && pnpm --filter @boticas/erp-web typecheck && pnpm --filter @boticas/erp-web build`
Expected: todo en verde.

- [ ] **Step 13: Commit**

```bash
git add apps/erp-web/src/features/catalogo/api/rubros-comerciales.api.ts apps/erp-web/src/features/catalogo/api/rubros-comerciales.types.ts apps/erp-web/src/features/catalogo/api/rubros-comerciales.api.test.ts apps/erp-web/src/features/catalogo/schemas/rubro-comercial.schema.ts apps/erp-web/src/features/catalogo/components/RubroComercialForm.tsx apps/erp-web/src/features/catalogo/pages/RubrosComercialesPage.tsx apps/erp-web/src/features/catalogo/pages/RubrosComercialesPage.test.tsx
git commit -m "feat(catalogo): agregar CRUD de Rubro Comercial en el frontend"
```

---

### Task 3: Config de Condición de Venta ✅ (completada — nota de implementación abajo)

**Files:**
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/condiciones-venta.config.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/condiciones-venta.config.test.tsx` (`.tsx`, no `.ts` — el archivo contiene JSX)
- Create: `apps/erp-web/src/features/catalogo/pages/CondicionesVentaPage.tsx`

> **Nota:** `SupportCatalogPage`/`SupportCatalogForm` (Task 1) divergieron del snippet de este plan durante su implementación real: la prop es `resolver: Resolver<TInput, unknown, TValues>` (de `react-hook-form`), no `schema: z.ZodType<TValues>`. Cada config debe exportar `<algo>Resolver = zodResolver(<algo>Schema)` y pasar `resolver={...}` a `SupportCatalogPage`, no `schema={...}`. Ajustar Tasks 4-7 igual.

**Interfaces:**
- Consumes: `createSupportCatalogApi`, `SupportCatalogPage`, `FieldDef` de `../index` (el módulo `support-catalog` de Task 1).
- Produces: `CondicionVenta`, `CondicionVentaRequest`, `condicionVentaSchema`, `condicionVentaFields`, `condicionVentaColumns`, `condicionesVentaApi`, `CondicionesVentaPage` — consumidos por Task 8 (rutas y `CatalogPage`).

Backend confirmado (`CondicionVentaRequest.java`): `codigo` (string, max 30), `denominacion` (string, min 2 max 200), `requiereReceta` (boolean), `requiereRetencion` (boolean), `fuente` (string, max 300, opcional), `versionFuente` (string, max 100, opcional), `vigenteDesde`/`vigenteHasta` (no se exponen en el formulario, ver spec). Response añade `estado`.

- [ ] **Step 1: Escribir el test de la config (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/condiciones-venta.config.test.ts`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  condicionVentaColumns,
  condicionVentaFields,
  condicionVentaSchema,
  condicionesVentaApi
} from './condiciones-venta.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Condiciones de venta"
          description="Administra las condiciones de venta del catálogo."
          resourceLabel="condición de venta"
          api={condicionesVentaApi}
          fields={condicionVentaFields}
          schema={condicionVentaSchema}
          columns={condicionVentaColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            requiereReceta: item.requiereReceta,
            requiereRetencion: item.requiereRetencion,
            fuente: item.fuente ?? '',
            versionFuente: item.versionFuente ?? ''
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = {
  codigo: 'VL',
  denominacion: 'Venta libre',
  requiereReceta: false,
  requiereRetencion: false,
  fuente: 'DIGEMID',
  versionFuente: '2026',
  vigenteDesde: null,
  vigenteHasta: null,
  estado: 'ACTIVO'
};

describe('condiciones-venta.config', () => {
  it('lista las condiciones de venta y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/condiciones-venta', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/condiciones-venta', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'VL');
    await user.type(screen.getByLabelText('Denominación'), 'Venta libre');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Venta libre')).toBeInTheDocument());
  });

  it('cambia el estado de una condicion de venta existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/condiciones-venta', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/condiciones-venta/VL/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Venta libre');

    await user.click(screen.getByRole('button', { name: 'Desactivar VL' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/condiciones-venta.config.test.ts`
Expected: FAIL — `Cannot find module './condiciones-venta.config'`.

- [ ] **Step 3: Implementar la config**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/condiciones-venta.config.ts`:

```ts
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type CondicionVenta = SupportCatalogItem & {
  denominacion: string;
  requiereReceta: boolean;
  requiereRetencion: boolean;
  fuente: string | null;
  versionFuente: string | null;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
};

export type CondicionVentaRequest = {
  codigo: string;
  denominacion: string;
  requiereReceta: boolean;
  requiereRetencion: boolean;
  fuente?: string | undefined;
  versionFuente?: string | undefined;
};

export const condicionVentaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  requiereReceta: z.boolean(),
  requiereRetencion: z.boolean(),
  fuente: z.string().max(300, 'Máximo 300 caracteres.').optional(),
  versionFuente: z.string().max(100, 'Máximo 100 caracteres.').optional()
});

export const condicionVentaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'requiereReceta', label: 'Requiere receta', type: 'checkbox' },
  { name: 'requiereRetencion', label: 'Requiere retención', type: 'checkbox' },
  { name: 'fuente', label: 'Fuente', type: 'text' },
  { name: 'versionFuente', label: 'Versión de fuente', type: 'text' }
];

export const condicionVentaColumns: SupportCatalogColumn<CondicionVenta>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Requiere receta', cell: (row) => (row.requiereReceta ? 'Sí' : 'No') }
];

export const condicionesVentaApi = createSupportCatalogApi<CondicionVenta, CondicionVentaRequest>(
  'condiciones-venta'
);
```

- [ ] **Step 4: Crear la página delgada**

Crear `apps/erp-web/src/features/catalogo/pages/CondicionesVentaPage.tsx`:

```tsx
import { SupportCatalogPage } from '../support-catalog';
import {
  condicionVentaColumns,
  condicionVentaFields,
  condicionVentaSchema,
  condicionesVentaApi
} from '../support-catalog/configs/condiciones-venta.config';

export function CondicionesVentaPage() {
  return (
    <SupportCatalogPage
      title="Condiciones de venta"
      description="Administra las condiciones de venta del catálogo."
      resourceLabel="condición de venta"
      api={condicionesVentaApi}
      fields={condicionVentaFields}
      schema={condicionVentaSchema}
      columns={condicionVentaColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        requiereReceta: item.requiereReceta,
        requiereRetencion: item.requiereRetencion,
        fuente: item.fuente ?? '',
        versionFuente: item.versionFuente ?? ''
      })}
    />
  );
}
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/condiciones-venta.config.test.ts`
Expected: PASS — 2 tests.

- [ ] **Step 6: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 7: Commit**

```bash
git add apps/erp-web/src/features/catalogo/support-catalog/configs/condiciones-venta.config.ts apps/erp-web/src/features/catalogo/support-catalog/configs/condiciones-venta.config.test.ts apps/erp-web/src/features/catalogo/pages/CondicionesVentaPage.tsx
git commit -m "feat(catalogo): agregar CRUD de Condicion de Venta en el frontend"
```

---

### Task 4: Config de Forma Farmacéutica ✅ (completada — aplica la misma nota de Task 3: `resolver`, no `schema`)

**Files:**
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.test.tsx` (`.tsx`, no `.ts`)
- Create: `apps/erp-web/src/features/catalogo/pages/FormasFarmaceuticasPage.tsx`

**Interfaces:**
- Consumes: igual que Task 3.
- Produces: `FormaFarmaceutica`, `FormaFarmaceuticaRequest`, `formaFarmaceuticaSchema`, `formaFarmaceuticaFields`, `formaFarmaceuticaColumns`, `formasFarmaceuticasApi`, `FormasFarmaceuticasPage` — consumidos por Task 8.

Backend confirmado (`FormaFarmaceuticaRequest.java`): `codigo` (string, max 30), `denominacion` (string, min 2 max 200), `fuente` (string, max 300, opcional).

- [ ] **Step 1: Escribir el test de la config (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.test.ts`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  formaFarmaceuticaColumns,
  formaFarmaceuticaFields,
  formaFarmaceuticaSchema,
  formasFarmaceuticasApi
} from './formas-farmaceuticas.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Formas farmacéuticas"
          description="Administra las formas farmacéuticas del catálogo."
          resourceLabel="forma farmacéutica"
          api={formasFarmaceuticasApi}
          fields={formaFarmaceuticaFields}
          schema={formaFarmaceuticaSchema}
          columns={formaFarmaceuticaColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            fuente: item.fuente ?? ''
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = { codigo: 'TAB', denominacion: 'Tableta', fuente: 'DIGEMID', estado: 'ACTIVO' };

describe('formas-farmaceuticas.config', () => {
  it('lista las formas farmaceuticas y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/formas-farmaceuticas', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/formas-farmaceuticas', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'TAB');
    await user.type(screen.getByLabelText('Denominación'), 'Tableta');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Tableta')).toBeInTheDocument());
  });

  it('cambia el estado de una forma farmaceutica existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/formas-farmaceuticas', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/formas-farmaceuticas/TAB/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Tableta');

    await user.click(screen.getByRole('button', { name: 'Desactivar TAB' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.test.ts`
Expected: FAIL — `Cannot find module './formas-farmaceuticas.config'`.

- [ ] **Step 3: Implementar la config**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.ts`:

```ts
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type FormaFarmaceutica = SupportCatalogItem & {
  denominacion: string;
  fuente: string | null;
};

export type FormaFarmaceuticaRequest = {
  codigo: string;
  denominacion: string;
  fuente?: string | undefined;
};

export const formaFarmaceuticaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  fuente: z.string().max(300, 'Máximo 300 caracteres.').optional()
});

export const formaFarmaceuticaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'fuente', label: 'Fuente', type: 'text' }
];

export const formaFarmaceuticaColumns: SupportCatalogColumn<FormaFarmaceutica>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Fuente', cell: (row) => row.fuente ?? '—' }
];

export const formasFarmaceuticasApi = createSupportCatalogApi<FormaFarmaceutica, FormaFarmaceuticaRequest>(
  'formas-farmaceuticas'
);
```

- [ ] **Step 4: Crear la página delgada**

Crear `apps/erp-web/src/features/catalogo/pages/FormasFarmaceuticasPage.tsx`:

```tsx
import { SupportCatalogPage } from '../support-catalog';
import {
  formaFarmaceuticaColumns,
  formaFarmaceuticaFields,
  formaFarmaceuticaSchema,
  formasFarmaceuticasApi
} from '../support-catalog/configs/formas-farmaceuticas.config';

export function FormasFarmaceuticasPage() {
  return (
    <SupportCatalogPage
      title="Formas farmacéuticas"
      description="Administra las formas farmacéuticas del catálogo."
      resourceLabel="forma farmacéutica"
      api={formasFarmaceuticasApi}
      fields={formaFarmaceuticaFields}
      schema={formaFarmaceuticaSchema}
      columns={formaFarmaceuticaColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        fuente: item.fuente ?? ''
      })}
    />
  );
}
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.test.ts`
Expected: PASS — 2 tests.

- [ ] **Step 6: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 7: Commit**

```bash
git add apps/erp-web/src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.ts apps/erp-web/src/features/catalogo/support-catalog/configs/formas-farmaceuticas.config.test.ts apps/erp-web/src/features/catalogo/pages/FormasFarmaceuticasPage.tsx
git commit -m "feat(catalogo): agregar CRUD de Forma Farmaceutica en el frontend"
```

---

### Task 5: Config de Vía de Administración

**Files:**
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/vias-administracion.config.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/vias-administracion.config.test.ts`
- Create: `apps/erp-web/src/features/catalogo/pages/ViasAdministracionPage.tsx`

**Interfaces:**
- Consumes: igual que Task 3.
- Produces: `ViaAdministracion`, `ViaAdministracionRequest`, `viaAdministracionSchema`, `viaAdministracionFields`, `viaAdministracionColumns`, `viasAdministracionApi`, `ViasAdministracionPage` — consumidos por Task 8.

Backend confirmado (`ViaAdministracionRequest.java`): idéntico en forma a `FormaFarmaceuticaRequest` — `codigo` (max 30), `denominacion` (min 2 max 200), `fuente` (max 300, opcional).

- [ ] **Step 1: Escribir el test de la config (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/vias-administracion.config.test.ts`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  viaAdministracionColumns,
  viaAdministracionFields,
  viaAdministracionSchema,
  viasAdministracionApi
} from './vias-administracion.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Vías de administración"
          description="Administra las vías de administración del catálogo."
          resourceLabel="vía de administración"
          api={viasAdministracionApi}
          fields={viaAdministracionFields}
          schema={viaAdministracionSchema}
          columns={viaAdministracionColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            fuente: item.fuente ?? ''
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = { codigo: 'ORAL', denominacion: 'Vía oral', fuente: 'DIGEMID', estado: 'ACTIVO' };

describe('vias-administracion.config', () => {
  it('lista las vias de administracion y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/vias-administracion', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'ORAL');
    await user.type(screen.getByLabelText('Denominación'), 'Vía oral');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Vía oral')).toBeInTheDocument());
  });

  it('cambia el estado de una via de administracion existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/vias-administracion/ORAL/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');

    await user.click(screen.getByRole('button', { name: 'Desactivar ORAL' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/vias-administracion.config.test.ts`
Expected: FAIL — `Cannot find module './vias-administracion.config'`.

- [ ] **Step 3: Implementar la config**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/vias-administracion.config.ts`:

```ts
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type ViaAdministracion = SupportCatalogItem & {
  denominacion: string;
  fuente: string | null;
};

export type ViaAdministracionRequest = {
  codigo: string;
  denominacion: string;
  fuente?: string | undefined;
};

export const viaAdministracionSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  fuente: z.string().max(300, 'Máximo 300 caracteres.').optional()
});

export const viaAdministracionFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'fuente', label: 'Fuente', type: 'text' }
];

export const viaAdministracionColumns: SupportCatalogColumn<ViaAdministracion>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Fuente', cell: (row) => row.fuente ?? '—' }
];

export const viasAdministracionApi = createSupportCatalogApi<ViaAdministracion, ViaAdministracionRequest>(
  'vias-administracion'
);
```

- [ ] **Step 4: Crear la página delgada**

Crear `apps/erp-web/src/features/catalogo/pages/ViasAdministracionPage.tsx`:

```tsx
import { SupportCatalogPage } from '../support-catalog';
import {
  viaAdministracionColumns,
  viaAdministracionFields,
  viaAdministracionSchema,
  viasAdministracionApi
} from '../support-catalog/configs/vias-administracion.config';

export function ViasAdministracionPage() {
  return (
    <SupportCatalogPage
      title="Vías de administración"
      description="Administra las vías de administración del catálogo."
      resourceLabel="vía de administración"
      api={viasAdministracionApi}
      fields={viaAdministracionFields}
      schema={viaAdministracionSchema}
      columns={viaAdministracionColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        fuente: item.fuente ?? ''
      })}
    />
  );
}
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/vias-administracion.config.test.ts`
Expected: PASS — 2 tests.

- [ ] **Step 6: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 7: Commit**

```bash
git add apps/erp-web/src/features/catalogo/support-catalog/configs/vias-administracion.config.ts apps/erp-web/src/features/catalogo/support-catalog/configs/vias-administracion.config.test.ts apps/erp-web/src/features/catalogo/pages/ViasAdministracionPage.tsx
git commit -m "feat(catalogo): agregar CRUD de Via de Administracion en el frontend"
```

---

### Task 6: Config de Unidad de Medida

**Files:**
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/unidades-medida.config.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/unidades-medida.config.test.ts`
- Create: `apps/erp-web/src/features/catalogo/pages/UnidadesMedidaPage.tsx`

**Interfaces:**
- Consumes: igual que Task 3.
- Produces: `UnidadMedida`, `UnidadMedidaRequest`, `unidadMedidaSchema`, `unidadMedidaFields`, `unidadMedidaColumns`, `unidadesMedidaApi`, `UnidadesMedidaPage` — consumidos por Task 8.

Backend confirmado (`UnidadMedidaRequest.java`): `codigo` (max 30), `denominacion` (min 2 max 150), `simbolo` (max 30, opcional), `permiteDecimal` (boolean), `fuente` (max 300, opcional).

- [ ] **Step 1: Escribir el test de la config (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/unidades-medida.config.test.ts`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  unidadMedidaColumns,
  unidadMedidaFields,
  unidadMedidaSchema,
  unidadesMedidaApi
} from './unidades-medida.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Unidades de medida"
          description="Administra las unidades de medida del catálogo."
          resourceLabel="unidad de medida"
          api={unidadesMedidaApi}
          fields={unidadMedidaFields}
          schema={unidadMedidaSchema}
          columns={unidadMedidaColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            simbolo: item.simbolo ?? '',
            permiteDecimal: item.permiteDecimal,
            fuente: item.fuente ?? ''
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = {
  codigo: 'UND',
  denominacion: 'Unidad',
  simbolo: 'u',
  permiteDecimal: false,
  fuente: 'DIGEMID',
  estado: 'ACTIVO'
};

describe('unidades-medida.config', () => {
  it('lista las unidades de medida y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/unidades-medida', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/unidades-medida', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'UND');
    await user.type(screen.getByLabelText('Denominación'), 'Unidad');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Unidad')).toBeInTheDocument());
  });

  it('cambia el estado de una unidad de medida existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/unidades-medida', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/unidades-medida/UND/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Unidad');

    await user.click(screen.getByRole('button', { name: 'Desactivar UND' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/unidades-medida.config.test.ts`
Expected: FAIL — `Cannot find module './unidades-medida.config'`.

- [ ] **Step 3: Implementar la config**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/unidades-medida.config.ts`:

```ts
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type UnidadMedida = SupportCatalogItem & {
  denominacion: string;
  simbolo: string | null;
  permiteDecimal: boolean;
  fuente: string | null;
};

export type UnidadMedidaRequest = {
  codigo: string;
  denominacion: string;
  simbolo?: string | undefined;
  permiteDecimal: boolean;
  fuente?: string | undefined;
};

export const unidadMedidaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(150, 'Máximo 150 caracteres.'),
  simbolo: z.string().max(30, 'Máximo 30 caracteres.').optional(),
  permiteDecimal: z.boolean(),
  fuente: z.string().max(300, 'Máximo 300 caracteres.').optional()
});

export const unidadMedidaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'simbolo', label: 'Símbolo', type: 'text' },
  { name: 'permiteDecimal', label: 'Permite decimal', type: 'checkbox' },
  { name: 'fuente', label: 'Fuente', type: 'text' }
];

export const unidadMedidaColumns: SupportCatalogColumn<UnidadMedida>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Símbolo', cell: (row) => row.simbolo ?? '—' },
  { header: 'Permite decimal', cell: (row) => (row.permiteDecimal ? 'Sí' : 'No') }
];

export const unidadesMedidaApi = createSupportCatalogApi<UnidadMedida, UnidadMedidaRequest>(
  'unidades-medida'
);
```

- [ ] **Step 4: Crear la página delgada**

Crear `apps/erp-web/src/features/catalogo/pages/UnidadesMedidaPage.tsx`:

```tsx
import { SupportCatalogPage } from '../support-catalog';
import {
  unidadMedidaColumns,
  unidadMedidaFields,
  unidadMedidaSchema,
  unidadesMedidaApi
} from '../support-catalog/configs/unidades-medida.config';

export function UnidadesMedidaPage() {
  return (
    <SupportCatalogPage
      title="Unidades de medida"
      description="Administra las unidades de medida del catálogo."
      resourceLabel="unidad de medida"
      api={unidadesMedidaApi}
      fields={unidadMedidaFields}
      schema={unidadMedidaSchema}
      columns={unidadMedidaColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        simbolo: item.simbolo ?? '',
        permiteDecimal: item.permiteDecimal,
        fuente: item.fuente ?? ''
      })}
    />
  );
}
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/unidades-medida.config.test.ts`
Expected: PASS — 2 tests.

- [ ] **Step 6: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 7: Commit**

```bash
git add apps/erp-web/src/features/catalogo/support-catalog/configs/unidades-medida.config.ts apps/erp-web/src/features/catalogo/support-catalog/configs/unidades-medida.config.test.ts apps/erp-web/src/features/catalogo/pages/UnidadesMedidaPage.tsx
git commit -m "feat(catalogo): agregar CRUD de Unidad de Medida en el frontend"
```

---

### Task 7: Config de Clasificación Controlada

**Files:**
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.ts`
- Create: `apps/erp-web/src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.test.ts`
- Create: `apps/erp-web/src/features/catalogo/pages/ClasificacionesControladasPage.tsx`

**Interfaces:**
- Consumes: igual que Task 3.
- Produces: `ClasificacionControlada`, `ClasificacionControladaRequest`, `clasificacionControladaSchema`, `clasificacionControladaFields`, `clasificacionControladaColumns`, `clasificacionesControladasApi`, `ClasificacionesControladasPage` — consumidos por Task 8.

Backend confirmado (`ClasificacionControladaRequest.java`): `codigo` (max 40), `denominacion` (min 2 max 200), `normaFuente` (max 300, opcional), `requiereRecetaEspecial` (boolean), `retieneReceta` (boolean), `vigenciaRecetaDias` (Integer, opcional).

- [ ] **Step 1: Escribir el test de la config (falla primero)**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.test.ts`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  clasificacionControladaColumns,
  clasificacionControladaFields,
  clasificacionControladaSchema,
  clasificacionesControladasApi
} from './clasificaciones-controladas.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Clasificaciones controladas"
          description="Administra las clasificaciones controladas del catálogo."
          resourceLabel="clasificación controlada"
          api={clasificacionesControladasApi}
          fields={clasificacionControladaFields}
          schema={clasificacionControladaSchema}
          columns={clasificacionControladaColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            normaFuente: item.normaFuente ?? '',
            requiereRecetaEspecial: item.requiereRecetaEspecial,
            retieneReceta: item.retieneReceta,
            vigenciaRecetaDias: item.vigenciaRecetaDias ?? undefined
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = {
  codigo: 'IIA',
  denominacion: 'Lista II-A',
  normaFuente: 'DS 023-2001-SA',
  requiereRecetaEspecial: true,
  retieneReceta: true,
  vigenciaRecetaDias: 30,
  estado: 'ACTIVO'
};

describe('clasificaciones-controladas.config', () => {
  it('lista las clasificaciones controladas y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/clasificaciones-controladas', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/clasificaciones-controladas', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'IIA');
    await user.type(screen.getByLabelText('Denominación'), 'Lista II-A');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Lista II-A')).toBeInTheDocument());
  });

  it('cambia el estado de una clasificacion controlada existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/clasificaciones-controladas', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/clasificaciones-controladas/IIA/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Lista II-A');

    await user.click(screen.getByRole('button', { name: 'Desactivar IIA' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.test.ts`
Expected: FAIL — `Cannot find module './clasificaciones-controladas.config'`.

- [ ] **Step 3: Implementar la config**

Crear `apps/erp-web/src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.ts`:

```ts
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type ClasificacionControlada = SupportCatalogItem & {
  denominacion: string;
  normaFuente: string | null;
  requiereRecetaEspecial: boolean;
  retieneReceta: boolean;
  vigenciaRecetaDias: number | null;
};

export type ClasificacionControladaRequest = {
  codigo: string;
  denominacion: string;
  normaFuente?: string | undefined;
  requiereRecetaEspecial: boolean;
  retieneReceta: boolean;
  vigenciaRecetaDias?: number | undefined;
};

export const clasificacionControladaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(40, 'Máximo 40 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  normaFuente: z.string().max(300, 'Máximo 300 caracteres.').optional(),
  requiereRecetaEspecial: z.boolean(),
  retieneReceta: z.boolean(),
  vigenciaRecetaDias: z.coerce.number().int().positive('Debe ser un número positivo.').optional()
});

export const clasificacionControladaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'normaFuente', label: 'Norma fuente', type: 'text' },
  { name: 'requiereRecetaEspecial', label: 'Requiere receta especial', type: 'checkbox' },
  { name: 'retieneReceta', label: 'Retiene receta', type: 'checkbox' },
  { name: 'vigenciaRecetaDias', label: 'Vigencia de receta (días)', type: 'number' }
];

export const clasificacionControladaColumns: SupportCatalogColumn<ClasificacionControlada>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Requiere receta especial', cell: (row) => (row.requiereRecetaEspecial ? 'Sí' : 'No') }
];

export const clasificacionesControladasApi = createSupportCatalogApi<
  ClasificacionControlada,
  ClasificacionControladaRequest
>('clasificaciones-controladas');
```

- [ ] **Step 4: Crear la página delgada**

Crear `apps/erp-web/src/features/catalogo/pages/ClasificacionesControladasPage.tsx`:

```tsx
import { SupportCatalogPage } from '../support-catalog';
import {
  clasificacionControladaColumns,
  clasificacionControladaFields,
  clasificacionControladaSchema,
  clasificacionesControladasApi
} from '../support-catalog/configs/clasificaciones-controladas.config';

export function ClasificacionesControladasPage() {
  return (
    <SupportCatalogPage
      title="Clasificaciones controladas"
      description="Administra las clasificaciones controladas del catálogo."
      resourceLabel="clasificación controlada"
      api={clasificacionesControladasApi}
      fields={clasificacionControladaFields}
      schema={clasificacionControladaSchema}
      columns={clasificacionControladaColumns}
      searchableFields={['codigo', 'denominacion']}
      toRequest={(values) => values}
      toDefaultValues={(item) => ({
        codigo: item.codigo,
        denominacion: item.denominacion,
        normaFuente: item.normaFuente ?? '',
        requiereRecetaEspecial: item.requiereRecetaEspecial,
        retieneReceta: item.retieneReceta,
        vigenciaRecetaDias: item.vigenciaRecetaDias ?? undefined
      })}
    />
  );
}
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project erp-web -- src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.test.ts`
Expected: PASS — 2 tests.

- [ ] **Step 6: Ejecutar toda la suite de `erp-web`, typecheck y build**

Run: `pnpm vitest run --project erp-web && pnpm --filter @boticas/erp-web typecheck && pnpm --filter @boticas/erp-web build`
Expected: todo en verde.

- [ ] **Step 7: Commit**

```bash
git add apps/erp-web/src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.ts apps/erp-web/src/features/catalogo/support-catalog/configs/clasificaciones-controladas.config.test.ts apps/erp-web/src/features/catalogo/pages/ClasificacionesControladasPage.tsx
git commit -m "feat(catalogo): agregar CRUD de Clasificacion Controlada en el frontend"
```

---

### Task 8: Rutas, `CatalogPage` y mocks MSW

**Files:**
- Modify: `apps/erp-web/src/features/catalogo/routes.tsx`
- Modify: `apps/erp-web/src/features/catalogo/pages/CatalogPage.tsx`
- Modify: `apps/erp-web/src/test/mocks/handlers.ts`
- Modify: `apps/erp-web/src/app/feature-routes.test.ts`

**Interfaces:**
- Consumes: `RubrosComercialesPage` (Task 2), `CondicionesVentaPage` (Task 3), `FormasFarmaceuticasPage` (Task 4), `ViasAdministracionPage` (Task 5), `UnidadesMedidaPage` (Task 6), `ClasificacionesControladasPage` (Task 7).

- [ ] **Step 1: Agregar las 6 rutas nuevas**

Editar `apps/erp-web/src/features/catalogo/routes.tsx`, agregando después de la ruta `catalogo/categorias`:

```tsx
import type { RouteObject } from 'react-router';

export const catalogRoutes = [
  {
    path: 'catalogo',
    lazy: async () => {
      const { CatalogPage } = await import('./pages/CatalogPage');
      return { Component: CatalogPage };
    }
  },
  {
    path: 'catalogo/marcas',
    lazy: async () => {
      const { MarcasPage } = await import('./pages/MarcasPage');
      return { Component: MarcasPage };
    }
  },
  {
    path: 'catalogo/categorias',
    lazy: async () => {
      const { CategoriasPage } = await import('./pages/CategoriasPage');
      return { Component: CategoriasPage };
    }
  },
  {
    path: 'catalogo/rubros-comerciales',
    lazy: async () => {
      const { RubrosComercialesPage } = await import('./pages/RubrosComercialesPage');
      return { Component: RubrosComercialesPage };
    }
  },
  {
    path: 'catalogo/condiciones-venta',
    lazy: async () => {
      const { CondicionesVentaPage } = await import('./pages/CondicionesVentaPage');
      return { Component: CondicionesVentaPage };
    }
  },
  {
    path: 'catalogo/formas-farmaceuticas',
    lazy: async () => {
      const { FormasFarmaceuticasPage } = await import('./pages/FormasFarmaceuticasPage');
      return { Component: FormasFarmaceuticasPage };
    }
  },
  {
    path: 'catalogo/vias-administracion',
    lazy: async () => {
      const { ViasAdministracionPage } = await import('./pages/ViasAdministracionPage');
      return { Component: ViasAdministracionPage };
    }
  },
  {
    path: 'catalogo/unidades-medida',
    lazy: async () => {
      const { UnidadesMedidaPage } = await import('./pages/UnidadesMedidaPage');
      return { Component: UnidadesMedidaPage };
    }
  },
  {
    path: 'catalogo/clasificaciones-controladas',
    lazy: async () => {
      const { ClasificacionesControladasPage } = await import('./pages/ClasificacionesControladasPage');
      return { Component: ClasificacionesControladasPage };
    }
  }
] satisfies RouteObject[];
```

- [ ] **Step 2: Agregar las 6 cards a `CatalogPage`**

Editar `apps/erp-web/src/features/catalogo/pages/CatalogPage.tsx`:

```tsx
import {
  Beaker,
  ClipboardList,
  FolderTree,
  Pill,
  Ruler,
  ShieldAlert,
  Store,
  Syringe,
  Tag
} from 'lucide-react';
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

const sections = [
  {
    to: '/catalogo/marcas',
    icon: Tag,
    title: 'Marcas',
    description: 'Administra las marcas comerciales del catálogo.'
  },
  {
    to: '/catalogo/categorias',
    icon: FolderTree,
    title: 'Categorías',
    description: 'Administra las categorías de productos del catálogo.'
  },
  {
    to: '/catalogo/rubros-comerciales',
    icon: Store,
    title: 'Rubros comerciales',
    description: 'Administra los rubros comerciales del catálogo.'
  },
  {
    to: '/catalogo/condiciones-venta',
    icon: ClipboardList,
    title: 'Condiciones de venta',
    description: 'Administra las condiciones de venta del catálogo.'
  },
  {
    to: '/catalogo/formas-farmaceuticas',
    icon: Pill,
    title: 'Formas farmacéuticas',
    description: 'Administra las formas farmacéuticas del catálogo.'
  },
  {
    to: '/catalogo/vias-administracion',
    icon: Syringe,
    title: 'Vías de administración',
    description: 'Administra las vías de administración del catálogo.'
  },
  {
    to: '/catalogo/unidades-medida',
    icon: Ruler,
    title: 'Unidades de medida',
    description: 'Administra las unidades de medida del catálogo.'
  },
  {
    to: '/catalogo/clasificaciones-controladas',
    icon: ShieldAlert,
    title: 'Clasificaciones controladas',
    description: 'Administra las clasificaciones controladas del catálogo.'
  }
];

export function CatalogPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">Módulo ERP</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
        Catálogo
      </h1>
      <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
        Productos, categorías, marcas y catálogos de soporte regulatorio.
      </p>

      <div className="mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {sections.map(({ to, icon: Icon, title, description }) => (
          <Link key={to} to={to} className="block focus-visible:outline-none">
            <Card className="hover:border-primary-300 hover:bg-primary-50/40 dark:hover:border-primary-700 dark:hover:bg-primary-900/20 h-full p-6 transition-colors">
              <div className="bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300 grid size-11 place-items-center rounded-xl">
                <Icon className="size-5" aria-hidden="true" />
              </div>
              <h2 className="mt-4 text-lg font-bold text-neutral-900 dark:text-neutral-50">
                {title}
              </h2>
              <p className="mt-1 text-sm leading-6 text-neutral-500 dark:text-neutral-400">
                {description}
              </p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
```

**Nota:** `CatalogPage.test.tsx` ya existente (`apps/erp-web/src/features/catalogo/pages/CatalogPage.test.tsx`) solo verifica los links a Marcas y Categorías por su `href`, no la cantidad total de cards — sigue pasando sin cambios tras agregar las 6 secciones nuevas.

- [ ] **Step 3: Agregar los handlers MSW faltantes**

Editar `apps/erp-web/src/test/mocks/handlers.ts`, agregando estos handlers al array `handlers` (después del handler de `/estructura-corporativa`, antes del cierre `];`):

```ts
  http.get('*/api/v1/catalogo/marcas', () =>
    HttpResponse.json({
      items: [
        { id: 'marca-1', tenantId: '11111111-1111-1111-1111-111111111111', codigo: 'BAYER', nombre: 'Bayer', descripcion: null, estado: 'ACTIVO' }
      ],
      page: 0,
      size: 20,
      totalElements: 1
    })
  ),
  http.get('*/api/v1/catalogo/categorias', () =>
    HttpResponse.json({
      items: [
        {
          id: 'categoria-1',
          tenantId: '11111111-1111-1111-1111-111111111111',
          categoriaPadreId: null,
          codigo: 'ANALGESICOS',
          nombre: 'Analgésicos',
          descripcion: null,
          nivel: 1,
          orden: 1,
          estado: 'ACTIVO'
        }
      ],
      page: 0,
      size: 20,
      totalElements: 1
    })
  ),
  http.get('*/api/v1/catalogo/rubros-comerciales', () =>
    HttpResponse.json({
      items: [
        {
          id: 'rubro-1',
          tenantId: '11111111-1111-1111-1111-111111111111',
          codigo: 'FARMA',
          nombre: 'Farmacéutico',
          descripcion: null,
          esFarmaceutico: true,
          orden: 1,
          estado: 'ACTIVO'
        }
      ],
      page: 0,
      size: 20,
      totalElements: 1
    })
  ),
  http.get('*/api/v1/catalogo/condiciones-venta', () =>
    HttpResponse.json([
      {
        codigo: 'VL',
        denominacion: 'Venta libre',
        requiereReceta: false,
        requiereRetencion: false,
        fuente: 'DIGEMID',
        versionFuente: '2026',
        vigenteDesde: null,
        vigenteHasta: null,
        estado: 'ACTIVO'
      },
      {
        codigo: 'RM',
        denominacion: 'Con receta médica',
        requiereReceta: true,
        requiereRetencion: false,
        fuente: 'DIGEMID',
        versionFuente: '2026',
        vigenteDesde: null,
        vigenteHasta: null,
        estado: 'ACTIVO'
      }
    ])
  ),
  http.get('*/api/v1/catalogo/formas-farmaceuticas', () =>
    HttpResponse.json([
      { codigo: 'TAB', denominacion: 'Tableta', fuente: 'DIGEMID', estado: 'ACTIVO' },
      { codigo: 'JBE', denominacion: 'Jarabe', fuente: 'DIGEMID', estado: 'ACTIVO' }
    ])
  ),
  http.get('*/api/v1/catalogo/vias-administracion', () =>
    HttpResponse.json([
      { codigo: 'ORAL', denominacion: 'Vía oral', fuente: 'DIGEMID', estado: 'ACTIVO' },
      { codigo: 'IV', denominacion: 'Vía intravenosa', fuente: 'DIGEMID', estado: 'ACTIVO' }
    ])
  ),
  http.get('*/api/v1/catalogo/unidades-medida', () =>
    HttpResponse.json([
      { codigo: 'UND', denominacion: 'Unidad', simbolo: 'u', permiteDecimal: false, fuente: 'DIGEMID', estado: 'ACTIVO' },
      { codigo: 'MG', denominacion: 'Miligramo', simbolo: 'mg', permiteDecimal: true, fuente: 'DIGEMID', estado: 'ACTIVO' }
    ])
  ),
  http.get('*/api/v1/catalogo/clasificaciones-controladas', () =>
    HttpResponse.json([
      {
        codigo: 'IIA',
        denominacion: 'Lista II-A',
        normaFuente: 'DS 023-2001-SA',
        requiereRecetaEspecial: true,
        retieneReceta: true,
        vigenciaRecetaDias: 30,
        estado: 'ACTIVO'
      },
      {
        codigo: 'IIIA',
        denominacion: 'Lista III-A',
        normaFuente: 'DS 023-2001-SA',
        requiereRecetaEspecial: false,
        retieneReceta: false,
        vigenciaRecetaDias: null,
        estado: 'ACTIVO'
      }
    ])
  )
```

- [ ] **Step 4: Ejecutar toda la suite de `erp-web`**

Run: `pnpm vitest run --project erp-web`
Expected: PASS — todos los tests existentes más los agregados en Tasks 1-7.

- [ ] **Step 5: Actualizar `feature-routes.test.ts`**

Editar `apps/erp-web/src/app/feature-routes.test.ts`, reemplazando el array `paths` esperado (dentro del primer `it`) por:

```ts
    expect(paths).toEqual([
      'dashboard',
      'catalogo',
      'catalogo/marcas',
      'catalogo/categorias',
      'catalogo/rubros-comerciales',
      'catalogo/condiciones-venta',
      'catalogo/formas-farmaceuticas',
      'catalogo/vias-administracion',
      'catalogo/unidades-medida',
      'catalogo/clasificaciones-controladas',
      'inventario',
      'compras',
      'ventas',
      'pos',
      'caja',
      'clientes',
      'seguridad',
      'seguridad/permisos',
      'seguridad/usuarios',
      'seguridad/usuarios/:userId',
      'seguridad/roles',
      'seguridad/roles/:roleId',
      'organizacion',
      'perfil',
      'perfil/seguridad',
      'perfil/configuraciones',
      'perfil/sucursal',
      'perfil/ayuda'
    ]);
```

(el resto del archivo, incluyendo el segundo `it`, queda sin cambios).

- [ ] **Step 6: Ejecutar el test de rutas**

Run: `pnpm vitest run --project erp-web -- src/app/feature-routes.test.ts`
Expected: PASS.

- [ ] **Step 7: Typecheck y build**

Run: `pnpm --filter @boticas/erp-web typecheck && pnpm --filter @boticas/erp-web build`
Expected: sin errores, build exitoso.

- [ ] **Step 8: Commit**

```bash
git add apps/erp-web/src/features/catalogo/routes.tsx apps/erp-web/src/features/catalogo/pages/CatalogPage.tsx apps/erp-web/src/test/mocks/handlers.ts apps/erp-web/src/app/feature-routes.test.ts
git commit -m "feat(catalogo): registrar rutas y mocks de los catalogos de soporte"
```

---

### Task 9: Verificación final end-to-end

**Files:** ninguno (solo verificación).

- [ ] **Step 1: Ejecutar el pipeline completo**

Run (desde `frontend/`): `pnpm check`
Expected: lint + typecheck + test + build en verde para todo el workspace.

- [ ] **Step 2: Verificar cobertura**

Run: `pnpm vitest run --coverage --project erp-web -- src/features/catalogo`
Expected: cobertura de statements/branches/functions/lines al 100% en todos los archivos nuevos de `features/catalogo/` (`support-catalog/`, `api/rubros-comerciales.*`, `schemas/rubro-comercial.schema.ts`, `components/RubroComercialForm.tsx`, `pages/RubrosComercialesPage.tsx` y las 5 páginas delgadas de `pages/*Page.tsx` de los catálogos de soporte). Si algún archivo queda por debajo de 100%, agregar el caso de test que falta antes de continuar.

- [ ] **Step 3: Verificación visual manual en navegador (modo mock)**

Run: `VITE_API_MODE=mock pnpm --filter @boticas/erp-web dev` (matar antes cualquier proceso zombi en el puerto 3000, ver advertencia del plan de dropdown de perfil).

Con el servidor arriba:
1. Iniciar sesión con las credenciales mock (`Boticas2026!`).
2. Ir a Catálogo — confirmar que se ven 8 cards (Marcas, Categorías, Rubros comerciales, Condiciones de venta, Formas farmacéuticas, Vías de administración, Unidades de medida, Clasificaciones controladas).
3. Entrar a Rubros comerciales — crear uno, editarlo, cambiar su estado.
4. Entrar a cada uno de los 5 catálogos de soporte restantes — para cada uno: confirmar que lista los 2 registros del mock, crear uno nuevo, buscar por texto (confirmar que filtra en cliente sin nueva request), cambiar el filtro de Estado, editar un registro, cambiar su estado.
5. Confirmar que no hay errores en la consola del navegador durante el flujo.

Expected: todos los pasos funcionan como se describe.

- [ ] **Step 4: Commit final si hubo ajustes de la verificación**

Solo si el Step 2 o el Step 3 encontraron y corrigieron algo:

```bash
git add -A
git commit -m "fix(catalogo): ajustes tras verificacion final de catalogos de soporte"
```

---

## Self-Review

**Spec coverage:**
- Rubro Comercial como slice propio (tenant-scoped) → Task 2.
- Módulo genérico `support-catalog/` (API factory, formulario dirigido por config, página con paginación/búsqueda en cliente) → Task 1.
- Los 5 catálogos globales de soporte como configuraciones sobre el módulo genérico → Tasks 3-7.
- Mocks MSW para los 6 nuevos + gap de Categorías/Marcas → Task 8.
- Rutas y `CatalogPage` con 8 cards → Task 8.
- Verificación (`pnpm check`, cobertura 100%, manual en navegador) → Task 9.

**Placeholder scan:** sin TBD/TODO; todo paso de código muestra el código completo.

**Type consistency:** `FieldDef` (Task 1) se usa con los mismos 4 `type` (`text`/`textarea`/`number`/`checkbox`) en las 5 configs (Tasks 3-7). `SupportCatalogApi<TItem, TRequest>` (Task 1) se instancia con los mismos nombres de método (`fetchList`/`fetchOne`/`create`/`update`/`changeStatus`/`listQuery`) en cada config. `SupportCatalogPageProps` (Task 1) se consume con las mismas props (`title`/`description`/`resourceLabel`/`api`/`fields`/`schema`/`columns`/`searchableFields`/`toRequest`/`toDefaultValues`) en las 5 páginas delgadas (Tasks 3-7) y en `SupportCatalogPage.test.tsx`.

**Scope check:** cada tarea produce un estado verificable de forma independiente vía sus propios tests. Dependencias reales: Tasks 3-7 dependen de Task 1 (módulo genérico); Task 8 depende de Tasks 2-7 (todas las páginas deben existir para enrutarlas); Task 9 depende de todas las anteriores. Task 2 es independiente de Task 1 (no usa el módulo genérico).
