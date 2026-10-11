import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import { withQuery } from '../../../shared/lib/query-string';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import type {
  AsociarPrincipioActivoPayload,
  ProductoRegulado,
  ProductoReguladoPayload,
  ProductoReguladoResumen
} from './productos-regulados.types';

export type FetchProductosReguladosParams = {
  q?: string | undefined;
  condicionVentaCodigo?: string | undefined;
  estadoRegulatorio?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchProductosRegulados(
  client: ApiClient,
  params: FetchProductosReguladosParams = {}
): Promise<PaginaResponse<ProductoReguladoResumen>> {
  return client.get<PaginaResponse<ProductoReguladoResumen>>(
    withQuery('/catalogo/productos-regulados', {
      q: params.q,
      condicionVentaCodigo: params.condicionVentaCodigo,
      estadoRegulatorio: params.estadoRegulatorio,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function productosReguladosQuery(params: FetchProductosReguladosParams = {}) {
  return queryOptions({
    queryKey: [
      'catalogo',
      'productos-regulados',
      'lista',
      params.q ?? '',
      params.condicionVentaCodigo ?? '',
      params.estadoRegulatorio ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchProductosRegulados(apiClient, params)
  });
}

export function fetchProductoRegulado(
  client: ApiClient,
  productoReguladoId: string
): Promise<ProductoRegulado> {
  return client.get<ProductoRegulado>(`/catalogo/productos-regulados/${productoReguladoId}`);
}

export function productoReguladoQuery(productoReguladoId: string) {
  return queryOptions({
    queryKey: ['catalogo', 'productos-regulados', 'detalle', productoReguladoId],
    queryFn: () => fetchProductoRegulado(apiClient, productoReguladoId)
  });
}

export function crearProductoRegulado(
  client: ApiClient,
  payload: ProductoReguladoPayload
): Promise<ProductoRegulado> {
  return client.post<ProductoRegulado, ProductoReguladoPayload>(
    '/catalogo/productos-regulados',
    payload
  );
}

export function actualizarProductoRegulado(
  client: ApiClient,
  productoReguladoId: string,
  payload: ProductoReguladoPayload
): Promise<ProductoRegulado> {
  return client.put<ProductoRegulado, ProductoReguladoPayload>(
    `/catalogo/productos-regulados/${productoReguladoId}`,
    payload
  );
}

export function cambiarEstadoProductoRegulado(
  client: ApiClient,
  productoReguladoId: string,
  status: string
): Promise<void> {
  return client.patch<void, { status: string }>(
    `/catalogo/productos-regulados/${productoReguladoId}/estado`,
    {
      status
    }
  );
}

export function asociarPrincipioActivo(
  client: ApiClient,
  productoReguladoId: string,
  payload: AsociarPrincipioActivoPayload
): Promise<ProductoRegulado> {
  return client.post<ProductoRegulado, AsociarPrincipioActivoPayload>(
    `/catalogo/productos-regulados/${productoReguladoId}/principios-activos`,
    payload
  );
}

export function desasociarPrincipioActivo(
  client: ApiClient,
  productoReguladoId: string,
  principioActivoId: string
): Promise<ProductoRegulado> {
  return client.delete<ProductoRegulado>(
    `/catalogo/productos-regulados/${productoReguladoId}/principios-activos/${principioActivoId}`
  );
}
