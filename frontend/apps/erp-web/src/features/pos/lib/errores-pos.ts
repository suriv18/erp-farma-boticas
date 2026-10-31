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
