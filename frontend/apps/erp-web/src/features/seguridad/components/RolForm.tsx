import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { rolSchema, type RolFormValues } from '../schemas/rol.schema';
import { FormField } from './FormField';

export type RolFormProps = {
  defaultValues?: RolFormValues;
  onSubmit: (values: RolFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
};

const ROLE_TYPES = ['GLOBAL', 'EMPRESA', 'ESTABLECIMIENTO', 'ALMACEN', 'TERMINAL'] as const;

export function RolForm({ defaultValues, onSubmit, submitLabel, isSubmitting = false }: RolFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<RolFormValues>({
    defaultValues: defaultValues ?? { code: '', name: '', description: '', roleType: 'ESTABLECIMIENTO', systemRole: false },
    mode: 'onTouched',
    resolver: zodResolver(rolSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormField label="Código" htmlFor="rol-code" error={errors.code?.message}>
        <input
          id="rol-code"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('code')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="rol-name" error={errors.name?.message}>
        <input
          id="rol-name"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('name')}
        />
      </FormField>

      <FormField label="Descripción" htmlFor="rol-description" error={errors.description?.message}>
        <textarea
          id="rol-description"
          rows={2}
          className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('description')}
        />
      </FormField>

      <FormField label="Tipo de rol" htmlFor="rol-type" error={errors.roleType?.message}>
        <select
          id="rol-type"
          className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm outline-none focus:border-teal-600 focus:ring-4 focus:ring-teal-100"
          {...register('roleType')}
        >
          {ROLE_TYPES.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </FormField>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-slate-600">
        <input
          type="checkbox"
          className="size-4 rounded border-slate-300 text-teal-700 focus:ring-teal-600"
          {...register('systemRole')}
        />
        Rol de sistema (no editable por usuarios finales)
      </label>

      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
