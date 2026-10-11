import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { server } from '../../../test/mocks/server';
import { condicionesVentaApi } from '../support-catalog/configs/condiciones-venta.config';
import {
  etiquetaDe,
  opcionesDe,
  opcionesDeValores,
  useOpcionesCategorias,
  useOpcionesMarcas,
  useOpcionesPrincipiosActivos,
  useOpcionesProductosRegulados,
  useOpcionesSoporte
} from './use-opciones';

function wrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

function pagina(items: unknown[]) {
  return { items, page: 0, size: 100, totalElements: items.length };
}

describe('opcionesDe', () => {
  it('mapea los items y tolera la ausencia de datos', () => {
    expect(opcionesDe(undefined, String, String)).toEqual([]);
    expect(
      opcionesDe(
        [{ id: 'a', nombre: 'Alfa' }],
        (item) => item.id,
        (item) => item.nombre
      )
    ).toEqual([{ value: 'a', label: 'Alfa' }]);
  });
});

describe('opcionesDeValores', () => {
  it('usa cada valor como valor y etiqueta', () => {
    expect(opcionesDeValores(['A', 'B'])).toEqual([
      { value: 'A', label: 'A' },
      { value: 'B', label: 'B' }
    ]);
  });
});

describe('etiquetaDe', () => {
  it('devuelve la etiqueta de la opcion o el propio valor como respaldo', () => {
    const opciones = [{ value: 'a', label: 'Alfa' }];

    expect(etiquetaDe(opciones, 'a')).toBe('Alfa');
    expect(etiquetaDe(opciones, 'z')).toBe('z');
  });
});

describe('use-opciones', () => {
  it('useOpcionesSoporte lista los codigos activos del catalogo de soporte', async () => {
    server.use(
      http.get('*/api/v1/catalogo/condiciones-venta', ({ request }) => {
        expect(new URL(request.url).searchParams.get('estado')).toBe('ACTIVO');
        return HttpResponse.json([
          { codigo: 'OTC', denominacion: 'Venta libre', estado: 'ACTIVO' }
        ]);
      })
    );

    const { result } = renderHook(() => useOpcionesSoporte(condicionesVentaApi), {
      wrapper: wrapper()
    });

    await waitFor(() =>
      expect(result.current).toEqual([{ value: 'OTC', label: 'OTC — Venta libre' }])
    );
  });

  it('useOpcionesMarcas y useOpcionesCategorias consultan solo activos sin enviar tenant', async () => {
    const consultas: string[] = [];
    server.use(
      http.get('*/api/v1/catalogo/marcas', ({ request }) => {
        consultas.push(new URL(request.url).search);
        return HttpResponse.json(pagina([{ id: 'm-1', nombre: 'Bayer' }]));
      }),
      http.get('*/api/v1/catalogo/categorias', ({ request }) => {
        consultas.push(new URL(request.url).search);
        return HttpResponse.json(pagina([{ id: 'c-1', nombre: 'Analgésicos' }]));
      })
    );

    const marcas = renderHook(() => useOpcionesMarcas(), { wrapper: wrapper() });
    const categorias = renderHook(() => useOpcionesCategorias(), { wrapper: wrapper() });

    await waitFor(() => expect(marcas.result.current).toEqual([{ value: 'm-1', label: 'Bayer' }]));
    await waitFor(() =>
      expect(categorias.result.current).toEqual([{ value: 'c-1', label: 'Analgésicos' }])
    );
    expect(consultas).toEqual(['?estado=ACTIVO&page=0&size=100', '?estado=ACTIVO&page=0&size=100']);
  });

  it('useOpcionesProductosRegulados y useOpcionesPrincipiosActivos mapean sus listados', async () => {
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados', () =>
        HttpResponse.json(pagina([{ id: 'pr-1', denominacion: 'Paracetamol 500 mg' }]))
      ),
      http.get('*/api/v1/catalogo/principios-activos', () =>
        HttpResponse.json([{ id: 'pa-1', denominacion: 'Paracetamol' }])
      )
    );

    const productos = renderHook(() => useOpcionesProductosRegulados(), {
      wrapper: wrapper()
    });
    const principios = renderHook(() => useOpcionesPrincipiosActivos(), {
      wrapper: wrapper()
    });

    await waitFor(() =>
      expect(productos.result.current).toEqual([{ value: 'pr-1', label: 'Paracetamol 500 mg' }])
    );
    await waitFor(() =>
      expect(principios.result.current).toEqual([{ value: 'pa-1', label: 'Paracetamol' }])
    );
  });
});
