import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateInventario } from '../api/invalidate';
import { desbloquearLote } from '../api/lotes.api';

type DesbloqueoDialogProps = {
  loteId: string;
  onClose: () => void;
};

export function DesbloqueoDialog({ loteId, onClose }: DesbloqueoDialogProps) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => desbloquearLote(apiClient, loteId),
    onSuccess: () => {
      onClose();
      void invalidateInventario(queryClient);
    }
  });

  return (
    <Modal open onClose={onClose} title="Desbloquear lote">
      <div className="space-y-4">
        <p className="text-sm text-neutral-600 dark:text-neutral-300">
          El lote volverá a estar habilitado. Un lote vencido no puede habilitarse.
        </p>
        {mutation.isError ? <FormError message={describeApiError(mutation.error)} /> : null}
        <Button disabled={mutation.isPending} onClick={() => mutation.mutate()}>
          Desbloquear lote
        </Button>
      </div>
    </Modal>
  );
}
