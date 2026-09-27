import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { Button, Card, EstadoBadge, Modal } from '@boticas/ui-web';
import { ApiError } from '@boticas/api-client';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { actualizarRol, cambiarEstadoRol, reemplazarPermisosRol, rolQuery } from '../api/roles.api';
import { permisosQuery } from '../api/permisos.api';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { PermisosChecklist } from '../components/PermisosChecklist';
import { RolForm } from '../components/RolForm';
import type { RolFormValues } from '../schemas/rol.schema';

export function RoleDetailPage() {
  const { roleId } = useParams<{ roleId: string }>();
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [confirmDeactivateOpen, setConfirmDeactivateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const rolResult = useQuery({
    ...rolQuery(roleId ?? '', tenantId ?? ''),
    enabled: Boolean(roleId) && Boolean(tenantId)
  });
  const rol = rolResult.data;

  const permisosResult = useQuery(permisosQuery({ size: 100 }));

  const [selectedCodes, setSelectedCodes] = useState<Set<string> | null>(null);
  const effectiveCodes = selectedCodes ?? new Set(rol?.permissionCodes ?? []);

  const savePermissionsMutation = useMutation({
    mutationFn: (codes: string[]) => reemplazarPermisosRol(apiClient, roleId as string, codes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (status: string) =>
      cambiarEstadoRol(apiClient, roleId as string, tenantId as string, status),
    onSuccess: () => {
      setConfirmDeactivateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: RolFormValues) =>
      actualizarRol(apiClient, roleId as string, {
        code: values.code,
        name: values.name,
        description: values.description,
        roleType: values.roleType
      }),
    onSuccess: () => {
      setEditOpen(false);
      setEditError(null);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    },
    onError: (error: unknown) => {
      setEditError(error instanceof ApiError ? error.message : 'No se pudo actualizar el rol.');
    }
  });

  if (!rol) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando rol…</p>;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Seguridad / Roles
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            {rol.name}
          </h1>
          <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">{rol.code}</p>
        </div>
        <EstadoBadge status={rol.status} />
      </div>

      <div className="mt-4 flex justify-end gap-2">
        {!rol.systemRole && (
          <Button
            variant="secondary"
            onClick={() => {
              setEditError(null);
              setEditOpen(true);
            }}
          >
            Editar rol
          </Button>
        )}
      </div>

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-neutral-950 dark:text-white">Permisos</h2>
        <div className="mt-4">
          <PermisosChecklist
            permisos={permisosResult.data?.items ?? []}
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
        onConfirm={() =>
          toggleStatusMutation.mutate(rol.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')
        }
        onCancel={() => setConfirmDeactivateOpen(false)}
      />

      <Modal open={editOpen} onClose={() => setEditOpen(false)} title="Editar rol">
        <RolForm
          defaultValues={{
            code: rol.code,
            name: rol.name,
            description: rol.description ?? '',
            roleType: rol.roleType as RolFormValues['roleType'],
            systemRole: rol.systemRole
          }}
          onSubmit={(values) => updateMutation.mutate(values)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
        />
        {editError && (
          <p className="mt-3 text-sm text-red-600 dark:text-red-400" role="alert">
            {editError}
          </p>
        )}
      </Modal>
    </div>
  );
}
