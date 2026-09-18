import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { permisosQuery } from '../api/permisos.api';
import { DataTable } from '../components/DataTable';
import { EstadoBadge } from '../components/EstadoBadge';

export function PermissionsPage() {
  const [search, setSearch] = useState('');
  const { data, isPending, isError } = useQuery(permisosQuery(search || undefined));

  return (
    <div className="mx-auto max-w-7xl">
      <div>
        <p className="text-sm font-semibold text-teal-700">Seguridad / Permisos</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-950">Permisos</h1>
        <p className="mt-2 text-sm text-slate-500">
          Catálogo de permisos disponibles para asignar a roles.
        </p>
      </div>

      <div className="mt-6">
        <label htmlFor="permisos-search" className="text-sm font-semibold text-slate-700">
          Buscar permiso
        </label>
        <input
          id="permisos-search"
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Código, nombre o módulo"
          className="mt-2 h-11 w-full max-w-md rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
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
