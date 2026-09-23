import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { actualizarCategoria, cambiarEstadoCategoria, crearCategoria, fetchCategorias } from './categorias.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleCategoria = {
  id: 'categoria-1',
  tenantId: 'tenant-1',
  categoriaPadreId: null,
  codigo: 'ANALGESICOS',
  nombre: 'Analgésicos',
  descripcion: null,
  nivel: 1,
  orden: 1,
  estado: 'ACTIVO'
};

describe('categorias.api', () => {
  it('fetchCategorias consulta /catalogo/categorias con tenantId, page y size por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/categorias', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleCategoria], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchCategorias(client, { tenantId: 'tenant-1' });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleCategoria]);
  });

  it('fetchCategorias envia q, page y size cuando se especifican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/categorias', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleCategoria], page: 1, size: 10, totalElements: 12 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchCategorias(client, { tenantId: 'tenant-1', q: 'analg', page: 1, size: 10 });

    expect(receivedUrl?.searchParams.get('q')).toBe('analg');
    expect(receivedUrl?.searchParams.get('page')).toBe('1');
    expect(receivedUrl?.searchParams.get('size')).toBe('10');
    expect(result.totalElements).toBe(12);
  });

  it('crearCategoria envia el payload y devuelve la categoria creada', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/catalogo/categorias', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleCategoria, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearCategoria(client, {
      tenantId: 'tenant-1',
      codigo: 'ANALGESICOS',
      nombre: 'Analgésicos',
      nivel: 1,
      orden: 1
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      codigo: 'ANALGESICOS',
      nombre: 'Analgésicos',
      nivel: 1,
      orden: 1
    });
    expect(result).toEqual(sampleCategoria);
  });

  it('actualizarCategoria envia PUT con el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/catalogo/categorias/categoria-1', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleCategoria, nombre: 'Analgésicos y antipiréticos' });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await actualizarCategoria(client, 'categoria-1', {
      tenantId: 'tenant-1',
      codigo: 'ANALGESICOS',
      nombre: 'Analgésicos y antipiréticos',
      nivel: 1,
      orden: 1
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      codigo: 'ANALGESICOS',
      nombre: 'Analgésicos y antipiréticos',
      nivel: 1,
      orden: 1
    });
    expect(result.nombre).toBe('Analgésicos y antipiréticos');
  });

  it('cambiarEstadoCategoria envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch('http://localhost/api/v1/catalogo/categorias/categoria-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoCategoria(client, 'categoria-1', 'tenant-1', 'INACTIVO');

    expect(receivedBody).toEqual({ tenantId: 'tenant-1', status: 'INACTIVO' });
  });
});
