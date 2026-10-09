import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { useEstablecimientos, useEstablecimientosEstado } from './use-establecimientos';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useEstablecimientos', () => {
  it('devuelve una lista vacía mientras carga y luego los establecimientos de todas las empresas', async () => {
    server.use(
      http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura))
    );

    const { result } = renderHook(() => useEstablecimientos(), { wrapper });

    expect(result.current).toEqual([]);
    await waitFor(() => expect(result.current.map(({ id }) => id)).toEqual(['est-1', 'est-2']));
  });
});

describe('useEstablecimientosEstado', () => {
  it('informa la carga y luego los establecimientos', async () => {
    server.use(
      http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura))
    );

    const { result } = renderHook(() => useEstablecimientosEstado(), { wrapper });

    expect(result.current).toMatchObject({ establecimientos: [], isPending: true, isError: false });
    await waitFor(() => expect(result.current.isPending).toBe(false));
    expect(result.current.establecimientos.map(({ id }) => id)).toEqual(['est-1', 'est-2']);
  });

  it('informa el error de la consulta', async () => {
    server.use(
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ title: 'Error' }, { status: 500 })
      )
    );

    const { result } = renderHook(() => useEstablecimientosEstado(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.establecimientos).toEqual([]);
  });
});
