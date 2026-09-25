import { useForm, type DefaultValues, type FieldValues, type Path, type Resolver } from 'react-hook-form';
import { Button, FormField } from '@boticas/ui-web';
import type { FieldDef } from './support-catalog.types';

const inputClassName =
  'focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100';

function toOptionalNumber(value: string): number | undefined {
  return value === '' ? undefined : Number(value);
}

export type SupportCatalogFormProps<TInput extends FieldValues, TValues extends FieldValues> = {
  fields: FieldDef[];
  resolver: Resolver<TInput, unknown, TValues>;
  defaultValues?: TInput;
  onSubmit: (values: TValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function SupportCatalogForm<TInput extends FieldValues, TValues extends FieldValues>({
  fields,
  resolver,
  defaultValues,
  onSubmit,
  submitLabel,
  isSubmitting = false
}: SupportCatalogFormProps<TInput, TValues>) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<TInput, unknown, TValues>({
    mode: 'onChange',
    resolver,
    ...(defaultValues ? { defaultValues: defaultValues as DefaultValues<TInput> } : {})
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {fields.map((field) => {
        const fieldName = field.name as Path<TInput>;
        const fieldError = errors[field.name]?.message as string | undefined;
        const htmlId = `support-catalog-${field.name}`;

        if (field.type === 'checkbox') {
          return (
            <label
              key={field.name}
              htmlFor={htmlId}
              className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300"
            >
              <input
                id={htmlId}
                type="checkbox"
                className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
                {...register(fieldName)}
              />
              {field.label}
            </label>
          );
        }

        if (field.type === 'textarea') {
          return (
            <FormField key={field.name} label={field.label} htmlFor={htmlId} error={fieldError}>
              <textarea
                id={htmlId}
                rows={3}
                className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
                {...register(fieldName)}
              />
            </FormField>
          );
        }

        return (
          <FormField key={field.name} label={field.label} htmlFor={htmlId} error={fieldError}>
            <input
              id={htmlId}
              type={field.type === 'number' ? 'number' : 'text'}
              className={inputClassName}
              {...register(fieldName, field.type === 'number' ? { setValueAs: toOptionalNumber } : {})}
            />
          </FormField>
        );
      })}

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
