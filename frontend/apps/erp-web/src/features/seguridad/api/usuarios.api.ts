import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './roles.types';
import type { AsignacionRol, AsignarRolPayload, CrearUsuarioPayload, Usuario } from './usuarios.types';

export type FetchUsuariosParams = {
  tenantId: string;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchUsuarios(
  client: ApiClient,
  params: FetchUsuariosParams
): Promise<PaginaResponse<Usuario>> {
  const query = new URLSearchParams({ tenantId: params.tenantId });
  if (params.search) query.set('search', params.search);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<Usuario>>(`/usuarios?${query.toString()}`);
}

export function usuariosQuery(params: FetchUsuariosParams) {
  return queryOptions({
    queryKey: [
      'seguridad',
      'usuarios',
      params.tenantId,
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchUsuarios(apiClient, params)
  });
}

export function crearUsuario(client: ApiClient, payload: CrearUsuarioPayload): Promise<Usuario> {
  return client.post<Usuario, CrearUsuarioPayload>('/usuarios', payload);
}

export function cambiarEstadoUsuario(
  client: ApiClient,
  userId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/usuarios/${userId}/estado?tenantId=${encodeURIComponent(tenantId)}`,
    { status }
  );
}

export function fetchUsuarioAsignaciones(
  client: ApiClient,
  userId: string,
  tenantId: string
): Promise<AsignacionRol[]> {
  return client.get<AsignacionRol[]>(
    `/usuarios/${userId}/asignaciones-rol?tenantId=${encodeURIComponent(tenantId)}`
  );
}

export function usuarioAsignacionesQuery(userId: string, tenantId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'usuarios', userId, 'asignaciones-rol', tenantId],
    queryFn: () => fetchUsuarioAsignaciones(apiClient, userId, tenantId)
  });
}

export function asignarRolUsuario(
  client: ApiClient,
  userId: string,
  payload: AsignarRolPayload
): Promise<AsignacionRol> {
  return client.post<AsignacionRol, AsignarRolPayload>(`/usuarios/${userId}/role-assignments`, payload);
}

export function revocarAsignacionRol(
  client: ApiClient,
  userId: string,
  assignmentId: string,
  tenantId: string
): Promise<void> {
  return client.delete<void>(
    `/usuarios/${userId}/asignaciones-rol/${assignmentId}?tenantId=${encodeURIComponent(tenantId)}`
  );
}
