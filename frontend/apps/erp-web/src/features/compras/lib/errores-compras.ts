import { ApiError } from '@boticas/api-client';
import { describeApiError } from '../../../shared/lib/describe-api-error';

const MENSAJES: Record<string, string> = {
  COM_PROVEEDOR_DUPLICADO: 'Ya existe un proveedor con ese documento.',
  COM_PROVEEDOR_NO_OPERABLE: 'El proveedor debe estar activo para emitir órdenes de compra.',
  COM_ORDEN_ESTADO_INVALIDO:
    'La orden no admite esta acción en su estado actual. Actualiza la pantalla.',
  COM_RECEPCION_ORDEN_NO_RECEPCIONABLE:
    'La orden no está en un estado que permita recibir mercadería.',
  COM_RECEPCION_EXCEDE_PENDIENTE:
    'La cantidad recibida excede lo pendiente de la orden más su tolerancia.',
  COM_RECEPCION_LINEA_NO_ENCONTRADA: 'Una línea de la recepción no corresponde a la orden.',
  COM_IDEMPOTENCY_CONFLICT:
    'Esta recepción ya se envió con datos distintos. Revisa el formulario y vuelve a registrar.',
  COM_ALMACEN_NO_OPERABLE: 'El almacén debe estar activo y controlar lotes.',
  COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO:
    'El almacén no pertenece al establecimiento destino de la orden.',
  COM_MODIFICACION_CONCURRENTE:
    'Otro usuario modificó este registro. Actualiza la pantalla e inténtalo de nuevo.',
  COM_SKU_NO_OPERABLE: 'Uno de los productos no está activo comercialmente.',
  COM_ESTABLECIMIENTO_NO_OPERABLE: 'El establecimiento destino debe estar activo.'
};

export function describeErrorCompras(error: unknown): string {
  const code = error instanceof ApiError ? error.problem?.code : undefined;
  if (code === undefined) return describeApiError(error);
  return MENSAJES[code] ?? `${describeApiError(error)} (${code})`;
}
