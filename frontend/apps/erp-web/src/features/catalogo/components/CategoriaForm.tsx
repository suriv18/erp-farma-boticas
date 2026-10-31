import { CatalogFormActions } from './CatalogFormActions';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { FormField, Input } from '@boticas/ui-web';
import { categoriaSchema, type CategoriaFormValues } from '../schemas/categoria.schema';

export type CategoriaFormProps = {
  defaultValues?: CategoriaFormValues;
  onSubmit: (values: CategoriaFormValues) => void;
  submitLabel: string;
  onCancel?: (() => void) | undefined;
  isSubmitting?: boolean;
  error?: string | null;
};

export function CategoriaForm({
  defaultValues,
  onSubmit,
  submitLabel,
  onCancel,
  isSubmitting = false,
  error = null
}: CategoriaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<CategoriaFormValues>({
    defaultValues: defaultValues ?? { codigo: '', nombre: '', descripcion: '', nivel: 1, orden: 0 },
    mode: 'onTouched',
    resolver: zodResolver(categoriaSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormField label="Código" htmlFor="categoria-codigo" error={errors.codigo?.message}>
        <Input
          id="categoria-codigo"
          aria-invalid={Boolean(errors.codigo)}
          aria-describedby={errors.codigo ? 'categoria-codigo-error' : undefined}
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="categoria-nombre" error={errors.nombre?.message}>
        <Input
          id="categoria-nombre"
          aria-invalid={Boolean(errors.nombre)}
          aria-describedby={errors.nombre ? 'categoria-nombre-error' : undefined}
          {...register('nombre')}
        />
      </FormField>

      <FormField
        label="Descripción"
        htmlFor="categoria-descripcion"
        error={errors.descripcion?.message}
      >
        <textarea
          id="categoria-descripcion"
          aria-invalid={Boolean(errors.descripcion)}
          aria-describedby={errors.descripcion ? 'categoria-descripcion-error' : undefined}
          rows={3}
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 w-full rounded-xl border border-neutral-200 bg-white px-4 py-2.5 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('descripcion')}
        />
      </FormField>

      <div className="grid grid-cols-2 gap-4">
        <FormField label="Nivel" htmlFor="categoria-nivel" error={errors.nivel?.message}>
          <Input
            id="categoria-nivel"
            aria-invalid={Boolean(errors.nivel)}
            aria-describedby={errors.nivel ? 'categoria-nivel-error' : undefined}
            type="number"
            min={1}
            {...register('nivel', { valueAsNumber: true })}
          />
        </FormField>
        <FormField label="Orden" htmlFor="categoria-orden" error={errors.orden?.message}>
          <Input
            id="categoria-orden"
            aria-invalid={Boolean(errors.orden)}
            aria-describedby={errors.orden ? 'categoria-orden-error' : undefined}
            type="number"
            min={0}
            {...register('orden', { valueAsNumber: true })}
          />
        </FormField>
      </div>

      <CatalogFormActions
        submitLabel={submitLabel}
        isSubmitting={isSubmitting}
        error={error}
        onCancel={onCancel}
      />
    </form>
  );
}
