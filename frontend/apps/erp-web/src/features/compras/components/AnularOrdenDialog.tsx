import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { anularOrden } from '../api/ordenes.api';
import { useMutacionCompras } from '../lib/use-mutacion-compras';
import { anularOrdenSchema, type AnularOrdenFormValues } from '../schemas/anular-orden.schema';

type AnularOrdenDialogProps = {
  ordenId: string;
  numero: string;
  onClose: () => void;
};

export function AnularOrdenDialog({ ordenId, numero, onClose }: AnularOrdenDialogProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<AnularOrdenFormValues>({
    defaultValues: { motivo: '' },
    mode: 'onTouched',
    resolver: zodResolver(anularOrdenSchema)
  });
  const anulacion = useMutacionCompras(
    (values: AnularOrdenFormValues) => anularOrden(apiClient, ordenId, { motivo: values.motivo }),
    onClose
  );

  return (
    <Modal open onClose={onClose} title="Anular orden de compra">
      <form
        className="space-y-4"
        noValidate
        onSubmit={(event) => {
          void handleSubmit((values) => anulacion.mutate(values))(event);
        }}
      >
        <p className="text-sm text-neutral-600 dark:text-neutral-300">
          Se anulará la orden {numero}. Esta acción no se puede deshacer.
        </p>
        <TextField
          id="anular-motivo"
          label="Motivo"
          error={errors.motivo?.message}
          {...register('motivo')}
        />
        {anulacion.mensajeError ? <FormError message={anulacion.mensajeError} /> : null}
        <Button type="submit" disabled={anulacion.isPending}>
          Confirmar anulación
        </Button>
      </form>
    </Modal>
  );
}
