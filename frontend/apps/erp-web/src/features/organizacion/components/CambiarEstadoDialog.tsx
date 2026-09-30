import { useState } from 'react';
import { Button, FormField, Modal, Select } from '@boticas/ui-web';
import { FormError } from './FormError';

export type CambiarEstadoDialogProps<T extends string> = {
  title: string;
  estados: readonly T[];
  current: T;
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (estado: T) => void;
  onClose: () => void;
};

export function CambiarEstadoDialog<T extends string>({
  title,
  estados,
  current,
  isSubmitting,
  error,
  onSubmit,
  onClose
}: CambiarEstadoDialogProps<T>) {
  const [estado, setEstado] = useState<T>(current);

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
            onChange={(event) => setEstado(event.target.value as T)}
          >
            {estados.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </Select>
        </FormField>
        {error ? <FormError message={error} /> : null}
        <Button type="submit" disabled={isSubmitting || estado === current}>
          Guardar estado
        </Button>
      </form>
    </Modal>
  );
}
