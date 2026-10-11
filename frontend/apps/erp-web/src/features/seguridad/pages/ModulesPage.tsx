import { useQuery } from '@tanstack/react-query';
import { DataTable, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { modulosQuery } from '../api/modulos.api';

export function ModulesPage() {
  const { data, isPending, isError } = useQuery(modulosQuery());

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Módulos"
        context="Seguridad / Módulos"
        description="Catálogo de módulos del sistema que agrupan el catálogo de permisos."
      />

      <div className="mt-6">
        <DataTable
          columns={[
            { header: 'Código', cell: (row) => row.code },
            { header: 'Nombre', cell: (row) => row.name },
            { header: 'Descripción', cell: (row) => row.description ?? '—' },
            { header: 'Orden', cell: (row) => row.order },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.active ? 'ACTIVO' : 'INACTIVO'} /> }
          ]}
          rows={data ?? []}
          rowKey={(row) => row.code}
          emptyMessage="No se encontraron módulos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el catálogo de módulos."
        />
      </div>
    </div>
  );
}
