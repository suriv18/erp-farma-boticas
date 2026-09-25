import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FieldValues, Resolver } from 'react-hook-form';
import { Button, DataTable, EstadoBadge, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { Pagination } from '../components/Pagination';
import { SupportCatalogForm } from './SupportCatalogForm';
import type { SupportCatalogApi } from './support-catalog.api';
import type { FieldDef, SupportCatalogItem } from './support-catalog.types';

export type SupportCatalogColumn<TItem> = {
  header: string;
  cell: (row: TItem) => React.ReactNode;
};

export type SupportCatalogPageProps<
  TItem extends SupportCatalogItem,
  TRequest,
  TValues extends FieldValues = TRequest & FieldValues,
  TInput extends FieldValues = TValues
> = {
  title: string;
  description: string;
  resourceLabel: string;
  api: SupportCatalogApi<TItem, TRequest>;
  fields: FieldDef[];
  resolver: Resolver<TInput, unknown, TValues>;
  columns: SupportCatalogColumn<TItem>[];
  searchableFields: (keyof TItem)[];
  toRequest: (values: TValues) => TRequest;
  toDefaultValues: (item: TItem) => TInput;
  pageSize?: number;
};

export function SupportCatalogPage<
  TItem extends SupportCatalogItem,
  TRequest,
  TValues extends FieldValues = TRequest & FieldValues,
  TInput extends FieldValues = TValues
>({
  title,
  description,
  resourceLabel,
  api,
  fields,
  resolver,
  columns,
  searchableFields,
  toRequest,
  toDefaultValues,
  pageSize = 20
}: SupportCatalogPageProps<TItem, TRequest, TValues, TInput>) {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<TItem | null>(null);
  const [search, setSearch] = useState('');
  const [estadoFilter, setEstadoFilter] = useState('');
  const [page, setPage] = useState(0);

  const { data, isPending, isError } = useQuery(api.listQuery(estadoFilter || undefined));

  const filtered = useMemo(() => {
    const items = data ?? [];
    if (!search) return items;
    const term = search.toLowerCase();
    return items.filter((item: TItem) =>
      searchableFields.some((field) => String(item[field]).toLowerCase().includes(term))
    );
  }, [data, search, searchableFields]);

  const paged = filtered.slice(page * pageSize, (page + 1) * pageSize);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo'] });

  const createMutation = useMutation({
    mutationFn: (values: TValues) => api.create(apiClient, toRequest(values)),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidate();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: TValues) => api.update(apiClient, editing!.codigo, toRequest(values)),
    onSuccess: () => {
      setEditing(null);
      void invalidate();
    }
  });

  const statusMutation = useMutation({
    mutationFn: (item: TItem) =>
      api.changeStatus(apiClient, item.codigo, item.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'),
    onSuccess: () => invalidate()
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Catálogo / {title}
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            {title}
          </h1>
          <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">{description}</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo {resourceLabel}</Button>
      </div>

      <div className="mt-6 flex flex-wrap items-end gap-4">
        <div>
          <label
            htmlFor="support-catalog-search"
            className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
          >
            Buscar {resourceLabel}
          </label>
          <input
            id="support-catalog-search"
            type="search"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Código o denominación"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 mt-2 h-11 w-full max-w-md rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          />
        </div>
        <div>
          <label
            htmlFor="support-catalog-estado"
            className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
          >
            Estado
          </label>
          <select
            id="support-catalog-estado"
            value={estadoFilter}
            onChange={(event) => {
              setEstadoFilter(event.target.value);
              setPage(0);
            }}
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 mt-2 h-11 rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          >
            <option value="">Todos</option>
            <option value="ACTIVO">Activo</option>
            <option value="INACTIVO">Inactivo</option>
          </select>
        </div>
      </div>

      <div className="mt-6">
        <DataTable<TItem>
          columns={[
            ...columns,
            { header: 'Estado', cell: (row: TItem) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row: TItem) => (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditing(row)}
                    className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  >
                    Editar {row.codigo}
                  </button>
                  <button
                    type="button"
                    onClick={() => statusMutation.mutate(row)}
                    className="font-semibold text-neutral-600 hover:underline dark:text-neutral-300"
                  >
                    {row.estado === 'ACTIVO' ? `Desactivar ${row.codigo}` : `Activar ${row.codigo}`}
                  </button>
                </div>
              )
            }
          ]}
          rows={paged}
          rowKey={(row) => row.codigo}
          emptyMessage="No se encontraron registros."
          isLoading={isPending}
          isError={isError}
          errorMessage={`No se pudo cargar el listado de ${resourceLabel}s.`}
          startIndex={page * pageSize}
        />
        <Pagination
          page={page}
          size={pageSize}
          totalElements={filtered.length}
          onPageChange={setPage}
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title={`Nuevo ${resourceLabel}`}>
        <SupportCatalogForm<TInput, TValues>
          fields={fields}
          resolver={resolver}
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear"
          isSubmitting={createMutation.isPending}
        />
      </Modal>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title={`Editar ${resourceLabel}`}>
        {editing ? (
          <SupportCatalogForm<TInput, TValues>
            fields={fields}
            resolver={resolver}
            defaultValues={toDefaultValues(editing)}
            onSubmit={(values) => updateMutation.mutate(values)}
            submitLabel="Guardar"
            isSubmitting={updateMutation.isPending}
          />
        ) : null}
      </Modal>
    </div>
  );
}
