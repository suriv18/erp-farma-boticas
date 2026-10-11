import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { actualizarMarca, cambiarEstadoMarca, crearMarca, fetchMarcas } from './marcas.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleMarca = {
  id: 'marca-1',
  tenantId: 'tenant-1',
  codigo: 'BAYER',
  nombre: 'Bayer',
  descripcion: null,
  estado: 'ACTIVO'
};

describe('marcas.api', () => {
  it('fetchMarcas consulta /catalogo/marcas con page y size por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/marcas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleMarca], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchMarcas(client, {});

    expect(receivedUrl?.searchParams.has('tenantId')).toBe(false);
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleMarca]);
  });

  it('fetchMarcas envia q, page y size cuando se especifican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/marcas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleMarca], page: 1, size: 10, totalElements: 15 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchMarcas(client, { q: 'bay', page: 1, size: 10 });

    expect(receivedUrl?.searchParams.get('q')).toBe('bay');
    expect(receivedUrl?.searchParams.get('page')).toBe('1');
    expect(receivedUrl?.searchParams.get('size')).toBe('10');
    expect(result.totalElements).toBe(15);
  });

  it('crearMarca envia el payload y devuelve la marca creada', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/catalogo/marcas', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleMarca, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearMarca(client, {
      codigo: 'BAYER',
      nombre: 'Bayer'
    });

    expect(receivedBody).toEqual({ codigo: 'BAYER', nombre: 'Bayer' });
    expect(result).toEqual(sampleMarca);
  });

  it('actualizarMarca envia PUT con el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/catalogo/marcas/marca-1', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleMarca, nombre: 'Bayer S.A.' });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await actualizarMarca(client, 'marca-1', {
      codigo: 'BAYER',
      nombre: 'Bayer S.A.'
    });

    expect(receivedBody).toEqual({ codigo: 'BAYER', nombre: 'Bayer S.A.' });
    expect(result.nombre).toBe('Bayer S.A.');
  });

  it('cambiarEstadoMarca envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch('http://localhost/api/v1/catalogo/marcas/marca-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoMarca(client, 'marca-1', 'INACTIVO');

    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });
});
