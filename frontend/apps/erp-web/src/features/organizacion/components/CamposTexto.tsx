import { get } from 'react-hook-form';
import type { FieldErrors, FieldValues, Path, UseFormRegister } from 'react-hook-form';
import { aMayusculasSinEspacios } from '../lib/form-values';
import { TextField } from './FormFields';

export type CampoTexto<T extends FieldValues> = {
  name: Path<T>;
  id: string;
  label: string;
  inputMode?: 'decimal' | 'numeric' | 'text';
  readOnly?: boolean;
  mayusculas?: boolean;
  deps?: ReadonlyArray<Path<T>>;
};

export type CamposTextoProps<T extends FieldValues> = {
  fields: ReadonlyArray<CampoTexto<T>>;
  register: UseFormRegister<T>;
  errors: FieldErrors<T>;
};

export function CamposTexto<T extends FieldValues>({
  fields,
  register,
  errors
}: CamposTextoProps<T>) {
  return fields.map(({ name, id, label, inputMode, readOnly, mayusculas, deps }) => (
    <TextField
      key={id}
      id={id}
      label={label}
      error={(get(errors, name) as { message?: string } | undefined)?.message}
      {...(inputMode ? { inputMode } : {})}
      {...(readOnly ? { readOnly } : {})}
      {...(mayusculas ? { className: 'uppercase' } : {})}
      {...register(name, {
        ...(mayusculas ? { setValueAs: aMayusculasSinEspacios } : {}),
        ...(deps ? { deps: [...deps] } : {})
      })}
    />
  ));
}
