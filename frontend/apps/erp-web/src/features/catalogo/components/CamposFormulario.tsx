import { get } from 'react-hook-form';
import type { FieldErrors, FieldValues, Path, UseFormRegister } from 'react-hook-form';
import { CheckboxField, SelectField, TextField } from '../../../shared/components/FormFields';

export type Opcion = { value: string; label: string };

export type CampoFormulario<T extends FieldValues> = {
  name: Path<T>;
  label: string;
} & (
  | { tipo: 'texto' | 'fecha' | 'decimal' | 'entero' | 'checkbox' }
  | { tipo: 'seleccion'; opciones: readonly Opcion[] }
);

export type CamposFormularioProps<T extends FieldValues> = {
  prefijo: string;
  campos: ReadonlyArray<CampoFormulario<T>>;
  register: UseFormRegister<T>;
  errors: FieldErrors<T>;
};

const TIPO_ENTRADA = { texto: 'text', fecha: 'date', decimal: 'text', entero: 'text' } as const;
const MODO_ENTRADA = { decimal: 'decimal', entero: 'numeric' } as const;

export function CamposFormulario<T extends FieldValues>({
  prefijo,
  campos,
  register,
  errors
}: CamposFormularioProps<T>) {
  return campos.map((campo) => {
    const id = `${prefijo}-${campo.name}`;
    const error = (get(errors, campo.name) as { message?: string } | undefined)?.message;
    const registration = register(campo.name);

    if (campo.tipo === 'checkbox') {
      return <CheckboxField key={id} id={id} label={campo.label} error={error} {...registration} />;
    }

    if (campo.tipo === 'seleccion') {
      return (
        <SelectField key={id} id={id} label={campo.label} error={error} {...registration}>
          <option value="">Seleccione…</option>
          {campo.opciones.map((opcion) => (
            <option key={opcion.value} value={opcion.value}>
              {opcion.label}
            </option>
          ))}
        </SelectField>
      );
    }

    return (
      <TextField
        key={id}
        id={id}
        label={campo.label}
        error={error}
        type={TIPO_ENTRADA[campo.tipo]}
        inputMode={
          campo.tipo === 'decimal' || campo.tipo === 'entero' ? MODO_ENTRADA[campo.tipo] : 'text'
        }
        {...registration}
      />
    );
  });
}
