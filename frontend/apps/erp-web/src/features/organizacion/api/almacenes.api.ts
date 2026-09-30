import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { buildQuery } from './query-string';
import type { ActualizarAlmacenPayload, Almacen, CrearAlmacenPayload } from './almacenes.types';

export type FetchAlmacenesParams = {
  tenantId: string;
  establecimientoId?: string | undefined;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchAlmacenes(
  client: ApiClient,
  params: FetchAlmacenesParams
): Promise<PaginaResponse<Almacen>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    establecimientoId: params.establecimientoId,
    search: params.search,
    page: params.page ?? 0,
    size: params.size ?? 20
  });
  return client.get<PaginaResponse<Almacen>>(`/organizacion/almacenes?${query}`);
}

export function almacenesQuery(params: FetchAlmacenesParams) {
  return queryOptions({
    queryKey: [
      'organizacion',
      'almacenes',
      'lista',
      params.tenantId,
      params.establecimientoId ?? '',
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchAlmacenes(apiClient, params)
  });
}

export function crearAlmacen(client: ApiClient, payload: CrearAlmacenPayload): Promise<Almacen> {
  return client.post<Almacen, CrearAlmacenPayload>('/organizacion/almacenes', payload);
}

export function actualizarAlmacen(
  client: ApiClient,
  almacenId: string,
  tenantId: string,
  payload: ActualizarAlmacenPayload
): Promise<Almacen> {
  return client.put<Almacen, ActualizarAlmacenPayload>(
    `/organizacion/almacenes/${almacenId}?${buildQuery({ tenantId })}`,
    payload
  );
}
