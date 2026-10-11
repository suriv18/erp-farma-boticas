import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import { Button, DataTable, EstadoBadge, Modal, PageHeader, iconButtonClassName } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearRol, rolesQuery } from '../api/roles.api';
import type { Rol } from '../api/roles.types';
import type { RolFormValues } from '../schemas/rol.schema';
import { RolForm } from '../components/RolForm';

export function RolesPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isPending, isError } = useQuery({
    ...rolesQuery({ tenantId: tenantId ?? '', page, size }),
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
      <PageHeader
        title="Roles"
        context="Seguridad / Roles"
        description="Administra los roles y sus permisos asociados."
        actions={<Button onClick={() => setCreateOpen(true)}>Nuevo rol</Button>}
      />

      <div className="mt-6">
        <DataTable<Rol>
          columns={[
            { header: 'Código', cell: (row) => row.code },
            { header: 'Nombre', cell: (row) => row.name },
            { header: 'Tipo', cell: (row) => row.roleType },
            { header: 'Permisos', cell: (row) => row.permissionCodes.length },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <Link
                  to={`/seguridad/roles/${row.id}`}
                  aria-label={`Ver detalle de ${row.name}`}
                  title={`Ver detalle de ${row.name}`}
                  className={iconButtonClassName()}
                >
                  <Eye className="size-4.5" aria-hidden="true" />
                </Link>
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron roles."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de roles."
          startIndex={page * size}
          pagination={{
            page,
            size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
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
