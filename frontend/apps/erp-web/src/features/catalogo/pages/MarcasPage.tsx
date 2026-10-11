import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Power } from 'lucide-react';
import {
  Button,
  DataTable,
  EstadoBadge,
  IconButton,
  Modal,
  PageHeader,
  ListFilters
} from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarMarca, cambiarEstadoMarca, crearMarca, marcasQuery } from '../api/marcas.api';
import type { Marca } from '../api/marcas.types';
import type { MarcaFormValues } from '../schemas/marca.schema';
import { MarcaForm } from '../components/MarcaForm';

export function MarcasPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Marca | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data, isPending, isError } = useQuery(
    marcasQuery({ q: search || undefined, page, size })
  );

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo', 'marcas'] });

  const createMutation = useMutation({
    mutationFn: (values: MarcaFormValues) =>
      crearMarca(apiClient, {
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined
      }),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidate();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: MarcaFormValues) =>
      actualizarMarca(apiClient, editing!.id, {
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined
      }),
    onSuccess: () => {
      setEditing(null);
      void invalidate();
    }
  });

  const statusMutation = useMutation({
    mutationFn: (marca: Marca) =>
      cambiarEstadoMarca(apiClient, marca.id, marca.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'),
    onSuccess: () => invalidate()
  });

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Marcas"
        context="Catálogo / Marcas"
        description="Administra las marcas comerciales del catálogo."
        actions={<Button onClick={() => setCreateOpen(true)}>Nueva marca</Button>}
      />

      <ListFilters
        label="Buscar marca"
        placeholder="Código o nombre"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      <div className="mt-6">
        <DataTable<Marca>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Descripción', cell: (row) => row.descripcion ?? '—' },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <div className="flex items-center gap-1">
                  <IconButton
                    icon={Pencil}
                    label={`Editar ${row.nombre}`}
                    onClick={() => setEditing(row)}
                  />
                  <IconButton
                    icon={Power}
                    label={
                      row.estado === 'ACTIVO' ? `Desactivar ${row.nombre}` : `Activar ${row.nombre}`
                    }
                    tone={row.estado === 'ACTIVO' ? 'danger' : 'default'}
                    onClick={() => statusMutation.mutate(row)}
                  />
                </div>
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron marcas."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de marcas."
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

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nueva marca">
        <MarcaForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear marca"
          isSubmitting={createMutation.isPending}
        />
      </Modal>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title="Editar marca">
        {editing ? (
          <MarcaForm
            defaultValues={{
              codigo: editing.codigo,
              nombre: editing.nombre,
              descripcion: editing.descripcion ?? ''
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
