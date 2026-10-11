# Frontend, parte 4: punto de venta (POS) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el placeholder de `/pos` por la pantalla de venta en efectivo: puesto de trabajo recordado (terminal y almacén), búsqueda/escaneo de productos con precio de referencia y stock, carrito con reglas espejo del backend, cobro con vuelto en vivo, envío idempotente y comprobante interno imprimible.

**Architecture:** La lógica del carrito y del cobro es pura (`lib/carrito.ts`, `lib/errores-pos.ts`) y se prueba al 100%. Los componentes son delgados (buscador, tabla del carrito, panel de cobro, aviso de turno) y `PosPage` los compone. El POS consume otras features solo por su `index.ts`: `catalogo` (`skusQuery`), `inventario` (`posicionesQuery`, `invalidateInventario`), `organizacion` (`TerminalSelector`, `usePuestoTrabajo`, `useEstablecimientos`), `caja` (`turnoActualQuery`, `invalidateCaja`) y `ventas` (`registrarVenta`, `ComprobanteVenta`, `invalidateVentas`).

**Tech Stack:** React 19.2, React Router 8, TanStack Query, `@boticas/ui-web`, Vitest + Testing Library + MSW, Playwright.

Spec: `docs/superpowers/specs/2026-10-03-frontend-caja-pos-ventas-design.md`. Requiere integrados los planes 1 (backend precio/listado de SKU), 2 (base y caja) y 3 (ventas).

## Global Constraints

- Todo archivo **nuevo** (código y tests) alcanza 100% de líneas, ramas, funciones y sentencias; no agregar archivos a `coverage-baseline.txt`; los reemplazados que figuren en ella se quitan (Task 5).
- Sin comentarios en el código; sin duplicación; `forwardRef` y React Router 8 bloqueados por ESLint; las features solo se importan por `index.ts`.
- Reglas espejo del backend (POR_VALIDAR el redondeo de visualización): cantidad `> 0`, hasta 4 decimales y máximo 10 enteros; fracción solo si `permiteVentaFraccion`; precio `>= 0` con hasta 4 decimales; máximo 100 líneas; monto recibido con hasta 2 decimales y `>=` total. El total mostrado es solo informativo: el comprobante usa los montos devueltos por el backend.
- `Idempotency-Key` estable por contenido del carrito mediante `useClaveIdempotencia` de `shared/lib` (la clave cambia cuando cambia el payload, incluido el monto recibido).
- Sin turno abierto en la terminal elegida el cobro queda bloqueado y se enlaza a `/caja`.
- Textos exactos: ver cada tarea (los usan los tests y el e2e).
- Comandos desde `frontend/` (`pnpm.cmd` si `pnpm.ps1` está bloqueado): `pnpm --filter @boticas/erp-web test -- <ruta>`, `pnpm check`, `pnpm e2e -- pos.spec.ts`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan difiere por nombres reales del repo (labels, fixtures), ajustar al comportamiento real sin reducir lo que verifica y anotarlo en el reporte.

Rutas abreviadas: `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: Publicar lo que el POS consume de inventario y fixtures

**Files:**
- Modify: `SRC/features/inventario/index.ts`
- Modify: `SRC/test/ventas-fixtures.ts`

**Interfaces:**
- Produces: `inventario/index.ts` exporta `posicionesQuery`, `invalidateInventario` y el tipo `Posicion`; fixtures `sampleSkuVenta` (SkuResumen con precio) y `sampleSkuFraccionable`.

- [ ] **Step 1: Implementar**

`SRC/features/inventario/index.ts`:

```ts
export { inventoryRoutes } from './routes';
export { posicionesQuery } from './api/posiciones.api';
export { invalidateInventario } from './api/invalidate';
export type { Posicion } from './api/inventario.types';
```

Agregar a `SRC/test/ventas-fixtures.ts`:

```ts
import type { SkuResumen } from '../features/catalogo';

export const sampleSkuVenta: SkuResumen = {
  id: 'sku-0001-aaaa',
  codigoInterno: 'MED-001',
  descripcionComercial: 'Paracetamol 500 mg',
  tipoSku: 'REGULADO',
  estado: 'ACTIVO',
  unidadVentaCodigo: 'UND',
  permiteVentaFraccion: false,
  precioVentaReferencia: 12.5
};

export const sampleSkuFraccionable: SkuResumen = {
  id: 'sku-0002-bbbb',
  codigoInterno: 'JAR-002',
  descripcionComercial: 'Jarabe por mililitro',
  tipoSku: 'NO_REGULADO',
  estado: 'ACTIVO',
  unidadVentaCodigo: 'ML',
  permiteVentaFraccion: true,
  precioVentaReferencia: null
};
```
(`sampleSku` de `inventario-fixtures` ya trae los campos nuevos desde el plan 2.)

- [ ] **Step 2: Verificar**

Run: `pnpm --filter @boticas/erp-web test -- src/features/inventario && pnpm typecheck && pnpm lint`
Expected: PASS (el índice no tiene lógica propia que cubrir; las funciones reexportadas ya están cubiertas).

- [ ] **Step 3: Commit**

```bash
git add frontend
git commit -m "feat(inventario): publicar posiciones e invalidacion para el POS

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Lógica pura del carrito, el cobro y los errores del POS

**Files:**
- Create: `SRC/features/pos/lib/carrito.ts`, `SRC/features/pos/lib/carrito.test.ts`
- Create: `SRC/features/pos/lib/errores-pos.ts`, `SRC/features/pos/lib/errores-pos.test.ts`

**Interfaces:**
- Consumes: `SkuResumen` (`../../catalogo`), `RegistrarVentaPayload` (`../../ventas`).
- Produces:

```ts
export const MAX_LINEAS = 100;
export type LineaCarrito = {
  skuId: string; codigoInterno: string; descripcion: string; unidadVentaCodigo: string;
  permiteVentaFraccion: boolean; cantidad: string; precio: string;
};
export type Carrito = LineaCarrito[];
export function lineaDesdeSku(sku: SkuResumen): LineaCarrito;
export function agregarSku(carrito: Carrito, sku: SkuResumen): Carrito;
export function actualizarLinea(carrito: Carrito, skuId: string, cambios: Partial<Pick<LineaCarrito, 'cantidad' | 'precio'>>): Carrito;
export function quitarLinea(carrito: Carrito, skuId: string): Carrito;
export function errorLinea(linea: LineaCarrito): string | null;
export function totalLinea(linea: LineaCarrito): number;
export function totalCarrito(carrito: Carrito): number;
export function esMontoValido(texto: string): boolean;
export function vueltoDe(total: number, recibido: string): number | null;
export function puedeCobrar(estado: { carrito: Carrito; recibido: string; hayTurno: boolean; hayAlmacen: boolean }): boolean;
export function toRegistrarVentaPayload(carrito: Carrito, terminalId: string, almacenId: string, recibido: string): RegistrarVentaPayload;
export function describeErrorPos(error: unknown): string;
```

- [ ] **Step 1: Escribir los tests que fallan**

`carrito.test.ts`:

```ts
import { sampleSkuFraccionable, sampleSkuVenta } from '../../../test/ventas-fixtures';
import {
  MAX_LINEAS,
  actualizarLinea,
  agregarSku,
  errorLinea,
  esMontoValido,
  lineaDesdeSku,
  puedeCobrar,
  quitarLinea,
  toRegistrarVentaPayload,
  totalCarrito,
  totalLinea,
  vueltoDe,
  type Carrito
} from './carrito';

const linea = lineaDesdeSku(sampleSkuVenta);

describe('lineaDesdeSku', () => {
  it('precarga cantidad 1 y el precio de referencia', () => {
    expect(linea).toEqual({
      skuId: 'sku-0001-aaaa',
      codigoInterno: 'MED-001',
      descripcion: 'Paracetamol 500 mg',
      unidadVentaCodigo: 'UND',
      permiteVentaFraccion: false,
      cantidad: '1',
      precio: '12.5'
    });
  });

  it('deja el precio vacío cuando el SKU no tiene precio de referencia y usa UND sin unidad', () => {
    const resultado = lineaDesdeSku({ ...sampleSkuFraccionable, unidadVentaCodigo: null });

    expect(resultado.precio).toBe('');
    expect(resultado.unidadVentaCodigo).toBe('UND');
    expect(resultado.permiteVentaFraccion).toBe(true);
  });
});

describe('agregarSku', () => {
  it('agrega una línea nueva', () => {
    expect(agregarSku([], sampleSkuVenta)).toEqual([linea]);
  });

  it('suma una unidad cuando el SKU ya está en el carrito', () => {
    expect(agregarSku([linea], sampleSkuVenta)).toEqual([{ ...linea, cantidad: '2' }]);
  });

  it('trata una cantidad inválida existente como cero al sumar', () => {
    expect(agregarSku([{ ...linea, cantidad: 'x' }], sampleSkuVenta)[0]?.cantidad).toBe('1');
  });

  it('no agrega más líneas del máximo permitido', () => {
    const lleno: Carrito = Array.from({ length: MAX_LINEAS }, (_, indice) => ({
      ...linea,
      skuId: `sku-${indice}`
    }));

    expect(agregarSku(lleno, sampleSkuVenta)).toBe(lleno);
  });
});

describe('actualizarLinea y quitarLinea', () => {
  it('cambia solo la línea indicada', () => {
    const otra = lineaDesdeSku(sampleSkuFraccionable);

    expect(actualizarLinea([linea, otra], linea.skuId, { cantidad: '3', precio: '10' })).toEqual([
      { ...linea, cantidad: '3', precio: '10' },
      otra
    ]);
  });

  it('quita la línea indicada', () => {
    const otra = lineaDesdeSku(sampleSkuFraccionable);

    expect(quitarLinea([linea, otra], linea.skuId)).toEqual([otra]);
  });
});

describe('errorLinea', () => {
  it('no reporta error en una línea válida', () => {
    expect(errorLinea(linea)).toBeNull();
    expect(errorLinea({ ...linea, precio: '0' })).toBeNull();
  });

  it.each(['', '0', '-1', 'abc', '1.00001', '12345678901'])('rechaza la cantidad "%s"', (cantidad) => {
    expect(errorLinea({ ...linea, cantidad })).toBe(
      'La cantidad debe ser mayor que cero con hasta 4 decimales.'
    );
  });

  it('rechaza decimales en un producto que no se vende por fracción', () => {
    expect(errorLinea({ ...linea, cantidad: '1.5' })).toBe('Este producto no se vende por fracción.');
  });

  it('acepta decimales en un producto fraccionable', () => {
    expect(errorLinea({ ...lineaDesdeSku(sampleSkuFraccionable), cantidad: '1.5', precio: '2' })).toBeNull();
  });

  it.each(['', '-1', 'abc', '1.00001'])('rechaza el precio "%s"', (precio) => {
    expect(errorLinea({ ...linea, precio })).toBe(
      'El precio debe ser mayor o igual a cero con hasta 4 decimales.'
    );
  });
});

describe('totales', () => {
  it('totalLinea multiplica y redondea a dos decimales', () => {
    expect(totalLinea({ ...linea, cantidad: '3', precio: '0.3333' })).toBe(1);
    expect(totalLinea({ ...lineaDesdeSku(sampleSkuFraccionable), cantidad: '1.5', precio: '2.5' })).toBe(3.75);
  });

  it('totalLinea es cero cuando la línea es inválida', () => {
    expect(totalLinea({ ...linea, cantidad: '' })).toBe(0);
  });

  it('totalCarrito suma las líneas', () => {
    expect(
      totalCarrito([{ ...linea, cantidad: '2' }, { ...lineaDesdeSku(sampleSkuFraccionable), cantidad: '1.5', precio: '2' }])
    ).toBe(28);
    expect(totalCarrito([])).toBe(0);
  });
});

describe('cobro', () => {
  it.each(['0', '30', '30.5', '30.50'])('acepta el monto %s', (texto) => {
    expect(esMontoValido(texto)).toBe(true);
  });

  it.each(['', '-1', '1.234', 'abc'])('rechaza el monto "%s"', (texto) => {
    expect(esMontoValido(texto)).toBe(false);
  });

  it('vueltoDe devuelve el vuelto o null si el monto no alcanza o no es válido', () => {
    expect(vueltoDe(25, '30')).toBe(5);
    expect(vueltoDe(25, '25')).toBe(0);
    expect(vueltoDe(25.1, '30')).toBe(4.9);
    expect(vueltoDe(25, '20')).toBeNull();
    expect(vueltoDe(25, 'abc')).toBeNull();
  });

  const base = { carrito: [linea], recibido: '20', hayTurno: true, hayAlmacen: true };

  it('puedeCobrar exige turno, almacén, carrito válido y monto suficiente', () => {
    expect(puedeCobrar(base)).toBe(true);
    expect(puedeCobrar({ ...base, hayTurno: false })).toBe(false);
    expect(puedeCobrar({ ...base, hayAlmacen: false })).toBe(false);
    expect(puedeCobrar({ ...base, carrito: [] })).toBe(false);
    expect(puedeCobrar({ ...base, carrito: [{ ...linea, cantidad: '' }] })).toBe(false);
    expect(puedeCobrar({ ...base, recibido: '5' })).toBe(false);
  });

  it('toRegistrarVentaPayload arma el cuerpo numérico para el backend', () => {
    expect(toRegistrarVentaPayload([{ ...linea, cantidad: '2' }], 'term-1', 'alm-1', '30')).toEqual({
      terminalId: 'term-1',
      almacenId: 'alm-1',
      lineas: [{ skuId: 'sku-0001-aaaa', cantidad: 2, precioUnitario: 12.5 }],
      pago: { montoRecibido: 30 }
    });
  });
});
```

`errores-pos.test.ts`:

```ts
import { ApiError } from '@boticas/api-client';
import { describeErrorPos } from './errores-pos';

const error = (code: string | undefined, status = 409, detail = 'detalle del backend') =>
  new ApiError('falló', status, { code, detail, status });

describe('describeErrorPos', () => {
  it.each([
    ['INV_STOCK_INSUFICIENTE', 'No hay stock suficiente para una de las líneas. Ajusta la cantidad.'],
    ['VEN_TURNO_NO_ABIERTO', 'La terminal no tiene un turno abierto. Abre el turno en Caja.'],
    ['VEN_IDEMPOTENCY_CONFLICT', 'Esta venta ya se envió con datos distintos. Revisa el carrito y vuelve a cobrar.'],
    ['VEN_MONTO_RECIBIDO_INSUFICIENTE', 'El monto recibido no cubre el total de la venta.'],
    ['VEN_TERMINAL_NO_OPERABLE', 'La terminal no está activa para operar.'],
    ['VEN_ALMACEN_NO_OPERABLE', 'El almacén no está habilitado para vender.']
  ])('traduce %s', (code, mensaje) => {
    expect(describeErrorPos(error(code))).toBe(mensaje);
  });

  it('para otro código del backend muestra el detalle y el código', () => {
    expect(describeErrorPos(error('VEN_SKU_NO_OPERABLE'))).toBe('detalle del backend (VEN_SKU_NO_OPERABLE)');
  });

  it('sin código usa el mensaje genérico de la API', () => {
    expect(describeErrorPos(error(undefined, 500))).toBe('detalle del backend');
    expect(describeErrorPos(new Error('x'))).toBe('No se pudo completar la operación. Inténtalo de nuevo.');
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos/lib`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 3: Implementar**

`carrito.ts`:

```ts
import type { SkuResumen } from '../../catalogo';
import type { RegistrarVentaPayload } from '../../ventas';

export const MAX_LINEAS = 100;

export type LineaCarrito = {
  skuId: string;
  codigoInterno: string;
  descripcion: string;
  unidadVentaCodigo: string;
  permiteVentaFraccion: boolean;
  cantidad: string;
  precio: string;
};

export type Carrito = LineaCarrito[];

const PATRON_CANTIDAD = /^\d{1,10}(\.\d{1,4})?$/;
const PATRON_PRECIO = /^\d{1,10}(\.\d{1,4})?$/;
const PATRON_MONTO = /^\d{1,9}(\.\d{1,2})?$/;

const redondear = (valor: number, decimales: number): number => {
  const factor = 10 ** decimales;
  return Math.round((valor + Number.EPSILON) * factor) / factor;
};

export const lineaDesdeSku = (sku: SkuResumen): LineaCarrito => ({
  skuId: sku.id,
  codigoInterno: sku.codigoInterno,
  descripcion: sku.descripcionComercial,
  unidadVentaCodigo: sku.unidadVentaCodigo ?? 'UND',
  permiteVentaFraccion: sku.permiteVentaFraccion,
  cantidad: '1',
  precio: sku.precioVentaReferencia === null ? '' : String(sku.precioVentaReferencia)
});

export function agregarSku(carrito: Carrito, sku: SkuResumen): Carrito {
  if (carrito.some(({ skuId }) => skuId === sku.id)) {
    return carrito.map((linea) =>
      linea.skuId === sku.id
        ? { ...linea, cantidad: String(redondear((Number(linea.cantidad) || 0) + 1, 4)) }
        : linea
    );
  }
  return carrito.length >= MAX_LINEAS ? carrito : [...carrito, lineaDesdeSku(sku)];
}

export const actualizarLinea = (
  carrito: Carrito,
  skuId: string,
  cambios: Partial<Pick<LineaCarrito, 'cantidad' | 'precio'>>
): Carrito => carrito.map((linea) => (linea.skuId === skuId ? { ...linea, ...cambios } : linea));

export const quitarLinea = (carrito: Carrito, skuId: string): Carrito =>
  carrito.filter((linea) => linea.skuId !== skuId);

export function errorLinea(linea: LineaCarrito): string | null {
  if (!PATRON_CANTIDAD.test(linea.cantidad) || Number(linea.cantidad) <= 0) {
    return 'La cantidad debe ser mayor que cero con hasta 4 decimales.';
  }
  if (!linea.permiteVentaFraccion && !Number.isInteger(Number(linea.cantidad))) {
    return 'Este producto no se vende por fracción.';
  }
  return PATRON_PRECIO.test(linea.precio)
    ? null
    : 'El precio debe ser mayor o igual a cero con hasta 4 decimales.';
}

export const totalLinea = (linea: LineaCarrito): number =>
  errorLinea(linea) === null ? redondear(Number(linea.cantidad) * Number(linea.precio), 2) : 0;

export const totalCarrito = (carrito: Carrito): number =>
  redondear(
    carrito.reduce((acumulado, linea) => acumulado + totalLinea(linea), 0),
    2
  );

export const esMontoValido = (texto: string): boolean => PATRON_MONTO.test(texto);

export function vueltoDe(total: number, recibido: string): number | null {
  if (!esMontoValido(recibido) || Number(recibido) < total) return null;
  return redondear(Number(recibido) - total, 2);
}

export function puedeCobrar(estado: {
  carrito: Carrito;
  recibido: string;
  hayTurno: boolean;
  hayAlmacen: boolean;
}): boolean {
  return (
    estado.hayTurno &&
    estado.hayAlmacen &&
    estado.carrito.length > 0 &&
    estado.carrito.every((linea) => errorLinea(linea) === null) &&
    vueltoDe(totalCarrito(estado.carrito), estado.recibido) !== null
  );
}

export const toRegistrarVentaPayload = (
  carrito: Carrito,
  terminalId: string,
  almacenId: string,
  recibido: string
): RegistrarVentaPayload => ({
  terminalId,
  almacenId,
  lineas: carrito.map((linea) => ({
    skuId: linea.skuId,
    cantidad: Number(linea.cantidad),
    precioUnitario: Number(linea.precio)
  })),
  pago: { montoRecibido: Number(recibido) }
});
```
Nota: el test `vueltoDe(25.1, '30')` → `4.9` y el de `totalLinea('3' × '0.3333')` → `1` (0.9999 redondeado a 1) validan el redondeo; si `Number.EPSILON` produce un valor distinto al esperado en algún caso límite, ajustar solo ese caso del test a un valor equivalente y anotarlo.

`errores-pos.ts`:

```ts
import { ApiError } from '@boticas/api-client';
import { describeApiError } from '../../../shared/lib/describe-api-error';

const MENSAJES: Record<string, string> = {
  INV_STOCK_INSUFICIENTE: 'No hay stock suficiente para una de las líneas. Ajusta la cantidad.',
  VEN_TURNO_NO_ABIERTO: 'La terminal no tiene un turno abierto. Abre el turno en Caja.',
  VEN_IDEMPOTENCY_CONFLICT:
    'Esta venta ya se envió con datos distintos. Revisa el carrito y vuelve a cobrar.',
  VEN_MONTO_RECIBIDO_INSUFICIENTE: 'El monto recibido no cubre el total de la venta.',
  VEN_TERMINAL_NO_OPERABLE: 'La terminal no está activa para operar.',
  VEN_ALMACEN_NO_OPERABLE: 'El almacén no está habilitado para vender.'
};

export function describeErrorPos(error: unknown): string {
  const code = error instanceof ApiError ? error.problem?.code : undefined;
  if (code === undefined) return describeApiError(error);
  return MENSAJES[code] ?? `${describeApiError(error)} (${code})`;
}
```
Con `describeApiError` el caso 409 con `detail` devuelve `problem.detail`; el caso 500 sin código usa `detail`.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos/lib && pnpm typecheck`
Expected: PASS con 100% en ambos archivos.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(pos): logica pura del carrito, cobro y errores

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Búsqueda de productos con precio de referencia y stock

**Files:**
- Create: `SRC/features/pos/lib/codigo-barras.ts` y `.test.ts`
- Create: `SRC/features/pos/components/StockDisponible.tsx` y `.test.tsx`
- Create: `SRC/features/pos/components/BuscadorProductos.tsx` y `.test.tsx`

**Interfaces:**
- Consumes: `skusQuery`, `SkuResumen` (`../../catalogo`); `posicionesQuery` (`../../inventario`); `formatoMoneda`.
- Produces: `esCodigoBarras(texto: string): boolean` (solo dígitos, longitud 8 a 14); `StockDisponible({ almacenId, skuId })`; `BuscadorProductos({ almacenId, deshabilitado, onElegir })` donde `onElegir(sku: SkuResumen)`.
- Textos exactos: etiqueta `Buscar producto`, botón `Buscar`, placeholder `Código, descripción o código de barras`, vacío `No se encontraron productos.`, error `No se pudo buscar productos.`, por resultado un botón `Agregar {codigoInterno}` con precio (`formatoMoneda`) o `Sin precio` y `Stock: {n}` (o `Stock: …` mientras carga); mensaje cuando no hay almacén: `Selecciona un almacén para ver el stock.`.

- [ ] **Step 1: Escribir los tests que fallan**

`codigo-barras.test.ts`:

```ts
import { esCodigoBarras } from './codigo-barras';

describe('esCodigoBarras', () => {
  it.each(['12345678', '7750001234567', '12345678901234'])('acepta %s', (texto) => {
    expect(esCodigoBarras(texto)).toBe(true);
  });

  it.each(['1234567', '123456789012345', '77500012345ab', '', 'paracetamol'])('rechaza "%s"', (texto) => {
    expect(esCodigoBarras(texto)).toBe(false);
  });
});
```

`StockDisponible.test.tsx`: con MSW respondiendo `GET */api/v1/inventario/posiciones` (verificar que se envían `almacenId` y `skuId`) y posiciones `[samplePosicion (disponible 90, vendible), {...samplePosicion, id:'pos-2', cantidadDisponible: 5, vendible: false}]` muestra `Stock: 90` (suma solo posiciones vendibles); sin posiciones muestra `Stock: 0`; mientras carga `Stock: …`; con error muestra `Stock: —`; sin `almacenId` muestra `Selecciona un almacén para ver el stock.` y no consulta.

`BuscadorProductos.test.tsx` (`renderRoute`, MSW de `*/api/v1/catalogo/skus` y `*/api/v1/inventario/posiciones`): (a) al enviar el formulario con `paracetamol` consulta con `q=paracetamol`, `estado=ACTIVO` y muestra el resultado con `Agregar MED-001`, precio `S/ 12.50` y `Stock: 90`; (b) un SKU sin precio muestra `Sin precio`; (c) `onElegir` recibe el SKU al pulsar `Agregar MED-001`; (d) un término con aspecto de código de barras (`7750001234567`) y un único resultado llama `onElegir` automáticamente una vez y limpia el campo; con dos resultados no autoagrega; (e) sin resultados muestra `No se encontraron productos.`; con error 500 muestra `No se pudo buscar productos.`; (f) con `deshabilitado` los botones `Agregar` están deshabilitados; (g) antes de buscar no consulta nada.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos`
Expected: FAIL.

- [ ] **Step 3: Implementar**

`codigo-barras.ts`:

```ts
export const esCodigoBarras = (texto: string): boolean => /^\d{8,14}$/.test(texto);
```

`StockDisponible.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { posicionesQuery } from '../../inventario';

type StockDisponibleProps = { almacenId: string; skuId: string };

export function StockDisponible({ almacenId, skuId }: StockDisponibleProps) {
  const { data, isPending, isError } = useQuery({
    ...posicionesQuery({ almacenId, skuId, size: 100 }),
    enabled: almacenId !== ''
  });

  if (almacenId === '') return <span>Selecciona un almacén para ver el stock.</span>;
  if (isError) return <span>Stock: —</span>;
  if (isPending) return <span>Stock: …</span>;
  const total = data.items
    .filter(({ vendible }) => vendible)
    .reduce((acumulado, { cantidadDisponible }) => acumulado + cantidadDisponible, 0);
  return <span>Stock: {total}</span>;
}
```
Con `enabled: false` y sin datos, `isPending` es `true`: el primer `if` evita mostrar "…" sin almacén.

`BuscadorProductos.tsx`:

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, Card } from '@boticas/ui-web';
import { TextField } from '../../../shared/components/FormFields';
import { formatoMoneda } from '../../../shared/lib/format';
import { skusQuery, type SkuResumen } from '../../catalogo';
import { esCodigoBarras } from '../lib/codigo-barras';
import { StockDisponible } from './StockDisponible';

type BuscadorProductosProps = {
  almacenId: string;
  deshabilitado: boolean;
  onElegir: (sku: SkuResumen) => void;
};

export function BuscadorProductos({ almacenId, deshabilitado, onElegir }: BuscadorProductosProps) {
  const [texto, setTexto] = useState('');
  const [termino, setTermino] = useState('');
  const { data, isError, isFetching } = useQuery({
    ...skusQuery({ q: termino, estado: 'ACTIVO', size: 10 }),
    enabled: termino !== ''
  });
  const resultados = data?.items ?? [];
  const unicoPorBarras = esCodigoBarras(termino) && resultados.length === 1 ? resultados[0] : undefined;
  ...
}
```
Completar el cuerpo así: un `<form>` con `TextField id="pos-buscar" label="Buscar producto" placeholder="Código, descripción o código de barras"` (valor `texto`) y `Button type="submit"` `Buscar`; `onSubmit` hace `preventDefault`, `setTermino(texto.trim())`. La autoadición cuando `unicoPorBarras` se resuelve en el `onSubmit` no es posible (los datos llegan después), por lo que se hace en un `useEffect` dependiente de `unicoPorBarras?.id` y `termino`: si existe, llama `onElegir(unicoPorBarras)`, `setTexto('')` y `setTermino('')`. Lista: si `termino !== ''` y `isError` → `No se pudo buscar productos.`; si `termino !== ''`, no `isFetching`, sin error y `resultados.length === 0` → `No se encontraron productos.`; si no, una `ul` con una fila por SKU: `{codigoInterno} — {descripcionComercial}`, precio (`formatoMoneda(precioVentaReferencia)` o `Sin precio`), `<StockDisponible almacenId={almacenId} skuId={id} />` y `Button` `Agregar {codigoInterno}` (`aria-label` igual) con `disabled={deshabilitado}` y `onClick={() => onElegir(sku)}`. Todo dentro de un `Card`.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos && pnpm typecheck && pnpm lint`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(pos): busqueda de productos con precio de referencia y stock

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Carrito, cobro, aviso de turno y puesto de venta

**Files:**
- Create: `SRC/features/pos/components/CarritoTable.tsx` y `.test.tsx`, `PanelCobro.tsx` y `.test.tsx`, `AvisoTurno.tsx` y `.test.tsx`, `PuestoVenta.tsx` y `.test.tsx`

**Interfaces:**
- Consumes: `lib/carrito.ts` (Task 2), `TerminalSelector`, `useEstablecimientos`, `PuestoTrabajo` (`../../organizacion`), `formatoMoneda`.
- Produces: `CarritoTable({ carrito, onCambiar, onQuitar })` con `onCambiar(skuId, cambios)`; `PanelCobro({ total, recibido, onRecibido, vuelto, puedeCobrar, isSubmitting, error, onCobrar })`; `AvisoTurno()` (enlace a `/caja`); `PuestoVenta({ puesto, onChange })` con `onChange(puesto: PuestoTrabajo)`.
- Textos exactos: tabla con columnas `Producto`, `Cantidad`, `Precio`, `Total`, vacío `El carrito está vacío.`; botones `Quitar {codigoInterno}`; inputs con etiquetas accesibles `Cantidad de {codigoInterno}` y `Precio de {codigoInterno}`; el error de línea se muestra bajo la fila con `role="alert"`; panel con `Total`, campo `Monto recibido`, `Vuelto` (monto o `—` si no alcanza), botón `Cobrar` (en curso: `Cobrando…`); aviso `No hay un turno abierto en esta terminal.` con enlace `Ir a Caja` a `/caja`; selector de almacén con etiqueta `Almacén` y opción `Selecciona un almacén`.

- [ ] **Step 1: Escribir los tests que fallan**

Casos obligatorios (estilo de los tests de componentes existentes, `renderRoute` cuando hay `Link`):
- `CarritoTable.test.tsx`: sin líneas muestra el vacío; con una línea muestra descripción, `Cantidad de MED-001` con `1`, `Precio de MED-001` con `12.5` y total `S/ 12.50`; escribir en los inputs llama `onCambiar('sku-0001-aaaa', { cantidad: '3' })` / `{ precio: '10' }`; `Quitar MED-001` llama `onQuitar('sku-0001-aaaa')`; una línea inválida (cantidad vacía) muestra `La cantidad debe ser mayor que cero con hasta 4 decimales.` en un `role="alert"` y total `S/ 0.00`.
- `PanelCobro.test.tsx`: muestra el total formateado; escribir en `Monto recibido` llama `onRecibido`; muestra `Vuelto` con `formatoMoneda(vuelto)` o `—` cuando `vuelto` es `null`; `Cobrar` deshabilitado si `puedeCobrar` es `false` o `isSubmitting` y habilitado en caso contrario; pulsarlo llama `onCobrar`; con `isSubmitting` el texto es `Cobrando…`; muestra `error` en `role="alert"`.
- `AvisoTurno.test.tsx`: muestra el texto y el enlace `Ir a Caja` con `href="/caja"`.
- `PuestoVenta.test.tsx` (MSW de estructura corporativa y terminales como en `TerminalSelector.test.tsx`): muestra `TerminalSelector` y, con establecimiento `est-1`, el selector `Almacén` con `Almacén Central` y `Almacén Frío`; elegir un almacén llama `onChange({ ...puesto, almacenId: 'alm-1' })`; elegir otro establecimiento llama `onChange` con terminal y almacén vacíos; elegir una terminal conserva el almacén; sin establecimiento el selector `Almacén` está deshabilitado.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos/components`
Expected: FAIL.

- [ ] **Step 3: Implementar**

`CarritoTable.tsx`: `DataTable<LineaCarrito>` con columnas `Producto` (`{descripcion}` más el código), `Cantidad` (`Input` `inputMode="decimal"`, `aria-label={`Cantidad de ${linea.codigoInterno}`}`, `value={linea.cantidad}`, `onChange={(e) => onCambiar(linea.skuId, { cantidad: e.target.value })}`, más la unidad), `Precio` (igual con `precio`), `Total` (`formatoMoneda(totalLinea(linea))` y, si `errorLinea(linea)`, un `<p role="alert">` con el mensaje) y una última columna con `Button variant="secondary"` `Quitar` (`aria-label={`Quitar ${linea.codigoInterno}`}`); `rowKey={(l) => l.skuId}`, `emptyMessage="El carrito está vacío."`.

`PanelCobro.tsx`: `Card` con `dl` (`Total` → `formatoMoneda(total)`, `Vuelto` → `vuelto === null ? '—' : formatoMoneda(vuelto)`), `TextField id="pos-recibido" label="Monto recibido" inputMode="decimal"`, `FormError` si `error`, `Button` `Cobrar` (`disabled={!puedeCobrar || isSubmitting}`; texto `Cobrando…` si `isSubmitting`).

`AvisoTurno.tsx`: `Card` con texto `No hay un turno abierto en esta terminal.` y `<Link to="/caja">Ir a Caja</Link>`.

`PuestoVenta.tsx`:

```tsx
import { Card } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import { TerminalSelector, useEstablecimientos, type PuestoTrabajo } from '../../organizacion';

type PuestoVentaProps = {
  puesto: PuestoTrabajo;
  onChange: (puesto: PuestoTrabajo) => void;
};

export function PuestoVenta({ puesto, onChange }: PuestoVentaProps) {
  const almacenes =
    useEstablecimientos().find(({ id }) => id === puesto.establecimientoId)?.warehouses ?? [];

  return (
    <Card className="space-y-4 p-4">
      <TerminalSelector
        establecimientoId={puesto.establecimientoId}
        terminalId={puesto.terminalId}
        onChange={(establecimientoId, terminalId) =>
          onChange({
            establecimientoId,
            terminalId,
            almacenId: establecimientoId === puesto.establecimientoId ? puesto.almacenId : ''
          })
        }
      />
      <SelectField
        id="pos-almacen"
        label="Almacén"
        value={puesto.almacenId}
        disabled={puesto.establecimientoId === ''}
        onChange={(event) => onChange({ ...puesto, almacenId: event.target.value })}
      >
        <option value="">Selecciona un almacén</option>
        {almacenes.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
    </Card>
  );
}
```
(la regla "al cambiar de establecimiento se limpia el almacén" ya vive aquí y en `CashRegisterPage`; si ambos duplican la misma expresión, extraer a `organizacion` una función pura `cambiarTerminal(puesto, establecimientoId, terminalId): PuestoTrabajo` junto a `usePuestoTrabajo`, exportarla por el índice, usarla en ambos sitios y cubrirla con su test.)

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos src/features/caja src/features/organizacion && pnpm typecheck && pnpm lint`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(pos): carrito, panel de cobro, aviso de turno y puesto de venta

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Pantalla POS, rutas, baseline y e2e

**Files:**
- Modify (reemplazar contenido): `SRC/features/pos/pages/PosPage.tsx`; Create: `SRC/features/pos/pages/PosPage.test.tsx`
- Create: `SRC/features/pos/routes.test.ts`
- Modify: `frontend/coverage-baseline.txt`
- Create: `E2E/support/pos-api.ts`, `E2E/pos.spec.ts`

**Interfaces:**
- Consumes: todo lo anterior, `turnoActualQuery` e `invalidateCaja` (`../../caja`), `registrarVenta`, `ComprobanteVenta`, `invalidateVentas` (`../../ventas`), `invalidateInventario` (`../../inventario`), `useClaveIdempotencia` (`shared/lib`), `apiClient`.
- Textos exactos: título `Punto de venta`; modal del comprobante con título `Venta registrada` y botón `Nueva venta`.

- [ ] **Step 1: Escribir los tests que fallan**

`routes.test.ts` análogo al de caja con `['pos']`.

`PosPage.test.tsx` (`renderRoute('/pos', PosPage, '/pos')`, MSW para `estructura-corporativa`, `terminales-pos`, `ventas/turnos/actual`, `catalogo/skus`, `inventario/posiciones` y `ventas/ventas`; el puesto se precarga con `localStorage.setItem('erp.puesto-trabajo', JSON.stringify({ establecimientoId: 'est-1', terminalId: 'term-1', almacenId: 'alm-1' }))` y se limpia en `afterEach`). Casos obligatorios:
- (a) sin turno (`turnos/actual` → 404): muestra `No hay un turno abierto en esta terminal.` con `Ir a Caja` y el botón `Cobrar` deshabilitado aunque haya carrito;
- (b) flujo completo: con turno abierto, buscar `paracetamol`, `Agregar MED-001`, la fila aparece con `Cantidad de MED-001` = `1` y precio `12.5`; cambiar la cantidad a `2` actualiza el total a `S/ 25.00`; escribir `30` en `Monto recibido` muestra `Vuelto` `S/ 5.00` y habilita `Cobrar`; al cobrar se hace `POST /ventas/ventas` con cuerpo `{ terminalId: 'term-1', almacenId: 'alm-1', lineas: [{ skuId: 'sku-0001-aaaa', cantidad: 2, precioUnitario: 12.5 }], pago: { montoRecibido: 30 } }` y cabecera `Idempotency-Key` no vacía; se abre el modal `Venta registrada` con el `ComprobanteVenta` (número de operación `EST001-T01-000001`); se invalidan `['ventas']`, `['caja']` e `['inventario']` (espiar `queryClient.invalidateQueries`); `Nueva venta` cierra el modal y vacía el carrito y el monto recibido;
- (c) reintento con el mismo carrito tras un error reutiliza la misma `Idempotency-Key` (dos `POST` con la misma clave: el primero responde 500, el segundo 201), y cambiar la cantidad entre intentos genera otra clave;
- (d) un error `INV_STOCK_INSUFICIENTE` del backend se muestra con el texto traducido en `role="alert"` y el carrito se conserva;
- (e) un SKU sin precio de referencia entra con precio vacío, `Cobrar` queda deshabilitado y el error de línea del precio aparece hasta que se digita;
- (f) sin terminal en el puesto guardado muestra el selector y `Selecciona una terminal para vender.`, y el buscador queda deshabilitado;
- (g) cambiar de terminal vacía el carrito.

`E2E/support/pos-api.ts`: `mockPosApi(page)` que combina los mocks de caja (reutilizar `mockCajaApi` de `caja-api.ts` importándolo, con un turno abierto inicial mediante una opción `{ turnoAbierto: true }` que se agrega a `mockCajaApi`), `catalogo/skus` (`pagina([sampleSkuVenta])`), `inventario/posiciones` (`pagina([samplePosicion])`) y `POST /api/v1/ventas/ventas` (responde 201 con `sampleVenta` ajustando `lineas[0].cantidad`, `total` y `pago.montoRecibido/vuelto` según el cuerpo). `abrirPosEn(page, path)`: mock + `login` + `page.addInitScript` que escribe `erp.puesto-trabajo` + `goto`.

`E2E/pos.spec.ts`:

```ts
import { expect, test } from '@playwright/test';
import { expectNoHorizontalOverflow } from './support/layout';
import { abrirPosEn } from './support/pos-api';

test.describe('Punto de venta', () => {
  test('vende en efectivo con vuelto y muestra el comprobante', async ({ page }) => {
    await abrirPosEn(page, '/pos');

    await expect(page.getByRole('heading', { name: 'Punto de venta', exact: true })).toBeVisible();
    await page.getByLabel('Buscar producto').fill('paracetamol');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await page.getByRole('button', { name: 'Agregar MED-001' }).click();
    await page.getByLabel('Cantidad de MED-001').fill('2');
    await page.getByLabel('Monto recibido').fill('30');
    await expect(page.getByText('S/ 5.00').first()).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Cobrar' }).click();

    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Venta registrada')).toBeVisible();
    await expect(page.getByText('Comprobante interno — no válido como comprobante de pago')).toBeVisible();
    await page.getByRole('button', { name: 'Nueva venta' }).click();
    await expect(page.getByText('El carrito está vacío.')).toBeVisible();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos`
Expected: FAIL.

- [ ] **Step 3: Implementar**

`PosPage.tsx`:

```tsx
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { useClaveIdempotencia } from '../../../shared/lib/use-clave-idempotencia';
import { invalidateCaja, turnoActualQuery } from '../../caja';
import { invalidateInventario } from '../../inventario';
import { useEstablecimientos, usePuestoTrabajo } from '../../organizacion';
import { ComprobanteVenta, invalidateVentas, registrarVenta, type Venta } from '../../ventas';
import { AvisoTurno } from '../components/AvisoTurno';
import { BuscadorProductos } from '../components/BuscadorProductos';
import { CarritoTable } from '../components/CarritoTable';
import { PanelCobro } from '../components/PanelCobro';
import { PuestoVenta } from '../components/PuestoVenta';
import {
  actualizarLinea, agregarSku, puedeCobrar, quitarLinea, toRegistrarVentaPayload, totalCarrito, vueltoDe,
  type Carrito
} from '../lib/carrito';
import { describeErrorPos } from '../lib/errores-pos';
```
Cuerpo: `usePuestoTrabajo()`; `useQuery({ ...turnoActualQuery(puesto.terminalId), enabled: puesto.terminalId !== '' })` → `hayTurno = turno != null`; estado local `carrito: Carrito`, `recibido: string`, `venta: Venta | null`, `error: string | null`, `enviando: boolean`; `claveDe = useClaveIdempotencia()`. `cobrar`: arma `payload = toRegistrarVentaPayload(carrito, puesto.terminalId, puesto.almacenId, recibido)`, `setEnviando(true)`, `setError(null)`, `registrarVenta(apiClient, payload, claveDe(payload))` → `then` guarda la venta y llama `invalidateVentas`, `invalidateCaja` e `invalidateInventario` con el `queryClient`; `catch` hace `setError(describeErrorPos(e))`; `finally` `setEnviando(false)` (puede usarse `useMutation` en su lugar si resulta más limpio, manteniendo `mensajeError` con `describeErrorPos`). Cambiar de terminal (`onChange` de `PuestoVenta` cuando cambia `terminalId`) vacía carrito, monto y error. `nombreEstablecimiento` = `useEstablecimientos().find(...)?.name ?? ''`. Render: `PageHeader title="Punto de venta" context="Operaciones / Punto de venta" description="Venta en efectivo."`; `PuestoVenta`; si `puesto.terminalId === ''` el texto `Selecciona una terminal para vender.`; si hay terminal y `turno === null` (ya cargó) `AvisoTurno`; `BuscadorProductos` (`deshabilitado={puesto.terminalId === ''}`, `onElegir={(sku) => setCarrito(agregarSku(carrito, sku))}`); `CarritoTable`; `PanelCobro` con `total={totalCarrito(carrito)}`, `vuelto={vueltoDe(total, recibido)}`, `puedeCobrar={puedeCobrar({ carrito, recibido, hayTurno, hayAlmacen: puesto.almacenId !== '' })}`. El `Modal` `Venta registrada` (`open={venta !== null}`, `onClose` = nueva venta) contiene `ComprobanteVenta` y un `Button` `Nueva venta` que vacía carrito, monto, error y `venta`.

- [ ] **Step 4: Ejecutar, quitar baseline y verificación completa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/pos` → PASS. Quitar `apps/erp-web/src/features/pos/routes.tsx` de `frontend/coverage-baseline.txt`. Run: `pnpm e2e -- pos.spec.ts` → PASS en desktop, tablet y móvil; `pnpm check` → verde.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(pos): pantalla de venta en efectivo con comprobante y e2e

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Verificación manual contra el backend real

**Files:** ninguno (verificación; cualquier defecto encontrado se corrige con su test).

- [ ] **Step 1: Levantar el entorno**

Con PostgreSQL local: `cd service-botica; .\gradlew.bat :bootstrap-app:bootRun` y en otra terminal `cd frontend; pnpm dev` con el modo mock desactivado (ver `.env.development`; el proxy redirige `/api` a `http://localhost:8080`).

- [ ] **Step 2: Recorrido en navegador**

Con un usuario `ADMIN` de FARMALAB: (1) en Catálogo, editar un SKU y asignar precio de referencia; (2) en Inventario, registrar un ingreso con lote del SKU; (3) en Caja, elegir establecimiento y terminal, abrir turno con fondo 100; (4) en POS, buscar el SKU por texto y por código de barras, vender 2 unidades con monto recibido mayor al total y verificar vuelto y comprobante; repetir con stock insuficiente y comprobar el mensaje; (5) en Ventas, ver la venta en el historial y su detalle con lote consumido (FEFO); (6) en Caja, cerrar el turno declarando un monto distinto y verificar la diferencia; (7) comprobar en Inventario que el stock bajó.

- [ ] **Step 3: Registrar resultado**

Si algo falla, abrir un fix con su test (TDD) y repetir el recorrido; al terminar, dejar constancia en `.superpowers/sdd/progress.md`.

---

## Self-Review

**Cobertura del spec (sección pos):** selección de terminal y almacén recordada en el navegador con respaldo si falla `localStorage` (`usePuestoTrabajo` + `PuestoVenta`); búsqueda y escaneo por texto o código de barras con precio de referencia y stock del almacén (`BuscadorProductos`, `StockDisponible`, `esCodigoBarras`); carrito con cantidad, precio editable, total en vivo, precio manual cuando el SKU no lo trae y reglas espejo del backend (`carrito.ts`); cobro en efectivo con monto recibido y vuelto en vivo, `Idempotency-Key` estable por contenido y comprobante imprimible (`PosPage`, `ComprobanteVenta`); bloqueo del cobro sin turno con enlace a Caja (`AvisoTurno`); traducción de errores `INV_STOCK_INSUFICIENTE`, `VEN_TURNO_NO_ABIERTO`, `VEN_IDEMPOTENCY_CONFLICT`, `VEN_MONTO_RECIBIDO_INSUFICIENTE`, `VEN_TERMINAL_NO_OPERABLE`, `VEN_ALMACEN_NO_OPERABLE` (`errores-pos.ts`); e2e simulado y verificación manual contra el backend real (Task 6).

**Escaneo de placeholders:** el código de producción de `lib/` y `StockDisponible` está completo; los tests de componentes y de página se detallan como casos con valores exactos; `BuscadorProductos` y `PosPage` se describen con su estructura y estado porque dependen del estilo real de los componentes de `ui-web`. No hay "TBD"; la duplicación potencial de la regla de limpieza del almacén al cambiar de establecimiento (`PuestoVenta` y `CashRegisterPage`) se resuelve extrayendo `cambiarTerminal` a `organizacion`.

**Consistencia de tipos:** `RegistrarVentaPayload`, `Venta`, `registrarVenta(client, payload, idempotencyKey)`, `ComprobanteVenta({ venta, nombreEstablecimiento })`, `invalidateVentas` (plan 3); `turnoActualQuery`, `invalidateCaja` (plan 2); `TerminalSelector`, `usePuestoTrabajo`, `PuestoTrabajo`, `useEstablecimientos` (planes 2–3); `SkuResumen` con `unidadVentaCodigo`, `permiteVentaFraccion` y `precioVentaReferencia` (planes 1–2); `posicionesQuery` e `invalidateInventario` (Task 1).
