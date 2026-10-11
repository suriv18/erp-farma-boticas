import { QueryClient } from '@tanstack/react-query';
import { invalidateOrganizacion } from './invalidate';

describe('invalidateOrganizacion', () => {
  it('invalida las consultas de la feature y la estructura corporativa', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    await invalidateOrganizacion(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['organizacion'] });
    expect(spy).toHaveBeenCalledWith({ queryKey: ['organization', 'corporate-structure'] });
  });
});
