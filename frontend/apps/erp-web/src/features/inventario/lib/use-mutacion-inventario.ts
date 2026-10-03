import { useMutation, useQueryClient } from '@tanstack/react-query';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateInventario } from '../api/invalidate';

export function useMutacionInventario<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  onClose: () => void
) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn,
    onSuccess: () => {
      onClose();
      void invalidateInventario(queryClient);
    }
  });

  return {
    mutate: mutation.mutate,
    isPending: mutation.isPending,
    mensajeError: mutation.isError ? describeApiError(mutation.error) : null
  };
}
