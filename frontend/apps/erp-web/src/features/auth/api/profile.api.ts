import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PerfilUsuario } from './profile.types';

const PERFIL_STALE_TIME_MS = 5 * 60_000;

export function fetchPerfil(client: ApiClient): Promise<PerfilUsuario> {
  return client.get<PerfilUsuario>('/auth/me');
}

export function perfilQuery(userId: string) {
  return queryOptions({
    queryKey: ['auth', 'perfil', userId],
    queryFn: () => fetchPerfil(apiClient),
    staleTime: PERFIL_STALE_TIME_MS
  });
}
