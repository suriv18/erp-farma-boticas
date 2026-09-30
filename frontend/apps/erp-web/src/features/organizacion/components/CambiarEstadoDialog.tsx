import { useState } from 'react';
import { Button, FormField, Modal, Select } from '@boticas/ui-web';
import { FormError } from './FormError';
import { CheckboxField } from './FormFields';

export type CambiarEstadoDialogProps<T extends string> = {
  title: string;
  estados: readonly T[];
  current: T;
  consecuencias?: Partial<Record<T, string>> | undefined;
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (estado: T) => void;
  onClose: () => void;
};

export function CambiarEstadoDialog<T extends string>({
  title,
  estados,
  current,
  consecuencias,
  isSubmitting,
  error,
  onSubmit,
  onClose
}: CambiarEstadoDialogProps<T>) {
  const [estado, setEstado] = useState<T>(current);
  const [confirmado, setConfirmado] = useState(false);
  const consecuencia = estado === current ? undefined : consecuencias?.[estado];
  const faltaConfirmar = consecuencia !== undefined && !confirmado;

  return (
    <Modal open onClose={onClose} title={title}>
      <form
        className="space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit(estado);
        }}
      >
        <FormField label="Estado" htmlFor="cambiar-estado">
          <Select
            id="cambiar-estado"
            value={estado}
            onChange={(event) => {
              setEstado(event.target.value as T);
              setConfirmado(false);
            }}
          >
            {estados.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </Select>
        </FormField>
        {consecuencia ? (
          <div
            role="note"
            className="border-warning-200 bg-warning-50 text-warning-800 dark:border-warning-800 dark:bg-warning-900/30 dark:text-warning-300 space-y-3 rounded-lg border px-3 py-3 text-sm"
          >
            <p>{consecuencia}</p>
            <CheckboxField
              id="confirmar-consecuencias"
              label="Entiendo las consecuencias"
              checked={confirmado}
              onChange={(event) => setConfirmado(event.target.checked)}
            />
          </div>
        ) : null}
        {error ? <FormError message={error} /> : null}
        <Button type="submit" disabled={isSubmitting || estado === current || faltaConfirmar}>
          Guardar estado
        </Button>
      </form>
    </Modal>
  );
}
