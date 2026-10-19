import { Button, DataTable, Input } from '@boticas/ui-web';
import { formatoMoneda } from '../../../shared/lib/format';
import { Minus, Plus, Trash2 } from 'lucide-react';
import { errorLinea, totalLinea, type Carrito, type LineaCarrito } from '../lib/carrito';

type CarritoTableProps = {
  carrito: Carrito;
  onCambiar: (skuId: string, cambios: Partial<Pick<LineaCarrito, 'cantidad' | 'precio'>>) => void;
  onQuitar: (skuId: string) => void;
};

export function CarritoTable({ carrito, onCambiar, onQuitar }: CarritoTableProps) {
  return (
    <section aria-labelledby="pos-carrito-titulo" className="min-w-0 space-y-3">
      <div className="flex items-center justify-between gap-3">
        <h2 id="pos-carrito-titulo" className="font-semibold">
          Carrito
        </h2>
        <span className="text-sm text-neutral-600 dark:text-neutral-300">
          {carrito.length} {carrito.length === 1 ? 'producto' : 'productos'}
        </span>
      </div>
      <p className="text-xs text-neutral-500 xl:hidden dark:text-neutral-400">
        Desliza la tabla para ver todas las columnas.
      </p>
      <DataTable<LineaCarrito>
        columns={[
          { header: 'N°', cell: (_, index) => <span className="tabular-nums">{index + 1}</span> },
          {
            header: 'Producto',
            cell: (linea) => (
              <div className="max-w-80 min-w-40 break-words">
                <p className="font-medium">{linea.descripcion}</p>
                <p className="text-xs text-neutral-500 dark:text-neutral-400">
                  {linea.codigoInterno} · {linea.unidadVentaCodigo}
                </p>
              </div>
            )
          },
          {
            header: 'Cantidad',
            cell: (linea) => {
              const cantidad = Number(linea.cantidad);
              const anterior = String(Math.round((cantidad - 1) * 10000) / 10000);
              const siguiente = String(Math.round((cantidad + 1) * 10000) / 10000);
              const esValida = (valor: string) =>
                errorLinea({ ...linea, cantidad: valor, precio: '0' }) === null;
              return (
                <div className="flex items-center gap-1">
                  <Button
                    variant="secondary"
                    className="size-11 shrink-0 p-0"
                    aria-label={`Disminuir cantidad de ${linea.codigoInterno}`}
                    title="Disminuir cantidad en 1"
                    disabled={!esValida(anterior)}
                    onClick={() => onCambiar(linea.skuId, { cantidad: anterior })}
                  >
                    <Minus className="size-4" aria-hidden="true" />
                  </Button>
                  <Input
                    className="w-[70px]! shrink-0 px-2! text-center tabular-nums"
                    inputMode="decimal"
                    aria-label={`Cantidad de ${linea.codigoInterno}`}
                    value={linea.cantidad}
                    aria-describedby={errorLinea(linea) ? `error-${linea.skuId}` : undefined}
                    onChange={(event) => onCambiar(linea.skuId, { cantidad: event.target.value })}
                  />
                  <Button
                    variant="secondary"
                    className="size-11 shrink-0 p-0"
                    aria-label={`Aumentar cantidad de ${linea.codigoInterno}`}
                    title="Aumentar cantidad en 1"
                    disabled={!esValida(siguiente)}
                    onClick={() => onCambiar(linea.skuId, { cantidad: siguiente })}
                  >
                    <Plus className="size-4" aria-hidden="true" />
                  </Button>
                </div>
              );
            }
          },
          {
            header: 'Precio unitario',
            cell: (linea) => (
              <Input
                className="w-[95px]! px-2! text-right tabular-nums"
                inputMode="decimal"
                aria-label={`Precio de ${linea.codigoInterno}`}
                value={linea.precio}
                aria-describedby={errorLinea(linea) ? `error-${linea.skuId}` : undefined}
                onChange={(event) => onCambiar(linea.skuId, { precio: event.target.value })}
              />
            )
          },
          {
            header: 'Importe',
            cell: (linea) => {
              const mensaje = errorLinea(linea);
              return (
                <div className="text-right">
                  <p className="font-semibold whitespace-nowrap tabular-nums">
                    {formatoMoneda(totalLinea(linea))}
                  </p>
                  {mensaje ? (
                    <p
                      id={`error-${linea.skuId}`}
                      role="alert"
                      className="text-danger-600 dark:text-danger-400 min-w-40 text-xs"
                    >
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
                className="size-11 p-0"
                title="Quitar producto"
                aria-label={`Quitar ${linea.codigoInterno}`}
                onClick={() => onQuitar(linea.skuId)}
              >
                <Trash2
                  className="text-danger-600 dark:text-danger-400 size-5"
                  aria-hidden="true"
                />
              </Button>
            )
          }
        ]}
        rows={carrito}
        rowKey={(linea) => linea.skuId}
        emptyMessage="El carrito está vacío."
      />
    </section>
  );
}
