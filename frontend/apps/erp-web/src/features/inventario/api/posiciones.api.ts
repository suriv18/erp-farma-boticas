import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { Posicion } from './inventario.types';

export type FetchPosicionesParams = {
  establecimientoId?: string | undefined;
  almacenId?: string | undefined;
  skuId?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchPosiciones(
  client: ApiClient,
  params: FetchPosicionesParams
): Promise<PaginaResponse<Posicion>> {
  return client.get<PaginaResponse<Posicion>>(
    withQuery('/inventario/posiciones', {
      establecimientoId: params.establecimientoId,
      almacenId: params.almacenId,
      skuId: params.skuId,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function posicionesQuery(params: FetchPosicionesParams) {
  return queryOptions({
    queryKey: [
      'inventario',
      'posiciones',
      'lista',
      params.establecimientoId ?? '',
      params.almacenId ?? '',
      params.skuId ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchPosiciones(apiClient, params)
  });
}
