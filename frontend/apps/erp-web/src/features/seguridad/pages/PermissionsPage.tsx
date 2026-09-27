import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { DataTable, EstadoBadge, PageHeader, ListFilters } from '@boticas/ui-web';
import { permisosQuery } from '../api/permisos.api';

export function PermissionsPage() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const { data, isPending, isError } = useQuery(permisosQuery({ search: search || undefined, page, size }));
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Permisos"
        context="Seguridad / Permisos"
        description="Catálogo de permisos disponibles para asignar a roles."
      />

      <ListFilters
        label="Buscar permiso"
        placeholder="Código, nombre o módulo"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      <div className="mt-6">
        <DataTable
          columns={[
            { header: 'Módulo', cell: (row) => row.moduleName },
            { header: 'Código', cell: (row) => row.code },
            { header: 'Nombre', cell: (row) => row.name },
            { header: 'Crítico', cell: (row) => (row.critical ? 'Sí' : 'No') },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.status} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.code}
          emptyMessage="No se encontraron permisos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el catálogo de permisos."
          startIndex={page * size}
          pagination={{
            page,
            size,
            totalElements,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>
    </div>
  );
}
