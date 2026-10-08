import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type {
  ActualizarMarcaPayload,
  CrearMarcaPayload,
  Marca,
  PaginaResponse
} from './marcas.types';

export type FetchMarcasParams = {
  q?: string | undefined;
  estado?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchMarcas(
  client: ApiClient,
  params: FetchMarcasParams
): Promise<PaginaResponse<Marca>> {
  const query = new URLSearchParams();
  if (params.q) query.set('q', params.q);
  if (params.estado) query.set('estado', params.estado);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<Marca>>(`/catalogo/marcas?${query.toString()}`);
}

export function marcasQuery(params: FetchMarcasParams) {
  return queryOptions({
    queryKey: [
      'catalogo',
      'marcas',
      params.q ?? '',
      params.estado ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchMarcas(apiClient, params)
  });
}

export function crearMarca(client: ApiClient, payload: CrearMarcaPayload): Promise<Marca> {
  return client.post<Marca, CrearMarcaPayload>('/catalogo/marcas', payload);
}

export function actualizarMarca(
  client: ApiClient,
  marcaId: string,
  payload: ActualizarMarcaPayload
): Promise<Marca> {
  return client.put<Marca, ActualizarMarcaPayload>(`/catalogo/marcas/${marcaId}`, payload);
}

export function cambiarEstadoMarca(
  client: ApiClient,
  marcaId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(`/catalogo/marcas/${marcaId}/estado`, {
    status
  });
}
