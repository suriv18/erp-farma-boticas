import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { BloquearLotePayload, Lote } from './inventario.types';

export function fetchLote(client: ApiClient, loteId: string): Promise<Lote> {
  return client.get<Lote>(`/inventario/lotes/${loteId}`);
}

export function loteQuery(loteId: string) {
  return queryOptions({
    queryKey: ['inventario', 'lotes', 'detalle', loteId],
    queryFn: () => fetchLote(apiClient, loteId)
  });
}

export function bloquearLote(
  client: ApiClient,
  loteId: string,
  payload: BloquearLotePayload
): Promise<Lote> {
  return client.post<Lote, BloquearLotePayload>(`/inventario/lotes/${loteId}/bloqueos`, payload);
}

export function desbloquearLote(client: ApiClient, loteId: string): Promise<Lote> {
  return client.delete<Lote>(`/inventario/lotes/${loteId}/bloqueos`);
}
