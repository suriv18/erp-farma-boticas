import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  actualizarSku,
  agregarCodigoBarra,
  cambiarEstadoSku,
  crearSku,
  eliminarCodigoBarra,
  fetchSku,
  fetchSkus,
  marcarCodigoBarraPrincipal,
  skuQuery,
  skusQuery
} from './skus.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
const base = 'http://localhost/api/v1/catalogo/skus';

const payload = {
  tipoSku: 'NO_REGULADO',
  codigoInterno: 'SKU-1',
  descripcionComercial: 'Alcohol 70%',
  permiteVentaFraccion: false,
  requiereLote: true,
  requiereVencimiento: true,
  afectoIgv: true,
  stockMinimoDefault: 0
};

describe('skus.api', () => {
  it('fetchSkus usa pagina 0 y tamano 20 por defecto sin enviar tenant', async () => {
    let url: URL | undefined;
    server.use(
      http.get(base, ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 });
      })
    );

    await fetchSkus(client, {});

    expect(Object.fromEntries(url?.searchParams ?? [])).toEqual({
      page: '0',
      size: '20'
    });
  });

  it('fetchSkus envia los filtros indicados', async () => {
    let url: URL | undefined;
    server.use(
      http.get(base, ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json({ items: [], page: 1, size: 50, totalElements: 0 });
      })
    );

    await fetchSkus(client, {
      q: 'alc',
      categoriaId: 'c-1',
      marcaId: 'm-1',
      tipoSku: 'REGULADO',
      estado: 'ACTIVO',
      page: 1,
      size: 50
    });

    expect(Object.fromEntries(url?.searchParams ?? [])).toEqual({
      q: 'alc',
      categoriaId: 'c-1',
      marcaId: 'm-1',
      tipoSku: 'REGULADO',
      estado: 'ACTIVO',
      page: '1',
      size: '50'
    });
  });

  it('skusQuery construye la clave con filtros y paginacion', () => {
    expect(skusQuery({}).queryKey).toEqual([
      'catalogo',
      'skus',
      'lista',
      '',
      '',
      '',
      '',
      '',
      0,
      20
    ]);
    expect(
      skusQuery({
        q: 'a',
        categoriaId: 'c',
        marcaId: 'm',
        tipoSku: 'REGULADO',
        estado: 'ACTIVO',
        page: 2,
        size: 50
      }).queryKey
    ).toEqual(['catalogo', 'skus', 'lista', 'a', 'c', 'm', 'REGULADO', 'ACTIVO', 2, 50]);
  });

  it('fetchSku y skuQuery consultan el detalle sin tenant', async () => {
    let search = '';
    server.use(
      http.get(`${base}/sku-1`, ({ request }) => {
        search = new URL(request.url).search;
        return HttpResponse.json({ id: 'sku-1' });
      })
    );

    await expect(fetchSku(client, 'sku-1')).resolves.toEqual({ id: 'sku-1' });
    expect(search).toBe('');
    expect(skuQuery('sku-1').queryKey).toEqual(['catalogo', 'skus', 'detalle', 'sku-1']);
  });

  it('crearSku y actualizarSku envian el payload', async () => {
    const bodies: unknown[] = [];
    server.use(
      http.post(base, async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json({ id: 'sku-1' }, { status: 201 });
      }),
      http.put(`${base}/sku-1`, async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json({ id: 'sku-1' });
      })
    );

    await crearSku(client, payload);
    await actualizarSku(client, 'sku-1', payload);

    expect(bodies).toEqual([payload, payload]);
  });

  it('cambiarEstadoSku envia solo el estado en el cuerpo', async () => {
    let body: unknown;
    server.use(
      http.patch(`${base}/sku-1/estado`, async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    await cambiarEstadoSku(client, 'sku-1', 'BLOQUEADO');

    expect(body).toEqual({ status: 'BLOQUEADO' });
  });

  it('gestiona los codigos de barra sin tenant en la consulta', async () => {
    const llamadas: string[] = [];
    let body: unknown;
    server.use(
      http.post(`${base}/sku-1/codigos-barra`, async ({ request }) => {
        llamadas.push(`POST ${new URL(request.url).search}`);
        body = await request.json();
        return HttpResponse.json({ id: 'sku-1' });
      }),
      http.delete(`${base}/sku-1/codigos-barra/775%2F1`, ({ request }) => {
        llamadas.push(`DELETE ${new URL(request.url).search}`);
        return HttpResponse.json({ id: 'sku-1' });
      }),
      http.patch(`${base}/sku-1/codigos-barra/775%2F1/principal`, ({ request }) => {
        llamadas.push(`PATCH ${new URL(request.url).search}`);
        return HttpResponse.json({ id: 'sku-1' });
      })
    );

    await agregarCodigoBarra(client, 'sku-1', { codigoBarra: '7750001' });
    await eliminarCodigoBarra(client, 'sku-1', '775/1');
    await marcarCodigoBarraPrincipal(client, 'sku-1', '775/1');

    expect(body).toEqual({ codigoBarra: '7750001' });
    expect([...new Set(llamadas)]).toEqual(['POST ', 'DELETE ', 'PATCH ']);
  });
});
