import type { ReactNode } from 'react';
import { Button, DataTable, Input } from '@boticas/ui-web';
import { formatoImporte } from '../lib/formato-compras';
import {
  errorLinea,
  totalLinea,
  type CambiosLinea,
  type LineaBorrador
} from '../lib/orden-calculo';

type LineasEditorProps = {
  lineas: LineaBorrador[];
  moneda: string;
  mostrarErrores: boolean;
  onCambiar: (skuId: string, cambios: CambiosLinea) => void;
  onQuitar: (skuId: string) => void;
  onRestablecerImpuesto: (skuId: string) => void;
};

type CampoEditable = keyof CambiosLinea;

const columnaEditable = (
  header: string,
  campo: CampoEditable,
  etiqueta: string,
  ancho: string,
  onCambiar: LineasEditorProps['onCambiar'],
  extra?: (linea: LineaBorrador) => ReactNode
) => ({
  header,
  cell: (linea: LineaBorrador) => (
    <div className="flex flex-col items-end gap-1">
      <Input
        className={`${ancho} text-right tabular-nums`}
        inputMode="decimal"
        aria-label={`${etiqueta} de ${linea.codigoInterno}`}
        value={linea[campo]}
        onChange={(event) => onCambiar(linea.skuId, { [campo]: event.target.value })}
      />
      {extra?.(linea)}
    </div>
  )
});

export function LineasEditor({
  lineas,
  moneda,
  mostrarErrores,
  onCambiar,
  onQuitar,
  onRestablecerImpuesto
}: LineasEditorProps) {
  return (
    <div className="space-y-3">
      <DataTable<LineaBorrador>
        columns={[
          {
            header: 'Producto',
            cell: (linea) => (
              <span className="font-medium text-neutral-900 dark:text-neutral-100">
                {`${linea.codigoInterno} — ${linea.descripcion}`}
              </span>
            )
          },
          columnaEditable('Cantidad', 'cantidad', 'Cantidad', 'w-24', onCambiar),
          columnaEditable('Precio', 'precio', 'Precio', 'w-28', onCambiar),
          columnaEditable('Descuento', 'descuento', 'Descuento', 'w-24', onCambiar),
          columnaEditable('Impuesto', 'impuesto', 'Impuesto', 'w-24', onCambiar, (linea) =>
            linea.impuestoManual ? (
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Usar impuesto sugerido de ${linea.codigoInterno}`}
                onClick={() => onRestablecerImpuesto(linea.skuId)}
              >
                Sugerido
              </Button>
            ) : null
          ),
          {
            header: 'Total',
            cell: (linea) => {
              const mensaje = mostrarErrores ? errorLinea(linea) : null;
              return (
                <div className="text-right">
                  <span className="font-semibold tabular-nums">
                    {formatoImporte(totalLinea(linea), moneda)}
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
          columnaEditable(
            'Exceso %',
            'toleranciaExceso',
            'Tolerancia de exceso',
            'w-20',
            onCambiar
          ),
          columnaEditable(
            'Defecto %',
            'toleranciaDefecto',
            'Tolerancia de defecto',
            'w-20',
            onCambiar
          ),
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
        rows={lineas}
        rowKey={(linea) => linea.skuId}
        emptyMessage="Aún no agregaste productos."
      />
      <p role="note" className="text-xs text-neutral-500 dark:text-neutral-400">
        El impuesto se sugiere al 18% en los productos afectos a IGV y puedes corregirlo.
      </p>
    </div>
  );
}
