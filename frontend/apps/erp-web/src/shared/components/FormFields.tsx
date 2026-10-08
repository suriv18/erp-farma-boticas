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
  error,
  ...props
}: {
  id: string;
  label: string;
  error?: string | undefined;
} & Omit<ComponentPropsWithRef<'input'>, 'id' | 'type'>) {
  return (
    <div>
      <label
        htmlFor={id}
        className="flex items-center gap-2 text-sm text-neutral-700 dark:text-neutral-200"
      >
        <input
          id={id}
          type="checkbox"
          aria-invalid={error ? true : undefined}
          className="text-primary-600 size-4 rounded border-neutral-300"
          {...props}
        />
        {label}
      </label>
      {error ? (
        <p role="alert" className="text-danger-600 dark:text-danger-400 mt-1 text-xs font-medium">
          {error}
        </p>
      ) : null}
    </div>
  );
}
