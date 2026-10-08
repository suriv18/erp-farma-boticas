import { useState } from 'react';
import { Button, FormField, Modal, Select } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';

export type EstadoDialogProps = {
  title: string;
  estados: readonly string[];
  current: string;
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (estado: string) => void;
  onClose: () => void;
};

export function EstadoDialog({
  title,
  estados,
  current,
  isSubmitting,
  error,
  onSubmit,
  onClose
}: EstadoDialogProps) {
  const [estado, setEstado] = useState(current);

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
            onChange={(event) => setEstado(event.target.value)}
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
