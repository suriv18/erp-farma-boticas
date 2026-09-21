import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { marcaSchema, type MarcaFormValues } from '../schemas/marca.schema';
import { FormField } from './FormField';

export type MarcaFormProps = {
  defaultValues?: MarcaFormValues;
  onSubmit: (values: MarcaFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function MarcaForm({
  defaultValues,
  onSubmit,
  submitLabel,
  isSubmitting = false
}: MarcaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<MarcaFormValues>({
    defaultValues: defaultValues ?? { codigo: '', nombre: '', descripcion: '' },
    mode: 'onTouched',
    resolver: zodResolver(marcaSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormField label="Código" htmlFor="marca-codigo" error={errors.codigo?.message}>
        <input
          id="marca-codigo"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="marca-nombre" error={errors.nombre?.message}>
        <input
          id="marca-nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('nombre')}
        />
      </FormField>

      <FormField
        label="Descripción"
        htmlFor="marca-descripcion"
        error={errors.descripcion?.message}
      >
        <textarea
          id="marca-descripcion"
          rows={3}
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('descripcion')}
        />
      </FormField>

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
