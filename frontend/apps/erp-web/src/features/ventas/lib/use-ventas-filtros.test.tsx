import { act, renderHook } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { MemoryRouter, useLocation } from 'react-router';
import { useVentasFiltros } from './use-ventas-filtros';

function renderFiltros(entrada: string) {
  const wrapper = ({ children }: PropsWithChildren) => (
    <MemoryRouter initialEntries={[entrada]}>{children}</MemoryRouter>
  );
  return renderHook(() => ({ ...useVentasFiltros(), location: useLocation() }), { wrapper });
}

describe('useVentasFiltros', () => {
  it('usa valores por defecto sin parámetros en la URL', () => {
    const { result } = renderFiltros('/ventas');

    expect(result.current.filtros).toEqual({
      establecimientoId: '',
      desde: '',
      hasta: '',
      page: 0,
      size: 20
    });
  });

  it('lee los filtros de la URL', () => {
    const { result } = renderFiltros(
      '/ventas?establecimientoId=est-1&desde=2026-10-01&hasta=2026-10-03&page=2&size=50'
    );

    expect(result.current.filtros).toEqual({
      establecimientoId: 'est-1',
      desde: '2026-10-01',
      hasta: '2026-10-03',
      page: 2,
      size: 50
    });
  });

  it.each([
    ['tamaño cero', '?size=0'],
    ['tamaño sobre el máximo', '?size=101'],
    ['tamaño no numérico', '?size=abc'],
    ['página negativa', '?page=-1'],
    ['página fraccionaria', '?page=1.5']
  ])('con %s usa los valores por defecto', (_nombre, search) => {
    const { result } = renderFiltros(`/ventas${search}`);

    expect(result.current.filtros.page).toBe(0);
    expect(result.current.filtros.size).toBe(20);
  });

  it('establecimiento, desde y hasta escriben el parámetro y reinician la página', () => {
    const { result } = renderFiltros('/ventas?page=4');

    act(() => result.current.setEstablecimiento('est-1'));
    expect(result.current.location.search).toBe('?establecimientoId=est-1');

    act(() => result.current.setDesde('2026-10-01'));
    expect(result.current.location.search).toBe('?establecimientoId=est-1&desde=2026-10-01');

    act(() => result.current.setHasta('2026-10-03'));
    expect(result.current.location.search).toBe(
      '?establecimientoId=est-1&desde=2026-10-01&hasta=2026-10-03'
    );
  });

  it('vaciar la fecha desde elimina el parámetro', () => {
    const { result } = renderFiltros('/ventas?desde=2026-10-01');

    act(() => result.current.setDesde(''));

    expect(result.current.location.search).toBe('');
  });

  it('setPage escribe la página y setSize reinicia la página', () => {
    const { result } = renderFiltros('/ventas');

    act(() => result.current.setPage(3));
    expect(result.current.location.search).toBe('?page=3');

    act(() => result.current.setSize(50));
    expect(result.current.location.search).toBe('?size=50');
  });

  it('dos actualizaciones en el mismo tick conservan ambos cambios', () => {
    const { result } = renderFiltros('/ventas');

    act(() => {
      result.current.setDesde('2026-10-01');
      result.current.setHasta('2026-10-03');
    });

    expect(result.current.location.search).toBe('?desde=2026-10-01&hasta=2026-10-03');
  });
});
