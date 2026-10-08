import { QueryClient } from '@tanstack/react-query';
import { invalidateVentas } from './invalidate';

describe('invalidateVentas', () => {
  it('invalida todas las consultas de ventas', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    await invalidateVentas(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['ventas'] });
  });
});
