import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FieldValues, Resolver } from 'react-hook-form';
import { Pencil, Power } from 'lucide-react';
import { Button, DataTable, EstadoBadge, IconButton, Modal, PageHeader, ListFilters } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
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
  const [size, setSize] = useState(pageSize);

  const { data, isPending, isError } = useQuery(api.listQuery(estadoFilter || undefined));

  const filtered = useMemo(() => {
    const items = data ?? [];
    if (!search) return items;
    const term = search.toLowerCase();
    return items.filter((item: TItem) =>
      searchableFields.some((field) => String(item[field]).toLowerCase().includes(term))
    );
  }, [data, search, searchableFields]);

  const currentPage = Math.min(page, Math.max(0, Math.ceil(filtered.length / size) - 1));
  const paged = filtered.slice(currentPage * size, (currentPage + 1) * size);

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
      <PageHeader
        title={title}
        context={<>Catálogo / {title}</>}
        description={description}
        actions={<Button onClick={() => setCreateOpen(true)}>Nuevo {resourceLabel}</Button>}
      />

      <ListFilters
        label={`Buscar ${resourceLabel}`}
        placeholder="Código o denominación"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      >
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
      </ListFilters>

      <div className="mt-6">
        <DataTable<TItem>
          columns={[
            ...columns,
            { header: 'Estado', cell: (row: TItem) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row: TItem) => (
                <div className="flex items-center gap-1">
                  <IconButton icon={Pencil} label={`Editar ${row.codigo}`} onClick={() => setEditing(row)} />
                  <IconButton
                    icon={Power}
                    label={row.estado === 'ACTIVO' ? `Desactivar ${row.codigo}` : `Activar ${row.codigo}`}
                    tone={row.estado === 'ACTIVO' ? 'danger' : 'default'}
                    onClick={() => statusMutation.mutate(row)}
                  />
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
          startIndex={currentPage * size}
          pagination={{
            page,
            size,
            totalElements: filtered.length,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>

      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title={`Nuevo ${resourceLabel}`}
      >
        <SupportCatalogForm<TInput, TValues>
          fields={fields}
          resolver={resolver}
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear"
          isSubmitting={createMutation.isPending}
        />
      </Modal>

      <Modal
        open={editing !== null}
        onClose={() => setEditing(null)}
        title={`Editar ${resourceLabel}`}
      >
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
