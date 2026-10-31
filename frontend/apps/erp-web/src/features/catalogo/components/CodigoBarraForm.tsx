import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { CODIGO_BARRA_VACIO } from '../lib/codigo-barra-form';
import { codigoBarraSchema, type CodigoBarraFormValues } from '../schemas/codigo-barra.schema';
import { CamposFormulario, type CampoFormulario } from './CamposFormulario';

const CAMPOS: CampoFormulario<CodigoBarraFormValues>[] = [
  { name: 'codigoBarra', label: 'Código de barras', tipo: 'texto' },
  { name: 'tipoCodigo', label: 'Tipo de código', tipo: 'texto' },
  { name: 'vigenteDesde', label: 'Vigente desde', tipo: 'fecha' },
  { name: 'vigenteHasta', label: 'Vigente hasta', tipo: 'fecha' }
];

export type CodigoBarraFormProps = {
  onSubmit: (values: CodigoBarraFormValues) => void;
  isSubmitting: boolean;
  error: string | null;
};

export function CodigoBarraForm({ onSubmit, isSubmitting, error }: CodigoBarraFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<CodigoBarraFormValues>({
    defaultValues: CODIGO_BARRA_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(codigoBarraSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <CamposFormulario
          prefijo="codigo-barra"
          campos={CAMPOS}
          register={register}
          errors={errors}
        />
      </div>
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        Agregar código de barras
      </Button>
    </form>
  );
}
