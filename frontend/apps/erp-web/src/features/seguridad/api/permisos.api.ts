import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './roles.types';
import type { Permiso } from './permisos.types';

export type FetchPermisosParams = {
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchPermisos(client: ApiClient, params: FetchPermisosParams): Promise<PaginaResponse<Permiso>> {
  const query = new URLSearchParams();
  if (params.search) query.set('search', params.search);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<Permiso>>(`/permisos?${query.toString()}`);
}

export function permisosQuery(params: FetchPermisosParams) {
  return queryOptions({
    queryKey: ['seguridad', 'permisos', params.search ?? '', params.page ?? 0, params.size ?? 20],
    queryFn: () => fetchPermisos(apiClient, params)
  });
}
