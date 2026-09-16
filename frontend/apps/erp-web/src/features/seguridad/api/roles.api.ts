import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { CrearRolPayload, PaginaResponse, Rol } from './roles.types';

export type FetchRolesParams = {
  tenantId: string;
  search?: string;
  page?: number;
  size?: number;
};

export function fetchRoles(client: ApiClient, params: FetchRolesParams): Promise<PaginaResponse<Rol>> {
  const query = new URLSearchParams({ tenantId: params.tenantId });
  if (params.search) query.set('search', params.search);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<Rol>>(`/roles?${query.toString()}`);
}

export function rolesQuery(params: FetchRolesParams) {
  return queryOptions({
    queryKey: ['seguridad', 'roles', params.tenantId, params.search ?? '', params.page ?? 0, params.size ?? 20],
    queryFn: () => fetchRoles(apiClient, params)
  });
}

export function crearRol(client: ApiClient, payload: CrearRolPayload): Promise<Rol> {
  return client.post<Rol, CrearRolPayload>('/roles', payload);
}

export function cambiarEstadoRol(
  client: ApiClient,
  roleId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/roles/${roleId}/estado?tenantId=${encodeURIComponent(tenantId)}`,
    { status }
  );
}

export function reemplazarPermisosRol(
  client: ApiClient,
  roleId: string,
  permissionCodes: string[]
): Promise<Rol> {
  return client.put<Rol, { permissionCodes: string[] }>(`/roles/${roleId}/permissions`, { permissionCodes });
}
