import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import { Button, FormField } from '@boticas/ui-web';
import { tiposDocumentoIdentidadApi } from '../../catalogo';
import { usuarioSchema, type UsuarioFormValues } from '../schemas/usuario.schema';

export type UsuarioFormProps = {
  defaultValues?: UsuarioFormValues;
  onSubmit: (values: UsuarioFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function UsuarioForm({
  defaultValues,
  onSubmit,
  submitLabel,
  isSubmitting = false
}: UsuarioFormProps) {
  const {
    formState: { errors },
    getValues,
    handleSubmit,
    register,
    setValue
  } = useForm<UsuarioFormValues>({
    defaultValues: defaultValues ?? {
      documentType: '',
      documentNumber: '',
      firstNames: '',
      lastNames: '',
      username: '',
      email: '',
      phone: '',
      displayName: '',
      mfaRequired: false
    },
    mode: 'onTouched',
    resolver: zodResolver(usuarioSchema)
  });

  const tiposDocumentoIdentidad = useQuery(tiposDocumentoIdentidadApi.listQuery('ACTIVO'));

  useEffect(() => {
    if (defaultValues || !tiposDocumentoIdentidad.data || getValues('documentType')) return;
    const dni = tiposDocumentoIdentidad.data.find((tipo) => tipo.sigla === 'DNI');
    if (dni) setValue('documentType', dni.codigo);
  }, [defaultValues, getValues, setValue, tiposDocumentoIdentidad.data]);

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormField
        label="Nombre visible"
        htmlFor="usuario-display-name"
        error={errors.displayName?.message}
      >
        <input
          id="usuario-display-name"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('displayName')}
        />
      </FormField>

      <div className="grid grid-cols-2 gap-4">
        <FormField
          label="Tipo de documento"
          htmlFor="usuario-document-type"
          error={errors.documentType?.message}
        >
          <select
            id="usuario-document-type"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('documentType')}
          >
            <option value="">Sin especificar</option>
            {tiposDocumentoIdentidad.data?.map((tipo) => (
              <option key={tipo.codigo} value={tipo.codigo}>
                {tipo.sigla}
              </option>
            ))}
          </select>
        </FormField>
        <FormField
          label="Número de documento"
          htmlFor="usuario-document-number"
          error={errors.documentNumber?.message}
        >
          <input
            id="usuario-document-number"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('documentNumber')}
          />
        </FormField>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <FormField label="Nombres" htmlFor="usuario-first-names" error={errors.firstNames?.message}>
          <input
            id="usuario-first-names"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('firstNames')}
          />
        </FormField>
        <FormField label="Apellidos" htmlFor="usuario-last-names" error={errors.lastNames?.message}>
          <input
            id="usuario-last-names"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('lastNames')}
          />
        </FormField>
      </div>

      <FormField label="Correo" htmlFor="usuario-email" error={errors.email?.message}>
        <input
          id="usuario-email"
          type="email"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('email')}
        />
      </FormField>

      <FormField label="Usuario" htmlFor="usuario-username" error={errors.username?.message}>
        <input
          id="usuario-username"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('username')}
        />
      </FormField>

      <FormField label="Teléfono" htmlFor="usuario-phone" error={errors.phone?.message}>
        <input
          id="usuario-phone"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('phone')}
        />
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300">
        <input
          type="checkbox"
          className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
          {...register('mfaRequired')}
        />
        Requiere autenticación multifactor
      </label>

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
