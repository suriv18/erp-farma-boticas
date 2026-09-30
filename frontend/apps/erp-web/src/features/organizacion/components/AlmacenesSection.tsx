import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { Button, DataTable, EstadoBadge, IconButton, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarAlmacen, almacenesQuery, crearAlmacen } from '../api/almacenes.api';
import type { Almacen } from '../api/almacenes.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { describeApiError } from '../lib/describe-api-error';
import { toAlmacenFormValues } from '../lib/form-defaults';
import { toActualizarAlmacenPayload, toCrearAlmacenPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { AlmacenFormValues } from '../schemas/almacen.schema';
import { AlmacenForm } from './AlmacenForm';

export function AlmacenesSection({ establecimientoId }: { establecimientoId: string }) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Almacen | null>(null);

  const { data, isPending, isError } = useQuery({
    ...almacenesQuery({ tenantId, establecimientoId, size: 100 }),
    enabled: tenantId !== ''
  });

  const onSaved = () => {
    setCreateOpen(false);
    setEditing(null);
    void invalidateOrganizacion(queryClient);
  };

  const createMutation = useMutation({
    mutationFn: (values: AlmacenFormValues) =>
      crearAlmacen(apiClient, toCrearAlmacenPayload(tenantId, establecimientoId, values)),
    onSuccess: onSaved
  });

  const updateMutation = useMutation({
    mutationFn: ({ almacenId, values }: { almacenId: string; values: AlmacenFormValues }) =>
      actualizarAlmacen(apiClient, almacenId, tenantId, toActualizarAlmacenPayload(values)),
    onSuccess: onSaved
  });

  const closeCreate = () => {
    setCreateOpen(false);
    createMutation.reset();
  };

  const closeEdit = () => {
    setEditing(null);
    updateMutation.reset();
  };

  return (
    <section aria-label="Almacenes" className="mt-8">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold text-neutral-950 dark:text-white">Almacenes</h2>
        <Button onClick={() => setCreateOpen(true)}>Nuevo almacén</Button>
      </div>

      <div className="mt-4">
        <DataTable<Almacen>
          columns={[
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Tipo', cell: (row) => row.tipo },
            {
              header: 'Estado',
              cell: (row) => <EstadoBadge status={row.activo ? 'ACTIVO' : 'INACTIVO'} />
            },
            {
              header: 'Acciones',
              cell: (row) => (
                <IconButton
                  icon={Pencil}
                  label={`Editar ${row.nombre}`}
                  onClick={() => setEditing(row)}
                />
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="Este establecimiento aún no tiene almacenes."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar los almacenes."
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nuevo almacén" size="lg">
        <AlmacenForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear almacén"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>

      <Modal open={editing !== null} onClose={closeEdit} title="Editar almacén" size="lg">
        {editing ? (
          <AlmacenForm
            isEdit
            defaultValues={toAlmacenFormValues(editing)}
            submitLabel="Guardar cambios"
            isSubmitting={updateMutation.isPending}
            error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
            onSubmit={(values) => updateMutation.mutate({ almacenId: editing.id, values })}
          />
        ) : null}
      </Modal>
    </section>
  );
}
