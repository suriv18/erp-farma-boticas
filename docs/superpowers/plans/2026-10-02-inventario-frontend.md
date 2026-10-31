# Frontend de inventario (posiciones, ingresos y lotes) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el fixture de `features/inventario/` por una integración real con el backend: tabla de posiciones con filtros, registro de ajustes/ingresos y detalle de lote con bloqueo y desbloqueo.

**Architecture:** Capa `api/` por recurso sobre `packages/api-client` (TanStack Query `queryOptions` + mutaciones que reciben el `ApiClient`), schemas zod, formularios react-hook-form dentro de `Modal`, y dos páginas (`InventoryPage` en `/inventario`, `LoteDetailPage` en `/inventario/lotes/:loteId`). Los filtros por establecimiento/almacén salen de `corporateStructureQuery` (vía `index.ts` de `organizacion`) y el selector de SKU de `skusQuery` (vía `index.ts` de `catalogo`). Los filtros y la página viven en la query string de la URL.

**Tech Stack:** React 19.2, React Router 8.3, TanStack Query, react-hook-form 7 + zod 4 + `@hookform/resolvers`, Tailwind 4, `@boticas/ui-web`, Vitest 4 + Testing Library + MSW 2, Playwright.

**Spec:** `docs/superpowers/specs/2026-10-02-inventario-frontend-design.md`

## Global Constraints

- Todo texto visible al usuario y todo mensaje de error va en español.
- Sin comentarios explicativos en el código; funciones lambda donde sea idiomático; prohibido el código duplicado.
- Los umbrales de Vitest son 100% de líneas, ramas, funciones y sentencias por archivo para todo archivo no listado en `frontend/coverage-baseline.txt`; ningún archivo nuevo se agrega a esa lista.
- Importar de `react-router` (nunca `react-router-dom`); prohibido `forwardRef`; `verbatimModuleSyntax` exige `import type`; `exactOptionalPropertyTypes` exige opcionales como `campo?: T | undefined`; `noUncheckedIndexedAccess` activo.
- Una feature solo importa de otra vía su `index.ts` (`../../organizacion`, `../../catalogo`); nunca archivos internos.
- MSW corre con `onUnhandledRequest: 'error'`: toda petición HTTP en un test necesita su handler.
- El tenant lo resuelve el backend desde el claim `tid`; el frontend no envía `tenantId`. El cliente antepone `/api/v1` a las rutas (`/inventario/posiciones`).
- Estados de lote exactos: `HABILITADO | CUARENTENA | BLOQUEADO | INMOVILIZADO_RECALL | VENCIDO | BAJA_DESTRUIDO`. Bloquear solo en `HABILITADO`/`CUARENTENA`; desbloquear solo en `BLOQUEADO`.
- Tipos de ajuste: `AJUSTE_INGRESO | AJUSTE_SALIDA`. Una salida exige `loteId`; un ingreso exige `loteId` o (`numeroLote` + `fechaVencimiento`). Motivo obligatorio, máximo 1000 caracteres; número de lote máximo 120.
- Cabecera opcional `Idempotency-Key` (1–200 caracteres) en `POST /inventario/movimientos`.
- Los commits terminan con la línea `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Comandos del frontend desde `frontend/`: `pnpm --filter @boticas/erp-web ...` o `pnpm vitest run <ruta>` (en Windows PowerShell, `pnpm.cmd` si la política bloquea `pnpm.ps1`).

## Ajustes al spec detectados al planificar

1. El backend no expone listado de lotes, así que el diálogo no puede ofrecer un selector de "lote existente". Dos modos: **desde una fila** (lote, almacén y SKU precargados; tipo ingreso/salida, cantidad, motivo) y **desde el botón de cabecera**, que pasa a llamarse **Registrar ingreso** y registra un ingreso con lote nuevo (almacén, SKU, número de lote, vencimiento, cantidad, motivo).
2. `DataTable` no tiene botón de reintento; el estado de error muestra el mensaje, igual que el resto de listados.
3. `POST /inventario/movimientos` acepta `Idempotency-Key`; el frontend envía una clave nueva (`crypto.randomUUID()`) por cada envío.
4. El umbral de vencimiento próximo queda fijado en 90 días como constante única `UMBRAL_VENCIMIENTO_PROXIMO_DIAS` y es **POR_VALIDAR**.

Estos ajustes se registran en el spec en la Tarea 11.

## Estructura de archivos

`FE` = `frontend/apps/erp-web/src/features/inventario`, `SH` = `frontend/apps/erp-web/src/shared`, `TS` = `frontend/apps/erp-web/src/test`. Rutas relativas a la raíz del repo.

| Archivo | Responsabilidad |
|---|---|
| `SH/lib/query-string.ts` | `buildQueryString`/`withQuery` compartidos |
| `SH/lib/pagina.types.ts` | `PaginaResponse<T>` compartido |
| `FE/api/inventario.types.ts` | tipos y constantes del dominio de inventario |
| `FE/api/posiciones.api.ts` | listado de posiciones |
| `FE/api/lotes.api.ts` | consultar, bloquear y desbloquear lote |
| `FE/api/movimientos.api.ts` | registrar movimiento con `Idempotency-Key` |
| `FE/api/invalidate.ts` | invalidación de queries de inventario |
| `FE/lib/estado-lote.ts` | tono del badge, `puedeBloquear`, `puedeDesbloquear` |
| `FE/lib/vencimiento.ts` | días y nivel de vencimiento |
| `FE/lib/formato.ts` | `codigoCorto`, `formatearInstante` |
| `FE/lib/estructura.ts` | opciones de establecimiento/almacén desde la estructura corporativa |
| `FE/lib/use-opciones-establecimientos.ts` | hook sobre `corporateStructureQuery` |
| `FE/lib/idempotencia.ts` | clave de idempotencia |
| `FE/lib/ajuste.ts` | defaults y payload del ajuste |
| `FE/lib/use-posiciones-filtros.ts` | filtros y paginación en la URL |
| `FE/schemas/campos.ts`, `ajuste.schema.ts`, `bloqueo.schema.ts` | validación zod |
| `FE/components/*` | badge, celda de vencimiento, acciones, tabla, selector de SKU, formulario y diálogos |
| `FE/pages/InventoryPage.tsx`, `LoteDetailPage.tsx` | páginas |
| `TS/inventario-fixtures.ts` | datos de prueba |
| `frontend/e2e/support/inventario-api.ts`, `frontend/e2e/inventario.spec.ts` | e2e con API simulada |

---

### Task 1: Utilidades compartidas (query string y página)

**Files:**
- Create: `frontend/apps/erp-web/src/shared/lib/query-string.ts`
- Create: `frontend/apps/erp-web/src/shared/lib/query-string.test.ts`
- Create: `frontend/apps/erp-web/src/shared/lib/pagina.types.ts`

**Interfaces:**
- Produces: `buildQueryString(params: QueryParams): string`, `withQuery(path: string, params: QueryParams): string`, `type QueryParams`, `type PaginaResponse<T> = { items: T[]; page: number; size: number; totalElements: number }`.

`organizacion` y `catalogo` ya tienen copias propias; migrarlas queda fuera de este plan para no tocar trabajo sin commitear de otras features.

- [ ] **Step 1: Escribir el test que falla**

`frontend/apps/erp-web/src/shared/lib/query-string.test.ts`:

```ts
import { buildQueryString, withQuery } from './query-string';

describe('query-string', () => {
  it('omite indefinidos y vacíos y convierte números y booleanos', () => {
    expect(buildQueryString({ a: 'x', b: undefined, c: '', d: 0, e: false })).toBe(
      'a=x&d=0&e=false'
    );
  });

  it('withQuery devuelve la ruta sin signo ? cuando no hay parámetros', () => {
    expect(withQuery('/ruta', { a: undefined, b: '' })).toBe('/ruta');
  });

  it('withQuery agrega la query a la ruta', () => {
    expect(withQuery('/ruta', { a: 1, b: 'dos' })).toBe('/ruta?a=1&b=dos');
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run (desde `frontend/`): `pnpm vitest run apps/erp-web/src/shared/lib/query-string.test.ts`
Expected: FAIL — no se puede resolver `./query-string`.

- [ ] **Step 3: Implementar**

`frontend/apps/erp-web/src/shared/lib/query-string.ts`:

```ts
export type QueryParams = Record<string, string | number | boolean | undefined>;

export function buildQueryString(params: QueryParams): string {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') query.set(key, String(value));
  });
  return query.toString();
}

export function withQuery(path: string, params: QueryParams): string {
  const query = buildQueryString(params);
  return query ? `${path}?${query}` : path;
}
```

`frontend/apps/erp-web/src/shared/lib/pagina.types.ts`:

```ts
export type PaginaResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/shared/lib/query-string.test.ts`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/shared/lib/query-string.ts frontend/apps/erp-web/src/shared/lib/query-string.test.ts frontend/apps/erp-web/src/shared/lib/pagina.types.ts
git commit -m "feat(shared): agregar utilidades compartidas de query string y paginacion

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Capa `api/` de inventario

**Files:**
- Create: `FE/api/inventario.types.ts`, `FE/api/invalidate.ts`, `FE/api/invalidate.test.ts`
- Create: `FE/api/posiciones.api.ts`, `FE/api/posiciones.api.test.ts`
- Create: `FE/api/lotes.api.ts`, `FE/api/lotes.api.test.ts`
- Create: `FE/api/movimientos.api.ts`, `FE/api/movimientos.api.test.ts`

**Interfaces:**
- Consumes: `withQuery`, `PaginaResponse` (Tarea 1); `apiClient` de `frontend/apps/erp-web/src/app/api`.
- Produces:
  - `ESTADOS_LOTE`, `type EstadoLote`, `TIPOS_AJUSTE`, `type TipoAjuste`, `type Posicion`, `type Lote`, `type Movimiento`, `type RegistrarMovimientoPayload`, `type BloquearLotePayload`.
  - `fetchPosiciones(client: ApiClient, params: FetchPosicionesParams): Promise<PaginaResponse<Posicion>>`, `posicionesQuery(params)`.
  - `fetchLote(client, loteId)`, `loteQuery(loteId)`, `bloquearLote(client, loteId, payload): Promise<Lote>`, `desbloquearLote(client, loteId): Promise<Lote>`.
  - `registrarMovimiento(client, payload, idempotencyKey): Promise<Movimiento>`.
  - `invalidateInventario(queryClient): Promise<void>`.
  - Claves de query: `['inventario','posiciones','lista',establecimientoId,almacenId,skuId,page,size]`, `['inventario','lotes','detalle',loteId]`.

- [ ] **Step 1: Tipos**

`FE/api/inventario.types.ts`:

```ts
export const ESTADOS_LOTE = [
  'HABILITADO',
  'CUARENTENA',
  'BLOQUEADO',
  'INMOVILIZADO_RECALL',
  'VENCIDO',
  'BAJA_DESTRUIDO'
] as const;

export type EstadoLote = (typeof ESTADOS_LOTE)[number];

export const TIPOS_AJUSTE = ['AJUSTE_INGRESO', 'AJUSTE_SALIDA'] as const;

export type TipoAjuste = (typeof TIPOS_AJUSTE)[number];

export type Posicion = {
  id: string;
  establecimientoId: string;
  almacenId: string;
  skuId: string;
  loteId: string;
  numeroLote: string;
  fechaVencimiento: string;
  estadoLote: EstadoLote;
  estadoInventario: string;
  cantidadFisica: number;
  cantidadReservada: number;
  cantidadDisponible: number;
  vendible: boolean;
  version: number;
  ultimoMovimientoAt: string | null;
};

export type Lote = {
  id: string;
  skuId: string;
  numeroLote: string;
  fechaVencimiento: string;
  estado: EstadoLote;
  motivoEstado: string | null;
  bloqueadoAt: string | null;
  vendible: boolean;
};

export type Movimiento = {
  id: string;
  posicionId: string;
  loteId: string;
  tipo: string;
  naturaleza: string;
  cantidad: number;
  stockAnterior: number;
  stockPosterior: number;
  fechaNegocio: string;
};

export type RegistrarMovimientoPayload = {
  almacenId: string;
  skuId: string;
  tipo: TipoAjuste;
  cantidad: number;
  motivo: string;
  loteId?: string | undefined;
  numeroLote?: string | undefined;
  fechaVencimiento?: string | undefined;
};

export type BloquearLotePayload = {
  motivo: string;
};
```

- [ ] **Step 2: Test de `invalidate` (falla) e implementación**

`FE/api/invalidate.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { invalidateInventario } from './invalidate';

describe('invalidateInventario', () => {
  it('invalida todas las queries de inventario', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue();

    await invalidateInventario(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });
});
```

`FE/api/invalidate.ts`:

```ts
import type { QueryClient } from '@tanstack/react-query';

export function invalidateInventario(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['inventario'] });
}
```

- [ ] **Step 3: Test de posiciones (falla)**

`FE/api/posiciones.api.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { pagina } from '../../../test/organizacion-fixtures';
import { samplePosicion } from '../../../test/inventario-fixtures';
import { fetchPosiciones, posicionesQuery } from './posiciones.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('posiciones.api', () => {
  it('fetchPosiciones envía filtros, página y tamaño', async () => {
    let received: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/inventario/posiciones', ({ request }) => {
        received = new URL(request.url);
        return HttpResponse.json(pagina([samplePosicion]));
      })
    );

    const result = await fetchPosiciones(client, {
      establecimientoId: 'est-1',
      almacenId: 'alm-1',
      skuId: 'sku-1',
      page: 2,
      size: 50
    });

    expect(received?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(received?.searchParams.get('almacenId')).toBe('alm-1');
    expect(received?.searchParams.get('skuId')).toBe('sku-1');
    expect(received?.searchParams.get('page')).toBe('2');
    expect(received?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([samplePosicion]);
  });

  it('fetchPosiciones omite los filtros ausentes y usa paginación por defecto', async () => {
    let received: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/inventario/posiciones', ({ request }) => {
        received = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchPosiciones(client, {});

    expect(received?.search).toBe('?page=0&size=20');
  });

  it('posicionesQuery arma la clave con valores por defecto', () => {
    expect(posicionesQuery({}).queryKey).toEqual([
      'inventario',
      'posiciones',
      'lista',
      '',
      '',
      '',
      0,
      20
    ]);
  });

  it('posicionesQuery arma la clave con los filtros indicados', () => {
    expect(
      posicionesQuery({
        establecimientoId: 'est-1',
        almacenId: 'alm-1',
        skuId: 'sku-1',
        page: 1,
        size: 10
      }).queryKey
    ).toEqual(['inventario', 'posiciones', 'lista', 'est-1', 'alm-1', 'sku-1', 1, 10]);
  });

  it('posicionesQuery consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([samplePosicion]));

    const result = await new QueryClient().fetchQuery(posicionesQuery({ almacenId: 'alm-1' }));

    expect(get).toHaveBeenCalledWith('/inventario/posiciones?almacenId=alm-1&page=0&size=20');
    expect(result.items).toEqual([samplePosicion]);
  });
});
```

- [ ] **Step 4: Fixtures de prueba (los usan los tests de api, componentes y páginas)**

`TS/inventario-fixtures.ts` (`frontend/apps/erp-web/src/test/inventario-fixtures.ts`):

```ts
import type { SkuResumen } from '../features/catalogo';
import type { Lote, Movimiento, Posicion } from '../features/inventario/api/inventario.types';
import type { CorporateStructure } from '../features/organizacion';

export const sampleEstructura: CorporateStructure = {
  asOf: '2026-10-02T10:00:00-05:00',
  companies: [
    {
      id: 'emp-1',
      legalName: 'Boticas SAC',
      tradeName: null,
      status: 'ACTIVE',
      establishments: [
        {
          id: 'est-1',
          code: 'EST001',
          name: 'Botica Central',
          status: 'ACTIVE',
          timeZone: 'America/Lima',
          warehouses: [
            { id: 'alm-1', code: 'ALM001', name: 'Almacén Central', status: 'ACTIVE' },
            { id: 'alm-2', code: 'ALM002', name: 'Almacén Frío', status: 'ACTIVE' }
          ],
          cashRegisters: []
        },
        {
          id: 'est-2',
          code: 'EST002',
          name: 'Botica Norte',
          status: 'ACTIVE',
          timeZone: 'America/Lima',
          warehouses: [{ id: 'alm-3', code: 'ALM003', name: 'Almacén Norte', status: 'ACTIVE' }],
          cashRegisters: []
        }
      ]
    }
  ]
};

export const sampleSku: SkuResumen = {
  id: 'sku-0001-aaaa',
  codigoInterno: 'MED-001',
  descripcionComercial: 'Paracetamol 500 mg',
  tipoSku: 'REGULADO',
  estado: 'ACTIVO'
};

export const samplePosicion: Posicion = {
  id: 'pos-1',
  establecimientoId: 'est-1',
  almacenId: 'alm-1',
  skuId: 'sku-0001-aaaa',
  loteId: 'lote-1',
  numeroLote: 'L001',
  fechaVencimiento: '2099-01-01',
  estadoLote: 'HABILITADO',
  estadoInventario: 'DISPONIBLE',
  cantidadFisica: 100,
  cantidadReservada: 10,
  cantidadDisponible: 90,
  vendible: true,
  version: 1,
  ultimoMovimientoAt: '2026-10-01T15:00:00Z'
};

export const sampleLote: Lote = {
  id: 'lote-1',
  skuId: 'sku-0001-aaaa',
  numeroLote: 'L001',
  fechaVencimiento: '2099-01-01',
  estado: 'HABILITADO',
  motivoEstado: null,
  bloqueadoAt: null,
  vendible: true
};

export const sampleMovimiento: Movimiento = {
  id: 'mov-1',
  posicionId: 'pos-1',
  loteId: 'lote-1',
  tipo: 'AJUSTE_INGRESO',
  naturaleza: 'E',
  cantidad: 5,
  stockAnterior: 100,
  stockPosterior: 105,
  fechaNegocio: '2026-10-02T10:00:00Z'
};
```

Verifica que `SkuResumen` y `CorporateStructure` se exportan como tipos desde los `index.ts` de `catalogo` y `organizacion` (ya lo hacen).

- [ ] **Step 5: Ejecutar y verificar que falla**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/api`
Expected: FAIL — no se puede resolver `./posiciones.api`.

- [ ] **Step 6: Implementar `posiciones.api.ts`**

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { Posicion } from './inventario.types';

export type FetchPosicionesParams = {
  establecimientoId?: string | undefined;
  almacenId?: string | undefined;
  skuId?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchPosiciones(
  client: ApiClient,
  params: FetchPosicionesParams
): Promise<PaginaResponse<Posicion>> {
  return client.get<PaginaResponse<Posicion>>(
    withQuery('/inventario/posiciones', {
      establecimientoId: params.establecimientoId,
      almacenId: params.almacenId,
      skuId: params.skuId,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function posicionesQuery(params: FetchPosicionesParams) {
  return queryOptions({
    queryKey: [
      'inventario',
      'posiciones',
      'lista',
      params.establecimientoId ?? '',
      params.almacenId ?? '',
      params.skuId ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchPosiciones(apiClient, params)
  });
}
```

- [ ] **Step 7: Tests e implementación de lotes**

`FE/api/lotes.api.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleLote } from '../../../test/inventario-fixtures';
import { bloquearLote, desbloquearLote, fetchLote, loteQuery } from './lotes.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('lotes.api', () => {
  it('fetchLote consulta el lote por id', async () => {
    server.use(
      http.get('http://localhost/api/v1/inventario/lotes/lote-1', () =>
        HttpResponse.json(sampleLote)
      )
    );

    expect(await fetchLote(client, 'lote-1')).toEqual(sampleLote);
  });

  it('loteQuery usa una clave por lote y consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(sampleLote);
    const options = loteQuery('lote-1');

    const result = await new QueryClient().fetchQuery(options);

    expect(options.queryKey).toEqual(['inventario', 'lotes', 'detalle', 'lote-1']);
    expect(get).toHaveBeenCalledWith('/inventario/lotes/lote-1');
    expect(result).toEqual(sampleLote);
  });

  it('bloquearLote envía el motivo por POST', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/inventario/lotes/lote-1/bloqueos', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleLote, estado: 'BLOQUEADO' });
      })
    );

    const lote = await bloquearLote(client, 'lote-1', { motivo: 'Control de calidad' });

    expect(body).toEqual({ motivo: 'Control de calidad' });
    expect(lote.estado).toBe('BLOQUEADO');
  });

  it('desbloquearLote usa DELETE sobre los bloqueos del lote', async () => {
    server.use(
      http.delete('http://localhost/api/v1/inventario/lotes/lote-1/bloqueos', () =>
        HttpResponse.json(sampleLote)
      )
    );

    expect(await desbloquearLote(client, 'lote-1')).toEqual(sampleLote);
  });
});
```

`FE/api/lotes.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { BloquearLotePayload, Lote } from './inventario.types';

export function fetchLote(client: ApiClient, loteId: string): Promise<Lote> {
  return client.get<Lote>(`/inventario/lotes/${loteId}`);
}

export function loteQuery(loteId: string) {
  return queryOptions({
    queryKey: ['inventario', 'lotes', 'detalle', loteId],
    queryFn: () => fetchLote(apiClient, loteId)
  });
}

export function bloquearLote(
  client: ApiClient,
  loteId: string,
  payload: BloquearLotePayload
): Promise<Lote> {
  return client.post<Lote, BloquearLotePayload>(`/inventario/lotes/${loteId}/bloqueos`, payload);
}

export function desbloquearLote(client: ApiClient, loteId: string): Promise<Lote> {
  return client.delete<Lote>(`/inventario/lotes/${loteId}/bloqueos`);
}
```

- [ ] **Step 8: Test e implementación de movimientos**

`FE/api/movimientos.api.test.ts`:

```ts
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { sampleMovimiento } from '../../../test/inventario-fixtures';
import { registrarMovimiento } from './movimientos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('movimientos.api', () => {
  it('registrarMovimiento envía el payload y la cabecera Idempotency-Key', async () => {
    let body: unknown;
    let key: string | null = null;
    server.use(
      http.post('http://localhost/api/v1/inventario/movimientos', async ({ request }) => {
        body = await request.json();
        key = request.headers.get('Idempotency-Key');
        return HttpResponse.json(sampleMovimiento, { status: 201 });
      })
    );
    const payload = {
      almacenId: 'alm-1',
      skuId: 'sku-1',
      tipo: 'AJUSTE_INGRESO' as const,
      cantidad: 5,
      motivo: 'Conteo',
      loteId: 'lote-1'
    };

    const result = await registrarMovimiento(client, payload, 'clave-1');

    expect(body).toEqual(payload);
    expect(key).toBe('clave-1');
    expect(result).toEqual(sampleMovimiento);
  });
});
```

`FE/api/movimientos.api.ts`:

```ts
import type { ApiClient } from '@boticas/api-client';
import type { Movimiento, RegistrarMovimientoPayload } from './inventario.types';

export function registrarMovimiento(
  client: ApiClient,
  payload: RegistrarMovimientoPayload,
  idempotencyKey: string
): Promise<Movimiento> {
  return client.post<Movimiento, RegistrarMovimientoPayload>('/inventario/movimientos', payload, {
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}
```

- [ ] **Step 9: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/api --coverage.include=apps/erp-web/src/features/inventario/api/** --coverage.thresholds.lines=100`
Expected: PASS, cobertura 100% en los archivos de `api/`. Si algún archivo queda bajo 100%, agrega el caso que falta antes de seguir.

- [ ] **Step 10: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/api frontend/apps/erp-web/src/test/inventario-fixtures.ts
git commit -m "feat(inventario): agregar capa api de posiciones, lotes y movimientos

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Lógica pura (`lib/`) sin dependencias de React

**Files:**
- Create: `FE/lib/estado-lote.ts` (+ `.test.ts`), `FE/lib/vencimiento.ts` (+ `.test.ts`), `FE/lib/formato.ts` (+ `.test.ts`), `FE/lib/estructura.ts` (+ `.test.ts`), `FE/lib/idempotencia.ts` (+ `.test.ts`)

**Interfaces:**
- Consumes: `EstadoLote`, `ESTADOS_LOTE` (Tarea 2); `CorporateStructure` de `../../organizacion`.
- Produces:
  - `tonoEstadoLote(estado: EstadoLote): 'success' | 'warning' | 'danger' | 'neutral'`, `puedeBloquear(estado)`, `puedeDesbloquear(estado)`.
  - `UMBRAL_VENCIMIENTO_PROXIMO_DIAS = 90`, `type NivelVencimiento = 'vencido' | 'proximo' | 'vigente'`, `diasParaVencer(fecha: string, hoy: Date): number`, `nivelVencimiento(fecha: string, hoy: Date): NivelVencimiento`.
  - `codigoCorto(id: string): string` (primeros 8 caracteres), `formatearInstante(instante: string | null): string`.
  - `type OpcionAlmacen = { id; nombre; establecimiento }`, `type OpcionEstablecimiento = { id; nombre; almacenes: OpcionAlmacen[] }`, `opcionesEstablecimientos(estructura: CorporateStructure | undefined): OpcionEstablecimiento[]`, `almacenesDe(opciones, establecimientoId: string | undefined): OpcionAlmacen[]`.
  - `nuevaClaveIdempotencia(): string`.

- [ ] **Step 1: Tests de `estado-lote` y `vencimiento` (fallan)**

`FE/lib/estado-lote.test.ts`:

```ts
import { ESTADOS_LOTE, type EstadoLote } from '../api/inventario.types';
import { puedeBloquear, puedeDesbloquear, tonoEstadoLote } from './estado-lote';

describe('estado-lote', () => {
  it.each<[EstadoLote, string]>([
    ['HABILITADO', 'success'],
    ['CUARENTENA', 'warning'],
    ['BLOQUEADO', 'danger'],
    ['INMOVILIZADO_RECALL', 'danger'],
    ['VENCIDO', 'neutral'],
    ['BAJA_DESTRUIDO', 'neutral']
  ])('el tono de %s es %s', (estado, tono) => {
    expect(tonoEstadoLote(estado)).toBe(tono);
  });

  it.each(ESTADOS_LOTE)('puedeBloquear(%s) solo es verdadero para habilitado y cuarentena', (estado) => {
    expect(puedeBloquear(estado)).toBe(estado === 'HABILITADO' || estado === 'CUARENTENA');
  });

  it.each(ESTADOS_LOTE)('puedeDesbloquear(%s) solo es verdadero para bloqueado', (estado) => {
    expect(puedeDesbloquear(estado)).toBe(estado === 'BLOQUEADO');
  });
});
```

`FE/lib/vencimiento.test.ts`:

```ts
import {
  UMBRAL_VENCIMIENTO_PROXIMO_DIAS,
  diasParaVencer,
  nivelVencimiento
} from './vencimiento';

const hoy = new Date(2026, 9, 2, 23, 30);

describe('vencimiento', () => {
  it('el umbral de vencimiento próximo es de 90 días', () => {
    expect(UMBRAL_VENCIMIENTO_PROXIMO_DIAS).toBe(90);
  });

  it.each([
    ['2026-10-02', 0],
    ['2026-10-01', -1],
    ['2026-10-03', 1],
    ['2026-12-31', 90],
    ['2027-01-01', 91]
  ])('diasParaVencer(%s) es %i', (fecha, dias) => {
    expect(diasParaVencer(fecha, hoy)).toBe(dias);
  });

  it.each([
    ['2026-10-01', 'vencido'],
    ['2026-10-02', 'proximo'],
    ['2026-12-31', 'proximo'],
    ['2027-01-01', 'vigente']
  ])('nivelVencimiento(%s) es %s', (fecha, nivel) => {
    expect(nivelVencimiento(fecha, hoy)).toBe(nivel);
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/lib`
Expected: FAIL — módulos no encontrados.

- [ ] **Step 3: Implementar**

`FE/lib/estado-lote.ts`:

```ts
import type { EstadoLote } from '../api/inventario.types';

type Tono = 'success' | 'warning' | 'danger' | 'neutral';

const TONOS: Record<EstadoLote, Tono> = {
  HABILITADO: 'success',
  CUARENTENA: 'warning',
  BLOQUEADO: 'danger',
  INMOVILIZADO_RECALL: 'danger',
  VENCIDO: 'neutral',
  BAJA_DESTRUIDO: 'neutral'
};

const BLOQUEABLES: readonly EstadoLote[] = ['HABILITADO', 'CUARENTENA'];

export const tonoEstadoLote = (estado: EstadoLote): Tono => TONOS[estado];

export const puedeBloquear = (estado: EstadoLote): boolean => BLOQUEABLES.includes(estado);

export const puedeDesbloquear = (estado: EstadoLote): boolean => estado === 'BLOQUEADO';
```

`FE/lib/vencimiento.ts`:

```ts
export const UMBRAL_VENCIMIENTO_PROXIMO_DIAS = 90;

const MS_POR_DIA = 86_400_000;

export type NivelVencimiento = 'vencido' | 'proximo' | 'vigente';

export function diasParaVencer(fechaVencimiento: string, hoy: Date): number {
  const inicioHoy = Date.UTC(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  return Math.round((Date.parse(fechaVencimiento) - inicioHoy) / MS_POR_DIA);
}

export function nivelVencimiento(fechaVencimiento: string, hoy: Date): NivelVencimiento {
  const dias = diasParaVencer(fechaVencimiento, hoy);
  if (dias < 0) return 'vencido';
  return dias <= UMBRAL_VENCIMIENTO_PROXIMO_DIAS ? 'proximo' : 'vigente';
}
```

- [ ] **Step 4: Tests e implementación de `formato`, `estructura`, `idempotencia`**

`FE/lib/formato.test.ts`:

```ts
import { codigoCorto, formatearInstante } from './formato';

describe('formato', () => {
  it('codigoCorto devuelve los primeros 8 caracteres', () => {
    expect(codigoCorto('0123456789abcdef')).toBe('01234567');
  });

  it('formatearInstante muestra un guion cuando no hay fecha', () => {
    expect(formatearInstante(null)).toBe('—');
  });

  it('formatearInstante usa el formato regional es-PE', () => {
    const instante = '2026-10-02T15:30:00Z';
    expect(formatearInstante(instante)).toBe(new Date(instante).toLocaleString('es-PE'));
  });
});
```

`FE/lib/formato.ts`:

```ts
export const codigoCorto = (id: string): string => id.slice(0, 8);

export function formatearInstante(instante: string | null): string {
  return instante === null ? '—' : new Date(instante).toLocaleString('es-PE');
}
```

`FE/lib/estructura.test.ts`:

```ts
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { almacenesDe, opcionesEstablecimientos } from './estructura';

describe('estructura', () => {
  it('opcionesEstablecimientos devuelve una lista vacía sin estructura', () => {
    expect(opcionesEstablecimientos(undefined)).toEqual([]);
  });

  it('opcionesEstablecimientos aplana empresas, establecimientos y almacenes', () => {
    expect(opcionesEstablecimientos(sampleEstructura)).toEqual([
      {
        id: 'est-1',
        nombre: 'Botica Central',
        almacenes: [
          { id: 'alm-1', nombre: 'Almacén Central', establecimiento: 'Botica Central' },
          { id: 'alm-2', nombre: 'Almacén Frío', establecimiento: 'Botica Central' }
        ]
      },
      {
        id: 'est-2',
        nombre: 'Botica Norte',
        almacenes: [{ id: 'alm-3', nombre: 'Almacén Norte', establecimiento: 'Botica Norte' }]
      }
    ]);
  });

  it('almacenesDe devuelve todos los almacenes sin establecimiento', () => {
    const opciones = opcionesEstablecimientos(sampleEstructura);

    expect(almacenesDe(opciones, undefined).map(({ id }) => id)).toEqual([
      'alm-1',
      'alm-2',
      'alm-3'
    ]);
    expect(almacenesDe(opciones, '').map(({ id }) => id)).toEqual(['alm-1', 'alm-2', 'alm-3']);
  });

  it('almacenesDe filtra por establecimiento', () => {
    const opciones = opcionesEstablecimientos(sampleEstructura);

    expect(almacenesDe(opciones, 'est-2').map(({ id }) => id)).toEqual(['alm-3']);
  });
});
```

`FE/lib/estructura.ts`:

```ts
import type { CorporateStructure } from '../../organizacion';

export type OpcionAlmacen = {
  id: string;
  nombre: string;
  establecimiento: string;
};

export type OpcionEstablecimiento = {
  id: string;
  nombre: string;
  almacenes: OpcionAlmacen[];
};

export function opcionesEstablecimientos(
  estructura: CorporateStructure | undefined
): OpcionEstablecimiento[] {
  return (estructura?.companies ?? []).flatMap((empresa) =>
    empresa.establishments.map((establecimiento) => ({
      id: establecimiento.id,
      nombre: establecimiento.name,
      almacenes: establecimiento.warehouses.map((almacen) => ({
        id: almacen.id,
        nombre: almacen.name,
        establecimiento: establecimiento.name
      }))
    }))
  );
}

export function almacenesDe(
  opciones: OpcionEstablecimiento[],
  establecimientoId: string | undefined
): OpcionAlmacen[] {
  const origen = establecimientoId
    ? opciones.filter(({ id }) => id === establecimientoId)
    : opciones;
  return origen.flatMap(({ almacenes }) => almacenes);
}
```

`FE/lib/idempotencia.test.ts`:

```ts
import { nuevaClaveIdempotencia } from './idempotencia';

describe('nuevaClaveIdempotencia', () => {
  it('genera un UUID distinto en cada llamada', () => {
    const primera = nuevaClaveIdempotencia();
    const segunda = nuevaClaveIdempotencia();

    expect(primera).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
    expect(primera).not.toBe(segunda);
  });
});
```

`FE/lib/idempotencia.ts`:

```ts
export const nuevaClaveIdempotencia = (): string => crypto.randomUUID();
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/lib`
Expected: PASS. Si `crypto.randomUUID` no existe en jsdom, falla el test de idempotencia: en ese caso agrega al test `vi.stubGlobal('crypto', { randomUUID: () => '...' })` con dos valores distintos y conserva la regex; no cambies la implementación.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/lib
git commit -m "feat(inventario): agregar logica pura de estado de lote, vencimiento y estructura

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Schemas zod y construcción del ajuste

**Files:**
- Create: `FE/schemas/campos.ts`, `FE/schemas/ajuste.schema.ts` (+ `.test.ts`), `FE/schemas/bloqueo.schema.ts` (+ `.test.ts`)
- Create: `FE/lib/ajuste.ts` (+ `.test.ts`)

**Interfaces:**
- Consumes: `TIPOS_AJUSTE`, `Posicion`, `RegistrarMovimientoPayload` (Tarea 2).
- Produces: `motivoRequerido`, `ajusteSchema`, `type AjusteFormValues = { almacenId: string; skuId: string; tipo: TipoAjuste; loteId: string; numeroLote: string; fechaVencimiento: string; cantidad: string; motivo: string }`, `bloqueoSchema`, `type BloqueoFormValues = { motivo: string }`, `AJUSTE_FORM_VACIO`, `ajusteDesdePosicion(posicion: Posicion): AjusteFormValues`, `toRegistrarMovimientoPayload(values: AjusteFormValues): RegistrarMovimientoPayload`.

- [ ] **Step 1: Tests (fallan)**

`FE/schemas/ajuste.schema.test.ts`:

```ts
import { AJUSTE_FORM_VACIO } from '../lib/ajuste';
import { ajusteSchema, type AjusteFormValues } from './ajuste.schema';

const valido: AjusteFormValues = {
  ...AJUSTE_FORM_VACIO,
  almacenId: 'alm-1',
  skuId: 'sku-1',
  loteId: 'lote-1',
  cantidad: '5',
  motivo: 'Conteo cíclico'
};

const mensajes = (values: AjusteFormValues) => {
  const result = ajusteSchema.safeParse(values);
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
};

describe('ajusteSchema', () => {
  it('acepta un ingreso sobre un lote existente', () => {
    expect(ajusteSchema.safeParse(valido).success).toBe(true);
  });

  it('acepta un ingreso con lote nuevo y vencimiento', () => {
    expect(
      ajusteSchema.safeParse({
        ...valido,
        loteId: '',
        numeroLote: 'L-NEW',
        fechaVencimiento: '2030-01-01'
      }).success
    ).toBe(true);
  });

  it('acepta una salida sobre un lote existente', () => {
    expect(ajusteSchema.safeParse({ ...valido, tipo: 'AJUSTE_SALIDA' }).success).toBe(true);
  });

  it('exige almacén y SKU', () => {
    expect(mensajes({ ...valido, almacenId: '', skuId: '' })).toEqual([
      'Selecciona un almacén.',
      'Selecciona un SKU.'
    ]);
  });

  it.each(['', '0', '-3', 'abc'])('rechaza la cantidad %j', (cantidad) => {
    expect(mensajes({ ...valido, cantidad })).toEqual([
      'La cantidad debe ser un número mayor que cero.'
    ]);
  });

  it('exige motivo y limita su longitud', () => {
    expect(mensajes({ ...valido, motivo: '   ' })).toEqual(['El motivo es obligatorio.']);
    expect(mensajes({ ...valido, motivo: 'x'.repeat(1001) })).toEqual([
      'El motivo no debe exceder 1000 caracteres.'
    ]);
  });

  it('limita el número de lote', () => {
    expect(mensajes({ ...valido, numeroLote: 'x'.repeat(121) })).toEqual([
      'El número de lote no debe exceder 120 caracteres.'
    ]);
  });

  it('rechaza una salida sin lote existente', () => {
    expect(mensajes({ ...valido, tipo: 'AJUSTE_SALIDA', loteId: '' })).toEqual([
      'Una salida requiere un lote existente.'
    ]);
  });

  it('rechaza un ingreso sin lote ni datos del lote nuevo', () => {
    expect(mensajes({ ...valido, loteId: '' })).toEqual([
      'Indica el número del lote.',
      'Indica la fecha de vencimiento.'
    ]);
  });

  it('rechaza un tipo de ajuste desconocido', () => {
    expect(mensajes({ ...valido, tipo: 'OTRO' as AjusteFormValues['tipo'] })).toEqual([
      'Selecciona el tipo de ajuste.'
    ]);
  });
});
```

`FE/schemas/bloqueo.schema.test.ts`:

```ts
import { bloqueoSchema } from './bloqueo.schema';

describe('bloqueoSchema', () => {
  it('acepta un motivo válido', () => {
    expect(bloqueoSchema.safeParse({ motivo: 'Control de calidad' }).success).toBe(true);
  });

  it('exige motivo', () => {
    const result = bloqueoSchema.safeParse({ motivo: ' ' });

    expect(result.success ? [] : result.error.issues.map(({ message }) => message)).toEqual([
      'El motivo es obligatorio.'
    ]);
  });

  it('limita el motivo a 1000 caracteres', () => {
    expect(bloqueoSchema.safeParse({ motivo: 'x'.repeat(1001) }).success).toBe(false);
  });
});
```

`FE/lib/ajuste.test.ts`:

```ts
import { samplePosicion } from '../../../test/inventario-fixtures';
import { AJUSTE_FORM_VACIO, ajusteDesdePosicion, toRegistrarMovimientoPayload } from './ajuste';

describe('ajuste', () => {
  it('el formulario vacío es un ingreso sin datos', () => {
    expect(AJUSTE_FORM_VACIO).toEqual({
      almacenId: '',
      skuId: '',
      tipo: 'AJUSTE_INGRESO',
      loteId: '',
      numeroLote: '',
      fechaVencimiento: '',
      cantidad: '',
      motivo: ''
    });
  });

  it('ajusteDesdePosicion precarga almacén, SKU y lote', () => {
    expect(ajusteDesdePosicion(samplePosicion)).toEqual({
      ...AJUSTE_FORM_VACIO,
      almacenId: 'alm-1',
      skuId: 'sku-0001-aaaa',
      loteId: 'lote-1'
    });
  });

  it('el payload sobre un lote existente envía loteId y omite los datos del lote nuevo', () => {
    expect(
      toRegistrarMovimientoPayload({
        ...ajusteDesdePosicion(samplePosicion),
        tipo: 'AJUSTE_SALIDA',
        cantidad: '2.5',
        motivo: 'Merma'
      })
    ).toEqual({
      almacenId: 'alm-1',
      skuId: 'sku-0001-aaaa',
      tipo: 'AJUSTE_SALIDA',
      cantidad: 2.5,
      motivo: 'Merma',
      loteId: 'lote-1'
    });
  });

  it('el payload con lote nuevo envía número y vencimiento sin loteId', () => {
    expect(
      toRegistrarMovimientoPayload({
        ...AJUSTE_FORM_VACIO,
        almacenId: 'alm-1',
        skuId: 'sku-1',
        numeroLote: ' L-NEW ',
        fechaVencimiento: '2030-01-01',
        cantidad: '10',
        motivo: 'Ingreso inicial'
      })
    ).toEqual({
      almacenId: 'alm-1',
      skuId: 'sku-1',
      tipo: 'AJUSTE_INGRESO',
      cantidad: 10,
      motivo: 'Ingreso inicial',
      numeroLote: 'L-NEW',
      fechaVencimiento: '2030-01-01'
    });
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/schemas apps/erp-web/src/features/inventario/lib/ajuste.test.ts`
Expected: FAIL — módulos no encontrados.

- [ ] **Step 3: Implementar**

`FE/schemas/campos.ts`:

```ts
import { z } from 'zod';

export const MOTIVO_MAX = 1000;

export const motivoRequerido = z
  .string()
  .trim()
  .min(1, 'El motivo es obligatorio.')
  .max(MOTIVO_MAX, `El motivo no debe exceder ${MOTIVO_MAX} caracteres.`);
```

`FE/schemas/bloqueo.schema.ts`:

```ts
import { z } from 'zod';
import { motivoRequerido } from './campos';

export const bloqueoSchema = z.object({ motivo: motivoRequerido });

export type BloqueoFormValues = z.infer<typeof bloqueoSchema>;
```

`FE/schemas/ajuste.schema.ts`:

```ts
import { z } from 'zod';
import { TIPOS_AJUSTE } from '../api/inventario.types';
import { motivoRequerido } from './campos';

const NUMERO_LOTE_MAX = 120;

export const ajusteSchema = z
  .object({
    almacenId: z.string().min(1, 'Selecciona un almacén.'),
    skuId: z.string().min(1, 'Selecciona un SKU.'),
    tipo: z.enum(TIPOS_AJUSTE, { error: 'Selecciona el tipo de ajuste.' }),
    loteId: z.string(),
    numeroLote: z
      .string()
      .max(NUMERO_LOTE_MAX, `El número de lote no debe exceder ${NUMERO_LOTE_MAX} caracteres.`),
    fechaVencimiento: z.string(),
    cantidad: z
      .string()
      .refine(
        (valor) => Number.isFinite(Number(valor)) && Number(valor) > 0,
        'La cantidad debe ser un número mayor que cero.'
      ),
    motivo: motivoRequerido
  })
  .refine((valores) => valores.tipo !== 'AJUSTE_SALIDA' || valores.loteId !== '', {
    path: ['loteId'],
    error: 'Una salida requiere un lote existente.'
  })
  .refine(
    (valores) =>
      valores.tipo !== 'AJUSTE_INGRESO' || valores.loteId !== '' || valores.numeroLote.trim() !== '',
    { path: ['numeroLote'], error: 'Indica el número del lote.' }
  )
  .refine(
    (valores) =>
      valores.tipo !== 'AJUSTE_INGRESO' || valores.loteId !== '' || valores.fechaVencimiento !== '',
    { path: ['fechaVencimiento'], error: 'Indica la fecha de vencimiento.' }
  );

export type AjusteFormValues = z.infer<typeof ajusteSchema>;
```

`FE/lib/ajuste.ts`:

```ts
import type { Posicion, RegistrarMovimientoPayload } from '../api/inventario.types';
import type { AjusteFormValues } from '../schemas/ajuste.schema';

export const AJUSTE_FORM_VACIO: AjusteFormValues = {
  almacenId: '',
  skuId: '',
  tipo: 'AJUSTE_INGRESO',
  loteId: '',
  numeroLote: '',
  fechaVencimiento: '',
  cantidad: '',
  motivo: ''
};

export const ajusteDesdePosicion = (posicion: Posicion): AjusteFormValues => ({
  ...AJUSTE_FORM_VACIO,
  almacenId: posicion.almacenId,
  skuId: posicion.skuId,
  loteId: posicion.loteId
});

export function toRegistrarMovimientoPayload(values: AjusteFormValues): RegistrarMovimientoPayload {
  return {
    almacenId: values.almacenId,
    skuId: values.skuId,
    tipo: values.tipo,
    cantidad: Number(values.cantidad),
    motivo: values.motivo,
    ...(values.loteId
      ? { loteId: values.loteId }
      : { numeroLote: values.numeroLote.trim(), fechaVencimiento: values.fechaVencimiento })
  };
}
```

`ajusteSchema` y `lib/ajuste.ts` se importan mutuamente solo por tipos/valores en una dirección (`ajuste.ts` importa el tipo del schema; el test del schema importa `AJUSTE_FORM_VACIO`), sin ciclo de valores.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/schemas apps/erp-web/src/features/inventario/lib/ajuste.test.ts`
Expected: PASS. Ajusta únicamente los mensajes esperados si zod 4 ordena los `issues` de otra forma (el contenido de los mensajes no cambia).

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/schemas frontend/apps/erp-web/src/features/inventario/lib/ajuste.ts frontend/apps/erp-web/src/features/inventario/lib/ajuste.test.ts
git commit -m "feat(inventario): agregar validacion zod y payload de ajustes de stock

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Filtros y paginación en la URL

**Files:**
- Create: `FE/lib/use-posiciones-filtros.ts`, `FE/lib/use-posiciones-filtros.test.tsx`
- Create: `FE/lib/use-opciones-establecimientos.ts`

**Interfaces:**
- Consumes: `corporateStructureQuery` de `../../organizacion`; `opcionesEstablecimientos` (Tarea 3).
- Produces:
  - `usePosicionesFiltros(): { filtros: FiltrosPosiciones; setEstablecimiento(id: string): void; setAlmacen(id: string): void; setSku(id: string): void; setPage(page: number): void; setSize(size: number): void }` con `FiltrosPosiciones = { establecimientoId: string; almacenId: string; skuId: string; page: number; size: number }`. Cambiar establecimiento limpia almacén y página; cualquier filtro o el tamaño reinician la página.
  - `useOpcionesEstablecimientos(): OpcionEstablecimiento[]`.

`use-opciones-establecimientos.ts` no lleva test propio: lo cubren los tests de componentes y páginas (Tareas 7 y 8).

- [ ] **Step 1: Test del hook (falla)**

`FE/lib/use-posiciones-filtros.test.tsx`:

```tsx
import { act, renderHook } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { MemoryRouter, useLocation } from 'react-router';
import { usePosicionesFiltros } from './use-posiciones-filtros';

function renderFiltros(entrada: string) {
  const wrapper = ({ children }: PropsWithChildren) => (
    <MemoryRouter initialEntries={[entrada]}>{children}</MemoryRouter>
  );
  return renderHook(() => ({ ...usePosicionesFiltros(), location: useLocation() }), { wrapper });
}

describe('usePosicionesFiltros', () => {
  it('usa valores por defecto sin parámetros en la URL', () => {
    const { result } = renderFiltros('/inventario');

    expect(result.current.filtros).toEqual({
      establecimientoId: '',
      almacenId: '',
      skuId: '',
      page: 0,
      size: 20
    });
  });

  it('lee los filtros de la URL', () => {
    const { result } = renderFiltros(
      '/inventario?establecimientoId=est-1&almacenId=alm-1&skuId=sku-1&page=3&size=50'
    );

    expect(result.current.filtros).toEqual({
      establecimientoId: 'est-1',
      almacenId: 'alm-1',
      skuId: 'sku-1',
      page: 3,
      size: 50
    });
  });

  it('cambiar el establecimiento limpia almacén y página', () => {
    const { result } = renderFiltros('/inventario?almacenId=alm-1&page=2');

    act(() => result.current.setEstablecimiento('est-2'));

    expect(result.current.location.search).toBe('?establecimientoId=est-2');
  });

  it('cambiar almacén o SKU reinicia la página y vaciar un filtro lo elimina', () => {
    const { result } = renderFiltros('/inventario?page=2&skuId=sku-1');

    act(() => result.current.setAlmacen('alm-1'));
    expect(result.current.location.search).toBe('?skuId=sku-1&almacenId=alm-1');

    act(() => result.current.setSku(''));
    expect(result.current.location.search).toBe('?almacenId=alm-1');
  });

  it('setPage escribe la página y setSize reinicia la página', () => {
    const { result } = renderFiltros('/inventario');

    act(() => result.current.setPage(2));
    expect(result.current.location.search).toBe('?page=2');

    act(() => result.current.setSize(50));
    expect(result.current.location.search).toBe('?size=50');
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/lib/use-posiciones-filtros.test.tsx`
Expected: FAIL — módulo no encontrado.

- [ ] **Step 3: Implementar**

`FE/lib/use-posiciones-filtros.ts`:

```ts
import { useSearchParams } from 'react-router';

export type FiltrosPosiciones = {
  establecimientoId: string;
  almacenId: string;
  skuId: string;
  page: number;
  size: number;
};

export function usePosicionesFiltros() {
  const [params, setParams] = useSearchParams();

  const filtros: FiltrosPosiciones = {
    establecimientoId: params.get('establecimientoId') ?? '',
    almacenId: params.get('almacenId') ?? '',
    skuId: params.get('skuId') ?? '',
    page: Number(params.get('page') ?? 0),
    size: Number(params.get('size') ?? 20)
  };

  const actualizar = (cambios: Record<string, string>) =>
    setParams((actuales) => {
      const siguientes = new URLSearchParams(actuales);
      Object.entries(cambios).forEach(([clave, valor]) =>
        valor === '' ? siguientes.delete(clave) : siguientes.set(clave, valor)
      );
      return siguientes;
    });

  return {
    filtros,
    setEstablecimiento: (id: string) =>
      actualizar({ establecimientoId: id, almacenId: '', page: '' }),
    setAlmacen: (id: string) => actualizar({ almacenId: id, page: '' }),
    setSku: (id: string) => actualizar({ skuId: id, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
```

`FE/lib/use-opciones-establecimientos.ts`:

```ts
import { useQuery } from '@tanstack/react-query';
import { corporateStructureQuery } from '../../organizacion';
import { opcionesEstablecimientos, type OpcionEstablecimiento } from './estructura';

export function useOpcionesEstablecimientos(): OpcionEstablecimiento[] {
  const { data } = useQuery(corporateStructureQuery);
  return opcionesEstablecimientos(data);
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/lib/use-posiciones-filtros.test.tsx`
Expected: PASS. Si `MemoryRouter` no se exporta desde `react-router` en esta versión, reemplaza el wrapper por `createMemoryRouter` + `RouterProvider` de `react-router/dom` (como `src/test/render-route.tsx`) manteniendo las mismas aserciones.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/lib/use-posiciones-filtros.ts frontend/apps/erp-web/src/features/inventario/lib/use-posiciones-filtros.test.tsx frontend/apps/erp-web/src/features/inventario/lib/use-opciones-establecimientos.ts
git commit -m "feat(inventario): agregar filtros de posiciones sincronizados con la URL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Componentes de presentación y tabla

**Files:**
- Create: `FE/components/EstadoLoteBadge.tsx` (+ `.test.tsx`), `VencimientoCelda.tsx` (+ test), `AccionesPosicion.tsx` (+ test), `SkuSelect.tsx` (+ test), `PosicionesTable.tsx` (+ test)

**Interfaces:**
- Consumes: `tonoEstadoLote`, `nivelVencimiento`, `codigoCorto` (Tarea 3); `skusQuery` de `../../catalogo`; `SelectField` de `shared/components/FormFields`; `renderRoute` de `src/test/render-route`.
- Produces:
  - `EstadoLoteBadge({ estado: EstadoLote })`.
  - `VencimientoCelda({ fecha: string })`.
  - `AccionesPosicion({ loteId: string; numeroLote: string; onAjustar: () => void })` — enlace "Ver detalle del lote {numeroLote}" a `/inventario/lotes/{loteId}` y botón "Ajustar stock del lote {numeroLote}".
  - `SkuSelect({ id; label; placeholder; search; value; onChange(skuId: string): void; error?: string | undefined })`.
  - `PosicionesTable({ rows: Posicion[]; isLoading: boolean; isError: boolean; pagination: PaginationProps; onAjustar(posicion: Posicion): void })`.

- [ ] **Step 1: Tests (fallan)**

`FE/components/EstadoLoteBadge.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { EstadoLoteBadge } from './EstadoLoteBadge';

describe('EstadoLoteBadge', () => {
  it('muestra el estado del lote', () => {
    render(<EstadoLoteBadge estado="CUARENTENA" />);

    expect(screen.getByText('CUARENTENA')).toBeInTheDocument();
  });
});
```

`FE/components/VencimientoCelda.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { VencimientoCelda } from './VencimientoCelda';

describe('VencimientoCelda', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 2, 12));
  });

  afterEach(() => vi.useRealTimers());

  it('marca como vencido una fecha pasada', () => {
    render(<VencimientoCelda fecha="2026-10-01" />);

    expect(screen.getByText('2026-10-01')).toBeInTheDocument();
    expect(screen.getByText('Vencido')).toBeInTheDocument();
  });

  it('marca como por vencer una fecha dentro del umbral', () => {
    render(<VencimientoCelda fecha="2026-12-31" />);

    expect(screen.getByText('Por vencer')).toBeInTheDocument();
  });

  it('no marca una fecha vigente', () => {
    render(<VencimientoCelda fecha="2030-01-01" />);

    expect(screen.getByText('2030-01-01')).toBeInTheDocument();
    expect(screen.queryByText('Vencido')).not.toBeInTheDocument();
    expect(screen.queryByText('Por vencer')).not.toBeInTheDocument();
  });
});
```

`FE/components/AccionesPosicion.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { AccionesPosicion } from './AccionesPosicion';

describe('AccionesPosicion', () => {
  it('enlaza al detalle del lote y dispara el ajuste', async () => {
    const onAjustar = vi.fn();
    render(
      <MemoryRouter>
        <AccionesPosicion loteId="lote-1" numeroLote="L001" onAjustar={onAjustar} />
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: 'Ver detalle del lote L001' })).toHaveAttribute(
      'href',
      '/inventario/lotes/lote-1'
    );
    await userEvent.click(screen.getByRole('button', { name: 'Ajustar stock del lote L001' }));

    expect(onAjustar).toHaveBeenCalledTimes(1);
  });
});
```

`FE/components/SkuSelect.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import { sampleSku } from '../../../test/inventario-fixtures';
import { SkuSelect } from './SkuSelect';

function renderSelect(props: Partial<Parameters<typeof SkuSelect>[0]> = {}) {
  const onChange = vi.fn();
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <SkuSelect
        id="sku"
        label="SKU"
        placeholder="Todos"
        search=""
        value=""
        onChange={onChange}
        {...props}
      />
    </QueryClientProvider>
  );
  return onChange;
}

describe('SkuSelect', () => {
  it('lista los SKU activos que coinciden con la búsqueda y notifica la selección', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get('*/api/v1/catalogo/skus', ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sampleSku]));
      })
    );
    const onChange = renderSelect({ search: 'para' });

    await userEvent.selectOptions(
      await screen.findByRole('combobox', { name: 'SKU' }),
      await screen.findByRole('option', { name: 'MED-001 — Paracetamol 500 mg' })
    );

    expect(received.get('q')).toBe('para');
    expect(received.get('estado')).toBe('ACTIVO');
    expect(received.get('size')).toBe('50');
    expect(onChange).toHaveBeenCalledWith('sku-0001-aaaa');
  });

  it('conserva el SKU seleccionado aunque no esté en los resultados', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([]))));
    renderSelect({ value: 'abcdef0123456789' });

    expect(await screen.findByRole('option', { name: 'abcdef01' })).toBeInTheDocument();
  });

  it('muestra el error del campo', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku]))));
    renderSelect({ error: 'Selecciona un SKU.' });

    expect(await screen.findByText('Selecciona un SKU.')).toBeInTheDocument();
  });
});
```

`FE/components/PosicionesTable.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { samplePosicion } from '../../../test/inventario-fixtures';
import { PosicionesTable } from './PosicionesTable';

const pagination = {
  page: 0,
  size: 20,
  totalElements: 2,
  onPageChange: vi.fn(),
  onSizeChange: vi.fn()
};

function renderTable(props: Partial<Parameters<typeof PosicionesTable>[0]> = {}) {
  const onAjustar = vi.fn();
  render(
    <MemoryRouter>
      <PosicionesTable
        rows={[
          samplePosicion,
          {
            ...samplePosicion,
            id: 'pos-2',
            loteId: 'lote-2',
            numeroLote: 'L002',
            estadoLote: 'BLOQUEADO',
            vendible: false,
            cantidadFisica: 8,
            cantidadReservada: 0,
            cantidadDisponible: 8
          }
        ]}
        isLoading={false}
        isError={false}
        pagination={pagination}
        onAjustar={onAjustar}
        {...props}
      />
    </MemoryRouter>
  );
  return onAjustar;
}

describe('PosicionesTable', () => {
  it('muestra lote, cantidades, estado y acciones de cada posición', () => {
    renderTable();

    expect(screen.getByText('L001')).toBeInTheDocument();
    expect(screen.getAllByText('sku-0001')).toHaveLength(2);
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('90')).toBeInTheDocument();
    expect(screen.getByText('HABILITADO')).toBeInTheDocument();
    expect(screen.getByText('BLOQUEADO')).toBeInTheDocument();
    expect(screen.getAllByText('No vendible')).toHaveLength(1);
    expect(screen.getByRole('link', { name: 'Ver detalle del lote L002' })).toHaveAttribute(
      'href',
      '/inventario/lotes/lote-2'
    );
  });

  it('dispara el ajuste con la posición de la fila', async () => {
    const onAjustar = renderTable();

    await userEvent.click(screen.getByRole('button', { name: 'Ajustar stock del lote L001' }));

    expect(onAjustar).toHaveBeenCalledWith(samplePosicion);
  });

  it('muestra el mensaje de lista vacía', () => {
    renderTable({ rows: [] });

    expect(screen.getByText('No hay stock registrado con los filtros indicados.')).toBeInTheDocument();
  });

  it('muestra el mensaje de error', () => {
    renderTable({ rows: [], isError: true });

    expect(screen.getByText('No se pudo cargar el inventario.')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/components`
Expected: FAIL — componentes no encontrados.

- [ ] **Step 3: Implementar**

`FE/components/EstadoLoteBadge.tsx`:

```tsx
import { Badge } from '@boticas/ui-web';
import type { EstadoLote } from '../api/inventario.types';
import { tonoEstadoLote } from '../lib/estado-lote';

export function EstadoLoteBadge({ estado }: { estado: EstadoLote }) {
  return <Badge tone={tonoEstadoLote(estado)}>{estado}</Badge>;
}
```

`FE/components/VencimientoCelda.tsx`:

```tsx
import { Badge } from '@boticas/ui-web';
import { nivelVencimiento } from '../lib/vencimiento';

export function VencimientoCelda({ fecha }: { fecha: string }) {
  const nivel = nivelVencimiento(fecha, new Date());
  return (
    <span className="inline-flex flex-wrap items-center gap-2">
      {fecha}
      {nivel === 'vencido' ? <Badge tone="danger">Vencido</Badge> : null}
      {nivel === 'proximo' ? <Badge tone="warning">Por vencer</Badge> : null}
    </span>
  );
}
```

`FE/components/AccionesPosicion.tsx`:

```tsx
import { Eye, SlidersHorizontal } from 'lucide-react';
import { Link } from 'react-router';
import { IconButton, iconButtonClassName } from '@boticas/ui-web';

type AccionesPosicionProps = {
  loteId: string;
  numeroLote: string;
  onAjustar: () => void;
};

export function AccionesPosicion({ loteId, numeroLote, onAjustar }: AccionesPosicionProps) {
  return (
    <div className="flex items-center gap-1">
      <Link
        to={`/inventario/lotes/${loteId}`}
        aria-label={`Ver detalle del lote ${numeroLote}`}
        title={`Ver detalle del lote ${numeroLote}`}
        className={iconButtonClassName()}
      >
        <Eye className="size-4.5" aria-hidden="true" />
      </Link>
      <IconButton
        icon={SlidersHorizontal}
        label={`Ajustar stock del lote ${numeroLote}`}
        onClick={onAjustar}
      />
    </div>
  );
}
```

`FE/components/SkuSelect.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { SelectField } from '../../../shared/components/FormFields';
import { skusQuery } from '../../catalogo';
import { codigoCorto } from '../lib/formato';

type SkuSelectProps = {
  id: string;
  label: string;
  placeholder: string;
  search: string;
  value: string;
  onChange: (skuId: string) => void;
  error?: string | undefined;
};

export function SkuSelect({ id, label, placeholder, search, value, onChange, error }: SkuSelectProps) {
  const { data } = useQuery(skusQuery({ q: search, estado: 'ACTIVO', size: 50 }));
  const items = data?.items ?? [];
  const faltaSeleccionado = value !== '' && !items.some((sku) => sku.id === value);

  return (
    <SelectField
      id={id}
      label={label}
      error={error}
      value={value}
      onChange={(event) => onChange(event.target.value)}
    >
      <option value="">{placeholder}</option>
      {faltaSeleccionado ? <option value={value}>{codigoCorto(value)}</option> : null}
      {items.map((sku) => (
        <option key={sku.id} value={sku.id}>
          {sku.codigoInterno} — {sku.descripcionComercial}
        </option>
      ))}
    </SelectField>
  );
}
```

`FE/components/PosicionesTable.tsx`:

```tsx
import { Badge, DataTable, type PaginationProps } from '@boticas/ui-web';
import type { Posicion } from '../api/inventario.types';
import { codigoCorto } from '../lib/formato';
import { AccionesPosicion } from './AccionesPosicion';
import { EstadoLoteBadge } from './EstadoLoteBadge';
import { VencimientoCelda } from './VencimientoCelda';

type PosicionesTableProps = {
  rows: Posicion[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
  onAjustar: (posicion: Posicion) => void;
};

export function PosicionesTable({
  rows,
  isLoading,
  isError,
  pagination,
  onAjustar
}: PosicionesTableProps) {
  return (
    <DataTable<Posicion>
      columns={[
        { header: 'SKU', cell: (row) => codigoCorto(row.skuId) },
        { header: 'Lote', cell: (row) => row.numeroLote },
        { header: 'Vencimiento', cell: (row) => <VencimientoCelda fecha={row.fechaVencimiento} /> },
        { header: 'Físico', cell: (row) => row.cantidadFisica },
        { header: 'Reservado', cell: (row) => row.cantidadReservada },
        { header: 'Disponible', cell: (row) => row.cantidadDisponible },
        {
          header: 'Estado',
          cell: (row) => (
            <div className="flex flex-wrap items-center gap-1">
              <EstadoLoteBadge estado={row.estadoLote} />
              {row.vendible ? null : <Badge tone="warning">No vendible</Badge>}
            </div>
          )
        },
        {
          header: 'Acciones',
          cell: (row) => (
            <AccionesPosicion
              loteId={row.loteId}
              numeroLote={row.numeroLote}
              onAjustar={() => onAjustar(row)}
            />
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay stock registrado con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el inventario."
      pagination={pagination}
    />
  );
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/components`
Expected: PASS. `codigoCorto('sku-0001-aaaa')` es `sku-0001`, valor que usa el test de la tabla.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/components
git commit -m "feat(inventario): agregar tabla de posiciones y componentes de presentacion

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Formulario y diálogos (ajuste, bloqueo, desbloqueo)

**Files:**
- Create: `FE/components/AjusteForm.tsx` (+ `.test.tsx`), `AjusteDialog.tsx` (+ test), `BloqueoDialog.tsx` (+ test), `DesbloqueoDialog.tsx` (+ test)

**Interfaces:**
- Consumes: `ajusteSchema`, `AjusteFormValues`, `bloqueoSchema`, `BloqueoFormValues` (Tarea 4); `AJUSTE_FORM_VACIO`, `ajusteDesdePosicion`, `toRegistrarMovimientoPayload` (Tarea 4); `registrarMovimiento`, `bloquearLote`, `desbloquearLote`, `invalidateInventario` (Tarea 2); `almacenesDe`, `useOpcionesEstablecimientos` (Tareas 3 y 5); `SkuSelect` (Tarea 6); `nuevaClaveIdempotencia` (Tarea 3); `describeApiError` de `shared/lib/describe-api-error`; `apiClient`.
- Produces:
  - `AjusteForm({ posicion: Posicion | null; almacenes: OpcionAlmacen[]; isSubmitting: boolean; error: string | null; onSubmit(values: AjusteFormValues): void })`. Con `posicion`: muestra lote y stock físico, tipo y cantidad/motivo. Sin `posicion`: almacén, búsqueda y selector de SKU, número de lote, vencimiento, cantidad y motivo (tipo fijo `AJUSTE_INGRESO`).
  - `AjusteDialog({ posicion: Posicion | null; onClose(): void })`.
  - `BloqueoDialog({ loteId: string; onClose(): void })`, `DesbloqueoDialog({ loteId: string; onClose(): void })`.
  - Etiquetas accesibles: `Almacén`, `Buscar SKU`, `SKU`, `Número de lote`, `Fecha de vencimiento`, `Tipo de ajuste`, `Cantidad`, `Motivo`; botones `Registrar ajuste`, `Registrar ingreso`, `Bloquear lote`, `Desbloquear lote`.

- [ ] **Step 1: Tests (fallan)**

`FE/components/AjusteForm.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import { samplePosicion, sampleSku } from '../../../test/inventario-fixtures';
import { AjusteForm } from './AjusteForm';

const almacenes = [
  { id: 'alm-1', nombre: 'Almacén Central', establecimiento: 'Botica Central' },
  { id: 'alm-3', nombre: 'Almacén Norte', establecimiento: 'Botica Norte' }
];

function renderForm(posicion: Parameters<typeof AjusteForm>[0]['posicion'], error: string | null = null) {
  const onSubmit = vi.fn();
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <AjusteForm
        posicion={posicion}
        almacenes={almacenes}
        isSubmitting={false}
        error={error}
        onSubmit={onSubmit}
      />
    </QueryClientProvider>
  );
  return { onSubmit, user: userEvent.setup() };
}

beforeEach(() => {
  server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku]))));
});

describe('AjusteForm', () => {
  it('desde una fila precarga el lote y envía una salida sin pedir datos del lote', async () => {
    const { onSubmit, user } = renderForm(samplePosicion);

    expect(screen.getByText('L001')).toBeInTheDocument();
    expect(screen.queryByLabelText('Almacén')).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Tipo de ajuste'), 'AJUSTE_SALIDA');
    await user.type(screen.getByLabelText('Cantidad'), '3');
    await user.type(screen.getByLabelText('Motivo'), 'Merma');
    await user.click(screen.getByRole('button', { name: 'Registrar ajuste' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        almacenId: 'alm-1',
        skuId: 'sku-0001-aaaa',
        tipo: 'AJUSTE_SALIDA',
        loteId: 'lote-1',
        numeroLote: '',
        fechaVencimiento: '',
        cantidad: '3',
        motivo: 'Merma'
      })
    );
  });

  it('sin fila registra un ingreso con lote nuevo', async () => {
    const { onSubmit, user } = renderForm(null);

    await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-3');
    await user.type(screen.getByLabelText('Buscar SKU'), 'para');
    await user.selectOptions(
      screen.getByLabelText('SKU'),
      await screen.findByRole('option', { name: 'MED-001 — Paracetamol 500 mg' })
    );
    await user.type(screen.getByLabelText('Número de lote'), 'L-NEW');
    await user.type(screen.getByLabelText('Fecha de vencimiento'), '2030-01-01');
    await user.type(screen.getByLabelText('Cantidad'), '12');
    await user.type(screen.getByLabelText('Motivo'), 'Ingreso inicial');
    await user.click(screen.getByRole('button', { name: 'Registrar ingreso' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        almacenId: 'alm-3',
        skuId: 'sku-0001-aaaa',
        tipo: 'AJUSTE_INGRESO',
        loteId: '',
        numeroLote: 'L-NEW',
        fechaVencimiento: '2030-01-01',
        cantidad: '12',
        motivo: 'Ingreso inicial'
      })
    );
  });

  it('muestra los errores de validación sin enviar', async () => {
    const { onSubmit, user } = renderForm(null);

    await user.click(screen.getByRole('button', { name: 'Registrar ingreso' }));

    expect(await screen.findByText('Selecciona un almacén.')).toBeInTheDocument();
    expect(screen.getByText('Selecciona un SKU.')).toBeInTheDocument();
    expect(screen.getByText('Indica el número del lote.')).toBeInTheDocument();
    expect(screen.getByText('Indica la fecha de vencimiento.')).toBeInTheDocument();
    expect(screen.getByText('El motivo es obligatorio.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error recibido del servidor', () => {
    renderForm(samplePosicion, 'Stock insuficiente.');

    expect(screen.getByRole('alert')).toHaveTextContent('Stock insuficiente.');
  });
});
```

`FE/components/AjusteDialog.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  sampleEstructura,
  sampleMovimiento,
  samplePosicion,
  sampleSku
} from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { AjusteDialog } from './AjusteDialog';

const movimientosUrl = '*/api/v1/inventario/movimientos';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku])))
  );
});

function renderDialog(posicion: typeof samplePosicion | null, onClose = vi.fn()) {
  const Dialog = () => <AjusteDialog posicion={posicion} onClose={onClose} />;
  return { onClose, ...renderRoute('/inventario', Dialog, '/inventario') };
}

describe('AjusteDialog', () => {
  it('desde una fila registra el ajuste con Idempotency-Key, cierra e invalida inventario', async () => {
    let body: unknown;
    let key: string | null = null;
    server.use(
      http.post(movimientosUrl, async ({ request }) => {
        body = await request.json();
        key = request.headers.get('Idempotency-Key');
        return HttpResponse.json(sampleMovimiento, { status: 201 });
      })
    );
    const { onClose, user, queryClient } = renderDialog(samplePosicion);
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    expect(screen.getByRole('heading', { name: 'Ajustar stock del lote' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('Cantidad'), '5');
    await user.type(screen.getByLabelText('Motivo'), 'Conteo');
    await user.click(screen.getByRole('button', { name: 'Registrar ajuste' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(body).toEqual({
      almacenId: 'alm-1',
      skuId: 'sku-0001-aaaa',
      tipo: 'AJUSTE_INGRESO',
      cantidad: 5,
      motivo: 'Conteo',
      loteId: 'lote-1'
    });
    expect(key).toMatch(/^[0-9a-f-]{36}$/);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });

  it('sin fila ofrece los almacenes de la estructura corporativa', async () => {
    renderDialog(null);

    expect(
      screen.getByRole('heading', { name: 'Registrar ingreso con lote nuevo' })
    ).toBeInTheDocument();
    expect(
      await screen.findByRole('option', { name: 'Botica Norte — Almacén Norte' })
    ).toBeInTheDocument();
  });

  it('muestra el error del backend y no cierra el diálogo', async () => {
    server.use(
      http.post(movimientosUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'El estado del lote no admite ingresos de stock.' },
          { status: 409 }
        )
      )
    );
    const { onClose, user } = renderDialog(samplePosicion);

    await user.type(screen.getByLabelText('Cantidad'), '5');
    await user.type(screen.getByLabelText('Motivo'), 'Conteo');
    await user.click(screen.getByRole('button', { name: 'Registrar ajuste' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El estado del lote no admite ingresos de stock.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });
});
```

`FE/components/BloqueoDialog.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleLote } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { BloqueoDialog } from './BloqueoDialog';

const url = '*/api/v1/inventario/lotes/lote-1/bloqueos';

function renderDialog(onClose = vi.fn()) {
  const Dialog = () => <BloqueoDialog loteId="lote-1" onClose={onClose} />;
  return { onClose, ...renderRoute('/inventario', Dialog, '/inventario') };
}

describe('BloqueoDialog', () => {
  it('exige el motivo antes de enviar', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Bloquear lote' }));

    expect(await screen.findByText('El motivo es obligatorio.')).toBeInTheDocument();
  });

  it('bloquea el lote con el motivo, cierra e invalida inventario', async () => {
    let body: unknown;
    server.use(
      http.post(url, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleLote, estado: 'BLOQUEADO' });
      })
    );
    const { onClose, user, queryClient } = renderDialog();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await user.type(screen.getByLabelText('Motivo'), 'Control de calidad');
    await user.click(screen.getByRole('button', { name: 'Bloquear lote' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(body).toEqual({ motivo: 'Control de calidad' });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });

  it('muestra el error del backend sin cerrar', async () => {
    server.use(
      http.post(url, () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'Solo un lote habilitado o en cuarentena puede bloquearse.' },
          { status: 409 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.type(screen.getByLabelText('Motivo'), 'Control');
    await user.click(screen.getByRole('button', { name: 'Bloquear lote' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Solo un lote habilitado o en cuarentena puede bloquearse.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });
});
```

`FE/components/DesbloqueoDialog.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleLote } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { DesbloqueoDialog } from './DesbloqueoDialog';

const url = '*/api/v1/inventario/lotes/lote-1/bloqueos';

function renderDialog(onClose = vi.fn()) {
  const Dialog = () => <DesbloqueoDialog loteId="lote-1" onClose={onClose} />;
  return { onClose, ...renderRoute('/inventario', Dialog, '/inventario') };
}

describe('DesbloqueoDialog', () => {
  it('desbloquea el lote, cierra e invalida inventario', async () => {
    server.use(http.delete(url, () => HttpResponse.json(sampleLote)));
    const { onClose, user, queryClient } = renderDialog();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await user.click(screen.getByRole('button', { name: 'Desbloquear lote' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });

  it('muestra el error del backend sin cerrar', async () => {
    server.use(
      http.delete(url, () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'Un lote vencido no puede habilitarse.' },
          { status: 409 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Desbloquear lote' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Un lote vencido no puede habilitarse.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/components`
Expected: FAIL — `AjusteForm`, `AjusteDialog`, `BloqueoDialog` y `DesbloqueoDialog` no existen.

- [ ] **Step 3: Implementar `AjusteForm.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import { TIPOS_AJUSTE, type Posicion } from '../api/inventario.types';
import { AJUSTE_FORM_VACIO, ajusteDesdePosicion } from '../lib/ajuste';
import type { OpcionAlmacen } from '../lib/estructura';
import { ajusteSchema, type AjusteFormValues } from '../schemas/ajuste.schema';
import { SkuSelect } from './SkuSelect';

type AjusteFormProps = {
  posicion: Posicion | null;
  almacenes: OpcionAlmacen[];
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (values: AjusteFormValues) => void;
};

export function AjusteForm({
  posicion,
  almacenes,
  isSubmitting,
  error,
  onSubmit
}: AjusteFormProps) {
  const [busquedaSku, setBusquedaSku] = useState('');
  const {
    formState: { errors },
    handleSubmit,
    register,
    setValue,
    control
  } = useForm<AjusteFormValues>({
    defaultValues: posicion ? ajusteDesdePosicion(posicion) : AJUSTE_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(ajusteSchema)
  });
  const skuId = useWatch({ control, name: 'skuId' });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {posicion ? (
        <dl className="grid gap-4 sm:grid-cols-2">
          <DatoItem label="Lote">{posicion.numeroLote}</DatoItem>
          <DatoItem label="Stock físico">{posicion.cantidadFisica}</DatoItem>
        </dl>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            id="ajuste-almacen"
            label="Almacén"
            error={errors.almacenId?.message}
            {...register('almacenId')}
          >
            <option value="">Selecciona un almacén</option>
            {almacenes.map((almacen) => (
              <option key={almacen.id} value={almacen.id}>
                {almacen.establecimiento} — {almacen.nombre}
              </option>
            ))}
          </SelectField>
          <TextField
            id="ajuste-buscar-sku"
            label="Buscar SKU"
            value={busquedaSku}
            onChange={(event) => setBusquedaSku(event.target.value)}
          />
          <SkuSelect
            id="ajuste-sku"
            label="SKU"
            placeholder="Selecciona un SKU"
            search={busquedaSku}
            value={skuId}
            error={errors.skuId?.message}
            onChange={(id) => setValue('skuId', id, { shouldValidate: true, shouldDirty: true })}
          />
          <TextField
            id="ajuste-numero-lote"
            label="Número de lote"
            error={errors.numeroLote?.message}
            {...register('numeroLote')}
          />
          <TextField
            id="ajuste-vencimiento"
            label="Fecha de vencimiento"
            type="date"
            error={errors.fechaVencimiento?.message}
            {...register('fechaVencimiento')}
          />
        </div>
      )}
      <div className="grid gap-4 sm:grid-cols-2">
        {posicion ? (
          <SelectField
            id="ajuste-tipo"
            label="Tipo de ajuste"
            error={errors.tipo?.message}
            {...register('tipo')}
          >
            {TIPOS_AJUSTE.map((tipo) => (
              <option key={tipo} value={tipo}>
                {tipo}
              </option>
            ))}
          </SelectField>
        ) : null}
        <TextField
          id="ajuste-cantidad"
          label="Cantidad"
          inputMode="decimal"
          error={errors.cantidad?.message}
          {...register('cantidad')}
        />
      </div>
      <TextField
        id="ajuste-motivo"
        label="Motivo"
        error={errors.motivo?.message}
        {...register('motivo')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {posicion ? 'Registrar ajuste' : 'Registrar ingreso'}
      </Button>
    </form>
  );
}
```

- [ ] **Step 4: Implementar los diálogos**

`FE/components/AjusteDialog.tsx`:

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateInventario } from '../api/invalidate';
import { registrarMovimiento } from '../api/movimientos.api';
import type { Posicion } from '../api/inventario.types';
import { toRegistrarMovimientoPayload } from '../lib/ajuste';
import { almacenesDe } from '../lib/estructura';
import { nuevaClaveIdempotencia } from '../lib/idempotencia';
import { useOpcionesEstablecimientos } from '../lib/use-opciones-establecimientos';
import type { AjusteFormValues } from '../schemas/ajuste.schema';
import { AjusteForm } from './AjusteForm';

type AjusteDialogProps = {
  posicion: Posicion | null;
  onClose: () => void;
};

export function AjusteDialog({ posicion, onClose }: AjusteDialogProps) {
  const queryClient = useQueryClient();
  const almacenes = almacenesDe(useOpcionesEstablecimientos(), undefined);

  const mutation = useMutation({
    mutationFn: (values: AjusteFormValues) =>
      registrarMovimiento(
        apiClient,
        toRegistrarMovimientoPayload(values),
        nuevaClaveIdempotencia()
      ),
    onSuccess: () => {
      onClose();
      void invalidateInventario(queryClient);
    }
  });

  return (
    <Modal
      open
      onClose={onClose}
      title={posicion ? 'Ajustar stock del lote' : 'Registrar ingreso con lote nuevo'}
      size="lg"
    >
      <AjusteForm
        posicion={posicion}
        almacenes={almacenes}
        isSubmitting={mutation.isPending}
        error={mutation.isError ? describeApiError(mutation.error) : null}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </Modal>
  );
}
```

`FE/components/BloqueoDialog.tsx`:

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateInventario } from '../api/invalidate';
import { bloquearLote } from '../api/lotes.api';
import { bloqueoSchema, type BloqueoFormValues } from '../schemas/bloqueo.schema';

type BloqueoDialogProps = {
  loteId: string;
  onClose: () => void;
};

export function BloqueoDialog({ loteId, onClose }: BloqueoDialogProps) {
  const queryClient = useQueryClient();
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<BloqueoFormValues>({
    defaultValues: { motivo: '' },
    mode: 'onTouched',
    resolver: zodResolver(bloqueoSchema)
  });

  const mutation = useMutation({
    mutationFn: (values: BloqueoFormValues) =>
      bloquearLote(apiClient, loteId, { motivo: values.motivo }),
    onSuccess: () => {
      onClose();
      void invalidateInventario(queryClient);
    }
  });

  return (
    <Modal open onClose={onClose} title="Bloquear lote">
      <form
        className="space-y-4"
        noValidate
        onSubmit={(event) => {
          void handleSubmit((values) => mutation.mutate(values))(event);
        }}
      >
        <TextField
          id="bloqueo-motivo"
          label="Motivo"
          error={errors.motivo?.message}
          {...register('motivo')}
        />
        {mutation.isError ? <FormError message={describeApiError(mutation.error)} /> : null}
        <Button type="submit" disabled={mutation.isPending}>
          Bloquear lote
        </Button>
      </form>
    </Modal>
  );
}
```

`FE/components/DesbloqueoDialog.tsx`:

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateInventario } from '../api/invalidate';
import { desbloquearLote } from '../api/lotes.api';

type DesbloqueoDialogProps = {
  loteId: string;
  onClose: () => void;
};

export function DesbloqueoDialog({ loteId, onClose }: DesbloqueoDialogProps) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => desbloquearLote(apiClient, loteId),
    onSuccess: () => {
      onClose();
      void invalidateInventario(queryClient);
    }
  });

  return (
    <Modal open onClose={onClose} title="Desbloquear lote">
      <div className="space-y-4">
        <p className="text-sm text-neutral-600 dark:text-neutral-300">
          El lote volverá a estar habilitado. Un lote vencido no puede habilitarse.
        </p>
        {mutation.isError ? <FormError message={describeApiError(mutation.error)} /> : null}
        <Button disabled={mutation.isPending} onClick={() => mutation.mutate()}>
          Desbloquear lote
        </Button>
      </div>
    </Modal>
  );
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/components`
Expected: PASS. Si el test "desde una fila" no recibe `almacenId`/`skuId`/`loteId` en `onSubmit` (campos sin registrar), registra esos tres campos con `<input type="hidden" {...register('almacenId')} />` etc. dentro del bloque `posicion` y vuelve a ejecutar; no cambies el schema.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/components
git commit -m "feat(inventario): agregar formulario de ajuste y dialogos de bloqueo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Páginas `InventoryPage` y `LoteDetailPage`

**Files:**
- Modify (reemplazo total): `FE/pages/InventoryPage.tsx`
- Create: `FE/pages/InventoryPage.test.tsx`, `FE/pages/LoteDetailPage.tsx`, `FE/pages/LoteDetailPage.test.tsx`

**Interfaces:**
- Consumes: `posicionesQuery`, `loteQuery` (Tarea 2); `usePosicionesFiltros`, `useOpcionesEstablecimientos`, `almacenesDe` (Tareas 3 y 5); `PosicionesTable`, `SkuSelect`, `AjusteDialog`, `BloqueoDialog`, `DesbloqueoDialog`, `EstadoLoteBadge`, `VencimientoCelda` (Tareas 6 y 7); `puedeBloquear`, `puedeDesbloquear`, `formatearInstante`; `useRouteParam` de `shared/lib/use-route-param`; `valueOrDash`, `yesNo` de `shared/lib/format`.
- Produces: `InventoryPage()` (ruta `/inventario`), `LoteDetailPage()` (ruta `/inventario/lotes/:loteId`, parámetro `loteId`).

- [ ] **Step 1: Tests de `InventoryPage` (fallan)**

`FE/pages/InventoryPage.test.tsx`:

```tsx
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  sampleEstructura,
  sampleMovimiento,
  samplePosicion,
  sampleSku
} from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { InventoryPage } from './InventoryPage';

const posicionesUrl = '*/api/v1/inventario/posiciones';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku])))
  );
});

function renderPage(entrada = '/inventario') {
  return renderRoute('/inventario', InventoryPage, entrada);
}

describe('InventoryPage', () => {
  it('lista las posiciones con cantidades, estado y acciones', async () => {
    server.use(http.get(posicionesUrl, () => HttpResponse.json(pagina([samplePosicion]))));

    renderPage();

    expect(await screen.findByText('L001')).toBeInTheDocument();
    expect(screen.getByText('HABILITADO')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver detalle del lote L001' })).toHaveAttribute(
      'href',
      '/inventario/lotes/lote-1'
    );
  });

  it('muestra el estado de carga, el vacío y el error', async () => {
    server.use(http.get(posicionesUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('No hay stock registrado con los filtros indicados.')
    ).toBeInTheDocument();
    primera.unmount();

    server.use(http.get(posicionesUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderPage();

    expect(await screen.findByText('No se pudo cargar el inventario.')).toBeInTheDocument();
  });

  it('filtra por establecimiento, almacén y SKU y refleja los filtros en la URL', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(posicionesUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([samplePosicion]));
      })
    );
    const { user, router } = renderPage();
    await screen.findByText('L001');

    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');
    await waitFor(() => expect(received.get('establecimientoId')).toBe('est-1'));
    const almacen = screen.getByLabelText('Almacén');
    expect(within(almacen).queryByRole('option', { name: 'Almacén Norte' })).not.toBeInTheDocument();
    await user.selectOptions(almacen, 'alm-2');
    await waitFor(() => expect(received.get('almacenId')).toBe('alm-2'));
    await user.selectOptions(
      screen.getByLabelText('SKU'),
      await screen.findByRole('option', { name: 'MED-001 — Paracetamol 500 mg' })
    );
    await waitFor(() => expect(received.get('skuId')).toBe('sku-0001-aaaa'));

    expect(router.state.location.search).toBe(
      '?establecimientoId=est-1&almacenId=alm-2&skuId=sku-0001-aaaa'
    );
  });

  it('pagina y cambia el tamaño de página', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(posicionesUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([samplePosicion], { totalElements: 120 }));
      })
    );
    const { user } = renderPage();
    await screen.findByText('L001');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Filas por página' }), '50');
    await waitFor(() => expect(received.get('size')).toBe('50'));
    expect(received.get('page')).toBe('0');
  });

  it('abre el ajuste de una fila y lo registra', async () => {
    server.use(
      http.get(posicionesUrl, () => HttpResponse.json(pagina([samplePosicion]))),
      http.post('*/api/v1/inventario/movimientos', () =>
        HttpResponse.json(sampleMovimiento, { status: 201 })
      )
    );
    const { user } = renderPage();

    await user.click(await screen.findByRole('button', { name: 'Ajustar stock del lote L001' }));
    const dialogo = screen.getByRole('dialog');
    await user.type(within(dialogo).getByLabelText('Cantidad'), '5');
    await user.type(within(dialogo).getByLabelText('Motivo'), 'Conteo');
    await user.click(within(dialogo).getByRole('button', { name: 'Registrar ajuste' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('abre y cierra el diálogo de ingreso con lote nuevo desde la cabecera', async () => {
    server.use(http.get(posicionesUrl, () => HttpResponse.json(pagina([samplePosicion]))));
    const { user } = renderPage();
    await screen.findByText('L001');

    await user.click(screen.getByRole('button', { name: 'Registrar ingreso' }));
    expect(
      screen.getByRole('heading', { name: 'Registrar ingreso con lote nuevo' })
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Tests de `LoteDetailPage` (fallan)**

`FE/pages/LoteDetailPage.test.tsx`:

```tsx
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleLote } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import type { EstadoLote } from '../api/inventario.types';
import { LoteDetailPage } from './LoteDetailPage';

const loteUrl = '*/api/v1/inventario/lotes/lote-1';

function renderPage() {
  return renderRoute('/inventario/lotes/:loteId', LoteDetailPage, '/inventario/lotes/lote-1');
}

describe('LoteDetailPage', () => {
  it('muestra los datos del lote habilitado y permite bloquearlo', async () => {
    server.use(http.get(loteUrl, () => HttpResponse.json(sampleLote)));

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Lote L001' })).toBeInTheDocument();
    expect(screen.getByText('HABILITADO')).toBeInTheDocument();
    expect(screen.getByText('2099-01-01')).toBeInTheDocument();
    expect(screen.getByText('Sí')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Inventario' })).toHaveAttribute('href', '/inventario');
    expect(screen.getByRole('button', { name: 'Bloquear lote' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Desbloquear lote' })).not.toBeInTheDocument();
  });

  it.each<[EstadoLote, string | null]>([
    ['HABILITADO', 'Bloquear lote'],
    ['CUARENTENA', 'Bloquear lote'],
    ['BLOQUEADO', 'Desbloquear lote'],
    ['INMOVILIZADO_RECALL', null],
    ['VENCIDO', null],
    ['BAJA_DESTRUIDO', null]
  ])('en estado %s la acción disponible es %s', async (estado, accion) => {
    server.use(http.get(loteUrl, () => HttpResponse.json({ ...sampleLote, estado })));

    renderPage();

    await screen.findByRole('heading', { name: 'Lote L001' });
    ['Bloquear lote', 'Desbloquear lote'].forEach((nombre) => {
      const boton = screen.queryByRole('button', { name: nombre });
      if (nombre === accion) expect(boton).toBeInTheDocument();
      else expect(boton).not.toBeInTheDocument();
    });
  });

  it('bloquea el lote con motivo desde el diálogo', async () => {
    let body: unknown;
    server.use(
      http.get(loteUrl, () => HttpResponse.json(sampleLote)),
      http.post(`${loteUrl}/bloqueos`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleLote, estado: 'BLOQUEADO' });
      })
    );
    const { user } = renderPage();

    await user.click(await screen.findByRole('button', { name: 'Bloquear lote' }));
    const dialogo = screen.getByRole('dialog');
    await user.type(within(dialogo).getByLabelText('Motivo'), 'Control de calidad');
    await user.click(within(dialogo).getByRole('button', { name: 'Bloquear lote' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(body).toEqual({ motivo: 'Control de calidad' });
  });

  it('muestra el motivo del bloqueo y desbloquea el lote', async () => {
    server.use(
      http.get(loteUrl, () =>
        HttpResponse.json({
          ...sampleLote,
          estado: 'BLOQUEADO',
          motivoEstado: 'Control de calidad',
          bloqueadoAt: '2026-10-02T15:30:00Z',
          vendible: false
        })
      ),
      http.delete(`${loteUrl}/bloqueos`, () => HttpResponse.json(sampleLote))
    );
    const { user } = renderPage();

    expect(await screen.findByText('Control de calidad')).toBeInTheDocument();
    expect(screen.getByText('No')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Desbloquear lote' }));
    const dialogo = screen.getByRole('dialog');
    await user.click(within(dialogo).getByRole('button', { name: 'Desbloquear lote' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('cierra el diálogo sin guardar', async () => {
    server.use(http.get(loteUrl, () => HttpResponse.json(sampleLote)));
    const { user } = renderPage();

    await user.click(await screen.findByRole('button', { name: 'Bloquear lote' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el estado de carga y el error de la consulta', async () => {
    server.use(
      http.get(loteUrl, () => HttpResponse.json({ title: 'No encontrado' }, { status: 404 }))
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });
});
```

- [ ] **Step 3: Ejecutar y verificar que falla**

Run: `pnpm vitest run apps/erp-web/src/features/inventario/pages`
Expected: FAIL — la página actual es el fixture y `LoteDetailPage` no existe.

- [ ] **Step 4: Implementar `InventoryPage.tsx` (reemplaza todo el contenido)**

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, ListFilters, PageHeader } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import { posicionesQuery } from '../api/posiciones.api';
import type { Posicion } from '../api/inventario.types';
import { AjusteDialog } from '../components/AjusteDialog';
import { PosicionesTable } from '../components/PosicionesTable';
import { SkuSelect } from '../components/SkuSelect';
import { almacenesDe } from '../lib/estructura';
import { useOpcionesEstablecimientos } from '../lib/use-opciones-establecimientos';
import { usePosicionesFiltros } from '../lib/use-posiciones-filtros';

export function InventoryPage() {
  const [dialogo, setDialogo] = useState<{ posicion: Posicion | null } | null>(null);
  const [busquedaSku, setBusquedaSku] = useState('');
  const { filtros, setEstablecimiento, setAlmacen, setSku, setPage, setSize } =
    usePosicionesFiltros();
  const establecimientos = useOpcionesEstablecimientos();

  const { data, isPending, isError } = useQuery(posicionesQuery(filtros));

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Inventario"
        context="Operaciones / Inventario"
        description="Stock por almacén, lote y vencimiento."
        actions={<Button onClick={() => setDialogo({ posicion: null })}>Registrar ingreso</Button>}
      />

      <ListFilters
        label="Buscar SKU"
        placeholder="Código o descripción"
        value={busquedaSku}
        onValueChange={setBusquedaSku}
      >
        <div className="w-full sm:w-56">
          <SelectField
            id="filtro-establecimiento"
            label="Establecimiento"
            value={filtros.establecimientoId}
            onChange={(event) => setEstablecimiento(event.target.value)}
          >
            <option value="">Todos</option>
            {establecimientos.map((establecimiento) => (
              <option key={establecimiento.id} value={establecimiento.id}>
                {establecimiento.nombre}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="w-full sm:w-56">
          <SelectField
            id="filtro-almacen"
            label="Almacén"
            value={filtros.almacenId}
            onChange={(event) => setAlmacen(event.target.value)}
          >
            <option value="">Todos</option>
            {almacenesDe(establecimientos, filtros.establecimientoId).map((almacen) => (
              <option key={almacen.id} value={almacen.id}>
                {almacen.nombre}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="w-full sm:w-64">
          <SkuSelect
            id="filtro-sku"
            label="SKU"
            placeholder="Todos"
            search={busquedaSku}
            value={filtros.skuId}
            onChange={setSku}
          />
        </div>
      </ListFilters>

      <div className="mt-6">
        <PosicionesTable
          rows={data?.items ?? []}
          isLoading={isPending}
          isError={isError}
          pagination={{
            page: filtros.page,
            size: filtros.size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
          onAjustar={(posicion) => setDialogo({ posicion })}
        />
      </div>

      {dialogo ? (
        <AjusteDialog posicion={dialogo.posicion} onClose={() => setDialogo(null)} />
      ) : null}
    </div>
  );
}
```

- [ ] **Step 5: Implementar `LoteDetailPage.tsx`**

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { valueOrDash, yesNo } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { loteQuery } from '../api/lotes.api';
import { BloqueoDialog } from '../components/BloqueoDialog';
import { DesbloqueoDialog } from '../components/DesbloqueoDialog';
import { EstadoLoteBadge } from '../components/EstadoLoteBadge';
import { VencimientoCelda } from '../components/VencimientoCelda';
import { puedeBloquear, puedeDesbloquear } from '../lib/estado-lote';
import { formatearInstante } from '../lib/formato';

export function LoteDetailPage() {
  const loteId = useRouteParam('loteId');
  const [dialogo, setDialogo] = useState<'bloquear' | 'desbloquear' | null>(null);

  const result = useQuery(loteQuery(loteId));

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const lote = result.data;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={`Lote ${lote.numeroLote}`}
        context={<Link to="/inventario">Inventario</Link>}
        description="Detalle del lote y control de bloqueo."
        actions={
          <>
            {puedeBloquear(lote.estado) ? (
              <Button variant="secondary" onClick={() => setDialogo('bloquear')}>
                Bloquear lote
              </Button>
            ) : null}
            {puedeDesbloquear(lote.estado) ? (
              <Button variant="secondary" onClick={() => setDialogo('desbloquear')}>
                Desbloquear lote
              </Button>
            ) : null}
          </>
        }
      />

      <Card className="mt-6 p-6">
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Estado">
            <EstadoLoteBadge estado={lote.estado} />
          </DatoItem>
          <DatoItem label="Número de lote">{lote.numeroLote}</DatoItem>
          <DatoItem label="SKU">{lote.skuId}</DatoItem>
          <DatoItem label="Vencimiento">
            <VencimientoCelda fecha={lote.fechaVencimiento} />
          </DatoItem>
          <DatoItem label="Vendible">{yesNo(lote.vendible)}</DatoItem>
          <DatoItem label="Motivo del estado">{valueOrDash(lote.motivoEstado)}</DatoItem>
          <DatoItem label="Bloqueado el">{formatearInstante(lote.bloqueadoAt)}</DatoItem>
        </dl>
      </Card>

      {dialogo === 'bloquear' ? (
        <BloqueoDialog loteId={lote.id} onClose={() => setDialogo(null)} />
      ) : null}
      {dialogo === 'desbloquear' ? (
        <DesbloqueoDialog loteId={lote.id} onClose={() => setDialogo(null)} />
      ) : null}
    </div>
  );
}
```

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario`
Expected: PASS en todo `features/inventario`. Los textos `Sí` y `No` del test de detalle deben ser únicos en la vista (solo el campo "Vendible"); si `getByText('Sí')` encuentra más de un nodo, usa `within` sobre la fila del campo o `getByText('Sí', { selector: 'dd' })`.

- [ ] **Step 7: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/pages
git commit -m "feat(inventario): reemplazar el fixture por posiciones reales y detalle de lote

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Rutas, baseline de cobertura y registro de la feature

**Files:**
- Modify: `FE/routes.tsx`
- Create: `FE/routes.test.ts`
- Modify: `frontend/apps/erp-web/src/app/feature-routes.test.ts`
- Modify: `frontend/coverage-baseline.txt` (quitar la línea de `features/inventario/routes.tsx`)

**Interfaces:**
- Consumes: `InventoryPage`, `LoteDetailPage` (Tarea 8).
- Produces: `inventoryRoutes` con los paths `inventario` e `inventario/lotes/:loteId`.

- [ ] **Step 1: Tests (fallan)**

`FE/routes.test.ts`:

```ts
import { inventoryRoutes } from './routes';

describe('inventoryRoutes', () => {
  it('declara las rutas de la feature', () => {
    expect(inventoryRoutes.map(({ path }) => path)).toEqual([
      'inventario',
      'inventario/lotes/:loteId'
    ]);
  });

  it.each(inventoryRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
```

En `frontend/apps/erp-web/src/app/feature-routes.test.ts`, después de `'inventario',` agrega `'inventario/lotes/:loteId',`.

Run: `pnpm vitest run apps/erp-web/src/features/inventario/routes.test.ts apps/erp-web/src/app/feature-routes.test.ts`
Expected: FAIL — falta la ruta del detalle.

- [ ] **Step 2: Implementar**

`FE/routes.tsx`:

```tsx
import type { RouteObject } from 'react-router';

export const inventoryRoutes = [
  {
    path: 'inventario',
    lazy: async () => {
      const { InventoryPage } = await import('./pages/InventoryPage');
      return { Component: InventoryPage };
    }
  },
  {
    path: 'inventario/lotes/:loteId',
    lazy: async () => {
      const { LoteDetailPage } = await import('./pages/LoteDetailPage');
      return { Component: LoteDetailPage };
    }
  }
] satisfies RouteObject[];
```

En `frontend/coverage-baseline.txt` elimina la línea `apps/erp-web/src/features/inventario/routes.tsx` y cualquier otra entrada de `features/inventario/` que ya esté cubierta (`grep -n "inventario" frontend/coverage-baseline.txt`).

- [ ] **Step 3: Ejecutar y verificar que pasa**

Run: `pnpm vitest run apps/erp-web/src/features/inventario apps/erp-web/src/app/feature-routes.test.ts`
Expected: PASS.

- [ ] **Step 4: Cobertura al 100% de la feature**

Run: `pnpm vitest run --coverage --coverage.include="apps/erp-web/src/features/inventario/**" --coverage.include="apps/erp-web/src/shared/lib/query-string.ts" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: sin archivos bajo 100%. Para cualquier archivo con líneas o ramas sin cubrir, agrega al test correspondiente el caso exacto que lo ejercita (el reporte indica el número de línea) y vuelve a correr.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/inventario/routes.tsx frontend/apps/erp-web/src/features/inventario/routes.test.ts frontend/apps/erp-web/src/app/feature-routes.test.ts frontend/coverage-baseline.txt
git commit -m "feat(inventario): registrar la ruta de detalle de lote y retirar la feature del baseline

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 10: E2E con Playwright (API simulada)

**Files:**
- Create: `frontend/e2e/support/layout.ts`
- Create: `frontend/e2e/support/inventario-api.ts`
- Create: `frontend/e2e/inventario.spec.ts`

**Interfaces:**
- Consumes: `json`, `login` de `frontend/e2e/support/login.ts`; `sampleEstructura`, `sampleSku`, `samplePosicion`, `sampleLote` de `apps/erp-web/src/test/inventario-fixtures`.
- Produces: `expectNoHorizontalOverflow(page)`, `abrirInventarioEn(page, path)`.

- [ ] **Step 1: Helper de layout**

`frontend/e2e/support/layout.ts`:

```ts
import { expect, type Page } from '@playwright/test';

export async function expectNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - window.innerWidth
  );
  expect(overflow).toBeLessThanOrEqual(1);
}
```

- [ ] **Step 2: API simulada**

`frontend/e2e/support/inventario-api.ts`:

```ts
import type { Page } from '@playwright/test';
import {
  sampleEstructura,
  sampleLote,
  samplePosicion,
  sampleSku
} from '../../apps/erp-web/src/test/inventario-fixtures';
import type { Lote, Posicion } from '../../apps/erp-web/src/features/inventario/api/inventario.types';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

export async function mockInventarioApi(page: Page) {
  const lotes = new Map<string, Lote>([
    [sampleLote.id, sampleLote],
    ['lote-2', { ...sampleLote, id: 'lote-2', numeroLote: 'L002' }]
  ]);
  const posiciones: Posicion[] = [
    samplePosicion,
    {
      ...samplePosicion,
      id: 'pos-2',
      almacenId: 'alm-3',
      establecimientoId: 'est-2',
      loteId: 'lote-2',
      numeroLote: 'L002'
    }
  ];

  await page.route('**/api/v1/estructura-corporativa', (route) =>
    json(route, 200, sampleEstructura)
  );
  await page.route(/\/api\/v1\/catalogo\/skus(\?|$)/, (route) =>
    json(route, 200, pagina([sampleSku]))
  );

  await page.route('**/api/v1/inventario/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const [recurso = '', id, accion] = url.pathname.split('/').slice(4);
    const body = request.postData() ? (JSON.parse(request.postData() ?? '{}') as Record<string, unknown>) : {};

    if (recurso === 'posiciones') {
      const almacenId = url.searchParams.get('almacenId');
      return json(
        route,
        200,
        pagina(posiciones.filter((posicion) => !almacenId || posicion.almacenId === almacenId))
      );
    }

    if (recurso === 'movimientos' && request.method() === 'POST') {
      const loteId = `lote-${posiciones.length + 1}`;
      lotes.set(loteId, { ...sampleLote, id: loteId, numeroLote: String(body['numeroLote']) });
      posiciones.push({
        ...samplePosicion,
        id: `pos-${posiciones.length + 1}`,
        almacenId: String(body['almacenId']),
        loteId,
        numeroLote: String(body['numeroLote']),
        cantidadFisica: Number(body['cantidad']),
        cantidadDisponible: Number(body['cantidad'])
      });
      return json(route, 201, {
        id: 'mov-1',
        posicionId: 'pos-x',
        loteId,
        tipo: body['tipo'],
        naturaleza: 'E',
        cantidad: body['cantidad'],
        stockAnterior: 0,
        stockPosterior: body['cantidad'],
        fechaNegocio: '2026-10-02T10:00:00Z'
      });
    }

    const lote = id ? lotes.get(id) : undefined;
    if (recurso !== 'lotes' || !lote) return json(route, 404, { title: 'Not Found' });

    if (accion === 'bloqueos') {
      const actualizado: Lote =
        request.method() === 'POST'
          ? { ...lote, estado: 'BLOQUEADO', motivoEstado: String(body['motivo']), vendible: false }
          : { ...lote, estado: 'HABILITADO', motivoEstado: null, vendible: true };
      lotes.set(lote.id, actualizado);
      return json(route, 200, actualizado);
    }

    return json(route, 200, lote);
  });
}

export async function abrirInventarioEn(page: Page, path: string) {
  await mockInventarioApi(page);
  await login(page);
  await page.goto(path);
}
```

- [ ] **Step 3: Spec e2e**

`frontend/e2e/inventario.spec.ts`:

```ts
import { expect, test } from '@playwright/test';
import { abrirInventarioEn } from './support/inventario-api';
import { expectNoHorizontalOverflow } from './support/layout';

test.describe('Inventario', () => {
  test('lista las posiciones y las filtra por almacén', async ({ page }) => {
    await abrirInventarioEn(page, '/inventario');

    await expect(page.getByRole('heading', { name: 'Inventario', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L001' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L002' })).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByLabel('Almacén', { exact: true }).selectOption('alm-3');

    await expect(page.getByRole('link', { name: 'Ver detalle del lote L002' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L001' })).toBeHidden();
    await expect(page).toHaveURL(/almacenId=alm-3/);
  });

  test('registra un ingreso con lote nuevo desde la cabecera', async ({ page }) => {
    await abrirInventarioEn(page, '/inventario');

    await page.getByRole('button', { name: 'Registrar ingreso' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Almacén').selectOption('alm-1');
    await dialog.getByLabel('SKU', { exact: true }).selectOption({ label: 'MED-001 — Paracetamol 500 mg' });
    await dialog.getByLabel('Número de lote').fill('L-NEW');
    await dialog.getByLabel('Fecha de vencimiento').fill('2030-01-01');
    await dialog.getByLabel('Cantidad').fill('12');
    await dialog.getByLabel('Motivo').fill('Ingreso inicial');
    await dialog.getByRole('button', { name: 'Registrar ingreso' }).click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByRole('link', { name: 'Ver detalle del lote L-NEW' })).toBeVisible();
  });

  test('bloquea y desbloquea un lote desde su detalle', async ({ page }) => {
    await abrirInventarioEn(page, '/inventario');

    await page.getByRole('link', { name: 'Ver detalle del lote L001' }).click();
    await expect(page.getByRole('heading', { name: 'Lote L001' })).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Bloquear lote' }).click();
    await page.getByRole('dialog').getByLabel('Motivo').fill('Control de calidad');
    await page.getByRole('dialog').getByRole('button', { name: 'Bloquear lote' }).click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('BLOQUEADO', { exact: true })).toBeVisible();
    await expect(page.getByText('Control de calidad')).toBeVisible();

    await page.getByRole('button', { name: 'Desbloquear lote' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Desbloquear lote' }).click();

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('HABILITADO', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Bloquear lote' })).toBeVisible();
  });
});
```

- [ ] **Step 4: Ejecutar el e2e**

Run (desde `frontend/`): `pnpm e2e e2e/inventario.spec.ts`
Expected: 3 tests en cada uno de los proyectos desktop, tablet y móvil en verde. Si algún selector no coincide (etiqueta ambigua entre `Almacén` del filtro y del diálogo), restringe con `.getByRole('dialog')` o `{ exact: true }`; no cambies la UI para acomodar el test.

- [ ] **Step 5: Commit**

```bash
git add frontend/e2e/support/layout.ts frontend/e2e/support/inventario-api.ts frontend/e2e/inventario.spec.ts
git commit -m "test(e2e): cubrir posiciones, ingreso con lote nuevo y bloqueo de lote

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Verificación final, prueba real y documentación

**Files:**
- Modify: `docs/superpowers/specs/2026-10-02-inventario-frontend-design.md` (ajustes de la sección "Ajustes al spec")
- Modify: `CLAUDE.md` (párrafo "Estado real del proyecto", frontend)

- [ ] **Step 1: `pnpm check` completo**

Run (desde `frontend/`): `pnpm check`
Expected: lint, typecheck, tests (con el gate de cobertura) y build en verde. Corrige cualquier error de lint/tipos que aparezca (por ejemplo `exactOptionalPropertyTypes` en props opcionales) sin silenciar reglas.

- [ ] **Step 2: Prueba real contra el backend**

1. Levanta PostgreSQL y el backend: `cd service-botica; .\gradlew.bat :bootstrap-app:bootRun`.
2. Levanta el frontend contra el backend: `cd frontend; $env:VITE_API_MODE='http'; pnpm dev` (Vite redirige `/api` a `http://localhost:8080`).
3. Con la sesión del tenant FARMALAB (rol ADMIN con permisos `inventario.*` sembrados por `V027`) y datos existentes de organización (un establecimiento con almacén que `permiteLotes`) y un SKU activo del catálogo, verifica en el navegador:
   - `/inventario` carga las posiciones y los selects de establecimiento/almacén con datos reales.
   - **Registrar ingreso**: crea un ingreso con lote nuevo; la posición aparece al guardar.
   - Desde una fila: un ajuste de salida con cantidad mayor al stock muestra el error del backend dentro del diálogo.
   - Detalle del lote: bloquear con motivo y desbloquear; el estado y el botón cambian.
4. Anota en la respuesta final qué se verificó y qué no pudo verificarse (por ejemplo, si faltaron datos semilla).

- [ ] **Step 3: Actualizar el spec**

En `docs/superpowers/specs/2026-10-02-inventario-frontend-design.md`:
- En "Diálogo Registrar ajuste", reemplaza el texto por los dos modos: desde una fila (precargado; tipo, cantidad, motivo) y desde la cabecera, botón **Registrar ingreso** (almacén, SKU, número de lote, vencimiento, cantidad, motivo; tipo fijo `AJUSTE_INGRESO`), y explica que el backend no expone listado de lotes.
- En "Estados", reemplaza "error con reintento" por "mensaje de error, igual que el resto de listados".
- Agrega bajo "API del backend consumida" que `POST /movimientos` envía `Idempotency-Key` con `crypto.randomUUID()` por envío.
- Agrega en "Pantallas" que el umbral de vencimiento próximo es de 90 días (**POR_VALIDAR**) y vive en `lib/vencimiento.ts`.
- Cambia el estado del documento a "implementado".

- [ ] **Step 4: Actualizar `CLAUDE.md`**

En el párrafo de `frontend/` del "Estado real del proyecto", cambia la mención de que inventario usa fixtures por: `features/inventario/` tiene UI real (posiciones paginadas con filtros por establecimiento/almacén/SKU en la URL, ajustes de stock con `Idempotency-Key`, detalle de lote con bloqueo/desbloqueo) integrada contra `packages/api-client`, sin handlers de MSW, con e2e de Playwright simulado y verificada en navegador contra el backend real. Limitación vigente: `PosicionResponse` no trae nombre del SKU. Actualiza también la frase del backend que dice que "los demás módulos de negocio (ventas, inventario, compras, etc.) aún no tienen controladores, adapters de persistencia ni migraciones propias" para reflejar que `inventario` y `compras` ya los tienen (migraciones V027–V032) — confirma contra `git log` antes de escribirlo.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-10-02-inventario-frontend-design.md CLAUDE.md
git commit -m "docs(inventario): reflejar el frontend de inventario implementado

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Auto-revisión

**Cobertura del spec:** posiciones con filtros en URL (Tareas 5 y 8); filtros establecimiento/almacén vía `corporateStructureQuery` y SKU vía `skusQuery` (Tareas 3, 5 y 6); ajustes ingreso/salida con motivo (Tareas 4 y 7); detalle de lote con bloquear/desbloquear según estado y error de lote vencido mostrado desde el backend (Tareas 3 y 8); vencimiento próximo con constante única (Tarea 3); estados de carga/vacío/error (Tareas 6 y 8); permisos sin mecanismo nuevo en UI (no se añade nada; el backend responde 403 y `describeApiError` lo traduce); pruebas unitarias, rutas, e2e y verificación real (Tareas 9–11). Los cambios respecto al spec están listados en "Ajustes al spec detectados al planificar" y se registran en la Tarea 11.

**Consistencia de tipos:** `FetchPosicionesParams` acepta `FiltrosPosiciones` (strings vacíos que `withQuery` descarta); `AjusteFormValues` es el único contrato entre `AjusteForm`, `toRegistrarMovimientoPayload` y `ajusteSchema`; `OpcionAlmacen`/`OpcionEstablecimiento` se producen en `estructura.ts` y se consumen en `AjusteForm`, `AjusteDialog` e `InventoryPage`; `useRouteParam('loteId')` coincide con el path `inventario/lotes/:loteId`; nombres de botones y etiquetas coinciden entre componentes, tests y e2e.

**Riesgos conocidos a vigilar al ejecutar:** `crypto.randomUUID` en jsdom (Tarea 3), `MemoryRouter` en `react-router` 8 (Tarea 5), que RHF conserve los `defaultValues` no registrados al enviar el formulario desde una fila (Tarea 7), y unicidad de `getByText('Sí'/'No')` en el detalle (Tarea 8). Cada uno tiene su alternativa indicada en el paso donde aparece.
