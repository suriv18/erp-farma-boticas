import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { pagina } from '../../../test/organizacion-fixtures';
import { sampleVenta, sampleVentaResumen } from '../../../test/ventas-fixtures';
import { fetchVenta, fetchVentas, registrarVenta, ventaQuery, ventasQuery } from './ventas.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('ventas.api', () => {
  it('fetchVentas envía filtros, página y tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/ventas/ventas', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleVentaResumen]));
      })
    );

    const result = await fetchVentas(client, {
      establecimientoId: 'est-1',
      desde: '2026-10-03T05:00:00.000Z',
      hasta: '2026-10-04T04:59:59.999Z',
      page: 1,
      size: 50
    });

    expect(recibido?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(recibido?.searchParams.get('desde')).toBe('2026-10-03T05:00:00.000Z');
    expect(recibido?.searchParams.get('hasta')).toBe('2026-10-04T04:59:59.999Z');
    expect(recibido?.searchParams.get('page')).toBe('1');
    expect(recibido?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([sampleVentaResumen]);
  });

  it('fetchVentas omite los filtros ausentes y usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/ventas/ventas', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchVentas(client, {});

    expect(recibido?.search).toBe('?page=0&size=20');
  });

  it('fetchVenta obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/ventas/ventas/venta-1', () =>
        HttpResponse.json(sampleVenta)
      )
    );

    await expect(fetchVenta(client, 'venta-1')).resolves.toEqual(sampleVenta);
  });

  it('registrarVenta envía el cuerpo con la Idempotency-Key', async () => {
    let body: unknown;
    let clave: string | null = null;
    server.use(
      http.post('http://localhost/api/v1/ventas/ventas', async ({ request }) => {
        body = await request.json();
        clave = request.headers.get('Idempotency-Key');
        return HttpResponse.json(sampleVenta, { status: 201 });
      })
    );
    const payload = {
      terminalId: 'term-1',
      almacenId: 'alm-1',
      lineas: [{ skuId: 'sku-0001-aaaa', cantidad: 2, precioUnitario: 12.5 }],
      pago: { montoRecibido: 30 }
    };

    await expect(registrarVenta(client, payload, 'clave-1')).resolves.toEqual(sampleVenta);
    expect(body).toEqual(payload);
    expect(clave).toBe('clave-1');
  });

  it('ventasQuery arma la clave con valores por defecto y con filtros', () => {
    expect(ventasQuery({}).queryKey).toEqual(['ventas', 'lista', '', '', '', 0, 20]);
    expect(
      ventasQuery({ establecimientoId: 'est-1', desde: 'd', hasta: 'h', page: 2, size: 10 })
        .queryKey
    ).toEqual(['ventas', 'lista', 'est-1', 'd', 'h', 2, 10]);
  });

  it('ventasQuery y ventaQuery consultan con el cliente de la aplicación', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce(pagina([sampleVentaResumen]))
      .mockResolvedValueOnce(sampleVenta);
    const queryClient = new QueryClient();

    await queryClient.fetchQuery(ventasQuery({ establecimientoId: 'est-1' }));
    const detalle = ventaQuery('venta-1');
    await queryClient.fetchQuery(detalle);

    expect(get).toHaveBeenNthCalledWith(1, '/ventas/ventas?establecimientoId=est-1&page=0&size=20');
    expect(get).toHaveBeenNthCalledWith(2, '/ventas/ventas/venta-1');
    expect(detalle.queryKey).toEqual(['ventas', 'detalle', 'venta-1']);
  });
});
