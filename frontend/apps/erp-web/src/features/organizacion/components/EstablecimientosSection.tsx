import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, DataTable, EstadoBadge, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { invalidateOrganizacion } from '../api/invalidate';
import { crearEstablecimiento, establecimientosQuery } from '../api/establecimientos.api';
import type { Establecimiento } from '../api/establecimientos.types';
import { describeApiError } from '../lib/describe-api-error';
import { toCrearEstablecimientoPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import { EstablecimientoForm } from './EstablecimientoForm';

export function EstablecimientosSection({
  empresaId,
  motivoSinAltas = null
}: {
  empresaId: string;
  motivoSinAltas?: string | null;
}) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isPending, isError } = useQuery({
    ...establecimientosQuery({ tenantId, empresaId, size: 100 }),
    enabled: tenantId !== ''
  });

  const createMutation = useMutation({
    mutationFn: (values: EstablecimientoFormValues) =>
      crearEstablecimiento(apiClient, toCrearEstablecimientoPayload(tenantId, empresaId, values)),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeCreate = () => {
    setCreateOpen(false);
    createMutation.reset();
  };

  return (
    <section aria-label="Establecimientos" className="mt-8">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold text-neutral-950 dark:text-white">Establecimientos</h2>
        <Button
          disabled={motivoSinAltas !== null}
          title={motivoSinAltas ?? undefined}
          onClick={() => setCreateOpen(true)}
        >
          Nuevo establecimiento
        </Button>
      </div>

      <div className="mt-4">
        <DataTable<Establecimiento>
          columns={[
            { header: 'Código', cell: (row) => row.codigo },
            {
              header: 'Nombre',
              cell: (row) => (
                <Link
                  className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  to={`/organizacion/establecimientos/${row.id}`}
                >
                  {row.nombre}
                </Link>
              )
            },
            { header: 'Perfil', cell: (row) => row.perfilOperacion },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estadoOperativo} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="Esta empresa aún no tiene establecimientos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar los establecimientos."
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nuevo establecimiento" size="lg">
        <EstablecimientoForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear establecimiento"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>
    </section>
  );
}
