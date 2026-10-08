import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { resolverPaginacion, type ParametrosLista } from './paginacion';
import { buildQuery } from './query-string';
import type {
  ActualizarEmpresaPayload,
  CrearEmpresaPayload,
  Empresa,
  EstadoEmpresa
} from './empresas.types';

export type FetchEmpresasParams = ParametrosLista;

export function fetchEmpresas(
  client: ApiClient,
  params: FetchEmpresasParams
): Promise<PaginaResponse<Empresa>> {
  const query = buildQuery({
    search: params.search,
    ...resolverPaginacion(params)
  });
  return client.get<PaginaResponse<Empresa>>(`/organizacion/empresas?${query}`);
}

export function empresasQuery(params: FetchEmpresasParams) {
  const { page, size } = resolverPaginacion(params);
  return queryOptions({
    queryKey: ['organizacion', 'empresas', 'lista', params.search ?? '', page, size],
    queryFn: () => fetchEmpresas(apiClient, params)
  });
}

export function fetchEmpresa(client: ApiClient, empresaId: string): Promise<Empresa> {
  return client.get<Empresa>(`/organizacion/empresas/${empresaId}`);
}

export function empresaQuery(empresaId: string) {
  return queryOptions({
    queryKey: ['organizacion', 'empresas', 'detalle', empresaId],
    queryFn: () => fetchEmpresa(apiClient, empresaId)
  });
}

export function crearEmpresa(client: ApiClient, payload: CrearEmpresaPayload): Promise<Empresa> {
  return client.post<Empresa, CrearEmpresaPayload>('/organizacion/empresas', payload);
}

export function actualizarEmpresa(
  client: ApiClient,
  empresaId: string,
  payload: ActualizarEmpresaPayload
): Promise<Empresa> {
  return client.put<Empresa, ActualizarEmpresaPayload>(
    `/organizacion/empresas/${empresaId}`,
    payload
  );
}

export function cambiarEstadoEmpresa(
  client: ApiClient,
  empresaId: string,
  estado: EstadoEmpresa
): Promise<Empresa> {
  return client.patch<Empresa, { estado: EstadoEmpresa }>(
    `/organizacion/empresas/${empresaId}/estado`,
    { estado }
  );
}
