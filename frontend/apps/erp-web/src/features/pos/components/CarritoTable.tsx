import { Button, Card, Input } from '@boticas/ui-web';
import { formatoMoneda } from '../../../shared/lib/format';
import { Minus, Plus, ShoppingBasket, Trash2 } from 'lucide-react';
import { errorLinea, totalLinea, type Carrito, type LineaCarrito } from '../lib/carrito';

type Cambios = Partial<Pick<LineaCarrito, 'cantidad' | 'precio'>>;

type CarritoTableProps = {
  carrito: Carrito;
  onCambiar: (skuId: string, cambios: Cambios) => void;
  onQuitar: (skuId: string) => void;
};

type LineaItemProps = {
  linea: LineaCarrito;
  onCambiar: (cambios: Cambios) => void;
  onQuitar: () => void;
};

const redondear = (valor: number) => String(Math.round(valor * 10000) / 10000);

function BotonCantidad({
  etiqueta,
  titulo,
  icono: Icono,
  deshabilitado,
  onClick
}: {
  etiqueta: string;
  titulo: string;
  icono: typeof Plus;
  deshabilitado: boolean;
  onClick: () => void;
}) {
  return (
    <Button
      variant="secondary"
      className="size-11 shrink-0 p-0"
      aria-label={etiqueta}
      title={titulo}
      disabled={deshabilitado}
      onClick={onClick}
    >
      <Icono className="size-4" aria-hidden="true" />
    </Button>
  );
}

function LineaItem({ linea, onCambiar, onQuitar }: LineaItemProps) {
  const mensaje = errorLinea(linea);
  const cantidad = Number(linea.cantidad);
  const anterior = redondear(cantidad - 1);
  const siguiente = redondear(cantidad + 1);
  const esValida = (valor: string) =>
    errorLinea({ ...linea, cantidad: valor, precio: '0' }) === null;
  const idError = mensaje ? `error-${linea.skuId}` : undefined;

  return (
    <li className="grid gap-x-4 gap-y-3 py-4 sm:grid-cols-[minmax(0,1fr)_auto_auto_auto] sm:items-center">
      <div className="min-w-0">
        <p className="font-medium break-words text-neutral-900 dark:text-neutral-100">
          {linea.descripcion}
        </p>
        <p className="font-mono text-xs text-neutral-500 dark:text-neutral-400">
          {linea.codigoInterno} · {linea.unidadVentaCodigo}
        </p>
      </div>
      <div className="flex items-center gap-1">
        <BotonCantidad
          etiqueta={`Disminuir cantidad de ${linea.codigoInterno}`}
          titulo="Disminuir cantidad en 1"
          icono={Minus}
          deshabilitado={!esValida(anterior)}
          onClick={() => onCambiar({ cantidad: anterior })}
        />
        <Input
          className="w-[70px]! shrink-0 px-2! text-center tabular-nums"
          inputMode="decimal"
          aria-label={`Cantidad de ${linea.codigoInterno}`}
          value={linea.cantidad}
          aria-describedby={idError}
          onChange={(event) => onCambiar({ cantidad: event.target.value })}
        />
        <BotonCantidad
          etiqueta={`Aumentar cantidad de ${linea.codigoInterno}`}
          titulo="Aumentar cantidad en 1"
          icono={Plus}
          deshabilitado={!esValida(siguiente)}
          onClick={() => onCambiar({ cantidad: siguiente })}
        />
      </div>
      <div className="flex items-center gap-2">
        <span aria-hidden="true" className="text-xs text-neutral-500 dark:text-neutral-400">
          S/
        </span>
        <Input
          className="w-[95px]! px-2! text-right tabular-nums"
          inputMode="decimal"
          aria-label={`Precio de ${linea.codigoInterno}`}
          value={linea.precio}
          aria-describedby={idError}
          onChange={(event) => onCambiar({ precio: event.target.value })}
        />
      </div>
      <div className="flex items-center justify-between gap-2 sm:min-w-32 sm:justify-end">
        <p className="text-lg font-semibold whitespace-nowrap text-neutral-950 tabular-nums dark:text-white">
          {formatoMoneda(totalLinea(linea))}
        </p>
        <Button
          variant="ghost"
          className="size-11 p-0"
          title="Quitar producto"
          aria-label={`Quitar ${linea.codigoInterno}`}
          onClick={onQuitar}
        >
          <Trash2 className="text-danger-600 dark:text-danger-400 size-5" aria-hidden="true" />
        </Button>
      </div>
      {mensaje ? (
        <p
          id={idError}
          role="alert"
          className="text-danger-600 dark:text-danger-400 text-xs sm:col-span-full"
        >
          {mensaje}
        </p>
      ) : null}
    </li>
  );
}

export function CarritoTable({ carrito, onCambiar, onQuitar }: CarritoTableProps) {
  return (
    <section aria-labelledby="pos-carrito-titulo" className="min-w-0">
      <Card className="p-5">
        <div className="flex items-center justify-between gap-3">
          <h2 id="pos-carrito-titulo" className="font-semibold">
            Carrito
          </h2>
          <span className="text-sm text-neutral-600 tabular-nums dark:text-neutral-300">
            {carrito.length} {carrito.length === 1 ? 'producto' : 'productos'}
          </span>
        </div>
        {carrito.length === 0 ? (
          <div className="flex flex-col items-center gap-2 py-10 text-center">
            <ShoppingBasket className="size-8 text-neutral-400" aria-hidden="true" />
            <p className="font-medium text-neutral-700 dark:text-neutral-200">
              El carrito está vacío.
            </p>
            <p className="text-sm text-neutral-500 dark:text-neutral-400">
              Busca un producto para agregarlo a la venta.
            </p>
          </div>
        ) : (
          <ul className="mt-2 divide-y divide-neutral-100 dark:divide-neutral-800">
            {carrito.map((linea) => (
              <LineaItem
                key={linea.skuId}
                linea={linea}
                onCambiar={(cambios) => onCambiar(linea.skuId, cambios)}
                onQuitar={() => onQuitar(linea.skuId)}
              />
            ))}
          </ul>
        )}
      </Card>
    </section>
  );
}
