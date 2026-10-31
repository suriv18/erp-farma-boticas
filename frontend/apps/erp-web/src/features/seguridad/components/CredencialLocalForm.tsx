import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button, FormField, Input } from '@boticas/ui-web';
import {
  credencialLocalSchema,
  type CredencialLocalFormValues
} from '../schemas/credencial-local.schema';

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
        <Input id="credencial-password" type="password" {...register('password')} />
      </FormField>

      <FormField
        label="Confirmar contraseña"
        htmlFor="credencial-confirm"
        error={errors.confirmPassword?.message}
      >
        <Input id="credencial-confirm" type="password" {...register('confirmPassword')} />
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
