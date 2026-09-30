import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { buildQuery } from './query-string';
import type {
  ActualizarEmpresaPayload,
  CrearEmpresaPayload,
  Empresa,
  EstadoEmpresa
} from './empresas.types';

export type FetchEmpresasParams = {
  tenantId: string;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchEmpresas(
  client: ApiClient,
  params: FetchEmpresasParams
): Promise<PaginaResponse<Empresa>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    search: params.search,
    page: params.page ?? 0,
    size: params.size ?? 20
  });
  return client.get<PaginaResponse<Empresa>>(`/organizacion/empresas?${query}`);
}

export function empresasQuery(params: FetchEmpresasParams) {
  return queryOptions({
    queryKey: [
      'organizacion',
      'empresas',
      'lista',
      params.tenantId,
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchEmpresas(apiClient, params)
  });
}

export function fetchEmpresa(
  client: ApiClient,
  tenantId: string,
  empresaId: string
): Promise<Empresa> {
  return client.get<Empresa>(`/organizacion/empresas/${empresaId}?${buildQuery({ tenantId })}`);
}

export function empresaQuery(tenantId: string, empresaId: string) {
  return queryOptions({
    queryKey: ['organizacion', 'empresas', 'detalle', tenantId, empresaId],
    queryFn: () => fetchEmpresa(apiClient, tenantId, empresaId)
  });
}

export function crearEmpresa(client: ApiClient, payload: CrearEmpresaPayload): Promise<Empresa> {
  return client.post<Empresa, CrearEmpresaPayload>('/organizacion/empresas', payload);
}

export function actualizarEmpresa(
  client: ApiClient,
  empresaId: string,
  tenantId: string,
  payload: ActualizarEmpresaPayload
): Promise<Empresa> {
  return client.put<Empresa, ActualizarEmpresaPayload>(
    `/organizacion/empresas/${empresaId}?${buildQuery({ tenantId })}`,
    payload
  );
}

export function cambiarEstadoEmpresa(
  client: ApiClient,
  empresaId: string,
  tenantId: string,
  estado: EstadoEmpresa
): Promise<Empresa> {
  return client.patch<Empresa, { estado: EstadoEmpresa }>(
    `/organizacion/empresas/${empresaId}/estado?${buildQuery({ tenantId })}`,
    { estado }
  );
}
