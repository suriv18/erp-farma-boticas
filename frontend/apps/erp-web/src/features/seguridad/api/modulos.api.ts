import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { Modulo } from './modulos.types';

export function fetchModulos(client: ApiClient): Promise<Modulo[]> {
  return client.get<Modulo[]>('/modulos');
}

export function modulosQuery() {
  return queryOptions({
    queryKey: ['seguridad', 'modulos'],
    queryFn: () => fetchModulos(apiClient)
  });
}
