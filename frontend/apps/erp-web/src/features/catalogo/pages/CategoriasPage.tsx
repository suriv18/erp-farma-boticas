import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, Modal } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { actualizarCategoria, cambiarEstadoCategoria, categoriasQuery, crearCategoria } from '../api/categorias.api';
import type { CategoriaProducto } from '../api/categorias.types';
import type { CategoriaFormValues } from '../schemas/categoria.schema';
import { CategoriaForm } from '../components/CategoriaForm';
import { EstadoBadge } from '../components/EstadoBadge';
import { Pagination } from '../components/Pagination';

const PAGE_SIZE = 20;

export function CategoriasPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<CategoriaProducto | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const { data, isPending, isError } = useQuery({
    ...categoriasQuery({ tenantId: tenantId ?? '', q: search || undefined, page, size: PAGE_SIZE }),
    enabled: Boolean(tenantId)
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo', 'categorias'] });

  const createMutation = useMutation({
    mutationFn: (values: CategoriaFormValues) =>
      crearCategoria(apiClient, {
        tenantId: tenantId ?? '',
        categoriaPadreId: values.categoriaPadreId || undefined,
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined,
        nivel: values.nivel,
        orden: values.orden
      }),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidate();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: CategoriaFormValues) =>
      actualizarCategoria(apiClient, editing!.id, {
        tenantId: tenantId ?? '',
        categoriaPadreId: values.categoriaPadreId || undefined,
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined,
        nivel: values.nivel,
        orden: values.orden
      }),
    onSuccess: () => {
      setEditing(null);
      void invalidate();
    }
  });

  const statusMutation = useMutation({
    mutationFn: (categoria: CategoriaProducto) =>
      cambiarEstadoCategoria(
        apiClient,
        categoria.id,
        tenantId ?? '',
        categoria.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
      ),
    onSuccess: () => invalidate()
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Catálogo / Categorías</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Categorías</h1>
          <p className="mt-2 text-sm text-slate-500">
            Administra las categorías de productos del catálogo.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nueva categoría</Button>
      </div>

      <div className="mt-6">
        <label htmlFor="categorias-search" className="text-sm font-semibold text-slate-700">
          Buscar categoría
        </label>
        <input
          id="categorias-search"
          type="search"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
          placeholder="Código o nombre"
          className="mt-2 h-11 w-full max-w-md rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
        />
      </div>

      <div className="mt-6">
        <DataTable<CategoriaProducto>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Nivel', cell: (row) => row.nivel },
            { header: 'Orden', cell: (row) => row.orden },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditing(row)}
                    className="font-semibold text-teal-700 hover:underline"
                  >
                    Editar {row.nombre}
                  </button>
                  <button
                    type="button"
                    onClick={() => statusMutation.mutate(row)}
                    className="font-semibold text-slate-600 hover:underline"
                  >
                    {row.estado === 'ACTIVO' ? `Desactivar ${row.nombre}` : `Activar ${row.nombre}`}
                  </button>
                </div>
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron categorías."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de categorías."
          startIndex={page * PAGE_SIZE}
        />
        <Pagination
          page={data?.page ?? page}
          size={data?.size ?? PAGE_SIZE}
          totalElements={data?.totalElements ?? 0}
          onPageChange={setPage}
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nueva categoría">
        <CategoriaForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear categoría"
          isSubmitting={createMutation.isPending}
        />
      </Modal>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title="Editar categoría">
        {editing ? (
          <CategoriaForm
            defaultValues={{
              categoriaPadreId: editing.categoriaPadreId ?? undefined,
              codigo: editing.codigo,
              nombre: editing.nombre,
              descripcion: editing.descripcion ?? '',
              nivel: editing.nivel,
              orden: editing.orden
            }}
            onSubmit={(values) => updateMutation.mutate(values)}
            submitLabel="Guardar cambios"
            isSubmitting={updateMutation.isPending}
          />
        ) : null}
      </Modal>
    </div>
  );
}
