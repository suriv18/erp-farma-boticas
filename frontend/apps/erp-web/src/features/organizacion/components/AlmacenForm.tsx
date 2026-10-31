import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, useWatch, type RegisterOptions } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { TIPOS_ALMACEN } from '../api/almacenes.types';
import { ALMACEN_FORM_VACIO } from '../lib/form-defaults';
import { almacenSchema, type AlmacenFormValues } from '../schemas/almacen.schema';
import { CamposTexto, type CampoTexto } from './CamposTexto';
import { FormError } from './FormError';
import { CheckboxField, SelectField } from './FormFields';

export type AlmacenFormProps = {
  defaultValues?: AlmacenFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: AlmacenFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

const CAMPOS_IDENTIFICACION: ReadonlyArray<CampoTexto<AlmacenFormValues>> = [
  { name: 'codigo', id: 'almacen-codigo', label: 'Código' },
  { name: 'nombre', id: 'almacen-nombre', label: 'Nombre' }
];

const CAMPOS_TEMPERATURA: ReadonlyArray<CampoTexto<AlmacenFormValues>> = [
  {
    name: 'temperaturaMinC',
    id: 'almacen-temperatura-min',
    label: 'Temperatura mínima (°C)',
    inputMode: 'decimal',
    deps: ['temperaturaMaxC']
  },
  {
    name: 'temperaturaMaxC',
    id: 'almacen-temperatura-max',
    label: 'Temperatura máxima (°C)',
    inputMode: 'decimal',
    deps: ['temperaturaMinC']
  }
];

type IndicadorAlmacen = {
  name:
    | 'permiteLotes'
    | 'permiteVencimiento'
    | 'permiteVenta'
    | 'permiteDespacho'
    | 'controlTemperatura';
  id: string;
  label: string;
};

const INDICADORES: ReadonlyArray<IndicadorAlmacen> = [
  { name: 'permiteLotes', id: 'almacen-lotes', label: 'Permite lotes' },
  { name: 'permiteVencimiento', id: 'almacen-vencimiento', label: 'Permite vencimiento' },
  { name: 'permiteVenta', id: 'almacen-venta', label: 'Permite venta' },
  { name: 'permiteDespacho', id: 'almacen-despacho', label: 'Permite despacho' },
  { name: 'controlTemperatura', id: 'almacen-control-temperatura', label: 'Controla temperatura' }
];

const camposIdentificacion = (isEdit: boolean) =>
  CAMPOS_IDENTIFICACION.map((campo) =>
    campo.name === 'codigo' ? { ...campo, readOnly: isEdit } : campo
  );

const camposTemperatura = (editable: boolean) =>
  CAMPOS_TEMPERATURA.map((campo) => ({ ...campo, readOnly: !editable }));

export function AlmacenForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: AlmacenFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    setValue,
    control
  } = useForm<AlmacenFormValues>({
    defaultValues: defaultValues ?? ALMACEN_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(almacenSchema)
  });
  const controlTemperatura = useWatch({ control, name: 'controlTemperatura' });
  const opcionesIndicador: Partial<
    Record<IndicadorAlmacen['name'], RegisterOptions<AlmacenFormValues, IndicadorAlmacen['name']>>
  > = {
    controlTemperatura: {
      deps: ['tipo', 'temperaturaMinC', 'temperaturaMaxC'],
      onChange: (event: { target: { checked: boolean } }) => {
        if (!event.target.checked) {
          setValue('temperaturaMinC', '');
          setValue('temperaturaMaxC', '');
        }
      }
    }
  };

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <CamposTexto fields={camposIdentificacion(isEdit)} register={register} errors={errors} />
        <SelectField
          id="almacen-tipo"
          label="Tipo de almacén"
          error={errors.tipo?.message}
          {...register('tipo', {
            deps: ['controlTemperatura'],
            onChange: (event: { target: { value: string } }) => {
              if (event.target.value === 'REFRIGERADO') {
                setValue('controlTemperatura', true, { shouldDirty: true });
              }
            }
          })}
        >
          {TIPOS_ALMACEN.map((tipo) => (
            <option key={tipo} value={tipo}>
              {tipo}
            </option>
          ))}
        </SelectField>
        <CamposTexto
          fields={camposTemperatura(controlTemperatura)}
          register={register}
          errors={errors}
        />
      </div>
      <div className="flex flex-wrap gap-6">
        {INDICADORES.map(({ name, id, label }) => (
          <CheckboxField
            key={id}
            id={id}
            label={label}
            error={errors[name]?.message}
            {...register(name, opcionesIndicador[name])}
          />
        ))}
        {isEdit ? (
          <CheckboxField id="almacen-activo" label="Almacén activo" {...register('activo')} />
        ) : null}
      </div>
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
