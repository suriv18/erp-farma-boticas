import type { QueryClient } from '@tanstack/react-query';

export function invalidateVentas(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['ventas'] });
}
