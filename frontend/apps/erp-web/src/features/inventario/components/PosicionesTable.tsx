import { Badge, DataTable, type PaginationProps } from '@boticas/ui-web';
import type { Posicion } from '../api/inventario.types';
import { codigoCorto } from '../lib/formato';
import { AccionesPosicion } from './AccionesPosicion';
import { EstadoLoteBadge } from './EstadoLoteBadge';
import { VencimientoCelda } from './VencimientoCelda';

type PosicionesTableProps = {
  rows: Posicion[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
  onAjustar: (posicion: Posicion) => void;
};

export function PosicionesTable({
  rows,
  isLoading,
  isError,
  pagination,
  onAjustar
}: PosicionesTableProps) {
  return (
    <DataTable<Posicion>
      columns={[
        { header: 'SKU', cell: (row) => codigoCorto(row.skuId) },
        { header: 'Lote', cell: (row) => row.numeroLote },
        { header: 'Vencimiento', cell: (row) => <VencimientoCelda fecha={row.fechaVencimiento} /> },
        { header: 'Físico', cell: (row) => row.cantidadFisica },
        { header: 'Reservado', cell: (row) => row.cantidadReservada },
        { header: 'Disponible', cell: (row) => row.cantidadDisponible },
        {
          header: 'Estado',
          cell: (row) => (
            <div className="flex flex-wrap items-center gap-1">
              <EstadoLoteBadge estado={row.estadoLote} />
              {row.vendible ? null : <Badge tone="warning">No vendible</Badge>}
            </div>
          )
        },
        {
          header: 'Acciones',
          cell: (row) => (
            <AccionesPosicion
              loteId={row.loteId}
              numeroLote={row.numeroLote}
              onAjustar={() => onAjustar(row)}
            />
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay stock registrado con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el inventario."
      pagination={pagination}
    />
  );
}
