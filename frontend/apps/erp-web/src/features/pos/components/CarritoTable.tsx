import { Button, DataTable, Input } from '@boticas/ui-web';
import { formatoMoneda } from '../../../shared/lib/format';
import { errorLinea, totalLinea, type Carrito, type LineaCarrito } from '../lib/carrito';

type CarritoTableProps = {
  carrito: Carrito;
  onCambiar: (skuId: string, cambios: Partial<Pick<LineaCarrito, 'cantidad' | 'precio'>>) => void;
  onQuitar: (skuId: string) => void;
};

export function CarritoTable({ carrito, onCambiar, onQuitar }: CarritoTableProps) {
  return (
    <DataTable<LineaCarrito>
      columns={[
        {
          header: 'Producto',
          cell: (linea) => `${linea.codigoInterno} — ${linea.descripcion}`
        },
        {
          header: 'Cantidad',
          cell: (linea) => (
            <div className="flex items-center gap-2">
              <Input
                inputMode="decimal"
                aria-label={`Cantidad de ${linea.codigoInterno}`}
                value={linea.cantidad}
                onChange={(event) => onCambiar(linea.skuId, { cantidad: event.target.value })}
              />
              <span>{linea.unidadVentaCodigo}</span>
            </div>
          )
        },
        {
          header: 'Precio',
          cell: (linea) => (
            <Input
              inputMode="decimal"
              aria-label={`Precio de ${linea.codigoInterno}`}
              value={linea.precio}
              onChange={(event) => onCambiar(linea.skuId, { precio: event.target.value })}
            />
          )
        },
        {
          header: 'Total',
          cell: (linea) => {
            const mensaje = errorLinea(linea);
            return (
              <div>
                <span>{formatoMoneda(totalLinea(linea))}</span>
                {mensaje ? (
                  <p role="alert" className="text-danger-600 dark:text-danger-400 text-xs">
                    {mensaje}
                  </p>
                ) : null}
              </div>
            );
          }
        },
        {
          header: 'Acciones',
          cell: (linea) => (
            <Button
              variant="secondary"
              aria-label={`Quitar ${linea.codigoInterno}`}
              onClick={() => onQuitar(linea.skuId)}
            >
              Quitar
            </Button>
          )
        }
      ]}
      rows={carrito}
      rowKey={(linea) => linea.skuId}
      emptyMessage="El carrito está vacío."
    />
  );
}
