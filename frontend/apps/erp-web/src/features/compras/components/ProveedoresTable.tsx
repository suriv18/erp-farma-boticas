import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import {
  Badge,
  DataTable,
  EstadoBadge,
  iconButtonClassName,
  type PaginationProps
} from '@boticas/ui-web';
import { valueOrDash } from '../../../shared/lib/format';
import type { Proveedor } from '../api/proveedores.types';
import { rolesProveedor } from '../lib/proveedor-roles';

type ProveedoresTableProps = {
  rows: Proveedor[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
};

export function ProveedoresTable({ rows, isLoading, isError, pagination }: ProveedoresTableProps) {
  return (
    <DataTable<Proveedor>
      columns={[
        { header: 'Documento', cell: (row) => row.numeroDocumento },
        { header: 'Razón social', cell: (row) => row.razonSocial },
        {
          header: 'Tipo',
          cell: (row) => {
            const roles = rolesProveedor(row);
            return roles.length === 0 ? (
              '—'
            ) : (
              <div className="flex flex-wrap gap-1">
                {roles.map((rol) => (
                  <Badge key={rol}>{rol}</Badge>
                ))}
              </div>
            );
          }
        },
        { header: 'Condición de pago', cell: (row) => valueOrDash(row.condicionPagoDefault) },
        { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
        {
          header: 'Acciones',
          cell: (row) => (
            <Link
              to={`/compras/proveedores/${row.id}`}
              aria-label={`Ver detalle de ${row.razonSocial}`}
              title={`Ver detalle de ${row.razonSocial}`}
              className={iconButtonClassName()}
            >
              <Eye className="size-4.5" aria-hidden="true" />
            </Link>
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay proveedores con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el listado de proveedores."
      pagination={pagination}
    />
  );
}
