import { redondear } from '../../../shared/lib/redondeo';
import type { SkuResumen } from '../../catalogo';
import type { LineaOrdenPayload } from '../api/ordenes.types';
import { impuestoSugerido } from './igv';
import { esCantidad, esMonto, esPrecio, esTolerancia } from './numeros-compras';

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

export const restablecerImpuesto = (lineas: LineaBorrador[], skuId: string): LineaBorrador[] =>
  lineas.map((linea) =>
    linea.skuId === skuId ? conImpuestoSugerido({ ...linea, impuestoManual: false }) : linea
  );

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
