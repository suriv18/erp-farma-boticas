import { act, renderHook } from '@testing-library/react';
import { usePreferenciaLocal } from './use-preferencia-local';

afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('usePreferenciaLocal', () => {
  it('usa el valor inicial cuando no hay nada guardado', () => {
    const { result } = renderHook(() => usePreferenciaLocal('clave', { a: 1 }));

    expect(result.current[0]).toEqual({ a: 1 });
  });

  it('guarda y recupera el valor entre montajes', () => {
    const primera = renderHook(() => usePreferenciaLocal('clave', { a: 1 }));
    act(() => primera.result.current[1]({ a: 2 }));
    primera.unmount();

    const segunda = renderHook(() => usePreferenciaLocal('clave', { a: 1 }));

    expect(segunda.result.current[0]).toEqual({ a: 2 });
  });

  it('ignora un valor guardado que no es JSON válido', () => {
    localStorage.setItem('clave', '{roto');

    const { result } = renderHook(() => usePreferenciaLocal('clave', 'inicial'));

    expect(result.current[0]).toBe('inicial');
  });

  it('sigue funcionando en memoria cuando el almacenamiento falla', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    const { result } = renderHook(() => usePreferenciaLocal('clave', 'inicial'));

    act(() => result.current[1]('nuevo'));

    expect(result.current[0]).toBe('nuevo');
  });
});
