import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleOrden, sampleOrdenResumen } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  anularOrden,
  aprobarOrden,
  crearOrden,
  emitirOrden,
  fetchOrden,
  fetchOrdenes,
  ordenQuery,
  ordenesQuery
} from './ordenes.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('ordenes.api', () => {
  it('crearOrden hace POST con el cuerpo', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/compras/ordenes', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const payload = {
      proveedorId: 'prov-1',
      establecimientoDestinoId: 'est-1',
      moneda: 'PEN',
      diasCredito: 30,
      lineas: [
        {
          skuId: 'sku-0001-aaaa',
          cantidad: 10,
          unidadMedidaCodigo: 'UND',
          precioUnitario: 5.5,
          descuento: 0,
          impuesto: 9.9,
          toleranciaExcesoPct: 0,
          toleranciaDefectoPct: 0
        }
      ]
    };

    await expect(crearOrden(client, payload)).resolves.toEqual(sampleOrden);
    expect(body).toEqual(payload);
  });

  it('fetchOrdenes envía proveedor, estado, página y tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/ordenes', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleOrdenResumen]));
      })
    );

    const result = await fetchOrdenes(client, {
      proveedorId: 'prov-1',
      estado: 'EMITIDA',
      page: 1,
      size: 50
    });

    expect(recibido?.searchParams.get('proveedorId')).toBe('prov-1');
    expect(recibido?.searchParams.get('estado')).toBe('EMITIDA');
    expect(recibido?.searchParams.get('page')).toBe('1');
    expect(recibido?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([sampleOrdenResumen]);
  });

  it('fetchOrdenes omite los filtros ausentes y usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/ordenes', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchOrdenes(client, {});

    expect(recibido?.search).toBe('?page=0&size=20');
  });

  it('fetchOrden obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/compras/ordenes/orden-1', () =>
        HttpResponse.json(sampleOrden)
      )
    );

    await expect(fetchOrden(client, 'orden-1')).resolves.toEqual(sampleOrden);
  });

  it.each([
    ['aprobarOrden', 'aprobacion', aprobarOrden, 'APROBADA'],
    ['emitirOrden', 'emision', emitirOrden, 'EMITIDA']
  ] as const)('%s hace POST a /%s', async (_nombre, ruta, accion, estado) => {
    let metodo = '';
    server.use(
      http.post(`http://localhost/api/v1/compras/ordenes/orden-1/${ruta}`, ({ request }) => {
        metodo = request.method;
        return HttpResponse.json({ ...sampleOrden, estado });
      })
    );

    const result = await accion(client, 'orden-1');

    expect(metodo).toBe('POST');
    expect(result.estado).toBe(estado);
  });

  it('anularOrden envía el motivo', async () => {
    let body: unknown;
    server.use(
      http.post(
        'http://localhost/api/v1/compras/ordenes/orden-1/anulacion',
        async ({ request }) => {
          body = await request.json();
          return HttpResponse.json({ ...sampleOrden, estado: 'CANCELADA' });
        }
      )
    );

    const result = await anularOrden(client, 'orden-1', { motivo: 'Error de digitación' });

    expect(result.estado).toBe('CANCELADA');
    expect(body).toEqual({ motivo: 'Error de digitación' });
  });

  it('ordenesQuery arma la clave con valores por defecto y con filtros', () => {
    expect(ordenesQuery({}).queryKey).toEqual(['compras', 'ordenes', 'lista', '', '', 0, 20]);
    expect(
      ordenesQuery({ proveedorId: 'prov-1', estado: 'EMITIDA', page: 2, size: 10 }).queryKey
    ).toEqual(['compras', 'ordenes', 'lista', 'prov-1', 'EMITIDA', 2, 10]);
  });

  it('ordenesQuery y ordenQuery consultan con el cliente de la aplicación', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce(pagina([sampleOrdenResumen]))
      .mockResolvedValueOnce(sampleOrden);
    const queryClient = new QueryClient();

    await queryClient.fetchQuery(ordenesQuery({ estado: 'EMITIDA' }));
    const detalle = ordenQuery('orden-1');
    await queryClient.fetchQuery(detalle);

    expect(get).toHaveBeenNthCalledWith(1, '/compras/ordenes?estado=EMITIDA&page=0&size=20');
    expect(get).toHaveBeenNthCalledWith(2, '/compras/ordenes/orden-1');
    expect(detalle.queryKey).toEqual(['compras', 'ordenes', 'detalle', 'orden-1']);
  });
});
