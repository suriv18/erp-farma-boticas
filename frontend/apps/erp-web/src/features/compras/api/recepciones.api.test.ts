import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleRecepcion } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  fetchRecepcion,
  fetchRecepciones,
  recepcionesQuery,
  registrarRecepcion
} from './recepciones.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('recepciones.api', () => {
  it('registrarRecepcion hace POST con el cuerpo y la Idempotency-Key', async () => {
    let body: unknown;
    let clave: string | null = null;
    server.use(
      http.post('http://localhost/api/v1/compras/recepciones', async ({ request }) => {
        clave = request.headers.get('Idempotency-Key');
        body = await request.json();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const payload = {
      ordenCompraId: 'orden-1',
      almacenId: 'alm-1',
      documentoProveedorTipo: '01',
      items: [
        {
          numeroLineaOrden: 1,
          numeroLote: 'L2026-01',
          fechaVencimiento: '2028-12-31',
          cantidadRecibida: 4,
          cantidadRechazada: 0,
          costoUnitario: 5.5
        }
      ]
    };

    await expect(registrarRecepcion(client, payload, 'clave-1')).resolves.toEqual(sampleRecepcion);
    expect(body).toEqual(payload);
    expect(clave).toBe('clave-1');
  });

  it('fetchRecepciones envía la orden, la página y el tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/recepciones', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleRecepcion]));
      })
    );

    const result = await fetchRecepciones(client, { ordenCompraId: 'orden-1', page: 1, size: 50 });

    expect(recibido?.search).toBe('?ordenCompraId=orden-1&page=1&size=50');
    expect(result.items).toEqual([sampleRecepcion]);
  });

  it('fetchRecepciones usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/recepciones', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchRecepciones(client, { ordenCompraId: 'orden-1' });

    expect(recibido?.search).toBe('?ordenCompraId=orden-1&page=0&size=20');
  });

  it('fetchRecepcion obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/compras/recepciones/rec-1', () =>
        HttpResponse.json(sampleRecepcion)
      )
    );

    await expect(fetchRecepcion(client, 'rec-1')).resolves.toEqual(sampleRecepcion);
  });

  it('recepcionesQuery arma la clave bajo compras y consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValueOnce(pagina([sampleRecepcion]));
    const queryClient = new QueryClient();
    const consulta = recepcionesQuery({ ordenCompraId: 'orden-1', size: 100 });

    await queryClient.fetchQuery(consulta);

    expect(consulta.queryKey).toEqual(['compras', 'recepciones', 'orden', 'orden-1', 0, 100]);
    expect(recepcionesQuery({ ordenCompraId: 'orden-1' }).queryKey).toEqual([
      'compras',
      'recepciones',
      'orden',
      'orden-1',
      0,
      20
    ]);
    expect(get).toHaveBeenCalledWith('/compras/recepciones?ordenCompraId=orden-1&page=0&size=100');
  });
});
