import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { createSupportCatalogApi } from './support-catalog.api';
import type { SupportCatalogItem } from './support-catalog.types';

type SampleItem = SupportCatalogItem & { denominacion: string };
type SampleRequest = { codigo: string; denominacion: string };

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleItem: SampleItem = { codigo: 'ORAL', denominacion: 'Vía oral', estado: 'ACTIVO' };

describe('createSupportCatalogApi', () => {
  const api = createSupportCatalogApi<SampleItem, SampleRequest>('vias-administracion');

  it('fetchList consulta el recurso sin filtro de estado', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/vias-administracion', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleItem]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.fetchList(client);

    expect(receivedUrl?.searchParams.get('estado')).toBeNull();
    expect(result).toEqual([sampleItem]);
  });

  it('fetchList envia el filtro de estado cuando se especifica', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/vias-administracion', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleItem]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await api.fetchList(client, 'ACTIVO');

    expect(receivedUrl?.searchParams.get('estado')).toBe('ACTIVO');
  });

  it('fetchOne consulta el recurso por codigo', async () => {
    server.use(
      http.get('http://localhost/api/v1/catalogo/vias-administracion/ORAL', () =>
        HttpResponse.json(sampleItem)
      )
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.fetchOne(client, 'ORAL');

    expect(result).toEqual(sampleItem);
  });

  it('create envia POST con el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/catalogo/vias-administracion', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleItem, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.create(client, { codigo: 'ORAL', denominacion: 'Vía oral' });

    expect(receivedBody).toEqual({ codigo: 'ORAL', denominacion: 'Vía oral' });
    expect(result).toEqual(sampleItem);
  });

  it('update envia PUT con el payload al codigo indicado', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/catalogo/vias-administracion/ORAL', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleItem, denominacion: 'Vía oral estricta' });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await api.update(client, 'ORAL', { codigo: 'ORAL', denominacion: 'Vía oral estricta' });

    expect(receivedBody).toEqual({ codigo: 'ORAL', denominacion: 'Vía oral estricta' });
    expect(result.denominacion).toBe('Vía oral estricta');
  });

  it('changeStatus envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch('http://localhost/api/v1/catalogo/vias-administracion/ORAL/estado', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await api.changeStatus(client, 'ORAL', 'INACTIVO');

    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });

  it('listQuery arma una queryKey que incluye el resource y el estado', () => {
    const options = api.listQuery('ACTIVO');
    expect(options.queryKey).toEqual(['catalogo', 'vias-administracion', 'ACTIVO']);
  });
});

describe('createSupportCatalogApi con paginated:true', () => {
  const paginatedApi = createSupportCatalogApi<SampleItem, SampleRequest>('tipos-documento-identidad', {
    paginated: true
  });

  it('fetchList extrae items de una respuesta paginada y pide una pagina grande', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/tipos-documento-identidad', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleItem], page: 0, size: 100, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await paginatedApi.fetchList(client);

    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('100');
    expect(result).toEqual([sampleItem]);
  });
});
