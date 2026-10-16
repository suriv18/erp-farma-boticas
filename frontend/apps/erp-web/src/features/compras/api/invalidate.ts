import type { QueryClient } from '@tanstack/react-query';

export function invalidateCompras(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['compras'] });
}
