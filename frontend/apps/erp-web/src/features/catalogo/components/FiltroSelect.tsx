import { FormField, Select } from '@boticas/ui-web';
import type { Opcion } from './CamposFormulario';

export type FiltroSelectProps = {
  id: string;
  label: string;
  value: string;
  onValueChange: (value: string) => void;
  opciones: readonly Opcion[];
  opcionVacia: string;
};

export function FiltroSelect({
  id,
  label,
  value,
  onValueChange,
  opciones,
  opcionVacia
}: FiltroSelectProps) {
  return (
    <div className="w-full sm:w-56">
      <FormField label={label} htmlFor={id}>
        <Select id={id} value={value} onChange={(event) => onValueChange(event.target.value)}>
          <option value="">{opcionVacia}</option>
          {opciones.map((opcion) => (
            <option key={opcion.value} value={opcion.value}>
              {opcion.label}
            </option>
          ))}
        </Select>
      </FormField>
    </div>
  );
}
