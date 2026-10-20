import { Card } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import {
  TerminalSelector,
  cambiarTerminal,
  useMiPuesto,
  type PuestoTrabajo
} from '../../organizacion';

type PuestoVentaProps = {
  puesto: PuestoTrabajo;
  onChange: (puesto: PuestoTrabajo) => void;
};

export function PuestoVenta({ puesto, onChange }: PuestoVentaProps) {
  const { establecimientos, isPending } = useMiPuesto();
  const almacenes = (
    establecimientos.find(({ id }) => id === puesto.establecimientoId)?.almacenes ?? []
  ).filter(({ permiteVenta }) => permiteVenta);
  const sinAlmacenes = !isPending && puesto.establecimientoId !== '' && almacenes.length === 0;

  return (
    <Card className="grid gap-4 p-5 md:grid-cols-3 md:[&>*:first-child]:col-span-2">
      <TerminalSelector
        establecimientoId={puesto.establecimientoId}
        terminalId={puesto.terminalId}
        onChange={(establecimientoId, terminalId) =>
          onChange(cambiarTerminal(puesto, establecimientoId, terminalId))
        }
      />
      <SelectField
        id="pos-almacen"
        label="Almacén"
        value={puesto.almacenId}
        disabled={puesto.establecimientoId === ''}
        onChange={(event) => onChange({ ...puesto, almacenId: event.target.value })}
      >
        <option value="">Selecciona un almacén</option>
        {almacenes.map(({ id, nombre }) => (
          <option key={id} value={id}>
            {nombre}
          </option>
        ))}
      </SelectField>
      {sinAlmacenes ? (
        <p role="alert" className="text-danger-600 dark:text-danger-400 text-sm md:col-span-3">
          Este establecimiento no tiene almacenes habilitados para venta.
        </p>
      ) : null}
    </Card>
  );
}
