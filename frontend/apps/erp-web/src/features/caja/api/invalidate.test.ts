import { QueryClient } from '@tanstack/react-query';
import { invalidateCaja } from './invalidate';

describe('invalidateCaja', () => {
  it('invalida todas las consultas de caja', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    await invalidateCaja(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['caja'] });
  });
});
