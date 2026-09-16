import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { fetchPermisos } from './permisos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('permisos.api', () => {
  it('fetchPermisos consulta /permisos con el filtro de busqueda', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/permisos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([
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
        ]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchPermisos(client, 'usuarios');

    expect(receivedUrl?.searchParams.get('search')).toBe('usuarios');
    expect(result).toHaveLength(1);
    expect(result[0]?.code).toBe('seguridad.usuarios.consultar');
  });

  it('fetchPermisos omite el parametro search cuando no se provee', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/permisos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await fetchPermisos(client);

    expect(receivedUrl?.searchParams.has('search')).toBe(false);
  });
});
