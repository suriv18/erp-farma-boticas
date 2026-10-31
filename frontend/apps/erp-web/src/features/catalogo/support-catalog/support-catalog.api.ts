import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { SupportCatalogItem } from './support-catalog.types';

type PaginaResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};

export type SupportCatalogApiOptions = {
  /** El GET de lista devuelve {items,page,size,totalElements} en vez de un array plano. */
  paginated?: boolean;
};

export type SupportCatalogApi<TItem extends SupportCatalogItem, TRequest> = {
  fetchList: (client: ApiClient, estado?: string) => Promise<TItem[]>;
  fetchOne: (client: ApiClient, codigo: string) => Promise<TItem>;
  create: (client: ApiClient, payload: TRequest) => Promise<TItem>;
  update: (client: ApiClient, codigo: string, payload: TRequest) => Promise<TItem>;
  changeStatus: (client: ApiClient, codigo: string, status: string) => Promise<void>;
  listQuery: (estado?: string) => ReturnType<typeof queryOptions<TItem[]>>;
};

const PAGINATED_LIST_SIZE = 100;

export function createSupportCatalogApi<TItem extends SupportCatalogItem, TRequest>(
  resource: string,
  options?: SupportCatalogApiOptions
): SupportCatalogApi<TItem, TRequest> {
  const paginated = options?.paginated ?? false;

  function fetchList(client: ApiClient, estado?: string): Promise<TItem[]> {
    const query = new URLSearchParams();
    if (estado) query.set('estado', estado);
    if (paginated) {
      query.set('page', '0');
      query.set('size', String(PAGINATED_LIST_SIZE));
      return client
        .get<PaginaResponse<TItem>>(`/catalogo/${resource}?${query.toString()}`)
        .then((response) => response.items);
    }
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return client.get<TItem[]>(`/catalogo/${resource}${suffix}`);
  }

  function fetchOne(client: ApiClient, codigo: string): Promise<TItem> {
    return client.get<TItem>(`/catalogo/${resource}/${codigo}`);
  }

  function create(client: ApiClient, payload: TRequest): Promise<TItem> {
    return client.post<TItem, TRequest>(`/catalogo/${resource}`, payload);
  }

  function update(client: ApiClient, codigo: string, payload: TRequest): Promise<TItem> {
    return client.put<TItem, TRequest>(`/catalogo/${resource}/${codigo}`, payload);
  }

  function changeStatus(client: ApiClient, codigo: string, status: string): Promise<void> {
    return client.patch<void, { status: string }>(`/catalogo/${resource}/${codigo}/estado`, { status });
  }

  function listQuery(estado?: string) {
    return queryOptions<TItem[], Error, TItem[], readonly unknown[]>({
      queryKey: ['catalogo', resource, estado ?? ''],
      queryFn: () => fetchList(apiClient, estado)
    });
  }

  return { fetchList, fetchOne, create, update, changeStatus, listQuery };
}
