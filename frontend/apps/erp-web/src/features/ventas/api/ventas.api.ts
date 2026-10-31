import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { RegistrarVentaPayload, Venta, VentaResumen } from './ventas.types';

export type FetchVentasParams = {
  establecimientoId?: string | undefined;
  desde?: string | undefined;
  hasta?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchVentas(
  client: ApiClient,
  params: FetchVentasParams
): Promise<PaginaResponse<VentaResumen>> {
  return client.get<PaginaResponse<VentaResumen>>(
    withQuery('/ventas/ventas', {
      establecimientoId: params.establecimientoId,
      desde: params.desde,
      hasta: params.hasta,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function ventasQuery(params: FetchVentasParams) {
  return queryOptions({
    queryKey: [
      'ventas',
      'lista',
      params.establecimientoId ?? '',
      params.desde ?? '',
      params.hasta ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchVentas(apiClient, params)
  });
}

export function fetchVenta(client: ApiClient, ventaId: string): Promise<Venta> {
  return client.get<Venta>(`/ventas/ventas/${ventaId}`);
}

export function ventaQuery(ventaId: string) {
  return queryOptions({
    queryKey: ['ventas', 'detalle', ventaId],
    queryFn: () => fetchVenta(apiClient, ventaId)
  });
}

export function registrarVenta(
  client: ApiClient,
  payload: RegistrarVentaPayload,
  idempotencyKey: string
): Promise<Venta> {
  return client.post<Venta, RegistrarVentaPayload>('/ventas/ventas', payload, {
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}
