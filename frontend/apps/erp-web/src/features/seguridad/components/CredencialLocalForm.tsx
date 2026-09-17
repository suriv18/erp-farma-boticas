import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { credencialLocalSchema, type CredencialLocalFormValues } from '../schemas/credencial-local.schema';
import { FormField } from './FormField';

export type CredencialLocalFormProps = {
  onSubmit: (values: { password: string; requireChange: boolean }) => void;
  isSubmitting?: boolean;
};

export function CredencialLocalForm({ onSubmit, isSubmitting = false }: CredencialLocalFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    reset
  } = useForm<CredencialLocalFormValues>({
    defaultValues: { password: '', confirmPassword: '', requireChange: true },
    mode: 'onTouched',
    resolver: zodResolver(credencialLocalSchema)
  });

  function submit(values: CredencialLocalFormValues) {
    onSubmit({ password: values.password, requireChange: values.requireChange });
    reset();
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
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
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
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('confirmPassword')}
        />
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
          {...register('requireChange')}
        />
        Exigir cambio de contraseña en el próximo inicio de sesión
      </label>

      <Button type="submit" disabled={isSubmitting}>
        Fijar contraseña
      </Button>
    </form>
  );
}
