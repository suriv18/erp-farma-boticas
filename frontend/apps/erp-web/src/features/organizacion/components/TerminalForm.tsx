import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { ESTADOS_TERMINAL } from '../api/terminales.types';
import { TERMINAL_FORM_VACIO } from '../lib/form-defaults';
import { terminalSchema, type TerminalFormValues } from '../schemas/terminal.schema';
import { CamposTexto, type CampoTexto } from './CamposTexto';
import { FormError } from './FormError';
import { CheckboxField, SelectField } from './FormFields';

export type TerminalFormProps = {
  defaultValues?: TerminalFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: TerminalFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

const CAMPOS_TERMINAL: ReadonlyArray<CampoTexto<TerminalFormValues>> = [
  { name: 'codigo', id: 'terminal-codigo', label: 'Código' },
  { name: 'nombre', id: 'terminal-nombre', label: 'Nombre' },
  { name: 'serieBoletaDefecto', id: 'terminal-serie-boleta', label: 'Serie de boleta' },
  { name: 'serieFacturaDefecto', id: 'terminal-serie-factura', label: 'Serie de factura' },
  { name: 'numeroSerieEquipo', id: 'terminal-numero-serie', label: 'Número de serie del equipo' },
  { name: 'hostname', id: 'terminal-hostname', label: 'Hostname' },
  { name: 'ipEquipo', id: 'terminal-ip', label: 'Dirección IP' },
  { name: 'impresoraCodigo', id: 'terminal-impresora', label: 'Código de impresora' }
];

const camposTerminal = (isEdit: boolean) =>
  CAMPOS_TERMINAL.map((campo) =>
    campo.name === 'codigo' ? { ...campo, readOnly: isEdit } : campo
  );

export function TerminalForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: TerminalFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<TerminalFormValues>({
    defaultValues: defaultValues ?? TERMINAL_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(terminalSchema)
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
        <CamposTexto fields={camposTerminal(isEdit)} register={register} errors={errors} />
        {isEdit ? (
          <SelectField
            id="terminal-estado"
            label="Estado"
            error={errors.estado?.message}
            {...register('estado')}
          >
            {ESTADOS_TERMINAL.map((estado) => (
              <option key={estado} value={estado}>
                {estado}
              </option>
            ))}
          </SelectField>
        ) : null}
      </div>
      <CheckboxField
        id="terminal-store-edge"
        label="Habilitar store edge"
        {...register('storeEdgeHabilitado')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
