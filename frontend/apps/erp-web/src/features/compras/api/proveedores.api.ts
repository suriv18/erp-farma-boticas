import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { EstadoProveedor, Proveedor, ProveedorPayload } from './proveedores.types';

export type FetchProveedoresParams = {
  estado?: string | undefined;
  texto?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchProveedores(
  client: ApiClient,
  params: FetchProveedoresParams
): Promise<PaginaResponse<Proveedor>> {
  return client.get<PaginaResponse<Proveedor>>(
    withQuery('/compras/proveedores', {
      estado: params.estado,
      texto: params.texto,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function proveedoresQuery(params: FetchProveedoresParams) {
  return queryOptions({
    queryKey: [
      'compras',
      'proveedores',
      'lista',
      params.estado ?? '',
      params.texto ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchProveedores(apiClient, params)
  });
}

export function fetchProveedor(client: ApiClient, proveedorId: string): Promise<Proveedor> {
  return client.get<Proveedor>(`/compras/proveedores/${proveedorId}`);
}

export function proveedorQuery(proveedorId: string) {
  return queryOptions({
    queryKey: ['compras', 'proveedores', 'detalle', proveedorId],
    queryFn: () => fetchProveedor(apiClient, proveedorId)
  });
}

export function crearProveedor(client: ApiClient, payload: ProveedorPayload): Promise<Proveedor> {
  return client.post<Proveedor, ProveedorPayload>('/compras/proveedores', payload);
}

export function actualizarProveedor(
  client: ApiClient,
  proveedorId: string,
  payload: ProveedorPayload
): Promise<Proveedor> {
  return client.put<Proveedor, ProveedorPayload>(`/compras/proveedores/${proveedorId}`, payload);
}

export function cambiarEstadoProveedor(
  client: ApiClient,
  proveedorId: string,
  estado: EstadoProveedor
): Promise<Proveedor> {
  return client.patch<Proveedor, { estado: EstadoProveedor }>(
    `/compras/proveedores/${proveedorId}/estado`,
    { estado }
  );
}
