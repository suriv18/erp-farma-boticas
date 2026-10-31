import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { CheckboxField, TextField } from '../../../shared/components/FormFields';
import { PROVEEDOR_FORM_VACIO } from '../lib/proveedor-form';
import { proveedorSchema, type ProveedorFormValues } from '../schemas/proveedor.schema';

type CampoTexto = Exclude<
  keyof ProveedorFormValues,
  'esLaboratorio' | 'esImportador' | 'esDistribuidor'
>;

const CAMPOS: ReadonlyArray<{ name: CampoTexto; id: string; label: string }> = [
  { name: 'tipoDocumento', id: 'proveedor-tipo-documento', label: 'Tipo de documento' },
  { name: 'numeroDocumento', id: 'proveedor-numero-documento', label: 'Número de documento' },
  { name: 'razonSocial', id: 'proveedor-razon-social', label: 'Razón social' },
  { name: 'nombreComercial', id: 'proveedor-nombre-comercial', label: 'Nombre comercial' },
  { name: 'direccion', id: 'proveedor-direccion', label: 'Dirección' },
  { name: 'ubigeo', id: 'proveedor-ubigeo', label: 'Ubigeo' },
  { name: 'telefono', id: 'proveedor-telefono', label: 'Teléfono' },
  { name: 'email', id: 'proveedor-email', label: 'Correo' },
  { name: 'contactoNombre', id: 'proveedor-contacto-nombre', label: 'Contacto' },
  { name: 'contactoTelefono', id: 'proveedor-contacto-telefono', label: 'Teléfono del contacto' },
  { name: 'contactoEmail', id: 'proveedor-contacto-email', label: 'Correo del contacto' },
  { name: 'condicionPagoDefault', id: 'proveedor-condicion-pago', label: 'Condición de pago' },
  { name: 'diasCreditoDefault', id: 'proveedor-dias-credito', label: 'Días de crédito' },
  { name: 'monedaDefault', id: 'proveedor-moneda', label: 'Moneda' },
  { name: 'calificacion', id: 'proveedor-calificacion', label: 'Calificación' }
];

export type ProveedorFormProps = {
  defaultValues?: ProveedorFormValues | undefined;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
  onSubmit: (values: ProveedorFormValues) => void;
  onCancel?: (() => void) | undefined;
};

export function ProveedorForm({
  defaultValues,
  submitLabel,
  isSubmitting = false,
  error = null,
  onSubmit,
  onCancel
}: ProveedorFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<ProveedorFormValues>({
    defaultValues: defaultValues ?? PROVEEDOR_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(proveedorSchema)
  });

  return (
    <form
      className="space-y-6"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        {CAMPOS.map(({ name, id, label }) => (
          <TextField
            key={id}
            id={id}
            label={label}
            error={errors[name]?.message}
            {...register(name)}
          />
        ))}
      </div>
      <div className="flex flex-wrap gap-6">
        <CheckboxField
          id="proveedor-laboratorio"
          label="Laboratorio"
          {...register('esLaboratorio')}
        />
        <CheckboxField id="proveedor-importador" label="Importador" {...register('esImportador')} />
        <CheckboxField
          id="proveedor-distribuidor"
          label="Distribuidor"
          {...register('esDistribuidor')}
        />
      </div>
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
