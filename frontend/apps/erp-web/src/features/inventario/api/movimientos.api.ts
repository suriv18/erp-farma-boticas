import type { ApiClient } from '@boticas/api-client';
import type { Movimiento, RegistrarMovimientoPayload } from './inventario.types';

export function registrarMovimiento(
  client: ApiClient,
  payload: RegistrarMovimientoPayload,
  idempotencyKey: string
): Promise<Movimiento> {
  return client.post<Movimiento, RegistrarMovimientoPayload>('/inventario/movimientos', payload, {
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}
