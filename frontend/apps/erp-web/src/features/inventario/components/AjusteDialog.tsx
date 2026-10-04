import { Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { registrarMovimiento } from '../api/movimientos.api';
import type { Posicion } from '../api/inventario.types';
import { toRegistrarMovimientoPayload } from '../lib/ajuste';
import { almacenesDe } from '../lib/estructura';
import { useClaveIdempotencia } from '../../../shared/lib/use-clave-idempotencia';
import { useMutacionInventario } from '../lib/use-mutacion-inventario';
import { useOpcionesEstablecimientos } from '../lib/use-opciones-establecimientos';
import type { AjusteFormValues } from '../schemas/ajuste.schema';
import { AjusteForm } from './AjusteForm';

type AjusteDialogProps = {
  posicion: Posicion | null;
  onClose: () => void;
};

export function AjusteDialog({ posicion, onClose }: AjusteDialogProps) {
  const almacenes = almacenesDe(useOpcionesEstablecimientos(), undefined);
  const claveDe = useClaveIdempotencia();
  const mutation = useMutacionInventario((values: AjusteFormValues) => {
    const payload = toRegistrarMovimientoPayload(values);
    return registrarMovimiento(apiClient, payload, claveDe(payload));
  }, onClose);

  return (
    <Modal
      open
      onClose={mutation.cerrar}
      title={posicion ? 'Ajustar stock del lote' : 'Registrar ingreso con lote nuevo'}
      size="lg"
    >
      <AjusteForm
        posicion={posicion}
        almacenes={almacenes}
        isSubmitting={mutation.isPending}
        error={mutation.mensajeError}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </Modal>
  );
}
