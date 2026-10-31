import { ApiError } from '@boticas/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { useMutacionInventario } from './use-mutacion-inventario';

function renderMutacion(mutationFn: (valor: string) => Promise<string>, onClose: () => void) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return {
    invalidate,
    ...renderHook(() => useMutacionInventario(mutationFn, onClose), { wrapper })
  };
}

describe('useMutacionInventario', () => {
  it('parte sin error y sin mutación en curso', () => {
    const { result } = renderMutacion(() => Promise.resolve('ok'), vi.fn());

    expect(result.current.mensajeError).toBeNull();
    expect(result.current.isPending).toBe(false);
  });

  it('al tener éxito cierra e invalida el inventario', async () => {
    const onClose = vi.fn();
    const mutationFn = vi.fn(() => Promise.resolve('ok'));
    const { result, invalidate } = renderMutacion(mutationFn, onClose);

    act(() => result.current.mutate('x'));

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(mutationFn).toHaveBeenCalledWith('x', expect.anything());
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });

  it('al fallar expone el mensaje y no cierra', async () => {
    const onClose = vi.fn();
    const error = new ApiError('prohibido', 403);
    const { result } = renderMutacion(() => Promise.reject(error), onClose);

    act(() => result.current.mutate('x'));

    await waitFor(() =>
      expect(result.current.mensajeError).toBe('No tienes permiso para esta acción.')
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it('cerrar delega en onClose cuando no hay mutación en curso', () => {
    const onClose = vi.fn();
    const { result } = renderMutacion(() => Promise.resolve('ok'), onClose);

    act(() => result.current.cerrar());

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('cerrar no hace nada mientras la mutación está en curso', async () => {
    const onClose = vi.fn();
    const { result } = renderMutacion(() => new Promise<string>(() => undefined), onClose);

    act(() => result.current.mutate('x'));
    await waitFor(() => expect(result.current.isPending).toBe(true));
    act(() => result.current.cerrar());

    expect(onClose).not.toHaveBeenCalled();
  });
});
