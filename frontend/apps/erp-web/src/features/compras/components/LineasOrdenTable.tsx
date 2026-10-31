import { DataTable } from '@boticas/ui-web';
import type { LineaOrden } from '../api/ordenes.types';
import { formatoImporte } from '../lib/formato-compras';

type LineasOrdenTableProps = { lineas: LineaOrden[]; moneda: string };

export function LineasOrdenTable({ lineas, moneda }: LineasOrdenTableProps) {
  return (
    <DataTable<LineaOrden>
      columns={[
        { header: 'N°', cell: (linea) => linea.numeroLinea },
        { header: 'Producto', cell: (linea) => linea.descripcion },
        { header: 'Cantidad', cell: (linea) => `${linea.cantidad} ${linea.unidadMedidaCodigo}` },
        { header: 'Precio', cell: (linea) => formatoImporte(linea.precioUnitario, moneda) },
        { header: 'Descuento', cell: (linea) => formatoImporte(linea.descuento, moneda) },
        { header: 'Impuesto', cell: (linea) => formatoImporte(linea.impuesto, moneda) },
        { header: 'Total', cell: (linea) => formatoImporte(linea.totalLinea, moneda) },
        { header: 'Recibido', cell: (linea) => linea.cantidadRecibida },
        { header: 'Pendiente', cell: (linea) => linea.cantidadPendiente }
      ]}
      rows={lineas}
      rowKey={(linea) => String(linea.numeroLinea)}
      emptyMessage="La orden no tiene líneas."
    />
  );
}
