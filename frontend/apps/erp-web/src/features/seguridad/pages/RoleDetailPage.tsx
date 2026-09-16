import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { Button, Card } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { cambiarEstadoRol, reemplazarPermisosRol, rolesQuery } from '../api/roles.api';
import { permisosQuery } from '../api/permisos.api';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { EstadoBadge } from '../components/EstadoBadge';
import { PermisosChecklist } from '../components/PermisosChecklist';

export function RoleDetailPage() {
  const { roleId } = useParams<{ roleId: string }>();
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [confirmDeactivateOpen, setConfirmDeactivateOpen] = useState(false);

  const rolesResult = useQuery({
    ...rolesQuery({ tenantId: tenantId ?? '', size: 100 }),
    enabled: Boolean(tenantId)
  });
  const rol = rolesResult.data?.items.find((item) => item.id === roleId);

  const permisosResult = useQuery(permisosQuery());

  const [selectedCodes, setSelectedCodes] = useState<Set<string> | null>(null);
  const effectiveCodes = selectedCodes ?? new Set(rol?.permissionCodes ?? []);

  const savePermissionsMutation = useMutation({
    mutationFn: (codes: string[]) => reemplazarPermisosRol(apiClient, roleId ?? '', codes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (status: string) => cambiarEstadoRol(apiClient, roleId ?? '', tenantId ?? '', status),
    onSuccess: () => {
      setConfirmDeactivateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  if (!rol) {
    return <p className="text-sm text-slate-500">Cargando rol…</p>;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Roles</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">{rol.name}</h1>
          <p className="mt-1 text-sm text-slate-500">{rol.code}</p>
        </div>
        <EstadoBadge status={rol.status} />
      </div>

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-slate-950">Permisos</h2>
        <div className="mt-4">
          <PermisosChecklist
            permisos={permisosResult.data ?? []}
            selectedCodes={effectiveCodes}
            onChange={setSelectedCodes}
          />
        </div>
        <div className="mt-4 flex justify-end">
          <Button
            onClick={() => savePermissionsMutation.mutate([...effectiveCodes])}
            disabled={savePermissionsMutation.isPending}
          >
            Guardar permisos
          </Button>
        </div>
      </Card>

      <div className="mt-6 flex justify-end">
        <Button variant="secondary" onClick={() => setConfirmDeactivateOpen(true)}>
          {rol.status === 'ACTIVO' ? 'Desactivar rol' : 'Activar rol'}
        </Button>
      </div>

      <ConfirmActionDialog
        open={confirmDeactivateOpen}
        title={rol.status === 'ACTIVO' ? 'Desactivar rol' : 'Activar rol'}
        description={
          rol.status === 'ACTIVO'
            ? 'Los usuarios con este rol perderán los permisos asociados.'
            : 'El rol volverá a estar disponible para asignación.'
        }
        confirmLabel={rol.status === 'ACTIVO' ? 'Desactivar' : 'Activar'}
        tone={rol.status === 'ACTIVO' ? 'danger' : 'default'}
        isPending={toggleStatusMutation.isPending}
        onConfirm={() => toggleStatusMutation.mutate(rol.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')}
        onCancel={() => setConfirmDeactivateOpen(false)}
      />
    </div>
  );
}
