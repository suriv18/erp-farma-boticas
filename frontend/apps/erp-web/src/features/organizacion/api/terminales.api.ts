import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { buildQuery } from './query-string';
import type { ActualizarTerminalPayload, CrearTerminalPayload, Terminal } from './terminales.types';

export type FetchTerminalesParams = {
  tenantId: string;
  establecimientoId?: string | undefined;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchTerminales(
  client: ApiClient,
  params: FetchTerminalesParams
): Promise<PaginaResponse<Terminal>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    establecimientoId: params.establecimientoId,
    search: params.search,
    page: params.page ?? 0,
    size: params.size ?? 20
  });
  return client.get<PaginaResponse<Terminal>>(`/organizacion/terminales-pos?${query}`);
}

export function terminalesQuery(params: FetchTerminalesParams) {
  return queryOptions({
    queryKey: [
      'organizacion',
      'terminales',
      'lista',
      params.tenantId,
      params.establecimientoId ?? '',
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchTerminales(apiClient, params)
  });
}

export function crearTerminal(client: ApiClient, payload: CrearTerminalPayload): Promise<Terminal> {
  return client.post<Terminal, CrearTerminalPayload>('/organizacion/terminales-pos', payload);
}

export function actualizarTerminal(
  client: ApiClient,
  terminalId: string,
  tenantId: string,
  payload: ActualizarTerminalPayload
): Promise<Terminal> {
  return client.put<Terminal, ActualizarTerminalPayload>(
    `/organizacion/terminales-pos/${terminalId}?${buildQuery({ tenantId })}`,
    payload
  );
}
