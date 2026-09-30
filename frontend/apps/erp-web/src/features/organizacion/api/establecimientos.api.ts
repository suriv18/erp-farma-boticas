import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { resolverPaginacion, type ParametrosLista } from './paginacion';
import { buildQuery } from './query-string';
import type {
  ActualizarEstablecimientoPayload,
  CrearEstablecimientoPayload,
  EstadoEstablecimiento,
  Establecimiento
} from './establecimientos.types';

export type FetchEstablecimientosParams = ParametrosLista & {
  empresaId?: string | undefined;
};

export function fetchEstablecimientos(
  client: ApiClient,
  params: FetchEstablecimientosParams
): Promise<PaginaResponse<Establecimiento>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    empresaId: params.empresaId,
    search: params.search,
    ...resolverPaginacion(params)
  });
  return client.get<PaginaResponse<Establecimiento>>(`/organizacion/establecimientos?${query}`);
}

export function establecimientosQuery(params: FetchEstablecimientosParams) {
  const { page, size } = resolverPaginacion(params);
  return queryOptions({
    queryKey: [
      'organizacion',
      'establecimientos',
      'lista',
      params.tenantId,
      params.empresaId ?? '',
      params.search ?? '',
      page,
      size
    ],
    queryFn: () => fetchEstablecimientos(apiClient, params)
  });
}

export function fetchEstablecimiento(
  client: ApiClient,
  tenantId: string,
  establecimientoId: string
): Promise<Establecimiento> {
  return client.get<Establecimiento>(
    `/organizacion/establecimientos/${establecimientoId}?${buildQuery({ tenantId })}`
  );
}

export function establecimientoQuery(tenantId: string, establecimientoId: string) {
  return queryOptions({
    queryKey: ['organizacion', 'establecimientos', 'detalle', tenantId, establecimientoId],
    queryFn: () => fetchEstablecimiento(apiClient, tenantId, establecimientoId)
  });
}

export function crearEstablecimiento(
  client: ApiClient,
  payload: CrearEstablecimientoPayload
): Promise<Establecimiento> {
  return client.post<Establecimiento, CrearEstablecimientoPayload>(
    '/organizacion/establecimientos',
    payload
  );
}

export function actualizarEstablecimiento(
  client: ApiClient,
  establecimientoId: string,
  tenantId: string,
  payload: ActualizarEstablecimientoPayload
): Promise<Establecimiento> {
  return client.put<Establecimiento, ActualizarEstablecimientoPayload>(
    `/organizacion/establecimientos/${establecimientoId}?${buildQuery({ tenantId })}`,
    payload
  );
}

export function cambiarEstadoEstablecimiento(
  client: ApiClient,
  establecimientoId: string,
  tenantId: string,
  estado: EstadoEstablecimiento
): Promise<Establecimiento> {
  return client.patch<Establecimiento, { estado: EstadoEstablecimiento }>(
    `/organizacion/establecimientos/${establecimientoId}/estado?${buildQuery({ tenantId })}`,
    { estado }
  );
}
