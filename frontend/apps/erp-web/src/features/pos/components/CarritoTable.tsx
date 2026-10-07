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
          cell: (linea) => (
            <span className="font-medium text-neutral-900 dark:text-neutral-100">{`${linea.codigoInterno} — ${linea.descripcion}`}</span>
          )
        },
        {
          header: 'Cantidad',
          cell: (linea) => (
            <div className="flex items-center gap-2">
              <Input
                className="w-24 text-right tabular-nums"
                inputMode="decimal"
                aria-label={`Cantidad de ${linea.codigoInterno}`}
                value={linea.cantidad}
                onChange={(event) => onCambiar(linea.skuId, { cantidad: event.target.value })}
              />
              <span className="text-xs text-neutral-500 dark:text-neutral-400">
                {linea.unidadVentaCodigo}
              </span>
            </div>
          )
        },
        {
          header: 'Precio',
          cell: (linea) => (
            <Input
              className="w-28 text-right tabular-nums"
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
              <div className="text-right">
                <span className="font-semibold tabular-nums">
                  {formatoMoneda(totalLinea(linea))}
                </span>
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
              variant="ghost"
              size="sm"
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
