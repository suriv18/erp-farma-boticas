import { act, renderHook } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { MemoryRouter, useLocation, useNavigate } from 'react-router';
import { usePosicionesFiltros } from './use-posiciones-filtros';

function renderFiltros(entrada: string) {
  const wrapper = ({ children }: PropsWithChildren) => (
    <MemoryRouter initialEntries={[entrada]}>{children}</MemoryRouter>
  );
  return renderHook(
    () => ({ ...usePosicionesFiltros(), location: useLocation(), navigate: useNavigate() }),
    { wrapper }
  );
}

describe('usePosicionesFiltros', () => {
  it('usa valores por defecto sin parámetros en la URL', () => {
    const { result } = renderFiltros('/inventario');

    expect(result.current.filtros).toEqual({
      establecimientoId: '',
      almacenId: '',
      skuId: '',
      page: 0,
      size: 20
    });
  });

  it('lee los filtros de la URL', () => {
    const { result } = renderFiltros(
      '/inventario?establecimientoId=est-1&almacenId=alm-1&skuId=sku-1&page=3&size=50'
    );

    expect(result.current.filtros).toEqual({
      establecimientoId: 'est-1',
      almacenId: 'alm-1',
      skuId: 'sku-1',
      page: 3,
      size: 50
    });
  });

  it('cambiar el establecimiento limpia almacén y página', () => {
    const { result } = renderFiltros('/inventario?almacenId=alm-1&page=2');

    act(() => result.current.setEstablecimiento('est-2'));

    expect(result.current.location.search).toBe('?establecimientoId=est-2');
  });

  it('cambiar almacén o SKU reinicia la página y vaciar un filtro lo elimina', () => {
    const { result } = renderFiltros('/inventario?page=2&skuId=sku-1');

    act(() => result.current.setAlmacen('alm-1'));
    expect(result.current.location.search).toBe('?skuId=sku-1&almacenId=alm-1');

    act(() => result.current.setSku(''));
    expect(result.current.location.search).toBe('?almacenId=alm-1');
  });

  it('setPage escribe la página y setSize reinicia la página', () => {
    const { result } = renderFiltros('/inventario');

    act(() => result.current.setPage(2));
    expect(result.current.location.search).toBe('?page=2');

    act(() => result.current.setSize(50));
    expect(result.current.location.search).toBe('?size=50');
  });

  it('aplica tamaño y página en un mismo tick sin perder ninguno', () => {
    const { result } = renderFiltros('/inventario?page=2');

    act(() => {
      result.current.setSize(50);
      result.current.setPage(0);
    });

    expect(result.current.location.search).toBe('?size=50&page=0');
  });

  it('resincroniza los parámetros tras una navegación externa', () => {
    const { result } = renderFiltros('/inventario?skuId=sku-1');

    act(() => void result.current.navigate('/inventario?almacenId=alm-9'));
    act(() => result.current.setPage(1));

    expect(result.current.location.search).toBe('?almacenId=alm-9&page=1');
  });

  it.each([
    ['página no numérica', '?page=abc', 0, 20],
    ['página negativa', '?page=-1', 0, 20],
    ['página fraccionaria', '?page=1.5', 0, 20],
    ['página vacía', '?page=', 0, 20],
    ['tamaño no numérico', '?size=abc', 0, 20],
    ['tamaño cero', '?size=0', 0, 20],
    ['tamaño sobre el máximo', '?size=101', 0, 20],
    ['tamaño fraccionario', '?size=10.5', 0, 20],
    ['valores válidos en los límites', '?page=0&size=100', 0, 100],
    ['tamaño mínimo', '?page=7&size=1', 7, 1]
  ])('con %s usa los valores válidos o los de por defecto', (_nombre, search, page, size) => {
    const { result } = renderFiltros(`/inventario${search}`);

    expect(result.current.filtros.page).toBe(page);
    expect(result.current.filtros.size).toBe(size);
  });
});
