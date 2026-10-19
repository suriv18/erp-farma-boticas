import { QueryClient } from '@tanstack/react-query';
import { invalidateCompras } from './invalidate';

describe('invalidateCompras', () => {
  it('invalida todas las consultas de compras', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    await invalidateCompras(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });
});
