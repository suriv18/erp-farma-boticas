import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { pagina } from '../../../test/organizacion-fixtures';
import { samplePosicion } from '../../../test/inventario-fixtures';
import { fetchPosiciones, posicionesQuery } from './posiciones.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('posiciones.api', () => {
  it('fetchPosiciones envía filtros, página y tamaño', async () => {
    let received: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/inventario/posiciones', ({ request }) => {
        received = new URL(request.url);
        return HttpResponse.json(pagina([samplePosicion]));
      })
    );

    const result = await fetchPosiciones(client, {
      establecimientoId: 'est-1',
      almacenId: 'alm-1',
      skuId: 'sku-1',
      page: 2,
      size: 50
    });

    expect(received?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(received?.searchParams.get('almacenId')).toBe('alm-1');
    expect(received?.searchParams.get('skuId')).toBe('sku-1');
    expect(received?.searchParams.get('page')).toBe('2');
    expect(received?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([samplePosicion]);
  });

  it('fetchPosiciones omite los filtros ausentes y usa paginación por defecto', async () => {
    let received: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/inventario/posiciones', ({ request }) => {
        received = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchPosiciones(client, {});

    expect(received?.search).toBe('?page=0&size=20');
  });

  it('posicionesQuery arma la clave con valores por defecto', () => {
    expect(posicionesQuery({}).queryKey).toEqual([
      'inventario',
      'posiciones',
      'lista',
      '',
      '',
      '',
      0,
      20
    ]);
  });

  it('posicionesQuery arma la clave con los filtros indicados', () => {
    expect(
      posicionesQuery({
        establecimientoId: 'est-1',
        almacenId: 'alm-1',
        skuId: 'sku-1',
        page: 1,
        size: 10
      }).queryKey
    ).toEqual(['inventario', 'posiciones', 'lista', 'est-1', 'alm-1', 'sku-1', 1, 10]);
  });

  it('posicionesQuery consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(pagina([samplePosicion]));

    const result = await new QueryClient().fetchQuery(posicionesQuery({ almacenId: 'alm-1' }));

    expect(get).toHaveBeenCalledWith('/inventario/posiciones?almacenId=alm-1&page=0&size=20');
    expect(result.items).toEqual([samplePosicion]);
  });
});
