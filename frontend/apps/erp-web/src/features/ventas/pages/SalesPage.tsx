import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@boticas/ui-web';
import { useEstablecimientos } from '../../organizacion';
import { ventasQuery } from '../api/ventas.api';
import { FiltrosVentas } from '../components/FiltrosVentas';
import { VentasTable } from '../components/VentasTable';
import { finDelDia, inicioDelDia } from '../lib/fechas';
import { useVentasFiltros } from '../lib/use-ventas-filtros';

export function SalesPage() {
  const { filtros, setEstablecimiento, setDesde, setHasta, setPage, setSize } = useVentasFiltros();
  const establecimientos = useEstablecimientos();
  const { data, isPending, isError } = useQuery(
    ventasQuery({
      establecimientoId: filtros.establecimientoId,
      desde: filtros.desde === '' ? undefined : inicioDelDia(filtros.desde),
      hasta: filtros.hasta === '' ? undefined : finDelDia(filtros.hasta),
      page: filtros.page,
      size: filtros.size
    })
  );

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Ventas"
        context="Operaciones / Ventas"
        description="Historial de ventas por local y fecha."
      />
      <FiltrosVentas
        establecimientos={establecimientos}
        filtros={filtros}
        onEstablecimiento={setEstablecimiento}
        onDesde={setDesde}
        onHasta={setHasta}
      />
      <div className="mt-6">
        <VentasTable
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
