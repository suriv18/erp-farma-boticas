import { useMutation, useQueryClient } from '@tanstack/react-query';
import { invalidateCompras } from '../api/invalidate';
import { describeErrorCompras } from './errores-compras';

export function useMutacionCompras<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn,
    onSuccess: () => {
      onSuccess?.();
      return invalidateCompras(queryClient);
    }
  });

  return {
    mutate: mutation.mutate,
    isPending: mutation.isPending,
    reset: mutation.reset,
    mensajeError: mutation.isError ? describeErrorCompras(mutation.error) : null
  };
}
