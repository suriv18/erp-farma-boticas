import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button } from '@boticas/ui-web';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { crearUsuario, usuariosQuery } from '../api/usuarios.api';
import type { Usuario } from '../api/usuarios.types';
import type { UsuarioFormValues } from '../schemas/usuario.schema';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';
import { Modal } from '../components/Modal';
import { UsuarioForm } from '../components/UsuarioForm';

export function UsersPage() {
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');

  const { data, isPending, isError } = useQuery({
    ...usuariosQuery({ tenantId: tenantId ?? '', search: search || undefined }),
    enabled: Boolean(tenantId)
  });

  const createMutation = useMutation({
    mutationFn: (values: UsuarioFormValues) =>
      crearUsuario(apiClient, {
        tenantId: tenantId ?? '',
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
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'usuarios'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-700">Seguridad / Usuarios</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Usuarios</h1>
          <p className="mt-2 text-sm text-slate-500">
            Administra las cuentas de acceso al sistema.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Nuevo usuario</Button>
      </div>

      <div className="mt-6">
        <label htmlFor="usuarios-search" className="text-sm font-semibold text-slate-700">
          Buscar usuario
        </label>
        <input
          id="usuarios-search"
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Nombre, correo o documento"
          className="mt-2 h-11 w-full max-w-md rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
        />
      </div>

      <div className="mt-6">
        <DataTable<Usuario>
          columns={[
            {
              header: 'Nombre',
              cell: (row) => (
                <Link to={`/seguridad/usuarios/${row.id}`} className="font-semibold text-teal-700 hover:underline">
                  {row.displayName ?? row.email ?? row.username ?? row.id}
                </Link>
              )
            },
            { header: 'Correo', cell: (row) => row.email ?? '—' },
            { header: 'MFA', cell: (row) => (row.mfaRequired ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron usuarios."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de usuarios."
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo usuario">
        <UsuarioForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear usuario"
          isSubmitting={createMutation.isPending}
        />
      </Modal>
    </div>
  );
}
