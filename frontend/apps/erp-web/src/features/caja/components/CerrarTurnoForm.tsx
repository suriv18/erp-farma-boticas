import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, useWatch } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { formatoMoneda } from '../../../shared/lib/format';
import { diferenciaArqueo } from '../lib/arqueo';
import {
  PATRON_MONTO,
  cerrarTurnoSchema,
  type CerrarTurnoFormValues
} from '../schemas/turno.schema';

type CerrarTurnoFormProps = {
  totalEstimado: number;
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (values: CerrarTurnoFormValues) => void;
};

export function CerrarTurnoForm({
  totalEstimado,
  isSubmitting,
  error,
  onSubmit
}: CerrarTurnoFormProps) {
  const {
    control,
    formState: { errors },
    handleSubmit,
    register
  } = useForm<CerrarTurnoFormValues>({
    defaultValues: { totalDeclarado: '', observacion: '' },
    mode: 'onTouched',
    resolver: zodResolver(cerrarTurnoSchema)
  });
  const totalDeclarado = useWatch({ control, name: 'totalDeclarado' });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <TextField
        id="turno-declarado"
        label="Total declarado"
        inputMode="decimal"
        error={errors.totalDeclarado?.message}
        {...register('totalDeclarado')}
      />
      {PATRON_MONTO.test(totalDeclarado) ? (
        <p className="text-sm text-neutral-600 dark:text-neutral-300">
          {`Diferencia estimada: ${formatoMoneda(diferenciaArqueo(totalEstimado, Number(totalDeclarado)))}`}
        </p>
      ) : null}
      <TextField
        id="turno-observacion"
        label="Observación"
        error={errors.observacion?.message}
        {...register('observacion')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        Cerrar turno
      </Button>
    </form>
  );
}
