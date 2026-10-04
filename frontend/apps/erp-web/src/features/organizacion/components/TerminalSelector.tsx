import { useQuery } from '@tanstack/react-query';
import { SelectField } from '../../../shared/components/FormFields';
import { terminalesQuery } from '../api/terminales.api';
import { useEstablecimientos } from '../lib/use-establecimientos';

type TerminalSelectorProps = {
  establecimientoId: string;
  terminalId: string;
  onChange: (establecimientoId: string, terminalId: string) => void;
};

export function TerminalSelector({
  establecimientoId,
  terminalId,
  onChange
}: TerminalSelectorProps) {
  const establecimientos = useEstablecimientos();
  const { data: terminales } = useQuery({
    ...terminalesQuery({ establecimientoId, size: 100 }),
    enabled: establecimientoId !== ''
  });
  const activas = (terminales?.items ?? []).filter(({ estado }) => estado === 'ACTIVO');

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <SelectField
        id="puesto-establecimiento"
        label="Establecimiento"
        value={establecimientoId}
        onChange={(event) => onChange(event.target.value, '')}
      >
        <option value="">Selecciona un establecimiento</option>
        {establecimientos.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="puesto-terminal"
        label="Terminal"
        value={terminalId}
        disabled={establecimientoId === ''}
        onChange={(event) => onChange(establecimientoId, event.target.value)}
      >
        <option value="">Selecciona una terminal</option>
        {activas.map(({ id, nombre }) => (
          <option key={id} value={id}>
            {nombre}
          </option>
        ))}
      </SelectField>
    </div>
  );
}
