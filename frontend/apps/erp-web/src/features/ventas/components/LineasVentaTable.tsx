import { DataTable } from '@boticas/ui-web';
import { formatoMoneda } from '../../../shared/lib/format';
import type { LineaVenta } from '../api/ventas.types';

export function LineasVentaTable({ lineas }: { lineas: LineaVenta[] }) {
  return (
    <DataTable<LineaVenta>
      columns={[
        { header: 'Producto', cell: (linea) => linea.descripcion },
        { header: 'Cantidad', cell: (linea) => `${linea.cantidad} ${linea.unidadVentaCodigo}` },
        { header: 'Precio', cell: (linea) => formatoMoneda(linea.precioUnitario) },
        { header: 'Total', cell: (linea) => formatoMoneda(linea.totalLinea) },
        {
          header: 'Lotes',
          cell: (linea) =>
            linea.lotes.length === 0 ? (
              '—'
            ) : (
              <ul>
                {linea.lotes.map((lote) => (
                  <li key={lote.loteId}>
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
