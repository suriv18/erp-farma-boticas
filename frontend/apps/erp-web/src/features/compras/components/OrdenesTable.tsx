import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import { DataTable, iconButtonClassName, type PaginationProps } from '@boticas/ui-web';
import type { OrdenResumen } from '../api/ordenes.types';
import { formatoFecha, formatoImporte } from '../lib/formato-compras';
import { EstadoOrdenBadge } from './EstadoOrdenBadge';

type OrdenesTableProps = {
  rows: OrdenResumen[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
};

export function OrdenesTable({ rows, isLoading, isError, pagination }: OrdenesTableProps) {
  return (
    <DataTable<OrdenResumen>
      columns={[
        { header: 'Número', cell: (row) => row.numero },
        { header: 'Proveedor', cell: (row) => row.proveedorRazonSocial },
        { header: 'Emisión', cell: (row) => formatoFecha(row.fechaEmision) },
        { header: 'Entrega estimada', cell: (row) => formatoFecha(row.fechaEntregaEstimada) },
        { header: 'Total', cell: (row) => formatoImporte(row.total, row.moneda) },
        { header: 'Estado', cell: (row) => <EstadoOrdenBadge estado={row.estado} /> },
        {
          header: 'Acciones',
          cell: (row) => (
            <Link
              to={`/compras/ordenes/${row.id}`}
              aria-label={`Ver detalle de la orden ${row.numero}`}
              title={`Ver detalle de la orden ${row.numero}`}
              className={iconButtonClassName()}
            >
              <Eye className="size-4.5" aria-hidden="true" />
            </Link>
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay órdenes de compra con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el listado de órdenes."
      pagination={pagination}
    />
  );
}
