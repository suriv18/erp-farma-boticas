# Frontend, parte 3: ventas (historial, detalle y comprobante) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el placeholder de `/ventas` por el historial paginado de ventas con filtros de fecha y local, el detalle `/ventas/:ventaId` (líneas, lotes consumidos, pago, anulación) y publicar la API de registro de venta y el comprobante interno imprimible que usará el POS.

**Architecture:** `features/ventas` sigue el patrón de `features/inventario` (`api/`, `lib/`, `components/`, `pages/`). Los filtros viven en la URL (`useSearchParams`) como en posiciones. La API de registro (`registrarVenta`) y el componente `ComprobanteVenta` se publican por `index.ts` para el plan 4 (POS).

**Tech Stack:** React 19.2, React Router 8, TanStack Query, `@boticas/ui-web` (`DataTable`, `Card`, `PageHeader`, `Badge`, `Button`), Vitest + Testing Library + MSW, Playwright.

Spec: `docs/superpowers/specs/2026-10-03-frontend-caja-pos-ventas-design.md`. Requiere el plan 2 integrado (`formatoMoneda`, `formatoFechaHora`, fixtures `ventas-fixtures.ts`, `TerminalSelector`).

## Global Constraints

- Todo archivo **nuevo** (código y tests) alcanza 100% de líneas, ramas, funciones y sentencias; no agregar archivos a `coverage-baseline.txt`; los reemplazados que figuren en ella se quitan (Task 4).
- Sin comentarios en el código; sin duplicación; `forwardRef` y React Router 8 bloqueados por ESLint; las features solo se importan por `index.ts`.
- Estados de carga, vacío y error en cada pantalla; textos en español exactos como se indican.
- Contratos del backend ya implementados:
  - `GET /api/v1/ventas/ventas?establecimientoId&desde&hasta&page&size` → `{ items: VentaResumen[], page, size, totalElements }`; `desde`/`hasta` son instantes ISO UTC (`2026-10-03T05:00:00.000Z`); `VentaResumen = { id, numeroOperacion, terminalId, fechaVenta, total, estado }`.
  - `GET /api/v1/ventas/ventas/{id}` → `Venta = { id, numeroOperacion, terminalId, turnoId, establecimientoId, vendedorId, fechaVenta, moneda, subtotal, descuentoTotal, impuestoTotal, total, estado, lineas: LineaVenta[], pago: Pago, anulacion: Anulacion | null }`; `LineaVenta = { numeroLinea, skuId, descripcion, unidadVentaCodigo, cantidad, precioUnitario, totalLinea, lotes: { loteId, cantidad }[] }`; `Pago = { medioPago, monto, montoRecibido, vuelto }`; `Anulacion = { anuladaAt, anuladaPorId, motivo }`.
  - `POST /api/v1/ventas/ventas` con cabecera `Idempotency-Key`, cuerpo `{ terminalId, almacenId, lineas: [{ skuId, cantidad, precioUnitario }], pago: { montoRecibido } }` → 201 `Venta`.
  - Estados de venta: `CONFIRMADA | ANULADA | PARCIALMENTE_DEVUELTA | DEVUELTA`; medio de pago `EFECTIVO`.
- Comprobante interno **no fiscal**: debe rotularse `Comprobante interno — no válido como comprobante de pago`.
- Comandos desde `frontend/` (`pnpm.cmd` si `pnpm.ps1` está bloqueado): `pnpm --filter @boticas/erp-web test -- <ruta>`, `pnpm check`, `pnpm e2e -- <archivo>`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan difiere por nombres reales del repo (labels, fixtures), ajustar al comportamiento real sin reducir lo que verifica y anotarlo en el reporte.

Rutas abreviadas: `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: Hook de establecimientos en organización

**Files:**
- Create: `SRC/features/organizacion/lib/use-establecimientos.ts`, `SRC/features/organizacion/lib/use-establecimientos.test.tsx`
- Modify: `SRC/features/organizacion/components/TerminalSelector.tsx`, `SRC/features/organizacion/index.ts`

**Interfaces:**
- Produces: `useEstablecimientos(): EstablishmentStructure[]` (aplana `companies[].establishments[]` de `corporateStructureQuery`; `[]` mientras carga); exportado por el índice junto con el tipo `EstablishmentStructure` (ya exportado).

- [ ] **Step 1: Escribir el test que falla**

```tsx
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { useEstablecimientos } from './use-establecimientos';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useEstablecimientos', () => {
  it('devuelve una lista vacía mientras carga y luego los establecimientos de todas las empresas', async () => {
    server.use(http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)));

    const { result } = renderHook(() => useEstablecimientos(), { wrapper });

    expect(result.current).toEqual([]);
    await waitFor(() => expect(result.current.map(({ id }) => id)).toEqual(['est-1', 'est-2']));
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- src/features/organizacion/lib/use-establecimientos`
Expected: FAIL (módulo inexistente).

- [ ] **Step 3: Implementar**

`use-establecimientos.ts`:

```ts
import { useQuery } from '@tanstack/react-query';
import { corporateStructureQuery, type EstablishmentStructure } from '../api/organization.api';

export function useEstablecimientos(): EstablishmentStructure[] {
  const { data } = useQuery(corporateStructureQuery);
  return (data?.companies ?? []).flatMap(({ establishments }) => establishments);
}
```
En `TerminalSelector.tsx` eliminar `useQuery(corporateStructureQuery)` y el `flatMap`, usando `const establecimientos = useEstablecimientos();` (importar de `../lib/use-establecimientos` y quitar imports no usados). En `index.ts` agregar `export { useEstablecimientos } from './lib/use-establecimientos';`.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/organizacion && pnpm typecheck`
Expected: PASS (los tests de `TerminalSelector` siguen verdes).

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "refactor(organizacion): hook de establecimientos compartido

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: API de ventas, tipos, fixtures, límites de fecha y filtros en la URL

**Files:**
- Create: `SRC/features/ventas/api/ventas.types.ts`, `SRC/features/ventas/api/ventas.api.ts`, `SRC/features/ventas/api/ventas.api.test.ts`, `SRC/features/ventas/api/invalidate.ts`, `SRC/features/ventas/api/invalidate.test.ts`
- Create: `SRC/features/ventas/lib/fechas.ts`, `SRC/features/ventas/lib/fechas.test.ts`, `SRC/features/ventas/lib/use-ventas-filtros.ts`, `SRC/features/ventas/lib/use-ventas-filtros.test.tsx`
- Modify: `SRC/test/ventas-fixtures.ts`

**Interfaces:**
- Produces: tipos `EstadoVenta`, `Venta`, `VentaResumen`, `LineaVenta`, `LoteConsumido`, `Pago`, `Anulacion`, `RegistrarVentaPayload`; `fetchVentas(client, params)`, `ventasQuery(params)` con clave `['ventas', 'lista', establecimientoId, desde, hasta, page, size]`; `fetchVenta(client, ventaId)`, `ventaQuery(ventaId)` con clave `['ventas', 'detalle', ventaId]`; `registrarVenta(client, payload, idempotencyKey)`; `invalidateVentas(queryClient)` (clave `['ventas']`); `inicioDelDia(fecha: string): string` y `finDelDia(fecha: string): string` (instantes ISO UTC del día local); `useVentasFiltros()` con `filtros: { establecimientoId, desde, hasta, page, size }` (`desde`/`hasta` como `YYYY-MM-DD`) y `setEstablecimiento`, `setDesde`, `setHasta`, `setPage`, `setSize`; fixtures `sampleVenta`, `sampleVentaResumen`, `sampleVentaAnulada`.

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `SRC/test/ventas-fixtures.ts` (conservar lo existente; agregar el import de tipos):

```ts
import type { Venta, VentaResumen } from '../features/ventas/api/ventas.types';

export const sampleVenta: Venta = {
  id: 'venta-1',
  numeroOperacion: 'EST001-T01-000001',
  terminalId: 'term-1',
  turnoId: 'turno-1',
  establecimientoId: 'est-1',
  vendedorId: 'user-1',
  fechaVenta: '2026-10-03T15:30:00Z',
  moneda: 'PEN',
  subtotal: 21.19,
  descuentoTotal: 0,
  impuestoTotal: 3.81,
  total: 25,
  estado: 'CONFIRMADA',
  lineas: [
    {
      numeroLinea: 1,
      skuId: 'sku-0001-aaaa',
      descripcion: 'Paracetamol 500 mg',
      unidadVentaCodigo: 'UND',
      cantidad: 2,
      precioUnitario: 12.5,
      totalLinea: 25,
      lotes: [{ loteId: 'lote-1', cantidad: 2 }]
    }
  ],
  pago: { medioPago: 'EFECTIVO', monto: 25, montoRecibido: 30, vuelto: 5 },
  anulacion: null
};

export const sampleVentaAnulada: Venta = {
  ...sampleVenta,
  estado: 'ANULADA',
  anulacion: { anuladaAt: '2026-10-03T16:00:00Z', anuladaPorId: 'user-2', motivo: 'Error de digitación' }
};

export const sampleVentaResumen: VentaResumen = {
  id: 'venta-1',
  numeroOperacion: 'EST001-T01-000001',
  terminalId: 'term-1',
  fechaVenta: '2026-10-03T15:30:00Z',
  total: 25,
  estado: 'CONFIRMADA'
};
```

`ventas.api.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { pagina } from '../../../test/organizacion-fixtures';
import { sampleVenta, sampleVentaResumen } from '../../../test/ventas-fixtures';
import { fetchVenta, fetchVentas, registrarVenta, ventaQuery, ventasQuery } from './ventas.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('ventas.api', () => {
  it('fetchVentas envía filtros, página y tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/ventas/ventas', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleVentaResumen]));
      })
    );

    const result = await fetchVentas(client, {
      establecimientoId: 'est-1',
      desde: '2026-10-03T05:00:00.000Z',
      hasta: '2026-10-04T04:59:59.999Z',
      page: 1,
      size: 50
    });

    expect(recibido?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(recibido?.searchParams.get('desde')).toBe('2026-10-03T05:00:00.000Z');
    expect(recibido?.searchParams.get('hasta')).toBe('2026-10-04T04:59:59.999Z');
    expect(recibido?.searchParams.get('page')).toBe('1');
    expect(recibido?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([sampleVentaResumen]);
  });

  it('fetchVentas omite los filtros ausentes y usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/ventas/ventas', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchVentas(client, {});

    expect(recibido?.search).toBe('?page=0&size=20');
  });

  it('fetchVenta obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/ventas/ventas/venta-1', () => HttpResponse.json(sampleVenta))
    );

    await expect(fetchVenta(client, 'venta-1')).resolves.toEqual(sampleVenta);
  });

  it('registrarVenta envía el cuerpo con la Idempotency-Key', async () => {
    let body: unknown;
    let clave: string | null = null;
    server.use(
      http.post('http://localhost/api/v1/ventas/ventas', async ({ request }) => {
        body = await request.json();
        clave = request.headers.get('Idempotency-Key');
        return HttpResponse.json(sampleVenta, { status: 201 });
      })
    );
    const payload = {
      terminalId: 'term-1',
      almacenId: 'alm-1',
      lineas: [{ skuId: 'sku-0001-aaaa', cantidad: 2, precioUnitario: 12.5 }],
      pago: { montoRecibido: 30 }
    };

    await expect(registrarVenta(client, payload, 'clave-1')).resolves.toEqual(sampleVenta);
    expect(body).toEqual(payload);
    expect(clave).toBe('clave-1');
  });

  it('ventasQuery arma la clave con valores por defecto y con filtros', () => {
    expect(ventasQuery({}).queryKey).toEqual(['ventas', 'lista', '', '', '', 0, 20]);
    expect(
      ventasQuery({ establecimientoId: 'est-1', desde: 'd', hasta: 'h', page: 2, size: 10 }).queryKey
    ).toEqual(['ventas', 'lista', 'est-1', 'd', 'h', 2, 10]);
  });

  it('ventasQuery y ventaQuery consultan con el cliente de la aplicación', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce(pagina([sampleVentaResumen]))
      .mockResolvedValueOnce(sampleVenta);
    const queryClient = new QueryClient();

    await queryClient.fetchQuery(ventasQuery({ establecimientoId: 'est-1' }));
    const detalle = ventaQuery('venta-1');
    await queryClient.fetchQuery(detalle);

    expect(get).toHaveBeenNthCalledWith(1, '/ventas/ventas?establecimientoId=est-1&page=0&size=20');
    expect(get).toHaveBeenNthCalledWith(2, '/ventas/ventas/venta-1');
    expect(detalle.queryKey).toEqual(['ventas', 'detalle', 'venta-1']);
  });
});
```

`invalidate.test.ts` análogo al de caja con `invalidateVentas` y `{ queryKey: ['ventas'] }`.

`fechas.test.ts`:

```ts
import { finDelDia, inicioDelDia } from './fechas';

describe('límites del día', () => {
  it('inicioDelDia es la medianoche local del día expresada como instante UTC', () => {
    expect(inicioDelDia('2026-10-03')).toBe(new Date(2026, 9, 3, 0, 0, 0, 0).toISOString());
  });

  it('finDelDia es el último milisegundo local del día expresado como instante UTC', () => {
    expect(finDelDia('2026-10-03')).toBe(new Date(2026, 9, 3, 23, 59, 59, 999).toISOString());
  });
});
```

`use-ventas-filtros.test.tsx` (patrón de `use-posiciones-filtros.test.tsx`, que debe leerse y copiarse en estilo): casos obligatorios — (a) sin parámetros: `establecimientoId ''`, `desde ''`, `hasta ''`, `page 0`, `size 20`; (b) lee de la URL `?establecimientoId=est-1&desde=2026-10-01&hasta=2026-10-03&page=2&size=50`; (c) `size` fuera de rango (`0`, `101`, `abc`) vuelve a 20 y `page` negativa o no entera vuelve a 0; (d) `setEstablecimiento('est-1')`, `setDesde('2026-10-01')`, `setHasta('2026-10-03')` escriben el parámetro y reinician `page`; `setDesde('')` lo elimina; `setPage(3)` y `setSize(50)` (que reinicia `page`); (e) dos actualizaciones consecutivas en el mismo tick conservan ambos cambios.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/ventas`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 3: Implementar**

`ventas.types.ts`:

```ts
export const ESTADOS_VENTA = ['CONFIRMADA', 'ANULADA', 'PARCIALMENTE_DEVUELTA', 'DEVUELTA'] as const;

export type EstadoVenta = (typeof ESTADOS_VENTA)[number];

export type LoteConsumido = {
  loteId: string;
  cantidad: number;
};

export type LineaVenta = {
  numeroLinea: number;
  skuId: string;
  descripcion: string;
  unidadVentaCodigo: string;
  cantidad: number;
  precioUnitario: number;
  totalLinea: number;
  lotes: LoteConsumido[];
};

export type Pago = {
  medioPago: string;
  monto: number;
  montoRecibido: number;
  vuelto: number;
};

export type Anulacion = {
  anuladaAt: string;
  anuladaPorId: string;
  motivo: string;
};

export type Venta = {
  id: string;
  numeroOperacion: string;
  terminalId: string;
  turnoId: string;
  establecimientoId: string;
  vendedorId: string;
  fechaVenta: string;
  moneda: string;
  subtotal: number;
  descuentoTotal: number;
  impuestoTotal: number;
  total: number;
  estado: EstadoVenta;
  lineas: LineaVenta[];
  pago: Pago;
  anulacion: Anulacion | null;
};

export type VentaResumen = {
  id: string;
  numeroOperacion: string;
  terminalId: string;
  fechaVenta: string;
  total: number;
  estado: EstadoVenta;
};

export type RegistrarVentaPayload = {
  terminalId: string;
  almacenId: string;
  lineas: { skuId: string; cantidad: number; precioUnitario: number }[];
  pago: { montoRecibido: number };
};
```

`ventas.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { RegistrarVentaPayload, Venta, VentaResumen } from './ventas.types';

export type FetchVentasParams = {
  establecimientoId?: string | undefined;
  desde?: string | undefined;
  hasta?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchVentas(
  client: ApiClient,
  params: FetchVentasParams
): Promise<PaginaResponse<VentaResumen>> {
  return client.get<PaginaResponse<VentaResumen>>(
    withQuery('/ventas/ventas', {
      establecimientoId: params.establecimientoId,
      desde: params.desde,
      hasta: params.hasta,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function ventasQuery(params: FetchVentasParams) {
  return queryOptions({
    queryKey: [
      'ventas',
      'lista',
      params.establecimientoId ?? '',
      params.desde ?? '',
      params.hasta ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchVentas(apiClient, params)
  });
}

export function fetchVenta(client: ApiClient, ventaId: string): Promise<Venta> {
  return client.get<Venta>(`/ventas/ventas/${ventaId}`);
}

export function ventaQuery(ventaId: string) {
  return queryOptions({
    queryKey: ['ventas', 'detalle', ventaId],
    queryFn: () => fetchVenta(apiClient, ventaId)
  });
}

export function registrarVenta(
  client: ApiClient,
  payload: RegistrarVentaPayload,
  idempotencyKey: string
): Promise<Venta> {
  return client.post<Venta, RegistrarVentaPayload>('/ventas/ventas', payload, {
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}
```

`invalidate.ts`:

```ts
import type { QueryClient } from '@tanstack/react-query';

export function invalidateVentas(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['ventas'] });
}
```

`fechas.ts`:

```ts
const instante = (fecha: string, hora: [number, number, number, number]): string => {
  const [anio = 0, mes = 1, dia = 1] = fecha.split('-').map(Number);
  return new Date(anio, mes - 1, dia, ...hora).toISOString();
};

export const inicioDelDia = (fecha: string): string => instante(fecha, [0, 0, 0, 0]);

export const finDelDia = (fecha: string): string => instante(fecha, [23, 59, 59, 999]);
```

`use-ventas-filtros.ts` replicando `use-posiciones-filtros.ts` (mismas funciones `enteroEnRango`, `useRef` de parámetros vigentes y `actualizar`) con estos campos y setters, de modo que la lógica de URL común **no se duplique**: extraer primero de `use-posiciones-filtros.ts` a `SRC/shared/lib/use-filtros-url.ts` (+ test) las piezas reutilizables — `enteroEnRango` y un hook `useParametrosUrl()` que devuelve `{ params, actualizar }` — y refactorizar `use-posiciones-filtros.ts` para usarlas (sus tests existentes deben seguir verdes sin cambios), y que `use-ventas-filtros.ts` las consuma:

```ts
import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';

export type FiltrosVentas = {
  establecimientoId: string;
  desde: string;
  hasta: string;
  page: number;
  size: number;
};

const PAGE_POR_DEFECTO = 0;
const SIZE_POR_DEFECTO = 20;
const SIZE_MAXIMO = 100;

export function useVentasFiltros() {
  const { params, actualizar } = useParametrosUrl();

  const filtros: FiltrosVentas = {
    establecimientoId: params.get('establecimientoId') ?? '',
    desde: params.get('desde') ?? '',
    hasta: params.get('hasta') ?? '',
    page: enteroEnRango(params.get('page'), 0, Number.MAX_SAFE_INTEGER, PAGE_POR_DEFECTO),
    size: enteroEnRango(params.get('size'), 1, SIZE_MAXIMO, SIZE_POR_DEFECTO)
  };

  return {
    filtros,
    setEstablecimiento: (id: string) => actualizar({ establecimientoId: id, page: '' }),
    setDesde: (fecha: string) => actualizar({ desde: fecha, page: '' }),
    setHasta: (fecha: string) => actualizar({ hasta: fecha, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
```
`use-filtros-url.ts` contiene exactamente la lógica hoy en `use-posiciones-filtros.ts` (`enteroEnRango`, `useSearchParams`, `useRef` de vigentes con su `useEffect`, y `actualizar(cambios: Record<string, string>)`), exportada tal cual; su test cubre `enteroEnRango` (vacío, no entero, fuera de rango, válido) y `actualizar` (setea, elimina con `''`, acumula en el mismo tick).

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/ventas src/features/inventario src/shared && pnpm typecheck`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(ventas): api, tipos, filtros en la URL y limites de fecha

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Componentes de ventas (tabla, filtros, líneas y comprobante imprimible)

**Files:**
- Create: `SRC/features/ventas/lib/estado-venta.ts` y `.test.ts`
- Create: `SRC/features/ventas/components/EstadoVentaBadge.tsx` y `.test.tsx`, `VentasTable.tsx` y `.test.tsx`, `FiltrosVentas.tsx` y `.test.tsx`, `LineasVentaTable.tsx` y `.test.tsx`, `ComprobanteVenta.tsx` y `.test.tsx`
- Modify: `frontend/packages/ui-web/src/styles.css` (reglas de impresión)

**Interfaces:**
- Consumes: Task 2 (tipos, fixtures), `formatoMoneda`, `formatoFechaHora`.
- Produces: `tonoEstadoVenta(estado: EstadoVenta): 'success' | 'danger' | 'warning' | 'neutral'`; `EstadoVentaBadge({ estado })`; `VentasTable({ rows, isLoading, isError, pagination })`; `FiltrosVentas({ establecimientos, filtros, onEstablecimiento, onDesde, onHasta })`; `LineasVentaTable({ lineas })`; `ComprobanteVenta({ venta, nombreEstablecimiento })`.
- Textos exactos: columnas `N° operación`, `Fecha`, `Total`, `Estado`, `Acciones`; enlace `Ver detalle de la venta {numeroOperacion}` hacia `/ventas/{id}`; vacío `No hay ventas con los filtros indicados.`; error `No se pudo cargar el historial de ventas.`; filtros con etiquetas `Establecimiento` (opción `Todos`), `Desde`, `Hasta`; botón del comprobante `Imprimir comprobante`; rótulo `Comprobante interno — no válido como comprobante de pago`.

- [ ] **Step 1: Escribir los tests que fallan**

`estado-venta.test.ts`: `tonoEstadoVenta('CONFIRMADA')` → `'success'`, `'ANULADA'` → `'danger'`, `'PARCIALMENTE_DEVUELTA'` → `'warning'`, `'DEVUELTA'` → `'neutral'`.

Tests de componentes (casos obligatorios; estilo de `PosicionesTable.test.tsx`, `renderRoute` para los que usan `Link`):
- `EstadoVentaBadge.test.tsx`: muestra el texto del estado.
- `VentasTable.test.tsx`: con `sampleVentaResumen` muestra el número de operación, la fecha formateada, `S/ 25.00`, el badge y el enlace `Ver detalle de la venta EST001-T01-000001` con `href="/ventas/venta-1"`; sin filas muestra el vacío; con `isError` muestra el error; con `isLoading` muestra `Cargando…`; la paginación llama a `onPageChange`/`onSizeChange` (como en `PosicionesTable.test.tsx`).
- `FiltrosVentas.test.tsx`: lista los establecimientos más `Todos`; al elegir uno llama `onEstablecimiento('est-1')`; al escribir en `Desde` y `Hasta` (campos `type="date"`) llama `onDesde('2026-10-01')` y `onHasta('2026-10-03')`; refleja los valores recibidos en `filtros`.
- `LineasVentaTable.test.tsx`: con `sampleVenta.lineas` muestra descripción, `2 UND`, precio `S/ 12.50`, total `S/ 25.00` y los lotes consumidos (`Lote lote-1 × 2`); una línea con varios lotes los lista todos; sin lotes muestra `—`.
- `ComprobanteVenta.test.tsx`: muestra el rótulo no fiscal, el nombre del establecimiento, el número de operación, la fecha, cada línea (descripción, `2 × S/ 12.50`, `S/ 25.00`), `Subtotal`, `IGV`, `Total`, `Recibido`, `Vuelto`; el botón `Imprimir comprobante` llama `window.print` (espiar con `vi.spyOn(window, 'print').mockImplementation(() => undefined)`); con `venta.anulacion` no nulo muestra `ANULADA` y el motivo.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/ventas/components src/features/ventas/lib/estado-venta`
Expected: FAIL.

- [ ] **Step 3: Implementar**

`estado-venta.ts`:

```ts
import type { EstadoVenta } from '../api/ventas.types';

const TONOS = {
  CONFIRMADA: 'success',
  ANULADA: 'danger',
  PARCIALMENTE_DEVUELTA: 'warning',
  DEVUELTA: 'neutral'
} as const satisfies Record<EstadoVenta, 'success' | 'danger' | 'warning' | 'neutral'>;

export const tonoEstadoVenta = (estado: EstadoVenta) => TONOS[estado];
```

`EstadoVentaBadge.tsx`:

```tsx
import { Badge } from '@boticas/ui-web';
import type { EstadoVenta } from '../api/ventas.types';
import { tonoEstadoVenta } from '../lib/estado-venta';

export function EstadoVentaBadge({ estado }: { estado: EstadoVenta }) {
  return <Badge tone={tonoEstadoVenta(estado)}>{estado}</Badge>;
}
```

`VentasTable.tsx`:

```tsx
import { Link } from 'react-router';
import { DataTable, type PaginationProps } from '@boticas/ui-web';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import type { VentaResumen } from '../api/ventas.types';
import { EstadoVentaBadge } from './EstadoVentaBadge';

type VentasTableProps = {
  rows: VentaResumen[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
};

export function VentasTable({ rows, isLoading, isError, pagination }: VentasTableProps) {
  return (
    <DataTable<VentaResumen>
      columns={[
        { header: 'N° operación', cell: (row) => row.numeroOperacion },
        { header: 'Fecha', cell: (row) => formatoFechaHora(row.fechaVenta) },
        { header: 'Total', cell: (row) => formatoMoneda(row.total) },
        { header: 'Estado', cell: (row) => <EstadoVentaBadge estado={row.estado} /> },
        {
          header: 'Acciones',
          cell: (row) => (
            <Link
              className="text-primary-600 hover:underline"
              to={`/ventas/${row.id}`}
              aria-label={`Ver detalle de la venta ${row.numeroOperacion}`}
            >
              Ver detalle
            </Link>
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay ventas con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el historial de ventas."
      pagination={pagination}
    />
  );
}
```

`FiltrosVentas.tsx`:

```tsx
import { Card } from '@boticas/ui-web';
import type { EstablishmentStructure } from '../../organizacion';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import type { FiltrosVentas as Filtros } from '../lib/use-ventas-filtros';

type FiltrosVentasProps = {
  establecimientos: EstablishmentStructure[];
  filtros: Filtros;
  onEstablecimiento: (id: string) => void;
  onDesde: (fecha: string) => void;
  onHasta: (fecha: string) => void;
};

export function FiltrosVentas({
  establecimientos,
  filtros,
  onEstablecimiento,
  onDesde,
  onHasta
}: FiltrosVentasProps) {
  return (
    <Card className="mt-6 grid gap-4 p-4 sm:grid-cols-3" aria-label="Filtros del listado">
      <SelectField
        id="filtro-establecimiento"
        label="Establecimiento"
        value={filtros.establecimientoId}
        onChange={(event) => onEstablecimiento(event.target.value)}
      >
        <option value="">Todos</option>
        {establecimientos.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <TextField
        id="filtro-desde"
        label="Desde"
        type="date"
        value={filtros.desde}
        onChange={(event) => onDesde(event.target.value)}
      />
      <TextField
        id="filtro-hasta"
        label="Hasta"
        type="date"
        value={filtros.hasta}
        onChange={(event) => onHasta(event.target.value)}
      />
    </Card>
  );
}
```
Si `Card` de ui-web no acepta `aria-label`/`className` con esas props, usar un `<section aria-label="Filtros del listado">` con las mismas clases de `ListFilters`. `EstablishmentStructure` se importa como tipo de `../../organizacion` (ya exportado).

`LineasVentaTable.tsx`: `DataTable<LineaVenta>` (o tabla simple con `Card`) con columnas `Producto` (descripción), `Cantidad` (`{cantidad} {unidadVentaCodigo}`), `Precio` (`formatoMoneda`), `Total` (`formatoMoneda(totalLinea)`), `Lotes` (cada lote como `<li>Lote {loteId} × {cantidad}</li>` en una `ul`, o `—` si no hay); `emptyMessage="La venta no tiene líneas."`.

`ComprobanteVenta.tsx`:

```tsx
import { Button, Card } from '@boticas/ui-web';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import type { Venta } from '../api/ventas.types';

type ComprobanteVentaProps = {
  venta: Venta;
  nombreEstablecimiento: string;
};

export function ComprobanteVenta({ venta, nombreEstablecimiento }: ComprobanteVentaProps) {
  return (
    <div className="space-y-4">
      <Card className="comprobante-imprimible mx-auto max-w-sm space-y-3 p-6 text-sm">
        <p className="text-center text-xs font-semibold uppercase">
          Comprobante interno — no válido como comprobante de pago
        </p>
        <div className="text-center">
          <p className="font-semibold">{nombreEstablecimiento}</p>
          <p>{venta.numeroOperacion}</p>
          <p>{formatoFechaHora(venta.fechaVenta)}</p>
          {venta.anulacion ? (
            <p className="font-semibold">ANULADA — {venta.anulacion.motivo}</p>
          ) : null}
        </div>
        <ul className="divide-y divide-neutral-200">
          {venta.lineas.map((linea) => (
            <li key={linea.numeroLinea} className="flex justify-between gap-2 py-2">
              <span>
                {linea.descripcion}
                <br />
                {linea.cantidad} × {formatoMoneda(linea.precioUnitario)}
              </span>
              <span>{formatoMoneda(linea.totalLinea)}</span>
            </li>
          ))}
        </ul>
        <dl className="space-y-1">
          <FilaTotal etiqueta="Subtotal" valor={venta.subtotal} />
          <FilaTotal etiqueta="IGV" valor={venta.impuestoTotal} />
          <FilaTotal etiqueta="Total" valor={venta.total} negrita />
          <FilaTotal etiqueta="Recibido" valor={venta.pago.montoRecibido} />
          <FilaTotal etiqueta="Vuelto" valor={venta.pago.vuelto} />
        </dl>
      </Card>
      <div className="flex justify-center">
        <Button variant="secondary" onClick={() => window.print()}>
          Imprimir comprobante
        </Button>
      </div>
    </div>
  );
}

function FilaTotal({ etiqueta, valor, negrita = false }: { etiqueta: string; valor: number; negrita?: boolean }) {
  return (
    <div className={negrita ? 'flex justify-between font-semibold' : 'flex justify-between'}>
      <dt>{etiqueta}</dt>
      <dd>{formatoMoneda(valor)}</dd>
    </div>
  );
}
```
`FilaTotal` queda en el mismo archivo solo si no excede la cobertura (se cubre al renderizar). La clase `negrita` por defecto `false` exige un test con ambas ramas (el `Total` la usa; las otras no).

`styles.css` de `ui-web` (agregar al final):

```css
@media print {
  body * {
    visibility: hidden;
  }

  .comprobante-imprimible,
  .comprobante-imprimible * {
    visibility: visible;
  }

  .comprobante-imprimible {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    max-width: none;
    border: 0;
    box-shadow: none;
  }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/ventas && pnpm typecheck && pnpm lint`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(ventas): tabla, filtros, lineas y comprobante interno imprimible

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Páginas, rutas, índice, baseline y e2e

**Files:**
- Modify (reemplazar contenido): `SRC/features/ventas/pages/SalesPage.tsx`; Create: `SRC/features/ventas/pages/SalesPage.test.tsx`
- Create: `SRC/features/ventas/pages/SaleDetailPage.tsx` y `.test.tsx`
- Modify: `SRC/features/ventas/routes.tsx`; Create: `SRC/features/ventas/routes.test.ts`
- Modify: `SRC/features/ventas/index.ts`, `SRC/app/feature-routes.test.ts`, `frontend/coverage-baseline.txt`
- Create: `E2E/support/ventas-api.ts`, `E2E/ventas.spec.ts`

**Interfaces:**
- Consumes: Tasks 1–3, `useEstablecimientos` (organización), `useRouteParam`.
- Produces: `index.ts` de ventas exporta `salesRoutes`, `registrarVenta`, `invalidateVentas`, `ComprobanteVenta` y los tipos `Venta`, `RegistrarVentaPayload`.
- Textos exactos: títulos `Ventas` y `Venta {numeroOperacion}`; secciones `Resumen`, `Líneas`, `Pago`, `Anulación`; migas con enlace a `/ventas`.

- [ ] **Step 1: Escribir los tests que fallan**

`SalesPage.test.tsx` (estilo de `InventoryPage.test.tsx`, con `renderRoute('/ventas', SalesPage, entrada)`): (a) lista las ventas con número, total y enlace al detalle; (b) estados de carga, vacío y error; (c) filtrar por establecimiento y fechas actualiza la URL y envía `establecimientoId`, `desde` (= `inicioDelDia`) y `hasta` (= `finDelDia`) al backend; (d) sin fechas no envía `desde`/`hasta`; (e) paginación: ir a otra página envía `page`.

`SaleDetailPage.test.tsx` (`renderRoute('/ventas/:ventaId', SaleDetailPage, '/ventas/venta-1')`): (a) muestra resumen (número, fecha, estado, establecimiento por nombre desde la estructura corporativa, vendedor), las líneas con sus lotes y el pago (`Recibido`, `Vuelto`); (b) para `sampleVentaAnulada` muestra la sección `Anulación` con el motivo y la fecha; para una venta confirmada no la muestra; (c) muestra el `ComprobanteVenta` con su botón `Imprimir comprobante`; (d) 404 muestra el error mediante `describeApiError`; (e) cargando muestra `Cargando…`.

`routes.test.ts` análogo al de caja con `['ventas', 'ventas/:ventaId']`. En `SRC/app/feature-routes.test.ts` reemplazar `'ventas'` por `'ventas'`, `'ventas/:ventaId'` en la posición correspondiente de la lista esperada.

`E2E/support/ventas-api.ts`: `mockVentasApi(page)` que registra `estructura-corporativa`, `GET /api/v1/ventas/ventas` (devuelve `pagina([sampleVentaResumen, {...sampleVentaResumen, id: 'venta-2', numeroOperacion: 'EST001-T01-000002', estado: 'ANULADA'}])`, filtrando por `establecimientoId` cuando llega) y `GET /api/v1/ventas/ventas/{id}` (devuelve `sampleVenta` o `sampleVentaAnulada` si el id es `venta-2`), más `abrirVentasEn(page, path)` (mock + `login` + `goto`).

`E2E/ventas.spec.ts`:

```ts
import { expect, test } from '@playwright/test';
import { expectNoHorizontalOverflow } from './support/layout';
import { abrirVentasEn } from './support/ventas-api';

test.describe('Ventas', () => {
  test('lista el historial y abre el detalle con el comprobante', async ({ page }) => {
    await abrirVentasEn(page, '/ventas');

    await expect(page.getByRole('heading', { name: 'Ventas', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Ver detalle de la venta EST001-T01-000001' })).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('link', { name: 'Ver detalle de la venta EST001-T01-000001' }).click();

    await expect(page.getByRole('heading', { name: 'Venta EST001-T01-000001' })).toBeVisible();
    await expect(page.getByText('Paracetamol 500 mg').first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Imprimir comprobante' })).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });

  test('muestra el motivo de una venta anulada', async ({ page }) => {
    await abrirVentasEn(page, '/ventas/venta-2');

    await expect(page.getByText('Error de digitación').first()).toBeVisible();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/ventas src/app`
Expected: FAIL.

- [ ] **Step 3: Implementar**

`SalesPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@boticas/ui-web';
import { useEstablecimientos } from '../../organizacion';
import { ventasQuery } from '../api/ventas.api';
import { FiltrosVentas } from '../components/FiltrosVentas';
import { VentasTable } from '../components/VentasTable';
import { finDelDia, inicioDelDia } from '../lib/fechas';
import { useVentasFiltros } from '../lib/use-ventas-filtros';

export function SalesPage() {
  const { filtros, setEstablecimiento, setDesde, setHasta, setPage, setSize } = useVentasFiltros();
  const establecimientos = useEstablecimientos();
  const { data, isPending, isError } = useQuery(
    ventasQuery({
      establecimientoId: filtros.establecimientoId,
      desde: filtros.desde === '' ? undefined : inicioDelDia(filtros.desde),
      hasta: filtros.hasta === '' ? undefined : finDelDia(filtros.hasta),
      page: filtros.page,
      size: filtros.size
    })
  );

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Ventas"
        context="Operaciones / Ventas"
        description="Historial de ventas por local y fecha."
      />
      <FiltrosVentas
        establecimientos={establecimientos}
        filtros={filtros}
        onEstablecimiento={setEstablecimiento}
        onDesde={setDesde}
        onHasta={setHasta}
      />
      <div className="mt-6">
        <VentasTable
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

`SaleDetailPage.tsx` (patrón de `LoteDetailPage.tsx`): `useRouteParam('ventaId')`, `useQuery(ventaQuery(ventaId))`, `useEstablecimientos()` para resolver el nombre (`establecimientos.find(({ id }) => id === venta.establecimientoId)?.name ?? venta.establecimientoId`); `Cargando…`; `FormError` con `describeApiError`; `PageHeader title={`Venta ${venta.numeroOperacion}`} context={<Link to="/ventas">Ventas</Link>}`; `Card` `Resumen` con `DatoItem` (`Estado` con `EstadoVentaBadge`, `Fecha`, `Establecimiento`, `Vendedor`, `Terminal`, `Total`); sección `Líneas` con `LineasVentaTable`; sección `Pago` (`Medio de pago`, `Monto`, `Recibido`, `Vuelto`); sección `Anulación` solo si `venta.anulacion` (`Motivo`, `Anulada el`, `Anulada por`); `ComprobanteVenta venta nombreEstablecimiento`.

`routes.tsx`:

```tsx
import type { RouteObject } from 'react-router';

export const salesRoutes = [
  {
    path: 'ventas',
    lazy: async () => {
      const { SalesPage } = await import('./pages/SalesPage');
      return { Component: SalesPage };
    }
  },
  {
    path: 'ventas/:ventaId',
    lazy: async () => {
      const { SaleDetailPage } = await import('./pages/SaleDetailPage');
      return { Component: SaleDetailPage };
    }
  }
] satisfies RouteObject[];
```

`index.ts`:

```ts
export { salesRoutes } from './routes';
export { registrarVenta } from './api/ventas.api';
export { invalidateVentas } from './api/invalidate';
export { ComprobanteVenta } from './components/ComprobanteVenta';
export type { Venta, RegistrarVentaPayload } from './api/ventas.types';
```

- [ ] **Step 4: Ejecutar y verificar que pasa, quitar baseline y verificación completa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/ventas src/app` → PASS. Quitar `apps/erp-web/src/features/ventas/routes.tsx` de `frontend/coverage-baseline.txt`. Run: `pnpm e2e -- ventas.spec.ts` → PASS en desktop, tablet y móvil; `pnpm check` → verde.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(ventas): historial, detalle de venta, rutas y e2e

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Cobertura del spec (sección ventas):** API de registrar/obtener/listar con `Idempotency-Key` y filtros `establecimientoId`, `desde`, `hasta` y paginación (Task 2); historial paginado con filtros de fecha y local (Tasks 3–4); detalle con líneas, lotes consumidos y pago (Tasks 3–4, más la anulación solo en lectura porque el backend ya la devuelve); comprobante interno imprimible reutilizable por el POS y publicado por `index.ts` (Tasks 3–4); rutas lazy y `feature-routes.test` actualizado (Task 4); estados de carga/vacío/error; e2e en tres tamaños con `page.route`. Los filtros de fecha convierten el día local a instante UTC porque el backend los recibe como `Instant`.

**Duplicación evitada:** la lógica de filtros en la URL se extrae a `shared/lib/use-filtros-url.ts` y la consumen posiciones y ventas; `useEstablecimientos` reemplaza el aplanado manual en `TerminalSelector`.

**Escaneo de placeholders:** los tests de componentes y páginas se describen como casos obligatorios con valores concretos (el implementador los escribe con el estilo del repositorio); el código de producción y los tests de API/lógica pura están completos. Notas de verificación explícitas: props de `Card` y formato `Intl` de moneda.

**Consistencia de tipos:** `Venta`, `VentaResumen`, `RegistrarVentaPayload`, `registrarVenta(client, payload, idempotencyKey)`, `invalidateVentas`, `ComprobanteVenta({ venta, nombreEstablecimiento })` son los nombres que usa el plan 4; `sampleVenta` y `sampleVentaResumen` viven en `test/ventas-fixtures.ts`, creado en el plan 2.
