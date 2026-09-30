import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { EMPRESA_FORM_VACIO } from '../lib/form-defaults';
import { empresaSchema, type EmpresaFormValues } from '../schemas/empresa.schema';
import { FormError } from './FormError';
import { CheckboxField, TextField } from './FormFields';

export type EmpresaFormProps = {
  defaultValues?: EmpresaFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: EmpresaFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function EmpresaForm({
  defaultValues,
  isEdit = false,
  onSubmit,
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

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          id="empresa-ruc"
          label="RUC"
          readOnly={isEdit}
          error={errors.ruc?.message}
          {...register('ruc')}
        />
        <TextField
          id="empresa-razon-social"
          label="Razón social"
          error={errors.razonSocial?.message}
          {...register('razonSocial')}
        />
        <TextField
          id="empresa-nombre-comercial"
          label="Nombre comercial"
          error={errors.nombreComercial?.message}
          {...register('nombreComercial')}
        />
        <TextField
          id="empresa-direccion-fiscal"
          label="Dirección fiscal"
          error={errors.direccionFiscal?.message}
          {...register('direccionFiscal')}
        />
        <TextField
          id="empresa-ubigeo-fiscal"
          label="Ubigeo fiscal"
          error={errors.ubigeoFiscal?.message}
          {...register('ubigeoFiscal')}
        />
        <TextField
          id="empresa-telefono"
          label="Teléfono"
          error={errors.telefono?.message}
          {...register('telefono')}
        />
        <TextField
          id="empresa-email"
          label="Correo"
          error={errors.email?.message}
          {...register('email')}
        />
        <TextField
          id="empresa-sitio-web"
          label="Sitio web"
          error={errors.sitioWeb?.message}
          {...register('sitioWeb')}
        />
        <TextField
          id="empresa-moneda"
          label="Moneda funcional"
          error={errors.monedaFuncional?.message}
          {...register('monedaFuncional')}
        />
        <TextField
          id="empresa-zona-horaria"
          label="Zona horaria"
          error={errors.zonaHoraria?.message}
          {...register('zonaHoraria')}
        />
      </div>
      <CheckboxField
        id="empresa-venta-online"
        label="Permite venta online"
        {...register('permiteVentaOnline')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
