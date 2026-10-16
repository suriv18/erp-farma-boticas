import { act, renderHook } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { MemoryRouter, useLocation } from 'react-router';
import { enteroEnRango, useParametrosUrl } from './use-filtros-url';

function renderParametros(entrada: string) {
  const wrapper = ({ children }: PropsWithChildren) => (
    <MemoryRouter initialEntries={[entrada]}>{children}</MemoryRouter>
  );
  return renderHook(() => ({ ...useParametrosUrl(), location: useLocation() }), { wrapper });
}

describe('enteroEnRango', () => {
  it.each([
    ['nulo', null, 7],
    ['vacío', '', 7],
    ['no entero', 'abc', 7],
    ['fraccionario', '1.5', 7],
    ['bajo el mínimo', '0', 7],
    ['sobre el máximo', '101', 7],
    ['válido', '50', 50],
    ['en el límite', '100', 100]
  ])('con valor %s devuelve el resultado esperado', (_nombre, valor, esperado) => {
    expect(enteroEnRango(valor, 1, 100, 7)).toBe(esperado);
  });
});

describe('useParametrosUrl', () => {
  it('expone los parámetros de la URL', () => {
    const { result } = renderParametros('/x?a=1');

    expect(result.current.params.get('a')).toBe('1');
  });

  it('actualizar establece y elimina con cadena vacía', () => {
    const { result } = renderParametros('/x?a=1&b=2');

    act(() => result.current.actualizar({ a: '', c: '3' }));

    expect(result.current.location.search).toBe('?b=2&c=3');
  });

  it('actualizar acumula cambios en el mismo tick', () => {
    const { result } = renderParametros('/x');

    act(() => {
      result.current.actualizar({ a: '1' });
      result.current.actualizar({ b: '2' });
    });

    expect(result.current.location.search).toBe('?a=1&b=2');
  });
});
