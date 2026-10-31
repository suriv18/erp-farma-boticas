# Compras, parte 5: recepción de mercadería (frontend) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar la pantalla `/compras/ordenes/:ordenId/recepcion` para registrar una recepción (total o parcial) contra una orden `EMITIDA` o `PARCIALMENTE_RECIBIDA`, integrada contra `POST /api/v1/compras/recepciones` con `Idempotency-Key`, y mostrar en el detalle de la orden el acceso `Registrar recepción` y la lista de recepciones (`GET /api/v1/compras/recepciones?ordenCompraId`).

**Architecture:** La validación y el armado del cuerpo son lógica pura (`lib/recepcion-items.ts`, `lib/recepcion-cabecera.ts`) probada al 100%, igual que `orden-calculo`/`orden-cabecera` de la parte 4. Las validaciones numéricas que ya existían privadas en `orden-calculo.ts` se extraen a `lib/numeros-compras.ts` para reutilizarlas sin duplicar, y la conversión "texto opcional a número" pasa a `shared/lib/form-values.ts` (`numeroOpcional`). El estado de la pantalla vive en `RecepcionPage` con `useState`; la clave de idempotencia sale de `useClaveIdempotencia` (estable por contenido, `reiniciar()` tras el éxito) y la invalidación de inventario usa `invalidateInventario` publicado por `features/inventario/index.ts`. Las consultas de recepciones cuelgan de la clave `['compras', 'recepciones', ...]`, así que `invalidateCompras` (ya llamado por `useMutacionCompras`) refresca órdenes y recepciones a la vez.

**Tech Stack:** React 19.2, React Router 8, TanStack Query, `@boticas/ui-web`, Vitest + Testing Library + MSW, Playwright.

Spec: `docs/superpowers/specs/2026-10-06-frontend-compras-design.md` (secciones Recepción, Reuso y arquitectura, Errores y estados, Pruebas, Supuestos). Requiere integradas las partes 1 a 4 (endpoint `GET /recepciones?ordenCompraId`, `useMutacionCompras` con `onSuccess(data)`, `formatoImporte`/`formatoFecha`, `fechaLocalISO`, `OrdenDetailPage`, rutas de compras).

## Global Constraints

- Todo archivo **nuevo** (código y tests) alcanza 100% de líneas, ramas, funciones y sentencias; no agregar archivos a `coverage-baseline.txt`.
- Sin comentarios en el código; sin duplicación; `forwardRef` y React Router 8 bloqueados por ESLint; las features solo se importan por su `index.ts` (`../../inventario`, `../../organizacion`).
- Contrato real del backend (`RecepcionRequest`, `ItemRecepcionRequest`, `Recepcion.registrar`): `ordenCompraId` y `almacenId` obligatorios; `documentoProveedorTipo` hasta 2 caracteres (el backend usa `01` si se omite), serie hasta 20, número hasta 40, guías de remisión (remitente y transportista) hasta 80, observación hasta 1000; temperatura entre -50 y 100 con hasta 2 decimales; humedad relativa entre 0 y 100 con hasta 2 decimales; entre 1 y 200 ítems. Por ítem: `numeroLineaOrden`, `numeroLote` de 1 a 120 caracteres, `fechaVencimiento` obligatoria, `fechaFabricacion` opcional, `cantidadRecibida > 0` con hasta 4 decimales, `cantidadRechazada` entre 0 y la recibida con hasta 4 decimales, motivo obligatorio (hasta 1000) si hay rechazo, `costoUnitario >= 0` con hasta 6 decimales.
- Reglas del frontend: solo se envían los ítems con cantidad recibida distinta de vacío y de cero; el costo unitario arranca con el precio unitario de la línea de la orden; solo se muestran las líneas con `cantidadPendiente > 0`; el vencimiento debe ser **posterior** a la fabricación (spec; el backend solo rechaza fabricación posterior al vencimiento); un ítem con cantidad aceptada (recibida − rechazada) mayor que cero no puede tener vencimiento anterior a hoy (espejo de `INV_LOTE_VENCIDO` de inventario; un lote vencido rechazado por completo sí se puede registrar). El exceso sobre lo pendiente más la tolerancia lo valida el backend (`COM_RECEPCION_EXCEDE_PENDIENTE`); la pantalla muestra lo pendiente como guía.
- El almacén se elige entre los almacenes `ACTIVE` del establecimiento destino de la orden (`useEstablecimientos`). El tipo de documento del proveedor se elige entre `01` Factura (por defecto, igual que el backend) y `03` Boleta de venta (decisión POR_VALIDAR: el backend acepta cualquier código de 2 caracteres).
- Los errores de cabecera e ítems se muestran solo después de intentar registrar (`intentado`). Los `COM_RECEPCION_*` ya traducidos en `lib/errores-compras.ts` se reutilizan; `COM_RECEPCION_INVALIDA` se deja sin traducción a propósito porque su detalle del backend ya es específico (se muestra detalle y código).
- Textos exactos: ver cada tarea (los usan los tests y el e2e).
- Comandos desde `frontend/` (`pnpm.cmd` si `pnpm.ps1` está bloqueado): `pnpm exec vitest run <ruta>`, `pnpm typecheck`, `pnpm lint`, `pnpm format`, `pnpm check`, `pnpm e2e compras.spec.ts`.
- `git add` solo de las rutas exactas listadas en cada commit (el árbol tiene cambios ajenos sin commitear); nunca `git add -A`, `git add frontend`, `git stash` ni `git clean`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan difiere por nombres reales del repo (labels, fixtures), ajustar al comportamiento real sin reducir lo que verifica y anotarlo en el reporte.

Rutas abreviadas: `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: API de recepciones y lógica pura de validación y armado

**Files:**
- Create: `SRC/features/compras/lib/numeros-compras.ts` y `.test.ts`
- Modify: `SRC/features/compras/lib/orden-calculo.ts` (usa `numeros-compras`)
- Modify: `SRC/features/compras/lib/orden-cabecera.ts` (usa `esPrecio` y `numeroOpcional`)
- Modify: `SRC/shared/lib/form-values.ts`; Create: `SRC/shared/lib/form-values.test.ts`
- Modify: `SRC/features/compras/lib/estado-orden.ts`, `SRC/features/compras/lib/estado-orden.test.ts`
- Create: `SRC/features/compras/api/recepciones.types.ts`
- Create: `SRC/features/compras/api/recepciones.api.ts` y `.test.ts`
- Create: `SRC/features/compras/lib/recepcion-items.ts` y `.test.ts`
- Create: `SRC/features/compras/lib/recepcion-cabecera.ts` y `.test.ts`
- Modify: `SRC/test/compras-fixtures.ts` (agrega `sampleRecepcion`)

**Interfaces:**
- Consumes: `Orden`, `LineaOrden` (`api/ordenes.types.ts`), `redondear` (`shared/lib/redondeo`), `emptyToUndefined` (`shared/lib/form-values`), `EstablishmentStructure`, `OrganizationalNode` (`../../organizacion`), `PaginaResponse`, `withQuery`, `apiClient`.
- Produces:

```ts
// lib/numeros-compras.ts
export function esCantidadNoNegativa(texto: string): boolean;
export function esCantidad(texto: string): boolean;
export function esPrecio(texto: string): boolean;
export function esMonto(texto: string): boolean;
export function esTolerancia(texto: string): boolean;
// shared/lib/form-values.ts
export function numeroOpcional(value: string): number | undefined;
// lib/estado-orden.ts
export const puedeRecibir: (estado: EstadoOrden) => boolean;
// api/recepciones.types.ts
export type LineaRecepcion = {
  id: string; numeroLinea: number; numeroLineaOrden: number; skuId: string; numeroLote: string;
  fechaFabricacion: string | null; fechaVencimiento: string; cantidadRecibida: number;
  cantidadAceptada: number; cantidadRechazada: number; costoUnitario: number | null;
  decisionCalidad: string; motivoDecision: string | null; observacion: string | null; loteId: string | null;
};
export type Recepcion = {
  id: string; numero: string; ordenCompraId: string; proveedorId: string; establecimientoId: string;
  almacenId: string; documentoProveedorTipo: string | null; documentoProveedorSerie: string | null;
  documentoProveedorNumero: string | null; guiaRemisionRemitente: string | null;
  guiaRemisionTransportista: string | null; fechaRecepcion: string; temperaturaRecepcionC: number | null;
  humedadRelativaPct: number | null; estado: string; observacion: string | null; lineas: LineaRecepcion[];
};
export type ItemRecepcionPayload = {
  numeroLineaOrden: number; numeroLote: string; fechaFabricacion?: string | undefined;
  fechaVencimiento: string; cantidadRecibida: number; cantidadRechazada: number;
  motivoRechazo?: string | undefined; costoUnitario: number;
};
export type RegistrarRecepcionPayload = {
  ordenCompraId: string; almacenId: string; documentoProveedorTipo: string;
  documentoProveedorSerie?: string | undefined; documentoProveedorNumero?: string | undefined;
  guiaRemisionRemitente?: string | undefined; guiaRemisionTransportista?: string | undefined;
  temperaturaRecepcionC?: number | undefined; humedadRelativaPct?: number | undefined;
  observacion?: string | undefined; items: ItemRecepcionPayload[];
};
// api/recepciones.api.ts
export type FetchRecepcionesParams = { ordenCompraId: string; page?: number | undefined; size?: number | undefined };
export function fetchRecepciones(client: ApiClient, params: FetchRecepcionesParams): Promise<PaginaResponse<Recepcion>>;
export function recepcionesQuery(params: FetchRecepcionesParams); // queryKey ['compras','recepciones','orden', ordenCompraId, page ?? 0, size ?? 20]
export function fetchRecepcion(client: ApiClient, recepcionId: string): Promise<Recepcion>;
export function registrarRecepcion(client: ApiClient, payload: RegistrarRecepcionPayload, idempotencyKey: string): Promise<Recepcion>;
// lib/recepcion-items.ts
export type ItemBorrador = {
  numeroLineaOrden: number; descripcion: string; unidadMedidaCodigo: string; cantidadPendiente: number;
  numeroLote: string; fechaFabricacion: string; fechaVencimiento: string; cantidadRecibida: string;
  cantidadRechazada: string; motivoRechazo: string; costoUnitario: string;
};
export type CambiosItem = Partial<Pick<ItemBorrador, 'numeroLote' | 'fechaFabricacion' | 'fechaVencimiento'
  | 'cantidadRecibida' | 'cantidadRechazada' | 'motivoRechazo' | 'costoUnitario'>>;
export function itemsDesdeOrden(orden: Orden): ItemBorrador[];
export function actualizarItem(items: ItemBorrador[], numeroLineaOrden: number, cambios: CambiosItem): ItemBorrador[];
export function itemIncluido(item: ItemBorrador): boolean;
export function errorItem(item: ItemBorrador, hoy: string): string | null;
export function toItemsPayload(items: ItemBorrador[]): ItemRecepcionPayload[];
// lib/recepcion-cabecera.ts
export type CabeceraRecepcion = {
  almacenId: string; documentoProveedorTipo: string; documentoProveedorSerie: string;
  documentoProveedorNumero: string; guiaRemisionRemitente: string; guiaRemisionTransportista: string;
  temperatura: string; humedad: string; observacion: string;
};
export type ErroresCabeceraRecepcion = Partial<Record<keyof CabeceraRecepcion, string>>;
export const TIPOS_DOCUMENTO_PROVEEDOR: readonly { codigo: string; nombre: string }[];
export const CABECERA_RECEPCION_VACIA: CabeceraRecepcion;
export function almacenesDeDestino(establecimientos: EstablishmentStructure[], establecimientoId: string): OrganizationalNode[];
export function erroresCabeceraRecepcion(cabecera: CabeceraRecepcion): ErroresCabeceraRecepcion;
export function puedeRegistrar(cabecera: CabeceraRecepcion, items: ItemBorrador[], hoy: string): boolean;
export function toRegistrarRecepcionPayload(ordenCompraId: string, cabecera: CabeceraRecepcion, items: ItemBorrador[]): RegistrarRecepcionPayload;
// test/compras-fixtures.ts
export const sampleRecepcion: Recepcion;
```

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/features/compras/lib/numeros-compras.test.ts`:

```ts
import { esCantidad, esCantidadNoNegativa, esMonto, esPrecio, esTolerancia } from './numeros-compras';

describe('numeros-compras', () => {
  it('esCantidadNoNegativa acepta cero y hasta 4 decimales', () => {
    expect(esCantidadNoNegativa('0')).toBe(true);
    expect(esCantidadNoNegativa('1.2345')).toBe(true);
    expect(esCantidadNoNegativa('1.23456')).toBe(false);
    expect(esCantidadNoNegativa('-1')).toBe(false);
    expect(esCantidadNoNegativa('')).toBe(false);
  });

  it('esCantidad además exige un valor mayor que cero', () => {
    expect(esCantidad('0')).toBe(false);
    expect(esCantidad('0.0001')).toBe(true);
  });

  it('esPrecio acepta cero y hasta 6 decimales', () => {
    expect(esPrecio('0')).toBe(true);
    expect(esPrecio('1.123456')).toBe(true);
    expect(esPrecio('1.1234567')).toBe(false);
    expect(esPrecio('abc')).toBe(false);
  });

  it('esMonto acepta hasta 2 decimales', () => {
    expect(esMonto('1.23')).toBe(true);
    expect(esMonto('1.234')).toBe(false);
  });

  it('esTolerancia acepta de 0 a 100 con hasta 4 decimales', () => {
    expect(esTolerancia('100')).toBe(true);
    expect(esTolerancia('100.0001')).toBe(false);
    expect(esTolerancia('1.00001')).toBe(false);
  });
});
```

`SRC/shared/lib/form-values.test.ts`:

```ts
import { emptyToUndefined, numeroOpcional, orEmpty } from './form-values';

describe('form-values', () => {
  it('emptyToUndefined recorta y convierte el vacío en undefined', () => {
    expect(emptyToUndefined('  ')).toBeUndefined();
    expect(emptyToUndefined(' F001 ')).toBe('F001');
  });

  it('orEmpty cambia null por texto vacío', () => {
    expect(orEmpty(null)).toBe('');
    expect(orEmpty('x')).toBe('x');
  });

  it('numeroOpcional convierte el texto recortado y deja undefined si está vacío', () => {
    expect(numeroOpcional('  ')).toBeUndefined();
    expect(numeroOpcional(' 3.81 ')).toBe(3.81);
    expect(numeroOpcional('-4.5')).toBe(-4.5);
  });
});
```

En `SRC/features/compras/lib/estado-orden.test.ts` agregar `puedeRecibir` al import y este test dentro del `describe`:

```ts
  it('recibir solo cuando está emitida o parcialmente recibida', () => {
    expect(ESTADOS_ORDEN.filter(puedeRecibir)).toEqual(['EMITIDA', 'PARCIALMENTE_RECIBIDA']);
  });
```

`SRC/features/compras/api/recepciones.api.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleRecepcion } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  fetchRecepcion,
  fetchRecepciones,
  recepcionesQuery,
  registrarRecepcion
} from './recepciones.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('recepciones.api', () => {
  it('registrarRecepcion hace POST con el cuerpo y la Idempotency-Key', async () => {
    let body: unknown;
    let clave: string | null = null;
    server.use(
      http.post('http://localhost/api/v1/compras/recepciones', async ({ request }) => {
        clave = request.headers.get('Idempotency-Key');
        body = await request.json();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const payload = {
      ordenCompraId: 'orden-1',
      almacenId: 'alm-1',
      documentoProveedorTipo: '01',
      items: [
        {
          numeroLineaOrden: 1,
          numeroLote: 'L2026-01',
          fechaVencimiento: '2028-12-31',
          cantidadRecibida: 4,
          cantidadRechazada: 0,
          costoUnitario: 5.5
        }
      ]
    };

    await expect(registrarRecepcion(client, payload, 'clave-1')).resolves.toEqual(sampleRecepcion);
    expect(body).toEqual(payload);
    expect(clave).toBe('clave-1');
  });

  it('fetchRecepciones envía la orden, la página y el tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/recepciones', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleRecepcion]));
      })
    );

    const result = await fetchRecepciones(client, { ordenCompraId: 'orden-1', page: 1, size: 50 });

    expect(recibido?.search).toBe('?ordenCompraId=orden-1&page=1&size=50');
    expect(result.items).toEqual([sampleRecepcion]);
  });

  it('fetchRecepciones usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/recepciones', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchRecepciones(client, { ordenCompraId: 'orden-1' });

    expect(recibido?.search).toBe('?ordenCompraId=orden-1&page=0&size=20');
  });

  it('fetchRecepcion obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/compras/recepciones/rec-1', () =>
        HttpResponse.json(sampleRecepcion)
      )
    );

    await expect(fetchRecepcion(client, 'rec-1')).resolves.toEqual(sampleRecepcion);
  });

  it('recepcionesQuery arma la clave bajo compras y consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValueOnce(pagina([sampleRecepcion]));
    const queryClient = new QueryClient();
    const consulta = recepcionesQuery({ ordenCompraId: 'orden-1', size: 100 });

    await queryClient.fetchQuery(consulta);

    expect(consulta.queryKey).toEqual(['compras', 'recepciones', 'orden', 'orden-1', 0, 100]);
    expect(recepcionesQuery({ ordenCompraId: 'orden-1' }).queryKey).toEqual([
      'compras',
      'recepciones',
      'orden',
      'orden-1',
      0,
      20
    ]);
    expect(get).toHaveBeenCalledWith('/compras/recepciones?ordenCompraId=orden-1&page=0&size=100');
  });
});
```

`SRC/features/compras/lib/recepcion-items.test.ts`:

```ts
import { sampleOrden } from '../../../test/compras-fixtures';
import {
  actualizarItem,
  errorItem,
  itemIncluido,
  itemsDesdeOrden,
  toItemsPayload,
  type ItemBorrador
} from './recepcion-items';

const HOY = '2026-10-08';
const ITEM: ItemBorrador = {
  numeroLineaOrden: 1,
  descripcion: 'Paracetamol 500 mg',
  unidadMedidaCodigo: 'UND',
  cantidadPendiente: 10,
  numeroLote: '',
  fechaFabricacion: '',
  fechaVencimiento: '',
  cantidadRecibida: '',
  cantidadRechazada: '0',
  motivoRechazo: '',
  costoUnitario: '5.5'
};
const valido = (cambios: Partial<ItemBorrador> = {}): ItemBorrador => ({
  ...ITEM,
  numeroLote: 'L2026-01',
  fechaVencimiento: '2027-12-31',
  cantidadRecibida: '4',
  ...cambios
});

describe('itemsDesdeOrden', () => {
  it('arma un ítem por línea pendiente con el costo por defecto del precio de la orden', () => {
    expect(itemsDesdeOrden(sampleOrden)).toEqual([ITEM]);
  });

  it('omite las líneas sin pendiente', () => {
    const orden = {
      ...sampleOrden,
      lineas: sampleOrden.lineas.flatMap((linea) => [
        { ...linea, cantidadRecibida: 10, cantidadPendiente: 0 },
        {
          ...linea,
          numeroLinea: 2,
          descripcion: 'Ibuprofeno 400 mg',
          precioUnitario: 3.25,
          cantidadRecibida: 4,
          cantidadPendiente: 6
        }
      ])
    };

    expect(itemsDesdeOrden(orden)).toEqual([
      {
        ...ITEM,
        numeroLineaOrden: 2,
        descripcion: 'Ibuprofeno 400 mg',
        cantidadPendiente: 6,
        costoUnitario: '3.25'
      }
    ]);
  });
});

describe('actualizarItem e itemIncluido', () => {
  it('cambia solo el ítem indicado', () => {
    const otro = { ...ITEM, numeroLineaOrden: 2 };

    const resultado = actualizarItem([ITEM, otro], 1, { numeroLote: 'L1', cantidadRecibida: '3' });

    expect(resultado[0]).toEqual({ ...ITEM, numeroLote: 'L1', cantidadRecibida: '3' });
    expect(resultado[1]).toBe(otro);
  });

  it.each(['', '   ', '0', '0.0'])('no incluye un ítem con cantidad recibida "%s"', (cantidadRecibida) => {
    expect(itemIncluido({ ...ITEM, cantidadRecibida })).toBe(false);
  });

  it.each(['4', ' 4 ', 'abc', '-1'])('incluye un ítem con cantidad recibida "%s"', (cantidadRecibida) => {
    expect(itemIncluido({ ...ITEM, cantidadRecibida })).toBe(true);
  });
});

describe('errorItem', () => {
  it('no reporta error en un ítem válido', () => {
    expect(errorItem(valido(), HOY)).toBeNull();
    expect(errorItem(valido({ fechaFabricacion: '2026-01-01' }), HOY)).toBeNull();
    expect(errorItem(valido({ fechaVencimiento: HOY }), HOY)).toBeNull();
    expect(errorItem(valido({ costoUnitario: '0' }), HOY)).toBeNull();
  });

  it.each(['', '   ', 'L'.repeat(121)])('rechaza el lote "%s"', (numeroLote) => {
    expect(errorItem(valido({ numeroLote }), HOY)).toBe(
      'El número de lote es obligatorio y admite hasta 120 caracteres.'
    );
  });

  it('exige la fecha de vencimiento', () => {
    expect(errorItem(valido({ fechaVencimiento: '' }), HOY)).toBe(
      'La fecha de vencimiento es obligatoria.'
    );
  });

  it.each(['2027-12-31', '2028-01-01'])(
    'exige un vencimiento posterior a la fabricación %s',
    (fechaFabricacion) => {
      expect(errorItem(valido({ fechaFabricacion }), HOY)).toBe(
        'La fecha de vencimiento debe ser posterior a la de fabricación.'
      );
    }
  );

  it.each(['abc', '-1', '1.23456', '0'])('rechaza la cantidad recibida "%s"', (cantidadRecibida) => {
    expect(errorItem(valido({ cantidadRecibida }), HOY)).toBe(
      'La cantidad recibida debe ser mayor que cero con hasta 4 decimales.'
    );
  });

  it.each(['', '-1', '4.5', '0.00001'])('rechaza la cantidad rechazada "%s"', (cantidadRechazada) => {
    expect(errorItem(valido({ cantidadRechazada }), HOY)).toBe(
      'La cantidad rechazada debe estar entre 0 y la cantidad recibida con hasta 4 decimales.'
    );
  });

  it('exige el motivo cuando hay rechazo', () => {
    expect(errorItem(valido({ cantidadRechazada: '1' }), HOY)).toBe('Indica el motivo del rechazo.');
    expect(errorItem(valido({ cantidadRechazada: '1', motivoRechazo: '   ' }), HOY)).toBe(
      'Indica el motivo del rechazo.'
    );
    expect(
      errorItem(valido({ cantidadRechazada: '1', motivoRechazo: 'Empaque dañado' }), HOY)
    ).toBeNull();
  });

  it('limita el motivo a 1000 caracteres solo cuando hay rechazo', () => {
    expect(
      errorItem(valido({ cantidadRechazada: '1', motivoRechazo: 'm'.repeat(1001) }), HOY)
    ).toBe('El motivo del rechazo admite hasta 1000 caracteres.');
    expect(errorItem(valido({ motivoRechazo: 'm'.repeat(1001) }), HOY)).toBeNull();
  });

  it.each(['', '-1', '1.1234567', 'abc'])('rechaza el costo "%s"', (costoUnitario) => {
    expect(errorItem(valido({ costoUnitario }), HOY)).toBe(
      'El costo unitario debe ser mayor o igual a cero con hasta 6 decimales.'
    );
  });

  it('no ingresa a inventario un lote vencido salvo que se rechace por completo', () => {
    expect(errorItem(valido({ fechaVencimiento: '2026-10-07' }), HOY)).toBe(
      'No se puede ingresar un lote vencido; recházalo por completo o corrige la fecha.'
    );
    expect(
      errorItem(
        valido({ fechaVencimiento: '2026-10-07', cantidadRechazada: '4', motivoRechazo: 'Vencido' }),
        HOY
      )
    ).toBeNull();
  });
});

describe('toItemsPayload', () => {
  it('envía solo los ítems recibidos, con números, lote recortado y motivo solo si hay rechazo', () => {
    expect(
      toItemsPayload([
        valido({ numeroLote: ' L1 ', fechaFabricacion: '2026-01-01' }),
        { ...ITEM, numeroLineaOrden: 2 },
        valido({ numeroLineaOrden: 3, cantidadRechazada: '1', motivoRechazo: ' Empaque dañado ' }),
        valido({ numeroLineaOrden: 4, motivoRechazo: 'ignorado' })
      ])
    ).toEqual([
      {
        numeroLineaOrden: 1,
        numeroLote: 'L1',
        fechaFabricacion: '2026-01-01',
        fechaVencimiento: '2027-12-31',
        cantidadRecibida: 4,
        cantidadRechazada: 0,
        motivoRechazo: undefined,
        costoUnitario: 5.5
      },
      {
        numeroLineaOrden: 3,
        numeroLote: 'L2026-01',
        fechaFabricacion: undefined,
        fechaVencimiento: '2027-12-31',
        cantidadRecibida: 4,
        cantidadRechazada: 1,
        motivoRechazo: 'Empaque dañado',
        costoUnitario: 5.5
      },
      {
        numeroLineaOrden: 4,
        numeroLote: 'L2026-01',
        fechaFabricacion: undefined,
        fechaVencimiento: '2027-12-31',
        cantidadRecibida: 4,
        cantidadRechazada: 0,
        motivoRechazo: undefined,
        costoUnitario: 5.5
      }
    ]);
  });
});
```

`SRC/features/compras/lib/recepcion-cabecera.test.ts`:

```ts
import { sampleEstructura } from '../../../test/inventario-fixtures';
import {
  CABECERA_RECEPCION_VACIA,
  TIPOS_DOCUMENTO_PROVEEDOR,
  almacenesDeDestino,
  erroresCabeceraRecepcion,
  puedeRegistrar,
  toRegistrarRecepcionPayload
} from './recepcion-cabecera';
import type { ItemBorrador } from './recepcion-items';

const HOY = '2026-10-08';
const completa = { ...CABECERA_RECEPCION_VACIA, almacenId: 'alm-1' };
const ITEM: ItemBorrador = {
  numeroLineaOrden: 1,
  descripcion: 'Paracetamol 500 mg',
  unidadMedidaCodigo: 'UND',
  cantidadPendiente: 10,
  numeroLote: '',
  fechaFabricacion: '',
  fechaVencimiento: '',
  cantidadRecibida: '',
  cantidadRechazada: '0',
  motivoRechazo: '',
  costoUnitario: '5.5'
};
const itemValido: ItemBorrador = {
  ...ITEM,
  numeroLote: 'L2026-01',
  fechaVencimiento: '2027-12-31',
  cantidadRecibida: '4'
};
const establecimientos = sampleEstructura.companies.flatMap(({ establishments }) => establishments);

describe('cabecera de la recepción', () => {
  it('arranca sin almacén y con factura como documento', () => {
    expect(CABECERA_RECEPCION_VACIA).toEqual({
      almacenId: '',
      documentoProveedorTipo: '01',
      documentoProveedorSerie: '',
      documentoProveedorNumero: '',
      guiaRemisionRemitente: '',
      guiaRemisionTransportista: '',
      temperatura: '',
      humedad: '',
      observacion: ''
    });
    expect(TIPOS_DOCUMENTO_PROVEEDOR).toEqual([
      { codigo: '01', nombre: 'Factura' },
      { codigo: '03', nombre: 'Boleta de venta' }
    ]);
  });
});

describe('almacenesDeDestino', () => {
  it('devuelve los almacenes del establecimiento destino', () => {
    expect(almacenesDeDestino(establecimientos, 'est-1').map(({ id }) => id)).toEqual([
      'alm-1',
      'alm-2'
    ]);
  });

  it('omite los almacenes inactivos', () => {
    const conInactivo = establecimientos.map((establecimiento) => ({
      ...establecimiento,
      warehouses: [
        ...establecimiento.warehouses,
        {
          id: `${establecimiento.id}-cerrado`,
          code: 'ALM999',
          name: 'Almacén Cerrado',
          status: 'INACTIVE' as const
        }
      ]
    }));

    expect(almacenesDeDestino(conInactivo, 'est-2').map(({ id }) => id)).toEqual(['alm-3']);
  });

  it('sin un establecimiento conocido no ofrece almacenes', () => {
    expect(almacenesDeDestino(establecimientos, 'est-x')).toEqual([]);
  });
});

describe('erroresCabeceraRecepcion', () => {
  it('no reporta errores en una cabecera con almacén', () => {
    expect(erroresCabeceraRecepcion(completa)).toEqual({});
  });

  it('exige el almacén', () => {
    expect(erroresCabeceraRecepcion(CABECERA_RECEPCION_VACIA)).toEqual({
      almacenId: 'Selecciona el almacén de recepción.'
    });
  });

  it.each([
    ['documentoProveedorSerie', 20, 'La serie admite hasta 20 caracteres.'],
    ['documentoProveedorNumero', 40, 'El número admite hasta 40 caracteres.'],
    ['guiaRemisionRemitente', 80, 'La guía de remisión admite hasta 80 caracteres.'],
    ['guiaRemisionTransportista', 80, 'La guía de remisión admite hasta 80 caracteres.'],
    ['observacion', 1000, 'La observación admite hasta 1000 caracteres.']
  ] as const)('limita %s a %i caracteres sin contar espacios de los extremos', (campo, maximo, mensaje) => {
    expect(erroresCabeceraRecepcion({ ...completa, [campo]: ` ${'a'.repeat(maximo)} ` })).toEqual({});
    expect(erroresCabeceraRecepcion({ ...completa, [campo]: 'a'.repeat(maximo + 1) })).toEqual({
      [campo]: mensaje
    });
  });

  it.each(['-50', '100', '4.25', '-0.5', ' 8 '])('acepta la temperatura "%s"', (temperatura) => {
    expect(erroresCabeceraRecepcion({ ...completa, temperatura })).toEqual({});
  });

  it.each(['-50.01', '100.5', '4.123', 'abc', '1000'])('rechaza la temperatura "%s"', (temperatura) => {
    expect(erroresCabeceraRecepcion({ ...completa, temperatura })).toEqual({
      temperatura: 'La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.'
    });
  });

  it.each(['0', '100', '65.5'])('acepta la humedad "%s"', (humedad) => {
    expect(erroresCabeceraRecepcion({ ...completa, humedad })).toEqual({});
  });

  it.each(['-1', '100.01', '50.123'])('rechaza la humedad "%s"', (humedad) => {
    expect(erroresCabeceraRecepcion({ ...completa, humedad })).toEqual({
      humedad: 'La humedad relativa debe estar entre 0 y 100 % con hasta 2 decimales.'
    });
  });
});

describe('puedeRegistrar', () => {
  it('exige cabecera válida y al menos un ítem recibido sin errores', () => {
    expect(puedeRegistrar(completa, [itemValido], HOY)).toBe(true);
    expect(puedeRegistrar(completa, [itemValido, { ...ITEM, numeroLineaOrden: 2 }], HOY)).toBe(true);
    expect(puedeRegistrar(CABECERA_RECEPCION_VACIA, [itemValido], HOY)).toBe(false);
    expect(puedeRegistrar(completa, [ITEM], HOY)).toBe(false);
    expect(puedeRegistrar(completa, [{ ...itemValido, numeroLote: '' }], HOY)).toBe(false);
  });
});

describe('toRegistrarRecepcionPayload', () => {
  it('omite los opcionales vacíos y envía solo los ítems recibidos', () => {
    expect(
      toRegistrarRecepcionPayload('orden-1', completa, [itemValido, { ...ITEM, numeroLineaOrden: 2 }])
    ).toEqual({
      ordenCompraId: 'orden-1',
      almacenId: 'alm-1',
      documentoProveedorTipo: '01',
      documentoProveedorSerie: undefined,
      documentoProveedorNumero: undefined,
      guiaRemisionRemitente: undefined,
      guiaRemisionTransportista: undefined,
      temperaturaRecepcionC: undefined,
      humedadRelativaPct: undefined,
      observacion: undefined,
      items: [
        {
          numeroLineaOrden: 1,
          numeroLote: 'L2026-01',
          fechaFabricacion: undefined,
          fechaVencimiento: '2027-12-31',
          cantidadRecibida: 4,
          cantidadRechazada: 0,
          motivoRechazo: undefined,
          costoUnitario: 5.5
        }
      ]
    });
  });

  it('incluye documento, guías, temperatura, humedad y observación cuando se llenaron', () => {
    expect(
      toRegistrarRecepcionPayload(
        'orden-1',
        {
          ...completa,
          documentoProveedorTipo: '03',
          documentoProveedorSerie: ' B001 ',
          documentoProveedorNumero: ' 98 ',
          guiaRemisionRemitente: ' T001-1 ',
          guiaRemisionTransportista: ' V001-2 ',
          temperatura: ' 4.5 ',
          humedad: '60',
          observacion: ' Cajas completas '
        },
        [itemValido]
      )
    ).toMatchObject({
      documentoProveedorTipo: '03',
      documentoProveedorSerie: 'B001',
      documentoProveedorNumero: '98',
      guiaRemisionRemitente: 'T001-1',
      guiaRemisionTransportista: 'V001-2',
      temperaturaRecepcionC: 4.5,
      humedadRelativaPct: 60,
      observacion: 'Cajas completas'
    });
  });
});
```

En `SRC/test/compras-fixtures.ts` agregar el import `import type { Recepcion } from '../features/compras/api/recepciones.types';` y al final:

```ts
export const sampleRecepcion: Recepcion = {
  id: 'rec-1',
  numero: 'REC-2026-000001',
  ordenCompraId: 'orden-1',
  proveedorId: 'prov-1',
  establecimientoId: 'est-1',
  almacenId: 'alm-1',
  documentoProveedorTipo: '01',
  documentoProveedorSerie: 'F001',
  documentoProveedorNumero: '123',
  guiaRemisionRemitente: null,
  guiaRemisionTransportista: null,
  fechaRecepcion: '2026-10-08T15:00:00Z',
  temperaturaRecepcionC: null,
  humedadRelativaPct: null,
  estado: 'CONFIRMADA',
  observacion: null,
  lineas: [
    {
      id: 'rec-linea-1',
      numeroLinea: 1,
      numeroLineaOrden: 1,
      skuId: 'sku-0001-aaaa',
      numeroLote: 'L2026-01',
      fechaFabricacion: null,
      fechaVencimiento: '2028-12-31',
      cantidadRecibida: 4,
      cantidadAceptada: 4,
      cantidadRechazada: 0,
      costoUnitario: 5.5,
      decisionCalidad: 'ACEPTADO',
      motivoDecision: null,
      observacion: null,
      loteId: 'lote-9'
    }
  ]
};
```

(el fixture se escribe en este paso porque los tests lo importan; `recepciones.types.ts` se crea en el Step 3, así que el typecheck de fixtures fallará hasta entonces).

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/shared/lib/form-values.test.ts apps/erp-web/src/features/compras/lib apps/erp-web/src/features/compras/api`
Expected: FAIL (módulos `numeros-compras`, `recepciones.api`, `recepcion-items`, `recepcion-cabecera` inexistentes; `numeroOpcional` y `puedeRecibir` no exportados).

- [ ] **Step 3: Implementar**

`SRC/features/compras/lib/numeros-compras.ts`:

```ts
const PATRON_CANTIDAD = /^\d{1,9}(\.\d{1,4})?$/;
const PATRON_PRECIO = /^\d{1,10}(\.\d{1,6})?$/;
const PATRON_MONTO = /^\d{1,10}(\.\d{1,2})?$/;
const PATRON_TOLERANCIA = /^\d{1,3}(\.\d{1,4})?$/;

export const esCantidadNoNegativa = (texto: string): boolean => PATRON_CANTIDAD.test(texto);

export const esCantidad = (texto: string): boolean =>
  esCantidadNoNegativa(texto) && Number(texto) > 0;

export const esPrecio = (texto: string): boolean => PATRON_PRECIO.test(texto);

export const esMonto = (texto: string): boolean => PATRON_MONTO.test(texto);

export const esTolerancia = (texto: string): boolean =>
  PATRON_TOLERANCIA.test(texto) && Number(texto) <= 100;
```

`SRC/features/compras/lib/orden-calculo.ts`: eliminar las líneas de `PATRON_CANTIDAD`, `PATRON_PRECIO`, `PATRON_MONTO`, `PATRON_TOLERANCIA`, `esCantidad`, `esPrecio`, `esMonto` y `esTolerancia` (hoy líneas 37-45) y agregar a los imports:

```ts
import { esCantidad, esMonto, esPrecio, esTolerancia } from './numeros-compras';
```

`SRC/shared/lib/form-values.ts`: agregar al final:

```ts
export function numeroOpcional(value: string): number | undefined {
  const limpio = emptyToUndefined(value);
  return limpio === undefined ? undefined : Number(limpio);
}
```

`SRC/features/compras/lib/orden-cabecera.ts`:
- Cambiar el import a `import { emptyToUndefined, numeroOpcional } from '../../../shared/lib/form-values';` y agregar `import { esPrecio } from './numeros-compras';`.
- Eliminar `const PATRON_TIPO_CAMBIO = /^\d{1,10}(\.\d{1,6})?$/;`.
- Reemplazar `tipoCambioInvalido` por:

```ts
const tipoCambioInvalido = (texto: string) =>
  texto !== '' && !(esPrecio(texto) && Number(texto) > 0);
```

- Eliminar la función `tipoCambioOpcional` y en `toCrearOrdenPayload` usar `tipoCambio: numeroOpcional(cabecera.tipoCambio),`.

`SRC/features/compras/lib/estado-orden.ts`: agregar después de `ANULABLES`:

```ts
const RECEPCIONABLES: readonly EstadoOrden[] = ['EMITIDA', 'PARCIALMENTE_RECIBIDA'];
```

y al final:

```ts
export const puedeRecibir = (estado: EstadoOrden): boolean => RECEPCIONABLES.includes(estado);
```

`SRC/features/compras/api/recepciones.types.ts`:

```ts
export type LineaRecepcion = {
  id: string;
  numeroLinea: number;
  numeroLineaOrden: number;
  skuId: string;
  numeroLote: string;
  fechaFabricacion: string | null;
  fechaVencimiento: string;
  cantidadRecibida: number;
  cantidadAceptada: number;
  cantidadRechazada: number;
  costoUnitario: number | null;
  decisionCalidad: string;
  motivoDecision: string | null;
  observacion: string | null;
  loteId: string | null;
};

export type Recepcion = {
  id: string;
  numero: string;
  ordenCompraId: string;
  proveedorId: string;
  establecimientoId: string;
  almacenId: string;
  documentoProveedorTipo: string | null;
  documentoProveedorSerie: string | null;
  documentoProveedorNumero: string | null;
  guiaRemisionRemitente: string | null;
  guiaRemisionTransportista: string | null;
  fechaRecepcion: string;
  temperaturaRecepcionC: number | null;
  humedadRelativaPct: number | null;
  estado: string;
  observacion: string | null;
  lineas: LineaRecepcion[];
};

export type ItemRecepcionPayload = {
  numeroLineaOrden: number;
  numeroLote: string;
  fechaFabricacion?: string | undefined;
  fechaVencimiento: string;
  cantidadRecibida: number;
  cantidadRechazada: number;
  motivoRechazo?: string | undefined;
  costoUnitario: number;
};

export type RegistrarRecepcionPayload = {
  ordenCompraId: string;
  almacenId: string;
  documentoProveedorTipo: string;
  documentoProveedorSerie?: string | undefined;
  documentoProveedorNumero?: string | undefined;
  guiaRemisionRemitente?: string | undefined;
  guiaRemisionTransportista?: string | undefined;
  temperaturaRecepcionC?: number | undefined;
  humedadRelativaPct?: number | undefined;
  observacion?: string | undefined;
  items: ItemRecepcionPayload[];
};
```

`SRC/features/compras/api/recepciones.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { Recepcion, RegistrarRecepcionPayload } from './recepciones.types';

export type FetchRecepcionesParams = {
  ordenCompraId: string;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchRecepciones(
  client: ApiClient,
  params: FetchRecepcionesParams
): Promise<PaginaResponse<Recepcion>> {
  return client.get<PaginaResponse<Recepcion>>(
    withQuery('/compras/recepciones', {
      ordenCompraId: params.ordenCompraId,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function recepcionesQuery(params: FetchRecepcionesParams) {
  return queryOptions({
    queryKey: [
      'compras',
      'recepciones',
      'orden',
      params.ordenCompraId,
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchRecepciones(apiClient, params)
  });
}

export function fetchRecepcion(client: ApiClient, recepcionId: string): Promise<Recepcion> {
  return client.get<Recepcion>(`/compras/recepciones/${recepcionId}`);
}

export function registrarRecepcion(
  client: ApiClient,
  payload: RegistrarRecepcionPayload,
  idempotencyKey: string
): Promise<Recepcion> {
  return client.post<Recepcion, RegistrarRecepcionPayload>('/compras/recepciones', payload, {
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}
```

`SRC/features/compras/lib/recepcion-items.ts`:

```ts
import { emptyToUndefined } from '../../../shared/lib/form-values';
import { redondear } from '../../../shared/lib/redondeo';
import type { Orden } from '../api/ordenes.types';
import type { ItemRecepcionPayload } from '../api/recepciones.types';
import { esCantidad, esCantidadNoNegativa, esPrecio } from './numeros-compras';

export type ItemBorrador = {
  numeroLineaOrden: number;
  descripcion: string;
  unidadMedidaCodigo: string;
  cantidadPendiente: number;
  numeroLote: string;
  fechaFabricacion: string;
  fechaVencimiento: string;
  cantidadRecibida: string;
  cantidadRechazada: string;
  motivoRechazo: string;
  costoUnitario: string;
};

export type CambiosItem = Partial<
  Pick<
    ItemBorrador,
    | 'numeroLote'
    | 'fechaFabricacion'
    | 'fechaVencimiento'
    | 'cantidadRecibida'
    | 'cantidadRechazada'
    | 'motivoRechazo'
    | 'costoUnitario'
  >
>;

export const itemsDesdeOrden = (orden: Orden): ItemBorrador[] =>
  orden.lineas
    .filter(({ cantidadPendiente }) => cantidadPendiente > 0)
    .map((linea) => ({
      numeroLineaOrden: linea.numeroLinea,
      descripcion: linea.descripcion,
      unidadMedidaCodigo: linea.unidadMedidaCodigo,
      cantidadPendiente: linea.cantidadPendiente,
      numeroLote: '',
      fechaFabricacion: '',
      fechaVencimiento: '',
      cantidadRecibida: '',
      cantidadRechazada: '0',
      motivoRechazo: '',
      costoUnitario: String(linea.precioUnitario)
    }));

export const actualizarItem = (
  items: ItemBorrador[],
  numeroLineaOrden: number,
  cambios: CambiosItem
): ItemBorrador[] =>
  items.map((item) =>
    item.numeroLineaOrden === numeroLineaOrden ? { ...item, ...cambios } : item
  );

export const itemIncluido = (item: ItemBorrador): boolean => {
  const texto = item.cantidadRecibida.trim();
  return texto !== '' && Number(texto) !== 0;
};

const cantidadAceptada = (item: ItemBorrador): number =>
  redondear(Number(item.cantidadRecibida) - Number(item.cantidadRechazada), 4);

export function errorItem(item: ItemBorrador, hoy: string): string | null {
  const lote = item.numeroLote.trim();
  if (lote === '' || lote.length > 120) {
    return 'El número de lote es obligatorio y admite hasta 120 caracteres.';
  }
  if (item.fechaVencimiento === '') return 'La fecha de vencimiento es obligatoria.';
  if (item.fechaFabricacion !== '' && item.fechaFabricacion >= item.fechaVencimiento) {
    return 'La fecha de vencimiento debe ser posterior a la de fabricación.';
  }
  if (!esCantidad(item.cantidadRecibida.trim())) {
    return 'La cantidad recibida debe ser mayor que cero con hasta 4 decimales.';
  }
  const rechazada = item.cantidadRechazada.trim();
  if (!esCantidadNoNegativa(rechazada) || Number(rechazada) > Number(item.cantidadRecibida)) {
    return 'La cantidad rechazada debe estar entre 0 y la cantidad recibida con hasta 4 decimales.';
  }
  const motivo = item.motivoRechazo.trim();
  const rechaza = Number(rechazada) > 0;
  if (rechaza && motivo === '') return 'Indica el motivo del rechazo.';
  if (rechaza && motivo.length > 1000) return 'El motivo del rechazo admite hasta 1000 caracteres.';
  if (!esPrecio(item.costoUnitario.trim())) {
    return 'El costo unitario debe ser mayor o igual a cero con hasta 6 decimales.';
  }
  return cantidadAceptada(item) > 0 && item.fechaVencimiento < hoy
    ? 'No se puede ingresar un lote vencido; recházalo por completo o corrige la fecha.'
    : null;
}

export const toItemsPayload = (items: ItemBorrador[]): ItemRecepcionPayload[] =>
  items.filter(itemIncluido).map((item) => {
    const cantidadRechazada = Number(item.cantidadRechazada);
    return {
      numeroLineaOrden: item.numeroLineaOrden,
      numeroLote: item.numeroLote.trim(),
      fechaFabricacion: emptyToUndefined(item.fechaFabricacion),
      fechaVencimiento: item.fechaVencimiento,
      cantidadRecibida: Number(item.cantidadRecibida),
      cantidadRechazada,
      motivoRechazo: cantidadRechazada > 0 ? emptyToUndefined(item.motivoRechazo) : undefined,
      costoUnitario: Number(item.costoUnitario)
    };
  });
```

`SRC/features/compras/lib/recepcion-cabecera.ts`:

```ts
import { emptyToUndefined, numeroOpcional } from '../../../shared/lib/form-values';
import type { EstablishmentStructure, OrganizationalNode } from '../../organizacion';
import type { RegistrarRecepcionPayload } from '../api/recepciones.types';
import { errorItem, itemIncluido, toItemsPayload, type ItemBorrador } from './recepcion-items';

export type CabeceraRecepcion = {
  almacenId: string;
  documentoProveedorTipo: string;
  documentoProveedorSerie: string;
  documentoProveedorNumero: string;
  guiaRemisionRemitente: string;
  guiaRemisionTransportista: string;
  temperatura: string;
  humedad: string;
  observacion: string;
};

export type ErroresCabeceraRecepcion = Partial<Record<keyof CabeceraRecepcion, string>>;

export const TIPOS_DOCUMENTO_PROVEEDOR: readonly { codigo: string; nombre: string }[] = [
  { codigo: '01', nombre: 'Factura' },
  { codigo: '03', nombre: 'Boleta de venta' }
];

export const CABECERA_RECEPCION_VACIA: CabeceraRecepcion = {
  almacenId: '',
  documentoProveedorTipo: '01',
  documentoProveedorSerie: '',
  documentoProveedorNumero: '',
  guiaRemisionRemitente: '',
  guiaRemisionTransportista: '',
  temperatura: '',
  humedad: '',
  observacion: ''
};

const LONGITUDES: readonly [keyof CabeceraRecepcion, number, string][] = [
  ['documentoProveedorSerie', 20, 'La serie admite hasta 20 caracteres.'],
  ['documentoProveedorNumero', 40, 'El número admite hasta 40 caracteres.'],
  ['guiaRemisionRemitente', 80, 'La guía de remisión admite hasta 80 caracteres.'],
  ['guiaRemisionTransportista', 80, 'La guía de remisión admite hasta 80 caracteres.'],
  ['observacion', 1000, 'La observación admite hasta 1000 caracteres.']
];

const PATRON_DOS_DECIMALES = /^-?\d{1,3}(\.\d{1,2})?$/;

const fueraDeRango = (texto: string, minimo: number, maximo: number): boolean => {
  const limpio = texto.trim();
  return (
    limpio !== '' &&
    !(PATRON_DOS_DECIMALES.test(limpio) && Number(limpio) >= minimo && Number(limpio) <= maximo)
  );
};

export const almacenesDeDestino = (
  establecimientos: EstablishmentStructure[],
  establecimientoId: string
): OrganizationalNode[] =>
  (establecimientos.find(({ id }) => id === establecimientoId)?.warehouses ?? []).filter(
    ({ status }) => status === 'ACTIVE'
  );

export function erroresCabeceraRecepcion(cabecera: CabeceraRecepcion): ErroresCabeceraRecepcion {
  const errores: ErroresCabeceraRecepcion = {};
  if (cabecera.almacenId === '') errores.almacenId = 'Selecciona el almacén de recepción.';
  LONGITUDES.forEach(([campo, maximo, mensaje]) => {
    if (cabecera[campo].trim().length > maximo) errores[campo] = mensaje;
  });
  if (fueraDeRango(cabecera.temperatura, -50, 100)) {
    errores.temperatura = 'La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.';
  }
  if (fueraDeRango(cabecera.humedad, 0, 100)) {
    errores.humedad = 'La humedad relativa debe estar entre 0 y 100 % con hasta 2 decimales.';
  }
  return errores;
}

export function puedeRegistrar(
  cabecera: CabeceraRecepcion,
  items: ItemBorrador[],
  hoy: string
): boolean {
  const incluidos = items.filter(itemIncluido);
  return (
    Object.keys(erroresCabeceraRecepcion(cabecera)).length === 0 &&
    incluidos.length > 0 &&
    incluidos.every((item) => errorItem(item, hoy) === null)
  );
}

export const toRegistrarRecepcionPayload = (
  ordenCompraId: string,
  cabecera: CabeceraRecepcion,
  items: ItemBorrador[]
): RegistrarRecepcionPayload => ({
  ordenCompraId,
  almacenId: cabecera.almacenId,
  documentoProveedorTipo: cabecera.documentoProveedorTipo,
  documentoProveedorSerie: emptyToUndefined(cabecera.documentoProveedorSerie),
  documentoProveedorNumero: emptyToUndefined(cabecera.documentoProveedorNumero),
  guiaRemisionRemitente: emptyToUndefined(cabecera.guiaRemisionRemitente),
  guiaRemisionTransportista: emptyToUndefined(cabecera.guiaRemisionTransportista),
  temperaturaRecepcionC: numeroOpcional(cabecera.temperatura),
  humedadRelativaPct: numeroOpcional(cabecera.humedad),
  observacion: emptyToUndefined(cabecera.observacion),
  items: toItemsPayload(items)
});
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/shared/lib apps/erp-web/src/features/compras && pnpm typecheck && pnpm lint`
Expected: PASS, incluidos los tests existentes de `orden-calculo` y `orden-cabecera` sin cambios (el refactor no altera su comportamiento). Luego `pnpm check` para confirmar 100% en `numeros-compras.ts`, `recepciones.api.ts`, `recepcion-items.ts` y `recepcion-cabecera.ts`.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/compras/lib/numeros-compras.ts frontend/apps/erp-web/src/features/compras/lib/numeros-compras.test.ts frontend/apps/erp-web/src/features/compras/lib/orden-calculo.ts frontend/apps/erp-web/src/features/compras/lib/orden-cabecera.ts frontend/apps/erp-web/src/shared/lib/form-values.ts frontend/apps/erp-web/src/shared/lib/form-values.test.ts frontend/apps/erp-web/src/features/compras/lib/estado-orden.ts frontend/apps/erp-web/src/features/compras/lib/estado-orden.test.ts frontend/apps/erp-web/src/features/compras/api/recepciones.types.ts frontend/apps/erp-web/src/features/compras/api/recepciones.api.ts frontend/apps/erp-web/src/features/compras/api/recepciones.api.test.ts frontend/apps/erp-web/src/features/compras/lib/recepcion-items.ts frontend/apps/erp-web/src/features/compras/lib/recepcion-items.test.ts frontend/apps/erp-web/src/features/compras/lib/recepcion-cabecera.ts frontend/apps/erp-web/src/features/compras/lib/recepcion-cabecera.test.ts frontend/apps/erp-web/src/test/compras-fixtures.ts
git commit -m "feat(compras): API de recepciones y validacion pura de la recepcion

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Formulario y página de recepción, ruta lazy

**Files:**
- Create: `SRC/features/compras/components/RecepcionCabeceraForm.tsx` y `.test.tsx`
- Create: `SRC/features/compras/components/ItemsRecepcionEditor.tsx` y `.test.tsx`
- Create: `SRC/features/compras/pages/RecepcionPage.tsx` y `.test.tsx`
- Modify: `SRC/features/compras/routes.tsx`, `SRC/features/compras/routes.test.ts`, `SRC/app/feature-routes.test.ts`

**Interfaces:**
- Consumes: Task 1 (`CabeceraRecepcion`, `ErroresCabeceraRecepcion`, `TIPOS_DOCUMENTO_PROVEEDOR`, `CABECERA_RECEPCION_VACIA`, `almacenesDeDestino`, `erroresCabeceraRecepcion`, `puedeRegistrar`, `toRegistrarRecepcionPayload`, `ItemBorrador`, `CambiosItem`, `itemsDesdeOrden`, `actualizarItem`, `itemIncluido`, `errorItem`, `registrarRecepcion`, `RegistrarRecepcionPayload`, `puedeRecibir`, `sampleRecepcion`), `ordenQuery`, `useMutacionCompras`, `describeErrorCompras`, `fechaLocalISO` (`lib/orden-cabecera`), `useClaveIdempotencia`, `useRouteParam`, `invalidateInventario` (`../../inventario`), `useEstablecimientos` y `OrganizationalNode` (`../../organizacion`).
- Produces:
  - `RecepcionCabeceraForm({ valores, errores, almacenes, onCambiar })` con `almacenes: OrganizationalNode[]` y `onCambiar(campo: keyof CabeceraRecepcion, valor: string)`.
  - `ItemsRecepcionEditor({ items, hoy, mostrarErrores, onCambiar })` con `onCambiar(numeroLineaOrden: number, cambios: CambiosItem)`.
  - `RecepcionPage` y la ruta `compras/ordenes/:ordenId/recepcion` (después de `compras/ordenes/:ordenId`).
- Textos exactos: cabecera con etiquetas `Almacén` (opción `Selecciona un almacén`), `Tipo de documento` (opciones `Factura`, `Boleta de venta`), `Serie del documento`, `Número del documento`, `Guía de remisión del remitente`, `Guía de remisión del transportista`, `Temperatura (°C)`, `Humedad relativa (%)`, `Observación`; cada línea es un `fieldset` con leyenda `Línea {numeroLineaOrden} — {descripcion}`, texto `Pendiente: {cantidadPendiente} {unidad}` y etiquetas `Número de lote`, `Fecha de fabricación`, `Fecha de vencimiento`, `Cantidad recibida`, `Cantidad rechazada`, `Motivo del rechazo`, `Costo unitario`; vacío `La orden no tiene líneas pendientes de recibir.`; nota `Solo se registran las líneas con cantidad recibida. El costo unitario parte del precio de la orden.`; página con título `Recepción de la orden {numero}`, contexto `Compras / Órdenes / {numero}` (enlace al detalle), descripción `Registra la mercadería recibida con su lote y vencimiento.`, sección `Productos`, botones `Registrar recepción` y `Cancelar`, avisos `Ingresa la cantidad recibida de al menos un producto.` y `La orden no admite recepciones en su estado actual.`.

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/features/compras/components/RecepcionCabeceraForm.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CABECERA_RECEPCION_VACIA } from '../lib/recepcion-cabecera';
import { RecepcionCabeceraForm } from './RecepcionCabeceraForm';

const almacenes = [
  { id: 'alm-1', code: 'ALM001', name: 'Almacén Central', status: 'ACTIVE' as const },
  { id: 'alm-2', code: 'ALM002', name: 'Almacén Frío', status: 'ACTIVE' as const }
];

function renderForm(overrides: Partial<Parameters<typeof RecepcionCabeceraForm>[0]> = {}) {
  const onCambiar = vi.fn();
  render(
    <RecepcionCabeceraForm
      valores={CABECERA_RECEPCION_VACIA}
      errores={{}}
      almacenes={almacenes}
      onCambiar={onCambiar}
      {...overrides}
    />
  );
  return { onCambiar, user: userEvent.setup() };
}

describe('RecepcionCabeceraForm', () => {
  it('ofrece los almacenes recibidos y los tipos de documento con factura por defecto', () => {
    renderForm();

    expect(screen.getByRole('option', { name: 'Selecciona un almacén' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Almacén Central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Almacén Frío' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Factura' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Boleta de venta' })).toBeInTheDocument();
    expect(screen.getByLabelText('Tipo de documento')).toHaveValue('01');
  });

  it('notifica cada campo con su nombre y valor', async () => {
    const { onCambiar, user } = renderForm();

    await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-2');
    await user.selectOptions(screen.getByLabelText('Tipo de documento'), '03');
    await user.type(screen.getByLabelText('Serie del documento'), 'F');
    await user.type(screen.getByLabelText('Número del documento'), '1');
    await user.type(screen.getByLabelText('Guía de remisión del remitente'), 'T');
    await user.type(screen.getByLabelText('Guía de remisión del transportista'), 'V');
    await user.type(screen.getByLabelText('Temperatura (°C)'), '4');
    await user.type(screen.getByLabelText('Humedad relativa (%)'), '6');
    await user.type(screen.getByLabelText('Observación'), 'O');

    expect(onCambiar).toHaveBeenCalledWith('almacenId', 'alm-2');
    expect(onCambiar).toHaveBeenCalledWith('documentoProveedorTipo', '03');
    expect(onCambiar).toHaveBeenCalledWith('documentoProveedorSerie', 'F');
    expect(onCambiar).toHaveBeenCalledWith('documentoProveedorNumero', '1');
    expect(onCambiar).toHaveBeenCalledWith('guiaRemisionRemitente', 'T');
    expect(onCambiar).toHaveBeenCalledWith('guiaRemisionTransportista', 'V');
    expect(onCambiar).toHaveBeenCalledWith('temperatura', '4');
    expect(onCambiar).toHaveBeenCalledWith('humedad', '6');
    expect(onCambiar).toHaveBeenCalledWith('observacion', 'O');
  });

  it('muestra los errores recibidos bajo cada campo', () => {
    renderForm({
      errores: {
        almacenId: 'Selecciona el almacén de recepción.',
        temperatura: 'La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.'
      }
    });

    expect(screen.getByText('Selecciona el almacén de recepción.')).toBeInTheDocument();
    expect(
      screen.getByText('La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.')
    ).toBeInTheDocument();
  });
});
```

`SRC/features/compras/components/ItemsRecepcionEditor.test.tsx`:

```tsx
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ItemBorrador } from '../lib/recepcion-items';
import { ItemsRecepcionEditor } from './ItemsRecepcionEditor';

const HOY = '2026-10-08';
const ITEM: ItemBorrador = {
  numeroLineaOrden: 1,
  descripcion: 'Paracetamol 500 mg',
  unidadMedidaCodigo: 'UND',
  cantidadPendiente: 10,
  numeroLote: '',
  fechaFabricacion: '',
  fechaVencimiento: '',
  cantidadRecibida: '',
  cantidadRechazada: '0',
  motivoRechazo: '',
  costoUnitario: '5.5'
};

function renderEditor(items: ItemBorrador[] = [ITEM], mostrarErrores = false) {
  const onCambiar = vi.fn();
  render(
    <ItemsRecepcionEditor
      items={items}
      hoy={HOY}
      mostrarErrores={mostrarErrores}
      onCambiar={onCambiar}
    />
  );
  return { onCambiar, user: userEvent.setup() };
}

const bloque = () =>
  within(screen.getByRole('group', { name: 'Línea 1 — Paracetamol 500 mg' }));

describe('ItemsRecepcionEditor', () => {
  it('muestra un bloque por línea con lo pendiente y el costo de la orden', () => {
    renderEditor();

    expect(bloque().getByText('Pendiente: 10 UND')).toBeInTheDocument();
    expect(bloque().getByLabelText('Costo unitario')).toHaveValue('5.5');
    expect(bloque().getByLabelText('Cantidad rechazada')).toHaveValue('0');
    expect(
      screen.getByText(
        'Solo se registran las líneas con cantidad recibida. El costo unitario parte del precio de la orden.'
      )
    ).toBeInTheDocument();
  });

  it('notifica cada cambio con el número de línea y el campo editado', async () => {
    const { onCambiar, user } = renderEditor();

    await user.type(bloque().getByLabelText('Número de lote'), 'X');
    await user.type(bloque().getByLabelText('Fecha de fabricación'), '2026-01-01');
    await user.type(bloque().getByLabelText('Fecha de vencimiento'), '2099-12-31');
    await user.type(bloque().getByLabelText('Cantidad recibida'), '4');
    await user.type(bloque().getByLabelText('Cantidad rechazada'), '1');
    await user.type(bloque().getByLabelText('Motivo del rechazo'), 'M');
    await user.type(bloque().getByLabelText('Costo unitario'), '1');

    expect(onCambiar).toHaveBeenCalledWith(1, { numeroLote: 'X' });
    expect(onCambiar).toHaveBeenCalledWith(1, { fechaFabricacion: '2026-01-01' });
    expect(onCambiar).toHaveBeenCalledWith(1, { fechaVencimiento: '2099-12-31' });
    expect(onCambiar).toHaveBeenCalledWith(1, { cantidadRecibida: '4' });
    expect(onCambiar).toHaveBeenCalledWith(1, { cantidadRechazada: '01' });
    expect(onCambiar).toHaveBeenCalledWith(1, { motivoRechazo: 'M' });
    expect(onCambiar).toHaveBeenCalledWith(1, { costoUnitario: '5.51' });
  });

  it('muestra el error de una línea con cantidad cuando se piden los errores', () => {
    renderEditor([{ ...ITEM, cantidadRecibida: '4' }], true);

    expect(bloque().getByRole('alert')).toHaveTextContent(
      'El número de lote es obligatorio y admite hasta 120 caracteres.'
    );
  });

  it('no valida las líneas sin cantidad recibida', () => {
    renderEditor([ITEM], true);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('no muestra errores antes de intentar registrar', () => {
    renderEditor([{ ...ITEM, cantidadRecibida: '4' }]);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('avisa cuando no quedan líneas pendientes', () => {
    renderEditor([]);

    expect(screen.getByText('La orden no tiene líneas pendientes de recibir.')).toBeInTheDocument();
  });
});
```

`SRC/features/compras/pages/RecepcionPage.test.tsx`:

```tsx
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { sampleOrden, sampleRecepcion } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import type { EstadoOrden } from '../api/ordenes.types';
import { RecepcionPage } from './RecepcionPage';

const recepcionesUrl = '*/api/v1/compras/recepciones';
const ordenUrl = '*/api/v1/compras/ordenes/orden-1';

function mockEntorno(estado: EstadoOrden = 'EMITIDA') {
  server.use(
    http.get(ordenUrl, () => HttpResponse.json({ ...sampleOrden, estado })),
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura))
  );
}

function renderPage() {
  return renderRoute(
    '/compras/ordenes/:ordenId/*',
    RecepcionPage,
    '/compras/ordenes/orden-1/recepcion'
  );
}

type Usuario = ReturnType<typeof renderPage>['user'];

const linea1 = () =>
  within(screen.getByRole('group', { name: 'Línea 1 — Paracetamol 500 mg' }));

const registrar = () => screen.getByRole('button', { name: 'Registrar recepción' });

async function completarRecepcion(user: Usuario) {
  await screen.findByRole('option', { name: 'Almacén Central' });
  await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-1');
  await user.type(screen.getByLabelText('Serie del documento'), 'F001');
  await user.type(screen.getByLabelText('Número del documento'), '123');
  await user.type(linea1().getByLabelText('Número de lote'), 'L2026-01');
  await user.type(linea1().getByLabelText('Fecha de vencimiento'), '2099-12-31');
  await user.type(linea1().getByLabelText('Cantidad recibida'), '4');
}

describe('RecepcionPage', () => {
  it('muestra las líneas pendientes con el costo de la orden y solo los almacenes del destino', async () => {
    mockEntorno();

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Recepción de la orden OC-2026-000001' })
    ).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'Almacén Central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Almacén Frío' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Almacén Norte' })).not.toBeInTheDocument();
    expect(linea1().getByText('Pendiente: 10 UND')).toBeInTheDocument();
    expect(linea1().getByLabelText('Costo unitario')).toHaveValue('5.5');
  });

  it('registra una recepción parcial con Idempotency-Key, invalida compras e inventario y vuelve al detalle', async () => {
    mockEntorno();
    let body: unknown;
    let clave: string | null = null;
    server.use(
      http.post(recepcionesUrl, async ({ request }) => {
        clave = request.headers.get('Idempotency-Key');
        body = await request.json();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user, router, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await completarRecepcion(user);
    await user.click(registrar());

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    expect(body).toEqual({
      ordenCompraId: 'orden-1',
      almacenId: 'alm-1',
      documentoProveedorTipo: '01',
      documentoProveedorSerie: 'F001',
      documentoProveedorNumero: '123',
      items: [
        {
          numeroLineaOrden: 1,
          numeroLote: 'L2026-01',
          fechaVencimiento: '2099-12-31',
          cantidadRecibida: 4,
          cantidadRechazada: 0,
          costoUnitario: 5.5
        }
      ]
    });
    expect(clave).toEqual(expect.any(String));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('exige el motivo cuando hay cantidad rechazada y luego lo envía', async () => {
    mockEntorno();
    const enviado = vi.fn();
    let body: unknown;
    server.use(
      http.post(recepcionesUrl, async ({ request }) => {
        enviado();
        body = await request.json();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user, router } = renderPage();
    await completarRecepcion(user);
    await user.clear(linea1().getByLabelText('Cantidad rechazada'));
    await user.type(linea1().getByLabelText('Cantidad rechazada'), '1');

    await user.click(registrar());

    expect(await linea1().findByRole('alert')).toHaveTextContent('Indica el motivo del rechazo.');
    expect(enviado).not.toHaveBeenCalled();

    await user.type(linea1().getByLabelText('Motivo del rechazo'), 'Empaque dañado');
    await user.click(registrar());

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    expect(body).toMatchObject({
      items: [{ cantidadRecibida: 4, cantidadRechazada: 1, motivoRechazo: 'Empaque dañado' }]
    });
  });

  it('no envía sin almacén ni cantidades y muestra qué falta', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(recepcionesUrl, () => {
        enviado();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Almacén Central' });

    await user.click(registrar());

    expect(await screen.findByText('Selecciona el almacén de recepción.')).toBeInTheDocument();
    expect(
      screen.getByText('Ingresa la cantidad recibida de al menos un producto.')
    ).toBeInTheDocument();
    expect(enviado).not.toHaveBeenCalled();
  });

  it('reutiliza la clave al reintentar y usa otra después de registrar con éxito', async () => {
    mockEntorno();
    const claves: (string | null)[] = [];
    server.use(
      http.post(recepcionesUrl, ({ request }) => {
        claves.push(request.headers.get('Idempotency-Key'));
        return claves.length === 1
          ? HttpResponse.json({ title: 'Error' }, { status: 500 })
          : HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user, router } = renderPage();
    await completarRecepcion(user);

    await user.click(registrar());
    await screen.findByRole('alert');
    await user.click(registrar());
    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    await user.click(registrar());
    await waitFor(() => expect(claves).toHaveLength(3));

    expect(claves[0]).toBe(claves[1]);
    expect(claves[2]).not.toBe(claves[1]);
  });

  it('muestra el error traducido del backend y no navega', async () => {
    mockEntorno();
    server.use(
      http.post(recepcionesUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_RECEPCION_EXCEDE_PENDIENTE', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user, router } = renderPage();
    await completarRecepcion(user);

    await user.click(registrar());

    expect(
      await screen.findByText('La cantidad recibida excede lo pendiente de la orden más su tolerancia.')
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1/recepcion');
  });

  it('avisa cuando la orden no admite recepciones', async () => {
    mockEntorno('BORRADOR');

    renderPage();

    expect(
      await screen.findByText('La orden no admite recepciones en su estado actual.')
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Registrar recepción' })).not.toBeInTheDocument();
  });

  it('muestra la carga y el error de la orden', async () => {
    server.use(
      http.get(ordenUrl, () => HttpResponse.json({ title: 'No encontrado' }, { status: 404 }))
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('cancela y vuelve al detalle de la orden', async () => {
    mockEntorno();
    const { user, router } = renderPage();
    await screen.findByRole('heading', { name: 'Recepción de la orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1');
  });
});
```

Nota del arnés: la ruta de prueba `/compras/ordenes/:ordenId/*` también coincide con `/compras/ordenes/orden-1`, así que tras registrar la página sigue montada; eso permite comprobar en el test de claves que `reiniciar()` descarta la clave usada (en la app real la navegación lleva a `OrdenDetailPage`).

En `SRC/features/compras/routes.test.ts` la lista esperada termina en:

```ts
      'compras/ordenes/nueva',
      'compras/ordenes/:ordenId',
      'compras/ordenes/:ordenId/recepcion'
```

En `SRC/app/feature-routes.test.ts` agregar `'compras/ordenes/:ordenId/recepcion',` después de `'compras/ordenes/:ordenId',`.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/components/RecepcionCabeceraForm.test.tsx apps/erp-web/src/features/compras/components/ItemsRecepcionEditor.test.tsx apps/erp-web/src/features/compras/pages/RecepcionPage.test.tsx apps/erp-web/src/features/compras/routes.test.ts apps/erp-web/src/app`
Expected: FAIL (componentes, página y ruta inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/components/RecepcionCabeceraForm.tsx`:

```tsx
import { Card } from '@boticas/ui-web';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import type { OrganizationalNode } from '../../organizacion';
import {
  TIPOS_DOCUMENTO_PROVEEDOR,
  type CabeceraRecepcion,
  type ErroresCabeceraRecepcion
} from '../lib/recepcion-cabecera';

type RecepcionCabeceraFormProps = {
  valores: CabeceraRecepcion;
  errores: ErroresCabeceraRecepcion;
  almacenes: OrganizationalNode[];
  onCambiar: (campo: keyof CabeceraRecepcion, valor: string) => void;
};

type CampoTexto = Exclude<keyof CabeceraRecepcion, 'almacenId' | 'documentoProveedorTipo'>;

const CAMPOS_TEXTO: readonly { campo: CampoTexto; label: string; inputMode: 'text' | 'decimal' }[] = [
  { campo: 'documentoProveedorSerie', label: 'Serie del documento', inputMode: 'text' },
  { campo: 'documentoProveedorNumero', label: 'Número del documento', inputMode: 'text' },
  { campo: 'guiaRemisionRemitente', label: 'Guía de remisión del remitente', inputMode: 'text' },
  {
    campo: 'guiaRemisionTransportista',
    label: 'Guía de remisión del transportista',
    inputMode: 'text'
  },
  { campo: 'temperatura', label: 'Temperatura (°C)', inputMode: 'decimal' },
  { campo: 'humedad', label: 'Humedad relativa (%)', inputMode: 'decimal' },
  { campo: 'observacion', label: 'Observación', inputMode: 'text' }
];

export function RecepcionCabeceraForm({
  valores,
  errores,
  almacenes,
  onCambiar
}: RecepcionCabeceraFormProps) {
  return (
    <Card className="grid gap-4 p-6 sm:grid-cols-2">
      <SelectField
        id="recepcion-almacen"
        label="Almacén"
        error={errores.almacenId}
        value={valores.almacenId}
        onChange={(event) => onCambiar('almacenId', event.target.value)}
      >
        <option value="">Selecciona un almacén</option>
        {almacenes.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="recepcion-tipo-documento"
        label="Tipo de documento"
        value={valores.documentoProveedorTipo}
        onChange={(event) => onCambiar('documentoProveedorTipo', event.target.value)}
      >
        {TIPOS_DOCUMENTO_PROVEEDOR.map(({ codigo, nombre }) => (
          <option key={codigo} value={codigo}>
            {nombre}
          </option>
        ))}
      </SelectField>
      {CAMPOS_TEXTO.map(({ campo, label, inputMode }) => (
        <TextField
          key={campo}
          id={`recepcion-${campo}`}
          label={label}
          inputMode={inputMode}
          error={errores[campo]}
          value={valores[campo]}
          onChange={(event) => onCambiar(campo, event.target.value)}
        />
      ))}
    </Card>
  );
}
```

`SRC/features/compras/components/ItemsRecepcionEditor.tsx`:

```tsx
import { Card } from '@boticas/ui-web';
import { TextField } from '../../../shared/components/FormFields';
import {
  errorItem,
  itemIncluido,
  type CambiosItem,
  type ItemBorrador
} from '../lib/recepcion-items';

type ItemsRecepcionEditorProps = {
  items: ItemBorrador[];
  hoy: string;
  mostrarErrores: boolean;
  onCambiar: (numeroLineaOrden: number, cambios: CambiosItem) => void;
};

const CAMPOS: readonly {
  campo: keyof CambiosItem;
  label: string;
  type: 'text' | 'date';
  inputMode: 'text' | 'decimal';
}[] = [
  { campo: 'numeroLote', label: 'Número de lote', type: 'text', inputMode: 'text' },
  { campo: 'fechaFabricacion', label: 'Fecha de fabricación', type: 'date', inputMode: 'text' },
  { campo: 'fechaVencimiento', label: 'Fecha de vencimiento', type: 'date', inputMode: 'text' },
  { campo: 'cantidadRecibida', label: 'Cantidad recibida', type: 'text', inputMode: 'decimal' },
  { campo: 'cantidadRechazada', label: 'Cantidad rechazada', type: 'text', inputMode: 'decimal' },
  { campo: 'motivoRechazo', label: 'Motivo del rechazo', type: 'text', inputMode: 'text' },
  { campo: 'costoUnitario', label: 'Costo unitario', type: 'text', inputMode: 'decimal' }
];

export function ItemsRecepcionEditor({
  items,
  hoy,
  mostrarErrores,
  onCambiar
}: ItemsRecepcionEditorProps) {
  if (items.length === 0) {
    return (
      <p className="text-sm text-neutral-500 dark:text-neutral-400">
        La orden no tiene líneas pendientes de recibir.
      </p>
    );
  }

  return (
    <div className="space-y-4">
      {items.map((item) => {
        const mensaje = mostrarErrores && itemIncluido(item) ? errorItem(item, hoy) : null;
        return (
          <Card key={item.numeroLineaOrden} className="p-5">
            <fieldset className="min-w-0 space-y-4">
              <legend className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                {`Línea ${item.numeroLineaOrden} — ${item.descripcion}`}
              </legend>
              <p className="text-xs text-neutral-500 dark:text-neutral-400">
                {`Pendiente: ${item.cantidadPendiente} ${item.unidadMedidaCodigo}`}
              </p>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {CAMPOS.map(({ campo, label, type, inputMode }) => (
                  <TextField
                    key={campo}
                    id={`recepcion-linea-${item.numeroLineaOrden}-${campo}`}
                    label={label}
                    type={type}
                    inputMode={inputMode}
                    value={item[campo]}
                    onChange={(event) =>
                      onCambiar(item.numeroLineaOrden, { [campo]: event.target.value })
                    }
                  />
                ))}
              </div>
              {mensaje ? (
                <p role="alert" className="text-danger-600 dark:text-danger-400 text-xs font-medium">
                  {mensaje}
                </p>
              ) : null}
            </fieldset>
          </Card>
        );
      })}
      <p role="note" className="text-xs text-neutral-500 dark:text-neutral-400">
        Solo se registran las líneas con cantidad recibida. El costo unitario parte del precio de la
        orden.
      </p>
    </div>
  );
}
```

(el texto de la nota debe renderizar en una sola línea lógica: si Prettier lo parte, JSX une con un espacio y el test con `getByText` sigue pasando; si no, usar una plantilla `{'...'}`).

`SRC/features/compras/pages/RecepcionPage.tsx`:

```tsx
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { Button, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { useClaveIdempotencia } from '../../../shared/lib/use-clave-idempotencia';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { invalidateInventario } from '../../inventario';
import { useEstablecimientos } from '../../organizacion';
import { ordenQuery } from '../api/ordenes.api';
import type { Orden } from '../api/ordenes.types';
import { registrarRecepcion } from '../api/recepciones.api';
import type { RegistrarRecepcionPayload } from '../api/recepciones.types';
import { ItemsRecepcionEditor } from '../components/ItemsRecepcionEditor';
import { RecepcionCabeceraForm } from '../components/RecepcionCabeceraForm';
import { describeErrorCompras } from '../lib/errores-compras';
import { puedeRecibir } from '../lib/estado-orden';
import { fechaLocalISO } from '../lib/orden-cabecera';
import {
  CABECERA_RECEPCION_VACIA,
  almacenesDeDestino,
  erroresCabeceraRecepcion,
  puedeRegistrar,
  toRegistrarRecepcionPayload,
  type CabeceraRecepcion
} from '../lib/recepcion-cabecera';
import {
  actualizarItem,
  itemIncluido,
  itemsDesdeOrden,
  type ItemBorrador
} from '../lib/recepcion-items';
import { useMutacionCompras } from '../lib/use-mutacion-compras';

function RecepcionForm({ orden }: { orden: Orden }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const establecimientos = useEstablecimientos();
  const { claveDe, reiniciar } = useClaveIdempotencia();
  const [cabecera, setCabecera] = useState<CabeceraRecepcion>(CABECERA_RECEPCION_VACIA);
  const [items, setItems] = useState<ItemBorrador[]>(() => itemsDesdeOrden(orden));
  const [intentado, setIntentado] = useState(false);
  const hoy = fechaLocalISO();
  const detalle = `/compras/ordenes/${orden.id}`;
  const registro = useMutacionCompras(
    (payload: RegistrarRecepcionPayload) =>
      registrarRecepcion(apiClient, payload, claveDe(payload)),
    () => {
      reiniciar();
      void invalidateInventario(queryClient);
      void navigate(detalle);
    }
  );

  const registrar = () => {
    setIntentado(true);
    if (puedeRegistrar(cabecera, items, hoy)) {
      registro.mutate(toRegistrarRecepcionPayload(orden.id, cabecera, items));
    }
  };

  return (
    <>
      <RecepcionCabeceraForm
        valores={cabecera}
        errores={intentado ? erroresCabeceraRecepcion(cabecera) : {}}
        almacenes={almacenesDeDestino(establecimientos, orden.establecimientoDestinoId)}
        onCambiar={(campo, valor) => setCabecera((actual) => ({ ...actual, [campo]: valor }))}
      />
      <section className="space-y-4">
        <h2 className="text-base font-semibold">Productos</h2>
        <ItemsRecepcionEditor
          items={items}
          hoy={hoy}
          mostrarErrores={intentado}
          onCambiar={(numeroLineaOrden, cambios) =>
            setItems((actuales) => actualizarItem(actuales, numeroLineaOrden, cambios))
          }
        />
        {intentado && !items.some(itemIncluido) ? (
          <FormError message="Ingresa la cantidad recibida de al menos un producto." />
        ) : null}
      </section>
      {registro.mensajeError ? <FormError message={registro.mensajeError} /> : null}
      <div className="flex flex-wrap gap-3">
        <Button disabled={registro.isPending} onClick={registrar}>
          Registrar recepción
        </Button>
        <Button variant="secondary" onClick={() => void navigate(detalle)}>
          Cancelar
        </Button>
      </div>
    </>
  );
}

export function RecepcionPage() {
  const ordenId = useRouteParam('ordenId');
  const result = useQuery(ordenQuery(ordenId));

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeErrorCompras(result.error)} />;

  const orden = result.data;

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <PageHeader
        title={`Recepción de la orden ${orden.numero}`}
        context={
          <Link to={`/compras/ordenes/${orden.id}`}>{`Compras / Órdenes / ${orden.numero}`}</Link>
        }
        description="Registra la mercadería recibida con su lote y vencimiento."
      />
      {puedeRecibir(orden.estado) ? (
        <RecepcionForm orden={orden} />
      ) : (
        <FormError message="La orden no admite recepciones en su estado actual." />
      )}
    </div>
  );
}
```

`SRC/features/compras/routes.tsx`: agregar al final del arreglo, después de `compras/ordenes/:ordenId`:

```tsx
  {
    path: 'compras/ordenes/:ordenId/recepcion',
    lazy: async () => {
      const { RecepcionPage } = await import('./pages/RecepcionPage');
      return { Component: RecepcionPage };
    }
  }
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras apps/erp-web/src/app && pnpm typecheck && pnpm lint`
Expected: PASS con 100% en `RecepcionCabeceraForm.tsx`, `ItemsRecepcionEditor.tsx` y `RecepcionPage.tsx` (confirmar con `pnpm check`). Ajustes permitidos sin reducir lo verificado: en los tests de componentes con props fijas, `user.type` emite el carácter agregado al valor inicial (`'01'`, `'5.51'`); si la librería normaliza distinto, ajustar solo esos valores esperados. Si el tipo inferido de `{ [campo]: event.target.value }` no es asignable a `CambiosItem`, tiparlo como `({ [campo]: event.target.value }) as CambiosItem` y anotarlo.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/compras/components/RecepcionCabeceraForm.tsx frontend/apps/erp-web/src/features/compras/components/RecepcionCabeceraForm.test.tsx frontend/apps/erp-web/src/features/compras/components/ItemsRecepcionEditor.tsx frontend/apps/erp-web/src/features/compras/components/ItemsRecepcionEditor.test.tsx frontend/apps/erp-web/src/features/compras/pages/RecepcionPage.tsx frontend/apps/erp-web/src/features/compras/pages/RecepcionPage.test.tsx frontend/apps/erp-web/src/features/compras/routes.tsx frontend/apps/erp-web/src/features/compras/routes.test.ts frontend/apps/erp-web/src/app/feature-routes.test.ts
git commit -m "feat(compras): registrar recepcion de una orden con lote, vencimiento e idempotencia

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Recepciones en el detalle de la orden y e2e

**Files:**
- Create: `SRC/features/compras/components/RecepcionesOrden.tsx` y `.test.tsx`
- Modify: `SRC/features/compras/pages/OrdenDetailPage.tsx`, `SRC/features/compras/pages/OrdenDetailPage.test.tsx`
- Modify: `E2E/support/compras-api.ts`, `E2E/compras.spec.ts`

**Interfaces:**
- Consumes: `recepcionesQuery`, `Recepcion`, `LineaRecepcion`, `puedeRecibir`, `sampleRecepcion` (Task 1), ruta `compras/ordenes/:ordenId/recepcion` (Task 2), `formatoFecha` (`lib/formato-compras`), `formatoFechaHora` (`shared/lib/format`).
- Produces: `RecepcionesOrden({ ordenId })`; en `OrdenDetailPage` el enlace `Registrar recepción` y la sección `Recepciones`.
- Textos exactos: enlace `Registrar recepción` a `/compras/ordenes/{id}/recepcion` (solo `EMITIDA` o `PARCIALMENTE_RECIBIDA`); sección `Recepciones`; tabla con columnas `Recepción`, `Fecha`, `Línea`, `Lote`, `Vencimiento`, `Recibido`, `Aceptado`, `Rechazado`; vacío `La orden aún no tiene recepciones.`; error `No se pudo cargar las recepciones de la orden.`.

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/features/compras/components/RecepcionesOrden.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleRecepcion } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { formatoFechaHora } from '../../../shared/lib/format';
import { formatoFecha } from '../lib/formato-compras';
import { RecepcionesOrden } from './RecepcionesOrden';

const recepcionesUrl = '*/api/v1/compras/recepciones';
const texto = (valor: string) => valor.replace(/\s/g, ' ');

function renderTabla() {
  const Pantalla = () => <RecepcionesOrden ordenId="orden-1" />;
  return renderRoute('/x', Pantalla, '/x');
}

describe('RecepcionesOrden', () => {
  it('consulta las recepciones de la orden y muestra una fila por línea recibida', async () => {
    let parametros: URLSearchParams | undefined;
    server.use(
      http.get(recepcionesUrl, ({ request }) => {
        parametros = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sampleRecepcion]));
      })
    );

    renderTabla();

    const fila = (await screen.findByText('REC-2026-000001')).closest('tr');
    expect(fila).toHaveTextContent(texto(formatoFechaHora('2026-10-08T15:00:00Z')));
    expect(fila).toHaveTextContent('L2026-01');
    expect(fila).toHaveTextContent(texto(formatoFecha('2028-12-31')));
    expect(parametros?.get('ordenCompraId')).toBe('orden-1');
    expect(parametros?.get('size')).toBe('100');
  });

  it('indica cuando la orden aún no tiene recepciones', async () => {
    server.use(http.get(recepcionesUrl, () => HttpResponse.json(pagina([]))));

    renderTabla();

    expect(await screen.findByText('La orden aún no tiene recepciones.')).toBeInTheDocument();
  });

  it('indica el error de consulta', async () => {
    server.use(
      http.get(recepcionesUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 }))
    );

    renderTabla();

    expect(
      await screen.findByText('No se pudo cargar las recepciones de la orden.')
    ).toBeInTheDocument();
  });
});
```

En `SRC/features/compras/pages/OrdenDetailPage.test.tsx`:
- Cambiar el import de fixtures a `import { sampleOrden, sampleProveedor, sampleRecepcion } from '../../../test/compras-fixtures';` y agregar `import { pagina } from '../../../test/organizacion-fixtures';`.
- En `mockOrden`, agregar como cuarto handler del `server.use`:

```tsx
    http.get('*/api/v1/compras/recepciones', () => HttpResponse.json(pagina([])))
```

- Agregar dentro del `describe`:

```tsx
  it.each(['EMITIDA', 'PARCIALMENTE_RECIBIDA'] as const)(
    'en estado %s ofrece registrar una recepción',
    async (estado) => {
      mockOrden(estado);

      renderPage();

      expect(await screen.findByRole('link', { name: 'Registrar recepción' })).toHaveAttribute(
        'href',
        '/compras/ordenes/orden-1/recepcion'
      );
    }
  );

  it.each(['BORRADOR', 'APROBADA', 'RECIBIDA', 'CANCELADA'] as const)(
    'en estado %s no ofrece registrar recepciones',
    async (estado) => {
      mockOrden(estado);

      renderPage();
      await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

      expect(screen.queryByRole('link', { name: 'Registrar recepción' })).not.toBeInTheDocument();
    }
  );

  it('lista las recepciones de la orden con su lote', async () => {
    mockOrden('PARCIALMENTE_RECIBIDA');
    server.use(
      http.get('*/api/v1/compras/recepciones', () => HttpResponse.json(pagina([sampleRecepcion])))
    );

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Recepciones' })).toBeInTheDocument();
    expect(await screen.findByText('REC-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('L2026-01')).toBeInTheDocument();
  });
```

En `E2E/support/compras-api.ts`:
- Cambiar el import de fixtures a:

```ts
import {
  sampleOrden,
  sampleOrdenResumen,
  sampleProveedor,
  sampleRecepcion
} from '../../apps/erp-web/src/test/compras-fixtures';
```

- Agregar `import type { Recepcion } from '../../apps/erp-web/src/features/compras/api/recepciones.types';`.
- Dentro de `mockComprasApi`, después de `let orden: Orden = sampleOrden;`, agregar `let recepciones: Recepcion[] = [];` y, antes del cierre de la función, la ruta:

```ts
  await page.route(/\/api\/v1\/compras\/recepciones(\?|$)/, (route) => {
    const request = route.request();
    if (request.method() !== 'POST') return json(route, 200, pagina(recepciones));
    if (!request.headers()['idempotency-key']) {
      return json(route, 400, { title: 'Falta Idempotency-Key' });
    }
    recepciones = [sampleRecepcion];
    orden = {
      ...orden,
      estado: 'PARCIALMENTE_RECIBIDA',
      lineas: orden.lineas.map((linea) => ({ ...linea, cantidadRecibida: 4, cantidadPendiente: 6 }))
    };
    return json(route, 201, sampleRecepcion);
  });
```

Agregar a `E2E/compras.spec.ts` dentro del `test.describe`:

```ts
  test('aprueba, emite y registra una recepción parcial', async ({ page }) => {
    await abrirComprasEn(page, '/compras/ordenes/orden-1');

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
    await page.getByRole('button', { name: 'Aprobar', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Emitir', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Emitir', exact: true }).click();
    await expect(page.getByText('La orden aún no tiene recepciones.')).toBeVisible();

    await page.getByRole('link', { name: 'Registrar recepción' }).click();
    await expect(
      page.getByRole('heading', { name: 'Recepción de la orden OC-2026-000001' })
    ).toBeVisible();
    await page.getByLabel('Almacén', { exact: true }).selectOption('alm-1');
    await page.getByLabel('Serie del documento').fill('F001');
    await page.getByLabel('Número del documento').fill('123');
    const linea = page.getByRole('group', { name: 'Línea 1 — Paracetamol 500 mg' });
    await expect(linea.getByText('Pendiente: 10 UND')).toBeVisible();
    await expect(linea.getByLabel('Costo unitario')).toHaveValue('5.5');
    await linea.getByLabel('Número de lote').fill('L2026-01');
    await linea.getByLabel('Fecha de vencimiento').fill('2099-12-31');
    await linea.getByLabel('Cantidad recibida').fill('4');
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Registrar recepción' }).click();

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
    await expect(page.getByText('Parcialmente recibida').first()).toBeVisible();
    await expect(page.getByText('REC-2026-000001')).toBeVisible();
    await expect(page.getByText('L2026-01')).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/components/RecepcionesOrden.test.tsx apps/erp-web/src/features/compras/pages/OrdenDetailPage.test.tsx`
Expected: FAIL (`RecepcionesOrden` inexistente; sin enlace ni sección de recepciones en el detalle).

- [ ] **Step 3: Implementar**

`SRC/features/compras/components/RecepcionesOrden.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { DataTable } from '@boticas/ui-web';
import { formatoFechaHora } from '../../../shared/lib/format';
import { recepcionesQuery } from '../api/recepciones.api';
import type { LineaRecepcion, Recepcion } from '../api/recepciones.types';
import { formatoFecha } from '../lib/formato-compras';

type FilaRecepcion = { recepcion: Recepcion; linea: LineaRecepcion };

export function RecepcionesOrden({ ordenId }: { ordenId: string }) {
  const { data, isPending, isError } = useQuery(
    recepcionesQuery({ ordenCompraId: ordenId, size: 100 })
  );
  const filas = (data?.items ?? []).flatMap((recepcion) =>
    recepcion.lineas.map((linea) => ({ recepcion, linea }))
  );

  return (
    <DataTable<FilaRecepcion>
      columns={[
        { header: 'Recepción', cell: ({ recepcion }) => recepcion.numero },
        { header: 'Fecha', cell: ({ recepcion }) => formatoFechaHora(recepcion.fechaRecepcion) },
        { header: 'Línea', cell: ({ linea }) => linea.numeroLineaOrden },
        { header: 'Lote', cell: ({ linea }) => linea.numeroLote },
        { header: 'Vencimiento', cell: ({ linea }) => formatoFecha(linea.fechaVencimiento) },
        { header: 'Recibido', cell: ({ linea }) => linea.cantidadRecibida },
        { header: 'Aceptado', cell: ({ linea }) => linea.cantidadAceptada },
        { header: 'Rechazado', cell: ({ linea }) => linea.cantidadRechazada }
      ]}
      rows={filas}
      rowKey={({ linea }) => linea.id}
      emptyMessage="La orden aún no tiene recepciones."
      isLoading={isPending}
      isError={isError}
      errorMessage="No se pudo cargar las recepciones de la orden."
    />
  );
}
```

`SRC/features/compras/pages/OrdenDetailPage.tsx`:
- Cambiar `import { Button, Card, PageHeader } from '@boticas/ui-web';` por `import { Button, Card, PageHeader, buttonClassName } from '@boticas/ui-web';`.
- Agregar `import { RecepcionesOrden } from '../components/RecepcionesOrden';` y cambiar el import de estados a `import { puedeAnular, puedeAprobar, puedeEmitir, puedeRecibir } from '../lib/estado-orden';`.
- En `actions`, después del botón `Anular`, agregar:

```tsx
            {puedeRecibir(orden.estado) ? (
              <Link
                to={`/compras/ordenes/${orden.id}/recepcion`}
                className={buttonClassName()}
              >
                Registrar recepción
              </Link>
            ) : null}
```

- Después de la `Card` de `Totales` y antes del bloque `{anulando ? ...}`, agregar:

```tsx
      <section className="mt-6">
        <h2 className="mb-3 text-base font-semibold">Recepciones</h2>
        <RecepcionesOrden ordenId={orden.id} />
      </section>
```

- [ ] **Step 4: Ejecutar y verificar**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras apps/erp-web/src/app apps/erp-web/src/shared` → PASS. Run: `pnpm format && pnpm e2e compras.spec.ts` → PASS en desktop, tablet y móvil (incluidos los tres tests anteriores, que ahora también consultan recepciones desde el detalle). Run: `pnpm check` → verde (umbral 100% por archivo nuevo). Ajuste permitido: si `toHaveTextContent` no encuentra la fecha por espacios especiales, conservar `texto(...)` y comparar contra `fila?.textContent?.replace(/\s/g, ' ')` con `toContain`.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/compras/components/RecepcionesOrden.tsx frontend/apps/erp-web/src/features/compras/components/RecepcionesOrden.test.tsx frontend/apps/erp-web/src/features/compras/pages/OrdenDetailPage.tsx frontend/apps/erp-web/src/features/compras/pages/OrdenDetailPage.test.tsx frontend/e2e/support/compras-api.ts frontend/e2e/compras.spec.ts
git commit -m "feat(compras): recepciones en el detalle de la orden y e2e de recepcion parcial

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Verificación manual contra el backend real (la ejecuta el usuario, no el agente)

Cierra el frontend de compras según la sección Pruebas del spec. Requiere PostgreSQL con las migraciones aplicadas y un usuario con `compras.*` e `inventario.*` (rol `ADMIN` de FARMALAB).

- [ ] Levantar el backend: en `service-botica/`, con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` configurados, `.\gradlew.bat :bootstrap-app:bootRun`.
- [ ] Levantar el frontend en modo `http`: en `frontend/`, `pnpm dev` y abrir `http://localhost:3000` (Vite redirige `/api` a `http://localhost:8080`).
- [ ] Crear un proveedor en `/compras/proveedores/nuevo` y verificar que queda `ACTIVO`.
- [ ] Crear una orden en `/compras/ordenes/nueva` con ese proveedor, un establecimiento destino que tenga un almacén activo con control de lotes y al menos un producto con cantidad 10.
- [ ] En el detalle, `Aprobar` y luego `Emitir`; confirmar que aparece `Registrar recepción` y que la sección `Recepciones` dice `La orden aún no tiene recepciones.`.
- [ ] `Registrar recepción`: elegir el almacén, factura con serie y número, lote, vencimiento futuro y cantidad recibida 4. Registrar y confirmar el regreso al detalle con estado `Parcialmente recibida`, la línea con recibido 4 y pendiente 6, y la fila `REC-...` con el lote en `Recepciones`.
- [ ] En `/inventario`, filtrar por el establecimiento, el almacén y el SKU: la posición del lote nuevo muestra 4 unidades disponibles.
- [ ] Registrar una segunda recepción por las 6 restantes, con 1 rechazada y motivo: la orden pasa a `Recibida`, el inventario suma 5 más y `Recepciones` lista ambas.
- [ ] Casos de error: intentar recibir más de lo pendiente más la tolerancia (`La cantidad recibida excede lo pendiente de la orden más su tolerancia.`); en la herramienta de red del navegador, reenviar el mismo POST con la misma `Idempotency-Key` y comprobar que no duplica stock.
- [ ] Anotar cualquier diferencia (mensajes, estados, almacenes ofrecidos) para un ajuste posterior.

---

## Self-Review

**Cobertura del spec (sección Recepción y relacionadas):** página por orden recepcionable en `/compras/ordenes/:ordenId/recepcion` con ruta lazy y tests de rutas (Task 2); cabecera con almacén de los almacenes del establecimiento destino, documento del proveedor (tipo, serie, número), guías de remisión, temperatura, humedad y observación (Task 1 lógica, Task 2 UI); un bloque por línea con pendiente con lote, fabricación opcional, vencimiento, recibida, rechazada con motivo y costo por defecto del precio de la orden (Task 1 y 2); solo se envían líneas con cantidad recibida (Task 1 `toItemsPayload`); validación espejo (Task 1 `errorItem` y `erroresCabeceraRecepcion`); `Idempotency-Key` estable por contenido con `useClaveIdempotencia` y `reiniciar()` tras el éxito (Task 2, test de claves); invalidación de órdenes y recepciones (`invalidateCompras` vía `useMutacionCompras`, misma raíz `['compras']`) e inventario (`invalidateInventario` por `index.ts`) y regreso al detalle (Task 2); detalle con `Registrar recepción` solo en `EMITIDA`/`PARCIALMENTE_RECIBIDA` y recepciones de la orden por `ordenCompraId` (Task 3); errores `COM_RECEPCION_*` traducidos reutilizando `errores-compras.ts` (Task 2, test de `COM_RECEPCION_EXCEDE_PENDIENTE`); cargando, error y vacío en la página y en la tabla (Task 2 y 3); e2e simulado desktop/tablet/móvil con recepción parcial y sin desborde horizontal (Task 3); verificación manual (sección final).

**Ajustes de revisión:** `COM_RECEPCION_INVALIDA` no se traduce para conservar el detalle específico del backend; la validación de vencido solo aplica si hay cantidad aceptada (permite rechazar por completo un lote vencido); el vencimiento se exige posterior a la fabricación como dice el spec, aunque el backend acepta igualdad; las validaciones numéricas se extraen a `numeros-compras.ts` y `numeroOpcional` a `shared/lib/form-values.ts` para no duplicar lo de la parte 4; `RecepcionForm` se monta solo con la orden cargada para inicializar los ítems con `useState` sin efectos.

**Escaneo de placeholders:** todo paso de código incluye el código; los ajustes permitidos están acotados (valores de `user.type` con props fijas, tipado del objeto computado, normalización de espacios en fechas). No hay "TBD".

**Consistencia de tipos:** `ItemBorrador`, `CambiosItem`, `itemsDesdeOrden`, `actualizarItem`, `itemIncluido`, `errorItem(item, hoy)`, `toItemsPayload`, `CabeceraRecepcion`, `ErroresCabeceraRecepcion`, `TIPOS_DOCUMENTO_PROVEEDOR`, `CABECERA_RECEPCION_VACIA`, `almacenesDeDestino`, `erroresCabeceraRecepcion`, `puedeRegistrar(cabecera, items, hoy)`, `toRegistrarRecepcionPayload(ordenCompraId, cabecera, items)`, `Recepcion`, `LineaRecepcion`, `RegistrarRecepcionPayload`, `ItemRecepcionPayload`, `recepcionesQuery({ ordenCompraId, page?, size? })`, `registrarRecepcion(client, payload, idempotencyKey)`, `puedeRecibir`, `numeroOpcional` y `sampleRecepcion` se usan con las mismas firmas en las tres tareas, el e2e y la verificación manual.
