import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { cambiarEstadoDispositivo, dispositivosQuery } from '../api/dispositivos.api';
import type { Dispositivo } from '../api/dispositivos.types';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';

type PendingAction = { device: Dispositivo; nextStatus: string; label: string };

export function DevicesPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);

  const { data, isPending, isError } = useQuery({
    ...dispositivosQuery(tenantId ?? ''),
    enabled: Boolean(tenantId)
  });

  const changeStatusMutation = useMutation({
    mutationFn: () =>
      cambiarEstadoDispositivo(
        apiClient,
        pendingAction?.device.id ?? '',
        tenantId ?? '',
        pendingAction?.nextStatus ?? ''
      ),
    onSuccess: () => {
      setPendingAction(null);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'dispositivos'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Dispositivos"
        context="Seguridad / Dispositivos"
        description="Dispositivos de tienda registrados para operación local."
      />

      <div className="mt-6">
        <DataTable<Dispositivo>
          columns={[
            { header: 'Versión agente', cell: (row) => row.agentVersion ?? '—' },
            { header: 'Huella', cell: (row) => row.fingerprintHash ?? '—' },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
            {
              header: '',
              cell: (row) =>
                row.status === 'PENDIENTE' ? (
                  <Button
                    variant="secondary"
                    onClick={() =>
                      setPendingAction({ device: row, nextStatus: 'CONFIABLE', label: 'Marcar confiable' })
                    }
                  >
                    Marcar confiable
                  </Button>
                ) : row.status === 'CONFIABLE' ? (
                  <Button
                    variant="secondary"
                    onClick={() =>
                      setPendingAction({ device: row, nextStatus: 'BLOQUEADO', label: 'Bloquear' })
                    }
                  >
                    Bloquear
                  </Button>
                ) : null
            }
          ]}
          rows={data ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron dispositivos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de dispositivos."
        />
      </div>

      <ConfirmActionDialog
        open={pendingAction !== null}
        title={pendingAction?.label ?? ''}
        description={
          pendingAction?.nextStatus === 'BLOQUEADO'
            ? 'El dispositivo dejará de poder operar hasta que se restablezca su confianza.'
            : 'El dispositivo quedará habilitado para operar en tienda.'
        }
        confirmLabel="Confirmar"
        tone={pendingAction?.nextStatus === 'BLOQUEADO' ? 'danger' : 'default'}
        isPending={changeStatusMutation.isPending}
        onConfirm={() => changeStatusMutation.mutate()}
        onCancel={() => setPendingAction(null)}
      />
    </div>
  );
}
