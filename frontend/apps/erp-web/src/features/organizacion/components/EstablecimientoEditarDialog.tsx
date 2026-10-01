import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarEstablecimiento } from '../api/establecimientos.api';
import type { Establecimiento } from '../api/establecimientos.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { describeApiError } from '../lib/describe-api-error';
import { toEstablecimientoFormValues } from '../lib/form-defaults';
import { toActualizarEstablecimientoPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import { EstablecimientoForm } from './EstablecimientoForm';

type EstablecimientoEditarDialogProps = {
  establecimiento: Establecimiento;
  onClose: () => void;
};

export function EstablecimientoEditarDialog({
  establecimiento,
  onClose
}: EstablecimientoEditarDialogProps) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (values: EstablecimientoFormValues) =>
      actualizarEstablecimiento(
        apiClient,
        establecimiento.id,
        tenantId,
        toActualizarEstablecimientoPayload(values)
      ),
    onSuccess: () => {
      onClose();
      void invalidateOrganizacion(queryClient);
    }
  });

  return (
    <Modal open onClose={onClose} title="Editar establecimiento" size="lg">
      <EstablecimientoForm
        isEdit
        defaultValues={toEstablecimientoFormValues(establecimiento)}
        submitLabel="Guardar cambios"
        isSubmitting={mutation.isPending}
        error={mutation.isError ? describeApiError(mutation.error) : null}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </Modal>
  );
}
