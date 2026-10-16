import { emptyToUndefined, numeroOpcional } from '../../../shared/lib/form-values';
import { redondear } from '../../../shared/lib/redondeo';
import type { LineaOrden, Orden } from '../api/ordenes.types';
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

const DECIMALES_CANTIDAD = 4;

const maximoRecibible = (linea: LineaOrden): number =>
  Math.max(
    0,
    redondear(
      linea.cantidad * (1 + (linea.toleranciaExcesoPct ?? 0) / 100) - linea.cantidadRecibida,
      DECIMALES_CANTIDAD
    )
  );

export const itemsDesdeOrden = (orden: Orden): ItemBorrador[] =>
  orden.lineas
    .filter((linea) => linea.cantidadPendiente > 0 || maximoRecibible(linea) > 0)
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

export const itemIncluido = (item: ItemBorrador): boolean =>
  [
    item.numeroLote,
    item.fechaFabricacion,
    item.fechaVencimiento,
    item.cantidadRecibida,
    item.motivoRechazo
  ].some((texto) => texto.trim() !== '') || Number(item.cantidadRechazada) !== 0;

const cantidadAceptada = (item: ItemBorrador): number =>
  redondear(Number(item.cantidadRecibida) - Number(item.cantidadRechazada), 4);

export type DiagnosticoItem = { campo: keyof CambiosItem; mensaje: string };

const falla = (campo: keyof CambiosItem, mensaje: string): DiagnosticoItem => ({ campo, mensaje });

export function diagnosticoItem(item: ItemBorrador, hoy: string): DiagnosticoItem | null {
  if (item.cantidadRecibida.trim() === '') {
    return falla('cantidadRecibida', 'Ingresa la cantidad recibida o deja la línea sin datos.');
  }
  const lote = item.numeroLote.trim();
  if (lote === '' || lote.length > 120) {
    return falla('numeroLote', 'El número de lote es obligatorio y admite hasta 120 caracteres.');
  }
  if (item.fechaVencimiento === '') {
    return falla('fechaVencimiento', 'La fecha de vencimiento es obligatoria.');
  }
  if (item.fechaFabricacion !== '' && item.fechaFabricacion >= item.fechaVencimiento) {
    return falla(
      'fechaVencimiento',
      'La fecha de vencimiento debe ser posterior a la de fabricación.'
    );
  }
  if (!esCantidad(item.cantidadRecibida.trim())) {
    return falla(
      'cantidadRecibida',
      'La cantidad recibida debe ser mayor que cero con hasta 4 decimales.'
    );
  }
  const rechazada = item.cantidadRechazada.trim() || '0';
  if (!esCantidadNoNegativa(rechazada) || Number(rechazada) > Number(item.cantidadRecibida)) {
    return falla(
      'cantidadRechazada',
      'La cantidad rechazada debe estar entre 0 y la cantidad recibida con hasta 4 decimales.'
    );
  }
  const motivo = item.motivoRechazo.trim();
  const rechaza = Number(rechazada) > 0;
  if (rechaza && motivo === '') return falla('motivoRechazo', 'Indica el motivo del rechazo.');
  if (rechaza && motivo.length > 1000) {
    return falla('motivoRechazo', 'El motivo del rechazo admite hasta 1000 caracteres.');
  }
  const costo = item.costoUnitario.trim();
  if (costo !== '' && !esPrecio(costo)) {
    return falla(
      'costoUnitario',
      'El costo unitario debe ser mayor o igual a cero con hasta 6 decimales.'
    );
  }
  return cantidadAceptada(item) > 0 && item.fechaVencimiento < hoy
    ? falla(
        'fechaVencimiento',
        'No se puede ingresar un lote vencido; recházalo por completo o corrige la fecha.'
      )
    : null;
}

export const errorItem = (item: ItemBorrador, hoy: string): string | null =>
  diagnosticoItem(item, hoy)?.mensaje ?? null;

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
      costoUnitario: numeroOpcional(item.costoUnitario)
    };
  });
