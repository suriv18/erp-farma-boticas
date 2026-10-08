import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { Sesion } from './sesiones.types';

export type FetchSesionesParams = {
  tenantId: string;
  userId?: string | undefined;
};

export function fetchSesiones(client: ApiClient, params: FetchSesionesParams): Promise<Sesion[]> {
  const query = new URLSearchParams({ tenantId: params.tenantId });
  if (params.userId) query.set('userId', params.userId);
  return client.get<Sesion[]>(`/sesiones?${query.toString()}`);
}

export function sesionesQuery(params: FetchSesionesParams) {
  return queryOptions({
    queryKey: ['seguridad', 'sesiones', params.tenantId, params.userId ?? ''],
    queryFn: () => fetchSesiones(apiClient, params)
  });
}

export function revocarSesion(
  client: ApiClient,
  sessionId: string,
  tenantId: string,
  reason?: string
): Promise<void> {
  return client.post<void, { reason?: string | undefined }>(
    `/sesiones/${sessionId}/revocacion?tenantId=${encodeURIComponent(tenantId)}`,
    { reason }
  );
}
