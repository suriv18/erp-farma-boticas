import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { bloquearLote } from '../api/lotes.api';
import { useMutacionInventario } from '../lib/use-mutacion-inventario';
import { bloqueoSchema, type BloqueoFormValues } from '../schemas/bloqueo.schema';

type BloqueoDialogProps = {
  loteId: string;
  onClose: () => void;
};

export function BloqueoDialog({ loteId, onClose }: BloqueoDialogProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<BloqueoFormValues>({
    defaultValues: { motivo: '' },
    mode: 'onTouched',
    resolver: zodResolver(bloqueoSchema)
  });

  const mutation = useMutacionInventario(
    (values: BloqueoFormValues) => bloquearLote(apiClient, loteId, { motivo: values.motivo }),
    onClose
  );

  return (
    <Modal open onClose={onClose} title="Bloquear lote">
      <form
        className="space-y-4"
        noValidate
        onSubmit={(event) => {
          void handleSubmit((values) => mutation.mutate(values))(event);
        }}
      >
        <TextField
          id="bloqueo-motivo"
          label="Motivo"
          error={errors.motivo?.message}
          {...register('motivo')}
        />
        {mutation.mensajeError ? <FormError message={mutation.mensajeError} /> : null}
        <Button type="submit" disabled={mutation.isPending}>
          Bloquear lote
        </Button>
      </form>
    </Modal>
  );
}
