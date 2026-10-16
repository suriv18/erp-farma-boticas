import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './marcas.types';
import type {
  ActualizarRubroComercialPayload,
  CrearRubroComercialPayload,
  RubroComercial
} from './rubros-comerciales.types';

export type FetchRubrosComercialesParams = {
  q?: string | undefined;
  esFarmaceutico?: boolean | undefined;
  estado?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchRubrosComerciales(
  client: ApiClient,
  params: FetchRubrosComercialesParams
): Promise<PaginaResponse<RubroComercial>> {
  const query = new URLSearchParams();
  if (params.q) query.set('q', params.q);
  if (params.esFarmaceutico !== undefined)
    query.set('esFarmaceutico', String(params.esFarmaceutico));
  if (params.estado) query.set('estado', params.estado);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<RubroComercial>>(
    `/catalogo/rubros-comerciales?${query.toString()}`
  );
}

export function rubrosComercialesQuery(params: FetchRubrosComercialesParams) {
  return queryOptions({
    queryKey: [
      'catalogo',
      'rubros-comerciales',
      params.q ?? '',
      params.esFarmaceutico ?? '',
      params.estado ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchRubrosComerciales(apiClient, params)
  });
}

export function crearRubroComercial(
  client: ApiClient,
  payload: CrearRubroComercialPayload
): Promise<RubroComercial> {
  return client.post<RubroComercial, CrearRubroComercialPayload>(
    '/catalogo/rubros-comerciales',
    payload
  );
}

export function actualizarRubroComercial(
  client: ApiClient,
  rubroComercialId: string,
  payload: ActualizarRubroComercialPayload
): Promise<RubroComercial> {
  return client.put<RubroComercial, ActualizarRubroComercialPayload>(
    `/catalogo/rubros-comerciales/${rubroComercialId}`,
    payload
  );
}

export function cambiarEstadoRubroComercial(
  client: ApiClient,
  rubroComercialId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/catalogo/rubros-comerciales/${rubroComercialId}/estado`,
    { status }
  );
}
