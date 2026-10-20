import { queryOptions } from '@tanstack/react-query';
import { apiClient } from '../../../app/api';
import type { MiPuesto } from './mi-puesto.types';

export const miPuestoQuery = queryOptions({
  queryKey: ['organization', 'mi-puesto'],
  queryFn: () => apiClient.get<MiPuesto>('/organizacion/mi-puesto')
});
