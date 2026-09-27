import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Power } from 'lucide-react';
import { Button, DataTable, EstadoBadge, IconButton, Modal, PageHeader, ListFilters } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import {
  actualizarCategoria,
  cambiarEstadoCategoria,
  categoriasQuery,
  crearCategoria
} from '../api/categorias.api';
import type { CategoriaProducto } from '../api/categorias.types';
import type { CategoriaFormValues } from '../schemas/categoria.schema';
import { CategoriaForm } from '../components/CategoriaForm';

export function CategoriasPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<CategoriaProducto | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data, isPending, isError } = useQuery({
    ...categoriasQuery({ tenantId: tenantId ?? '', q: search || undefined, page, size }),
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
      <PageHeader
        title="Categorías"
        context="Catálogo / Categorías"
        description="Administra las categorías de productos del catálogo."
        actions={<Button onClick={() => setCreateOpen(true)}>Nueva categoría</Button>}
      />

      <ListFilters
        label="Buscar categoría"
        placeholder="Código o nombre"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

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
                <div className="flex items-center gap-1">
                  <IconButton icon={Pencil} label={`Editar ${row.nombre}`} onClick={() => setEditing(row)} />
                  <IconButton
                    icon={Power}
                    label={row.estado === 'ACTIVO' ? `Desactivar ${row.nombre}` : `Activar ${row.nombre}`}
                    tone={row.estado === 'ACTIVO' ? 'danger' : 'default'}
                    onClick={() => statusMutation.mutate(row)}
                  />
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
          startIndex={page * size}
          pagination={{
            page,
            size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
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
