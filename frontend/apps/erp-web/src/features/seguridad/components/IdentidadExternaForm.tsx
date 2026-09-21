import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import type { VincularIdentidadPayload } from '../api/usuarios.types';
import {
  identidadExternaSchema,
  KNOWN_PROVIDERS,
  type IdentidadExternaFormValues
} from '../schemas/identidad-externa.schema';
import { FormField } from './FormField';

export type IdentidadExternaFormProps = {
  onSubmit: (payload: VincularIdentidadPayload) => void;
  onCancel: () => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function IdentidadExternaForm({
  onSubmit,
  onCancel,
  submitLabel,
  isSubmitting = false
}: IdentidadExternaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    watch
  } = useForm<IdentidadExternaFormValues>({
    defaultValues: { providerOption: 'GOOGLE', providerCustom: '', subject: '', issuer: '', emailClaim: '' },
    mode: 'onTouched',
    resolver: zodResolver(identidadExternaSchema)
  });

  const providerOption = watch('providerOption');

  function submit(values: IdentidadExternaFormValues) {
    onSubmit({
      provider: values.providerOption === 'OTRO' ? (values.providerCustom ?? '').trim() : values.providerOption,
      subject: values.subject,
      issuer: values.issuer || undefined,
      emailClaim: values.emailClaim || undefined
    });
  }

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(submit)(event);
      }}
    >
      <FormField label="Proveedor" htmlFor="identidad-provider" error={errors.providerOption?.message}>
        <select
          id="identidad-provider"
          className="h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:border-primary-600 focus:ring-4 focus:ring-primary-100 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:focus:ring-primary-900/40"
          {...register('providerOption')}
        >
          {KNOWN_PROVIDERS.map((provider) => (
            <option key={provider} value={provider}>
              {provider}
            </option>
          ))}
          <option value="OTRO">Otro</option>
        </select>
      </FormField>

      {providerOption === 'OTRO' ? (
        <FormField label="Nombre del proveedor" htmlFor="identidad-provider-custom" error={errors.providerCustom?.message}>
          <input
            id="identidad-provider-custom"
            className="h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:border-primary-600 focus:ring-4 focus:ring-primary-100 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:focus:ring-primary-900/40"
            {...register('providerCustom')}
          />
        </FormField>
      ) : null}

      <FormField label="Identificador (subject)" htmlFor="identidad-subject" error={errors.subject?.message}>
        <input
          id="identidad-subject"
          className="h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:border-primary-600 focus:ring-4 focus:ring-primary-100 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:focus:ring-primary-900/40"
          {...register('subject')}
        />
      </FormField>

      <FormField label="Emisor (opcional)" htmlFor="identidad-issuer" error={errors.issuer?.message}>
        <input
          id="identidad-issuer"
          className="h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:border-primary-600 focus:ring-4 focus:ring-primary-100 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:focus:ring-primary-900/40"
          {...register('issuer')}
        />
      </FormField>

      <FormField label="Correo asociado (opcional)" htmlFor="identidad-email" error={errors.emailClaim?.message}>
        <input
          id="identidad-email"
          type="email"
          className="h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:border-primary-600 focus:ring-4 focus:ring-primary-100 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:focus:ring-primary-900/40"
          {...register('emailClaim')}
        />
      </FormField>

      <div className="flex justify-end gap-3">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
