import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { resolverPaginacion, type ParametrosLista } from './paginacion';
import { buildQuery } from './query-string';
import type { ActualizarTerminalPayload, CrearTerminalPayload, Terminal } from './terminales.types';

export type FetchTerminalesParams = ParametrosLista & {
  establecimientoId?: string | undefined;
};

export function fetchTerminales(
  client: ApiClient,
  params: FetchTerminalesParams
): Promise<PaginaResponse<Terminal>> {
  const query = buildQuery({
    establecimientoId: params.establecimientoId,
    search: params.search,
    ...resolverPaginacion(params)
  });
  return client.get<PaginaResponse<Terminal>>(`/organizacion/terminales-pos?${query}`);
}

export function terminalesQuery(params: FetchTerminalesParams) {
  const { page, size } = resolverPaginacion(params);
  return queryOptions({
    queryKey: [
      'organizacion',
      'terminales',
      'lista',
      params.establecimientoId ?? '',
      params.search ?? '',
      page,
      size
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
  payload: ActualizarTerminalPayload
): Promise<Terminal> {
  return client.put<Terminal, ActualizarTerminalPayload>(
    `/organizacion/terminales-pos/${terminalId}`,
    payload
  );
}
