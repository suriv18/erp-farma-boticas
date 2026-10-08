import { Modal } from '@boticas/ui-web';
import type { AsignacionRolFormValues } from '../schemas/asignacion-rol.schema';
import { AsignacionRolForm } from './AsignacionRolForm';

export type AsignarRolDialogProps = {
  open: boolean;
  tenantId: string;
  onSubmit: (values: AsignacionRolFormValues) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  errorMessage?: string | undefined;
};

export function AsignarRolDialog({
  open,
  tenantId,
  onSubmit,
  onCancel,
  isSubmitting = false,
  errorMessage
}: AsignarRolDialogProps) {
  if (!open) return null;

  return (
    <Modal open={open} onClose={onCancel} title="Asignar rol">
      <AsignacionRolForm
        tenantId={tenantId}
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={isSubmitting}
        errorMessage={errorMessage}
      />
    </Modal>
  );
}
