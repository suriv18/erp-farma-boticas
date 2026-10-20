import { useEffect } from 'react';
import { SelectField } from '../../../shared/components/FormFields';
import { useMiPuesto } from '../lib/use-mi-puesto';
import { resolverPuesto } from '../lib/resolver-puesto';

const MOTIVO_NO_OPERABLE: Record<string, string> = {
  BLOQUEADO: 'bloqueada',
  MANTENIMIENTO: 'en mantenimiento'
};

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
  const { establecimientos, isPending } = useMiPuesto();
  const terminales =
    establecimientos.find(({ id }) => id === establecimientoId)?.terminales ?? [];

  useEffect(() => {
    if (isPending) return;
    const siguiente = resolverPuesto(establecimientos, { establecimientoId, terminalId });
    if (siguiente) onChange(siguiente.establecimientoId, siguiente.terminalId);
  }, [isPending, establecimientos, establecimientoId, terminalId, onChange]);

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <SelectField
        id="puesto-establecimiento"
        label="Establecimiento"
        value={establecimientoId}
        onChange={(event) => onChange(event.target.value, '')}
      >
        <option value="">Selecciona un establecimiento</option>
        {establecimientos.map(({ id, nombre }) => (
          <option key={id} value={id}>
            {nombre}
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
        {terminales.map(({ id, nombre, estado, operable }) => (
          <option key={id} value={id} disabled={!operable}>
            {operable
              ? nombre
              : `${nombre} (${MOTIVO_NO_OPERABLE[estado] ?? estado.toLowerCase()})`}
          </option>
        ))}
      </SelectField>
    </div>
  );
}
