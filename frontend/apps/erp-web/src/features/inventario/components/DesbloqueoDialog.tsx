import { Button, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { desbloquearLote } from '../api/lotes.api';
import { useMutacionInventario } from '../lib/use-mutacion-inventario';

type DesbloqueoDialogProps = {
  loteId: string;
  onClose: () => void;
};

export function DesbloqueoDialog({ loteId, onClose }: DesbloqueoDialogProps) {
  const mutation = useMutacionInventario(() => desbloquearLote(apiClient, loteId), onClose);

  return (
    <Modal open onClose={mutation.cerrar} title="Desbloquear lote">
      <div className="space-y-4">
        <p className="text-sm text-neutral-600 dark:text-neutral-300">
          El lote volverá a estar habilitado. Un lote vencido no puede habilitarse.
        </p>
        {mutation.mensajeError ? <FormError message={mutation.mensajeError} /> : null}
        <Button disabled={mutation.isPending} onClick={() => mutation.mutate(undefined)}>
          Desbloquear lote
        </Button>
      </div>
    </Modal>
  );
}
