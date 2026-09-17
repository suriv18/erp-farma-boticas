import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { usuarioSchema, type UsuarioFormValues } from '../schemas/usuario.schema';
import { FormField } from './FormField';

export type UsuarioFormProps = {
  defaultValues?: UsuarioFormValues;
  onSubmit: (values: UsuarioFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

export function UsuarioForm({ defaultValues, onSubmit, submitLabel, isSubmitting = false }: UsuarioFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<UsuarioFormValues>({
    defaultValues:
      defaultValues ?? {
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

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormField label="Nombre visible" htmlFor="usuario-display-name" error={errors.displayName?.message}>
        <input
          id="usuario-display-name"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('displayName')}
        />
      </FormField>

      <div className="grid grid-cols-2 gap-4">
        <FormField label="Nombres" htmlFor="usuario-first-names" error={errors.firstNames?.message}>
          <input
            id="usuario-first-names"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('firstNames')}
          />
        </FormField>
        <FormField label="Apellidos" htmlFor="usuario-last-names" error={errors.lastNames?.message}>
          <input
            id="usuario-last-names"
            className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
            {...register('lastNames')}
          />
        </FormField>
      </div>

      <FormField label="Correo" htmlFor="usuario-email" error={errors.email?.message}>
        <input
          id="usuario-email"
          type="email"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('email')}
        />
      </FormField>

      <FormField label="Usuario" htmlFor="usuario-username" error={errors.username?.message}>
        <input
          id="usuario-username"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('username')}
        />
      </FormField>

      <FormField label="Teléfono" htmlFor="usuario-phone" error={errors.phone?.message}>
        <input
          id="usuario-phone"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('phone')}
        />
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
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
