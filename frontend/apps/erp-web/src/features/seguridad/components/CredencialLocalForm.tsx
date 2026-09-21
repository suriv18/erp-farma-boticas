import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import {
  credencialLocalSchema,
  type CredencialLocalFormValues
} from '../schemas/credencial-local.schema';
import { FormField } from './FormField';

export type CredencialLocalFormProps = {
  onSubmit: (values: { password: string; requireChange: boolean }) => void;
  isSubmitting?: boolean;
  errorMessage?: string | undefined;
};

export function CredencialLocalForm({
  onSubmit,
  isSubmitting = false,
  errorMessage
}: CredencialLocalFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<CredencialLocalFormValues>({
    defaultValues: { password: '', confirmPassword: '', requireChange: true },
    mode: 'onTouched',
    resolver: zodResolver(credencialLocalSchema)
  });

  function submit(values: CredencialLocalFormValues) {
    onSubmit({ password: values.password, requireChange: values.requireChange });
  }

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit(submit)(event);
      }}
    >
      <FormField label="Contraseña" htmlFor="credencial-password" error={errors.password?.message}>
        <input
          id="credencial-password"
          type="password"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('password')}
        />
      </FormField>

      <FormField
        label="Confirmar contraseña"
        htmlFor="credencial-confirm"
        error={errors.confirmPassword?.message}
      >
        <input
          id="credencial-confirm"
          type="password"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('confirmPassword')}
        />
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-neutral-600 dark:text-neutral-300">
        <input
          type="checkbox"
          className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
          {...register('requireChange')}
        />
        Exigir cambio de contraseña en el próximo inicio de sesión
      </label>

      {errorMessage ? (
        <p role="alert" className="text-danger-700 dark:text-danger-400 text-sm font-medium">
          {errorMessage}
        </p>
      ) : null}

      <Button type="submit" disabled={isSubmitting}>
        Fijar contraseña
      </Button>
    </form>
  );
}
