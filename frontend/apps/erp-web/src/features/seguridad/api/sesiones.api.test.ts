import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { fetchSesiones, revocarSesion } from './sesiones.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const SAMPLE_SESSION = {
  id: 'sesion-1',
  userId: 'user-1',
  provider: 'local',
  authMethod: 'PASSWORD',
  channel: 'WEB',
  ipAddress: '127.0.0.1',
  userAgent: 'vitest',
  deviceId: null,
  loginAt: '2026-09-01T00:00:00Z',
  lastUsedAt: null,
  expiresAt: null,
  logoutAt: null,
  revokedAt: null,
  revocationReason: null,
  status: 'ACTIVA'
};

describe('sesiones.api', () => {
  it('fetchSesiones consulta /sesiones con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/sesiones', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([SAMPLE_SESSION]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchSesiones(client, { tenantId: 'tenant-1' });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.has('userId')).toBe(false);
    expect(result).toHaveLength(1);
    expect(result[0]?.id).toBe('sesion-1');
  });

  it('fetchSesiones envia userId cuando se filtra por usuario', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/sesiones', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await fetchSesiones(client, { tenantId: 'tenant-1', userId: 'user-1' });

    expect(receivedUrl?.searchParams.get('userId')).toBe('user-1');
  });

  it('revocarSesion envia POST a /sesiones/{id}/revocacion con el motivo', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.post('http://localhost/api/v1/sesiones/sesion-1/revocacion', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await revocarSesion(client, 'sesion-1', 'tenant-1', 'Cierre manual');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ reason: 'Cierre manual' });
  });
});
