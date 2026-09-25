import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, Modal } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import {
  actualizarRubroComercial,
  cambiarEstadoRubroComercial,
  crearRubroComercial,
  rubrosComercialesQuery
} from '../api/rubros-comerciales.api';
import type { RubroComercial } from '../api/rubros-comerciales.types';
import type { RubroComercialFormValues } from '../schemas/rubro-comercial.schema';
import { RubroComercialForm } from '../components/RubroComercialForm';
import { Pagination } from '../components/Pagination';

const PAGE_SIZE = 20;

export function RubrosComercialesPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<RubroComercial | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const { data, isPending, isError } = useQuery({
    ...rubrosComercialesQuery({ tenantId: tenantId ?? '', q: search || undefined, page, size: PAGE_SIZE }),
    enabled: Boolean(tenantId)
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo', 'rubros-comerciales'] });

  const createMutation = useMutation({
    mutationFn: (values: RubroComercialFormValues) =>
      crearRubroComercial(apiClient, {
        tenantId: tenantId ?? '',
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined,
        esFarmaceutico: values.esFarmaceutico,
        orden: values.orden
      }),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidate();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: RubroComercialFormValues) =>
      actualizarRubroComercial(apiClient, editing!.id, {
        tenantId: tenantId!,
        codigo: values.codigo,
        nombre: values.nombre,
        descripcion: values.descripcion || undefined,
        esFarmaceutico: values.esFarmaceutico,
        orden: values.orden
      }),
    onSuccess: () => {
      setEditing(null);
      void invalidate();
    }
  });

  const statusMutation = useMutation({
    mutationFn: (rubro: RubroComercial) =>
      cambiarEstadoRubroComercial(
        apiClient,
        rubro.id,
        tenantId!,
        rubro.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
      ),
    onSuccess: () => invalidate()
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Catálogo / Rubros comerciales
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            Rubros comerciales
          </h1>
          <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
            Administra los rubros comerciales del catálogo.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo rubro comercial</Button>
      </div>

      <div className="mt-6">
        <label
          htmlFor="rubros-search"
          className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
        >
          Buscar rubro comercial
        </label>
        <input
          id="rubros-search"
          type="search"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
          placeholder="Código o nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 mt-2 h-11 w-full max-w-md rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
        />
      </div>

      <div className="mt-6">
        <DataTable<RubroComercial>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Farmacéutico', cell: (row) => (row.esFarmaceutico ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditing(row)}
                    className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  >
                    Editar {row.nombre}
                  </button>
                  <button
                    type="button"
                    onClick={() => statusMutation.mutate(row)}
                    className="font-semibold text-neutral-600 hover:underline dark:text-neutral-300"
                  >
                    {row.estado === 'ACTIVO' ? `Desactivar ${row.nombre}` : `Activar ${row.nombre}`}
                  </button>
                </div>
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron rubros comerciales."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de rubros comerciales."
          startIndex={page * PAGE_SIZE}
        />
        <Pagination
          page={data?.page ?? page}
          size={data?.size ?? PAGE_SIZE}
          totalElements={data?.totalElements ?? 0}
          onPageChange={setPage}
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo rubro comercial">
        <RubroComercialForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear rubro comercial"
          isSubmitting={createMutation.isPending}
        />
      </Modal>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title="Editar rubro comercial">
        {editing ? (
          <RubroComercialForm
            defaultValues={{
              codigo: editing.codigo,
              nombre: editing.nombre,
              descripcion: editing.descripcion ?? '',
              esFarmaceutico: editing.esFarmaceutico,
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
