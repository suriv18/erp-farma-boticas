import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { changePasswordSchema, type ChangePasswordValues } from '../schemas/change-password.schema';

type ChangePasswordFormProps = {
  onSubmit: (values: ChangePasswordValues) => Promise<void>;
  submitError?: string | null;
};

const fields = [
  { name: 'currentPassword', label: 'Contraseña actual', autoComplete: 'current-password' },
  { name: 'newPassword', label: 'Nueva contraseña', autoComplete: 'new-password' },
  { name: 'confirmPassword', label: 'Confirmar nueva contraseña', autoComplete: 'new-password' }
] as const;

const inputClassName =
  'focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 mt-2 h-12 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm transition outline-none hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100';

export function ChangePasswordForm({ onSubmit, submitError }: ChangePasswordFormProps) {
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register
  } = useForm<ChangePasswordValues>({
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
    mode: 'onTouched',
    resolver: zodResolver(changePasswordSchema)
  });

  const submit = handleSubmit(onSubmit);

  return (
    <form
      className="mt-8 space-y-5"
      noValidate
      onSubmit={(event) => {
        void submit(event);
      }}
    >
      {submitError ? (
        <p
          role="alert"
          className="border-danger-200 bg-danger-50 text-danger-700 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300 rounded-xl border px-3.5 py-3 text-xs font-medium"
        >
          {submitError}
        </p>
      ) : null}

      {fields.map(({ name, label, autoComplete }) => (
        <div key={name}>
          <label htmlFor={name} className="text-sm font-semibold text-neutral-700 dark:text-neutral-200">
            {label}
          </label>
          <input
            id={name}
            type="password"
            autoComplete={autoComplete}
            aria-describedby={errors[name] ? `${name}-error` : undefined}
            aria-invalid={Boolean(errors[name])}
            className={inputClassName}
            {...register(name)}
          />
          {errors[name] ? (
            <p id={`${name}-error`} role="alert" className="text-danger-600 dark:text-danger-400 mt-1.5 text-xs font-medium">
              {errors[name].message}
            </p>
          ) : null}
        </div>
      ))}

      <Button
        type="submit"
        className="bg-primary-600 hover:bg-primary-700 focus-visible:outline-primary-600 h-12 w-full text-[15px]"
        disabled={isSubmitting}
      >
        {isSubmitting ? (
          <>
            <LoaderCircle className="size-4.5 animate-spin" aria-hidden="true" />
            Actualizando contraseña
          </>
        ) : (
          'Cambiar contraseña'
        )}
      </Button>
    </form>
  );
}
