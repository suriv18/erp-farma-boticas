import { Link } from 'react-router';
import { DataTable, type PaginationProps } from '@boticas/ui-web';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import type { VentaResumen } from '../api/ventas.types';
import { EstadoVentaBadge } from './EstadoVentaBadge';

type VentasTableProps = {
  rows: VentaResumen[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
};

export function VentasTable({ rows, isLoading, isError, pagination }: VentasTableProps) {
  return (
    <DataTable<VentaResumen>
      columns={[
        { header: 'N° operación', cell: (row) => row.numeroOperacion },
        { header: 'Fecha', cell: (row) => formatoFechaHora(row.fechaVenta) },
        { header: 'Total', cell: (row) => formatoMoneda(row.total) },
        { header: 'Estado', cell: (row) => <EstadoVentaBadge estado={row.estado} /> },
        {
          header: 'Acciones',
          cell: (row) => (
            <Link
              className="text-primary-600 hover:underline"
              to={`/ventas/${row.id}`}
              aria-label={`Ver detalle de la venta ${row.numeroOperacion}`}
            >
              Ver detalle
            </Link>
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay ventas con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el historial de ventas."
      pagination={pagination}
    />
  );
}
