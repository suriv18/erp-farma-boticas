import { ApiError } from '@boticas/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { useMutacionCaja } from './use-mutacion-caja';

function renderMutacion(mutationFn: (valor: string) => Promise<string>) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { invalidate, ...renderHook(() => useMutacionCaja(mutationFn), { wrapper }) };
}

describe('useMutacionCaja', () => {
  it('parte sin error, sin datos y sin mutación en curso', () => {
    const { result } = renderMutacion(() => Promise.resolve('ok'));

    expect(result.current.mensajeError).toBeNull();
    expect(result.current.isPending).toBe(false);
    expect(result.current.data).toBeUndefined();
  });

  it('al tener éxito expone el resultado e invalida la caja', async () => {
    const mutationFn = vi.fn(() => Promise.resolve('ok'));
    const { result, invalidate } = renderMutacion(mutationFn);

    act(() => result.current.mutate('x'));

    await waitFor(() => expect(result.current.data).toBe('ok'));
    expect(mutationFn).toHaveBeenCalledWith('x', expect.anything());
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['caja'] });
  });

  it('al fallar expone el mensaje del error', async () => {
    const { result } = renderMutacion(() => Promise.reject(new ApiError('prohibido', 403)));

    act(() => result.current.mutate('x'));

    await waitFor(() =>
      expect(result.current.mensajeError).toBe('No tienes permiso para esta acción.')
    );
  });

  it('indica que está en curso mientras la mutación no termina', async () => {
    const { result } = renderMutacion(() => new Promise<string>(() => undefined));

    act(() => result.current.mutate('x'));

    await waitFor(() => expect(result.current.isPending).toBe(true));
  });
});
