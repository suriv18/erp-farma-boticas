import type { QueryClient } from '@tanstack/react-query';

export function invalidateInventario(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['inventario'] });
}
