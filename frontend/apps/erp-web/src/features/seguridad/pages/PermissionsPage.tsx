import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { DataTable, EstadoBadge } from '@boticas/ui-web';
import { permisosQuery } from '../api/permisos.api';

export function PermissionsPage() {
  const [search, setSearch] = useState('');
  const { data, isPending, isError } = useQuery(permisosQuery(search || undefined));

  return (
    <div className="mx-auto max-w-7xl">
      <div>
        <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
          Seguridad / Permisos
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
          Permisos
        </h1>
        <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
          Catálogo de permisos disponibles para asignar a roles.
        </p>
      </div>

      <div className="mt-6">
        <label
          htmlFor="permisos-search"
          className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
        >
          Buscar permiso
        </label>
        <input
          id="permisos-search"
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Código, nombre o módulo"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 mt-2 h-11 w-full max-w-md rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
        />
      </div>

      <div className="mt-6">
        <DataTable
          columns={[
            { header: 'Módulo', cell: (row) => row.moduleName },
            { header: 'Código', cell: (row) => row.code },
            { header: 'Nombre', cell: (row) => row.name },
            { header: 'Crítico', cell: (row) => (row.critical ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> }
          ]}
          rows={data ?? []}
          rowKey={(row) => row.code}
          emptyMessage="No se encontraron permisos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el catálogo de permisos."
        />
      </div>
    </div>
  );
}
