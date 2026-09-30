import type { ComponentPropsWithRef } from 'react';
import { FormField, Input, Select } from '@boticas/ui-web';
import type { InputProps, SelectProps } from '@boticas/ui-web';

type FieldProps = {
  id: string;
  label: string;
  error?: string | undefined;
};

export function TextField({ id, label, error, ...props }: FieldProps & Omit<InputProps, 'id'>) {
  return (
    <FormField label={label} htmlFor={id} error={error}>
      <Input id={id} aria-invalid={error ? true : undefined} {...props} />
    </FormField>
  );
}

export function SelectField({
  id,
  label,
  error,
  children,
  ...props
}: FieldProps & Omit<SelectProps, 'id'>) {
  return (
    <FormField label={label} htmlFor={id} error={error}>
      <Select id={id} aria-invalid={error ? true : undefined} {...props}>
        {children}
      </Select>
    </FormField>
  );
}

export function CheckboxField({
  id,
  label,
  ...props
}: { id: string; label: string } & Omit<ComponentPropsWithRef<'input'>, 'id' | 'type'>) {
  return (
    <label
      htmlFor={id}
      className="flex items-center gap-2 text-sm text-neutral-700 dark:text-neutral-200"
    >
      <input
        id={id}
        type="checkbox"
        className="text-primary-600 size-4 rounded border-neutral-300"
        {...props}
      />
      {label}
    </label>
  );
}
