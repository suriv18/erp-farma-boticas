import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { resolverPaginacion, type ParametrosLista } from './paginacion';
import { buildQuery } from './query-string';
import type { ActualizarAlmacenPayload, Almacen, CrearAlmacenPayload } from './almacenes.types';

export type FetchAlmacenesParams = ParametrosLista & {
  establecimientoId?: string | undefined;
};

export function fetchAlmacenes(
  client: ApiClient,
  params: FetchAlmacenesParams
): Promise<PaginaResponse<Almacen>> {
  const query = buildQuery({
    establecimientoId: params.establecimientoId,
    search: params.search,
    ...resolverPaginacion(params)
  });
  return client.get<PaginaResponse<Almacen>>(`/organizacion/almacenes?${query}`);
}

export function almacenesQuery(params: FetchAlmacenesParams) {
  const { page, size } = resolverPaginacion(params);
  return queryOptions({
    queryKey: [
      'organizacion',
      'almacenes',
      'lista',
      params.establecimientoId ?? '',
      params.search ?? '',
      page,
      size
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
  payload: ActualizarAlmacenPayload
): Promise<Almacen> {
  return client.put<Almacen, ActualizarAlmacenPayload>(
    `/organizacion/almacenes/${almacenId}`,
    payload
  );
}
