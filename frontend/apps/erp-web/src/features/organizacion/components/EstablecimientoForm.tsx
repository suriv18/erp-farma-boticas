import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { PERFILES_OPERACION, TIPOS_ESTABLECIMIENTO } from '../api/establecimientos.types';
import { ESTABLECIMIENTO_FORM_VACIO } from '../lib/form-defaults';
import {
  establecimientoSchema,
  type EstablecimientoFormValues
} from '../schemas/establecimiento.schema';
import { CamposTexto, type CampoTexto } from './CamposTexto';
import { FormError } from './FormError';
import { CheckboxField, SelectField } from './FormFields';
import { FormSection } from './FormSection';

export type EstablecimientoFormProps = {
  defaultValues?: EstablecimientoFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: EstablecimientoFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

type Campos = ReadonlyArray<CampoTexto<EstablecimientoFormValues>>;

const camposIdentificacion = (isEdit: boolean): Campos => [
  { name: 'codigo', id: 'establecimiento-codigo', label: 'Código', readOnly: isEdit },
  { name: 'nombre', id: 'establecimiento-nombre', label: 'Nombre' }
];

const CAMPOS_REGULATORIO: Campos = [
  {
    name: 'categoriaRegulatoriaCodigo',
    id: 'establecimiento-categoria',
    label: 'Categoría regulatoria'
  },
  { name: 'codigoAnexoSunat', id: 'establecimiento-anexo', label: 'Anexo SUNAT' },
  { name: 'codigoDigemid', id: 'establecimiento-digemid', label: 'Código DIGEMID' }
];

const CAMPOS_UBICACION: Campos = [
  { name: 'direccion', id: 'establecimiento-direccion', label: 'Dirección' },
  { name: 'ubigeo', id: 'establecimiento-ubigeo', label: 'Ubigeo' },
  { name: 'referencia', id: 'establecimiento-referencia', label: 'Referencia' },
  { name: 'latitud', id: 'establecimiento-latitud', label: 'Latitud', inputMode: 'decimal' },
  { name: 'longitud', id: 'establecimiento-longitud', label: 'Longitud', inputMode: 'decimal' },
  { name: 'telefono', id: 'establecimiento-telefono', label: 'Teléfono' },
  { name: 'email', id: 'establecimiento-email', label: 'Correo' }
];

const CAMPOS_OPERACION: Campos = [
  { name: 'zonaHoraria', id: 'establecimiento-zona-horaria', label: 'Zona horaria' }
];

export function EstablecimientoForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: EstablecimientoFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<EstablecimientoFormValues>({
    defaultValues: defaultValues ?? ESTABLECIMIENTO_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(establecimientoSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormSection title="Identificación">
        <div className="grid gap-4 sm:grid-cols-2">
          <CamposTexto fields={camposIdentificacion(isEdit)} register={register} errors={errors} />
          <SelectField
            id="establecimiento-tipo"
            label="Tipo de establecimiento"
            error={errors.tipoEstablecimiento?.message}
            {...register('tipoEstablecimiento')}
          >
            {TIPOS_ESTABLECIMIENTO.map((tipo) => (
              <option key={tipo} value={tipo}>
                {tipo}
              </option>
            ))}
          </SelectField>
        </div>
        <CheckboxField
          id="establecimiento-principal"
          label="Es principal"
          {...register('esPrincipal')}
        />
      </FormSection>

      <FormSection title="Regulatorio">
        <div className="grid gap-4 sm:grid-cols-2">
          <CamposTexto fields={CAMPOS_REGULATORIO} register={register} errors={errors} />
        </div>
      </FormSection>

      <FormSection title="Ubicación">
        <div className="grid gap-4 sm:grid-cols-2">
          <CamposTexto fields={CAMPOS_UBICACION} register={register} errors={errors} />
        </div>
      </FormSection>

      <FormSection title="Operación">
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            id="establecimiento-perfil"
            label="Perfil de operación"
            error={errors.perfilOperacion?.message}
            {...register('perfilOperacion')}
          >
            {PERFILES_OPERACION.map((perfil) => (
              <option key={perfil} value={perfil}>
                {perfil}
              </option>
            ))}
          </SelectField>
          <CamposTexto fields={CAMPOS_OPERACION} register={register} errors={errors} />
        </div>
        <div className="flex flex-wrap gap-6">
          <CheckboxField
            id="establecimiento-venta-online"
            label="Permite venta online"
            {...register('permiteVentaOnline')}
          />
          <CheckboxField
            id="establecimiento-delivery"
            label="Permite delivery"
            {...register('permiteDelivery')}
          />
        </div>
      </FormSection>

      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
