import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import { withQuery } from '../../../shared/lib/query-string';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import type { AgregarCodigoBarraPayload, Sku, SkuPayload, SkuResumen } from './skus.types';

export type FetchSkusParams = {
  q?: string | undefined;
  categoriaId?: string | undefined;
  marcaId?: string | undefined;
  tipoSku?: string | undefined;
  estado?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchSkus(
  client: ApiClient,
  params: FetchSkusParams
): Promise<PaginaResponse<SkuResumen>> {
  return client.get<PaginaResponse<SkuResumen>>(
    withQuery('/catalogo/skus', {
      q: params.q,
      categoriaId: params.categoriaId,
      marcaId: params.marcaId,
      tipoSku: params.tipoSku,
      estado: params.estado,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function skusQuery(params: FetchSkusParams) {
  return queryOptions({
    queryKey: [
      'catalogo',
      'skus',
      'lista',
      params.q ?? '',
      params.categoriaId ?? '',
      params.marcaId ?? '',
      params.tipoSku ?? '',
      params.estado ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchSkus(apiClient, params)
  });
}

export function fetchSku(client: ApiClient, skuId: string): Promise<Sku> {
  return client.get<Sku>(`/catalogo/skus/${skuId}`);
}

export function skuQuery(skuId: string) {
  return queryOptions({
    queryKey: ['catalogo', 'skus', 'detalle', skuId],
    queryFn: () => fetchSku(apiClient, skuId)
  });
}

export function crearSku(client: ApiClient, payload: SkuPayload): Promise<Sku> {
  return client.post<Sku, SkuPayload>('/catalogo/skus', payload);
}

export function actualizarSku(client: ApiClient, skuId: string, payload: SkuPayload): Promise<Sku> {
  return client.put<Sku, SkuPayload>(`/catalogo/skus/${skuId}`, payload);
}

export function cambiarEstadoSku(client: ApiClient, skuId: string, status: string): Promise<void> {
  return client.patch<void, { status: string }>(`/catalogo/skus/${skuId}/estado`, { status });
}

export function agregarCodigoBarra(
  client: ApiClient,
  skuId: string,
  payload: AgregarCodigoBarraPayload
): Promise<Sku> {
  return client.post<Sku, AgregarCodigoBarraPayload>(
    `/catalogo/skus/${skuId}/codigos-barra`,
    payload
  );
}

export function eliminarCodigoBarra(
  client: ApiClient,
  skuId: string,
  codigoBarra: string
): Promise<Sku> {
  return client.delete<Sku>(
    `/catalogo/skus/${skuId}/codigos-barra/${encodeURIComponent(codigoBarra)}`
  );
}

export function marcarCodigoBarraPrincipal(
  client: ApiClient,
  skuId: string,
  codigoBarra: string
): Promise<Sku> {
  return client.patch<Sku, Record<string, never>>(
    `/catalogo/skus/${skuId}/codigos-barra/${encodeURIComponent(codigoBarra)}/principal`,
    {}
  );
}
