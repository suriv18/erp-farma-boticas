import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { fetchPermisos, permisosQuery } from './permisos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const PAGE_RESPONSE = {
  items: [
    {
      moduleCode: 'SEGURIDAD',
      moduleName: 'Seguridad',
      code: 'seguridad.usuarios.consultar',
      resource: 'USUARIO',
      action: 'CONSULTAR',
      name: 'Consultar usuarios',
      description: null,
      critical: false,
      status: 'ACTIVO'
    }
  ],
  page: 0,
  size: 20,
  totalElements: 1
};

describe('permisos.api', () => {
  it('fetchPermisos consulta /permisos con el filtro de busqueda y la paginacion', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/permisos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json(PAGE_RESPONSE);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchPermisos(client, { search: 'usuarios', page: 1, size: 10 });

    expect(receivedUrl?.searchParams.get('search')).toBe('usuarios');
    expect(receivedUrl?.searchParams.get('page')).toBe('1');
    expect(receivedUrl?.searchParams.get('size')).toBe('10');
    expect(result.items).toHaveLength(1);
    expect(result.totalElements).toBe(1);
  });

  it('fetchPermisos omite el parametro search cuando no se provee y usa paginacion por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/permisos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await fetchPermisos(client, {});

    expect(receivedUrl?.searchParams.has('search')).toBe(false);
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
  });

  it('permisosQuery arma la queryKey con los valores por defecto cuando no se pasan', () => {
    const { queryKey } = permisosQuery({});

    expect(queryKey).toEqual(['seguridad', 'permisos', '', 0, 20]);
  });
});
