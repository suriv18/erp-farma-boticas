import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@boticas/ui-web';
import { ordenesQuery } from '../api/ordenes.api';
import { proveedoresQuery } from '../api/proveedores.api';
import { FiltrosOrdenes } from '../components/FiltrosOrdenes';
import { OrdenesTable } from '../components/OrdenesTable';
import { useOrdenesFiltros } from '../lib/use-ordenes-filtros';

export function OrdenesPage() {
  const { filtros, setProveedor, setEstado, setPage, setSize } = useOrdenesFiltros();
  const { data: proveedores } = useQuery(proveedoresQuery({ size: 100 }));
  const { data, isPending, isError } = useQuery(
    ordenesQuery({
      proveedorId: filtros.proveedorId === '' ? undefined : filtros.proveedorId,
      estado: filtros.estado === '' ? undefined : filtros.estado,
      page: filtros.page,
      size: filtros.size
    })
  );

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Órdenes de compra"
        context="Compras / Órdenes"
        description="Seguimiento de las órdenes de compra y su recepción."
      />
      <FiltrosOrdenes
        proveedores={proveedores?.items ?? []}
        filtros={filtros}
        onProveedor={setProveedor}
        onEstado={setEstado}
      />
      <div className="mt-6">
        <OrdenesTable
          rows={data?.items ?? []}
          isLoading={isPending}
          isError={isError}
          pagination={{
            page: filtros.page,
            size: filtros.size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>
    </div>
  );
}
