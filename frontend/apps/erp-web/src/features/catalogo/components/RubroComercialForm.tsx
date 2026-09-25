import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button, FormField } from '@boticas/ui-web';
import { rubroComercialSchema, type RubroComercialFormValues } from '../schemas/rubro-comercial.schema';

export type RubroComercialFormProps = {
  defaultValues?: RubroComercialFormValues;
  onSubmit: (values: RubroComercialFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function RubroComercialForm({
  defaultValues,
  onSubmit,
  submitLabel,
  isSubmitting = false
}: RubroComercialFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<RubroComercialFormValues>({
    defaultValues: defaultValues ?? {
      codigo: '',
      nombre: '',
      descripcion: '',
      esFarmaceutico: false,
      orden: 0
    },
    mode: 'onTouched',
    resolver: zodResolver(rubroComercialSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormField label="Código" htmlFor="rubro-codigo" error={errors.codigo?.message}>
        <input
          id="rubro-codigo"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="rubro-nombre" error={errors.nombre?.message}>
        <input
          id="rubro-nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('nombre')}
        />
      </FormField>

      <FormField label="Descripción" htmlFor="rubro-descripcion" error={errors.descripcion?.message}>
        <textarea
          id="rubro-descripcion"
          rows={3}
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('descripcion')}
        />
      </FormField>

      <FormField label="Orden" htmlFor="rubro-orden" error={errors.orden?.message}>
        <input
          id="rubro-orden"
          type="number"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('orden', { valueAsNumber: true })}
        />
      </FormField>

      <label
        htmlFor="rubro-es-farmaceutico"
        className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300"
      >
        <input
          id="rubro-es-farmaceutico"
          type="checkbox"
          className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
          {...register('esFarmaceutico')}
        />
        Es farmacéutico
      </label>

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
