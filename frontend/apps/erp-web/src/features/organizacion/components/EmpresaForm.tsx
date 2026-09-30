import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { EMPRESA_FORM_VACIO } from '../lib/form-defaults';
import { empresaSchema, type EmpresaFormValues } from '../schemas/empresa.schema';
import { FormError } from './FormError';
import { CamposTexto, type CampoTexto } from './CamposTexto';
import { CheckboxField } from './FormFields';

export type EmpresaFormProps = {
  defaultValues?: EmpresaFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: EmpresaFormValues) => void;
  onCancel?: (() => void) | undefined;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

const CAMPOS_EMPRESA: ReadonlyArray<CampoTexto<EmpresaFormValues>> = [
  { name: 'ruc', id: 'empresa-ruc', label: 'RUC' },
  { name: 'razonSocial', id: 'empresa-razon-social', label: 'Razón social' },
  { name: 'nombreComercial', id: 'empresa-nombre-comercial', label: 'Nombre comercial' },
  { name: 'direccionFiscal', id: 'empresa-direccion-fiscal', label: 'Dirección fiscal' },
  { name: 'ubigeoFiscal', id: 'empresa-ubigeo-fiscal', label: 'Ubigeo fiscal' },
  { name: 'telefono', id: 'empresa-telefono', label: 'Teléfono' },
  { name: 'email', id: 'empresa-email', label: 'Correo' },
  { name: 'sitioWeb', id: 'empresa-sitio-web', label: 'Sitio web' },
  { name: 'monedaFuncional', id: 'empresa-moneda', label: 'Moneda funcional' },
  { name: 'zonaHoraria', id: 'empresa-zona-horaria', label: 'Zona horaria' }
];

const camposEmpresa = (isEdit: boolean) =>
  CAMPOS_EMPRESA.map((campo) => (campo.name === 'ruc' ? { ...campo, readOnly: isEdit } : campo));

export function EmpresaForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  onCancel,
  submitLabel,
  isSubmitting = false,
  error = null
}: EmpresaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<EmpresaFormValues>({
    defaultValues: defaultValues ?? EMPRESA_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(empresaSchema)
  });

  const campos = camposEmpresa(isEdit);

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <CamposTexto fields={campos} register={register} errors={errors} />
      </div>
      <CheckboxField
        id="empresa-venta-online"
        label="Permite venta online"
        {...register('permiteVentaOnline')}
      />
      {error ? <FormError message={error} /> : null}
      <div className="flex flex-wrap gap-3">
        <Button type="submit" disabled={isSubmitting}>
          {submitLabel}
        </Button>
        {onCancel ? (
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancelar
          </Button>
        ) : null}
      </div>
    </form>
  );
}
