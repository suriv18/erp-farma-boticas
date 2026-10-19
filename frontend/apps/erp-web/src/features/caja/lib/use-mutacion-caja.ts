import { useMutation, useQueryClient } from '@tanstack/react-query';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateCaja } from '../api/invalidate';

export function useMutacionCaja<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>
) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn,
    onSuccess: () => invalidateCaja(queryClient)
  });

  return {
    mutate: mutation.mutate,
    data: mutation.data,
    isPending: mutation.isPending,
    mensajeError: mutation.isError ? describeApiError(mutation.error) : null
  };
}
