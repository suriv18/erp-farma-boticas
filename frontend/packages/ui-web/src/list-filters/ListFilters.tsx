import { useId, type ReactNode } from 'react';
import { FormField } from '../form-field/FormField';

export type ListFiltersProps = {
  label: string;
  placeholder: string;
  value: string;
  onValueChange: (value: string) => void;
  children?: ReactNode;
};

export function ListFilters({
  label,
  placeholder,
  value,
  onValueChange,
  children
}: ListFiltersProps) {
  const id = useId();
  return (
    <section
      aria-label="Filtros del listado"
      className="mt-6 flex flex-wrap items-end gap-4 rounded-xl border border-neutral-200 bg-white p-4 dark:border-neutral-800 dark:bg-neutral-900"
    >
      <div className="w-full sm:max-w-md sm:flex-1">
        <FormField label={label} htmlFor={id}>
          <input
            id={id}
            type="search"
            value={value}
            onChange={(event) => onValueChange(event.target.value)}
            placeholder={placeholder}
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          />
        </FormField>
      </div>
      {children}
    </section>
  );
}
