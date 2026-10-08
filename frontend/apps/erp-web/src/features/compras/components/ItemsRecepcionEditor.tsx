import { Card } from '@boticas/ui-web';
import { TextField } from '../../../shared/components/FormFields';
import {
  errorItem,
  itemIncluido,
  type CambiosItem,
  type ItemBorrador
} from '../lib/recepcion-items';

type ItemsRecepcionEditorProps = {
  items: ItemBorrador[];
  hoy: string;
  mostrarErrores: boolean;
  onCambiar: (numeroLineaOrden: number, cambios: CambiosItem) => void;
};

const CAMPOS: readonly {
  campo: keyof CambiosItem;
  label: string;
  type: 'text' | 'date';
  inputMode: 'text' | 'decimal';
}[] = [
  { campo: 'numeroLote', label: 'Número de lote', type: 'text', inputMode: 'text' },
  { campo: 'fechaFabricacion', label: 'Fecha de fabricación', type: 'date', inputMode: 'text' },
  { campo: 'fechaVencimiento', label: 'Fecha de vencimiento', type: 'date', inputMode: 'text' },
  { campo: 'cantidadRecibida', label: 'Cantidad recibida', type: 'text', inputMode: 'decimal' },
  { campo: 'cantidadRechazada', label: 'Cantidad rechazada', type: 'text', inputMode: 'decimal' },
  { campo: 'motivoRechazo', label: 'Motivo del rechazo', type: 'text', inputMode: 'text' },
  { campo: 'costoUnitario', label: 'Costo unitario', type: 'text', inputMode: 'decimal' }
];

export function ItemsRecepcionEditor({
  items,
  hoy,
  mostrarErrores,
  onCambiar
}: ItemsRecepcionEditorProps) {
  if (items.length === 0) {
    return (
      <p className="text-sm text-neutral-500 dark:text-neutral-400">
        La orden no tiene líneas pendientes de recibir.
      </p>
    );
  }

  return (
    <div className="space-y-4">
      {items.map((item) => {
        const mensaje = mostrarErrores && itemIncluido(item) ? errorItem(item, hoy) : null;
        return (
          <Card key={item.numeroLineaOrden} className="p-5">
            <fieldset className="min-w-0 space-y-4">
              <legend className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                {`Línea ${item.numeroLineaOrden} — ${item.descripcion}`}
              </legend>
              <p className="text-xs text-neutral-500 dark:text-neutral-400">
                {`Pendiente: ${item.cantidadPendiente} ${item.unidadMedidaCodigo}`}
              </p>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {CAMPOS.map(({ campo, label, type, inputMode }) => (
                  <TextField
                    key={campo}
                    id={`recepcion-linea-${item.numeroLineaOrden}-${campo}`}
                    label={label}
                    type={type}
                    inputMode={inputMode}
                    value={item[campo]}
                    onChange={(event) =>
                      onCambiar(item.numeroLineaOrden, { [campo]: event.target.value })
                    }
                  />
                ))}
              </div>
              {mensaje ? (
                <p
                  role="alert"
                  className="text-danger-600 dark:text-danger-400 text-xs font-medium"
                >
                  {mensaje}
                </p>
              ) : null}
            </fieldset>
          </Card>
        );
      })}
      <p role="note" className="text-xs text-neutral-500 dark:text-neutral-400">
        Solo se registran las líneas con cantidad recibida. El costo unitario parte del precio de la
        orden.
      </p>
    </div>
  );
}
