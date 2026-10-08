import { act, renderHook } from '@testing-library/react';
import { PUESTO_VACIO, cambiarTerminal, usePuestoTrabajo } from './use-puesto-trabajo';

afterEach(() => localStorage.clear());

describe('usePuestoTrabajo', () => {
  it('empieza vacío y recuerda el puesto elegido', () => {
    const primera = renderHook(() => usePuestoTrabajo());
    expect(primera.result.current.puesto).toEqual(PUESTO_VACIO);

    act(() =>
      primera.result.current.setPuesto({
        establecimientoId: 'est-1',
        terminalId: 'term-1',
        almacenId: 'alm-1'
      })
    );
    primera.unmount();

    const segunda = renderHook(() => usePuestoTrabajo());
    expect(segunda.result.current.puesto).toEqual({
      establecimientoId: 'est-1',
      terminalId: 'term-1',
      almacenId: 'alm-1'
    });
  });
});

describe('cambiarTerminal', () => {
  const puesto = { establecimientoId: 'est-1', terminalId: 'term-1', almacenId: 'alm-1' };

  it('conserva el almacén al elegir otra terminal del mismo establecimiento', () => {
    expect(cambiarTerminal(puesto, 'est-1', 'term-2')).toEqual({ ...puesto, terminalId: 'term-2' });
  });

  it('limpia el almacén al cambiar de establecimiento', () => {
    expect(cambiarTerminal(puesto, 'est-2', '')).toEqual({
      establecimientoId: 'est-2',
      terminalId: '',
      almacenId: ''
    });
  });
});
