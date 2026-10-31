import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import { withQuery } from '../../../shared/lib/query-string';
import type { PrincipioActivo, PrincipioActivoPayload } from './principios-activos.types';

export type FetchPrincipiosActivosParams = {
  texto?: string | undefined;
  estado?: string | undefined;
};

export function fetchPrincipiosActivos(
  client: ApiClient,
  params: FetchPrincipiosActivosParams = {}
): Promise<PrincipioActivo[]> {
  return client.get<PrincipioActivo[]>(
    withQuery('/catalogo/principios-activos', { texto: params.texto, estado: params.estado })
  );
}

export function principiosActivosQuery(params: FetchPrincipiosActivosParams = {}) {
  return queryOptions({
    queryKey: ['catalogo', 'principios-activos', params.texto ?? '', params.estado ?? ''],
    queryFn: () => fetchPrincipiosActivos(apiClient, params)
  });
}

export function crearPrincipioActivo(
  client: ApiClient,
  payload: PrincipioActivoPayload
): Promise<PrincipioActivo> {
  return client.post<PrincipioActivo, PrincipioActivoPayload>(
    '/catalogo/principios-activos',
    payload
  );
}

export function actualizarPrincipioActivo(
  client: ApiClient,
  principioActivoId: string,
  payload: PrincipioActivoPayload
): Promise<PrincipioActivo> {
  return client.put<PrincipioActivo, PrincipioActivoPayload>(
    `/catalogo/principios-activos/${principioActivoId}`,
    payload
  );
}

export function cambiarEstadoPrincipioActivo(
  client: ApiClient,
  principioActivoId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/catalogo/principios-activos/${principioActivoId}/estado`,
    { status }
  );
}
