import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
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
import {
  actualizarRubroComercial,
  cambiarEstadoRubroComercial,
  crearRubroComercial,
  rubrosComercialesQuery
} from '../api/rubros-comerciales.api';
import type { RubroComercial } from '../api/rubros-comerciales.types';
import type { RubroComercialFormValues } from '../schemas/rubro-comercial.schema';
import { RubroComercialForm } from '../components/RubroComercialForm';

export function RubrosComercialesPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<RubroComercial | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data, isPending, isError } = useQuery(
    rubrosComercialesQuery({
      q: search || undefined,
      page,
      size: size
    })
  );

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['catalogo', 'rubros-comerciales'] });

  const createMutation = useMutation({
    mutationFn: (values: RubroComercialFormValues) =>
      crearRubroComercial(apiClient, {
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
        rubro.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
      ),
    onSuccess: () => invalidate()
  });

  return (
    <div className="erp-page">
      <PageHeader
        title="Rubros comerciales"
        context="Catálogo / Rubros comerciales"
        description="Administra los rubros comerciales del catálogo."
        actions={
          <Button
            onClick={() => {
              createMutation.reset();
              setCreateOpen(true);
            }}
          >
            Nuevo rubro comercial
          </Button>
        }
      />

      <ListFilters
        label="Buscar rubro comercial"
        placeholder="Código o nombre"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      {statusMutation.error ? (
        <div className="mt-4">
          <FormError message={describeApiError(statusMutation.error)} />
        </div>
      ) : null}

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
                <div className="flex items-center gap-1">
                  <IconButton
                    icon={Pencil}
                    label={`Editar ${row.nombre}`}
                    onClick={() => {
                      updateMutation.reset();
                      setEditing(row);
                    }}
                  />
                  <IconButton
                    icon={Power}
                    label={
                      row.estado === 'ACTIVO' ? `Desactivar ${row.nombre}` : `Activar ${row.nombre}`
                    }
                    tone={row.estado === 'ACTIVO' ? 'danger' : 'default'}
                    disabled={statusMutation.isPending}
                    onClick={() => statusMutation.mutate(row)}
                  />
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
          startIndex={page * size}
          pagination={{
            page,
            size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: (value) => {
              setSize(value);
              setPage(0);
            }
          }}
        />
      </div>

      <Modal
        open={createOpen}
        onClose={() => {
          if (!createMutation.isPending) setCreateOpen(false);
        }}
        title="Nuevo rubro comercial"
      >
        <RubroComercialForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear rubro comercial"
          isSubmitting={createMutation.isPending}
          error={createMutation.error ? describeApiError(createMutation.error) : null}
          onCancel={() => setCreateOpen(false)}
        />
      </Modal>

      <Modal
        open={editing !== null}
        onClose={() => {
          if (!updateMutation.isPending) setEditing(null);
        }}
        title="Editar rubro comercial"
      >
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
            error={updateMutation.error ? describeApiError(updateMutation.error) : null}
            onCancel={() => setEditing(null)}
          />
        ) : null}
      </Modal>
    </div>
  );
}
