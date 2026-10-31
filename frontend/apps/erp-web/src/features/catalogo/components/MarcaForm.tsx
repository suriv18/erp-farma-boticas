import { CatalogFormActions } from './CatalogFormActions';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { FormField, Input } from '@boticas/ui-web';
import { marcaSchema, type MarcaFormValues } from '../schemas/marca.schema';

export type MarcaFormProps = {
  defaultValues?: MarcaFormValues;
  onSubmit: (values: MarcaFormValues) => void;
  submitLabel: string;
  onCancel?: (() => void) | undefined;
  isSubmitting?: boolean;
  error?: string | null;
};

export function MarcaForm({
  defaultValues,
  onSubmit,
  submitLabel,
  onCancel,
  isSubmitting = false,
  error = null
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
        <Input
          id="marca-codigo"
          aria-invalid={Boolean(errors.codigo)}
          aria-describedby={errors.codigo ? 'marca-codigo-error' : undefined}
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="marca-nombre" error={errors.nombre?.message}>
        <Input
          id="marca-nombre"
          aria-invalid={Boolean(errors.nombre)}
          aria-describedby={errors.nombre ? 'marca-nombre-error' : undefined}
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
          aria-invalid={Boolean(errors.descripcion)}
          aria-describedby={errors.descripcion ? 'marca-descripcion-error' : undefined}
          rows={3}
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('descripcion')}
        />
      </FormField>

      <CatalogFormActions
        submitLabel={submitLabel}
        isSubmitting={isSubmitting}
        error={error}
        onCancel={onCancel}
      />
    </form>
  );
}
