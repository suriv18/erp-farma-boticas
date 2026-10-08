import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { login, logout, refresh } from './auth.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleResponse = {
  accessToken: 'access-123',
  refreshToken: 'refresh-123',
  tokenType: 'Bearer',
  accessExpiresAt: '2026-09-05T10:10:00Z',
  refreshExpiresAt: '2026-09-12T10:00:00Z',
  tenantId: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  sessionId: '33333333-3333-3333-3333-333333333333',
  passwordChangeRequired: false
};

describe('auth.api', () => {
  it('login envia login, password y channel WEB, y devuelve el token', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/auth/login', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleResponse);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await login(client, {
      login: 'admin@boticas.pe',
      password: 'Boticas2026!'
    });

    expect(receivedBody).toEqual({
      login: 'admin@boticas.pe',
      password: 'Boticas2026!',
      channel: 'WEB'
    });
    expect(result).toEqual(sampleResponse);
  });

  it('refresh envia el refreshToken y devuelve el nuevo token', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/auth/refresh', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleResponse);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await refresh(client, 'refresh-123');

    expect(receivedBody).toEqual({ refreshToken: 'refresh-123' });
    expect(result).toEqual(sampleResponse);
  });

  it('logout llama al endpoint sin body', async () => {
    let called = false;
    server.use(
      http.post('http://localhost/api/v1/auth/logout', () => {
        called = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await logout(client);

    expect(called).toBe(true);
  });
});
