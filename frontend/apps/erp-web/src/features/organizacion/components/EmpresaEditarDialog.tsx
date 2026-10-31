import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarEmpresa } from '../api/empresas.api';
import type { Empresa } from '../api/empresas.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { describeApiError } from '../lib/describe-api-error';
import { toEmpresaFormValues } from '../lib/form-defaults';
import { toActualizarEmpresaPayload } from '../lib/form-payloads';
import type { EmpresaFormValues } from '../schemas/empresa.schema';
import { EmpresaForm } from './EmpresaForm';

type EmpresaEditarDialogProps = {
  empresa: Empresa;
  onClose: () => void;
};

export function EmpresaEditarDialog({ empresa, onClose }: EmpresaEditarDialogProps) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (values: EmpresaFormValues) =>
      actualizarEmpresa(apiClient, empresa.id, toActualizarEmpresaPayload(values)),
    onSuccess: () => {
      onClose();
      void invalidateOrganizacion(queryClient);
    }
  });

  return (
    <Modal open onClose={onClose} title="Editar empresa" size="lg">
      <EmpresaForm
        isEdit
        defaultValues={toEmpresaFormValues(empresa)}
        submitLabel="Guardar cambios"
        isSubmitting={mutation.isPending}
        error={mutation.isError ? describeApiError(mutation.error) : null}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </Modal>
  );
}
