import { DataTable } from '@boticas/ui-web';
import { formatoMoneda } from '../../../shared/lib/format';
import type { LineaVenta } from '../api/ventas.types';

export function LineasVentaTable({ lineas }: { lineas: LineaVenta[] }) {
  return (
    <DataTable<LineaVenta>
      columns={[
        {
          header: 'Producto',
          cell: (linea) => (
            <span className="block max-w-md min-w-40 font-medium [overflow-wrap:anywhere]">
              {linea.descripcion}
            </span>
          )
        },
        {
          header: 'Cantidad',
          cell: (linea) => (
            <span className="whitespace-nowrap tabular-nums">
              {linea.cantidad} {linea.unidadVentaCodigo}
            </span>
          )
        },
        {
          header: 'Precio unitario',
          cell: (linea) => (
            <span className="block text-right whitespace-nowrap tabular-nums">
              {formatoMoneda(linea.precioUnitario)}
            </span>
          )
        },
        {
          header: 'Importe',
          cell: (linea) => (
            <span className="block text-right font-semibold whitespace-nowrap tabular-nums">
              {formatoMoneda(linea.totalLinea)}
            </span>
          )
        },
        {
          header: 'Lotes',
          cell: (linea) =>
            linea.lotes.length === 0 ? (
              '—'
            ) : (
              <ul>
                {linea.lotes.map((lote) => (
                  <li
                    key={lote.loteId}
                    className="min-w-32 text-xs leading-5 [overflow-wrap:anywhere]"
                  >
                    Lote {lote.loteId} × {lote.cantidad}
                  </li>
                ))}
              </ul>
            )
        }
      ]}
      rows={lineas}
      rowKey={(linea) => String(linea.numeroLinea)}
      emptyMessage="La venta no tiene líneas."
    />
  );
}
