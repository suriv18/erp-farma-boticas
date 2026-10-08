import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { Recepcion, RegistrarRecepcionPayload } from './recepciones.types';

export type FetchRecepcionesParams = {
  ordenCompraId: string;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchRecepciones(
  client: ApiClient,
  params: FetchRecepcionesParams
): Promise<PaginaResponse<Recepcion>> {
  return client.get<PaginaResponse<Recepcion>>(
    withQuery('/compras/recepciones', {
      ordenCompraId: params.ordenCompraId,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function recepcionesQuery(params: FetchRecepcionesParams) {
  return queryOptions({
    queryKey: [
      'compras',
      'recepciones',
      'orden',
      params.ordenCompraId,
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchRecepciones(apiClient, params)
  });
}

export function fetchRecepcion(client: ApiClient, recepcionId: string): Promise<Recepcion> {
  return client.get<Recepcion>(`/compras/recepciones/${recepcionId}`);
}

export function registrarRecepcion(
  client: ApiClient,
  payload: RegistrarRecepcionPayload,
  idempotencyKey: string
): Promise<Recepcion> {
  return client.post<Recepcion, RegistrarRecepcionPayload>('/compras/recepciones', payload, {
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}
