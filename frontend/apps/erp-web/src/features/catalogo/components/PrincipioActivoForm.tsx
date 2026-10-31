import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { PRINCIPIO_ACTIVO_VACIO } from '../lib/principio-activo-form';
import {
  principioActivoSchema,
  type PrincipioActivoFormValues
} from '../schemas/principio-activo.schema';
import { CamposFormulario, type CampoFormulario } from './CamposFormulario';

const CAMPOS: CampoFormulario<PrincipioActivoFormValues>[] = [
  { name: 'denominacion', label: 'Denominación', tipo: 'texto' },
  { name: 'codigoFuente', label: 'Código fuente', tipo: 'texto' },
  { name: 'nombreNormalizado', label: 'Nombre normalizado', tipo: 'texto' },
  { name: 'fuente', label: 'Fuente', tipo: 'texto' }
];

export type PrincipioActivoFormProps = {
  defaultValues?: PrincipioActivoFormValues;
  onSubmit: (values: PrincipioActivoFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function PrincipioActivoForm({
  defaultValues = PRINCIPIO_ACTIVO_VACIO,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: PrincipioActivoFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<PrincipioActivoFormValues>({
    defaultValues,
    mode: 'onTouched',
    resolver: zodResolver(principioActivoSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <CamposFormulario
        prefijo="principio-activo"
        campos={CAMPOS}
        register={register}
        errors={errors}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
