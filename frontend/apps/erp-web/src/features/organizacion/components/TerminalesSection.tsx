import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { Button, DataTable, EstadoBadge, IconButton, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { invalidateOrganizacion } from '../api/invalidate';
import { actualizarTerminal, crearTerminal, terminalesQuery } from '../api/terminales.api';
import type { Terminal } from '../api/terminales.types';
import { describeApiError } from '../lib/describe-api-error';
import { toTerminalFormValues } from '../lib/form-defaults';
import { toActualizarTerminalPayload, toCrearTerminalPayload } from '../lib/form-payloads';
import { valueOrDash } from '../lib/format';
import { useTenantId } from '../lib/use-tenant-id';
import type { TerminalFormValues } from '../schemas/terminal.schema';
import { TerminalForm } from './TerminalForm';

export function TerminalesSection({
  establecimientoId,
  motivoSinAltas = null
}: {
  establecimientoId: string;
  motivoSinAltas?: string | null;
}) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Terminal | null>(null);

  const { data, isPending, isError } = useQuery({
    ...terminalesQuery({ tenantId, establecimientoId, size: 100 }),
    enabled: tenantId !== ''
  });

  const onSaved = () => {
    setCreateOpen(false);
    setEditing(null);
    void invalidateOrganizacion(queryClient);
  };

  const createMutation = useMutation({
    mutationFn: (values: TerminalFormValues) =>
      crearTerminal(apiClient, toCrearTerminalPayload(tenantId, establecimientoId, values)),
    onSuccess: onSaved
  });

  const updateMutation = useMutation({
    mutationFn: ({ terminalId, values }: { terminalId: string; values: TerminalFormValues }) =>
      actualizarTerminal(apiClient, terminalId, tenantId, toActualizarTerminalPayload(values)),
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
    <section aria-label="Terminales POS" className="mt-8">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold text-neutral-950 dark:text-white">Terminales POS</h2>
        <Button
          disabled={motivoSinAltas !== null}
          title={motivoSinAltas ?? undefined}
          onClick={() => setCreateOpen(true)}
        >
          Nuevo terminal
        </Button>
      </div>

      <div className="mt-4">
        <DataTable<Terminal>
          columns={[
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Dirección IP', cell: (row) => valueOrDash(row.ipEquipo) },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
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
          emptyMessage="Este establecimiento aún no tiene terminales."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar los terminales."
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nuevo terminal" size="lg">
        <TerminalForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear terminal"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>

      <Modal open={editing !== null} onClose={closeEdit} title="Editar terminal" size="lg">
        {editing ? (
          <TerminalForm
            isEdit
            defaultValues={toTerminalFormValues(editing)}
            submitLabel="Guardar cambios"
            isSubmitting={updateMutation.isPending}
            error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
            onSubmit={(values) => updateMutation.mutate({ terminalId: editing.id, values })}
          />
        ) : null}
      </Modal>
    </section>
  );
}
