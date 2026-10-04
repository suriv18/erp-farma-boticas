import { Card } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import {
  TerminalSelector,
  cambiarTerminal,
  useEstablecimientos,
  type PuestoTrabajo
} from '../../organizacion';

type PuestoVentaProps = {
  puesto: PuestoTrabajo;
  onChange: (puesto: PuestoTrabajo) => void;
};

export function PuestoVenta({ puesto, onChange }: PuestoVentaProps) {
  const almacenes =
    useEstablecimientos().find(({ id }) => id === puesto.establecimientoId)?.warehouses ?? [];

  return (
    <Card className="space-y-4 p-4">
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
        {almacenes.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
    </Card>
  );
}
