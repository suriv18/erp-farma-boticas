import type { QueryClient } from '@tanstack/react-query';

export function invalidateCaja(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['caja'] });
}
