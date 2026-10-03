import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateInventario } from '../api/invalidate';
import { bloquearLote } from '../api/lotes.api';
import { bloqueoSchema, type BloqueoFormValues } from '../schemas/bloqueo.schema';

type BloqueoDialogProps = {
  loteId: string;
  onClose: () => void;
};

export function BloqueoDialog({ loteId, onClose }: BloqueoDialogProps) {
  const queryClient = useQueryClient();
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<BloqueoFormValues>({
    defaultValues: { motivo: '' },
    mode: 'onTouched',
    resolver: zodResolver(bloqueoSchema)
  });

  const mutation = useMutation({
    mutationFn: (values: BloqueoFormValues) =>
      bloquearLote(apiClient, loteId, { motivo: values.motivo }),
    onSuccess: () => {
      onClose();
      void invalidateInventario(queryClient);
    }
  });

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
        {mutation.isError ? <FormError message={describeApiError(mutation.error)} /> : null}
        <Button type="submit" disabled={mutation.isPending}>
          Bloquear lote
        </Button>
      </form>
    </Modal>
  );
}
