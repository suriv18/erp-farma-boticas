import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { Button, Card } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import {
  asignarRolUsuario,
  cambiarEstadoUsuario,
  desvincularIdentidadExterna,
  identidadesExternasQuery,
  provisionarCredencialLocal,
  revocarAsignacionRol,
  usuarioAsignacionesQuery,
  usuariosQuery,
  vincularIdentidadExterna
} from '../api/usuarios.api';
import type { AsignacionRol, IdentidadExterna, VincularIdentidadPayload } from '../api/usuarios.types';
import { AsignarRolDialog } from '../components/AsignarRolDialog';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { CredencialLocalForm } from '../components/CredencialLocalForm';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';
import { IdentidadExternaForm } from '../components/IdentidadExternaForm';
import { Modal } from '../components/Modal';
import type { AsignacionRolFormValues } from '../schemas/asignacion-rol.schema';

export function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();

  const [confirmStatusOpen, setConfirmStatusOpen] = useState(false);
  const [assignRoleOpen, setAssignRoleOpen] = useState(false);
  const [revokeAssignmentId, setRevokeAssignmentId] = useState<string | null>(null);
  const [linkIdentityOpen, setLinkIdentityOpen] = useState(false);
  const [unlinkIdentity, setUnlinkIdentity] = useState<IdentidadExterna | null>(null);

  const usuariosResult = useQuery({
    ...usuariosQuery({ tenantId: tenantId ?? '', size: 100 }),
    enabled: Boolean(tenantId)
  });
  const usuario = usuariosResult.data?.items.find((item) => item.id === userId);

  const asignacionesResult = useQuery({
    ...usuarioAsignacionesQuery(userId ?? '', tenantId ?? ''),
    enabled: Boolean(userId && tenantId)
  });

  const identidadesResult = useQuery({
    ...identidadesExternasQuery(userId ?? '', tenantId ?? ''),
    enabled: Boolean(userId && tenantId)
  });

  const invalidateAsignaciones = () =>
    queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios', userId, 'asignaciones-rol'] });
  const invalidateIdentidades = () =>
    queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios', userId, 'identidades-externas'] });

  const toggleStatusMutation = useMutation({
    mutationFn: (status: string) => cambiarEstadoUsuario(apiClient, userId ?? '', tenantId ?? '', status),
    onSuccess: () => {
      setConfirmStatusOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
    }
  });

  const assignRoleMutation = useMutation({
    mutationFn: (values: AsignacionRolFormValues) =>
      asignarRolUsuario(apiClient, userId ?? '', {
        tenantId: tenantId ?? '',
        roleId: values.roleId,
        scopeType: values.scopeType,
        companyId: values.companyId || undefined,
        establishmentId: values.establishmentId || undefined,
        warehouseId: values.warehouseId || undefined,
        terminalId: values.terminalId || undefined,
        validFrom: values.validFrom || undefined,
        validUntil: values.validUntil || undefined
      }),
    onSuccess: () => {
      setAssignRoleOpen(false);
      void invalidateAsignaciones();
    }
  });

  const revokeAssignmentMutation = useMutation({
    mutationFn: (assignmentId: string) => revocarAsignacionRol(apiClient, userId ?? '', assignmentId, tenantId ?? ''),
    onSuccess: () => {
      setRevokeAssignmentId(null);
      void invalidateAsignaciones();
    }
  });

  const linkIdentityMutation = useMutation({
    mutationFn: (payload: VincularIdentidadPayload) =>
      vincularIdentidadExterna(apiClient, userId ?? '', tenantId ?? '', payload),
    onSuccess: () => {
      setLinkIdentityOpen(false);
      void invalidateIdentidades();
    }
  });

  const unlinkIdentityMutation = useMutation({
    mutationFn: (identity: IdentidadExterna) =>
      desvincularIdentidadExterna(apiClient, userId ?? '', tenantId ?? '', identity.provider, identity.subject),
    onSuccess: () => {
      setUnlinkIdentity(null);
      void invalidateIdentidades();
    }
  });

  const provisionCredentialMutation = useMutation({
    mutationFn: (values: { password: string; requireChange: boolean }) =>
      provisionarCredencialLocal(apiClient, userId ?? '', {
        tenantId: tenantId ?? '',
        password: values.password,
        requireChange: values.requireChange
      })
  });

  if (!usuario) {
    if (usuariosResult.isError || asignacionesResult.isError || identidadesResult.isError) {
      return <p className="text-sm text-rose-700">No se pudo cargar la información.</p>;
    }
    return <p className="text-sm text-slate-500">Cargando usuario…</p>;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Usuarios</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">
            {usuario.displayName ?? usuario.email ?? usuario.username ?? usuario.id}
          </h1>
          <p className="mt-1 text-sm text-slate-500">{usuario.email}</p>
        </div>
        <EstadoBadge status={usuario.status} />
      </div>

      <div className="mt-4 flex justify-end">
        <Button variant="secondary" onClick={() => setConfirmStatusOpen(true)}>
          {usuario.status === 'ACTIVO' ? 'Desactivar usuario' : 'Activar usuario'}
        </Button>
      </div>

      <Card className="mt-6 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-bold text-slate-950">Roles asignados</h2>
          <Button size="sm" onClick={() => setAssignRoleOpen(true)}>
            Asignar rol
          </Button>
        </div>
        <div className="mt-4">
          <DataTable<AsignacionRol>
            columns={[
              { header: 'Rol', cell: (row) => row.roleName },
              { header: 'Ámbito', cell: (row) => row.scopeType },
              { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
              {
                header: '',
                cell: (row) => (
                  <Button size="sm" variant="secondary" onClick={() => setRevokeAssignmentId(row.id)}>
                    Revocar
                  </Button>
                )
              }
            ]}
            rows={asignacionesResult.data ?? []}
            rowKey={(row) => row.id}
            emptyMessage="Sin roles asignados."
            isLoading={asignacionesResult.isPending}
            isError={asignacionesResult.isError}
            errorMessage="No se pudo cargar la información."
          />
        </div>
      </Card>

      <Card className="mt-6 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-bold text-slate-950">Identidades externas</h2>
          <Button size="sm" onClick={() => setLinkIdentityOpen(true)}>
            Vincular identidad
          </Button>
        </div>
        <div className="mt-4">
          <DataTable<IdentidadExterna>
            columns={[
              { header: 'Proveedor', cell: (row) => row.provider },
              { header: 'Identificador', cell: (row) => row.subject },
              {
                header: '',
                cell: (row) => (
                  <Button size="sm" variant="secondary" onClick={() => setUnlinkIdentity(row)}>
                    Desvincular
                  </Button>
                )
              }
            ]}
            rows={identidadesResult.data ?? []}
            rowKey={(row) => `${row.provider}:${row.subject}`}
            emptyMessage="Sin identidades externas vinculadas."
            isLoading={identidadesResult.isPending}
            isError={identidadesResult.isError}
            errorMessage="No se pudo cargar la información."
          />
        </div>
      </Card>

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-slate-950">Credencial local</h2>
        <p className="mt-1 text-sm text-slate-500">Fija una contraseña inicial para el acceso local del usuario.</p>
        <div className="mt-4">
          <CredencialLocalForm
            onSubmit={(values) => provisionCredentialMutation.mutate(values)}
            isSubmitting={provisionCredentialMutation.isPending}
          />
        </div>
      </Card>

      <ConfirmActionDialog
        open={confirmStatusOpen}
        title={usuario.status === 'ACTIVO' ? 'Desactivar usuario' : 'Activar usuario'}
        description={
          usuario.status === 'ACTIVO'
            ? 'El usuario perderá acceso al sistema.'
            : 'El usuario recuperará acceso al sistema.'
        }
        confirmLabel={usuario.status === 'ACTIVO' ? 'Desactivar' : 'Activar'}
        tone={usuario.status === 'ACTIVO' ? 'danger' : 'default'}
        isPending={toggleStatusMutation.isPending}
        onConfirm={() => toggleStatusMutation.mutate(usuario.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')}
        onCancel={() => setConfirmStatusOpen(false)}
      />

      <AsignarRolDialog
        open={assignRoleOpen}
        tenantId={tenantId ?? ''}
        isSubmitting={assignRoleMutation.isPending}
        onSubmit={(values) => assignRoleMutation.mutate(values)}
        onCancel={() => setAssignRoleOpen(false)}
      />

      <ConfirmActionDialog
        open={revokeAssignmentId !== null}
        title="Revocar asignación de rol"
        description="El usuario perderá los permisos asociados a este rol en este ámbito."
        confirmLabel="Confirmar revocación"
        tone="danger"
        isPending={revokeAssignmentMutation.isPending}
        onConfirm={() => revokeAssignmentMutation.mutate(revokeAssignmentId ?? '')}
        onCancel={() => setRevokeAssignmentId(null)}
      />

      <Modal open={linkIdentityOpen} onClose={() => setLinkIdentityOpen(false)} title="Vincular identidad externa">
        <IdentidadExternaForm
          submitLabel="Vincular"
          isSubmitting={linkIdentityMutation.isPending}
          onSubmit={(payload) => linkIdentityMutation.mutate(payload)}
          onCancel={() => setLinkIdentityOpen(false)}
        />
      </Modal>

      <ConfirmActionDialog
        open={unlinkIdentity !== null}
        title="Desvincular identidad externa"
        description="El usuario ya no podrá iniciar sesión con este proveedor."
        confirmLabel="Desvincular"
        tone="danger"
        isPending={unlinkIdentityMutation.isPending}
        onConfirm={() => {
          if (unlinkIdentity) unlinkIdentityMutation.mutate(unlinkIdentity);
        }}
        onCancel={() => setUnlinkIdentity(null)}
      />
    </div>
  );
}
