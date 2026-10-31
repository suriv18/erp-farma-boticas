import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { abrirTurnoSchema, type AbrirTurnoFormValues } from '../schemas/turno.schema';

type AbrirTurnoFormProps = {
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (values: AbrirTurnoFormValues) => void;
};

export function AbrirTurnoForm({ isSubmitting, error, onSubmit }: AbrirTurnoFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<AbrirTurnoFormValues>({
    defaultValues: { fondoInicial: '' },
    mode: 'onTouched',
    resolver: zodResolver(abrirTurnoSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <TextField
        id="turno-fondo"
        label="Fondo inicial"
        inputMode="decimal"
        error={errors.fondoInicial?.message}
        {...register('fondoInicial')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        Abrir turno
      </Button>
    </form>
  );
}
