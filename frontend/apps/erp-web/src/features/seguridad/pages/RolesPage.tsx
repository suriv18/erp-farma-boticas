import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearRol, rolesQuery } from '../api/roles.api';
import type { Rol } from '../api/roles.types';
import type { RolFormValues } from '../schemas/rol.schema';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';
import { Modal } from '../components/Modal';
import { RolForm } from '../components/RolForm';

export function RolesPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isPending, isError } = useQuery({
    ...rolesQuery({ tenantId: tenantId ?? '' }),
    enabled: Boolean(tenantId)
  });

  const createMutation = useMutation({
    mutationFn: (values: RolFormValues) =>
      crearRol(apiClient, {
        tenantId: tenantId ?? '',
        code: values.code,
        name: values.name,
        description: values.description,
        roleType: values.roleType,
        systemRole: values.systemRole
      }),
    onSuccess: () => {
      setCreateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Roles</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Roles</h1>
          <p className="mt-2 text-sm text-slate-500">
            Administra los roles y sus permisos asociados.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo rol</Button>
      </div>

      <div className="mt-6">
        <DataTable<Rol>
          columns={[
            {
              header: 'Código',
              cell: (row) => (
                <Link to={`/seguridad/roles/${row.id}`} className="font-semibold text-teal-700 hover:underline">
                  {row.code}
                </Link>
              )
            },
            { header: 'Nombre', cell: (row) => row.name },
            { header: 'Tipo', cell: (row) => row.roleType },
            { header: 'Permisos', cell: (row) => row.permissionCodes.length },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron roles."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de roles."
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo rol">
        <RolForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear rol"
          isSubmitting={createMutation.isPending}
        />
      </Modal>
    </div>
  );
}
