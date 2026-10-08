import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  actualizarProductoRegulado,
  asociarPrincipioActivo,
  cambiarEstadoProductoRegulado,
  crearProductoRegulado,
  desasociarPrincipioActivo,
  fetchProductoRegulado,
  fetchProductosRegulados,
  productoReguladoQuery,
  productosReguladosQuery
} from './productos-regulados.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
const base = 'http://localhost/api/v1/catalogo/productos-regulados';

const resumen = {
  id: 'pr-1',
  denominacion: 'Paracetamol',
  condicionVentaCodigo: 'OTC',
  estadoRegulatorio: 'VIGENTE'
};

describe('productos-regulados.api', () => {
  it('fetchProductosRegulados usa pagina 0 y tamano 20 por defecto', async () => {
    let url: URL | undefined;
    server.use(
      http.get(base, ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json({ items: [resumen], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchProductosRegulados(client);

    expect(url?.searchParams.get('page')).toBe('0');
    expect(url?.searchParams.get('size')).toBe('20');
    expect(url?.searchParams.has('q')).toBe(false);
    expect(result.items).toEqual([resumen]);
  });

  it('fetchProductosRegulados envia los filtros indicados', async () => {
    let url: URL | undefined;
    server.use(
      http.get(base, ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json({ items: [], page: 2, size: 50, totalElements: 0 });
      })
    );

    await fetchProductosRegulados(client, {
      q: 'para',
      condicionVentaCodigo: 'OTC',
      estadoRegulatorio: 'VIGENTE',
      page: 2,
      size: 50
    });

    expect(Object.fromEntries(url?.searchParams ?? [])).toEqual({
      q: 'para',
      condicionVentaCodigo: 'OTC',
      estadoRegulatorio: 'VIGENTE',
      page: '2',
      size: '50'
    });
  });

  it('productosReguladosQuery construye la clave con filtros y paginacion', () => {
    expect(productosReguladosQuery().queryKey).toEqual([
      'catalogo',
      'productos-regulados',
      'lista',
      '',
      '',
      '',
      0,
      20
    ]);
    expect(
      productosReguladosQuery({
        q: 'a',
        condicionVentaCodigo: 'OTC',
        estadoRegulatorio: 'VIGENTE',
        page: 1,
        size: 50
      }).queryKey
    ).toEqual(['catalogo', 'productos-regulados', 'lista', 'a', 'OTC', 'VIGENTE', 1, 50]);
  });

  it('fetchProductoRegulado y productoReguladoQuery consultan el detalle', async () => {
    server.use(http.get(`${base}/pr-1`, () => HttpResponse.json({ id: 'pr-1' })));

    await expect(fetchProductoRegulado(client, 'pr-1')).resolves.toEqual({ id: 'pr-1' });
    expect(productoReguladoQuery('pr-1').queryKey).toEqual([
      'catalogo',
      'productos-regulados',
      'detalle',
      'pr-1'
    ]);
  });

  it('crearProductoRegulado y actualizarProductoRegulado envian el payload', async () => {
    const bodies: unknown[] = [];
    server.use(
      http.post(base, async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json({ id: 'pr-1' }, { status: 201 });
      }),
      http.put(`${base}/pr-1`, async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json({ id: 'pr-1' });
      })
    );
    const payload = { tipoProducto: 'FARMACEUTICO', denominacion: 'Paracetamol' };

    await crearProductoRegulado(client, payload);
    await actualizarProductoRegulado(client, 'pr-1', payload);

    expect(bodies).toEqual([payload, payload]);
  });

  it('cambiarEstadoProductoRegulado envia PATCH con el estado', async () => {
    let body: unknown;
    server.use(
      http.patch(`${base}/pr-1/estado`, async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    await cambiarEstadoProductoRegulado(client, 'pr-1', 'SUSPENDIDO');

    expect(body).toEqual({ status: 'SUSPENDIDO' });
  });

  it('asocia y desasocia principios activos', async () => {
    let body: unknown;
    let deleted = false;
    server.use(
      http.post(`${base}/pr-1/principios-activos`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ id: 'pr-1' });
      }),
      http.delete(`${base}/pr-1/principios-activos/pa-1`, () => {
        deleted = true;
        return HttpResponse.json({ id: 'pr-1' });
      })
    );

    await asociarPrincipioActivo(client, 'pr-1', {
      principioActivoId: 'pa-1',
      esPrincipal: true,
      orden: 1
    });
    await desasociarPrincipioActivo(client, 'pr-1', 'pa-1');

    expect(body).toEqual({ principioActivoId: 'pa-1', esPrincipal: true, orden: 1 });
    expect(deleted).toBe(true);
  });
});
