import { queryOptions } from '@tanstack/react-query';
import { ApiError, type ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import { withQuery } from '../../../shared/lib/query-string';
import type { AbrirTurnoPayload, CerrarTurnoPayload, Turno } from './caja.types';

export async function fetchTurnoActual(
  client: ApiClient,
  terminalId: string
): Promise<Turno | null> {
  try {
    return await client.get<Turno>(withQuery('/ventas/turnos/actual', { terminalId }));
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export function turnoActualQuery(terminalId: string) {
  return queryOptions({
    queryKey: ['caja', 'turno-actual', terminalId],
    queryFn: () => fetchTurnoActual(apiClient, terminalId)
  });
}

export function abrirTurno(client: ApiClient, payload: AbrirTurnoPayload): Promise<Turno> {
  return client.post<Turno, AbrirTurnoPayload>('/ventas/turnos', payload);
}

export function cerrarTurno(
  client: ApiClient,
  turnoId: string,
  payload: CerrarTurnoPayload
): Promise<Turno> {
  return client.post<Turno, CerrarTurnoPayload>(`/ventas/turnos/${turnoId}/cierre`, payload);
}
