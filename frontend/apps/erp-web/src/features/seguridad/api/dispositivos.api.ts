import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { Dispositivo } from './dispositivos.types';

export function fetchDispositivos(client: ApiClient, tenantId: string): Promise<Dispositivo[]> {
  return client.get<Dispositivo[]>(`/dispositivos?tenantId=${encodeURIComponent(tenantId)}`);
}

export function dispositivosQuery(tenantId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'dispositivos', tenantId],
    queryFn: () => fetchDispositivos(apiClient, tenantId)
  });
}

export function cambiarEstadoDispositivo(
  client: ApiClient,
  deviceId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/dispositivos/${deviceId}/estado?tenantId=${encodeURIComponent(tenantId)}`,
    { status }
  );
}
