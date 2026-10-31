# Compras, parte 3: órdenes de compra, listado, detalle y acciones (frontend) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar al módulo de compras el listado de órdenes (filtros de proveedor y estado en la URL, paginación), el detalle de una orden (resumen, totales, líneas con recibido y pendiente) y las acciones Aprobar, Emitir y Anular con motivo, integradas contra `/api/v1/compras/ordenes`.

**Architecture:** Mismo patrón de la parte 2 y de ventas: `api/` con `queryOptions`, `lib/` con lógica pura (estados, formato de importes y fechas), componentes delgados y páginas que los componen. Las acciones usan `useMutacionCompras`, que invalida `['compras']` y expone el error traducido con `describeErrorCompras`. El detalle resuelve el nombre del proveedor con `proveedorQuery` y el del establecimiento con `useEstablecimientos` de `organizacion`.

**Tech Stack:** React 19.2, React Router 8, TanStack Query, react-hook-form + zod, `@boticas/ui-web`, Vitest + Testing Library + MSW, Playwright.

Spec: `docs/superpowers/specs/2026-10-06-frontend-compras-design.md`. Requiere integrada la parte 2 (`invalidateCompras`, `describeErrorCompras`, `sampleProveedor`, `proveedorQuery`, `proveedoresQuery`, hub y rutas de compras). No depende de la parte 1.

## Global Constraints

- Todo archivo **nuevo** (código y tests) alcanza 100% de líneas, ramas, funciones y sentencias; no agregar archivos a `coverage-baseline.txt`.
- Sin comentarios en el código; sin duplicación; `forwardRef` y React Router 8 bloqueados por ESLint; las features solo se importan por su `index.ts`.
- Estados de orden: `BORRADOR`, `EN_APROBACION`, `APROBADA`, `EMITIDA`, `PARCIALMENTE_RECIBIDA`, `RECIBIDA`, `CANCELADA`, `CERRADA`. Acciones espejo del backend: aprobar si `BORRADOR` o `EN_APROBACION`; emitir si `APROBADA`; anular si `BORRADOR`, `EN_APROBACION`, `APROBADA` o `EMITIDA` (el backend además rechaza anular una orden con recepciones y responde `COM_ORDEN_ESTADO_INVALIDO`, que la pantalla muestra traducido); el motivo de anulación es obligatorio y admite hasta 300 caracteres.
- Los importes de una orden se muestran en la moneda de la orden (`moneda`, código ISO de 3 letras); las fechas `fechaEmision` y `fechaEntregaEstimada` llegan como `YYYY-MM-DD` (sin hora) y se muestran con `toLocaleDateString('es-PE')` sin desfase de zona horaria.
- El botón "Registrar recepción" no existe todavía: lo agrega la parte 5 junto con su ruta (no dejar enlaces a rutas inexistentes).
- Textos exactos: ver cada tarea (los usan los tests y el e2e).
- Comandos desde `frontend/` (`pnpm.cmd` si `pnpm.ps1` está bloqueado): `pnpm exec vitest run <ruta>`, `pnpm typecheck`, `pnpm lint`, `pnpm check`, `pnpm e2e compras.spec.ts`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan difiere por nombres reales del repo (labels, fixtures), ajustar al comportamiento real sin reducir lo que verifica y anotarlo en el reporte.

Rutas abreviadas: `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: Tipos, API, estados, formato y mutación de órdenes

**Files:**
- Create: `SRC/features/compras/api/ordenes.types.ts`, `SRC/features/compras/api/ordenes.api.ts`, `SRC/features/compras/api/ordenes.api.test.ts`
- Create: `SRC/features/compras/lib/estado-orden.ts` y `.test.ts`
- Create: `SRC/features/compras/lib/formato-compras.ts` y `.test.ts`
- Create: `SRC/features/compras/lib/use-mutacion-compras.ts` y `.test.tsx`
- Create: `SRC/features/compras/components/EstadoOrdenBadge.tsx` y `.test.tsx`
- Modify: `SRC/test/compras-fixtures.ts`

**Interfaces:**
- Consumes: `describeErrorCompras`, `invalidateCompras` (parte 2).
- Produces:

```ts
export const ESTADOS_ORDEN = ['BORRADOR', 'EN_APROBACION', 'APROBADA', 'EMITIDA',
  'PARCIALMENTE_RECIBIDA', 'RECIBIDA', 'CANCELADA', 'CERRADA'] as const;
export type EstadoOrden = (typeof ESTADOS_ORDEN)[number];
export type OrdenResumen = {
  id: string; numero: string; proveedorId: string; proveedorRazonSocial: string;
  establecimientoDestinoId: string; fechaEmision: string; fechaEntregaEstimada: string | null;
  moneda: string; total: number; estado: EstadoOrden;
};
export type LineaOrden = {
  numeroLinea: number; skuId: string; descripcion: string; cantidad: number;
  unidadMedidaCodigo: string; precioUnitario: number; descuento: number; impuesto: number;
  totalLinea: number; toleranciaExcesoPct: number | null; toleranciaDefectoPct: number | null;
  cantidadRecibida: number; cantidadPendiente: number;
};
export type Orden = {
  id: string; numero: string; proveedorId: string; establecimientoDestinoId: string;
  fechaEmision: string; fechaEntregaEstimada: string | null; moneda: string;
  tipoCambio: number | null; condicionPago: string | null; diasCredito: number;
  subtotal: number; descuentoTotal: number; impuestoTotal: number; total: number;
  estado: EstadoOrden; observacion: string | null; aprobadoAt: string | null; lineas: LineaOrden[];
};
export type AnularOrdenPayload = { motivo: string };
export type FetchOrdenesParams = {
  proveedorId?: string | undefined; estado?: string | undefined;
  page?: number | undefined; size?: number | undefined;
};
export function fetchOrdenes(client: ApiClient, params: FetchOrdenesParams): Promise<PaginaResponse<OrdenResumen>>;
export function ordenesQuery(params: FetchOrdenesParams);   // key ['compras','ordenes','lista', proveedorId, estado, page, size]
export function fetchOrden(client: ApiClient, ordenId: string): Promise<Orden>;
export function ordenQuery(ordenId: string);                // key ['compras','ordenes','detalle', ordenId]
export function aprobarOrden(client: ApiClient, ordenId: string): Promise<Orden>;
export function emitirOrden(client: ApiClient, ordenId: string): Promise<Orden>;
export function anularOrden(client: ApiClient, ordenId: string, payload: AnularOrdenPayload): Promise<Orden>;
export const etiquetaEstadoOrden: (estado: EstadoOrden) => string;
export const tonoEstadoOrden: (estado: EstadoOrden) => 'success' | 'warning' | 'danger' | 'neutral';
export const puedeAprobar: (estado: EstadoOrden) => boolean;
export const puedeEmitir: (estado: EstadoOrden) => boolean;
export const puedeAnular: (estado: EstadoOrden) => boolean;
export function formatoImporte(valor: number, moneda: string): string;
export function formatoFecha(fecha: string | null): string;   // null -> '—'
export function useMutacionCompras<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>, onSuccess?: () => void
): { mutate: (variables: TVariables) => void; isPending: boolean; mensajeError: string | null; reset: () => void };
export function EstadoOrdenBadge({ estado }: { estado: EstadoOrden }): JSX.Element;
export const sampleOrden: Orden; export const sampleOrdenResumen: OrdenResumen;   // test/compras-fixtures.ts
```

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `SRC/test/compras-fixtures.ts` (junto a `sampleProveedor`):

```ts
import type { Orden, OrdenResumen } from '../features/compras/api/ordenes.types';

export const sampleOrden: Orden = {
  id: 'orden-1',
  numero: 'OC-2026-000001',
  proveedorId: 'prov-1',
  establecimientoDestinoId: 'est-1',
  fechaEmision: '2026-10-03',
  fechaEntregaEstimada: null,
  moneda: 'PEN',
  tipoCambio: null,
  condicionPago: 'CREDITO 30',
  diasCredito: 30,
  subtotal: 55,
  descuentoTotal: 0,
  impuestoTotal: 9.9,
  total: 64.9,
  estado: 'BORRADOR',
  observacion: 'Reposición',
  aprobadoAt: null,
  lineas: [
    {
      numeroLinea: 1,
      skuId: 'sku-0001-aaaa',
      descripcion: 'Paracetamol 500 mg',
      cantidad: 10,
      unidadMedidaCodigo: 'UND',
      precioUnitario: 5.5,
      descuento: 0,
      impuesto: 9.9,
      totalLinea: 64.9,
      toleranciaExcesoPct: 0,
      toleranciaDefectoPct: 0,
      cantidadRecibida: 0,
      cantidadPendiente: 10
    }
  ]
};

export const sampleOrdenResumen: OrdenResumen = {
  id: 'orden-1',
  numero: 'OC-2026-000001',
  proveedorId: 'prov-1',
  proveedorRazonSocial: 'Laboratorios Perú SAC',
  establecimientoDestinoId: 'est-1',
  fechaEmision: '2026-10-03',
  fechaEntregaEstimada: null,
  moneda: 'PEN',
  total: 64.9,
  estado: 'EMITIDA'
};
```
(Fusionar el `import type` con el existente al inicio del archivo.)

`SRC/features/compras/api/ordenes.api.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleOrden, sampleOrdenResumen } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  anularOrden,
  aprobarOrden,
  emitirOrden,
  fetchOrden,
  fetchOrdenes,
  ordenQuery,
  ordenesQuery
} from './ordenes.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('ordenes.api', () => {
  it('fetchOrdenes envía proveedor, estado, página y tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/ordenes', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleOrdenResumen]));
      })
    );

    const result = await fetchOrdenes(client, {
      proveedorId: 'prov-1',
      estado: 'EMITIDA',
      page: 1,
      size: 50
    });

    expect(recibido?.searchParams.get('proveedorId')).toBe('prov-1');
    expect(recibido?.searchParams.get('estado')).toBe('EMITIDA');
    expect(recibido?.searchParams.get('page')).toBe('1');
    expect(recibido?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([sampleOrdenResumen]);
  });

  it('fetchOrdenes omite los filtros ausentes y usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/ordenes', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchOrdenes(client, {});

    expect(recibido?.search).toBe('?page=0&size=20');
  });

  it('fetchOrden obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/compras/ordenes/orden-1', () =>
        HttpResponse.json(sampleOrden)
      )
    );

    await expect(fetchOrden(client, 'orden-1')).resolves.toEqual(sampleOrden);
  });

  it.each([
    ['aprobarOrden', 'aprobacion', aprobarOrden, 'APROBADA'],
    ['emitirOrden', 'emision', emitirOrden, 'EMITIDA']
  ] as const)('%s hace POST a /%s', async (_nombre, ruta, accion, estado) => {
    let metodo = '';
    server.use(
      http.post(`http://localhost/api/v1/compras/ordenes/orden-1/${ruta}`, ({ request }) => {
        metodo = request.method;
        return HttpResponse.json({ ...sampleOrden, estado });
      })
    );

    const result = await accion(client, 'orden-1');

    expect(metodo).toBe('POST');
    expect(result.estado).toBe(estado);
  });

  it('anularOrden envía el motivo', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/compras/ordenes/orden-1/anulacion', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleOrden, estado: 'CANCELADA' });
      })
    );

    const result = await anularOrden(client, 'orden-1', { motivo: 'Error de digitación' });

    expect(result.estado).toBe('CANCELADA');
    expect(body).toEqual({ motivo: 'Error de digitación' });
  });

  it('ordenesQuery arma la clave con valores por defecto y con filtros', () => {
    expect(ordenesQuery({}).queryKey).toEqual(['compras', 'ordenes', 'lista', '', '', 0, 20]);
    expect(
      ordenesQuery({ proveedorId: 'prov-1', estado: 'EMITIDA', page: 2, size: 10 }).queryKey
    ).toEqual(['compras', 'ordenes', 'lista', 'prov-1', 'EMITIDA', 2, 10]);
  });

  it('ordenesQuery y ordenQuery consultan con el cliente de la aplicación', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce(pagina([sampleOrdenResumen]))
      .mockResolvedValueOnce(sampleOrden);
    const queryClient = new QueryClient();

    await queryClient.fetchQuery(ordenesQuery({ estado: 'EMITIDA' }));
    const detalle = ordenQuery('orden-1');
    await queryClient.fetchQuery(detalle);

    expect(get).toHaveBeenNthCalledWith(1, '/compras/ordenes?estado=EMITIDA&page=0&size=20');
    expect(get).toHaveBeenNthCalledWith(2, '/compras/ordenes/orden-1');
    expect(detalle.queryKey).toEqual(['compras', 'ordenes', 'detalle', 'orden-1']);
  });
});
```

`SRC/features/compras/lib/estado-orden.test.ts`:

```ts
import { ESTADOS_ORDEN } from '../api/ordenes.types';
import {
  etiquetaEstadoOrden,
  puedeAnular,
  puedeAprobar,
  puedeEmitir,
  tonoEstadoOrden
} from './estado-orden';

describe('estado-orden', () => {
  it('etiqueta y tono de cada estado', () => {
    expect(ESTADOS_ORDEN.map(etiquetaEstadoOrden)).toEqual([
      'Borrador',
      'En aprobación',
      'Aprobada',
      'Emitida',
      'Parcialmente recibida',
      'Recibida',
      'Cancelada',
      'Cerrada'
    ]);
    expect(ESTADOS_ORDEN.map(tonoEstadoOrden)).toEqual([
      'neutral',
      'warning',
      'success',
      'success',
      'warning',
      'success',
      'danger',
      'neutral'
    ]);
  });

  it('aprobar solo en borrador o en aprobación', () => {
    expect(ESTADOS_ORDEN.filter(puedeAprobar)).toEqual(['BORRADOR', 'EN_APROBACION']);
  });

  it('emitir solo cuando está aprobada', () => {
    expect(ESTADOS_ORDEN.filter(puedeEmitir)).toEqual(['APROBADA']);
  });

  it('anular hasta antes de recibir mercadería', () => {
    expect(ESTADOS_ORDEN.filter(puedeAnular)).toEqual([
      'BORRADOR',
      'EN_APROBACION',
      'APROBADA',
      'EMITIDA'
    ]);
  });
});
```

`SRC/features/compras/lib/formato-compras.test.ts`:

```ts
import { formatoMoneda } from '../../../shared/lib/format';
import { formatoFecha, formatoImporte } from './formato-compras';

describe('formato-compras', () => {
  it('formatea el importe en la moneda de la orden', () => {
    expect(formatoImporte(64.9, 'PEN')).toBe(formatoMoneda(64.9));
    expect(formatoImporte(10, 'USD')).toMatch(/10[.,]00/);
  });

  it('con una moneda inválida muestra el código y el importe', () => {
    expect(formatoImporte(10, 'ZZ')).toBe('ZZ 10.00');
  });

  it('formatea la fecha sin desfase de zona horaria', () => {
    expect(formatoFecha('2026-10-03')).toBe(new Date(2026, 9, 3).toLocaleDateString('es-PE'));
  });

  it('muestra un guion cuando no hay fecha', () => {
    expect(formatoFecha(null)).toBe('—');
  });
});
```

`SRC/features/compras/lib/use-mutacion-compras.test.tsx`:

```tsx
import { ApiError } from '@boticas/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { useMutacionCompras } from './use-mutacion-compras';

function renderMutacion(
  mutationFn: (valor: string) => Promise<string>,
  onSuccess?: () => void
) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { invalidate, ...renderHook(() => useMutacionCompras(mutationFn, onSuccess), { wrapper }) };
}

describe('useMutacionCompras', () => {
  it('parte sin error y sin mutación en curso', () => {
    const { result } = renderMutacion(() => Promise.resolve('ok'));

    expect(result.current.mensajeError).toBeNull();
    expect(result.current.isPending).toBe(false);
  });

  it('al tener éxito invalida compras y llama al callback', async () => {
    const onSuccess = vi.fn();
    const mutationFn = vi.fn(() => Promise.resolve('ok'));
    const { result, invalidate } = renderMutacion(mutationFn, onSuccess);

    act(() => result.current.mutate('x'));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(mutationFn).toHaveBeenCalledWith('x', expect.anything());
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('al tener éxito sin callback solo invalida', async () => {
    const { result, invalidate } = renderMutacion(() => Promise.resolve('ok'));

    act(() => result.current.mutate('x'));

    await waitFor(() => expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] }));
  });

  it('al fallar expone el mensaje traducido y reset lo limpia', async () => {
    const error = new ApiError('conflicto', 409, {
      code: 'COM_ORDEN_ESTADO_INVALIDO',
      detail: 'x',
      status: 409
    });
    const { result } = renderMutacion(() => Promise.reject(error));

    act(() => result.current.mutate('x'));

    await waitFor(() =>
      expect(result.current.mensajeError).toBe(
        'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
      )
    );

    act(() => result.current.reset());

    await waitFor(() => expect(result.current.mensajeError).toBeNull());
  });
});
```

`SRC/features/compras/components/EstadoOrdenBadge.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { EstadoOrdenBadge } from './EstadoOrdenBadge';

describe('EstadoOrdenBadge', () => {
  it('muestra la etiqueta del estado de la orden', () => {
    render(<EstadoOrdenBadge estado="PARCIALMENTE_RECIBIDA" />);

    expect(screen.getByText('Parcialmente recibida')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/api/ordenes.types.ts`:

```ts
export const ESTADOS_ORDEN = [
  'BORRADOR',
  'EN_APROBACION',
  'APROBADA',
  'EMITIDA',
  'PARCIALMENTE_RECIBIDA',
  'RECIBIDA',
  'CANCELADA',
  'CERRADA'
] as const;

export type EstadoOrden = (typeof ESTADOS_ORDEN)[number];

export type OrdenResumen = {
  id: string;
  numero: string;
  proveedorId: string;
  proveedorRazonSocial: string;
  establecimientoDestinoId: string;
  fechaEmision: string;
  fechaEntregaEstimada: string | null;
  moneda: string;
  total: number;
  estado: EstadoOrden;
};

export type LineaOrden = {
  numeroLinea: number;
  skuId: string;
  descripcion: string;
  cantidad: number;
  unidadMedidaCodigo: string;
  precioUnitario: number;
  descuento: number;
  impuesto: number;
  totalLinea: number;
  toleranciaExcesoPct: number | null;
  toleranciaDefectoPct: number | null;
  cantidadRecibida: number;
  cantidadPendiente: number;
};

export type Orden = {
  id: string;
  numero: string;
  proveedorId: string;
  establecimientoDestinoId: string;
  fechaEmision: string;
  fechaEntregaEstimada: string | null;
  moneda: string;
  tipoCambio: number | null;
  condicionPago: string | null;
  diasCredito: number;
  subtotal: number;
  descuentoTotal: number;
  impuestoTotal: number;
  total: number;
  estado: EstadoOrden;
  observacion: string | null;
  aprobadoAt: string | null;
  lineas: LineaOrden[];
};

export type AnularOrdenPayload = { motivo: string };
```

`SRC/features/compras/api/ordenes.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { AnularOrdenPayload, Orden, OrdenResumen } from './ordenes.types';

export type FetchOrdenesParams = {
  proveedorId?: string | undefined;
  estado?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchOrdenes(
  client: ApiClient,
  params: FetchOrdenesParams
): Promise<PaginaResponse<OrdenResumen>> {
  return client.get<PaginaResponse<OrdenResumen>>(
    withQuery('/compras/ordenes', {
      proveedorId: params.proveedorId,
      estado: params.estado,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function ordenesQuery(params: FetchOrdenesParams) {
  return queryOptions({
    queryKey: [
      'compras',
      'ordenes',
      'lista',
      params.proveedorId ?? '',
      params.estado ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchOrdenes(apiClient, params)
  });
}

export function fetchOrden(client: ApiClient, ordenId: string): Promise<Orden> {
  return client.get<Orden>(`/compras/ordenes/${ordenId}`);
}

export function ordenQuery(ordenId: string) {
  return queryOptions({
    queryKey: ['compras', 'ordenes', 'detalle', ordenId],
    queryFn: () => fetchOrden(apiClient, ordenId)
  });
}

export function aprobarOrden(client: ApiClient, ordenId: string): Promise<Orden> {
  return client.post<Orden, Record<string, never>>(`/compras/ordenes/${ordenId}/aprobacion`, {});
}

export function emitirOrden(client: ApiClient, ordenId: string): Promise<Orden> {
  return client.post<Orden, Record<string, never>>(`/compras/ordenes/${ordenId}/emision`, {});
}

export function anularOrden(
  client: ApiClient,
  ordenId: string,
  payload: AnularOrdenPayload
): Promise<Orden> {
  return client.post<Orden, AnularOrdenPayload>(`/compras/ordenes/${ordenId}/anulacion`, payload);
}
```

`SRC/features/compras/lib/estado-orden.ts`:

```ts
import type { EstadoOrden } from '../api/ordenes.types';

type Tono = 'success' | 'warning' | 'danger' | 'neutral';

const ETIQUETAS = {
  BORRADOR: 'Borrador',
  EN_APROBACION: 'En aprobación',
  APROBADA: 'Aprobada',
  EMITIDA: 'Emitida',
  PARCIALMENTE_RECIBIDA: 'Parcialmente recibida',
  RECIBIDA: 'Recibida',
  CANCELADA: 'Cancelada',
  CERRADA: 'Cerrada'
} as const satisfies Record<EstadoOrden, string>;

const TONOS = {
  BORRADOR: 'neutral',
  EN_APROBACION: 'warning',
  APROBADA: 'success',
  EMITIDA: 'success',
  PARCIALMENTE_RECIBIDA: 'warning',
  RECIBIDA: 'success',
  CANCELADA: 'danger',
  CERRADA: 'neutral'
} as const satisfies Record<EstadoOrden, Tono>;

const ANULABLES: readonly EstadoOrden[] = ['BORRADOR', 'EN_APROBACION', 'APROBADA', 'EMITIDA'];

export const etiquetaEstadoOrden = (estado: EstadoOrden): string => ETIQUETAS[estado];

export const tonoEstadoOrden = (estado: EstadoOrden): Tono => TONOS[estado];

export const puedeAprobar = (estado: EstadoOrden): boolean =>
  estado === 'BORRADOR' || estado === 'EN_APROBACION';

export const puedeEmitir = (estado: EstadoOrden): boolean => estado === 'APROBADA';

export const puedeAnular = (estado: EstadoOrden): boolean => ANULABLES.includes(estado);
```

`SRC/features/compras/lib/formato-compras.ts`:

```ts
export function formatoImporte(valor: number, moneda: string): string {
  try {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: moneda }).format(valor);
  } catch {
    return `${moneda} ${valor.toFixed(2)}`;
  }
}

export function formatoFecha(fecha: string | null): string {
  if (fecha === null) return '—';
  const [anio = '0', mes = '1', dia = '1'] = fecha.split('-');
  return new Date(Number(anio), Number(mes) - 1, Number(dia)).toLocaleDateString('es-PE');
}
```

`SRC/features/compras/lib/use-mutacion-compras.ts`:

```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { invalidateCompras } from '../api/invalidate';
import { describeErrorCompras } from './errores-compras';

export function useMutacionCompras<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn,
    onSuccess: () => {
      onSuccess?.();
      return invalidateCompras(queryClient);
    }
  });

  return {
    mutate: mutation.mutate,
    isPending: mutation.isPending,
    reset: mutation.reset,
    mensajeError: mutation.isError ? describeErrorCompras(mutation.error) : null
  };
}
```

`SRC/features/compras/components/EstadoOrdenBadge.tsx`:

```tsx
import { Badge } from '@boticas/ui-web';
import type { EstadoOrden } from '../api/ordenes.types';
import { etiquetaEstadoOrden, tonoEstadoOrden } from '../lib/estado-orden';

export function EstadoOrdenBadge({ estado }: { estado: EstadoOrden }) {
  return <Badge tone={tonoEstadoOrden(estado)}>{etiquetaEstadoOrden(estado)}</Badge>;
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras && pnpm typecheck && pnpm lint`
Expected: PASS con 100% en los archivos nuevos (`ordenes.types.ts` queda cubierto cuando lo importan las pantallas de la Task 2; `onSuccess?.()` queda cubierto por los dos tests del hook).

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): tipos, api, estados y formato de ordenes de compra

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Listado de órdenes

**Files:**
- Create: `SRC/features/compras/lib/use-ordenes-filtros.ts`
- Create: `SRC/features/compras/components/FiltrosOrdenes.tsx` y `.test.tsx`
- Create: `SRC/features/compras/components/OrdenesTable.tsx` y `.test.tsx`
- Create: `SRC/features/compras/pages/OrdenesPage.tsx` y `.test.tsx`

**Interfaces:**
- Consumes: `ordenesQuery`, `OrdenResumen`, `ESTADOS_ORDEN`, `EstadoOrdenBadge`, `etiquetaEstadoOrden`, `formatoImporte`, `formatoFecha`, `proveedoresQuery`, `Proveedor`.
- Produces:
  - `useOrdenesFiltros(): { filtros: { proveedorId: string; estado: string; page: number; size: number }, setProveedor(id), setEstado(estado), setPage(page: number), setSize(size: number) }`.
  - `FiltrosOrdenes({ proveedores, filtros, onProveedor, onEstado })`.
  - `OrdenesTable({ rows, isLoading, isError, pagination })`.
- Textos exactos: `OrdenesPage` título `Órdenes de compra`, contexto `Compras / Órdenes`, descripción `Seguimiento de las órdenes de compra y su recepción.`; filtros `Proveedor` (opción `Todos`) y `Estado` (opción `Todos` y las etiquetas de estado); tabla con columnas `Número`, `Proveedor`, `Emisión`, `Entrega estimada`, `Total`, `Estado`, `Acciones`; vacío `No hay órdenes de compra con los filtros indicados.`; error `No se pudo cargar el listado de órdenes.`; enlace por fila `Ver detalle de la orden {numero}` a `/compras/ordenes/{id}`.

- [ ] **Step 1: Escribir los tests que fallan**

`FiltrosOrdenes.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { FiltrosOrdenes } from './FiltrosOrdenes';

const filtros = { proveedorId: '', estado: '', page: 0, size: 20 };

function renderFiltros(overrides: Partial<Parameters<typeof FiltrosOrdenes>[0]> = {}) {
  const onProveedor = vi.fn();
  const onEstado = vi.fn();
  render(
    <FiltrosOrdenes
      proveedores={[sampleProveedor]}
      filtros={filtros}
      onProveedor={onProveedor}
      onEstado={onEstado}
      {...overrides}
    />
  );
  return { onProveedor, onEstado, user: userEvent.setup() };
}

describe('FiltrosOrdenes', () => {
  it('ofrece los proveedores y las etiquetas de estado', () => {
    renderFiltros();

    expect(screen.getByRole('option', { name: 'Laboratorios Perú SAC' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'En aprobación' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Parcialmente recibida' })).toBeInTheDocument();
  });

  it('notifica el proveedor y el estado elegidos', async () => {
    const { onProveedor, onEstado, user } = renderFiltros();

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'prov-1');
    await user.selectOptions(screen.getByLabelText('Estado'), 'EMITIDA');

    expect(onProveedor).toHaveBeenCalledWith('prov-1');
    expect(onEstado).toHaveBeenCalledWith('EMITIDA');
  });

  it('refleja los filtros recibidos', () => {
    renderFiltros({ filtros: { ...filtros, proveedorId: 'prov-1', estado: 'APROBADA' } });

    expect(screen.getByLabelText('Proveedor')).toHaveValue('prov-1');
    expect(screen.getByLabelText('Estado')).toHaveValue('APROBADA');
  });
});
```

`OrdenesTable.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { sampleOrdenResumen } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { formatoMoneda } from '../../../shared/lib/format';
import { OrdenesTable } from './OrdenesTable';

const pagination = {
  page: 0,
  size: 20,
  totalElements: 1,
  onPageChange: vi.fn(),
  onSizeChange: vi.fn()
};

function renderTable(overrides: Partial<Parameters<typeof OrdenesTable>[0]> = {}) {
  const Pantalla = () => (
    <OrdenesTable
      rows={[sampleOrdenResumen]}
      isLoading={false}
      isError={false}
      pagination={pagination}
      {...overrides}
    />
  );
  return renderRoute('/compras/ordenes', Pantalla, '/compras/ordenes');
}

describe('OrdenesTable', () => {
  it('muestra número, proveedor, fechas, total, estado y enlace al detalle', () => {
    renderTable();

    expect(screen.getByText('OC-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByText(new Date(2026, 9, 3).toLocaleDateString('es-PE'))).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText(formatoMoneda(64.9))).toBeInTheDocument();
    expect(screen.getByText('Emitida')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de la orden OC-2026-000001' })
    ).toHaveAttribute('href', '/compras/ordenes/orden-1');
  });

  it('muestra la fecha de entrega estimada cuando existe', () => {
    renderTable({ rows: [{ ...sampleOrdenResumen, fechaEntregaEstimada: '2026-10-20' }] });

    expect(screen.getByText(new Date(2026, 9, 20).toLocaleDateString('es-PE'))).toBeInTheDocument();
  });

  it('muestra el vacío, la carga y el error', () => {
    renderTable({ rows: [] });
    expect(
      screen.getByText('No hay órdenes de compra con los filtros indicados.')
    ).toBeInTheDocument();
  });

  it('muestra el estado de carga', () => {
    renderTable({ rows: [], isLoading: true });
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra el error de carga', () => {
    renderTable({ rows: [], isError: true });
    expect(screen.getByText('No se pudo cargar el listado de órdenes.')).toBeInTheDocument();
  });
});
```

`OrdenesPage.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleOrdenResumen, sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { OrdenesPage } from './OrdenesPage';

const ordenesUrl = '*/api/v1/compras/ordenes';
const proveedoresUrl = '*/api/v1/compras/proveedores';

beforeEach(() => {
  server.use(http.get(proveedoresUrl, () => HttpResponse.json(pagina([sampleProveedor]))));
});

function renderPage(entrada = '/compras/ordenes') {
  return renderRoute('/compras/ordenes', OrdenesPage, entrada);
}

function capturar() {
  const captura = { params: new URLSearchParams() };
  server.use(
    http.get(ordenesUrl, ({ request }) => {
      captura.params = new URL(request.url).searchParams;
      return HttpResponse.json(pagina([sampleOrdenResumen], { totalElements: 120 }));
    })
  );
  return captura;
}

describe('OrdenesPage', () => {
  it('lista las órdenes con enlace al detalle', async () => {
    server.use(http.get(ordenesUrl, () => HttpResponse.json(pagina([sampleOrdenResumen]))));

    renderPage();

    expect(await screen.findByText('OC-2026-000001')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Órdenes de compra' })).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de la orden OC-2026-000001' })
    ).toHaveAttribute('href', '/compras/ordenes/orden-1');
  });

  it('muestra el estado de carga, el vacío y el error', async () => {
    server.use(http.get(ordenesUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('No hay órdenes de compra con los filtros indicados.')
    ).toBeInTheDocument();
    primera.unmount();

    server.use(http.get(ordenesUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderPage();

    expect(await screen.findByText('No se pudo cargar el listado de órdenes.')).toBeInTheDocument();
  });

  it('filtra por proveedor y estado y refleja los filtros en la URL', async () => {
    const captura = capturar();
    const { user, router } = renderPage();
    await screen.findByText('OC-2026-000001');

    await user.selectOptions(await screen.findByLabelText('Proveedor'), 'prov-1');
    await waitFor(() => expect(captura.params.get('proveedorId')).toBe('prov-1'));
    await user.selectOptions(screen.getByLabelText('Estado'), 'EMITIDA');
    await waitFor(() => expect(captura.params.get('estado')).toBe('EMITIDA'));

    expect(router.state.location.search).toBe('?proveedorId=prov-1&estado=EMITIDA');
  });

  it('lee los filtros iniciales de la URL y no envía los vacíos', async () => {
    const captura = capturar();

    renderPage('/compras/ordenes?estado=APROBADA&page=2&size=50');

    await waitFor(() => expect(captura.params.get('estado')).toBe('APROBADA'));
    expect(captura.params.get('page')).toBe('2');
    expect(captura.params.get('size')).toBe('50');
    expect(captura.params.has('proveedorId')).toBe(false);
  });

  it('pagina y cambia el tamaño de página', async () => {
    const captura = capturar();
    const { user } = renderPage();
    await screen.findByText('OC-2026-000001');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(captura.params.get('page')).toBe('1'));
    await user.selectOptions(screen.getByLabelText('Filas por página'), '50');
    await waitFor(() => expect(captura.params.get('size')).toBe('50'));
    expect(captura.params.get('page')).toBe('0');
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/components apps/erp-web/src/features/compras/pages/OrdenesPage.test.tsx`
Expected: FAIL (componentes y página inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/lib/use-ordenes-filtros.ts`:

```ts
import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';

export type FiltrosOrdenesUrl = {
  proveedorId: string;
  estado: string;
  page: number;
  size: number;
};

const PAGE_POR_DEFECTO = 0;
const SIZE_POR_DEFECTO = 20;
const SIZE_MAXIMO = 100;

export function useOrdenesFiltros() {
  const { params, actualizar } = useParametrosUrl();

  const filtros: FiltrosOrdenesUrl = {
    proveedorId: params.get('proveedorId') ?? '',
    estado: params.get('estado') ?? '',
    page: enteroEnRango(params.get('page'), 0, Number.MAX_SAFE_INTEGER, PAGE_POR_DEFECTO),
    size: enteroEnRango(params.get('size'), 1, SIZE_MAXIMO, SIZE_POR_DEFECTO)
  };

  return {
    filtros,
    setProveedor: (proveedorId: string) => actualizar({ proveedorId, page: '' }),
    setEstado: (estado: string) => actualizar({ estado, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
```

`SRC/features/compras/components/FiltrosOrdenes.tsx`:

```tsx
import { Card } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import { ESTADOS_ORDEN } from '../api/ordenes.types';
import type { Proveedor } from '../api/proveedores.types';
import { etiquetaEstadoOrden } from '../lib/estado-orden';
import type { FiltrosOrdenesUrl } from '../lib/use-ordenes-filtros';

type FiltrosOrdenesProps = {
  proveedores: Proveedor[];
  filtros: FiltrosOrdenesUrl;
  onProveedor: (proveedorId: string) => void;
  onEstado: (estado: string) => void;
};

export function FiltrosOrdenes({
  proveedores,
  filtros,
  onProveedor,
  onEstado
}: FiltrosOrdenesProps) {
  return (
    <Card className="mt-6 grid gap-4 p-4 sm:grid-cols-2" aria-label="Filtros del listado">
      <SelectField
        id="filtro-proveedor"
        label="Proveedor"
        value={filtros.proveedorId}
        onChange={(event) => onProveedor(event.target.value)}
      >
        <option value="">Todos</option>
        {proveedores.map(({ id, razonSocial }) => (
          <option key={id} value={id}>
            {razonSocial}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="filtro-estado-orden"
        label="Estado"
        value={filtros.estado}
        onChange={(event) => onEstado(event.target.value)}
      >
        <option value="">Todos</option>
        {ESTADOS_ORDEN.map((estado) => (
          <option key={estado} value={estado}>
            {etiquetaEstadoOrden(estado)}
          </option>
        ))}
      </SelectField>
    </Card>
  );
}
```

`SRC/features/compras/components/OrdenesTable.tsx`:

```tsx
import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import { DataTable, iconButtonClassName, type PaginationProps } from '@boticas/ui-web';
import type { OrdenResumen } from '../api/ordenes.types';
import { formatoFecha, formatoImporte } from '../lib/formato-compras';
import { EstadoOrdenBadge } from './EstadoOrdenBadge';

type OrdenesTableProps = {
  rows: OrdenResumen[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
};

export function OrdenesTable({ rows, isLoading, isError, pagination }: OrdenesTableProps) {
  return (
    <DataTable<OrdenResumen>
      columns={[
        { header: 'Número', cell: (row) => row.numero },
        { header: 'Proveedor', cell: (row) => row.proveedorRazonSocial },
        { header: 'Emisión', cell: (row) => formatoFecha(row.fechaEmision) },
        { header: 'Entrega estimada', cell: (row) => formatoFecha(row.fechaEntregaEstimada) },
        { header: 'Total', cell: (row) => formatoImporte(row.total, row.moneda) },
        { header: 'Estado', cell: (row) => <EstadoOrdenBadge estado={row.estado} /> },
        {
          header: 'Acciones',
          cell: (row) => (
            <Link
              to={`/compras/ordenes/${row.id}`}
              aria-label={`Ver detalle de la orden ${row.numero}`}
              title={`Ver detalle de la orden ${row.numero}`}
              className={iconButtonClassName()}
            >
              <Eye className="size-4.5" aria-hidden="true" />
            </Link>
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay órdenes de compra con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el listado de órdenes."
      pagination={pagination}
    />
  );
}
```

`SRC/features/compras/pages/OrdenesPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@boticas/ui-web';
import { ordenesQuery } from '../api/ordenes.api';
import { proveedoresQuery } from '../api/proveedores.api';
import { FiltrosOrdenes } from '../components/FiltrosOrdenes';
import { OrdenesTable } from '../components/OrdenesTable';
import { useOrdenesFiltros } from '../lib/use-ordenes-filtros';

export function OrdenesPage() {
  const { filtros, setProveedor, setEstado, setPage, setSize } = useOrdenesFiltros();
  const { data: proveedores } = useQuery(proveedoresQuery({ size: 100 }));
  const { data, isPending, isError } = useQuery(
    ordenesQuery({
      proveedorId: filtros.proveedorId === '' ? undefined : filtros.proveedorId,
      estado: filtros.estado === '' ? undefined : filtros.estado,
      page: filtros.page,
      size: filtros.size
    })
  );

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Órdenes de compra"
        context="Compras / Órdenes"
        description="Seguimiento de las órdenes de compra y su recepción."
      />
      <FiltrosOrdenes
        proveedores={proveedores?.items ?? []}
        filtros={filtros}
        onProveedor={setProveedor}
        onEstado={setEstado}
      />
      <div className="mt-6">
        <OrdenesTable
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
        />
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras && pnpm typecheck && pnpm lint`
Expected: PASS con 100% en los archivos nuevos (`use-ordenes-filtros.ts` y `OrdenesPage.tsx` por los tests de página).

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): listado de ordenes con filtros en la URL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Detalle, acciones, rutas y e2e

**Files:**
- Create: `SRC/features/compras/schemas/anular-orden.schema.ts` y `.test.ts`
- Create: `SRC/features/compras/components/LineasOrdenTable.tsx` y `.test.tsx`
- Create: `SRC/features/compras/components/AnularOrdenDialog.tsx` y `.test.tsx`
- Create: `SRC/features/compras/pages/OrdenDetailPage.tsx` y `.test.tsx`
- Modify: `SRC/features/compras/pages/PurchasesPage.tsx`, `SRC/features/compras/pages/PurchasesPage.test.tsx`, `SRC/features/compras/routes.tsx`, `SRC/features/compras/routes.test.ts`, `SRC/app/feature-routes.test.ts`
- Modify: `E2E/support/compras-api.ts`, `E2E/compras.spec.ts`

**Interfaces:**
- Consumes: `ordenQuery`, `aprobarOrden`, `emitirOrden`, `anularOrden`, `Orden`, `LineaOrden`, `puedeAprobar`, `puedeEmitir`, `puedeAnular`, `EstadoOrdenBadge`, `useMutacionCompras`, `formatoImporte`, `formatoFecha`, `proveedorQuery`, `useEstablecimientos` (`../../organizacion`), `useRouteParam`, `DatoItem`, `FormError`.
- Produces: `anularOrdenSchema` / `AnularOrdenFormValues = { motivo: string }`; `LineasOrdenTable({ lineas, moneda })`; `AnularOrdenDialog({ ordenId, numero, onClose })`; rutas `compras/ordenes` y `compras/ordenes/:ordenId`.
- Textos exactos: detalle con título `Orden {numero}`, contexto `Compras / Órdenes`; botones de cabecera `Aprobar`, `Emitir` y `Anular` (según estado); secciones `Resumen`, `Totales` y `Líneas`; columnas de líneas `N°`, `Producto`, `Cantidad`, `Precio`, `Descuento`, `Impuesto`, `Total`, `Recibido`, `Pendiente`; diálogo `Anular orden de compra` con campo `Motivo` y botón `Confirmar anulación`; el motivo vacío muestra `El motivo es obligatorio.`; hub con tarjeta `Órdenes de compra` hacia `/compras/ordenes`.

- [ ] **Step 1: Escribir los tests que fallan**

`anular-orden.schema.test.ts`:

```ts
import { anularOrdenSchema } from './anular-orden.schema';

const mensajeDe = (motivo: string) => {
  const resultado = anularOrdenSchema.safeParse({ motivo });
  return resultado.success ? undefined : resultado.error.issues[0]?.message;
};

describe('anularOrdenSchema', () => {
  it('exige un motivo', () => {
    expect(mensajeDe('   ')).toBe('El motivo es obligatorio.');
  });

  it('limita el motivo a 300 caracteres', () => {
    expect(mensajeDe('a'.repeat(301))).toBe('El motivo no debe exceder 300 caracteres.');
    expect(mensajeDe('Error de digitación')).toBeUndefined();
  });
});
```

`LineasOrdenTable.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { sampleOrden } from '../../../test/compras-fixtures';
import { formatoMoneda } from '../../../shared/lib/format';
import { LineasOrdenTable } from './LineasOrdenTable';

describe('LineasOrdenTable', () => {
  it('muestra producto, cantidades, importes y el recibido y pendiente de cada línea', () => {
    render(<LineasOrdenTable lineas={sampleOrden.lineas} moneda="PEN" />);

    expect(screen.getByText('Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByText('10 UND')).toBeInTheDocument();
    expect(screen.getByText(formatoMoneda(5.5))).toBeInTheDocument();
    expect(screen.getByText(formatoMoneda(9.9))).toBeInTheDocument();
    expect(screen.getByText(formatoMoneda(64.9))).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '0' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '10' })).toBeInTheDocument();
  });

  it('muestra el vacío cuando no hay líneas', () => {
    render(<LineasOrdenTable lineas={[]} moneda="PEN" />);

    expect(screen.getByText('La orden no tiene líneas.')).toBeInTheDocument();
  });
});
```

`AnularOrdenDialog.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleOrden } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { AnularOrdenDialog } from './AnularOrdenDialog';

const anularUrl = '*/api/v1/compras/ordenes/orden-1/anulacion';

function renderDialog() {
  const onClose = vi.fn();
  const Pantalla = () => (
    <AnularOrdenDialog ordenId="orden-1" numero="OC-2026-000001" onClose={onClose} />
  );
  return { onClose, ...renderRoute('/x', Pantalla, '/x') };
}

describe('AnularOrdenDialog', () => {
  it('exige el motivo antes de anular', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Confirmar anulación' }));

    expect(await screen.findByText('El motivo es obligatorio.')).toBeInTheDocument();
  });

  it('anula la orden con el motivo, invalida compras y se cierra', async () => {
    let body: unknown;
    server.use(
      http.post(anularUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleOrden, estado: 'CANCELADA' });
      })
    );
    const { user, onClose, queryClient } = renderDialog();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    expect(screen.getByRole('heading', { name: 'Anular orden de compra' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('Motivo'), 'Error de digitación');
    await user.click(screen.getByRole('button', { name: 'Confirmar anulación' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(body).toEqual({ motivo: 'Error de digitación' });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error traducido y no se cierra', async () => {
    server.use(
      http.post(anularUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_ORDEN_ESTADO_INVALIDO', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user, onClose } = renderDialog();

    await user.type(screen.getByLabelText('Motivo'), 'Duplicada');
    await user.click(screen.getByRole('button', { name: 'Confirmar anulación' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it('cierra con la X', async () => {
    const { onClose } = renderDialog();

    await userEvent.setup().click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
```

`OrdenDetailPage.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { sampleOrden, sampleProveedor } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { formatoMoneda } from '../../../shared/lib/format';
import type { EstadoOrden } from '../api/ordenes.types';
import { OrdenDetailPage } from './OrdenDetailPage';

const ordenUrl = '*/api/v1/compras/ordenes/orden-1';

function mockOrden(estado: EstadoOrden, overrides: object = {}) {
  server.use(
    http.get(ordenUrl, () => HttpResponse.json({ ...sampleOrden, estado, ...overrides })),
    http.get('*/api/v1/compras/proveedores/prov-1', () => HttpResponse.json(sampleProveedor)),
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura))
  );
}

function renderPage() {
  return renderRoute('/compras/ordenes/:ordenId', OrdenDetailPage, '/compras/ordenes/orden-1');
}

const botones = () =>
  ['Aprobar', 'Emitir', 'Anular'].filter((nombre) =>
    screen.queryByRole('button', { name: nombre })
  );

describe('OrdenDetailPage', () => {
  it('muestra el resumen, los totales y las líneas con el proveedor y el destino', async () => {
    mockOrden('BORRADOR');

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Orden OC-2026-000001' })).toBeInTheDocument();
    expect(await screen.findByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByText('Botica Central')).toBeInTheDocument();
    expect(screen.getByText('Borrador')).toBeInTheDocument();
    expect(screen.getByText('CREDITO 30')).toBeInTheDocument();
    expect(screen.getByText('Reposición')).toBeInTheDocument();
    expect(screen.getAllByText(formatoMoneda(64.9)).length).toBeGreaterThan(0);
    expect(screen.getByText('Paracetamol 500 mg')).toBeInTheDocument();
  });

  it('muestra guiones cuando faltan datos opcionales y el id si el destino no se conoce', async () => {
    mockOrden('BORRADOR', {
      condicionPago: null,
      observacion: null,
      aprobadoAt: null,
      establecimientoDestinoId: 'est-desconocido'
    });

    renderPage();

    expect(await screen.findByText('est-desconocido')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  it('muestra la fecha de aprobación cuando la orden fue aprobada', async () => {
    mockOrden('APROBADA', { aprobadoAt: '2026-10-04T15:00:00Z' });

    renderPage();

    expect(await screen.findByText(new Date('2026-10-04T15:00:00Z').toLocaleString('es-PE'))).toBeInTheDocument();
  });

  it.each([
    ['BORRADOR', ['Aprobar', 'Anular']],
    ['EN_APROBACION', ['Aprobar', 'Anular']],
    ['APROBADA', ['Emitir', 'Anular']],
    ['EMITIDA', ['Anular']],
    ['PARCIALMENTE_RECIBIDA', []],
    ['CANCELADA', []]
  ] as const)('en estado %s ofrece %j', async (estado, esperados) => {
    mockOrden(estado);

    renderPage();
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    expect(botones()).toEqual(esperados);
  });

  it('muestra la carga y el error de consulta', async () => {
    server.use(http.get(ordenUrl, () => HttpResponse.json({ title: 'No encontrado' }, { status: 404 })));

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it.each([
    ['Aprobar', 'aprobacion', 'BORRADOR'],
    ['Emitir', 'emision', 'APROBADA']
  ] as const)('%s la orden y refresca las consultas', async (boton, ruta, estado) => {
    let llamadas = 0;
    mockOrden(estado);
    server.use(
      http.post(`*/api/v1/compras/ordenes/orden-1/${ruta}`, () => {
        llamadas += 1;
        return HttpResponse.json({ ...sampleOrden, estado: 'EMITIDA' });
      })
    );
    const { user, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: boton }));

    await waitFor(() => expect(llamadas).toBe(1));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error traducido cuando la acción falla', async () => {
    mockOrden('BORRADOR');
    server.use(
      http.post('*/api/v1/compras/ordenes/orden-1/aprobacion', () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_ORDEN_ESTADO_INVALIDO', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: 'Aprobar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
    );
  });

  it('abre el diálogo de anulación y lo cierra', async () => {
    mockOrden('EMITIDA');
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: 'Anular' }));
    expect(screen.getByRole('heading', { name: 'Anular orden de compra' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('heading', { name: 'Anular orden de compra' })).not.toBeInTheDocument();
  });
});
```

En `PurchasesPage.test.tsx` agregar al test existente (o como test nuevo):

```tsx
  it('muestra también el acceso a las órdenes de compra', () => {
    renderRoute('/compras', PurchasesPage, '/compras');

    expect(screen.getByRole('link', { name: /Órdenes de compra/ })).toHaveAttribute(
      'href',
      '/compras/ordenes'
    );
  });
```

En `routes.test.ts` cambiar la lista esperada por:

```ts
    expect(purchasesRoutes.map(({ path }) => path)).toEqual([
      'compras',
      'compras/proveedores',
      'compras/proveedores/nuevo',
      'compras/proveedores/:proveedorId',
      'compras/ordenes',
      'compras/ordenes/:ordenId'
    ]);
```

En `SRC/app/feature-routes.test.ts` agregar tras `'compras/proveedores/:proveedorId',`:

```ts
      'compras/ordenes',
      'compras/ordenes/:ordenId',
```

Ampliar `E2E/support/compras-api.ts`: agregar al inicio los imports `import { sampleOrden, sampleOrdenResumen } from '../../apps/erp-web/src/test/compras-fixtures';` y `import type { Orden } from '../../apps/erp-web/src/features/compras/api/ordenes.types';`, dentro de `mockComprasApi` (después del primer `let proveedores`) `let orden: Orden = sampleOrden;` y, antes del cierre de la función, estas rutas:

```ts
  await page.route('**/api/v1/estructura-corporativa', (route) =>
    json(route, 200, sampleEstructura)
  );
  await page.route(/\/api\/v1\/compras\/ordenes(\?|$)/, (route) =>
    json(
      route,
      200,
      pagina([{ ...sampleOrdenResumen, estado: orden.estado, total: orden.total }])
    )
  );
  await page.route(/\/api\/v1\/compras\/ordenes\/orden-1(\/(aprobacion|emision|anulacion))?(\?|$)/, (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname.endsWith('/aprobacion')) orden = { ...orden, estado: 'APROBADA' };
    if (pathname.endsWith('/emision')) orden = { ...orden, estado: 'EMITIDA' };
    if (pathname.endsWith('/anulacion')) orden = { ...orden, estado: 'CANCELADA' };
    return json(route, 200, orden);
  });
```
con `import { sampleEstructura } from '../../apps/erp-web/src/test/inventario-fixtures';`.

Agregar a `E2E/compras.spec.ts`, dentro del `test.describe`:

```ts
  test('aprueba, emite y anula una orden de compra', async ({ page }) => {
    await abrirComprasEn(page, '/compras');

    await page.getByRole('link', { name: /Órdenes de compra/ }).click();
    await expect(page.getByText('OC-2026-000001')).toBeVisible();
    await expectNoHorizontalOverflow(page);
    await page.getByRole('link', { name: 'Ver detalle de la orden OC-2026-000001' }).click();

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
    await page.getByRole('button', { name: 'Aprobar', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Emitir', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Emitir', exact: true }).click();
    await expect(page.getByText('Emitida').first()).toBeVisible();

    await page.getByRole('button', { name: 'Anular', exact: true }).click();
    await page.getByLabel('Motivo').fill('Orden duplicada');
    await page.getByRole('button', { name: 'Confirmar anulación' }).click();
    await expect(page.getByText('Cancelada').first()).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras apps/erp-web/src/app`
Expected: FAIL (esquema, componentes, página y rutas inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/schemas/anular-orden.schema.ts`:

```ts
import { z } from 'zod';

export const anularOrdenSchema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'El motivo es obligatorio.')
    .max(300, 'El motivo no debe exceder 300 caracteres.')
});

export type AnularOrdenFormValues = z.infer<typeof anularOrdenSchema>;
```

`SRC/features/compras/components/LineasOrdenTable.tsx`:

```tsx
import { DataTable } from '@boticas/ui-web';
import type { LineaOrden } from '../api/ordenes.types';
import { formatoImporte } from '../lib/formato-compras';

type LineasOrdenTableProps = { lineas: LineaOrden[]; moneda: string };

export function LineasOrdenTable({ lineas, moneda }: LineasOrdenTableProps) {
  return (
    <DataTable<LineaOrden>
      columns={[
        { header: 'N°', cell: (linea) => linea.numeroLinea },
        { header: 'Producto', cell: (linea) => linea.descripcion },
        { header: 'Cantidad', cell: (linea) => `${linea.cantidad} ${linea.unidadMedidaCodigo}` },
        { header: 'Precio', cell: (linea) => formatoImporte(linea.precioUnitario, moneda) },
        { header: 'Descuento', cell: (linea) => formatoImporte(linea.descuento, moneda) },
        { header: 'Impuesto', cell: (linea) => formatoImporte(linea.impuesto, moneda) },
        { header: 'Total', cell: (linea) => formatoImporte(linea.totalLinea, moneda) },
        { header: 'Recibido', cell: (linea) => linea.cantidadRecibida },
        { header: 'Pendiente', cell: (linea) => linea.cantidadPendiente }
      ]}
      rows={lineas}
      rowKey={(linea) => String(linea.numeroLinea)}
      emptyMessage="La orden no tiene líneas."
    />
  );
}
```

`SRC/features/compras/components/AnularOrdenDialog.tsx`:

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { anularOrden } from '../api/ordenes.api';
import { useMutacionCompras } from '../lib/use-mutacion-compras';
import { anularOrdenSchema, type AnularOrdenFormValues } from '../schemas/anular-orden.schema';

type AnularOrdenDialogProps = {
  ordenId: string;
  numero: string;
  onClose: () => void;
};

export function AnularOrdenDialog({ ordenId, numero, onClose }: AnularOrdenDialogProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<AnularOrdenFormValues>({
    defaultValues: { motivo: '' },
    mode: 'onTouched',
    resolver: zodResolver(anularOrdenSchema)
  });
  const anulacion = useMutacionCompras(
    (values: AnularOrdenFormValues) => anularOrden(apiClient, ordenId, { motivo: values.motivo }),
    onClose
  );

  return (
    <Modal open onClose={onClose} title="Anular orden de compra">
      <form
        className="space-y-4"
        noValidate
        onSubmit={(event) => {
          void handleSubmit((values) => anulacion.mutate(values))(event);
        }}
      >
        <p className="text-sm text-neutral-600 dark:text-neutral-300">
          Se anulará la orden {numero}. Esta acción no se puede deshacer.
        </p>
        <TextField
          id="anular-motivo"
          label="Motivo"
          error={errors.motivo?.message}
          {...register('motivo')}
        />
        {anulacion.mensajeError ? <FormError message={anulacion.mensajeError} /> : null}
        <Button type="submit" disabled={anulacion.isPending}>
          Confirmar anulación
        </Button>
      </form>
    </Modal>
  );
}
```

`SRC/features/compras/pages/OrdenDetailPage.tsx`:

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { valueOrDash } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { useEstablecimientos } from '../../organizacion';
import { aprobarOrden, emitirOrden, ordenQuery } from '../api/ordenes.api';
import { proveedorQuery } from '../api/proveedores.api';
import { AnularOrdenDialog } from '../components/AnularOrdenDialog';
import { EstadoOrdenBadge } from '../components/EstadoOrdenBadge';
import { LineasOrdenTable } from '../components/LineasOrdenTable';
import { describeErrorCompras } from '../lib/errores-compras';
import { puedeAnular, puedeAprobar, puedeEmitir } from '../lib/estado-orden';
import { formatoFecha, formatoImporte } from '../lib/formato-compras';
import { useMutacionCompras } from '../lib/use-mutacion-compras';

export function OrdenDetailPage() {
  const ordenId = useRouteParam('ordenId');
  const establecimientos = useEstablecimientos();
  const [anulando, setAnulando] = useState(false);
  const result = useQuery(ordenQuery(ordenId));
  const proveedor = useQuery({
    ...proveedorQuery(result.data?.proveedorId ?? ''),
    enabled: result.data !== undefined
  });
  const aprobacion = useMutacionCompras(() => aprobarOrden(apiClient, ordenId));
  const emision = useMutacionCompras(() => emitirOrden(apiClient, ordenId));

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeErrorCompras(result.error)} />;

  const orden = result.data;
  const importe = (valor: number) => formatoImporte(valor, orden.moneda);
  const destino =
    establecimientos.find(({ id }) => id === orden.establecimientoDestinoId)?.name ??
    orden.establecimientoDestinoId;
  const mensajeError = aprobacion.mensajeError ?? emision.mensajeError;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={`Orden ${orden.numero}`}
        context={<Link to="/compras/ordenes">Compras / Órdenes</Link>}
        description="Detalle de la orden, sus líneas y lo recibido."
        actions={
          <>
            <EstadoOrdenBadge estado={orden.estado} />
            {puedeAprobar(orden.estado) ? (
              <Button disabled={aprobacion.isPending} onClick={() => aprobacion.mutate(undefined)}>
                Aprobar
              </Button>
            ) : null}
            {puedeEmitir(orden.estado) ? (
              <Button disabled={emision.isPending} onClick={() => emision.mutate(undefined)}>
                Emitir
              </Button>
            ) : null}
            {puedeAnular(orden.estado) ? (
              <Button variant="secondary" onClick={() => setAnulando(true)}>
                Anular
              </Button>
            ) : null}
          </>
        }
      />

      {mensajeError ? (
        <div className="mt-6">
          <FormError message={mensajeError} />
        </div>
      ) : null}

      <Card className="mt-6 p-6">
        <h2 className="mb-4 text-base font-semibold">Resumen</h2>
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Proveedor">
            {proveedor.data?.razonSocial ?? orden.proveedorId}
          </DatoItem>
          <DatoItem label="Destino">{destino}</DatoItem>
          <DatoItem label="Emisión">{formatoFecha(orden.fechaEmision)}</DatoItem>
          <DatoItem label="Entrega estimada">{formatoFecha(orden.fechaEntregaEstimada)}</DatoItem>
          <DatoItem label="Condición de pago">{valueOrDash(orden.condicionPago)}</DatoItem>
          <DatoItem label="Días de crédito">{orden.diasCredito}</DatoItem>
          <DatoItem label="Moneda">{orden.moneda}</DatoItem>
          <DatoItem label="Aprobada el">
            {orden.aprobadoAt === null ? '—' : new Date(orden.aprobadoAt).toLocaleString('es-PE')}
          </DatoItem>
          <DatoItem label="Observación">{valueOrDash(orden.observacion)}</DatoItem>
        </dl>
      </Card>

      <section className="mt-6">
        <h2 className="mb-3 text-base font-semibold">Líneas</h2>
        <LineasOrdenTable lineas={orden.lineas} moneda={orden.moneda} />
      </section>

      <Card className="mt-6 p-6">
        <h2 className="mb-4 text-base font-semibold">Totales</h2>
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <DatoItem label="Subtotal">{importe(orden.subtotal)}</DatoItem>
          <DatoItem label="Descuento">{importe(orden.descuentoTotal)}</DatoItem>
          <DatoItem label="Impuesto">{importe(orden.impuestoTotal)}</DatoItem>
          <DatoItem label="Total">{importe(orden.total)}</DatoItem>
        </dl>
      </Card>

      {anulando ? (
        <AnularOrdenDialog
          ordenId={ordenId}
          numero={orden.numero}
          onClose={() => setAnulando(false)}
        />
      ) : null}
    </div>
  );
}
```
Nota: `aprobacion.mutate(undefined)` coincide con `useMutacionCompras<undefined, Orden>` (la función ignora el argumento); `proveedorQuery('')` con `enabled: false` mientras no hay orden nunca consulta.

Actualizar `SRC/features/compras/pages/PurchasesPage.tsx`: importar `ShoppingCart` de `lucide-react` junto a `Truck` y poner la tarjeta de órdenes antes de la de proveedores:

```tsx
const sections = [
  {
    to: '/compras/ordenes',
    icon: ShoppingCart,
    title: 'Órdenes de compra',
    description: 'Consulta, aprueba, emite y anula las órdenes de compra.'
  },
  {
    to: '/compras/proveedores',
    icon: Truck,
    title: 'Proveedores',
    description: 'Administra los laboratorios, importadores y distribuidores.'
  }
];
```
Si `PurchasesPage.test.tsx` hace `getByRole('link', { name: /Proveedores/ })`, sigue siendo único (la tarjeta de órdenes no contiene esa palabra; su descripción dice "órdenes de compra").

Actualizar `SRC/features/compras/routes.tsx`: agregar al arreglo, después de la ruta `compras/proveedores/:proveedorId`:

```tsx
  {
    path: 'compras/ordenes',
    lazy: async () => {
      const { OrdenesPage } = await import('./pages/OrdenesPage');
      return { Component: OrdenesPage };
    }
  },
  {
    path: 'compras/ordenes/:ordenId',
    lazy: async () => {
      const { OrdenDetailPage } = await import('./pages/OrdenDetailPage');
      return { Component: OrdenDetailPage };
    }
  }
```

- [ ] **Step 4: Ejecutar y verificar**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras apps/erp-web/src/app` → PASS. Run: `pnpm format && pnpm e2e compras.spec.ts` → PASS en desktop, tablet y móvil; `pnpm check` → verde (umbral 100% por archivo).
Ajustes permitidos sin reducir lo que verifican: el texto de la fecha de aprobación usa `toLocaleString('es-PE')` igual que el código; si el test de líneas encuentra más de una celda con `0` o `10`, usar `getAllByRole('cell', { name: '10' })`.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): detalle de orden con aprobar, emitir y anular

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Cobertura del spec (sección Órdenes de compra, listado, detalle y acciones):** listado con filtros de proveedor y estado en la URL, paginación, total por moneda y enlace al detalle (Task 2); detalle con estado, proveedor, destino, totales y líneas con recibido y pendiente (Task 3); acciones según estado — Aprobar, Emitir, Anular con motivo obligatorio — que invalidan las consultas y muestran el error del backend (Task 1 y 3); hub con tarjeta de órdenes y rutas lazy (Task 3); cobertura 100% en archivos nuevos y e2e en tres viewports (Task 3). El botón "Registrar recepción" y la creación de órdenes quedan para las partes 5 y 4, respectivamente.

**Escaneo de placeholders:** todo paso de código incluye el código. Los ajustes permitidos están acotados (celdas repetidas en el test de líneas). No hay "TBD".

**Consistencia de tipos:** `EstadoOrden`, `ESTADOS_ORDEN`, `Orden`, `OrdenResumen`, `LineaOrden`, `AnularOrdenPayload`, `FetchOrdenesParams`, `ordenesQuery`, `ordenQuery`, `aprobarOrden`, `emitirOrden`, `anularOrden`, `etiquetaEstadoOrden`, `tonoEstadoOrden`, `puedeAprobar`/`puedeEmitir`/`puedeAnular`, `formatoImporte`, `formatoFecha`, `useMutacionCompras`, `EstadoOrdenBadge`, `sampleOrden` y `sampleOrdenResumen` se usan con las mismas firmas en las tres tareas. Las partes 4 y 5 consumen `Orden`, `LineaOrden`, `ordenQuery`, `useMutacionCompras`, `formatoImporte`, `formatoFecha`, `EstadoOrdenBadge` y `sampleOrden` tal como quedan definidos aquí.
