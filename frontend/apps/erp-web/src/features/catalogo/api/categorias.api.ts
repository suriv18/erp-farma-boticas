import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './marcas.types';
import type { ActualizarCategoriaPayload, CategoriaProducto, CrearCategoriaPayload } from './categorias.types';

export type FetchCategoriasParams = {
  tenantId: string;
  q?: string | undefined;
  categoriaPadreId?: string | undefined;
  estado?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchCategorias(
  client: ApiClient,
  params: FetchCategoriasParams
): Promise<PaginaResponse<CategoriaProducto>> {
  const query = new URLSearchParams({ tenantId: params.tenantId });
  if (params.q) query.set('q', params.q);
  if (params.categoriaPadreId) query.set('categoriaPadreId', params.categoriaPadreId);
  if (params.estado) query.set('estado', params.estado);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  return client.get<PaginaResponse<CategoriaProducto>>(`/catalogo/categorias?${query.toString()}`);
}

export function categoriasQuery(params: FetchCategoriasParams) {
  return queryOptions({
    queryKey: [
      'catalogo',
      'categorias',
      params.tenantId,
      params.q ?? '',
      params.categoriaPadreId ?? '',
      params.estado ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchCategorias(apiClient, params)
  });
}

export function crearCategoria(
  client: ApiClient,
  payload: CrearCategoriaPayload
): Promise<CategoriaProducto> {
  return client.post<CategoriaProducto, CrearCategoriaPayload>('/catalogo/categorias', payload);
}

export function actualizarCategoria(
  client: ApiClient,
  categoriaId: string,
  payload: ActualizarCategoriaPayload
): Promise<CategoriaProducto> {
  return client.put<CategoriaProducto, ActualizarCategoriaPayload>(
    `/catalogo/categorias/${categoriaId}`,
    payload
  );
}

export function cambiarEstadoCategoria(
  client: ApiClient,
  categoriaId: string,
  tenantId: string,
  status: string
): Promise<void> {
  return client.patch<void, { tenantId: string; status: string }>(
    `/catalogo/categorias/${categoriaId}/estado`,
    { tenantId, status }
  );
}
