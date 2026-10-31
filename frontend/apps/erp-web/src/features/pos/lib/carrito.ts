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
