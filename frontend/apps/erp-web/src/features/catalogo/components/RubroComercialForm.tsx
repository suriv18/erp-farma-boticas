import { CatalogFormActions } from './CatalogFormActions';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { FormField, Input } from '@boticas/ui-web';
import {
  rubroComercialSchema,
  type RubroComercialFormValues
} from '../schemas/rubro-comercial.schema';

export type RubroComercialFormProps = {
  defaultValues?: RubroComercialFormValues;
  onSubmit: (values: RubroComercialFormValues) => void;
  submitLabel: string;
  onCancel?: (() => void) | undefined;
  isSubmitting?: boolean;
  error?: string | null;
};

export function RubroComercialForm({
  defaultValues,
  onSubmit,
  submitLabel,
  onCancel,
  isSubmitting = false,
  error = null
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
        <Input
          id="rubro-codigo"
          aria-invalid={Boolean(errors.codigo)}
          aria-describedby={errors.codigo ? 'rubro-codigo-error' : undefined}
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="rubro-nombre" error={errors.nombre?.message}>
        <Input
          id="rubro-nombre"
          aria-invalid={Boolean(errors.nombre)}
          aria-describedby={errors.nombre ? 'rubro-nombre-error' : undefined}
          {...register('nombre')}
        />
      </FormField>

      <FormField
        label="Descripción"
        htmlFor="rubro-descripcion"
        error={errors.descripcion?.message}
      >
        <textarea
          id="rubro-descripcion"
          aria-invalid={Boolean(errors.descripcion)}
          aria-describedby={errors.descripcion ? 'rubro-descripcion-error' : undefined}
          rows={3}
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('descripcion')}
        />
      </FormField>

      <FormField label="Orden" htmlFor="rubro-orden" error={errors.orden?.message}>
        <Input
          id="rubro-orden"
          aria-invalid={Boolean(errors.orden)}
          aria-describedby={errors.orden ? 'rubro-orden-error' : undefined}
          type="number"
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

      <CatalogFormActions
        submitLabel={submitLabel}
        isSubmitting={isSubmitting}
        error={error}
        onCancel={onCancel}
      />
    </form>
  );
}
