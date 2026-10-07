# Compras, parte 4: crear orden de compra (frontend) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar la pantalla `/compras/ordenes/nueva` para crear una orden de compra en borrador: cabecera (proveedor, destino, entrega, moneda, condiciones), editor de líneas con búsqueda de SKU, IGV sugerido al 18% y editable, y totales en vivo, integrada contra `POST /api/v1/compras/ordenes`.

**Architecture:** La lógica de líneas y totales es pura (`lib/orden-calculo.ts`, `lib/igv.ts`, `lib/orden-cabecera.ts`) y se prueba al 100%, igual que el carrito del POS; el estado vive en `NuevaOrdenPage` con `useState`, no en react-hook-form, porque las líneas son una lista dinámica con valores derivados. Los componentes son delgados (`OrdenCabeceraForm`, `BuscadorSku`, `LineasEditor`, `TotalesOrden`). El indicador de IGV del SKU (`afectoIgv`) no viene en el resumen de SKU, así que al elegir un producto se lee su detalle con `skuQuery`.

**Tech Stack:** React 19.2, React Router 8, TanStack Query, `@boticas/ui-web`, Vitest + Testing Library + MSW, Playwright.

Spec: `docs/superpowers/specs/2026-10-06-frontend-compras-design.md`. Requiere integradas las partes 2 y 3 (`useMutacionCompras`, `formatoImporte`, `Orden`, `proveedoresQuery`, `OrdenesPage`, rutas de compras).

## Global Constraints

- Todo archivo **nuevo** (código y tests) alcanza 100% de líneas, ramas, funciones y sentencias; no agregar archivos a `coverage-baseline.txt`.
- Sin comentarios en el código; sin duplicación; `forwardRef` y React Router 8 bloqueados por ESLint; las features solo se importan por su `index.ts`.
- Reglas espejo del backend para la orden: entre 1 y 200 líneas; cantidad `> 0` con hasta 4 decimales; precio `>= 0` con hasta 6 decimales; descuento e impuesto `>= 0` con hasta 2 decimales; tolerancias de exceso y defecto entre 0 y 100 con hasta 4 decimales; total de línea `>= 0` (precio × cantidad − descuento + impuesto); moneda de 3 letras mayúsculas; tipo de cambio `> 0` con hasta 6 decimales (si se omite el backend usa 1); días de crédito entero `>= 0`; condición de pago hasta 80 caracteres; observación hasta 1500; la fecha de entrega estimada no puede ser anterior a hoy. Al elegir proveedor se precargan moneda, condición de pago y días de crédito con los valores por defecto del proveedor (`PEN` y vacío si no los tiene).
- IGV sugerido: 18% de (precio × cantidad − descuento) redondeado a 2 decimales, solo para productos con `afectoIgv`; mientras la línea no se edite a mano el impuesto se recalcula al cambiar cantidad, precio o descuento; al editarlo a mano deja de recalcularse. La tasa vive solo en `lib/igv.ts` y la pantalla la rotula como sugerida (decisión POR_VALIDAR del spec).
- Un mismo SKU no se agrega dos veces (se ignora el segundo intento); la unidad de la línea es la unidad de venta del SKU (`UND` si no tiene).
- Textos exactos: ver cada tarea (los usan los tests y el e2e).
- Comandos desde `frontend/` (`pnpm.cmd` si `pnpm.ps1` está bloqueado): `pnpm exec vitest run <ruta>`, `pnpm typecheck`, `pnpm lint`, `pnpm check`, `pnpm e2e compras.spec.ts`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan difiere por nombres reales del repo (labels, fixtures), ajustar al comportamiento real sin reducir lo que verifica y anotarlo en el reporte.

Rutas abreviadas: `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: Lógica pura de líneas, IGV y cabecera; API y publicación del detalle de SKU

**Files:**
- Create: `SRC/shared/lib/redondeo.ts` y `.test.ts`
- Create: `SRC/features/compras/lib/igv.ts` y `.test.ts`
- Create: `SRC/features/compras/lib/orden-calculo.ts` y `.test.ts`
- Create: `SRC/features/compras/lib/orden-cabecera.ts` y `.test.ts`
- Modify: `SRC/features/compras/api/ordenes.types.ts`, `SRC/features/compras/api/ordenes.api.ts`, `SRC/features/compras/api/ordenes.api.test.ts`
- Modify: `SRC/features/compras/lib/use-mutacion-compras.ts`, `SRC/features/compras/lib/use-mutacion-compras.test.tsx`
- Modify: `SRC/features/catalogo/index.ts`

**Interfaces:**
- Consumes: `Proveedor` (parte 2), `Orden`, `useMutacionCompras` (parte 3), `SkuResumen` y `skuQuery`/`Sku` (`../../catalogo`).
- Produces:

```ts
export function redondear(valor: number, decimales: number): number;   // shared/lib/redondeo
export const TASA_IGV = 0.18;
export function impuestoSugerido(base: number, afectoIgv: boolean): number;
export const MAX_LINEAS_ORDEN = 200;
export type LineaBorrador = {
  skuId: string; codigoInterno: string; descripcion: string; unidadMedidaCodigo: string;
  afectoIgv: boolean; cantidad: string; precio: string; descuento: string; impuesto: string;
  impuestoManual: boolean; toleranciaExceso: string; toleranciaDefecto: string;
};
export type CambiosLinea = Partial<Pick<LineaBorrador,
  'cantidad' | 'precio' | 'descuento' | 'impuesto' | 'toleranciaExceso' | 'toleranciaDefecto'>>;
export function lineaDesdeSku(sku: SkuResumen, afectoIgv: boolean): LineaBorrador;
export function agregarSku(lineas: LineaBorrador[], sku: SkuResumen, afectoIgv: boolean): LineaBorrador[];
export function actualizarLinea(lineas: LineaBorrador[], skuId: string, cambios: CambiosLinea): LineaBorrador[];
export function quitarLinea(lineas: LineaBorrador[], skuId: string): LineaBorrador[];
export function errorLinea(linea: LineaBorrador): string | null;
export function importeBruto(linea: LineaBorrador): number;
export function totalLinea(linea: LineaBorrador): number;
export type TotalesOrden = { subtotal: number; descuento: number; impuesto: number; total: number };
export function totalesOrden(lineas: LineaBorrador[]): TotalesOrden;
export function toLineasPayload(lineas: LineaBorrador[]): LineaOrdenPayload[];
export type CabeceraOrden = {
  proveedorId: string; establecimientoDestinoId: string; fechaEntregaEstimada: string;
  moneda: string; tipoCambio: string; condicionPago: string; diasCredito: string; observacion: string;
};
export type ErroresCabecera = Partial<Record<keyof CabeceraOrden, string>>;
export const CABECERA_VACIA: CabeceraOrden;
export function cabeceraDesdeProveedor(cabecera: CabeceraOrden, proveedor: Proveedor): CabeceraOrden;
export function erroresCabecera(cabecera: CabeceraOrden, hoy: string): ErroresCabecera;
export function puedeCrear(cabecera: CabeceraOrden, lineas: LineaBorrador[], hoy: string): boolean;
export function fechaLocalISO(ahora?: Date): string;
export function toCrearOrdenPayload(cabecera: CabeceraOrden, lineas: LineaBorrador[]): CrearOrdenPayload;
// api/ordenes.types.ts
export type LineaOrdenPayload = {
  skuId: string; cantidad: number; unidadMedidaCodigo: string; precioUnitario: number;
  descuento: number; impuesto: number; toleranciaExcesoPct: number; toleranciaDefectoPct: number;
};
export type CrearOrdenPayload = {
  proveedorId: string; establecimientoDestinoId: string; fechaEntregaEstimada?: string | undefined;
  moneda: string; tipoCambio?: number | undefined; condicionPago?: string | undefined;
  diasCredito: number; observacion?: string | undefined; lineas: LineaOrdenPayload[];
};
// api/ordenes.api.ts
export function crearOrden(client: ApiClient, payload: CrearOrdenPayload): Promise<Orden>;
// lib/use-mutacion-compras.ts: onSuccess pasa a recibir el dato: (data: TData) => void
// catalogo/index.ts además exporta skuQuery
```

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/shared/lib/redondeo.test.ts`:

```ts
import { redondear } from './redondeo';

describe('redondear', () => {
  it('redondea al número de decimales indicado', () => {
    expect(redondear(1.005, 2)).toBe(1.01);
    expect(redondear(0.9999, 2)).toBe(1);
    expect(redondear(2.5, 0)).toBe(3);
    expect(redondear(1.23456, 4)).toBe(1.2346);
  });
});
```

`SRC/features/compras/lib/igv.test.ts`:

```ts
import { TASA_IGV, impuestoSugerido } from './igv';

describe('igv', () => {
  it('la tasa sugerida es 18%', () => {
    expect(TASA_IGV).toBe(0.18);
  });

  it('sugiere el 18% de la base redondeado a dos decimales para productos afectos', () => {
    expect(impuestoSugerido(5.5, true)).toBe(0.99);
    expect(impuestoSugerido(10.01, true)).toBe(1.8);
  });

  it('no sugiere impuesto para productos no afectos', () => {
    expect(impuestoSugerido(100, false)).toBe(0);
  });
});
```

`SRC/features/compras/lib/orden-calculo.test.ts`:

```ts
import { sampleSkuFraccionable, sampleSkuVenta } from '../../../test/ventas-fixtures';
import {
  MAX_LINEAS_ORDEN,
  actualizarLinea,
  agregarSku,
  errorLinea,
  importeBruto,
  lineaDesdeSku,
  quitarLinea,
  toLineasPayload,
  totalLinea,
  totalesOrden,
  type LineaBorrador
} from './orden-calculo';

const base = lineaDesdeSku(sampleSkuVenta, true);
const conPrecio = (cambios: Partial<LineaBorrador> = {}): LineaBorrador => ({
  ...base,
  precio: '5.5',
  impuesto: '0.99',
  ...cambios
});

describe('lineaDesdeSku', () => {
  it('arranca con cantidad 1, precio vacío y sin impuesto', () => {
    expect(base).toEqual({
      skuId: 'sku-0001-aaaa',
      codigoInterno: 'MED-001',
      descripcion: 'Paracetamol 500 mg',
      unidadMedidaCodigo: 'UND',
      afectoIgv: true,
      cantidad: '1',
      precio: '',
      descuento: '0',
      impuesto: '0',
      impuestoManual: false,
      toleranciaExceso: '0',
      toleranciaDefecto: '0'
    });
  });

  it('usa UND cuando el SKU no tiene unidad de venta', () => {
    expect(lineaDesdeSku({ ...sampleSkuFraccionable, unidadVentaCodigo: null }, false)).toMatchObject({
      unidadMedidaCodigo: 'UND',
      afectoIgv: false
    });
  });
});

describe('agregarSku, actualizarLinea y quitarLinea', () => {
  it('agrega un SKU nuevo', () => {
    expect(agregarSku([], sampleSkuVenta, true)).toEqual([base]);
  });

  it('ignora un SKU que ya está en la orden', () => {
    const lineas = [base];

    expect(agregarSku(lineas, sampleSkuVenta, true)).toBe(lineas);
  });

  it('no agrega más líneas del máximo permitido', () => {
    const lleno = Array.from({ length: MAX_LINEAS_ORDEN }, (_, indice) => ({
      ...base,
      skuId: `sku-${indice}`
    }));

    expect(agregarSku(lleno, sampleSkuVenta, true)).toBe(lleno);
  });

  it('recalcula el impuesto sugerido al cambiar precio, cantidad o descuento mientras no se edite a mano', () => {
    const conPrecioEscrito = actualizarLinea([base], base.skuId, { precio: '5.5' });
    expect(conPrecioEscrito[0]?.impuesto).toBe('0.99');

    const conCantidad = actualizarLinea(conPrecioEscrito, base.skuId, { cantidad: '2' });
    expect(conCantidad[0]?.impuesto).toBe('1.98');

    const conDescuento = actualizarLinea(conCantidad, base.skuId, { descuento: '1' });
    expect(conDescuento[0]?.impuesto).toBe('1.8');
  });

  it('no sugiere impuesto en productos no afectos', () => {
    const noAfecta = lineaDesdeSku(sampleSkuVenta, false);

    expect(actualizarLinea([noAfecta], noAfecta.skuId, { precio: '5.5' })[0]?.impuesto).toBe('0');
  });

  it('con un descuento inválido sugiere sobre el importe sin descuento', () => {
    const lineas = actualizarLinea([base], base.skuId, { precio: '10', descuento: 'abc' });

    expect(lineas[0]?.impuesto).toBe('1.8');
  });

  it('al editar el impuesto a mano deja de recalcularlo', () => {
    const manual = actualizarLinea([conPrecio()], base.skuId, { impuesto: '2' });
    expect(manual[0]).toMatchObject({ impuesto: '2', impuestoManual: true });

    const despues = actualizarLinea(manual, base.skuId, { cantidad: '3' });
    expect(despues[0]?.impuesto).toBe('2');
  });

  it('cambia solo la línea indicada', () => {
    const otra = { ...base, skuId: 'sku-0002-bbbb', codigoInterno: 'JAR-002' };

    const resultado = actualizarLinea([base, otra], base.skuId, { cantidad: '5' });

    expect(resultado[0]?.cantidad).toBe('5');
    expect(resultado[1]).toBe(otra);
  });

  it('quita la línea indicada', () => {
    const otra = { ...base, skuId: 'sku-0002-bbbb' };

    expect(quitarLinea([base, otra], base.skuId)).toEqual([otra]);
  });
});

describe('errorLinea', () => {
  it('no reporta error en una línea válida', () => {
    expect(errorLinea(conPrecio())).toBeNull();
    expect(errorLinea(conPrecio({ precio: '0', impuesto: '0' }))).toBeNull();
  });

  it.each(['', '0', '-1', 'abc', '1.00001', '1234567890'])('rechaza la cantidad "%s"', (cantidad) => {
    expect(errorLinea(conPrecio({ cantidad }))).toBe(
      'La cantidad debe ser mayor que cero con hasta 4 decimales.'
    );
  });

  it.each(['', '-1', 'abc', '1.0000001'])('rechaza el precio "%s"', (precio) => {
    expect(errorLinea(conPrecio({ precio }))).toBe(
      'El precio debe ser mayor o igual a cero con hasta 6 decimales.'
    );
  });

  it.each(['', '-1', '1.234'])('rechaza el descuento "%s"', (descuento) => {
    expect(errorLinea(conPrecio({ descuento }))).toBe(
      'El descuento debe ser mayor o igual a cero con hasta 2 decimales.'
    );
  });

  it.each(['', '-1', '1.234'])('rechaza el impuesto "%s"', (impuesto) => {
    expect(errorLinea(conPrecio({ impuesto }))).toBe(
      'El impuesto debe ser mayor o igual a cero con hasta 2 decimales.'
    );
  });

  it.each(['', '101', '-1', '1.00001', 'x'])('rechaza la tolerancia de exceso "%s"', (valor) => {
    expect(errorLinea(conPrecio({ toleranciaExceso: valor }))).toBe(
      'Las tolerancias deben estar entre 0 y 100 con hasta 4 decimales.'
    );
  });

  it('rechaza la tolerancia de defecto fuera de rango', () => {
    expect(errorLinea(conPrecio({ toleranciaDefecto: '100.5' }))).toBe(
      'Las tolerancias deben estar entre 0 y 100 con hasta 4 decimales.'
    );
    expect(errorLinea(conPrecio({ toleranciaDefecto: '100' }))).toBeNull();
  });

  it('rechaza un descuento que deja el total de la línea en negativo', () => {
    expect(errorLinea(conPrecio({ descuento: '10', impuesto: '0' }))).toBe(
      'El descuento no puede superar el importe de la línea más el impuesto.'
    );
  });
});

describe('totales', () => {
  it('importeBruto multiplica y redondea a dos decimales y es cero si la línea es inválida', () => {
    expect(importeBruto(conPrecio({ cantidad: '3', precio: '0.3333' }))).toBe(1);
    expect(importeBruto(conPrecio({ cantidad: '' }))).toBe(0);
    expect(importeBruto(conPrecio({ precio: '' }))).toBe(0);
  });

  it('totalLinea suma bruto menos descuento más impuesto y es cero si hay error', () => {
    expect(totalLinea(conPrecio({ cantidad: '2', descuento: '1', impuesto: '1.8' }))).toBe(11.8);
    expect(totalLinea(conPrecio({ cantidad: '' }))).toBe(0);
  });

  it('totalesOrden suma solo las líneas válidas', () => {
    const segunda = conPrecio({
      skuId: 'sku-0002-bbbb',
      cantidad: '2',
      precio: '10',
      descuento: '1',
      impuesto: '3.42'
    });
    const invalida = conPrecio({ skuId: 'sku-0003-cccc', cantidad: '' });

    expect(totalesOrden([conPrecio(), segunda, invalida])).toEqual({
      subtotal: 25.5,
      descuento: 1,
      impuesto: 4.41,
      total: 28.91
    });
    expect(totalesOrden([])).toEqual({ subtotal: 0, descuento: 0, impuesto: 0, total: 0 });
  });

  it('toLineasPayload arma el cuerpo numérico para el backend', () => {
    expect(
      toLineasPayload([conPrecio({ cantidad: '2', toleranciaExceso: '5', toleranciaDefecto: '2.5' })])
    ).toEqual([
      {
        skuId: 'sku-0001-aaaa',
        cantidad: 2,
        unidadMedidaCodigo: 'UND',
        precioUnitario: 5.5,
        descuento: 0,
        impuesto: 0.99,
        toleranciaExcesoPct: 5,
        toleranciaDefectoPct: 2.5
      }
    ]);
  });
});
```

`SRC/features/compras/lib/orden-cabecera.test.ts`:

```ts
import { sampleProveedor } from '../../../test/compras-fixtures';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { lineaDesdeSku } from './orden-calculo';
import {
  CABECERA_VACIA,
  cabeceraDesdeProveedor,
  erroresCabecera,
  fechaLocalISO,
  puedeCrear,
  toCrearOrdenPayload
} from './orden-cabecera';

const HOY = '2026-10-06';
const completa = {
  ...CABECERA_VACIA,
  proveedorId: 'prov-1',
  establecimientoDestinoId: 'est-1',
  moneda: 'PEN',
  diasCredito: '0'
};
const lineaValida = { ...lineaDesdeSku(sampleSkuVenta, true), precio: '5.5', impuesto: '0.99' };

describe('cabecera de la orden', () => {
  it('la cabecera vacía arranca en soles sin proveedor ni destino', () => {
    expect(CABECERA_VACIA).toEqual({
      proveedorId: '',
      establecimientoDestinoId: '',
      fechaEntregaEstimada: '',
      moneda: 'PEN',
      tipoCambio: '',
      condicionPago: '',
      diasCredito: '0',
      observacion: ''
    });
  });

  it('al elegir proveedor precarga moneda, condición de pago y días de crédito', () => {
    expect(cabeceraDesdeProveedor(CABECERA_VACIA, sampleProveedor)).toEqual({
      ...CABECERA_VACIA,
      proveedorId: 'prov-1',
      moneda: 'PEN',
      condicionPago: 'CREDITO 30',
      diasCredito: '30'
    });
  });

  it('usa PEN y condición vacía cuando el proveedor no trae valores por defecto', () => {
    expect(
      cabeceraDesdeProveedor(CABECERA_VACIA, {
        ...sampleProveedor,
        monedaDefault: null,
        condicionPagoDefault: null
      })
    ).toMatchObject({ moneda: 'PEN', condicionPago: '' });
  });

  it('fechaLocalISO usa la fecha local con ceros a la izquierda', () => {
    expect(fechaLocalISO(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(fechaLocalISO(new Date(2026, 9, 6))).toBe('2026-10-06');
  });

  it('fechaLocalISO usa la fecha de hoy por defecto', () => {
    expect(fechaLocalISO()).toBe(fechaLocalISO(new Date()));
  });
});

describe('erroresCabecera', () => {
  it('no reporta errores en una cabecera completa', () => {
    expect(erroresCabecera(completa, HOY)).toEqual({});
  });

  it('exige proveedor y destino', () => {
    expect(erroresCabecera(CABECERA_VACIA, HOY)).toEqual({
      proveedorId: 'Selecciona un proveedor.',
      establecimientoDestinoId: 'Selecciona el establecimiento de destino.'
    });
  });

  it('rechaza una fecha de entrega anterior a hoy y acepta hoy y el futuro', () => {
    expect(erroresCabecera({ ...completa, fechaEntregaEstimada: '2026-10-05' }, HOY)).toEqual({
      fechaEntregaEstimada: 'La fecha de entrega no puede ser anterior a hoy.'
    });
    expect(erroresCabecera({ ...completa, fechaEntregaEstimada: HOY }, HOY)).toEqual({});
    expect(erroresCabecera({ ...completa, fechaEntregaEstimada: '2026-10-20' }, HOY)).toEqual({});
  });

  it('valida la moneda, el tipo de cambio, la condición, los días y la observación', () => {
    expect(erroresCabecera({ ...completa, moneda: 'pen' }, HOY)).toEqual({
      moneda: 'La moneda debe ser un código de 3 letras mayúsculas.'
    });
    expect(erroresCabecera({ ...completa, tipoCambio: '0' }, HOY)).toEqual({
      tipoCambio: 'El tipo de cambio debe ser mayor que cero con hasta 6 decimales.'
    });
    expect(erroresCabecera({ ...completa, tipoCambio: '3.8123456' }, HOY)).toHaveProperty('tipoCambio');
    expect(erroresCabecera({ ...completa, tipoCambio: '3.81' }, HOY)).toEqual({});
    expect(erroresCabecera({ ...completa, condicionPago: 'a'.repeat(81) }, HOY)).toEqual({
      condicionPago: 'La condición de pago no debe exceder 80 caracteres.'
    });
    expect(erroresCabecera({ ...completa, diasCredito: '-1' }, HOY)).toEqual({
      diasCredito: 'Los días de crédito deben ser un entero mayor o igual a 0.'
    });
    expect(erroresCabecera({ ...completa, observacion: 'a'.repeat(1501) }, HOY)).toEqual({
      observacion: 'La observación no debe exceder 1500 caracteres.'
    });
  });
});

describe('puedeCrear', () => {
  it('exige cabecera válida y al menos una línea válida', () => {
    expect(puedeCrear(completa, [lineaValida], HOY)).toBe(true);
    expect(puedeCrear(CABECERA_VACIA, [lineaValida], HOY)).toBe(false);
    expect(puedeCrear(completa, [], HOY)).toBe(false);
    expect(puedeCrear(completa, [{ ...lineaValida, precio: '' }], HOY)).toBe(false);
  });
});

describe('toCrearOrdenPayload', () => {
  it('omite los opcionales vacíos y convierte los números', () => {
    expect(toCrearOrdenPayload(completa, [lineaValida])).toEqual({
      proveedorId: 'prov-1',
      establecimientoDestinoId: 'est-1',
      fechaEntregaEstimada: undefined,
      moneda: 'PEN',
      tipoCambio: undefined,
      condicionPago: undefined,
      diasCredito: 0,
      observacion: undefined,
      lineas: [
        {
          skuId: 'sku-0001-aaaa',
          cantidad: 1,
          unidadMedidaCodigo: 'UND',
          precioUnitario: 5.5,
          descuento: 0,
          impuesto: 0.99,
          toleranciaExcesoPct: 0,
          toleranciaDefectoPct: 0
        }
      ]
    });
  });

  it('incluye fecha, tipo de cambio, condición y observación cuando se llenaron', () => {
    const payload = toCrearOrdenPayload(
      {
        ...completa,
        fechaEntregaEstimada: '2026-10-20',
        tipoCambio: ' 3.81 ',
        condicionPago: ' CREDITO 30 ',
        diasCredito: '30',
        observacion: ' Reposición '
      },
      [lineaValida]
    );

    expect(payload).toMatchObject({
      fechaEntregaEstimada: '2026-10-20',
      tipoCambio: 3.81,
      condicionPago: 'CREDITO 30',
      diasCredito: 30,
      observacion: 'Reposición'
    });
  });
});
```

En `SRC/features/compras/api/ordenes.api.test.ts` agregar `crearOrden` al import y este test dentro del `describe`:

```ts
  it('crearOrden hace POST con el cuerpo', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/compras/ordenes', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const payload = {
      proveedorId: 'prov-1',
      establecimientoDestinoId: 'est-1',
      moneda: 'PEN',
      diasCredito: 30,
      lineas: [
        {
          skuId: 'sku-0001-aaaa',
          cantidad: 10,
          unidadMedidaCodigo: 'UND',
          precioUnitario: 5.5,
          descuento: 0,
          impuesto: 9.9,
          toleranciaExcesoPct: 0,
          toleranciaDefectoPct: 0
        }
      ]
    };

    await expect(crearOrden(client, payload)).resolves.toEqual(sampleOrden);
    expect(body).toEqual(payload);
  });
```

En `SRC/features/compras/lib/use-mutacion-compras.test.tsx` cambiar el test `al tener éxito invalida compras y llama al callback` para verificar el dato:

```tsx
    await waitFor(() => expect(onSuccess).toHaveBeenCalledWith('ok'));
```
(reemplaza la línea `await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));` de ese test).

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/shared/lib/redondeo.test.ts apps/erp-web/src/features/compras`
Expected: FAIL (módulos inexistentes, `crearOrden` no exportado, callback sin dato).

- [ ] **Step 3: Implementar**

`SRC/shared/lib/redondeo.ts`:

```ts
export function redondear(valor: number, decimales: number): number {
  const factor = 10 ** decimales;
  return Math.round((valor + Number.EPSILON) * factor) / factor;
}
```

`SRC/features/compras/lib/igv.ts`:

```ts
import { redondear } from '../../../shared/lib/redondeo';

export const TASA_IGV = 0.18;

export const impuestoSugerido = (base: number, afectoIgv: boolean): number =>
  afectoIgv ? redondear(base * TASA_IGV, 2) : 0;
```

`SRC/features/compras/lib/orden-calculo.ts`:

```ts
import { redondear } from '../../../shared/lib/redondeo';
import type { SkuResumen } from '../../catalogo';
import type { LineaOrdenPayload } from '../api/ordenes.types';
import { impuestoSugerido } from './igv';

export const MAX_LINEAS_ORDEN = 200;

export type LineaBorrador = {
  skuId: string;
  codigoInterno: string;
  descripcion: string;
  unidadMedidaCodigo: string;
  afectoIgv: boolean;
  cantidad: string;
  precio: string;
  descuento: string;
  impuesto: string;
  impuestoManual: boolean;
  toleranciaExceso: string;
  toleranciaDefecto: string;
};

export type CambiosLinea = Partial<
  Pick<
    LineaBorrador,
    'cantidad' | 'precio' | 'descuento' | 'impuesto' | 'toleranciaExceso' | 'toleranciaDefecto'
  >
>;

export type TotalesOrden = {
  subtotal: number;
  descuento: number;
  impuesto: number;
  total: number;
};

const PATRON_CANTIDAD = /^\d{1,9}(\.\d{1,4})?$/;
const PATRON_PRECIO = /^\d{1,10}(\.\d{1,6})?$/;
const PATRON_MONTO = /^\d{1,10}(\.\d{1,2})?$/;
const PATRON_TOLERANCIA = /^\d{1,3}(\.\d{1,4})?$/;

const esCantidad = (texto: string) => PATRON_CANTIDAD.test(texto) && Number(texto) > 0;
const esPrecio = (texto: string) => PATRON_PRECIO.test(texto);
const esMonto = (texto: string) => PATRON_MONTO.test(texto);
const esTolerancia = (texto: string) => PATRON_TOLERANCIA.test(texto) && Number(texto) <= 100;

export const importeBruto = (linea: LineaBorrador): number =>
  esCantidad(linea.cantidad) && esPrecio(linea.precio)
    ? redondear(Number(linea.cantidad) * Number(linea.precio), 2)
    : 0;

const baseImponible = (linea: LineaBorrador): number => {
  const descuento = esMonto(linea.descuento) ? Number(linea.descuento) : 0;
  return Math.max(0, redondear(importeBruto(linea) - descuento, 2));
};

const conImpuestoSugerido = (linea: LineaBorrador): LineaBorrador =>
  linea.impuestoManual
    ? linea
    : { ...linea, impuesto: String(impuestoSugerido(baseImponible(linea), linea.afectoIgv)) };

export const lineaDesdeSku = (sku: SkuResumen, afectoIgv: boolean): LineaBorrador => ({
  skuId: sku.id,
  codigoInterno: sku.codigoInterno,
  descripcion: sku.descripcionComercial,
  unidadMedidaCodigo: sku.unidadVentaCodigo ?? 'UND',
  afectoIgv,
  cantidad: '1',
  precio: '',
  descuento: '0',
  impuesto: '0',
  impuestoManual: false,
  toleranciaExceso: '0',
  toleranciaDefecto: '0'
});

export function agregarSku(
  lineas: LineaBorrador[],
  sku: SkuResumen,
  afectoIgv: boolean
): LineaBorrador[] {
  if (lineas.some(({ skuId }) => skuId === sku.id) || lineas.length >= MAX_LINEAS_ORDEN) {
    return lineas;
  }
  return [...lineas, lineaDesdeSku(sku, afectoIgv)];
}

export const actualizarLinea = (
  lineas: LineaBorrador[],
  skuId: string,
  cambios: CambiosLinea
): LineaBorrador[] =>
  lineas.map((linea) =>
    linea.skuId === skuId
      ? conImpuestoSugerido({
          ...linea,
          ...cambios,
          impuestoManual: linea.impuestoManual || cambios.impuesto !== undefined
        })
      : linea
  );

export const quitarLinea = (lineas: LineaBorrador[], skuId: string): LineaBorrador[] =>
  lineas.filter((linea) => linea.skuId !== skuId);

const importeSinValidar = (linea: LineaBorrador): number =>
  importeBruto(linea) - Number(linea.descuento) + Number(linea.impuesto);

export function errorLinea(linea: LineaBorrador): string | null {
  if (!esCantidad(linea.cantidad)) {
    return 'La cantidad debe ser mayor que cero con hasta 4 decimales.';
  }
  if (!esPrecio(linea.precio)) {
    return 'El precio debe ser mayor o igual a cero con hasta 6 decimales.';
  }
  if (!esMonto(linea.descuento)) {
    return 'El descuento debe ser mayor o igual a cero con hasta 2 decimales.';
  }
  if (!esMonto(linea.impuesto)) {
    return 'El impuesto debe ser mayor o igual a cero con hasta 2 decimales.';
  }
  if (!esTolerancia(linea.toleranciaExceso) || !esTolerancia(linea.toleranciaDefecto)) {
    return 'Las tolerancias deben estar entre 0 y 100 con hasta 4 decimales.';
  }
  return importeSinValidar(linea) < 0
    ? 'El descuento no puede superar el importe de la línea más el impuesto.'
    : null;
}

export const totalLinea = (linea: LineaBorrador): number =>
  errorLinea(linea) === null ? redondear(importeSinValidar(linea), 2) : 0;

export function totalesOrden(lineas: LineaBorrador[]): TotalesOrden {
  const validas = lineas.filter((linea) => errorLinea(linea) === null);
  const sumar = (valor: (linea: LineaBorrador) => number) =>
    redondear(
      validas.reduce((acumulado, linea) => acumulado + valor(linea), 0),
      2
    );
  return {
    subtotal: sumar(importeBruto),
    descuento: sumar((linea) => Number(linea.descuento)),
    impuesto: sumar((linea) => Number(linea.impuesto)),
    total: sumar(totalLinea)
  };
}

export const toLineasPayload = (lineas: LineaBorrador[]): LineaOrdenPayload[] =>
  lineas.map((linea) => ({
    skuId: linea.skuId,
    cantidad: Number(linea.cantidad),
    unidadMedidaCodigo: linea.unidadMedidaCodigo,
    precioUnitario: Number(linea.precio),
    descuento: Number(linea.descuento),
    impuesto: Number(linea.impuesto),
    toleranciaExcesoPct: Number(linea.toleranciaExceso),
    toleranciaDefectoPct: Number(linea.toleranciaDefecto)
  }));
```

`SRC/features/compras/lib/orden-cabecera.ts`:

```ts
import { emptyToUndefined } from '../../../shared/lib/form-values';
import type { CrearOrdenPayload } from '../api/ordenes.types';
import type { Proveedor } from '../api/proveedores.types';
import { errorLinea, toLineasPayload, type LineaBorrador } from './orden-calculo';

export type CabeceraOrden = {
  proveedorId: string;
  establecimientoDestinoId: string;
  fechaEntregaEstimada: string;
  moneda: string;
  tipoCambio: string;
  condicionPago: string;
  diasCredito: string;
  observacion: string;
};

export type ErroresCabecera = Partial<Record<keyof CabeceraOrden, string>>;

export const CABECERA_VACIA: CabeceraOrden = {
  proveedorId: '',
  establecimientoDestinoId: '',
  fechaEntregaEstimada: '',
  moneda: 'PEN',
  tipoCambio: '',
  condicionPago: '',
  diasCredito: '0',
  observacion: ''
};

const PATRON_TIPO_CAMBIO = /^\d{1,10}(\.\d{1,6})?$/;

export const cabeceraDesdeProveedor = (
  cabecera: CabeceraOrden,
  proveedor: Proveedor
): CabeceraOrden => ({
  ...cabecera,
  proveedorId: proveedor.id,
  moneda: proveedor.monedaDefault ?? 'PEN',
  condicionPago: proveedor.condicionPagoDefault ?? '',
  diasCredito: String(proveedor.diasCreditoDefault)
});

export function fechaLocalISO(ahora: Date = new Date()): string {
  const mes = String(ahora.getMonth() + 1).padStart(2, '0');
  const dia = String(ahora.getDate()).padStart(2, '0');
  return `${ahora.getFullYear()}-${mes}-${dia}`;
}

const tipoCambioInvalido = (texto: string) =>
  texto !== '' && !(PATRON_TIPO_CAMBIO.test(texto) && Number(texto) > 0);

export function erroresCabecera(cabecera: CabeceraOrden, hoy: string): ErroresCabecera {
  const errores: ErroresCabecera = {};
  if (cabecera.proveedorId === '') errores.proveedorId = 'Selecciona un proveedor.';
  if (cabecera.establecimientoDestinoId === '') {
    errores.establecimientoDestinoId = 'Selecciona el establecimiento de destino.';
  }
  if (cabecera.fechaEntregaEstimada !== '' && cabecera.fechaEntregaEstimada < hoy) {
    errores.fechaEntregaEstimada = 'La fecha de entrega no puede ser anterior a hoy.';
  }
  if (!/^[A-Z]{3}$/.test(cabecera.moneda)) {
    errores.moneda = 'La moneda debe ser un código de 3 letras mayúsculas.';
  }
  if (tipoCambioInvalido(cabecera.tipoCambio.trim())) {
    errores.tipoCambio = 'El tipo de cambio debe ser mayor que cero con hasta 6 decimales.';
  }
  if (cabecera.condicionPago.length > 80) {
    errores.condicionPago = 'La condición de pago no debe exceder 80 caracteres.';
  }
  if (!/^\d{1,4}$/.test(cabecera.diasCredito)) {
    errores.diasCredito = 'Los días de crédito deben ser un entero mayor o igual a 0.';
  }
  if (cabecera.observacion.length > 1500) {
    errores.observacion = 'La observación no debe exceder 1500 caracteres.';
  }
  return errores;
}

export const puedeCrear = (
  cabecera: CabeceraOrden,
  lineas: LineaBorrador[],
  hoy: string
): boolean =>
  Object.keys(erroresCabecera(cabecera, hoy)).length === 0 &&
  lineas.length > 0 &&
  lineas.every((linea) => errorLinea(linea) === null);

const tipoCambioOpcional = (texto: string): number | undefined => {
  const limpio = emptyToUndefined(texto);
  return limpio === undefined ? undefined : Number(limpio);
};

export const toCrearOrdenPayload = (
  cabecera: CabeceraOrden,
  lineas: LineaBorrador[]
): CrearOrdenPayload => ({
  proveedorId: cabecera.proveedorId,
  establecimientoDestinoId: cabecera.establecimientoDestinoId,
  fechaEntregaEstimada: emptyToUndefined(cabecera.fechaEntregaEstimada),
  moneda: cabecera.moneda,
  tipoCambio: tipoCambioOpcional(cabecera.tipoCambio),
  condicionPago: emptyToUndefined(cabecera.condicionPago),
  diasCredito: Number(cabecera.diasCredito),
  observacion: emptyToUndefined(cabecera.observacion),
  lineas: toLineasPayload(lineas)
});
```

`SRC/features/compras/api/ordenes.types.ts`: agregar al final:

```ts
export type LineaOrdenPayload = {
  skuId: string;
  cantidad: number;
  unidadMedidaCodigo: string;
  precioUnitario: number;
  descuento: number;
  impuesto: number;
  toleranciaExcesoPct: number;
  toleranciaDefectoPct: number;
};

export type CrearOrdenPayload = {
  proveedorId: string;
  establecimientoDestinoId: string;
  fechaEntregaEstimada?: string | undefined;
  moneda: string;
  tipoCambio?: number | undefined;
  condicionPago?: string | undefined;
  diasCredito: number;
  observacion?: string | undefined;
  lineas: LineaOrdenPayload[];
};
```

`SRC/features/compras/api/ordenes.api.ts`: ampliar el import de tipos a `import type { AnularOrdenPayload, CrearOrdenPayload, Orden, OrdenResumen } from './ordenes.types';` y agregar:

```ts
export function crearOrden(client: ApiClient, payload: CrearOrdenPayload): Promise<Orden> {
  return client.post<Orden, CrearOrdenPayload>('/compras/ordenes', payload);
}
```

`SRC/features/compras/lib/use-mutacion-compras.ts`: reemplazar por:

```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { invalidateCompras } from '../api/invalidate';
import { describeErrorCompras } from './errores-compras';

export function useMutacionCompras<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  onSuccess?: (data: TData) => void
) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn,
    onSuccess: (data) => {
      onSuccess?.(data);
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

`SRC/features/catalogo/index.ts`: cambiar `export { skusQuery } from './api/skus.api';` por:

```ts
export { skusQuery, skuQuery } from './api/skus.api';
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/shared/lib apps/erp-web/src/features/compras apps/erp-web/src/features/catalogo && pnpm typecheck && pnpm lint`
Expected: PASS con 100% en los archivos nuevos. Si `redondear(1.005, 2)` da `1` en tu entorno, ajustar solo ese caso del test a `redondear(1.006, 2)` → `1.01` y anotarlo.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): logica pura de lineas, IGV sugerido y cabecera de la orden

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Componentes de la orden nueva

**Files:**
- Create: `SRC/features/compras/components/OrdenCabeceraForm.tsx` y `.test.tsx`
- Create: `SRC/features/compras/components/BuscadorSku.tsx` y `.test.tsx`
- Create: `SRC/features/compras/components/LineasEditor.tsx` y `.test.tsx`
- Create: `SRC/features/compras/components/TotalesOrden.tsx` y `.test.tsx`

**Interfaces:**
- Consumes: Task 1 (`CabeceraOrden`, `ErroresCabecera`, `LineaBorrador`, `CambiosLinea`, `errorLinea`, `totalLinea`, `TotalesOrden`), `Proveedor`, `EstablishmentStructure` (`../../organizacion`), `skusQuery`, `SkuResumen` (`../../catalogo`), `formatoImporte`.
- Produces:
  - `OrdenCabeceraForm({ valores, errores, proveedores, establecimientos, onCambiar })` con `onCambiar(campo: keyof CabeceraOrden, valor: string)`.
  - `BuscadorSku({ onElegir })` con `onElegir(sku: SkuResumen)`.
  - `LineasEditor({ lineas, moneda, onCambiar, onQuitar })` con `onCambiar(skuId: string, cambios: CambiosLinea)` y `onQuitar(skuId: string)`.
  - `TotalesOrden({ totales, moneda })`.
- Textos exactos: cabecera con etiquetas `Proveedor` (opción `Selecciona un proveedor`), `Establecimiento de destino` (opción `Selecciona un establecimiento`), `Fecha de entrega estimada`, `Moneda`, `Tipo de cambio`, `Condición de pago`, `Días de crédito`, `Observación`; buscador con etiqueta `Buscar producto`, placeholder `Código o descripción`, botón `Buscar`, vacío `No se encontraron productos.`, error `No se pudo buscar productos.` y por resultado `{codigoInterno} — {descripcionComercial}` con botón `Agregar` (nombre accesible `Agregar {codigoInterno}`); editor con columnas `Producto`, `Cantidad`, `Precio`, `Descuento`, `Impuesto`, `Total`, `Exceso %`, `Defecto %`, `Acciones`, inputs con nombre accesible `Cantidad de {codigo}`, `Precio de {codigo}`, `Descuento de {codigo}`, `Impuesto de {codigo}`, `Tolerancia de exceso de {codigo}` y `Tolerancia de defecto de {codigo}`, botón `Quitar {codigo}`, vacío `Aún no agregaste productos.` y nota `El impuesto se sugiere al 18% en los productos afectos a IGV y puedes corregirlo.`; totales con `Subtotal`, `Descuento`, `Impuesto` y `Total`.

- [ ] **Step 1: Escribir los tests que fallan**

`OrdenCabeceraForm.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { CABECERA_VACIA } from '../lib/orden-cabecera';
import { OrdenCabeceraForm } from './OrdenCabeceraForm';

const establecimientos = (sampleEstructura.companies[0]?.establishments ?? []).slice(0, 2);

function renderForm(overrides: Partial<Parameters<typeof OrdenCabeceraForm>[0]> = {}) {
  const onCambiar = vi.fn();
  render(
    <OrdenCabeceraForm
      valores={CABECERA_VACIA}
      errores={{}}
      proveedores={[sampleProveedor]}
      establecimientos={establecimientos}
      onCambiar={onCambiar}
      {...overrides}
    />
  );
  return { onCambiar, user: userEvent.setup() };
}

describe('OrdenCabeceraForm', () => {
  it('ofrece los proveedores y los establecimientos', () => {
    renderForm();

    expect(screen.getByRole('option', { name: 'Laboratorios Perú SAC' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Botica Central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Botica Norte' })).toBeInTheDocument();
    expect(screen.getByLabelText('Moneda')).toHaveValue('PEN');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('0');
  });

  it('notifica cada campo con su nombre y valor', async () => {
    const { onCambiar, user } = renderForm();

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'prov-1');
    await user.selectOptions(screen.getByLabelText('Establecimiento de destino'), 'est-1');
    await user.type(screen.getByLabelText('Fecha de entrega estimada'), '2026-10-20');
    await user.type(screen.getByLabelText('Tipo de cambio'), '3');
    await user.type(screen.getByLabelText('Condición de pago'), 'C');
    await user.type(screen.getByLabelText('Observación'), 'O');

    expect(onCambiar).toHaveBeenCalledWith('proveedorId', 'prov-1');
    expect(onCambiar).toHaveBeenCalledWith('establecimientoDestinoId', 'est-1');
    expect(onCambiar).toHaveBeenCalledWith('fechaEntregaEstimada', '2026-10-20');
    expect(onCambiar).toHaveBeenCalledWith('tipoCambio', '3');
    expect(onCambiar).toHaveBeenCalledWith('condicionPago', 'C');
    expect(onCambiar).toHaveBeenCalledWith('observacion', 'O');
  });

  it('notifica los cambios de moneda y días de crédito', async () => {
    const { onCambiar, user } = renderForm();

    await user.type(screen.getByLabelText('Moneda'), 'X');
    await user.type(screen.getByLabelText('Días de crédito'), '5');

    expect(onCambiar).toHaveBeenCalledWith('moneda', 'PENX');
    expect(onCambiar).toHaveBeenCalledWith('diasCredito', '05');
  });

  it('muestra los errores recibidos bajo cada campo', () => {
    renderForm({
      errores: {
        proveedorId: 'Selecciona un proveedor.',
        establecimientoDestinoId: 'Selecciona el establecimiento de destino.'
      }
    });

    expect(screen.getByText('Selecciona un proveedor.')).toBeInTheDocument();
    expect(screen.getByText('Selecciona el establecimiento de destino.')).toBeInTheDocument();
  });
});
```

`BuscadorSku.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { BuscadorSku } from './BuscadorSku';

const skusUrl = '*/api/v1/catalogo/skus';

function renderBuscador() {
  const onElegir = vi.fn();
  const Pantalla = () => <BuscadorSku onElegir={onElegir} />;
  return { onElegir, ...renderRoute('/x', Pantalla, '/x') };
}

describe('BuscadorSku', () => {
  it('no consulta antes de buscar', () => {
    const consulta = vi.fn();
    server.use(
      http.get(skusUrl, () => {
        consulta();
        return HttpResponse.json(pagina([]));
      })
    );

    renderBuscador();

    expect(consulta).not.toHaveBeenCalled();
  });

  it('busca activos por el texto y entrega el SKU elegido', async () => {
    const parametros = vi.fn();
    server.use(
      http.get(skusUrl, ({ request }) => {
        parametros(new URL(request.url).searchParams);
        return HttpResponse.json(pagina([sampleSkuVenta]));
      })
    );
    const { onElegir, user } = renderBuscador();

    await user.type(screen.getByLabelText('Buscar producto'), '  paracetamol ');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));
    await user.click(await screen.findByRole('button', { name: 'Agregar MED-001' }));

    expect(screen.getByText('MED-001 — Paracetamol 500 mg')).toBeInTheDocument();
    expect(parametros.mock.calls[0]?.[0].get('q')).toBe('paracetamol');
    expect(parametros.mock.calls[0]?.[0].get('estado')).toBe('ACTIVO');
    expect(onElegir).toHaveBeenCalledWith(sampleSkuVenta);
  });

  it('muestra el vacío y el error de búsqueda', async () => {
    server.use(http.get(skusUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderBuscador();

    await primera.user.type(screen.getByLabelText('Buscar producto'), 'zzz');
    await primera.user.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByText('No se encontraron productos.')).toBeInTheDocument();
    primera.unmount();

    server.use(http.get(skusUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    const segunda = renderBuscador();
    await segunda.user.type(screen.getByLabelText('Buscar producto'), 'zzz');
    await segunda.user.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByText('No se pudo buscar productos.')).toBeInTheDocument();
  });
});
```

`LineasEditor.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { formatoMoneda } from '../../../shared/lib/format';
import { lineaDesdeSku } from '../lib/orden-calculo';
import { LineasEditor } from './LineasEditor';

const linea = { ...lineaDesdeSku(sampleSkuVenta, true), precio: '5.5', impuesto: '0.99' };

function renderEditor(lineas = [linea]) {
  const onCambiar = vi.fn();
  const onQuitar = vi.fn();
  render(<LineasEditor lineas={lineas} moneda="PEN" onCambiar={onCambiar} onQuitar={onQuitar} />);
  return { onCambiar, onQuitar, user: userEvent.setup() };
}

describe('LineasEditor', () => {
  it('muestra el vacío y la nota del IGV sugerido', () => {
    renderEditor([]);

    expect(screen.getByText('Aún no agregaste productos.')).toBeInTheDocument();
    expect(
      screen.getByText('El impuesto se sugiere al 18% en los productos afectos a IGV y puedes corregirlo.')
    ).toBeInTheDocument();
  });

  it('muestra los valores de la línea y su total', () => {
    renderEditor();

    expect(screen.getByText('MED-001 — Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByLabelText('Cantidad de MED-001')).toHaveValue('1');
    expect(screen.getByLabelText('Precio de MED-001')).toHaveValue('5.5');
    expect(screen.getByLabelText('Descuento de MED-001')).toHaveValue('0');
    expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('0.99');
    expect(screen.getByLabelText('Tolerancia de exceso de MED-001')).toHaveValue('0');
    expect(screen.getByLabelText('Tolerancia de defecto de MED-001')).toHaveValue('0');
    expect(screen.getByText(formatoMoneda(6.49))).toBeInTheDocument();
  });

  it('notifica cada cambio con el SKU y el campo editado', async () => {
    const { onCambiar, user } = renderEditor();

    await user.type(screen.getByLabelText('Cantidad de MED-001'), '2');
    await user.type(screen.getByLabelText('Precio de MED-001'), '1');
    await user.type(screen.getByLabelText('Descuento de MED-001'), '1');
    await user.type(screen.getByLabelText('Impuesto de MED-001'), '1');
    await user.type(screen.getByLabelText('Tolerancia de exceso de MED-001'), '1');
    await user.type(screen.getByLabelText('Tolerancia de defecto de MED-001'), '1');

    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { cantidad: '12' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { precio: '5.51' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { descuento: '01' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { impuesto: '0.991' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { toleranciaExceso: '01' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { toleranciaDefecto: '01' });
  });

  it('quita una línea', async () => {
    const { onQuitar, user } = renderEditor();

    await user.click(screen.getByRole('button', { name: 'Quitar MED-001' }));

    expect(onQuitar).toHaveBeenCalledWith('sku-0001-aaaa');
  });

  it('muestra el error de una línea inválida y total cero', () => {
    renderEditor([{ ...linea, cantidad: '' }]);

    expect(screen.getByRole('alert')).toHaveTextContent(
      'La cantidad debe ser mayor que cero con hasta 4 decimales.'
    );
    expect(screen.getByText(formatoMoneda(0))).toBeInTheDocument();
  });
});
```

`TotalesOrden.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { formatoMoneda } from '../../../shared/lib/format';
import { TotalesOrden } from './TotalesOrden';

describe('TotalesOrden', () => {
  it('muestra subtotal, descuento, impuesto y total en la moneda de la orden', () => {
    render(
      <TotalesOrden totales={{ subtotal: 55, descuento: 5, impuesto: 9, total: 59 }} moneda="PEN" />
    );

    expect(screen.getByText('Subtotal').nextElementSibling).toHaveTextContent(formatoMoneda(55));
    expect(screen.getByText('Descuento').nextElementSibling).toHaveTextContent(formatoMoneda(5));
    expect(screen.getByText('Impuesto').nextElementSibling).toHaveTextContent(formatoMoneda(9));
    expect(screen.getByText('Total').nextElementSibling).toHaveTextContent(formatoMoneda(59));
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/components`
Expected: FAIL (componentes inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/components/OrdenCabeceraForm.tsx`:

```tsx
import { Card } from '@boticas/ui-web';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import type { EstablishmentStructure } from '../../organizacion';
import type { Proveedor } from '../api/proveedores.types';
import type { CabeceraOrden, ErroresCabecera } from '../lib/orden-cabecera';

type OrdenCabeceraFormProps = {
  valores: CabeceraOrden;
  errores: ErroresCabecera;
  proveedores: Proveedor[];
  establecimientos: EstablishmentStructure[];
  onCambiar: (campo: keyof CabeceraOrden, valor: string) => void;
};

export function OrdenCabeceraForm({
  valores,
  errores,
  proveedores,
  establecimientos,
  onCambiar
}: OrdenCabeceraFormProps) {
  return (
    <Card className="grid gap-4 p-6 sm:grid-cols-2">
      <SelectField
        id="orden-proveedor"
        label="Proveedor"
        error={errores.proveedorId}
        value={valores.proveedorId}
        onChange={(event) => onCambiar('proveedorId', event.target.value)}
      >
        <option value="">Selecciona un proveedor</option>
        {proveedores.map(({ id, razonSocial }) => (
          <option key={id} value={id}>
            {razonSocial}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="orden-destino"
        label="Establecimiento de destino"
        error={errores.establecimientoDestinoId}
        value={valores.establecimientoDestinoId}
        onChange={(event) => onCambiar('establecimientoDestinoId', event.target.value)}
      >
        <option value="">Selecciona un establecimiento</option>
        {establecimientos.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <TextField
        id="orden-entrega"
        label="Fecha de entrega estimada"
        type="date"
        error={errores.fechaEntregaEstimada}
        value={valores.fechaEntregaEstimada}
        onChange={(event) => onCambiar('fechaEntregaEstimada', event.target.value)}
      />
      <TextField
        id="orden-moneda"
        label="Moneda"
        error={errores.moneda}
        value={valores.moneda}
        onChange={(event) => onCambiar('moneda', event.target.value)}
      />
      <TextField
        id="orden-tipo-cambio"
        label="Tipo de cambio"
        inputMode="decimal"
        error={errores.tipoCambio}
        value={valores.tipoCambio}
        onChange={(event) => onCambiar('tipoCambio', event.target.value)}
      />
      <TextField
        id="orden-condicion-pago"
        label="Condición de pago"
        error={errores.condicionPago}
        value={valores.condicionPago}
        onChange={(event) => onCambiar('condicionPago', event.target.value)}
      />
      <TextField
        id="orden-dias-credito"
        label="Días de crédito"
        inputMode="numeric"
        error={errores.diasCredito}
        value={valores.diasCredito}
        onChange={(event) => onCambiar('diasCredito', event.target.value)}
      />
      <TextField
        id="orden-observacion"
        label="Observación"
        error={errores.observacion}
        value={valores.observacion}
        onChange={(event) => onCambiar('observacion', event.target.value)}
      />
    </Card>
  );
}
```

`SRC/features/compras/components/BuscadorSku.tsx`:

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Plus, Search } from 'lucide-react';
import { Button, Card } from '@boticas/ui-web';
import { TextField } from '../../../shared/components/FormFields';
import { skusQuery, type SkuResumen } from '../../catalogo';

type BuscadorSkuProps = { onElegir: (sku: SkuResumen) => void };

export function BuscadorSku({ onElegir }: BuscadorSkuProps) {
  const [texto, setTexto] = useState('');
  const [termino, setTermino] = useState('');
  const { data, isError, isFetching } = useQuery({
    ...skusQuery({ q: termino, estado: 'ACTIVO', size: 10 }),
    enabled: termino !== ''
  });
  const resultados = data?.items ?? [];
  const buscando = termino !== '';
  const sinResultados = buscando && !isError && !isFetching && resultados.length === 0;

  return (
    <Card className="space-y-4 p-5">
      <form
        className="flex items-end gap-3"
        onSubmit={(evento) => {
          evento.preventDefault();
          setTermino(texto.trim());
        }}
      >
        <div className="min-w-0 flex-1">
          <TextField
            id="orden-buscar-sku"
            label="Buscar producto"
            icon={Search}
            placeholder="Código o descripción"
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
          />
        </div>
        <Button type="submit">Buscar</Button>
      </form>
      {buscando && isError ? (
        <p className="text-danger-600 dark:text-danger-400 text-sm">No se pudo buscar productos.</p>
      ) : null}
      {sinResultados ? (
        <p className="text-sm text-neutral-500 dark:text-neutral-400">No se encontraron productos.</p>
      ) : null}
      {resultados.length > 0 ? (
        <ul className="divide-y divide-neutral-100 dark:divide-neutral-800">
          {resultados.map((sku) => (
            <li key={sku.id} className="flex items-center justify-between gap-4 py-3">
              <span className="min-w-0 text-sm font-medium text-neutral-900 dark:text-neutral-100">
                {sku.codigoInterno} — {sku.descripcionComercial}
              </span>
              <Button size="sm" aria-label={`Agregar ${sku.codigoInterno}`} onClick={() => onElegir(sku)}>
                <Plus className="size-4" aria-hidden="true" />
                Agregar
              </Button>
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}
```

`SRC/features/compras/components/LineasEditor.tsx`:

```tsx
import { Button, DataTable, Input } from '@boticas/ui-web';
import { formatoImporte } from '../lib/formato-compras';
import {
  errorLinea,
  totalLinea,
  type CambiosLinea,
  type LineaBorrador
} from '../lib/orden-calculo';

type LineasEditorProps = {
  lineas: LineaBorrador[];
  moneda: string;
  onCambiar: (skuId: string, cambios: CambiosLinea) => void;
  onQuitar: (skuId: string) => void;
};

type CampoEditable = keyof CambiosLinea;

const columnaEditable = (
  header: string,
  campo: CampoEditable,
  etiqueta: string,
  ancho: string,
  onCambiar: LineasEditorProps['onCambiar']
) => ({
  header,
  cell: (linea: LineaBorrador) => (
    <Input
      className={`${ancho} text-right tabular-nums`}
      inputMode="decimal"
      aria-label={`${etiqueta} de ${linea.codigoInterno}`}
      value={linea[campo]}
      onChange={(event) => onCambiar(linea.skuId, { [campo]: event.target.value })}
    />
  )
});

export function LineasEditor({ lineas, moneda, onCambiar, onQuitar }: LineasEditorProps) {
  return (
    <div className="space-y-3">
      <DataTable<LineaBorrador>
        columns={[
          {
            header: 'Producto',
            cell: (linea) => (
              <span className="font-medium text-neutral-900 dark:text-neutral-100">
                {`${linea.codigoInterno} — ${linea.descripcion}`}
              </span>
            )
          },
          columnaEditable('Cantidad', 'cantidad', 'Cantidad', 'w-24', onCambiar),
          columnaEditable('Precio', 'precio', 'Precio', 'w-28', onCambiar),
          columnaEditable('Descuento', 'descuento', 'Descuento', 'w-24', onCambiar),
          columnaEditable('Impuesto', 'impuesto', 'Impuesto', 'w-24', onCambiar),
          {
            header: 'Total',
            cell: (linea) => {
              const mensaje = errorLinea(linea);
              return (
                <div className="text-right">
                  <span className="font-semibold tabular-nums">
                    {formatoImporte(totalLinea(linea), moneda)}
                  </span>
                  {mensaje ? (
                    <p role="alert" className="text-danger-600 dark:text-danger-400 text-xs">
                      {mensaje}
                    </p>
                  ) : null}
                </div>
              );
            }
          },
          columnaEditable(
            'Exceso %',
            'toleranciaExceso',
            'Tolerancia de exceso',
            'w-20',
            onCambiar
          ),
          columnaEditable(
            'Defecto %',
            'toleranciaDefecto',
            'Tolerancia de defecto',
            'w-20',
            onCambiar
          ),
          {
            header: 'Acciones',
            cell: (linea) => (
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Quitar ${linea.codigoInterno}`}
                onClick={() => onQuitar(linea.skuId)}
              >
                Quitar
              </Button>
            )
          }
        ]}
        rows={lineas}
        rowKey={(linea) => linea.skuId}
        emptyMessage="Aún no agregaste productos."
      />
      <p role="note" className="text-xs text-neutral-500 dark:text-neutral-400">
        El impuesto se sugiere al 18% en los productos afectos a IGV y puedes corregirlo.
      </p>
    </div>
  );
}
```

`SRC/features/compras/components/TotalesOrden.tsx`:

```tsx
import { Card } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { formatoImporte } from '../lib/formato-compras';
import type { TotalesOrden as Totales } from '../lib/orden-calculo';

type TotalesOrdenProps = { totales: Totales; moneda: string };

export function TotalesOrden({ totales, moneda }: TotalesOrdenProps) {
  return (
    <Card className="p-6">
      <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <DatoItem label="Subtotal">{formatoImporte(totales.subtotal, moneda)}</DatoItem>
        <DatoItem label="Descuento">{formatoImporte(totales.descuento, moneda)}</DatoItem>
        <DatoItem label="Impuesto">{formatoImporte(totales.impuesto, moneda)}</DatoItem>
        <DatoItem label="Total">{formatoImporte(totales.total, moneda)}</DatoItem>
      </dl>
    </Card>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/components && pnpm typecheck && pnpm lint`
Expected: PASS con 100% en los archivos nuevos. En el test de `OrdenCabeceraForm`, `user.type` sobre un campo controlado con valor fijo (`valores` no cambia) emite siempre el carácter agregado al valor inicial: por eso se esperan `'PENX'` y `'05'`; si la librería normaliza distinto, ajustar solo esos valores esperados. Igual en `LineasEditor` (`'12'`, `'5.51'`, `'01'`, `'0.991'`).

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): cabecera, buscador de SKU, editor de lineas y totales de la orden

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Pantalla, ruta, acceso desde el listado y e2e

**Files:**
- Create: `SRC/features/compras/pages/NuevaOrdenPage.tsx` y `.test.tsx`
- Modify: `SRC/features/compras/pages/OrdenesPage.tsx`, `SRC/features/compras/pages/OrdenesPage.test.tsx`
- Modify: `SRC/features/compras/routes.tsx`, `SRC/features/compras/routes.test.ts`, `SRC/app/feature-routes.test.ts`
- Modify: `E2E/support/compras-api.ts`, `E2E/compras.spec.ts`

**Interfaces:**
- Consumes: todo lo anterior, `useEstablecimientos` (`../../organizacion`), `skuQuery` (`../../catalogo`), `useMutacionCompras`, `crearOrden`, `proveedoresQuery`, `apiClient`.
- Produces: ruta `compras/ordenes/nueva` (antes de `compras/ordenes/:ordenId`) y el botón `Nueva orden` en el listado.
- Textos exactos: título `Nueva orden de compra`, contexto `Compras / Órdenes`, descripción `Registra una orden en borrador para un proveedor.`; secciones `Productos`; botones `Crear orden` y `Cancelar`; avisos `Agrega al menos un producto.` y `No se pudo leer el producto. Inténtalo de nuevo.`; en el listado el enlace `Nueva orden` a `/compras/ordenes/nueva`.

- [ ] **Step 1: Escribir los tests que fallan**

`NuevaOrdenPage.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { sampleOrden, sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { formatoMoneda } from '../../../shared/lib/format';
import { NuevaOrdenPage } from './NuevaOrdenPage';

const ordenesUrl = '*/api/v1/compras/ordenes';

function mockEntorno(detalleSku: () => Response | Promise<Response> = () =>
  HttpResponse.json({ id: 'sku-0001-aaaa', afectoIgv: true })) {
  server.use(
    http.get('*/api/v1/compras/proveedores', () => HttpResponse.json(pagina([sampleProveedor]))),
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSkuVenta]))),
    http.get('*/api/v1/catalogo/skus/sku-0001-aaaa', detalleSku)
  );
}

function renderPage() {
  return renderRoute('*', NuevaOrdenPage, '/compras/ordenes/nueva');
}

type Usuario = ReturnType<typeof renderPage>['user'];

async function elegirCabecera(user: Usuario) {
  await screen.findByRole('option', { name: 'Laboratorios Perú SAC' });
  await user.selectOptions(screen.getByLabelText('Proveedor'), 'prov-1');
  await user.selectOptions(screen.getByLabelText('Establecimiento de destino'), 'est-1');
}

async function agregarProducto(user: Usuario) {
  await user.type(screen.getByLabelText('Buscar producto'), 'paracetamol');
  await user.click(screen.getByRole('button', { name: 'Buscar' }));
  await user.click(await screen.findByRole('button', { name: 'Agregar MED-001' }));
}

describe('NuevaOrdenPage', () => {
  it('precarga las condiciones del proveedor al elegirlo', async () => {
    mockEntorno();
    const { user } = renderPage();

    await elegirCabecera(user);

    expect(screen.getByLabelText('Moneda')).toHaveValue('PEN');
    expect(screen.getByLabelText('Condición de pago')).toHaveValue('CREDITO 30');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('30');
  });

  it('crea la orden con IGV sugerido, totales en vivo y abre su detalle', async () => {
    mockEntorno();
    let body: unknown;
    server.use(
      http.post(ordenesUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const { user, router, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await elegirCabecera(user);
    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    await waitFor(() => expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('0.99'));
    expect(screen.getAllByText(formatoMoneda(6.49)).length).toBeGreaterThan(0);

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    expect(body).toEqual({
      proveedorId: 'prov-1',
      establecimientoDestinoId: 'est-1',
      moneda: 'PEN',
      condicionPago: 'CREDITO 30',
      diasCredito: 30,
      lineas: [
        {
          skuId: 'sku-0001-aaaa',
          cantidad: 1,
          unidadMedidaCodigo: 'UND',
          precioUnitario: 5.5,
          descuento: 0,
          impuesto: 0.99,
          toleranciaExcesoPct: 0,
          toleranciaDefectoPct: 0
        }
      ]
    });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('un producto no afecto a IGV no sugiere impuesto', async () => {
    mockEntorno(() => HttpResponse.json({ id: 'sku-0001-aaaa', afectoIgv: false }));
    const { user } = renderPage();

    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('0');
  });

  it('al corregir el impuesto a mano ya no se recalcula', async () => {
    mockEntorno();
    const { user } = renderPage();
    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    await user.clear(screen.getByLabelText('Impuesto de MED-001'));
    await user.type(screen.getByLabelText('Impuesto de MED-001'), '2');
    await user.clear(screen.getByLabelText('Cantidad de MED-001'));
    await user.type(screen.getByLabelText('Cantidad de MED-001'), '3');

    expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('2');
  });

  it('no duplica un producto ya agregado y permite quitarlo', async () => {
    mockEntorno();
    const { user } = renderPage();
    await agregarProducto(user);
    await screen.findByLabelText('Precio de MED-001');

    await user.click(screen.getByRole('button', { name: 'Agregar MED-001' }));

    await waitFor(() => expect(screen.getAllByLabelText('Cantidad de MED-001')).toHaveLength(1));
    await user.click(screen.getByRole('button', { name: 'Quitar MED-001' }));

    expect(screen.getByText('Aún no agregaste productos.')).toBeInTheDocument();
  });

  it('avisa cuando no se puede leer el detalle del producto', async () => {
    mockEntorno(() => HttpResponse.json({ title: 'Error' }, { status: 500 }));
    const { user } = renderPage();

    await agregarProducto(user);

    expect(
      await screen.findByText('No se pudo leer el producto. Inténtalo de nuevo.')
    ).toBeInTheDocument();
    expect(screen.queryByLabelText('Cantidad de MED-001')).not.toBeInTheDocument();
  });

  it('no envía una orden incompleta y muestra qué falta', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(ordenesUrl, () => {
        enviado();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    expect(await screen.findByText('Selecciona un proveedor.')).toBeInTheDocument();
    expect(screen.getByText('Selecciona el establecimiento de destino.')).toBeInTheDocument();
    expect(screen.getByText('Agrega al menos un producto.')).toBeInTheDocument();
    expect(enviado).not.toHaveBeenCalled();
  });

  it('no envía mientras una línea tenga errores', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(ordenesUrl, () => {
        enviado();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const { user } = renderPage();
    await elegirCabecera(user);
    await agregarProducto(user);
    await screen.findByLabelText('Precio de MED-001');

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El precio debe ser mayor o igual a cero con hasta 6 decimales.'
    );
    expect(enviado).not.toHaveBeenCalled();
  });

  it('muestra el error traducido del backend y no navega', async () => {
    mockEntorno();
    server.use(
      http.post(ordenesUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_PROVEEDOR_NO_OPERABLE', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user, router } = renderPage();
    await elegirCabecera(user);
    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    expect(await screen.findByText(
      'El proveedor debe estar activo para emitir órdenes de compra.'
    )).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/compras/ordenes/nueva');
  });

  it('cancela y vuelve al listado de órdenes', async () => {
    mockEntorno();
    const { user, router } = renderPage();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/compras/ordenes');
  });
});
```

En `OrdenesPage.test.tsx` agregar:

```tsx
  it('ofrece crear una orden nueva', async () => {
    server.use(http.get(ordenesUrl, () => HttpResponse.json(pagina([]))));

    renderPage();

    expect(await screen.findByRole('link', { name: 'Nueva orden' })).toHaveAttribute(
      'href',
      '/compras/ordenes/nueva'
    );
  });
```

En `routes.test.ts` la lista esperada pasa a:

```ts
      'compras/ordenes',
      'compras/ordenes/nueva',
      'compras/ordenes/:ordenId'
```
(manteniendo las cuatro primeras rutas de compras y proveedores). En `SRC/app/feature-routes.test.ts` agregar `'compras/ordenes/nueva',` entre `'compras/ordenes',` y `'compras/ordenes/:ordenId',`.

Ampliar `E2E/support/compras-api.ts`: importar `import { sampleSkuVenta } from '../../apps/erp-web/src/test/ventas-fixtures';` y cambiar la ruta de listado de órdenes para distinguir método y agregar los SKU:

```ts
  await page.route(/\/api\/v1\/compras\/ordenes(\?|$)/, (route) =>
    route.request().method() === 'POST'
      ? json(route, 201, sampleOrden)
      : json(route, 200, pagina([{ ...sampleOrdenResumen, estado: orden.estado, total: orden.total }]))
  );
  await page.route(/\/api\/v1\/catalogo\/skus(\?|$)/, (route) =>
    json(route, 200, pagina([sampleSkuVenta]))
  );
  await page.route(/\/api\/v1\/catalogo\/skus\/sku-0001-aaaa$/, (route) =>
    json(route, 200, { id: 'sku-0001-aaaa', afectoIgv: true })
  );
```
(reemplaza el handler existente `ordenes(\?|$)` de la parte 3).

Agregar a `E2E/compras.spec.ts`:

```ts
  test('crea una orden de compra con IGV sugerido', async ({ page }) => {
    await abrirComprasEn(page, '/compras/ordenes');

    await page.getByRole('link', { name: 'Nueva orden' }).click();
    await expect(page.getByRole('heading', { name: 'Nueva orden de compra' })).toBeVisible();
    await page.getByLabel('Proveedor').selectOption('prov-1');
    await page.getByLabel('Establecimiento de destino').selectOption('est-1');
    await page.getByLabel('Buscar producto').fill('paracetamol');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await page.getByRole('button', { name: 'Agregar MED-001' }).click();
    await page.getByLabel('Precio de MED-001').fill('5.5');
    await expect(page.getByLabel('Impuesto de MED-001')).toHaveValue('0.99');
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: 'Crear orden' }).click();

    await expect(page.getByRole('heading', { name: 'Orden OC-2026-000001' })).toBeVisible();
  });
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/pages apps/erp-web/src/features/compras/routes.test.ts apps/erp-web/src/app`
Expected: FAIL (página, botón y ruta inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/pages/NuevaOrdenPage.tsx`:

```tsx
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { Button, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { skuQuery, type SkuResumen } from '../../catalogo';
import { useEstablecimientos } from '../../organizacion';
import { crearOrden } from '../api/ordenes.api';
import type { CrearOrdenPayload } from '../api/ordenes.types';
import { proveedoresQuery } from '../api/proveedores.api';
import { BuscadorSku } from '../components/BuscadorSku';
import { LineasEditor } from '../components/LineasEditor';
import { OrdenCabeceraForm } from '../components/OrdenCabeceraForm';
import { TotalesOrden } from '../components/TotalesOrden';
import {
  actualizarLinea,
  agregarSku,
  quitarLinea,
  totalesOrden,
  type LineaBorrador
} from '../lib/orden-calculo';
import {
  CABECERA_VACIA,
  cabeceraDesdeProveedor,
  erroresCabecera,
  fechaLocalISO,
  puedeCrear,
  toCrearOrdenPayload,
  type CabeceraOrden
} from '../lib/orden-cabecera';
import { useMutacionCompras } from '../lib/use-mutacion-compras';

export function NuevaOrdenPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const establecimientos = useEstablecimientos();
  const { data: proveedores } = useQuery(proveedoresQuery({ estado: 'ACTIVO', size: 100 }));
  const [cabecera, setCabecera] = useState<CabeceraOrden>(CABECERA_VACIA);
  const [lineas, setLineas] = useState<LineaBorrador[]>([]);
  const [intentado, setIntentado] = useState(false);
  const [errorSku, setErrorSku] = useState<string | null>(null);
  const hoy = fechaLocalISO();
  const creacion = useMutacionCompras(
    (payload: CrearOrdenPayload) => crearOrden(apiClient, payload),
    (orden) => {
      void navigate(`/compras/ordenes/${orden.id}`);
    }
  );

  const cambiarCabecera = (campo: keyof CabeceraOrden, valor: string) => {
    const proveedor =
      campo === 'proveedorId' ? proveedores?.items.find(({ id }) => id === valor) : undefined;
    setCabecera((actual) =>
      proveedor
        ? cabeceraDesdeProveedor(actual, proveedor)
        : { ...actual, [campo]: valor }
    );
  };

  const elegirSku = async (sku: SkuResumen) => {
    setErrorSku(null);
    const detalle = await queryClient.fetchQuery(skuQuery(sku.id)).catch(() => undefined);
    if (detalle === undefined) {
      setErrorSku('No se pudo leer el producto. Inténtalo de nuevo.');
      return;
    }
    setLineas((actuales) => agregarSku(actuales, sku, detalle.afectoIgv));
  };

  const crear = () => {
    setIntentado(true);
    if (puedeCrear(cabecera, lineas, hoy)) creacion.mutate(toCrearOrdenPayload(cabecera, lineas));
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <PageHeader
        title="Nueva orden de compra"
        context={<Link to="/compras/ordenes">Compras / Órdenes</Link>}
        description="Registra una orden en borrador para un proveedor."
      />
      <OrdenCabeceraForm
        valores={cabecera}
        errores={intentado ? erroresCabecera(cabecera, hoy) : {}}
        proveedores={proveedores?.items ?? []}
        establecimientos={establecimientos}
        onCambiar={cambiarCabecera}
      />
      <section className="space-y-4">
        <h2 className="text-base font-semibold">Productos</h2>
        <BuscadorSku
          onElegir={(sku) => {
            void elegirSku(sku);
          }}
        />
        {errorSku ? <FormError message={errorSku} /> : null}
        <LineasEditor
          lineas={lineas}
          moneda={cabecera.moneda}
          onCambiar={(skuId, cambios) =>
            setLineas((actuales) => actualizarLinea(actuales, skuId, cambios))
          }
          onQuitar={(skuId) => setLineas((actuales) => quitarLinea(actuales, skuId))}
        />
        {intentado && lineas.length === 0 ? <FormError message="Agrega al menos un producto." /> : null}
      </section>
      <TotalesOrden totales={totalesOrden(lineas)} moneda={cabecera.moneda} />
      {creacion.mensajeError ? <FormError message={creacion.mensajeError} /> : null}
      <div className="flex flex-wrap gap-3">
        <Button disabled={creacion.isPending} onClick={crear}>
          Crear orden
        </Button>
        <Button variant="secondary" onClick={() => void navigate('/compras/ordenes')}>
          Cancelar
        </Button>
      </div>
    </div>
  );
}
```

Nota: `cabecera.moneda` puede quedar inválida (por ejemplo `pen`) mientras se escribe; `formatoImporte` ya cae a `CODIGO 0.00` cuando la moneda es inválida, así que la página no se rompe.

`SRC/features/compras/pages/OrdenesPage.tsx`: importar `Link` de `react-router` y `buttonClassName` de `@boticas/ui-web` (junto a `PageHeader`) y agregar al `PageHeader` la propiedad:

```tsx
        actions={
          <Link to="/compras/ordenes/nueva" className={buttonClassName()}>
            Nueva orden
          </Link>
        }
```

`SRC/features/compras/routes.tsx`: insertar entre `compras/ordenes` y `compras/ordenes/:ordenId`:

```tsx
  {
    path: 'compras/ordenes/nueva',
    lazy: async () => {
      const { NuevaOrdenPage } = await import('./pages/NuevaOrdenPage');
      return { Component: NuevaOrdenPage };
    }
  },
```

- [ ] **Step 4: Ejecutar y verificar**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras apps/erp-web/src/app apps/erp-web/src/shared` → PASS. Run: `pnpm format && pnpm e2e compras.spec.ts` → PASS en desktop, tablet y móvil; `pnpm check` → verde (umbral 100% por archivo).
Ajustes permitidos sin reducir lo que verifican: el helper `mockEntorno` del test puede tiparse con `() => Response | Promise<Response>` o con `HttpResponseResolver` de MSW según los tipos reales; si `user.type` sobre un campo ya con valor agrega al final, usar `user.clear` antes como en los otros tests.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): crear orden de compra con IGV sugerido y totales en vivo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Cobertura del spec (sección Crear orden):** proveedor activo (lista de hasta 100), establecimiento destino con `useEstablecimientos`, fecha de entrega, moneda, tipo de cambio, condición de pago, días de crédito y observación (Task 2 y 3); editor de líneas con búsqueda de SKU por `skusQuery`, cantidad, precio, descuento, impuesto y tolerancias, unidad de venta del SKU, máximo de 200 líneas (Task 1 y 2); IGV sugerido del 18% editable y recalculado hasta que se edite a mano, aislado en `lib/igv.ts` y rotulado como sugerido (Task 1, 2 y 3); totales en vivo con redondeo a dos decimales (Task 1 y 2); navegación al detalle tras crear (Task 3); reglas espejo del backend (Task 1); cobertura 100% y e2e (Task 3).

**Escaneo de placeholders:** todo paso de código incluye el código. Los ajustes permitidos están acotados a casos concretos (tipo de retorno del mock de MSW, comportamiento de `user.type` sobre campos controlados). No hay "TBD".

**Consistencia de tipos:** `LineaBorrador`, `CambiosLinea`, `TotalesOrden`, `CabeceraOrden`, `ErroresCabecera`, `LineaOrdenPayload`, `CrearOrdenPayload`, `crearOrden`, `agregarSku`, `actualizarLinea`, `quitarLinea`, `errorLinea`, `totalLinea`, `totalesOrden`, `toLineasPayload`, `cabeceraDesdeProveedor`, `erroresCabecera`, `puedeCrear`, `fechaLocalISO`, `toCrearOrdenPayload` y `useMutacionCompras(fn, onSuccess(data))` se usan con las mismas firmas en las tres tareas; `useMutacionCompras` cambia en esta parte a `onSuccess?: (data: TData) => void`, compatible con los usos de la parte 3 (`onClose` sin parámetros).
