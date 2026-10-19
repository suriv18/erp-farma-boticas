import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { fetchModulos } from './modulos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('modulos.api', () => {
  it('fetchModulos consulta /modulos', async () => {
    let requested = false;
    server.use(
      http.get('http://localhost/api/v1/modulos', () => {
        requested = true;
        return HttpResponse.json([
          { code: 'SEGURIDAD', name: 'Seguridad', description: null, order: 1, active: true }
        ]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchModulos(client);

    expect(requested).toBe(true);
    expect(result).toHaveLength(1);
    expect(result[0]?.code).toBe('SEGURIDAD');
  });
});
