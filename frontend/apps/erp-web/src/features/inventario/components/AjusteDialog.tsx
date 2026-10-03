import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateInventario } from '../api/invalidate';
import { registrarMovimiento } from '../api/movimientos.api';
import type { Posicion } from '../api/inventario.types';
import { toRegistrarMovimientoPayload } from '../lib/ajuste';
import { almacenesDe } from '../lib/estructura';
import { nuevaClaveIdempotencia } from '../lib/idempotencia';
import { useOpcionesEstablecimientos } from '../lib/use-opciones-establecimientos';
import type { AjusteFormValues } from '../schemas/ajuste.schema';
import { AjusteForm } from './AjusteForm';

type AjusteDialogProps = {
  posicion: Posicion | null;
  onClose: () => void;
};

export function AjusteDialog({ posicion, onClose }: AjusteDialogProps) {
  const queryClient = useQueryClient();
  const almacenes = almacenesDe(useOpcionesEstablecimientos(), undefined);

  const mutation = useMutation({
    mutationFn: (values: AjusteFormValues) =>
      registrarMovimiento(
        apiClient,
        toRegistrarMovimientoPayload(values),
        nuevaClaveIdempotencia()
      ),
    onSuccess: () => {
      onClose();
      void invalidateInventario(queryClient);
    }
  });

  return (
    <Modal
      open
      onClose={onClose}
      title={posicion ? 'Ajustar stock del lote' : 'Registrar ingreso con lote nuevo'}
      size="lg"
    >
      <AjusteForm
        posicion={posicion}
        almacenes={almacenes}
        isSubmitting={mutation.isPending}
        error={mutation.isError ? describeApiError(mutation.error) : null}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </Modal>
  );
}
