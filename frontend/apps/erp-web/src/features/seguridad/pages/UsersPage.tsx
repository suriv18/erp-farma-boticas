import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import { ApiError } from '@boticas/api-client';
import {
  Button,
  DataTable,
  EstadoBadge,
  Modal,
  PageHeader,
  ListFilters,
  iconButtonClassName
} from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearUsuario, usuariosQuery } from '../api/usuarios.api';
import type { Usuario } from '../api/usuarios.types';
import type { UsuarioFormValues } from '../schemas/usuario.schema';
import { UsuarioForm } from '../components/UsuarioForm';

export function UsersPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [createError, setCreateError] = useState<string | null>(null);

  const { data, isPending, isError } = useQuery({
    ...usuariosQuery({ tenantId: tenantId ?? '', search: search || undefined, page, size }),
    enabled: Boolean(tenantId)
  });

  const createMutation = useMutation({
    mutationFn: (values: UsuarioFormValues) =>
      crearUsuario(apiClient, {
        tenantId: tenantId ?? '',
        documentType: values.documentType || undefined,
        documentNumber: values.documentNumber || undefined,
        displayName: values.displayName || undefined,
        firstNames: values.firstNames || undefined,
        lastNames: values.lastNames || undefined,
        email: values.email || undefined,
        username: values.username || undefined,
        phone: values.phone || undefined,
        mfaRequired: values.mfaRequired
      }),
    onSuccess: () => {
      setCreateOpen(false);
      setCreateError(null);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
    },
    onError: (error: unknown) => {
      setCreateError(error instanceof ApiError ? error.message : 'No se pudo crear el usuario.');
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Usuarios"
        context="Seguridad / Usuarios"
        description="Administra las cuentas de acceso al sistema."
        actions={
          <Button
            onClick={() => {
              setCreateError(null);
              setCreateOpen(true);
            }}
          >
            Nuevo usuario
          </Button>
        }
      />

      <ListFilters
        label="Buscar usuario"
        placeholder="Nombre, correo o documento"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      <div className="mt-6">
        <DataTable<Usuario>
          columns={[
            {
              header: 'Nombre',
              cell: (row) => row.displayName ?? row.email ?? row.username ?? row.id
            },
            { header: 'Correo', cell: (row) => row.email ?? '—' },
            { header: 'MFA', cell: (row) => (row.mfaRequired ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <Link
                  to={`/seguridad/usuarios/${row.id}`}
                  aria-label={`Ver detalle de ${row.displayName ?? row.email ?? row.username ?? row.id}`}
                  title={`Ver detalle de ${row.displayName ?? row.email ?? row.username ?? row.id}`}
                  className={iconButtonClassName()}
                >
                  <Eye className="size-4.5" aria-hidden="true" />
                </Link>
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron usuarios."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de usuarios."
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

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo usuario">
        <UsuarioForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear usuario"
          isSubmitting={createMutation.isPending}
        />
        {createError && (
          <p className="mt-3 text-sm text-red-600 dark:text-red-400" role="alert">
            {createError}
          </p>
        )}
      </Modal>
    </div>
  );
}
