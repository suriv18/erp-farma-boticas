import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { cambiarEstadoDispositivo, fetchDispositivos } from './dispositivos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const SAMPLE_DEVICE = {
  id: 'device-1',
  companyId: 'company-1',
  establishmentId: 'establishment-1',
  terminalId: null,
  fingerprintHash: 'abc',
  certificateThumbprint: null,
  agentVersion: '1.0.0',
  status: 'PENDIENTE',
  registeredAt: '2026-09-01T00:00:00Z',
  lastContactAt: null
};

describe('dispositivos.api', () => {
  it('fetchDispositivos consulta /dispositivos con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/dispositivos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([SAMPLE_DEVICE]);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchDispositivos(client, 'tenant-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toHaveLength(1);
    expect(result[0]?.id).toBe('device-1');
  });

  it('cambiarEstadoDispositivo envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.patch('http://localhost/api/v1/dispositivos/device-1/estado', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoDispositivo(client, 'device-1', 'tenant-1', 'CONFIABLE');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ status: 'CONFIABLE' });
  });
});
