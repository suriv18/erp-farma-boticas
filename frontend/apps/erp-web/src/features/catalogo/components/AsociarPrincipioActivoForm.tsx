import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { ASOCIACION_PRINCIPIO_ACTIVO_VACIA } from '../lib/asociacion-principio-activo';
import {
  asociacionPrincipioActivoSchema,
  type AsociacionPrincipioActivoFormValues
} from '../schemas/asociacion-principio-activo.schema';
import { CamposFormulario, type Opcion } from './CamposFormulario';

export type AsociarPrincipioActivoFormProps = {
  principios: readonly Opcion[];
  unidades: readonly Opcion[];
  onSubmit: (values: AsociacionPrincipioActivoFormValues) => void;
  isSubmitting: boolean;
  error: string | null;
};

export function AsociarPrincipioActivoForm({
  principios,
  unidades,
  onSubmit,
  isSubmitting,
  error
}: AsociarPrincipioActivoFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<AsociacionPrincipioActivoFormValues>({
    defaultValues: ASOCIACION_PRINCIPIO_ACTIVO_VACIA,
    mode: 'onTouched',
    resolver: zodResolver(asociacionPrincipioActivoSchema)
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
          prefijo="asociar-principio-activo"
          campos={[
            {
              name: 'principioActivoId',
              label: 'Principio activo',
              tipo: 'seleccion',
              opciones: principios
            },
            { name: 'concentracionTexto', label: 'Concentración', tipo: 'texto' },
            { name: 'cantidad', label: 'Cantidad', tipo: 'decimal' },
            {
              name: 'unidadMedidaCodigo',
              label: 'Unidad de medida',
              tipo: 'seleccion',
              opciones: unidades
            },
            { name: 'orden', label: 'Orden', tipo: 'entero' },
            { name: 'esPrincipal', label: 'Es principio activo principal', tipo: 'checkbox' }
          ]}
          register={register}
          errors={errors}
        />
      </div>
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        Asociar principio activo
      </Button>
    </form>
  );
}
