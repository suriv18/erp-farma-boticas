import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { Permiso } from './permisos.types';

export function fetchPermisos(client: ApiClient, search?: string): Promise<Permiso[]> {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  const query = params.toString();
  return client.get<Permiso[]>(`/permisos${query ? `?${query}` : ''}`);
}

export function permisosQuery(search?: string) {
  return queryOptions({
    queryKey: ['seguridad', 'permisos', search ?? ''],
    queryFn: () => fetchPermisos(apiClient, search)
  });
}
