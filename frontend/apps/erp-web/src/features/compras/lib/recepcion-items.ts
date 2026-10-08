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
