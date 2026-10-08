import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { AnularOrdenPayload, Orden, OrdenResumen } from './ordenes.types';

export type FetchOrdenesParams = {
  proveedorId?: string | undefined;
  estado?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchOrdenes(
  client: ApiClient,
  params: FetchOrdenesParams
): Promise<PaginaResponse<OrdenResumen>> {
  return client.get<PaginaResponse<OrdenResumen>>(
    withQuery('/compras/ordenes', {
      proveedorId: params.proveedorId,
      estado: params.estado,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function ordenesQuery(params: FetchOrdenesParams) {
  return queryOptions({
    queryKey: [
      'compras',
      'ordenes',
      'lista',
      params.proveedorId ?? '',
      params.estado ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchOrdenes(apiClient, params)
  });
}

export function fetchOrden(client: ApiClient, ordenId: string): Promise<Orden> {
  return client.get<Orden>(`/compras/ordenes/${ordenId}`);
}

export function ordenQuery(ordenId: string) {
  return queryOptions({
    queryKey: ['compras', 'ordenes', 'detalle', ordenId],
    queryFn: () => fetchOrden(apiClient, ordenId)
  });
}

export function aprobarOrden(client: ApiClient, ordenId: string): Promise<Orden> {
  return client.post<Orden, Record<string, never>>(`/compras/ordenes/${ordenId}/aprobacion`, {});
}

export function emitirOrden(client: ApiClient, ordenId: string): Promise<Orden> {
  return client.post<Orden, Record<string, never>>(`/compras/ordenes/${ordenId}/emision`, {});
}

export function anularOrden(
  client: ApiClient,
  ordenId: string,
  payload: AnularOrdenPayload
): Promise<Orden> {
  return client.post<Orden, AnularOrdenPayload>(`/compras/ordenes/${ordenId}/anulacion`, payload);
}
