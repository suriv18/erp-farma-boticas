import { QueryClient } from '@tanstack/react-query';
import { invalidateInventario } from './invalidate';

describe('invalidateInventario', () => {
  it('invalida todas las queries de inventario', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue();

    await invalidateInventario(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });
});
