import { ApiError } from '@boticas/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { useMutacionCompras } from './use-mutacion-compras';

function renderMutacion(mutationFn: (valor: string) => Promise<string>, onSuccess?: () => void) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return {
    invalidate,
    ...renderHook(() => useMutacionCompras(mutationFn, onSuccess), { wrapper })
  };
}

describe('useMutacionCompras', () => {
  it('parte sin error y sin mutación en curso', () => {
    const { result } = renderMutacion(() => Promise.resolve('ok'));

    expect(result.current.mensajeError).toBeNull();
    expect(result.current.isPending).toBe(false);
  });

  it('al tener éxito invalida compras y llama al callback', async () => {
    const onSuccess = vi.fn();
    const mutationFn = vi.fn(() => Promise.resolve('ok'));
    const { result, invalidate } = renderMutacion(mutationFn, onSuccess);

    act(() => result.current.mutate('x'));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(mutationFn).toHaveBeenCalledWith('x', expect.anything());
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('al tener éxito sin callback solo invalida', async () => {
    const { result, invalidate } = renderMutacion(() => Promise.resolve('ok'));

    act(() => result.current.mutate('x'));

    await waitFor(() => expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] }));
  });

  it('al fallar expone el mensaje traducido y reset lo limpia', async () => {
    const error = new ApiError('conflicto', 409, {
      code: 'COM_ORDEN_ESTADO_INVALIDO',
      detail: 'x',
      status: 409
    });
    const { result } = renderMutacion(() => Promise.reject(error));

    act(() => result.current.mutate('x'));

    await waitFor(() =>
      expect(result.current.mensajeError).toBe(
        'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
      )
    );

    act(() => result.current.reset());

    await waitFor(() => expect(result.current.mensajeError).toBeNull());
  });
});
