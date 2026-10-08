import type { QueryClient } from '@tanstack/react-query';

export async function invalidateOrganizacion(queryClient: QueryClient): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['organizacion'] }),
    queryClient.invalidateQueries({ queryKey: ['organization', 'corporate-structure'] })
  ]);
}
