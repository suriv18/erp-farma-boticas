import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from './client';

const server = setupServer(
  http.get('http://localhost/api/v1/health', () => HttpResponse.json({ status: 'ok' })),
  http.post('http://localhost/api/v1/resources', async ({ request }) =>
    HttpResponse.json(await request.json(), { status: 201 })
  )
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('createApiClient', () => {
  it('returns parsed JSON for a successful response', async () => {
    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await expect(client.get<{ status: string }>('/health')).resolves.toEqual({ status: 'ok' });
  });

  it('sends JSON commands using the versioned base URL', async () => {
    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await expect(client.post('/resources', { name: 'lote' })).resolves.toEqual({ name: 'lote' });
  });

  describe('createApiClient with auth hooks', () => {
    it('adjunta el header Authorization cuando getAccessToken devuelve un token', async () => {
      let receivedAuthHeader: string | null = null;
      server.use(
        http.get('http://localhost/api/v1/protected', ({ request }) => {
          receivedAuthHeader = request.headers.get('authorization');
          return HttpResponse.json({ ok: true });
        })
      );

      const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
      client.setAuthHooks({
        getAccessToken: () => 'token-123',
        onUnauthorized: async () => null
      });

      await client.get('/protected');
      expect(receivedAuthHeader).toBe('Bearer token-123');
    });

    it('reintenta la request una vez cuando recibe 401 y onUnauthorized devuelve un nuevo token', async () => {
      let attempt = 0;
      server.use(
        http.get('http://localhost/api/v1/protected', ({ request }) => {
          attempt += 1;
          const auth = request.headers.get('authorization');
          if (attempt === 1) return HttpResponse.json({}, { status: 401 });
          if (auth === 'Bearer refreshed-token') return HttpResponse.json({ ok: true });
          return HttpResponse.json({}, { status: 401 });
        })
      );

      const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
      client.setAuthHooks({
        getAccessToken: () => 'expired-token',
        onUnauthorized: async () => 'refreshed-token'
      });

      await expect(client.get('/protected')).resolves.toEqual({ ok: true });
      expect(attempt).toBe(2);
    });

    it('relanza el 401 original cuando onUnauthorized no puede renovar el token', async () => {
      server.use(
        http.get('http://localhost/api/v1/protected', () => HttpResponse.json({}, { status: 401 }))
      );

      const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
      client.setAuthHooks({
        getAccessToken: () => 'expired-token',
        onUnauthorized: async () => null
      });

      await expect(client.get('/protected')).rejects.toMatchObject({ status: 401 });
    });
  });
});
