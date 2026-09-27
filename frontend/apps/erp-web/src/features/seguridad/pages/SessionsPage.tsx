import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { revocarSesion, sesionesQuery } from '../api/sesiones.api';
import type { Sesion } from '../api/sesiones.types';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';

export function SessionsPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [sessionToRevoke, setSessionToRevoke] = useState<Sesion | null>(null);

  const { data, isPending, isError } = useQuery({
    ...sesionesQuery({ tenantId: tenantId ?? '' }),
    enabled: Boolean(tenantId)
  });

  const revokeMutation = useMutation({
    mutationFn: () => revocarSesion(apiClient, sessionToRevoke?.id ?? '', tenantId ?? ''),
    onSuccess: () => {
      setSessionToRevoke(null);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'sesiones'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Sesiones"
        context="Seguridad / Sesiones"
        description="Sesiones locales de acceso activas y su historial reciente."
      />

      <div className="mt-6">
        <DataTable<Sesion>
          columns={[
            { header: 'Canal', cell: (row) => row.channel },
            { header: 'IP', cell: (row) => row.ipAddress ?? '—' },
            { header: 'Inicio', cell: (row) => new Date(row.loginAt).toLocaleString() },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
            {
              header: '',
              cell: (row) =>
                row.status === 'ACTIVA' ? (
                  <Button variant="secondary" onClick={() => setSessionToRevoke(row)}>
                    Revocar
                  </Button>
                ) : null
            }
          ]}
          rows={data ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron sesiones."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de sesiones."
        />
      </div>

      <ConfirmActionDialog
        open={sessionToRevoke !== null}
        title="Revocar sesión"
        description="El usuario perderá el acceso de esta sesión de inmediato."
        confirmLabel="Revocar sesión"
        tone="danger"
        isPending={revokeMutation.isPending}
        onConfirm={() => revokeMutation.mutate()}
        onCancel={() => setSessionToRevoke(null)}
      />
    </div>
  );
}
